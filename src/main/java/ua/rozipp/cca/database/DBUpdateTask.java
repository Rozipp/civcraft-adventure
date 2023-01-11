package ua.rozipp.cca.database;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DBUpdateTask implements Runnable {

    private static final ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<>();
    private static SessionFactory sessionFactory;
    private static boolean running = true;

    public DBUpdateTask(SessionFactory sessionFactory) {
        DBUpdateTask.sessionFactory = sessionFactory;
    }

    public static void stop(){
        running = false;
    }

    public static void add(SQLObject obj) {
        if (!saveObjects.contains(obj)) saveObjects.add(obj);
    }

    public static void saveNow() {
        synchronized (saveObjects) {
            if (!saveObjects.isEmpty()){
                try (Session session = sessionFactory.openSession()) {
                    while (!saveObjects.isEmpty()) {
                        SQLObject obj = saveObjects.poll();
                        if (obj != null) session.save(obj);
                    }
                    session.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            while (running){
                try {
                    if (saveObjects.isEmpty()) {
                        Thread.sleep(500);
                    } else {
                        SQLObject obj = saveObjects.poll();
                        if (obj == null) continue;
                        session.save(obj);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            saveNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
