/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.manager;

import org.joda.time.DateTime;

/**
 * Metadata for the original photo image file.
 */
public class PhotoImageMetadata {

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

	public double	latitude		= Double.MIN_VALUE;
	public double	longitude		= Double.MIN_VALUE;

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
}
