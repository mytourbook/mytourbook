/*******************************************************************************
 * Copyright (C) 2020, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.NatTable_LabelProvider;
import net.tourbook.common.util.NatTable_LabelProvider_WithTourTooltip;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.TreeColumnDefinition;
import net.tourbook.data.TourData;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.TreeColumnFactory;
import net.tourbook.ui.views.TourInfoToolTipCellLabelProvider;
import net.tourbook.ui.views.TourInfoToolTipStyledCellLabelProvider;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Image;

public class TourBook_ColumnFactory {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private static final NumberFormat     _nf0;
   private static final NumberFormat     _nf1;
   private static final NumberFormat     _nf2;

   static {

      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2 = NumberFormat.getNumberInstance();
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   private ColumnManager        _columnManager_NatTable;
   private ColumnManager        _columnManager_Tree;

   private TreeColumnDefinition _colDef_TimeZoneOffset_Tree;

   private boolean              _isShowSummaryRow;

   private boolean              _isShowToolTipIn_Date;
   private boolean              _isShowToolTipIn_Tags;
   private boolean              _isShowToolTipIn_Time;
   private boolean              _isShowToolTipIn_Title;
   private boolean              _isShowToolTipIn_WeekDay;

   private PixelConverter       _pc;

   public TourBook_ColumnFactory(final ColumnManager columnManager_NatTable, final ColumnManager columnManager_Tree, final PixelConverter pc) {

      _columnManager_NatTable = columnManager_NatTable;
      _columnManager_Tree = columnManager_Tree;
      _pc = pc;
   }

   /**
    * Defines all columns for the table viewer in the column manager, the sequence defines the
    * default columns
    * <p>
    * All columns <b>MUST</b> also be defined in {@link CSVExport}
    *
    * @param parent
    */
   void defineAllColumns() {

      defineColumn_0_RowNumbering();

      // Time
      defineColumn_1_Date();
      defineColumn_Time_WeekDay();
      defineColumn_Time_TourStartTime();
      defineColumn_Time_TimeZoneDifference();
      defineColumn_Time_TimeZone();
      defineColumn_Time_ElapsedTime();
      defineColumn_Time_RecordedTime();
      defineColumn_Time_PausedTime();
      defineColumn_Time_MovingTime();
      defineColumn_Time_BreakTime();
      defineColumn_Time_BreakTime_Relative();
      defineColumn_Time_WeekNo();
      defineColumn_Time_WeekYear();

      // Tour
      defineColumn_Tour_TypeImage();
      defineColumn_Tour_TypeText();
      defineColumn_Tour_Title();
      defineColumn_Tour_Marker();
      defineColumn_Tour_Photos();
      defineColumn_Tour_Tags();
      defineColumn_Tour_Location_Start();
      defineColumn_Tour_Location_End();
//    defineColumn_Tour_TagIds();            // for debugging

      // Motion / Bewegung
      defineColumn_Motion_Distance();
      defineColumn_Motion_MaxSpeed();
      defineColumn_Motion_AvgSpeed();
      defineColumn_Motion_AvgPace();

      // Elevation
      defineColumn_Elevation_Up();
      defineColumn_Elevation_Down();
      defineColumn_Elevation_Max();
      defineColumn_Elevation_AvgChange();

      // Weather
      defineColumn_Weather_Clouds();
      defineColumn_Weather_Temperature_Avg();
      defineColumn_Weather_Temperature_Min();
      defineColumn_Weather_Temperature_Max();
      defineColumn_Weather_WindSpeed();
      defineColumn_Weather_WindDirection();

      // Body
      defineColumn_Body_Calories();
      defineColumn_Body_RestPulse();
      defineColumn_Body_MaxPulse();
      defineColumn_Body_AvgPulse();
      defineColumn_Body_Weight();
      defineColumn_Body_Person();

      // Power - Leistung
      defineColumn_Power_Avg();
      defineColumn_Power_Max();
      defineColumn_Power_Normalized();
      defineColumn_Power_TotalWork();

      // Powertrain - Antrieb/Pedal
      defineColumn_Powertrain_AvgCadence();
      defineColumn_Powertrain_SlowVsFastCadencePercentage();
      defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter();
      defineColumn_Powertrain_CadenceMultiplier();
      defineColumn_Powertrain_Gear_FrontShiftCount();
      defineColumn_Powertrain_Gear_RearShiftCount();
      defineColumn_Powertrain_AvgLeftPedalSmoothness();
      defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
      defineColumn_Powertrain_AvgRightPedalSmoothness();
      defineColumn_Powertrain_AvgRightTorqueEffectiveness();
      defineColumn_Powertrain_PedalLeftRightBalance();

      // Training - Trainingsanalyse
      defineColumn_Training_FTP();
      defineColumn_Training_PowerToWeightRatio();
      defineColumn_Training_IntensityFactor();
      defineColumn_Training_StressScore();
      defineColumn_Training_TrainingEffect();
      defineColumn_Training_TrainingEffect_Anaerobic();
      defineColumn_Training_TrainingPerformance();

      // Running dynamics
      defineColumn_RunDyn_StanceTime_Min();
      defineColumn_RunDyn_StanceTime_Max();
      defineColumn_RunDyn_StanceTime_Avg();

      defineColumn_RunDyn_StanceTimeBalance_Min();
      defineColumn_RunDyn_StanceTimeBalance_Max();
      defineColumn_RunDyn_StanceTimeBalance_Avg();

      defineColumn_RunDyn_StepLength_Min();
      defineColumn_RunDyn_StepLength_Max();
      defineColumn_RunDyn_StepLength_Avg();

      defineColumn_RunDyn_VerticalOscillation_Min();
      defineColumn_RunDyn_VerticalOscillation_Max();
      defineColumn_RunDyn_VerticalOscillation_Avg();

      defineColumn_RunDyn_VerticalRatio_Min();
      defineColumn_RunDyn_VerticalRatio_Max();
      defineColumn_RunDyn_VerticalRatio_Avg();

      // Surfing
      defineColumn_Surfing_NumberOfEvents();
      defineColumn_Surfing_MinSpeed_StartStop();
      defineColumn_Surfing_MinSpeed_Surfing();
      defineColumn_Surfing_MinTimeDuration();
      defineColumn_Surfing_MinDistance();

      // Device
      defineColumn_Device_Name();
      defineColumn_Device_Distance();
      defineColumn_Device_BatteryPercentage_Start();
      defineColumn_Device_BatteryPercentage_End();

      // Data
      defineColumn_Data_DPTolerance();
      defineColumn_Data_ImportFilePath();
      defineColumn_Data_ImportFileName();
      defineColumn_Data_TimeInterval();
      defineColumn_Data_NumTimeSlices();
   }

   /**
    * Column: #
    * <p>
    * This is only used for the table view.
    */
   private void defineColumn_0_RowNumbering() {

//      {
//         // Column: 1st column will be hidden because the alignment for the first column is always to the left
//
//         final ColumnDefinition colDef = TableColumnFactory.DATA_FIRST_COLUMN.createColumn(_columnManager_Table, _pc);
//
//         colDef.setIsDefaultColumn();
//         colDef.setCanModifyVisibility(true);
//         colDef.setIsColumnMoveable(true);
//         colDef.setHideColumn();
//         colDef.setLabelProvider(new CellLabelProvider() {
//            @Override
//            public void update(final ViewerCell cell) {}
//         });
//      }

//      {
//         // Column: #
//
//         final ColumnDefinition colDef = TableColumnFactory.DATA_SEQUENCE.createColumn(_columnManager_Table, _pc);
//
//         colDef.setIsDefaultColumn();
//         colDef.setCanModifyVisibility(true);
//         colDef.setIsColumnMoveable(true);
//         colDef.setLabelProvider(new CellLabelProvider() {
//            @Override
//            public void update(final ViewerCell cell) {
//
//               final int tourSequence = ((TVITourBookItem) cell.getElement()).col_Sequence;
//
//               cell.setText(Integer.toString(tourSequence));
//            }
//         });
//      }
   }

   /**
    * Column: Date
    */
   private void defineColumn_1_Date() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_DATE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setCanModifyVisibility(false);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider_WithTourTooltip() {

         @Override
         public String getValueText(final Object item) {

            final TVITourBookTour tourItem = (TVITourBookTour) (TVITourBookItem) item;

            // show full date
            return tourItem.colDateTime_Text;
         }

         @Override
         public boolean isShowTooltip() {
            return _isShowToolTipIn_Date;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_DATE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setCanModifyVisibility(false);
      colDef_Tree.setLabelProvider(new TourInfoToolTipStyledCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Date == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookItem) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookItem tviItem = (TVITourBookItem) element;

            if (element instanceof TVITourBookTour) {

               // tour item

               final TVITourBookTour tourItem = (TVITourBookTour) tviItem;

               // show day only
               cell.setText(tourItem.treeColumn);

            } else {

               // year/month or week item

               final StyledString styledString = new StyledString();

               boolean isShowSummaryRow = false;
               if (element instanceof TVITourBookYear && _isShowSummaryRow) {
                  isShowSummaryRow = ((TVITourBookYear) element).isRowSummary;
               }

               if (isShowSummaryRow) {

                  // show summary row

                  styledString.append(Messages.Tour_Book_Label_Total);
               } else {
                  styledString.append(tviItem.treeColumn);
               }

               styledString.append(UI.SPACE3);
               styledString.append(Long.toString(tviItem.colCounter), StyledString.QUALIFIER_STYLER);

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Body - Avg pulse
    */
   private void defineColumn_Body_AvgPulse() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_AVG_PULSE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgPulse;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PULSE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgPulse;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Calories
    */
   private void defineColumn_Body_Calories() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_CALORIES.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colCalories / 1000.0;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_CALORIES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colCalories / 1000.0;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Max pulse
    */
   private void defineColumn_Body_MaxPulse() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colMaxPulse;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PULSE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colMaxPulse;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Person
    */
   private void defineColumn_Body_Person() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_PERSON.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbPersonId = ((TVITourBookTour) element).colPersonId;

            return PersonManager.getPersonName(dbPersonId);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_PERSON.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long dbPersonId = ((TVITourBookTour) element).colPersonId;

               cell.setText(PersonManager.getPersonName(dbPersonId));
            }
         }
      });
   }

   /**
    * Column: Body - Rest pulse
    */
   private void defineColumn_Body_RestPulse() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_RESTPULSE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int restPulse = ((TVITourBookItem) element).colRestPulse;

            if (restPulse == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(restPulse);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_RESTPULSE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int restPulse = ((TVITourBookItem) element).colRestPulse;

            if (restPulse == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(restPulse));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Body - Body weight
    */
   private void defineColumn_Body_Weight() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.BODY_WEIGHT.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double weight = UI.convertBodyWeightFromMetric(((TVITourBookItem) element).colBodyWeight);

            if (weight == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(weight);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.BODY_WEIGHT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double weight = UI.convertBodyWeightFromMetric(((TVITourBookItem) element).colBodyWeight);

            if (weight == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(weight));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Data - DP tolerance
    */
   private void defineColumn_Data_DPTolerance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DATA_DP_TOLERANCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int dpTolerance = ((TVITourBookItem) element).colDPTolerance;

            if (dpTolerance == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(dpTolerance / 10.0);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_DP_TOLERANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int dpTolerance = ((TVITourBookItem) element).colDPTolerance;

            if (dpTolerance == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(dpTolerance / 10.0));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Data - Import filename
    */
   private void defineColumn_Data_ImportFileName() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DATA_IMPORT_FILE_NAME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).col_ImportFileName;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_IMPORT_FILE_NAME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).col_ImportFileName);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Data - Import filepath
    */
   private void defineColumn_Data_ImportFilePath() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DATA_IMPORT_FILE_PATH.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).col_ImportFilePath;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_IMPORT_FILE_PATH.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).col_ImportFilePath);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Data - Number of time slices
    */
   private void defineColumn_Data_NumTimeSlices() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DATA_NUM_TIME_SLICES.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colNumberOfTimeSlices;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_NUM_TIME_SLICES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfTimeSlices;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Data - Time interval
    */

   private void defineColumn_Data_TimeInterval() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
            if (dbTimeInterval == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Long.toString(dbTimeInterval);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DATA_TIME_INTERVAL.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final short dbTimeInterval = ((TVITourBookTour) element).getColumnTimeInterval();
               if (dbTimeInterval == 0) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(Long.toString(dbTimeInterval));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Device - Battery % end
    */
   private void defineColumn_Device_BatteryPercentage_End() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DEVICE_BATTERY_SOC_END.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).colBatterySoC_End;
            if (value == -1) {
               return UI.EMPTY_STRING;
            } else {
               return colDef_NatTable.printValue_0(value);
            }

         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_BATTERY_SOC_END.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVITourBookItem) element).colBatterySoC_End;

            if (value == -1) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               colDef_Tree.printValue_0(cell, value);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Device - Battery % start
    */
   private void defineColumn_Device_BatteryPercentage_Start() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DEVICE_BATTERY_SOC_START.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).colBatterySoC_Start;

            if (value == -1) {
               return UI.EMPTY_STRING;
            } else {
               return colDef_NatTable.printValue_0(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_BATTERY_SOC_START.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long value = ((TVITourBookItem) element).colBatterySoC_Start;

            if (value == -1) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               colDef_Tree.printValue_0(cell, value);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Device - Device distance
    */
   private void defineColumn_Device_Distance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();
            final double value = dbStartDistance / UI.UNIT_VALUE_DISTANCE;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_DISTANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long dbStartDistance = ((TVITourBookTour) element).getColumnStartDistance();
               final double value = dbStartDistance / UI.UNIT_VALUE_DISTANCE;

               colDef_Tree.printValue_0(cell, value);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Device - Device name
    */
   private void defineColumn_Device_Name() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.DEVICE_NAME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String dbValue = ((TVITourBookItem) element).colDeviceName;

            if (dbValue == null) {
               return UI.EMPTY_STRING;
            } else {
               return dbValue;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.DEVICE_NAME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String dbValue = ((TVITourBookItem) element).colDeviceName;

            if (dbValue == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(dbValue);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Average elevation change (m/km or ft/mi)
    */
   private void defineColumn_Elevation_AvgChange() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.ALTITUDE_AVG_CHANGE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final float dbAvgAltitudeChange = UI.convertAverageElevationChangeFromMetric(((TVITourBookItem) element).colAltitude_AvgChange);

            return colDef_NatTable.printValue_0(dbAvgAltitudeChange);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_AVG_CHANGE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float dbAvgAltitudeChange = UI.convertAverageElevationChangeFromMetric(((TVITourBookItem) element).colAltitude_AvgChange);

            colDef_Tree.printValue_0(cell, dbAvgAltitudeChange);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Elevation down (m)
    */
   private void defineColumn_Elevation_Down() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_DOWN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;
            final double value = -dbAltitudeDown / UI.UNIT_VALUE_ELEVATION;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_DOWN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double dbAltitudeDown = ((TVITourBookItem) element).colAltitudeDown;
            final double value = -dbAltitudeDown / UI.UNIT_VALUE_ELEVATION;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Max elevation
    */
   private void defineColumn_Elevation_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.ALTITUDE_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;
            final double value = dbMaxAltitude / UI.UNIT_VALUE_ELEVATION;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbMaxAltitude = ((TVITourBookItem) element).colMaxAltitude;
            final double value = dbMaxAltitude / UI.UNIT_VALUE_ELEVATION;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Elevation - Elevation up (m)
    */
   private void defineColumn_Elevation_Up() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.ALTITUDE_SUMMARIZED_BORDER_UP.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;
            final double value = dbAltitudeUp / UI.UNIT_VALUE_ELEVATION;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_UP.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final long dbAltitudeUp = ((TVITourBookItem) element).colAltitudeUp;
            final double value = dbAltitudeUp / UI.UNIT_VALUE_ELEVATION;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Avg pace min/km - min/mi
    */
   private void defineColumn_Motion_AvgPace() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double pace = ((TVITourBookItem) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               return UI.EMPTY_STRING;
            } else {
               return UI.format_mm_ss((long) pace);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_AVG_PACE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double pace = ((TVITourBookItem) element).colAvgPace * UI.UNIT_VALUE_DISTANCE;

            if (pace == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(UI.format_mm_ss((long) pace));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Avg speed km/h - mph
    */
   private void defineColumn_Motion_AvgSpeed() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_AVG_SPEED.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Distance (km/miles)
    */
   private void defineColumn_Motion_Distance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.MOTION_DISTANCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTourDistance
                  / 1000.0
                  / UI.UNIT_VALUE_DISTANCE;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_DISTANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTourDistance
                  / 1000.0
                  / UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Motion - Max speed
    */
   private void defineColumn_Motion_MaxSpeed() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.MOTION_MAX_SPEED.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colMaxSpeed / UI.UNIT_VALUE_DISTANCE;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Avg power
    */
   private void defineColumn_Power_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWER_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_Avg;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Max power
    */
   private void defineColumn_Power_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWER_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_Max;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Normalized power
    */
   private void defineColumn_Power_Normalized() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWER_NORMALIZED.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_Normalized;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_NORMALIZED.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_Normalized;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Power - Total work
    */
   private void defineColumn_Power_TotalWork() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWER_TOTAL_WORK.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_TotalWork / 1000_000.0;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWER_TOTAL_WORK.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_TotalWork / 1000_000.0;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Avg cadence
    */
   private void defineColumn_Powertrain_AvgCadence() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colAvgCadence;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_CADENCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colAvgCadence;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgLeftPedalSmoothness
    */
   private void defineColumn_Powertrain_AvgLeftPedalSmoothness() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS.createColumn(_columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgLeftPedalSmoothness;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftPedalSmoothness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgLeftTorqueEffectiveness
    */
   private void defineColumn_Powertrain_AvgLeftTorqueEffectiveness() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgLeftTorqueEffectiveness;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgLeftTorqueEffectiveness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgRightPedalSmoothness
    */
   private void defineColumn_Powertrain_AvgRightPedalSmoothness() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS.createColumn(_columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgRightPedalSmoothness;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightPedalSmoothness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - AvgRightTorqueEffectiveness
    */
   private void defineColumn_Powertrain_AvgRightTorqueEffectiveness() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colPower_AvgRightTorqueEffectiveness;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colPower_AvgRightTorqueEffectiveness;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Cadence multiplier
    */
   private void defineColumn_Powertrain_CadenceMultiplier() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double dbCadenceMultiplier = ((TVITourBookItem) element).colCadenceMultiplier;

            if (dbCadenceMultiplier == 0) {
               return UI.EMPTY_STRING;
            } else {
               return _nf1.format(dbCadenceMultiplier);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_CADENCE_MULTIPLIER.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double dbCadenceMultiplier = ((TVITourBookItem) element).colCadenceMultiplier;

            if (dbCadenceMultiplier == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(_nf1.format(dbCadenceMultiplier));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Front shift count.
    */
   private void defineColumn_Powertrain_Gear_FrontShiftCount() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colFrontShiftCount;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_GEAR_FRONT_SHIFT_COUNT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colFrontShiftCount;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Rear shift count.
    */
   private void defineColumn_Powertrain_Gear_RearShiftCount() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colRearShiftCount;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_GEAR_REAR_SHIFT_COUNT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colRearShiftCount;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Pedal left/right balance
    */

   private void defineColumn_Powertrain_PedalLeftRightBalance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colPower_PedalLeftRightBalance;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colPower_PedalLeftRightBalance;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Slow vs fast cadence Percentage
    */
   private void defineColumn_Powertrain_SlowVsFastCadencePercentage() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES.createColumn(
            _columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String value = ((TVITourBookItem) element).colSlowVsFastCadence;

            return value;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String value = ((TVITourBookItem) element).colSlowVsFastCadence;

            cell.setText(value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Powertrain - Cadence zones delimiter value
    */
   private void defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER.createColumn(
            _columnManager_NatTable,
            _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int value = ((TVITourBookItem) element).colCadenceZonesDelimiter;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER.createColumn(_columnManager_Tree,
            _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int value = ((TVITourBookItem) element).colCadenceZonesDelimiter;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time max
    */
   private void defineColumn_RunDyn_StanceTime_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Avg;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Avg;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time max
    */
   private void defineColumn_RunDyn_StanceTime_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Max;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Max;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time min
    */
   private void defineColumn_RunDyn_StanceTime_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Min;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTime_Min;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance avg
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Avg;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance max
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Max;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Stance time balance min
    */
   private void defineColumn_RunDyn_StanceTimeBalance_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Min;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STANCE_TIME_BALANCE_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StanceTimeBalance_Min;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length avg
    */
   private void defineColumn_RunDyn_StepLength_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STEP_LENGTH_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Avg
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Avg
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length max
    */
   private void defineColumn_RunDyn_StepLength_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STEP_LENGTH_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Max
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Max
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Step length min
    */
   private void defineColumn_RunDyn_StepLength_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_STEP_LENGTH_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Min
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_STEP_LENGTH_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_StepLength_Min
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation avg
    */
   private void defineColumn_RunDyn_VerticalOscillation_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Avg
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Avg
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation max
    */
   private void defineColumn_RunDyn_VerticalOscillation_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Max
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Max
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical oscillation min
    */
   private void defineColumn_RunDyn_VerticalOscillation_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Min
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               return colDef_NatTable.printValue_0(value);
            } else {
               return colDef_NatTable.printDoubleValue(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_OSCILLATION_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalOscillation_Min
                  * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

            if (UI.UNIT_IS_DISTANCE_KILOMETER) {
               colDef_Tree.printValue_0(cell, value);
            } else {
               colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio avg
    */
   private void defineColumn_RunDyn_VerticalRatio_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Avg;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Avg;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio max
    */
   private void defineColumn_RunDyn_VerticalRatio_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Max;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Max;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Running Dynamics - Vertical ratio min
    */
   private void defineColumn_RunDyn_VerticalRatio_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Min;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.RUN_DYN_VERTICAL_RATIO_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colRunDyn_VerticalRatio_Min;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Min distance
    */
   private void defineColumn_Surfing_MinDistance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.SURFING_MIN_DISTANCE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinDistance;
            final boolean isMinDistance = ((TVITourBookItem) element).col_Surfing_IsMinDistance;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET || isMinDistance == false) {
               return UI.EMPTY_STRING;
            } else {

               int minSurfingDistance = value;

               // convert imperial -> metric
               if (UI.UNIT_IS_LENGTH_YARD) {
                  minSurfingDistance = (int) (minSurfingDistance / UI.UNIT_YARD + 0.5);
               }

               return Integer.toString(minSurfingDistance);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_DISTANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinDistance;
            final boolean isMinDistance = ((TVITourBookItem) element).col_Surfing_IsMinDistance;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET || isMinDistance == false) {
               cell.setText(UI.EMPTY_STRING);
            } else {

               int minSurfingDistance = value;

               // convert imperial -> metric
               if (UI.UNIT_IS_LENGTH_YARD) {
                  minSurfingDistance = (int) (minSurfingDistance / UI.UNIT_YARD + 0.5);
               }

               cell.setText(Integer.toString(minSurfingDistance));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Min start/stop speed
    */
   private void defineColumn_Surfing_MinSpeed_StartStop() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.SURFING_MIN_SPEED_START_STOP.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_StartStop;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_SPEED_START_STOP.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_StartStop;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Min surfing speed
    */
   private void defineColumn_Surfing_MinSpeed_Surfing() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.SURFING_MIN_SPEED_SURFING.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_Surfing;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_MIN_SPEED_SURFING.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinSpeed_Surfing;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Min surfing time duration
    */
   private void defineColumn_Surfing_MinTimeDuration() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.SURFING_MIN_TIME_DURATION.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final short value = ((TVITourBookItem) element).col_Surfing_MinTimeDuration;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDefTree = TreeColumnFactory.SURFING_MIN_TIME_DURATION.createColumn(_columnManager_Tree, _pc);
      colDefTree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final short value = ((TVITourBookItem) element).col_Surfing_MinTimeDuration;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(value));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Surfing - Number of surfing events
    */
   private void defineColumn_Surfing_NumberOfEvents() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.SURFING_NUMBER_OF_EVENTS.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).col_Surfing_NumberOfEvents;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               return UI.EMPTY_STRING;
            } else {
               return Long.toString(value);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.SURFING_NUMBER_OF_EVENTS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).col_Surfing_NumberOfEvents;

            if (value == 0 || value == TourData.SURFING_VALUE_IS_NOT_SET) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Long.toString(value));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - break time (h)
    */
   private void defineColumn_Time_BreakTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__COMPUTED_BREAK_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourComputedTime_Break;

            return colDef_NatTable.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__COMPUTED_BREAK_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookItem item = (TVITourBookItem) element;

            final long value = item.colTourComputedTime_Break;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Relative break time %
    */
   private void defineColumn_Time_BreakTime_Relative() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__COMPUTED_BREAK_TIME_RELATIVE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final TVITourBookItem item = (TVITourBookItem) element;

            final long dbPausedTime = item.colTourComputedTime_Break;
            final long dbElapsedTime = item.colTourDeviceTime_Elapsed;

            final double relativePausedTime = dbElapsedTime == 0
                  ? 0
                  : (double) dbPausedTime / dbElapsedTime * 100;

            return _nf1.format(relativePausedTime);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__COMPUTED_BREAK_TIME_RELATIVE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            /*
             * display paused time relative to the elapsed time
             */

            final Object element = cell.getElement();
            final TVITourBookItem item = (TVITourBookItem) element;

            final long dbPausedTime = item.colTourComputedTime_Break;
            final long dbElapsedTime = item.colTourDeviceTime_Elapsed;

            final double relativePausedTime = dbElapsedTime == 0
                  ? 0
                  : (double) dbPausedTime / dbElapsedTime * 100;

            cell.setText(_nf1.format(relativePausedTime));

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Elapsed time (h)
    */
   private void defineColumn_Time_ElapsedTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourDeviceTime_Elapsed;

            return colDef_NatTable.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__DEVICE_ELAPSED_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourDeviceTime_Elapsed;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Moving time (h)
    */
   private void defineColumn_Time_MovingTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourComputedTime_Moving;

            return colDef_NatTable.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__COMPUTED_MOVING_TIME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourComputedTime_Moving;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - paused time (h)
    */
   private void defineColumn_Time_PausedTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__DEVICE_PAUSED_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourDeviceTime_Paused;

            return colDef_NatTable.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__DEVICE_PAUSED_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final TVITourBookItem item = (TVITourBookItem) element;

            final long value = item.colTourDeviceTime_Paused;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Recorded time (h)
    */
   private void defineColumn_Time_RecordedTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME__DEVICE_RECORDED_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colTourDeviceTime_Recorded;

            return colDef_NatTable.printLongValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME__DEVICE_RECORDED_TIME.createColumn(_columnManager_Tree, _pc);

      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colTourDeviceTime_Recorded;

            colDef_Tree.printLongValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Timezone
    */
   private void defineColumn_Time_TimeZone() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_TIME_ZONE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String timeZoneId = ((TVITourBookTour) element).colTimeZoneId;

            return timeZoneId == null ? UI.EMPTY_STRING : timeZoneId;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_TIME_ZONE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final String timeZoneId = ((TVITourBookTour) element).colTimeZoneId;
               cell.setText(timeZoneId == null ? UI.EMPTY_STRING : timeZoneId);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Timezone difference
    */
   private void defineColumn_Time_TimeZoneDifference() {

      _colDef_TimeZoneOffset_Tree = TreeColumnFactory.TIME_TIME_ZONE_DIFFERENCE.createColumn(_columnManager_Tree, _pc);
      _colDef_TimeZoneOffset_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;

               cell.setText(tourDateTime.timeZoneOffsetLabel);

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Tour start time
    */
   private void defineColumn_Time_TourStartTime() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider_WithTourTooltip() {

         @Override
         public String getValueText(final Object element) {

            final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;
            final ZonedDateTime tourStartDateTime = tourDateTime.tourZonedDateTime;

            final ValueFormat valueFormatter = colDef_NatTable.getValueFormat_Detail();

            if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {
               return tourStartDateTime.format(TimeTools.Formatter_Time_M);
            } else {
               return tourStartDateTime.format(TimeTools.Formatter_Time_S);
            }
         }

         @Override
         public boolean isShowTooltip() {
            return _isShowToolTipIn_Time;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Time == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final TourDateTime tourDateTime = ((TVITourBookTour) element).colTourDateTime;
               final ZonedDateTime tourStartDateTime = tourDateTime.tourZonedDateTime;

               final ValueFormat valueFormatter = colDef_Tree.getValueFormat_Detail();

               if (valueFormatter.equals(ValueFormat.TIME_HH_MM_SS)) {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_M));
               } else {
                  cell.setText(tourStartDateTime.format(TimeTools.Formatter_Time_S));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Week day
    */
   private void defineColumn_Time_WeekDay() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_WEEK_DAY.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider_WithTourTooltip() {

         @Override
         public String getValueText(final Object element) {

            return ((TVITourBookTour) element).colWeekDay;
         }

         @Override
         public boolean isShowTooltip() {
            return _isShowToolTipIn_WeekDay;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEK_DAY.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_WeekDay == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(((TVITourBookTour) element).colWeekDay);
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Time - Week
    */
   private void defineColumn_Time_WeekNo() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_WEEK_NO.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int week = ((TVITourBookItem) element).colWeekNo;

            if (week == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(week);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEK_NO.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVITourBookItem) element).colWeekNo;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Time - Week year
    */
   private void defineColumn_Time_WeekYear() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TIME_WEEKYEAR.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int week = ((TVITourBookItem) element).colWeekYear;

            if (week == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(week);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TIME_WEEKYEAR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int week = ((TVITourBookItem) element).colWeekYear;

            if (week == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(week));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Tour - Tour end location
    */
   private void defineColumn_Tour_Location_End() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_LOCATION_END.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String tourLocation = ((TVITourBookItem) element).colTourLocation_End;

            if (tourLocation == null) {
               return UI.EMPTY_STRING;
            } else {
               return tourLocation;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_LOCATION_END.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String tourLocation = ((TVITourBookItem) element).colTourLocation_End;

            if (tourLocation == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(tourLocation);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Tour - Tour start location
    */
   private void defineColumn_Tour_Location_Start() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_LOCATION_START.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final String tourLocation = ((TVITourBookItem) element).colTourLocation_Start;

            if (tourLocation == null) {
               return UI.EMPTY_STRING;
            } else {
               return tourLocation;
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_LOCATION_START.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String tourLocation = ((TVITourBookItem) element).colTourLocation_Start;

            if (tourLocation == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(tourLocation);
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Tour - Markers
    */
   private void defineColumn_Tour_Marker() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
            if (markerIds == null) {
               return UI.EMPTY_STRING;
            } else {
               return _nf0.format(markerIds.size());
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_NUM_MARKERS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final ArrayList<Long> markerIds = ((TVITourBookTour) element).getMarkerIds();
               if (markerIds == null) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(_nf0.format(markerIds.size()));
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour - Number of photos
    */
   private void defineColumn_Tour_Photos() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_NUM_PHOTOS.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long value = ((TVITourBookItem) element).colNumberOfPhotos;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_NUM_PHOTOS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final long value = ((TVITourBookItem) element).colNumberOfPhotos;

            colDef_Tree.printValue_0(cell, value);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column for debugging: Tag ids
    */
   @SuppressWarnings("unused")
   private void defineColumn_Tour_TagIds() {

//      final TreeColumnDefinition colDef = new TreeColumnDefinition(_columnManager, "TOUR_TAG_IDS", SWT.TRAIL); //$NON-NLS-1$
//
//      colDef.setColumnCategory(net.tourbook.ui.Messages.ColumnFactory_Category_Tour);
//      colDef.setColumnLabel("Tag ID");
//      colDef.setColumnHeaderText("Tag ID");
//
//      colDef.setDefaultColumnWidth(30);
//
//      colDef.setLabelProvider(new CellLabelProvider() {
//
//         @Override
//         public void update(final ViewerCell cell) {
//            final Object element = cell.getElement();
//            if (element instanceof TVITourBookTour) {
//
//               final ArrayList<Long> tagIds = ((TVITourBookTour) element).getTagIds();
//               if (tagIds == null) {
//                  cell.setText(UI.EMPTY_STRING);
//               } else {
//
//                  cell.setText(tagIds.stream()
////                      .map(Object::toString)
////                      .sorted()
//                        .map(n -> Long.toString(n))
//                        .collect(Collectors.joining(",")));
//
//                  setCellColor(cell, element);
//               }
//            }
//         }
//      });
   }

   /**
    * Column: Tour - Tags
    */
   private void defineColumn_Tour_Tags() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_TAGS.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider_WithTourTooltip() {

         @Override
         public String getValueText(final Object element) {
            return TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds());
         }

         @Override
         public boolean isShowTooltip() {
            return _isShowToolTipIn_Tags;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TAGS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Tags == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               cell.setText(TourDatabase.getTagNames(((TVITourBookTour) element).getTagIds()));
               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour - Title
    */
   private void defineColumn_Tour_Title() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_TITLE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider_WithTourTooltip() {

         @Override
         public String getValueText(final Object element) {

            final String colTourTitle = ((TVITourBookTour) element).colTourTitle;

            if (colTourTitle == null) {
// used for debugging
//             return "<NULL>";
               return UI.EMPTY_STRING;
            } else {
               return colTourTitle;
            }
         }

         @Override
         public boolean isShowTooltip() {
            return _isShowToolTipIn_Title;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TITLE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new TourInfoToolTipCellLabelProvider() {

         @Override
         public Long getTourId(final ViewerCell cell) {

            if (_isShowToolTipIn_Title == false) {
               return null;
            }

            final Object element = cell.getElement();
            if ((element instanceof TVITourBookTour)) {
               return ((TVITourBookTour) element).getTourId();
            }

            return null;
         }

         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final String colTourTitle = ((TVITourBookTour) element).colTourTitle;

               if (colTourTitle == null) {
                  cell.setText(UI.EMPTY_STRING);
               } else {
                  cell.setText(colTourTitle);
               }

               setCellColor(cell, element);
            }
         }
      });
   }

   /**
    * Column: Tour - Tour type image
    */
   private void defineColumn_Tour_TypeImage() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            /**
             * Tour type image for the NatTable is implemented in
             * net.tourbook.ui.views.tourBook.TourBookView.NatTable_Configuration_TourType
             * <p>
             * When a label provider is not defined then a warning message is displayed from the
             * data provider !
             */
            return UI.EMPTY_STRING;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TYPE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
               final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

               /*
                * when a tour type image is modified, it will keep the same image resource only the
                * content is modified but in the rawDataView the modified image is not displayed
                * compared with the tourBookView which displays the correct image
                */
               cell.setImage(tourTypeImage);
            }
         }
      });
   }

   /**
    * Column: Tour - Tour type text
    */
   private void defineColumn_Tour_TypeText() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
            return net.tourbook.ui.UI.getTourTypeLabel(tourTypeId);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TVITourBookTour) {

               final long tourTypeId = ((TVITourBookTour) element).getTourTypeId();
               cell.setText(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
            }
         }
      });
   }

   /**
    * Column: Training - FTP
    */
   private void defineColumn_Training_FTP() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_FTP.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int dbValue = ((TVITourBookItem) element).colTraining_FTP;

            if (dbValue == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(dbValue);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_FTP.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int dbValue = ((TVITourBookItem) element).colTraining_FTP;

            if (dbValue == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(dbValue));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training - PowerIntensityFactor
    */
   private void defineColumn_Training_IntensityFactor() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_INTENSITY_FACTOR.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_IntensityFactor;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_INTENSITY_FACTOR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_IntensityFactor;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   private void defineColumn_Training_PowerToWeightRatio() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_POWER_TO_WEIGHT.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_PowerToWeight;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_POWER_TO_WEIGHT.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_PowerToWeight;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training - PowerTrainingStressScore
    */
   private void defineColumn_Training_StressScore() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_STRESS_SCORE.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingStressScore;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_STRESS_SCORE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingStressScore;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training: Training Effect
    */
   private void defineColumn_Training_TrainingEffect() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_EFFECT_AEROB.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Aerob;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_EFFECT_AEROB.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Aerob;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training: Training effect anaerobic
    */
   private void defineColumn_Training_TrainingEffect_Anaerobic() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_EFFECT_ANAEROB.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Anaerobic;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_EFFECT_ANAEROB.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingEffect_Anaerobic;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Training - Training Performance
    */
   private void defineColumn_Training_TrainingPerformance() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.TRAINING_PERFORMANCE_LEVEL.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = ((TVITourBookItem) element).colTraining_TrainingPerformance;

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.TRAINING_TRAINING_PERFORMANCE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final double value = ((TVITourBookItem) element).colTraining_TrainingPerformance;

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Clouds
    */
   private void defineColumn_Weather_Clouds() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_CLOUDS.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setIsDefaultColumn();
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            /**
             * Image for the NatTable is implemented in TourBookView.NatTable_ConfigField_Weather
             * <p>
             * When a label provider is not defined then a warning message is displayed from the
             * data provider !
             */

            return UI.EMPTY_STRING;
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_CLOUDS.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final String windClouds = ((TVITourBookItem) element).colClouds;

            if (windClouds == null) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               final Image img = net.tourbook.common.UI.IMAGE_REGISTRY.get(windClouds);
               if (img != null) {
                  cell.setImage(img);
               } else {
                  cell.setText(windClouds);
               }
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Avg temperature
    */
   private void defineColumn_Weather_Temperature_Avg() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_TEMPERATURE_AVG.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Avg);

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_AVG.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Avg);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Max temperature
    */
   private void defineColumn_Weather_Temperature_Max() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_TEMPERATURE_MAX.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Max);

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_MAX.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Max);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Min temperature
    */
   private void defineColumn_Weather_Temperature_Min() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_TEMPERATURE_MIN.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Min);

            return colDef_NatTable.printDoubleValue(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_TEMPERATURE_MIN.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final double value = UI.convertTemperatureFromMetric(((TVITourBookItem) element).colTemperature_Min);

            colDef_Tree.printDoubleValue(cell, value, element instanceof TVITourBookTour);

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Wind direction
    */
   private void defineColumn_Weather_WindDirection() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_WIND_DIR.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int windDir = ((TVITourBookItem) element).colWindDir;

            if (windDir == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(windDir);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_WIND_DIR.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windDir = ((TVITourBookItem) element).colWindDir;

            if (windDir == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windDir));
            }

            setCellColor(cell, element);
         }
      });
   }

   /**
    * Column: Weather - Wind speed
    */
   private void defineColumn_Weather_WindSpeed() {

      final TableColumnDefinition colDef_NatTable = TableColumnFactory.WEATHER_WIND_SPEED.createColumn(_columnManager_NatTable, _pc);
      colDef_NatTable.setLabelProvider_NatTable(new NatTable_LabelProvider() {

         @Override
         public String getValueText(final Object element) {

            final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / UI.UNIT_VALUE_DISTANCE);

            if (windSpeed == 0) {
               return UI.EMPTY_STRING;
            } else {
               return Integer.toString(windSpeed);
            }
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.WEATHER_WIND_SPEED.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();
            final int windSpeed = (int) (((TVITourBookItem) element).colWindSpd / UI.UNIT_VALUE_DISTANCE);

            if (windSpeed == 0) {
               cell.setText(UI.EMPTY_STRING);
            } else {
               cell.setText(Integer.toString(windSpeed));
            }

            setCellColor(cell, element);
         }
      });
   }

   ColumnDefinition getColDef_TimeZoneOffset_Tree() {
      return _colDef_TimeZoneOffset_Tree;
   }

   void setCellColor(final ViewerCell cell, final Object element) {

      boolean isShowSummaryRow = false;

      if (element instanceof TVITourBookYear && _isShowSummaryRow) {
         isShowSummaryRow = ((TVITourBookYear) element).isRowSummary;
      }

      if (isShowSummaryRow) {

         // show no other color

      } else {

         if (element instanceof TVITourBookYear) {
            cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB));
         } else if (element instanceof TVITourBookYearCategorized) {
            cell.setForeground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_SUB_SUB));
//         } else if (element instanceof TVITourBookTour) {
//            cell.setForeground(JFaceResources.getColorRegistry().get(UI.VIEW_COLOR_TOUR));
         }
      }
   }

   void setIsShowSummaryRow(final boolean isShowSummaryRow) {

      this._isShowSummaryRow = isShowSummaryRow;
   }

   void updateToolTipState() {

      _isShowToolTipIn_Date = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE);
      _isShowToolTipIn_Time = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME);
      _isShowToolTipIn_WeekDay = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY);
      _isShowToolTipIn_Title = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE);
      _isShowToolTipIn_Tags = _prefStore.getBoolean(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS);
   }

}
