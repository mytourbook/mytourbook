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
package net.tourbook.application;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.measurement_system.MeasurementSystem;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.CustomControlContribution;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class MeasurementSystemContributionItem extends CustomControlContribution {

   private static final String           ID                    = "net.tourbook.measurementSelector"; //$NON-NLS-1$

   private static final char             NL                    = UI.NEW_LINE;

   private final static IPreferenceStore _prefStore_Common     = CommonActivator.getPrefStore();

   private IPropertyChangeListener       _prefChangeListener_Common;

   private boolean                       _isFireSelectionEvent = true;

   private Combo                         _combo;

   public MeasurementSystemContributionItem() {
      this(ID);
   }

   protected MeasurementSystemContributionItem(final String id) {
      super(id);
   }

   /**
    * listen for changes in the person list
    */
   private void addPrefListener() {

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

               _isFireSelectionEvent = false;
               {
                  updateUI_MeasurementSystem();
               }
               _isFireSelectionEvent = true;
            }
         }

      };

      // register the listener
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   @Override
   protected Control createControl(final Composite parent) {

      final Composite ui = createUI(parent);

      addPrefListener();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      if (net.tourbook.common.UI.IS_OSX) {

         return createUI_10_ComboBox(parent);

      } else {

         /*
          * on win32 a few pixel above and below the combobox are drawn, wrapping it into a
          * composite removes the pixels
          */
         final Composite container = new Composite(parent, SWT.NONE);
         GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(container);
         {
            final Composite control = createUI_10_ComboBox(container);
            control.setLayoutData(new GridData(SWT.NONE, SWT.CENTER, false, true));
         }

         return container;
      }
   }

   private Composite createUI_10_ComboBox(final Composite parent) {

      _combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
      _combo.setToolTipText(Messages.App_measurement_tooltip);

      _combo.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);
         }
      });

      _combo.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {

            if (_isFireSelectionEvent == false) {
               return;
            }

            if (TourManager.isTourEditorModified()) {

               // prevent to change the measurement system when a tour is modified in the tour editor
               // -> select old measurement system

               _combo.getDisplay().asyncExec(() -> {

                  // restore previous selection
                  selectActiveSystem();
               });

            } else {

               onSelectSystem();
            }
         }
      });

      updateUI_MeasurementSystem();

      return _combo;
   }

   private void onSelectSystem() {

      final int selectedIndex = _combo.getSelectionIndex();

      if (selectedIndex == -1) {
         return;
      }

      MeasurementSystem_Manager.setActiveSystemProfileIndex(selectedIndex, true);
   }

   private void selectActiveSystem() {

      final String DASH = UI.DASH_WITH_SPACE;

// SET_FORMATTING_OFF

      final String tooltipData = UI.EMPTY_STRING

         + OtherMessages.PREF_SYSTEM_LABEL_DISTANCE              + DASH + MeasurementSystem_Manager.getActiveSystemOption_Distance().getLabel()      + NL
         + OtherMessages.PREF_SYSTEM_LABEL_LENGTH                + DASH + MeasurementSystem_Manager.getActiveSystemOption_Length().getLabel()        + NL
         + OtherMessages.PREF_SYSTEM_LABEL_LENGTH_SMALL          + DASH + MeasurementSystem_Manager.getActiveSystemOption_Length_Small().getLabel()  + NL
         + OtherMessages.PREF_SYSTEM_LABEL_ELEVATION             + DASH + MeasurementSystem_Manager.getActiveSystemOption_Elevation().getLabel()     + NL
         + OtherMessages.PREF_SYSTEM_LABEL_HEIGHT                + DASH + MeasurementSystem_Manager.getActiveSystemOption_Height().getLabel()        + NL
         + OtherMessages.PREF_SYSTEM_LABEL_PACE                  + DASH + MeasurementSystem_Manager.getActiveSystemOption_Pace().getLabel()          + NL
         + OtherMessages.PREF_SYSTEM_LABEL_TEMPERATURE           + DASH + MeasurementSystem_Manager.getActiveSystemOption_Temperature().getLabel()   + NL
         + OtherMessages.PREF_SYSTEM_LABEL_WEIGHT                + DASH + MeasurementSystem_Manager.getActiveSystemOption_Weight().getLabel()        + NL
         + OtherMessages.PREF_SYSTEM_LABEL_PRESSURE_ATMOSPHERE   + DASH + MeasurementSystem_Manager.getActiveSystemOption_Pressure_Atmospheric().getLabel()

      ;
// SET_FORMATTING_ON

      final int activeSystemProfileIndex = MeasurementSystem_Manager.getActiveSystem_ProfileIndex();
      _combo.select(activeSystemProfileIndex);

      _combo.setToolTipText(String.format(OtherMessages.MEASUREMENT_SYSTEM_TOOLTIP, tooltipData));
   }

   private void updateUI_MeasurementSystem() {

      _combo.removeAll();

      // fill combo box
      for (final MeasurementSystem systemProfile : MeasurementSystem_Manager.getCurrentProfiles()) {
         _combo.add(systemProfile.getName());
      }

      // the names could have a different lenght -> show the whole system name
      _combo.getParent().layout(true, true);

      // select saved system
      selectActiveSystem();
   }
}
