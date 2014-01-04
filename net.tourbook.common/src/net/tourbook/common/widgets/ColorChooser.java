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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.actions.ActionColorChooserAddColorsFromProfile;
import net.tourbook.common.widgets.actions.ActionColorChooserClearCustomColors;
import net.tourbook.common.widgets.actions.ActionColorChooserSetColorsFromProfile;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class ColorChooser extends Composite {

	private static final int						NUMBER_OF_HORIZONTAL_COLORS				= 12;
	private static final int						NUMBER_OF_VERTICAL_COLORS				= 4;

	private static final String						STATE_COLOR_CHOOSER_SELECTED_COL3		= "STATE_COLOR_CHOOSER_SELECTED_COL3";		//$NON-NLS-1$
	private static final String						STATE_COLOR_CHOOSER_SELECTED_COLOR		= "STATE_COLOR_CHOOSER_SELECTED_COLOR";	//$NON-NLS-1$
	private static final String						STATE_COLOR_CHOOSER_SELECTED_TAB		= "STATE_COLOR_CHOOSER_SELECTED_TAB";		//$NON-NLS-1$
	private static final String						XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS	= "XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS";	//$NON-NLS-1$

	private static final String						TAG_CUSTOM_COLOR						= "color";									//$NON-NLS-1$
	private static final String						ATTR_RED								= "red";									//$NON-NLS-1$
	private static final String						ATTR_GREEN								= "green";									//$NON-NLS-1$
	private static final String						ATTR_BLUE								= "blue";									//$NON-NLS-1$
	private static final String						ATTR_NUMBER_OF_HORIZONTAL_COLORS		= "hColors";								//$NON-NLS-1$
	private static final String						ATTR_NUMBER_OF_VERTICAL_COLORS			= "vColors";								//$NON-NLS-1$
	private static final String						ATTR_POSITION_HORIZONTAL				= "hPos";									//$NON-NLS-1$
	private static final String						ATTR_POSITION_VERTICAL					= "vPos";									//$NON-NLS-1$

	private static final double						A_120									= 2 * Math.PI / 3;
	private static final double						SQRT_3									= Math.sqrt(3);
	private static final double						TWO_DIV_SQRT_3							= 2. / SQRT_3;

	private static final double						SINUS_120								= Math.sin(A_120);
	private static final double						SINUS_240								= -SINUS_120;
	private static final double						COSINUS120								= Math.cos(A_120);
	private static final double						COSINUS_240								= COSINUS120;

	private RGB										_chooserRGB;
	private int										_chooserSize;
	private int										_chooserRadius;
	private int										_hexagonRadius;

	private int										_col3;

	private int										_selectedValueRed;
	private int										_selectedValueGreen;
	private int										_selectedValueBlue;
	private int										_selectedValueHue;
	private int										_selectedValueSaturation;
	private int										_selectedValueBrightness;

	private boolean									_hexagonChangeState						= false;

	private MouseAdapter							_customColorMouseListener;

	private Object									DEFAULT_COLOR_ID						= new Object();

	private RGB										_hexagonDefaultRGB;
	private RGB										_customColorsDefaultRGB;

	private IProfileColors							_profileColors;

	private ActionColorChooserClearCustomColors		_actionColorChooserClearCustomColors;
	private ActionColorChooserSetColorsFromProfile	_actionColorChooserSetColorsFromProfile;
	private ActionColorChooserAddColorsFromProfile	_actionColorChooserAddColorsFromProfile;

	/*
	 * UI controls
	 */
	private TabFolder								_tabFolder;

	private ImageCanvas								_hexagonCanvas;
	private Scale									_scaleHexagon;
	private Spinner									_spinnerHexagon;

	private Label									_lblHoveredColor;
	private Label									_lblSelectedColor;
	private Label[][]								_customColors;

	private Scale									_scaleRed;
	private Scale									_scaleGreen;
	private Scale									_scaleBlue;
	private Scale									_scaleHue;
	private Scale									_scaleSaturation;
	private Scale									_scaleBrightness;

	private Spinner									_spinnerRed;
	private Spinner									_spinnerGreen;
	private Spinner									_spinnerBlue;
	private Spinner									_spinnerHue;
	private Spinner									_spinnerSaturation;
	private Spinner									_spinnerBrightness;

	{
		final Display display = Display.getCurrent();

		_hexagonDefaultRGB = new RGB(0x80, 0x80, 0x80);
//		_customColorsDefaultRGB = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
		_customColorsDefaultRGB = display.getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW).getRGB();
	}

	public ColorChooser(final Composite parent, final int style) {

		super(parent, style);

		setHexagonSize();

		initUI();
		createUI(parent);

		createActions();
	}

	public void actionAddColorsFromProfile() {

		int colorPosCounter = 0;

		final RGB[] profileColors = _profileColors.getProfileColors();

		Outer: for (final RGB rgb : profileColors) {

			while (true) {

				// search for a free position

				final int hPos = colorPosCounter % NUMBER_OF_HORIZONTAL_COLORS;
				final int vPos = colorPosCounter / NUMBER_OF_HORIZONTAL_COLORS;

				if (vPos >= NUMBER_OF_VERTICAL_COLORS) {

					// ignore colors which do not fit in the color grid

					showProfileWarning();

					break Outer;
				}

				final Label colorLabel = _customColors[vPos][hPos];
				if (colorLabel.getData() == DEFAULT_COLOR_ID) {

					// a free position is available

					setColorInColorLabel(colorLabel, rgb);

					break;
				}

				colorPosCounter++;
			}
		}
	}

	public void actionResetCustomColors() {

		for (int verticalIndex = 0; verticalIndex < NUMBER_OF_VERTICAL_COLORS; verticalIndex++) {

			final Label[] _horizontalColors = _customColors[verticalIndex];

			for (int horizontalIndex = 0; horizontalIndex < NUMBER_OF_HORIZONTAL_COLORS; horizontalIndex++) {

				setColorInColorLabel(_horizontalColors[horizontalIndex], _customColorsDefaultRGB);
			}
		}
	}

	public void actionSetColorsFromProfile() {

		int colorPosCounter = 0;
		int hPos = 0;
		int vPos = 0;

		for (final RGB rgb : _profileColors.getProfileColors()) {

			hPos = colorPosCounter % NUMBER_OF_HORIZONTAL_COLORS;
			vPos = colorPosCounter / NUMBER_OF_HORIZONTAL_COLORS;

			if (vPos >= NUMBER_OF_VERTICAL_COLORS) {

				// ignore colors which do not fit in the color grid

				showProfileWarning();

				break;
			}

			setColorInColorLabel(_customColors[vPos][hPos], rgb);

			colorPosCounter++;
		}

		fillupCustomColorsWithDefaultColors(colorPosCounter);
	}

	public void chooseRGBFromHexagon(final MouseEvent e) {

		setChooserRGB(getRgbFromHexagon(e));

		updateUI();
	}

	private void createActions() {

		_actionColorChooserClearCustomColors = new ActionColorChooserClearCustomColors(this);
		_actionColorChooserSetColorsFromProfile = new ActionColorChooserSetColorsFromProfile(this);
		_actionColorChooserAddColorsFromProfile = new ActionColorChooserAddColorsFromProfile(this);
	}

	private void createUI(final Composite parent) {

		// force layout
		GridLayoutFactory.fillDefaults().spacing(0, 5).applyTo(this);
//		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_10_Tabs(this);
			createUI_40_SelectedColor(this);
			createUI_50_CustomColors(this);
		}

		// set selected color
		setChooserRGB(new RGB(_selectedValueRed, _selectedValueGreen, _selectedValueBlue));
	}

	private void createUI_10_Tabs(final Composite parent) {

		_tabFolder = new TabFolder(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_tabFolder);
		_tabFolder.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		// hexagon
		final TabItem hexagonTab = new TabItem(_tabFolder, SWT.NONE);
		hexagonTab.setText(Messages.color_chooser_hexagon);
		createUI_22_Tab_Hexagon(_tabFolder, hexagonTab);

		// rgb / hsb
		final TabItem rgbTab = new TabItem(_tabFolder, SWT.NONE);
		rgbTab.setText(Messages.color_chooser_rgb);
		createUI_30_Tab_RGB_HSB(_tabFolder, rgbTab);

		_tabFolder.pack();
	}

	/**
	 * Hexagon-Tab
	 */
	private void createUI_22_Tab_Hexagon(final Composite parent, final TabItem hexagonTab) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			{
				_hexagonCanvas = new ImageCanvas(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.hint(_chooserSize, _chooserSize)
						.grab(true, false)
						.align(SWT.CENTER, SWT.FILL)
						.applyTo(_hexagonCanvas);
				_hexagonCanvas.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

				final Image hexagonImage = new Image(container.getDisplay(), _chooserSize, _chooserSize);
				_hexagonCanvas.setImage(hexagonImage);

				_hexagonCanvas.setToolTipText(Messages.Color_Chooser_Hexagon_Tooltip);

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
						onHexagonMouseMove(e);
					}
				});
			}

			final Composite scaleContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(scaleContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(scaleContainer);
//			scaleContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
			{
				_scaleHexagon = new Scale(scaleContainer, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(_scaleHexagon);
				_scaleHexagon.setMinimum(0);
				_scaleHexagon.setMaximum(255);
				_scaleHexagon.setPageIncrement(16);
				_scaleHexagon.addListener(SWT.Selection, new Listener() {
					public void handleEvent(final Event e) {
						onHexagonScale();
					}
				});

				_spinnerHexagon = new Spinner(scaleContainer, SWT.BORDER);
				_spinnerHexagon.setMinimum(0);
				_spinnerHexagon.setMaximum(255);
				_spinnerHexagon.setPageIncrement(16);
				_spinnerHexagon.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onHexagonSpinner();
					}
				});
				_spinnerHexagon.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onHexagonSpinner();
					}
				});
			}
		}

		hexagonTab.setControl(container);
	}

	private void createUI_30_Tab_RGB_HSB(final Composite parent, final TabItem rgbTab) {

		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		final int scaleStyle = SWT.NONE;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.swtDefaults()//
				.numColumns(3)
//				.spacing(5, 10)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		{
			createUI_32_RGB(container, gd, scaleStyle);

			// vertical spacer
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().span(3, 1).indent(0, 10).applyTo(label);

			createUI_34_HSB(container, gd, scaleStyle);
		}

		rgbTab.setControl(container);
	}

	private void createUI_32_RGB(final Composite parent, final GridData gd, final int scaleStyle) {

		/*
		 * red
		 */
		{
			createUI_39_RgbLabel(parent, Messages.color_chooser_red, Display.getCurrent().getSystemColor(SWT.COLOR_RED));

			_scaleRed = new Scale(parent, scaleStyle);
			_scaleRed.setLayoutData(gd);
			_scaleRed.setMinimum(0);
			_scaleRed.setMaximum(255);
			_scaleRed.setPageIncrement(16);
			_scaleRed.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleRgbRed();
				}
			});

			_spinnerRed = new Spinner(parent, SWT.BORDER);
			_spinnerRed.setMinimum(0);
			_spinnerRed.setMaximum(255);
			_spinnerRed.setPageIncrement(16);
			_spinnerRed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerRgbRed();
				}
			});
			_spinnerRed.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerRgbRed();
				}
			});
		}

		/*
		 * green
		 */
		{
			createUI_39_RgbLabel(
					parent,
					Messages.color_chooser_green,
					Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

			_scaleGreen = new Scale(parent, scaleStyle);
			_scaleGreen.setLayoutData(gd);
			_scaleGreen.setMinimum(0);
			_scaleGreen.setMaximum(255);
			_scaleGreen.setPageIncrement(16);
			_scaleGreen.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleRgbGreen();
				}
			});

			_spinnerGreen = new Spinner(parent, SWT.BORDER);
			_spinnerGreen.setMinimum(0);
			_spinnerGreen.setMaximum(255);
			_spinnerGreen.setIncrement(1);
			_spinnerGreen.setPageIncrement(16);
			_spinnerGreen.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerRgbGreen();
				}
			});
			_spinnerGreen.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerRgbGreen();
				}
			});
		}

		/*
		 * blue
		 */
		{
			createUI_39_RgbLabel(
					parent,
					Messages.color_chooser_blue,
					Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

			_scaleBlue = new Scale(parent, scaleStyle);
			_scaleBlue.setLayoutData(gd);
			_scaleBlue.setMinimum(0);
			_scaleBlue.setMaximum(255);
			_scaleBlue.setPageIncrement(16);
			_scaleBlue.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleRgbBlue();
				}
			});

			_spinnerBlue = new Spinner(parent, SWT.BORDER);
			_spinnerBlue.setMinimum(0);
			_spinnerBlue.setMaximum(255);
			_spinnerBlue.setIncrement(1);
			_spinnerBlue.setPageIncrement(16);
			_spinnerBlue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerRgbBlue();
				}
			});
			_spinnerBlue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerRgbBlue();
				}
			});
		}
	}

	private void createUI_34_HSB(final Composite parent, final GridData gd, final int scaleStyle) {

		// hue        - the hue        value for the HSB color (from 0 to 360)
		// saturation - the saturation value for the HSB color (from 0 to 1)
		// brightness - the brightness value for the HSB color (from 0 to 1)

		/*
		 * HUE
		 */
		{
			final Label hueLabel = new Label(parent, SWT.NONE);
			hueLabel.setText(Messages.color_chooser_hue);

			_scaleHue = new Scale(parent, scaleStyle);
			_scaleHue.setLayoutData(gd);
			_scaleHue.setMinimum(0);
			_scaleHue.setMaximum(360);
			_scaleHue.setPageIncrement(20);
			_scaleHue.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleHsbHUE();
				}
			});

			_spinnerHue = new Spinner(parent, SWT.BORDER);
			_spinnerHue.setMinimum(0);
			_spinnerHue.setMaximum(360);
			_spinnerHue.setIncrement(1);
			_spinnerHue.setPageIncrement(20);
			_spinnerHue.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerHsbHUE();
				}
			});
			_spinnerHue.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerHsbHUE();
				}
			});
		}

		/*
		 * saturation
		 */
		{
			final Label saturationLabel = new Label(parent, SWT.NONE);
			saturationLabel.setText(Messages.color_chooser_saturation);

			_scaleSaturation = new Scale(parent, scaleStyle);
			_scaleSaturation.setLayoutData(gd);
			_scaleSaturation.setMinimum(0);
			_scaleSaturation.setMaximum(100);
			_scaleSaturation.setPageIncrement(10);
			_scaleSaturation.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleHsbSaturation();
				}
			});

			_spinnerSaturation = new Spinner(parent, SWT.BORDER);
			_spinnerSaturation.setMinimum(0);
			_spinnerSaturation.setMaximum(100);
			_spinnerSaturation.setPageIncrement(10);
			_spinnerSaturation.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerHsbSaturation();
				}
			});
			_spinnerSaturation.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerHsbSaturation();
				}
			});
		}

		/*
		 * brightness
		 */
		{
			final Label brightnessLabel = new Label(parent, SWT.NONE);
			brightnessLabel.setText(Messages.color_chooser_brightness);

			_scaleBrightness = new Scale(parent, scaleStyle);
			_scaleBrightness.setLayoutData(gd);
			_scaleBrightness.setMinimum(0);
			_scaleBrightness.setMaximum(100);
			_scaleBrightness.setPageIncrement(10);
			_scaleBrightness.addListener(SWT.Selection, new Listener() {
				public void handleEvent(final Event e) {
					onScaleHsbBrightness();
				}
			});

			_spinnerBrightness = new Spinner(parent, SWT.BORDER);
			_spinnerBrightness.setMinimum(0);
			_spinnerBrightness.setMaximum(100);
			_spinnerBrightness.setPageIncrement(10);
			_spinnerBrightness.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSpinnerHsbBrightness();
				}
			});
			_spinnerBrightness.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSpinnerHsbBrightness();
				}
			});
		}
	}

	private void createUI_39_RgbLabel(final Composite parent, final String labelText, final Color rgbColor) {

		final Composite rgbContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(rgbContainer);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(rgbContainer);
//		rgbContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			final Label label = new Label(rgbContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).align(SWT.FILL, SWT.CENTER).applyTo(label);
			label.setText(labelText);

//			final Label redLabel = new Label(rgbContainer, SWT.NONE);
//			GridDataFactory.fillDefaults()//
////							.grab(false, true)
//					.align(SWT.END, SWT.FILL)
//					.hint(5, 10)
//					.applyTo(redLabel);
//			redLabel.setBackground(rgbColor);
		}
	}

	private void createUI_40_SelectedColor(final Composite parent) {

		final int colorHeight = _chooserSize / 10;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.minSize(SWT.DEFAULT, SWT.DEFAULT)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(5, 1)
				.equalWidth(true)
				.applyTo(container);
		{
			{
				// Label: Selected color
				Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Color_Chooser_Label_SelectedColor);

				// Label: Hovered color
				label = new Label(container, SWT.NONE);
				label.setText(Messages.Color_Chooser_Label_HoveredColor);
			}

			{
				// Color Label: Selected color
				_lblSelectedColor = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.hint(SWT.DEFAULT, colorHeight)
						.applyTo(_lblSelectedColor);

				// Color Label: Hovered color
				_lblHoveredColor = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, true)
						.applyTo(_lblHoveredColor);
				_lblHoveredColor.setToolTipText(Messages.Color_Chooser_HoveredColor_Tooltip);

				// set initial color that the label is slightly visible
				_lblHoveredColor.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
			}
		}
	}

	private void createUI_50_CustomColors(final Composite parent) {

		// Link: Custom colors
		{
			final Link linkCustomColor = new Link(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(linkCustomColor);
			linkCustomColor.setText(Messages.Color_Chooser_Link_CustomColors);
			linkCustomColor.setToolTipText(Messages.Color_Chooser_Link_CustomColors_Tooltip);

			linkCustomColor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					UI.openControlMenu(linkCustomColor);
				}
			});

			/*
			 * create link context menu
			 */
			final MenuManager menuMgr = new MenuManager();

			menuMgr.setRemoveAllWhenShown(true);
			menuMgr.addMenuListener(new IMenuListener() {
				public void menuAboutToShow(final IMenuManager menuManager) {
					fillContextMenu(menuManager);
				}
			});

			linkCustomColor.setMenu(menuMgr.createContextMenu(linkCustomColor));
		}

		createUI_52_CustomColors_Colors(parent);
	}

	private void createUI_52_CustomColors_Colors(final Composite parent) {

		final int colorSpacing = 5;
		final int columnSpacing = (NUMBER_OF_HORIZONTAL_COLORS - 5) * colorSpacing;

		final int customColorSize = (_chooserSize - columnSpacing) / NUMBER_OF_HORIZONTAL_COLORS;
//		final int customColorSize = (_chooserSize) / NUMBER_OF_HORIZONTAL_COLORS;

		_customColors = new Label[NUMBER_OF_VERTICAL_COLORS][NUMBER_OF_HORIZONTAL_COLORS];

		// Container: custom colors
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(NUMBER_OF_HORIZONTAL_COLORS)
				.spacing(colorSpacing, colorSpacing)
				.applyTo(container);
		{
			for (int rowIndex = 0; rowIndex < NUMBER_OF_VERTICAL_COLORS; rowIndex++) {
				for (int columnIndex = 0; columnIndex < NUMBER_OF_HORIZONTAL_COLORS; columnIndex++) {

//					final Label colorLabel = new Label(container, SWT.BORDER);
					final Label colorLabel = new Label(container, SWT.NONE);
					GridDataFactory.fillDefaults()//
							.align(SWT.BEGINNING, SWT.CENTER)
							.hint(customColorSize, customColorSize)
							.applyTo(colorLabel);

					_customColors[rowIndex][columnIndex] = colorLabel;

					colorLabel.addMouseListener(_customColorMouseListener);
				}
			}
		}
	}

	private void drawHexagon() {

		final GC gc = new GC(_hexagonCanvas.getImage());
		{
			for (int x = -_chooserRadius; x < _chooserRadius; x++) {
				for (int y = -_chooserRadius; y < _chooserRadius; y++) {

					final Color fgColor = new Color(this.getDisplay(), getRgbFromHexagon(x, y));
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

	private void fillContextMenu(final IMenuManager menuMgr) {

		if (_profileColors != null) {
			menuMgr.add(_actionColorChooserAddColorsFromProfile);
			menuMgr.add(_actionColorChooserSetColorsFromProfile);
			menuMgr.add(new Separator());
		}

		menuMgr.add(_actionColorChooserClearCustomColors);
	}

	private void fillupCustomColorsWithDefaultColors() {

		for (int verticalIndex = 0; verticalIndex < NUMBER_OF_VERTICAL_COLORS; verticalIndex++) {

			final Label[] _horizontalColors = _customColors[verticalIndex];

			for (int horizontalIndex = 0; horizontalIndex < NUMBER_OF_HORIZONTAL_COLORS; horizontalIndex++) {

				final Label colorLabel = _horizontalColors[horizontalIndex];

				// set color only when not yet set
				if (!(colorLabel.getData() instanceof RGB)) {

					setColorInColorLabel(colorLabel, _customColorsDefaultRGB);
				}
			}
		}
	}

	/**
	 * Fillup grid with default colors.
	 * 
	 * @param colorPosCounter
	 */
	private void fillupCustomColorsWithDefaultColors(int colorPosCounter) {

		while (true) {

			final int hPos = colorPosCounter % NUMBER_OF_HORIZONTAL_COLORS;
			final int vPos = colorPosCounter / NUMBER_OF_HORIZONTAL_COLORS;

			if (vPos >= NUMBER_OF_VERTICAL_COLORS) {

				// ignore colors which do not fit in the color grid
				break;
			}

			setColorInColorLabel(_customColors[vPos][hPos], _customColorsDefaultRGB);

			colorPosCounter++;
		}
	}

	/**
	 * @return Return {@link RGB} from the currently selected color.
	 */
	public RGB getRGB() {
		return _chooserRGB;
	}

	private RGB getRgbFromHexagon(final int x, final int y) {

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
		return _hexagonDefaultRGB;
	}

	private RGB getRgbFromHexagon(final MouseEvent event) {

		final int x = event.x - _chooserRadius;
		final int y = event.y - _chooserRadius;

		return getRgbFromHexagon(x, y);
	}

	private void initUI() {

		_customColorMouseListener = new MouseAdapter() {

			@Override
			public void mouseDown(final MouseEvent e) {
				onColorLabelMouseDown(e);
			}
		};
	}

	private void onColorLabelMouseDown(final MouseEvent event) {

		final Label colorLabel = (Label) (event.widget);

		if (event.button == 3) {

			// right button: push color to color chooser

			RGB rgb;
			final Object labelData = colorLabel.getData();
			if (labelData instanceof RGB) {

				rgb = (RGB) labelData;

			} else {

				// set default color

				rgb = _customColorsDefaultRGB;
			}

			setChooserRGB(rgb);

			updateUI();

		} else {

			// left button: get from color chooser

			if (UI.isCtrlKey(event)) {

				// set default color

				setColorInColorLabel(colorLabel, _customColorsDefaultRGB);

			} else {

				// set chooser color

				setColorInColorLabel(colorLabel, _chooserRGB);
			}
		}
	}

	private void onHexagonMouseMove(final MouseEvent event) {

		// update hovered color
		updateUI_HoveredColor(getRgbFromHexagon(event));

		if (!_hexagonChangeState) {
			return;
		}

		chooseRGBFromHexagon(event);
	}

	private void onHexagonScale() {

		_col3 = _scaleHexagon.getSelection();
		_spinnerHexagon.setSelection(_col3);

		drawHexagon();
		_hexagonCanvas.redraw();
	}

	private void onHexagonSpinner() {

		_col3 = _spinnerHexagon.getSelection();
		_scaleHexagon.setSelection(_col3);

		drawHexagon();
		_hexagonCanvas.redraw();
	}

	private void onScaleHsbBrightness() {

		_selectedValueBrightness = _scaleBrightness.getSelection();

		updateUI_RGB();
	}

	private void onScaleHsbHUE() {

		_selectedValueHue = _scaleHue.getSelection();

		updateUI_RGB();
	}

	private void onScaleHsbSaturation() {

		_selectedValueSaturation = _scaleSaturation.getSelection();

		updateUI_RGB();
	}

	private void onScaleRgbBlue() {

		_selectedValueBlue = _scaleBlue.getSelection();

		updateUI_HSB();
	}

	private void onScaleRgbGreen() {

		_selectedValueGreen = _scaleGreen.getSelection();

		updateUI_HSB();
	}

	private void onScaleRgbRed() {

		_selectedValueRed = _scaleRed.getSelection();

		updateUI_HSB();
	}

	private void onSpinnerHsbBrightness() {

		_selectedValueBrightness = _spinnerBrightness.getSelection();

		updateUI_RGB();
	}

	private void onSpinnerHsbHUE() {

		_selectedValueHue = _spinnerHue.getSelection();

		updateUI_RGB();
	}

	private void onSpinnerHsbSaturation() {

		_selectedValueSaturation = _spinnerSaturation.getSelection();

		updateUI_RGB();
	}

	private void onSpinnerRgbBlue() {

		_selectedValueBlue = _spinnerBlue.getSelection();

		updateUI_HSB();
	}

	private void onSpinnerRgbGreen() {

		_selectedValueGreen = _spinnerGreen.getSelection();

		updateUI_HSB();
	}

	private void onSpinnerRgbRed() {

		_selectedValueRed = _spinnerRed.getSelection();

		updateUI_HSB();
	}

	public void restoreState(final IDialogSettings state) {

		_tabFolder.setSelection(Util.getStateInt(state, STATE_COLOR_CHOOSER_SELECTED_TAB, 0));

		final RGB stateRGB = Util.getStateRGB(state, STATE_COLOR_CHOOSER_SELECTED_COLOR, _chooserRGB);
		setChooserRGB(stateRGB);

		_col3 = Util.getStateInt(state, STATE_COLOR_CHOOSER_SELECTED_COL3, 0);
		_spinnerHexagon.setSelection(_col3);
		_scaleHexagon.setSelection(_col3);
		drawHexagon();

		restoreState_CustomColors(state);
	}

	private void restoreState_CustomColors(final IDialogSettings state) {

		final String stateValue = Util.getStateString(state, XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS, null);

		if ((stateValue != null) && (stateValue.length() > 0)) {

			try {

				final Reader reader = new StringReader(stateValue);

				restoreState_CustomColors_Colors(XMLMemento.createReadRoot(reader));

			} catch (final WorkbenchException e) {
				// ignore
			}
		}

	}

	private void restoreState_CustomColors_Colors(final XMLMemento xmlMemento) {

		final Integer xmlNumberOfHorizontalColors = xmlMemento.getInteger(ATTR_NUMBER_OF_HORIZONTAL_COLORS);
		final Integer xmlNumberOfVerticalColors = xmlMemento.getInteger(ATTR_NUMBER_OF_VERTICAL_COLORS);

		boolean useSavedColorPosition = true;

		if (xmlNumberOfHorizontalColors == null //
				|| xmlNumberOfVerticalColors == null
				|| NUMBER_OF_HORIZONTAL_COLORS != xmlNumberOfHorizontalColors
				|| NUMBER_OF_VERTICAL_COLORS != xmlNumberOfVerticalColors) {

			/*
			 * The save colors have another structure than the current color structure -> sort
			 * colors by sequence and not by previous position.
			 */
			useSavedColorPosition = false;
		}

		int colorPosCounter = 0;

		for (final IMemento colorMomento : xmlMemento.getChildren()) {

			final Integer red = colorMomento.getInteger(ATTR_RED);
			final Integer green = colorMomento.getInteger(ATTR_GREEN);
			final Integer blue = colorMomento.getInteger(ATTR_BLUE);

			Integer hPos = colorMomento.getInteger(ATTR_POSITION_HORIZONTAL);
			Integer vPos = colorMomento.getInteger(ATTR_POSITION_VERTICAL);

			if (red == null || green == null || blue == null || hPos == null || vPos == null) {
				// ignore
				continue;
			}

			if (useSavedColorPosition == false) {

				// use sequential positioning

				hPos = colorPosCounter % NUMBER_OF_HORIZONTAL_COLORS;
				vPos = colorPosCounter / NUMBER_OF_HORIZONTAL_COLORS;
			}

			setColorInColorLabel(_customColors[vPos][hPos], new RGB(red, green, blue));

			colorPosCounter++;
		}

		// fillup to show tooltips
		fillupCustomColorsWithDefaultColors();
	}

	public void saveState(final IDialogSettings state) {

		state.put(STATE_COLOR_CHOOSER_SELECTED_TAB, _tabFolder.getSelectionIndex());
		state.put(STATE_COLOR_CHOOSER_SELECTED_COL3, _col3);

		Util.setState(state, STATE_COLOR_CHOOSER_SELECTED_COLOR, _chooserRGB);

		saveState_CustomColors(state);
	}

	private void saveState_CustomColors(final IDialogSettings state) {

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS);

		saveState_CustomColors_Colors(xmlMemento);

		// Write the XML block to the state store.
		final Writer writer = new StringWriter();
		try {

			xmlMemento.save(writer);
			state.put(XML_STATE_COLOR_CHOOSER_CUSTOM_COLORS, writer.toString());

		} catch (final IOException e) {

			StatusUtil.log(e);

		} finally {

			try {
				writer.close();
			} catch (final IOException e) {
				StatusUtil.log(e);
			}
		}
	}

	private void saveState_CustomColors_Colors(final XMLMemento xmlMemento) {

		xmlMemento.putInteger(ATTR_NUMBER_OF_HORIZONTAL_COLORS, NUMBER_OF_HORIZONTAL_COLORS);
		xmlMemento.putInteger(ATTR_NUMBER_OF_VERTICAL_COLORS, NUMBER_OF_VERTICAL_COLORS);

		for (int verticalIndex = 0; verticalIndex < NUMBER_OF_VERTICAL_COLORS; verticalIndex++) {

			final Label[] _horizontalColors = _customColors[verticalIndex];

			for (int horizontalIndex = 0; horizontalIndex < NUMBER_OF_HORIZONTAL_COLORS; horizontalIndex++) {

				final Label colorLabel = _horizontalColors[horizontalIndex];

				final Object labelData = colorLabel.getData();
				if (labelData instanceof RGB) {

					final RGB rgb = (RGB) labelData;

					final IMemento xmlCustomColor = xmlMemento.createChild(TAG_CUSTOM_COLOR);

					xmlCustomColor.putInteger(ATTR_RED, rgb.red);
					xmlCustomColor.putInteger(ATTR_GREEN, rgb.green);
					xmlCustomColor.putInteger(ATTR_BLUE, rgb.blue);

					xmlCustomColor.putInteger(ATTR_POSITION_HORIZONTAL, horizontalIndex);
					xmlCustomColor.putInteger(ATTR_POSITION_VERTICAL, verticalIndex);
				}
			}
		}
	}

	private void setChooserRGB(final RGB rgb) {

		_chooserRGB = rgb;

		updateUI_SelectedColor();

		_lblSelectedColor.setToolTipText(NLS.bind(//
				Messages.Color_Chooser_SelectedColor_Tooltip,
				new Object[] { rgb.red, rgb.green, rgb.blue }));
	}

	private void setColorInColorLabel(final Label colorLabel, final RGB rgb) {

		// set background color
		final Color labelColor = new Color(getDisplay(), rgb);
		{
			colorLabel.setBackground(labelColor);
		}
		labelColor.dispose();

		// set data
		if (rgb == _customColorsDefaultRGB) {

			// set default color data, this indicates that the color is not set

			colorLabel.setData(DEFAULT_COLOR_ID);

		} else {

			// set custom color

			colorLabel.setData(rgb);
		}

		// set tooltip always
		colorLabel.setToolTipText(NLS.bind(//
				Messages.Color_Chooser_Label_ColorCustomColors_Tooltip,
				new Object[] { rgb.red, rgb.green, rgb.blue }));
	}

	private void setHexagonSize() {

		_chooserSize = 300;

		_chooserRadius = _chooserSize / 2;
		_hexagonRadius = (int) (_chooserSize / 2.1);
	}

	public void setProfileColors(final IProfileColors profileColors) {

		_profileColors = profileColors;
	}

	/**
	 * Set RGB for the color chooser
	 * 
	 * @param rgb
	 */
	public void setRGB(final RGB rgb) {

		setChooserRGB(rgb);

		updateUI();
	}

	private void showProfileWarning() {

		// show info that there are too many colors which could be set into the custom colors
		MessageDialog.openInformation(
				getShell(),
				Messages.Color_Chooser_Dialog_SetColorsFromProfile_Title,
				Messages.Color_Chooser_Dialog_SetColorsFromProfile_Message);
	}

	/**
	 * Updates all controls in the UI with the color from {@link #_chooserRGB}.
	 */
	private void updateUI() {

		updateUI_SelectedColor();
		updateUI_Controls();

		updateValues_RGB();
		updateValues_HSB();
	}

	private void updateUI_Controls() {

		final int red = _chooserRGB.red;
		final int green = _chooserRGB.green;
		final int blue = _chooserRGB.blue;

		_scaleRed.setSelection(red);
		_scaleGreen.setSelection(green);
		_scaleBlue.setSelection(blue);

		_spinnerRed.setSelection(red);
		_spinnerGreen.setSelection(green);
		_spinnerBlue.setSelection(blue);

		final float hsb[] = _chooserRGB.getHSB();

		final int hue = (int) hsb[0];
		final int saturation = (int) (hsb[1] * 100);
		final int brightness = (int) (hsb[2] * 100);

		_scaleHue.setSelection(hue);
		_scaleSaturation.setSelection(saturation);
		_scaleBrightness.setSelection(brightness);

		_spinnerHue.setSelection(hue);
		_spinnerSaturation.setSelection(saturation);
		_spinnerBrightness.setSelection(brightness);
	}

	private void updateUI_HoveredColor(final RGB hoveredRGB) {

		final Color color = new Color(Display.getCurrent(), hoveredRGB);
		{
			_lblHoveredColor.setBackground(color);
			_lblHoveredColor.setForeground(color);
		}
		color.dispose();
	}

	private void updateUI_HSB() {

		setChooserRGB(new RGB(//
				_selectedValueRed,
				_selectedValueGreen,
				_selectedValueBlue));

		updateUI_Controls();

		updateValues_HSB();
	}

	private void updateUI_RGB() {

		setChooserRGB(new RGB(//
				_selectedValueHue,
				(float) _selectedValueSaturation / 100,
				(float) _selectedValueBrightness / 100));

		updateUI_Controls();

		updateValues_RGB();
	}

	private void updateUI_SelectedColor() {

		final Color color = new Color(Display.getCurrent(), _chooserRGB);
		{
			_lblSelectedColor.setBackground(color);
			_lblSelectedColor.setForeground(color);
		}
		color.dispose();
	}

	private void updateValues_HSB() {

		final float hsb[] = _chooserRGB.getHSB();

		_selectedValueHue = (int) hsb[0];
		_selectedValueSaturation = (int) (hsb[1] * 100);
		_selectedValueBrightness = (int) (hsb[2] * 100);
	}

	private void updateValues_RGB() {

		_selectedValueRed = _chooserRGB.red;
		_selectedValueGreen = _chooserRGB.green;
		_selectedValueBlue = _chooserRGB.blue;
	}

}
