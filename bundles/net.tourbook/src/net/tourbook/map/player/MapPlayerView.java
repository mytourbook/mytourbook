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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25FPSManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class MapPlayerView extends ViewPart {

   public static final String           ID                     = "net.tourbook.map.player.MapPlayerView"; //$NON-NLS-1$
   //
   private static final String          STATE_IS_SHOW_END_TIME = "STATE_IS_SHOW_END_TIME";                //$NON-NLS-1$
   //
   private static final IDialogSettings _state                 = TourbookPlugin.getState(ID);
   //
   private IPartListener2               _partListener;
   //
   private Action                       _actionPlayControl_PlayAndPause;
   private Action                       _actionPlayControl_Stop;
   //
   private boolean                      _isPlaying;
   private boolean                      _isShow_EndTime_Or_RemainingTime;
   //
   private int                          _currentTime;
   private int                          _endTime;

   /*
    * UI controls
    */
   private Label   _lblTime_Current;
   private Label   _lblTime_EndOrRemaining;

   private Scale   _scaleTimeline;

   private Spinner _spinnerFramesPerSecond;
   private int     _numAllFrames;

   private enum PlayControl {

      PlayOrPause, //
      Stop
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

      {
         /*
          * Action: Play/Pause
          */
         _actionPlayControl_PlayAndPause = new Action() {
            @Override
            public void run() {
               onSelectPlayControl(PlayControl.PlayOrPause);
            }
         };

         _actionPlayControl_PlayAndPause.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Play));
         _actionPlayControl_PlayAndPause.setToolTipText("");
      }
      {
         /*
          * Action: Stop
          */
         _actionPlayControl_Stop = new Action() {
            @Override
            public void run() {
               onSelectPlayControl(PlayControl.Stop);
            }
         };

         _actionPlayControl_Stop.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Stop));
         _actionPlayControl_Stop.setToolTipText("");
      }
   }

   @Override
   public void createPartControl(final Composite parent) {

      createActions();
      addPartListener();

      createUI(parent);

      enableActions(false);

      restoreState();

      MapPlayerManager.setMapPlayerViewer(this);

      updatePlayer_InUIThread(MapPlayerManager.isPlayerEnabled(),
            MapPlayerManager.getNumberofAllFrames(),
            MapPlayerManager.getForegroundFPS(),
            MapPlayerManager.isAnimateFromRelativePosition());
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

      final String defaultTime = UI.format_mm_ss_WithSign(-1000);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(5).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         UI.createSpacer_Horizontal(container, 1);
         {

            _lblTime_Current = UI.createLabel(container, defaultTime);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblTime_Current);
//            _lblTime_Current.setBackground(UI.SYS_COLOR_GREEN);
         }
         {
            _scaleTimeline = new Scale(container, SWT.HORIZONTAL);
            _scaleTimeline.setMinimum(0);
            _scaleTimeline.setMaximum(10);
            _scaleTimeline.addSelectionListener(widgetSelectedAdapter(selectionEvent -> updateUI_FromTimeline()));
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(0, 5)
                  .applyTo(_scaleTimeline);
//            _scaleTimeline.setBackground(UI.SYS_COLOR_MAGENTA);
         }
         {
            _lblTime_EndOrRemaining = UI.createLabel(container, defaultTime);
            _lblTime_EndOrRemaining.setToolTipText("Total or remaining time\nClick to toggle between total and remaining time");
            _lblTime_EndOrRemaining.addMouseListener(MouseListener.mouseDownAdapter(mouseEvent -> onMouseDown_TimeEndOrRemaining()));
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblTime_EndOrRemaining);
//            _lblTime_EndOrRemaining.setBackground(UI.SYS_COLOR_CYAN);
         }
         UI.createSpacer_Horizontal(container, 1);
      }

      // set default label width
      container.layout(true, true);
   }

   private void createUI_20_PlayerControls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         UI.createSpacer_Horizontal(container, 1);
