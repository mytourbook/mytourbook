/*******************************************************************************
 * Copyright (C) 2022, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map.player;

import static org.eclipse.swt.events.KeyListener.keyPressedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.tooltip.ActionToolbarSlideoutAdv;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.tooltip.ICloseOpenedDialogs;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.MtMath;
import net.tourbook.map.MapManager;
import net.tourbook.map.model.SlideoutMapModel;
import net.tourbook.map25.Map25FPSManager;
import net.tourbook.map25.renderer.TourTrack_Bucket;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.MapPosition;
import org.oscim.renderer.GLViewport;

public class ModelPlayerView extends ViewPart implements ICloseOpenedDialogs {

   public static final String           ID     = "net.tourbook.map.player.ModelPlayerView"; //$NON-NLS-1$
   //
   private static final IDialogSettings _state = TourbookPlugin.getState(ID);
   //
// SET_FORMATTING_OFF

   private static final ImageDescriptor _imageDescriptor_Loop              = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Loop);
   private static final ImageDescriptor _imageDescriptor_Loop_Disabled     = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Loop_Disabled);
   private static final ImageDescriptor _imageDescriptor_Pause             = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Pause);
   private static final ImageDescriptor _imageDescriptor_Pause_Disabled    = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Pause_Disabled);
   private static final ImageDescriptor _imageDescriptor_Play              = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Play);
   private static final ImageDescriptor _imageDescriptor_Play_Disabled     = CommonActivator.getThemedImageDescriptor(CommonImages.PlayControl_Play_Disabled);

   // SET_FORMATTING_ON
   //
   private static final Color        JOG_WHEEL_COLOR_GREATER_0                           = new Color(26, 142, 26);
   private static final Color        JOG_WHEEL_COLOR_LESS_0                              = new Color(227, 64, 23);
   //
   static final int                  RELATIVE_MODEL_POSITION_ON_RETURN_PATH_START_TO_END = -1;
   static final int                  RELATIVE_MODEL_POSITION_ON_RETURN_PATH_END_TO_START = 2;
   //
   private IPartListener2            _partListener;
   //
   private Action                    _actionPlayControl_PlayAndPause;
   private Action                    _actionPlayControl_Loop;
   private Action_ShowMapModel       _actionShowMapModel;
   private Action_ShowMapModelCursor _actionShowMapModelCursor;
   private Action_SlideoutMapModel   _actionSlideoutMapModel;
   //
   private boolean                   _isInUpdateTimeline;
   //
   private int                       _currentTimelineMaxValue;
   private int                       _currentTimelineValue;
   //
   private OpenDialogManager         _openDialogManager                                  = new OpenDialogManager();
   //
   private PixelConverter            _pc;
   //
   /*
    * UI controls
    */
   private Display   _display;
   private Composite _parent;

   private Label     _lblModelCursorSize;
   private Label     _lblModelSize;
   private Label     _lblSpeedMultiplier;
   private Label     _lblSpeedJogWheel;
   private Label     _lblSpeedJogWheel_Value;
   private Label     _lblTimeline;
   private Label     _lblTimeline_Value;
   private Label     _lblTurningAngle;

   private Button    _chkIsRelivePlaying;

   private Scale     _scaleTimeline;
   private Scale     _scaleSpeedJogWheel;

   private Spinner   _spinnerModelCursorSize;
   private Spinner   _spinnerModelSize;
   private Spinner   _spinnerSpeedMultiplier;
   private Spinner   _spinnerTurningMultiplier;

   private class Action_PlayControl_Loop extends Action {

      Action_PlayControl_Loop() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Model_Player_Button_PlayLoop_Tooltip);

         setImageDescriptor(_imageDescriptor_Loop);
         setDisabledImageDescriptor(_imageDescriptor_Loop_Disabled);
      }

      @Override
      public void run() {
         onPlayControl_Loop();
      }
   }

   private class Action_PlayControl_PlayAndPause extends Action {

      Action_PlayControl_PlayAndPause() {

         super(null, AS_PUSH_BUTTON);

         setImageDescriptor(_imageDescriptor_Play);
         setDisabledImageDescriptor(_imageDescriptor_Play_Disabled);
      }

      @Override
      public void run() {
         onPlayControl_PlayOrPause();
      }
   }

   private class Action_ShowMapModel extends Action {

      Action_ShowMapModel() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Model_Player_Button_MapModel_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapModel));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapModel_Disabled));
      }

      @Override
      public void run() {

         ModelPlayerManager.setIsMapModelVisible(this.isChecked());

         enableControls();
      }

   }

   private class Action_ShowMapModelCursor extends Action {

      Action_ShowMapModelCursor() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Model_Player_Button_MapModelCursor_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapModelCursor));
         setDisabledImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapModelCursor_Disabled));
      }

      @Override
      public void run() {

         ModelPlayerManager.setIsMapModelCursorVisible(this.isChecked());

         enableControls();
      }

   }

   private class Action_SlideoutMapModel extends ActionToolbarSlideoutAdv {

      private SlideoutMapModel __slideoutMapModel;

      public Action_SlideoutMapModel() {

         super(TourbookPlugin.getThemedImageDescriptor(Images.MapModelList),
               TourbookPlugin.getThemedImageDescriptor(Images.MapModelList_Disabled));
      }

      @Override
      protected AdvancedSlideout createSlideout(final ToolItem toolItem) {

         __slideoutMapModel = new SlideoutMapModel(toolItem, _state);

         return __slideoutMapModel;
      }

      @Override
      protected void onBeforeOpenSlideout() {
         closeOpenedDialogs(this);
      }
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == ModelPlayerView.this) {
               Map25FPSManager.setBackgroundFPSToAnimationFPS(true);
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == ModelPlayerView.this) {
               Map25FPSManager.setBackgroundFPSToAnimationFPS(false);
            }
         }

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };

      getViewSite().getPage().addPartListener(_partListener);
   }

   /**
    * Close all opened dialogs except the opening dialog.
    *
    * @param openingDialog
    */
   @Override
   public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
      _openDialogManager.closeOpenedDialogs(openingDialog);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionPlayControl_Loop          = new Action_PlayControl_Loop();
      _actionPlayControl_PlayAndPause  = new Action_PlayControl_PlayAndPause();
      _actionShowMapModel              = new Action_ShowMapModel();
      _actionShowMapModelCursor        = new Action_ShowMapModelCursor();
      _actionSlideoutMapModel          = new Action_SlideoutMapModel();

