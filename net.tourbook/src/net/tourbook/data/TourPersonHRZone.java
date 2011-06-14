/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;

import org.eclipse.swt.graphics.RGB;

@Entity
public class TourPersonHRZone implements Cloneable, Comparable<TourPersonHRZone> {

	private static final String	NAME_SHORTCUT_PREFIX	= " (";							//$NON-NLS-1$
	private static final String	NAME_SHORTCUT_POSTFIX	= ")";								//$NON-NLS-1$

	public static final int		DB_LENGTH_ZONE_NAME		= 255;
	public static final int		DB_LENGTH_DESCRIPTION	= 2000;

	/**
	 * Unique id for the {@link TourPersonHRZone} entity
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long				hrZoneId				= TourDatabase.ENTITY_IS_NOT_SAVED;

	@ManyToOne(optional = false)
	private TourPerson			tourPerson;

	/**
	 * Name for the zone
	 */
	private String				zoneName;

	/**
	 * Shortcut name for the zone
	 */
	private String				nameShortcut;

	/**
	 * Description
	 */
	private String				description;

	/**
	 * Minimum value of the hr zone in % of hr max
	 */
	private int					zoneMinValue;

	/**
	 * Maximum value of the hr zone in % of hr max
	 */
	private int					zoneMaxValue;

	/**
	 * HR zone color
	 */
	private int					colorRed;
	private int					colorGreen;
	private int					colorBlue;

	/**
	 * unique id for manually created markers because the {@link #hrZoneId} is 0 when the marker is
	 * not persisted
	 */
	@Transient
	private long				_createId				= 0;

	/**
	 * cached color
	 */
	@Transient
	private RGB					_color;
	@Transient
	private RGB					_colorDark;

	/**
	 * manually created marker or imported marker create a unique id to identify them, saved marker
	 * are compared with the marker id
	 */
	private static int			_createCounter			= 0;

	public TourPersonHRZone() {}

	public TourPersonHRZone(final TourPerson tourPerson) {

		this.tourPerson = tourPerson;

		_createId = ++_createCounter;
	}

	private void checkColors() {
		if (_color == null) {
			_color = new RGB(colorRed, colorGreen, colorBlue);
			_colorDark = getDarkColor(_color);
		}
	}

//	/**
//	 * @return Returns a deep clone of {@link TourPersonHRZone}
//	 */
//	public TourPersonHRZone deepClone() {
//
//		try {
//
//			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			final ObjectOutputStream oos = new ObjectOutputStream(baos);
//			oos.writeObject(this);
//
//			final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
//			final ObjectInputStream ois = new ObjectInputStream(bais);
//
//			final TourPersonHRZone newObject = (TourPersonHRZone) ois.readObject();
//
//			return newObject;
//
//		} catch (final Exception e) {
//			StatusUtil.log(e);
//			return null;
//		}
//	}

	@Override
	public TourPersonHRZone clone() {

		try {

			// create clones for shallow copied fields so that they can be modified

			final TourPersonHRZone newHrZone = (TourPersonHRZone) super.clone();

			newHrZone.zoneName = zoneName == null ? null : new String(zoneName);
			newHrZone.nameShortcut = nameShortcut == null ? null : new String(nameShortcut);
			newHrZone.description = description == null ? null : new String(description);

			return newHrZone;

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
			return null;
		}
	}

