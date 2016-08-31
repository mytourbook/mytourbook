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
package net.tourbook.photo;

import java.time.LocalDateTime;

/**
 * Metadata for the original photo image file.
 */
public class PhotoImageMetadata {

	/**
	 * Is <code>true</code> when the image data are loaded from the image file, otherwise it is
	 * <code>false</code>.
	 */
	public boolean			isExifFromImage;

	/**
	 * Last modified date/time of the image file which is provided by the file system with the
	 * system time zone.
	 */
	public LocalDateTime	fileDateTime;

	/**
	 * Exif date/time which has no time zone.
	 */
	public LocalDateTime	exifDateTime;

	public int				imageWidth		= Integer.MIN_VALUE;
	public int				imageHeight		= Integer.MIN_VALUE;

	public int				orientation		= 1;

	public double			imageDirection	= Double.MIN_VALUE;

	public double			altitude		= Double.MIN_VALUE;

	/**
	 * Double.MIN_VALUE cannot be used, it cannot be saved in the database. 0 is the value when the
	 * value is not set !!!
	 */
	public double			latitude		= 0;
	public double			longitude		= 0;

	public String			gpsAreaInfo;

	/**
	 * Title
	 */
	public String			objectName;

	/**
	 * Description
	 */
	public String			captionAbstract;

	/**
	 * Camera or scanner name
	 */
	public String			model;

	public PhotoImageMetadata() {}

	@Override
	public String toString() {

		return "PhotoImageMetadata\n[\n" //$NON-NLS-1$

				+ ("fileDateTime=" + fileDateTime + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("exifDateTime=" + exifDateTime + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageWidth=" + imageWidth + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageHeight=" + imageHeight + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("orientation=" + orientation + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("imageDirection=" + imageDirection + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("altitude=" + altitude + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("latitude=" + latitude + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("longitude=" + longitude + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("gpsAreaInfo=" + gpsAreaInfo + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("objectName=" + objectName + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("captionAbstract=" + captionAbstract + ", \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("model=" + model + "\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ "]"; //$NON-NLS-1$
	}
}
