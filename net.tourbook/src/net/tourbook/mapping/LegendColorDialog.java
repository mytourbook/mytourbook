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
package net.tourbook.mapping;

import net.tourbook.colors.ColorDefinition;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class LegendColorDialog extends TitleAreaDialog {

//	private static final String		DIALOG_SETTINGS_SELECTED_VALUE_POINT	= "selected.value.point";	//$NON-NLS-1$

	private static final String		VALUE_SPACER							= "999";					//$NON-NLS-1$

//	private Scale					fScaleRed;
//	private Scale					fScaleGreen;
//	private Scale					fScaleBlue;
//	private Label					fLblRedValue;
//	private Label					fLblGreenValue;
//	private Label					fLblBlueValue;

	private Canvas					fLegendCanvas;
	private Image					fLegendImage;

//	private Combo					fComboValuePoint;
	private Combo					fComboMaxBrightness;
	private Combo					fComboMinBrightness;

	private Scale					fScaleMinBrightness;
	private Label					fLblMinBrightnessValue;
	private Scale					fScaleMaxBrightness;
	private Label					fLblMaxBrightnessValue;

	private Button					fChkForceMinValue;
	private Label					fLblMinValue;
	private Text					fTxtMinValue;
//	private Label					fLblMinValueUnit;

	private Button					fChkForceMaxValue;
	private Label					fLblMaxValue;
	private Text					fTxtMaxValue;
//	private Label					fLblMaxValueUnit;

	private final ILegendProvider	fLegendProvider;
	private ValueColor[]			fValueColors;

	private LegendColor				fLegendColorWorkingCopy;

	private final SelectionAdapter	fDefaultSelectionAdapter;
	private final IDialogSettings	dialogSettings;

	private ColorDefinition			fColorDefinition;

	private boolean					fInitializeControls;

	private ColorSelector			fColorSelectorMax;
	private ColorSelector			fColorSelectorHigh;
	private ColorSelector			fColorSelectorMid;
	private ColorSelector			fColorSelectorLow;
	private ColorSelector			fColorSelectorMin;

	{
		fDefaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateLegendData();
			}
		};
	}
	{
		dialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	public LegendColorDialog(final Shell parentShell, final LegendProvider legendProvider) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fLegendProvider = legendProvider;
	}

	@Override
	public boolean close() {

		// keep selected value point
//		dialogSettings.put(DIALOG_SETTINGS_SELECTED_VALUE_POINT, fComboValuePoint.getSelectionIndex());

		return super.close();
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(Messages.legendcolor_dialog_title_name);

		/*
		 * initialize dialog by selecting select first value point
		 */

		// selecte previous value point
//		try {
//			fComboValuePoint.select(dialogSettings.getInt(DIALOG_SETTINGS_SELECTED_VALUE_POINT));
//		} catch (final NumberFormatException e) {
//			fComboValuePoint.select(0);
//		}
		fInitializeControls = true;
		{
			updateUI();
		}
		fInitializeControls = false;

		/*
		 * update color selectors
		 */
		ValueColor valueColor;
		valueColor = fValueColors[4];
		fColorSelectorMax.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[3];
		fColorSelectorHigh.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[2];
		fColorSelectorMid.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[1];
		fColorSelectorLow.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[0];
		fColorSelectorMin.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));

		setTitle(Messages.legendcolor_dialog_title);
		setMessage(NLS.bind(Messages.legendcolor_dialog_title_message, fColorDefinition.getVisibleName()));
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		return dlgAreaContainer;
	}

	/**
	 * create a new legend image with current size and configuration
	 */
	private void createLegendImage() {

		// dispose old legend image
		if (fLegendImage != null && !fLegendImage.isDisposed()) {
			fLegendImage.dispose();
		}

		final Point legendSize = fLegendCanvas.getSize();
		final int legendWidth = Math.max(30, legendSize.x);
		final int legendHeight = Math.max(100, legendSize.y);
		final RGB rgbTransparent = new RGB(0xfe, 0xfe, 0xfe);

		final ImageData overlayImageData = new ImageData(legendWidth, legendHeight, 24, //
				new PaletteData(0xff, 0xff00, 0xff00000));

		overlayImageData.transparentPixel = overlayImageData.palette.getPixel(rgbTransparent);

		final Display display = Display.getCurrent();
		fLegendImage = new Image(display, overlayImageData);
		final Rectangle imageBounds = fLegendImage.getBounds();

		final Color transparentColor = new Color(display, rgbTransparent);
		final GC gc = new GC(fLegendImage);
		{
			gc.setBackground(transparentColor);
			gc.fillRectangle(imageBounds);

			TourPainter.drawLegendColors(gc, imageBounds, fLegendProvider, true);
		}
		gc.dispose();
		transparentColor.dispose();
	}

	private void createUI(final Composite parent) {

		/*
		 * dialog container
		 */
		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().extendedMargins(10, 10, 5, 5).numColumns(3).applyTo(dlgContainer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);

		createUILegend(dlgContainer);
		createUILegendColorSelector(dlgContainer);

		/*
		 * value container
		 */
		final Composite valueContainer = new Composite(dlgContainer, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(valueContainer);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);

//		createUIValuePointColor(valueContainer);
		createUIBrightness(valueContainer);
		createUIMinMaxValue(valueContainer);

	}

	private void createUIBrightness(final Composite parent) {

		Label label;

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.legendcolor_dialog_group_minmax_brightness);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(group);

		/*
		 * combobox: max brightness
		 */

		label = new Label(group, SWT.NONE);
		label.setText(Messages.legendcolor_dialog_max_brightness_label);
		label.setToolTipText(Messages.legendcolor_dialog_max_brightness_tooltip);

		fComboMaxBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMaxBrightness.addSelectionListener(fDefaultSelectionAdapter);
		for (final String comboLabel : LegendColor.BRIGHTNESS_LABELS) {
			fComboMaxBrightness.add(comboLabel);
		}

		// spacer
