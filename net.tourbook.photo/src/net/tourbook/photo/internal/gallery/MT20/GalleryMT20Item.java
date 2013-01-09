/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.gallery.MT20;

import java.util.Collection;

import net.tourbook.photo.Photo;

/**
 * This gallery has it's origin in http://www.eclipse.org/nebula/widgets/gallery/gallery.php but has
 * been modified in many areas, like grouping has been removed, filtering has been added.
 */

public class GalleryMT20Item {

	public GalleryMT20					gallery;

	/**
	 * Screen viewport for this gallery item where it is painted for the currently scrolled gallery
	 * position.
	 */
	public int							viewPortX;
	public int							viewPortY;

	public int							width;
	public int							height;

	/**
	 * Photo, can be <code>null</code> when not yet initialized.
	 */
	public Photo						photo;

	/**
	 * Each gallery item needs a uniqueue id.
	 */
	public String						uniqueItemID;

	/**
	 * When width is <code>-1</code>, the image is not yet painted
	 */
	public int							imagePaintedWidth	= -1;
	public int							imagePaintedHeight;

	/**
	 * Is <code>true</code> when this gallery item is currently be hovered with the mouse.
	 */
	public boolean						isHovered;

	public boolean						isInHoveredGroup;

	/**
	 * These are the selected gallery items and this values is only set while a gallery item is
	 * hovered which is contained in this collection.
	 */
	public Collection<GalleryMT20Item>	allSelectedGalleryItems;

	/**
	 * Number of stars which are hovered.
	 */
	public int							hoveredStars;

	/**
	 * X position where this gallery item is painted on the canvas.
	 */
	public int							photoPaintedX;

	/**
	 * Y position where this gallery item is painted on the canvas.
	 */
	public int							photoPaintedY;

	public GalleryMT20Item(final GalleryMT20 galleryMT20) {
		this.gallery = galleryMT20;
	}

	@Override
	public String toString() {
		return "" //$NON-NLS-1$
//				"GalleryMT20Item"
				+ (" 			x=" + viewPortX) //$NON-NLS-1$
				+ ("\ty=" + viewPortY) //$NON-NLS-1$
				+ ("\t" + width) //$NON-NLS-1$
				+ ("x" + height) //$NON-NLS-1$
				+ (" " + photo) //$NON-NLS-1$
//
		;
	}

}
