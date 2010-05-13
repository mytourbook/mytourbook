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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageAppearance extends PreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isModified	= false;

	/*
	 * UI tools
	 */
	private int						_defaultSpinnerWidth;
	private PixelConverter			_pc;
	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;

	/*
	 * UI controls
	 */
	private Spinner					_spinnerRecentTourTypes;
	private Spinner					_spinnerRecentTags;
	private Button					_btnMemMonitor;

	public PrefPageAppearance() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUITools(parent);

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		Label label;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * number of recent tour types
			 */
			label = new Label(container, NONE);
			label.setText(Messages.Pref_Appearance_NumberOfRecent_TourTypes);

			// spinner
			_spinnerRecentTourTypes = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinnerRecentTourTypes);
			_spinnerRecentTourTypes.setToolTipText(Messages.Pref_Appearance_NumberOfRecent_TourTypes_Tooltip);
			_spinnerRecentTourTypes.setMinimum(0);
			_spinnerRecentTourTypes.setMaximum(9);
			_spinnerRecentTourTypes.addSelectionListener(_defaultSelectionAdapter);
			_spinnerRecentTourTypes.addMouseWheelListener(_defaultMouseWheelListener);

			/*
			 * number of recent tags
			 */
			label = new Label(container, NONE);
			label.setText(Messages.pref_appearance_number_of_recent_tags);

			// spinner
			_spinnerRecentTags = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.hint(_defaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinnerRecentTags);
			_spinnerRecentTags.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
			_spinnerRecentTags.setMinimum(0);
			_spinnerRecentTags.setMaximum(9);
			_spinnerRecentTags.addSelectionListener(_defaultSelectionAdapter);
			_spinnerRecentTags.addMouseWheelListener(_defaultMouseWheelListener);

			/*
			 * memory monitor
			 */
			_btnMemMonitor = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_btnMemMonitor);
			_btnMemMonitor.setText(Messages.pref_appearance_showMemoryMonitor);
		}
		return container;
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUITools(final Composite parent) {
		_pc = new PixelConverter(parent);
		_defaultSpinnerWidth = _pc.convertWidthInCharsToPixels(5);

		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeProperty();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
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

		_btnMemMonitor.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR));

		super.performDefaults();

		// this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();
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

		_btnMemMonitor.setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR));
	}

	private void saveState() {

		_prefStore.setValue(//
				ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS,
				_spinnerRecentTags.getSelection());

		_prefStore.setValue(
				ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES,
				_spinnerRecentTourTypes.getSelection());

		_prefStore.setValue(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR, _btnMemMonitor.getSelection());
	}
}
