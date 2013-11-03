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

import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.color.MapColorId;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.tourtrack.ComboEntry;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
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

	private MouseWheelListener		_defaultMouseWheelListener;
	private SelectionAdapter		_defaultSelectionListener;

	private boolean					_isAnotherDialogOpened;

	private MapColorId				_trackColorId;
	private boolean					_isUpdateUI;

	private PixelConverter			_pc;

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
	private Button					_chkShowInterior;
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
	private Combo					_comboName;
	private Combo					_comboOutlineColorMode;
	private Combo					_comboOutlineColorMode_Hovered;
	private Combo					_comboOutlineColorMode_HovSel;
	private Combo					_comboOutlineColorMode_Selected;

	private Label					_lblAltitudeOffsetDistanceUnit;
	private Label					_lblInteriorColor;
	private Label					_lblInteriorColor_Hovered;
	private Label					_lblInteriorColor_HovSel;
	private Label					_lblInteriorColor_Selected;
	private Label					_lblOutlineColor;
	private Label					_lblOutlineColor_HovSel;
	private Label					_lblOutlineColor_Selected;
	private Label					_lblTrackColor;
	private Label					_lblTrackPositionThreshold;
	private Label					_lblTrackPositionThresholdAbsolute;

	private Spinner					_spinnerAltitudeOffsetDistance;
	private Spinner					_spinnerDirectionArrowDistance;
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

	private Text					_textName;

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

		_defaultMouseWheelListener = new MouseWheelListener() {
			public void mouseScrolled(final MouseEvent event) {
				Util.adjustSpinnerValueOnMouseScroll(event);
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

		_pc = new PixelConverter(parent);

		_shellContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().margins(SHELL_MARGIN, SHELL_MARGIN).applyTo(_shellContainer);
//		_shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_000_DialogTitle(_shellContainer);

			final Composite container = new Composite(_shellContainer, SWT.NO_FOCUS);
			GridLayoutFactory.fillDefaults()//
					.numColumns(2)
					.applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			{
				createUI_050_Name(container);
				createUI_100_Line(container);
				createUI_110_DirectionArrow(container);
				createUI_200_TrackPosition(container);
				createUI_300_TrackColor(container);
				createUI_350_Outline(container);
				createUI_400_ExtrudePath(container);
				createUI_500_Altitude(container);
				createUI_600_PathResolution(container);
			}
		}

		return _shellContainer;
	}

	private void createUI_000_DialogTitle(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			/*
			 * Label: Title
			 */
			final Label title = new Label(container, SWT.LEAD);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(title);
			title.setFont(JFaceResources.getBannerFont());
			title.setText(Messages.TourTrack_Properties_Label_DialogTitle);

			/*
			 * Combo: Configutation
			 */
			_comboName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.hint(_pc.convertHorizontalDLUsToPixels(15 * 4), SWT.DEFAULT)
					.applyTo(_comboName);
			_comboName.setVisibleItemCount(20);
			_comboName.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectConfig();
				}
			});

			/*
			 * Button: Default
			 */
			_btnDefault = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.align(SWT.TRAIL, SWT.FILL)
					.grab(false, false)
					.applyTo(_btnDefault);
			_btnDefault.setText(Messages.TourTrack_Properties_Button_Default);
			_btnDefault.setToolTipText(Messages.TourTrack_Properties_Button_Default_Tooltip);
			_btnDefault.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectDefault();
				}
			});
		}
	}

	private void createUI_050_Name(final Composite parent) {

		/*
		 * Name
		 */
		{
			/*
			 * Label
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_Name);
			label.setToolTipText(Messages.TourTrack_Properties_Label_Name_Tooltip);

			/*
			 * Text
			 */
			_textName = new Text(parent, SWT.BORDER);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_textName);
			_textName.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					onModifyName();
				}
			});
		}
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
			_spinnerOutlineWidth.setMinimum(TourTrackConfigManager.OUTLINE_WIDTH_MIN);
			_spinnerOutlineWidth.setMaximum(TourTrackConfigManager.OUTLINE_WIDTH_MAX);
			_spinnerOutlineWidth.setIncrement(1);
			_spinnerOutlineWidth.setPageIncrement(10);
			_spinnerOutlineWidth.addSelectionListener(_defaultSelectionListener);
			_spinnerOutlineWidth.addMouseWheelListener(_defaultMouseWheelListener);
		}
	}

	private void createUI_110_DirectionArrow(final Composite parent) {

		/*
		 * Direction Arrow
		 */
		{
			/*
			 * Label
			 */
			final Label label = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

			label.setText(Messages.TourTrack_Properties_Label_DirectionArrowSize);
			label.setToolTipText(Messages.TourTrack_Properties_Label_DirectionArrowSize_Tooltip);

			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				/*
				 * Size
				 */
				_spinnerDirectionArrowSize = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerDirectionArrowSize);
				_spinnerDirectionArrowSize.setMinimum(TourTrackConfigManager.DIRECTION_ARROW_SIZE_MIN);
				_spinnerDirectionArrowSize.setMaximum(TourTrackConfigManager.DIRECTION_ARROW_SIZE_MAX);
				_spinnerDirectionArrowSize.setIncrement(10);
				_spinnerDirectionArrowSize.setPageIncrement(50);
				_spinnerDirectionArrowSize.addSelectionListener(_defaultSelectionListener);
				_spinnerDirectionArrowSize.addMouseWheelListener(_defaultMouseWheelListener);

				/*
				 * Vertical distance
				 */
				_spinnerDirectionArrowDistance = new Spinner(container, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerDirectionArrowDistance);
				_spinnerDirectionArrowDistance.setMinimum(TourTrackConfigManager.DIRECTION_ARROW_VERTICAL_DISTANCE_MIN);
				_spinnerDirectionArrowDistance.setMaximum(TourTrackConfigManager.DIRECTION_ARROW_VERTICAL_DISTANCE_MAX);
				_spinnerDirectionArrowDistance.setIncrement(1);
				_spinnerDirectionArrowDistance.setPageIncrement(5);
				_spinnerDirectionArrowDistance.addSelectionListener(_defaultSelectionListener);
				_spinnerDirectionArrowDistance.addMouseWheelListener(_defaultMouseWheelListener);
			}
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
				_spinnerTrackPositionThreshold.setMinimum(TourTrackConfigManager.TRACK_POSITION_THRESHOLD_MIN);
				_spinnerTrackPositionThreshold.setMaximum(TourTrackConfigManager.TRACK_POSITION_THRESHOLD_MAX);
				_spinnerTrackPositionThreshold.setIncrement(1);
				_spinnerTrackPositionThreshold.setPageIncrement(10);
				_spinnerTrackPositionThreshold.addSelectionListener(_defaultSelectionListener);
				_spinnerTrackPositionThreshold.addMouseWheelListener(_defaultMouseWheelListener);

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

	}

	private void createUI_350_Outline(final Composite parent) {
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
			_chkShowInterior = new Button(container, SWT.CHECK);
			GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkShowInterior);
			_chkShowInterior.setText(Messages.TourTrack_Properties_Checkbox_ExtrudePath);
			_chkShowInterior.setToolTipText(Messages.TourTrack_Properties_Checkbox_ExtrudePath_Tooltip);
			_chkShowInterior.addSelectionListener(_defaultSelectionListener);

			createUI_410_Interior(container);
			createUI_420_Verticals(container);
		}
	}

	private void createUI_410_Interior(final Composite parent) {

		/*
		 * Curtain/Interior color
		 */
		{
			_lblInteriorColor = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblInteriorColor);

			_lblInteriorColor.setText(Messages.TourTrack_Properties_Label_CurtainColor);
			_lblInteriorColor.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColor_Tooltip);

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
			_lblInteriorColor_Selected = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblInteriorColor_Selected);

			_lblInteriorColor_Selected.setText(Messages.TourTrack_Properties_Label_CurtainColorSelected);
			_lblInteriorColor_Selected.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorSelected_Tooltip);

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
			_lblInteriorColor_Hovered = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblInteriorColor_Hovered);

			_lblInteriorColor_Hovered.setText(Messages.TourTrack_Properties_Label_CurtainColorHovered);
			_lblInteriorColor_Hovered.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovered_Tooltip);

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
			_lblInteriorColor_HovSel = new Label(parent, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.CENTER)
					.indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
					.applyTo(_lblInteriorColor_HovSel);

			_lblInteriorColor_HovSel.setText(Messages.TourTrack_Properties_Label_CurtainColorHovSel);
			_lblInteriorColor_HovSel.setToolTipText(Messages.TourTrack_Properties_Label_CurtainColorHovSel_Tooltip);

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
				_spinnerAltitudeOffsetDistance.setMinimum(TourTrackConfigManager.ALTITUDE_OFFSET_MIN);
				_spinnerAltitudeOffsetDistance.setMaximum(TourTrackConfigManager.ALTITUDE_OFFSET_MAX);
				_spinnerAltitudeOffsetDistance.setIncrement(1);
				_spinnerAltitudeOffsetDistance.setPageIncrement(10);
				_spinnerAltitudeOffsetDistance.addSelectionListener(_defaultSelectionListener);
				_spinnerAltitudeOffsetDistance.addMouseWheelListener(_defaultMouseWheelListener);

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

		final Spinner spinnerOpacity = new Spinner(parent, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(spinnerOpacity);

		spinnerOpacity.setMinimum(TourTrackConfigManager.OPACITY_MIN);
		spinnerOpacity.setMaximum(TourTrackConfigManager.OPACITY_MAX);
		spinnerOpacity.setDigits(TourTrackConfigManager.OPACITY_DIGITS);
		spinnerOpacity.setIncrement(1);
		spinnerOpacity.setPageIncrement(10);
		spinnerOpacity.addSelectionListener(_defaultSelectionListener);
		spinnerOpacity.addMouseWheelListener(_defaultMouseWheelListener);

		return spinnerOpacity;
	}

	private Spinner createUI_Spinner_PositionSize(final Composite container) {

		final Spinner spinnerPositionSize = new Spinner(container, SWT.BORDER);
		GridDataFactory.fillDefaults() //
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(spinnerPositionSize);

		spinnerPositionSize.setMinimum(TourTrackConfigManager.TRACK_POSITION_SIZE_MIN);
		spinnerPositionSize.setMaximum(TourTrackConfigManager.TRACK_POSITION_SIZE_MAX);
		spinnerPositionSize.setIncrement(1);
		spinnerPositionSize.setPageIncrement(50);
		spinnerPositionSize.addSelectionListener(_defaultSelectionListener);
		spinnerPositionSize.addMouseWheelListener(_defaultMouseWheelListener);

		return spinnerPositionSize;
	}

	private void enableControls() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final boolean isAbsoluteAltitude = trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isClampToGround = trackConfig.altitudeMode == WorldWind.CLAMP_TO_GROUND;
		final boolean isAbsoluteAltitudeEnabled = _chkAltitudeOffset.getSelection() && isAbsoluteAltitude;
		final boolean isShowCurtain = isClampToGround == false && trackConfig.isShowInterior;
		final boolean isTrackPositionVisible = trackConfig.outlineWidth > 0.0;
		final boolean isShowTrackPosition = trackConfig.isShowTrackPosition & isTrackPositionVisible;

		// Hr zones are not yet supported
		final boolean isGradientColor = _trackColorId != MapColorId.HrZone;

		final boolean isOutlineSolidColor = trackConfig.outlineColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_Hovered = trackConfig.outlineColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_HovSel = trackConfig.outlineColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isOutlineSolidColor_Selected = trackConfig.outlineColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

		// vertical lines are painted with the outline color
		final boolean isOutLineColor = isOutlineSolidColor || trackConfig.isDrawVerticals;

		final boolean isInteriorSolidColor = isShowCurtain
				&& trackConfig.interiorColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_Hovered = isShowCurtain
				&& trackConfig.interiorColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_HovSel = isShowCurtain
				&& trackConfig.interiorColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
		final boolean isInteriorSolidColor_Selected = isShowCurtain
				&& trackConfig.interiorColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

		// altitude
		_chkAltitudeOffset.setEnabled(isAbsoluteAltitude);
		_spinnerAltitudeOffsetDistance.setEnabled(isAbsoluteAltitudeEnabled);

		// track position
		_chkTrackPositions.setEnabled(isTrackPositionVisible);

		_lblTrackPositionThreshold.setEnabled(isShowTrackPosition);

		_spinnerTrackPositionSize.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionSize_Hovered.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionSize_Selected.setEnabled(isShowTrackPosition);
		_spinnerTrackPositionThreshold.setEnabled(isShowTrackPosition);

		// extrude track
		_chkShowInterior.setEnabled(isClampToGround == false);

		/*
		 * Outline
		 */
		_colorOutlineColor.setEnabled(isOutLineColor);

		_spinnerOutlineOpacity_Hovered.setEnabled(isOutlineSolidColor_Hovered);
		_spinnerOutlineOpacity_HovSel.setEnabled(isOutlineSolidColor_HovSel);
		_spinnerOutlineOpacity_Selected.setEnabled(isOutlineSolidColor_Selected);

		/*
		 * Interior
		 */
		_lblInteriorColor.setEnabled(isShowCurtain);
		_lblInteriorColor_Hovered.setEnabled(isShowCurtain);
		_lblInteriorColor_HovSel.setEnabled(isShowCurtain);
		_lblInteriorColor_Selected.setEnabled(isShowCurtain);

		_comboInteriorColorMode.setEnabled(isShowCurtain);
		_comboInteriorColorMode_Hovered.setEnabled(isShowCurtain);
		_comboInteriorColorMode_HovSel.setEnabled(isShowCurtain);
		_comboInteriorColorMode_Selected.setEnabled(isShowCurtain);

		_colorInteriorColor.setEnabled(isInteriorSolidColor);
		_colorInteriorColor_Hovered.setEnabled(isInteriorSolidColor_Hovered);
		_colorInteriorColor_HovSel.setEnabled(isInteriorSolidColor_HovSel);
		_colorInteriorColor_Selected.setEnabled(isInteriorSolidColor_Selected);

		_spinnerInteriorOpacity.setEnabled(isInteriorSolidColor);
		_spinnerInteriorOpacity_Hovered.setEnabled(isInteriorSolidColor_Hovered);
		_spinnerInteriorOpacity_HovSel.setEnabled(isInteriorSolidColor_HovSel);
		_spinnerInteriorOpacity_Selected.setEnabled(isInteriorSolidColor_Selected);

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

	private void onDispose() {

	}

	private void onModifyConfig() {

		saveStateWithRecreateCheck();

		updateUI();

		enableControls();

		// update layer
		Map3Manager.getLayer_TourTrack().onModifyConfig();

		// update sliders
		final Map3View map3View = Map3Manager.getMap3View();
		if (map3View != null) {
			map3View.onModifyConfig();
		}
	}

	private void onModifyName() {

		if (_isUpdateUI) {
			return;
		}

		// update text in the combo
		final int selectedIndex = _comboName.getSelectionIndex();

		_comboName.setItem(selectedIndex, _textName.getText());

		saveState();
	}

	@Override
	protected void onMouseMoveInToolTip(final MouseEvent mouseEvent) {

	}

	private void onSelectConfig() {

		final int selectedIndex = _comboName.getSelectionIndex();
		final ArrayList<TourTrackConfig> allConfigurations = TourTrackConfigManager.getAllConfigurations();

		final TourTrackConfig selectedConfig = allConfigurations.get(selectedIndex);
		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		if (selectedConfig == trackConfig) {

			// config has not changed
			return;
		}

		// keep data from previous config
		saveState();

		TourTrackConfigManager.setActiveConfig(selectedConfig);

		updateUI_SetActiveConfig();
	}

	private void onSelectDefault() {

		TourTrackConfigManager.resetActiveConfig();

		updateUI_SetActiveConfig();
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

		_isUpdateUI = true;

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		// get active config AFTER getting the index because this could change the active config
		final int activeConfigIndex = TourTrackConfigManager.getActiveConfigIndex();

		_comboName.select(activeConfigIndex);
		_textName.setText(trackConfig.name);

		// track
		_spinnerDirectionArrowDistance.setSelection((int) (trackConfig.directionArrowDistance));
		_spinnerDirectionArrowSize.setSelection((int) (trackConfig.directionArrowSize));

		// line color
		_spinnerOutlineWidth.setSelection((int) (trackConfig.outlineWidth));

		_comboOutlineColorMode.select(trackConfig.getColorModeIndex(trackConfig.outlineColorMode));
		_comboOutlineColorMode_Hovered.select(trackConfig.getColorModeIndex(trackConfig.outlineColorMode_Hovered));
		_comboOutlineColorMode_HovSel.select(trackConfig.getColorModeIndex(trackConfig.outlineColorMode_HovSel));
		_comboOutlineColorMode_Selected.select(trackConfig.getColorModeIndex(trackConfig.outlineColorMode_Selected));

		_colorOutlineColor.setColorValue(trackConfig.outlineColor);
		_colorOutlineColor_Hovered.setColorValue(trackConfig.outlineColor_Hovered);
		_colorOutlineColor_HovSel.setColorValue(trackConfig.outlineColor_HovSel);
		_colorOutlineColor_Selected.setColorValue(trackConfig.outlineColor_Selected);

		_spinnerOutlineOpacity.setSelection((//
				int) (trackConfig.outlineOpacity * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerOutlineOpacity_Hovered.setSelection(//
				(int) (trackConfig.outlineOpacity_Hovered * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerOutlineOpacity_HovSel.setSelection(//
				(int) (trackConfig.outlineOpacity_HovSel * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerOutlineOpacity_Selected.setSelection(//
				(int) (trackConfig.outlineOpacity_Selected * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));

		// curtain color
		_chkShowInterior.setSelection(trackConfig.isShowInterior);

		_comboInteriorColorMode.select(trackConfig.getColorModeIndex(trackConfig.interiorColorMode));
		_comboInteriorColorMode_Hovered.select(trackConfig.getColorModeIndex(trackConfig.interiorColorMode_Hovered));
		_comboInteriorColorMode_HovSel.select(trackConfig.getColorModeIndex(trackConfig.interiorColorMode_HovSel));
		_comboInteriorColorMode_Selected.select(trackConfig.getColorModeIndex(trackConfig.interiorColorMode_Selected));

		_colorInteriorColor.setColorValue(trackConfig.interiorColor);
		_colorInteriorColor_Hovered.setColorValue(trackConfig.interiorColor_Hovered);
		_colorInteriorColor_HovSel.setColorValue(trackConfig.interiorColor_HovSel);
		_colorInteriorColor_Selected.setColorValue(trackConfig.interiorColor_Selected);

		_spinnerInteriorOpacity.setSelection(//
				(int) (trackConfig.interiorOpacity * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerInteriorOpacity_Hovered.setSelection(//
				(int) (trackConfig.interiorOpacity_Hovered * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerInteriorOpacity_HovSel.setSelection(//
				(int) (trackConfig.interiorOpacity_HovSel * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));
		_spinnerInteriorOpacity_Selected.setSelection(//
				(int) (trackConfig.interiorOpacity_Selected * TourTrackConfigManager.OPACITY_DIGITS_FACTOR));

		// verticals
		_chkDrawVerticals.setSelection(trackConfig.isDrawVerticals);

		// track position
		_chkTrackPositions.setSelection(trackConfig.isShowTrackPosition);
		_spinnerTrackPositionSize.setSelection((int) (trackConfig.trackPositionSize));
		_spinnerTrackPositionSize_Hovered.setSelection((int) (trackConfig.trackPositionSize_Hovered));
		_spinnerTrackPositionSize_Selected.setSelection((int) (trackConfig.trackPositionSize_Selected));
		_spinnerTrackPositionThreshold.setSelection(trackConfig.trackPositionThreshold);

		// altitude
		_comboAltitude.select(trackConfig.getAltitudeModeIndex());
		_chkAltitudeOffset.setSelection(trackConfig.isAbsoluteOffset);
		_spinnerAltitudeOffsetDistance.setSelection(//
				(int) (trackConfig.altitudeVerticalOffset / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE));
		_chkFollowTerrain.setSelection(trackConfig.isFollowTerrain);

		// path
		_comboPathResolution.select(trackConfig.getPathResolutionIndex());

		updateUI();

		_isUpdateUI = false;
	}

	private void saveState() {

		final int altitudeOffsetMetric = (int) (_spinnerAltitudeOffsetDistance.getSelection() * net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE);

		// update config

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		trackConfig.name = _textName.getText();

		// track
		trackConfig.directionArrowSize = _spinnerDirectionArrowSize.getSelection();
		trackConfig.directionArrowDistance = _spinnerDirectionArrowDistance.getSelection();

		// line
		trackConfig.outlineWidth = _spinnerOutlineWidth.getSelection();
		trackConfig.outlineColorMode = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode)].value;
		trackConfig.outlineColorMode_Hovered = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Hovered)].value;
		trackConfig.outlineColorMode_HovSel = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_HovSel)].value;
		trackConfig.outlineColorMode_Selected = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Selected)].value;
		trackConfig.outlineColor = _colorOutlineColor.getColorValue();
		trackConfig.outlineColor_Hovered = _colorOutlineColor_Hovered.getColorValue();
		trackConfig.outlineColor_HovSel = _colorOutlineColor_HovSel.getColorValue();
		trackConfig.outlineColor_Selected = _colorOutlineColor_Selected.getColorValue();

		trackConfig.outlineOpacity = _spinnerOutlineOpacity.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.outlineOpacity_Hovered = _spinnerOutlineOpacity_Hovered.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.outlineOpacity_HovSel = _spinnerOutlineOpacity_HovSel.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.outlineOpacity_Selected = _spinnerOutlineOpacity_Selected.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;

		// interior
		trackConfig.isShowInterior = _chkShowInterior.getSelection();
		trackConfig.interiorColorMode = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode)].value;
		trackConfig.interiorColorMode_Hovered = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Hovered)].value;
		trackConfig.interiorColorMode_HovSel = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_HovSel)].value;
		trackConfig.interiorColorMode_Selected = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Selected)].value;
		trackConfig.interiorColor = _colorInteriorColor.getColorValue();
		trackConfig.interiorColor_Hovered = _colorInteriorColor_Hovered.getColorValue();
		trackConfig.interiorColor_HovSel = _colorInteriorColor_HovSel.getColorValue();
		trackConfig.interiorColor_Selected = _colorInteriorColor_Selected.getColorValue();

		trackConfig.interiorOpacity = _spinnerInteriorOpacity.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.interiorOpacity_Hovered = _spinnerInteriorOpacity_Hovered.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.interiorOpacity_HovSel = _spinnerInteriorOpacity_HovSel.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;
		trackConfig.interiorOpacity_Selected = _spinnerInteriorOpacity_Selected.getSelection()
				/ TourTrackConfigManager.OPACITY_DIGITS_FACTOR;

		// verticals
		trackConfig.isDrawVerticals = _chkDrawVerticals.getSelection();

		// track position
		trackConfig.isShowTrackPosition = _chkTrackPositions.getSelection();
		trackConfig.trackPositionSize = _spinnerTrackPositionSize.getSelection();
		trackConfig.trackPositionSize_Hovered = _spinnerTrackPositionSize_Hovered.getSelection();
		trackConfig.trackPositionSize_Selected = _spinnerTrackPositionSize_Selected.getSelection();
		trackConfig.trackPositionThreshold = _spinnerTrackPositionThreshold.getSelection();

		// altitude
		trackConfig.isAbsoluteOffset = _chkAltitudeOffset.getSelection();
		trackConfig.altitudeMode = TourTrackConfig.ALTITUDE_MODE[getComboIndex(_comboAltitude)].value;
		trackConfig.altitudeVerticalOffset = altitudeOffsetMetric;
		trackConfig.isFollowTerrain = _chkFollowTerrain.getSelection();

		// path
		trackConfig.pathResolution = TourTrackConfig.PATH_RESOLUTION[getComboIndex(_comboPathResolution)].value;
	}

	/**
	 * Saves state values from the UI in the tour track configuration.
	 */
	private void saveStateWithRecreateCheck() {

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		final boolean backupIsAltitudeOffset = trackConfig.isAbsoluteOffset;
		final boolean backupIsAbsoluteAltitudeMode = trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean backupIsFollowTerrain = trackConfig.isFollowTerrain;
		final int backupAltitudeOffsetDistance = trackConfig.altitudeVerticalOffset;
		final int backupPathResolution = trackConfig.pathResolution;

		saveState();

		final boolean isAbsoluteAltitudeMode = trackConfig.altitudeMode == WorldWind.ABSOLUTE;
		final boolean isAbsoluteAltitudeNotModified = backupIsAltitudeOffset == trackConfig.isAbsoluteOffset
				&& backupIsAbsoluteAltitudeMode == isAbsoluteAltitudeMode;

		/*
		 * check if tracks must be recreated
		 */
		if (//
		true
		// altitude offset (vertical distance) is NOT modified
				&& backupAltitudeOffsetDistance == trackConfig.altitudeVerticalOffset //

				// path resolution is NOT modified
				&& backupPathResolution == trackConfig.pathResolution //

				// follow terrain is NOT modified
				&& backupIsFollowTerrain == trackConfig.isFollowTerrain //

				// direction arrow distance is NOT modified
//				&& backupDirectionArrowDistance == _trackConfig.directionArrowDistance//

				&& isAbsoluteAltitudeNotModified
		//
		) {

			trackConfig.isRecreateTracks = false;

		} else {

			// altitude offset or altitude mode has changed

			trackConfig.isRecreateTracks = true;
		}
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

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		// position threshold
		final double positionThreshold = Math.pow(10, trackConfig.trackPositionThreshold) / 1000;

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

		for (final TourTrackConfig config : TourTrackConfigManager.getAllConfigurations()) {
			_comboName.add(config.name);
		}

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

	private void updateUI_SetActiveConfig() {

		restoreState();

		enableControls();

		final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

		trackConfig.isRecreateTracks = true;

		// update track layer
		Map3Manager.getLayer_TourTrack().onModifyConfig();
	}

}
