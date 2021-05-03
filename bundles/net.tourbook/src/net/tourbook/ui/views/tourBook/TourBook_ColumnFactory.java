/*******************************************************************************
 * Copyright (C) 2020 Wolfgang Schramm and Contributors
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
import net.tourbook.photo.ImageUtils;
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

   private final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

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

            final String dbValue = ((TVITourBookItem) element).col_DeviceName;

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
            final String dbValue = ((TVITourBookItem) element).col_DeviceName;

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

            final float value = ((TVITourBookItem) element).colAltitude_AvgChange
                  / UI.UNIT_VALUE_ELEVATION
                  * UI.UNIT_VALUE_DISTANCE;

            return colDef_NatTable.printValue_0(value);
         }
      });

      final TreeColumnDefinition colDef_Tree = TreeColumnFactory.ALTITUDE_AVG_CHANGE.createColumn(_columnManager_Tree, _pc);
      colDef_Tree.setIsDefaultColumn();
      colDef_Tree.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            final float dbAvgAltitudeChange = ((TVITourBookItem) element).colAltitude_AvgChange / UI.UNIT_VALUE_ELEVATION
                  * UI.UNIT_VALUE_DISTANCE;

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
               cell.setImage(ImageUtils.decodeStringToImage(
                     "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"));

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
