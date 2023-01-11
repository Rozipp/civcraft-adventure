package ua.rozipp.cca.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import ua.rozipp.abstractplugin.exception.InvalidNameException;

/*
 * Any object that needs to be saved will extend this object so it can be
 * saved in the database.
 */
public interface SQLObject {

	void load(ResultSet rs) throws SQLException, InvalidNameException;

	void saveNow() throws SQLException;

	void delete() throws SQLException;

	int getId();

}
