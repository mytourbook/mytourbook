/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.WorldWind;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map3 tour track layer properties dialog.
 */
public class TourTrackPropertiesUI extends AnimatedToolTipShell {

	private static final int		SHELL_MARGIN		= 5;

	private static final int		MIN_IMAGE_WIDTH		= 10;

	/**
	 * This value is small because a map do not yet load large images !!!
	 */
	private static final int		MAX_IMAGE_WIDTH		= 200;

	private static final String[]	ALTITUDE_MODE_LABEL	= {
			Messages.Altitude_Mode_ClampToGround,
			Messages.Altitude_Mode_Absolute,
			Messages.Altitude_Mode_RelativeToGround,	};

	private static final int[]		ALTITUDE_MODE_VALUE	= {
			WorldWind.CLAMP_TO_GROUND,
			WorldWind.ABSOLUTE,
			WorldWind.RELATIVE_TO_GROUND,				};

	private IDialogSettings			_state;

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer			= new WaitTimer();

	private boolean					_canOpenToolTip;
	private boolean					_isWaitTimerStarted;
	private boolean					_isUpdateUI;

	private SelectionAdapter		_selectionListener;

	/*
	 * UI resources
	 */

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Combo					_comboAltitude;

	private TourTrackConfig			_tourTrackConfig;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public TourTrackPropertiesUI(final Control ownerControl, final ToolBar toolBar, final IDialogSettings state) {

		super(ownerControl);

		_state = state;

		addListener(ownerControl, toolBar);

		_selectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (_isUpdateUI) {
					return;
				}
				onModify();
			}
		};

//		PhotoManager.addPhotoEventListener(this);

		setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
		setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
		setIsKeepShellOpenWhenMoved(false);
		setFadeInSteps(1);
	}

	private void addListener(final Control ownerControl, final ToolBar toolBar) {

		toolBar.addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseExit(final MouseEvent e) {

				// prevent to open the tooltip
				_canOpenToolTip = false;
			}
		});

		ownerControl.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	protected void beforeHideToolTip() {}

	@Override
	protected boolean canShowToolTip() {
		return true;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite container = createUI(parent);

		updateUI();

		restoreState();

		enableControls();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults()//
				.margins(SHELL_MARGIN, SHELL_MARGIN)
				.numColumns(3)
				.applyTo(_shellContainer);
		{
			createUI_10(_shellContainer);

			// spacer
			final Label label = new Label(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().hint(20, 0).applyTo(label);
		}

		return _shellContainer;
	}

	private void createUI_10(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NO_FOCUS);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * label: Altitude
			 */
			final Label label = new Label(container, SWT.NO_FOCUS);
			GridDataFactory.fillDefaults().applyTo(label);

			label.setText(Messages.TourTrac_Properties_Label_Altitude);

			/*
			 * combo: Altitude
			 */
			_comboAltitude = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboAltitude);
			_comboAltitude.setVisibleItemCount(10);
			_comboAltitude.addSelectionListener(_selectionListener);

//			tourPath.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
//			tourPath.setAltitudeMode(WorldWind.ABSOLUTE);
//			tourPath.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
//			tourPath.setFollowTerrain(true);

		}
	}

	private void enableControls() {
		// TODO Auto-generated method stub

	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int tipWidth = tipSize.x;
//		final int tipHeight = tipSize.y;

		final int itemWidth = _toolTipItemBounds.width;
		final int itemHeight = _toolTipItemBounds.height;

		final int itemWidth2 = itemWidth / 2;
		final int tipWidth2 = tipWidth / 2;

		final int devX = _toolTipItemBounds.x + itemWidth2 - tipWidth2;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	@Override
	protected Rectangle noHideOnMouseMove() {
		return _toolTipItemBounds;
	}

	private void onDispose() {

	}

	private void onModify() {

		int altitudeMode = _comboAltitude.getSelectionIndex();
		if (altitudeMode == -1) {
			altitudeMode = 0;
		}

		// update config
		_tourTrackConfig.altitudeMode = altitudeMode;

		enableControls();

		// update layer
		Map3Manager.getTourTrackLayer().onModifyConfig();
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

	public void restoreState() {

		_tourTrackConfig = Map3Manager.getTourTrackLayer().getConfig();

	}

	private void updateUI() {

		for (final String value : ALTITUDE_MODE_LABEL) {
			_comboAltitude.add(value);
		}
	}

}
