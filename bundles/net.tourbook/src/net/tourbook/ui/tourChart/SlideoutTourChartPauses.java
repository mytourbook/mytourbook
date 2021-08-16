/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.ui.tourChart;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutTourChartPauses extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private ActionOpenPrefDialog  _actionPrefDialog;
   private ActionResetToDefaults _actionRestoreDefaults;

   /*
    * UI controls
    */
   private TourChart _tourChart;

   private Button    _chkShowPauseTooltip;

   public SlideoutTourChartPauses(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourChart tourChart) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   protected boolean closeShellAfterHidden() {

      /*
       * Close the tooltip that the state is saved.
       */

      return true;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {

      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(Messages.Tour_Action_EditChartPreferences, PrefPageAppearanceTourChart.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_tourChart.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

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
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
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
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText("Pauses de parcours ...");//Messages.Slideout_TourInfoOptions_Label_Title);

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
      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show pause tooltip
             */
            _chkShowPauseTooltip = new Button(container, SWT.CHECK);
            _chkShowPauseTooltip.setText(Messages.Slideout_ChartPauseOptions_Checkbox_IsShowPauseTooltip);
            _chkShowPauseTooltip.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkShowPauseTooltip);
         }
      }
   }

   private void onChangeUI() {

      final boolean isShowPauseTooltip = _chkShowPauseTooltip.getSelection();

      _prefStore.setValue(ITourbookPreferences.GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP, isShowPauseTooltip);

      /*
       * Update chart config
       */
      final TourChartConfiguration tourChartConfiguration = _tourChart.getTourChartConfig();

      tourChartConfiguration.isShowPauseTooltip = isShowPauseTooltip;

      // update chart with new settings
      _tourChart.updateUI_PausesLayer();
   }

   @Override
   public void resetToDefaults() {

      /*
       * Update UI with defaults from pref store
       */
      _chkShowPauseTooltip.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP));

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tourChartConfiguration = _tourChart.getTourChartConfig();

      _chkShowPauseTooltip.setSelection(tourChartConfiguration.isShowPauseTooltip);
   }

}
