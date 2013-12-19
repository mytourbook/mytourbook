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
/**
 * @author Alfred Barten
 */
package net.tourbook.srtm;

import java.util.ArrayList;

import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.color.SRTMProfileImage;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.graphics.Image;

public class SRTMProfile implements Cloneable {

	static final String		DEFAULT_SRTM_RESOLUTION	= IPreferences.SRTM_RESOLUTION_VERY_FINE;
	static final boolean	DEFAULT_IS_SHADOW		= false;
	static final float		DEFAULT_SHADOW_VALUE	= 0.8f;

	/*
	 * saved profile fields in xml file
	 */
	private int				_profileId;
	private String			_profileName;
	private String			_tilePath;
	private boolean			_shadow					= DEFAULT_IS_SHADOW;
	private String			_resolution				= DEFAULT_SRTM_RESOLUTION;
	private float			_shadowValue			= DEFAULT_SHADOW_VALUE;

	private ProfileImage	_profileImage			= new SRTMProfileImage();

	/*
	 * not saved fields
	 */
	private int				_savedProfileKeyHashCode;

	public SRTMProfile() {}

	@Override
	public SRTMProfile clone() {

		SRTMProfile clonedObject = null;

		try {

			clonedObject = (SRTMProfile) super.clone();

			clonedObject._profileName = new String(_profileName);
			clonedObject._resolution = new String(_resolution);
			clonedObject._tilePath = new String(_tilePath);

			clonedObject._profileImage = _profileImage.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	/**
	 * Create a copy from another profile.
	 * 
	 * @param sourceProfile
	 */
	public void copyFromOtherProfile(final SRTMProfile sourceProfile) {

		_profileId = sourceProfile.getProfileId();
		_shadow = sourceProfile.isShadowState();
		_shadowValue = sourceProfile.getShadowValue();

		_profileName = new String(sourceProfile.getProfileName());
		_resolution = new String(sourceProfile.getResolution());
		_tilePath = new String(sourceProfile.getTilePath());

		_profileImage = sourceProfile._profileImage.clone();
	}

	/**
	 * Creates SRTM profile image, this image must be disposed who created it.
	 * 
	 * @param width
	 * @param height
	 * @param isHorizontal
	 * @return
	 */
	public Image createImage(final int width, final int height, final boolean isHorizontal) {

		return _profileImage.createImage(width, height, isHorizontal);
	}

	/**
	 * creates the profile key for all profile properties which are saved in the xml file
	 */
	public void createSavedProfileKey() {
		_savedProfileKeyHashCode = getProfileKeyHashCode();
	}

	public void disposeImage() {
		_profileImage.disposeImage();
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof SRTMProfile)) {
			return false;
		}

		final SRTMProfile other = (SRTMProfile) obj;
		if (_profileId != other._profileId) {
			return false;
		}

		return true;
	}

	public int getProfileId() {
		return _profileId;
	}

	public String getProfileKey() {

		return _profileImage.getVertexKey()
				+ isShadowState()
				+ getResolutionValue()
				+ _tilePath
				+ Float.toString(_shadowValue);
	}

	public int getProfileKeyHashCode() {
		return getProfileKey().hashCode();
	}

	public String getProfileName() {
		return _profileName;
	}

	public String getResolution() {
		return _resolution;
	}

	/**
	 * elevation is used at every grid-th pixel in both directions; the other values are
	 * interpolated i.e. it gives the resolution of the image!
	 */
	public int getResolutionValue() {
		if (_resolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH)) {
			return 64;
		} else if (_resolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH)) {
			return 16;
		} else if (_resolution.equals(IPreferences.SRTM_RESOLUTION_FINE)) {
			return 4;
		} else if (_resolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE)) {
			return 1;
		} else {
			return 4;
		}
	}

	public ProfileImage getRgbVertexImage() {
		return _profileImage;
	}

	public int getSavedProfileKeyHashCode() {
		return _savedProfileKeyHashCode;
	}

	public int getShadowRGB(final int elev) {

		final float dimFactor = _shadowValue;
		final int rgb = _profileImage.getRGB(elev);

		byte blue = (byte) ((rgb & 0xFF0000) >> 16);
		byte green = (byte) ((rgb & 0xFF00) >> 8);
		byte red = (byte) ((rgb & 0xFF) >> 0);

		red *= dimFactor;
		green *= dimFactor;
		blue *= dimFactor;

		return (//
				(blue & 0xFF) << 16)
				+ ((green & 0xFF) << 8)
				+ (red & 0xFF);
	}

	public float getShadowValue() {
		return _shadowValue;
	}

	public String getTilePath() {
		return _tilePath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + _profileId;
		return result;
	}

	public boolean isShadowState() {
		return _shadow;
	}

	/**
	 * set default vertex list
	 */
	public void setDefaultVertexes() {

		final ArrayList<RGBVertex> verticies = _profileImage.getRgbVertices();

		if (verticies.size() > 0) {
			return;
		}

		final ArrayList<RGBVertex> defaultVerticies = new ArrayList<RGBVertex>();

		defaultVerticies.add(0, new RGBVertex(0, 0, 0, 255));
		defaultVerticies.add(1, new RGBVertex(1000, 0, 255, 0));
		defaultVerticies.add(2, new RGBVertex(2000, 255, 0, 0));

		_profileImage.setVertices(defaultVerticies);
	}

	/**
	 * Set unique id for each profile
	 * 
	 * @param profileId
	 */
	public void setProfileId(final int profileId) {
		_profileId = profileId;
	}

	public void setProfileName(final String profileName) {
		_profileName = profileName;
	}

	public void setResolution(final String resolution) {
		_resolution = resolution;
	}

	public void setShadowState(final Boolean isShadow) {
		_shadow = isShadow;
	}

	public void setShadowValue(final float value) {
		_shadowValue = value;
	}

	public void setTilePath(final String tilePath) {
		_tilePath = tilePath;
	}

	public void setVertices(final ArrayList<RGBVertex> vertices) {
		_profileImage.setVertices(vertices);
	}

}
