/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.action.Action;

public class ActionSyncMapWithOtherMaps extends Action {

	private Map3View _map3View;

	public ActionSyncMapWithOtherMaps(final Map3View mapView) {

		super(null, AS_CHECK_BOX);

		_map3View = mapView;

		setToolTipText(Messages.Map_Action_SynchWithOtherMap);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncMapWithOtherMap));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncMapWithOtherMap_Disabled));
	}

	@Override
	public void run() {
		_map3View.actionSynchMapWithOtherMap();
	}

}
