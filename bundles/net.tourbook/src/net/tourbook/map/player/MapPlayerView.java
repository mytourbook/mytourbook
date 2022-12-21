/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapManager;
import net.tourbook.map25.Map25FPSManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;

public class MapPlayerView extends ViewPart {

   public static final String           ID                     = "net.tourbook.map.player.MapPlayerView"; //$NON-NLS-1$
   //
   private static final String          DEFAULT_TIME_START     = ".  00:00";                              //$NON-NLS-1$
   private static final String          DEFAULT_TIME_END       = ".  00:00          .";                   //$NON-NLS-1$
   //
   private static final String          STATE_IS_SHOW_END_TIME = "STATE_IS_SHOW_END_TIME";                //$NON-NLS-1$
   //
   private static final IDialogSettings _state                 = TourbookPlugin.getState(ID);
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
   private IPartListener2 _partListener;
   //
   private Action         _actionPlayControl_PlayAndPause;
   private Action         _actionPlayControl_Loop;
   //
   private boolean        _isIgnoreTimelineEvent;
   private boolean        _isShow_EndTime_Or_RemainingTime;
   //
   private int[]          _updateCounter = new int[1];
   //
   /*
    * UI controls
    */
   private Composite _parent;

   private Label     _lblFPS;
   private Label     _lblTimeline_Current;
   private Label     _lblWobbleNavi_Current;
   private Label     _lblTime_EndOrRemaining_AllFrames;
   private Label     _lblTime_EndOrRemaining_VisibleFrames;
   private Label     _lblTimeline;
   private Label     _lblSpeedWobbler;

   private Button    _chkIsRelivePlaying;

   private Scale     _scaleTimeline;
   private Scale     _scaleSpeedJogWheel;

   private Spinner   _spinnerFramesPerSecond;

   private class Action_PlayControl_Loop extends Action {

      Action_PlayControl_Loop() {

         super(null, AS_CHECK_BOX);

         setToolTipText("Click to toggle loop and no loop");

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

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == MapPlayerView.this) {
               Map25FPSManager.setBackgroundFPSToAnimationFPS(true);
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == MapPlayerView.this) {
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

   private void createActions() {

      _actionPlayControl_Loop = new Action_PlayControl_Loop();
      _actionPlayControl_PlayAndPause = new Action_PlayControl_PlayAndPause();
   }

   @Override
   public void createPartControl(final Composite parent) {

      _parent = parent;

      createActions();
      addPartListener();

      createUI(parent);

      enableControls();

      restoreState();

      MapPlayerManager.setMapPlayerViewer(this);

      parent.getDisplay().asyncExec(() -> {

         // set default label width
//         _scaleWobbleNaviagator.getParent().getParent().layout(true, true);

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

      final GridDataFactory gridDataAlignEndCenter = GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(6)
            .spacing(5, 0)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      /*
       * Timeline for all frames
       */
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            _lblTimeline = UI.createLabel(container, "&Timeline");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblTimeline);
//            _lblTimeline_AllFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _lblTimeline_Current = UI.createLabel(container, DEFAULT_TIME_START);
            gridDataAlignEndCenter.applyTo(_lblTimeline_Current);
//            _lblTime_Current_AllFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleTimeline = new Scale(container, SWT.HORIZONTAL);
            _scaleTimeline.setMinimum(0);
            _scaleTimeline.setMaximum(10);
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
            _lblTime_EndOrRemaining_AllFrames = UI.createLabel(container, DEFAULT_TIME_END);
            _lblTime_EndOrRemaining_AllFrames.setToolTipText("Total or remaining time Click to toggle between total and remaining time");
            _lblTime_EndOrRemaining_AllFrames.addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown_TimeEndOrRemaining()));
            gridDataAlignEndCenter.applyTo(_lblTime_EndOrRemaining_AllFrames);
//            _lblTime_EndOrRemaining_AllFrames.setBackground(UI.SYS_COLOR_CYAN);
         }
         UI.createSpacer_Horizontal(container, 1);
      }

