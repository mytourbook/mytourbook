/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.PreferencesUtil;

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.preferences.IMappingPreferences;
import de.byteholder.geoclipse.preferences.PrefPageMapProviders;

public class ActionManageMapProviders extends Action {

	private TourMapView	_tourMapView;

	public ActionManageMapProviders(final TourMapView tourMapView) {

		super(Messages.Map_Action_ManageMapProviders, AS_PUSH_BUTTON);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(net.tourbook.Messages.Image__options));

		_tourMapView = tourMapView;
	}

	@Override
	public void run() {

		// set the currently displayed map provider so that this mp will be selected in the pref page
		final IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

		prefStore.setValue(//
				IMappingPreferences.MAP_FACTORY_LAST_SELECTED_MAP_PROVIDER,
				_tourMapView.getMap().getMapProvider().getId());

		PreferencesUtil.createPreferenceDialogOn(
				Display.getCurrent().getActiveShell(),
				PrefPageMapProviders.PREF_PAGE_MAP_PROVIDER_ID,
				null,
				null).open();
	}

}
