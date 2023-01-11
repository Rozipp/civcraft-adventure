package ua.rozipp.cca.database;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import java.util.List;

public class HibernateUtil {

    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static void createSessionFactory(String dbcUrl, String user, String pass, boolean recreate_table, List<Class> civObjectList) {
        try {
            //стандартные настройки для хибернат
            //для тех, кто использует другую базу данных нужно заметить поле DRIVER, DIALECT и кусок URL легко гуглятся под любую базу
            StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();

            registryBuilder.applySetting(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
            registryBuilder.applySetting(Environment.URL, dbcUrl);
            registryBuilder.applySetting(Environment.USER, user);
            registryBuilder.applySetting(Environment.PASS, pass);
            registryBuilder.applySetting(Environment.DIALECT, "MySQL5InnoDB");
            registryBuilder.applySetting(Environment.HBM2DDL_AUTO, (recreate_table) ? "create" : "update");
            registryBuilder.applySetting(Environment.SHOW_SQL, true);

            registry = registryBuilder.build();

            MetadataSources sources = new MetadataSources(registry);
            civObjectList.forEach(cl -> sources.addAnnotatedClass(cl));

            Metadata metadata = sources.getMetadataBuilder().build();

            sessionFactory = metadata.getSessionFactoryBuilder().build();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (registry != null) StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void closeSessionFactory() {
        getSessionFactory().close();
    }

}