//		new Label(group, SWT.NONE);

		/*
		 * scale: max brightness factor
		 */
//		label = new Label(group, SWT.NONE);
//		label.setText("Brightness:");
		fScaleMaxBrightness = new Scale(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleMaxBrightness);
		fScaleMaxBrightness.setMinimum(0);
		fScaleMaxBrightness.setMaximum(100);
		fScaleMaxBrightness.setPageIncrement(10);
		fScaleMaxBrightness.addSelectionListener(fDefaultSelectionAdapter);

		fLblMaxBrightnessValue = new Label(group, SWT.NONE);
		fLblMaxBrightnessValue.setText(VALUE_SPACER);
		fLblMaxBrightnessValue.pack(true);

		/*
		 * combobox: min brightness
		 */

		label = new Label(group, SWT.NONE);
		label.setText(Messages.legendcolor_dialog_min_brightness_label);
		label.setToolTipText(Messages.legendcolor_dialog_min_brightness_tooltip);

		fComboMinBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMinBrightness.addSelectionListener(fDefaultSelectionAdapter);
		for (final String comboLabel : LegendColor.BRIGHTNESS_LABELS) {
			fComboMinBrightness.add(comboLabel);
		}

		// spacer
//		new Label(group, SWT.NONE);

		/*
		 * scale: min brightness factor
		 */
//		label = new Label(group, SWT.NONE);
//		label.setText("Brightness:");
		fScaleMinBrightness = new Scale(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleMinBrightness);
		fScaleMinBrightness.setMinimum(0);
		fScaleMinBrightness.setMaximum(100);
		fScaleMinBrightness.setPageIncrement(10);
		fScaleMinBrightness.addSelectionListener(fDefaultSelectionAdapter);

		fLblMinBrightnessValue = new Label(group, SWT.NONE);
		fLblMinBrightnessValue.setText(VALUE_SPACER);
		fLblMinBrightnessValue.pack(true);

	}

	private void createUILegend(final Composite parent) {
		/*
		 * legend
		 */
		fLegendCanvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(fLegendCanvas);
		fLegendCanvas.addPaintListener(new PaintListener() {
			public void paintControl(final PaintEvent e) {
				if (fLegendImage == null || fLegendImage.isDisposed()) {
					return;
				}

				e.gc.drawImage(fLegendImage, 0, 0);
			}
		});
		fLegendCanvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				// update image when the dialog is created
				updateLegendImage();
			}
		});
		fLegendCanvas.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(final DisposeEvent e) {
				if (fLegendImage != null) {
					fLegendImage.dispose();
				}

			}
		});
	}

	private void createUILegendColorSelector(final Composite parent) {

		final Composite selectorContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(selectorContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 20, 10, 10).applyTo(selectorContainer);

//		selectorContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		/*
		 * max color
		 */
		final Composite maxContainer = new Composite(selectorContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(maxContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(maxContainer);

//		maxContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		fColorSelectorMax = new ColorSelector(maxContainer);
		GridDataFactory.swtDefaults()
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(fColorSelectorMax.getButton());
		fColorSelectorMax.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorSelector(fColorSelectorMax, 0);
			}
		});

		/*
		 * high color
		 */
		final Composite highContainer = new Composite(selectorContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(highContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(highContainer);

		fColorSelectorHigh = new ColorSelector(highContainer);
		GridDataFactory.swtDefaults()
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(fColorSelectorHigh.getButton());

		fColorSelectorHigh.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorSelector(fColorSelectorHigh, 1);
			}
		});

		/*
		 * mid color
		 */
