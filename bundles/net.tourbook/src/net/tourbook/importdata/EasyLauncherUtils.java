/*******************************************************************************
 * Copyright (C) 2023, 2026 Wolfgang Schramm and Contributors
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
import net.tourbook.data.Equipment;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.equipment.EquipmentGroup;
import net.tourbook.equipment.EquipmentGroupManager;
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

   public static void createTooltipText_Cadence(final ImportLauncher importLauncher, final StringBuilder sb) {

      final Enum<CadenceConfig> cadConfig = importLauncher.cadenceConfig;

      if (CadenceConfig.CADENCE_CONFIG_BY_SPEED.equals(cadConfig)) {

         final List<SpeedCadence> allCadSpeeds = importLauncher.allCadenceSpeeds;

         final StringBuilder sbSpeed = new StringBuilder();

         for (final SpeedCadence cadSpeed : allCadSpeeds) {

            final double avgSpeed = (cadSpeed.avgSpeed / UI.UNIT_VALUE_DISTANCE) + 0.0001;

            sbSpeed.append(UI.SPACE3);
            sbSpeed.append((int) avgSpeed);
            sbSpeed.append(UI.SPACE);
            sbSpeed.append(UI.UNIT_LABEL_SPEED);
            sbSpeed.append(UI.DASH_WITH_DOUBLE_SPACE);
            sbSpeed.append(cadSpeed.cadenceMultiplier.getNlsLabel());
            sbSpeed.append(NL);
         }

         sb.append("Set cadence . . . By Speed\n\n%s".formatted(sbSpeed.toString()));
         sb.append(NL);

      } else if (CadenceConfig.CADENCE_CONFIG_ONE_FOR_ALL.equals(cadConfig)) {

         final CadenceMultiplier oneCadence = importLauncher.cadenceOne;

         String cadLabel = "NO";

         if (oneCadence != null) {
            cadLabel = oneCadence.getNlsLabel();
         }

         sb.append("Set cadence . . . %s".formatted(cadLabel));
         sb.append(NL);
      }
   }

   public static void createTooltipText_Equipment(final ImportLauncher importLauncher, final StringBuilder sb) {

      final Enum<EquipmentConfig> eqConfig = importLauncher.equipmentConfig;

      if (EquipmentConfig.EQUIPMENT_CONFIG_BY_SPEED == eqConfig) {

         final List<SpeedEquipment> allEqSpeeds = importLauncher.allEquipmentSpeeds;

         final StringBuilder sbSpeed = new StringBuilder();

         for (final SpeedEquipment eqSpeed : allEqSpeeds) {

            final String eqGroupId = eqSpeed.equipmentGroupID;
            final int avgSpeed = Math.round(eqSpeed.avgSpeed / UI.UNIT_VALUE_DISTANCE);

            final EquipmentGroup eqGroup = EquipmentGroupManager.getEquipmentGroup(eqGroupId);
            final Set<Equipment> allEquipment = EquipmentGroupManager.getEquipment(eqGroupId);

            if (eqGroup == null || allEquipment == null) {
               continue;
            }

            sbSpeed.append(UI.SPACE3);
            sbSpeed.append(avgSpeed);
            sbSpeed.append(UI.SPACE);
            sbSpeed.append(UI.UNIT_LABEL_SPEED);
            sbSpeed.append(UI.DASH_WITH_DOUBLE_SPACE);
            sbSpeed.append(eqGroup.name);
            sbSpeed.append(NL);
            sbSpeed.append(NL);
            sbSpeed.append(createTooltipText_Equipment_OneGroup(allEquipment, UI.SPACE8));
            sbSpeed.append(NL);
         }

         sb.append("Set equipment . . . By Speed\n\n%s".formatted(sbSpeed.toString()));

      } else if (EquipmentConfig.EQUIPMENT_CONFIG_ONE_FOR_ALL == eqConfig) {

         final String eqGroupId = importLauncher.equipmentOneGroupID;

         final EquipmentGroup eqGroup = EquipmentGroupManager.getEquipmentGroup(eqGroupId);
         final Set<Equipment> allEquipment = EquipmentGroupManager.getEquipment(eqGroupId);

         if (eqGroup == null || allEquipment == null) {
            return;
         }

         final String eqText = createTooltipText_Equipment_OneGroup(allEquipment, UI.SPACE3);

         sb.append(Messages.Import_Data_HTML_SetEquipment_YES.formatted(eqGroup.name, eqText));
         sb.append(NL);
      }
   }

   private static String createTooltipText_Equipment_OneGroup(final Set<Equipment> allEquipment,
                                                              final String horizontalIndent) {

      final List<Equipment> sortedEquipment = new ArrayList<>(allEquipment);
      Collections.sort(sortedEquipment);

      final StringBuilder sb = new StringBuilder();

      for (final Equipment equipment : sortedEquipment) {

         sb.append(horizontalIndent + equipment.getName() + NL);
      }

      return sb.toString();
   }

   public static void createTooltipText_Tags(final ImportLauncher importLauncher, final StringBuilder sb) {

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

      sb.append(Messages.Import_Data_HTML_SetTourTags_YES.formatted(tagGroup.name, sbTags.toString()));
      sb.append(NL);
   }

   public static void createTooltipText_TourType(final ImportLauncher importLauncher,
                                                 final String tileName,
                                                 final StringBuilder sb) {

      final Enum<TourTypeConfig> ttConfig = importLauncher.tourTypeConfig;

      if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

         final List<SpeedTourType> allSpeedTourTypes = importLauncher.allTourTypeSpeeds;

         final StringBuilder sbSpeed = new StringBuilder();

         for (final SpeedTourType speedTT : allSpeedTourTypes) {

            final long tourTypeId = speedTT.tourTypeId;
            final double avgSpeed = (speedTT.avgSpeed / UI.UNIT_VALUE_DISTANCE) + 0.0001;

            sbSpeed.append(UI.SPACE3);
            sbSpeed.append((int) avgSpeed);
            sbSpeed.append(UI.SPACE);
            sbSpeed.append(UI.UNIT_LABEL_SPEED);
            sbSpeed.append(UI.DASH_WITH_DOUBLE_SPACE);
            sbSpeed.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
            sbSpeed.append(NL);
         }

         sb.append("Set tour type . . . By Speed\n\n%s".formatted(sbSpeed.toString()));
         sb.append(NL);

      } else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

         final TourType oneTourType = importLauncher.oneTourType;

         String ttName = "NO";

         if (oneTourType != null) {
            ttName = oneTourType.getName();
         }

         sb.append("Set tour type . . . %s".formatted(ttName));
         sb.append(NL);
      }
   }

   private String createLauncherTooltip(final ImportLauncher importLauncher) {

      final StringBuilder sb = new StringBuilder();

      final String launcherName = importLauncher.name.trim();
      final String tileDescription = importLauncher.description.trim();

      {
         // launcher name

         if (launcherName.length() > 0) {

            sb.append(launcherName);
            sb.append(NL);
            sb.append(NL);
         }
      }
      {
         // tile description

         if (tileDescription.length() > 0) {

            sb.append(tileDescription);
            sb.append(NL);
            sb.append(NL);
         }
      }
      {
         // tour type text

         if (importLauncher.isSetTourType) {

            createTooltipText_TourType(importLauncher, launcherName, sb);
         }
      }
      {
         // tag group tags

         if (importLauncher.isSetTourTagGroup) {

            createTooltipText_Tags(importLauncher, sb);

         } else {

            sb.append(Messages.Import_Data_HTML_SetTourTags_NO);
            sb.append(NL);
         }
      }
      {
         // equipment

         if (importLauncher.isSetEquipment) {

            createTooltipText_Equipment(importLauncher, sb);

         } else {

            sb.append(Messages.Import_Data_HTML_SetEquipment_NO);
            sb.append(NL);
         }
      }
      {
         // cadence

         if (importLauncher.isSetCadence) {

            createTooltipText_Cadence(importLauncher, sb);

         } else {

            sb.append("Set cadence . . . NO");
            sb.append(NL);
         }
      }
      {
         // last marker

         final double distance = importLauncher.lastMarkerDistance / 1000.0 / UI.UNIT_VALUE_DISTANCE;

         final String distanceValue = _nf1.format(distance) + UI.SPACE1 + UI.UNIT_LABEL_DISTANCE;

         sb.append(importLauncher.isSetLastMarker
               ? NLS.bind(Messages.Import_Data_HTML_LastMarker_Yes, distanceValue, importLauncher.lastMarkerText)
               : Messages.Import_Data_HTML_LastMarker_No);

         sb.append(NL);
      }
      {
         // adjust temperature

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

         sb.append(NL);
      }
      {
         // adjust elevation

         sb.append(importLauncher.isReplaceFirstTimeSliceElevation
               ? Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_Yes
               : Messages.Import_Data_HTML_ReplaceFirstTimeSliceElevation_No);

         sb.append(NL);
      }
      {
         // set elevation from SRTM

         sb.append(importLauncher.isReplaceElevationFromSRTM
               ? Messages.Import_Data_HTML_ReplaceElevationFromSRTM_Yes
               : Messages.Import_Data_HTML_ReplaceElevationFromSRTM_No);

         sb.append(NL);
      }
      {
         // retrieve weather data

         sb.append(importLauncher.isRetrieveWeatherData
               ? Messages.Import_Data_HTML_RetrieveWeatherData_Yes
               : Messages.Import_Data_HTML_RetrieveWeatherData_No);

         sb.append(NL);
      }
      {
         // retrieve tour location

         sb.append(importLauncher.isRetrieveTourLocation
               ? Messages.Import_Data_HTML_RetrieveTourLocation_Yes
               : Messages.Import_Data_HTML_RetrieveTourLocation_No);

         sb.append(NL);
      }
      {
         // save tour

         sb.append(importLauncher.isSaveTour
               ? Messages.Import_Data_HTML_SaveTour_Yes
               : Messages.Import_Data_HTML_SaveTour_No);

         sb.append(NL);
      }

      return sb.toString();
   }

   public void defineAllColumns(final ColumnManager columnManager, final PixelConverter pc) {

      _columnManager = columnManager;
      _pc = pc;

      defineColumn_10_LauncherName();
      defineColumn_50_03_TourTypeImage();
      defineColumn_50_08_TourTags();
      defineColumn_50_09_Equipment();
      defineColumn_50_10_Cadence();
      defineColumn_90_IsShowInDashboard();
      defineColumn_50_041_Remove2ndLastTimeSliceMarker();
      defineColumn_50_042_LastMarkerDistance();
      defineColumn_50_05_AdjustTemperature();
      defineColumn_50_07_IsAdjustElevation();
      defineColumn_50_50_RetrieveWeatherData();
      defineColumn_50_51_RetrieveTourLocation();
      defineColumn_50_99_IsSaveTour();
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

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "tourTypeImage", SWT.LEAD); //$NON-NLS-1$
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

   private void defineColumn_50_041_Remove2ndLastTimeSliceMarker() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isRemove2ndLastTimeSliceMarker", SWT.CENTER); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Remove2ndLastTimeSliceMarker_Label);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Remove2ndLastTimeSliceMarker_Header);
      colDef.setColumnHeaderToolTipText(Messages.Dialog_ImportConfig_Column_Remove2ndLastTimeSliceMarker_Label);

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

            cell.setText(importLauncher.isRemove2ndLastTimeSliceMarker
                  ? Messages.App_Label_BooleanYes
                  : UI.EMPTY_STRING);
         }
      });
   }

   /**
    * Column: Set last marker
    */
   private void defineColumn_50_042_LastMarkerDistance() {

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

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "isAdjustTemperature", SWT.CENTER); //$NON-NLS-1$

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

   private void defineColumn_50_09_Equipment() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "equipment", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel(Messages.Dialog_ImportConfig_Column_Equipment);
      colDef.setColumnHeaderText(Messages.Dialog_ImportConfig_Column_Equipment);

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

            if (importLauncher.isSetEquipment) {

               final Enum<EquipmentConfig> eqConfig = importLauncher.equipmentConfig;

               String eqText = UI.EMPTY_STRING;

               if (EquipmentConfig.EQUIPMENT_CONFIG_ONE_FOR_ALL == eqConfig) {

                  final EquipmentGroup eqGroup = EquipmentGroupManager.getEquipmentGroup(importLauncher.equipmentOneGroupID);

                  eqText = eqGroup == null ? UI.EMPTY_STRING : eqGroup.name;

               } else if (EquipmentConfig.EQUIPMENT_CONFIG_BY_SPEED == eqConfig) {

                  final List<SpeedEquipment> allEqSpeeds = importLauncher.allEquipmentSpeeds;

                  eqText = "Speed #" + allEqSpeeds.size();
               }

               cell.setText(eqText);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   private void defineColumn_50_10_Cadence() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, "cadence", SWT.LEAD); //$NON-NLS-1$

      colDef.setColumnLabel("Cadence");
      colDef.setColumnHeaderText("Cadence");

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

            if (importLauncher.isSetCadence) {

               final Enum<CadenceConfig> cadConfig = importLauncher.cadenceConfig;

               String eqText = UI.EMPTY_STRING;

               if (CadenceConfig.CADENCE_CONFIG_ONE_FOR_ALL == cadConfig) {

                  final CadenceMultiplier cadenceOne = importLauncher.cadenceOne;

                  if (cadenceOne != null) {

                     eqText = cadenceOne.getNlsLabel();
                  }

               } else if (CadenceConfig.CADENCE_CONFIG_BY_SPEED == cadConfig) {

                  final List<SpeedCadence> allCadSpeeds = importLauncher.allCadenceSpeeds;

                  eqText = "Speed #" + allCadSpeeds.size();
               }

               cell.setText(eqText);

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
