/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourLogManager.AutoOpenWhen;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutTourLogOptions extends ToolbarSlideout {

   private SelectionAdapter _defaultSelectionListener;

   private PixelConverter   _pc;

   /*
    * UI controls
    */
   private Button _chkEvent_DeleteSomething;
   private Button _chkEvent_DownloadSomething;
   private Button _chkEvent_SaveSomething;
   private Button _chkEvent_TourAdjustments;
   private Button _chkEvent_TourImport;
   private Button _chkEvent_TourUpload;

   private Button _rdoOpenLog_AllEvents;
   private Button _rdoOpenLog_Never;
   private Button _rdoOpenLog_SelectedEvents;

   /**
    * @param ownerControl
    * @param toolBar
    * @param state
    */
   public SlideoutTourLogOptions(final Control ownerControl,
                                 final ToolBar toolBar) {

      super(ownerControl, toolBar);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
//					.numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_TourLogOptions_Label_Title);
      label.setFont(JFaceResources.getBannerFont());
      GridDataFactory.fillDefaults().applyTo(label);

      MTFont.setBannerFont(label);
   }

   private void createUI_20_Controls(final Composite parent) {

      final int defaultTextWidth = _pc.convertWidthInCharsToPixels(50);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            // label: When auto open?
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Slideout_TourLogOptions_Label_AutoOpenLogView);
            GridDataFactory.fillDefaults()
                  .hint(defaultTextWidth, SWT.DEFAULT)
                  .applyTo(label);
         }
         {
            // radio: Always
            _rdoOpenLog_AllEvents = new Button(container, SWT.RADIO);
            _rdoOpenLog_AllEvents.setText(Messages.Slideout_TourLogOptions_Radio_OpenLog_AllEvents);
            _rdoOpenLog_AllEvents.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)

                  // show more space above