      /*
       * Timeline for visible frames
       */
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            _lblSpeedWobbler = UI.createLabel(container, "&Speed");
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblSpeedWobbler);
//            _lblTimeline_VisibleFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _lblWobbleNavi_Current = UI.createLabel(container, DEFAULT_TIME_START);
            gridDataAlignEndCenter.applyTo(_lblWobbleNavi_Current);
//            _lblTime_Current_VisibleFrames.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleSpeedJogWheel = new Scale(container, SWT.HORIZONTAL);
            _scaleSpeedJogWheel.setMinimum(0);
            _scaleSpeedJogWheel.setMaximum(MapPlayerManager.SPEED_JOG_WHEEL_MAX);
            _scaleSpeedJogWheel.setPageIncrement(5);
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
            _lblTime_EndOrRemaining_VisibleFrames = UI.createLabel(container, DEFAULT_TIME_END);
            _lblTime_EndOrRemaining_VisibleFrames.setToolTipText("Total or remaining time Click to toggle between total and remaining time");
            _lblTime_EndOrRemaining_VisibleFrames.addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown_TimeEndOrRemaining()));
            gridDataAlignEndCenter.applyTo(_lblTime_EndOrRemaining_VisibleFrames);
//            _lblTime_EndOrRemaining_VisibleFrames.setBackground(UI.SYS_COLOR_CYAN);
         }
         UI.createSpacer_Horizontal(container, 1);
      }
   }

   private void createUI_20_PlayerControls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(6).applyTo(container);
      {
         UI.createSpacer_Horizontal(container, 1);
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionPlayControl_PlayAndPause);
            tbm.add(_actionPlayControl_Loop);

            tbm.update(true);
