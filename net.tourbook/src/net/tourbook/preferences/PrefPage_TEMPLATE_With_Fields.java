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
package net.tourbook.preferences;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPage_TEMPLATE_With_Fields extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID			= "net.tourbook.preferences.PrefPageGeneralExternalProgramsID"; //$NON-NLS-1$

	private IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	/*
	 * UI controls
	 */
	private PixelConverter		_pc;

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
		enableControls();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		_pc = new PixelConverter(parent);
	}

	private void enableControls() {

	}

	@Override
	public void init(final IWorkbench workbench) {

		setPreferenceStore(_prefStore);
	}

	@Override
	public boolean okToLeave() {

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			saveState();

		}

		return isOK;
	}

	private void restoreState() {

	}

	private void saveState() {

	}
}
