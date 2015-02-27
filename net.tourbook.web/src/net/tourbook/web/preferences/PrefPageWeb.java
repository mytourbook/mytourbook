/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.web.preferences;

import net.tourbook.web.Activator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageWeb extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= Activator.getDefault().getPreferenceStore();

	/*
	 * UI constrols
	 */

	/*
	 * none UI controls
	 */

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

		}
	}

	private void fireModifyEvent() {

//		if (_isToolTipModified) {
//
//			_isToolTipModified = false;
//
//			// fire one event for all modified tooltip values
//			getPreferenceStore().setValue(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED, Math.random());
//		}
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			fireModifyEvent();
		}

		return isOK;
	}

	private void restoreState() {

	}

}
