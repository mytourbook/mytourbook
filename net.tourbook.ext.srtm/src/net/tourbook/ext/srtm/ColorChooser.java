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
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class ColorChooser extends Composite {

	private int					chooserSize				= 0;
	private int					chooserRadius			= 0;
	private int					hexagonRadius			= 0;

	private static final double	a120					= 2 * Math.PI / 3;
	private static final double	sqrt3					= Math.sqrt(3);
	private static final double	twodivsqrt3				= 2. / sqrt3;
	private static final double	sin120					= Math.sin(a120);
	private static final double	cos120					= Math.cos(a120);
	private static final double	sin240					= -sin120;
	private static final double	cos240					= cos120;

	private int					col3					= 0;

	private TabFolder			fTabFolder;
	private ImageCanvas			fHexagonCanvas;
	private RGB					choosedRGB;
	private Label				choosedColorLabel;
	private Scale				redScale;
	private Scale				greenScale;
	private Scale				blueScale;
	private Scale				hueScale;
	private Scale				saturationScale;
	private Scale				brightnessScale;

 	private int					scaleValueRed			= 0;
	private int					scaleValueGreen			= 0;
	private int					scaleValueBlue			= 0;
	private int					scaleValueHue			= 0;
	private int					scaleValueSaturation	= 0;
	private int					scaleValueBrightness	= 0;

	private boolean				hexagonChangeState		= false;

	public ColorChooser(final Composite parent, final int style) {
		super(parent, style);
		setSize(330);
		createUI(parent);
	}

	public void chooseRGBFromHexagon(final MouseEvent e) {
		final int x = e.x - chooserRadius;
		final int y = e.y - chooserRadius;
		choosedRGB = getRGBFromHexagon(x, y);
		updateUI();
	}

	private void createUI(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(this);
		{
			// choosed Color Label
			choosedColorLabel = new Label(this, SWT.BORDER | SWT.SHADOW_NONE);
			GridDataFactory.fillDefaults().hint(SWT.DEFAULT, chooserSize / 4).applyTo(choosedColorLabel);
			choosedColorLabel.setToolTipText(Messages.color_chooser_choosed_color);

			createUITabs(this);
		}

		// set selected color
		choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
		updateChoosedColorButton();
	}

	private void createUITabs(final Composite parent) {

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

		/*
		 * tabfolder
		 */
		fTabFolder = new TabFolder(parent, SWT.NONE);

		final TabItem hexagonTab = new TabItem(fTabFolder, SWT.NONE);
		hexagonTab.setText(Messages.color_chooser_hexagon);

		final TabItem rgbTab = new TabItem(fTabFolder, SWT.NONE);
		rgbTab.setText(Messages.color_chooser_rgb);

		/*
		 * Hexagon-Tab
		 */
		final Composite hexagonContainer = new Composite(fTabFolder, SWT.NONE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(hexagonContainer);
		GridLayoutFactory.swtDefaults().applyTo(hexagonContainer);
		{
			final Image hexagonImage = new Image(hexagonContainer.getDisplay(), chooserSize, chooserSize);
			paintHexagon(hexagonImage);

			fHexagonCanvas = new ImageCanvas(hexagonContainer, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().hint(chooserSize, chooserSize).applyTo(fHexagonCanvas);

			fHexagonCanvas.setImage(hexagonImage);
			fHexagonCanvas.setToolTipText(Messages.color_chooser_hexagon_move);
			fHexagonCanvas.addMouseListener(new MouseListener() {
				public void mouseDoubleClick(final MouseEvent e) {}

				public void mouseDown(final MouseEvent e) {
					hexagonChangeState = true;
					chooseRGBFromHexagon(e);
				}

				public void mouseUp(final MouseEvent e) {
					hexagonChangeState = false;
				}
			});
			fHexagonCanvas.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(final MouseEvent e) {
					if (!hexagonChangeState) {
						return;
					}
					chooseRGBFromHexagon(e);
				}
			});

			final Slider hexagonSlider = new Slider(hexagonContainer, SWT.HORIZONTAL);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(hexagonSlider);
			hexagonSlider.setMinimum(0);
			hexagonSlider.setMaximum(255);
			hexagonSlider.setIncrement(8);

			hexagonSlider.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event event) {
					col3 = new Integer(hexagonSlider.getSelection()).intValue();
					paintHexagon(hexagonImage);
					fHexagonCanvas.redraw();
				}
			});
			hexagonTab.setControl(hexagonContainer);
		}

		/*
		 * RGB/HSB-Tab
		 */
		final Composite rgbComposite = new Composite(fTabFolder, SWT.NONE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(rgbComposite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(rgbComposite);
		{
			/*
			 * red
			 */
			final Label redLabel = new Label(rgbComposite, SWT.NONE);
			redLabel.setText(Messages.color_chooser_red);
			redScale = new Scale(rgbComposite, SWT.BORDER);
			redScale.setMinimum(0);
			redScale.setMaximum(255);
			redScale.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			redScale.setLayoutData(gd);
			redScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueRed = new Integer(redScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesHSB();
				}
			});

			/*
			 * green
			 */
			final Label greenLabel = new Label(rgbComposite, SWT.NONE);
			greenLabel.setText(Messages.color_chooser_green);
			greenScale = new Scale(rgbComposite, SWT.BORDER);
			greenScale.setMinimum(0);
			greenScale.setMaximum(255);
			greenScale.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			greenScale.setLayoutData(gd);
			greenScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueGreen = new Integer(greenScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesHSB();
				}
			});

			/*
			 * blue
			 */
			final Label blueLabel = new Label(rgbComposite, SWT.NONE);
			blueLabel.setText(Messages.color_chooser_blue);
			blueScale = new Scale(rgbComposite, SWT.BORDER);
			blueScale.setMinimum(0);
			blueScale.setMaximum(255);
			blueScale.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			blueScale.setLayoutData(gd);
			blueScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueBlue = new Integer(blueScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueRed, scaleValueGreen, scaleValueBlue);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesHSB();
				}
			});
			rgbTab.setControl(rgbComposite);

			// hue        - the hue        value for the HSB color (from 0 to 360)
			// saturation - the saturation value for the HSB color (from 0 to 1)
			// brightness - the brightness value for the HSB color (from 0 to 1)

			/*
			 * HUE
			 */
			final Label hueLabel = new Label(rgbComposite, SWT.NONE);
			hueLabel.setText(Messages.color_chooser_hue);
			hueScale = new Scale(rgbComposite, SWT.BORDER);
			hueScale.setMinimum(0);
			hueScale.setMaximum(360);
			hueScale.setLayoutData(gd);
			hueScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueHue = new Integer(hueScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueHue,
							(float) scaleValueSaturation / 100,
							(float) scaleValueBrightness / 100);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesRGB();
				}
			});

			/*
			 * saturation
			 */
			final Label saturationLabel = new Label(rgbComposite, SWT.NONE);
			saturationLabel.setText(Messages.color_chooser_saturation);
			saturationScale = new Scale(rgbComposite, SWT.BORDER);
			saturationScale.setMinimum(0);
			saturationScale.setMaximum(100);
			saturationScale.setLayoutData(gd);
			saturationScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueSaturation = new Integer(saturationScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueHue,
							(float) scaleValueSaturation / 100,
							(float) scaleValueBrightness / 100);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesRGB();
				}
			});

			/*
			 * brightness
			 */
			final Label brightnessLabel = new Label(rgbComposite, SWT.NONE);
			brightnessLabel.setText(Messages.color_chooser_brightness);
			brightnessScale = new Scale(rgbComposite, SWT.BORDER);
			brightnessScale.setMinimum(0);
			brightnessScale.setMaximum(100);
			brightnessScale.setLayoutData(gd);
			brightnessScale.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					scaleValueBrightness = new Integer(brightnessScale.getSelection()).intValue();
					choosedRGB = new RGB(scaleValueHue,
							(float) scaleValueSaturation / 100,
							(float) scaleValueBrightness / 100);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesRGB();
				}
			});
		}

		fTabFolder.pack();
	}

	public RGB getRGB() {
		return choosedRGB;
	}

	private RGB getRGBFromHexagon(final int x, final int y) {

		final double a = Math.atan2(y, x);
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
		final int xs = (int) (xr + yr / sqrt3);
		final int ys = (int) (yr * twodivsqrt3);
		if (xs >= 0 && xs < hexagonRadius && ys >= 0 && ys < hexagonRadius) {
			final int col1 = (255 * xs / hexagonRadius);
			final int col2 = (255 * ys / hexagonRadius);
			switch (sector) {
			case 0:
				return new RGB(col3, col2, col1);
			case 1:
				return new RGB(col1, col3, col2);
			case 2:
				return new RGB(col2, col1, col3);
			}
		}

		// return widget background color
		return Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
	}

	private void paintHexagon(final Image hexagonImage) {

		final GC gc = new GC(hexagonImage);
		{
			for (int x = -chooserRadius; x < chooserRadius; x++) {
				for (int y = -chooserRadius; y < chooserRadius; y++) {

					final Color fgColor = new Color(this.getDisplay(), getRGBFromHexagon(x, y));
					{
						gc.setForeground(fgColor);
						gc.drawPoint(x + chooserRadius, y + chooserRadius);
					}
					fgColor.dispose();
				}
			}
		}
		gc.dispose();
	}

	/**
	 * Set RGB for the color chooser
	 * 
	 * @param rgb
	 */
	public void setRGB(final RGB rgb) {
		choosedRGB = rgb;
		updateUI();
	}

	private void setSize(final int size) {
		chooserSize = size;
		chooserRadius = chooserSize / 2;
		hexagonRadius = (int) (chooserSize / 2.2);
	}

	private void updateChoosedColorButton() {
		final Color color = new Color(Display.getCurrent(), choosedRGB);
		{
			choosedColorLabel.setBackground(color);
			choosedColorLabel.setForeground(color);
		}
		color.dispose();
	}

	private void updateScales() {

		redScale.setSelection(choosedRGB.red);
		greenScale.setSelection(choosedRGB.green);
		blueScale.setSelection(choosedRGB.blue);

		final float hsb[] = choosedRGB.getHSB();
		hueScale.setSelection((int) hsb[0]);
		saturationScale.setSelection((int) (hsb[1] * 100));
		brightnessScale.setSelection((int) (hsb[2] * 100));
	}

	private void updateScaleValuesHSB() {
		final float hsb[] = choosedRGB.getHSB();
		scaleValueHue = (int) hsb[0];
		scaleValueSaturation = (int) (hsb[1] * 100);
		scaleValueBrightness = (int) (hsb[2] * 100);
	}

	private void updateScaleValuesRGB() {
		scaleValueRed = choosedRGB.red;
		scaleValueGreen = choosedRGB.green;
		scaleValueBlue = choosedRGB.blue;
	}

	private void updateUI() {
		updateChoosedColorButton();
		updateScales();
		updateScaleValuesRGB();
		updateScaleValuesHSB();
	}

}
