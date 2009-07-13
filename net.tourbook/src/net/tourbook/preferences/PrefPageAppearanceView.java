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
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceView extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		VIEW_TIME_LAYOUT_HH_MM		= "hh_mm";											//$NON-NLS-1$
	public static final String		VIEW_TIME_LAYOUT_HH_MM_SS	= "hh_mm_ss";										//$NON-NLS-1$

	private final IPreferenceStore	fPrefStore					= TourbookPlugin.getDefault().getPreferenceStore();
	private boolean					fIsModified;

	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();

		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);

		final Group colorGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorGroup);
		GridLayoutFactory.fillDefaults()//
				.margins(5, 5)
				.spacing(30, LayoutConstants.getSpacing().y)
				.numColumns(2)
				.applyTo(colorGroup);
		colorGroup.setText(Messages.pref_view_layout_label_color_group);

		final Composite containerDefaultView = new Composite(colorGroup, SWT.NONE);
		{
			// color: tag category
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY,
					Messages.pref_view_layout_label_category,
					containerDefaultView));

			// color: tag 
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE, //
					Messages.pref_view_layout_label_title,
					containerDefaultView));

			// color: sub tag (year)
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB,
					Messages.pref_view_layout_label_sub,
					containerDefaultView));

			// color: sub sub tag (month)
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB,
					Messages.pref_view_layout_label_sub_sub,
					containerDefaultView));
//
//			GridLayout gl = (GridLayout) containerDefaultView.getLayout();
//			gl.marginHeight = 5;
//			gl.marginWidth = 5;
		}

		final Composite containerSegmenter = new Composite(colorGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(containerSegmenter);
		{
			// color: up
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP,
					Messages.pref_view_layout_label_segmenter_up,
					containerSegmenter));

			// color: down
			addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN,
					Messages.pref_view_layout_label_segmenter_down,
					containerSegmenter));
		}

		/*
		 * container: column time format
		 */
		final Group formatGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(formatGroup);
		formatGroup.setText(Messages.pref_view_layout_group_display_format);

		/*
		 * recording time format: hh:mm
		 */
		addField(new RadioGroupFieldEditor(ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT,
				Messages.pref_view_layout_label_recording_time_format,
				2,
				new String[][] {
						{ Messages.pref_view_layout_label_format_hh_mm, VIEW_TIME_LAYOUT_HH_MM },
						{ Messages.pref_view_layout_label_format_hh_mm_ss, VIEW_TIME_LAYOUT_HH_MM_SS } },
				formatGroup,
				false));

		/*
		 * driving time format: hh:mm
		 */
		addField(new RadioGroupFieldEditor(ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT,
				Messages.pref_view_layout_label_driving_time_format,
				2,
				new String[][] {
						{ Messages.pref_view_layout_label_format_hh_mm, VIEW_TIME_LAYOUT_HH_MM },
						{ Messages.pref_view_layout_label_format_hh_mm_ss, VIEW_TIME_LAYOUT_HH_MM_SS } },
				formatGroup,
				false));

		// set group margin after the fields are created
		final GridLayout gl = (GridLayout) formatGroup.getLayout();
		gl.marginHeight = 5;
		gl.marginWidth = 5;
		gl.numColumns = 2;

		/*
		 * container: other
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).applyTo(container);

		// show lines
		addField(new BooleanFieldEditor(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES,
				Messages.pref_view_layout_display_lines,
				container));
	}

	private void fireModifyEvent() {

		if (fIsModified) {

			fIsModified = false;

			UI.setViewColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
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
