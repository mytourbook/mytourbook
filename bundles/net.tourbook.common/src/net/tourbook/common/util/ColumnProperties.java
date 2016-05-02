/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import net.tourbook.common.formatter.ValueFormat;

/**
 * Contains customized table/tree column properties, a column is identified by the {@link #columnId}
 * .
 * 
 * @since 16.5
 */
public class ColumnProperties {

	String		columnId;

	ValueFormat	valueFormat_Category;
	ValueFormat	valueFormat_Detail;

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ColumnProperties)) {
			return false;
		}
		final ColumnProperties other = (ColumnProperties) obj;
		if (columnId == null) {
			if (other.columnId != null) {
				return false;
			}
		} else if (!columnId.equals(other.columnId)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((columnId == null) ? 0 : columnId.hashCode());

		return result;
	}

	@Override
	public String toString() {
		return "ColumnProperties [" //
				+ ("columnId=" + columnId + ", ")
				+ ("valueFormat_Category=" + valueFormat_Category + ", ")
				+ ("valueFormat_Detail=" + valueFormat_Detail + ", ")
				+ "]";
	}
}
