/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.ui;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;

/**
 * The {@link SQLFilter} provides a where clause and sets the statement parameters for the selected
 * person and tour type
 */
public class SQLFilter {

	private String			_sqlWhereClause	= UI.EMPTY_STRING;

	private ArrayList<Long>	_parameters		= new ArrayList<Long>();

	public SQLFilter() {

		final StringBuilder sb = new StringBuilder();

		/*
		 * get person filter
		 */
		final TourPerson activePerson = TourbookPlugin.getActivePerson();
		if (activePerson == null) {

			// select all people

		} else {

			// select only one person

			sb.append(" AND TourData.tourPerson_personId = ?"); //$NON-NLS-1$
			_parameters.add(activePerson.getPersonId());
		}

		/*
		 * get tour type filter
		 */
		final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();
		if (activeTourTypeFilter != null) {

			final SQLData sqlData = activeTourTypeFilter.getSQLData();

			sb.append(sqlData.whereString);
			_parameters.addAll(sqlData.longParameters);
		}

		_sqlWhereClause = sb.toString();
	}

	/**
	 * @return Returns the where clause to filter the tour types
	 */
	public String getWhereClause() {
		return _sqlWhereClause;
	}

	/**
	 * Sets the parameters into the filter statement
	 * 
	 * @param statement
	 * @param startIndex
	 *            the first parameter is 1
	 * @throws SQLException
	 */
	public void setParameters(final PreparedStatement statement, final int startIndex) throws SQLException {

		int parameterIndex = startIndex;
		for (final Long longParameter : _parameters) {

			if (longParameter != null) {
				statement.setLong(parameterIndex, longParameter.longValue());
				parameterIndex++;
			}
		}
	}

	@Override
	public String toString() {
		return _sqlWhereClause;
	}
}
