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
package net.tourbook.map2.view;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.ColorValue;
import net.tourbook.common.color.IGradientColors;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.common.color.MapColorProfile;
import net.tourbook.map2.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

public class DialogMap2ColorEditor extends TitleAreaDialog {

	private static final String		VALUE_SPACER		= "999";												//$NON-NLS-1$

	private static final int		SPINNER_MIN_VALUE	= -200;
	private static final int		SPINNER_MAX_VALUE	= 10000;

	private static final String		STATE_LIVE_UPDATE	= "IsLiveUpdate";										//$NON-NLS-1$

	private final IDialogSettings	_state				= TourbookPlugin.getStateSection("DialogMappingColor"); //$NON-NLS-1$

	private IMapColorUpdater		_mapColorUpdater;

	private final IGradientColors	_colorProvider;
	private Map2ColorProfile		_mapColorWorkingCopy;

	private final SelectionAdapter	_defaultSelectionAdapter;

	private ColorDefinition			_colorDefinition;

	private boolean					_isInitializeControls;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Canvas					_canvasMappingColor;
	private Image					_imageMappingColor;

	private Button					_btnApply;

	private Button					_chkForceMinValue;
	private Button					_chkForceMaxValue;
	private Button					_chkLiveUpdate;

	private Combo					_cboMaxBrightness;
	private Combo					_cboMinBrightness;

	private Label					_lblMinBrightnessValue;
	private Label					_lblMaxBrightnessValue;
	private Label					_lblMinValue;
	private Label					_lblMaxValue;

	private Scale					_scaleMinBrightness;
	private Scale					_scaleMaxBrightness;

	private Spinner					_spinMinValue;
	private Spinner					_spinMaxValue;

