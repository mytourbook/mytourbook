/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutYearStatisticOptions extends ToolbarSlideout implements IActionResetToDefault {

   private RefTour_YearStatistic_View _refTour_YearStatistic_View;

   private ActionResetToDefaults      _actionRestoreDefaults;

   private ChartOptions_Grid          _gridUI;

   private IDialogSettings            _state;

   private SelectionListener          _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _chkShowAvgAltimeter;
   private Button _chkShowAvgPulse;
   private Button _chkShowAvgSpeed;
   private Button _chkShowMaxPulse;

   public SlideoutYearStatisticOptions(final RefTour_YearStatistic_View refTour_YearStatistic_View,
                                       final Control ownerControl,
                                       final ToolBar toolBar,
                                       final String prefStoreGridPrefix,
                                       final IDialogSettings state) {

      super(ownerControl, toolBar);

      _refTour_YearStatistic_View = refTour_YearStatistic_View;
      _state = state;

      _gridUI = new ChartOptions_Grid(prefStoreGridPrefix);
   }

   private void createActions() {

      /*
       * Action: Restore default
       */
      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            createUI_20_Graphs(container);

            _gridUI.createUI(container);

            _gridUI.enableGridOptions(ChartOptions_Grid.GRID_VERTICAL_DISTANCE
                  | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
                  | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_RefTour_YearStatisticOptions_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   private void createUI_20_Graphs(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_RefTour_Group_Graphs);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
//    group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Show avg speed
             */
            _chkShowAvgSpeed = new Button(group, SWT.CHECK);
            _chkShowAvgSpeed.setText(Messages.Slideout_RefTour_Checkbox_AvgSpeed);
            _chkShowAvgSpeed.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show avg altimeter (VAM)
             */
            _chkShowAvgAltimeter = new Button(group, SWT.CHECK);
            _chkShowAvgAltimeter.setText(Messages.Slideout_RefTour_Checkbox_AvgAltimeter);
            _chkShowAvgAltimeter.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show avg pulse
             */
            _chkShowAvgPulse = new Button(group, SWT.CHECK);
            _chkShowAvgPulse.setText(Messages.Slideout_RefTour_Checkbox_AvgPulse);
            _chkShowAvgPulse.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show max pulse
             */
            _chkShowMaxPulse = new Button(group, SWT.CHECK);
            _chkShowMaxPulse.setText(Messages.Slideout_RefTour_Checkbox_MaxPulse);
            _chkShowMaxPulse.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      // update chart async that the UI is updated immediately

      Display.getCurrent().asyncExec(() -> {

         saveState();

         _refTour_YearStatistic_View.updateUI_YearChart(false);
      });
   }

   @Override
   public void resetToDefaults() {

      _gridUI.resetToDefaults();
      _gridUI.saveState();

      _chkShowAvgAltimeter.setSelection(true);
      _chkShowAvgPulse.setSelection(true);
      _chkShowAvgSpeed.setSelection(true);
      _chkShowMaxPulse.setSelection(true);
   }

   private void restoreState() {

      _gridUI.restoreState();

      _chkShowAvgAltimeter.setSelection(Util.getStateBoolean(_state, RefTour_YearStatistic_View.STATE_SHOW_AVG_ALTIMETER, true));
      _chkShowAvgPulse.setSelection(Util.getStateBoolean(_state, RefTour_YearStatistic_View.STATE_SHOW_AVG_PULSE, true));
      _chkShowAvgSpeed.setSelection(Util.getStateBoolean(_state, RefTour_YearStatistic_View.STATE_SHOW_AVG_SPEED, true));
      _chkShowMaxPulse.setSelection(Util.getStateBoolean(_state, RefTour_YearStatistic_View.STATE_SHOW_MAX_PULSE, true));
   }

   private void saveState() {

      _state.put(RefTour_YearStatistic_View.STATE_SHOW_AVG_ALTIMETER, _chkShowAvgAltimeter.getSelection());
      _state.put(RefTour_YearStatistic_View.STATE_SHOW_AVG_PULSE, _chkShowAvgPulse.getSelection());
      _state.put(RefTour_YearStatistic_View.STATE_SHOW_AVG_SPEED, _chkShowAvgSpeed.getSelection());
      _state.put(RefTour_YearStatistic_View.STATE_SHOW_MAX_PULSE, _chkShowMaxPulse.getSelection());
   }

}