//		new Label(selectorContainer, SWT.NONE);
		final Composite midContainer = new Composite(selectorContainer, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(midContainer);
		GridLayoutFactory.fillDefaults().margins(0, 5).applyTo(midContainer);

		fColorSelectorMid = new ColorSelector(midContainer);
		GridDataFactory.swtDefaults().applyTo(fColorSelectorMid.getButton());
		fColorSelectorMid.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorSelector(fColorSelectorMid, 2);
			}
		});
//		new Label(selectorContainer, SWT.NONE);

		/*
		 * low color
		 */
		final Composite lowContainer = new Composite(selectorContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(lowContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(lowContainer);

		fColorSelectorLow = new ColorSelector(lowContainer);
		GridDataFactory.swtDefaults()
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.END)
				.applyTo(fColorSelectorLow.getButton());

		fColorSelectorLow.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorSelector(fColorSelectorLow, 3);
			}
		});

		/*
		 * min color
		 */
		final Composite minContainer = new Composite(selectorContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(minContainer);
		GridLayoutFactory.fillDefaults().extendedMargins(0, 0, 0, 0).applyTo(minContainer);

		fColorSelectorMin = new ColorSelector(minContainer);
		GridDataFactory.swtDefaults()
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.END)
				.applyTo(fColorSelectorMin.getButton());

		fColorSelectorMin.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelectColorSelector(fColorSelectorMin, 4);
			}
		});
//		minContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
	}

	private void createUIMinMaxValue(final Composite parent) {

		final ModifyListener validateFieldOnModify = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		final Listener validateFieldOnVerify = new Listener() {
			public void handleEvent(final Event e) {
				verifyIntegerEvent(e);
			}
		};

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.legendcolor_dialog_group_minmax_value);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(group);

		/*
		 * checkbox: overwrite min value
		 */

		fChkForceMinValue = new Button(group, SWT.CHECK);
		fChkForceMinValue.setText(Messages.legendcolor_dialog_chk_min_value_text);
		fChkForceMinValue.setToolTipText(Messages.legendcolor_dialog_chk_min_value_tooltip);
		GridDataFactory.swtDefaults().applyTo(fChkForceMinValue);
		fChkForceMinValue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
				validateFields();
			}
		});

		/*
		 * input: min value
		 */
		fLblMinValue = new Label(group, SWT.NONE);
		fLblMinValue.setText(Messages.legendcolor_dialog_txt_min_value);
		GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(fLblMinValue);

		fTxtMinValue = new Text(group, SWT.BORDER);
		GridDataFactory.swtDefaults().hint(UI.DEFAULT_FIELD_WIDTH, SWT.DEFAULT).applyTo(fTxtMinValue);
		fTxtMinValue.addModifyListener(validateFieldOnModify);
		fTxtMinValue.addListener(SWT.Verify, validateFieldOnVerify);

//		fLblMinValueUnit = new Label(group, SWT.NONE);
//		fLblMinValueUnit.setText(fLegendProvider.getLegendConfig().unitText);

		/*
		 * checkbox: overwrite max value
		 */

		fChkForceMaxValue = new Button(group, SWT.CHECK);
		fChkForceMaxValue.setText(Messages.legendcolor_dialog_chk_max_value_text);
		fChkForceMaxValue.setToolTipText(Messages.legendcolor_dialog_chk_max_value_tooltip);
		GridDataFactory.swtDefaults().applyTo(fChkForceMaxValue);
		fChkForceMaxValue.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				enableControls();
				validateFields();
			}
		});

		/*
		 * input: max value
		 */
		fLblMaxValue = new Label(group, SWT.NONE);
		fLblMaxValue.setText(Messages.legendcolor_dialog_txt_max_value);
		GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(fLblMaxValue);

		fTxtMaxValue = new Text(group, SWT.BORDER);
		GridDataFactory.swtDefaults().hint(UI.DEFAULT_FIELD_WIDTH, SWT.DEFAULT).applyTo(fTxtMaxValue);
		fTxtMaxValue.addModifyListener(validateFieldOnModify);
		fTxtMaxValue.addListener(SWT.Verify, validateFieldOnVerify);

