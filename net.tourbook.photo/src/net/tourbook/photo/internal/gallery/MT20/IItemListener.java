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
	 * Mouse down event occured in the gallery item.
	 * 
	 * @param mouseDownItem
	 * @param itemMouseX
	 * @param itemMouseY
	 * @return Returns <code>true</code> when the mouse down event is handled and no further actions
	 *         should be done in the gallery (e.g. no select item).
	 */
	public boolean onItemMouseDown(GalleryMT20Item mouseDownItem, int itemMouseX, int itemMouseY);

	/**
	 * Mouse has hovered another gallery item.
	 * 
	 * @param exitHoveredItem
	 *            Item which was previously hovered, cannot be <code>null</code>. When it's
	 *            <code>null</code> this method is not called.
	 * @param itemMouseY
	 *            Mouse X position relative to the hovered item.
	 * @param itemMouseX
	 *            Mouse Y position relative to the hovered item.
	 */
	public void onItemMouseExit(GalleryMT20Item exitHoveredItem, int itemMouseX, int itemMouseY);

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
	public void onItemMouseHovered(GalleryMT20Item hoveredItem, int itemMouseX, int itemMouseY);

}
