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
import net.tourbook.map3.Messages;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogTourTrackConfig extends AnimatedToolTipShell {

	private static final int	SHELL_MARGIN		= 5;

	private static final int	MIN_IMAGE_WIDTH		= 10;

	/**
	 * This value is small because a map do not yet load large images !!!
	 */
	private static final int	MAX_IMAGE_WIDTH		= 200;

	private IDialogSettings		_state;

	// initialize with default values which are (should) never be used
	private Rectangle			_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer		_waitTimer			= new WaitTimer();

	private boolean				_canOpenToolTip;
	private boolean				_isWaitTimerStarted;
	private boolean				_isUpdateUI;

	private SelectionAdapter	_selectionListener;

	private TourTrackConfig		_tourTrackConfig;

	/*
	 * UI resources
	 */

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	private Composite			_shellContainer;

	private Button				_chkFollowTerrain;
	private Combo				_comboAltitude;
	private Combo				_comboPathType;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public DialogTourTrackConfig(final Control ownerControl, final ToolBar toolBar, final IDialogSettings state) {

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
		setFadeOutSteps(20);
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
			{
				/*
				 * label: Altitude
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				label.setText(Messages.TourTrac_Properties_Label_Altitude);

				/*
				 * combo: Altitude
				 */
				_comboAltitude = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboAltitude);
				_comboAltitude.setVisibleItemCount(10);
				_comboAltitude.addSelectionListener(_selectionListener);
			}

			{
				/*
				 * checkbox: Follow terrain
				 */
				_chkFollowTerrain = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkFollowTerrain);
				_chkFollowTerrain.setText(Messages.TourTrac_Properties_Checkbox_FollowTerrain);
				_chkFollowTerrain.addSelectionListener(_selectionListener);
			}

			{
				/*
				 * label: Path type
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				label.setText(Messages.TourTrac_Properties_Label_PathType);

				/*
				 * combo: Path type
				 */
				_comboPathType = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboPathType);
				_comboPathType.setVisibleItemCount(10);
				_comboPathType.addSelectionListener(_selectionListener);
			}
		}
	}

	private void enableControls() {

		final boolean isAbsoluteAltitude = _tourTrackConfig.altitudeMode == WorldWind.ABSOLUTE;

		_chkFollowTerrain.setEnabled(isAbsoluteAltitude == false);
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

		int altitudeModeIndex = _comboAltitude.getSelectionIndex();
		if (altitudeModeIndex == -1) {
			altitudeModeIndex = 0;
		}

		// update config
		_tourTrackConfig.altitudeMode = TourTrackConfig.ALTITUDE_MODE_VALUE[altitudeModeIndex];
		_tourTrackConfig.isFollowTerrain = _chkFollowTerrain.getSelection();

		enableControls();

		// update track layer
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

		_comboAltitude.select(_tourTrackConfig.getAltitudeModeIndex());
		_comboPathType.select(_tourTrackConfig.getPathTypeIndex());
		_chkFollowTerrain.setSelection(_tourTrackConfig.isFollowTerrain);

	}

	private void updateUI() {

		// fill altitude mode combo
		for (final String value : TourTrackConfig.ALTITUDE_MODE_LABEL) {
			_comboAltitude.add(value);
		}

		// fill path type combo
		for (final String value : TourTrackConfig.PATH_TYPE_LABEL) {
			_comboPathType.add(value);
		}
	}

}
