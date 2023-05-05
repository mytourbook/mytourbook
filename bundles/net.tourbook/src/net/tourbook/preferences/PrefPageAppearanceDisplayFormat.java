/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.preferences.ICommonPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceDisplayFormat extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageAppearanceDisplayFormat"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = CommonActivator.getPrefStore();

   private PixelConverter         _pc;

   private SelectionListener      _defaultSelectionListener;

   /*
    * UI controls
    */
   private CTabFolder _tabFolder;
   private CTabItem   _tab1_OneTour;
   private CTabItem   _tab2_MultipleTours;

   private Button     _chkLiveUpdate;

   private Button     _rdoCadence_1_0;
   private Button     _rdoCadence_1_1;
   private Button     _rdoCadence_1_2;
   private Button     _rdoDistance_1_0;
   private Button     _rdoDistance_1_1;
   private Button     _rdoDistance_1_2;
   private Button     _rdoDistance_1_3;
   private Button     _rdoElevation_1_0;
   private Button     _rdoElevation_1_1;
   private Button     _rdoPower_1_0;
   private Button     _rdoPower_1_1;
   private Button     _rdoPulse_1_0;
   private Button     _rdoPulse_1_1;
   private Button     _rdoSpeed_1_0;
   private Button     _rdoSpeed_1_1;
   private Button     _rdoSpeed_1_2;
   private Button     _rdoTemperature_1_0;
   private Button     _rdoTemperature_1_1;
   private Button     _rdoTemperature_1_2;

   private Button     _rdoTime_Elapsed_HH;
   private Button     _rdoTime_Elapsed_HH_MM;
   private Button     _rdoTime_Elapsed_HH_MM_SS;
   private Button     _rdoTime_Recorded_HH;
   private Button     _rdoTime_Recorded_HH_MM;
   private Button     _rdoTime_Recorded_HH_MM_SS;
   private Button     _rdoTime_Paused_HH;
   private Button     _rdoTime_Paused_HH_MM;
   private Button     _rdoTime_Paused_HH_MM_SS;
   private Button     _rdoTime_Moving_HH;
   private Button     _rdoTime_Moving_HH_MM;
   private Button     _rdoTime_Moving_HH_MM_SS;
   private Button     _rdoTime_Break_HH;
   private Button     _rdoTime_Break_HH_MM;
   private Button     _rdoTime_Break_HH_MM_SS;

   private Button     _rdoCadence_1_0_Summary;
   private Button     _rdoCadence_1_1_Summary;
   private Button     _rdoCadence_1_2_Summary;
   private Button     _rdoDistance_1_0_Summary;
   private Button     _rdoDistance_1_1_Summary;
   private Button     _rdoDistance_1_2_Summary;
   private Button     _rdoDistance_1_3_Summary;
   private Button     _rdoElevation_1_0_Summary;
   private Button     _rdoElevation_1_1_Summary;
   private Button     _rdoPower_1_0_Summary;
   private Button     _rdoPower_1_1_Summary;
   private Button     _rdoPulse_1_0_Summary;
   private Button     _rdoPulse_1_1_Summary;
   private Button     _rdoSpeed_1_0_Summary;
   private Button     _rdoSpeed_1_1_Summary;
   private Button     _rdoSpeed_1_2_Summary;
   private Button     _rdoTemperature_1_0_Summary;
   private Button     _rdoTemperature_1_1_Summary;
   private Button     _rdoTemperature_1_2_Summary;

   private Button     _rdoTime_Elapsed_HH_Summary;
   private Button     _rdoTime_Elapsed_HH_MM_Summary;
   private Button     _rdoTime_Elapsed_HH_MM_SS_Summary;
   private Button     _rdoTime_Recorded_HH_Summary;
   private Button     _rdoTime_Recorded_HH_MM_Summary;
   private Button     _rdoTime_Recorded_HH_MM_SS_Summary;
   private Button     _rdoTime_Paused_HH_Summary;
   private Button     _rdoTime_Paused_HH_MM_Summary;
   private Button     _rdoTime_Paused_HH_MM_SS_Summary;
   private Button     _rdoTime_Moving_HH_Summary;
   private Button     _rdoTime_Moving_HH_MM_Summary;
   private Button     _rdoTime_Moving_HH_MM_SS_Summary;
   private Button     _rdoTime_Break_HH_Summary;
   private Button     _rdoTime_Break_HH_MM_Summary;
   private Button     _rdoTime_Break_HH_MM_SS_Summary;

   /*
    * UI controls
    */

   public PrefPageAppearanceDisplayFormat() {
//		noDefaultAndApplyButton();
   }

   @Override
   public void applyData(final Object data) {

      // data contains the folder index, this is set when the pref page is opened from a link

      if (data instanceof Integer) {
         _tabFolder.setSelection((Integer) data);
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
      {
         {
            /*
             * Label: Info
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_DisplayFormat_Label_Info);
            GridDataFactory.fillDefaults()//
                  .span(2, 1)
                  .applyTo(label);
         }

         _tabFolder = new CTabFolder(container, SWT.NONE);
         _tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
         {
            _tab1_OneTour = new CTabItem(_tabFolder, SWT.NONE);
            _tab1_OneTour.setText(Messages.Pref_DisplayFormat_Tab_OneTour);
            _tab1_OneTour.setToolTipText(Messages.Pref_DisplayFormat_Tab_OneTour_Tooltip);
            _tab1_OneTour.setControl(createUI_20_Formats_Tour(_tabFolder));

            _tab2_MultipleTours = new CTabItem(_tabFolder, SWT.NONE);
            _tab2_MultipleTours.setText(Messages.Pref_DisplayFormat_Tab_MultipleTours);
            _tab2_MultipleTours.setToolTipText(Messages.Pref_DisplayFormat_Tab_MultipleTours_Tooltip);
            _tab2_MultipleTours.setControl(createUI_30_Formats_Summary(_tabFolder));
         }

         createUI_99_LiveUpdate(container);
      }

      return container;
   }

   private Control createUI_20_Formats_Tour(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)
            .spacing(20, 5)
            .applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_22_Time(container);
         createUI_24_Other(container);
      }

      return container;
   }

   private void createUI_22_Time(final Composite parent) {

      final String formatName_HH = FormatManager.getValueFormatterName(ValueFormat.TIME_HH);
      final String formatName_HH_MM = FormatManager.getValueFormatterName(ValueFormat.TIME_HH_MM);
      final String formatName_HH_MM_SS = FormatManager.getValueFormatterName(ValueFormat.TIME_HH_MM_SS);
      {
         /*
          * Elapsed time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_elapsed_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Elapsed_HH = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH.setText(formatName_HH);
            _rdoTime_Elapsed_HH.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Elapsed_HH_MM = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH_MM.setText(formatName_HH_MM);
            _rdoTime_Elapsed_HH_MM.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Elapsed_HH_MM_SS = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH_MM_SS.setText(formatName_HH_MM_SS);
            _rdoTime_Elapsed_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Recorded time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_recorded_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Recorded_HH = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH.setText(formatName_HH);
            _rdoTime_Recorded_HH.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Recorded_HH_MM = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH_MM.setText(formatName_HH_MM);
            _rdoTime_Recorded_HH_MM.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Recorded_HH_MM_SS = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH_MM_SS.setText(formatName_HH_MM_SS);
            _rdoTime_Recorded_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Paused time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_paused_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Paused_HH = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH.setText(formatName_HH);
            _rdoTime_Paused_HH.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Paused_HH_MM = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH_MM.setText(formatName_HH_MM);
            _rdoTime_Paused_HH_MM.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Paused_HH_MM_SS = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH_MM_SS.setText(formatName_HH_MM_SS);
            _rdoTime_Paused_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Moving time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_moving_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Moving_HH = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH.setText(formatName_HH);
            _rdoTime_Moving_HH.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Moving_HH_MM = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH_MM.setText(formatName_HH_MM);
            _rdoTime_Moving_HH_MM.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Moving_HH_MM_SS = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH_MM_SS.setText(formatName_HH_MM_SS);
            _rdoTime_Moving_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Break time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_break_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Break_HH = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH.setText(formatName_HH);
            _rdoTime_Break_HH.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Break_HH_MM = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH_MM.setText(formatName_HH_MM);
            _rdoTime_Break_HH_MM.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Break_HH_MM_SS = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH_MM_SS.setText(formatName_HH_MM_SS);
            _rdoTime_Break_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void createUI_24_Other(final Composite parent) {

      final String formatName_1_0 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_0);
      final String formatName_1_1 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_1);
      final String formatName_1_2 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_2);
      final String formatName_1_3 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_3);

      {
         /*
          * Elevation: m / ft
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Altitude);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoElevation_1_0 = new Button(container, SWT.RADIO);
            _rdoElevation_1_0.setText(formatName_1_0);
            _rdoElevation_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoElevation_1_1 = new Button(container, SWT.RADIO);
            _rdoElevation_1_1.setText(formatName_1_1);
            _rdoElevation_1_1.addSelectionListener(_defaultSelectionListener);
         }

         // vertical indent
         final int vIndent = _pc.convertVerticalDLUsToPixels(4);
         GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(label);
         GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(container);
      }
      {
         /*
          * Pulse: bpm
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Pulse);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoPulse_1_0 = new Button(container, SWT.RADIO);
            _rdoPulse_1_0.setText(formatName_1_0);
            _rdoPulse_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoPulse_1_1 = new Button(container, SWT.RADIO);
            _rdoPulse_1_1.setText(formatName_1_1);
            _rdoPulse_1_1.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Power: W
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Power);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoPower_1_0 = new Button(container, SWT.RADIO);
            _rdoPower_1_0.setText(formatName_1_0);
            _rdoPower_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoPower_1_1 = new Button(container, SWT.RADIO);
            _rdoPower_1_1.setText(formatName_1_1);
            _rdoPower_1_1.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Cadence: rpm/spm
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Cadence);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoCadence_1_0 = new Button(container, SWT.RADIO);
            _rdoCadence_1_0.setText(formatName_1_0);
            _rdoCadence_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoCadence_1_1 = new Button(container, SWT.RADIO);
            _rdoCadence_1_1.setText(formatName_1_1);
            _rdoCadence_1_1.addSelectionListener(_defaultSelectionListener);

            _rdoCadence_1_2 = new Button(container, SWT.RADIO);
            _rdoCadence_1_2.setText(formatName_1_2);
            _rdoCadence_1_2.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Speed: km/h
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Speed);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoSpeed_1_0 = new Button(container, SWT.RADIO);
            _rdoSpeed_1_0.setText(formatName_1_0);
            _rdoSpeed_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoSpeed_1_1 = new Button(container, SWT.RADIO);
            _rdoSpeed_1_1.setText(formatName_1_1);
            _rdoSpeed_1_1.addSelectionListener(_defaultSelectionListener);

            _rdoSpeed_1_2 = new Button(container, SWT.RADIO);
            _rdoSpeed_1_2.setText(formatName_1_2);
            _rdoSpeed_1_2.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Temperature: Celsius or Fahrenheit
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Temperature);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTemperature_1_0 = new Button(container, SWT.RADIO);
            _rdoTemperature_1_0.setText(formatName_1_0);
            _rdoTemperature_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoTemperature_1_1 = new Button(container, SWT.RADIO);
            _rdoTemperature_1_1.setText(formatName_1_1);
            _rdoTemperature_1_1.addSelectionListener(_defaultSelectionListener);

            _rdoTemperature_1_2 = new Button(container, SWT.RADIO);
            _rdoTemperature_1_2.setText(formatName_1_2);
            _rdoTemperature_1_2.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Distance: # ... #.###
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Distance);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         {
            _rdoDistance_1_0 = new Button(container, SWT.RADIO);
            _rdoDistance_1_0.setText(formatName_1_0);
            _rdoDistance_1_0.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_1 = new Button(container, SWT.RADIO);
            _rdoDistance_1_1.setText(formatName_1_1);
            _rdoDistance_1_1.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_2 = new Button(container, SWT.RADIO);
            _rdoDistance_1_2.setText(formatName_1_2);
            _rdoDistance_1_2.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_3 = new Button(container, SWT.RADIO);
            _rdoDistance_1_3.setText(formatName_1_3);
            _rdoDistance_1_3.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private Control createUI_30_Formats_Summary(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)
            .spacing(20, 5)
            .applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_32_Time_Summary(container);
         createUI_34_Other_Summary(container);
      }

      return container;
   }

   private void createUI_32_Time_Summary(final Composite parent) {

      final String formatName_HH = FormatManager.getValueFormatterName(ValueFormat.TIME_HH);
      final String formatName_HH_MM = FormatManager.getValueFormatterName(ValueFormat.TIME_HH_MM);
      final String formatName_HH_MM_SS = FormatManager.getValueFormatterName(ValueFormat.TIME_HH_MM_SS);
      {
         /*
          * Elapsed time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_elapsed_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Elapsed_HH_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH_Summary.setText(formatName_HH);
            _rdoTime_Elapsed_HH_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Elapsed_HH_MM_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH_MM_Summary.setText(formatName_HH_MM);
            _rdoTime_Elapsed_HH_MM_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Elapsed_HH_MM_SS_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Elapsed_HH_MM_SS_Summary.setText(formatName_HH_MM_SS);
            _rdoTime_Elapsed_HH_MM_SS_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Recorded time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_recorded_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Recorded_HH_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH_Summary.setText(formatName_HH);
            _rdoTime_Recorded_HH_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Recorded_HH_MM_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH_MM_Summary.setText(formatName_HH_MM);
            _rdoTime_Recorded_HH_MM_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Recorded_HH_MM_SS_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Recorded_HH_MM_SS_Summary.setText(formatName_HH_MM_SS);
            _rdoTime_Recorded_HH_MM_SS_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Paused time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_paused_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Paused_HH_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH_Summary.setText(formatName_HH);
            _rdoTime_Paused_HH_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Paused_HH_MM_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH_MM_Summary.setText(formatName_HH_MM);
            _rdoTime_Paused_HH_MM_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Paused_HH_MM_SS_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Paused_HH_MM_SS_Summary.setText(formatName_HH_MM_SS);
            _rdoTime_Paused_HH_MM_SS_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Moving time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_moving_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Moving_HH_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH_Summary.setText(formatName_HH);
            _rdoTime_Moving_HH_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Moving_HH_MM_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH_MM_Summary.setText(formatName_HH_MM);
            _rdoTime_Moving_HH_MM_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Moving_HH_MM_SS_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Moving_HH_MM_SS_Summary.setText(formatName_HH_MM_SS);
            _rdoTime_Moving_HH_MM_SS_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Break time format: hh ... hh:mm:ss
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.pref_view_layout_label_break_time_format);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTime_Break_HH_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH_Summary.setText(formatName_HH);
            _rdoTime_Break_HH_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Break_HH_MM_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH_MM_Summary.setText(formatName_HH_MM);
            _rdoTime_Break_HH_MM_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTime_Break_HH_MM_SS_Summary = new Button(container, SWT.RADIO);
            _rdoTime_Break_HH_MM_SS_Summary.setText(formatName_HH_MM_SS);
            _rdoTime_Break_HH_MM_SS_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void createUI_34_Other_Summary(final Composite parent) {

      final String formatName_1_0 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_0);
      final String formatName_1_1 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_1);
      final String formatName_1_2 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_2);
      final String formatName_1_3 = FormatManager.getValueFormatterName(ValueFormat.NUMBER_1_3);

      {
         /*
          * Pulse: m / ft
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Altitude);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoElevation_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoElevation_1_0_Summary.setText(formatName_1_0);
            _rdoElevation_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoElevation_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoElevation_1_1_Summary.setText(formatName_1_1);
            _rdoElevation_1_1_Summary.addSelectionListener(_defaultSelectionListener);
         }

         // vertical indent
         final int vIndent = _pc.convertVerticalDLUsToPixels(4);
         GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(label);
         GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(container);
      }
      {
         /*
          * Pulse: bpm
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Pulse);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoPulse_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoPulse_1_0_Summary.setText(formatName_1_0);
            _rdoPulse_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoPulse_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoPulse_1_1_Summary.setText(formatName_1_1);
            _rdoPulse_1_1_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Power: W
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Power);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            _rdoPower_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoPower_1_0_Summary.setText(formatName_1_0);
            _rdoPower_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoPower_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoPower_1_1_Summary.setText(formatName_1_1);
            _rdoPower_1_1_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Cadence: rpm/spm
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Cadence);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoCadence_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoCadence_1_0_Summary.setText(formatName_1_0);
            _rdoCadence_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoCadence_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoCadence_1_1_Summary.setText(formatName_1_1);
            _rdoCadence_1_1_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoCadence_1_2_Summary = new Button(container, SWT.RADIO);
            _rdoCadence_1_2_Summary.setText(formatName_1_2);
            _rdoCadence_1_2_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Speed: km/h
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Speed);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoSpeed_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoSpeed_1_0_Summary.setText(formatName_1_0);
            _rdoSpeed_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoSpeed_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoSpeed_1_1_Summary.setText(formatName_1_1);
            _rdoSpeed_1_1_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoSpeed_1_2_Summary = new Button(container, SWT.RADIO);
            _rdoSpeed_1_2_Summary.setText(formatName_1_2);
            _rdoSpeed_1_2_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Temperature: Celsius or Fahrenheit
          */
         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Temperature);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
         {
            _rdoTemperature_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoTemperature_1_0_Summary.setText(formatName_1_0);
            _rdoTemperature_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTemperature_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoTemperature_1_1_Summary.setText(formatName_1_1);
            _rdoTemperature_1_1_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoTemperature_1_2_Summary = new Button(container, SWT.RADIO);
            _rdoTemperature_1_2_Summary.setText(formatName_1_2);
            _rdoTemperature_1_2_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }

      {
         /*
          * Distance: # ... #.###
          */

         final Label label = new Label(parent, SWT.NONE);
         label.setText(Messages.Pref_DisplayFormat_Label_Distance);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         {
            _rdoDistance_1_0_Summary = new Button(container, SWT.RADIO);
            _rdoDistance_1_0_Summary.setText(formatName_1_0);
            _rdoDistance_1_0_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_1_Summary = new Button(container, SWT.RADIO);
            _rdoDistance_1_1_Summary.setText(formatName_1_1);
            _rdoDistance_1_1_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_2_Summary = new Button(container, SWT.RADIO);
            _rdoDistance_1_2_Summary.setText(formatName_1_2);
            _rdoDistance_1_2_Summary.addSelectionListener(_defaultSelectionListener);

            _rdoDistance_1_3_Summary = new Button(container, SWT.RADIO);
            _rdoDistance_1_3_Summary.setText(formatName_1_3);
            _rdoDistance_1_3_Summary.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void createUI_99_LiveUpdate(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .indent(0, _pc.convertVerticalDLUsToPixels(8))
            .applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * Checkbox: live update
          */
         _chkLiveUpdate = new Button(container, SWT.CHECK);
         _chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
         _chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
         _chkLiveUpdate.addSelectionListener(_defaultSelectionListener);
      }
   }

   private void doLiveUpdate() {

      if (_chkLiveUpdate.getSelection()) {
         performApply();
      }
   }

   @Override
   public void init(final IWorkbench workbench) {
      setPreferenceStore(_prefStore);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onSelection());
   }

   @Override
   public boolean okToLeave() {

      saveUIState();

      return super.okToLeave();
   }

   private void onSelection() {

      doLiveUpdate();
   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   public boolean performCancel() {

      saveUIState();

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

// SET_FORMATTING_OFF

      final String cadence                      = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_CADENCE);
      final String distance                     = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE);
      final String elevation                    = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE);
      final String power                        = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_POWER);
      final String pulse                        = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PULSE);
      final String speed                        = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_SPEED);
      final String temperature                  = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE);

      final String elapsedTime                  = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME);
      final String recordedTime                 = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME);
      final String pausedTime                   = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
      final String movingTime                   = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME);
      final String breakTime                    = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME);

      final String cadence_Summary              = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_CADENCE_SUMMARY);
      final String distance_Summary             = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE_SUMMARY);
      final String elevation_Summary            = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE_SUMMARY);
      final String power_Summary                = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_POWER_SUMMARY);
      final String pulse_Summary                = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PULSE_SUMMARY);
      final String speed_Summary                = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_SPEED_SUMMARY);
      final String temperature_Summary          = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE_SUMMARY);

      final String elapsedTime_Summary          = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY);
      final String recordedTime_Summary         = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME_SUMMARY);
      final String pausedTime_Summary           = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME_SUMMARY);
      final String movingTime_Summary           = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME_SUMMARY);
      final String breakTime_Summary            = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME_SUMMARY);

      final boolean isCadence_1_0               = ValueFormat.NUMBER_1_0.name()     .equals(cadence);
      final boolean isCadence_1_1               = ValueFormat.NUMBER_1_1.name()     .equals(cadence);
      final boolean isCadence_1_2               = ValueFormat.NUMBER_1_2.name()     .equals(cadence);
      final boolean isCadence_1_0_Summary       = ValueFormat.NUMBER_1_0.name()     .equals(cadence_Summary);
      final boolean isCadence_1_1_Summary       = ValueFormat.NUMBER_1_1.name()     .equals(cadence_Summary);
      final boolean isCadence_1_2_Summary       = ValueFormat.NUMBER_1_2.name()     .equals(cadence_Summary);

      final boolean isDistance_1_0              = ValueFormat.NUMBER_1_0.name()     .equals(distance);
      final boolean isDistance_1_1              = ValueFormat.NUMBER_1_1.name()     .equals(distance);
      final boolean isDistance_1_2              = ValueFormat.NUMBER_1_2.name()     .equals(distance);
      final boolean isDistance_1_3              = ValueFormat.NUMBER_1_3.name()     .equals(distance);
      final boolean isDistance_1_0_Summary      = ValueFormat.NUMBER_1_0.name()     .equals(distance_Summary);
      final boolean isDistance_1_1_Summary      = ValueFormat.NUMBER_1_1.name()     .equals(distance_Summary);
      final boolean isDistance_1_2_Summary      = ValueFormat.NUMBER_1_2.name()     .equals(distance_Summary);
      final boolean isDistance_1_3_Summary      = ValueFormat.NUMBER_1_3.name()     .equals(distance_Summary);

      final boolean isElevation_1_0             = ValueFormat.NUMBER_1_0.name()     .equals(elevation);
      final boolean isElevation_1_0_Summary     = ValueFormat.NUMBER_1_0.name()     .equals(elevation_Summary);

      final boolean isPower_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(power);
      final boolean isPower_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(power_Summary);

      final boolean isPulse_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(pulse);
      final boolean isPulse_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(pulse_Summary);

      final boolean isSpeed_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(speed);
      final boolean isSpeed_1_1                 = ValueFormat.NUMBER_1_1.name()     .equals(speed);
      final boolean isSpeed_1_2                 = ValueFormat.NUMBER_1_2.name()     .equals(speed);
      final boolean isSpeed_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(speed_Summary);
      final boolean isSpeed_1_1_Summary         = ValueFormat.NUMBER_1_1.name()     .equals(speed_Summary);
      final boolean isSpeed_1_2_Summary         = ValueFormat.NUMBER_1_2.name()     .equals(speed_Summary);

      final boolean isTemperature_1_0           = ValueFormat.NUMBER_1_0.name()     .equals(temperature);
      final boolean isTemperature_1_1           = ValueFormat.NUMBER_1_1.name()     .equals(temperature);
      final boolean isTemperature_1_2           = ValueFormat.NUMBER_1_2.name()     .equals(temperature);
      final boolean isTemperature_1_0_Summary   = ValueFormat.NUMBER_1_0.name()     .equals(temperature_Summary);
      final boolean isTemperature_1_1_Summary   = ValueFormat.NUMBER_1_1.name()     .equals(temperature_Summary);
      final boolean isTemperature_1_2_Summary   = ValueFormat.NUMBER_1_2.name()     .equals(temperature_Summary);

      final boolean isElapsed_HH                = ValueFormat.TIME_HH.name()        .equals(elapsedTime);
      final boolean isElapsed_HH_MM             = ValueFormat.TIME_HH_MM.name()     .equals(elapsedTime);
      final boolean isElapsed_HH_MM_SS          = ValueFormat.TIME_HH_MM_SS.name()  .equals(elapsedTime);
      final boolean isElapsed_HH_Summary        = ValueFormat.TIME_HH.name()        .equals(elapsedTime_Summary);
      final boolean isElapsed_HH_MM_Summary     = ValueFormat.TIME_HH_MM.name()     .equals(elapsedTime_Summary);
      final boolean isElapsed_HH_MM_SS_Summary  = ValueFormat.TIME_HH_MM_SS.name()  .equals(elapsedTime_Summary);

      final boolean isRecorded_HH               = ValueFormat.TIME_HH.name()        .equals(recordedTime);
      final boolean isRecorded_HH_MM            = ValueFormat.TIME_HH_MM.name()     .equals(recordedTime);
      final boolean isRecorded_HH_MM_SS         = ValueFormat.TIME_HH_MM_SS.name()  .equals(recordedTime);
      final boolean isRecorded_HH_Summary       = ValueFormat.TIME_HH.name()        .equals(recordedTime_Summary);
      final boolean isRecorded_HH_MM_Summary    = ValueFormat.TIME_HH_MM.name()     .equals(recordedTime_Summary);
      final boolean isRecorded_HH_MM_SS_Summary = ValueFormat.TIME_HH_MM_SS.name()  .equals(recordedTime_Summary);

      final boolean isPaused_HH                 = ValueFormat.TIME_HH.name()        .equals(pausedTime);
      final boolean isPaused_HH_MM              = ValueFormat.TIME_HH_MM.name()     .equals(pausedTime);
      final boolean isPaused_HH_MM_SS           = ValueFormat.TIME_HH_MM_SS.name()  .equals(pausedTime);
      final boolean isPaused_HH_Summary         = ValueFormat.TIME_HH.name()        .equals(pausedTime_Summary);
      final boolean isPaused_HH_MM_Summary      = ValueFormat.TIME_HH_MM.name()     .equals(pausedTime_Summary);
      final boolean isPaused_HH_MM_SS_Summary   = ValueFormat.TIME_HH_MM_SS.name()  .equals(pausedTime_Summary);

      final boolean isMoving_HH                 = ValueFormat.TIME_HH.name()        .equals(movingTime);
      final boolean isMoving_HH_MM              = ValueFormat.TIME_HH_MM.name()     .equals(movingTime);
      final boolean isMoving_HH_MM_SS           = ValueFormat.TIME_HH_MM_SS.name()  .equals(movingTime);
      final boolean isMoving_HH_Summary         = ValueFormat.TIME_HH.name()        .equals(movingTime_Summary);
      final boolean isMoving_HH_MM_Summary      = ValueFormat.TIME_HH_MM.name()     .equals(movingTime_Summary);
      final boolean isMoving_HH_MM_SS_Summary   = ValueFormat.TIME_HH_MM_SS.name()  .equals(movingTime_Summary);

      final boolean isBreak_HH                  = ValueFormat.TIME_HH.name()        .equals(breakTime);
      final boolean isBreak_HH_MM               = ValueFormat.TIME_HH_MM.name()     .equals(breakTime);
      final boolean isBreak_HH_MM_SS            = ValueFormat.TIME_HH_MM_SS.name()  .equals(breakTime);
      final boolean isBreak_HH_Summary          = ValueFormat.TIME_HH.name()        .equals(breakTime_Summary);
      final boolean isBreak_HH_MM_Summary       = ValueFormat.TIME_HH_MM.name()     .equals(breakTime_Summary);
      final boolean isBreak_HH_MM_SS_Summary    = ValueFormat.TIME_HH_MM_SS.name()  .equals(breakTime_Summary);

      final int selectedTab = _tabFolder.getSelectionIndex();

      if (selectedTab == 0) {

         // One tour is selected

         _rdoCadence_1_0                        .setSelection(isCadence_1_0);
         _rdoCadence_1_1                        .setSelection(isCadence_1_1);
         _rdoCadence_1_2                        .setSelection(isCadence_1_2);

         _rdoDistance_1_0                       .setSelection(isDistance_1_0);
         _rdoDistance_1_1                       .setSelection(isDistance_1_1);
         _rdoDistance_1_2                       .setSelection(isDistance_1_2);
         _rdoDistance_1_3                       .setSelection(isDistance_1_3);

         _rdoElevation_1_0                      .setSelection(isElevation_1_0);
         _rdoElevation_1_1                      .setSelection(!isElevation_1_0);

         _rdoPower_1_0                          .setSelection(isPower_1_0);
         _rdoPower_1_1                          .setSelection(!isPower_1_0);

         _rdoPulse_1_0                          .setSelection(isPulse_1_0);
         _rdoPulse_1_1                          .setSelection(!isPulse_1_0);

         _rdoSpeed_1_0                          .setSelection(isSpeed_1_0);
         _rdoSpeed_1_1                          .setSelection(isSpeed_1_1);
         _rdoSpeed_1_2                          .setSelection(isSpeed_1_2);

         _rdoTemperature_1_0                    .setSelection(isTemperature_1_0);
         _rdoTemperature_1_1                    .setSelection(isTemperature_1_1);
         _rdoTemperature_1_2                    .setSelection(isTemperature_1_2);

         _rdoTime_Elapsed_HH                    .setSelection(isElapsed_HH);
         _rdoTime_Elapsed_HH_MM                 .setSelection(isElapsed_HH_MM);
         _rdoTime_Elapsed_HH_MM_SS              .setSelection(isElapsed_HH_MM_SS);

         _rdoTime_Recorded_HH                   .setSelection(isRecorded_HH);
         _rdoTime_Recorded_HH_MM                .setSelection(isRecorded_HH_MM);
         _rdoTime_Recorded_HH_MM_SS             .setSelection(isRecorded_HH_MM_SS);

         _rdoTime_Paused_HH                     .setSelection(isPaused_HH);
         _rdoTime_Paused_HH_MM                  .setSelection(isPaused_HH_MM);
         _rdoTime_Paused_HH_MM_SS               .setSelection(isPaused_HH_MM_SS);

         _rdoTime_Moving_HH                     .setSelection(isMoving_HH);
         _rdoTime_Moving_HH_MM                  .setSelection(isMoving_HH_MM);
         _rdoTime_Moving_HH_MM_SS               .setSelection(isMoving_HH_MM_SS);

         _rdoTime_Break_HH                      .setSelection(isBreak_HH);
         _rdoTime_Break_HH_MM                   .setSelection(isBreak_HH_MM);
         _rdoTime_Break_HH_MM_SS                .setSelection(isBreak_HH_MM_SS);


      } else if (selectedTab == 1) {

         // Multiple tours are selected

         _rdoCadence_1_0_Summary                .setSelection(isCadence_1_0_Summary);
         _rdoCadence_1_1_Summary                .setSelection(isCadence_1_1_Summary);
         _rdoCadence_1_2_Summary                .setSelection(isCadence_1_2_Summary);

         _rdoDistance_1_0_Summary               .setSelection(isDistance_1_0_Summary);
         _rdoDistance_1_1_Summary               .setSelection(isDistance_1_1_Summary);
         _rdoDistance_1_2_Summary               .setSelection(isDistance_1_2_Summary);
         _rdoDistance_1_3_Summary               .setSelection(isDistance_1_3_Summary);

         _rdoElevation_1_0_Summary              .setSelection(isElevation_1_0_Summary);
         _rdoElevation_1_1_Summary              .setSelection(!isElevation_1_0_Summary);

         _rdoPower_1_0_Summary                  .setSelection(isPower_1_0_Summary);
         _rdoPower_1_1_Summary                  .setSelection(!isPower_1_0_Summary);

         _rdoPulse_1_0_Summary                  .setSelection(isPulse_1_0_Summary);
         _rdoPulse_1_1_Summary                  .setSelection(!isPulse_1_0_Summary);

         _rdoSpeed_1_0_Summary                  .setSelection(isSpeed_1_0_Summary);
         _rdoSpeed_1_1_Summary                  .setSelection(isSpeed_1_1_Summary);
         _rdoSpeed_1_2_Summary                  .setSelection(isSpeed_1_2_Summary);

         _rdoTemperature_1_0_Summary            .setSelection(isTemperature_1_0_Summary);
         _rdoTemperature_1_1_Summary            .setSelection(isTemperature_1_1_Summary);
         _rdoTemperature_1_2_Summary            .setSelection(isTemperature_1_2_Summary);

         _rdoTime_Elapsed_HH_Summary            .setSelection(isElapsed_HH_Summary);
         _rdoTime_Elapsed_HH_MM_Summary         .setSelection(isElapsed_HH_MM_Summary);
         _rdoTime_Elapsed_HH_MM_SS_Summary      .setSelection(isElapsed_HH_MM_SS_Summary);

         _rdoTime_Recorded_HH_Summary           .setSelection(isRecorded_HH_Summary);
         _rdoTime_Recorded_HH_MM_Summary        .setSelection(isRecorded_HH_MM_Summary);
         _rdoTime_Recorded_HH_MM_SS_Summary     .setSelection(isRecorded_HH_MM_SS_Summary);

         _rdoTime_Paused_HH_Summary             .setSelection(isPaused_HH_Summary);
         _rdoTime_Paused_HH_MM_Summary          .setSelection(isPaused_HH_MM_Summary);
         _rdoTime_Paused_HH_MM_SS_Summary       .setSelection(isPaused_HH_MM_SS_Summary);

         _rdoTime_Moving_HH_Summary             .setSelection(isMoving_HH_Summary);
         _rdoTime_Moving_HH_MM_Summary          .setSelection(isMoving_HH_MM_Summary);
         _rdoTime_Moving_HH_MM_SS_Summary       .setSelection(isMoving_HH_MM_SS_Summary);

         _rdoTime_Break_HH_Summary              .setSelection(isBreak_HH_Summary);
         _rdoTime_Break_HH_MM_Summary           .setSelection(isBreak_HH_MM_Summary);
         _rdoTime_Break_HH_MM_SS_Summary        .setSelection(isBreak_HH_MM_SS_Summary);
      }

