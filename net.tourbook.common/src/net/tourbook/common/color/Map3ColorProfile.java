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
package net.tourbook.common.color;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

/**
 * Contains all colors for one graph to paint a tour in the 3D map.
 */
public class Map3ColorProfile extends MapColorProfile implements Cloneable {

	private static final String	PROFILE_NAME_DEFAULT	= Messages.Map3_Color_ProfileName_Default;
	public static final String	PROFILE_NAME_NEW		= Messages.Map3_Color_ProfileName_New;

	/**
	 * Unique id to identify a color profile.
	 */
	private int					_profileId;

	/**
	 * Name which is visible in the UI.
	 */
	private String				_profileName			= PROFILE_NAME_DEFAULT;

	private ProfileImage		_profileImage			= new Map3ProfileImage();

	/**
	 * When <code>true</code>, the vertex values in {@link #_profileImage} are absolute, otherwise
	 * they are relative.
	 */
	private boolean				_isAbsoluteValues;

	private boolean				_isActiveColorProfile;
	private boolean				_isOverwriteLegendValues;

	private static int			_idCounter				= 0;

	public Map3ColorProfile() {

		_profileId = createProfileId();
	}

	/**
	 * @param isValueAbsolute
	 * @param rgbVertices
	 * @param minBrightness
	 * @param minBrightnessFactor
	 * @param maxBrightness
	 * @param maxBrightnessFactor
	 */
	public Map3ColorProfile(final boolean isValueAbsolute,
							final RGBVertex[] rgbVertices,
							final int minBrightness,
							final int minBrightnessFactor,
							final int maxBrightness,
							final int maxBrightnessFactor) {

		this();

		_isAbsoluteValues = isValueAbsolute;
		_profileImage.setVertices(rgbVertices);

		this.minBrightness = minBrightness;
		this.minBrightnessFactor = minBrightnessFactor;
		this.maxBrightness = maxBrightness;
		this.maxBrightnessFactor = maxBrightnessFactor;
	}

	@Override
	public Map3ColorProfile clone() {

		Map3ColorProfile clonedObject = null;

		try {

			clonedObject = (Map3ColorProfile) super.clone();

			clonedObject._profileId = createProfileId();
			clonedObject._profileName = new String(_profileName);

			clonedObject._profileImage = _profileImage.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	/**
	 * @return Returns a unique id.
	 */
	private int createProfileId() {

		return ++_idCounter;
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Map3ColorProfile)) {
			return false;
		}
		final Map3ColorProfile other = (Map3ColorProfile) obj;
		if (_profileId != other._profileId) {
			return false;
		}
		return true;
	}

	public int getProfileId() {
		return _profileId;
	}

	public ProfileImage getProfileImage() {
		return _profileImage;
	}

	public String getProfileName() {
		return _profileName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _profileId;
		return result;
	}

	public boolean isAbsoluteValues() {
		return _isAbsoluteValues;
	}

	public boolean isActiveColorProfile() {
		return _isActiveColorProfile;
	}

	public boolean isOverwriteLegendValues() {
		return _isOverwriteLegendValues;
	}

	public void setDuplicatedName() {

		_profileName = _profileName + UI.SPACE + getProfileId();
	}

	public void setIsAbsoluteValues(final boolean isAbsoluteValues) {
		_isAbsoluteValues = isAbsoluteValues;
	}

	public void setIsActiveColorProfile(final boolean isActiveColorProfile) {
		_isActiveColorProfile = isActiveColorProfile;
	}

	public void setIsOverwriteLegendValues(final boolean isOverwriteLegendValues) {
		_isOverwriteLegendValues = isOverwriteLegendValues;
	}

	public void setProfileName(final String name) {
		_profileName = name;
	}

	@Override
	public String toString() {
		return String.format(
				"Map3ColorProfile [_profileId=%s, _profileName=%s, _isActiveColorProfile=%s, _profileImage=%s]", //$NON-NLS-1$
				_profileId,
				_profileName,
				_isActiveColorProfile,
				_profileImage);
	}

}
