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
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

public class MapPlayerView extends ViewPart {

   //
   public static final String ID = "net.tourbook.map.player.MapPlayerView"; //$NON-NLS-1$

   //
   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state     = TourbookPlugin.getState(ID);
   //
   private ITourEventListener            _tourEventListener;
   //
   private Action                        _actionPlayControl_PlayAndPause;
   private Action                        _actionPlayControl_Stop;
   //
   private boolean                       _isPlaying;
   private int                           _timeCurrent;
   private int                           _timeStart;
   private int                           _timeEnd;
   //
   /*
    * UI controls
    */
   private Label _lblTime_Current;
   private Label _lblTime_End;
   private Label _lblTime_Start;
   private Scale _scaleTimeline;

   public enum PlayControl {
      PlayAndPause, Stop

   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == MapPlayerView.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);
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
               onSelectPlayControl(PlayControl.PlayAndPause);
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

      onSelectTime();
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         {
            _scaleTimeline = new Scale(container, SWT.HORIZONTAL);
            _scaleTimeline.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectTime()));
            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_scaleTimeline);
         }
         {
            _lblTime_Start = UI.createLabel(container, UI.SPACE1);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .indent(25, 0)
                  .applyTo(_lblTime_Start);
//            _lblTime_Start.setBackground(UI.SYS_COLOR_RED);

            _lblTime_Current = UI.createLabel(container, UI.SPACE1);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.CENTER, SWT.FILL)
                  .applyTo(_lblTime_Current);
//            _lblTime_Current.setBackground(UI.SYS_COLOR_GREEN);

            _lblTime_End = UI.createLabel(container, UI.SPACE1);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .indent(25, 0)
                  .applyTo(_lblTime_End);
//            _lblTime_End.setBackground(UI.SYS_COLOR_BLUE);
         }
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionPlayControl_PlayAndPause);
            tbm.add(_actionPlayControl_Stop);

            tbm.update(true);
         }
      }
   }

   @Override
   public void dispose() {

      super.dispose();
   }

   private void enableActions() {

   }

   private void onSelectionChanged(final ISelection eventData) {

   }

   private void onSelectPlayControl(final PlayControl playControl) {

      switch (playControl) {
      case PlayAndPause:

         // toggle play + pause

         if (_isPlaying) {

            // is playing -> pause

            _isPlaying = false;

            _actionPlayControl_PlayAndPause.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Play));
            _actionPlayControl_PlayAndPause.setToolTipText("");

         } else {

            // is paused -> play

            _isPlaying = true;

            _actionPlayControl_PlayAndPause.setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.PlayControl_Pause));
            _actionPlayControl_PlayAndPause.setToolTipText("");
         }
         break;

      case Stop:
         break;

      }

   }

   private void onSelectTime() {

      final int selectedTime = _scaleTimeline.getSelection();

      final int timeRange = _timeEnd - _timeStart;
      final float timeSlice = (float) selectedTime / timeRange;
      _timeCurrent = (int) (timeSlice * timeRange);

      _lblTime_Current.setText(UI.format_mm_ss(_timeCurrent));
      _lblTime_Start.setText(UI.format_mm_ss(_timeStart));
      _lblTime_End.setText(UI.format_mm_ss(_timeEnd));
   }

   private void restoreState() {

      _timeStart = 0;
      _timeEnd = 1000;

      _scaleTimeline.setMinimum(_timeStart);
      _scaleTimeline.setMaximum(_timeEnd);

      final int timeRange = _timeEnd - _timeStart;
      final int pageIncrement = timeRange / 20;
      _scaleTimeline.setPageIncrement(pageIncrement);

      enableActions();
   }

   @PersistState
   private void saveState() {

   }

   @Override
   public void setFocus() {

      _scaleTimeline.setFocus();
   }

}
