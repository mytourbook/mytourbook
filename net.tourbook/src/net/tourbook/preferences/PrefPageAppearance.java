/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageAppearance extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPageAppearance";	//$NON-NLS-1$

	private final boolean			_isOSX		= net.tourbook.util.UI.IS_OSX;
	private final boolean			_isLinux	= net.tourbook.util.UI.IS_LINUX;

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isModified	= false;

	/*
	 * UI tools
	 */
	private int						_hintDefaultSpinnerWidth;
	private PixelConverter			_pc;
	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;

	/*
	 * UI controls
	 */
	private Spinner					_spinnerRecentTourTypes;
	private Spinner					_spinnerRecentTags;
	private Button					_chkMemMonitor;
	private Button					_btnResetAllToggleDialogs;

	private Button					_chkAutoOpenTagging;
	private Spinner					_spinnerAutoOpenDelay;
	private Label					_lblAutoTagDelay;
	private Label					_lblAutoOpenMS;
	private Button					_chkTaggingAnimation;

	public PrefPageAppearance() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUITools(parent);

		final Composite container = createUI(parent);

		restoreState();
		enableControls();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
		{

			createUI10Tagging(container);
			createUI20RecentEntries(container);
			createUI30MemoryMonitor(container);
			createUI40ResetAllToggleDialogs(container);
		}

		return container;
	}

	private void createUI10Tagging(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_Appearance_Group_Tagging);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * number of recent tags
			 */
			final Label label = new Label(group, NONE);
			label.setText(Messages.pref_appearance_number_of_recent_tags);
			label.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);

			// spinner
			_spinnerRecentTags = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinnerRecentTags);
			_spinnerRecentTags.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
			_spinnerRecentTags.setMinimum(0);
			_spinnerRecentTags.setMaximum(9);
			_spinnerRecentTags.addSelectionListener(_defaultSelectionAdapter);
			_spinnerRecentTags.addMouseWheelListener(_defaultMouseWheelListener);

			/*
			 * autoopen tagging
			 */
