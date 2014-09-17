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
package net.tourbook.ui.tourChart;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.data.TourMarker;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartMarker extends AnimatedToolTipShell implements IColorSelectorListener {

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;
	private IPropertyChangeListener	_defaultPropertyChangeListener;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onChangeUI();
			}
		};

		_defaultPropertyChangeListener = new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI();
			}
		};
	}

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;
	private TourChart				_tourChart;

	private Button					_chkShowHiddenMarker;
	private Button					_chkShowLabelTempPosition;
	private Button					_chkShowMarkerLabel;
	private Button					_chkShowMarkerPoint;
	private Button					_chkShowMarkerTooltip;
//	private Button					_chkShowSignImage;
	private Button					_chkDrawMarkerWithDefaultColor;

	/**
	 * Label temporary position, this position is not saved in the marker.
	 */
	private Combo					_comboLabelTempPosition;
	private Combo					_comboTooltipPosition;

	private ColorSelectorExtended	_colorDefaultMarker;
	private ColorSelectorExtended	_colorDeviceMarker;
	private ColorSelectorExtended	_colorHiddenMarker;

	private Label					_lblLabelOffset;
	private Label					_lblMarkerPointSize;
//	private Label					_lblSignImageSize;

	private Spinner					_spinHoverSize;
	private Spinner					_spinLabelOffset;
	private Spinner					_spinMarkerPointSize;

//	private Spinner					_spinSignImageSize;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartMarker(	final Control ownerControl,
									final ToolBar toolBar,
									final IDialogSettings state,
									final TourChart tourChart) {

		super(ownerControl);

		_tourChart = tourChart;

		addListener(ownerControl, toolBar);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
		setFadeOutSteps(10);
		setFadeOutDelaySteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {

	}

	@Override
	protected boolean canCloseToolTip() {

		/*
		 * Do not hide this dialog when the color selector dialog or other dialogs are opened
		 * because it will lock the UI completely !!!
		 */

		final boolean isCanClose = _isAnotherDialogOpened == false;

		return isCanClose;
	}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected boolean closeShellAfterHidden() {

		/*
		 * Close the tooltip that the state is saved.
		 */

		return true;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

		fillUI();
		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().applyTo(container);
			{
				createUI_10_Checkboxes(container);
				createUI_50_TempPosition(container);
				createUI_90_Bottom(container);

			}
		}

		_shellContainer.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});

		return _shellContainer;
	}

	private void createUI_10_Checkboxes(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			{
				final Composite ttContainer = new Composite(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.span(2, 1)
						.applyTo(ttContainer);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ttContainer);
				{
					{
						/*
						 * Show marker tooltip
						 */
						_chkShowMarkerTooltip = new Button(ttContainer, SWT.CHECK);
						GridDataFactory.fillDefaults()//
								.applyTo(_chkShowMarkerTooltip);
						_chkShowMarkerTooltip.setText(//
								Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerTooltip);
						_chkShowMarkerTooltip.addSelectionListener(_defaultSelectionAdapter);
					}
					{
						/*
						 * Combo: tooltip position
						 */
						_comboTooltipPosition = new Combo(ttContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
						GridDataFactory.fillDefaults()//
//								.indent(_pc.convertWidthInCharsToPixels(3), 0)
								.grab(true, false)
								.align(SWT.END, SWT.FILL)
								.applyTo(_comboTooltipPosition);
						_comboTooltipPosition.setVisibleItemCount(20);
						_comboTooltipPosition.setToolTipText(//
								Messages.Slideout_ChartMarkerOptions_Combo_TooltipPosition_Tooltip);
						_comboTooltipPosition.addSelectionListener(_defaultSelectionAdapter);
					}
				}
			}

			{
				/*
				 * Show labels
				 */
				// show label
				_chkShowMarkerLabel = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowMarkerLabel);
				_chkShowMarkerLabel.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarker);
				_chkShowMarkerLabel.addSelectionListener(_defaultSelectionAdapter);
			}

			{
				/*
				 * Show marker point
				 */
				_chkShowMarkerPoint = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowMarkerPoint);
				_chkShowMarkerPoint.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerPoint);
				_chkShowMarkerPoint.addSelectionListener(_defaultSelectionAdapter);
			}

