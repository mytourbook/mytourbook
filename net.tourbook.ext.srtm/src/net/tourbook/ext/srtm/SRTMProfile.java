/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ext.srtm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;

public class SRTMProfile {

	private static final int		IMAGE_MIN_WIDTH		= 10;
	private static final int		IMAGE_MIN_HEIGHT	= 10;

	private static final long		serialVersionUID	= 1L;

	/*
	 * saved profile fields in xml file
	 */
	private int						fProfileId;
	private String					fProfileName;
	private String					fTilePath;
	private boolean					fIsShadow			= false;
	private String					fResolution			= IPreferences.SRTM_RESOLUTION_VERY_FINE;

	/*
	 * not saved fields
	 */
	private int						fSavedProfileKeyHashCode;

	private Image					fProfileImage;

	/**
	 * list with all vertexes
	 */
	private ArrayList<RGBVertex>	fVertexList			= new ArrayList<RGBVertex>();

	private boolean					fIsHorizontal		= true;
	private float					fShadowValue;

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

		// ensure old list is empty
		fVertexList.clear();

		// copy vertex list and it's content
		final ArrayList<RGBVertex> newVertexList = newProfile.getVertexList();
		for (int ix = 0; ix < newVertexList.size(); ix++) {
			fVertexList.add(ix, new RGBVertex(newVertexList.get(ix)));
		}

