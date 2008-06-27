package net.tourbook.ui;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

public class TourTypeSQL {

	String			sqlSelect;
	ArrayList<Long>	tourTypes;

	public TourTypeSQL(final String sqlString) {
		sqlSelect = sqlString;
	}

	public TourTypeSQL(final String sqlString, final ArrayList<Long> sqlTourTypes) {
		sqlSelect = sqlString;
		tourTypes = sqlTourTypes;
	}

	/**
	 * @return Returns the where clause to filter the tour types
	 */
	public String getWhereClause() {
		if (sqlSelect == null) {
			return UI.EMPTY_STRING;
		} else {
			return sqlSelect;
		}
	}

	/**
	 * Sets the tour types as parameters into the satement
	 * 
	 * @param statement
	 * @param startIndex
	 * @throws SQLException
	 */
	public void setSQLParameters(final PreparedStatement statement, final int startIndex) throws SQLException {

		int parameterIndex = startIndex;
		for (final Long tourType : tourTypes) {

			if (tourType == null) {
//				statement.setNull(parameterIndex, java.sql.Types.BIGINT);
			} else {
				statement.setLong(parameterIndex, tourType.longValue());
			}

			parameterIndex++;
		}
	}

}
