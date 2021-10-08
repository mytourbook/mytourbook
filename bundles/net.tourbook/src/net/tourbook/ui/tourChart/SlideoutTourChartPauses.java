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

import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart pauses properties slideout.
 */
public class SlideoutTourChartPauses extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;
   private FocusListener          _keepOpenListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent focusEvent) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent focusEvent) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   private ActionResetToDefaults _actionRestoreDefaults;

   /*
    * UI controls
    */
   private TourChart _tourChart;

   private Button    _chkShowPauseTooltip;

   private Combo     _comboTooltipPosition;

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
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      createActions();

      final Composite ui = createUI(parent);

      fillUI();
      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
         {
            createUI_10_Header(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Label: Slideout title
             */
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Slideout_ChartPausesOptions_Label_Title);

            MTFont.setBannerFont(label);
         }
         {
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.BEGINNING)
                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionRestoreDefaults);

            tbm.update(true);
         }
      }
   }

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Show pause tooltip
             */
            _chkShowPauseTooltip = new Button(container, SWT.CHECK);
            _chkShowPauseTooltip.setText(Messages.Slideout_ChartPausesOptions_Checkbox_IsShowPauseTooltip);
            _chkShowPauseTooltip.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().applyTo(_chkShowPauseTooltip);
         }
         {
            /*
             * Combo: tooltip position
             */
            _comboTooltipPosition = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboTooltipPosition.setVisibleItemCount(20);
            _comboTooltipPosition.setToolTipText(Messages.Slideout_ChartPausesOptions_Combo_TooltipPosition_Tooltip);
            _comboTooltipPosition.addSelectionListener(_defaultSelectionListener);
            _comboTooltipPosition.addFocusListener(_keepOpenListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_comboTooltipPosition);
         }
      }
   }

   private void fillUI() {

      Arrays.asList(ChartPauseToolTip.TOOLTIP_POSITIONS).forEach(tooltipPosition -> _comboTooltipPosition.add(tooltipPosition));
   }

   private void onChangeUI() {

      final boolean isShowPauseTooltip = _chkShowPauseTooltip.getSelection();
      final int tooltipPosition = _comboTooltipPosition.getSelectionIndex();

      _prefStore.setValue(ITourbookPreferences.GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP, isShowPauseTooltip);
      _prefStore.setValue(ITourbookPreferences.GRAPH_PAUSES_TOOLTIP_POSITION, tooltipPosition);

      /*
       * Update chart config
       */
      final TourChartConfiguration tourChartConfiguration = _tourChart.getTourChartConfig();
      tourChartConfiguration.isShowPauseTooltip = isShowPauseTooltip;
      tourChartConfiguration.pauseTooltipPosition = tooltipPosition;

      // update chart with new settings
      _tourChart.updateUI_PausesLayer(true);
   }

   @Override
   public void resetToDefaults() {

      _chkShowPauseTooltip.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP));
      _comboTooltipPosition.select(_prefStore.getDefaultInt(ITourbookPreferences.GRAPH_PAUSES_TOOLTIP_POSITION));

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tourChartConfiguration = _tourChart.getTourChartConfig();

      _chkShowPauseTooltip.setSelection(tourChartConfiguration.isShowPauseTooltip);

      final int pauseTooltipPosition = tourChartConfiguration.pauseTooltipPosition < 0
            ? ChartPauseToolTip.DEFAULT_TOOLTIP_POSITION
            : tourChartConfiguration.pauseTooltipPosition;

      _comboTooltipPosition.select(pauseTooltipPosition);
   }

}
