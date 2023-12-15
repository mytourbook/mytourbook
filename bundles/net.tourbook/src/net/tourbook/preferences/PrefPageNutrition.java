/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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

import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageNutrition extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String ID = "net.tourbook.preferences.PrefPageNutrition"; //$NON-NLS-1$

   private PixelConverter     _pixelConverter;
   private int                _defaultInfoWidth;
   private int                _defaultSpinnerWidth;
   private MouseWheelListener _defaultMouseWheelListener;


   /*
    * UI controls
    */
   private Spinner _spinnerCaloriesPerHourTarget;
   private Button             _chkIgnoreFirstHour;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .spacing(5, 15)
            .applyTo(container);
      {
         /*
          * Ignore 1st hour
          */
         {
            _chkIgnoreFirstHour = new Button(container, SWT.CHECK);
            _chkIgnoreFirstHour.setText("Ignore 1st hour in nutrition averages computation");
            _chkIgnoreFirstHour.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               //todo fb
            }));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_chkIgnoreFirstHour);
         }
         createUI_20_NutritionTargets(container);
      }

      return container;
   }

   private void createUI_20_NutritionTargets(final Composite parent) {

      final int verticalIndent = 20;

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Messages.Pref_TourNutrition_Group_Targets");
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         /*
          * label: cache size
          */
         final Label label = UI.createLabel(group, "Calories (kcal/hr) Messages.Pref_TourNutrition_Label_CaloriesTarget");
         GridDataFactory.fillDefaults()
               .indent(0, verticalIndent)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(label);

         // spinner: cache size
         _spinnerCaloriesPerHourTarget = new Spinner(group, SWT.BORDER);

         // cache size ==1 causes "java.lang.IllegalStateException: Queue full"
         _spinnerCaloriesPerHourTarget.setMinimum(2);
         _spinnerCaloriesPerHourTarget.setMaximum(100_000);
         _spinnerCaloriesPerHourTarget.addMouseWheelListener(_defaultMouseWheelListener);
         GridDataFactory.fillDefaults()
               .indent(0, verticalIndent)
               .hint(_defaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinnerCaloriesPerHourTarget);
      }
   }
   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pixelConverter = new PixelConverter(parent);
      _defaultInfoWidth = _pixelConverter.convertWidthInCharsToPixels(50);
      _defaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pixelConverter.convertWidthInCharsToPixels(UI.IS_OSX ? 14 : 7);

      _defaultMouseWheelListener = mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
   }

   @Override
   protected void performDefaults() {

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      return super.performOk();
   }

}
