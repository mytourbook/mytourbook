/*******************************************************************************
 * Copyright (C) 2023, 2024 Wolfgang Schramm and Contributors
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.tag.TagGroup;
import net.tourbook.tag.TagGroupManager;
import net.tourbook.tour.CadenceMultiplier;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.joda.time.Period;
import org.joda.time.PeriodType;

/**
 * Provides items for the easy (i)mport (l)auncher (IL)
 */
public class EasyLauncherUtils {

   private static final String   COLUMN_ADJUST_TEMPERATURE = "{0} - {1} {2}";                 //$NON-NLS-1$

   private static final char     NL                        = UI.NEW_LINE;

   private ColumnManager         _columnManager;
   private TableColumnDefinition _colDef_TourTypeImage;

   private PixelConverter        _pc;

   private final PeriodType      _durationTemplate         = PeriodType

         .yearMonthDayTime()

         // hide these components
         .withMillisRemoved();

   private final NumberFormat    _nf1                      = NumberFormat.getNumberInstance();
   {
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
   }

   public static void getTagGroupText(final ImportLauncher importLauncher, final StringBuilder sb) {

      final TagGroup tagGroup = TagGroupManager.getTagGroup(importLauncher.tourTagGroupID);
      final Set<TourTag> allTags = TagGroupManager.getTags(importLauncher.tourTagGroupID);

      if (tagGroup == null || allTags == null) {

         return;
      }

      final List<TourTag> sortedTags = new ArrayList<>(allTags);
      Collections.sort(sortedTags);

      final StringBuilder sbTags = new StringBuilder();

      for (final TourTag tourTag : sortedTags) {

         sbTags.append(UI.SPACE3 + tourTag.getTagName() + NL);
      }

      sb.append(NL);
      sb.append(Messages.Import_Data_HTML_TourTags_YES.formatted(tagGroup.name, sbTags.toString()));
   }

   public static String getTourTypeText(final ImportLauncher importLauncher, final String tileName) {

      final StringBuilder ttText = new StringBuilder();
      final Enum<TourTypeConfig> ttConfig = importLauncher.tourTypeConfig;

      if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

         final ArrayList<SpeedTourType> speedTourTypes = importLauncher.speedTourTypes;
         boolean isSpeedAdded = false;

         for (final SpeedTourType speedTT : speedTourTypes) {

            if (isSpeedAdded) {
               ttText.append(NL);
            }

            final long tourTypeId = speedTT.tourTypeId;
            final double avgSpeed = (speedTT.avgSpeed / UI.UNIT_VALUE_DISTANCE) + 0.0001;

            ttText.append(UI.SPACE3);
            ttText.append((int) avgSpeed);
            ttText.append(UI.SPACE);
            ttText.append(UI.UNIT_LABEL_SPEED);
            ttText.append(UI.DASH_WITH_DOUBLE_SPACE);
            ttText.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));

