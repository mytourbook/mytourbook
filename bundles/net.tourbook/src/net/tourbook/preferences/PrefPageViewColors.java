/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.common.preferences.BooleanFieldEditor2;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageViewColors extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private boolean					_isOtherModified;

	@Override
	protected void createFieldEditors() {

		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

			createUI10Colors(parent);
		}
	}

	private void createUI10Colors(final Composite parent) {

		final Group colorGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorGroup);
		GridLayoutFactory.fillDefaults()//
				.margins(5, 5)
				.spacing(30, LayoutConstants.getSpacing().y)
				.numColumns(2)
				.applyTo(colorGroup);
		colorGroup.setText(Messages.pref_view_layout_label_color_group);
		{
			final Composite containerDefaultView = new Composite(colorGroup, SWT.NONE);
			{
				// color: tag category
				addField(new ColorFieldEditor(
						ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY,
						Messages.pref_view_layout_label_category,
						containerDefaultView));

				// color: tag
				addField(new ColorFieldEditor(ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE, //
						Messages.pref_view_layout_label_title,
						containerDefaultView));

				// color: sub tag (year)
				addField(new ColorFieldEditor(
						ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB,
						Messages.pref_view_layout_label_sub,
						containerDefaultView));

				// color: sub sub tag (month)
				addField(new ColorFieldEditor(
						ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB,
						Messages.pref_view_layout_label_sub_sub,
						containerDefaultView));
			}

			final Composite containerSegmenter = new Composite(colorGroup, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(containerSegmenter);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerSegmenter);
			{
				// show lines
				final BooleanFieldEditor2 editorLines = new BooleanFieldEditor2(
						ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES,
						Messages.pref_view_layout_display_lines,
						containerSegmenter);
				addField(editorLines);

				final GridLayout gl = (GridLayout) containerSegmenter.getLayout();
				gl.numColumns = 2;

				final Button editorLinesControl = editorLines.getChangeControl(containerSegmenter);
				GridDataFactory.fillDefaults().span(2, 1).indent(0, 10).applyTo(editorLinesControl);
				editorLinesControl.setToolTipText(Messages.pref_view_layout_display_lines_Tooltip);
			}
		}
	}

	private void fireModifyEvent() {

		if (_isOtherModified) {

			_isOtherModified = false;

			UI.setViewColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
		}

	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	public boolean okToLeave() {

		if (_isOtherModified) {

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		_isOtherModified = true;

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

		_isOtherModified = true;

		super.propertyChange(event);
	}

}
