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
package net.tourbook.device.polar.hrm;

import net.tourbook.common.UI;
import net.tourbook.device.Activator;
import net.tourbook.tour.DialogAdjustAltitude;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePolar extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= Activator.getDefault().getPreferenceStore();

	private Group					_groupTitleDescription;
	private Group					_groupSliceAdjustment;
	private Group					_ppdImportInfo;
	private Spinner					_spinnerSliceAdjustment;

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUI10TitleDescription(parent);
		createUI20HorizontalAdjustment(parent);
		createUI30PPDImportInfo(parent);
	}

	private void createUI10TitleDescription(final Composite parent) {

		RadioGroupFieldEditor editor;
		_groupTitleDescription = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupTitleDescription);
		_groupTitleDescription.setText(Messages.PrefPage_Polar_Group_TitleDescription);
		{
			/*
			 * radio: altitude
			 */
			editor = new RadioGroupFieldEditor(
					IPreferences.TITLE_DESCRIPTION,
					Messages.PrefPage_Polar_Field_TitleDescription,
					1,
					new String[][] {
							new String[] {
									Messages.PrefPage_Polar_Radio_TitleFromTitle,
									IPreferences.TITLE_DESCRIPTION_TITLE_FROM_TITLE },
							new String[] {
									Messages.PrefPage_Polar_Radio_TitleFromDescription,
									IPreferences.TITLE_DESCRIPTION_TITLE_FROM_DESCRIPTION }, },
					_groupTitleDescription,
					false);
			addField(editor);

		}

		// force layout after the fields are set !!!
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupTitleDescription);

		final Label editorControl = editor.getLabelControl(_groupTitleDescription);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.TOP).applyTo(editorControl);
	}

	private void createUI20HorizontalAdjustment(final Composite parent) {

		_groupSliceAdjustment = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupSliceAdjustment);
		_groupSliceAdjustment.setText(Messages.PrefPage_Polar_Group_HorizontalAdjustment);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(_groupSliceAdjustment);
		{
			/*
			 * label: slice adjustment
			 */
			Label label = new Label(_groupSliceAdjustment, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.PrefPage_Polar_Label_SliceAdjustment);

			/*
			 * spinner: slice adjustment
			 */
			_spinnerSliceAdjustment = new Spinner(_groupSliceAdjustment, SWT.BORDER);
			_spinnerSliceAdjustment.setMinimum(-DialogAdjustAltitude.MAX_ADJUST_GEO_POS_SLICES);
			_spinnerSliceAdjustment.setMaximum(DialogAdjustAltitude.MAX_ADJUST_GEO_POS_SLICES);
			_spinnerSliceAdjustment.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			/*
			 * label: slice adjustment
			 */
			label = new Label(_groupSliceAdjustment, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.PrefPage_Polar_Label_Slices);

			/*
			 * label: info
			 */
			label = new Label(_groupSliceAdjustment, SWT.WRAP);
			GridDataFactory
					.fillDefaults()
					.span(3, 1)
					.grab(true, false)
					.indent(0, 10)
					.hint(400, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.PrefPage_Polar_Label_AdjustmentInfo);
		}
	}

	private void createUI30PPDImportInfo(final Composite parent) {

		_ppdImportInfo = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_ppdImportInfo);
		_ppdImportInfo.setText(Messages.PrefPage_Polar_Group_PPDImportInfo);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(_ppdImportInfo);
		
		/*
		 * label: info
		 */
		final Label label = new Label(_ppdImportInfo, SWT.WRAP);
		GridDataFactory.fillDefaults().grab(true, false).hint(400, SWT.DEFAULT).applyTo(label);
		label.setText(Messages.PrefPage_Polar_Label_PPDImportInfo);
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void performDefaults() {

		_spinnerSliceAdjustment.setSelection(_prefStore.getDefaultInt(IPreferences.SLICE_ADJUSTMENT_VALUE));

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		// spinner: slice adjustment
		_spinnerSliceAdjustment.setSelection(_prefStore.getInt(IPreferences.SLICE_ADJUSTMENT_VALUE));
	}

	private void saveState() {

		// spinner: slice adjustment
		_prefStore.setValue(IPreferences.SLICE_ADJUSTMENT_VALUE, _spinnerSliceAdjustment.getSelection());
	}

}
