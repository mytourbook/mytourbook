/*******************************************************************************
 * Copyright (C) 2016, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

public class ChartOptions_Layout {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener_10;

   private String                 _prefStorePrefix;

   /*
    * UI controls
    */
   private Spinner _spinnerYAxisWidth;

   @SuppressWarnings("unused")
   private ChartOptions_Layout() {}

   /**
    * @param prefStorePrefix
    *           Prefix for the grid pref store values.
    */
   public ChartOptions_Layout(final String prefStorePrefix) {

      _prefStorePrefix = prefStorePrefix;
   }

   public void createUI(final Composite parent) {

      initUI();

      createUI_10_Grid(parent);
   }

   private void createUI_10_Grid(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Layout");
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(group);
      GridLayoutFactory.swtDefaults()
            .extendedMargins(0, 0, -5, 0)
            .numColumns(3)
            .applyTo(group);
//      group.setBackground(UI.SYS_COLOR_GREEN);
      {
         {
            /*
             * Y axis width
             */

            final String tooltipText = "Width of the y-axis";

            {
               final Label label = new Label(group, SWT.NONE);
               label.setText("Vertical a&xis width");
               label.setToolTipText(tooltipText);
               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .applyTo(label);
            }
            {
               _spinnerYAxisWidth = new Spinner(group, SWT.BORDER);
               _spinnerYAxisWidth.setMinimum(0);
               _spinnerYAxisWidth.setMaximum(1000);
               _spinnerYAxisWidth.setIncrement(1);
               _spinnerYAxisWidth.setPageIncrement(5);
               _spinnerYAxisWidth.setToolTipText(tooltipText);
               _spinnerYAxisWidth.addMouseWheelListener(_defaultMouseWheelListener_10);
               _spinnerYAxisWidth.addSelectionListener(_defaultSelectionListener);
               GridDataFactory.fillDefaults().applyTo(_spinnerYAxisWidth);
            }
            {
               final Label spacer = UI.createSpacer_Horizontal(group, 1);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(spacer);
            }
         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener_10 = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 5);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      saveState();
   }

   public void resetToDefaults() {

      _spinnerYAxisWidth.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.CHART_Y_AXIS_WIDTH));

      onChangeUI();
   }

   public void restoreState() {

      _spinnerYAxisWidth.setSelection(Util.getPrefixPref_Int(_prefStore, _prefStorePrefix, ITourbookPreferences.CHART_Y_AXIS_WIDTH));
   }

   private void saveState() {

      _prefStore.setValue(_prefStorePrefix + ITourbookPreferences.CHART_Y_AXIS_WIDTH, _spinnerYAxisWidth.getSelection());
   }
}
