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
import net.tourbook.common.UI;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Map3 tour track layer properties dialog.
 */
public class DialogTourTrackConfig extends AnimatedToolTipShell {

	private static final int	SHELL_MARGIN		= 5;

	// initialize with default values which are (should) never be used
	private Rectangle			_toolTipItemBounds	= new Rectangle(0, 0, 50, 50);

	private final WaitTimer		_waitTimer			= new WaitTimer();

	private boolean				_canOpenToolTip;
	private boolean				_isWaitTimerStarted;
	private boolean				_isUpdateUI;

	private SelectionAdapter	_defaultSelectionListener;

	private TourTrackConfig		_trackConfig;

	/*
	 * UI resources
	 */

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	private Composite			_shellContainer;

	private Button				_chkDrawVerticals;
	private Button				_chkExtrudePath;
	private Button				_chkFollowTerrain;
	private Button				_chkTrackPositions;

	private Combo				_comboAltitude;
	private Combo				_comboPathType;

	private Label				_lblAbsoluteOffset;
	private Label				_lblCurtainOpacity;
	private Label				_lblTrackPositionSize;

	private Spinner				_spinnerAltitudeOffset;
	private Spinner				_spinnerInteriorOpacity;
	private Spinner				_spinnerOutlineWidth;
	private Spinner				_spinnerTrackPositionSize;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public DialogTourTrackConfig(final Control ownerControl, final ToolBar toolBar) {

		super(ownerControl);

		addListener(ownerControl, toolBar);

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelection();
			}
		};

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
			createUI_010_UI(_shellContainer);

			// spacer
			final Label label = new Label(_shellContainer, SWT.NONE);
			GridDataFactory.fillDefaults().hint(20, 0).applyTo(label);
		}

		return _shellContainer;
	}

	private void createUI_010_UI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NO_FOCUS);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_100_Altitude(container);

			{
				/*
				 * label: Outline width
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				label.setText(Messages.TourTrack_Properties_Label_OutlineWidth);

				/*
				 * Spinner: Track position size
				 */
				_spinnerOutlineWidth = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerOutlineWidth);
				_spinnerOutlineWidth.setMinimum(0);
				_spinnerOutlineWidth.setMaximum(10);
				_spinnerOutlineWidth.setIncrement(1);
				_spinnerOutlineWidth.setPageIncrement(10);
				_spinnerOutlineWidth.addSelectionListener(_defaultSelectionListener);
				_spinnerOutlineWidth.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onSelection();
					}
				});

			}

			{
				/*
				 * checkbox: Show track positions
				 */
				_chkTrackPositions = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTrackPositions);
				_chkTrackPositions.setText(Messages.TourTrack_Properties_Checkbox_ShowTrackPositions);
				_chkTrackPositions.addSelectionListener(_defaultSelectionListener);

				/*
				 * label: Track position size
				 */
				_lblTrackPositionSize = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblTrackPositionSize);

				_lblTrackPositionSize.setText(Messages.TourTrack_Properties_Label_TrackPositionSize);
				_lblTrackPositionSize.setToolTipText(Messages.TourTrack_Properties_Label_TrackPositionSize_Tooltip);

				/*
				 * Spinner: Track position size
				 */
				_spinnerTrackPositionSize = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerTrackPositionSize);
				_spinnerTrackPositionSize.setMinimum(0);
				_spinnerTrackPositionSize.setMaximum(60);
				_spinnerTrackPositionSize.setIncrement(1);
				_spinnerTrackPositionSize.setPageIncrement(10);
				_spinnerTrackPositionSize.addSelectionListener(_defaultSelectionListener);
				_spinnerTrackPositionSize.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onSelection();
					}
				});
			}

			{
				/*
				 * checkbox: Extrude path
				 */
				_chkExtrudePath = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkExtrudePath);
				_chkExtrudePath.setText(Messages.TourTrack_Properties_Checkbox_ExtrudePath);
				_chkExtrudePath.addSelectionListener(_defaultSelectionListener);

				/*
				 * Label: Curtain opacity
				 */
				_lblCurtainOpacity = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.FILL, SWT.CENTER)
						.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_lblCurtainOpacity);

				_lblCurtainOpacity.setText(Messages.TourTrack_Properties_Label_CurtainOpacity);

				/*
				 * Spinner: Track position size
				 */
				_spinnerInteriorOpacity = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerInteriorOpacity);
				_spinnerInteriorOpacity.setMinimum(0);
				_spinnerInteriorOpacity.setMaximum(100);
				_spinnerInteriorOpacity.setDigits(2);
				_spinnerInteriorOpacity.setIncrement(1);
				_spinnerInteriorOpacity.setPageIncrement(10);
				_spinnerInteriorOpacity.addSelectionListener(_defaultSelectionListener);
				_spinnerInteriorOpacity.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onSelection();
					}
				});

				/*
				 * Checkbox: Draw verticals for the extruded path
				 */
				_chkDrawVerticals = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
						.applyTo(_chkDrawVerticals);
				_chkDrawVerticals.setText(Messages.TourTrack_Properties_Checkbox_DrawVerticals);
				_chkDrawVerticals.addSelectionListener(_defaultSelectionListener);
			}

			{
				/*
				 * label: Path type
				 */
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

				label.setText(Messages.TourTrack_Properties_Label_PathType);

				/*
				 * combo: Path type
				 */
				_comboPathType = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboPathType);
				_comboPathType.setVisibleItemCount(10);
				_comboPathType.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void createUI_100_Altitude(final Composite container) {

		{
			/*
			 * label: Altitude
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_Altitude);

			/*
			 * combo: Altitude
			 */
			_comboAltitude = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboAltitude);
			_comboAltitude.setVisibleItemCount(10);
			_comboAltitude.addSelectionListener(_defaultSelectionListener);
		}

		{
			/*
			 * label: Absolute offset
			 */
			_lblAbsoluteOffset = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblAbsoluteOffset);

			_lblAbsoluteOffset.setText(Messages.TourTrack_Properties_Label_AltitudeOffset);

			/*
			 * Spinner: Altitude offset
			 */
			_spinnerAltitudeOffset = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerAltitudeOffset);
			_spinnerAltitudeOffset.setMinimum(-10000);
			_spinnerAltitudeOffset.setMaximum(100000);
			_spinnerAltitudeOffset.setIncrement(1);
			_spinnerAltitudeOffset.setPageIncrement(10);
			_spinnerAltitudeOffset.addSelectionListener(_defaultSelectionListener);
			_spinnerAltitudeOffset.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onSelection();
				}
			});
		}

		{
			/*
			 * checkbox: Follow terrain
			 */
			_chkFollowTerrain = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_chkFollowTerrain);
			_chkFollowTerrain.setText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain);
			_chkFollowTerrain.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void enableControls() {

		final boolean isAbsoluteAltitude = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isShowCurtain = _trackConfig.isExtrudePath;
		final boolean isShowTrackPosition = _trackConfig.isShowTrackPosition;

		_lblAbsoluteOffset.setEnabled(isAbsoluteAltitude);
		_spinnerAltitudeOffset.setEnabled(isAbsoluteAltitude);
		_chkFollowTerrain.setEnabled(isAbsoluteAltitude == false);

		_lblTrackPositionSize.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionSize.setEnabled(isShowTrackPosition);

		_lblCurtainOpacity.setEnabled(isShowCurtain);
		_spinnerInteriorOpacity.setEnabled(isShowCurtain);
		_chkDrawVerticals.setEnabled(isShowCurtain);
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

		saveState();

		enableControls();

		// update track layer
		Map3Manager.getTourTrackLayer().onModifyConfig();
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

	}

	private void onSelection() {

		if (_isUpdateUI) {
			return;
		}

		onModify();
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

		_trackConfig = Map3Manager.getTourTrackLayer().getConfig();

		_chkDrawVerticals.setSelection(_trackConfig.isDrawVerticals);
		_chkExtrudePath.setSelection(_trackConfig.isExtrudePath);
		_chkFollowTerrain.setSelection(_trackConfig.isFollowTerrain);
		_chkTrackPositions.setSelection(_trackConfig.isShowTrackPosition);

		_comboAltitude.select(_trackConfig.getAltitudeModeIndex());
		_comboPathType.select(_trackConfig.getPathTypeIndex());

		_spinnerAltitudeOffset.setSelection(_trackConfig.altitudeOffset);
		_spinnerOutlineWidth.setSelection((int) (_trackConfig.outlineWidth));
		_spinnerInteriorOpacity.setSelection((int) (_trackConfig.interiorOpacity * 100));
		_spinnerTrackPositionSize.setSelection((int) (_trackConfig.trackPositionSize));

	}

	private void saveState() {

		final int backupAltitudeOffset = _trackConfig.altitudeOffset;
		final boolean backupIsAbsoluteAltitude = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;

		int altitudeModeIndex = _comboAltitude.getSelectionIndex();
		if (altitudeModeIndex == -1) {
			altitudeModeIndex = 0;
		}
		int pathTypeIndex = _comboPathType.getSelectionIndex();
		if (pathTypeIndex == -1) {
			pathTypeIndex = 0;
		}

		// update config

		_trackConfig.altitudeMode = TourTrackConfig.ALTITUDE_MODE_VALUE[altitudeModeIndex];
		_trackConfig.altitudeOffset = _spinnerAltitudeOffset.getSelection();

		_trackConfig.isDrawVerticals = _chkDrawVerticals.getSelection();
		_trackConfig.isExtrudePath = _chkExtrudePath.getSelection();
		_trackConfig.isFollowTerrain = _chkFollowTerrain.getSelection();
		_trackConfig.isShowTrackPosition = _chkTrackPositions.getSelection();

		_trackConfig.interiorOpacity = _spinnerInteriorOpacity.getSelection() / 100.0;
		_trackConfig.outlineWidth = _spinnerOutlineWidth.getSelection();
		_trackConfig.pathType = TourTrackConfig.PATH_TYPE_VALUE[pathTypeIndex];
		_trackConfig.trackPositionSize = _spinnerTrackPositionSize.getSelection();

		final boolean isAbsoluteAltitude = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;

		if (backupAltitudeOffset == _trackConfig.altitudeOffset && backupIsAbsoluteAltitude == isAbsoluteAltitude) {

			_trackConfig.isRecreateTracks = false;

		} else {

			// altitude offset or altitude mode has changed

			_trackConfig.isRecreateTracks = true;
		}
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
