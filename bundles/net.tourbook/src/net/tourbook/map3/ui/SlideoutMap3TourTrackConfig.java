/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map3.ui;

import static org.eclipse.swt.events.MouseTrackListener.mouseExitAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import gov.nasa.worldwind.WorldWind;

import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEntry;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.tourtrack.TourTrackConfig;
import net.tourbook.map3.layer.tourtrack.TourTrackConfigManager;
import net.tourbook.map3.view.Map3Manager;
import net.tourbook.map3.view.Map3View;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
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
public class SlideoutMap3TourTrackConfig extends AnimatedToolTipShell implements IColorSelectorListener {

   private static final String FORMAT_POSITION_THRESHOLD = "%,.0f %s"; //$NON-NLS-1$

   private static final int    SHELL_MARGIN              = 5;

   // initialize with default values which are (should) never be used
   private Rectangle          _toolTipItemBounds = new Rectangle(0, 0, 50, 50);

   private final WaitTimer    _waitTimer         = new WaitTimer();

   private Map3View           _map3View;

   private boolean            _canOpenToolTip;
   private boolean            _isWaitTimerStarted;

   private MouseWheelListener _defaultMouseWheelListener;
   private SelectionListener  _defaultSelectionListener;
   private FocusListener      _keepOpenListener;

   private MapGraphId         _trackColorId;

   private boolean            _isUpdateUI;

   /*
    * UI controls
    */
   private Composite             _shellContainer;

   private Button                _btnReset;
   private Button                _btnTrackColor;

   private Button                _chkAltitudeOffset;
   private Button                _chkAltitudeOffsetRandom;
   private Button                _chkDrawVerticals;
   private Button                _chkShowInterior;
   private Button                _chkFollowTerrain;
   private Button                _chkTrackPositions;

   private ColorSelectorExtended _colorInteriorColor;
   private ColorSelectorExtended _colorInteriorColor_Hovered;
   private ColorSelectorExtended _colorInteriorColor_HovSel;
   private ColorSelectorExtended _colorInteriorColor_Selected;
   private ColorSelectorExtended _colorOutlineColor;
   private ColorSelectorExtended _colorOutlineColor_Hovered;
   private ColorSelectorExtended _colorOutlineColor_HovSel;
   private ColorSelectorExtended _colorOutlineColor_Selected;

   private Combo                 _comboAltitude;
   private Combo                 _comboInteriorColorMode;
   private Combo                 _comboInteriorColorMode_Selected;
   private Combo                 _comboInteriorColorMode_Hovered;
   private Combo                 _comboInteriorColorMode_HovSel;
   private Combo                 _comboName;
   private Combo                 _comboOutlineColorMode;
   private Combo                 _comboOutlineColorMode_Hovered;
   private Combo                 _comboOutlineColorMode_HovSel;
   private Combo                 _comboOutlineColorMode_Selected;

   private Label                 _lblAltitudeOffsetAbsoluteUnit;
   private Label                 _lblAltitudeOffsetRelativeUnit;
   private Label                 _lblConfigName;
   private Label                 _lblInteriorColor;
   private Label                 _lblInteriorColor_Hovered;
   private Label                 _lblInteriorColor_HovSel;
   private Label                 _lblInteriorColor_Selected;
   private Label                 _lblOutlineColor;
   private Label                 _lblOutlineColor_HovSel;
   private Label                 _lblOutlineColor_Selected;
   private Label                 _lblTrackColor;
   private Label                 _lblTrackPositionThreshold;
   private Label                 _lblTrackPositionThresholdAbsolute;

   private Button                _rdoOffsetAbsolute;
   private Button                _rdoOffsetRelative;

   private Spinner               _spinnerAltitudeOffsetAbsolute;
   private Spinner               _spinnerAltitudeOffsetRelative;
   private Spinner               _spinnerDirectionArrowDistance;
   private Spinner               _spinnerDirectionArrowSize;
   private Spinner               _spinnerInteriorOpacity;
   private Spinner               _spinnerInteriorOpacity_Hovered;
   private Spinner               _spinnerInteriorOpacity_HovSel;
   private Spinner               _spinnerInteriorOpacity_Selected;
   private Spinner               _spinnerOutlineOpacity;
   private Spinner               _spinnerOutlineOpacity_Hovered;
   private Spinner               _spinnerOutlineOpacity_HovSel;
   private Spinner               _spinnerOutlineOpacity_Selected;
   private Spinner               _spinnerOutlineWidth;
   private Spinner               _spinnerTrackColorOpacity;
   private Spinner               _spinnerTrackPositionSize;
   private Spinner               _spinnerTrackPositionSize_Hovered;
   private Spinner               _spinnerTrackPositionSize_Selected;
   private Spinner               _spinnerTrackPositionSize_HovSel;
   private Spinner               _spinnerTrackPositionThreshold;

   private Text                  _textConfigName;

   private final class WaitTimer implements Runnable {
      @Override
      public void run() {
         open_Runnable();
      }
   }

   public SlideoutMap3TourTrackConfig(final Control ownerControl, final ToolBar toolBar, final Map3View map3View) {

      super(ownerControl);

      _map3View = map3View;

      addListener(toolBar);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onModifyConfig());

      _defaultMouseWheelListener = mouseEvent -> {
         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onModifyConfig();
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };

      setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
      setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
      setIsKeepShellOpenWhenMoved(false);
      setFadeInSteps(1);
      setFadeOutSteps(20);
   }

   private void addListener(final ToolBar toolBar) {

      toolBar.addMouseTrackListener(mouseExitAdapter(mouseEvent -> {

         // prevent to open the tooltip
         _canOpenToolTip = false;
      }));
   }

   @Override
   protected boolean canShowToolTip() {

      initBeforeDialogIsOpened();

      return true;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {
      setIsAnotherDialogOpened(isDialogOpened);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      final Composite container = createUI(parent);

      updateUI_Initial();

      restoreState();

      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

//		_pc = new PixelConverter(parent);

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
            createUI_000_ConfigurationName(container);
            createUI_100_Line(container);
            createUI_110_DirectionArrow(container);
            createUI_200_TrackPosition(container);
            createUI_250_TrackColor(container);
            createUI_300_Outline(container);
            createUI_400_Interior(container);
            createUI_500_Altitude(container);
            createUI_999_Name(container);
         }
      }

      return _shellContainer;
   }

   private void createUI_000_ConfigurationName(final Composite parent) {

      /*
       * Label: Title
       */
      final Label title = new Label(parent, SWT.LEAD);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(title);
      MTFont.setBannerFont(title);

      title.setText(Messages.TourTrack_Properties_Label_ConfigName);
      title.setToolTipText(Messages.TourTrack_Properties_Label_ConfigName_Tooltip);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {

         /*
          * Combo: Configuration
          */
         _comboName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboName.setVisibleItemCount(20);
         _comboName.addFocusListener(_keepOpenListener);
         _comboName.addSelectionListener(widgetSelectedAdapter(this::onSelectConfig));
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.BEGINNING, SWT.CENTER)
// this is too small in linux
//					.hint(_pc.convertHorizontalDLUsToPixels(15 * 4), SWT.DEFAULT)
               .applyTo(_comboName);

         /*
          * Button: Reset
          */
         _btnReset = new Button(container, SWT.PUSH);
         _btnReset.setText(Messages.TourTrack_Properties_Button_Default);
         _btnReset.setToolTipText(Messages.TourTrack_Properties_Button_Default_Tooltip);
         _btnReset.addSelectionListener(widgetSelectedAdapter(this::onSelectDefaultConfig));
         GridDataFactory.fillDefaults()
               .align(SWT.END, SWT.CENTER)
               .applyTo(_btnReset);
      }
   }

   private void createUI_100_Line(final Composite parent) {

      {
         /*
          * label: Line width
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.TourTrack_Properties_Label_OutlineWidth);
         label.setToolTipText(Messages.TourTrack_Properties_Label_OutlineWidth_Tooltip);
         UI.gridLayoutData_AlignFillCenter().applyTo(label);

         /*
          * Spinner: Line width
          */
         _spinnerOutlineWidth = new Spinner(parent, SWT.BORDER);
         _spinnerOutlineWidth.setMinimum(TourTrackConfigManager.OUTLINE_WIDTH_MIN);
         _spinnerOutlineWidth.setMaximum(TourTrackConfigManager.OUTLINE_WIDTH_MAX);
         _spinnerOutlineWidth.setIncrement(1);
         _spinnerOutlineWidth.setPageIncrement(10);
         _spinnerOutlineWidth.addSelectionListener(_defaultSelectionListener);
         _spinnerOutlineWidth.addMouseWheelListener(_defaultMouseWheelListener);
         UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerOutlineWidth);
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
         UI.gridLayoutData_AlignFillCenter().applyTo(label);

         label.setText(Messages.TourTrack_Properties_Label_DirectionArrow);
         label.setToolTipText(Messages.TourTrack_Properties_Label_DirectionArrow_Tooltip);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            /*
             * Size
             */
            _spinnerDirectionArrowSize = new Spinner(container, SWT.BORDER);
            _spinnerDirectionArrowSize.setMinimum(TourTrackConfigManager.DIRECTION_ARROW_SIZE_MIN);
            _spinnerDirectionArrowSize.setMaximum(TourTrackConfigManager.DIRECTION_ARROW_SIZE_MAX);
            _spinnerDirectionArrowSize.setIncrement(10);
            _spinnerDirectionArrowSize.setPageIncrement(50);
            _spinnerDirectionArrowSize.addSelectionListener(_defaultSelectionListener);
            _spinnerDirectionArrowSize.addMouseWheelListener(_defaultMouseWheelListener);
            UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerDirectionArrowSize);

            /*
             * Vertical distance
             */
            _spinnerDirectionArrowDistance = new Spinner(container, SWT.BORDER);
            _spinnerDirectionArrowDistance.setMinimum(TourTrackConfigManager.DIRECTION_ARROW_VERTICAL_DISTANCE_MIN);
            _spinnerDirectionArrowDistance.setMaximum(TourTrackConfigManager.DIRECTION_ARROW_VERTICAL_DISTANCE_MAX);
            _spinnerDirectionArrowDistance.setIncrement(1);
            _spinnerDirectionArrowDistance.setPageIncrement(5);
            _spinnerDirectionArrowDistance.addSelectionListener(_defaultSelectionListener);
            _spinnerDirectionArrowDistance.addMouseWheelListener(_defaultMouseWheelListener);
            UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerDirectionArrowDistance);
         }
      }
   }

   private void createUI_200_TrackPosition(final Composite parent) {

      /*
       * checkbox: Show track positions
       */
      _chkTrackPositions = new Button(parent, SWT.CHECK);
      _chkTrackPositions.setText(Messages.TourTrack_Properties_Checkbox_ShowTrackPositions);
      _chkTrackPositions.setToolTipText(Messages.TourTrack_Properties_Checkbox_ShowTrackPositions_Tooltip);
      _chkTrackPositions.addSelectionListener(_defaultSelectionListener);
      UI.gridLayoutData_Span_2_1().applyTo(_chkTrackPositions);
      {
         /*
          * label: Track position threshold
          */
         _lblTrackPositionThreshold = new Label(parent, SWT.NONE);
         _lblTrackPositionThreshold.setText(Messages.TourTrack_Properties_Label_TrackPositionThreshold);
         _lblTrackPositionThreshold.setToolTipText(Messages.TourTrack_Properties_Label_TrackPositionThreshold_Tooltip);
         UI.gridLayoutData_AlignFillCenter()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_lblTrackPositionThreshold);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            /*
             * Spinner: Track position threshold
             */
            _spinnerTrackPositionThreshold = new Spinner(container, SWT.BORDER);
            _spinnerTrackPositionThreshold.setMinimum(TourTrackConfigManager.TRACK_POSITION_THRESHOLD_MIN);
            _spinnerTrackPositionThreshold.setMaximum(TourTrackConfigManager.TRACK_POSITION_THRESHOLD_MAX);
            _spinnerTrackPositionThreshold.setIncrement(1);
            _spinnerTrackPositionThreshold.setPageIncrement(10);
            _spinnerTrackPositionThreshold.addSelectionListener(_defaultSelectionListener);
            _spinnerTrackPositionThreshold.addMouseWheelListener(_defaultMouseWheelListener);
            UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerTrackPositionThreshold);

            /*
             * Label: eye distance
             */
            _lblTrackPositionThresholdAbsolute = new Label(container, SWT.NONE);
            _lblTrackPositionThresholdAbsolute.setText(UI.EMPTY_STRING);
         }
      }
   }

   private void createUI_250_TrackColor(final Composite parent) {

      /*
       * Track color
       */
      {
         _lblTrackColor = new Label(parent, SWT.NONE);
         _lblTrackColor.setText(Messages.TourTrack_Properties_Label_TrackColor);
         _lblTrackColor.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_TrackColor_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter().applyTo(_lblTrackColor);

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            /*
             * Button: Track color
             */
            _btnTrackColor = new Button(container, SWT.PUSH);
            _btnTrackColor.setImage(net.tourbook.ui.UI.getGraphImage(MapGraphId.Altitude));
            _btnTrackColor.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectTrackColor()));

            _spinnerTrackColorOpacity = createUI_Spinner_ColorOpacity(container);
         }
      }
   }

   private void createUI_300_Outline(final Composite parent) {

      /*
       * Normal color
       */
      {
         _lblOutlineColor = new Label(parent, SWT.NONE);
         _lblOutlineColor.setText(Messages.TourTrack_Properties_Label_OutlineColor);
         _lblOutlineColor.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_OutlineColor_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter().applyTo(_lblOutlineColor);

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
         _lblOutlineColor_Selected.setText(Messages.TourTrack_Properties_Label_OutlineColorSelected);
         _lblOutlineColor_Selected.setToolTipText(NLS.bind(
               Messages.TourTrack_Properties_Label_OutlineColorSelected_Tooltip,
               UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter().applyTo(_lblOutlineColor_Selected);

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
         label.setText(Messages.TourTrack_Properties_Label_OutlineColorHovered);
         label.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_OutlineColorHovered_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter().applyTo(label);

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
         UI.gridLayoutData_AlignFillCenter().applyTo(_lblOutlineColor_HovSel);

         _lblOutlineColor_HovSel.setText(Messages.TourTrack_Properties_Label_OutlineColorHovSel);
         _lblOutlineColor_HovSel.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_OutlineColorHovSel_Tooltip, UI.TRANSFORM_OPACITY_MAX));

         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
         {
            _comboOutlineColorMode_HovSel = createUI_Combo_ColorMode(container);
            _colorOutlineColor_HovSel = createUI_ColorSelector(container);
            _spinnerOutlineOpacity_HovSel = createUI_Spinner_ColorOpacity(container);
            _spinnerTrackPositionSize_HovSel = createUI_Spinner_PositionSize(container);
         }
      }
   }

   private void createUI_400_Interior(final Composite container) {

      /*
       * Extrude path
       */
      {
         _chkShowInterior = new Button(container, SWT.CHECK);
         _chkShowInterior.setText(Messages.TourTrack_Properties_Checkbox_ExtrudePath);
         _chkShowInterior.setToolTipText(Messages.TourTrack_Properties_Checkbox_ExtrudePath_Tooltip);
         _chkShowInterior.addSelectionListener(_defaultSelectionListener);
         UI.gridLayoutData_Span_2_1().applyTo(_chkShowInterior);

         createUI_410__Interior(container);
         createUI_420__Verticals(container);
      }
   }

   private void createUI_410__Interior(final Composite parent) {

      /*
       * Curtain/Interior color
       */
      {
         _lblInteriorColor = new Label(parent, SWT.NONE);
         _lblInteriorColor.setText(Messages.TourTrack_Properties_Label_CurtainColor);
         _lblInteriorColor.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_CurtainColor_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_lblInteriorColor);

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
         _lblInteriorColor_Selected.setText(Messages.TourTrack_Properties_Label_CurtainColorSelected);
         _lblInteriorColor_Selected.setToolTipText(NLS.bind(
               Messages.TourTrack_Properties_Label_CurtainColorSelected_Tooltip,
               UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_lblInteriorColor_Selected);

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
         _lblInteriorColor_Hovered.setText(Messages.TourTrack_Properties_Label_CurtainColorHovered);
         _lblInteriorColor_Hovered.setToolTipText(NLS.bind(
               Messages.TourTrack_Properties_Label_CurtainColorHovered_Tooltip,
               UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_lblInteriorColor_Hovered);

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
         _lblInteriorColor_HovSel.setText(Messages.TourTrack_Properties_Label_CurtainColorHovSel);
         _lblInteriorColor_HovSel.setToolTipText(NLS.bind(Messages.TourTrack_Properties_Label_CurtainColorHovSel_Tooltip, UI.TRANSFORM_OPACITY_MAX));
         UI.gridLayoutData_AlignFillCenter()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_lblInteriorColor_HovSel);

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

   private void createUI_420__Verticals(final Composite parent) {

      /*
       * Checkbox: Draw verticals for the extruded path
       */
      _chkDrawVerticals = new Button(parent, SWT.CHECK);
      _chkDrawVerticals.setText(Messages.TourTrack_Properties_Checkbox_DrawVerticals);
      _chkDrawVerticals.setToolTipText(Messages.TourTrack_Properties_Checkbox_DrawVerticals_Tooltip);
      _chkDrawVerticals.addSelectionListener(_defaultSelectionListener);
      UI.gridLayoutData_Span_2_1()
            .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
            .applyTo(_chkDrawVerticals);
   }

   private void createUI_500_Altitude(final Composite parent) {

      {
         /*
          * label: Altitude mode
          */
         final Label label = new Label(parent, SWT.NONE);
         UI.gridLayoutData_AlignFillCenter().applyTo(label);

         label.setText(Messages.TourTrack_Properties_Label_Altitude);
         label.setToolTipText(Messages.TourTrack_Properties_Label_Altitude_Tooltip);

         /*
          * combo: Altitude
          */
         _comboAltitude = new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
         _comboAltitude.setVisibleItemCount(10);
         _comboAltitude.addSelectionListener(_defaultSelectionListener);
         _comboAltitude.addFocusListener(_keepOpenListener);
         UI.gridLayoutData_AlignBeginningFill().applyTo(_comboAltitude);
      }

      {
         /*
          * checkbox: Altitude offset
          */
         _chkAltitudeOffset = new Button(parent, SWT.CHECK);
         _chkAltitudeOffset.setText(Messages.TourTrack_Properties_Checkbox_AltitudeOffset);
         _chkAltitudeOffset.setToolTipText(Messages.TourTrack_Properties_Checkbox_AltitudeOffset_Tooltip);
         _chkAltitudeOffset.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .indent(UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_chkAltitudeOffset);

         /*
          * Checkbox: Random
          */
         _chkAltitudeOffsetRandom = new Button(parent, SWT.CHECK);
         _chkAltitudeOffsetRandom.setText(Messages.TourTrack_Properties_Checkbox_AltitudeOffsetRandom);
         _chkAltitudeOffsetRandom.setToolTipText(Messages.TourTrack_Properties_Checkbox_AltitudeOffsetRandom_Tooltip);
         _chkAltitudeOffsetRandom.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults().applyTo(_chkAltitudeOffsetRandom);

      }

      {
         /*
          * Radio: Absolute
          */
         _rdoOffsetAbsolute = new Button(parent, SWT.RADIO);
         _rdoOffsetAbsolute.setText(Messages.TourTrack_Properties_Radio_AltitudeOffsetAbsolute);
         _rdoOffsetAbsolute.setToolTipText(Messages.TourTrack_Properties_Radio_AltitudeOffsetAbsolute_Tooltip);
         _rdoOffsetAbsolute.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_rdoOffsetAbsolute);

         final Composite containerOffsetAbsolute = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerOffsetAbsolute);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerOffsetAbsolute);
         {
            /*
             * Spinner: Altitude offset
             */
            _spinnerAltitudeOffsetAbsolute = new Spinner(containerOffsetAbsolute, SWT.BORDER);
            _spinnerAltitudeOffsetAbsolute.setMinimum(TourTrackConfigManager.ALTITUDE_OFFSET_ABSOLUTE_MIN);
            _spinnerAltitudeOffsetAbsolute.setMaximum(TourTrackConfigManager.ALTITUDE_OFFSET_ABSOLUTE_MAX);
            _spinnerAltitudeOffsetAbsolute.setIncrement(1);
            _spinnerAltitudeOffsetAbsolute.setPageIncrement(10);
            _spinnerAltitudeOffsetAbsolute.addSelectionListener(_defaultSelectionListener);
            _spinnerAltitudeOffsetAbsolute.addMouseWheelListener(_defaultMouseWheelListener);
            UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerAltitudeOffsetAbsolute);

            /*
             * Label: m (meter/feet)
             */
            _lblAltitudeOffsetAbsoluteUnit = new Label(containerOffsetAbsolute, SWT.NONE);
            _lblAltitudeOffsetAbsoluteUnit.setText(UI.UNIT_LABEL_ELEVATION);
         }
      }

      {
         /*
          * Radio: Relative
          */
         _rdoOffsetRelative = new Button(parent, SWT.RADIO);
         _rdoOffsetRelative.setText(Messages.TourTrack_Properties_Radio_AltitudeOffsetRelative);
         _rdoOffsetRelative.setToolTipText(Messages.TourTrack_Properties_Radio_AltitudeOffsetRelative_Tooltip);
         _rdoOffsetRelative.addSelectionListener(_defaultSelectionListener);
         GridDataFactory.fillDefaults()
               .indent(2 * UI.FORM_FIRST_COLUMN_INDENT, 0)
               .applyTo(_rdoOffsetRelative);

         final Composite containerOffsetRelative = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerOffsetRelative);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerOffsetRelative);
         {
            /*
             * Spinner: Altitude offset relative
             */
            _spinnerAltitudeOffsetRelative = new Spinner(containerOffsetRelative, SWT.BORDER);
            _spinnerAltitudeOffsetRelative.setMinimum(TourTrackConfigManager.ALTITUDE_OFFSET_RELATIVE_MIN);
            _spinnerAltitudeOffsetRelative.setMaximum(TourTrackConfigManager.ALTITUDE_OFFSET_RELATIVE_MAX);
            _spinnerAltitudeOffsetRelative.setIncrement(1);
            _spinnerAltitudeOffsetRelative.setPageIncrement(10);
            _spinnerAltitudeOffsetRelative.addSelectionListener(_defaultSelectionListener);
            _spinnerAltitudeOffsetRelative.addMouseWheelListener(_defaultMouseWheelListener);
            UI.gridLayoutData_AlignBeginningFill().applyTo(_spinnerAltitudeOffsetRelative);

            /*
             * Label: %
             */
            _lblAltitudeOffsetRelativeUnit = new Label(containerOffsetRelative, SWT.NONE);
            _lblAltitudeOffsetRelativeUnit.setText(UI.SYMBOL_PERCENTAGE);
         }
      }

      {
         /*
          * checkbox: Follow terrain
          */
         _chkFollowTerrain = new Button(parent, SWT.CHECK);
         _chkFollowTerrain.setText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain);
         _chkFollowTerrain.setToolTipText(Messages.TourTrack_Properties_Checkbox_IsFollowTerrain_Tooltip);
         _chkFollowTerrain.addSelectionListener(_defaultSelectionListener);
         UI.gridLayoutData_Span_2_1().applyTo(_chkFollowTerrain);
      }
   }

   private void createUI_999_Name(final Composite parent) {

      /*
       * Name
       */
      {
         /*
          * Label
          */
         _lblConfigName = new Label(parent, SWT.NONE);
         _lblConfigName.setText(Messages.TourTrack_Properties_Label_Name);
         UI.gridLayoutData_AlignFillCenter().applyTo(_lblConfigName);

         /*
          * Text
          */
         _textConfigName = new Text(parent, SWT.BORDER);
         _textConfigName.addModifyListener(modifyEvent -> onModifyName());
         UI.gridLayoutData_AlignFillCenter().applyTo(_textConfigName);
      }
   }

   private ColorSelectorExtended createUI_ColorSelector(final Composite parent) {

      final ColorSelectorExtended colorSelector = new ColorSelectorExtended(parent);

      colorSelector.addOpenListener(this);
      colorSelector.addListener(propertyChangeEvent -> onModifyConfig());

      GridDataFactory.swtDefaults()
            .grab(false, true)
            .align(SWT.BEGINNING, SWT.BEGINNING)
            .applyTo(colorSelector.getButton());

      return colorSelector;
   }

   private Combo createUI_Combo_ColorMode(final Composite container) {

      final Combo combo = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
      combo.setVisibleItemCount(10);
      combo.addSelectionListener(_defaultSelectionListener);
      combo.addFocusListener(_keepOpenListener);
      UI.gridLayoutData_AlignBeginningFill().applyTo(combo);

      return combo;
   }

   private Spinner createUI_Spinner_ColorOpacity(final Composite parent) {

      final Spinner spinnerOpacity = new Spinner(parent, SWT.BORDER);

      spinnerOpacity.setMinimum(0);
      spinnerOpacity.setMaximum(UI.TRANSFORM_OPACITY_MAX);
      spinnerOpacity.setIncrement(1);
      spinnerOpacity.setPageIncrement(10);
      spinnerOpacity.addSelectionListener(_defaultSelectionListener);
      spinnerOpacity.addMouseWheelListener(_defaultMouseWheelListener);

      UI.gridLayoutData_AlignBeginningFill().applyTo(spinnerOpacity);

      return spinnerOpacity;
   }

   private Spinner createUI_Spinner_PositionSize(final Composite container) {

      final Spinner spinnerPositionSize = new Spinner(container, SWT.BORDER);

      spinnerPositionSize.setMinimum(TourTrackConfigManager.TRACK_POSITION_SIZE_MIN);
      spinnerPositionSize.setMaximum(TourTrackConfigManager.TRACK_POSITION_SIZE_MAX);
      spinnerPositionSize.setIncrement(1);
      spinnerPositionSize.setPageIncrement(50);
      spinnerPositionSize.addSelectionListener(_defaultSelectionListener);
      spinnerPositionSize.addMouseWheelListener(_defaultMouseWheelListener);

      UI.gridLayoutData_AlignBeginningFill().applyTo(spinnerPositionSize);

      return spinnerPositionSize;
   }

   private void enableControls() {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      final boolean isAbsoluteAltitudeMode = config.altitudeMode == WorldWind.ABSOLUTE;
      final boolean isAltitudeOffset = _chkAltitudeOffset.getSelection();
      final boolean isAbsoluteAltitudeEnabled = isAltitudeOffset && isAbsoluteAltitudeMode;
      final boolean isClampToGround = config.altitudeMode == WorldWind.CLAMP_TO_GROUND;
      final boolean isShowCurtain = isClampToGround == false && config.isShowInterior;
      final boolean isTrackPositionVisible = config.outlineWidth > 0.0;
      final boolean isShowTrackPosition = config.isShowTrackPosition && isTrackPositionVisible;
      final boolean isOffsetModeAbsolute = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;
      final boolean isOffsetModeRelative = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE;

      final boolean isOutlineSolidColor = config.outlineColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isOutlineSolidColor_Hovered = config.outlineColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isOutlineSolidColor_HovSel = config.outlineColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isOutlineSolidColor_Selected = config.outlineColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

      // vertical lines are painted with the outline color
      final boolean isOutLineColor = isOutlineSolidColor || config.isDrawVerticals;

      final boolean isInteriorSolidColor = isShowCurtain && config.interiorColorMode == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isInteriorSolidColor_Hovered = isShowCurtain && config.interiorColorMode_Hovered == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isInteriorSolidColor_HovSel = isShowCurtain && config.interiorColorMode_HovSel == TourTrackConfig.COLOR_MODE_SOLID_COLOR;
      final boolean isInteriorSolidColor_Selected = isShowCurtain && config.interiorColorMode_Selected == TourTrackConfig.COLOR_MODE_SOLID_COLOR;

      // Hr zones are not yet supported
      final boolean isGradientColor = _trackColorId != MapGraphId.HrZone;

      // altitude
      _chkAltitudeOffset.setEnabled(isAbsoluteAltitudeMode);
      _chkAltitudeOffsetRandom.setEnabled(isAbsoluteAltitudeMode && isAltitudeOffset);
      _rdoOffsetAbsolute.setEnabled(isAbsoluteAltitudeEnabled);
      _rdoOffsetRelative.setEnabled(isAbsoluteAltitudeEnabled);
      _spinnerAltitudeOffsetAbsolute.setEnabled(isAbsoluteAltitudeEnabled && isOffsetModeAbsolute);
      _spinnerAltitudeOffsetRelative.setEnabled(isAbsoluteAltitudeEnabled && isOffsetModeRelative);
      _lblAltitudeOffsetAbsoluteUnit.setEnabled(isAbsoluteAltitudeEnabled && isOffsetModeAbsolute);
      _lblAltitudeOffsetRelativeUnit.setEnabled(isAbsoluteAltitudeEnabled && isOffsetModeRelative);

      // track position
      _chkTrackPositions.setEnabled(isTrackPositionVisible);
      _lblTrackPositionThreshold.setEnabled(isShowTrackPosition);
      _lblTrackPositionThresholdAbsolute.setEnabled(isShowTrackPosition);

      _spinnerTrackPositionSize.setEnabled(isShowTrackPosition);
      _spinnerTrackPositionSize_Hovered.setEnabled(isShowTrackPosition);
      _spinnerTrackPositionSize_Selected.setEnabled(isShowTrackPosition);
      _spinnerTrackPositionSize_HovSel.setEnabled(isShowTrackPosition);
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

   private void initBeforeDialogIsOpened() {

      _trackColorId = _map3View.getTrackColorId();

      _btnTrackColor.setImage(net.tourbook.ui.UI.getGraphImage(_trackColorId));

      enableControls();
   }

   @Override
   protected Rectangle noHideOnMouseMove() {

      return _toolTipItemBounds;
   }

   private void onModifyConfig() {

      saveStateWithRecreateCheck();

      updateUI();

      enableControls();

      Map3Manager.getLayer_TourTrack().onModifyConfig();

      // update sliders
      updateUI_Map3();
   }

   private void onModifyName() {

      if (_isUpdateUI) {
         return;
      }

      // update text in the combo
      final int selectedIndex = _comboName.getSelectionIndex();

      _comboName.setItem(selectedIndex, _textConfigName.getText());

      saveState();
   }

   private void onSelectConfig(final SelectionEvent selectionEvent) {

      final int selectedIndex = _comboName.getSelectionIndex();
      final List<TourTrackConfig> allConfigurations = TourTrackConfigManager.getAllConfigurations();

      final TourTrackConfig selectedConfig = allConfigurations.get(selectedIndex);
      final TourTrackConfig trackConfig = TourTrackConfigManager.getActiveConfig();

      if (selectedConfig == trackConfig) {

         // config has not changed
         return;
      }

      // keep data from previous config
      saveState();

      final TourTrackConfig previousConfig = (TourTrackConfig) TourTrackConfigManager.getActiveConfig().clone();

      TourTrackConfigManager.setActiveConfig(selectedConfig);

      updateUI_SetActiveConfig(previousConfig);
   }

   private void onSelectDefaultConfig(final SelectionEvent selectionEvent) {

      TourTrackConfig previousConfig = null;

      if (Util.isCtrlKeyPressed(selectionEvent)) {

         // reset All configurations

         TourTrackConfigManager.resetAllConfigurations();

         updateUI_ComboConfigName(true);

      } else {

         // reset active config

         previousConfig = (TourTrackConfig) TourTrackConfigManager.getActiveConfig().clone();

         TourTrackConfigManager.resetActiveConfig();
      }

      updateUI_SetActiveConfig(previousConfig);
   }

   private void onSelectTrackColor() {

      setIsAnotherDialogOpened(true);

      _map3View.actionOpenTrackColorDialog();

      setIsAnotherDialogOpened(false);
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

// SET_FORMATTING_OFF

   /**
    * Restores state values from the tour track configuration and update the UI.
    */
   public void restoreState() {

      _isUpdateUI = true;

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      // get active config AFTER getting the index because this could change the active config
      final int activeConfigIndex = TourTrackConfigManager.getActiveConfigIndex();

      final boolean isOffsetAbsolute = config.altitudeOffsetMode == TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;

      _comboName.select(activeConfigIndex);
      _textConfigName.setText(config.name);

      // track
      _spinnerDirectionArrowDistance   .setSelection((int) (config.directionArrowDistance));
      _spinnerDirectionArrowSize       .setSelection((int) (config.directionArrowSize));
      _spinnerTrackColorOpacity        .setSelection((int) (config.trackColorOpacity * UI.TRANSFORM_OPACITY_MAX));

      // line color
      _spinnerOutlineWidth             .setSelection((int) (config.outlineWidth));

      _comboOutlineColorMode           .select(config.getColorModeIndex(config.outlineColorMode));
      _comboOutlineColorMode_Hovered   .select(config.getColorModeIndex(config.outlineColorMode_Hovered));
      _comboOutlineColorMode_HovSel    .select(config.getColorModeIndex(config.outlineColorMode_HovSel));
      _comboOutlineColorMode_Selected  .select(config.getColorModeIndex(config.outlineColorMode_Selected));

      _colorOutlineColor               .setColorValue(config.outlineColor);
      _colorOutlineColor_Hovered       .setColorValue(config.outlineColor_Hovered);
      _colorOutlineColor_HovSel        .setColorValue(config.outlineColor_HovSel);
      _colorOutlineColor_Selected      .setColorValue(config.outlineColor_Selected);

      _spinnerOutlineOpacity           .setSelection((int) (config.outlineOpacity            * UI.TRANSFORM_OPACITY_MAX));
      _spinnerOutlineOpacity_Hovered   .setSelection((int) (config.outlineOpacity_Hovered    * UI.TRANSFORM_OPACITY_MAX));
      _spinnerOutlineOpacity_HovSel    .setSelection((int) (config.outlineOpacity_HovSel     * UI.TRANSFORM_OPACITY_MAX));
      _spinnerOutlineOpacity_Selected  .setSelection((int) (config.outlineOpacity_Selected   * UI.TRANSFORM_OPACITY_MAX));

      // curtain color
      _chkShowInterior                 .setSelection(config.isShowInterior);

      _comboInteriorColorMode          .select(config.getColorModeIndex(config.interiorColorMode));
      _comboInteriorColorMode_Hovered  .select(config.getColorModeIndex(config.interiorColorMode_Hovered));
      _comboInteriorColorMode_HovSel   .select(config.getColorModeIndex(config.interiorColorMode_HovSel));
      _comboInteriorColorMode_Selected .select(config.getColorModeIndex(config.interiorColorMode_Selected));

      _colorInteriorColor              .setColorValue(config.interiorColor);
      _colorInteriorColor_Hovered      .setColorValue(config.interiorColor_Hovered);
      _colorInteriorColor_HovSel       .setColorValue(config.interiorColor_HovSel);
      _colorInteriorColor_Selected     .setColorValue(config.interiorColor_Selected);

      _spinnerInteriorOpacity          .setSelection((int) (config.interiorOpacity           * UI.TRANSFORM_OPACITY_MAX));
      _spinnerInteriorOpacity_Hovered  .setSelection((int) (config.interiorOpacity_Hovered   * UI.TRANSFORM_OPACITY_MAX));
      _spinnerInteriorOpacity_HovSel   .setSelection((int) (config.interiorOpacity_HovSel    * UI.TRANSFORM_OPACITY_MAX));
      _spinnerInteriorOpacity_Selected .setSelection((int) (config.interiorOpacity_Selected  * UI.TRANSFORM_OPACITY_MAX));

      // verticals
      _chkDrawVerticals                .setSelection(config.isDrawVerticals);

      // track position
      _chkTrackPositions                  .setSelection(config.isShowTrackPosition);
      _spinnerTrackPositionSize           .setSelection((int) (config.trackPositionSize));
      _spinnerTrackPositionSize_Hovered   .setSelection((int) (config.trackPositionSize_Hovered));
      _spinnerTrackPositionSize_Selected  .setSelection((int) (config.trackPositionSize_Selected));
      _spinnerTrackPositionSize_HovSel    .setSelection((int) (config.trackPositionSize_HovSel));
      _spinnerTrackPositionThreshold      .setSelection(config.trackPositionThreshold);

      // altitude
      _comboAltitude                      .select(config.getAltitudeModeIndex());
      _chkAltitudeOffset                  .setSelection(config.isAltitudeOffset);
      _chkAltitudeOffsetRandom            .setSelection(config.isAltitudeOffsetRandom);
      _rdoOffsetAbsolute                  .setSelection(isOffsetAbsolute);
      _rdoOffsetRelative                  .setSelection(!isOffsetAbsolute);
      _spinnerAltitudeOffsetAbsolute      .setSelection((int) (config.altitudeOffsetDistanceAbsolute / UI.UNIT_VALUE_ELEVATION));
      _spinnerAltitudeOffsetRelative      .setSelection(config.altitudeOffsetDistanceRelative);
      _chkFollowTerrain                   .setSelection(config.isFollowTerrain);

      updateUI();

      _isUpdateUI = false;
   }

   private void saveState() {

		final int altitudeOffsetMetric = (int) (_spinnerAltitudeOffsetAbsolute.getSelection() * UI.UNIT_VALUE_ELEVATION);

		// update config

		final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

		config.name = _textConfigName.getText();

		// track
		config.directionArrowSize               = _spinnerDirectionArrowSize.getSelection();
		config.directionArrowDistance           = _spinnerDirectionArrowDistance.getSelection();
		config.trackColorOpacity                = (float) _spinnerTrackColorOpacity.getSelection() / UI.TRANSFORM_OPACITY_MAX;

		// line
		config.outlineWidth                     = _spinnerOutlineWidth.getSelection();
		config.outlineColorMode                 = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode)].value;
		config.outlineColorMode_Hovered         = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Hovered)].value;
		config.outlineColorMode_HovSel          = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_HovSel)].value;
		config.outlineColorMode_Selected        = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboOutlineColorMode_Selected)].value;
		config.outlineColor                     = _colorOutlineColor.getColorValue();
		config.outlineColor_Hovered             = _colorOutlineColor_Hovered.getColorValue();
		config.outlineColor_HovSel              = _colorOutlineColor_HovSel.getColorValue();
		config.outlineColor_Selected            = _colorOutlineColor_Selected.getColorValue();

		config.outlineOpacity                   = (float) _spinnerOutlineOpacity.getSelection()            / UI.TRANSFORM_OPACITY_MAX;
		config.outlineOpacity_Hovered           = (float) _spinnerOutlineOpacity_Hovered.getSelection()    / UI.TRANSFORM_OPACITY_MAX;
		config.outlineOpacity_HovSel            = (float) _spinnerOutlineOpacity_HovSel.getSelection()     / UI.TRANSFORM_OPACITY_MAX;
		config.outlineOpacity_Selected          = (float) _spinnerOutlineOpacity_Selected.getSelection()   / UI.TRANSFORM_OPACITY_MAX;

		// interior
		config.isShowInterior                   = _chkShowInterior.getSelection();
		config.interiorColorMode                = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode)].value;
		config.interiorColorMode_Hovered        = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Hovered)].value;
		config.interiorColorMode_HovSel         = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_HovSel)].value;
		config.interiorColorMode_Selected       = TourTrackConfig.TRACK_COLOR_MODE[getComboIndex(_comboInteriorColorMode_Selected)].value;
		config.interiorColor                    = _colorInteriorColor.getColorValue();
		config.interiorColor_Hovered            = _colorInteriorColor_Hovered.getColorValue();
		config.interiorColor_HovSel             = _colorInteriorColor_HovSel.getColorValue();
		config.interiorColor_Selected           = _colorInteriorColor_Selected.getColorValue();

		config.interiorOpacity                  = (float) _spinnerInteriorOpacity.getSelection()           / UI.TRANSFORM_OPACITY_MAX;
		config.interiorOpacity_Hovered          = (float) _spinnerInteriorOpacity_Hovered.getSelection()   / UI.TRANSFORM_OPACITY_MAX;
		config.interiorOpacity_HovSel           = (float) _spinnerInteriorOpacity_HovSel.getSelection()    / UI.TRANSFORM_OPACITY_MAX;
		config.interiorOpacity_Selected         = (float) _spinnerInteriorOpacity_Selected.getSelection()  / UI.TRANSFORM_OPACITY_MAX;

		// verticals
		config.isDrawVerticals                  = _chkDrawVerticals.getSelection();

		// track position
		config.isShowTrackPosition              = _chkTrackPositions.getSelection();
		config.trackPositionSize                = _spinnerTrackPositionSize.getSelection();
		config.trackPositionSize_Hovered        = _spinnerTrackPositionSize_Hovered.getSelection();
		config.trackPositionSize_Selected       = _spinnerTrackPositionSize_Selected.getSelection();
		config.trackPositionSize_HovSel         = _spinnerTrackPositionSize_HovSel.getSelection();
		config.trackPositionThreshold           = _spinnerTrackPositionThreshold.getSelection();

		// altitude
		config.isAltitudeOffset                 = _chkAltitudeOffset.getSelection();
		config.isAltitudeOffsetRandom           = _chkAltitudeOffsetRandom.getSelection();
		config.altitudeMode                     = TourTrackConfig.ALTITUDE_MODE[getComboIndex(_comboAltitude)].value;
		config.altitudeOffsetMode               = _rdoOffsetRelative.getSelection()
                                       				? TourTrackConfigManager.ALTITUDE_OFFSET_MODE_RELATIVE
                                       				: TourTrackConfigManager.ALTITUDE_OFFSET_MODE_ABSOLUTE;

		config.altitudeOffsetDistanceAbsolute   = altitudeOffsetMetric;
		config.altitudeOffsetDistanceRelative   = _spinnerAltitudeOffsetRelative.getSelection();
		config.isFollowTerrain                  = _chkFollowTerrain.getSelection();
   }

