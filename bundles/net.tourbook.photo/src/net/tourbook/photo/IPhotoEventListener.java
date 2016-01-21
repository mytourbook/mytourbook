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
package net.tourbook.photo;

import org.eclipse.ui.IViewPart;

public interface IPhotoEventListener {

	/**
	 * @param viewPart
	 *            View which fired this event or <code>null</code> when part in undefined, e.g.
	 *            fired froom a tooltip.
	 * @param photoEventId
	 * @param data
	 *            Contains event specific data.
	 */
	void photoEvent(IViewPart viewPart, PhotoEventId photoEventId, Object data);

}