// SET_FORMATTING_ON
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;
      _display = parent.getDisplay();

      initUI();

      createActions();
      fillActionBars();

      addPartListener();

      createUI(parent);

      parent.getDisplay().asyncExec(() -> {

         // run async because the theme may not yet been initialized
         restoreState();

         enableControls();

         ModelPlayerManager.setModelPlayerView(this);

         updatePlayer_InUIThread();
      });
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .spacing(5, 0)
            .applyTo(container);
      {
         createUI_10_Timeline(container);
         createUI_20_PlayerControls(container);
      }
   }

   private void createUI_10_Timeline(final Composite parent) {

      final GridDataFactory gridDataAlignEndCenter = GridDataFactory.fillDefaults()
            .align(SWT.CENTER, SWT.CENTER)
            .hint(_pc.convertWidthInCharsToPixels(8), SWT.DEFAULT);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(5)
            .spacing(5, 0)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      /*
       * Timeline for all frames
       */
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            _lblTimeline = UI.createLabel(container, Messages.Model_Player_Label_Timeline);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblTimeline);
//            _lblTimeline_AllFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleTimeline = new Scale(container, SWT.HORIZONTAL);
            _scaleTimeline.setMinimum(0);
            _scaleTimeline.setMaximum(100);
            _scaleTimeline.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onTimeline_Selection()));
            _scaleTimeline.addKeyListener(keyPressedAdapter(keyEvent -> onTimeline_Key(keyEvent)));
            _scaleTimeline.addMouseWheelListener(mouseEvent -> onTimeline_MouseWheel(mouseEvent));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 5)
                  .applyTo(_scaleTimeline);
