/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap2Appearance;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map 2D properties slideout
 */
public class SlideoutMap2_MapOptions extends ToolbarSlideout implements IColorSelectorListener {

// SET_FORMATTING_OFF
	
	private static final String		MAP_ACTION_EDIT2D_MAP_PREFERENCES	= net.tourbook.map2.Messages.Map_Action_Edit2DMapPreferences;
	
// SET_FORMATTING_ON

	final static IPreferenceStore	_prefStore							= TourbookPlugin.getPrefStore();
	final private IDialogSettings	_state;

	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionListener;
	private SelectionAdapter		_defaultState_SelectionListener;
	private MouseWheelListener		_defaultState_MouseWheelListener;
	private FocusListener			_keepOpenListener;
	private IPropertyChangeListener	_defaultState_ChangePropertyListener;

	private Action					_actionRestoreDefaults;
	private ActionOpenPrefDialog	_actionPrefDialog;

	private PixelConverter			_pc;

	private Map2View				_map2View;

	/*
	 * UI controls
	 */
	private Composite				_parent;

	private Button					_chkSliderPath;
	private Button					_chkTrackOpacity;
	private Button					_chkZoomWithMousePosition;

	private Label					_lblSliderPath_Color;
	private Label					_lblSliderPath_Segments;
	private Label					_lblSliderPath_Width;

	private Spinner					_spinnerSliderPath_Opacity;
	private Spinner					_spinnerSliderPath_Segments;
	private Spinner					_spinnerSliderPath_LineWidth;
	private Spinner					_spinnerTrackOpacity;

	private ColorSelectorExtended	_colorSliderPathColor;

	/**
	 * @param ownerControl
	 * @param toolBar
	 * @param map2View
	 * @param state
	 */
	public SlideoutMap2_MapOptions(	final Control ownerControl,
									final ToolBar toolBar,
									final Map2View map2View,
									final IDialogSettings state) {

		super(ownerControl, toolBar);

		_map2View = map2View;
		_state = state;
	}

	@Override
	public void colorDialogOpened(final boolean isAnotherDialogOpened) {
		setIsAnotherDialogOpened(isAnotherDialogOpened);
	}

	private void createActions() {

		_actionRestoreDefaults = new Action() {
			@Override
			public void run() {
				resetToDefaults();
			}
		};

		_actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(
				Messages.Image__App_RestoreDefault));
		_actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

		_actionPrefDialog = new ActionOpenPrefDialog(MAP_ACTION_EDIT2D_MAP_PREFERENCES, PrefPageMap2Appearance.ID);
		_actionPrefDialog.closeThisTooltip(this);
		_actionPrefDialog.setShell(_parent.getShell());
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		initUI(parent);

		createActions();

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(shellContainer);
		{
			createUI_10_Header(shellContainer);
			createUI_20_SliderPath(shellContainer);
			createUI_30_MapOptions(shellContainer);
		}

