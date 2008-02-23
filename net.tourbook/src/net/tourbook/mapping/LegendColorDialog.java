/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.List;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

public class LegendColorDialog extends Dialog {

	private static final String		DIALOG_SETTINGS_SELECTED_VALUE_POINT	= "selected.value.point";

	private static final String		VALUE_SPACER							= "999";

	private Scale					fScaleRed;
	private Scale					fScaleGreen;
	private Scale					fScaleBlue;
	private Label					fLblRedValue;
	private Label					fLblGreenValue;
	private Label					fLblBlueValue;

	private Canvas					fLegendCanvas;
	private Image					fLegendImage;

	private Combo					fComboValuePoint;
	private Combo					fComboMaxBrightness;
	private Combo					fComboMinBrightness;

	private Scale					fScaleMinBrightness;
	private Label					fLblMinBrightnessValue;
	private Scale					fScaleMaxBrightness;
	private Label					fLblMaxBrightnessValue;

	private ILegendProvider			fLegendProvider;
	private ValueColor[]			fValueColors;

	private LegendColor				fLegendColorWorkingCopy;

	private final SelectionAdapter	fDefaultSelectionAdapter;
	{
		fDefaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLegendData();
			}
		};
	}

	private final IDialogSettings	dialogSettings;
	{
		dialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	public LegendColorDialog(Shell parentShell, LegendProvider legendProvider) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

		fLegendProvider = legendProvider;
	}

	@Override
	public boolean close() {

		// keep selected value point
		dialogSettings.put(DIALOG_SETTINGS_SELECTED_VALUE_POINT, fComboValuePoint.getSelectionIndex());

		return super.close();
	}

	@Override
	public void create() {

		super.create();

		getShell().setText("Legend Color");

		/*
		 * initialize dialog by selecting select first value point
		 */

		// selecte previous value point
		try {
			fComboValuePoint.select(dialogSettings.getInt(DIALOG_SETTINGS_SELECTED_VALUE_POINT));
		} catch (NumberFormatException e) {
			fComboValuePoint.select(0);
		}

		updateUI();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

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

	private void createUI(Composite parent) {

		/*
		 * dialog container
		 */
		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

		/*
		 * legend
		 */
		fLegendCanvas = new Canvas(container, SWT.DOUBLE_BUFFERED);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(fLegendCanvas);
		fLegendCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				if (fLegendImage == null || fLegendImage.isDisposed()) {
					return;
				}

				e.gc.drawImage(fLegendImage, 0, 0);
			}
		});
		fLegendCanvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				// update image when the dialog is created
				updateLegendImage();
			}
		});
		fLegendCanvas.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				if (fLegendImage != null) {
					fLegendImage.dispose();
				}

			}
		});
		/*
		 * value container
		 */
		Composite valueContainer = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(valueContainer);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);

		createUIValuePointColor(valueContainer);
		createUIBrightness(valueContainer);

	}

	private void createUIBrightness(Composite parent) {

		Label label;

		Group group = new Group(parent, SWT.NONE);
		group.setText("Min/Max Brightness");
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(group);

		/*
		 * combobox: max brightness
		 */

		label = new Label(group, SWT.NONE);
		label.setText("Max Brightness:");
		label.setToolTipText("Set the brightness from the max value point to infinity");

		fComboMaxBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMaxBrightness.addSelectionListener(fDefaultSelectionAdapter);
		for (String comboLabel : LegendColor.BrightnessLabels) {
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
		label.setText("Min Brightness:");
		label.setToolTipText("Set the brightness from the min value point to infinity");

		fComboMinBrightness = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboMinBrightness.addSelectionListener(fDefaultSelectionAdapter);
		for (String comboLabel : LegendColor.BrightnessLabels) {
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

	private void createUIValuePointColor(Composite parent) {

		Label label;

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);

		/*
		 * combobox: range point
		 */

		label = new Label(container, SWT.NONE);
		label.setText("Value Point:");

		fComboValuePoint = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboValuePoint.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateUI();
			}
		});
		// revert sequence so that max is on top
		List<String> unitLabels = fLegendProvider.getLegendConfig().unitLabels;
		for (int unitIndex = unitLabels.size() - 1; unitIndex >= 0; unitIndex--) {
			fComboValuePoint.add(unitLabels.get(unitIndex));
		}

		/*
		 * group: value point color
		 */
		Group group = new Group(parent, SWT.NONE);
		group.setText("Value Point Color");
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 10).applyTo(group);

		/*
		 * red
		 */
		label = new Label(group, SWT.NONE);
		label.setText("Red:");

		fScaleRed = new Scale(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleRed);
		fScaleRed.setMinimum(0);
		fScaleRed.setMaximum(1000);
		fScaleRed.setPageIncrement(100);
		fScaleRed.addSelectionListener(fDefaultSelectionAdapter);

		fLblRedValue = new Label(group, SWT.NONE);
		fLblRedValue.setText(VALUE_SPACER);

		/*
		 * green
		 */
		label = new Label(group, SWT.NONE);
		label.setText("Green:");

		fScaleGreen = new Scale(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleGreen);
		fScaleGreen.setMinimum(0);
		fScaleGreen.setMaximum(1000);
		fScaleGreen.setPageIncrement(100);
		fScaleGreen.addSelectionListener(fDefaultSelectionAdapter);

		fLblGreenValue = new Label(group, SWT.NONE);
		fLblGreenValue.setText(VALUE_SPACER);

		/*
		 * blue
		 */
		label = new Label(group, SWT.NONE);
		label.setText("Blue:");

		fScaleBlue = new Scale(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleBlue);
		fScaleBlue.setMinimum(0);
		fScaleBlue.setMaximum(1000);
		fScaleBlue.setPageIncrement(100);
		fScaleBlue.addSelectionListener(fDefaultSelectionAdapter);

		fLblBlueValue = new Label(group, SWT.NONE);
		fLblBlueValue.setText(VALUE_SPACER);
	}

	private void enableControls() {

		final int minBrightness = fComboMinBrightness.getSelectionIndex();
		final int maxBrightness = fComboMaxBrightness.getSelectionIndex();

		fScaleMinBrightness.setEnabled(minBrightness != 0);
		fLblMinBrightnessValue.setEnabled(minBrightness != 0);

		fScaleMaxBrightness.setEnabled(maxBrightness != 0);
		fLblMaxBrightnessValue.setEnabled(maxBrightness != 0);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return dialogSettings;
	}

	public LegendColor getLegendColor() {
		return fLegendColorWorkingCopy;
	}

	/**
	 * Set the {@link LegendColor} which will be displayed in this dialog
	 * 
	 * @param legendColor
	 */
	public void setLegendColor(LegendColor legendColor) {

		/*
		 * use a copy of the legendColor to support the cancel feature
		 */
		fLegendColorWorkingCopy = legendColor.getCopy();
		fValueColors = fLegendColorWorkingCopy.valueColors;
	}

	/**
	 * update color labels from the scale value
	 */
	private void updateColorLabels() {

		fLblRedValue.setText(Integer.toString(fScaleRed.getSelection() / 10));
		fLblRedValue.pack(true);

		fLblGreenValue.setText(Integer.toString(fScaleGreen.getSelection() / 10));
		fLblGreenValue.pack(true);

		fLblBlueValue.setText(Integer.toString(fScaleBlue.getSelection() / 10));
		fLblBlueValue.pack(true);

		fLblMinBrightnessValue.setText(Integer.toString(fScaleMinBrightness.getSelection()));
		fLblMinBrightnessValue.pack(true);

		fLblMaxBrightnessValue.setText(Integer.toString(fScaleMaxBrightness.getSelection()));
		fLblMaxBrightnessValue.pack(true);
	}

	/**
	 * Update legend data from the UI
	 */
	private void updateLegendData() {

		int selectedRange = fComboValuePoint.getSelectionIndex();
		if (selectedRange == -1) {
			return;
		}

		int items = fComboValuePoint.getItemCount() - 1;
		ValueColor rangeColor = fValueColors[items - selectedRange];

		// update red/green/blue in the legend provider
		rangeColor.red = 255 * fScaleRed.getSelection() / 1000;
		rangeColor.green = 255 * fScaleGreen.getSelection() / 1000;
		rangeColor.blue = 255 * fScaleBlue.getSelection() / 1000;

		fLegendColorWorkingCopy.minBrightness = fComboMinBrightness.getSelectionIndex();
		fLegendColorWorkingCopy.minBrightnessFactor = fScaleMinBrightness.getSelection();
		fLegendColorWorkingCopy.maxBrightness = fComboMaxBrightness.getSelectionIndex();
		fLegendColorWorkingCopy.maxBrightnessFactor = fScaleMaxBrightness.getSelection();

		enableControls();
		updateColorLabels();
		updateLegendImage();
	}

	private void updateLegendImage() {

		final LegendColor legendColor = fLegendProvider.getLegendColor();
		legendColor.valueColors = fValueColors;
		legendColor.minBrightnessFactor = fLegendColorWorkingCopy.minBrightnessFactor;
		legendColor.maxBrightnessFactor = fLegendColorWorkingCopy.maxBrightnessFactor;
		legendColor.minBrightness = fLegendColorWorkingCopy.minBrightness;
		legendColor.maxBrightness = fLegendColorWorkingCopy.maxBrightness;

		createLegendImage();
		fLegendCanvas.redraw();
	}

	/**
	 * Update UI from legend data
	 */
	private void updateUI() {

		int selectedRange = fComboValuePoint.getSelectionIndex();
		if (selectedRange == -1) {
			return;
		}

		int items = fComboValuePoint.getItemCount() - 1;
		ValueColor rangeColor = fValueColors[items - selectedRange];

		// update red/green/blue scale from range color
		fScaleRed.setSelection(1000 * rangeColor.red / 255);
		fScaleGreen.setSelection(1000 * rangeColor.green / 255);
		fScaleBlue.setSelection(1000 * rangeColor.blue / 255);

		// update brightness
		fComboMinBrightness.select(fLegendColorWorkingCopy.minBrightness);
		fComboMaxBrightness.select(fLegendColorWorkingCopy.maxBrightness);
		fScaleMinBrightness.setSelection(fLegendColorWorkingCopy.minBrightnessFactor);
		fScaleMaxBrightness.setSelection(fLegendColorWorkingCopy.maxBrightnessFactor);

		updateColorLabels();
		enableControls();
	}

}
