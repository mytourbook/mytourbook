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
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
public class SlideoutMarkerOptions extends AnimatedToolTipShell {

	// initialize with default values which are (should) never be used
	private Rectangle			_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer		_waitTimer			= new WaitTimer();

	private boolean				_canOpenToolTip;
	private boolean				_isWaitTimerStarted;

	private SelectionAdapter	_defaultSelectionAdapter;
	private MouseWheelListener	_defaultMouseWheelListener;
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
	private Composite			_shellContainer;

	private Spinner				_spinMarkerSize;

	private TourChart			_tourChart;

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

		return true;
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
				/*
				 * Marker size
				 */
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

		_tourChart.updateUI_MarkerOptions(_spinMarkerSize.getSelection());
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

		_spinMarkerSize.setSelection(tcc.markerPointSize);
	}

}
