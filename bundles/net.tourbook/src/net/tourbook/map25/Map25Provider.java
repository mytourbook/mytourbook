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
package net.tourbook.map25;

import de.byteholder.geoclipse.map.UI;

public class Map25Provider implements Cloneable {

	private static int	_idCounter;

	private int			id;

	public String		name			= UI.EMPTY_STRING;
	public String		description		= UI.EMPTY_STRING;
	public String		offlineFolder	= UI.EMPTY_STRING;

	public String		url				= UI.EMPTY_STRING;
	public String		tilePath		= UI.EMPTY_STRING;

	public String		apiKey			= UI.EMPTY_STRING;

	public Map25Provider() {

		id = ++_idCounter;
	}

	@Override
	public Object clone() {

		try {

			final Map25Provider clonedProvider = (Map25Provider) super.clone();

			return clonedProvider;

		} catch (final CloneNotSupportedException e) {

			// this shouldn't happen, since we are Cloneable
			throw new InternalError(e);
		}
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}

		final Map25Provider other = (Map25Provider) obj;
		if (id != other.id) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + id;

		return result;
	}

	@Override
	public String toString() {
		return "Map25Provider ["
//				+ "id=" + id + ", "
				+ "name=" + name + ", "
//				+ "description=" + description + ", "
//				+ "offlineFolder=" + offlineFolder + ", "
//				+ "url=" + url + ", "
//				+ "tilePath=" + tilePath + ", "
//				+ "apiKey=" + apiKey
				+ "]";
	}

}
