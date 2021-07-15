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

import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * This class generates and contains the labels for one row in the view
 */
class GraphInfo {

   private final TourChartAnalyzerView _tourChartAnalyzerView;

   double                              prevLeftValue  = Double.MIN_VALUE;
   double                              prevRightValue = Double.MIN_VALUE;

   double                              prevMinValue   = Double.MIN_VALUE;
   double                              prevMaxValue   = Double.MIN_VALUE;

   double                              prevAvgValue   = Double.MIN_VALUE;
   double                              prevDiffValue  = Double.MIN_VALUE;

   ChartDataSerie                      chartData;
   private int                         _columnSpacing;

   private final Color                 _bgColorData;
   Color                               valueForegroundColor;

   /*
    * UI controls
    */
   private final Composite _parent;

   Label                   labelAvg;
   Label                   labelDiff;
   Label                   labelLeft;
   Label                   labelMax;
   Label                   labelMin;
   Label                   labelRight;

   Label                   labelValueUnit;
   Label                   labelValueLabel;

   GraphInfo(final TourChartAnalyzerView tourChartAnalyzerView, final ChartDataSerie chartData, final Composite parent) {

      _tourChartAnalyzerView = tourChartAnalyzerView;
      _columnSpacing = _tourChartAnalyzerView._columnSpacing;

      _parent = parent;
      _bgColorData = Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      this.chartData = chartData;

      valueForegroundColor = _tourChartAnalyzerView.getColor(chartData.getRgbGraph_Text());
   }

   void createUI_Info_10_Left() {

      labelLeft = new Label(_parent, SWT.TRAIL);
      labelLeft.setForeground(valueForegroundColor);
      labelLeft.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelLeft);
   }

   void createUI_Info_20_Right() {

      labelRight = new Label(_parent, SWT.TRAIL);
      labelRight.setForeground(valueForegroundColor);
      labelRight.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelRight);
   }

   void createUI_Info_30_Min() {

      labelMin = new Label(_parent, SWT.TRAIL);
      labelMin.setForeground(valueForegroundColor);
      labelMin.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelMin);
   }

   void createUI_Info_40_Max() {

      labelMax = new Label(_parent, SWT.TRAIL);
      labelMax.setForeground(valueForegroundColor);
      labelMax.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelMax);
   }

   void createUI_Info_50_Diff() {

      labelDiff = new Label(_parent, SWT.TRAIL);
      labelDiff.setForeground(valueForegroundColor);
      labelDiff.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelDiff);
   }

   void createUI_Info_60_Avg() {

      labelAvg = new Label(_parent, SWT.TRAIL);
      labelAvg.setForeground(valueForegroundColor);
      labelAvg.setBackground(_bgColorData);

      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .applyTo(labelAvg);
   }

   void createUI_Value_Label() {

      String labelText;
      if (chartData instanceof ChartDataYSerie) {
         labelText = ((ChartDataYSerie) chartData).getYTitle();
      } else {
         labelText = chartData.getLabel();
      }

      labelValueLabel = new Label(_parent, SWT.NONE);
      labelValueLabel.setText(UI.SPACE + labelText);
      labelValueLabel.setForeground(valueForegroundColor);
      labelValueLabel.setBackground(_bgColorData);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(labelValueLabel);
   }

   void createUI_Value_Unit() {

      String toolTip;
      if (chartData instanceof ChartDataYSerie) {
         toolTip = ((ChartDataYSerie) chartData).getYTitle();
      } else {
         toolTip = chartData.getLabel();
      }

      final String unitLabel = chartData.getUnitLabel();

      labelValueUnit = new Label(_parent, SWT.NONE);
      labelValueUnit.setText(unitLabel.length() > 0 ? UI.SPACE + unitLabel : unitLabel);
      labelValueUnit.setToolTipText(toolTip);
      labelValueUnit.setForeground(valueForegroundColor);
      labelValueUnit.setBackground(_bgColorData);
      GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .indent(_columnSpacing, 0)
            .grab(true, false)
            .applyTo(labelValueUnit);
   }
}