//			{
//				/*
//				 * Show sign image
//				 */
//				_chkShowSignImage = new Button(container, SWT.CHECK);
//				GridDataFactory.fillDefaults()//
//						.span(2, 1)
//						.applyTo(_chkShowSignImage);
//				_chkShowSignImage.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowSignImage);
//				_chkShowSignImage.addSelectionListener(_defaultSelectionAdapter);
//			}

			{
				/*
				 * Show hidden marker
				 */
				_chkShowHiddenMarker = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowHiddenMarker);
				_chkShowHiddenMarker.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
				_chkShowHiddenMarker.addSelectionListener(_defaultSelectionAdapter);
			}

			{
				/*
				 * Draw marker with default color
				 */
				_chkDrawMarkerWithDefaultColor = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkDrawMarkerWithDefaultColor);
				_chkDrawMarkerWithDefaultColor.setText(//
						Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor);
				_chkDrawMarkerWithDefaultColor.setToolTipText(//
						Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarkerWithDefaultColor_Tooltip);
				_chkDrawMarkerWithDefaultColor.addSelectionListener(_defaultSelectionAdapter);
			}
		}
	}

	private void createUI_50_TempPosition(final Composite parent) {

		{
			/*
			 * Temp position
			 */
			final Composite tempContainer = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(2, 1)
					.applyTo(tempContainer);
			GridLayoutFactory.fillDefaults().numColumns(1).applyTo(tempContainer);
//			tempContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			{
				// show temp position
				_chkShowLabelTempPosition = new Button(tempContainer, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.applyTo(_chkShowLabelTempPosition);
				_chkShowLabelTempPosition.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition);
				_chkShowLabelTempPosition.setToolTipText(//
						Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowTempPosition_Tooltip);
				_chkShowLabelTempPosition.addSelectionListener(_defaultSelectionAdapter);

				{
					/*
					 * Combo: temp position
					 */
					_comboLabelTempPosition = new Combo(tempContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
					GridDataFactory.fillDefaults()//
							.indent(_pc.convertWidthInCharsToPixels(3), 0)
							.applyTo(_comboLabelTempPosition);
					_comboLabelTempPosition.setVisibleItemCount(20);
					_comboLabelTempPosition.addSelectionListener(_defaultSelectionAdapter);
				}
			}
		}
	}

	private void createUI_90_Bottom(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(_pc.convertWidthInCharsToPixels(4), 0)
				.applyTo(container);
		{
			createUI_92_Sizes(container);
			createUI_94_Colors(container);
		}
	}

	private void createUI_92_Sizes(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			{
				/*
				 * Label offset
				 */

				// Label
				_lblLabelOffset = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblLabelOffset);
				_lblLabelOffset.setText(Messages.Slideout_ChartMarkerOptions_Label_Offset);
				_lblLabelOffset.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_Offset_Tooltip);

				// Spinner
				_spinLabelOffset = new Spinner(container, SWT.BORDER);
				_spinLabelOffset.setMinimum(-99);
				_spinLabelOffset.setMaximum(100);
				_spinLabelOffset.setPageIncrement(5);
				_spinLabelOffset.addSelectionListener(_defaultSelectionAdapter);
				_spinLabelOffset.addMouseWheelListener(_defaultMouseWheelListener);
			}

			{
				/*
				 * Marker point size
				 */

				// Label
				_lblMarkerPointSize = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblMarkerPointSize);
				_lblMarkerPointSize.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize);
				_lblMarkerPointSize.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize_Tooltip);

				// Spinner
				_spinMarkerPointSize = new Spinner(container, SWT.BORDER);
				_spinMarkerPointSize.setMinimum(0);
				_spinMarkerPointSize.setMaximum(100);
				_spinMarkerPointSize.setPageIncrement(5);
				_spinMarkerPointSize.addSelectionListener(_defaultSelectionAdapter);
				_spinMarkerPointSize.addMouseWheelListener(_defaultMouseWheelListener);
			}

