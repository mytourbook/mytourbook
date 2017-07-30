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

import java.util.UUID;

import de.byteholder.geoclipse.map.UI;

public class Map25Provider implements Cloneable {

	private String		_id;
	private UUID		_uuid;

	public boolean		isEnabled;
	public boolean		isDefault;

	public String		name			= UI.EMPTY_STRING;
	public String		description		= UI.EMPTY_STRING;

	public String		url				= UI.EMPTY_STRING;
	public String		tilePath		= UI.EMPTY_STRING;

	public String		apiKey			= UI.EMPTY_STRING;

	public TileEncoding	tileEncoding	= TileEncoding.MVT;

	public Map25Provider() {

		_uuid = UUID.randomUUID();
		_id = _uuid.toString();
	}

	/**
	 * @param notCheckedUUID
	 *            Contains a UUID string but it can be invalid.
	 */
	public Map25Provider(final String notCheckedUUID) {

		UUID uuid;
		try {
			uuid = UUID.fromString(notCheckedUUID);
		} catch (final Exception e) {
			uuid = UUID.randomUUID();
		}

		_uuid = uuid;
		_id = _uuid.toString();
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
		if (_uuid == null) {
			if (other._uuid != null) {
				return false;
			}
		} else if (!_uuid.equals(other._uuid)) {
			return false;
		}

		return true;
	}

	/**
	 * @return Returns the map provider {@link UUID} as string.
	 */
	public String getId() {
		return _id;
	}

	public UUID getUuid() {
		return _uuid;
	}

	@Override
	public int hashCode() {

		return _uuid.hashCode();
	}

	@Override
	public String toString() {
		return "Map25Provider [" //$NON-NLS-1$
//				+ "id=" + id + ", "
				+ "name=" + name + ", " //$NON-NLS-1$ //$NON-NLS-2$
				//				+ "description=" + description + ", "
				//				+ "offlineFolder=" + offlineFolder + ", "
				//				+ "url=" + url + ", "
				//				+ "tilePath=" + tilePath + ", "
				//				+ "apiKey=" + apiKey
				+ "]"; //$NON-NLS-1$
	}

}
