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
package net.tourbook.map3.action;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionShowEntireTour extends Action {

	private Map3View	_map3View;

	public ActionShowEntireTour(final Map3View map3View) {

		super(null, AS_PUSH_BUTTON);

		_map3View = map3View;

		setToolTipText(Messages.map_action_zoom_show_entire_tour);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_zoom_show_entire_tour));
	}

	@Override
	public void run() {
		_map3View.actionZoomShowEntireTour();
	}

}
