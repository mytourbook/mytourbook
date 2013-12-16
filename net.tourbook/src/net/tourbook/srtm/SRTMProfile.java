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

import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.color.RGBVertexImage;

import org.eclipse.swt.graphics.Image;

public class SRTMProfile {

	static final String		DEFAULT_SRTM_RESOLUTION	= IPreferences.SRTM_RESOLUTION_VERY_FINE;
	static final boolean	DEFAULT_IS_SHADOW		= false;
	static final float		DEFAULT_SHADOW_VALUE	= 0.8f;

	/*
	 * saved profile fields in xml file
	 */
	private int				_profileId;
	private String			_profileName;
	private String			_tilePath;
	private boolean			_sShadow				= DEFAULT_IS_SHADOW;
	private String			_rResolution			= DEFAULT_SRTM_RESOLUTION;
	private float			_shadowValue			= DEFAULT_SHADOW_VALUE;

	/*
	 * not saved fields
	 */
	private int				_savedProfileKeyHashCode;

	private RGBVertexImage	_rgbVertexImage			= new RGBVertexImage();

	public SRTMProfile() {}

	/**
	 * Creates a clone from another profile
	 * 
	 * @param otherProfile
	 *            profile which gets cloned
	 */
	public SRTMProfile(final SRTMProfile otherProfile) {
		cloneProfile(otherProfile);
	}

	/**
	 * clone a profile from another profile
	 * 
	 * @param newProfile
	 */
	public void cloneProfile(final SRTMProfile newProfile) {

		// copy vertex list and it's content

		_rgbVertexImage.cloneVertices(newProfile.getRgbVertexImage());

		_profileId = newProfile.getProfileId();
		_profileName = new String(newProfile.getProfileName());
		_tilePath = new String(newProfile.getTilePath());
		_sShadow = newProfile.isShadowState();
		_shadowValue = newProfile.getShadowValue();
		_rResolution = new String(newProfile.getResolution());
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

		return _rgbVertexImage.createVertexImage(width, height, isHorizontal);
	}

	/**
	 * creates the profile key for all profile properties which are saved in the xml file
	 */
	public void createSavedProfileKey() {
		_savedProfileKeyHashCode = getProfileKeyHashCode();
	}

	public void disposeImage() {
		_rgbVertexImage.disposeImage();
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
		return _rgbVertexImage.getVertexKey()
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
		return _rResolution;
	}

	/**
	 * elevation is used at every grid-th pixel in both directions; the other values are
	 * interpolated i.e. it gives the resolution of the image!
	 */
	public int getResolutionValue() {
		if (_rResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH)) {
			return 64;
		} else if (_rResolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH)) {
			return 16;
		} else if (_rResolution.equals(IPreferences.SRTM_RESOLUTION_FINE)) {
			return 4;
		} else if (_rResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE)) {
			return 1;
		} else {
			return 4;
		}
	}

	public RGBVertexImage getRgbVertexImage() {
		return _rgbVertexImage;
	}

	public int getSavedProfileKeyHashCode() {
		return _savedProfileKeyHashCode;
	}

	public int getShadowRGB(final int elev) {

		final float dimFactor = _shadowValue;
		final int rgb = _rgbVertexImage.getRGB(elev);

		byte blue = (byte) ((rgb & 0xFF0000) >> 16);
		byte green = (byte) ((rgb & 0xFF00) >> 8);
		byte red = (byte) ((rgb & 0xFF) >> 0);

		red *= dimFactor;
		green *= dimFactor;
		blue *= dimFactor;

//		return rgb;
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

//	public void setHorizontal() {
//		fIsHorizontal = true;
//	}
//
//	public void set(String s) {
//		final Pattern pattern = Pattern.compile("^([-]*[0-9]*),([0-9]*),([0-9]*),([0-9]*);(.*)$"); //$NON-NLS-1$
//		_vertexList.clear();
//		int ix = 0;
//		while (s.length() > 0) {
//			final Matcher matcher = pattern.matcher(s);
//			if (matcher.matches()) {
//				final Long elev = new Long(matcher.group(1));
//				final Integer red = new Integer(matcher.group(2));
//				final Integer green = new Integer(matcher.group(3));
//				final Integer blue = new Integer(matcher.group(4));
//				final RGBVertex rgbVertex = new RGBVertex();
//				rgbVertex.setElevation(elev.longValue());
//				rgbVertex.setRGB(new RGB(red.intValue(), green.intValue(), blue.intValue()));
//				_vertexList.add(ix, rgbVertex);
//				ix++;
//				s = matcher.group(5); // rest
//			}
//		}
//		sort();
//
//		_vertexArray = _vertexList.toArray(new RGBVertex[_vertexList.size()]);
//	}

	public boolean isShadowState() {
		return _sShadow;
	}

	/**
	 * set default vertex list
	 */
	public void setDefaultVertexes() {

		final ArrayList<RGBVertex> verticies = _rgbVertexImage.getRgbVerticies();

		if (verticies.size() > 0) {
			return;
		}

		final ArrayList<RGBVertex> defaultVerticies = new ArrayList<RGBVertex>();

		defaultVerticies.add(0, new RGBVertex(0, 0, 255, 0));
		defaultVerticies.add(1, new RGBVertex(0, 255, 0, 1000));
		defaultVerticies.add(2, new RGBVertex(255, 0, 0, 2000));

		_rgbVertexImage.setVerticies(defaultVerticies);
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
		_rResolution = resolution;
	}

	public void setShadowState(final Boolean isShadow) {
		_sShadow = isShadow;
	}

	public void setShadowValue(final float value) {
		_shadowValue = value;
	}

	public void setTilePath(final String tilePath) {
		_tilePath = tilePath;
	}

	public void setVertexList(final ArrayList<RGBVertex> vertexList) {
		_rgbVertexImage.setVerticies(vertexList);
	}

//	/**
//	 * paint the profile image vertical, default is horizontal
//	 */
//	public void setVertical() {
//		fIsHorizontal = false;
//	}
//	public void sort() {
//		Collections.sort(_vertexList);
//		_vertexArray = _vertexList.toArray(new RGBVertex[_vertexList.size()]);
//	}

}