	@Override
	public int compareTo(final TourPersonHRZone otherHrZone) {

		return zoneMinValue < otherHrZone.zoneMinValue ? //
				-1
				: zoneMinValue == otherHrZone.zoneMinValue ? //
						0
						: 1;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TourPersonHRZone)) {
			return false;
		}

		final TourPersonHRZone other = (TourPersonHRZone) obj;

		if (_createId == 0) {

			// hr zone is from the database
			if (hrZoneId != other.hrZoneId) {
				return false;
			}
		} else {

			// hr zone was create
			if (_createId != other._createId) {
				return false;
			}
		}

		return true;
	}

	public RGB getColor() {
		checkColors();
		return _color;
	}

	public RGB getDarkColor() {
		checkColors();
		return _colorDark;
	}

	private RGB getDarkColor(final RGB rgb) {

		final double darkFactor = 0.9;

		final int red = (int) Math.max(0, rgb.red * darkFactor);
		final int green = (int) Math.max(0, rgb.green * darkFactor);
		final int blue = (int) Math.max(0, rgb.blue * darkFactor);

		return new RGB(red, green, blue);
	}

	public String getDescription() {
		return description;
	}

	/**
	 * @return Returns the name of the zone combined with the zone name shortcut
	 */
	public String getNameLong() {

		if ((zoneName == null || zoneName.length() == 0) && (nameShortcut == null || nameShortcut.length() == 0)) {
			// nothing is defined
			return UI.EMPTY_STRING;
		}

		if (zoneName == null || zoneName.length() == 0) {
			return nameShortcut;
		}

		if (nameShortcut == null || nameShortcut.length() == 0) {
			return zoneName;
		}

		return zoneName + NAME_SHORTCUT_PREFIX + nameShortcut + NAME_SHORTCUT_POSTFIX;
	}

	/**
	 * @return Returns the name of the zone combined with the zone name shortcut
	 */
	public String getNameLongShortcutFirst() {

		if ((zoneName == null || zoneName.length() == 0) && (nameShortcut == null || nameShortcut.length() == 0)) {
			// nothing is defined
			return UI.EMPTY_STRING;
		}

		if (zoneName == null || zoneName.length() == 0) {
			return nameShortcut;
		}

		if (nameShortcut == null || nameShortcut.length() == 0) {
			return zoneName;
		}

		return nameShortcut + UI.SYMBOL_COLON + UI.SPACE + zoneName;
	}

	/**
	 * @return Returns the short name or the name when the short name is not available.
	 */
	public String getNameShort() {

		if (nameShortcut != null && nameShortcut.length() > 0) {
			return nameShortcut;
		}

		if (zoneName != null && zoneName.length() > 0) {
			return zoneName;
		}

		return UI.EMPTY_STRING;
	}

	public String getNameShortcut() {
		return nameShortcut;
	}

	public TourPerson getTourPerson() {
		return tourPerson;
	}

	/**
	 * @return Returns maximum value of the hr zone in % of hr max
	 */
	public int getZoneMaxValue() {
		return zoneMaxValue;
	}

	/**
	 * @return Returns minimum value of the hr zone in % of hr max
	 */
	public int getZoneMinValue() {
		return zoneMinValue;
	}

	public String getZoneName() {
		return zoneName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (_createId ^ (_createId >>> 32));
		result = prime * result + (int) (hrZoneId ^ (hrZoneId >>> 32));
		return result;
	}

	public void setColor(final RGB rgb) {

		// cache rgb values
		_color = rgb;
		_colorDark = getDarkColor(rgb);

		colorRed = rgb.red;
		colorGreen = rgb.green;
		colorBlue = rgb.blue;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public void setNameShortcut(final String nameShortcut) {
		this.nameShortcut = nameShortcut;
	}

	public void setTourPerson(final TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

	public void setZoneMaxValue(final int zoneMaxValue) {
		this.zoneMaxValue = zoneMaxValue;
	}

	public void setZoneMinValue(final int zoneMinValue) {
		this.zoneMinValue = zoneMinValue;
	}

	public void setZoneName(final String zoneName) {
		this.zoneName = zoneName;
	}

	@Override
	public String toString() {
		return "TourPersonHRZone [zoneMinValue=" //$NON-NLS-1$
				+ zoneMinValue
				+ ", zoneMaxValue=" //$NON-NLS-1$
				+ zoneMaxValue
				+ ", zoneName=" //$NON-NLS-1$
				+ zoneName
				+ "]"; //$NON-NLS-1$
	}

}
