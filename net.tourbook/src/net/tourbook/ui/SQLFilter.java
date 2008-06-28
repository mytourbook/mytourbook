package net.tourbook.ui;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.data.TourPerson;
import net.tourbook.plugin.TourbookPlugin;

/**
 * The {@link SQLFilter} provides a where clause and sets the parameters for the selected person and
 * tour type
 */
public class SQLFilter {

	private String			fWhereClause	= UI.EMPTY_STRING;

	private ArrayList<Long>	fParameters		= new ArrayList<Long>();

	public SQLFilter() {

		final TourbookPlugin plugin = TourbookPlugin.getDefault();
		final StringBuilder sb = new StringBuilder();

		/*
		 * get person filter
		 */
		final TourPerson activePerson = plugin.getActivePerson();
		if (activePerson == null) {

			// select all people

		} else {

			// select only one person

			sb.append(" AND TourData.tourPerson_personId = ?");
			fParameters.add(activePerson.getPersonId());
		}

		/*
		 * get tour type filter
		 */
		final TourTypeFilter activeTourTypeFilter = plugin.getActiveTourTypeFilter();
		if (activeTourTypeFilter != null) {

			final SQLData sqlData = activeTourTypeFilter.getSQLData();

			sb.append(sqlData.whereString);
			fParameters.addAll(sqlData.longParameters);
		}

		fWhereClause = sb.toString();
	}

	/**
	 * @return Returns the where clause to filter the tour types
	 */
	public String getWhereClause() {
		return fWhereClause;
	}

	/**
	 * Sets the parameters into the statement
	 * 
	 * @param statement
	 * @param startIndex
	 *            the first parameter is 1
	 * @throws SQLException
	 */
	public void setParameters(final PreparedStatement statement, final int startIndex) throws SQLException {

		int parameterIndex = startIndex;
		for (final Long longParameter : fParameters) {

			if (longParameter != null) {
				statement.setLong(parameterIndex, longParameter.longValue());
				parameterIndex++;
			}
		}
	}
}
