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
package de.byteholder.geoclipse.mapprovider;


import org.eclipse.jface.action.Action;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.Messages;

public class ActionZoomOutToMinZoom extends Action {

	private IMapDefaultActions	fMapActionProvider;

	public ActionZoomOutToMinZoom(final IMapDefaultActions mapActionProvider) {

		super(null, AS_PUSH_BUTTON);

		fMapActionProvider = mapActionProvider;

		setToolTipText(Messages.Map_Action_ZoomOutToMinZoom_Tooltip);

		setImageDescriptor(Activator.getImageDescriptor(Messages.App_Image_ZoomOutToMinZoom));
		setDisabledImageDescriptor(Activator.getImageDescriptor(Messages.App_Image_ZoomOutToMinZoom_Disabled));
	}

	@Override
	public void run() {
		fMapActionProvider.actionZoomOutToMinZoom();
	}

}