		fProfileId = newProfile.getProfileId();
		fProfileName = new String(newProfile.getProfileName());
		fTilePath = new String(newProfile.getTilePath());
		fIsShadow = newProfile.isShadowState();
		fShadowValue = newProfile.getShadowValue();
		fResolution = new String(newProfile.getResolution());
	}

	/**
	 * Creates or recreates the vertex image
	 * 
	 * @param display
	 * @param width
	 * @param height
	 * @return new image
	 */
	public Image createImage(final Display display, int width, int height) {

		// dispose previous image
		disposeImage();

		// ensure min image size
		width = width < IMAGE_MIN_WIDTH ? IMAGE_MIN_WIDTH : width;
		height = height < IMAGE_MIN_HEIGHT ? IMAGE_MIN_HEIGHT : height;

		fProfileImage = new Image(display, width, height);

		/*
		 * draw colors
		 */
		final GC gc = new GC(fProfileImage);
		final long elevMax = fVertexList.size() == 0 ? 8850 : fVertexList.get(fVertexList.size() - 1).getElevation();

		final int horizontal = fIsHorizontal ? width : height + 1;
		final int vertical = fIsHorizontal ? height : width;

		for (int x = 0; x < horizontal; x++) {

			final long elev = elevMax * x / horizontal;

			final RGB rgb = getRGB(elev);
			final Color color = new Color(display, rgb);
			gc.setForeground(color);

			if (fIsHorizontal) {

				final int x1 = horizontal - x - 1;
				final int x2 = horizontal - x - 1;

				final int y1 = 0;
				final int y2 = vertical;

				gc.drawLine(x1, y1, x2, y2);

			} else {

				final int x1 = 0;
				final int x2 = vertical;

				final int y1 = horizontal - x - 1;
				final int y2 = horizontal - x - 1;

				gc.drawLine(x1, y1, x2, y2);
			}
		}

		/*
		 * draw text
		 */
		final Transform transform = new Transform(display);
		for (int ix = 0; ix < fVertexList.size(); ix++) {

			final long elev = fVertexList.get(ix).getElevation();

			if (elev < 0) {
				continue;
			}

			final RGB rgb = getRGB(elev);
			rgb.red = 255 - rgb.red;
			rgb.green = 255 - rgb.green;
			rgb.blue = 255 - rgb.blue;
			final Color color = new Color(display, rgb);
			gc.setForeground(color);

			int x = elevMax == 0 ? 0 : (int) (elev * horizontal / elevMax);
			x = Math.max(x, 13);

			// Rotate by -90 degrees	

			if (fIsHorizontal) {
				final int dx = horizontal - x - 1;
				final int dy = vertical - 3;
				transform.setElements(0, -1, 1, 0, dx, dy);
			} else {
				final int dx = 3;
				final int dy = horizontal - x - 1;
				transform.setElements(1, 0, 0, 1, dx, dy);
			}

			gc.setTransform(transform);

			gc.drawText(net.tourbook.util.UI.EMPTY_STRING + elev, 0, 0, true);
		}
		transform.dispose();

		return fProfileImage;
	}

	/**
	 * creates the profile key for all profile properties which are saved in the xml file
	 */
	public void createSavedProfileKey() {
		fSavedProfileKeyHashCode = getProfileKeyHashCode();
	}

	public void disposeImage() {
		if (fProfileImage != null && fProfileImage.isDisposed() == false) {
			fProfileImage.dispose();
		}
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
		if (fProfileId != other.fProfileId) {
			return false;
		}
		return true;
	}

	public Image getImage() {
		return fProfileImage;
	}

	public int getProfileId() {
		return fProfileId;
	}

	public String getProfileKey() {
		return getVertexKey() + isShadowState() + getResolutionValue() + fTilePath + Float.toString(fShadowValue);
	}

	public int getProfileKeyHashCode() {
		return getProfileKey().hashCode();
	}

	public String getProfileName() {
		return fProfileName;
	}

	public String getResolution() {
		return fResolution;
	}

	/**
	 * elevation is used at every grid-th pixel in both directions; the other values are
	 * interpolated i.e. it gives the resolution of the image!
	 */
	public int getResolutionValue() {
		if (fResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH)) {
			return 64;
		} else if (fResolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH)) {
			return 16;
		} else if (fResolution.equals(IPreferences.SRTM_RESOLUTION_FINE)) {
			return 4;
		} else if (fResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE)) {
			return 1;
		} else {
			return 4;
		}
	}

	public RGB getRGB(final long elev) {

		if (fVertexList.size() == 0) {
			return new RGB(255, 255, 255);
		}

		if (fVertexList.size() == 1) {
			return fVertexList.get(0).getRGB();
		}

		for (int ix = fVertexList.size() - 2; ix >= 0; ix--) {
			if (elev > fVertexList.get(ix).getElevation()) {

				final RGB rgb1 = fVertexList.get(ix).getRGB();
				final RGB rgb2 = fVertexList.get(ix + 1).getRGB();
				final long elev1 = fVertexList.get(ix).getElevation();
				final long elev2 = fVertexList.get(ix + 1).getElevation();

				final long dElevG = elev2 - elev1;
				final long dElev1 = elev - elev1;
				final long dElev2 = elev2 - elev;

				int red = (int) ((double) (rgb2.red * dElev1 + rgb1.red * dElev2) / dElevG);
				int green = (int) ((double) (rgb2.green * dElev1 + rgb1.green * dElev2) / dElevG);
				int blue = (int) ((double) (rgb2.blue * dElev1 + rgb1.blue * dElev2) / dElevG);

				if (red > 0xFF)
					red = 0xFF;
				if (green > 0xFF)
					green = 0xFF;
				if (blue > 0xFF)
					blue = 0xFF;
				if (red < 0)
					red = 0;
				if (green < 0)
					green = 0;
				if (blue < 0)
					blue = 0;

				return new RGB(red, green, blue);
			}
		}
		return new RGB(255, 255, 255);
	}

	public int getSavedProfileKeyHashCode() {
		return fSavedProfileKeyHashCode;
	}

	public float getShadowValue() {
		return fShadowValue;
	}

	public String getTilePath() {
		return fTilePath;
	}

	private String getVertexKey() {
		final StringBuilder sb = new StringBuilder();
		for (final RGBVertex vertex : fVertexList) {
			sb.append(vertex.toString());
		}
		return sb.toString();
	}

	public ArrayList<RGBVertex> getVertexList() {
		return fVertexList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + fProfileId;
		return result;
	}

	public boolean isShadowState() {
		return fIsShadow;
	}

	public void set(String s) {
		final Pattern pattern = Pattern.compile("^([-]*[0-9]*),([0-9]*),([0-9]*),([0-9]*);(.*)$"); //$NON-NLS-1$
		fVertexList.clear();
		int ix = 0;
		while (s.length() > 0) {
			final Matcher matcher = pattern.matcher(s);
			if (matcher.matches()) {
				final Long elev = new Long(matcher.group(1));
				final Integer red = new Integer(matcher.group(2));
				final Integer green = new Integer(matcher.group(3));
				final Integer blue = new Integer(matcher.group(4));
				final RGBVertex rgbVertex = new RGBVertex();
				rgbVertex.setElev(elev.longValue());
				rgbVertex.setRGB(new RGB(red.intValue(), green.intValue(), blue.intValue()));
				fVertexList.add(ix, rgbVertex);
				ix++;
				s = matcher.group(5); // rest
			}
		}
		sort();
	}

	/**
	 * set default vertex list
	 */
	public void setDefaultVertexes() {
		if (fVertexList.size() > 0) {
			return;
		}
		fVertexList.add(0, new RGBVertex(0, 0, 255, 0));
		fVertexList.add(1, new RGBVertex(0, 255, 0, 1000));
		fVertexList.add(2, new RGBVertex(255, 0, 0, 2000));
	}

	public void setHorizontal() {
		fIsHorizontal = true;
	}

	/**
	 * Set unique id for each profile
	 * 
	 * @param profileId
	 */
	public void setProfileId(final int profileId) {
		fProfileId = profileId;
	}

	public void setProfileName(final String profileName) {
		fProfileName = profileName;
	}

	public void setResolution(final String resolution) {
		fResolution = resolution;
	}

	public void setShadowState(final Boolean isShadow) {
		fIsShadow = isShadow;
	}

	public void setShadowValue(final float value) {
		fShadowValue = value;
	}

	public void setTilePath(final String tilePath) {
		fTilePath = tilePath;
	}

	public void setVertexList(final ArrayList<RGBVertex> vertexList) {
		fVertexList.clear();
		for (final RGBVertex vertex : vertexList) {
			fVertexList.add(vertex);
		}
	}

	/**
	 * paint the profile image vertical, default is horizontal
	 */
	public void setVertical() {
		fIsHorizontal = false;
	}

	public void sort() {
		Collections.sort(fVertexList);
	}

}