// SET_FORMATTING_ON

      _chkLiveUpdate.setSelection(_prefStore.getDefaultBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE));

      super.performDefaults();

      doLiveUpdate();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      final String cadence                      = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_CADENCE);
      final String distance                     = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE);
      final String elevation                    = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE);
      final String power                        = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_POWER);
      final String pulse                        = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PULSE);
      final String speed                        = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_SPEED);
      final String temperature                  = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE);

      final String elapsedTime                  = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME);
      final String recordedTime                 = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME);
      final String pausedTime                   = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
      final String movingTime                   = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME);
      final String breakTime                    = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME);

      final String cadence_Summary              = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_CADENCE_SUMMARY);
      final String distance_Summary             = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE_SUMMARY);
      final String elevation_Summary            = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE_SUMMARY);
      final String power_Summary                = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_POWER_SUMMARY);
      final String pulse_Summary                = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PULSE_SUMMARY);
      final String speed_Summary                = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_SPEED_SUMMARY);
      final String temperature_Summary          = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE_SUMMARY);

      final String elapsedTime_Summary          = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY);
      final String recordedTime_Summary         = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME_SUMMARY);
      final String pausedTime_Summary           = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME_SUMMARY);
      final String movingTime_Summary           = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME_SUMMARY);
      final String breakTime_Summary            = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME_SUMMARY);

      final boolean isCadence_1_0               = ValueFormat.NUMBER_1_0.name()     .equals(cadence);
      final boolean isCadence_1_1               = ValueFormat.NUMBER_1_1.name()     .equals(cadence);
      final boolean isCadence_1_2               = ValueFormat.NUMBER_1_2.name()     .equals(cadence);
      final boolean isCadence_1_0_Summary       = ValueFormat.NUMBER_1_0.name()     .equals(cadence_Summary);
      final boolean isCadence_1_1_Summary       = ValueFormat.NUMBER_1_1.name()     .equals(cadence_Summary);
      final boolean isCadence_1_2_Summary       = ValueFormat.NUMBER_1_2.name()     .equals(cadence_Summary);

      final boolean isDistance_1_0              = ValueFormat.NUMBER_1_0.name()     .equals(distance);
      final boolean isDistance_1_1              = ValueFormat.NUMBER_1_1.name()     .equals(distance);
      final boolean isDistance_1_2              = ValueFormat.NUMBER_1_2.name()     .equals(distance);
      final boolean isDistance_1_3              = ValueFormat.NUMBER_1_3.name()     .equals(distance);
      final boolean isDistance_1_0_Summary      = ValueFormat.NUMBER_1_0.name()     .equals(distance_Summary);
      final boolean isDistance_1_1_Summary      = ValueFormat.NUMBER_1_1.name()     .equals(distance_Summary);
      final boolean isDistance_1_2_Summary      = ValueFormat.NUMBER_1_2.name()     .equals(distance_Summary);
      final boolean isDistance_1_3_Summary      = ValueFormat.NUMBER_1_3.name()     .equals(distance_Summary);

      final boolean isElevation_1_0             = ValueFormat.NUMBER_1_0.name()     .equals(elevation);
      final boolean isElevation_1_0_Summary     = ValueFormat.NUMBER_1_0.name()     .equals(elevation_Summary);

      final boolean isPower_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(power);
      final boolean isPower_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(power_Summary);

      final boolean isPulse_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(pulse);
      final boolean isPulse_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(pulse_Summary);

      final boolean isSpeed_1_0                 = ValueFormat.NUMBER_1_0.name()     .equals(speed);
      final boolean isSpeed_1_1                 = ValueFormat.NUMBER_1_1.name()     .equals(speed);
      final boolean isSpeed_1_2                 = ValueFormat.NUMBER_1_2.name()     .equals(speed);
      final boolean isSpeed_1_0_Summary         = ValueFormat.NUMBER_1_0.name()     .equals(speed_Summary);
      final boolean isSpeed_1_1_Summary         = ValueFormat.NUMBER_1_1.name()     .equals(speed_Summary);
      final boolean isSpeed_1_2_Summary         = ValueFormat.NUMBER_1_2.name()     .equals(speed_Summary);

      final boolean isTemperature_1_0           = ValueFormat.NUMBER_1_0.name()     .equals(temperature);
      final boolean isTemperature_1_1           = ValueFormat.NUMBER_1_1.name()     .equals(temperature);
      final boolean isTemperature_1_2           = ValueFormat.NUMBER_1_2.name()     .equals(temperature);
      final boolean isTemperature_1_0_Summary   = ValueFormat.NUMBER_1_0.name()     .equals(temperature_Summary);
      final boolean isTemperature_1_1_Summary   = ValueFormat.NUMBER_1_1.name()     .equals(temperature_Summary);
      final boolean isTemperature_1_2_Summary   = ValueFormat.NUMBER_1_2.name()     .equals(temperature_Summary);

      final boolean isElapsed_HH                = ValueFormat.TIME_HH.name()        .equals(elapsedTime);
      final boolean isElapsed_HH_MM             = ValueFormat.TIME_HH_MM.name()     .equals(elapsedTime);
      final boolean isElapsed_HH_MM_SS          = ValueFormat.TIME_HH_MM_SS.name()  .equals(elapsedTime);
      final boolean isElapsed_HH_Summary        = ValueFormat.TIME_HH.name()        .equals(elapsedTime_Summary);
      final boolean isElapsed_HH_MM_Summary     = ValueFormat.TIME_HH_MM.name()     .equals(elapsedTime_Summary);
      final boolean isElapsed_HH_MM_SS_Summary  = ValueFormat.TIME_HH_MM_SS.name()  .equals(elapsedTime_Summary);

      final boolean isRecorded_HH               = ValueFormat.TIME_HH.name()        .equals(recordedTime);
      final boolean isRecorded_HH_MM            = ValueFormat.TIME_HH_MM.name()     .equals(recordedTime);
      final boolean isRecorded_HH_MM_SS         = ValueFormat.TIME_HH_MM_SS.name()  .equals(recordedTime);
      final boolean isRecorded_HH_Summary       = ValueFormat.TIME_HH.name()        .equals(recordedTime_Summary);
      final boolean isRecorded_HH_MM_Summary    = ValueFormat.TIME_HH_MM.name()     .equals(recordedTime_Summary);
      final boolean isRecorded_HH_MM_SS_Summary = ValueFormat.TIME_HH_MM_SS.name()  .equals(recordedTime_Summary);

      final boolean isPaused_HH                 = ValueFormat.TIME_HH.name()        .equals(pausedTime);
      final boolean isPaused_HH_MM              = ValueFormat.TIME_HH_MM.name()     .equals(pausedTime);
      final boolean isPaused_HH_MM_SS           = ValueFormat.TIME_HH_MM_SS.name()  .equals(pausedTime);
      final boolean isPaused_HH_Summary         = ValueFormat.TIME_HH.name()        .equals(pausedTime_Summary);
      final boolean isPaused_HH_MM_Summary      = ValueFormat.TIME_HH_MM.name()     .equals(pausedTime_Summary);
      final boolean isPaused_HH_MM_SS_Summary   = ValueFormat.TIME_HH_MM_SS.name()  .equals(pausedTime_Summary);

      final boolean isMoving_HH                 = ValueFormat.TIME_HH.name()        .equals(movingTime);
      final boolean isMoving_HH_MM              = ValueFormat.TIME_HH_MM.name()     .equals(movingTime);
      final boolean isMoving_HH_MM_SS           = ValueFormat.TIME_HH_MM_SS.name()  .equals(movingTime);
      final boolean isMoving_HH_Summary         = ValueFormat.TIME_HH.name()        .equals(movingTime_Summary);
      final boolean isMoving_HH_MM_Summary      = ValueFormat.TIME_HH_MM.name()     .equals(movingTime_Summary);
      final boolean isMoving_HH_MM_SS_Summary   = ValueFormat.TIME_HH_MM_SS.name()  .equals(movingTime_Summary);

      final boolean isBreak_HH                  = ValueFormat.TIME_HH.name()        .equals(breakTime);
      final boolean isBreak_HH_MM               = ValueFormat.TIME_HH_MM.name()     .equals(breakTime);
      final boolean isBreak_HH_MM_SS            = ValueFormat.TIME_HH_MM_SS.name()  .equals(breakTime);
      final boolean isBreak_HH_Summary          = ValueFormat.TIME_HH.name()        .equals(breakTime_Summary);
      final boolean isBreak_HH_MM_Summary       = ValueFormat.TIME_HH_MM.name()     .equals(breakTime_Summary);
      final boolean isBreak_HH_MM_SS_Summary    = ValueFormat.TIME_HH_MM_SS.name()  .equals(breakTime_Summary);

      _rdoCadence_1_0                           .setSelection(isCadence_1_0);
      _rdoCadence_1_1                           .setSelection(isCadence_1_1);
      _rdoCadence_1_2                           .setSelection(isCadence_1_2);
      _rdoCadence_1_0_Summary                   .setSelection(isCadence_1_0_Summary);
      _rdoCadence_1_1_Summary                   .setSelection(isCadence_1_1_Summary);
      _rdoCadence_1_2_Summary                   .setSelection(isCadence_1_2_Summary);

      _rdoDistance_1_0                          .setSelection(isDistance_1_0);
      _rdoDistance_1_1                          .setSelection(isDistance_1_1);
      _rdoDistance_1_2                          .setSelection(isDistance_1_2);
      _rdoDistance_1_3                          .setSelection(isDistance_1_3);
      _rdoDistance_1_0_Summary                  .setSelection(isDistance_1_0_Summary);
      _rdoDistance_1_1_Summary                  .setSelection(isDistance_1_1_Summary);
      _rdoDistance_1_2_Summary                  .setSelection(isDistance_1_2_Summary);
      _rdoDistance_1_3_Summary                  .setSelection(isDistance_1_3_Summary);

      _rdoElevation_1_0                         .setSelection(isElevation_1_0);
      _rdoElevation_1_1                         .setSelection(!isElevation_1_0);
      _rdoElevation_1_0_Summary                 .setSelection(isElevation_1_0_Summary);
      _rdoElevation_1_1_Summary                 .setSelection(!isElevation_1_0_Summary);

      _rdoPower_1_0                             .setSelection(isPower_1_0);
      _rdoPower_1_1                             .setSelection(!isPower_1_0);
      _rdoPower_1_0_Summary                     .setSelection(isPower_1_0_Summary);
      _rdoPower_1_1_Summary                     .setSelection(!isPower_1_0_Summary);

      _rdoPulse_1_0                             .setSelection(isPulse_1_0);
      _rdoPulse_1_1                             .setSelection(!isPulse_1_0);
      _rdoPulse_1_0_Summary                     .setSelection(isPulse_1_0_Summary);
      _rdoPulse_1_1_Summary                     .setSelection(!isPulse_1_0_Summary);

      _rdoSpeed_1_0                             .setSelection(isSpeed_1_0);
      _rdoSpeed_1_1                             .setSelection(isSpeed_1_1);
      _rdoSpeed_1_2                             .setSelection(isSpeed_1_2);
      _rdoSpeed_1_0_Summary                     .setSelection(isSpeed_1_0_Summary);
      _rdoSpeed_1_1_Summary                     .setSelection(isSpeed_1_1_Summary);
      _rdoSpeed_1_2_Summary                     .setSelection(isSpeed_1_2_Summary);

      _rdoTemperature_1_0                       .setSelection(isTemperature_1_0);
      _rdoTemperature_1_1                       .setSelection(isTemperature_1_1);
      _rdoTemperature_1_2                       .setSelection(isTemperature_1_2);
      _rdoTemperature_1_0_Summary               .setSelection(isTemperature_1_0_Summary);
      _rdoTemperature_1_1_Summary               .setSelection(isTemperature_1_1_Summary);
      _rdoTemperature_1_2_Summary               .setSelection(isTemperature_1_2_Summary);

      _rdoTime_Elapsed_HH                       .setSelection(isElapsed_HH);
      _rdoTime_Elapsed_HH_MM                    .setSelection(isElapsed_HH_MM);
      _rdoTime_Elapsed_HH_MM_SS                 .setSelection(isElapsed_HH_MM_SS);
      _rdoTime_Elapsed_HH_Summary               .setSelection(isElapsed_HH_Summary);
      _rdoTime_Elapsed_HH_MM_Summary            .setSelection(isElapsed_HH_MM_Summary);
      _rdoTime_Elapsed_HH_MM_SS_Summary         .setSelection(isElapsed_HH_MM_SS_Summary);

      _rdoTime_Recorded_HH                      .setSelection(isRecorded_HH);
      _rdoTime_Recorded_HH_MM                   .setSelection(isRecorded_HH_MM);
      _rdoTime_Recorded_HH_MM_SS                .setSelection(isRecorded_HH_MM_SS);
      _rdoTime_Recorded_HH_Summary              .setSelection(isRecorded_HH_Summary);
      _rdoTime_Recorded_HH_MM_Summary           .setSelection(isRecorded_HH_MM_Summary);
      _rdoTime_Recorded_HH_MM_SS_Summary        .setSelection(isRecorded_HH_MM_SS_Summary);

      _rdoTime_Paused_HH                        .setSelection(isPaused_HH);
      _rdoTime_Paused_HH_MM                     .setSelection(isPaused_HH_MM);
      _rdoTime_Paused_HH_MM_SS                  .setSelection(isPaused_HH_MM_SS);
      _rdoTime_Paused_HH_Summary                .setSelection(isPaused_HH_Summary);
      _rdoTime_Paused_HH_MM_Summary             .setSelection(isPaused_HH_MM_Summary);
      _rdoTime_Paused_HH_MM_SS_Summary          .setSelection(isPaused_HH_MM_SS_Summary);

      _rdoTime_Moving_HH                        .setSelection(isMoving_HH);
      _rdoTime_Moving_HH_MM                     .setSelection(isMoving_HH_MM);
      _rdoTime_Moving_HH_MM_SS                  .setSelection(isMoving_HH_MM_SS);
      _rdoTime_Moving_HH_Summary                .setSelection(isMoving_HH_Summary);
      _rdoTime_Moving_HH_MM_Summary             .setSelection(isMoving_HH_MM_Summary);
      _rdoTime_Moving_HH_MM_SS_Summary          .setSelection(isMoving_HH_MM_SS_Summary);

      _rdoTime_Break_HH                         .setSelection(isBreak_HH);
      _rdoTime_Break_HH_MM                      .setSelection(isBreak_HH_MM);
      _rdoTime_Break_HH_MM_SS                   .setSelection(isBreak_HH_MM_SS);
      _rdoTime_Break_HH_Summary                 .setSelection(isBreak_HH_Summary);
      _rdoTime_Break_HH_MM_Summary              .setSelection(isBreak_HH_MM_Summary);
      _rdoTime_Break_HH_MM_SS_Summary           .setSelection(isBreak_HH_MM_SS_Summary);

