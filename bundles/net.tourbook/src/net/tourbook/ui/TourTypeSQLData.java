/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

public class TourTypeSQLData {

	private String			_whereString;
	private ArrayList<Long>	_parameters;

	public TourTypeSQLData(final String whereString, final ArrayList<Long> parameters) {

		_whereString = whereString;
		_parameters = parameters;
	}

	public ArrayList<Long> getParameters() {
		return _parameters;
	}

	public String getWhereString() {
		return _whereString;
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
}
