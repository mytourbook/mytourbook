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
import net.tourbook.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;

public class TourChartSmoothingView extends ViewPart {

	private static final int		MAX_TAU						= 5000;

	public static final String		ID							= "net.tourbook.ui.views.TourChartSmoothingView";		//$NON-NLS-1$

	private final IPreferenceStore	_prefStore					= TourbookPlugin.getDefault()//
																		.getPreferenceStore();

	private final boolean			_isOSX						= net.tourbook.util.UI.IS_OSX;
	private final boolean			_isLinux					= net.tourbook.util.UI.IS_LINUX;

	private PixelConverter			_pc;
	private int						_hintDefaultSpinnerWidth;

	private SelectionAdapter		_defaultSelectionListener;
	private MouseWheelListener		_defaultSpinnerMouseWheelListener;

	private boolean					_isUpdateUI;

	/*
	 * UI resources
	 */
	private Image					_imageAltitude				= TourbookPlugin.getImageDescriptor(
																		Messages.Image__graph_altitude).createImage();
	private Image					_imageAltitudeDisabled		= TourbookPlugin
																		.getImageDescriptor(
																				Messages.Image__graph_altitude_disabled)
																		.createImage();
	private Image					_imageGradient				= TourbookPlugin.getImageDescriptor(
																		Messages.Image__graph_gradient).createImage();
	private Image					_imageGradientDisabled		= TourbookPlugin
																		.getImageDescriptor(
																				Messages.Image__graph_gradient_disabled)
																		.createImage();
	private Image					_imagePulse					= TourbookPlugin.getImageDescriptor(
																		Messages.Image__graph_heartbeat).createImage();
	private Image					_imagePulseDisabled			= TourbookPlugin
																		.getImageDescriptor(
																				Messages.Image__graph_heartbeat_disabled)
																		.createImage();
	private Image					_imageSpeed					= TourbookPlugin.getImageDescriptor(
																		Messages.Image__graph_speed).createImage();
	private Image					_imageSpeedDisabled			= TourbookPlugin
																		.getImageDescriptor(
																				Messages.Image__graph_speed_disabled)
																		.createImage();

	/*
	 * UI controls
	 */
	private FormToolkit				_tk;

	private Combo					_comboAlgorithm;
	private Button					_chkIsSynchSmoothing;
	private Button					_chkIsAltitudeSmoothing;
	private Button					_chkIsPulseSmoothing;
	private Spinner					_spinnerGradientTau;
	private Spinner					_spinnerPulseTau;
	private Spinner					_spinnerSpeedTau;
	private Spinner					_spinnerRepeatedSmoothing;
	private Spinner					_spinnerRepeatedTau;
	private Spinner					_spinnerLastUsed;

	private Label					_lblSpeedSmoothing;
	private Label					_lblGradientSmoothing;
	private Label					_lblRepeatedSmoothing;
	private Label					_lblRepeatedTau;

	private CLabel					_iconSpeed;
	private CLabel					_iconPulse;
	private CLabel					_iconGradient;
	private CLabel					_iconAltitude;

	/*
	 * non UI
	 */
	public static final String		SMOOTHING_ALGORITHM_JAMET	= "jamet";												//$NON-NLS-1$
	public static final String		SMOOTHING_ALGORITHM_INITIAL	= "initial";											//$NON-NLS-1$

	private static String[][]		SMOOTHING_ALGORITHM			= {
			{ SMOOTHING_ALGORITHM_INITIAL, Messages.TourChart_Smoothing_Algorithm_Initial },
			{ SMOOTHING_ALGORITHM_JAMET, Messages.TourChart_Smoothing_Algorithm_Jamet },
																//
																};

	public TourChartSmoothingView() {}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		updateUI();

