/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;

/**
 * Delete selected marker
 */
public class ActionDeleteMarker extends Action {

	private TourMarkerView	fMarkerView;

	public ActionDeleteMarker(TourMarkerView markerView) {

		fMarkerView = markerView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.TourMarker_Image_delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.TourMarker_Image_delete_disabled));

		setText(Messages.TourMarker_Action_delete_marker);

		setEnabled(false);
	}

	public void run() {
		fMarkerView.removeSelectedMarkers();
	}

}
