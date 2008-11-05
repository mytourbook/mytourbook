/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearance extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	fPrefStore				= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					fIsModified;

	public PrefPageAppearance() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);

		// line width
		final IntegerFieldEditor editor = new IntegerFieldEditor(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS,
				Messages.pref_appearance_number_of_recent_tags,
				container);
		addField(editor);
		UI.setFieldWidth(container, editor, UI.DEFAULT_FIELD_WIDTH);
		editor.setValidRange(2, 9);
		editor.getLabelControl(container).setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void performDefaults() {
		fIsModified = true;
		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK && fIsModified) {

			fIsModified = false;

			// fire one event for all modifications
//			getPreferenceStore().setValue(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED, Math.random());
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		fIsModified = true;
		super.propertyChange(event);
	}

}
