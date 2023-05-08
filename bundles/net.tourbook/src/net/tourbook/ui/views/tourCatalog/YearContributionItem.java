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
package net.tourbook.ui.views.tourCatalog;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Spinner;

public class YearContributionItem extends ControlContribution {

   private static final String        ID       = "net.tourbook.ui.views.tourCatalog.YearContributionItem"; //$NON-NLS-1$

   private static final boolean       IS_OSX   = UI.IS_OSX;
   private static final boolean       IS_LINUX = UI.IS_LINUX;

   private RefTour_YearStatistic_View _refTour_YearStatistic_View;

   /*
    * UI controls
    */
   Combo   comboLastVisibleYear;
   Spinner spinnerNumberOfVisibleYears;

   protected YearContributionItem(final RefTour_YearStatistic_View refTour_YearStatistic_View) {

      super(ID);

      _refTour_YearStatistic_View = refTour_YearStatistic_View;
   }

   @Override
   protected Control createControl(final Composite parent) {

      final PixelConverter pc = new PixelConverter(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .extendedMargins(0, 5, 0, 0)
            .applyTo(container);
      {
         {
            /*
             * Last visible year
             */
            comboLastVisibleYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
            comboLastVisibleYear.setToolTipText(Messages.Year_Statistic_Combo_LastYears_Tooltip);
            comboLastVisibleYear.setVisibleItemCount(50);

            comboLastVisibleYear.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onSelectLastVisibleYear()));

            comboLastVisibleYear.addTraverseListener(traverseEvent -> {
               if (traverseEvent.detail == SWT.TRAVERSE_RETURN) {
                  onSelectLastVisibleYear();
               }
            });

            GridDataFactory.fillDefaults()
                  .hint(pc.convertWidthInCharsToPixels(IS_OSX ? 12 : IS_LINUX ? 12 : 5), SWT.DEFAULT)
                  .applyTo(comboLastVisibleYear);
         }
         {
            /*
             * Number of visible years
             */
            spinnerNumberOfVisibleYears = new Spinner(container, SWT.BORDER);
            spinnerNumberOfVisibleYears.setMinimum(1);
            spinnerNumberOfVisibleYears.setMaximum(100);
            spinnerNumberOfVisibleYears.setIncrement(1);
            spinnerNumberOfVisibleYears.setPageIncrement(5);
            spinnerNumberOfVisibleYears.setToolTipText(Messages.Year_Statistic_Combo_NumberOfYears_Tooltip);

            spinnerNumberOfVisibleYears.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onSelectNumberOfVisibleYears()));

            spinnerNumberOfVisibleYears.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 1);
               onSelectNumberOfVisibleYears();
            });
         }
      }

      return container;

   }

   private void onSelectLastVisibleYear() {

      _refTour_YearStatistic_View.onSelect_LastVisibleYear();
   }

   private void onSelectNumberOfVisibleYears() {

      _refTour_YearStatistic_View.onSelect_NumberOfVisibleYears();
   }

}
