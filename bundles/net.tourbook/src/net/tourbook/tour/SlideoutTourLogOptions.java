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

import net.tourbook.Messages;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourLogManager.AutoOpenTourLogView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
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

   private IDialogSettings  _state;

   private SelectionAdapter _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _chkEvent_DeleteTour;

   private Button _rdoOpenLog_Never;
   private Button _rdoOpenLog_AnyEvents;
   private Button _rdoOpenLog_SelectedEvents;

   /**
    * @param ownerControl
    * @param toolBar
    * @param state
    */
   public SlideoutTourLogOptions(final Control ownerControl,
                                 final ToolBar toolBar) {

      super(ownerControl, toolBar);

      _state = TourLogManager.getState();
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

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

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourLogOptions_Label_AutoOpenLogView);
         }
         {
            // radio: Never
            _rdoOpenLog_Never = new Button(container, SWT.RADIO);
            _rdoOpenLog_Never.setText(Messages.Slideout_TourLogOptions_Radio_OpenLog_Never);
            _rdoOpenLog_Never.addSelectionListener(_defaultSelectionListener);
         }
         {
            // radio: Always
            _rdoOpenLog_AnyEvents = new Button(container, SWT.RADIO);
            _rdoOpenLog_AnyEvents.setText(Messages.Slideout_TourLogOptions_Radio_OpenLog_AnyEvents);
            _rdoOpenLog_AnyEvents.addSelectionListener(_defaultSelectionListener);
         }
         {
            // radio: Options
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
                  // checkbox: Delete tour
                  _chkEvent_DeleteTour = new Button(containerSelectedEvents, SWT.CHECK);
                  _chkEvent_DeleteTour.setText(Messages.Slideout_TourLogOptions_Checkbox_Event_DeleteTour);
                  _chkEvent_DeleteTour.addSelectionListener(_defaultSelectionListener);
               }

            }
         }
      }
   }

   private void enableControls() {

      final boolean isSelectedEvents = _rdoOpenLog_SelectedEvents.getSelection();

      _chkEvent_DeleteTour.setEnabled(isSelectedEvents);
   }

   private void initUI() {

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

      final AutoOpenTourLogView autoOpen = (AutoOpenTourLogView) Util.getStateEnum(_state,
            TourLogManager.STATE_AUTO_OPEN_TOUR_LOG_VIEW,
            AutoOpenTourLogView.NEVER);

// SET_FORMATTING_OFF

      switch (autoOpen) {
      case ANY_EVENTS:        _rdoOpenLog_AnyEvents      .setSelection(true);
      case SELECTED_EVENTS:   _rdoOpenLog_SelectedEvents .setSelection(true);
      default:                _rdoOpenLog_Never          .setSelection(true);
      }

// SET_FORMATTING_ON

   }

   private void saveState() {

      TourLogManager.AutoOpenTourLogView stateAutoOpen = AutoOpenTourLogView.NEVER;

      if (_rdoOpenLog_Never.getSelection()) {

         stateAutoOpen = AutoOpenTourLogView.NEVER;

      } else if (_rdoOpenLog_AnyEvents.getSelection()) {

         stateAutoOpen = AutoOpenTourLogView.ANY_EVENTS;

      } else if (_rdoOpenLog_SelectedEvents.getSelection()) {

         stateAutoOpen = AutoOpenTourLogView.SELECTED_EVENTS;
      }

      Util.setStateEnum(_state, TourLogManager.STATE_AUTO_OPEN_TOUR_LOG_VIEW, stateAutoOpen);

   }

}
