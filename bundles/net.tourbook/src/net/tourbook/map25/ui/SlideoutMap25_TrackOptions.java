/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout for 2.5D tour track configuration.
 */
public class SlideoutMap25_TrackOptions extends ToolbarSlideout implements IColorSelectorListener {

	private Map25View				_map25View;

	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;
	private SelectionAdapter		_defaultSelectionListener;
	private FocusListener			_keepOpenListener;

	private boolean					_isUpdateUI;

	private PixelConverter			_pc;
	private int						_firstColumnIndent;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Button					_chkShowSliderLocation;
	private Button					_chkShowSliderPath;

	private Combo					_comboName;

	private Label					_lblConfigName;
	private Label					_lblSliderLocation_Size;
	private Label					_lblSliderLocation_Color;
	private Label					_lblSliderPath_Width;
	private Label					_lblSliderPath_Color;

	private Spinner					_spinnerOutline_Opacity;
	private Spinner					_spinnerOutline_Width;
	private Spinner					_spinnerSliderLocation_Size;
	private Spinner					_spinnerSliderLocation_Opacity;
	private Spinner					_spinnerSliderPath_LineWidth;
	private Spinner					_spinnerSliderPath_Opacity;

	private Text					_textConfigName;

	private ColorSelectorExtended	_colorOutline_Color;
	private ColorSelectorExtended	_colorSliderLocation_Left;
	private ColorSelectorExtended	_colorSliderLocation_Right;
	private ColorSelectorExtended	_colorSliderPathColor;

	public SlideoutMap25_TrackOptions(	final Composite ownerControl,
										final ToolBar toolbar,
										final Map25View map25View) {

		super(ownerControl, toolbar);

		_map25View = map25View;
	}

	@Override
	public void colorDialogOpened(final boolean isAnotherDialogOpened) {

		setIsAnotherDialogOpened(isAnotherDialogOpened);
	}

	private void createActions() {

	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		fillUI();
		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

//		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(UI.SHELL_MARGIN, UI.SHELL_MARGIN).applyTo(_shellContainer);
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final Composite container = new Composite(_shellContainer, SWT.NO_FOCUS);
			GridLayoutFactory
					.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			{
				createUI_000_Title(container);

				createUI_100_Line(container);
				createUI_200_SliderLocation(container);
				createUI_210_SliderPath(container);

				createUI_999_ConfigName(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_000_Title(final Composite parent) {

		/*
		 * Label: Title
		 */
		final Label title = new Label(parent, SWT.LEAD);
		title.setText(Messages.Slideout_Map_TrackOptions_Label_Title);
		title.setToolTipText(Messages.Slideout_Map25TrackOptions_Label_ConfigName_Tooltip);
		MTFont.setBannerFont(title);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(title);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{

			/*
			 * Combo: Configutation
			 */
			_comboName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory
					.fillDefaults()
					.grab(true, false)
					.align(SWT.BEGINNING, SWT.CENTER)
					// this is too small in linux
					//					.hint(_pc.convertHorizontalDLUsToPixels(15 * 4), SWT.DEFAULT)
					.applyTo(_comboName);
			_comboName.setVisibleItemCount(20);
			_comboName.addFocusListener(_keepOpenListener);
			_comboName.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectConfig();
				}
			});

//			/*
//			 * Button: Reset
//			 */
//			_btnReset = new Button(container, SWT.PUSH);
//			GridDataFactory
//					.fillDefaults()//
//					.align(SWT.END, SWT.CENTER)
//					.applyTo(_btnReset);
//			_btnReset.setText(Messages.Slideout_Map25TrackOptions_Button_Default);
//			_btnReset.setToolTipText(Messages.Slideout_Map25TrackOptions_Button_Default_Tooltip);
//			_btnReset.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(final SelectionEvent e) {
//					onSelectDefaultConfig(e);
//				}
//			});
		}
	}

