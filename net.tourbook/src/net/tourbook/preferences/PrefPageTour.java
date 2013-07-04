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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

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

public class PrefPageTour extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPageTour";			//$NON-NLS-1$

	private final boolean			_isOSX		= net.tourbook.common.UI.IS_OSX;
	private final boolean			_isLinux	= net.tourbook.common.UI.IS_LINUX;

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

//	private boolean					_isModified			= false;

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

	private Button					_rdoDbSystemEmbedded;
	private Button					_rdoDbSystemServer;

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
			createUI10_TourDB(container);
			createUI20_TourCache(container);
		}

		return container;
	}

	private void createUI10_TourDB(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_TourDb_Group_TourDB);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			_rdoDbSystemEmbedded = new Button(group, SWT.RADIO);
			_rdoDbSystemEmbedded.setText(Messages.Pref_TourDb_Radio_DbSystem_Embedded);
			_rdoDbSystemEmbedded.setToolTipText(Messages.Pref_TourDb_Radio_DbSystem_Embedded_Tooltip);

			_rdoDbSystemServer = new Button(group, SWT.RADIO);
			_rdoDbSystemServer.setText(Messages.Pref_TourDb_Radio_DbSystem_Server);
			_rdoDbSystemServer.setToolTipText(Messages.Pref_TourDb_Radio_DbSystem_Server_Tooltip);
		}
	}

	private void createUI20_TourCache(final Composite parent) {

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
				enableControls();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
			}
		};
	}

	@Override
	protected void performApply() {

		saveState();

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		_spinnerTourCacheSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.TOUR_CACHE_SIZE));

		final boolean isEmbedded = _prefStore.getDefaultBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);
		_rdoDbSystemEmbedded.setSelection(isEmbedded);
		_rdoDbSystemServer.setSelection(!isEmbedded);

		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		final int oldCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
		final boolean oldIsEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

		saveState();

		boolean isRestart = false;

		final int newCacheSize = _prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE);
		final boolean newIsEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);

		if (newCacheSize != oldCacheSize) {

			// tour cache size is modified

			if (MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
					Messages.Pref_Tour_Dialog_TourCacheIsModified_Title,
					Messages.Pref_Tour_Dialog_TourCacheIsModified_Message)) {

				isRestart = true;
			}
		}

		if (isRestart == false && oldIsEmbedded != newIsEmbedded) {

			// db system is modified

			if (MessageDialog.openQuestion(
					Display.getDefault().getActiveShell(),
					Messages.Pref_TourDb_Dialog_TourDbSystemIsModified_Title,
					Messages.Pref_TourDb_Dialog_TourDbSystemIsModified_Message)) {

				isRestart = true;
			}
		}

		if (isRestart) {

			Display.getCurrent().asyncExec(new Runnable() {
				@Override
				public void run() {
					PlatformUI.getWorkbench().restart();
				}
			});
		}

		return true;
	}

	private void restoreState() {

		_spinnerTourCacheSize.setSelection(_prefStore.getInt(ITourbookPreferences.TOUR_CACHE_SIZE));

		// tour db system
		final boolean isEmbedded = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED);
		_rdoDbSystemEmbedded.setSelection(isEmbedded);
		_rdoDbSystemServer.setSelection(!isEmbedded);

	}

	private void saveState() {

		_prefStore.setValue(//
				ITourbookPreferences.TOUR_CACHE_SIZE,
				_spinnerTourCacheSize.getSelection());

		_prefStore.setValue(//
				ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED,
				_rdoDbSystemEmbedded.getSelection());

	}
}
