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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageViews extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		VIEW_TIME_LAYOUT_HH_MM							= "hh_mm";						//$NON-NLS-1$
	public static final String		VIEW_TIME_LAYOUT_HH_MM_SS						= "hh_mm_ss";					//$NON-NLS-1$

	public static final String		VIEW_DOUBLE_CLICK_ACTION_NONE					= "None";						//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_QUICK_EDIT				= "QuickEdit";					//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_EDIT_TOUR				= "EditTour";					//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_EDIT_MARKER			= "EditMarker";				//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_ADJUST_ALTITUDE		= "AdjustAltitude";			//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_OPEN_TOUR_IN_EDIT_AREA	= "OpenTourSeparately";		//$NON-NLS-1$

	private String[][]				_doubleClickActions								= new String[][] {
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_QuickEdit, VIEW_DOUBLE_CLICK_ACTION_QUICK_EDIT },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_EditTour, VIEW_DOUBLE_CLICK_ACTION_EDIT_TOUR },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_EditMarker, VIEW_DOUBLE_CLICK_ACTION_EDIT_MARKER },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_AdjustAltitude, VIEW_DOUBLE_CLICK_ACTION_ADJUST_ALTITUDE },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_OpenTour, VIEW_DOUBLE_CLICK_ACTION_OPEN_TOUR_IN_EDIT_AREA },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_None, VIEW_DOUBLE_CLICK_ACTION_NONE }, };

	private final IPreferenceStore	_prefStore										= TourbookPlugin
																							.getDefault()
																							.getPreferenceStore();

	@Override
	protected void createFieldEditors() {

		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

			createUI10ViewActions(parent);
		}
	}

	private void createUI10ViewActions(final Composite parent) {

		/*
		 * group: column time format
		 */
		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_ViewActions_Group);
		{
			/*
			 * label: info
			 */
			Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(label);
			label.setText(Messages.PrefPage_ViewActions_Label_Info);

			// spacer
			label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().span(2, 1).hint(0, 2).applyTo(label);

			/*
			 * combo: double click
			 */

			addField(new ComboFieldEditor(
					ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS,
					Messages.PrefPage_ViewActions_Label_DoubleClick,
					_doubleClickActions,
					group));

			/**
			 * modifier key's do not work correctly in a tree or table
			 */
//			/*
//			 * combo: ctrl double click
//			 */
//			addField(new ComboFieldEditor(
//					ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS_CTRL,
//					Messages.PrefPage_ViewActions_Label_DoubleClickWithCtrl,
//					_doubleClickActions,
//					group));
//
//			/*
//			 * combo: shift double click
//			 */
//			addField(new ComboFieldEditor(
//					ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS_SHIFT,
//					Messages.PrefPage_ViewActions_Label_DoubleClickWithShift,
//					_doubleClickActions,
//					group));
//
//			/*
//			 * combo: ctrl+shift double click
//			 */
//			addField(new ComboFieldEditor(
//					ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS_CTRL_SHIFT,
//					Messages.PrefPage_ViewActions_Label_DoubleClickWithCtrlShift,
//					_doubleClickActions,
//					group));
		}
		// set group margin after the fields are created
		final GridLayout gl = (GridLayout) group.getLayout();
		gl.marginHeight = 5;
		gl.marginWidth = 5;
		gl.numColumns = 2;

	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

}
