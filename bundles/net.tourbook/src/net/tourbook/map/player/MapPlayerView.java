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
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class MapPlayerView extends ViewPart {

   public static final String           ID                             = "net.tourbook.map.player.MapPlayerView"; //$NON-NLS-1$
   //
   private static final String          STATE_IS_LINK_WITH_OTHER_VIEWS = "STATE_IS_LINK_WITH_OTHER_VIEWS";        //$NON-NLS-1$
   //
   private static final IDialogSettings _state                         = TourbookPlugin.getState(ID);
   //
   private ITourEventListener           _tourEventListener;
   //
   private Action                       _actionPlayControl_PlayAndPause;
   private Action                       _actionPlayControl_Stop;
   //
   private boolean                      _isPlaying;
   private boolean                      _isShow_EndTime_Or_RemainingTime;
   private int                          _timeCurrent;
   private int                          _timeEnd;

   /*
    * UI controls
    */
   private Label _lblTime_Current;
   private Label _lblTime_EndOrRemaining;

   private Scale _scaleTimeline;

   private enum PlayControl {

      PlayOrPause, //
      Stop
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == MapPlayerView.this) {
               return;
            }

            if ((eventId == TourEventId.MAP_PLAYER) && eventData instanceof MapPlayerData) {

               // run in display thread, this event is fired from the shader thread
               _lblTime_Current.getDisplay().asyncExec(() -> {

                  if (_lblTime_Current.isDisposed()) {
                     return;
                  }

                  onSelectionChanged((MapPlayerData) eventData);

               });
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
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

      createUI(parent);

      addTourEventListener();

      restoreState();

      parent.getDisplay().asyncExec(() -> onSelectionChanged(null));
   }

   private void createUI(final Composite parent) {

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
            _scaleTimeline.addSelectionListener(widgetSelectedAdapter(selectionEvent -> updateUI()));
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
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults().span(5, 1).applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionPlayControl_PlayAndPause);
            tbm.add(_actionPlayControl_Stop);

            tbm.update(true);
         }
      }

      // set default label width
      container.layout(true, true);
   }

   @Override
   public void dispose() {

      super.dispose();
   }

   private void enableActions(final boolean isPlayerEnabled) {

// SET_FORMATTING_OFF

      _lblTime_Current                 .setEnabled(isPlayerEnabled);
      _lblTime_EndOrRemaining          .setEnabled(isPlayerEnabled);
      _scaleTimeline                   .setEnabled(isPlayerEnabled);

      _actionPlayControl_PlayAndPause  .setEnabled(isPlayerEnabled);
      _actionPlayControl_Stop          .setEnabled(isPlayerEnabled);

// SET_FORMATTING_ON
   }

   private void onMouseDown_TimeEndOrRemaining() {

      _isShow_EndTime_Or_RemainingTime = !_isShow_EndTime_Or_RemainingTime;

      updateUI();
   }

   private void onSelectionChanged(final MapPlayerData mapPlayerData) {

      if (mapPlayerData != null && mapPlayerData.isPlayerEnabled) {

         _timeEnd = mapPlayerData.endTime;

         _scaleTimeline.setMaximum(_timeEnd);
         _scaleTimeline.setPageIncrement(_timeEnd / 20);

         _scaleTimeline.setSelection(0);

         updateUI();

         enableActions(mapPlayerData.isPlayerEnabled);

      } else {

         enableActions(false);
      }

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

      _isShow_EndTime_Or_RemainingTime = Util.getStateBoolean(_state, STATE_IS_LINK_WITH_OTHER_VIEWS, true);

      enableActions(false);
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_LINK_WITH_OTHER_VIEWS, _isShow_EndTime_Or_RemainingTime);
   }

   @Override
   public void setFocus() {

      _scaleTimeline.setFocus();
   }

   private void updateUI() {

      final int timelineSelection = _scaleTimeline.getSelection();

      final float timePart = (float) timelineSelection / _timeEnd;
      _timeCurrent = (int) (timePart * _timeEnd);

      final long endOrRemainingTime = _isShow_EndTime_Or_RemainingTime
            ? _timeEnd
            : _timeCurrent - _timeEnd;

      final String timeCurrentText = UI.format_mm_ss_WithSign(_timeCurrent);
      final String timeEndOrRemainingText = UI.format_mm_ss_WithSign(endOrRemainingTime);

      _lblTime_Current.setText(timeCurrentText);
      _lblTime_EndOrRemaining.setText(timeEndOrRemainingText);

      // set layout when text length is larger than the default: -00:00
//    _lblTime_Current.getParent().layout(true, true);
   }

}
