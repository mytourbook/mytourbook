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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ChartType;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

public class TourChartPropertyView extends ViewPart {

   public static final String     ID         = "net.tourbook.views.TourChartPropertyView";      //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private SelectionAdapter       _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button _rdoChartType_Bar;
   private Button _rdoChartType_Dot;
   private Button _rdoChartType_Line;

   @Override
   public void createPartControl(final Composite parent) {

      initUI();
      createUI(parent);
      restoreState();
   }

   private void createUI(final Composite parent) {

      final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      scrolledContainer.setExpandVertical(true);
      scrolledContainer.setExpandHorizontal(true);

      final Composite container = new Composite(scrolledContainer, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_10_ChartType(container);
      }

      /*
       * setup scrolled container
       */
      scrolledContainer.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            scrolledContainer.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         }
      });

      scrolledContainer.setContent(container);
   }

   private void createUI_10_ChartType(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(2).margins(5, 5).applyTo(container);
      {
         {
            /*
             * chart type
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.TourChart_Property_label_chart_type);
         }

         final Composite group = new Composite(container, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(group);
         {
            {
               // radio: line chart
               _rdoChartType_Line = new Button(group, SWT.RADIO);
               _rdoChartType_Line.setText(Messages.TourChart_Property_chart_type_line);
               _rdoChartType_Line.addSelectionListener(_defaultSelectionListener);
            }
            {
               // radio: bar chart
               _rdoChartType_Bar = new Button(group, SWT.RADIO);
               _rdoChartType_Bar.setText(Messages.TourChart_Property_chart_type_bar);
               _rdoChartType_Bar.addSelectionListener(_defaultSelectionListener);
            }
            {
               // radio: dot
               _rdoChartType_Dot = new Button(group, SWT.RADIO);
               _rdoChartType_Dot.setText(Messages.TourChart_Property_ChartType_Dot);
               _rdoChartType_Dot.addSelectionListener(_defaultSelectionListener);
            }
         }
      }
   }

   private void enableControls() {

   }

   private void initUI() {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent event) {
            onChangeProperty();
         }
      };
   }

   /**
    * Property was changed, fire a property change event
    */
   private void onChangeProperty() {

      enableControls();

      saveState();

      TourManager.getInstance().removeAllToursFromCache();

      // fire unique event for all changes
      TourManager.fireEvent(TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED);
   }

   private void restoreState() {

      /*
       * Chart type
       */
      ChartType chartType;
      final String chartTypeName = _prefStore.getString(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE);
      try {
         chartType = ChartType.valueOf(ChartType.class, chartTypeName);
      } catch (final Exception e) {
         // set default value
         chartType = ChartType.LINE;
      }

      switch (chartType) {
      case BAR:
         _rdoChartType_Bar.setSelection(true);
         break;

      case DOT:
         _rdoChartType_Dot.setSelection(true);
         break;

      case LINE:
      default:
         _rdoChartType_Line.setSelection(true);
         break;
      }

      enableControls();
   }

   /**
    * Update new values in the pref store
    */
   private void saveState() {

      /*
       * Chart type
       */
      final ChartType chartType = _rdoChartType_Line.getSelection() ? ChartType.LINE
            : _rdoChartType_Dot.getSelection() ? ChartType.DOT
                  : ChartType.BAR;

      _prefStore.setValue(ITourbookPreferences.GRAPH_PROPERTY_CHARTTYPE, chartType.name());
   }

   @Override
   public void setFocus() {

      _rdoChartType_Line.setFocus();
   }

}