//            toolbar.setBackground(UI.SYS_COLOR_CYAN);
         }
         {
            /*
             * Relive playing
             */
            _chkIsRelivePlaying = new Button(container, SWT.CHECK);
            _chkIsRelivePlaying.setText("&Re-live playing");
//            _chkIsRelivePlaying.setToolTipText(Messages.Map_Player_Checkbox_IsReLivePlaying_Tooltip);
            _chkIsRelivePlaying.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectReLivePlaying()));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_chkIsRelivePlaying);
         }
         {
            /*
             * Foreground: Frames per Second
             */
            _spinnerFramesPerSecond = new Spinner(container, SWT.BORDER);
            _spinnerFramesPerSecond.setToolTipText("Frames per second, when running in the foreground and having the focus");
            _spinnerFramesPerSecond.setMinimum(-1);
            _spinnerFramesPerSecond.setMaximum(Map25FPSManager.DEFAULT_FOREGROUND_FPS);
            _spinnerFramesPerSecond.setIncrement(1);
            _spinnerFramesPerSecond.setPageIncrement(5);
            _spinnerFramesPerSecond.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectFPS()));
            _spinnerFramesPerSecond.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               onSelectFPS();
            });
            GridDataFactory.fillDefaults().applyTo(_spinnerFramesPerSecond);

            _lblFPS = UI.createLabel(container, "fps");
         }
         UI.createSpacer_Horizontal(container, 1);
      }
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      MapPlayerManager.setMapPlayerViewer(null);

      super.dispose();
   }

   private void enableControls() {

      final boolean isEnabled = MapPlayerManager.isPlayerEnabled() && MapPlayerManager.isAnimationVisible();

// SET_FORMATTING_OFF

      _lblFPS                                .setEnabled(isEnabled);
      _lblTimeline_Current                   .setEnabled(isEnabled);
      _lblWobbleNavi_Current                 .setEnabled(isEnabled);
      _lblTime_EndOrRemaining_AllFrames      .setEnabled(isEnabled);
      _lblTime_EndOrRemaining_VisibleFrames  .setEnabled(isEnabled);
      _lblTimeline                           .setEnabled(isEnabled);
      _lblSpeedWobbler                       .setEnabled(isEnabled);

      _scaleSpeedJogWheel                     .setEnabled(isEnabled);
      _scaleTimeline                         .setEnabled(isEnabled);

      _spinnerFramesPerSecond                   .setEnabled(isEnabled);

      _actionPlayControl_PlayAndPause           .setEnabled(isEnabled);
      _actionPlayControl_Loop                   .setEnabled(isEnabled);

// SET_FORMATTING_ON
   }

   /**
    * Sync maps with current player position
    *
    * @param relativePosition
    * @param useVisibleFrames
    */
   private void fireMapPlayerPosition(final double relativePosition) {

      final MapPlayerData mapPlayerData = MapPlayerManager.getMapPlayerData();
      if (mapPlayerData == null) {
         return;
      }

      int geoLocationIndex = 0;

      final int[] allNotClipped_GeoLocationIndices = mapPlayerData.allNotClipped_GeoLocationIndices;
      final int numNotClippedPositions = allNotClipped_GeoLocationIndices.length;

      if (numNotClippedPositions == 0) {
         return;
      }

      int positionIndex = (int) (numNotClippedPositions * relativePosition);

      // check bounds
      positionIndex = positionIndex >= numNotClippedPositions ? numNotClippedPositions - 1 : positionIndex;

      geoLocationIndex = allNotClipped_GeoLocationIndices[positionIndex];

//         System.out.println((System.currentTimeMillis()
//
//               + " posIdx:" + positionIndex
//               + " geoLocIdx:" + geoLocationIndex
//
//         ));
      // TODO remove SYSTEM.OUT.PRINTLN

      final GeoPoint[] anyGeoPoints = mapPlayerData.anyGeoPoints;
      final GeoPoint geoLocation = anyGeoPoints[geoLocationIndex];

      // lat/lon -> 0...1
      final double modelProjectedPositionX = MercatorProjection.longitudeToX(geoLocation.getLongitude());
      final double modelProjectedPositionY = MercatorProjection.latitudeToY(geoLocation.getLatitude());

      final MapPosition mapPosition = new MapPosition();
      mapPosition.x = modelProjectedPositionX;
      mapPosition.y = modelProjectedPositionY;
      mapPosition.setScale(mapPlayerData.mapScale);

      MapPlayerManager.setRelativePosition(relativePosition);

      MapManager.fireSyncMapEvent(mapPosition, this, null);
   }

   private boolean movePlayheadTo_End() {

      final float timelineSelection = _scaleTimeline.getSelection();

      if (timelineSelection == _scaleTimeline.getMinimum()) {

         // beginning of timeline + moving left -> start from the end

         // prevent selection event
         _isIgnoreTimelineEvent = true;
         _scaleTimeline.setSelection(_scaleTimeline.getMaximum());

         MapPlayerManager.setRelativePosition(1);

         return true;
      }

      return false;
   }

   private boolean movePlayheadTo_Start() {

      final float timelineSelection = _scaleTimeline.getSelection();

      if (timelineSelection == _scaleTimeline.getMaximum()) {

         // end of timeline + moving right -> start from 0

         // prevent selection event
         _isIgnoreTimelineEvent = true;
         _scaleTimeline.setSelection(0);

         MapPlayerManager.setRelativePosition(0);

         return true;
      }

      return false;
   }

   private void onMouseDown_TimeEndOrRemaining() {

      _isShow_EndTime_Or_RemainingTime = !_isShow_EndTime_Or_RemainingTime;

//      updateUI_FromTimeline_VisibleFrames();
   }

   private void onPlayControl_Loop() {

      final boolean isPlayingLoop = _actionPlayControl_Loop.isChecked();

      MapPlayerManager.setIsPlayingLoop(isPlayingLoop);

      if (isPlayingLoop
            && MapPlayerManager.isPlayerRunning() == false
            && MapPlayerManager.isLastFrame()) {

         // start new anmimation

         _isIgnoreTimelineEvent = true;
         _scaleTimeline.setSelection(0);

         MapPlayerManager.setIsPlayerRunning(true);
         MapPlayerManager.setRelativePosition(0);

         updateUI_PlayAndPausedControls();
      }
   }

   private void onPlayControl_PlayOrPause() {

      togglePlayAndPaused();
   }

   private void onSelectFPS() {

      final int selectedFPS = _spinnerFramesPerSecond.getSelection();

      MapPlayerManager.setForegroundFPS(selectedFPS);

      // adjust timeline
      updateUI_TimelineMaxValue(selectedFPS);
   }

   private void onSelectReLivePlaying() {

      MapPlayerManager.setIsReLivePlaying(_chkIsRelivePlaying.getSelection());
   }

   private void onSpeedJogWheel_Key(final KeyEvent keyEvent) {

      boolean isMoved = false;
      boolean isForward = false;

      final int eventKeyCode = keyEvent.keyCode;

      final int jogWheelSelection = _scaleSpeedJogWheel.getSelection();
      final int jogWheelSpeed = jogWheelSelection - MapPlayerManager.SPEED_JOG_WHEEL_MAX_HALF;

      if (eventKeyCode == SWT.HOME) {

         // move to the left

         if (jogWheelSpeed > 0) {

            // select speed 0

            _scaleSpeedJogWheel.setSelection(MapPlayerManager.SPEED_JOG_WHEEL_MAX_HALF);

            keyEvent.doit = false;

         } else if (jogWheelSpeed < 0 && jogWheelSelection > 0) {

            // select speed -max

            _scaleSpeedJogWheel.setSelection(0);

            keyEvent.doit = false;
         }

      } else if (eventKeyCode == SWT.END) {

         // move to the right

         if (jogWheelSpeed < 0) {

            // select speed 0

            _scaleSpeedJogWheel.setSelection(MapPlayerManager.SPEED_JOG_WHEEL_MAX_HALF);

            keyEvent.doit = false;

         } else if (jogWheelSpeed > 0 && jogWheelSpeed < MapPlayerManager.SPEED_JOG_WHEEL_MAX_HALF) {

            // select speed max

            _scaleSpeedJogWheel.setSelection(MapPlayerManager.SPEED_JOG_WHEEL_MAX);

            keyEvent.doit = false;
         }

      } else if (eventKeyCode == SWT.ARROW_LEFT
            || eventKeyCode == SWT.PAGE_DOWN) {

         isMoved = movePlayheadTo_End();

      } else if (eventKeyCode == SWT.ARROW_RIGHT
            || eventKeyCode == SWT.PAGE_UP) {

         isMoved = movePlayheadTo_Start();
         isForward = true;
      }

      if (isMoved == false) {

         // accelerate movement

         UI.adjustScaleValueOnKey(keyEvent, isForward);
      }
   }

   private void onSpeedJogWheel_MouseWheel(final MouseEvent mouseEvent) {

   }

   private void onSpeedJogWheel_Selection() {

      MapPlayerManager.setMovingSpeedFromJogWheel(_scaleSpeedJogWheel.getSelection());
   }

   private void onTimeline_Key(final KeyEvent keyEvent) {

      if (keyEvent.character == ' ') {

         togglePlayAndPaused();

      } else {

         boolean isMoved = false;
         boolean isForward = false;

         final int eventKeyCode = keyEvent.keyCode;

         if (eventKeyCode == SWT.ARROW_LEFT
               || eventKeyCode == SWT.PAGE_DOWN) {

            isMoved = movePlayheadTo_End();

         } else if (eventKeyCode == SWT.ARROW_RIGHT
               || eventKeyCode == SWT.PAGE_UP) {

            isMoved = movePlayheadTo_Start();
            isForward = true;
         }

         if (isMoved == false) {

            // accelerate movement

            UI.adjustScaleValueOnKey(keyEvent, isForward);
         }
      }
   }

   private void onTimeline_MouseWheel(final MouseEvent mouseEvent) {

      if (mouseEvent.count < 0) {

         // scrolled down

         movePlayheadTo_End();

      } else {

         // scrolled up

         movePlayheadTo_Start();
      }
   }

   private void onTimeline_Selection() {

      if (_isIgnoreTimelineEvent) {
         _isIgnoreTimelineEvent = false;
         return;
      }

      stopPlayerWhenRunning();

      final double timelineSelection = _scaleTimeline.getSelection();
      final int scaleMaximum = _scaleTimeline.getMaximum();
      final double relativePosition = timelineSelection / scaleMaximum;

      fireMapPlayerPosition(relativePosition);
   }

   private void restoreState() {

      _isShow_EndTime_Or_RemainingTime = Util.getStateBoolean(_state, STATE_IS_SHOW_END_TIME, true);

      _actionPlayControl_Loop.setChecked(MapPlayerManager.isPlayingLoop());
      _chkIsRelivePlaying.setSelection(MapPlayerManager.isReLivePlaying());

      _scaleSpeedJogWheel.setSelection(MapPlayerManager.getWobblerSpeedValue());

      updateUI_PlayAndPausedControls();
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_SHOW_END_TIME, _isShow_EndTime_Or_RemainingTime);
   }

   @Override
   public void setFocus() {

      _scaleTimeline.setFocus();
//      _scaleTimeline_VisibleFrames.setFocus();
   }

   private void stopPlayerWhenRunning() {

      if (MapPlayerManager.isPlayerRunning()) {

         MapPlayerManager.setIsPlayerRunning(false);
         updateUI_PlayAndPausedControls();
      }
   }

   /**
    * Toggle play and pause controls
    */
   private void togglePlayAndPaused() {

      final boolean isPlayerRunning = MapPlayerManager.isPlayerRunning();

      if (isPlayerRunning == false && MapPlayerManager.isLastFrame()) {

         // start new anmimation

         _isIgnoreTimelineEvent = true;
         _scaleTimeline.setSelection(0);

         MapPlayerManager.setIsPlayerRunning(true);
         MapPlayerManager.setRelativePosition(0);

      } else {

         if (isPlayerRunning) {

            // is playing -> pause

            MapPlayerManager.setIsPlayerRunning(false);

         } else {

            // is paused -> play

            MapPlayerManager.setIsPlayerRunning(true);
         }
      }

      updateUI_PlayAndPausedControls();
   }

   void updateAnimationVisibility() {

      enableControls();
   }

   /**
    * This is called when new data are set into the shader
    */
   public void updatePlayer() {

      // run in display thread, this method is called from the shader thread

      if (_parent.isDisposed()) {
         return;
      }

      _parent.getDisplay().asyncExec(() -> {

         if (_parent.isDisposed()) {
            return;
         }

         updatePlayer_InUIThread();

      });
   }

   /**
    * This is called when new data are set into the shader, data are available from
    * {@link MapPlayerManager#getMapPlayerData()}
    */
   private void updatePlayer_InUIThread() {

      final int foregroundFPS = MapPlayerManager.getForegroundFPS();

      updateUI_TimelineMaxValue(foregroundFPS);

      _spinnerFramesPerSecond.setSelection(foregroundFPS);

      enableControls();
   }

   private void updateUI_PlayAndPausedControls() {

      if (MapPlayerManager.isPlayerRunning()) {

         _actionPlayControl_PlayAndPause.setToolTipText("Pause the playback");

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Pause);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Pause_Disabled);

      } else {

         _actionPlayControl_PlayAndPause.setToolTipText("Play");

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Play);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Play_Disabled);
      }
   }

   /**
    * Update the timelines max and page increment values
    *
    * @param selectedFPS
    */
   private void updateUI_TimelineMaxValue(final int selectedFPS) {

      final int minScaleTicks = 100;

      final MapPlayerData mapPlayerData = MapPlayerManager.getMapPlayerData();
      if (mapPlayerData != null && mapPlayerData.allNotClipped_GeoLocationIndices != null) {

         final int lastMaximum = _scaleTimeline.getMaximum();
         final int numNotClippedFrames = mapPlayerData.allNotClipped_GeoLocationIndices.length;

         if (lastMaximum != numNotClippedFrames) {

            // update max only when changed

            final int lastSelection = _scaleTimeline.getSelection();
            final float lastRelativeSelection = (float) lastSelection / lastMaximum;

            final float notClippedPageIncrement = (float) numNotClippedFrames / minScaleTicks;
            final float relativeSelection = numNotClippedFrames * lastRelativeSelection;

            _scaleTimeline.setIncrement(1);
            _scaleTimeline.setPageIncrement((int) notClippedPageIncrement);
            _scaleTimeline.setMaximum(numNotClippedFrames);

            // reselect last position
            _isIgnoreTimelineEvent = true;
            _scaleTimeline.setSelection((int) relativeSelection);
         }
      }
   }

}