//		fLblMaxValueUnit = new Label(group, SWT.NONE);
//		fLblMaxValueUnit.setText(fLegendProvider.getLegendConfig().unitText);
	}

	private void enableControls() {

		// min brightness
		final int minBrightness = fComboMinBrightness.getSelectionIndex();
		fScaleMinBrightness.setEnabled(minBrightness != 0);
		fLblMinBrightnessValue.setEnabled(minBrightness != 0);

		// max brightness
		final int maxBrightness = fComboMaxBrightness.getSelectionIndex();
		fScaleMaxBrightness.setEnabled(maxBrightness != 0);
		fLblMaxBrightnessValue.setEnabled(maxBrightness != 0);

		// min value
		boolean isChecked = fChkForceMinValue.getSelection();
		fLblMinValue.setEnabled(isChecked);
		fTxtMinValue.setEnabled(isChecked);

		// max value
		isChecked = fChkForceMaxValue.getSelection();
		fLblMaxValue.setEnabled(isChecked);
		fTxtMaxValue.setEnabled(isChecked);
	}

//	private void createUIValuePointColor(final Composite parent) {
//
//		Label label;
//
//		final Composite container = new Composite(parent, SWT.NONE);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//
//		/*
//		 * combobox: range point
//		 */
//
//		label = new Label(container, SWT.NONE);
//		label.setText(Messages.legendcolor_dialog_lbl_value_point);
//
//		fComboValuePoint = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
//		fComboValuePoint.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(final SelectionEvent e) {
//				updateUI();
//			}
//		});
//		// revert sequence so that max is on top
//		final List<String> unitLabels = fLegendProvider.getLegendConfig().unitLabels;
//		for (int unitIndex = unitLabels.size() - 1; unitIndex >= 0; unitIndex--) {
//			fComboValuePoint.add(unitLabels.get(unitIndex));
//		}
//
//		/*
//		 * group: value point color
//		 */
//		final Group group = new Group(parent, SWT.NONE);
//		group.setText(Messages.legendcolor_dialog_group_value_point_color);
//		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
//		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(group);
//
//		/*
//		 * red
//		 */
//		label = new Label(group, SWT.NONE);
//		label.setText(Messages.legendcolor_dialog_lbl_red);
//
//		fScaleRed = new Scale(group, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleRed);
//		fScaleRed.setMinimum(0);
//		fScaleRed.setMaximum(1000);
//		fScaleRed.setPageIncrement(100);
//		fScaleRed.addSelectionListener(fDefaultSelectionAdapter);
//
//		fLblRedValue = new Label(group, SWT.NONE);
//		fLblRedValue.setText(VALUE_SPACER);
//
//		/*
//		 * green
//		 */
//		label = new Label(group, SWT.NONE);
//		label.setText(Messages.legendcolor_dialog_lbl_green);
//
//		fScaleGreen = new Scale(group, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleGreen);
//		fScaleGreen.setMinimum(0);
//		fScaleGreen.setMaximum(1000);
//		fScaleGreen.setPageIncrement(100);
//		fScaleGreen.addSelectionListener(fDefaultSelectionAdapter);
//
//		fLblGreenValue = new Label(group, SWT.NONE);
//		fLblGreenValue.setText(VALUE_SPACER);
//
//		/*
//		 * blue
//		 */
//		label = new Label(group, SWT.NONE);
//		label.setText(Messages.legendcolor_dialog_lbl_blue);
//
//		fScaleBlue = new Scale(group, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleBlue);
//		fScaleBlue.setMinimum(0);
//		fScaleBlue.setMaximum(1000);
//		fScaleBlue.setPageIncrement(100);
//		fScaleBlue.addSelectionListener(fDefaultSelectionAdapter);
//
//		fLblBlueValue = new Label(group, SWT.NONE);
//		fLblBlueValue.setText(VALUE_SPACER);
//	}

	private void enableOK(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {
			okButton.setEnabled(isEnabled);
		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return dialogSettings;
	}

	public LegendColor getLegendColor() {
		return fLegendColorWorkingCopy;
	}

	private void onSelectColorSelector(final ColorSelector colorSelector, final int valueColorIndex) {

//		RGB colorValue = fColorSelectorMax.getColorValue();
//
//		final int selectedValuePoint = 0;

//		final int items = fComboValuePoint.getItemCount() - 1;
//		final ValueColor valueColor = fValueColors[items - valueColorIndex];
		final ValueColor valueColor = fValueColors[4 - valueColorIndex];

		final RGB colorValue = colorSelector.getColorValue();
		valueColor.red = colorValue.red;
		valueColor.green = colorValue.green;
		valueColor.blue = colorValue.blue;

//		fComboValuePoint.select(valueColorIndex);

		updateUI();
	}

	/**
	 * Set the {@link LegendColor} which will be displayed in this dialog, it will use a copy of the
	 * supplied {@link LegendColor}
	 * 
	 * @param colorDefinition
	 */
	public void setLegendColor(final ColorDefinition colorDefinition) {

		fColorDefinition = colorDefinition;

		/*
		 * use a copy of the legendColor to support the cancel feature
		 */
		fLegendColorWorkingCopy = colorDefinition.getNewLegendColor().getCopy();
		fValueColors = fLegendColorWorkingCopy.valueColors;
	}

	/**
	 * update labels from the scale value
	 */
	private void updateLabels() {

//		fLblRedValue.setText(Integer.toString(fScaleRed.getSelection() / 10));
//		fLblRedValue.pack(true);
//
//		fLblGreenValue.setText(Integer.toString(fScaleGreen.getSelection() / 10));
//		fLblGreenValue.pack(true);
//
//		fLblBlueValue.setText(Integer.toString(fScaleBlue.getSelection() / 10));
//		fLblBlueValue.pack(true);

		fLblMinBrightnessValue.setText(Integer.toString(fScaleMinBrightness.getSelection()));
		fLblMinBrightnessValue.pack(true);

		fLblMaxBrightnessValue.setText(Integer.toString(fScaleMaxBrightness.getSelection()));
		fLblMaxBrightnessValue.pack(true);
	}

	/**
	 * Update legend data from the UI
	 */
	private void updateLegendData() {

//		final int selectedValuePoint = fComboValuePoint.getSelectionIndex();
//		if (selectedValuePoint == -1) {
//			return;
//		}
//
//		final int items = fComboValuePoint.getItemCount() - 1;
//		ValueColor valueColor = fValueColors[items - selectedValuePoint];

		// update red/green/blue in the legend provider
//		valueColor.red = 255 * fScaleRed.getSelection() / 1000;
//		valueColor.green = 255 * fScaleGreen.getSelection() / 1000;
//		valueColor.blue = 255 * fScaleBlue.getSelection() / 1000;

		// update color selector
		ValueColor valueColor = fValueColors[4];
		fColorSelectorMax.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[3];
		fColorSelectorHigh.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[2];
		fColorSelectorMid.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[1];
		fColorSelectorLow.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));
		valueColor = fValueColors[0];
		fColorSelectorMin.setColorValue(new RGB(valueColor.red, valueColor.green, valueColor.blue));

		fLegendColorWorkingCopy.minBrightness = fComboMinBrightness.getSelectionIndex();
		fLegendColorWorkingCopy.minBrightnessFactor = fScaleMinBrightness.getSelection();
		fLegendColorWorkingCopy.maxBrightness = fComboMaxBrightness.getSelectionIndex();
		fLegendColorWorkingCopy.maxBrightnessFactor = fScaleMaxBrightness.getSelection();

		fLegendColorWorkingCopy.isMinValueOverwrite = fChkForceMinValue.getSelection();
		try {
			fLegendColorWorkingCopy.overwriteMinValue = Integer.parseInt(fTxtMinValue.getText());
		} catch (final NumberFormatException e) {}

		fLegendColorWorkingCopy.isMaxValueOverwrite = fChkForceMaxValue.getSelection();
		try {
			fLegendColorWorkingCopy.overwriteMaxValue = Integer.parseInt(fTxtMaxValue.getText());
		} catch (final NumberFormatException e) {}

		enableControls();
		updateLabels();
		updateLegendImage();
	}

	private void updateLegendImage() {

		final LegendColor legendColor = fLegendProvider.getLegendColor();
		legendColor.valueColors = fValueColors;
		legendColor.minBrightnessFactor = fLegendColorWorkingCopy.minBrightnessFactor;
		legendColor.maxBrightnessFactor = fLegendColorWorkingCopy.maxBrightnessFactor;
		legendColor.minBrightness = fLegendColorWorkingCopy.minBrightness;
		legendColor.maxBrightness = fLegendColorWorkingCopy.maxBrightness;

		legendColor.isMinValueOverwrite = fLegendColorWorkingCopy.isMinValueOverwrite;
		legendColor.overwriteMinValue = fLegendColorWorkingCopy.overwriteMinValue;
		legendColor.isMaxValueOverwrite = fLegendColorWorkingCopy.isMaxValueOverwrite;
		legendColor.overwriteMaxValue = fLegendColorWorkingCopy.overwriteMaxValue;

		createLegendImage();
		fLegendCanvas.redraw();
	}

	/**
	 * Update UI from legend data
	 */
	private void updateUI() {

//		final int selectedValueColor = fComboValuePoint.getSelectionIndex();
//		if (selectedValueColor == -1) {
//			return;
//		}
//
//		final int items = fComboValuePoint.getItemCount() - 1;
//		final ValueColor valueColor = fValueColors[items - selectedValueColor];
//
//		// update red/green/blue scale from range color
//		fScaleRed.setSelection(1000 * valueColor.red / 255);
//		fScaleGreen.setSelection(1000 * valueColor.green / 255);
//		fScaleBlue.setSelection(1000 * valueColor.blue / 255);

		// update min/max brightness
		fComboMinBrightness.select(fLegendColorWorkingCopy.minBrightness);
		fScaleMinBrightness.setSelection(fLegendColorWorkingCopy.minBrightnessFactor);
		fComboMaxBrightness.select(fLegendColorWorkingCopy.maxBrightness);
		fScaleMaxBrightness.setSelection(fLegendColorWorkingCopy.maxBrightnessFactor);

		// update min/max value
		fChkForceMinValue.setSelection(fLegendColorWorkingCopy.isMinValueOverwrite);
		fTxtMinValue.setText(Integer.toString(fLegendColorWorkingCopy.overwriteMinValue));
		fChkForceMaxValue.setSelection(fLegendColorWorkingCopy.isMaxValueOverwrite);
		fTxtMaxValue.setText(Integer.toString(fLegendColorWorkingCopy.overwriteMaxValue));

		updateLabels();
		enableControls();
	}

	private void validateFields() {

		if (fInitializeControls) {
			return;
		}

		setErrorMessage(null);

		final boolean isMinEnabled = fChkForceMinValue.getSelection();
		if (isMinEnabled) {

			// validate min value

			if (verifyIntegerValue(fTxtMinValue.getText()) == false) {
				setErrorMessage(Messages.legendcolor_dialog_error_min_value_is_required);
				enableOK(false);
				fTxtMinValue.setFocus();
				return;
			}
		}

		final boolean isMaxEnabled = fChkForceMaxValue.getSelection();
		if (isMaxEnabled) {

			if (verifyIntegerValue(fTxtMaxValue.getText()) == false) {
				setErrorMessage(Messages.legendcolor_dialog_error_max_value_is_required);
				enableOK(false);
				fTxtMaxValue.setFocus();
				return;
			}
		}

		if (isMinEnabled
				&& isMaxEnabled
				&& (Integer.parseInt(fTxtMaxValue.getText()) <= Integer.parseInt(fTxtMinValue.getText()))) {

			setErrorMessage(Messages.legendcolor_dialog_error_max_greater_min);
			enableOK(false);
			return;
		}

		setMessage(NLS.bind(Messages.legendcolor_dialog_title_message, fColorDefinition.getVisibleName()));

		updateLegendData();
		enableOK(true);
	}

	private void verifyIntegerEvent(final Event e) {

		// allow backspace, del and - key
		if (e.character == SWT.BS || e.character == SWT.DEL || e.character == '-') {
			return;
		}

		try {
			Integer.parseInt(e.text);
		} catch (final NumberFormatException ex) {
			e.doit = false;
		}
	}

	private boolean verifyIntegerValue(final String valueString) {

		if (valueString.trim().length() == 0) {
			return false;
		}

		try {
			Integer.parseInt(valueString);
			return true;
		} catch (final NumberFormatException ex) {
			return false;
		}
	}
}
