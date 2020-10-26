/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import net.tourbook.common.UI;
import net.tourbook.measurement_system.MeasurementSystem;
import net.tourbook.measurement_system.MeasurementSystem_Manager;
import net.tourbook.preferences.ITourbookPreferences;
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

   private final static IPreferenceStore _prefStore            = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener       _prefChangeListener;

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

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

               _isFireSelectionEvent = false;
               {
                  updateUI_MeasurementSystem();
               }
               _isFireSelectionEvent = true;
            }
         }

      };
      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
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
            _prefStore.removePropertyChangeListener(_prefChangeListener);
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

      MeasurementSystem_Manager.setActiveSystemProfileIndex(selectedIndex);
   }

   private void selectActiveSystem() {

      final String systemData = UI.EMPTY_STRING

            + Messages.Pref_System_Label_Distance + UI.DASH_WITH_SPACE + MeasurementSystem_Manager.getActiveSystem_Distance().getLabel() + NL
            + Messages.Pref_System_Label_Elevation + UI.DASH_WITH_SPACE + MeasurementSystem_Manager.getActiveSystem_Elevation().getLabel() + NL
            + Messages.Pref_System_Label_Temperature + UI.DASH_WITH_SPACE + MeasurementSystem_Manager.getActiveSystem_Temperature().getLabel() + NL
            + Messages.Pref_System_Label_Weight + UI.DASH_WITH_SPACE + MeasurementSystem_Manager.getActiveSystem_Weight().getLabel() + NL

            + Messages.Pref_System_Label_AtmosphericPressure + UI.DASH_WITH_SPACE
            + MeasurementSystem_Manager.getActiveSystem_AtmosphericPressure().getLabel()

      ;

      final int activeSystemProfileIndex = MeasurementSystem_Manager.getActiveSystem_ProfileIndex();
      _combo.select(activeSystemProfileIndex);

      _combo.setToolTipText(String.format(Messages.Measurement_System_Tooltip, systemData));
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
