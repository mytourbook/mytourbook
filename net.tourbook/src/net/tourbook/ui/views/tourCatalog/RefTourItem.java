/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

public class RefTourItem {

	public long		refId;
	public long		tourId;

	public String	label;
	public int		startIndex;
	public int		endIndex;

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof RefTourItem)) {
			return false;
		}
		final RefTourItem other = (RefTourItem) obj;
		if (refId != other.refId) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;

		result = prime * result + (int) (refId ^ (refId >>> 32));

		return result;
	}

	@Override
	public String toString() {
		return "RefTourItem ["
				+ ("refId=" + refId + ", ")
				+ ("tourId=" + tourId + ", ")
				+ ("label=" + label + ", ")
				+ ("startIndex=" + startIndex + ", ")
				+ ("endIndex=" + endIndex)
				+ "]";
	}
}