//				eclipse 3.7 supports this feature
//				if (_isOSX) {
//					// label: OSX is not supported, feature is not working
//					final Label label = new Label(container, SWT.WRAP);
//					GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
//					label.setText(Messages.Pref_Appearance_Label_NoOSXSupport);
//				}
			_chkAutoOpenTagging = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkAutoOpenTagging);
			_chkAutoOpenTagging.setText(Messages.Pref_Appearance_Check_AutoOpenTagging);
			_chkAutoOpenTagging.addSelectionListener(_defaultSelectionAdapter);
			_chkAutoOpenTagging.setToolTipText(Messages.Pref_Appearance_Label_AutoOpenTagging_Tooltip);

			final Composite autoTagContainer = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, false).indent(16, 0).span(2, 1).applyTo(autoTagContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(autoTagContainer);
			{

				// label: delay
				_lblAutoTagDelay = new Label(autoTagContainer, SWT.NONE);
				_lblAutoTagDelay.setText(Messages.Pref_Appearance_Label_AutoOpenTaggingDelay);
				_lblAutoTagDelay.setToolTipText(Messages.Pref_Appearance_Label_AutoOpenTagging_Tooltip);

				// spinner
				_spinnerAutoOpenDelay = new Spinner(autoTagContainer, SWT.BORDER);
				GridDataFactory.fillDefaults()//
						.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_spinnerAutoOpenDelay);
				_spinnerAutoOpenDelay.setMinimum(0);
				_spinnerAutoOpenDelay.setMaximum(3000);
				_spinnerAutoOpenDelay.addSelectionListener(_defaultSelectionAdapter);
				_spinnerAutoOpenDelay.addMouseWheelListener(_defaultMouseWheelListener);

				// label: ms
				_lblAutoOpenMS = new Label(autoTagContainer, SWT.NONE);
				_lblAutoOpenMS.setText(UI.UNIT_LABEL_MS);

				// check: show animation
				_chkTaggingAnimation = new Button(autoTagContainer, SWT.CHECK);
				GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkTaggingAnimation);
				_chkTaggingAnimation.setText(Messages.Pref_Appearance_Check_TaggingAnimation);
				_chkTaggingAnimation.addSelectionListener(_defaultSelectionAdapter);
			}
		}
	}

	private void createUI20RecentEntries(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			/*
			 * number of recent tour types
			 */
			final Label label = new Label(container, NONE);
			label.setText(Messages.Pref_Appearance_NumberOfRecent_TourTypes);
			label.setToolTipText(Messages.Pref_Appearance_NumberOfRecent_TourTypes_Tooltip);

			// spinner
			_spinnerRecentTourTypes = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinnerRecentTourTypes);
			_spinnerRecentTourTypes.setToolTipText(Messages.Pref_Appearance_NumberOfRecent_TourTypes_Tooltip);
			_spinnerRecentTourTypes.setMinimum(0);
			_spinnerRecentTourTypes.setMaximum(9);
			_spinnerRecentTourTypes.addSelectionListener(_defaultSelectionAdapter);
			_spinnerRecentTourTypes.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	/**
	 * memory monitor
	 */
	private void createUI30MemoryMonitor(final Composite parent) {

		_chkMemMonitor = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkMemMonitor);
		_chkMemMonitor.setText(Messages.pref_appearance_showMemoryMonitor);
	}

	private void createUI40ResetAllToggleDialogs(final Composite parent) {

		_btnResetAllToggleDialogs = new Button(parent, SWT.PUSH);
		GridDataFactory.fillDefaults()//
				.indent(0, 10)
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_btnResetAllToggleDialogs);
		_btnResetAllToggleDialogs.setText(Messages.Pref_Appearance_Button_ResetAllToggleDialogs);
		_btnResetAllToggleDialogs.setToolTipText(Messages.Pref_Appearance_Button_ResetAllToggleDialogs_Tooltip);
		_btnResetAllToggleDialogs.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onResetAllToggleDialogs();
			}
		});
	}

	private void enableControls() {

		final boolean isTagAutoOpen = _chkAutoOpenTagging.getSelection();
		final boolean isEnabled = true; // eclipse 3.7 supports this feature in OSX

		_chkAutoOpenTagging.setEnabled(isEnabled);
		_lblAutoOpenMS.setEnabled(isEnabled && isTagAutoOpen);
		_lblAutoTagDelay.setEnabled(isEnabled && isTagAutoOpen);
		_spinnerAutoOpenDelay.setEnabled(isEnabled && isTagAutoOpen);
		_chkTaggingAnimation.setEnabled(isEnabled && isTagAutoOpen);
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUITools(final Composite parent) {

		_pc = new PixelConverter(parent);
		_hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
				enableControls();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeProperty();
			}
		};
	}

	/**
	 * Property was changed, fire a property change event
	 */
	private void onChangeProperty() {
		_isModified = true;
	}

	private void onResetAllToggleDialogs() {

		_prefStore.setValue(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR, false);
		_prefStore.setValue(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING, false);

		_prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_REIMPORT_TOUR, false);
		_prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_REIMPORT_ALL_TIME_SLICES, false);
		_prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_REIMPORT_ALTITUDE_VALUES, false);

		MessageDialog.openInformation(
				getShell(),
				Messages.Pref_Appearance_Dialog_ResetAllToggleDialogs_Title,
				Messages.Pref_Appearance_Dialog_ResetAllToggleDialogs_Message);
	}

	@Override
	protected void performApply() {

		saveState();

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

		_spinnerRecentTags.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS));

		_spinnerRecentTourTypes.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES));

		_chkAutoOpenTagging.setSelection(_prefStore
				.getDefaultBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN));
		_chkTaggingAnimation.setSelection(_prefStore
				.getDefaultBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION));
		_spinnerAutoOpenDelay.setSelection(_prefStore
				.getDefaultInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));

		_chkMemMonitor.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR));

		super.performDefaults();

		// this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();

		enableControls();
	}

	@Override
	public boolean performOk() {

		saveState();

		final boolean isShowMemoryOld = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR);

		final boolean isOK = super.performOk();

		final boolean isShowMemoryNew = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR);

		if (isOK && _isModified) {
			_isModified = false;
		}

		if (isShowMemoryNew != isShowMemoryOld) {
			if (MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
					Messages.pref_appearance_showMemoryMonitor_title,
					Messages.pref_appearance_showMemoryMonitor_message)) {

				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						PlatformUI.getWorkbench().restart();
					}
				});
			}
		}

		return isOK;
	}

	private void restoreState() {

		_spinnerRecentTags.setSelection(//
				_prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS));

		_spinnerRecentTourTypes.setSelection(//
				_prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES));

		_chkAutoOpenTagging.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN));
		_chkTaggingAnimation.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION));
		_spinnerAutoOpenDelay.setSelection(_prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));

		_chkMemMonitor.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR));
	}

	private void saveState() {

		_prefStore.setValue(//
				ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS,
				_spinnerRecentTags.getSelection());

		_prefStore.setValue(
				ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES,
				_spinnerRecentTourTypes.getSelection());

		_prefStore.setValue(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN, _chkAutoOpenTagging.getSelection());
		_prefStore.setValue(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION, _chkTaggingAnimation.getSelection());
		_prefStore.setValue(
				ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY,
				_spinnerAutoOpenDelay.getSelection());

		_prefStore.setValue(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR, _chkMemMonitor.getSelection());
	}
}
