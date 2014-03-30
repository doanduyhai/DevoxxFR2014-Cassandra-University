package info.archinnov.demo.embedded;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import com.datastax.driver.core.Session;
import info.archinnov.achilles.embedded.CassandraEmbeddedServerBuilder;
import info.archinnov.achilles.persistence.PersistenceManager;

public class EmbeddedPersistenceManagerFactoryBean extends AbstractFactoryBean<PersistenceManager> {
    private static PersistenceManager manager;

    static {
        manager = CassandraEmbeddedServerBuilder
                .noEntityPackages()
                .withKeyspaceName("devoxx")
                .cleanDataFilesAtStartup(false)
                .buildPersistenceManager();

        final Session session = manager.getNativeSession();

        createTables(session);
    }

    private static void createTables(Session session) {
        session.execute("CREATE TABLE IF NOT EXISTS countdown(id text PRIMARY KEY,value text)");
        session.execute("CREATE TABLE IF NOT EXISTS ratelimit(id text,column text,value text, PRIMARY KEY(id,column))");
        session.execute("CREATE TABLE IF NOT EXISTS time_stamp(id text PRIMARY KEY,value text)");
        session.execute("CREATE TABLE IF NOT EXISTS writebarrier(id text PRIMARY KEY,value text)");
        session.executeAsync("TRUNCATE countdown");
        session.executeAsync("TRUNCATE ratelimit");
        session.executeAsync("TRUNCATE time_stamp");
        session.executeAsync("TRUNCATE writebarrier");
    }

    @Override
    public Class<?> getObjectType() {
        return PersistenceManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    protected PersistenceManager createInstance() throws Exception {
        return manager;
    }
}
