package ua.rozipp.cca.objects;

import org.hibernate.Session;
import ua.rozipp.cca.database.ADatabaseMaster;

public class DBObjects {

    public void loadAllObjects(ADatabaseMaster dbMaster) {
        try (Session session = dbMaster.getSessionFactory().openSession()) {
            Towns.addAll(dbMaster.getObjectsFromDB(session, "Town"));

        }
    }

}