	private void createUI_100_Line(final Composite parent) {

		{
			/*
			 * Line width
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_Map25TrackOptions_Label_OutlineWidth);
			label.setToolTipText(Messages.Slideout_Map25TrackOptions_Label_OutlineWidth_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			// spinner
			_spinnerOutline_Width = new Spinner(parent, SWT.BORDER);
			_spinnerOutline_Width.setDigits(1);
			_spinnerOutline_Width.setMinimum((int) (Map25ConfigManager.OUTLINE_WIDTH_MIN * 10.0f));
			_spinnerOutline_Width.setMaximum((int) (Map25ConfigManager.OUTLINE_WIDTH_MAX * 10.0f));
			_spinnerOutline_Width.setIncrement(1);
			_spinnerOutline_Width.setPageIncrement(10);
			_spinnerOutline_Width.addSelectionListener(_defaultSelectionListener);
			_spinnerOutline_Width.addMouseWheelListener(_defaultMouseWheelListener);
			GridDataFactory
					.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerOutline_Width);
		}
		{
			/*
			 * Line
			 */

			// label
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Slideout_Map25TrackOptions_Label_OutlineColor);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				{
					// color
					_colorOutline_Color = new ColorSelectorExtended(container);
					_colorOutline_Color.addListener(_defaultPropertyChangeListener);
					_colorOutline_Color.addOpenListener(this);
				}
				{
					// opacity
					_spinnerOutline_Opacity = new Spinner(container, SWT.BORDER);
					_spinnerOutline_Opacity.setMinimum(Map25ConfigManager.OUTLINE_OPACITY_MIN);
					_spinnerOutline_Opacity.setMaximum(Map25ConfigManager.OUTLINE_OPACITY_MAX);
					_spinnerOutline_Opacity.setIncrement(1);
					_spinnerOutline_Opacity.setPageIncrement(10);
					_spinnerOutline_Opacity.addSelectionListener(_defaultSelectionListener);
					_spinnerOutline_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
	}

	private void createUI_200_SliderLocation(final Composite parent) {

		{
			/*
			 * Chart slider
			 */
			// checkbox
			_chkShowSliderLocation = new Button(parent, SWT.CHECK);
			_chkShowSliderLocation.setText(Messages.Slideout_Map_Options_Checkbox_ChartSlider);
			_chkShowSliderLocation.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowSliderLocation);
		}
		{
			/*
			 * Size
			 */

			// label
			_lblSliderLocation_Size = new Label(parent, SWT.NONE);
			_lblSliderLocation_Size.setText(Messages.Slideout_Map_Options_Label_SliderLocation_Size);
			GridDataFactory.fillDefaults()
					.indent(_firstColumnIndent, SWT.DEFAULT)
					.applyTo(_lblSliderLocation_Size);

			// size
			_spinnerSliderLocation_Size = new Spinner(parent, SWT.BORDER);
			_spinnerSliderLocation_Size.setMinimum(Map25ConfigManager.SLIDER_LOCATION_SIZE_MIN);
			_spinnerSliderLocation_Size.setMaximum(Map25ConfigManager.SLIDER_LOCATION_SIZE_MAX);
			_spinnerSliderLocation_Size.setIncrement(1);
			_spinnerSliderLocation_Size.setPageIncrement(10);
			_spinnerSliderLocation_Size.addSelectionListener(_defaultSelectionListener);
			_spinnerSliderLocation_Size.addMouseWheelListener(_defaultMouseWheelListener);
		}
		{
			/*
			 * Color
			 */

			// label
			_lblSliderLocation_Color = new Label(parent, SWT.NONE);
			_lblSliderLocation_Color.setText(Messages.Slideout_Map_Options_Label_SliderLocation_Color);
			_lblSliderLocation_Color.setToolTipText(Messages.Slideout_Map_Options_Label_SliderLocation_Color_Tooltip);
			GridDataFactory.fillDefaults()
					.indent(_firstColumnIndent, SWT.DEFAULT)
					.applyTo(_lblSliderLocation_Color);

			final Composite colorContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(colorContainer);
			{
				// color: left
				_colorSliderLocation_Left = new ColorSelectorExtended(colorContainer);
				_colorSliderLocation_Left.addListener(_defaultPropertyChangeListener);
				_colorSliderLocation_Left.addOpenListener(this);

				// color: right
				_colorSliderLocation_Right = new ColorSelectorExtended(colorContainer);
				_colorSliderLocation_Right.addListener(_defaultPropertyChangeListener);
				_colorSliderLocation_Right.addOpenListener(this);

				// opacity
				_spinnerSliderLocation_Opacity = new Spinner(colorContainer, SWT.BORDER);
				_spinnerSliderLocation_Opacity.setMinimum(Map25ConfigManager.SLIDER_LOCATION_OPACITY_MIN);
				_spinnerSliderLocation_Opacity.setMaximum(Map25ConfigManager.SLIDER_LOCATION_OPACITY_MAX);
				_spinnerSliderLocation_Opacity.setIncrement(1);
				_spinnerSliderLocation_Opacity.setPageIncrement(10);
				_spinnerSliderLocation_Opacity.addSelectionListener(_defaultSelectionListener);
				_spinnerSliderLocation_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_210_SliderPath(final Composite parent) {

		{
			/*
			 * Slider path
			 */
			// checkbox
			_chkShowSliderPath = new Button(parent, SWT.CHECK);
			_chkShowSliderPath.setText(Messages.Slideout_Map_Options_Checkbox_SliderPath);
			_chkShowSliderPath.setToolTipText(Messages.Slideout_Map_Options_Checkbox_SliderPath_Tooltip);
			_chkShowSliderPath.addSelectionListener(_defaultSelectionListener);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowSliderPath);
		}
		{
			/*
			 * Line width
			 */

			// label
			_lblSliderPath_Width = new Label(parent, SWT.NONE);
			_lblSliderPath_Width.setText(Messages.Slideout_Map_Options_Label_SliderPath_Width);
			GridDataFactory.fillDefaults().indent(_firstColumnIndent, SWT.DEFAULT).applyTo(_lblSliderPath_Width);

			// spinner
			_spinnerSliderPath_LineWidth = new Spinner(parent, SWT.BORDER);
			_spinnerSliderPath_LineWidth.setDigits(1);
			_spinnerSliderPath_LineWidth.setMinimum((int) (Map25ConfigManager.SLIDER_PATH_LINE_WIDTH_MIN * 10.0f));
			_spinnerSliderPath_LineWidth.setMaximum((int) (Map25ConfigManager.SLIDER_PATH_LINE_WIDTH_MAX * 10.0f));
			_spinnerSliderPath_LineWidth.setIncrement(1);
			_spinnerSliderPath_LineWidth.setPageIncrement(10);
			_spinnerSliderPath_LineWidth.addSelectionListener(_defaultSelectionListener);
			_spinnerSliderPath_LineWidth.addMouseWheelListener(_defaultMouseWheelListener);
		}
		{
			/*
			 * Color + opacity
			 */

			// label
			_lblSliderPath_Color = new Label(parent, SWT.NONE);
			_lblSliderPath_Color.setText(Messages.Slideout_Map_Options_Label_SliderPath_Color);
			GridDataFactory.fillDefaults().indent(_firstColumnIndent, SWT.DEFAULT).applyTo(_lblSliderPath_Color);

			final Composite colorContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
			{
				// color
				_colorSliderPathColor = new ColorSelectorExtended(colorContainer);
				_colorSliderPathColor.addListener(_defaultPropertyChangeListener);
				_colorSliderPathColor.addOpenListener(this);

				// opacity
				_spinnerSliderPath_Opacity = new Spinner(colorContainer, SWT.BORDER);
				_spinnerSliderPath_Opacity.setMinimum(Map25ConfigManager.SLIDER_PATH_OPACITY_MIN);
				_spinnerSliderPath_Opacity.setMaximum(Map25ConfigManager.SLIDER_PATH_OPACITY_MAX);
				_spinnerSliderPath_Opacity.setIncrement(1);
				_spinnerSliderPath_Opacity.setPageIncrement(10);
				_spinnerSliderPath_Opacity.addSelectionListener(_defaultSelectionListener);
				_spinnerSliderPath_Opacity.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_999_ConfigName(final Composite parent) {

		/*
		 * Name
		 */
		{
			/*
			 * Label
			 */
			_lblConfigName = new Label(parent, SWT.NONE);
			_lblConfigName.setText(Messages.Slideout_Map25TrackOptions_Label_Name);
			_lblConfigName.setToolTipText(Messages.Slideout_Map_TrackOptions_Label_Title_Tooltip);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblConfigName);

			/*
			 * Text
			 */
			_textConfigName = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_textConfigName);
			_textConfigName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onModifyName();
				}
			});
		}
	}

