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
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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
public class SlideoutMarkerOptions extends AnimatedToolTipShell implements IColorSelectorListener {

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private SelectionAdapter		_defaultSelectionAdapter;
	private MouseWheelListener		_defaultMouseWheelListener;
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

//		_defaultModifyListener = new ModifyListener() {
//			@Override
//			public void modifyText(final ModifyEvent e) {
//				if (_isUpdateUI) {
//					return;
//				}
//				onChangeMarkerUI();
//			}
//		};
	}
	/*
	 * UI controls
	 */
	private TourChart				_tourChart;

	private Button					_chkShowHiddenMarker;
	private Button					_chkShowMarkerLabel;
	private Composite				_shellContainer;
	private ColorSelectorExtended	_colorDefaultMarker;
	private ColorSelectorExtended	_colorDeviceMarker;
	private ColorSelectorExtended	_colorHiddenMarker;
	private Spinner					_spinMarkerSize;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutMarkerOptions(	final Control ownerControl,
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

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				createUI_10_Left(container);
				createUI_20_Right(container);

//				/*
//				 * Link:
//				 */
//				final Link link = new Link(container, SWT.NONE);
//				GridDataFactory.fillDefaults()//
//						.align(SWT.FILL, SWT.END)
//						.applyTo(link);
//				link.setText(Messages.TourChart_Smoothing_Link_PrefBreakTime);
//				link.setEnabled(true);
//				link.addSelectionListener(new SelectionAdapter() {
//					@Override
//					public void widgetSelected(final SelectionEvent e) {
//						PreferencesUtil.createPreferenceDialogOn(
//								parent.getShell(),
//								PrefPageComputedValues.ID,
//								null,
//								PrefPageComputedValues.TAB_FOLDER_BREAK_TIME).open();
//					}
//				});

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

	private void createUI_10_Left(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * Show labels
			 */
			{
				_chkShowMarkerLabel = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowMarkerLabel);
				_chkShowMarkerLabel.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowMarker);
				_chkShowMarkerLabel.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeUI();
					}
				});
			}

			/*
			 * Show hidden labels
			 */
			{
				_chkShowHiddenMarker = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkShowHiddenMarker);
				_chkShowHiddenMarker.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
				_chkShowHiddenMarker.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onChangeUI();
					}
				});
			}
			/*
			 * Marker size
			 */
			{
				// Label
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(label);
				label.setText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize);
				label.setToolTipText(Messages.Slideout_ChartMarkerOptions_Label_MarkerSize_Tooltip);

				// Spinner
				_spinMarkerSize = new Spinner(container, SWT.BORDER);
				_spinMarkerSize.setMinimum(0);
				_spinMarkerSize.setMaximum(20);
				_spinMarkerSize.setPageIncrement(5);
				_spinMarkerSize.addSelectionListener(_defaultSelectionAdapter);
				_spinMarkerSize.addMouseWheelListener(_defaultMouseWheelListener);
			}
		}
	}

	private void createUI_20_Right(final Composite parent) {

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
				_colorDefaultMarker.addListener(new IPropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent event) {
						onChangeUI();
					}
				});
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
				_colorDeviceMarker.addListener(new IPropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent event) {
						onChangeUI();
					}
				});
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
				_colorHiddenMarker.addListener(new IPropertyChangeListener() {
					public void propertyChange(final PropertyChangeEvent event) {
						onChangeUI();
					}
				});
			}
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

		final boolean isShowHiddenMarker = _chkShowHiddenMarker.getSelection();
		final boolean isShowMarkerLabel = _chkShowMarkerLabel.getSelection();
		final RGB defaultColor = _colorDefaultMarker.getColorValue();
		final RGB deviceColor = _colorDeviceMarker.getColorValue();
		final RGB hiddenColor = _colorHiddenMarker.getColorValue();
		final int markerPointSize = _spinMarkerSize.getSelection();

		/*
		 * Update pref store
		 */
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE, markerPointSize);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER, isShowHiddenMarker);
		_prefStore.setValue(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL, isShowMarkerLabel);

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
		tcc.markerPointSize = markerPointSize;
		tcc.isShowHiddenMarker = isShowHiddenMarker;
		tcc.isShowMarkerLabel = isShowMarkerLabel;

		tcc.markerColorDefault = defaultColor;
		tcc.markerColorDevice = deviceColor;
		tcc.markerColorHidden = hiddenColor;

		// update chart with new settings
		_tourChart.updateUI_MarkerOptions();
	}

	private void onDispose() {

		Map3Manager.setMap3LayerDialog(null);
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

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

		_chkShowHiddenMarker.setSelection(tcc.isShowHiddenMarker);
		_chkShowMarkerLabel.setSelection(tcc.isShowMarkerLabel);
		_colorDefaultMarker.setColorValue(tcc.markerColorDefault);
		_colorDeviceMarker.setColorValue(tcc.markerColorDevice);
		_colorHiddenMarker.setColorValue(tcc.markerColorHidden);
		_spinMarkerSize.setSelection(tcc.markerPointSize);
	}

}
