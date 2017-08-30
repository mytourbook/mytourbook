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
package net.tourbook.map2.action;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;

public class ActionSyncMap2WithOtherMap extends Action {

	private Map2View _map2View;

	public ActionSyncMap2WithOtherMap(final Map2View mapView) {

		super(null, AS_CHECK_BOX);

		_map2View = mapView;

		setToolTipText(Messages.Map_Action_SynchWithOtherMap);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncMapWithOtherMap));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncMapWithOtherMap_Disabled));
	}

	@Override
	public void run() {

		_map2View.actionSync_WithOtherMap(isChecked());
	}

}
