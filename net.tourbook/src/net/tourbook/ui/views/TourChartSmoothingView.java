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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

public class TourChartSmoothingView extends ViewPart {

	private static final int		MAX_TAU			= 5000;

	public static final String		ID				= "net.tourbook.ui.views.TourChartSmoothingView";		//$NON-NLS-1$

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getDefault()//
															.getPreferenceStore();

	private final IDialogSettings	_state			= TourbookPlugin.getDefault()//
															.getDialogSettingsSection(ID);

	private SelectionAdapter		_defaultSelectionListener;
	private ModifyListener			_defaultModifyListener;
	private MouseWheelListener		_defaultSpinnerMouseWheelListener;

	private boolean					_isUpdateUI;

	/*
	 * UI controls/resources
	 */
	private FormToolkit				_tk;

	private Button					_chkIsSpeedSmoothing;
	private Button					_chkIsAltitudeSmoothing;
	private Button					_chkIsPulseSmoothing;
	private Spinner					_spinnerSpeedTau;
	private Spinner					_spinnerAltitudeTau;
	private Spinner					_spinnerPulseTau;

	private Button					_btnDefault;

	private Image					_imageSpeed		= TourbookPlugin.getImageDescriptor(//
															Messages.Image__graph_speed).createImage();
	private Image					_imageAltitude	= TourbookPlugin.getImageDescriptor(//
															Messages.Image__graph_altitude).createImage();
	private Image					_imagePulse		= TourbookPlugin.getImageDescriptor(//
															Messages.Image__graph_heartbeat).createImage();

	public TourChartSmoothingView() {}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);

		restoreStore();
		enableControls();
	}

	private void createUI(final Composite parent) {

		initUI(parent);

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		{
			createUI10SmoothAltitude(container);
			createUI20SmoothPulse(container);
			createUI30SmoothSpeed(container);
//			createUI40Actions(container);
		}
	}

	private void createUI10SmoothAltitude(final Composite parent) {

		/*
		 * image: altitude
		 */
		final CLabel iconSpeed = new CLabel(parent, SWT.NONE);
		iconSpeed.setBackground(_tk.getColors().getBackground());
		iconSpeed.setImage(_imageAltitude);

		/*
		 * checkbox: smooth altitude
		 */
		_chkIsAltitudeSmoothing = _tk.createButton(
				parent,
				Messages.TourChart_Smoothing_Checkbox_IsAltitudeSmoothing,
				SWT.CHECK);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_chkIsAltitudeSmoothing);
		_chkIsAltitudeSmoothing.addSelectionListener(_defaultSelectionListener);

		/*
		 * spinner: tau
		 */
		_spinnerAltitudeTau = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerAltitudeTau);
		_spinnerAltitudeTau.setDigits(1);
		_spinnerAltitudeTau.setMinimum(1);
		_spinnerAltitudeTau.setMaximum(MAX_TAU);
		_spinnerAltitudeTau.addSelectionListener(_defaultSelectionListener);
		_spinnerAltitudeTau.addMouseWheelListener(_defaultSpinnerMouseWheelListener);
		_spinnerAltitudeTau.addModifyListener(_defaultModifyListener);
	}

	private void createUI20SmoothPulse(final Composite parent) {

		/*
		 * image: pulse
		 */
		final CLabel iconSpeed = new CLabel(parent, SWT.NONE);
		iconSpeed.setBackground(_tk.getColors().getBackground());
		iconSpeed.setImage(_imagePulse);

		/*
		 * checkbox: smooth speed
		 */
		_chkIsPulseSmoothing = _tk.createButton(
				parent,
				Messages.TourChart_Smoothing_Checkbox_IsPulseSmoothing,
				SWT.CHECK);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_chkIsPulseSmoothing);
		_chkIsPulseSmoothing.addSelectionListener(_defaultSelectionListener);

		/*
		 * spinner: tau
		 */
		_spinnerPulseTau = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerPulseTau);
		_spinnerPulseTau.setDigits(1);
		_spinnerPulseTau.setMinimum(1);
		_spinnerPulseTau.setMaximum(MAX_TAU);
		_spinnerPulseTau.addSelectionListener(_defaultSelectionListener);
		_spinnerPulseTau.addMouseWheelListener(_defaultSpinnerMouseWheelListener);
		_spinnerPulseTau.addModifyListener(_defaultModifyListener);
	}

	private void createUI30SmoothSpeed(final Composite parent) {

		/*
		 * image: speed
		 */
		final CLabel iconSpeed = new CLabel(parent, SWT.NONE);
		iconSpeed.setBackground(_tk.getColors().getBackground());
		iconSpeed.setImage(_imageSpeed);

		/*
		 * checkbox: smooth speed
		 */
		_chkIsSpeedSmoothing = _tk.createButton(
				parent,
				Messages.TourChart_Smoothing_Checkbox_IsSpeedSmoothing,
				SWT.CHECK);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_chkIsSpeedSmoothing);
		_chkIsSpeedSmoothing.addSelectionListener(_defaultSelectionListener);

		/*
		 * spinner: tau
		 */
		_spinnerSpeedTau = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerSpeedTau);
		_spinnerSpeedTau.setDigits(1);
		_spinnerSpeedTau.setMinimum(1);
		_spinnerSpeedTau.setMaximum(MAX_TAU);
		_spinnerSpeedTau.addSelectionListener(_defaultSelectionListener);
		_spinnerSpeedTau.addMouseWheelListener(_defaultSpinnerMouseWheelListener);
		_spinnerSpeedTau.addModifyListener(_defaultModifyListener);
	}

	private void createUI40Actions(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults()//
//				.grab(false, true)
				.indent(0, 10)
				.span(3, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * button: default
			 */
			_btnDefault = new Button(container, SWT.NONE);
			_btnDefault.setText(UI.SPACE2 + Messages.TourChart_Smoothing_Button_Default + UI.SPACE2);
			_btnDefault.setToolTipText(Messages.TourChart_Smoothing_Button_Default_Tooltip);
			_btnDefault.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onActionRestoreSettings();
				}
			});
