/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.statistic.IStatisticOptions;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 */
public class Slideout_YearStatisticOptions extends ToolbarSlideout implements IActionResetToDefault {

   private ActionResetToDefaults _actionRestoreDefaults;

   private ChartOptions_Grid     _gridUI;

   private IStatisticOptions     _yearStatisticOptions;

   /*
    * UI controls
    */

   public Slideout_YearStatisticOptions(final Control ownerControl,
                                        final ToolBar toolBar,
                                        final String prefStoreGridPrefix) {

      super(ownerControl, toolBar);

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
         GridLayoutFactory.fillDefaults()//
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            if (_yearStatisticOptions != null) {
               _yearStatisticOptions.createUI(container);
            }

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
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   @Override
   public void resetToDefaults() {

      _gridUI.resetToDefaults();
      _gridUI.saveState();

      if (_yearStatisticOptions != null) {
         _yearStatisticOptions.resetToDefaults();
         _yearStatisticOptions.saveState();
      }
   }

   private void restoreState() {

      _gridUI.restoreState();

      if (_yearStatisticOptions != null) {
         _yearStatisticOptions.restoreState();
      }
   }

   public void setStatisticOptions(final IStatisticOptions statisticOptions) {

      _yearStatisticOptions = statisticOptions;
   }
}
