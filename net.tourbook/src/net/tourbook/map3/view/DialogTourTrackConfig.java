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
import net.tourbook.map3.layer.tourtrack.ComboEntry;
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
	private ColorSelectorExtended	_colorInteriorColor_Hovered;
	private ColorSelectorExtended	_colorInteriorColor_HovSel;
	private ColorSelectorExtended	_colorInteriorColor_Selected;
	private ColorSelectorExtended	_colorOutlineColor;
	private ColorSelectorExtended	_colorOutlineColor_Hovered;
	private ColorSelectorExtended	_colorOutlineColor_HovSel;
	private ColorSelectorExtended	_colorOutlineColor_Selected;

	private Combo					_comboAltitude;
	private Combo					_comboPathResolution;
	private Combo					_comboInteriorColorMode;
	private Combo					_comboInteriorColorMode_Selected;
	private Combo					_comboInteriorColorMode_Hovered;
	private Combo					_comboInteriorColorMode_HovSel;
	private Combo					_comboOutlineColorMode;
	private Combo					_comboOutlineColorMode_Hovered;
	private Combo					_comboOutlineColorMode_HovSel;
	private Combo					_comboOutlineColorMode_Selected;

	private Label					_lblAltitudeOffsetDistanceUnit;
	private Label					_lblCurtainColor;
	private Label					_lblCurtainColor_Hovered;
	private Label					_lblCurtainColor_HovSel;
	private Label					_lblCurtainColor_Selected;
	private Label					_lblOutlineColor;
	private Label					_lblOutlineColor_HovSel;
	private Label					_lblOutlineColor_Selected;
	private Label					_lblTrackColor;
	private Label					_lblTrackPositionThreshold;
	private Label					_lblTrackPositionThresholdAbsolute;

	private Spinner					_spinnerAltitudeOffsetDistance;
	private Spinner					_spinnerDirectionArrowSize;
	private Spinner					_spinnerInteriorOpacity;
	private Spinner					_spinnerInteriorOpacity_Hovered;
	private Spinner					_spinnerInteriorOpacity_HovSel;
	private Spinner					_spinnerInteriorOpacity_Selected;
	private Spinner					_spinnerOutlineOpacity_Hovered;
	private Spinner					_spinnerOutlineOpacity_HovSel;
	private Spinner					_spinnerOutlineOpacity_Selected;
	private Spinner					_spinnerOutlineWidth;
	private Spinner					_spinnerOutlineOpacity;
	private Spinner					_spinnerTrackPositionSize;
	private Spinner					_spinnerTrackPositionSize_Hovered;
	private Spinner					_spinnerTrackPositionSize_Selected;
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
				onModifyConfig();
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
				createUI_000_DialogTitle(container);
				createUI_100_Line(container);
				createUI_110_DirectionArrow(container);
				createUI_200_TrackPosition(container);
				createUI_300_TrackColor(container);
				createUI_400_ExtrudePath(container);
				createUI_500_Altitude(container);
				createUI_600_PathResolution(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_000_DialogTitle(final Composite parent) {

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
					onModifyConfig();
				}
			});
		}
	}

	private void createUI_110_DirectionArrow(final Composite parent) {

		{
			/*
			 * label: Direction arrow
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_DirectionArrowSize);
			label.setToolTipText(Messages.TourTrack_Properties_Label_DirectionArrowSize_Tooltip);

			/*
			 * Spinner: Direction Arrow
			 */
			_spinnerDirectionArrowSize = new Spinner(parent, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerDirectionArrowSize);
			_spinnerDirectionArrowSize.setMinimum(10);
			_spinnerDirectionArrowSize.setMaximum(100);
			_spinnerDirectionArrowSize.setIncrement(10);
			_spinnerDirectionArrowSize.setPageIncrement(50);
			_spinnerDirectionArrowSize.addSelectionListener(_defaultSelectionListener);
			_spinnerDirectionArrowSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					Util.adjustSpinnerValueOnMouseScroll(event);
					onModifyConfig();
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
						onModifyConfig();
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

	private void createUI_300_TrackColor(final Composite parent) {

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

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				/*
				 * Button: Track color
				 */
				_btnTrackColor = new Button(container, SWT.PUSH);
				_btnTrackColor.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(net.tourbook.ui.UI.GRAPH_ALTITUDE));
				_btnTrackColor.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectTrackColor();
					}
				});
			}
		}

		/*
		 * Normal color
		 */
		{
			_lblOutlineColor = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblOutlineColor);

			_lblOutlineColor.setText(Messages.TourTrack_Properties_Label_OutlineColor);
			_lblOutlineColor.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColor_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
			{
				_comboOutlineColorMode = createUI_Combo_ColorMode(container);
				_colorOutlineColor = createUI_ColorSelector(container);
				_spinnerOutlineOpacity = createUI_Spinner_ColorOpacity(container);
				_spinnerTrackPositionSize = createUI_Spinner_PositionSize(container);
			}
		}

		/*
		 * Selected color
		 */
		{
			_lblOutlineColor_Selected = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.applyTo(_lblOutlineColor_Selected);

			_lblOutlineColor_Selected.setText(Messages.TourTrack_Properties_Label_OutlineColorSelected);
			_lblOutlineColor_Selected.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColorSelected_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
			{
				_comboOutlineColorMode_Selected = createUI_Combo_ColorMode(container);
				_colorOutlineColor_Selected = createUI_ColorSelector(container);
				_spinnerOutlineOpacity_Selected = createUI_Spinner_ColorOpacity(container);
				_spinnerTrackPositionSize_Selected = createUI_Spinner_PositionSize(container);
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
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
			{
				_comboOutlineColorMode_Hovered = createUI_Combo_ColorMode(container);
				_colorOutlineColor_Hovered = createUI_ColorSelector(container);
				_spinnerOutlineOpacity_Hovered = createUI_Spinner_ColorOpacity(container);
				_spinnerTrackPositionSize_Hovered = createUI_Spinner_PositionSize(container);
			}
		}

		/*
		 * Hovered + Selected color
		 */
		{
			_lblOutlineColor_HovSel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblOutlineColor_HovSel);

			_lblOutlineColor_HovSel.setText(Messages.TourTrack_Properties_Label_OutlineColorHovSel);
			_lblOutlineColor_HovSel.setToolTipText(Messages.TourTrack_Properties_Label_OutlineColorHovSel_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_comboOutlineColorMode_HovSel = createUI_Combo_ColorMode(container);
				_colorOutlineColor_HovSel = createUI_ColorSelector(container);
				_spinnerOutlineOpacity_HovSel = createUI_Spinner_ColorOpacity(container);
			}
		}
	}

	private void createUI_400_ExtrudePath(final Composite container) {

		/*
		 * Extrude path
		 */
		{
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
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_comboInteriorColorMode = createUI_Combo_ColorMode(container);
				_colorInteriorColor = createUI_ColorSelector(container);
				_spinnerInteriorOpacity = createUI_Spinner_ColorOpacity(container);
			}
		}

		/*
		 * Curtain selected color
		 */
		{
			_lblCurtainColor_Selected = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColor_Selected);

			_lblCurtainColor_Selected.setText(Messages.TourTrack_Properties_Label_CurtainColorSelected);
			_lblCurtainColor_Selected.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorSelected_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_comboInteriorColorMode_Selected = createUI_Combo_ColorMode(container);
				_colorInteriorColor_Selected = createUI_ColorSelector(container);
				_spinnerInteriorOpacity_Selected = createUI_Spinner_ColorOpacity(container);
			}
		}

		/*
		 * Curtain hovered color
		 */
		{
			_lblCurtainColor_Hovered = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColor_Hovered);

			_lblCurtainColor_Hovered.setText(Messages.TourTrack_Properties_Label_CurtainColorHovered);
			_lblCurtainColor_Hovered.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovered_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_comboInteriorColorMode_Hovered = createUI_Combo_ColorMode(container);
				_colorInteriorColor_Hovered = createUI_ColorSelector(container);
				_spinnerInteriorOpacity_Hovered = createUI_Spinner_ColorOpacity(container);
			}
		}

		/*
		 * Curtain hovered + selected color
		 */
		{
			_lblCurtainColor_HovSel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblCurtainColor_HovSel);

			_lblCurtainColor_HovSel.setText(Messages.TourTrack_Properties_Label_CurtainColorHovSel);
			_lblCurtainColor_HovSel.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovSel_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_comboInteriorColorMode_HovSel = createUI_Combo_ColorMode(container);
				_colorInteriorColor_HovSel = createUI_ColorSelector(container);
				_spinnerInteriorOpacity_HovSel = createUI_Spinner_ColorOpacity(container);
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
			_chkAltitudeOffset.setToolTipText(Messages.TourTrack_Properties_Checkbox_AltitudeOffset_Tooltip);
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
						onModifyConfig();
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
					.applyTo(_chkFollowTerrain);
			_chkFollowTerrain.setText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain);
			_chkFollowTerrain.setToolTipText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain_Tooltip);
			_chkFollowTerrain.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void createUI_600_PathResolution(final Composite parent) {

		{
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
	}

	private ColorSelectorExtended createUI_ColorSelector(final Composite parent) {

		final ColorSelectorExtended colorSelector = new ColorSelectorExtended(parent);
		GridDataFactory.swtDefaults()//
				.grab(false, true)
				.align(SWT.BEGINNING, SWT.BEGINNING)
				.applyTo(colorSelector.getButton());

		colorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent event) {
				onModifyConfig();
			}
		});
		colorSelector.addOpenListener(this);

		return colorSelector;
	}

	private Combo createUI_Combo_ColorMode(final Composite container) {

		final Combo combo = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(combo);
		combo.setVisibleItemCount(10);
		combo.addSelectionListener(_defaultSelectionListener);

		return combo;
	}

	private Spinner createUI_Spinner_ColorOpacity(final Composite parent) {

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
				onModifyConfig();
			}
		});

		return spinnerOutlineOpacity;
	}

	private Spinner createUI_Spinner_PositionSize(final Composite container) {

		final Spinner spinnerTrackPositionSize = new Spinner(container, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(spinnerTrackPositionSize);

		/**
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 * <p>
		 * Setting minimum position size to 0 will almost every time crash the whole app when the
		 * user is changing from 0 to 1, could not figure out why, therefore the minimum is set to
		 * 1.
		 * <p>
		 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		 */
		spinnerTrackPositionSize.setMinimum(1);

		spinnerTrackPositionSize.setMaximum(60);
		spinnerTrackPositionSize.setIncrement(1);
		spinnerTrackPositionSize.setPageIncrement(50);
		spinnerTrackPositionSize.addSelectionListener(_defaultSelectionListener);
		spinnerTrackPositionSize.addMouseWheelListener(new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
				onModifyConfig();
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

		final boolean isOutlineSolidColor = _trackConfig.outlineColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_Hovered = _trackConfig.outlineColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_HovSel = _trackConfig.outlineColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_Selected = _trackConfig.outlineColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

		final boolean isInteriorSolidColor = isShowCurtain
				&& _trackConfig.interiorColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_Hovered = isShowCurtain
				&& _trackConfig.interiorColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_HovSel = isShowCurtain
				&& _trackConfig.interiorColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_Selected = isShowCurtain
				&& _trackConfig.interiorColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

		// altitude
		_chkAltitudeOffset.setEnabled(isAbsoluteAltitude);
		_spinnerAltitudeOffsetDistance.setEnabled(isAbsoluteAltitudeEnabled);

		// track position
		_chkTrackPositions.setEnabled(isTrackPositionVisible);

		_lblTrackPositionThreshold.setEnabled(isShowTrackPosition);

		_spinnerTrackPositionSize.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionSize_Hovered.setEnabled(isTrackPositionVisible);
		_spinnerTrackPositionSize_Selected.setEnabled(isTrackPositionVisible);
		_spinnerTrackPositionThreshold.setEnabled(isShowTrackPosition);

		// extrude track
		_chkExtrudePath.setEnabled(isClampToGround == false);

		_lblCurtainColor.setEnabled(isShowCurtain);
		_lblCurtainColor_Hovered.setEnabled(isShowCurtain);
		_lblCurtainColor_HovSel.setEnabled(isShowCurtain);
		_lblCurtainColor_Selected.setEnabled(isShowCurtain);

		// vertical lines are painted with the outline color
		_colorOutlineColor.setEnabled(isOutlineSolidColor || _trackConfig.isDrawVerticals);
		_colorOutlineColor_Hovered.setEnabled(isOutlineSolidColor_Hovered);
		_colorOutlineColor_HovSel.setEnabled(isOutlineSolidColor_HovSel);
		_colorOutlineColor_Selected.setEnabled(isOutlineSolidColor_Selected);

		// interior
		_comboInteriorColorMode.setEnabled(isShowCurtain);
		_comboInteriorColorMode_Hovered.setEnabled(isShowCurtain);
		_comboInteriorColorMode_HovSel.setEnabled(isShowCurtain);
		_comboInteriorColorMode_Selected.setEnabled(isShowCurtain);
		_colorInteriorColor.setEnabled(isInteriorSolidColor);
		_colorInteriorColor_Hovered.setEnabled(isInteriorSolidColor_Hovered);
		_colorInteriorColor_HovSel.setEnabled(isInteriorSolidColor_HovSel);
		_colorInteriorColor_Selected.setEnabled(isInteriorSolidColor_Selected);
		_spinnerInteriorOpacity.setEnabled(isShowCurtain);
		_spinnerInteriorOpacity_Hovered.setEnabled(isShowCurtain);
		_spinnerInteriorOpacity_HovSel.setEnabled(isShowCurtain);
		_spinnerInteriorOpacity_Selected.setEnabled(isShowCurtain);

		_chkDrawVerticals.setEnabled(isShowCurtain);

		// track color
		_btnTrackColor.setEnabled(isGradientColor);
	}

	/**
	 * @param combo
	 * @return Returns combo selection index or 0 when nothing is selected.
	 */
	private int getComboIndex(final Combo combo) {

		int pathResolutionIndex = combo.getSelectionIndex();

		if (pathResolutionIndex == -1) {
			pathResolutionIndex = 0;
		}

		return pathResolutionIndex;
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
		Map3Manager.getLayer_TourTrack().onModifyConfig();

		// update map
//		Map3Manager.redrawMap();
	}

	private void onDispose() {

	}

	private void onModifyConfig() {

		saveState();

		enableControls();

		// update layer
		Map3Manager.getLayer_TourTrack().onModifyConfig();

		// update sliders
		final Map3View map3View = Map3Manager.getMap3View();
		if (map3View != null) {
			map3View.onModifyConfig();
		}
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

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

		_trackConfig = Map3Manager.getLayer_TourTrack().getConfig();

		// track
		_spinnerDirectionArrowSize.setSelection((int) (_trackConfig.directionArrowSize));

		// line color
		_spinnerOutlineWidth.setSelection((int) (_trackConfig.outlineWidth));
		_comboOutlineColorMode.select(_trackConfig.getColorModeIndex(_trackConfig.outlineColorMode));
		_comboOutlineColorMode_Hovered.select(_trackConfig.getColorModeIndex(_trackConfig.outlineColorMode_Hovered));
		_comboOutlineColorMode_HovSel.select(_trackConfig.getColorModeIndex(_trackConfig.outlineColorMode_HovSel));
		_comboOutlineColorMode_Selected.select(_trackConfig.getColorModeIndex(_trackConfig.outlineColorMode_Selected));
		_colorOutlineColor.setColorValue(_trackConfig.outlineColor);
		_colorOutlineColor_Hovered.setColorValue(_trackConfig.outlineColor_Hovered);
		_colorOutlineColor_HovSel.setColorValue(_trackConfig.outlineColor_HovSel);
		_colorOutlineColor_Selected.setColorValue(_trackConfig.outlineColor_Selected);
		_spinnerOutlineOpacity.setSelection((int) (_trackConfig.outlineOpacity * 100));
		_spinnerOutlineOpacity_Hovered.setSelection((int) (_trackConfig.outlineOpacity_Hovered * 100));
		_spinnerOutlineOpacity_HovSel.setSelection((int) (_trackConfig.outlineOpacity_HovSel * 100));
		_spinnerOutlineOpacity_Selected.setSelection((int) (_trackConfig.outlineOpacity_Selected * 100));

		// curtain color
		_chkExtrudePath.setSelection(_trackConfig.isExtrudePath);
		_comboInteriorColorMode.select(_trackConfig.getColorModeIndex(_trackConfig.interiorColorMode));
		_comboInteriorColorMode_Hovered.select(_trackConfig.getColorModeIndex(_trackConfig.interiorColorMode_Hovered));
		_comboInteriorColorMode_HovSel.select(_trackConfig.getColorModeIndex(_trackConfig.interiorColorMode_HovSel));
		_comboInteriorColorMode_Selected
				.select(_trackConfig.getColorModeIndex(_trackConfig.interiorColorMode_Selected));
		_colorInteriorColor.setColorValue(_trackConfig.interiorColor);
		_colorInteriorColor_Hovered.setColorValue(_trackConfig.interiorColor_Hovered);
		_colorInteriorColor_HovSel.setColorValue(_trackConfig.interiorColor_HovSel);
		_colorInteriorColor_Selected.setColorValue(_trackConfig.interiorColor_Selected);
		_spinnerInteriorOpacity.setSelection((int) (_trackConfig.interiorOpacity * 100));
		_spinnerInteriorOpacity_Hovered.setSelection((int) (_trackConfig.interiorOpacity_Hovered * 100));
		_spinnerInteriorOpacity_HovSel.setSelection((int) (_trackConfig.interiorOpacity_HovSel * 100));
		_spinnerInteriorOpacity_Selected.setSelection((int) (_trackConfig.interiorOpacity_Selected * 100));

		// verticals
		_chkDrawVerticals.setSelection(_trackConfig.isDrawVerticals);

		// track position
		_chkTrackPositions.setSelection(_trackConfig.isShowTrackPosition);
		_spinnerTrackPositionSize.setSelection((int) (_trackConfig.trackPositionSize * 1));
		_spinnerTrackPositionSize_Hovered.setSelection((int) (_trackConfig.trackPositionSize_Hovered * 1));
		_spinnerTrackPositionSize_Selected.setSelection((int) (_trackConfig.trackPositionSize_Selected * 1));
		_spinnerTrackPositionThreshold.setSelection((int) _trackConfig.trackPositionThreshold);

		// altitude
		_comboAltitude.select(_trackConfig.getAltitudeModeIndex());
		_chkAltitudeOffset.setSelection(_trackConfig.isAbsoluteOffset);
		_spinnerAltitudeOffsetDistance.setSelection(//
				(int) (_trackConfig.altitudeVerticalOffset / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE));
		_chkFollowTerrain.setSelection(_trackConfig.isFollowTerrain);

		// path
		_comboPathResolution.select(_trackConfig.getPathResolutionIndex());

		updateUI();
	}

	/**
	 * Saves state values from the UI in the tour track configuration.
	 */
	private void saveState() {

		final boolean backupIsAltitudeOffset = _trackConfig.isAbsoluteOffset;
		final boolean backupIsAbsoluteAltitudeMode = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean backupIsFollowTerrain = _trackConfig.isFollowTerrain;
		final int backupAltitudeOffsetDistance = _trackConfig.altitudeVerticalOffset;
		final int backupPathResolution = _trackConfig.pathResolution;

		int altitudeModeIndex = _comboAltitude.getSelectionIndex();
		if (altitudeModeIndex == -1) {
			altitudeModeIndex = 0;
		}

		final int altitudeOffsetMetric = (int) (_spinnerAltitudeOffsetDistance.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);

		// update config

		// track
		_trackConfig.directionArrowSize = _spinnerDirectionArrowSize.getSelection();

		// line
		_trackConfig.outlineWidth = _spinnerOutlineWidth.getSelection();
		_trackConfig.outlineColorMode = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode)].value;
		_trackConfig.outlineColorMode_Hovered = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Hovered)].value;
		_trackConfig.outlineColorMode_HovSel = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_HovSel)].value;
		_trackConfig.outlineColorMode_Selected = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Selected)].value;
		_trackConfig.outlineColor = _colorOutlineColor.getColorValue();
		_trackConfig.outlineColor_Hovered = _colorOutlineColor_Hovered.getColorValue();
		_trackConfig.outlineColor_HovSel = _colorOutlineColor_HovSel.getColorValue();
		_trackConfig.outlineColor_Selected = _colorOutlineColor_Selected.getColorValue();
		_trackConfig.outlineOpacity = _spinnerOutlineOpacity.getSelection() / 100.0;
		_trackConfig.outlineOpacity_Hovered = _spinnerOutlineOpacity_Hovered.getSelection() / 100.0;
		_trackConfig.outlineOpacity_HovSel = _spinnerOutlineOpacity_HovSel.getSelection() / 100.0;
		_trackConfig.outlineOpacity_Selected = _spinnerOutlineOpacity_Selected.getSelection() / 100.0;

		// curtain
		_trackConfig.isExtrudePath = _chkExtrudePath.getSelection();
		_trackConfig.interiorColorMode = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode)].value;
		_trackConfig.interiorColorMode_Hovered = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Hovered)].value;
		_trackConfig.interiorColorMode_HovSel = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_HovSel)].value;
		_trackConfig.interiorColorMode_Selected = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Selected)].value;
		_trackConfig.interiorColor = _colorInteriorColor.getColorValue();
		_trackConfig.interiorColor_Hovered = _colorInteriorColor_Hovered.getColorValue();
		_trackConfig.interiorColor_HovSel = _colorInteriorColor_HovSel.getColorValue();
		_trackConfig.interiorColor_Selected = _colorInteriorColor_Selected.getColorValue();
		_trackConfig.interiorOpacity = _spinnerInteriorOpacity.getSelection() / 100.0;
		_trackConfig.interiorOpacity_Hovered = _spinnerInteriorOpacity_Hovered.getSelection() / 100.0;
		_trackConfig.interiorOpacity_HovSel = _spinnerInteriorOpacity_HovSel.getSelection() / 100.0;
		_trackConfig.interiorOpacity_Selected = _spinnerInteriorOpacity_Selected.getSelection() / 100.0;

		// verticals
		_trackConfig.isDrawVerticals = _chkDrawVerticals.getSelection();

		// track position
		_trackConfig.isShowTrackPosition = _chkTrackPositions.getSelection();
		_trackConfig.trackPositionSize = _spinnerTrackPositionSize.getSelection() / 1.0;
		_trackConfig.trackPositionSize_Hovered = _spinnerTrackPositionSize_Hovered.getSelection() / 1.0;
		_trackConfig.trackPositionSize_Selected = _spinnerTrackPositionSize_Selected.getSelection() / 1.0;
		_trackConfig.trackPositionThreshold = _spinnerTrackPositionThreshold.getSelection();

		// altitude
		_trackConfig.isAbsoluteOffset = _chkAltitudeOffset.getSelection();
		_trackConfig.altitudeMode = TourTrackConfig.ALTITUDE_MODE[getComboIndex(_comboAltitude)].value;
		_trackConfig.altitudeVerticalOffset = altitudeOffsetMetric;
		_trackConfig.isFollowTerrain = _chkFollowTerrain.getSelection();

		// path
		_trackConfig.pathResolution = TourTrackConfig.PATH_RESOLUTION[getComboIndex(_comboPathResolution)].value;

		final boolean isAbsoluteAltitudeMode = _trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isAbsoluteAltitudeNotModified = //
		backupIsAltitudeOffset == _trackConfig.isAbsoluteOffset
				&& backupIsAbsoluteAltitudeMode == isAbsoluteAltitudeMode;

		/*
		 * check if tracks must be recreated
		 */
		if (//
			// altitude offset (vertical distance) is NOT modified
		backupAltitudeOffsetDistance == _trackConfig.altitudeVerticalOffset //

				// path resolution is NOT modified
				&& backupPathResolution == _trackConfig.pathResolution //

				// follow terrain is NOT modified
				&& backupIsFollowTerrain == _trackConfig.isFollowTerrain //

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

		if (_lblAltitudeOffsetDistanceUnit == null) {

			// dialog is not yet created

			return;
		}

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

	private void updateUI_FillCombo(final Combo combo) {

		// fill track color combo
		for (final ComboEntry colorMode : TourTrackConfig.TRACK_COLOR_MODE) {
			combo.add(colorMode.label);
		}
	}

	private void updateUI_initial() {

		// fill altitude mode combo
		for (final ComboEntry altiMode : TourTrackConfig.ALTITUDE_MODE) {
			_comboAltitude.add(altiMode.label);
		}

		// fill path resolution combo
		for (final ComboEntry pathResolution : TourTrackConfig.PATH_RESOLUTION) {
			_comboPathResolution.add(pathResolution.label);
		}

		updateUI_FillCombo(_comboOutlineColorMode);
		updateUI_FillCombo(_comboOutlineColorMode_Hovered);
		updateUI_FillCombo(_comboOutlineColorMode_HovSel);
		updateUI_FillCombo(_comboOutlineColorMode_Selected);

		updateUI_FillCombo(_comboInteriorColorMode);
		updateUI_FillCombo(_comboInteriorColorMode_Hovered);
		updateUI_FillCombo(_comboInteriorColorMode_HovSel);
		updateUI_FillCombo(_comboInteriorColorMode_Selected);
	}

}
