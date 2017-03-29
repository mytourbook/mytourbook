/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter;

import java.util.ArrayList;

public class TourFilterSQLData { 

	private String				_whereString;
	private ArrayList<Object>	_parameters;

	public TourFilterSQLData(final String whereString, final ArrayList<Object> parameters) {

		_whereString = whereString;
		_parameters = parameters;
	}

	public ArrayList<Object> getParameters() {
		return _parameters;
	}

	public String getWhereString() {
		return _whereString;
	}

	@Override
	public String toString() {

		return "TourFilterSQLData [\n" // //$NON-NLS-1$

				+ ("_whereString=" + _whereString + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("_parameters=" + _parameters + "\n") //$NON-NLS-1$ //$NON-NLS-2$

				+ "]"; //$NON-NLS-1$
	}
}