//			{
//				/*
//				 * Sign image size
//				 */
//
//				// Label
//				_lblSignImageSize = new Label(container, SWT.NONE);
//				GridDataFactory.fillDefaults()//
//						.align(SWT.FILL, SWT.CENTER)
//						.applyTo(_lblSignImageSize);
//				_lblSignImageSize.setText(Messages.Slideout_ChartMarkerOptions_Label_SignImageSize);
//				_lblSignImageSize.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_SignImageSize_Tooltip);
//
//				// Spinner
//				_spinSignImageSize = new Spinner(container, SWT.BORDER);
//				_spinSignImageSize.setMinimum(10);
//				_spinSignImageSize.setMaximum(200);
//				_spinSignImageSize.setPageIncrement(5);
//				_spinSignImageSize.addSelectionListener(_defaultSelectionAdapter);
//				_spinSignImageSize.addMouseWheelListener(_defaultMouseWheelListener);
//			}

			{
				/*
				 * Hover size
				 */

				// Label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize);
				label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HoverSize_Tooltip);

				// Spinner
				_spinHoverSize = new Spinner(container, SWT.BORDER);
				_spinHoverSize.setMinimum(0);
				_spinHoverSize.setMaximum(100);
				_spinHoverSize.setPageIncrement(5);
				_spinHoverSize.addSelectionListener(_defaultSelectionAdapter);
				_spinHoverSize.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_94_Colors(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.BEGINNING)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * Default color
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerColor);

				// Color selector
				_colorDefaultMarker = new ColorSelectorExtended(container);
				GridDataFactory.swtDefaults()//
						.grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_colorDefaultMarker.getButton());

				_colorDefaultMarker.addOpenListener(this);
				_colorDefaultMarker.addListener(_defaultPropertyChangeListener);
			}

			/*
			 * Device marker color
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor);
				label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_DeviceMarkerColor_Tooltip);

				// Color selector
				_colorDeviceMarker = new ColorSelectorExtended(container);
				GridDataFactory.swtDefaults()//
						.grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_colorDeviceMarker.getButton());

				_colorDeviceMarker.addOpenListener(this);
				_colorDeviceMarker.addListener(_defaultPropertyChangeListener);
			}

			/*
			 * Hidden marker color
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor);
				label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_HiddenMarkerColor_Tooltip);

				// Color selector
				_colorHiddenMarker = new ColorSelectorExtended(container);
				GridDataFactory.swtDefaults()//
						.grab(false, true)
						.align(SWT.BEGINNING, SWT.BEGINNING)
						.applyTo(_colorHiddenMarker.getButton());

				_colorHiddenMarker.addOpenListener(this);
				_colorHiddenMarker.addListener(_defaultPropertyChangeListener);
			}
		}
	}

	private void enableControls() {

		final boolean isShowTempPosition = _chkShowLabelTempPosition.getSelection();
		final boolean isLabelVisible = _chkShowMarkerLabel.getSelection();
//		final boolean isImageVisible = _chkShowSignImage.getSelection();
		final boolean isMarkerPointVisible = _chkShowMarkerPoint.getSelection();
		final boolean isTooltipVisible = _chkShowMarkerTooltip.getSelection();

		final boolean isMarkerVisible = isLabelVisible || isMarkerPointVisible;// || isImageVisible;

		_comboTooltipPosition.setEnabled(isTooltipVisible);

		_chkShowLabelTempPosition.setEnabled(isLabelVisible);
		_comboLabelTempPosition.setEnabled(isLabelVisible && isShowTempPosition);

		_chkShowHiddenMarker.setEnabled(isMarkerVisible);
		_chkDrawMarkerWithDefaultColor.setEnabled(isMarkerVisible);

		_lblLabelOffset.setEnabled(isLabelVisible);
		_spinLabelOffset.setEnabled(isLabelVisible);

		_lblMarkerPointSize.setEnabled(isMarkerPointVisible);
		_spinMarkerPointSize.setEnabled(isMarkerPointVisible);

//		_lblSignImageSize.setEnabled(isImageVisible);
//		_spinSignImageSize.setEnabled(isImageVisible);
	}

	private void fillUI() {

		/*
		 * Fill position combos
		 */
		for (final String position : TourMarker.LABEL_POSITIONS) {
			_comboLabelTempPosition.add(position);
		}

		for (final String position : ChartMarkerToolTip.TOOLTIP_POSITIONS) {
			_comboTooltipPosition.add(position);
		}

	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
//
//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeUI() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		final boolean isDrawMarkerWithDefaultColor = _chkDrawMarkerWithDefaultColor.getSelection();
		final boolean isShowHiddenMarker = _chkShowHiddenMarker.getSelection();
		final boolean isShowLabelTempPos = _chkShowLabelTempPosition.getSelection();
		final boolean isShowMarkerLabel = _chkShowMarkerLabel.getSelection();
		final boolean isShowMarkerPoint = _chkShowMarkerPoint.getSelection();
		final boolean isShowMarkerTooltip = _chkShowMarkerTooltip.getSelection();
//		final boolean isShowSignImage = _chkShowSignImage.getSelection();

		final int hoverSize = _spinHoverSize.getSelection();
		final int labelOffset = _spinLabelOffset.getSelection();
		final int markerPointSize = _spinMarkerPointSize.getSelection();
