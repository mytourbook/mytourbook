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
import net.tourbook.application.TourbookPlugin;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceView extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		VIEW_TIME_LAYOUT_HH_MM		= "hh_mm";											//$NON-NLS-1$
	public static final String		VIEW_TIME_LAYOUT_HH_MM_SS	= "hh_mm_ss";										//$NON-NLS-1$

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isOtherModified;
	private boolean					_isToolTipModified;

	private SelectionAdapter		_toolTipSelectionAdapter;

	{
		_toolTipSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_isToolTipModified = true;
			}
		};
	}

	/*
	 * UI constrols
	 */
	private Button					_chkTourImportDate;
	private Button					_chkTourImportTime;
	private Button					_chkTourImportTitle;
	private Button					_chkTourImportTags;

	private Button					_chkTourBookDate;
	private Button					_chkTourBookTime;
	private Button					_chkTourBookTitle;
	private Button					_chkTourBookTags;
	private Button					_chkTourBookWeekday;

	private Button					_chkTaggingTag;
	private Button					_chkTaggingTitle;
	private Button					_chkTaggingTags;

	private Button					_chkTourCatalogRefTour;
	private Button					_chkTourCatalogTitle;
	private Button					_chkTourCatalogTags;

	@Override
	protected void createFieldEditors() {

		createUI();

		restoreState();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

			createUI10Colors(parent);
			createUI20TimeFormat(parent);
			createUI30ViewTooltip(parent);
			createUI50Other(parent);
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
//
//			GridLayout gl = (GridLayout) containerDefaultView.getLayout();
//			gl.marginHeight = 5;
//			gl.marginWidth = 5;
			}

			final Composite containerSegmenter = new Composite(colorGroup, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(containerSegmenter);
			{
				// color: up
				addField(new ColorFieldEditor(
						ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP,
						Messages.pref_view_layout_label_segmenter_up,
						containerSegmenter));

				// color: down
				addField(new ColorFieldEditor(
						ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN,
						Messages.pref_view_layout_label_segmenter_down,
						containerSegmenter));
			}
		}
	}

	private void createUI20TimeFormat(final Composite parent) {
		/*
		 * container: column time format
		 */
		final Group formatGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(formatGroup);
		formatGroup.setText(Messages.pref_view_layout_group_display_format);

		/*
		 * recording time format: hh:mm
		 */
		addField(new RadioGroupFieldEditor(
				ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT,
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
		addField(new RadioGroupFieldEditor(
				ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT,
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
	}

	private void createUI30ViewTooltip(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_ViewTooltip_Group);
		GridLayoutFactory.swtDefaults().applyTo(group);

		{
			final Label label = new Label(group, SWT.WRAP);
			label.setText(Messages.PrefPage_ViewTooltip_Label_Info);
			GridDataFactory.fillDefaults().span(6, 1).hint(400, SWT.DEFAULT).applyTo(label);

			final Composite container = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).applyTo(container);
			GridLayoutFactory
					.fillDefaults()
					.spacing(20, LayoutConstants.getSpacing().y)
					.numColumns(6)
					.applyTo(container);
			{
				createUI31ToolTipTourImport(container);
				createUI32ToolTipTourBook(container);
				createUI33ToolTipTagging(container);
				createUI34ToolTipTourCatalog(container);
			}

			createUI40ToolTipActions(group);
		}
	}

	private void createUI31ToolTipTourImport(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_RawData);

		/*
		 * checkbox: date
		 */
		_chkTourImportDate = new Button(container, SWT.CHECK);
		_chkTourImportDate.setText(Messages.PrefPage_ViewTooltip_Label_Date);
		_chkTourImportDate.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: time
		 */
		_chkTourImportTime = new Button(container, SWT.CHECK);
		_chkTourImportTime.setText(Messages.PrefPage_ViewTooltip_Label_Time);
		_chkTourImportTime.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: title
		 */
		_chkTourImportTitle = new Button(container, SWT.CHECK);
		_chkTourImportTitle.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourImportTitle.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourImportTags = new Button(container, SWT.CHECK);
		_chkTourImportTags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourImportTags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI32ToolTipTourBook(final Composite container) {

		final Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_TourBook);

		/*
		 * checkbox: first column (year/month/day)
		 */
		_chkTourBookDate = new Button(container, SWT.CHECK);
		_chkTourBookDate.setText(Messages.PrefPage_ViewTooltip_Label_Day);
		_chkTourBookDate.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: time
		 */
		_chkTourBookTime = new Button(container, SWT.CHECK);
		_chkTourBookTime.setText(Messages.PrefPage_ViewTooltip_Label_Time);
		_chkTourBookTime.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: title
		 */
		_chkTourBookTitle = new Button(container, SWT.CHECK);
		_chkTourBookTitle.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourBookTitle.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourBookTags = new Button(container, SWT.CHECK);
		_chkTourBookTags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourBookTags.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: weekday
		 */
		_chkTourBookWeekday = new Button(container, SWT.CHECK);
		_chkTourBookWeekday.setText(Messages.PrefPage_ViewTooltip_Label_WeekDay);
		_chkTourBookWeekday.addSelectionListener(_toolTipSelectionAdapter);
	}

	private void createUI33ToolTipTagging(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_TaggedTour);

		/*
		 * checkbox: first column (tag)
		 */
		_chkTaggingTag = new Button(container, SWT.CHECK);
		_chkTaggingTag.setText(Messages.PrefPage_ViewTooltip_Label_TagFirstColumn);
		_chkTaggingTag.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);

		/*
		 * checkbox: title
		 */
		_chkTaggingTitle = new Button(container, SWT.CHECK);
		_chkTaggingTitle.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTaggingTitle.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTaggingTags = new Button(container, SWT.CHECK);
		_chkTaggingTags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTaggingTags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI34ToolTipTourCatalog(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_TourCatalog);

		/*
		 * checkbox: first column (reference tour)
		 */
		_chkTourCatalogRefTour = new Button(container, SWT.CHECK);
		_chkTourCatalogRefTour.setText(Messages.PrefPage_ViewTooltip_Label_ReferenceTour);
		_chkTourCatalogRefTour.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);

		/*
		 * checkbox: title
		 */
		_chkTourCatalogTitle = new Button(container, SWT.CHECK);
		_chkTourCatalogTitle.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourCatalogTitle.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourCatalogTags = new Button(container, SWT.CHECK);
		_chkTourCatalogTags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourCatalogTags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI40ToolTipActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(6, 1)
				.indent(0, 5)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * button: Enable all
			 */
			final Button btnEnableAll = new Button(container, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(btnEnableAll);
			setButtonLayoutData(btnEnableAll);
			final GridData gd = (GridData) btnEnableAll.getLayoutData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = SWT.END;

			btnEnableAll.setText(Messages.PrefPage_ViewTooltip_Button_EnableAll);
			btnEnableAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectEnable(true);
				}
			});

			/*
			 * button: disable all
			 */
			final Button btnDisableAll = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(btnDisableAll);
			btnDisableAll.setText(Messages.PrefPage_ViewTooltip_Button_DisableAll);
			btnDisableAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectEnable(false);
				}
			});
			setButtonLayoutData(btnDisableAll);
		}
	}

	private void createUI50Other(final Composite parent) {

		/*
		 * container: other
		 */
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).applyTo(container);

		// show lines
		addField(new BooleanFieldEditor(
				ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES,
				Messages.pref_view_layout_display_lines,
				container));
	}

	private void fireModifyEvent() {

		if (_isOtherModified) {

			_isOtherModified = false;

			UI.setViewColorsFromPrefStore();

			// fire one event for all modified colors
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
		}

		if (_isToolTipModified) {

			_isToolTipModified = false;

			// fire one event for all modified tooltip values
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED, Math.random());
		}
	}

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

	private void onSelectEnable(final boolean isSelected) {

		_chkTourImportDate.setSelection(isSelected);
		_chkTourImportTime.setSelection(isSelected);
		_chkTourImportTitle.setSelection(isSelected);
		_chkTourImportTags.setSelection(isSelected);

		_chkTourBookDate.setSelection(isSelected);
		_chkTourBookTime.setSelection(isSelected);
		_chkTourBookTitle.setSelection(isSelected);
		_chkTourBookTags.setSelection(isSelected);
		_chkTourBookWeekday.setSelection(isSelected);

		_chkTaggingTag.setSelection(isSelected);
		_chkTaggingTitle.setSelection(isSelected);
		_chkTaggingTags.setSelection(isSelected);

		_chkTourCatalogRefTour.setSelection(isSelected);
		_chkTourCatalogTitle.setSelection(isSelected);
		_chkTourCatalogTags.setSelection(isSelected);
	}

	@Override
	protected void performDefaults() {

		_isOtherModified = true;
		_isToolTipModified = true;

		super.performDefaults();

		_chkTourImportDate.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE));
		_chkTourImportTime.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME));
		_chkTourImportTitle.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE));
		_chkTourImportTags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS));

		_chkTourBookDate.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE));
		_chkTourBookTime.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME));
		_chkTourBookTitle.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE));
		_chkTourBookTags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS));
		_chkTourBookWeekday.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY));

		_chkTaggingTag.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG));
		_chkTaggingTitle.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE));
		_chkTaggingTags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS));

		_chkTourCatalogRefTour.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR));
		_chkTourCatalogTitle.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE));
		_chkTourCatalogTags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS));
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE, _chkTourImportDate.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME, _chkTourImportTime.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE, _chkTourImportTitle.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS, _chkTourImportTags.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE, _chkTourBookDate.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME, _chkTourBookTime.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE, _chkTourBookTitle.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS, _chkTourBookTags.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY, _chkTourBookWeekday.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG, _chkTaggingTag.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE, _chkTaggingTitle.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS, _chkTaggingTags.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR,//
					_chkTourCatalogRefTour.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE,//
					_chkTourCatalogTitle.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS, _chkTourCatalogTags.getSelection());

			fireModifyEvent();
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

		_isOtherModified = true;

		super.propertyChange(event);
	}

	private void restoreState() {

		_chkTourImportDate.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE));
		_chkTourImportTime.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME));
		_chkTourImportTitle.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE));
		_chkTourImportTags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS));

		_chkTourBookDate.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE));
		_chkTourBookTime.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME));
		_chkTourBookTitle.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE));
		_chkTourBookTags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS));
		_chkTourBookWeekday.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY));

		_chkTaggingTag.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG));
		_chkTaggingTitle.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE));
		_chkTaggingTags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS));

		_chkTourCatalogRefTour.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR));
		_chkTourCatalogTitle.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE));
		_chkTourCatalogTags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS));
	}

}
