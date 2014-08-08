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
package net.tourbook.photo;

import org.joda.time.DateTime;

/**
 * Metadata for the original photo image file.
 */
public class PhotoImageMetadata {

	/**
	 * Is <code>true</code> when the image data are loaded from the image file, otherwise it is
	 * <code>false</code>.
	 */
	public boolean	isExifFromImage;

	/**
	 * Last modified in GMT
	 */
	public DateTime	fileDateTime;
	public DateTime	exifDateTime;

	public int		imageWidth		= Integer.MIN_VALUE;
	public int		imageHeight		= Integer.MIN_VALUE;

	public int		orientation		= 1;

	public double	imageDirection	= Double.MIN_VALUE;

	public double	altitude		= Double.MIN_VALUE;

	/**
	 * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
	 * value is not set !!!
	 */
	public double	latitude		= 0;
	public double	longitude		= 0;

	public String	gpsAreaInfo;

	/**
	 * Title
	 */
	public String	objectName;

	/**
	 * Description
	 */
	public String	captionAbstract;

	/**
	 * Camera or scanner name
	 */
	public String	model;

	public PhotoImageMetadata() {}

	@Override
	public String toString() {
		return "PhotoImageMetadata\n[\nfileDateTime=" //$NON-NLS-1$
				+ fileDateTime
				+ ", \n" //$NON-NLS-1$
				+ "exifDateTime=" //$NON-NLS-1$
				+ exifDateTime
				+ ", \n" //$NON-NLS-1$
				+ "imageWidth=" //$NON-NLS-1$
				+ imageWidth
				+ ", \n" //$NON-NLS-1$
				+ "imageHeight=" //$NON-NLS-1$
				+ imageHeight
				+ ", \n" //$NON-NLS-1$
				+ "orientation=" //$NON-NLS-1$
				+ orientation
				+ ", \n" //$NON-NLS-1$
				+ "imageDirection=" //$NON-NLS-1$
				+ imageDirection
				+ ", \n" //$NON-NLS-1$
				+ "altitude=" //$NON-NLS-1$
				+ altitude
				+ ", \n" //$NON-NLS-1$
				+ "latitude=" //$NON-NLS-1$
				+ latitude
				+ ", \nlongitude=" //$NON-NLS-1$
				+ longitude
				+ ", \ngpsAreaInfo=" //$NON-NLS-1$
				+ gpsAreaInfo
				+ ", \nobjectName=" //$NON-NLS-1$
				+ objectName
				+ ", \ncaptionAbstract=" //$NON-NLS-1$
				+ captionAbstract
				+ ", \nmodel=" //$NON-NLS-1$
				+ model
				+ "\n]"; //$NON-NLS-1$
	}
}
