/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceTourEditor extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private int	fOldDescriptionLines;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUI(parent);

	}

	private void createUI(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		// text: description height
		final IntegerFieldEditor fieldEditor = new IntegerFieldEditor(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT,
				Messages.pref_tour_editor_description_height,
				group);
		fieldEditor.setValidRange(2, 100);
		fieldEditor.getLabelControl(group).setToolTipText(Messages.pref_tour_editor_description_height_tooltip);
		UI.setFieldWidth(group, fieldEditor, UI.DEFAULT_FIELD_WIDTH);
		addField(fieldEditor);

		// set margins after the field editors are added
		final GridLayout groupLayout = (GridLayout) group.getLayout();
		groupLayout.marginWidth = 5;
		groupLayout.marginHeight = 5;
	}

	public void init(final IWorkbench workbench) {
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(prefStore);

		fOldDescriptionLines = prefStore.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		final int newDescriptionLines = prefStore.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
		if (fOldDescriptionLines != newDescriptionLines) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.pref_tour_editor_dlg_desc_height_title,
					Messages.pref_tour_editor_dlg_desc_height_message);
		}
		return isOK;
	}
}
