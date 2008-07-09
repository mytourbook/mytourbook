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

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageTagColors extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	fPrefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		// color: tag category
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_TAG_CATEGORY_COLOR,
				Messages.pref_tag_color_tag_category,
				parent));

		// color: tag 
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_TAG_COLOR, //
				Messages.pref_tag_color_tag,
				parent));

		// color: sub tag (year)
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_SUB_TAG_COLOR,
				Messages.pref_tag_color_sub_tag,
				parent));

		// color: sub sub tag (month)
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_SUB_SUB_TAG_COLOR,
				Messages.pref_tag_color_sub_sub_tag,
				parent));

	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	protected void performDefaults() {
		// TODO Auto-generated method stub
		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			UI.setTagColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.TAG_COLOR_CHANGED, Math.random());
		}

		return isOK;
	}

}
