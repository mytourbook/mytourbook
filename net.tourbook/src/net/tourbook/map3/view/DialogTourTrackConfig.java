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
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.color.MapColorId;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.tourtrack.AltitudeMode;
import net.tourbook.map3.layer.tourtrack.PathResolution;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
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
public class DialogTourTrackConfig extends AnimatedToolTipShell implements IColorSelectorListener {

	private static final String		FORMAT_POSITION_THRESHOLD	= "%,.0f %s";					//$NON-NLS-1$

	private static final int		SHELL_MARGIN				= 5;

	private Map3View				_map3View;

	// initialize with default values which are (should) never be used
	private Rectangle				_toolTipItemBounds			= new Rectangle(0, 0, 50, 50);

	private final WaitTimer			_waitTimer					= new WaitTimer();

	private boolean					_canOpenToolTip;
	private boolean					_isWaitTimerStarted;
	private boolean					_isUpdateUI;

	private SelectionAdapter		_defaultSelectionListener;

	private TourTrackConfig			_trackConfig;
	private boolean					_isAnotherDialogOpened;

	private MapColorId				_trackColorId;

	/*
	 * UI resources
	 */

	/*
	 * UI controls
	 */
	private Composite				_shellContainer;

	private Button					_btnDefault;
	private Button					_btnTrackColor;

	private Button					_chkAltitudeOffset;
	private Button					_chkDrawVerticals;
	private Button					_chkExtrudePath;
	private Button					_chkFollowTerrain;
	private Button					_chkTrackPositions;

	private ColorSelectorExtended	_colorInteriorColor;
	private ColorSelectorExtended	_colorInteriorColorHovered;
	private ColorSelectorExtended	_colorInteriorColorHovSel;
	private ColorSelectorExtended	_colorInteriorColorSelected;
	private ColorSelectorExtended	_colorOutlineColorHovered;
	private ColorSelectorExtended	_colorOutlineColorHovSel;
	private ColorSelectorExtended	_colorOutlineColorSelected;

	private Combo					_comboAltitude;
	private Combo					_comboPathResolution;

	private Label					_lblAltitudeOffsetDistanceUnit;
	private Label					_lblCurtainColor;
	private Label					_lblCurtainColorHovered;
	private Label					_lblCurtainColorHovSel;
	private Label					_lblCurtainColorSelected;
	private Label					_lblOutlineColorHovSel;
	private Label					_lblOutlineColorSelected;
	private Label					_lblTrackColor;
	private Label					_lblTrackPositionSize;
	private Label					_lblTrackPositionThreshold;
	private Label					_lblTrackPositionThresholdAbsolute;

	private Spinner					_spinnerAltitudeOffsetDistance;
	private Spinner					_spinnerInteriorOpacity;
	private Spinner					_spinnerInteriorOpacityHovered;
	private Spinner					_spinnerInteriorOpacityHovSel;
	private Spinner					_spinnerInteriorOpacitySelected;
	private Spinner					_spinnerOutlineOpacityHovered;
	private Spinner					_spinnerOutlineOpacityHovSel;
	private Spinner					_spinnerOutlineOpacitySelected;
	private Spinner					_spinnerOutlineWidth;
	private Spinner					_spinnerTrackPositionSize;
	private Spinner					_spinnerTrackPositionSizeHovered;
	private Spinner					_spinnerTrackPositionSizeSelected;
	private Spinner					_spinnerTrackPositionThreshold;

	private final class WaitTimer implements Runnable {
		@Override
		public void run() {
			open_Runnable();
		}
	}

	public DialogTourTrackConfig(final Control ownerControl, final ToolBar toolBar, final Map3View map3View) {

		super(ownerControl);

		_map3View = map3View;

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

		initBeforeOpened();

		return true;
	}

	@Override
	public void colorDialogOpened(final boolean isDialogOpened) {

		_isAnotherDialogOpened = isDialogOpened;
	}

	@Override
	protected Composite createToolTipContentArea(final Composite parent) {

		final Composite container = createUI(parent);

		updateUI_initial();

		restoreState();

		enableControls();

		return container;
	}