		restoreStore();
		enableControls();
	}

	private void createUI(final Composite parent) {

		initUI(parent);

		final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
		{
			final Composite container = _tk.createComposite(scrolledContainer);
			GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
			GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			{
				createUI10SmoothingAlgorithm(container);
				createUI20SmoothSpeed(container);
				createUI30SmoothGradient(container);
				createUI40SmoothAltitude(container);
				createUI50SmoothPulse(container);
			}

			// setup scrolled container
			scrolledContainer.setExpandVertical(true);
			scrolledContainer.setExpandHorizontal(true);
			scrolledContainer.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(final ControlEvent e) {
					scrolledContainer.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
			});

			scrolledContainer.setContent(container);
		}
	}

	private void createUI10SmoothingAlgorithm(final Composite parent) {

		final Composite container = _tk.createComposite(parent);
		GridDataFactory.fillDefaults().span(3, 1).grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 10).applyTo(container);
		{
			/*
			 * label: smoothing algorithm
			 */
			final Label label = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_SmoothingAlgorithm);
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);

			/*
			 * combo: smoothing algorithm
			 */
			_comboAlgorithm = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_comboAlgorithm);
			_tk.adapt(_comboAlgorithm, true, true);
			_comboAlgorithm.setVisibleItemCount(10);
			_comboAlgorithm.addSelectionListener(_defaultSelectionListener);

			/*
			 * label: repeated smoothing
			 */
			_lblRepeatedSmoothing = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_RepeatedSmoothing);
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblRepeatedSmoothing);
			_lblRepeatedSmoothing.setToolTipText(Messages.TourChart_Smoothing_Label_RepeatedSmoothing_Tooltip);

			/*
			 * spinner: repeated smoothing
			 */
			_spinnerRepeatedSmoothing = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerRepeatedSmoothing);
			_spinnerRepeatedSmoothing.setMinimum(0);
			_spinnerRepeatedSmoothing.setMaximum(5);
			_spinnerRepeatedSmoothing.addSelectionListener(_defaultSelectionListener);
			_spinnerRepeatedSmoothing.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * label: repeated tau
			 */
			_lblRepeatedTau = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_RepeatedTau);
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblRepeatedTau);
			_lblRepeatedTau.setToolTipText(Messages.TourChart_Smoothing_Label_RepeatedTau_Tooltip);

			/*
			 * spinner: repeated tau
			 */
			_spinnerRepeatedTau = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
					.applyTo(_spinnerRepeatedTau);
			_spinnerRepeatedTau.setDigits(1);
			_spinnerRepeatedTau.setMinimum(1);
			_spinnerRepeatedTau.setMaximum(10);
			_spinnerRepeatedTau.addSelectionListener(_defaultSelectionListener);
			_spinnerRepeatedTau.addMouseWheelListener(_defaultSpinnerMouseWheelListener);

			/*
			 * checkbox: sync smoothing
			 */
			_chkIsSynchSmoothing = _tk.createButton(
					container,
					Messages.TourChart_Smoothing_Checkbox_IsSyncSmoothing,
					SWT.CHECK);
			GridDataFactory.fillDefaults() //
					.align(SWT.FILL, SWT.CENTER)
					.span(2, 1)
					.applyTo(_chkIsSynchSmoothing);
			_chkIsSynchSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsSyncSmoothing_Tooltip);
			_chkIsSynchSmoothing.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void createUI20SmoothSpeed(final Composite parent) {

		/*
		 * image: speed
		 */
		_iconSpeed = new CLabel(parent, SWT.NONE);
		_iconSpeed.setBackground(_tk.getColors().getBackground());
		_iconSpeed.setImage(_imageSpeed);

		/*
		 * label: smooth speed
		 */
		_lblSpeedSmoothing = _tk.createLabel(parent, Messages.TourChart_Smoothing_Label_SpeedSmoothing);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblSpeedSmoothing);
		_lblSpeedSmoothing.setToolTipText(Messages.TourChart_Smoothing_Label_SpeedSmoothing_Tooltip);

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
	}

	private void createUI30SmoothGradient(final Composite parent) {

		/*
		 * image: gradient
		 */
		_iconGradient = new CLabel(parent, SWT.NONE);
		_iconGradient.setBackground(_tk.getColors().getBackground());
		_iconGradient.setImage(_imageGradient);

		/*
		 * label: smooth gradient
		 */
		_lblGradientSmoothing = _tk.createLabel(
				parent,
				Messages.TourChart_Smoothing_Checkbox_IsGradientSmoothing,
				SWT.CHECK);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_lblGradientSmoothing);
		_lblGradientSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsGradientSmoothing_Tooltip);

		/*
		 * spinner: gradient tau
		 */
		_spinnerGradientTau = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(_spinnerGradientTau);
		_spinnerGradientTau.setDigits(1);
		_spinnerGradientTau.setMinimum(1);
		_spinnerGradientTau.setMaximum(MAX_TAU);
		_spinnerGradientTau.addSelectionListener(_defaultSelectionListener);
		_spinnerGradientTau.addMouseWheelListener(_defaultSpinnerMouseWheelListener);
	}

	private void createUI40SmoothAltitude(final Composite parent) {

		/*
		 * image: altitude
		 */
		_iconAltitude = new CLabel(parent, SWT.NONE);
		_iconAltitude.setBackground(_tk.getColors().getBackground());
		_iconAltitude.setImage(_imageAltitude);

		/*
		 * checkbox: smooth altitude
		 */
		_chkIsAltitudeSmoothing = _tk.createButton(
				parent,
				Messages.TourChart_Smoothing_Checkbox_IsAltitudeSmoothing,
				SWT.CHECK);
		GridDataFactory.fillDefaults() //
				.align(SWT.FILL, SWT.CENTER)
				.span(2, 1)
				.applyTo(_chkIsAltitudeSmoothing);
		_chkIsAltitudeSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsAltitudeSmoothing_Tooltip);
		_chkIsAltitudeSmoothing.addSelectionListener(_defaultSelectionListener);
	}

	private void createUI50SmoothPulse(final Composite parent) {

		/*
		 * image: pulse
		 */
		_iconPulse = new CLabel(parent, SWT.NONE);
		_iconPulse.setBackground(_tk.getColors().getBackground());
		_iconPulse.setImage(_imagePulse);

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
		_chkIsPulseSmoothing.setToolTipText(Messages.TourChart_Smoothing_Checkbox_IsPulseSmoothing_Tooltip);

		/*
		 * spinner: speed tau
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
	}

	@Override
	public void dispose() {

		Util.disposeResource(_imageAltitude);
		Util.disposeResource(_imageAltitudeDisabled);
		Util.disposeResource(_imageGradient);
		Util.disposeResource(_imageGradientDisabled);
		Util.disposeResource(_imagePulse);
		Util.disposeResource(_imagePulseDisabled);
		Util.disposeResource(_imageSpeed);
		Util.disposeResource(_imageSpeedDisabled);

		super.dispose();
	}

	private void enableControls() {

		final String selectedAlgo = getSelectedAlgorithm();
		final boolean isJamet = selectedAlgo.equals(SMOOTHING_ALGORITHM_JAMET);
		final boolean isRepeated = _spinnerRepeatedSmoothing.getSelection() != 0;

		_lblSpeedSmoothing.setEnabled(isJamet);
		_lblGradientSmoothing.setEnabled(isJamet);
		_lblRepeatedSmoothing.setEnabled(isJamet);
		_lblRepeatedTau.setEnabled(isJamet && isRepeated);

		_chkIsAltitudeSmoothing.setEnabled(isJamet);
		_chkIsPulseSmoothing.setEnabled(isJamet);
		_chkIsSynchSmoothing.setEnabled(isJamet);

		_spinnerSpeedTau.setEnabled(isJamet);
		_spinnerGradientTau.setEnabled(isJamet);
		_spinnerPulseTau.setEnabled(isJamet && _chkIsPulseSmoothing.getSelection());
		_spinnerRepeatedSmoothing.setEnabled(isJamet);
		_spinnerRepeatedTau.setEnabled(isJamet && isRepeated);

		_iconAltitude.setImage(isJamet ? _imageAltitude : _imageAltitudeDisabled);
		_iconGradient.setImage(isJamet ? _imageGradient : _imageGradientDisabled);
		_iconPulse.setImage(isJamet ? _imagePulse : _imagePulseDisabled);
		_iconSpeed.setImage(isJamet ? _imageSpeed : _imageSpeedDisabled);

	}

	private String getSelectedAlgorithm() {
		return SMOOTHING_ALGORITHM[_comboAlgorithm.getSelectionIndex()][0];
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_hintDefaultSpinnerWidth = _isLinux ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(_isOSX ? 10 : 5);

		_tk = new FormToolkit(parent.getDisplay());

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModifySmoothing(e.widget);
			}
		};

		_defaultSpinnerMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				if (_isUpdateUI) {
					return;
				}
				onModifySmoothing(event.widget);
			}
		};
	}

	private void onModifySmoothing(final Widget widget) {

		updateSyncedSlider(widget);

		enableControls();

		saveStore();

		TourManager.getInstance().removeAllToursFromCache();
//		TourManager.fireEvent(TourEventId.CLEAR_DISPLAYED_TOUR, null, TourChartPropertyView.this);

		// fire unique event for all changes
		TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED, null);
	}

	private void restoreStore() {

		_isUpdateUI = true;

		// smoothing algorithm
		final String prefAlgoId = _prefStore.getString(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM);
		int prefAlgoIndex = -1;
		for (int algoIndex = 0; algoIndex < SMOOTHING_ALGORITHM.length; algoIndex++) {
			if (SMOOTHING_ALGORITHM[algoIndex][0].equals(prefAlgoId)) {
				prefAlgoIndex = algoIndex;
				break;
			}
		}
		if (prefAlgoIndex == -1) {
			prefAlgoIndex = 0;
		}
		_comboAlgorithm.select(prefAlgoIndex);

		// repeated smoothing
		_spinnerRepeatedSmoothing.setSelection(//
				_prefStore.getInt(ITourbookPreferences.GRAPH_SMOOTHING_REPEATED_SMOOTHING));
		_spinnerRepeatedTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_REPEATED_TAU) * 10));

		// altitude
		_chkIsAltitudeSmoothing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_SMOOTHING_IS_ALTITUDE));

		// gradient
		_spinnerGradientTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_GRADIENT_TAU) * 10));

		// pulse
		_chkIsPulseSmoothing.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_SMOOTHING_IS_PULSE));
		_spinnerPulseTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_PULSE_TAU) * 10));

		// speed
		_spinnerSpeedTau.setSelection(//
				(int) (_prefStore.getDouble(ITourbookPreferences.GRAPH_SMOOTHING_SPEED_TAU) * 10));

		_isUpdateUI = false;
	}

	/**
	 * Update new values in the pref store
	 */
	private void saveStore() {

		// smoothing algorithm
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM, getSelectedAlgorithm());
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_REPEATED_SMOOTHING,//
				_spinnerRepeatedSmoothing.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_REPEATED_TAU,//
				_spinnerRepeatedTau.getSelection() / 10.0);

		// altitude
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_IS_ALTITUDE, _chkIsAltitudeSmoothing.getSelection());

		// gradient
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_GRADIENT_TAU,//
				_spinnerGradientTau.getSelection() / 10.0);

		// pulse
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_IS_PULSE, _chkIsPulseSmoothing.getSelection());
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_PULSE_TAU, _spinnerPulseTau.getSelection() / 10.0);

		// speed smoothing
		_prefStore.setValue(ITourbookPreferences.GRAPH_SMOOTHING_SPEED_TAU, _spinnerSpeedTau.getSelection() / 10.0);
	}

	@Override
	public void setFocus() {

	}

	private void updateSyncedSlider(final Widget widget) {

		int synchValue = -1;

		if (widget == _chkIsSynchSmoothing && _chkIsSynchSmoothing.getSelection() && _spinnerLastUsed != null) {

			// synch is checked, set spinner to the last used values, when available

			synchValue = _spinnerLastUsed.getSelection();

		} else {

			if (widget == _spinnerGradientTau) {

				synchValue = _spinnerGradientTau.getSelection();
				_spinnerLastUsed = _spinnerGradientTau;

			} else if (widget == _spinnerPulseTau) {

				synchValue = _spinnerPulseTau.getSelection();
				_spinnerLastUsed = _spinnerPulseTau;

			} else if (widget == _spinnerSpeedTau) {

				synchValue = _spinnerSpeedTau.getSelection();
				_spinnerLastUsed = _spinnerSpeedTau;
			}
		}

		// set last used spinner if existing
		if (_chkIsSynchSmoothing.getSelection() == false) {
			// no synching
			return;
		}

		if (synchValue != -1) {
			_isUpdateUI = true;
			{
				_spinnerGradientTau.setSelection(synchValue);
				_spinnerPulseTau.setSelection(synchValue);
				_spinnerSpeedTau.setSelection(synchValue);
			}
			_isUpdateUI = false;
		}
	}

	private void updateUI() {

		_isUpdateUI = true;
		{
			/*
			 * fillup algorithm combo
			 */
			for (final String[] algo : SMOOTHING_ALGORITHM) {
				_comboAlgorithm.add(algo[1]);
			}
			_comboAlgorithm.select(0);
		}
		_isUpdateUI = false;
	}

}
