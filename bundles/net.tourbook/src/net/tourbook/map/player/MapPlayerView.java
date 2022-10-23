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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
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
   private float          _currentTime;
   private float          _endTime;
   //
   private int[]          _updateCounter = new int[1];
   //
   /*
    * UI controls
    */
   private Composite _parent;

   private Label     _lblFPS;
   private Label     _lblTime_Current;
   private Label     _lblTime_EndOrRemaining;

   private Button    _chkIsRelivePlaying;

   private Scale     _scaleTimeline;

   private Spinner   _spinnerFramesPerSecond;

   private class Action_PlayControl_Loop extends Action {

      Action_PlayControl_Loop() {

         super(null, AS_CHECK_BOX);

         setToolTipText(Messages.Map_Player_Action_Loop_Tooltip);

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

      enableActions();

      restoreState();

      MapPlayerManager.setMapPlayerViewer(this);

      parent.getDisplay().asyncExec(() -> {

         // set default label width
         _scaleTimeline.getParent().getParent().layout(true, true);

         updatePlayer_InUIThread();
      });
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_10_Timeline(container);
         createUI_20_PlayerControls(container);
      }
   }

   private void createUI_10_Timeline(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         UI.createSpacer_Horizontal(container, 1);
         {

            _lblTime_Current = UI.createLabel(container, DEFAULT_TIME_START);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblTime_Current);
//            _lblTime_Current.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleTimeline = new Scale(container, SWT.HORIZONTAL);
            _scaleTimeline.setMinimum(1);
            _scaleTimeline.setMaximum(10);
            _scaleTimeline.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onTimeline_Selection()));
            _scaleTimeline.addKeyListener(keyPressedAdapter(keyEvent -> onTimeline_Key(keyEvent)));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 5)
                  .applyTo(_scaleTimeline);
