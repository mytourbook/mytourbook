/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.common.util;

import org.eclipse.swt.graphics.Image;

/**
 * Contains the context for a hovered area which displays a tool tip when the mouse hoveres the
 * hovered area.
 * <p>
 * Each property is set in this context or it can be <code>null</code> when documented.
 */
public class HoveredAreaContext {

	/**
	 * Tour tool tip provider which provides a tool tip for the hovered area.
	 */
	public ITourToolTipProvider	tourToolTipProvider;

	/**
	 * Image which is displayed when the mouse is hovered over the hovered area, when
	 * <code>null</code> an hovered image is not available.
	 */
	public Image				hoveredImage;

	/**
	 * Left corner of the hovereded area relativ to the client area
	 */
	public int					hoveredTopLeftX;

	/**
	 * top corner of the hovereded area
	 */
	public int					hoveredTopLeftY;

	/**
	 * Hovered area width
	 */
	public int					hoveredWidth;

	/**
	 * Hovered area height
	 */
	public int					hoveredHeight;

	/**
	 * @param tourToolTipProvider
	 * @param hoveredArea
	 * @param devTopLeftX
	 * @param devTopLeftY
	 * @param devWidth
	 * @param devHeight
	 */
	public HoveredAreaContext(	final ITourToolTipProvider tourToolTipProvider,
								final IHoveredArea hoveredArea,
								final int devTopLeftX,
								final int devTopLeftY,
								final int devWidth,
								final int devHeight) {

		this.tourToolTipProvider = tourToolTipProvider;

		hoveredImage = hoveredArea.getHoveredImage();

		hoveredTopLeftX = devTopLeftX;
		hoveredTopLeftY = devTopLeftY;
		hoveredWidth = devWidth;
		hoveredHeight = devHeight;
	}

	@Override
	public String toString() {
		return "HoveredAreaContext [" // //$NON-NLS-1$
//				+ ("tourToolTipProvider=" + tourToolTipProvider + ", ")
//				+ ("hoveredImage=" + hoveredImage + ", ")
//				+ ("hoveredTopLeftX=" + hoveredTopLeftX + ", ")
//				+ ("hoveredTopLeftY=" + hoveredTopLeftY + ", ")
//				+ ("hoveredWidth=" + hoveredWidth + ", ")
//				+ ("hoveredHeight=" + hoveredHeight)
				+ "]" //$NON-NLS-1$
				+ (" " + this.hashCode()); //$NON-NLS-1$
	}

}
