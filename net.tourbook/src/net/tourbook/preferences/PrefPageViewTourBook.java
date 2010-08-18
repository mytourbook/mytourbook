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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.BooleanFieldEditor2;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageViewTourBook extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID	= "net.tourbook.preferences.PrefPageViewTourBook";	//$NON-NLS-1$

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);
		{
			createUI10ViewTooltip(parent);
		}
	}

	private void createUI10ViewTooltip(final Composite parent) {

		BooleanFieldEditor2 editor;

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_ViewTooltip_Group);
		{
			final Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults().span(5, 1).hint(400, SWT.DEFAULT).applyTo(label);
			label.setText(Messages.PrefPage_ViewTooltip_Label_Info);

			/*
			 * checkbox: first column (year/month/day)
			 */
			addField(editor = new BooleanFieldEditor2(
					ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE,
					Messages.PrefPage_ViewTooltip_Label_Day,
					group));

			// set vertical space before the first field editor
			final GridData gd = (GridData) editor.getChangeControl(group).getLayoutData();
//			gd.verticalIndent = 10;

			/*
			 * checkbox: time
			 */
			addField(new BooleanFieldEditor(
					ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME,
					Messages.PrefPage_ViewTooltip_Label_Time,
					group));

			/*
			 * checkbox:
			 */
			addField(new BooleanFieldEditor(
					ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY,
					Messages.PrefPage_ViewTooltip_Label_WeekDay,
					group));

			/*
			 * checkbox:
			 */
			addField(new BooleanFieldEditor(
					ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE,
					Messages.PrefPage_ViewTooltip_Label_Title,
					group));

			/*
			 * checkbox:
			 */
			addField(new BooleanFieldEditor(
					ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS,
					Messages.PrefPage_ViewTooltip_Label_Tags,
					group));
		}

		// force layout after the fields are set !!!
		GridLayoutFactory.swtDefaults().numColumns(5).applyTo(group);
	}

	public void init(final IWorkbench workbench) {

		setPreferenceStore(TourbookPlugin.getDefault().getPreferenceStore());
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK, Math.random());
		}

		return isOK;
	}
}
