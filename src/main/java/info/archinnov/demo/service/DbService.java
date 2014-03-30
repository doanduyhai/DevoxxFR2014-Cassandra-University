package info.archinnov.demo.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import org.springframework.stereotype.Service;
import info.archinnov.achilles.persistence.PersistenceManager;
import info.archinnov.achilles.type.TypedMap;
import info.archinnov.demo.model.RateLimit;
import info.archinnov.demo.model.ValueWithTimestamp;

@Service
public class DbService {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private int threshold = 5;

    @Inject
    private PersistenceManager manager;


    public void setThresholdForRateLimit(int threshold) {
        this.threshold = threshold;
    }

    public void insertForRateLimit(String value, int ttl) {
        final TypedMap countMap = manager
                .nativeQuery("SELECT count(*) FROM ratelimit WHERE id='ratelimit' LIMIT 100")
                .first();
        if (countMap != null) {
            final Long count = (Long) countMap.get("count");
            if (count >= threshold) {
                throw new IllegalStateException("Vous ne pouvez pas avoir plus de " + threshold + " valeurs par tranche de " + ttl + " " +
                        "secondes");
            }
        }

        manager.nativeQuery("INSERT INTO ratelimit(id,column,value) VALUES('ratelimit','" + value + "'," +
                "'" + value + "') USING TTL " + ttl).first();
    }

    public List<RateLimit> fetchRateLimitedValues() {
        List<RateLimit> result = new ArrayList<>();
        final List<TypedMap> maps = manager
                .nativeQuery("SELECT value,ttl(value) FROM ratelimit WHERE id='ratelimit' LIMIT 100").get();
        for (TypedMap map : maps) {
            result.add(new RateLimit(map.<String>getTyped("value"), map.<Integer>getTyped("ttl(value)")));
        }
        return result;
    }

    public String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        return format.format(new Date());
    }

    public ValueWithTimestamp insertWithCurrentTimePlus(String value, int shiftInSecs) {
        long timeStampInMicros = getTimestampInMicros(shiftInSecs);

        manager.nativeQuery("INSERT INTO time_stamp(id,value) VALUES('time_stamp'," +
                "'" + value + "') USING TIMESTAMP " + timeStampInMicros).first();

        final TypedMap map = manager.nativeQuery("SELECT value,writetime(value) FROM time_stamp WHERE " +
                "id='time_stamp'").first();

        String readValue = map.getTyped("value");
        long timestamp = map.getTyped("writetime(value)");
        long shortTimestamp = timestamp / 1000;
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);

        return new ValueWithTimestamp(readValue, format.format(new Date(shortTimestamp)));
    }

    public String insertForWriteBarrier(String value) {
        manager.nativeQuery("INSERT INTO writebarrier(id,value) VALUES('writebarrier','" + value + "')").first();
        final TypedMap map = manager.nativeQuery("SELECT value FROM writebarrier WHERE id='writebarrier'")
                .first();
        if (map != null) {
            return map.getTyped("value");
        } else {
            return null;
        }
    }

    public void deleteWithWriteBarrier(int shiftInSecs) {
        long timestampInMicros = getTimestampInMicros(shiftInSecs);
        manager.nativeQuery("DELETE FROM writebarrier USING TIMESTAMP " + timestampInMicros + " WHERE " +
                "id='writebarrier'").first();
    }

    private long getTimestampInMicros(int shiftInSecs) {
        final long currentTimeMillis = System.currentTimeMillis();
        return (currentTimeMillis + 1000 * shiftInSecs) * 1000;
    }

    public void resetDb() {
        manager.nativeQuery("TRUNCATE countdown").execute();
        manager.nativeQuery("TRUNCATE ratelimit").execute();
        manager.nativeQuery("TRUNCATE time_stamp").execute();
        manager.nativeQuery("TRUNCATE writebarrier").execute();
    }
}
