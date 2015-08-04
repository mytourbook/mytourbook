/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourSegmenter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartSegmenterConfig extends AnimatedToolTipShell {

	private static final IDialogSettings	_segmenterState		= TourSegmenterView.getState();

	// initialize with default values which are (should) never be used
	private Rectangle						_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer					_waitTimer			= new WaitTimer();

	private boolean							_isWaitTimerStarted;
	private boolean							_canOpenToolTip;

	private IPropertyChangeListener			_defaultChangePropertyListener;
	private SelectionAdapter				_defaultSelectionAdapter;
	private MouseWheelListener				_defaultMouseWheelListener;

	{
		_defaultSelectionAdapter = new SelectionAdapter() {
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

		_defaultChangePropertyListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				onChangeUI();
			}
		};
	}

	private PixelConverter					_pc;
	private TourSegmenterView				_tourSegmenterView;

	/*
	 * UI controls
	 */
	private Composite						_shellContainer;

	private Button							_chkShowSegmentValues;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public SlideoutTourChartSegmenterConfig(final Control ownerControl,
											final ToolBar toolBar,
											final TourSegmenterView tourSegmenterView) {

		super(ownerControl);

		_tourSegmenterView = tourSegmenterView;

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
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite ui = createUI(parent);

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
						 * Label: Title
						 */
						final Label label = new Label(ttContainer, SWT.NONE);
						GridDataFactory.fillDefaults().applyTo(label);
						label.setText(Messages.Slideout_SegmenterChartOptions_Label_Title);

					}
					{
						/*
						 * Checkbox: Show segment values
						 */
						_chkShowSegmentValues = new Button(ttContainer, SWT.CHECK);
						GridDataFactory.fillDefaults()//
								.span(2, 1)
								.applyTo(_chkShowSegmentValues);
						_chkShowSegmentValues
								.setText(Messages.Slideout_SegmenterChartOptions_Checkbox_IsShowSegmentValues);
						_chkShowSegmentValues.addSelectionListener(_defaultSelectionAdapter);
					}
				}
			}
		}
	}

	private void enableControls() {

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
		final int tipHeight = tipSize.y;

//		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		// center horizontally
		final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
		int devY = _toolTipItemBounds.y + itemHeight + 0;

		final Rectangle displayBounds = this.getShell().getDisplay().getBounds();

		if (devY + tipHeight > displayBounds.height) {

			// slideout is below bottom, show it above the action button

			devY = _toolTipItemBounds.y - tipHeight;
		}

		return new Point(devX, devY);

	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onChangeUI() {

		/*
		 * Update state
		 */
		_segmenterState.put(TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUES, _chkShowSegmentValues.getSelection());

		/*
		 * Update UI
		 */
		_tourSegmenterView.fireSegmentLayerChanged();

		enableControls();
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

		_chkShowSegmentValues.setSelection(Util.getStateBoolean(
				_segmenterState,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUES,
				TourSegmenterView.STATE_IS_SHOW_SEGMENTER_VALUES_DEFAULT));
	}

}
