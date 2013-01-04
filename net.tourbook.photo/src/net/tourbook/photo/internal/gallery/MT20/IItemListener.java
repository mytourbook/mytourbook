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

public interface IItemListener {

	/**
	 * Mouse has hovered another gallery item.
	 * 
	 * @param exitHoveredItem
	 *            Item which was previously hovered, can be <code>null</code>.
	 * @param itemMouseY
	 *            Mouse X position relative to the hovered item.
	 * @param itemMouseX
	 *            Mouse Y position relative to the hovered item.
	 */
	public void exitItem(GalleryMT20Item exitHoveredItem, int itemMouseX, int itemMouseY);

	/**
	 * Mouse has hovered a gallery item.
	 * 
	 * @param hoveredItem
	 *            Hovered item or <code>null</code> when mouse has not hovered over a gallery item.
	 * @param itemMouseY
	 *            Mouse X position relative to the hovered item.
	 * @param itemMouseX
	 *            Mouse Y position relative to the hovered item.
	 */
	public void hoveredItem(GalleryMT20Item hoveredItem, int itemMouseX, int itemMouseY);

}
