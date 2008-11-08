/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.ui.views.tourDataEditor;

class TimeSlice {

	int	serieIndex;

	/*
	 * uniqueCreateIndex is required because changing the serieIndex when items are remove from the
	 * viewer fails because table item widgets are disposed and the viewer trys to select them
	 */
	int	uniqueCreateIndex;

	public TimeSlice(final int serieIndex) {
		uniqueCreateIndex = this.serieIndex = serieIndex;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TimeSlice)) {
			return false;
		}
		final TimeSlice other = (TimeSlice) obj;
		if (serieIndex != other.serieIndex) {
			return false;
		}
		if (uniqueCreateIndex != other.uniqueCreateIndex) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + serieIndex;
		result = prime * result + uniqueCreateIndex;
		return result;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("createIndex:\t");//$NON-NLS-1$
		sb.append(uniqueCreateIndex);
		sb.append("\t\tdataIndex:\t");//$NON-NLS-1$
		sb.append(serieIndex);

		return sb.toString();
	}
}
