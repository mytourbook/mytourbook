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
/**
 * @author Alfred Barten
 */
package net.tourbook.ext.srtm;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class ColorChooser {

	private int					chooserSize		= 0;
	private int					chooserRadius	= 0;
	private int					hexagonRadius	= 0;
	private static final double	a120			= 2 * Math.PI / 3;
	private static final double	sqrt3			= Math.sqrt(3);
	private static final double	twodivsqrt3		= 2. / sqrt3;
	private static final double	sin120			= Math.sin(a120);
	private static final double	cos120			= Math.cos(a120);
	private static final double	sin240			= -sin120;
	private static final double	cos240			= cos120;
	private int					col3			= 0;
	private Composite			composite;
	private GC					gc;
	private Display				chooserDisplay;
	private Label				hexagonLabel;
	private RGB					choosedRGB;
	private Button				choosedColorButton;
	private Scale               redScale;
	private Scale               greenScale;
	private Scale               blueScale;
	private Scale    	        hueScale;
	private Scale    	        saturationScale;
	private Scale    	        brightnessScale;
	private int                 scaleValueRed   = 0;
	private int                 scaleValueGreen = 0;
	private int                 scaleValueBlue  = 0;
	private int                 scaleValueHue   = 0;
	private int                 scaleValueSaturation = 0;
	private int                 scaleValueBrightness = 0;	
	
	public ColorChooser(Composite composite) {
		this.composite = composite;
		setSize(330);
	}

	private RGB getRGBFromHexagon(int x, int y) {

		double a = Math.atan2(y, x);
		int sector, xr, yr;
		// rotate sector to positive y
		if (a < -a120 || a > a120) {
			sector = 2;
			xr = (int) (x * cos240 - y * sin240);
			yr = (int) (x * sin240 + y * cos240);
		} else if (a < 0) {
			sector = 1;
			xr = (int) (x * cos120 - y * sin120);
			yr = (int) (x * sin120 + y * cos120);
		} else {
			sector = 0;
			xr = x;
			yr = y;
		}
		// shear sector to square in positive x to ask for the borders
		int xs = (int) (xr + yr / sqrt3);
		int ys = (int) (yr * twodivsqrt3);
		if (xs >= 0 && xs < hexagonRadius && ys >= 0 && ys < hexagonRadius) {
			int col1 = (int) (255 * xs / hexagonRadius);
			int col2 = (int) (255 * ys / hexagonRadius);
			switch (sector) {
			case 0:
				return new RGB(col3, col2, col1);
			case 1:
				return new RGB(col1, col3, col2);
			case 2:
				return new RGB(col2, col1, col3);
			}			
		}
		return new RGB(0, 0, 0);
	}

	private void setHexagon() {
		for (int x = -chooserRadius; x < chooserRadius; x++) {
			for (int y = -chooserRadius; y < chooserRadius; y++) {
				gc.setForeground(new Color(chooserDisplay, getRGBFromHexagon(x, y)));
				gc.drawPoint(x + chooserRadius, y + chooserRadius);
			}
		}
	}

	public void setChooser() {
		
		GridDataFactory.fillDefaults().grab(false, false).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(composite);

		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1);
		gridData.widthHint = chooserSize - 50;
		
		// choosed Color Button
		choosedColorButton = new Button(composite, SWT.PUSH);
		choosedColorButton.setLayoutData(gridData);
		choosedColorButton.setToolTipText(Messages.color_chooser_choosed_color);
		choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
		updateChoosedColorButton(composite.getDisplay());

		final TabFolder tabFolder = new TabFolder(composite, SWT.BORDER);
    	TabItem hexagonTab = new TabItem(tabFolder, SWT.NONE);
		hexagonTab.setText(Messages.color_chooser_hexagon);
    	TabItem rgbTab = new TabItem(tabFolder, SWT.NONE);
		rgbTab.setText(Messages.color_chooser_rgb);
    	TabItem hsbTab = new TabItem(tabFolder, SWT.NONE);
		hsbTab.setText(Messages.color_chooser_hsb);
		
		// Hexagon-Tab
		final Composite	hexagonComposite = new Composite(tabFolder, SWT.NONE);
		
		GridDataFactory.fillDefaults().grab(false, false).applyTo(hexagonComposite);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(hexagonComposite);

		final Image hexagonImage = new Image(hexagonComposite.getDisplay(), chooserSize, chooserSize);
		gc = new GC(hexagonImage);

		setHexagon();

		hexagonLabel = new Label(hexagonComposite, SWT.CENTER);
		hexagonLabel.setImage(hexagonImage);
		hexagonLabel.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {}

			public void mouseDown(MouseEvent e) {
				int x = e.x - chooserRadius;
				int y = e.y - chooserRadius;
				choosedRGB = getRGBFromHexagon(x, y);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesRGB();
				updateScaleValuesHSB();
			}

			public void mouseUp(MouseEvent e) {}
		});

		final Slider hexagonSlider = new Slider(hexagonComposite, SWT.HORIZONTAL);
		hexagonSlider.setMinimum(0);
		hexagonSlider.setMaximum(255);
		hexagonSlider.setIncrement(8);
		hexagonSlider.setLayoutData(gridData);

