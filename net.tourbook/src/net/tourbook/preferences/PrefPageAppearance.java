/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.ui.BooleanFieldEditor2;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageAppearance extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	fPrefStore	= TourbookPlugin.getDefault().getPreferenceStore();
 
	public PrefPageAppearance() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(container);
		{
			// editor: line width
			final IntegerFieldEditor editor = new IntegerFieldEditor(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS,
					Messages.pref_appearance_number_of_recent_tags,
					container);
			addField(editor);
			UI.setFieldWidth(container, editor, UI.DEFAULT_FIELD_WIDTH);
			editor.setValidRange(2, 9);
			editor.getLabelControl(container).setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);

			// checkbox: show memory monitor
			final BooleanFieldEditor2 chkMemoryMonitor = new BooleanFieldEditor2(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR,
					Messages.pref_appearance_showMemoryMonitor,
					container);
			addField(chkMemoryMonitor);
			final GridData gd = (GridData) chkMemoryMonitor.getChangeControl(container).getLayoutData();
			gd.horizontalSpan = 2;
		}
		GridLayoutFactory.swtDefaults()//
				.margins(5, 5)
				.numColumns(2)
				.applyTo(container);

	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(fPrefStore);
	}

	@Override
	public boolean performOk() {

		final boolean isShowMemoryOld = fPrefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR);

		// update prefstore from fields
		final boolean isOK = super.performOk();

		final boolean isShowMemoryNew = fPrefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR);
		
		if (isShowMemoryNew != isShowMemoryOld) {
			if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
					Messages.pref_appearance_showMemoryMonitor_title,
					Messages.pref_appearance_showMemoryMonitor_message)) {

				Display.getCurrent().asyncExec(new Runnable() {
					public void run() {
						PlatformUI.getWorkbench().restart();
					}
				});
			}
		}

		return isOK;
	}

}
