/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * Shows the selected tours in a conconi test chart
 */
public class ConconiChartView extends ViewPart {

	public static final String		ID									= "net.tourbook.views.ConconiChartView";	//$NON-NLS-1$

	private static final String		STATE_CONCONIT_TOURS_VIEWER_WIDTH	= "STATE_CONCONIT_TOURS_VIEWER_WIDTH";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore							= TourbookPlugin.getDefault() //
																				.getPreferenceStore();
	private final IDialogSettings	_state								= TourbookPlugin.getDefault().//
																				getDialogSettingsSection(ID);
	@Override
	public void createPartControl(final Composite parent) {
		// TODO Auto-generated method stub

	}
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}


}
