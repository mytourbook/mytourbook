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
   //todo fb Tooltip
   // p 109 Performance Nutrition for runners M. Fitzgerald
   // "Research has shown...."
   private Button _chkIgnoreFirstHour;

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
      }

      return container;
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