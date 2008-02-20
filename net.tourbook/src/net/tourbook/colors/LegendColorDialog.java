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
package net.tourbook.colors;

import java.util.Arrays;
import java.util.List;

import net.tourbook.mapping.ILegendProvider;
import net.tourbook.mapping.LegendColor;
import net.tourbook.mapping.LegendConfig;
import net.tourbook.mapping.LegendProvider;
import net.tourbook.mapping.TourPainter;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

public class LegendColorDialog extends Dialog {

	private Scale						fScaleRed;
	private Scale						fScaleGreen;
	private Scale						fScaleBlue;
	private Canvas						fLegendCanvas;
	private Image						fLegendImage;

	private static final List<Integer>	fUnitValues	= Arrays.asList(10, 50, 100, 150, 190);
	private static final List<String>	fUnitLabels	= Arrays.asList("min", "low", "mid", "high", "max");

	private ILegendProvider				fLegendProvider;

	public LegendColorDialog(Shell parentShell) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public boolean close() {

		if (fLegendImage != null) {
			fLegendImage.dispose();
		}

		return super.close();
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		fLegendProvider = new LegendProvider(new LegendConfig(), new LegendColor(), 0);
		updateLegendProvider();

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

			TourPainter.drawLegendColors(gc, imageBounds, fLegendProvider);
		}
		gc.dispose();
		transparentColor.dispose();
	}

	@Override
	public void create() {

		super.create();

//		fLegendCanvas.layout(true);
	}

	private void createUI(Composite parent) {

		Label label;

		final SelectionAdapter defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateLegendImage();
			}
		};

		Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
//		container.addControlListener(new ControlAdapter() {
//			@Override
//			public void controlResized(ControlEvent e) {
////				System.out.println(fLegendCanvas.getSize());
//				onSelectScale();
//			}
//		});

		/*
		 * legend
		 */
		fLegendCanvas = new Canvas(container, SWT.DOUBLE_BUFFERED);
		GridDataFactory.fillDefaults().grab(false, true).applyTo(fLegendCanvas);
		fLegendCanvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				onPaintLegend(e.gc);
			}
		});
		fLegendCanvas.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				updateLegendImage();
			}
		});

		Composite valueContainer = new Composite(container, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(valueContainer);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(valueContainer);

		/*
		 * red
		 */
		label = new Label(valueContainer, SWT.NONE);
		label.setText("Red:");
		fScaleRed = new Scale(valueContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleRed);
		fScaleRed.setMinimum(0);
		fScaleRed.setMaximum(255);
		fScaleRed.setPageIncrement(10);
		fScaleRed.addSelectionListener(defaultSelectionAdapter);

		/*
		 * green
		 */
		label = new Label(valueContainer, SWT.NONE);
		label.setText("Green:");
		fScaleGreen = new Scale(valueContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleGreen);
		fScaleGreen.setMinimum(0);
		fScaleGreen.setMaximum(255);
		fScaleGreen.setPageIncrement(10);
		fScaleGreen.addSelectionListener(defaultSelectionAdapter);

		/*
		 * blue
		 */
		label = new Label(valueContainer, SWT.NONE);
		label.setText("Blue:");
		fScaleBlue = new Scale(valueContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleBlue);
		fScaleBlue.setMinimum(0);
		fScaleBlue.setMaximum(255);
		fScaleBlue.setPageIncrement(10);
		fScaleBlue.addSelectionListener(defaultSelectionAdapter);

	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}

	private void onPaintLegend(GC gc) {

		if (fLegendImage == null || fLegendImage.isDisposed()) {
			return;
		}

		gc.drawImage(fLegendImage, 0, 0);
	}

	private void updateLegendImage() {
		updateLegendProvider();
		createLegendImage();
		fLegendCanvas.redraw();
	}

	private void updateLegendProvider() {

		LegendConfig legendConfig = fLegendProvider.getLegendConfig();
		legendConfig.units = fUnitValues;
		legendConfig.unitLabels = fUnitLabels;
		legendConfig.legendMinValue = 0;
		legendConfig.legendMaxValue = 200;
		legendConfig.unitText = "";

		LegendColor legendColor = fLegendProvider.getLegendColor();
		legendColor.minValue = 10;
		legendColor.lowValue = 50;
		legendColor.midValue = 100;
		legendColor.highValue = 150;
		legendColor.maxValue = 190;

		legendColor.dimmFactor = 0.5F;

		legendColor.maxColor1 = fScaleRed.getSelection();
		legendColor.maxColor2 = fScaleGreen.getSelection();
		legendColor.maxColor3 = fScaleBlue.getSelection();

		legendColor.color1 = LegendColor.COLOR_RED;
		legendColor.color2 = LegendColor.COLOR_GREEN;
		legendColor.color3 = LegendColor.COLOR_BLUE;

	}

}
