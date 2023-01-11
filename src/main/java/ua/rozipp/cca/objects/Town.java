package ua.rozipp.cca.objects;

import lombok.Getter;
import lombok.Setter;
import ua.rozipp.abstractplugin.exception.InvalidNameException;
import ua.rozipp.cca.database.SQLObject;

import javax.persistence.*;
import java.sql.ResultSet;
import java.sql.SQLException;

@Entity
@Table(name = "town")
public class Town implements SQLObject {

	/**
	 * Primary key.
	 */
	@Id
	@GeneratedValue
	@Column(nullable = false)
	@Getter
	@Setter
	private int id;

	/**
	 * Greeting.
	 */
	@Getter
	@Setter
	private String name;

	/**
	 * Greeting target.
	 */
	@Getter
	@Setter
	private String target;

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {

	}

	@Override
	public void saveNow() throws SQLException {

	}

	@Override
	public void delete() throws SQLException {

	}
}