	private Composite createUI(final Composite parent) {

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_shellContainer);
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			final Composite container = new Composite(_shellContainer, SWT.NO_FOCUS);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			{
				createUI_000_Title(container);
				createUI_100_Line(container);
				createUI_200_TrackPosition(container);
				createUI_300_OutlineColor(container);
				createUI_400_ExtrudePath(container);
				createUI_500_Altitude(container);
				createUI_600_PathResolution(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_000_Title(final Composite parent) {

		/*
		 * Label: Title
		 */
		final Label title = new Label(parent, SWT.LEAD);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(title);
		title.setFont(JFaceResources.getBannerFont());
		title.setText(Messages.TourTrack_Properties_Label_DialogTitle);

		/*
		 * Button: Default
		 */
		_btnDefault = new Button(parent, SWT.PUSH);
		GridDataFactory.fillDefaults()//
				.align(SWT.TRAIL, SWT.FILL)
				.grab(false, false)
				.applyTo(_btnDefault);
		_btnDefault.setText(Messages.TourTrack_Properties_Button_Default);
		_btnDefault.setToolTipText(Messages.TourTrack_Properties_Button_Default_Tooltip);
		_btnDefault.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onActionDefault();
			}
		});
	}

	private void createUI_100_Line(final Composite parent) {

		{
			/*
			 * label: Line width
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_OutlineWidth);
			label.setToolTipText(Messages.TourTrack_Properties_Label_OutlineWidth_Tooltip);

			/*
			 * Spinner: Line width
			 */
			_spinnerOutlineWidth = new Spinner(parent, SWT.BORDER);
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
	}

	private void createUI_200_TrackPosition(final Composite parent) {

		/*
		 * checkbox: Show track positions
		 */
		_chkTrackPositions = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkTrackPositions);
		_chkTrackPositions.setText(Messages.TourTrack_Properties_Checkbox_ShowTrackPositions);
		_chkTrackPositions.setToolTipText(Messages.TourTrack_Properties_Checkbox_ShowTrackPositions_Tooltip);
		_chkTrackPositions.addSelectionListener(_defaultSelectionListener);

		{
			/*
			 * label: Track position size
			 */
			_lblTrackPositionSize = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblTrackPositionSize);

			_lblTrackPositionSize.setText(Messages.TourTrack_Properties_Label_TrackPositionSize);
			_lblTrackPositionSize.setToolTipText(Messages.TourTrack_Properties_Label_TrackPositionSize_Tooltip);

			/*
			 * Spinner: Track position size
			 */
			_spinnerTrackPositionSize = createUI_PositionSize(parent);
		}
		{
			/*
			 * label: Track position threshold
			 */
			_lblTrackPositionThreshold = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblTrackPositionThreshold);

			_lblTrackPositionThreshold.setText(//
					Messages.TourTrack_Properties_Label_TrackPositionThreshold);
			_lblTrackPositionThreshold.setToolTipText(//
					Messages.TourTrack_Properties_Label_TrackPositionThreshold_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				/*
				 * Spinner: Track position threshold
				 */
				_spinnerTrackPositionThreshold = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerTrackPositionThreshold);
				_spinnerTrackPositionThreshold.setMinimum(3);
				_spinnerTrackPositionThreshold.setMaximum(10);
				_spinnerTrackPositionThreshold.setIncrement(1);
				_spinnerTrackPositionThreshold.setPageIncrement(10);
				_spinnerTrackPositionThreshold.addSelectionListener(_defaultSelectionListener);
				_spinnerTrackPositionThreshold.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onSelection();
					}
				});

				/*
				 * Label: eye distance
				 */
				_lblTrackPositionThresholdAbsolute = new Label(container, SWT.NONE);
				_lblTrackPositionThresholdAbsolute.setText(UI.EMPTY_STRING);
			}
		}
	}

	private void createUI_300_OutlineColor(final Composite parent) {

		/*
		 * Track color
		 */
		{
			/*
			 * Label: Track color
			 */
			_lblTrackColor = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblTrackColor);

			_lblTrackColor.setText(Messages.TourTrack_Properties_Label_TrackColor);
			_lblTrackColor.setToolTipText(Messages.TourTrack_Properties_Label_TrackColor_Tooltip);

			/*
			 * Button: Track color
			 */
			_btnTrackColor = new Button(parent, SWT.PUSH);
			_btnTrackColor.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(net.tourbook.ui.UI.GRAPH_ALTITUDE));
			_btnTrackColor.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectTrackColor();
				}
			});
		}

		/*
		 * Selected color
		 */
		{
			_lblOutlineColorSelected = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblOutlineColorSelected);

			_lblOutlineColorSelected.setText(Messages.TourTrack_Properties_Label_OutlineColorSelected);
			_lblOutlineColorSelected.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColorSelected_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_colorOutlineColorSelected = createUI_ColorSelector(container);
				_spinnerOutlineOpacitySelected = createUI_ColorOpacity(container);
				_spinnerTrackPositionSizeSelected = createUI_PositionSize(container);
			}
		}

		/*
		 * Hovered color
		 */
		{
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_OutlineColorHovered);
			label.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColorHovered_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_colorOutlineColorHovered = createUI_ColorSelector(container);
				_spinnerOutlineOpacityHovered = createUI_ColorOpacity(container);
				_spinnerTrackPositionSizeHovered = createUI_PositionSize(container);
			}
		}

		/*
		 * Hovered + Selected color
		 */
		{
			_lblOutlineColorHovSel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblOutlineColorHovSel);

			_lblOutlineColorHovSel.setText(Messages.TourTrack_Properties_Label_OutlineColorHovSel);
			_lblOutlineColorHovSel.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColorHovSel_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_colorOutlineColorHovSel = createUI_ColorSelector(container);
				_spinnerOutlineOpacityHovSel = createUI_ColorOpacity(container);
			}
		}
	}

	private void createUI_400_ExtrudePath(final Composite container) {
		{
			/*
			 * checkbox: Extrude path
			 */
			_chkExtrudePath = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkExtrudePath);
			_chkExtrudePath.setText(Messages.TourTrack_Properties_Checkbox_ExtrudePath);
			_chkExtrudePath.setToolTipText(Messages.TourTrack_Properties_Checkbox_ExtrudePath_Tooltip);
			_chkExtrudePath.addSelectionListener(_defaultSelectionListener);

			createUI_410_Curtain(container);
			createUI_420_Verticals(container);
		}
	}

	private void createUI_410_Curtain(final Composite parent) {

		/*
		 * Curtain color
		 */
		{
			_lblCurtainColor = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColor);

			_lblCurtainColor.setText(Messages.TourTrack_Properties_Label_CurtainColor);
			_lblCurtainColor.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColor_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_colorInteriorColor = createUI_ColorSelector(container);
				_spinnerInteriorOpacity = createUI_ColorOpacity(container);
			}
		}

		/*
		 * Curtain selected color
		 */
		{
			_lblCurtainColorSelected = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColorSelected);

			_lblCurtainColorSelected.setText(Messages.TourTrack_Properties_Label_CurtainColorSelected);
			_lblCurtainColorSelected.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorSelected_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_colorInteriorColorSelected = createUI_ColorSelector(container);
				_spinnerInteriorOpacitySelected = createUI_ColorOpacity(container);
			}
		}

		/*
		 * Curtain hovered color
		 */
		{
			_lblCurtainColorHovered = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColorHovered);

			_lblCurtainColorHovered.setText(Messages.TourTrack_Properties_Label_CurtainColorHovered);
			_lblCurtainColorHovered.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovered_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_colorInteriorColorHovered = createUI_ColorSelector(container);
				_spinnerInteriorOpacityHovered = createUI_ColorOpacity(container);
			}
		}

		/*
		 * Curtain hovered + selected color
		 */
		{
			_lblCurtainColorHovSel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColorHovSel);

			_lblCurtainColorHovSel.setText(Messages.TourTrack_Properties_Label_CurtainColorHovSel);
			_lblCurtainColorHovSel.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovSel_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_colorInteriorColorHovSel = createUI_ColorSelector(container);
				_spinnerInteriorOpacityHovSel = createUI_ColorOpacity(container);
			}
		}
	}

	private void createUI_420_Verticals(final Composite parent) {

		/*
		 * Checkbox: Draw verticals for the extruded path
		 */
		_chkDrawVerticals = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults()//
				.span(2, 1)
				.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
				.applyTo(_chkDrawVerticals);
		_chkDrawVerticals.setText(Messages.TourTrack_Properties_Checkbox_DrawVerticals);
		_chkDrawVerticals.setToolTipText(Messages.TourTrack_Properties_Checkbox_DrawVerticals_Tooltip);
		_chkDrawVerticals.addSelectionListener(_defaultSelectionListener);
	}

	private void createUI_500_Altitude(final Composite parent) {

		{
			/*
			 * label: Altitude mode
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_Altitude);
			label.setToolTipText(Messages.TourTrack_Properties_Label_Altitude_Tooltip);

			/*
			 * combo: Altitude
			 */
			_comboAltitude = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboAltitude);
			_comboAltitude.setVisibleItemCount(10);
			_comboAltitude.addSelectionListener(_defaultSelectionListener);
		}

		{
			/*
			 * checkbox: Altitude offset
			 */
			_chkAltitudeOffset = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_chkAltitudeOffset);
			_chkAltitudeOffset.setText(Messages.TourTrack_Properties_Checkbox_AltitudeOffset);
			_chkAltitudeOffset.addSelectionListener(_defaultSelectionListener);

			final Composite containerAltitude = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerAltitude);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerAltitude);
			{
				/*
				 * Spinner: Altitude offset
				 */
				_spinnerAltitudeOffsetDistance = new Spinner(containerAltitude, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerAltitudeOffsetDistance);
				_spinnerAltitudeOffsetDistance.setMinimum(-1000000);
				_spinnerAltitudeOffsetDistance.setMaximum(1000000);
				_spinnerAltitudeOffsetDistance.setIncrement(1);
				_spinnerAltitudeOffsetDistance.setPageIncrement(10);
				_spinnerAltitudeOffsetDistance.addSelectionListener(_defaultSelectionListener);
				_spinnerAltitudeOffsetDistance.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						onSelection();
					}
				});

				/*
				 * Label: m (meter/feet)
				 */
				_lblAltitudeOffsetDistanceUnit = new Label(containerAltitude, SWT.NONE);
				_lblAltitudeOffsetDistanceUnit.setText(UI.UNIT_LABEL_ALTITUDE);
			}
		}

		{
			/*
			 * checkbox: Follow terrain
			 */
			_chkFollowTerrain = new Button(parent, SWT.CHECK);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_chkFollowTerrain);
			_chkFollowTerrain.setText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain);
			_chkFollowTerrain.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void createUI_600_PathResolution(final Composite parent) {

		/*
		 * label: Path resolution
		 */
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

		label.setText(Messages.TourTrack_Properties_Label_PathResolution);
		label.setToolTipText(Messages.TourTrack_Properties_Label_PathResolution_Tooltip);

		/*
		 * combo: Path resolution
		 */
		_comboPathResolution = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(_comboPathResolution);
		_comboPathResolution.setVisibleItemCount(10);
		_comboPathResolution.addSelectionListener(_defaultSelectionListener);
	}

	private Spinner createUI_ColorOpacity(final Composite parent) {

		final Spinner spinnerOutlineOpacity = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(spinnerOutlineOpacity);

		spinnerOutlineOpacity.setMinimum(0);
		spinnerOutlineOpacity.setMaximum(100);
		spinnerOutlineOpacity.setDigits(2);
		spinnerOutlineOpacity.setIncrement(1);
		spinnerOutlineOpacity.setPageIncrement(10);
		spinnerOutlineOpacity.addSelectionListener(_defaultSelectionListener);

		spinnerOutlineOpacity.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				onSelection();
			}
		});

		return spinnerOutlineOpacity;
	}

	private ColorSelectorExtended createUI_ColorSelector(final Composite parent) {

		final ColorSelectorExtended colorSelector = new ColorSelectorExtended(parent);
		GridDataFactory.swtDefaults()//
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(colorSelector.getButton());

		colorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onSelection();
			}
		});
		colorSelector.addOpenListener(this);

		return colorSelector;
	}

	private Spinner createUI_PositionSize(final Composite container) {

		final Spinner spinnerTrackPositionSize = new Spinner(container, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(spinnerTrackPositionSize);
		spinnerTrackPositionSize.setMinimum(0);
		spinnerTrackPositionSize.setMaximum(60);
//		spinnerTrackPositionSize.setDigits(1);
		spinnerTrackPositionSize.setIncrement(1);
		spinnerTrackPositionSize.setPageIncrement(50);
		spinnerTrackPositionSize.addSelectionListener(_defaultSelectionListener);
		spinnerTrackPositionSize.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				onSelection();
			}
		});
		return spinnerTrackPositionSize;
	}

	private void enableControls() {

		final boolean isAbsoluteAltitude = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isClampToGround = _trackConfig.altitudeMode == WorldWind.CLAMP_TO_GROUND;
		final boolean isAbsoluteAltitudeEnabled = _chkAltitudeOffset.getSelection() && isAbsoluteAltitude;
		final boolean isShowCurtain = isClampToGround == false && _trackConfig.isExtrudePath;
		final boolean isTrackPositionVisible = _trackConfig.outlineWidth > 0.0;
		final boolean isShowTrackPosition = _trackConfig.isShowTrackPosition & isTrackPositionVisible;

		// Hr zones are not yet supported
		final boolean isGradientColor = _trackColorId != MapColorId.HrZone;

		final boolean canFollowTerrain = isAbsoluteAltitude == false;

		// altitude
		_chkAltitudeOffset.setEnabled(isAbsoluteAltitude);
		_spinnerAltitudeOffsetDistance.setEnabled(isAbsoluteAltitudeEnabled);
		_chkFollowTerrain.setEnabled(canFollowTerrain);

		// track position
		_chkTrackPositions.setEnabled(isTrackPositionVisible);

		_lblTrackPositionSize.setEnabled(isShowTrackPosition);
		_lblTrackPositionThreshold.setEnabled(isShowTrackPosition);

		_spinnerTrackPositionSize.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionSizeHovered.setEnabled(isTrackPositionVisible);
		_spinnerTrackPositionSizeSelected.setEnabled(isTrackPositionVisible);
		_spinnerTrackPositionThreshold.setEnabled(isShowTrackPosition);

		// extrude track
		_chkExtrudePath.setEnabled(isClampToGround == false);
		_lblCurtainColor.setEnabled(isShowCurtain);
		_lblCurtainColorHovered.setEnabled(isShowCurtain);
		_lblCurtainColorHovSel.setEnabled(isShowCurtain);
		_lblCurtainColorSelected.setEnabled(isShowCurtain);
		_colorInteriorColor.setEnabled(isShowCurtain);
		_colorInteriorColorHovered.setEnabled(isShowCurtain);
		_colorInteriorColorHovSel.setEnabled(isShowCurtain);
		_colorInteriorColorSelected.setEnabled(isShowCurtain);
		_spinnerInteriorOpacity.setEnabled(isShowCurtain);
		_spinnerInteriorOpacityHovered.setEnabled(isShowCurtain);
		_spinnerInteriorOpacityHovSel.setEnabled(isShowCurtain);
		_spinnerInteriorOpacitySelected.setEnabled(isShowCurtain);
		_chkDrawVerticals.setEnabled(isShowCurtain);

		// track color
		_btnTrackColor.setEnabled(isGradientColor);
	}

	@Override
	public Point getToolTipLocation(final Point tipSize) {

		final int itemHeight = _toolTipItemBounds.height;

		final int devX = _toolTipItemBounds.x;
		final int devY = _toolTipItemBounds.y + itemHeight + 0;

		return new Point(devX, devY);
	}

	private void initBeforeOpened() {

		_trackColorId = _map3View.getTrackColorId();

		_btnTrackColor.setImage(net.tourbook.ui.UI.getGraphImage(_trackColorId));

		enableControls();
	}

	@Override
	protected Rectangle noHideOnMouseMove() {

		return _toolTipItemBounds;
	}

	private void onActionDefault() {

		_trackConfig.reset();

		restoreState();

		enableControls();

		// update track layer
		Map3Manager.getTourTrackLayer().onModifyConfig();

		// update map
		Map3Manager.redraw();
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

	private void onSelectTrackColor() {

		_isAnotherDialogOpened = true;

		_map3View.actionOpenTrackColorDialog();

		_isAnotherDialogOpened = false;
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

	/**
	 * Restores state values from the tour track configuration and update the UI.
	 */
	public void restoreState() {

		_trackConfig = Map3Manager.getTourTrackLayer().getConfig();

		// line color
		_spinnerOutlineWidth.setSelection((int) (_trackConfig.outlineWidth));
		_colorOutlineColorHovSel.setColorValue(_trackConfig.outlineColorHovSel);
		_colorOutlineColorSelected.setColorValue(_trackConfig.outlineColorSelected);
		_colorOutlineColorHovered.setColorValue(_trackConfig.outlineColorHovered);
		_spinnerOutlineOpacityHovSel.setSelection((int) (_trackConfig.outlineOpacityHovSel * 100));
		_spinnerOutlineOpacitySelected.setSelection((int) (_trackConfig.outlineOpacitySelected * 100));
		_spinnerOutlineOpacityHovered.setSelection((int) (_trackConfig.outlineOpacityHovered * 100));

		// altitude
		_comboAltitude.select(_trackConfig.getAltitudeModeIndex());
		_chkAltitudeOffset.setSelection(_trackConfig.isAbsoluteOffset);
		_spinnerAltitudeOffsetDistance.setSelection(//
				(int) (_trackConfig.altitudeOffsetDistance / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE));
		_chkFollowTerrain.setSelection(_trackConfig.isFollowTerrain);

		// curtain color
		_chkExtrudePath.setSelection(_trackConfig.isExtrudePath);
		_colorInteriorColor.setColorValue(_trackConfig.interiorColor);
		_colorInteriorColorHovered.setColorValue(_trackConfig.interiorColorHovered);
		_colorInteriorColorHovSel.setColorValue(_trackConfig.interiorColorHovSel);
		_colorInteriorColorSelected.setColorValue(_trackConfig.interiorColorSelected);
		_spinnerInteriorOpacity.setSelection((int) (_trackConfig.interiorOpacity * 100));
		_spinnerInteriorOpacityHovered.setSelection((int) (_trackConfig.interiorOpacityHovered * 100));
		_spinnerInteriorOpacityHovSel.setSelection((int) (_trackConfig.interiorOpacityHovSel * 100));
		_spinnerInteriorOpacitySelected.setSelection((int) (_trackConfig.interiorOpacitySelected * 100));

		// verticals
		_chkDrawVerticals.setSelection(_trackConfig.isDrawVerticals);

		// track position
		_chkTrackPositions.setSelection(_trackConfig.isShowTrackPosition);
		_spinnerTrackPositionSize.setSelection((int) (_trackConfig.trackPositionSize * 1));
		_spinnerTrackPositionSizeHovered.setSelection((int) (_trackConfig.trackPositionSizeHovered * 1));
		_spinnerTrackPositionSizeSelected.setSelection((int) (_trackConfig.trackPositionSizeSelected * 1));
		_spinnerTrackPositionThreshold.setSelection((int) _trackConfig.trackPositionThreshold);

		// path
		_comboPathResolution.select(_trackConfig.getPathResolutionIndex());

		updateUI();
	}

	/**
	 * Saves state values from the UI in the tour track configuration.
	 */
	private void saveState() {

		final boolean backupIsAltitudeOffset = _trackConfig.isAbsoluteOffset;
		final int backupAltitudeOffsetDistance = _trackConfig.altitudeOffsetDistance;
		final boolean backupIsAbsoluteAltitudeMode = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final int backupPathResolution = _trackConfig.pathResolution;

		int altitudeModeIndex = _comboAltitude.getSelectionIndex();
		if (altitudeModeIndex == -1) {
			altitudeModeIndex = 0;
		}

		int pathResolutionIndex = _comboPathResolution.getSelectionIndex();
		if (pathResolutionIndex == -1) {
			pathResolutionIndex = 0;
		}

		final int altitudeOffsetMetric = (int) (_spinnerAltitudeOffsetDistance.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);

		// update config

		// line
		_trackConfig.outlineWidth = _spinnerOutlineWidth.getSelection();
		_trackConfig.outlineColorHovSel = _colorOutlineColorHovSel.getColorValue();
		_trackConfig.outlineColorSelected = _colorOutlineColorSelected.getColorValue();
		_trackConfig.outlineColorHovered = _colorOutlineColorHovered.getColorValue();
		_trackConfig.outlineOpacityHovSel = _spinnerOutlineOpacityHovSel.getSelection() / 100.0;
		_trackConfig.outlineOpacitySelected = _spinnerOutlineOpacitySelected.getSelection() / 100.0;
		_trackConfig.outlineOpacityHovered = _spinnerOutlineOpacityHovered.getSelection() / 100.0;

		// altitude
		_trackConfig.isAbsoluteOffset = _chkAltitudeOffset.getSelection();
		_trackConfig.altitudeMode = TourTrackConfig.ALTITUDE_MODE[altitudeModeIndex].value;
		_trackConfig.altitudeOffsetDistance = altitudeOffsetMetric;
		_trackConfig.isFollowTerrain = _chkFollowTerrain.getSelection();

		// curtain
		_trackConfig.isExtrudePath = _chkExtrudePath.getSelection();
		_trackConfig.interiorColor = _colorInteriorColor.getColorValue();
		_trackConfig.interiorColorHovered = _colorInteriorColorHovered.getColorValue();
		_trackConfig.interiorColorHovSel = _colorInteriorColorHovSel.getColorValue();
		_trackConfig.interiorColorSelected = _colorInteriorColorSelected.getColorValue();
		_trackConfig.interiorOpacity = _spinnerInteriorOpacity.getSelection() / 100.0;
		_trackConfig.interiorOpacityHovered = _spinnerInteriorOpacityHovered.getSelection() / 100.0;
		_trackConfig.interiorOpacityHovSel = _spinnerInteriorOpacityHovSel.getSelection() / 100.0;
		_trackConfig.interiorOpacitySelected = _spinnerInteriorOpacitySelected.getSelection() / 100.0;

		// verticals
		_trackConfig.isDrawVerticals = _chkDrawVerticals.getSelection();

		// track position
		_trackConfig.isShowTrackPosition = _chkTrackPositions.getSelection();
		_trackConfig.trackPositionSize = _spinnerTrackPositionSize.getSelection() / 1.0;
		_trackConfig.trackPositionSizeHovered = _spinnerTrackPositionSizeHovered.getSelection() / 1.0;
		_trackConfig.trackPositionSizeSelected = _spinnerTrackPositionSizeSelected.getSelection() / 1.0;
		_trackConfig.trackPositionThreshold = _spinnerTrackPositionThreshold.getSelection();

		// path
		_trackConfig.pathResolution = TourTrackConfig.PATH_RESOLUTION[pathResolutionIndex].value;

		final boolean isAbsoluteAltitudeMode = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isAbsoluteAltitudeNotModified = //
		backupIsAltitudeOffset == _trackConfig.isAbsoluteOffset
				&& backupIsAbsoluteAltitudeMode == isAbsoluteAltitudeMode;

		/*
		 * check if tracks must be recreated
		 */
		if (//
			// altitude offset (vertical distance) is not modified
		backupAltitudeOffsetDistance == _trackConfig.altitudeOffsetDistance //

				// path resolution is not modified
				&& backupPathResolution == _trackConfig.pathResolution //

				&& isAbsoluteAltitudeNotModified
		//
		) {

			_trackConfig.isRecreateTracks = false;

		} else {

			// altitude offset or altitude mode has changed

			_trackConfig.isRecreateTracks = true;
		}

		updateUI();
	}

	public void updateMeasurementSystem() {

		_lblAltitudeOffsetDistanceUnit.setText(UI.UNIT_LABEL_ALTITUDE);
		_lblAltitudeOffsetDistanceUnit.getParent().layout();

		restoreState();
	}

	private void updateUI() {

		// position threshold
		final double positionThreshold = Math.pow(10, _trackConfig.trackPositionThreshold) / 1000;

		_lblTrackPositionThresholdAbsolute.setText(String.format(
				FORMAT_POSITION_THRESHOLD,
				positionThreshold,
				UI.UNIT_LABEL_DISTANCE));

		_lblTrackPositionThresholdAbsolute.getParent().layout();
	}

	private void updateUI_initial() {

		// fill altitude mode combo
		for (final AltitudeMode altiMode : TourTrackConfig.ALTITUDE_MODE) {
			_comboAltitude.add(altiMode.label);
		}

		// fill path resolution combo
		for (final PathResolution pathResolution : TourTrackConfig.PATH_RESOLUTION) {
			_comboPathResolution.add(pathResolution.label);
		}
	}

}