// SET_FORMATTING_ON

      // live update
      _chkLiveUpdate.setSelection(_prefStore.getBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE));

      // folder
      _tabFolder.setSelection(_prefStore.getInt(ICommonPreferences.DISPLAY_FORMAT_SELECTED_TAB));
   }

   private void saveState() {

      final String cadenceFormat = _rdoCadence_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoCadence_1_1.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String distanceFormat = _rdoDistance_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoDistance_1_1.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : _rdoDistance_1_2.getSelection()
                        ? ValueFormat.NUMBER_1_2.name()
                        : ValueFormat.NUMBER_1_3.name();

      final String elevationFormat = _rdoElevation_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String powerFormat = _rdoPower_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String pulseFormat = _rdoPulse_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String speedFormat = _rdoSpeed_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoSpeed_1_1.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String temperatureFormat = _rdoTemperature_1_0.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoTemperature_1_1.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String elapsedFormat = _rdoTime_Elapsed_HH.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Elapsed_HH_MM.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String recordedFormat = _rdoTime_Recorded_HH.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Recorded_HH_MM.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String pausedFormat = _rdoTime_Paused_HH.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Paused_HH_MM.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String movingFormat = _rdoTime_Moving_HH.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Moving_HH_MM.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String breakFormat = _rdoTime_Break_HH.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Break_HH_MM.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      //
      // SUMMARY   SUMMARY   SUMMARY   SUMMARY   SUMMARY   SUMMARY   SUMMARY
      //

      final String cadenceFormat_Summary = _rdoCadence_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoCadence_1_1_Summary.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String distanceFormat_Summary = _rdoDistance_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoDistance_1_1_Summary.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : _rdoDistance_1_2_Summary.getSelection()
                        ? ValueFormat.NUMBER_1_2.name()
                        : ValueFormat.NUMBER_1_3.name();

      final String elevationFormat_Summary = _rdoElevation_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String powerFormat_Summary = _rdoPower_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String pulseFormat_Summary = _rdoPulse_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : ValueFormat.NUMBER_1_1.name();

      final String speedFormat_Summary = _rdoSpeed_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoSpeed_1_1_Summary.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String temperatureFormat_Summary = _rdoTemperature_1_0_Summary.getSelection()
            ? ValueFormat.NUMBER_1_0.name()
            : _rdoTemperature_1_1_Summary.getSelection()
                  ? ValueFormat.NUMBER_1_1.name()
                  : ValueFormat.NUMBER_1_2.name();

      final String elapsedFormat_Summary = _rdoTime_Elapsed_HH_Summary.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Elapsed_HH_MM_Summary.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String recordedFormat_Summary = _rdoTime_Recorded_HH_Summary.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Recorded_HH_MM_Summary.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String pausedFormat_Summary = _rdoTime_Paused_HH_Summary.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Paused_HH_MM_Summary.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String movingFormat_Summary = _rdoTime_Moving_HH_Summary.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Moving_HH_MM_Summary.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

      final String breakFormat_Summary = _rdoTime_Break_HH_Summary.getSelection()
            ? ValueFormat.TIME_HH.name()
            : _rdoTime_Break_HH_MM_Summary.getSelection()
                  ? ValueFormat.TIME_HH_MM.name()
                  : ValueFormat.TIME_HH_MM_SS.name();

// SET_FORMATTING_OFF

      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_CADENCE,                cadenceFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_DISTANCE,               distanceFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE,               elevationFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_POWER,                  powerFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PULSE,                  pulseFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_SPEED,                  speedFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE,            temperatureFormat);

      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME,           elapsedFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME,          recordedFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME,            pausedFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME,            movingFormat);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME,             breakFormat);

      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_CADENCE_SUMMARY,        cadenceFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_DISTANCE_SUMMARY,       distanceFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE_SUMMARY,       elevationFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_POWER_SUMMARY,          powerFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PULSE_SUMMARY,          pulseFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_SPEED_SUMMARY,          speedFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_TEMPERATURE_SUMMARY,    temperatureFormat_Summary);

      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY,   elapsedFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_RECORDED_TIME_SUMMARY,  recordedFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME_SUMMARY,    pausedFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_MOVING_TIME_SUMMARY,    movingFormat_Summary);
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_BREAK_TIME_SUMMARY,     breakFormat_Summary);

// SET_FORMATTING_ON

      // live update
      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());

      /*
       * Publish modifications
       */
      FormatManager.updateDisplayFormats();

      // fire one event for all modifications
      _prefStore.setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
   }

   private void saveUIState() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }
}