//                  .indent(0, 6)
                  .applyTo(_rdoOpenLog_AllEvents);
         }
         {
            // radio: Never
            _rdoOpenLog_Never = new Button(container, SWT.RADIO);
            _rdoOpenLog_Never.setText(Messages.Slideout_TourLogOptions_Radio_OpenLog_Never);
            _rdoOpenLog_Never.addSelectionListener(_defaultSelectionListener);
         }
         {
            // radio: Selected events
            _rdoOpenLog_SelectedEvents = new Button(container, SWT.RADIO);
            _rdoOpenLog_SelectedEvents.setText(Messages.Slideout_TourLogOptions_Radio_OpenLog_SelectedEvents);
            _rdoOpenLog_SelectedEvents.addSelectionListener(_defaultSelectionListener);
         }
         {
            final Composite containerSelectedEvents = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).indent(16, 0).applyTo(containerSelectedEvents);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(containerSelectedEvents);
            {
               {
                  _chkEvent_TourImport = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_TourImport.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_ImportTour);
                  _chkEvent_TourImport.addSelectionListener(_defaultSelectionListener);
               }
               {
                  _chkEvent_SaveSomething = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_SaveSomething.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_SaveSomething);
                  _chkEvent_SaveSomething.addSelectionListener(_defaultSelectionListener);
               }
               {
                  _chkEvent_DeleteSomething = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_DeleteSomething.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_DeleteSomething);
                  _chkEvent_DeleteSomething.addSelectionListener(_defaultSelectionListener);
               }
               {
                  _chkEvent_DownloadSomething = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_DownloadSomething.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_DownloadSomething);
                  _chkEvent_DownloadSomething.addSelectionListener(_defaultSelectionListener);
               }
               {
                  _chkEvent_TourUpload = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_TourUpload.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_TourUpload);
                  _chkEvent_TourUpload.addSelectionListener(_defaultSelectionListener);
               }
               {
                  _chkEvent_TourAdjustments = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_TourAdjustments.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_TourAdjustments);
                  _chkEvent_TourAdjustments.addSelectionListener(_defaultSelectionListener);
               }
            }
         }
         {
            // label: More info
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Slideout_TourLogOptions_Label_AutoOpenLogView_MoreInfo);
            GridDataFactory.fillDefaults()
                  .hint(defaultTextWidth, SWT.DEFAULT)

                  // show more space above
                  .indent(0, 16)
                  .applyTo(label);
         }
      }
   }

   private void enableControls() {

      final boolean isSelectedEvents = _rdoOpenLog_SelectedEvents.getSelection();

// SET_FORMATTING_OFF

      _chkEvent_DeleteSomething     .setEnabled(isSelectedEvents);
      _chkEvent_DownloadSomething   .setEnabled(isSelectedEvents);
      _chkEvent_SaveSomething       .setEnabled(isSelectedEvents);
      _chkEvent_TourAdjustments     .setEnabled(isSelectedEvents);
      _chkEvent_TourImport          .setEnabled(isSelectedEvents);
      _chkEvent_TourUpload          .setEnabled(isSelectedEvents);

// SET_FORMATTING_ON
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();
   }

   private void restoreState() {

      final AutoOpenWhen autoOpenWhen = TourLogManager.getAutoOpenWhen();

// SET_FORMATTING_OFF

      _rdoOpenLog_AllEvents      .setSelection(autoOpenWhen.equals(AutoOpenWhen.ALL_EVENTS));
      _rdoOpenLog_SelectedEvents .setSelection(autoOpenWhen.equals(AutoOpenWhen.SELECTED_EVENTS));
      _rdoOpenLog_Never          .setSelection(autoOpenWhen.equals(AutoOpenWhen.NEVER));

// SET_FORMATTING_ON

      for (final AutoOpenEvent autoOpenEvent : TourLogManager.getAutoOpenEvents()) {

         if (autoOpenEvent.equals(AutoOpenEvent.DELETE_SOMETHING)) {
            _chkEvent_DeleteSomething.setSelection(true);
         }

         if (autoOpenEvent.equals(AutoOpenEvent.SAVE_SOMETHING)) {
            _chkEvent_SaveSomething.setSelection(true);
         }

         if (autoOpenEvent.equals(AutoOpenEvent.DOWNLOAD_SOMETHING)) {
            _chkEvent_DownloadSomething.setSelection(true);
         }

         if (autoOpenEvent.equals(AutoOpenEvent.TOUR_IMPORT)) {
            _chkEvent_TourImport.setSelection(true);
         }

         if (autoOpenEvent.equals(AutoOpenEvent.TOUR_ADJUSTMENTS)) {
            _chkEvent_TourAdjustments.setSelection(true);
         }

         if (autoOpenEvent.equals(AutoOpenEvent.TOUR_UPLOAD)) {
            _chkEvent_TourUpload.setSelection(true);
         }
      }
   }

   private void saveState() {

      AutoOpenWhen autoOpenWhen = AutoOpenWhen.NEVER;

      if (_rdoOpenLog_Never.getSelection()) {

         autoOpenWhen = AutoOpenWhen.NEVER;

      } else if (_rdoOpenLog_AllEvents.getSelection()) {

         autoOpenWhen = AutoOpenWhen.ALL_EVENTS;

      } else if (_rdoOpenLog_SelectedEvents.getSelection()) {

         autoOpenWhen = AutoOpenWhen.SELECTED_EVENTS;
      }

      final ArrayList<AutoOpenEvent> allOpenEvents = new ArrayList<>();

      if (_chkEvent_DeleteSomething.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.DELETE_SOMETHING);
      }

      if (_chkEvent_SaveSomething.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.SAVE_SOMETHING);
      }

      if (_chkEvent_DownloadSomething.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.DOWNLOAD_SOMETHING);
      }

      if (_chkEvent_TourImport.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.TOUR_IMPORT);
      }

      if (_chkEvent_TourUpload.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.TOUR_UPLOAD);
      }

      if (_chkEvent_TourAdjustments.getSelection()) {
         allOpenEvents.add(AutoOpenEvent.TOUR_ADJUSTMENTS);
      }

      TourLogManager.saveState_AutoOpenValues(autoOpenWhen, allOpenEvents);
   }

}