//		slider.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
		hexagonSlider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				col3 = new Integer(hexagonSlider.getSelection()).intValue();
				setHexagon();
				hexagonLabel.setImage(hexagonImage);
			}
		});
		hexagonTab.setControl(hexagonComposite);

		// RGB-Tab
		final Composite	rgbComposite = new Composite(tabFolder, SWT.NONE);
		
		GridDataFactory.fillDefaults().grab(false, false).applyTo(rgbComposite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(rgbComposite);

		final Label redLabel = new Label(rgbComposite, SWT.NONE);
		redLabel.setText(Messages.color_chooser_red);
		redScale = new Scale(rgbComposite, SWT.BORDER);
		redScale.setMinimum(0);
		redScale.setMaximum(255);
		redScale.setBackground(new Color(rgbComposite.getDisplay(), 255, 0, 0));
		redScale.setLayoutData(gridData);
		redScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueRed = new Integer(redScale.getSelection()).intValue();
				choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesHSB();
			}
		});
		final Label greenLabel = new Label(rgbComposite, SWT.NONE);
		greenLabel.setText(Messages.color_chooser_green);
		greenScale = new Scale(rgbComposite, SWT.BORDER);
		greenScale.setMinimum(0);
		greenScale.setMaximum(255);
		greenScale.setBackground(new Color(rgbComposite.getDisplay(), 0, 255, 0));
		greenScale.setLayoutData(gridData);
		greenScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueGreen = new Integer(greenScale.getSelection()).intValue();
				choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesHSB();
			}
		});
		final Label blueLabel = new Label(rgbComposite, SWT.NONE);
		blueLabel.setText(Messages.color_chooser_blue);
		blueScale = new Scale(rgbComposite, SWT.BORDER);
		blueScale.setMinimum(0);
		blueScale.setMaximum(255);
		blueScale.setBackground(new Color(rgbComposite.getDisplay(), 0, 0, 255));
		blueScale.setLayoutData(gridData);
		blueScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueBlue = new Integer(blueScale.getSelection()).intValue();
				choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesHSB();
			}
		});		
		rgbTab.setControl(rgbComposite);
		
		// HSB-Tab
//	    hue        - the hue        value for the HSB color (from 0 to 360)
//	    saturation - the saturation value for the HSB color (from 0 to 1)
//	    brightness - the brightness value for the HSB color (from 0 to 1) 
		final Composite	hsbComposite = new Composite(tabFolder, SWT.NONE);
		
		GridDataFactory.fillDefaults().grab(false, false).applyTo(hsbComposite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(hsbComposite);

		final Label hueLabel = new Label(hsbComposite, SWT.NONE);
		hueLabel.setText(Messages.color_chooser_hue);
		hueScale = new Scale(hsbComposite, SWT.BORDER);
		hueScale.setMinimum(0);
		hueScale.setMaximum(360);
		hueScale.setLayoutData(gridData);
		hueScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueHue = new Integer(hueScale.getSelection()).intValue();
				choosedRGB = new RGB((float)scaleValueHue, (float)scaleValueSaturation/100, (float)scaleValueBrightness/100);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesRGB();
			}
		});
		final Label saturationLabel = new Label(hsbComposite, SWT.NONE);
		saturationLabel.setText(Messages.color_chooser_saturation);
		saturationScale = new Scale(hsbComposite, SWT.BORDER);
		saturationScale.setMinimum(0);
		saturationScale.setMaximum(100);
		saturationScale.setLayoutData(gridData);
		saturationScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueSaturation = new Integer(saturationScale.getSelection()).intValue();
				choosedRGB = new RGB((float)scaleValueHue, (float)scaleValueSaturation/100, (float)scaleValueBrightness/100);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesRGB();
			}
		});
		final Label brightnessLabel = new Label(hsbComposite, SWT.NONE);
		brightnessLabel.setText(Messages.color_chooser_brightness);
		brightnessScale = new Scale(hsbComposite, SWT.BORDER);
		brightnessScale.setMinimum(0);
		brightnessScale.setMaximum(100);
		brightnessScale.setLayoutData(gridData);
		brightnessScale.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				scaleValueBrightness = new Integer(brightnessScale.getSelection()).intValue();
				choosedRGB = new RGB((float)scaleValueHue, (float)scaleValueSaturation/100, (float)scaleValueBrightness/100);
				updateChoosedColorButton(e.display);
				updateScales();
				updateScaleValuesRGB();				
			}
		});		
		hsbTab.setControl(hsbComposite);

		tabFolder.pack();
		
	}

	private void updateScales() {
		redScale.setSelection(choosedRGB.red);
		greenScale.setSelection(choosedRGB.green);
		blueScale.setSelection(choosedRGB.blue);
		float hsb[] = choosedRGB.getHSB();
		hueScale.setSelection((int)hsb[0]);
		saturationScale.setSelection((int)(hsb[1]*100));
		brightnessScale.setSelection((int)(hsb[2]*100));
	}
	
	private void updateChoosedColorButton(Display display) {
		Color color = new Color(display, choosedRGB);
		choosedColorButton.setBackground(color);		
		choosedColorButton.setForeground(color);
	}
	
	private void updateScaleValuesRGB() {
		scaleValueRed = choosedRGB.red;
		scaleValueGreen = choosedRGB.green;
		scaleValueBlue = choosedRGB.blue;
	}
	
	private void updateScaleValuesHSB() {
		float hsb[] = choosedRGB.getHSB();
		scaleValueHue = (int)hsb[0];
		scaleValueSaturation = (int)(hsb[1]*100);
		scaleValueBrightness = (int)(hsb[2]*100);
	}
	
	public RGB getRGB() {
		return choosedRGB;
	}

	public void setSize(int size) {
		this.chooserSize = size;
		chooserRadius = chooserSize / 2;
		hexagonRadius = (int) (chooserSize / 2.2);
	}

}