//            _scaleTimeline.setBackground(UI.SYS_COLOR_MAGENTA);
         }
         {
            _lblTime_EndOrRemaining = UI.createLabel(container, DEFAULT_TIME_END);
            _lblTime_EndOrRemaining.setToolTipText(Messages.Map_Player_Lable_TimeEndOrRemaining_Tooltip);
            _lblTime_EndOrRemaining.addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown_TimeEndOrRemaining()));
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblTime_EndOrRemaining);
//            _lblTime_EndOrRemaining.setBackground(UI.SYS_COLOR_CYAN);
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
            _chkIsRelivePlaying.setText(Messages.Map_Player_Checkbox_IsReLivePlaying);
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
            _spinnerFramesPerSecond.setToolTipText(Messages.Map_Player_Spinner_FramesPerSecond_Tooptip);
            _spinnerFramesPerSecond.setMinimum(1);
            _spinnerFramesPerSecond.setMaximum(Map25FPSManager.DEFAULT_FOREGROUND_FPS);
            _spinnerFramesPerSecond.setIncrement(1);
            _spinnerFramesPerSecond.setPageIncrement(5);
            _spinnerFramesPerSecond.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectFPS()));
            _spinnerFramesPerSecond.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               onSelectFPS();
            });
            GridDataFactory.fillDefaults().applyTo(_spinnerFramesPerSecond);

            _lblFPS = UI.createLabel(container, Messages.Map_Player_Label_FramesPerSecond);
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

   private void enableActions() {

      final boolean isEnabled = MapPlayerManager.isPlayerEnabled() && MapPlayerManager.isAnimationVisible();

// SET_FORMATTING_OFF

      _lblFPS                          .setEnabled(isEnabled);
      _lblTime_Current                 .setEnabled(isEnabled);
      _lblTime_EndOrRemaining          .setEnabled(isEnabled);

      _scaleTimeline                   .setEnabled(isEnabled);
      _spinnerFramesPerSecond          .setEnabled(isEnabled);

      _actionPlayControl_PlayAndPause  .setEnabled(isEnabled);
      _actionPlayControl_Loop          .setEnabled(isEnabled);

// SET_FORMATTING_ON
   }

   private void onMouseDown_TimeEndOrRemaining() {

      _isShow_EndTime_Or_RemainingTime = !_isShow_EndTime_Or_RemainingTime;

      updateUI_FromTimeline();
   }

   private void onPlayControl_Loop() {

      final boolean isPlayingLoop = _actionPlayControl_Loop.isChecked();

      MapPlayerManager.setIsPlayingLoop(isPlayingLoop);

      if (isPlayingLoop
            && MapPlayerManager.isPlayerRunning() == false
            && MapPlayerManager.isLastFrame()) {

         // start new anmimation

         MapPlayerManager.setIsPlayerRunning(true);
         MapPlayerManager.setRelativePosition(0);

         updateUI_PlayAndPaused();
      }
   }

   private void onPlayControl_PlayOrPause() {

      togglePlayAndPaused();
   }

   private void onSelectFPS() {

      final int selectedFPS = _spinnerFramesPerSecond.getSelection();

      MapPlayerManager.setForegroundFPS(selectedFPS);

      // adjust timeline
      updateUI_Timeline(selectedFPS);
   }

   private void onSelectReLivePlaying() {

      MapPlayerManager.setIsReLivePlaying(_chkIsRelivePlaying.getSelection());
   }

   private void onTimeline_Key(final KeyEvent keyEvent) {

      if (keyEvent.character == ' ') {

         togglePlayAndPaused();

      } else {

         if (keyEvent.keyCode == SWT.ARROW_LEFT) {

            final float timelineSelection = _scaleTimeline.getSelection();

            if (timelineSelection == _scaleTimeline.getMinimum()) {

               // beginning of timeline + moving left -> start from the end

               // prevent selection event
               _isIgnoreTimelineEvent = true;

               MapPlayerManager.setRelativePosition(1);
            }

         } else if (keyEvent.keyCode == SWT.ARROW_RIGHT) {

            final float timelineSelection = _scaleTimeline.getSelection();

            if (timelineSelection == _scaleTimeline.getMaximum()) {

               // end of timeline + moving right -> start from 0

               // prevent selection event
               _isIgnoreTimelineEvent = true;

               MapPlayerManager.setRelativePosition(0);
            }
         }
      }
   }

   private void onTimeline_Selection() {

      if (_isIgnoreTimelineEvent) {
         _isIgnoreTimelineEvent = false;
         return;
      }

      // stop player
      if (MapPlayerManager.isPlayerRunning()) {

         MapPlayerManager.setIsPlayerRunning(false);
         updateUI_PlayAndPaused();
      }

      updateUI_FromTimeline();

      final float timelineSelection = _scaleTimeline.getSelection();
      final float relativePosition = timelineSelection / MapPlayerManager.getNumberofAllFrames();

      MapPlayerManager.setRelativePosition(relativePosition);

   }

   private void restoreState() {

      _isShow_EndTime_Or_RemainingTime = Util.getStateBoolean(_state, STATE_IS_SHOW_END_TIME, true);

      _actionPlayControl_Loop.setChecked(MapPlayerManager.isPlayingLoop());
      _chkIsRelivePlaying.setSelection(MapPlayerManager.isReLivePlaying());

      updateUI_PlayAndPaused();
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_SHOW_END_TIME, _isShow_EndTime_Or_RemainingTime);
   }

   @Override
   public void setFocus() {

      _scaleTimeline.setFocus();
   }

   /**
    * Toggle play and pause controls
    */
   private void togglePlayAndPaused() {

      final boolean isPlayerRunning = MapPlayerManager.isPlayerRunning();

      if (isPlayerRunning == false && MapPlayerManager.isLastFrame()) {

         // start new anmimation

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

      updateUI_PlayAndPaused();
   }

   void updateAnimationVisibility() {

      enableActions();
   }

   private void updateCurrentTime(final int currentTime) {

      _currentTime = currentTime;

      /*
       * Update UI
       */
      final float endOrRemainingTime = _isShow_EndTime_Or_RemainingTime
            ? _endTime
            : _currentTime - _endTime;

      final String timeCurrentText = UI.format_mm_ss_WithSign(Math.round(_currentTime));
      final String timeEndOrRemainingText = UI.format_mm_ss_WithSign(Math.round(endOrRemainingTime));
      final int currentFrameNumber = MapPlayerManager.getCurrentFrameNumber();

      _lblTime_Current.setText(timeCurrentText);
      _lblTime_EndOrRemaining.setText(timeEndOrRemainingText + UI.SPACE2 + Integer.toString(currentFrameNumber));

      // set layout when text length is larger than the default: -00:00
//    _lblTime_Current.getParent().layout(true, true);
   }

   public void updateFrameNumber(final int currentFrameNumber) {

      if (_parent.isDisposed()) {
         return;
      }

      final int numAllFrames = MapPlayerManager.getNumberofAllFrames();

      final float relativeFrame = (float) currentFrameNumber / numAllFrames;
      final float currentTime = relativeFrame * _endTime;

      final int currentTimeInUI = (int) currentTime;

      _updateCounter[0]++;

      // update in UI thread
      _parent.getDisplay().asyncExec(new Runnable() {

         final int __runnableCounter = _updateCounter[0];

         @Override
         public void run() {

            // skip all updates which has not yet been executed
            if (__runnableCounter != _updateCounter[0]) {

               // a new update occurred
               return;
            }

            if (_parent.isDisposed()) {
               return;
            }

            _scaleTimeline.setSelection(currentFrameNumber);

            // this is a very expensive operation: 28 ms for each frame !!!
//          _scaleTimeline.setToolTipText(Integer.toString(currentFrameNumber));

            updateCurrentTime(currentTimeInUI);

            // stop playing when end of animation is reached
            final boolean isLastFrame = currentFrameNumber == numAllFrames;
            if (isLastFrame && MapPlayerManager.isPlayingLoop() == false) {

               MapPlayerManager.setIsPlayerRunning(false);

               updateUI_PlayAndPaused();
            }
         }
      });
   }

   public void updatePlayer() {

      // run in display thread, this method call is started in the shader thread

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

   private void updatePlayer_InUIThread() {

      final int foregroundFPS = MapPlayerManager.getForegroundFPS();

      updateUI_Timeline(foregroundFPS);

      _spinnerFramesPerSecond.setSelection(foregroundFPS);
      _scaleTimeline.setSelection(MapPlayerManager.getCurrentFrameNumber() - 1);

      updateUI_FromTimeline();

      enableActions();
   }

   private void updateUI_FromTimeline() {

      final int timelineSelection = _scaleTimeline.getSelection();

      final float relativeTime = (float) timelineSelection / MapPlayerManager.getNumberofAllFrames();
      final int currentTime = (int) (relativeTime * _endTime);

      updateCurrentTime(currentTime);
   }

   private void updateUI_PlayAndPaused() {

      if (MapPlayerManager.isPlayerRunning()) {

         _actionPlayControl_PlayAndPause.setToolTipText(Messages.Map_Player_PlayControl_Pause_Tooltip);

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Pause);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Pause_Disabled);

      } else {

         _actionPlayControl_PlayAndPause.setToolTipText(Messages.Map_Player_PlayContol_Play_Tooptip);

         _actionPlayControl_PlayAndPause.setImageDescriptor(_imageDescriptor_Play);
         _actionPlayControl_PlayAndPause.setDisabledImageDescriptor(_imageDescriptor_Play_Disabled);
      }
   }

   private void updateUI_Timeline(final int selectedFPS) {

      final int numAllFrames = MapPlayerManager.getNumberofAllFrames();
      final float pageIncrement = (float) numAllFrames / 10;

      _endTime = numAllFrames / selectedFPS;

      _scaleTimeline.setMaximum(numAllFrames);
      _scaleTimeline.setPageIncrement((int) pageIncrement);
   }

}