//		final int signImageSize = _spinSignImageSize.getSelection();
		final int tempPosition = _comboLabelTempPosition.getSelectionIndex();
		final int ttPosition = _comboTooltipPosition.getSelectionIndex();

		final RGB defaultColor = _colorDefaultMarker.getColorValue();
		final RGB deviceColor = _colorDeviceMarker.getColorValue();
		final RGB hiddenColor = _colorHiddenMarker.getColorValue();

		/*
		 * Update pref store
		 */
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE, hoverSize);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET, labelOffset);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION, tempPosition);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE, markerPointSize);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION, ttPosition);
//		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_SIGN_IMAGE_SIZE, signImageSize);

		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR, isDrawMarkerWithDefaultColor);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER, isShowHiddenMarker);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION, isShowLabelTempPos);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL, isShowMarkerLabel);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT, isShowMarkerPoint);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP, isShowMarkerTooltip);
//		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_SIGN_IMAGE, isShowSignImage);

		PreferenceConverter.setValue(_prefStore, //
				ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT,
				defaultColor);
		PreferenceConverter.setValue(_prefStore, //
				ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE,
				deviceColor);
		PreferenceConverter.setValue(_prefStore, //
				ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN,
				hiddenColor);

		/*
		 * Update chart config
		 */
		tcc.isDrawMarkerWithDefaultColor = isDrawMarkerWithDefaultColor;
		tcc.isShowHiddenMarker = isShowHiddenMarker;
		tcc.isShowLabelTempPos = isShowLabelTempPos;
		tcc.isShowMarkerLabel = isShowMarkerLabel;
		tcc.isShowMarkerPoint = isShowMarkerPoint;
		tcc.isShowMarkerTooltip = isShowMarkerTooltip;
//		tcc.isShowSignImage = isShowSignImage;

		tcc.markerHoverSize = hoverSize;
		tcc.markerLabelOffset = labelOffset;
		tcc.markerLabelTempPos = tempPosition;
		tcc.markerPointSize = markerPointSize;
		tcc.markerTooltipPosition = ttPosition;
//		tcc.markerSignImageSize = signImageSize;

		tcc.markerColorDefault = defaultColor;
		tcc.markerColorDevice = deviceColor;
		tcc.markerColorHidden = hiddenColor;

		// update chart with new settings
		_tourChart.updateUI_MarkerLayer();

		enableControls();

		// notify pref listener
		TourbookPlugin.getDefault().getPreferenceStore()//
				.setValue(ITourbookPreferences.GRAPH_MARKER_IS_MODIFIED, Math.random());
	}

	private void onDispose() {

	}

	/**
	 * @param toolTipItemBounds
	 * @param isOpenDelayed
	 */
	public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

		if (isToolTipVisible()) {

			return;
		}

		if (isOpenDelayed == false) {

			if (toolTipItemBounds != null) {

				_toolTipItemBounds = toolTipItemBounds;

				showToolTip();
			}

		} else {

			if (toolTipItemBounds == null) {

				// item is not hovered any more

				_canOpenToolTip = false;

				return;
			}

			_toolTipItemBounds = toolTipItemBounds;
			_canOpenToolTip = true;

			if (_isWaitTimerStarted == false) {

				_isWaitTimerStarted = true;

				Display.getCurrent().timerExec(50, _waitTimer);
			}
		}
	}

	private void open_Runnable() {

		_isWaitTimerStarted = false;

		if (_canOpenToolTip) {
			showToolTip();
		}
	}

	private void restoreState() {

		final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

		final int markerTooltipPosition = tcc.markerTooltipPosition < 0
				? ChartMarkerToolTip.DEFAULT_TOOLTIP_POSITION
				: tcc.markerTooltipPosition;

		_chkDrawMarkerWithDefaultColor.setSelection(tcc.isDrawMarkerWithDefaultColor);
		_chkShowHiddenMarker.setSelection(tcc.isShowHiddenMarker);
		_chkShowMarkerLabel.setSelection(tcc.isShowMarkerLabel);
		_chkShowMarkerPoint.setSelection(tcc.isShowMarkerPoint);
		_chkShowMarkerTooltip.setSelection(tcc.isShowMarkerTooltip);
//		_chkShowSignImage.setSelection(tcc.isShowSignImage);
		_chkShowLabelTempPosition.setSelection(tcc.isShowLabelTempPos);

		_comboLabelTempPosition.select(tcc.markerLabelTempPos);
		_comboTooltipPosition.select(markerTooltipPosition);

		_colorDefaultMarker.setColorValue(tcc.markerColorDefault);
		_colorDeviceMarker.setColorValue(tcc.markerColorDevice);
		_colorHiddenMarker.setColorValue(tcc.markerColorHidden);

		_spinHoverSize.setSelection(tcc.markerHoverSize);
		_spinLabelOffset.setSelection(tcc.markerLabelOffset);
		_spinMarkerPointSize.setSelection(tcc.markerPointSize);
//		_spinSignImageSize.setSelection(tcc.markerSignImageSize);
	}

}
