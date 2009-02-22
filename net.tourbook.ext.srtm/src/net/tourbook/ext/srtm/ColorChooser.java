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
import org.eclipse.swt.widgets.Slider;

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
	private Label				chooserLabel;
	private RGB					choosedRGB;
	private Button				choosedColorButton;

	public ColorChooser(Composite composite) {
		this.composite = composite;
		setSize(330);
	}

	private RGB getRGB(int x, int y) {

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
				gc.setForeground(new Color(chooserDisplay, getRGB(x, y)));
				gc.drawPoint(x + chooserRadius, y + chooserRadius);
			}
		}
	}

	public void setChooser() {
		GridDataFactory.fillDefaults().grab(false, false).applyTo(composite);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(composite);

		Display chooserDisplay = composite.getDisplay();
		final Image image = new Image(chooserDisplay, chooserSize, chooserSize);
		gc = new GC(image);

		setHexagon();

		chooserLabel = new Label(composite, SWT.NONE);
		chooserLabel.setImage(image);
		chooserLabel.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {}

			public void mouseDown(MouseEvent e) {
				int x = e.x - chooserRadius;
				int y = e.y - chooserRadius;
				choosedRGB = getRGB(x, y);
				choosedColorButton.setBackground(new Color(e.display, choosedRGB));
			}

			public void mouseUp(MouseEvent e) {}
		});

		final Slider slider = new Slider(composite, SWT.HORIZONTAL);
		slider.setBounds(100, 50, 200, 20);
		slider.setMinimum(0);
		slider.setMaximum(255);
		slider.setIncrement(8);
		slider.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));

//		slider.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//			}
//		});
		slider.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				col3 = new Integer(slider.getSelection()).intValue();
				setHexagon();
				chooserLabel.setImage(image);
//				switch (event.detail) {
//		        case SWT.DRAG:
//				case SWT.ARROW_UP:
// 	            case SWT.ARROW_DOWN:
//		        case SWT.ARROW_DOWN:
//		        case SWT.DRAG:
//		        case SWT.END:
//		        case SWT.HOME:
//		        case SWT.PAGE_DOWN:
//		        case SWT.PAGE_UP:
//				}
			}
		});

		choosedColorButton = new Button(composite, SWT.PUSH);
		choosedColorButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
		choosedColorButton.setText("                              "); //$NON-NLS-1$
		// choosedColorButton.setSize(300, 50); doesn't work
		choosedColorButton.setToolTipText(Messages.color_chooser_choosed_color);
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
