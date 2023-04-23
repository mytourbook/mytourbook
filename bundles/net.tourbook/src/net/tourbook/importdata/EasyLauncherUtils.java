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
package net.tourbook.importdata;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TableColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;

/**
 * Provides items for the easy (i)mport (l)auncher (IL)
 */
public class EasyLauncherUtils {

   private static final String   COLUMN_ADJUST_TEMPERATURE = "{0} - {1} {2}";                 //$NON-NLS-1$

   private ColumnManager         _columnManager;
   private TableColumnDefinition _colDef_TourTypeImage;

   private PixelConverter        _pc;

   private final NumberFormat    _nf1                      = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   public void defineAllColumns(final ColumnManager columnManager, final PixelConverter pc) {

      _columnManager = columnManager;
      _pc = pc;

      defineColumn_10_LauncherName();
      defineColumn_20_TourTypeImage();
      defineColumn_30_LastMarkerDistance();
      defineColumn_40_AdjustTemperature();
      defineColumn_50_RetrieveWeatherData();
      defineColumn_80_IsSaveTour();
      defineColumn_82_IsAdjustElevation();
      defineColumn_90_IsShowInDashboard();
      defineColumn_99_Description();
   }

   /**
    * Column: Item name
    */
   private void defineColumn_10_LauncherName() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "launcherName", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Name);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Name);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(17));
      colDef.setColumnWeightData(new ColumnWeightData(17));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((ImportLauncher) cell.getElement()).name);
         }
      });
   }

   /**
    * Column: Tour type
    */
   private void defineColumn_20_TourTypeImage() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
      _colDef_TourTypeImage = colDef;

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourType);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourType);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
      colDef.setColumnWeightData(new ColumnWeightData(12));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new CellLabelProvider() {

         // !!! set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Set last marker
    */
   private void defineColumn_30_LastMarkerDistance() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSetLastMarker", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_LastMarker_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_LastMarker_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_LastMarker_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            if (importLauncher.isSetLastMarker) {

               final double distance = getMarkerDistanceValue(importLauncher);

               cell.setText(_nf1.format(distance));

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Adjust temperature
    */
   private void defineColumn_40_AdjustTemperature() {

      final TableColumnDefinition colDef = new TableColumnDefinition(
            _columnManager,
            "isAdjustTemperature", //$NON-NLS-1$
            SWT.CENTER);

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_AdjustTemperature_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_AdjustTemperature_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_AdjustTemperature_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(15));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            if (importLauncher.isAdjustTemperature) {

               final float temperature = importLauncher.tourAvgTemperature;

               final String logText = NLS.bind(
                     COLUMN_ADJUST_TEMPERATURE,
                     new Object[] {
                           importLauncher.temperatureAdjustmentDuration,
                           (int) (UI.convertTemperatureFromMetric(temperature) + 0.5),
                           UI.UNIT_LABEL_TEMPERATURE });

               cell.setText(logText);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Retrieve weather data
    */
   private void defineColumn_50_RetrieveWeatherData() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isRetrieveWeatherData", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_RetrieveWeatherData_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_RetrieveWeatherData_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_RetrieveWeatherData_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            cell.setText(importLauncher.isRetrieveWeatherData
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Is save tour
    */
   private void defineColumn_80_IsSaveTour() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSaveTour", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Save_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Save_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_SaveTour_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            cell.setText(importLauncher.isSaveTour
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Is adjust elevation
    */
   private void defineColumn_82_IsAdjustElevation() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isAdjustElevation", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_AdjustElevation_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_AdjustElevation_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_ReplaceFirstTimeSliceElevation_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((ImportLauncher) cell.getElement()).isReplaceFirstTimeSliceElevation
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Show in dashboard
    */
   private void defineColumn_90_IsShowInDashboard() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "showInDash", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_ShowInDash_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_ShowInDash_Header);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));
      colDef.setColumnWeightData(new ColumnWeightData(8));

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            cell.setText(
                  ((ImportLauncher) cell.getElement()).isShowInDashboard
                        ? Messages.App_Label_BooleanYes
                        : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Item description
    */
   private void defineColumn_99_Description() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "configDescription", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Description);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Description);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(25));
      colDef.setColumnWeightData(new ColumnWeightData(25));

      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((ImportLauncher) cell.getElement()).description);
         }
      });
   }

   public TableColumnDefinition getColDef_TourTypeImage() {
      return _colDef_TourTypeImage;
   }

   /**
    * @param importLauncher
    * @return Returns from the model the last marker distance value in the current measurment
    *         system.
    */
   private double getMarkerDistanceValue(final ImportLauncher importLauncher) {

      return importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;
   }
}