	private void enableControls() {

		final boolean isShowSliderPath = _chkShowSliderPath.getSelection();
		final boolean isShowSliderLocation = _chkShowSliderLocation.getSelection();

		/*
		 * Slider location
		 */
		_colorSliderLocation_Left.setEnabled(isShowSliderLocation);
		_colorSliderLocation_Right.setEnabled(isShowSliderLocation);

		_lblSliderLocation_Size.setEnabled(isShowSliderLocation);
		_lblSliderLocation_Color.setEnabled(isShowSliderLocation);

		_spinnerSliderLocation_Opacity.setEnabled(isShowSliderLocation);
		_spinnerSliderLocation_Size.setEnabled(isShowSliderLocation);

		/*
		 * Slider path
		 */
		_colorSliderPathColor.setEnabled(isShowSliderPath);

		_lblSliderPath_Color.setEnabled(isShowSliderPath);
		_lblSliderPath_Width.setEnabled(isShowSliderPath);

		_spinnerSliderPath_LineWidth.setEnabled(isShowSliderPath);
		_spinnerSliderPath_Opacity.setEnabled(isShowSliderPath);
	}

	private void fillUI() {

		final boolean backupIsUpdateUI = _isUpdateUI;
		_isUpdateUI = true;
		{

			for (final Map25TrackConfig config : Map25ConfigManager.getAllTourTrackConfigs()) {
				_comboName.add(config.name);
			}
		}
		_isUpdateUI = backupIsUpdateUI;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
		_firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyConfig();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				onModifyConfig();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onModifyConfig();
			}
		};

		_keepOpenListener = new FocusListener() {

			@Override
			public void focusGained(final FocusEvent e) {

				/*
				 * This will fix the problem that when the list of a combobox is displayed, then the
				 * slideout will disappear :-(((
				 */
				setIsAnotherDialogOpened(true);
			}

			@Override
			public void focusLost(final FocusEvent e) {
				setIsAnotherDialogOpened(false);
			}
		};

	}

	private void onModifyConfig() {

		saveState();

		enableControls();

		final Map25App mapApp = _map25View.getMapApp();

		mapApp.getLayer_Tour().onModifyConfig();
		mapApp.getLayer_SliderPath().onModifyConfig();
		mapApp.getLayer_SliderLocation().onModifyConfig();
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboName.getSelectionIndex();

		_comboName.setItem(selectedIndex, _textConfigName.getText());

		saveState();
	}

	private void onSelectConfig() {

		final int selectedIndex = _comboName.getSelectionIndex();
		final ArrayList<Map25TrackConfig> allConfigurations = Map25ConfigManager.getAllTourTrackConfigs();

		final Map25TrackConfig selectedConfig = allConfigurations.get(selectedIndex);
		final Map25TrackConfig trackConfig = Map25ConfigManager.getActiveTourTrackConfig();

		if (selectedConfig == trackConfig) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState();

		// update model
		Map25ConfigManager.setActiveTrackConfig(selectedConfig);

		// update UI
		updateUI_SetActiveConfig();
	}

	/**
	 * Restores state values from the tour track configuration and update the UI.
	 */
	private void restoreState() {

		_isUpdateUI = true;

		final Map25TrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

		// get active config AFTER getting the index because this could change the active config
		final int activeConfigIndex = Map25ConfigManager.getActiveTourTrackConfigIndex();

		_comboName.select(activeConfigIndex);
		_textConfigName.setText(config.name);

		// track line
		_colorOutline_Color.setColorValue(config.outlineColor);
		_spinnerOutline_Width.setSelection((int) (config.outlineWidth * 10));
		_spinnerOutline_Opacity.setSelection(config.outlineOpacity);

		// slider location
		_chkShowSliderLocation.setSelection(config.isShowSliderLocation);
		_colorSliderLocation_Left.setColorValue(config.sliderLocation_Left_Color);
		_colorSliderLocation_Right.setColorValue(config.sliderLocation_Right_Color);
		_spinnerSliderLocation_Opacity.setSelection(config.sliderLocation_Opacity);
		_spinnerSliderLocation_Size.setSelection(config.sliderLocation_Size);

		// slider  path
		_chkShowSliderPath.setSelection(config.isShowSliderPath);
		_colorSliderPathColor.setColorValue(config.sliderPath_Color);
		_spinnerSliderPath_LineWidth.setSelection((int) (config.sliderPath_LineWidth * 10));
		_spinnerSliderPath_Opacity.setSelection(config.sliderPath_Opacity);

		_isUpdateUI = false;
	}

	private void saveState() {

		// update config

		final Map25TrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

		config.name = _textConfigName.getText();

		// track line
		config.outlineColor = _colorOutline_Color.getColorValue();
		config.outlineWidth = _spinnerOutline_Width.getSelection() / 10.0f;
		config.outlineOpacity = _spinnerOutline_Opacity.getSelection();

		// slider location
		config.isShowSliderLocation = _chkShowSliderLocation.getSelection();
		config.sliderLocation_Left_Color = _colorSliderLocation_Left.getColorValue();
		config.sliderLocation_Right_Color = _colorSliderLocation_Right.getColorValue();
		config.sliderLocation_Opacity = _spinnerSliderLocation_Opacity.getSelection();
		config.sliderLocation_Size = _spinnerSliderLocation_Size.getSelection();

		// slider path
		config.isShowSliderPath = _chkShowSliderPath.getSelection();
		config.sliderPath_Color = _colorSliderPathColor.getColorValue();
		config.sliderPath_LineWidth = _spinnerSliderPath_LineWidth.getSelection() / 10.0f;
		config.sliderPath_Opacity = _spinnerSliderPath_Opacity.getSelection();
	}

	private void updateUI_SetActiveConfig() {

		restoreState();

		enableControls();

		final Map25App mapApp = _map25View.getMapApp();

		mapApp.getLayer_Tour().onModifyConfig();
		mapApp.getLayer_SliderPath().onModifyConfig();
		mapApp.getLayer_SliderLocation().onModifyConfig();
	}
}
