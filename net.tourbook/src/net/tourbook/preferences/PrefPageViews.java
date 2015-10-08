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
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
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

public class PrefPageViews extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		VIEW_TIME_LAYOUT_HH_MM							= "hh_mm";						//$NON-NLS-1$
	public static final String		VIEW_TIME_LAYOUT_HH_MM_SS						= "hh_mm_ss";					//$NON-NLS-1$

	public static final String		VIEW_DOUBLE_CLICK_ACTION_NONE					= "None";						//$NON-NLS-1$
	public static final String		VIEW_DOUBLE_CLICK_ACTION_NONE_NO_WARNING		= "NoneNoWarning";				//$NON-NLS-1$
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
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_None, VIEW_DOUBLE_CLICK_ACTION_NONE },
			{ Messages.PrefPage_ViewActions_Label_DoubleClick_NoneNoWarning, VIEW_DOUBLE_CLICK_ACTION_NONE_NO_WARNING },
																					//
																					};

	private final IPreferenceStore	_prefStore										= TourbookPlugin
																							.getDefault()
																							.getPreferenceStore();
	private boolean					_isToolTipModified;

	/*
	 * UI constrols
	 */
	private Button					_chkTourImport_Date;
	private Button					_chkTourImport_Time;
	private Button					_chkTourImport_Title;
	private Button					_chkTourImport_Tags;

	private Button					_chkTourBook_Date;
	private Button					_chkTourBook_Time;
	private Button					_chkTourBook_Title;
	private Button					_chkTourBook_Tags;
	private Button					_chkTourBook_Weekday;

	private Button					_chkCollateTour_Date;
	private Button					_chkCollateTour_Time;
	private Button					_chkCollateTour_Title;
	private Button					_chkCollateTour_Tags;
	private Button					_chkCollateTour_Weekday;

	private Button					_chkTagging_Tag;
	private Button					_chkTagging_Title;
	private Button					_chkTagging_Tags;

	private Button					_chkTourCatalog_RefTour;
	private Button					_chkTourCatalog_Title;
	private Button					_chkTourCatalog_Tags;

	/*
	 * none UI controls
	 */
	private SelectionAdapter		_toolTipSelectionAdapter;
	{
		_toolTipSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_isToolTipModified = true;
			}
		};
	}

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

			createUI_10_ViewActions(parent);
			createUI_30_ViewTooltip(parent);
		}
	}

	private void createUI_10_ViewActions(final Composite parent) {

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

			/*
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

	private void createUI_30_ViewTooltip(final Composite parent) {

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
				createUI_31_ToolTip_TourImport(container);
				createUI_33_ToolTip_TourBook(container);
				createUI_35_ToolTip_CollateTour(container);
				createUI_37_ToolTip_Tagging(container);
				createUI_39_ToolTip_TourCatalog(container);
			}

			createUI39ToolTipActions(group);
		}
	}

	private void createUI_31_ToolTip_TourImport(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.grab(true, false)
				.applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_RawData);

		/*
		 * checkbox: date
		 */
		_chkTourImport_Date = new Button(container, SWT.CHECK);
		_chkTourImport_Date.setText(Messages.PrefPage_ViewTooltip_Label_Date);
		_chkTourImport_Date.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: time
		 */
		_chkTourImport_Time = new Button(container, SWT.CHECK);
		_chkTourImport_Time.setText(Messages.PrefPage_ViewTooltip_Label_Time);
		_chkTourImport_Time.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: title
		 */
		_chkTourImport_Title = new Button(container, SWT.CHECK);
		_chkTourImport_Title.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourImport_Title.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourImport_Tags = new Button(container, SWT.CHECK);
		_chkTourImport_Tags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourImport_Tags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI_33_ToolTip_TourBook(final Composite container) {

		final Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_TourBook);

		/*
		 * checkbox: first column (year/month/day)
		 */
		_chkTourBook_Date = new Button(container, SWT.CHECK);
		_chkTourBook_Date.setText(Messages.PrefPage_ViewTooltip_Label_Day);
		_chkTourBook_Date.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: time
		 */
		_chkTourBook_Time = new Button(container, SWT.CHECK);
		_chkTourBook_Time.setText(Messages.PrefPage_ViewTooltip_Label_Time);
		_chkTourBook_Time.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: title
		 */
		_chkTourBook_Title = new Button(container, SWT.CHECK);
		_chkTourBook_Title.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourBook_Title.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourBook_Tags = new Button(container, SWT.CHECK);
		_chkTourBook_Tags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourBook_Tags.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: weekday
		 */
		_chkTourBook_Weekday = new Button(container, SWT.CHECK);
		_chkTourBook_Weekday.setText(Messages.PrefPage_ViewTooltip_Label_WeekDay);
		_chkTourBook_Weekday.addSelectionListener(_toolTipSelectionAdapter);
	}

	private void createUI_35_ToolTip_CollateTour(final Composite container) {

		final Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_CollatedTours);

		/*
		 * checkbox: first column (year/month/day)
		 */
		_chkCollateTour_Date = new Button(container, SWT.CHECK);
		_chkCollateTour_Date.setText(Messages.PrefPage_ViewTooltip_Chkbox_Collation);
		_chkCollateTour_Date.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: time
		 */
		_chkCollateTour_Time = new Button(container, SWT.CHECK);
		_chkCollateTour_Time.setText(Messages.PrefPage_ViewTooltip_Label_Time);
		_chkCollateTour_Time.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: title
		 */
		_chkCollateTour_Title = new Button(container, SWT.CHECK);
		_chkCollateTour_Title.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkCollateTour_Title.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkCollateTour_Tags = new Button(container, SWT.CHECK);
		_chkCollateTour_Tags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkCollateTour_Tags.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: weekday
		 */
		_chkCollateTour_Weekday = new Button(container, SWT.CHECK);
		_chkCollateTour_Weekday.setText(Messages.PrefPage_ViewTooltip_Label_WeekDay);
		_chkCollateTour_Weekday.addSelectionListener(_toolTipSelectionAdapter);
	}

	private void createUI_37_ToolTip_Tagging(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_TaggedTour);

		/*
		 * checkbox: first column (tag)
		 */
		_chkTagging_Tag = new Button(container, SWT.CHECK);
		_chkTagging_Tag.setText(Messages.PrefPage_ViewTooltip_Label_TagFirstColumn);
		_chkTagging_Tag.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);

		/*
		 * checkbox: title
		 */
		_chkTagging_Title = new Button(container, SWT.CHECK);
		_chkTagging_Title.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTagging_Title.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTagging_Tags = new Button(container, SWT.CHECK);
		_chkTagging_Tags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTagging_Tags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI_39_ToolTip_TourCatalog(final Composite container) {

		Label label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText(Messages.PrefPage_ViewTooltip_Label_ReferenceTours);

		/*
		 * checkbox: first column (reference tour)
		 */
		_chkTourCatalog_RefTour = new Button(container, SWT.CHECK);
		_chkTourCatalog_RefTour.setText(Messages.PrefPage_ViewTooltip_Label_ReferenceTour);
		_chkTourCatalog_RefTour.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);

		/*
		 * checkbox: title
		 */
		_chkTourCatalog_Title = new Button(container, SWT.CHECK);
		_chkTourCatalog_Title.setText(Messages.PrefPage_ViewTooltip_Label_Title);
		_chkTourCatalog_Title.addSelectionListener(_toolTipSelectionAdapter);

		/*
		 * checkbox: tags
		 */
		_chkTourCatalog_Tags = new Button(container, SWT.CHECK);
		_chkTourCatalog_Tags.setText(Messages.PrefPage_ViewTooltip_Label_Tags);
		_chkTourCatalog_Tags.addSelectionListener(_toolTipSelectionAdapter);

		// spacer
		label = new Label(container, SWT.NONE);
		GridDataFactory.fillDefaults().span(1, 1).applyTo(label);
	}

	private void createUI39ToolTipActions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.span(6, 1)
				.indent(0, 10)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * button: Enable all
			 */
			final Button btnEnableAll = new Button(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(btnEnableAll);
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
		}
	}

	private void fireModifyEvent() {

		if (_isToolTipModified) {

			_isToolTipModified = false;

			// fire one event for all modified tooltip values
			getPreferenceStore().setValue(ITourbookPreferences.VIEW_TOOLTIP_IS_MODIFIED, Math.random());
		}
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void onSelectEnable(final boolean isSelected) {

		_chkTourImport_Date.setSelection(isSelected);
		_chkTourImport_Time.setSelection(isSelected);
		_chkTourImport_Title.setSelection(isSelected);
		_chkTourImport_Tags.setSelection(isSelected);

		_chkTourBook_Date.setSelection(isSelected);
		_chkTourBook_Time.setSelection(isSelected);
		_chkTourBook_Title.setSelection(isSelected);
		_chkTourBook_Tags.setSelection(isSelected);
		_chkTourBook_Weekday.setSelection(isSelected);

		_chkCollateTour_Date.setSelection(isSelected);
		_chkCollateTour_Time.setSelection(isSelected);
		_chkCollateTour_Title.setSelection(isSelected);
		_chkCollateTour_Tags.setSelection(isSelected);
		_chkCollateTour_Weekday.setSelection(isSelected);

		_chkTagging_Tag.setSelection(isSelected);
		_chkTagging_Title.setSelection(isSelected);
		_chkTagging_Tags.setSelection(isSelected);

		_chkTourCatalog_RefTour.setSelection(isSelected);
		_chkTourCatalog_Title.setSelection(isSelected);
		_chkTourCatalog_Tags.setSelection(isSelected);
	}

	@Override
	protected void performDefaults() {

		_isToolTipModified = true;

		super.performDefaults();

		_chkTourImport_Date.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE));
		_chkTourImport_Time.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME));
		_chkTourImport_Title.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE));
		_chkTourImport_Tags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS));

		_chkTourBook_Date.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE));
		_chkTourBook_Time.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME));
		_chkTourBook_Title.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE));
		_chkTourBook_Tags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS));
		_chkTourBook_Weekday.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY));

		_chkCollateTour_Date.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_COLLATION));
		_chkCollateTour_Time.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TIME));
		_chkCollateTour_Title.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TITLE));
		_chkCollateTour_Tags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TAGS));
		_chkCollateTour_Weekday.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_WEEKDAY));

		_chkTagging_Tag.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG));
		_chkTagging_Title.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE));
		_chkTagging_Tags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS));

		_chkTourCatalog_RefTour.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR));
		_chkTourCatalog_Title.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE));
		_chkTourCatalog_Tags.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS));
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();

		if (isOK) {

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE,//
					_chkTourImport_Date.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME,//
					_chkTourImport_Time.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE, //
					_chkTourImport_Title.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS, //
					_chkTourImport_Tags.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE, //
					_chkTourBook_Date.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME, //
					_chkTourBook_Time.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE, //
					_chkTourBook_Title.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS, //
					_chkTourBook_Tags.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY, //
					_chkTourBook_Weekday.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_COLLATION, //
					_chkCollateTour_Date.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TIME, //
					_chkCollateTour_Time.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TITLE, //
					_chkCollateTour_Title.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TAGS, //
					_chkCollateTour_Tags.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_WEEKDAY,//
					_chkCollateTour_Weekday.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG, //
					_chkTagging_Tag.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE, //
					_chkTagging_Title.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS, //
					_chkTagging_Tags.getSelection());

			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR,//
					_chkTourCatalog_RefTour.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE,//
					_chkTourCatalog_Title.getSelection());
			_prefStore.setValue(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS, //
					_chkTourCatalog_Tags.getSelection());

			fireModifyEvent();
		}

		return isOK;
	}

	private void restoreState() {

		_chkTourImport_Date.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE));
		_chkTourImport_Time.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME));
		_chkTourImport_Title.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE));
		_chkTourImport_Tags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS));

		_chkTourBook_Date.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE));
		_chkTourBook_Time.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME));
		_chkTourBook_Title.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE));
		_chkTourBook_Tags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS));
		_chkTourBook_Weekday.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY));

		_chkCollateTour_Date.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_COLLATION));
		_chkCollateTour_Time.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TIME));
		_chkCollateTour_Title.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TITLE));
		_chkCollateTour_Tags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TAGS));
		_chkCollateTour_Weekday.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_WEEKDAY));

		_chkTagging_Tag.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG));
		_chkTagging_Title.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE));
		_chkTagging_Tags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS));

		_chkTourCatalog_RefTour.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR));
		_chkTourCatalog_Title.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE));
		_chkTourCatalog_Tags.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS));
	}

}
