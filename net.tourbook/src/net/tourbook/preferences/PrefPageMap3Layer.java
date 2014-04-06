/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.map3.ui.Map3LayerUI;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageMap3Layer extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID	= "net.tourbook.preferences.PrefPageMap3Layer"; //$NON-NLS-1$

	public PrefPageMap3Layer() {

		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		// close layer tooltip because the UI could be changed in this pref page
		Map3Manager.closeMap3LayerTooltip();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			new Map3LayerUI(container);
		}

		return container;
	}

	public void init(final IWorkbench workbench) {}

}
