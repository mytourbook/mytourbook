/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
 * The filter provides a sql WHERE which contains the selected person and tour type.
 */
public class SQLFilter {

	private String			_sqlWhereClause	= UI.EMPTY_STRING;

	private ArrayList<Long>	_parameters		= new ArrayList<Long>();

	public SQLFilter() {
		this(true);
	}

	/**
	 * @param isUsePhotoFilter
	 *            When <code>false</code>, the photo filter is disabled.
	 */
	public SQLFilter(final boolean isUsePhotoFilter) {

		final StringBuilder sb = new StringBuilder();

		/*
		 * app filter: person
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
		 * app filter: photo
		 */
		if (isUsePhotoFilter && TourbookPlugin.getActivePhotoFilter()) {

			sb.append(" AND TourData.numberOfPhotos > 0"); //$NON-NLS-1$
		}

		/*
		 * app filter: tour type
		 */
		final TourTypeFilter activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();
		if (activeTourTypeFilter != null) {

			final TourTypeSQLData sqlData = activeTourTypeFilter.getSQLData();

			sb.append(sqlData.getWhereString());
			_parameters.addAll(sqlData.getParameters());
		}

		_sqlWhereClause = sb.toString();
	}

	/**
	 * @return Returns the WHERE clause to filter tours by person and tour types
	 */
	public String getWhereClause() {
		return _sqlWhereClause;
	}

	/**
	 * Sets the parameters into the filter statement
	 * 
	 * @param statement
	 * @param startIndex
	 *            Sets the parameter start index, the first parameter is 1
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