//         {
//            final ToolBar toolbar = new ToolBar(containerControls, SWT.FLAT);
//
//            final ToolBarManager tbm = new ToolBarManager(toolbar);
//
//            tbm.add(_actionPlayControl_PlayAndPause);
//            tbm.add(_actionPlayControl_Stop);
//
//            tbm.update(true);
////            toolbar.setBackground(UI.SYS_COLOR_CYAN);
//         }
         {
            /*
             * Foreground: Frames per Second
             */
            _spinnerFramesPerSecond = new Spinner(container, SWT.BORDER);
            _spinnerFramesPerSecond.setToolTipText("Frames per second, when running in the foreground and having the focus");
            _spinnerFramesPerSecond.setMinimum(1);
            _spinnerFramesPerSecond.setMaximum(Map25FPSManager.DEFAULT_FOREGROUND_FPS);
            _spinnerFramesPerSecond.setIncrement(1);
            _spinnerFramesPerSecond.setPageIncrement(5);
            _spinnerFramesPerSecond.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectFPS()));
            _spinnerFramesPerSecond.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               onSelectFPS();
            });

            UI.createLabel(container, "fps");
         }
      }
   }

   @Override
   public void dispose() {

      getViewSite().getPage().removePartListener(_partListener);

      MapPlayerManager.setMapPlayerViewer(null);

      super.dispose();
   }

   private void enableActions(final boolean isPlayerEnabled) {

// SET_FORMATTING_OFF

      _lblTime_Current                 .setEnabled(isPlayerEnabled);
      _lblTime_EndOrRemaining          .setEnabled(isPlayerEnabled);
      _scaleTimeline                   .setEnabled(isPlayerEnabled);
      _spinnerFramesPerSecond          .setEnabled(isPlayerEnabled);

      _actionPlayControl_PlayAndPause  .setEnabled(isPlayerEnabled);
      _actionPlayControl_Stop          .setEnabled(isPlayerEnabled);

// SET_FORMATTING_ON
   }

   private void onMouseDown_TimeEndOrRemaining() {

      _isShow_EndTime_Or_RemainingTime = !_isShow_EndTime_Or_RemainingTime;

      updateUI_FromTimeline();
   }

   private void onSelectFPS() {

      final int selectedFPS = _spinnerFramesPerSecond.getSelection();

      // adjust timeline
      updateUI_Timeline(selectedFPS, _numAllFrames);

      MapPlayerManager.setForegroundFPS(selectedFPS);
   }

   private void onSelectPlayControl(final PlayControl playControl) {

      switch (playControl) {
      case PlayOrPause:

         // toggle play + pause

         if (_isPlaying) {

            // is playing -> pause

            _isPlaying = false;

            _actionPlayControl_PlayAndPause.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Play));
            _actionPlayControl_PlayAndPause.setToolTipText("Play");

         } else {

            // is paused -> play

            _isPlaying = true;

            _actionPlayControl_PlayAndPause.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Pause));
            _actionPlayControl_PlayAndPause.setToolTipText("Pause the playback");
         }
         break;

      case Stop:
         break;

      }

   }

   private void restoreState() {

      _isShow_EndTime_Or_RemainingTime = Util.getStateBoolean(_state, STATE_IS_SHOW_END_TIME, true);
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_SHOW_END_TIME, _isShow_EndTime_Or_RemainingTime);
   }

   @Override
   public void setFocus() {

      _scaleTimeline.setFocus();
   }

   public void updateFrameNumber(final int currentFrameNumber) {

      final float relativeFrame = (float) currentFrameNumber / _numAllFrames;
      final int currentTime = (int) (relativeFrame * _endTime);

      if (_currentTime != currentTime) {

      }
      final int currentTimeInUI = currentTime;

      _lblTime_Current.getDisplay().asyncExec(() -> {

         if (_lblTime_Current.isDisposed()) {
            return;
         }

         _scaleTimeline.setSelection(currentFrameNumber);
         updateUI_CurrentTime(currentTimeInUI);
      });
   }

   public void updatePlayer(final boolean isPlayerEnabled,
                            final int numAllFrames,
                            final int foregroundFPS,
                            final boolean isAnimateFromRelativePosition) {

      // run in display thread, this method call is started in the shader thread

      _lblTime_Current.getDisplay().asyncExec(() -> {

         if (_lblTime_Current.isDisposed()) {
            return;
         }

         updatePlayer_InUIThread(isPlayerEnabled, numAllFrames, foregroundFPS, isAnimateFromRelativePosition);

      });
   }

   private void updatePlayer_InUIThread(final boolean isPlayerEnabled,
                                        final int numAllFrames,
                                        final int foregroundFPS,
                                        final boolean isAnimateFromRelativePosition) {

      _numAllFrames = numAllFrames;

      updateUI_Timeline(foregroundFPS, numAllFrames);

      _spinnerFramesPerSecond.setSelection(foregroundFPS);

      if (isAnimateFromRelativePosition == false) {

         // start from the beginning
         _scaleTimeline.setSelection(0);
      }

      updateUI_FromTimeline();

      enableActions(isPlayerEnabled);
   }

   private void updateUI_CurrentTime(final int currentTime) {

      _currentTime = currentTime;

      final long endOrRemainingTime = _isShow_EndTime_Or_RemainingTime
            ? _endTime
            : _currentTime - _endTime;

      final String timeCurrentText = UI.format_mm_ss_WithSign(_currentTime);
      final String timeEndOrRemainingText = UI.format_mm_ss_WithSign(endOrRemainingTime);

      _lblTime_Current.setText(timeCurrentText);
      _lblTime_EndOrRemaining.setText(timeEndOrRemainingText);

      // set layout when text length is larger than the default: -00:00
//    _lblTime_Current.getParent().layout(true, true);
   }

   private void updateUI_FromTimeline() {

      final int timelineSelection = _scaleTimeline.getSelection();

      final float relativeTime = (float) timelineSelection / _endTime;
      final int currentTime = (int) (relativeTime * _endTime);

      updateUI_CurrentTime(currentTime);
   }

   private void updateUI_Timeline(final int selectedFPS, final int numAllFrames) {

      _endTime = numAllFrames / selectedFPS;

      _scaleTimeline.setMaximum(numAllFrames);
      _scaleTimeline.setPageIncrement(numAllFrames);
   }

}