		return shellContainer;
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				/*
				 * Slideout title
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Slideout_Map2_MapOptions_Title);
				MTFont.setBannerFont(label);
				GridDataFactory
						.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(label);
			}
			{
				/*
				 * Actionbar
				 */
				final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.END, SWT.BEGINNING)
						.applyTo(toolbar);

				final ToolBarManager tbm = new ToolBarManager(toolbar);

				tbm.add(_actionRestoreDefaults);
				tbm.add(_actionPrefDialog);

				tbm.update(true);
			}
		}
	}

	private void createUI_20_SliderPath(final Composite parent) {

		{
			/*
			 * Slider path
			 */
			// checkbox
			_chkSliderPath = new Button(parent, SWT.CHECK);
			_chkSliderPath.setText(Messages.Slideout_Map2_MapOptions_Checkbox_SliderPath);
			_chkSliderPath.setToolTipText(Messages.Slideout_Map2_MapOptions_Checkbox_SliderPath_Tooltip);
			_chkSliderPath.addSelectionListener(_defaultState_SelectionListener);
		}

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()
				.grab(true, false)
				.indent(_pc.convertWidthInCharsToPixels(3), 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Segments
				 */

				// label
				_lblSliderPath_Segments = new Label(container, SWT.NONE);
				_lblSliderPath_Segments.setText(Messages.Slideout_Map2_MapOptions_Label_SliderPath_Segements);

				// spinner
				_spinnerSliderPath_Segments = new Spinner(container, SWT.BORDER);
				_spinnerSliderPath_Segments.setMinimum(1);
				_spinnerSliderPath_Segments.setMaximum(10000);
				_spinnerSliderPath_Segments.setIncrement(1);
				_spinnerSliderPath_Segments.setPageIncrement(10);
				_spinnerSliderPath_Segments.addSelectionListener(_defaultState_SelectionListener);
				_spinnerSliderPath_Segments.addMouseWheelListener(_defaultState_MouseWheelListener);
			}
			{
				/*
				 * Line width
				 */

				// label
				_lblSliderPath_Width = new Label(container, SWT.NONE);
				_lblSliderPath_Width.setText(Messages.Slideout_Map2_MapOptions_Label_SliderPath_Width);

				// spinner
				_spinnerSliderPath_LineWidth = new Spinner(container, SWT.BORDER);
				_spinnerSliderPath_LineWidth.setMinimum(1);
				_spinnerSliderPath_LineWidth.setMaximum(200);
				_spinnerSliderPath_LineWidth.setIncrement(1);
				_spinnerSliderPath_LineWidth.setPageIncrement(10);
				_spinnerSliderPath_LineWidth.addSelectionListener(_defaultState_SelectionListener);
				_spinnerSliderPath_LineWidth.addMouseWheelListener(_defaultState_MouseWheelListener);
			}
			{
				/*
				 * Color + opacity
				 */

				// label
				_lblSliderPath_Color = new Label(container, SWT.NONE);
				_lblSliderPath_Color.setText(Messages.Slideout_Map2_MapOptions_Label_SliderPath_Color);

				final Composite colorContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(colorContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(colorContainer);
				{
					// color
					_colorSliderPathColor = new ColorSelectorExtended(colorContainer);
					_colorSliderPathColor.addListener(_defaultState_ChangePropertyListener);
					_colorSliderPathColor.addOpenListener(this);

					// opacity
					_spinnerSliderPath_Opacity = new Spinner(colorContainer, SWT.BORDER);
					_spinnerSliderPath_Opacity.setMinimum(1);
					_spinnerSliderPath_Opacity.setMaximum(100);
					_spinnerSliderPath_Opacity.setIncrement(1);
					_spinnerSliderPath_Opacity.setPageIncrement(10);
					_spinnerSliderPath_Opacity.addSelectionListener(_defaultState_SelectionListener);
					_spinnerSliderPath_Opacity.addMouseWheelListener(_defaultState_MouseWheelListener);
				}
			}
		}
	}

	private void createUI_30_MapOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Zoom to mouse position
				 */
				{
					// checkbox
					_chkZoomWithMousePosition = new Button(container, SWT.CHECK);
					_chkZoomWithMousePosition.setText(
							Messages.Slideout_Map2_MapOptions_Checkbox_ZoomWithMousePosition);
					_chkZoomWithMousePosition.setToolTipText(
							Messages.Slideout_Map2_MapOptions_Checkbox_ZoomWithMousePosition_Tooltip);
					_chkZoomWithMousePosition.addSelectionListener(_defaultState_SelectionListener);
					GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkZoomWithMousePosition);
				}
			}
			{
				/*
				 * Tour track opacity
				 */
				{
					// checkbox
					_chkTrackOpacity = new Button(container, SWT.CHECK);
					_chkTrackOpacity.setText(Messages.Pref_Map2_Checkbox_TrackOpacity);
					_chkTrackOpacity.setToolTipText(Messages.Pref_Map2_Checkbox_TrackOpacity_Tooltip);
					_chkTrackOpacity.addSelectionListener(_defaultSelectionListener);
				}
				{
					// spinner
					_spinnerTrackOpacity = new Spinner(container, SWT.BORDER);
					_spinnerTrackOpacity.setMinimum(PrefPageMap2Appearance.MAP_OPACITY_MINIMUM);
					_spinnerTrackOpacity.setMaximum(100);
					_spinnerTrackOpacity.setIncrement(1);
					_spinnerTrackOpacity.setPageIncrement(10);
					_spinnerTrackOpacity.addSelectionListener(_defaultSelectionListener);
					_spinnerTrackOpacity.addMouseWheelListener(_defaultMouseWheelListener);
				}
			}
		}
	}

	private void enableControls() {

		final boolean isUseTrackOpacity = _chkTrackOpacity.getSelection();
		final boolean isShowSliderPath = _chkSliderPath.getSelection();

		_spinnerTrackOpacity.setEnabled(isUseTrackOpacity);

		_colorSliderPathColor.setEnabled(isShowSliderPath);
		_lblSliderPath_Color.setEnabled(isShowSliderPath);
		_lblSliderPath_Width.setEnabled(isShowSliderPath);
		_lblSliderPath_Segments.setEnabled(isShowSliderPath);
		_spinnerSliderPath_Opacity.setEnabled(isShowSliderPath);
		_spinnerSliderPath_Segments.setEnabled(isShowSliderPath);
		_spinnerSliderPath_LineWidth.setEnabled(isShowSliderPath);
	}

	private void initUI(final Composite parent) {

		_parent = parent;

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_defaultState_ChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI_MapUpdate();
			}
		};

		_defaultState_MouseWheelListener = new MouseWheelListener() {
			@Override
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI_MapUpdate();
			}
		};
		_defaultState_SelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI_MapUpdate();
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

	private void onChangeUI() {

		saveState();

		enableControls();
	}

	private void onChangeUI_MapUpdate() {

		onChangeUI();

		_map2View.restoreState_Map2Options(true);
	}

	private void resetToDefaults() {

// SET_FORMATTING_OFF

		_chkZoomWithMousePosition.setSelection(		Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT);
		
		// slider path
		_chkSliderPath.setSelection(				Map2View.STATE_IS_SHOW_SLIDER_PATH_DEFAULT);
		_spinnerSliderPath_Opacity.setSelection(	Map2View.STATE_SLIDER_PATH_OPACITY_DEFAULT);
		_spinnerSliderPath_Segments.setSelection(	Map2View.STATE_SLIDER_PATH_SEGMENTS_DEFAULT);
		_spinnerSliderPath_LineWidth.setSelection(	Map2View.STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT);
		_colorSliderPathColor.setColorValue(		Map2View.STATE_SLIDER_PATH_COLOR_DEFAULT);
		
		// track opacity
		_chkTrackOpacity.setSelection(		_prefStore.getDefaultBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(	_prefStore.getDefaultInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
		
// SET_FORMATTING_ON

		onChangeUI_MapUpdate();
	}

	private void restoreState() {

// SET_FORMATTING_OFF

		_chkZoomWithMousePosition.setSelection(		Util.getStateBoolean(_state, 	Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,	Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION_DEFAULT));
		
		// slider path
		_chkSliderPath.setSelection(				Util.getStateBoolean(_state, 	Map2View.STATE_IS_SHOW_SLIDER_PATH, 	Map2View.STATE_IS_SHOW_SLIDER_PATH_DEFAULT));
		_spinnerSliderPath_Opacity.setSelection(	Util.getStateInt(_state, 		Map2View.STATE_SLIDER_PATH_OPACITY,		Map2View.STATE_SLIDER_PATH_OPACITY_DEFAULT));
		_spinnerSliderPath_Segments.setSelection(	Util.getStateInt(_state, 		Map2View.STATE_SLIDER_PATH_SEGMENTS,	Map2View.STATE_SLIDER_PATH_SEGMENTS_DEFAULT));
		_spinnerSliderPath_LineWidth.setSelection(	Util.getStateInt(_state, 		Map2View.STATE_SLIDER_PATH_LINE_WIDTH, 	Map2View.STATE_SLIDER_PATH_LINE_WIDTH_DEFAULT));
		_colorSliderPathColor.setColorValue(		Util.getStateRGB(_state, 		Map2View.STATE_SLIDER_PATH_COLOR, 		Map2View.STATE_SLIDER_PATH_COLOR_DEFAULT));
		
		// track opacity
		_chkTrackOpacity.setSelection(			_prefStore.getBoolean(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY));
		_spinnerTrackOpacity.setSelection(		_prefStore.getInt(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY));
				
// SET_FORMATTING_ON
	}

	private void saveState() {

// SET_FORMATTING_OFF
		
		_state.put(Map2View.STATE_IS_ZOOM_WITH_MOUSE_POSITION,	_chkZoomWithMousePosition.getSelection());
		
		// slider path
		_state.put(Map2View.STATE_IS_SHOW_SLIDER_PATH,			_chkSliderPath.getSelection());
		_state.put(Map2View.STATE_SLIDER_PATH_OPACITY, 			_spinnerSliderPath_Opacity.getSelection());
		_state.put(Map2View.STATE_SLIDER_PATH_SEGMENTS, 		_spinnerSliderPath_Segments.getSelection());
		_state.put(Map2View.STATE_SLIDER_PATH_LINE_WIDTH, 		_spinnerSliderPath_LineWidth.getSelection());
		Util.setState(_state, Map2View.STATE_SLIDER_PATH_COLOR, _colorSliderPathColor.getColorValue());

		// track opacity
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,		_chkTrackOpacity.getSelection());
		_prefStore.setValue(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,		_spinnerTrackOpacity.getSelection());

// SET_FORMATTING_ON

	}
}