	private ColorSelector			_colorSelectorMax;
	private ColorSelector			_colorSelectorHigh;
	private ColorSelector			_colorSelectorMid;
	private ColorSelector			_colorSelectorLow;
	private ColorSelector			_colorSelectorMin;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateModelFromUI();
				doLiveUpdate();
			}
		};

	}

	public DialogMap2ColorEditor(	final Shell parentShell,
								final IGradientColors colorProvider,
								final IMapColorUpdater mapColorUpdater) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		_colorProvider = colorProvider;
		_mapColorUpdater = mapColorUpdater;
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(Messages.legendcolor_dialog_title_name);

		/*
		 * initialize dialog by selecting select first value point
		 */

		_isInitializeControls = true;
		{
			updateUI();
		}
		_isInitializeControls = false;

		/*
		 * update color selectors
		 */
		final ColorValue[] colorValues = _mapColorWorkingCopy.getColorValues();
		ColorValue colorValue;

		colorValue = colorValues[4];
		_colorSelectorMax.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[3];
		_colorSelectorHigh.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[2];
		_colorSelectorMid.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[1];
		_colorSelectorLow.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[0];
		_colorSelectorMin.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		setTitle(Messages.legendcolor_dialog_title);
		setMessage(NLS.bind(Messages.legendcolor_dialog_title_message, _colorDefinition.getVisibleName()));
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		restoreState();

		enableControls();

		return dlgAreaContainer;
	}

	/**
	 * create a new legend image with current size and configuration
	 */
	private void createLegendImage() {

		// dispose old image
		UI.disposeResource(_imageMappingColor);

		final Point legendSize = _canvasMappingColor.getSize();

		final int legendWidth = Math.max(140, legendSize.x);
		final int legendHeight = Math.max(100, legendSize.y);

		_imageMappingColor = TourMapPainter.createMapLegendImage(
				Display.getCurrent(),
				_colorProvider,
				legendWidth,
				legendHeight);
	}

	private void createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		/*
		 * dialog container
		 */
		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().extendedMargins(10, 10, 5, 5).numColumns(3).applyTo(dlgContainer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
//		dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI10Legend(dlgContainer);
			createUI20LegendColorSelector(dlgContainer);

			// value container
			final Composite valueContainer = new Composite(dlgContainer, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(valueContainer);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);
//			valueContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
			{
				createUI_30_MinMaxValue(valueContainer);
				createUI_40_Brightness(valueContainer);

				createUI_50_Apply(valueContainer);

// this is not yet implemented
//				createUI_60_RestoreDefaults(valueContainer);
			}
		}
	}

	private void createUI_30_MinMaxValue(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.legendcolor_dialog_group_minmax_value);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(group);
		{
			/*
			 * checkbox: overwrite min value
			 */
			_chkForceMinValue = new Button(group, SWT.CHECK);
			_chkForceMinValue.setText(Messages.legendcolor_dialog_chk_min_value_text);
			_chkForceMinValue.setToolTipText(Messages.legendcolor_dialog_chk_min_value_tooltip);
			GridDataFactory.swtDefaults().applyTo(_chkForceMinValue);
			_chkForceMinValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableControls();
					validateFields();
				}
			});

			/*
			 * input: min value
			 */
			_lblMinValue = new Label(group, SWT.NONE);
			_lblMinValue.setText(Messages.legendcolor_dialog_txt_min_value);
			GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(_lblMinValue);

			_spinMinValue = new Spinner(group, SWT.BORDER);
			_spinMinValue.setMinimum(SPINNER_MIN_VALUE);
			_spinMinValue.setMaximum(SPINNER_MAX_VALUE);
			_spinMinValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});
			_spinMinValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					validateFields();
				}
			});

			/*
			 * checkbox: overwrite max value
			 */

			_chkForceMaxValue = new Button(group, SWT.CHECK);
			_chkForceMaxValue.setText(Messages.legendcolor_dialog_chk_max_value_text);
			_chkForceMaxValue.setToolTipText(Messages.legendcolor_dialog_chk_max_value_tooltip);
			GridDataFactory.swtDefaults().applyTo(_chkForceMaxValue);
			_chkForceMaxValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableControls();
					validateFields();
				}
			});

			/*
			 * input: max value
			 */
			_lblMaxValue = new Label(group, SWT.NONE);
			_lblMaxValue.setText(Messages.legendcolor_dialog_txt_max_value);
			GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(_lblMaxValue);

			_spinMaxValue = new Spinner(group, SWT.BORDER);
			_spinMaxValue.setMinimum(SPINNER_MIN_VALUE);
			_spinMaxValue.setMaximum(SPINNER_MAX_VALUE);
			_spinMaxValue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});
			_spinMaxValue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					validateFields();
				}
			});
		}
	}

	private void createUI_40_Brightness(final Composite parent) {

		Label label;

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.legendcolor_dialog_group_minmax_brightness);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(group);
		{
			{
				/*
				 * combobox: min brightness
				 */

				label = new Label(group, SWT.NONE);
				label.setText(Messages.legendcolor_dialog_min_brightness_label);
				label.setToolTipText(Messages.legendcolor_dialog_min_brightness_tooltip);

				_cboMinBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMinBrightness.addSelectionListener(_defaultSelectionAdapter);
				for (final String comboLabel : MapColorProfile.BRIGHTNESS_LABELS) {
					_cboMinBrightness.add(comboLabel);
				}

				/*
				 * scale: min brightness factor
				 */
				_scaleMinBrightness = new Scale(group, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleMinBrightness);
				_scaleMinBrightness.setMinimum(0);
				_scaleMinBrightness.setMaximum(100);
				_scaleMinBrightness.setPageIncrement(10);
				_scaleMinBrightness.addSelectionListener(_defaultSelectionAdapter);

				_lblMinBrightnessValue = new Label(group, SWT.NONE);
				_lblMinBrightnessValue.setText(VALUE_SPACER);
				_lblMinBrightnessValue.pack(true);
			}

			{
				/*
				 * combobox: max brightness
				 */

				label = new Label(group, SWT.NONE);
				label.setText(Messages.legendcolor_dialog_max_brightness_label);
				label.setToolTipText(Messages.legendcolor_dialog_max_brightness_tooltip);

				_cboMaxBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
				_cboMaxBrightness.addSelectionListener(_defaultSelectionAdapter);
				for (final String comboLabel : MapColorProfile.BRIGHTNESS_LABELS) {
					_cboMaxBrightness.add(comboLabel);
				}

				/*
				 * scale: max brightness factor
				 */
				_scaleMaxBrightness = new Scale(group, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleMaxBrightness);
				_scaleMaxBrightness.setMinimum(0);
				_scaleMaxBrightness.setMaximum(100);
				_scaleMaxBrightness.setPageIncrement(10);
				_scaleMaxBrightness.addSelectionListener(_defaultSelectionAdapter);

				_lblMaxBrightnessValue = new Label(group, SWT.NONE);
				_lblMaxBrightnessValue.setText(VALUE_SPACER);
				_lblMaxBrightnessValue.pack(true);
			}

		}
	}

	private void createUI_50_Apply(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.indent(0, 20)
				.align(SWT.FILL, SWT.END)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * button: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_chkLiveUpdate);
			_chkLiveUpdate.setText(Messages.LegendColor_Dialog_Check_LiveUpdate);
			_chkLiveUpdate.setToolTipText(Messages.LegendColor_Dialog_Check_LiveUpdate_Tooltip);
			_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					enableControls();
				}
			});

			/*
			 * button: apply
			 */
			_btnApply = new Button(container, SWT.NONE);
			_btnApply.setText(Messages.App_Action_Apply);
			_btnApply.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_mapColorUpdater.applyMapColors(_mapColorWorkingCopy);
				}
			});
			setButtonLayoutData(_btnApply);
		}
	}

	private void createUI10Legend(final Composite parent) {

		/*
		 * legend
		 */
		_canvasMappingColor = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		GridDataFactory.fillDefaults()//
				.grab(false, true)
				.hint(_pc.convertWidthInCharsToPixels(18), SWT.DEFAULT)
				.applyTo(_canvasMappingColor);
		_canvasMappingColor.addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				if (_imageMappingColor == null || _imageMappingColor.isDisposed()) {
					return;
				}

				e.gc.drawImage(_imageMappingColor, 0, 0);
			}
		});
		_canvasMappingColor.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				// update image when the dialog is created or resized
				updateUI_LegendImage();
			}
		});
		_canvasMappingColor.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(final DisposeEvent e) {
				if (_imageMappingColor != null) {
					_imageMappingColor.dispose();
				}

			}
		});
	}

	private void createUI20LegendColorSelector(final Composite parent) {

		final Composite selectorContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(selectorContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 20, 10, 10).applyTo(selectorContainer);
		{
			/*
			 * max color
			 */
			final Composite maxContainer = new Composite(selectorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(maxContainer);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(maxContainer);

			_colorSelectorMax = new ColorSelector(maxContainer);
			GridDataFactory
					.swtDefaults()
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(_colorSelectorMax.getButton());
			_colorSelectorMax.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColor(_colorSelectorMax, 0);
				}
			});

			/*
			 * high color
			 */
			final Composite highContainer = new Composite(selectorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(highContainer);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(highContainer);

			_colorSelectorHigh = new ColorSelector(highContainer);
			GridDataFactory
					.swtDefaults()
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.BEGINNING)
					.applyTo(_colorSelectorHigh.getButton());

			_colorSelectorHigh.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColor(_colorSelectorHigh, 1);
				}
			});

			/*
			 * mid color
			 */
			final Composite midContainer = new Composite(selectorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(midContainer);
			GridLayoutFactory.fillDefaults().margins(0, 5).applyTo(midContainer);

			_colorSelectorMid = new ColorSelector(midContainer);
			GridDataFactory.swtDefaults().applyTo(_colorSelectorMid.getButton());
			_colorSelectorMid.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColor(_colorSelectorMid, 2);
				}
			});

			/*
			 * low color
			 */
			final Composite lowContainer = new Composite(selectorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(lowContainer);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(lowContainer);

			_colorSelectorLow = new ColorSelector(lowContainer);
			GridDataFactory
					.swtDefaults()
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.END)
					.applyTo(_colorSelectorLow.getButton());

			_colorSelectorLow.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColor(_colorSelectorLow, 3);
				}
			});

			/*
			 * min color
			 */
			final Composite minContainer = new Composite(selectorContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(false, true).applyTo(minContainer);
			GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(minContainer);

			_colorSelectorMin = new ColorSelector(minContainer);
			GridDataFactory
					.swtDefaults()
					.grab(false, true)
					.align(SWT.BEGINNING, SWT.END)
					.applyTo(_colorSelectorMin.getButton());

			_colorSelectorMin.addListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					onSelectColor(_colorSelectorMin, 4);
				}
			});
		}
	}

	private void doLiveUpdate() {

		if (_chkLiveUpdate.getSelection() == true) {
			_mapColorUpdater.applyMapColors(_mapColorWorkingCopy);
		}
	}

	private void enableControls() {

		// min brightness
		final int minBrightness = _cboMinBrightness.getSelectionIndex();
		_scaleMinBrightness.setEnabled(minBrightness != 0);
		_lblMinBrightnessValue.setEnabled(minBrightness != 0);

		// max brightness
		final int maxBrightness = _cboMaxBrightness.getSelectionIndex();
		_scaleMaxBrightness.setEnabled(maxBrightness != 0);
		_lblMaxBrightnessValue.setEnabled(maxBrightness != 0);

		// min value
		boolean isChecked = _chkForceMinValue.getSelection();
		_lblMinValue.setEnabled(isChecked);
		_spinMinValue.setEnabled(isChecked);

		// max value
		isChecked = _chkForceMaxValue.getSelection();
		_lblMaxValue.setEnabled(isChecked);
		_spinMaxValue.setEnabled(isChecked);

		// live update/apply
		final boolean isLiveUpdate = _chkLiveUpdate.getSelection();
		_btnApply.setEnabled(isLiveUpdate == false);
	}

	private void enableOK(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isEnabled);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
	}

	private void onSelectColor(final ColorSelector colorSelector, final int valueColorIndex) {

		// update model

		final ColorValue colorValue = _mapColorWorkingCopy.getColorValues()[4 - valueColorIndex];
		final RGB selectedRGB = colorSelector.getColorValue();

		colorValue.red = selectedRGB.red;
		colorValue.green = selectedRGB.green;
		colorValue.blue = selectedRGB.blue;

		updateUI();
		updateUI_LegendImage();

		updateModelFromUI();
		doLiveUpdate();
	}

	private void onSelectDefaults() {

	}

	private void restoreState() {
		_chkLiveUpdate.setSelection(_state.getBoolean(STATE_LIVE_UPDATE));
	}

	private void saveState() {
		_state.put(STATE_LIVE_UPDATE, _chkLiveUpdate.getSelection());
	}

	/**
	 * Initialized the dialog by setting the {@link Map2ColorProfile} which will be displayed in
	 * this dialog, it will use a copy of the supplied {@link Map2ColorProfile}
	 * 
	 * @param colorDefinition
	 */
	public void setLegendColor(final ColorDefinition colorDefinition) {

		_colorDefinition = colorDefinition;

		// use a copy of the legendColor to support the cancel feature
		_mapColorWorkingCopy = colorDefinition.getNewMapColor().clone();

//		System.out.println(UI.timeStampNano()
//				+ " ["
//				+ getClass().getSimpleName()
//				+ "] \t"
//				+ Arrays.toString(colorValues));
//		// TODO remove SYSTEM.OUT.PRINTLN

	}

	/**
	 * Update legend data from the UI
	 */
	private void updateModelFromUI() {

		// update color selector

		final ColorValue[] colorValues = _mapColorWorkingCopy.getColorValues();

		ColorValue colorValue = colorValues[4];
		_colorSelectorMax.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[3];
		_colorSelectorHigh.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[2];
		_colorSelectorMid.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[1];
		_colorSelectorLow.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		colorValue = colorValues[0];
		_colorSelectorMin.setColorValue(new RGB(colorValue.red, colorValue.green, colorValue.blue));

		_mapColorWorkingCopy.setMinBrightness(_cboMinBrightness.getSelectionIndex());
		_mapColorWorkingCopy.setMinBrightnessFactor(_scaleMinBrightness.getSelection());
		_mapColorWorkingCopy.setMaxBrightness(_cboMaxBrightness.getSelectionIndex());
		_mapColorWorkingCopy.setMaxBrightnessFactor(_scaleMaxBrightness.getSelection());

		_mapColorWorkingCopy.setMinValueOverwrite(_chkForceMinValue.getSelection());
		_mapColorWorkingCopy.setOverwriteMinValue(_spinMinValue.getSelection());

		_mapColorWorkingCopy.setMaxValueOverwrite(_chkForceMaxValue.getSelection());
		_mapColorWorkingCopy.setOverwriteMaxValue(_spinMaxValue.getSelection());

		enableControls();

		updateUI_Labels();
		updateUI_LegendImage();
	}

	/**
	 * Update UI from legend data
	 */
	private void updateUI() {

		// update min/max brightness
		_cboMinBrightness.select(_mapColorWorkingCopy.getMinBrightness());
		_scaleMinBrightness.setSelection(_mapColorWorkingCopy.getMinBrightnessFactor());
		_cboMaxBrightness.select(_mapColorWorkingCopy.getMaxBrightness());
		_scaleMaxBrightness.setSelection(_mapColorWorkingCopy.getMaxBrightnessFactor());

		// update min/max value
		_chkForceMinValue.setSelection(_mapColorWorkingCopy.isMinValueOverwrite());
		_spinMinValue.setSelection(_mapColorWorkingCopy.getOverwriteMinValue());
		_chkForceMaxValue.setSelection(_mapColorWorkingCopy.isMaxValueOverwrite());
		_spinMaxValue.setSelection(_mapColorWorkingCopy.getOverwriteMaxValue());

		updateUI_Labels();
		enableControls();
	}

	/**
	 * update labels from the scale value
	 */
	private void updateUI_Labels() {

		_lblMinBrightnessValue.setText(Integer.toString(_scaleMinBrightness.getSelection()));
		_lblMinBrightnessValue.pack(true);

		_lblMaxBrightnessValue.setText(Integer.toString(_scaleMaxBrightness.getSelection()));
		_lblMaxBrightnessValue.pack(true);
	}

	private void updateUI_LegendImage() {

		final MapColorProfile mapColorProfile = _colorProvider.getMapColorProfile();

		if (mapColorProfile instanceof Map2ColorProfile) {

			final Map2ColorProfile map2ColorProfile = (Map2ColorProfile) mapColorProfile;

			map2ColorProfile.setColorValues(_mapColorWorkingCopy.getColorValues());

			map2ColorProfile.setMinBrightnessFactor(_mapColorWorkingCopy.getMinBrightnessFactor());
			map2ColorProfile.setMaxBrightnessFactor(_mapColorWorkingCopy.getMaxBrightnessFactor());
			map2ColorProfile.setMinBrightness(_mapColorWorkingCopy.getMinBrightness());
			map2ColorProfile.setMaxBrightness(_mapColorWorkingCopy.getMaxBrightness());

			map2ColorProfile.setMinValueOverwrite(_mapColorWorkingCopy.isMinValueOverwrite());
			map2ColorProfile.setOverwriteMinValue(_mapColorWorkingCopy.getOverwriteMinValue());
			map2ColorProfile.setMaxValueOverwrite(_mapColorWorkingCopy.isMaxValueOverwrite());
			map2ColorProfile.setOverwriteMaxValue(_mapColorWorkingCopy.getOverwriteMaxValue());

			createLegendImage();

			_canvasMappingColor.redraw();
		}
	}

	private void validateFields() {

		if (_isInitializeControls) {
			return;
		}

		setErrorMessage(null);

		final boolean isMinEnabled = _chkForceMinValue.getSelection();
		final boolean isMaxEnabled = _chkForceMaxValue.getSelection();

		if (isMinEnabled && isMaxEnabled && (_spinMaxValue.getSelection() <= _spinMinValue.getSelection())) {

			setErrorMessage(Messages.legendcolor_dialog_error_max_greater_min);
			enableOK(false);
			return;
		}

		setMessage(NLS.bind(Messages.legendcolor_dialog_title_message, _colorDefinition.getVisibleName()));

		updateModelFromUI();
		doLiveUpdate();

		enableOK(true);
	}
}
