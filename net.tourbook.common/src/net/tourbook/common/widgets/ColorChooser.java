/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.widgets;

import net.tourbook.common.Messages;

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

	private static final double	A_120					= 2 * Math.PI / 3;
	private static final double	SQRT_3					= Math.sqrt(3);
	private static final double	TWO_DIV_SQRT_3			= 2. / SQRT_3;

	private static final double	SINUS_120				= Math.sin(A_120);
	private static final double	SINUS_240				= -SINUS_120;
	private static final double	COSINUS120				= Math.cos(A_120);
	private static final double	COSINUS_240				= COSINUS120;

	private RGB					_chooserRGB;
	private int					_chooserSize			= 0;
	private int					_chooserRadius			= 0;
	private int					_hexagonRadius			= 0;

	private int					_col3					= 0;

	private int					_scaleValueRed			= 0;
	private int					_scaleValueGreen		= 0;
	private int					_scaleValueBlue			= 0;
	private int					_scaleValueHue			= 0;
	private int					_scaleValueSaturation	= 0;
	private int					_scaleValueBrightness	= 0;

	private boolean				_hexagonChangeState		= false;

	/*
	 * UI controls
	 */
	private TabFolder			_tabFolder;
	private ImageCanvas			_hexagonCanvas;

	private Label				_lblChoosedColor;
	private Scale				_scaleRed;
	private Scale				_scaleGreen;
	private Scale				_scaleBlue;
	private Scale				_scaleHue;
	private Scale				_scaleSaturation;
	private Scale				_scaleBrightness;

	public ColorChooser(final Composite parent, final int style) {
		super(parent, style);
		setSize(330);
		createUI(parent);
	}

	public void chooseRGBFromHexagon(final MouseEvent e) {
		final int x = e.x - _chooserRadius;
		final int y = e.y - _chooserRadius;
		_chooserRGB = getRGBFromHexagon(x, y);
		updateUI();
	}

	private void createUI(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(this);
		{
			// choosed Color Label
			_lblChoosedColor = new Label(this, SWT.BORDER | SWT.SHADOW_NONE);
			GridDataFactory.fillDefaults().hint(SWT.DEFAULT, _chooserSize / 4).applyTo(_lblChoosedColor);
			_lblChoosedColor.setToolTipText(Messages.color_chooser_choosed_color);

			createUITabs(this);
		}

		// set selected color
		_chooserRGB = new RGB(_scaleValueRed, _scaleValueGreen, _scaleValueBlue);
		updateChoosedColorButton();
	}

	private void createUITabs(final Composite parent) {

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);

		/*
		 * tabfolder
		 */
		_tabFolder = new TabFolder(parent, SWT.NONE);

		final TabItem hexagonTab = new TabItem(_tabFolder, SWT.NONE);
		hexagonTab.setText(Messages.color_chooser_hexagon);

		final TabItem rgbTab = new TabItem(_tabFolder, SWT.NONE);
		rgbTab.setText(Messages.color_chooser_rgb);

		/*
		 * Hexagon-Tab
		 */
		final Composite hexagonContainer = new Composite(_tabFolder, SWT.NONE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(hexagonContainer);
		GridLayoutFactory.swtDefaults().applyTo(hexagonContainer);
		{
			final Image hexagonImage = new Image(hexagonContainer.getDisplay(), _chooserSize, _chooserSize);
			paintHexagon(hexagonImage);

			_hexagonCanvas = new ImageCanvas(hexagonContainer, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().hint(_chooserSize, _chooserSize).applyTo(_hexagonCanvas);

			_hexagonCanvas.setImage(hexagonImage);
			_hexagonCanvas.setToolTipText(Messages.color_chooser_hexagon_move);
			_hexagonCanvas.addMouseListener(new MouseListener() {
				public void mouseDoubleClick(final MouseEvent e) {}

				public void mouseDown(final MouseEvent e) {
					_hexagonChangeState = true;
					chooseRGBFromHexagon(e);
				}

				public void mouseUp(final MouseEvent e) {
					_hexagonChangeState = false;
				}
			});
			_hexagonCanvas.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(final MouseEvent e) {
					if (!_hexagonChangeState) {
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
					_col3 = new Integer(hexagonSlider.getSelection()).intValue();
					paintHexagon(hexagonImage);
					_hexagonCanvas.redraw();
				}
			});
			hexagonTab.setControl(hexagonContainer);
		}

		/*
		 * RGB/HSB-Tab
		 */
		final Composite rgbComposite = new Composite(_tabFolder, SWT.NONE);

		GridDataFactory.fillDefaults().grab(false, false).applyTo(rgbComposite);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(rgbComposite);
		{
			/*
			 * red
			 */
			final Label redLabel = new Label(rgbComposite, SWT.NONE);
			redLabel.setText(Messages.color_chooser_red);
			_scaleRed = new Scale(rgbComposite, SWT.BORDER);
			_scaleRed.setMinimum(0);
			_scaleRed.setMaximum(255);
			_scaleRed.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			_scaleRed.setLayoutData(gd);
			_scaleRed.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueRed = new Integer(_scaleRed.getSelection()).intValue();
					_chooserRGB = new RGB(_scaleValueRed, _scaleValueGreen, _scaleValueBlue);
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
			_scaleGreen = new Scale(rgbComposite, SWT.BORDER);
			_scaleGreen.setMinimum(0);
			_scaleGreen.setMaximum(255);
			_scaleGreen.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			_scaleGreen.setLayoutData(gd);
			_scaleGreen.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueGreen = new Integer(_scaleGreen.getSelection()).intValue();
					_chooserRGB = new RGB(_scaleValueRed, _scaleValueGreen, _scaleValueBlue);
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
			_scaleBlue = new Scale(rgbComposite, SWT.BORDER);
			_scaleBlue.setMinimum(0);
			_scaleBlue.setMaximum(255);
			_scaleBlue.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
			_scaleBlue.setLayoutData(gd);
			_scaleBlue.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueBlue = new Integer(_scaleBlue.getSelection()).intValue();
					_chooserRGB = new RGB(_scaleValueRed, _scaleValueGreen, _scaleValueBlue);
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
			_scaleHue = new Scale(rgbComposite, SWT.BORDER);
			_scaleHue.setMinimum(0);
			_scaleHue.setMaximum(360);
			_scaleHue.setLayoutData(gd);
			_scaleHue.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueHue = new Integer(_scaleHue.getSelection()).intValue();
					_chooserRGB = new RGB(
							_scaleValueHue,
							(float) _scaleValueSaturation / 100,
							(float) _scaleValueBrightness / 100);
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
			_scaleSaturation = new Scale(rgbComposite, SWT.BORDER);
			_scaleSaturation.setMinimum(0);
			_scaleSaturation.setMaximum(100);
			_scaleSaturation.setLayoutData(gd);
			_scaleSaturation.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueSaturation = new Integer(_scaleSaturation.getSelection()).intValue();
					_chooserRGB = new RGB(
							_scaleValueHue,
							(float) _scaleValueSaturation / 100,
							(float) _scaleValueBrightness / 100);
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
			_scaleBrightness = new Scale(rgbComposite, SWT.BORDER);
			_scaleBrightness.setMinimum(0);
			_scaleBrightness.setMaximum(100);
			_scaleBrightness.setLayoutData(gd);
			_scaleBrightness.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					_scaleValueBrightness = new Integer(_scaleBrightness.getSelection()).intValue();
					_chooserRGB = new RGB(
							_scaleValueHue,
							(float) _scaleValueSaturation / 100,
							(float) _scaleValueBrightness / 100);
					updateChoosedColorButton();
					updateScales();
					updateScaleValuesRGB();
				}
			});
		}

		_tabFolder.pack();
	}

	public RGB getRGB() {
		return _chooserRGB;
	}

	private RGB getRGBFromHexagon(final int x, final int y) {

		final double a = Math.atan2(y, x);
		int sector, xr, yr;
		// rotate sector to positive y
		if (a < -A_120 || a > A_120) {
			sector = 2;
			xr = (int) (x * COSINUS_240 - y * SINUS_240);
			yr = (int) (x * SINUS_240 + y * COSINUS_240);
		} else if (a < 0) {
			sector = 1;
			xr = (int) (x * COSINUS120 - y * SINUS_120);
			yr = (int) (x * SINUS_120 + y * COSINUS120);
		} else {
			sector = 0;
			xr = x;
			yr = y;
		}
		// shear sector to square in positive x to ask for the borders
		final int xs = (int) (xr + yr / SQRT_3);
		final int ys = (int) (yr * TWO_DIV_SQRT_3);
		if (xs >= 0 && xs < _hexagonRadius && ys >= 0 && ys < _hexagonRadius) {
			final int col1 = (255 * xs / _hexagonRadius);
			final int col2 = (255 * ys / _hexagonRadius);
			switch (sector) {
			case 0:
				return new RGB(_col3, col2, col1);
			case 1:
				return new RGB(col1, _col3, col2);
			case 2:
				return new RGB(col2, col1, _col3);
			}
		}

		// return grey
		return new RGB(127, 127, 127);

	}

	private void paintHexagon(final Image hexagonImage) {

		final GC gc = new GC(hexagonImage);
		{
			for (int x = -_chooserRadius; x < _chooserRadius; x++) {
				for (int y = -_chooserRadius; y < _chooserRadius; y++) {

					final Color fgColor = new Color(this.getDisplay(), getRGBFromHexagon(x, y));
					{
						gc.setForeground(fgColor);
						gc.drawPoint(x + _chooserRadius, y + _chooserRadius);
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
		_chooserRGB = rgb;
		updateUI();
	}

	private void setSize(final int size) {
		_chooserSize = size;
		_chooserRadius = _chooserSize / 2;
		_hexagonRadius = (int) (_chooserSize / 2.2);
	}

	private void updateChoosedColorButton() {
		final Color color = new Color(Display.getCurrent(), _chooserRGB);
		{
			_lblChoosedColor.setBackground(color);
			_lblChoosedColor.setForeground(color);
		}
		color.dispose();
	}

	private void updateScales() {

		_scaleRed.setSelection(_chooserRGB.red);
		_scaleGreen.setSelection(_chooserRGB.green);
		_scaleBlue.setSelection(_chooserRGB.blue);

		final float hsb[] = _chooserRGB.getHSB();
		_scaleHue.setSelection((int) hsb[0]);
		_scaleSaturation.setSelection((int) (hsb[1] * 100));
		_scaleBrightness.setSelection((int) (hsb[2] * 100));
	}

	private void updateScaleValuesHSB() {
		final float hsb[] = _chooserRGB.getHSB();
		_scaleValueHue = (int) hsb[0];
		_scaleValueSaturation = (int) (hsb[1] * 100);
		_scaleValueBrightness = (int) (hsb[2] * 100);
	}

	private void updateScaleValuesRGB() {
		_scaleValueRed = _chooserRGB.red;
		_scaleValueGreen = _chooserRGB.green;
		_scaleValueBlue = _chooserRGB.blue;
	}

	private void updateUI() {
		updateChoosedColorButton();
		updateScales();
		updateScaleValuesRGB();
		updateScaleValuesHSB();
	}

}