            isSpeedAdded = true;
         }

      } else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

         final TourType oneTourType = importLauncher.oneTourType;
         if (oneTourType != null) {

            final String ttName = oneTourType.getName();
            final CadenceMultiplier ttCadence = importLauncher.oneTourTypeCadence;

            // show this text only when the name is different
            if (!tileName.equals(ttName)) {
               ttText.append(String.format("%s (%s)", ttName, ttCadence.getNlsLabel()));//$NON-NLS-1$
            }
         }
      }

      return ttText.toString();
   }

   private String createLauncherTooltip(final ImportLauncher importLauncher) {

      final StringBuilder sb = new StringBuilder();

      final String tileName = importLauncher.name.trim();
      final String tileDescription = importLauncher.description.trim();
      final String tourTypeText = getTourTypeText(importLauncher, tileName);

      {
         // tour type name

         if (tileName.length() > 0) {

            sb.append(tileName);
            sb.append(NL);
         }
      }
      {
         // tile description

         if (tileDescription.length() > 0) {

            sb.append(NL);
            sb.append(tileDescription);
            sb.append(NL);
         }
      }
      {
         // tour type text

         if (tourTypeText.length() > 0) {

            sb.append(NL);
            sb.append(tourTypeText);
            sb.append(NL);
         }
      }
      {
         // tag group tags

         if (importLauncher.isSetTourTagGroup) {

            getTagGroupText(importLauncher, sb);

         } else {

            sb.append(NL);
            sb.append(Messages.Import_Data_HTML_TourTags_No.formatted());
         }
      }
      {
         // last marker

         final double distance = importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;

         final String distanceValue = _nf1.format(distance) + UI.SPACE1 + UI.UNIT_LABEL_DISTANCE;

         sb.append(NL);

         sb.append(importLauncher.isSetLastMarker
               ? NLS.bind(Messages.Import_Data_HTML_LastMarker_Yes, distanceValue, importLauncher.lastMarkerText)
               : Messages.Import_Data_HTML_LastMarker_No);
      }
      {
         // adjust temperature

         sb.append(NL);

         if (importLauncher.isAdjustTemperature) {

            final float temperature = UI.convertTemperatureFromMetric(importLauncher.tourAvgTemperature);

            final String temperatureText = NLS.bind(Messages.Import_Data_HTML_AdjustTemperature_Yes,
                  new Object[] {
                        getDurationText(importLauncher),
                        _nf1.format(temperature),
                        UI.UNIT_LABEL_TEMPERATURE });

            sb.append(temperatureText);

         } else {

            sb.append(Messages.Import_Data_HTML_AdjustTemperature_No);
         }
      }
      {
         // adjust elevation

         sb.append(NL);

         sb.append(importLauncher.isReplaceFirstTimeSliceElevation
               ? Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_Yes
               : Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_No);
      }
      {
         // set elevation from SRTM

         sb.append(NL);

         sb.append(importLauncher.isReplaceElevationFromSRTM
               ? Messages.Import_Data_HTML_ReplaceElevationFromSRTM_Yes
               : Messages.Import_Data_HTML_ReplaceElevationFromSRTM_No);
      }
      {
         // retrieve weather data

         sb.append(NL);

         sb.append(importLauncher.isRetrieveWeatherData
               ? Messages.Import_Data_HTML_RetrieveWeatherData_Yes
               : Messages.Import_Data_HTML_RetrieveWeatherData_No);
      }
      {
         // retrieve tour location

         sb.append(NL);

         sb.append(importLauncher.isRetrieveTourLocation
               ? Messages.Import_Data_HTML_RetrieveTourLocation_Yes
               : Messages.Import_Data_HTML_RetrieveTourLocation_No);
      }
      {
         // save tour

         sb.append(NL);

         sb.append(importLauncher.isSaveTour
               ? Messages.Import_Data_HTML_SaveTour_Yes
               : Messages.Import_Data_HTML_SaveTour_No);
      }

      return sb.toString();
   }

   public void defineAllColumns(final ColumnManager columnManager, final PixelConverter pc) {

      _columnManager = columnManager;
      _pc = pc;

      defineColumn_10_LauncherName();
      defineColumn_50_03_TourTypeImage();
      defineColumn_50_08_TourTags();
      defineColumn_50_04_LastMarkerDistance();
      defineColumn_50_05_AdjustTemperature();
      defineColumn_50_07_IsAdjustElevation();
      defineColumn_50_50_RetrieveWeatherData();
      defineColumn_50_51_RetrieveTourLocation();
      defineColumn_50_99_IsSaveTour();
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
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((ImportLauncher) cell.getElement()).name);
         }
      });
   }

   /**
    * Column: Tour type
    */
   private void defineColumn_50_03_TourTypeImage() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "colorImage", SWT.LEAD); //$NON-NLS-1$
      _colDef_TourTypeImage = colDef;

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourType);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourType);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));
      colDef.setColumnWeightData(new ColumnWeightData(12));

      colDef.setIsDefaultColumn();
      colDef.setCanModifyVisibility(false);

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         // !!! set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Set last marker
    */
   private void defineColumn_50_04_LastMarkerDistance() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSetLastMarker", SWT.TRAIL); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_LastMarker_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_LastMarker_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_LastMarker_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

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
   private void defineColumn_50_05_AdjustTemperature() {

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
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

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
    * Column: Is adjust elevation
    */
   private void defineColumn_50_07_IsAdjustElevation() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isAdjustElevation", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_AdjustElevation_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_AdjustElevation_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_ReplaceFirstTimeSliceElevation_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         @Override
         public void update(final ViewerCell cell) {

            cell.setText(((ImportLauncher) cell.getElement()).isReplaceFirstTimeSliceElevation
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   private void defineColumn_50_08_TourTags() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "tourTags", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_TourTags_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_TourTags_Header);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            if (importLauncher.isSetTourTagGroup) {

               final TagGroup tagGroup = TagGroupManager.getTagGroup(importLauncher.tourTagGroupID);

               cell.setText(tagGroup == null ? UI.EMPTY_STRING : tagGroup.name);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Retrieve weather data
    */
   private void defineColumn_50_50_RetrieveWeatherData() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isRetrieveWeatherData", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_RetrieveWeatherData_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_RetrieveWeatherData_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_RetrieveWeatherData_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

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
    * Column: Retrieve weather data
    */
   private void defineColumn_50_51_RetrieveTourLocation() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isRetrieveTourLocation", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_RetrieveTourLocation_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_RetrieveTourLocation_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_RetrieveTourLocation_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         @Override
         public void update(final ViewerCell cell) {

            final ImportLauncher importLauncher = (ImportLauncher) cell.getElement();

            cell.setText(importLauncher.isRetrieveTourLocation
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Is save tour
    */
   private void defineColumn_50_99_IsSaveTour() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isSaveTour", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Save_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Save_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Checkbox_SaveTour_Tooltip);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(7));
      colDef.setColumnWeightData(new ColumnWeightData(7));

      colDef.setIsDefaultColumn();

      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

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
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

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
         public String getToolTipText(final Object element) {

            return createLauncherTooltip((ImportLauncher) element);
         }

         @Override
         public void update(final ViewerCell cell) {
            cell.setText(((ImportLauncher) cell.getElement()).description);
         }
      });
   }

   public TableColumnDefinition getColDef_TourTypeImage() {
      return _colDef_TourTypeImage;
   }

   private String getDurationText(final ImportLauncher importLauncher) {

      final int duration = importLauncher.temperatureAdjustmentDuration;
      final Period durationPeriod = new Period(0, duration * 1000L, _durationTemplate);

      return durationPeriod.toString(UI.DEFAULT_DURATION_FORMATTER);
   }

   /**
    * @param importLauncher
    *
    * @return Returns from the model the last marker distance value in the current measurment
    *         system.
    */
   private double getMarkerDistanceValue(final ImportLauncher importLauncher) {

      return importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;
   }
}