// SET_FORMATTING_ON

   /**
    * Saves state values from the UI in the tour track configuration.
    */
   private void saveStateWithRecreateCheck() {

      final TourTrackConfig activeConfig = TourTrackConfigManager.getActiveConfig();

      final TourTrackConfig clonedTrackConfig = (TourTrackConfig) activeConfig.clone();

      saveState();

      activeConfig.checkTrackRecreation(clonedTrackConfig);
   }

   public void updateMeasurementSystem() {

      if (_lblAltitudeOffsetAbsoluteUnit == null) {

         // dialog is not yet created

         return;
      }

      _lblAltitudeOffsetAbsoluteUnit.setText(UI.UNIT_LABEL_ELEVATION);
      _lblAltitudeOffsetAbsoluteUnit.getParent().layout();

      restoreState();
   }

   private void updateUI() {

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();

      // position threshold
      final double positionThreshold = Math.pow(10, config.trackPositionThreshold) / 1000;

      _lblTrackPositionThresholdAbsolute.setText(String.format(//
            FORMAT_POSITION_THRESHOLD,
            positionThreshold,
            UI.UNIT_LABEL_DISTANCE));

      _lblTrackPositionThresholdAbsolute.getParent().layout();

      _lblConfigName.setToolTipText(NLS.bind(//
            Messages.TourTrack_Properties_Label_Name_Tooltip,
            config.defaultId));
   }

   private void updateUI_ComboConfigName(final boolean isReplaceItems) {

      final boolean backupIsUpdateUI = _isUpdateUI;
      _isUpdateUI = true;
      {
         int backupNameIndex = 0;

         if (isReplaceItems) {
            backupNameIndex = _comboName.getSelectionIndex();
            _comboName.removeAll();
         }

         for (final TourTrackConfig config : TourTrackConfigManager.getAllConfigurations()) {
            _comboName.add(config.name);
         }

         if (isReplaceItems) {

            if (backupNameIndex < 0) {
               backupNameIndex = 0;
            }
            _comboName.select(backupNameIndex);
         }
      }
      _isUpdateUI = backupIsUpdateUI;
   }

   private void updateUI_FillCombo(final Combo combo) {

      // fill track color combo
      for (final ComboEntry colorMode : TourTrackConfig.TRACK_COLOR_MODE) {
         combo.add(colorMode.label);
      }
   }

   private void updateUI_Initial() {

      updateUI_ComboConfigName(false);

      // fill altitude mode combo
      for (final ComboEntry altiMode : TourTrackConfig.ALTITUDE_MODE) {
         _comboAltitude.add(altiMode.label);
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

   private void updateUI_Map3() {

      final Map3View map3View = Map3Manager.getMap3View();

      if (map3View != null) {
         map3View.onModifyConfig();
      }
   }

   private void updateUI_SetActiveConfig(final TourTrackConfig previousConfig) {

      restoreState();

      enableControls();

      final TourTrackConfig config = TourTrackConfigManager.getActiveConfig();
      config.checkTrackRecreation(previousConfig);

      Map3Manager.getLayer_TourTrack().onModifyConfig();

      updateUI_Map3();
   }

}
