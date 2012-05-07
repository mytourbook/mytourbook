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
package net.tourbook.photo.gallery.MT20;

/**
 * This gallery has it's origin in http://www.eclipse.org/nebula/widgets/gallery/gallery.php but has
 * been modified in many areas, like grouping has been removed, filtering has been added.
 */

public class GalleryMT20Item {

	public GalleryMT20	galleryMT20;

	public Object		data;

	/**
	 * Screen viewport for this gallery item where it is painted for the currently scrolled gallery
	 * position.
	 */
	public int			viewPortX;
	public int			viewPortY;

	public int			width;
	public int			height;

	/**
	 * Each gallery item needs a uniqueue id.
	 */
	public String		uniqueItemID;

	public GalleryMT20Item(final GalleryMT20 galleryMT20) {
		this.galleryMT20 = galleryMT20;
	}

	/**
	 * Set data for the gallery item and a unique key to identify it (e.g. for selection)
	 * 
	 * @param data
	 * @param uniqueItemID
	 */
	public void setData(final Object data, final String uniqueItemID) {
		this.data = data;
		this.uniqueItemID = uniqueItemID;
	}

	@Override
	public String toString() {
		return ""
//				"GalleryMT20Item"
				+ (" 			x=" + viewPortX)
				+ ("\ty=" + viewPortY)
				+ ("\t" + width)
				+ ("x" + height)
				+ (" " + data)
//
		;
	}

}
