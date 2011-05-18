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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageTour extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPageTour";			//$NON-NLS-1$

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
	private Spinner					_spinnerTourCacheSize;

	public PrefPageTour() {
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
		}

		return container;
	}

	private void createUI10Tagging(final Composite parent) {

		final int verticalIndent = 20;

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_Tour_Group_TourCache);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * label: info
			 */
			Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.hint(350, SWT.DEFAULT)
					.grab(true, false)
					.span(2, 1)
					.applyTo(label);
			label.setText(Messages.Pref_Tour_Label_TourCacheSize_Info);

			/*
			 * label: cache size
			 */
			label = new Label(group, NONE);
			GridDataFactory.fillDefaults()//
					.indent(0, verticalIndent)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.Pref_Tour_Label_TourCacheSize);

			// spinner: cache size
			_spinnerTourCacheSize = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.indent(0, verticalIndent)
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_spinnerTourCacheSize);
			_spinnerTourCacheSize.setMinimum(0);
			_spinnerTourCacheSize.setMaximum(100000);
			_spinnerTourCacheSize.addSelectionListener(_defaultSelectionAdapter);
			_spinnerTourCacheSize.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private void enableControls() {

	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUITools(final Composite parent) {

		_pc = new PixelConverter(parent);
		_hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 14 : 7);

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

	@Override
	protected void performApply() {

		saveState();

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		_isModified = true;

		_spinnerTourCacheSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.TOUR_CACHE_SIZE));

		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		final int oldCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);

		saveState();

		final boolean isOK = super.performOk();

		final int newCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);

		if (isOK && _isModified) {
			_isModified = false;
		}

		if (newCacheSize != oldCacheSize) {
			if (MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
					Messages.Pref_Tour_Dialog_TourCacheIsModified_Title,
					Messages.Pref_Tour_Dialog_TourCacheIsModified_Message)) {

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

		_spinnerTourCacheSize.setSelection(//
				_prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE));

	}

	private void saveState() {

		_prefStore.setValue(//
				ITourbookPreferences.TOUR_CACHE_SIZE,
				_spinnerTourCacheSize.getSelection());

	}
}