//            _scaleTimeline_AllFrames.setBackground(UI.SYS_COLOR_MAGENTA);
         }
         {
            _lblTimeline_Value = new Label(container, SWT.CENTER);
            gridDataAlignEndCenter.applyTo(_lblTimeline_Value);
//            _lblTimeline_Value.setBackground(UI.SYS_COLOR_CYAN);
         }
         UI.createSpacer_Horizontal(container, 1);
      }

      /*
       * Jog wheel to set the model speed
       */
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            _lblSpeedJogWheel = UI.createLabel(container, Messages.Model_Player_Label_Speed);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblSpeedJogWheel);
//            _lblTimeline_VisibleFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleSpeedJogWheel = new Scale(container, SWT.HORIZONTAL);
            _scaleSpeedJogWheel.setMinimum(0);
            _scaleSpeedJogWheel.setMaximum(ModelPlayerManager.SPEED_JOG_WHEEL_MAX);
            _scaleSpeedJogWheel.setPageIncrement(4);
            _scaleSpeedJogWheel.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSpeedJogWheel_Selection()));
            _scaleSpeedJogWheel.addKeyListener(keyPressedAdapter(keyEvent -> onSpeedJogWheel_Key(keyEvent)));
            _scaleSpeedJogWheel.addMouseWheelListener(mouseEvent -> onSpeedJogWheel_MouseWheel(mouseEvent));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 5)
                  .applyTo(_scaleSpeedJogWheel);
//            _scaleTimeline_VisibleFrames.setBackground(UI.SYS_COLOR_MAGENTA);
         }
         {
            _lblSpeedJogWheel_Value = new Label(container, SWT.CENTER);
            gridDataAlignEndCenter.applyTo(_lblSpeedJogWheel_Value);
//            _lblSpeedJogWheel_Value.setBackground(UI.SYS_COLOR_CYAN);
         }
         UI.createSpacer_Horizontal(container, 1);
      }
   }

   private void createUI_20_PlayerControls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(0, 10)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(10).applyTo(container);
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            /*
             * Speed multiplier
             */
            _lblSpeedMultiplier = UI.createLabel(container, Messages.Model_Player_Label_SpeedMultiplier);
            _lblSpeedMultiplier.setToolTipText(Messages.Model_Player_Label_SpeedMultiplier_Tooltip);

            _spinnerSpeedMultiplier = new Spinner(container, SWT.BORDER);
            _spinnerSpeedMultiplier.setToolTipText(Messages.Model_Player_Label_SpeedMultiplier_Tooltip);
            _spinnerSpeedMultiplier.setMinimum(1);
            _spinnerSpeedMultiplier.setMaximum(10_000_000);
            _spinnerSpeedMultiplier.setIncrement(1);
            _spinnerSpeedMultiplier.setPageIncrement(5);
            _spinnerSpeedMultiplier.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_SpeedMultiplier()));
            _spinnerSpeedMultiplier.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 5);
               onSelect_SpeedMultiplier();
            });
//            GridDataFactory.fillDefaults().applyTo(_spinnerSpeedMultiplier);
         }
         {
            /*
             * Model size
             */
            _lblModelSize = UI.createLabel(container, Messages.Model_Player_Label_ModelSize);

            _spinnerModelSize = new Spinner(container, SWT.BORDER);
            _spinnerModelSize.setMinimum(ModelPlayerManager.MODEL_SIZE_MIN);
            _spinnerModelSize.setMaximum(ModelPlayerManager.MODEL_SIZE_MAX);
            _spinnerModelSize.setIncrement(10);
            _spinnerModelSize.setPageIncrement(50);
            _spinnerModelSize.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ModelSize()));
            _spinnerModelSize.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onSelect_ModelSize();
            });
//            GridDataFactory.fillDefaults().applyTo(_spinnerModelSize);
         }
         {
            /*
             * Model cursor size
             */
            _lblModelCursorSize = UI.createLabel(container, Messages.Model_Player_Label_ModelCursorSize);

            _spinnerModelCursorSize = new Spinner(container, SWT.BORDER);
            _spinnerModelCursorSize.setMinimum(ModelPlayerManager.MODEL_CURSOR_SIZE_MIN);
            _spinnerModelCursorSize.setMaximum(ModelPlayerManager.MODEL_CURSOR_SIZE_MAX);
            _spinnerModelCursorSize.setIncrement(10);
            _spinnerModelCursorSize.setPageIncrement(50);
            _spinnerModelCursorSize.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ModelCursorSize()));
            _spinnerModelCursorSize.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
               onSelect_ModelCursorSize();
            });
