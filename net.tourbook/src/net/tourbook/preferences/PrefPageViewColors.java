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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageViewColors extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	fPrefStore	= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					fIsModified;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();

		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		final Group colorGroup = new Group(parent, SWT.NONE);
		colorGroup.setText(Messages.pref_tag_label_color_group);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorGroup);

		// color: tag category
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_TAG_CATEGORY_COLOR,
				Messages.pref_tag_color_tag_category,
				colorGroup));

		// color: tag 
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_TAG_COLOR, //
				Messages.pref_tag_color_tag,
				colorGroup));

		// color: sub tag (year)
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_SUB_TAG_COLOR,
				Messages.pref_tag_color_sub_tag,
				colorGroup));

		// color: sub sub tag (month)
		addField(new ColorFieldEditor(ITourbookPreferences.TAG_VIEW_SUB_SUB_TAG_COLOR,
				Messages.pref_tag_color_sub_sub_tag,
				colorGroup));

		final GridLayout gl = (GridLayout) colorGroup.getLayout();
		gl.marginHeight = 5;
		gl.marginWidth = 5;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).applyTo(container);

		// show lines
		addField(new BooleanFieldEditor(ITourbookPreferences.TAG_VIEW_SHOW_LINES,
				Messages.pref_tag_show_lines,
				container));
	}

	private void fireModifyEvent() {

		if (fIsModified) {

			fIsModified = false;

			UI.setTagColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.TAG_COLOR_AND_LAYOUT_CHANGED, Math.random());
		}
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	public boolean okToLeave() {

		if (fIsModified) {

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {
		fIsModified = true;
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

	@Override
	public void propertyChange(final PropertyChangeEvent event) {
		fIsModified = true;
		super.propertyChange(event);
	}

}
