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
package net.tourbook.photo;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

public interface IExternalGalleryListener {

	/**
	 * @param eventType
	 *            Mouse event type.
	 * @param mouseEvent
	 * @return Returns <code>true</code> when mouse events are handled externally and the gallery
	 *         mouse events are skipped.
	 */
	boolean isMouseEventHandledExternally(int eventType, MouseEvent mouseEvent);

	/**
	 * Can be used to paint on the {@link GC} after all other paints are done.
	 * 
	 * @param gc
	 * @param clippingArea
	 * @param clientArea
	 */
	void onPaintAfter(GC gc, Rectangle clippingArea, Rectangle clientArea);

}