//            GridDataFactory.fillDefaults().applyTo(_spinnerModelCursorSize);
         }
         {
            /*
             * Model turning angle
             */
            _lblTurningAngle = UI.createLabel(container, Messages.Model_Player_Label_TurningAngle);
            _lblTurningAngle.setToolTipText(Messages.Model_Player_Label_TurningAngle_Tooltip);

            _spinnerTurningMultiplier = new Spinner(container, SWT.BORDER);
            _spinnerTurningMultiplier.setToolTipText(Messages.Model_Player_Label_TurningAngle_Tooltip);
            _spinnerTurningMultiplier.setMinimum(0);
            _spinnerTurningMultiplier.setMaximum(50);
            _spinnerTurningMultiplier.setIncrement(1);
            _spinnerTurningMultiplier.setPageIncrement(5);
            _spinnerTurningMultiplier.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_TurningAngle()));
            _spinnerTurningMultiplier.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
               onSelect_TurningAngle();
            });
//            GridDataFactory.fillDefaults().applyTo(_spinnerTurningAngle);
         }
         {
            /*
             * Relive playing
             */
            _chkIsRelivePlaying = new Button(container, SWT.CHECK);
            _chkIsRelivePlaying.setText(Messages.Model_Player_Checkbox_ReLivePlaying);
            _chkIsRelivePlaying.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ReLivePlaying()));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_chkIsRelivePlaying);

         }
      }
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      ModelPlayerManager.setModelPlayerView(null);

      super.dispose();
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean canShowMapModel          = ModelPlayerManager.canShowMapModel();
      final boolean isMapModelVisible        = ModelPlayerManager.isMapModelVisible();
      final boolean isMapModelCursorVisible  = ModelPlayerManager.isMapModelCursorVisible();

      final boolean canEditMapModel          = canShowMapModel && isMapModelVisible;
      final boolean canEditMapModelCursor    = canShowMapModel && isMapModelCursorVisible;
      final boolean canEditModelOrCursor     = canShowMapModel && (isMapModelVisible || isMapModelCursorVisible);


      _actionPlayControl_Loop             .setEnabled(canShowMapModel);
      _actionPlayControl_PlayAndPause     .setEnabled(canShowMapModel);
      _actionShowMapModel                 .setEnabled(canShowMapModel);
      _actionShowMapModelCursor           .setEnabled(canShowMapModel);
      _actionSlideoutMapModel             .setEnabled(canEditMapModel);

      _lblModelCursorSize                 .setEnabled(canEditMapModelCursor);
      _lblModelSize                       .setEnabled(canEditMapModel);
      _lblSpeedMultiplier                 .setEnabled(canShowMapModel);
      _lblSpeedJogWheel                   .setEnabled(canShowMapModel);
      _lblSpeedJogWheel_Value             .setEnabled(canShowMapModel);
      _lblTimeline                        .setEnabled(canShowMapModel);
      _lblTimeline_Value                  .setEnabled(canShowMapModel);
      _lblTurningAngle                    .setEnabled(canEditModelOrCursor);

      _chkIsRelivePlaying                 .setEnabled(canShowMapModel);

      _scaleSpeedJogWheel                 .setEnabled(canShowMapModel);
      _scaleTimeline                      .setEnabled(canShowMapModel);

      _spinnerModelCursorSize             .setEnabled(canEditMapModelCursor);
      _spinnerModelSize                   .setEnabled(canEditMapModel);
      _spinnerSpeedMultiplier             .setEnabled(canShowMapModel);
      _spinnerTurningMultiplier           .setEnabled(canEditModelOrCursor);

