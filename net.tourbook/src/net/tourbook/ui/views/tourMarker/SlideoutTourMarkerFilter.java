/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourMarker;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
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
public class SlideoutTourMarkerFilter extends AnimatedToolTipShell implements IColorSelectorListener {

	private static final int		GEO_FILTER_DIGITS	= 4;

	private final IDialogSettings	_state				= TourbookPlugin.getState(TourMarkerAllView.ID);

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private TourMarkerAllView		_tourMarkerAllView;

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_isWaitTimerStarted;
	private boolean					_canOpenToolTip;
	private boolean					_isAnotherDialogOpened;

	private MouseWheelListener		_accuracyMouseWheelListener;
	private SelectionAdapter		_accuracySelectionListener;
	private SelectionAdapter		_defaultSelectionAdapter;

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Button					_chkLatLonAccuracy;

	private Label					_lblGeoFilter;

	private Spinner					_spinnerGeoFilter;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourMarkerFilter(final Control ownerControl,
									final ToolBar toolBar,
									final IDialogSettings state,
									final TourMarkerAllView tourMarkerAllView) {

		super(ownerControl);

		_tourMarkerAllView = tourMarkerAllView;

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
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		initUI(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().applyTo(_shellContainer);
		{
			final Composite container = new Composite(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().applyTo(container);
			{
				createUI_10_Checkboxes(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_10_Checkboxes(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Geo filter range
			 */
			{
				_lblGeoFilter = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.applyTo(_lblGeoFilter);
				_lblGeoFilter.setText(Messages.Slideout_TourMarkerFilter_Label_GeoFilterDiff);

				_spinnerGeoFilter = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
//						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_spinnerGeoFilter);
				_spinnerGeoFilter.setMinimum(0);
				_spinnerGeoFilter.setMaximum((int) (1 * Math.pow(10, GEO_FILTER_DIGITS)));
				_spinnerGeoFilter.setDigits(GEO_FILTER_DIGITS);
				_spinnerGeoFilter.setPageIncrement(50);
				_spinnerGeoFilter.addMouseWheelListener(_accuracyMouseWheelListener);
				_spinnerGeoFilter.addSelectionListener(_accuracySelectionListener);
			}
//			{
//				/*
//				 * Show hidden marker
//				 */
//				_chkLatLonAccuracy = new Button(container, SWT.CHECK);
//				GridDataFactory.fillDefaults()//
//						.span(2, 1)
//						.applyTo(_chkLatLonAccuracy);
//				_chkLatLonAccuracy.setText(Messages.Slideout_ChartMarkerOptions_Checkbox_IsShowHiddenMarker);
//				_chkLatLonAccuracy.addSelectionListener(_defaultSelectionAdapter);
//			}

		}
	}

	private void enableControls() {

		final boolean isGeoFilterActive = _tourMarkerAllView.isGeoFilterActive();

		_lblGeoFilter.setEnabled(isGeoFilterActive);
		_spinnerGeoFilter.setEnabled(isGeoFilterActive);
	}

	public Shell getShell() {

		if (_shellContainer == null) {
			return null;
		}

		return _shellContainer.getShell();
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int itemHeight = _toolTipItemBounds.height;

		final int devX = _toolTipItemBounds.x - tipSize.x / 2;
		final int devY = _toolTipItemBounds.y + itemHeight;

		return new Point(devX, devY);
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_accuracyMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				UI.adjustSpinnerValueOnMouseScroll(event);
				onSelection_GeoFilter();
			}
		};

		_accuracySelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelection_GeoFilter();
			}
		};

		_defaultSelectionAdapter = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onChangeUI();
			}
		};
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeUI() {

		saveState();

//		_tourMarkerAllView.updateUI();
	}

	private void onSelection_GeoFilter() {

		final double accuracy = _spinnerGeoFilter.getSelection() / Math.pow(10, GEO_FILTER_DIGITS);

		_state.put(TourMarkerAllView.STATE_GEO_FILTER_ACCURACY, accuracy);

		_tourMarkerAllView.updateUI_GeoFilter();
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

		final double stateGeoFilterRange = Util.getStateDouble(_state,//
				TourMarkerAllView.STATE_GEO_FILTER_ACCURACY,
				TourMarkerAllView.DEFAULT_GEO_FILTER_ACCURACY);

		_spinnerGeoFilter.setSelection((int) (stateGeoFilterRange * Math.pow(10, GEO_FILTER_DIGITS)));
	}

	private void saveState() {

	}

}
