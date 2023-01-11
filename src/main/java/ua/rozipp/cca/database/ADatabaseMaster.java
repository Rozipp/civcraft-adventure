package ua.rozipp.cca.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import ua.rozipp.abstractplugin.ASettingMaster;
import ua.rozipp.abstractplugin.exception.InvalidConfiguration;
import ua.rozipp.cca.CCAPlugin;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ADatabaseMaster {

    public static String tb_prefix = "";
    private final DBUpdateTask dbUpdateTask;

    public ADatabaseMaster(ASettingMaster setting, Logger logger, List<Class> classList) throws InvalidConfiguration {
        String pref = "mysql.";
        logger.fine("--------Initializing DATA SQL Database--------");
        boolean recreate_tables = setting.getBooleanOrDefault(null, "clear_table_data", false);

        String hostname = setting.getString(null, pref + "hostname");
        String port = setting.getString(null, pref + "port");
        String db_name = setting.getString(null, pref + "database");
        String username = setting.getString(null, pref + "username");
        String password = setting.getString(null, pref + "password");
        String useSSL = "false"; /*RHCConfigs.read("config", pref, "useSSL").asText();*/

        String otherProperty = "?useUnicode=true&serverTimezone=UTC"; // Изменять при необходимости
        otherProperty += "&useSSL=" + useSSL;
        String dsn = "jdbc:mysql://" + hostname + ":" + port + "/" + tb_prefix + db_name + otherProperty;
        logger.info("\t dsn: " + dsn);
        logger.info("\t Using " + hostname + ":" + port + " user:" + username + " DB:" + db_name);

        HibernateUtil.createSessionFactory(dsn, username, password, recreate_tables, classList);
        logger.info("----- Done Building Tables ----");
        tb_prefix = setting.getString(null, pref + "table_prefix");

        logger.fine("--------Initializing SQL Finished--------");

        dbUpdateTask = new DBUpdateTask();
        CCAPlugin.getInstance().getTaskMaster().asyncTask("DBUpdateTask", dbUpdateTask, 0);
    }

    public List<Object> getObjectsFromDB(Session session, String tableName) {
        return session.createQuery("FROM " + tableName).list();
    }

    public SessionFactory getSessionFactory() {
        return HibernateUtil.getSessionFactory();
    }

    public void update(SQLObject object) {
        dbUpdateTask.add(object);
    }

    public void save(SQLObject object) {
        dbUpdateTask.add(object);
    }

    public void updateNow(Object obj) {
        Session session = getSessionFactory().openSession();
        session.beginTransaction();
        session.saveOrUpdate(obj);
        session.flush();
        session.getTransaction().commit();
    }

    public void delete(Object obj) {
        Session session = getSessionFactory().openSession();
        session.beginTransaction();
        session.delete(obj);
        session.flush();
        session.close();
    }

    public void stopDatabaseMaster() {
        dbUpdateTask.stop();
        HibernateUtil.closeSessionFactory();
    }

    public <T extends SQLObject> T getSQLObjectFromDB(Class<T> clazz, int id) {
        Session session = getSessionFactory().openSession();
        session.beginTransaction();
        T obj = session.get(clazz, id);
        session.close();
        return obj;
    }

    public class DBUpdateTask implements Runnable {

        private final ReentrantLock lock = new ReentrantLock();
        private final ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<>();

        public void stop() {
            lock.lock();
        }

        public void add(SQLObject obj) {
            if (!saveObjects.contains(obj)) saveObjects.add(obj);
        }

        @Override
        public void run() {
            try (Session session = getSessionFactory().openSession()) {
                session.beginTransaction();
                while (lock.tryLock()) {
                    try {
                        if (saveObjects.isEmpty()) {
                            lock.unlock();
                            Thread.sleep(500);
                            continue;
                        }
                        SQLObject obj = saveObjects.poll();
                        if (obj != null) session.save(obj);
                        lock.unlock();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                synchronized (saveObjects) {
                    while (!saveObjects.isEmpty()) {
                        SQLObject obj = saveObjects.poll();
                        if (obj != null) session.save(obj);
                    }
                }

                session.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}


