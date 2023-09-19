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
package net.tourbook.tour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TimeZoneData;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

class DialogSetTimeZone_WizardPage extends WizardPage {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private PixelConverter         _pc;

   /*
    * UI controls
    */
   private Combo  _comboTimeZone;

   private Link   _linkDefaultTimeZone;

   private Button _rdoSetTimeZone_Remove;
   private Button _rdoSetTimeZone_FromList;
   private Button _rdoSetTimeZone_FromGeo;
   private Button _rdoSetTourStart_YYMMDD;

   protected DialogSetTimeZone_WizardPage(final String pageName) {

      super(pageName);

      setTitle(Messages.Dialog_SetTimeZone_Dialog_Title);
   }

   @Override
   public void createControl(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite page = createUI(parent);

      // set wizard page control
      setControl(page);

      restoreState();
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.swtDefaults().applyTo(container);
      {
         createUI_10_Controls(container);
      }

      return container;
   }

   private void createUI_10_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Info
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.Dialog_SetTimeZone_Label_Info);
         }
         {
            /*
             * Set time zone from geo position
             */

            // radio
            _rdoSetTimeZone_FromGeo = new Button(container, SWT.RADIO);
            _rdoSetTimeZone_FromGeo.setText(Messages.Dialog_SetTimeZone_Radio_SetTimeZone_FromGeo);
            GridDataFactory.fillDefaults()//
                  .indent(0, 10)
                  .applyTo(_rdoSetTimeZone_FromGeo);
         }
         {
            /*
             * Set time zone from list
             */

            // radio
            _rdoSetTimeZone_FromList = new Button(container, SWT.RADIO);
            _rdoSetTimeZone_FromList.setText(Messages.Dialog_SetTimeZone_Radio_SetTimeZone_FromCombo);

            // content
            final Composite setContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(setContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(setContainer);
            {
               {
                  // combo
                  _comboTimeZone = new Combo(setContainer, SWT.READ_ONLY | SWT.BORDER);
                  _comboTimeZone.setVisibleItemCount(50);
                  GridDataFactory.fillDefaults()//
                        .indent(_pc.convertWidthInCharsToPixels(2), 0)
                        .align(SWT.BEGINNING, SWT.FILL)
                        .applyTo(_comboTimeZone);

                  // fill combobox
                  for (final TimeZoneData timeZone : TimeTools.getAllTimeZones()) {
                     _comboTimeZone.add(timeZone.label);
                  }
               }

               {
                  // link: set default

                  _linkDefaultTimeZone = new Link(setContainer, SWT.NONE);
                  _linkDefaultTimeZone.setText(Messages.Tour_Editor_Link_SetDefaultTimeZone);
                  _linkDefaultTimeZone.setToolTipText(NLS.bind(
                        Messages.Tour_Editor_Link_SetDefaultTimeZone_Tooltip,
                        TimeTools.getDefaultTimeZoneId()));
                  _linkDefaultTimeZone.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

                     // select default time zone
                     _comboTimeZone.select(TimeTools.getTimeZoneIndex_Default());
                  }));
               }
            }
            {
               /*
                * Remove Time zone
                */

               // radio
               _rdoSetTimeZone_Remove = new Button(container, SWT.RADIO);
               _rdoSetTimeZone_Remove.setText(Messages.Dialog_SetTimeZone_Radio_RemoveTimeZone);
            }
            {
               /**
                * Adjust tour start YYMMDD with time zone
                * <p>
                * Year/month/day of the tour start is kept separately to be used e.g. in the Tour
                * Book view. This will set just these components which could be wrong when the tour
                * import had a bug.
                */

               // radio
               _rdoSetTourStart_YYMMDD = new Button(container, SWT.RADIO);
               _rdoSetTourStart_YYMMDD.setText(Messages.Dialog_SetTimeZone_Radio_AdjustTourStart_YYMMDD);
               _rdoSetTourStart_YYMMDD.setToolTipText(Messages.Dialog_SetTimeZone_Radio_AdjustTourStartYYMMDD_Tooltip);
            }

         }
      }
   }

   private void restoreState() {

      final int timeZoneAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SET_TIME_ZONE_ACTION);
      final String timeZoneId = _prefStore.getString(ITourbookPreferences.DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID);
      final int timeZoneIndex = TimeTools.getTimeZoneIndex_WithDefault(timeZoneId);

      _rdoSetTimeZone_FromList.setSelection(timeZoneAction == DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_LIST);
      _rdoSetTimeZone_FromGeo.setSelection(timeZoneAction == DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_GEO_POSITION);
      _rdoSetTimeZone_Remove.setSelection(timeZoneAction == DialogSetTimeZone.TIME_ZONE_ACTION_REMOVE_TIME_ZONE);
      _rdoSetTourStart_YYMMDD.setSelection(timeZoneAction == DialogSetTimeZone.TIME_ZONE_ACTION_ADJUST_TOUR_START_YYMMDD);

      _comboTimeZone.select(timeZoneIndex);
   }

   void saveState() {

      final int selectedTimeZoneIndex = _comboTimeZone.getSelectionIndex();
      final TimeZoneData timeZoneData = TimeTools.getTimeZone_ByIndex(selectedTimeZoneIndex);

      final String timeZoneId = timeZoneData.zoneId;

      int timeZoneAction = -1;

      if (_rdoSetTimeZone_FromList.getSelection()) {
         timeZoneAction = DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_LIST;

      } else if (_rdoSetTimeZone_FromGeo.getSelection()) {
         timeZoneAction = DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_GEO_POSITION;

      } else if (_rdoSetTimeZone_Remove.getSelection()) {
         timeZoneAction = DialogSetTimeZone.TIME_ZONE_ACTION_REMOVE_TIME_ZONE;

      } else if (_rdoSetTourStart_YYMMDD.getSelection()) {
         timeZoneAction = DialogSetTimeZone.TIME_ZONE_ACTION_ADJUST_TOUR_START_YYMMDD;

      }

      _prefStore.setValue(ITourbookPreferences.DIALOG_SET_TIME_ZONE_ACTION, timeZoneAction);
      _prefStore.setValue(ITourbookPreferences.DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID, timeZoneId);
   }

}