//
//			/*
//			 * button: update
//			 */
//			_btnSaveSettings = new Button(container, SWT.NONE);
//			_btnSaveSettings.setText(Messages.App_Action_Save);
//			_btnSaveSettings.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onActionSaveSettings();
//				}
//			});
		}
	}

	@Override
	public void dispose() {

		Util.disposeResource(_imageSpeed);
		Util.disposeResource(_imageAltitude);
		Util.disposeResource(_imagePulse);

		super.dispose();
	}

	private void enableControls() {

		final boolean isSpeed = _chkIsSpeedSmoothing.getSelection();

		_spinnerSpeedTau.setEnabled(isSpeed);
		_spinnerAltitudeTau.setEnabled(isSpeed);
		_spinnerPulseTau.setEnabled(false);

		_chkIsAltitudeSmoothing.setEnabled(false);
		_chkIsPulseSmoothing.setEnabled(false);
	}

	private void initUI(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		_defaultModifyListener = new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifySmoothing();
			}
		};

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifySmoothing();
			}
		};

		_defaultSpinnerMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				if (_isUpdateUI) {
					return;
				}
				onModifySmoothing();
			}
		};
	}

	private void onActionRestoreSettings() {
		// TODO Auto-generated method stub

	}

	private void onModifySmoothing() {

		enableControls();

		saveStore();

		TourManager.getInstance().removeAllToursFromCache();
//		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourChartPropertyView.this);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void restoreStore() {

		// altitude
		_chkIsAltitudeSmoothing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_SMOOTHING_IS_ALTITUDE));
		_spinnerAltitudeTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_ALTITUDE_TAU) * 10));

		// pulse
		_chkIsPulseSmoothing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_SMOOTHING_IS_PULSE));
		_spinnerPulseTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_PULSE_TAU) * 10));

		// speed
		_chkIsSpeedSmoothing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_SMOOTHING_IS_SPEED));
		_spinnerSpeedTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_SPEED_TAU) * 10));
	}

	/**
	 * Update new values in the pref store
	 */
	private void saveStore() {

		// altitude
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_IS_ALTITUDE, _chkIsAltitudeSmoothing.getSelection());
		_prefStore.setValue(
				ITourbookPreferences.GRAPH_SMOOTHING_ALTITUDE_TAU,
				_spinnerAltitudeTau.getSelection() / 10.0);

		// pulse
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_IS_PULSE, _chkIsPulseSmoothing.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_PULSE_TAU, _spinnerPulseTau.getSelection() / 10.0);

		// speed smoothing
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_IS_SPEED, _chkIsSpeedSmoothing.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_SPEED_TAU, _spinnerSpeedTau.getSelection() / 10.0);
	}

	@Override
	public void setFocus() {

	}

}