// SET_FORMATTING_ON
   }

   private void fillActionBars() {

      /*
       * Fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionShowMapModel);
      tbm.add(_actionShowMapModelCursor);

      tbm.add(new Separator());

      tbm.add(_actionPlayControl_PlayAndPause);
      tbm.add(_actionPlayControl_Loop);

      tbm.add(new Separator());

      tbm.add(_actionSlideoutMapModel);
   }

   /**
    * Sync maps with current player position
    *
    * @param useVisibleFrames
    */
   private void fireMapPosition() {

      final ModelPlayerData modelPlayerData = ModelPlayerManager.getModelPlayerData();
      if (modelPlayerData == null) {
         return;
      }

      final int[] allNotClipped_GeoLocationIndices = modelPlayerData.allNotClipped_GeoLocationIndices;
      final int numNotClippedPositions = allNotClipped_GeoLocationIndices.length;

      if (numNotClippedPositions == 0) {
         return;
      }

      final double relativePosition = getTimelineRelativePosition();

      final float[] allDistanceSeries = modelPlayerData.allDistanceSeries;
      final int lastDistanceIndex = allDistanceSeries.length - 1;

      final float totalDistance = allDistanceSeries[lastDistanceIndex];
      final float positionDistance = (float) (relativePosition * totalDistance);

      final int distanceIndex = MtMath.searchIndex(allDistanceSeries, positionDistance);

      final MapPosition mapPosition = fireMapPosition_CreateProjectedMapPosition(modelPlayerData, distanceIndex);

      MapManager.fireSyncMapEvent(mapPosition, this, null);
   }

   private MapPosition fireMapPosition_CreateProjectedMapPosition(final ModelPlayerData modelPlayerData, final int geoLocationIndex) {

      final int projectedIndex = geoLocationIndex * 2;

      final double projectedPositionX = modelPlayerData.allProjectedPoints_NormalTrack[projectedIndex];
      final double projectedPositionY = modelPlayerData.allProjectedPoints_NormalTrack[projectedIndex + 1];

      final MapPosition mapPosition = new MapPosition();

      mapPosition.x = projectedPositionX;
      mapPosition.y = projectedPositionY;

      return mapPosition;
   }

   /**
    * @return Returns the timeline relative position 0...1
    */
   private double getTimelineRelativePosition() {

      final int timelineSelection = _scaleTimeline.getSelection();
      final double relativePosition = timelineSelection / (double) _scaleTimeline.getMaximum();

      return relativePosition;
   }

   private void initUI() {

      _pc = new PixelConverter(_parent);

   }

   private boolean moveTimelinePlayheadTo_End() {

      final int timelineSelection = _scaleTimeline.getSelection();

      if (timelineSelection == _scaleTimeline.getMinimum()

            // loop only when looping is selected
            && _actionPlayControl_Loop.isChecked()) {

         // beginning of timeline + moving left -> start from the end

         /*
          * Run async otherwise the selection is not at the correct position when scale was moved
          * with the mouse wheel. The mouse event do not have a doit property to prevent the
          * wrong selection.
          */
         _display.asyncExec(() -> {

            if (_parent.isDisposed()) {
               return;
            }

            setTimelineSelection(1);
            setMapAndModelPosition(RELATIVE_MODEL_POSITION_ON_RETURN_PATH_START_TO_END);
         });

         return true;
      }

      return false;
   }

   private boolean moveTimelinePlayheadTo_Start() {

      final int timelineSelection = _scaleTimeline.getSelection();

      if (timelineSelection == _scaleTimeline.getMaximum()

            // loop only when looping is selected
            && _actionPlayControl_Loop.isChecked()) {

         // end of timeline + moving right -> start from 0

         _display.asyncExec(() -> {

            if (_parent.isDisposed()) {
               return;
            }

            setTimelineSelection(0);
            setMapAndModelPosition(RELATIVE_MODEL_POSITION_ON_RETURN_PATH_END_TO_START);
         });

         return true;
      }

      return false;
   }

   private void onPlayControl_Loop() {

      final boolean isPlayingLoop = _actionPlayControl_Loop.isChecked();

      ModelPlayerManager.setIsPlayingLoop(isPlayingLoop);

      if (isPlayingLoop
            && ModelPlayerManager.isPlayerRunning() == false
            && ModelPlayerManager.isLastFrame()) {

         // start new anmimation

         setTimelineSelection(0);
         setMapAndModelPosition(0);

         ModelPlayerManager.setIsPlayerRunning(true);

         updateUI_PlayAndPausedControls();
      }
   }

   private void onPlayControl_PlayOrPause() {

      togglePlayAndPaused();
   }

   private void onSelect_ModelCursorSize() {

      ModelPlayerManager.setModelCursorSize(_spinnerModelCursorSize.getSelection());
   }

   private void onSelect_ModelSize() {

      ModelPlayerManager.setModelSize(_spinnerModelSize.getSelection());
   }

   private void onSelect_ReLivePlaying() {

      ModelPlayerManager.setIsReLivePlaying(_chkIsRelivePlaying.getSelection());
   }

   private void onSelect_SpeedMultiplier() {

      ModelPlayerManager.setSpeedMultiplier(_spinnerSpeedMultiplier.getSelection());

      // adjust timeline
      updateUI_TimelineMaxValue();
   }

   private void onSelect_TurningAngle() {

      ModelPlayerManager.setTurningAngle(_spinnerTurningMultiplier.getSelection());
   }

   private void onSpeedJogWheel_Key(final KeyEvent keyEvent) {

      final int eventKeyCode = keyEvent.keyCode;

      final int jogWheelSelection = _scaleSpeedJogWheel.getSelection();
      final int jogWheelSpeed = jogWheelSelection - ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF;

      boolean isJogWheelSelected = false;

      if (eventKeyCode == ' ') {

         if (ModelPlayerManager.isPlayerRunning()) {

            // stop running

            ModelPlayerManager.setIsPlayerRunning(false);

         } else {

            // start running

            ModelPlayerManager.setIsPlayerRunning(true);
         }

         updateUI_PlayAndPausedControls();

      } else if (eventKeyCode == '0') {

         // select speed 0

         ModelPlayerManager.setIsPlayerRunning(false);
         updateUI_PlayAndPausedControls();

         setJogWheel_Value(ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF);

         isJogWheelSelected = true;

      } else if (eventKeyCode == SWT.HOME) {

         // move to the left

         if (jogWheelSpeed > 0) {

            // select speed 0

            setJogWheel_Value(ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF);

            isJogWheelSelected = true;

         } else if (jogWheelSpeed < 0 && jogWheelSelection > 0) {

            // select speed -max

            setJogWheel_Value(0);

            isJogWheelSelected = true;
         }

      } else if (eventKeyCode == SWT.END) {

         // move to the right

         if (jogWheelSpeed < 0) {

            // select speed 0

            setJogWheel_Value(ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF);

            isJogWheelSelected = true;

         } else if (jogWheelSpeed > 0 && jogWheelSpeed < ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF) {

            // select speed max

            setJogWheel_Value(ModelPlayerManager.SPEED_JOG_WHEEL_MAX);

            isJogWheelSelected = true;
         }
      }

      if (isJogWheelSelected) {

         keyEvent.doit = false;

         // fire selection
         _scaleSpeedJogWheel.getDisplay().asyncExec(this::onSpeedJogWheel_Selection);
      }

   }

   private void onSpeedJogWheel_MouseWheel(final MouseEvent mouseEvent) {

      final int speedSelection = _scaleSpeedJogWheel.getSelection();

      updateUI_JogWheel(speedSelection);
   }

   private void onSpeedJogWheel_Selection() {

      final int selectedSpeed = _scaleSpeedJogWheel.getSelection();
      final int movingSpeed = selectedSpeed - ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF;

      updateUI_JogWheel(selectedSpeed);

      // start playing when jog wheel is modified
      if (movingSpeed != 0 && ModelPlayerManager.isPlayerRunning() == false) {

         ModelPlayerManager.setIsPlayerRunning(true);
         updateUI_PlayAndPausedControls();
      }

      ModelPlayerManager.setMovingSpeedFromJogWheel(selectedSpeed);
   }

   private void onTimeline_Key(final KeyEvent keyEvent) {

      if (keyEvent.character == ' ') {

         // set moving direction otherwise the model could move backward
         ModelPlayerManager.setMovingSpeedFromJogWheel(_scaleSpeedJogWheel.getSelection());

         togglePlayAndPaused();

      } else {

         boolean isPlayheadMoved = false;
         boolean isForward = false;

         final int eventKeyCode = keyEvent.keyCode;

         if (eventKeyCode == SWT.ARROW_LEFT
               || eventKeyCode == SWT.PAGE_DOWN) {

            isPlayheadMoved = moveTimelinePlayheadTo_End();

         } else if (eventKeyCode == SWT.ARROW_RIGHT
               || eventKeyCode == SWT.PAGE_UP) {

            isPlayheadMoved = moveTimelinePlayheadTo_Start();
            isForward = true;
         }

         if (isPlayheadMoved == false) {

            // accelerate movement

            if (eventKeyCode == SWT.ARROW_LEFT || eventKeyCode == SWT.ARROW_RIGHT) {

               if (UI.adjustScaleValueOnKey(keyEvent, isForward)) {

                  // timeline scale is selected with new value -> do other selection actions

                  isPlayheadMoved = true;

                  setMapAndModelPosition(getTimelineRelativePosition());
               }
            }
         }

         if (isPlayheadMoved) {

            // playhead is moved with all other selection actions -> prevent default action

            keyEvent.doit = false;

            updateUI_TimelineValue(_scaleTimeline.getSelection());
         }
      }
   }

   private void onTimeline_MouseWheel(final MouseEvent mouseEvent) {

      stopPlayerWhenRunning();

      if (mouseEvent.count < 0) {

         // mouse is scrolled down

         moveTimelinePlayheadTo_End();

      } else {

         // mouse is scrolled up

         moveTimelinePlayheadTo_Start();
      }
   }

   private void onTimeline_Selection() {

      if (_isInUpdateTimeline) {
         return;
      }

      final int timelineSelection = _scaleTimeline.getSelection();

      updateUI_TimelineValue(timelineSelection);

      stopPlayerWhenRunning();

      setMapAndModelPosition(getTimelineRelativePosition());
   }

   void restoreState() {

// SET_FORMATTING_OFF

      _actionPlayControl_Loop    .setChecked(ModelPlayerManager.isPlayingLoop());
      _actionShowMapModel        .setChecked(ModelPlayerManager.isMapModelVisible());
      _actionShowMapModelCursor  .setChecked(ModelPlayerManager.isMapModelCursorVisible());

      _chkIsRelivePlaying        .setSelection(ModelPlayerManager.isReLivePlaying());

      _spinnerModelCursorSize    .setSelection(ModelPlayerManager.getModelCursorSize());
      _spinnerModelSize          .setSelection(ModelPlayerManager.getModelSize());

// SET_FORMATTING_ON

      setJogWheel_Value(ModelPlayerManager.getJogWheelSpeed());

      updateUI_PlayAndPausedControls();
   }

   @Override
   public void setFocus() {

      Map25FPSManager.setBackgroundFPSToAnimationFPS(true);

      _scaleTimeline.setFocus();
   }

   private void setJogWheel_Value(final int jogWheelValue) {

      _scaleSpeedJogWheel.setSelection(jogWheelValue);

      updateUI_JogWheel(jogWheelValue);
   }

   /**
    * Fire map position and start model animation
    *
    * @param relativeModelPosition
    * @param shortestDistanceMapPosition
    *           When this is not <code>null</code> then move the model to this map position by using
    *           the shortest distance
    */
   private void setMapAndModelPosition(final double relativeModelPosition) {

      setTimeline_Tooltip();

      fireMapPosition();

      ModelPlayerManager.setRelativePosition(relativeModelPosition);
   }

   private void setTimeline_Tooltip() {

      final int selection = _scaleTimeline.getSelection();

      _scaleTimeline.setToolTipText(Integer.toString(selection));
   }

   /**
    * Select timeline from a relative position
    *
    * @param relativePosition
    */
   private void setTimelineSelection(final double relativePosition) {

      final int timelineValue = (int) (_currentTimelineMaxValue * relativePosition);

      _scaleTimeline.setSelection(timelineValue);

      updateUI_TimelineValue(timelineValue);
   }

   private void stopPlayerWhenRunning() {

      if (ModelPlayerManager.isPlayerRunning()) {

         ModelPlayerManager.setIsPlayerRunning(false);
         updateUI_PlayAndPausedControls();
      }
   }

   /**
    * Toggle play and pause controls
    */
   private void togglePlayAndPaused() {

      final boolean isPlayerRunning = ModelPlayerManager.isPlayerRunning();

      if (isPlayerRunning == false && ModelPlayerManager.isLastFrame()) {

         // start new anmimation

         _scaleTimeline.setSelection(0);

         ModelPlayerManager.setIsPlayerRunning(true);
         ModelPlayerManager.setRelativePosition(0);

      } else {

         if (isPlayerRunning) {

            // is playing -> pause

            ModelPlayerManager.setIsPlayerRunning(false);

         } else {

            // is paused -> play

            ModelPlayerManager.setIsPlayerRunning(true);
         }
      }

      updateUI_PlayAndPausedControls();
   }

   void updateMapModelVisibility() {

      enableControls();
   }

   /**
    * This is called when new data are set into the shader in
    * {@link net.tourbook.map25.renderer.TourTrack_Shader#bindBufferData(TourTrack_Bucket, GLViewport)}
    */
   public void updatePlayer() {

      // run in display thread, this method is called from the shader thread

      if (_parent.isDisposed()) {
         return;
      }

      _display.asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         updatePlayer_InUIThread();

      });
   }

   /**
    * This is called when new data are set into the shader, data are available from
    * {@link ModelPlayerManager#getModelPlayerData()}
    */
   private void updatePlayer_InUIThread() {

      updateUI_TimelineMaxValue();

      _spinnerSpeedMultiplier.setSelection(ModelPlayerManager.getSpeedMultiplier());
      _spinnerTurningMultiplier.setSelection((ModelPlayerManager.getModelTurningAngle()));

      enableControls();
   }

   public void updatePlayer_Timeline(final double relativePosition_CurrentFrame) {

      _display.asyncExec(() -> {

         if (_scaleTimeline.isDisposed()) {
            return;
         }

         final int timelineValue = (int) (_currentTimelineMaxValue * relativePosition_CurrentFrame);

         _isInUpdateTimeline = true;
         {
            _scaleTimeline.setSelection(timelineValue);
            _scaleTimeline.setToolTipText(Integer.toString(timelineValue));
            updateUI_TimelineValue(timelineValue);
         }
         _isInUpdateTimeline = false;
      });
   }

   /**
    * Set speed jog wheel value in the UI
    *
    * @param jogWheelValue
    */
   private void updateUI_JogWheel(final int jogWheelValue) {

      final int movingSpeed = jogWheelValue - ModelPlayerManager.SPEED_JOG_WHEEL_MAX_HALF;

      final String speedValue = Integer.toString(movingSpeed);

      _lblSpeedJogWheel_Value.setText(speedValue);
      _scaleSpeedJogWheel.setToolTipText(speedValue);

      Color fgColor;
      Color bgColor;

      if (movingSpeed == 0) {

         fgColor = ThemeUtil.getDefaultForegroundColor_Shell();
         bgColor = ThemeUtil.getDefaultBackgroundColor_Table();

      } else if (movingSpeed > 0) {

         fgColor = UI.SYS_COLOR_WHITE;
         bgColor = JOG_WHEEL_COLOR_GREATER_0;

      } else {

         fgColor = UI.SYS_COLOR_WHITE;
         bgColor = JOG_WHEEL_COLOR_LESS_0;
      }

      _lblSpeedJogWheel_Value.setBackground(bgColor);
      _lblSpeedJogWheel_Value.setForeground(fgColor);
   }

   private void updateUI_PlayAndPausedControls() {

      if (ModelPlayerManager.isPlayerRunning()) {

         _actionPlayControl_PlayAndPause.setToolTipText(Messages.Model_Player_Button_Pause_Tooltip);

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Pause);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Pause_Disabled);

      } else {

         _actionPlayControl_PlayAndPause.setToolTipText(Messages.Model_Player_Button_Play_Tooltip);

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Play);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Play_Disabled);
      }
   }

   /**
    * Update the timeline values when a new tour is selected
    */
   private void updateUI_TimelineMaxValue() {

      final int minScaleTicks = 50;

      final ModelPlayerData modelPlayerData = ModelPlayerManager.getModelPlayerData();

      if (modelPlayerData == null || modelPlayerData.allNotClipped_GeoLocationIndices == null) {
         return;
      }

      final int newMaximum = modelPlayerData.allNotClipped_GeoLocationIndices.length - 1;

      // update only when modified
      if (newMaximum == _currentTimelineMaxValue) {
         return;
      }

      _currentTimelineMaxValue = newMaximum;

      final float pageIncrement = (float) newMaximum / minScaleTicks;
      final float relativeSelection = (float) (newMaximum * ModelPlayerManager.getCurrentRelativePosition());
      final int timelineValue = (int) relativeSelection;

      _scaleTimeline.setPageIncrement((int) pageIncrement);
      _scaleTimeline.setMaximum(newMaximum);

      // reselect last position
      _scaleTimeline.setSelection(timelineValue);

      updateUI_TimelineValue(timelineValue);
      setTimeline_Tooltip();
   }

   private void updateUI_TimelineValue(final int newTimelineValue) {

      // update only when modified
      if (newTimelineValue == _currentTimelineValue) {
         return;
      }

      _currentTimelineValue = newTimelineValue;

      _lblTimeline_Value.setText(Integer.toString(newTimelineValue));
   }

}
