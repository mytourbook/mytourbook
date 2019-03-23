/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;

import org.eclipse.jface.action.Action;

public class ActionSyncMapWithTour extends Action {

	private Map2View	fMapView;

	public ActionSyncMapWithTour(final Map2View mapView) {

		super(null, AS_CHECK_BOX);

		fMapView = mapView;

		setToolTipText(Messages.map_action_synch_with_tour);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_tour));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_synch_with_tour_disabled));
	}

	@Override
	public void run() {
		fMapView.action_SyncWith_Tour();
	}

}
