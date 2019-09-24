/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views;

import net.tourbook.Messages;
import net.tourbook.common.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

class SmoothingUI_NoSmoothing implements ISmoothingAlgorithm {

   /*
    * UI controls
    */
   private FormToolkit _tk;

   SmoothingUI_NoSmoothing() {}

   @Override
   public Composite createUI(final SmoothingUI smoothingUI,
                             final Composite parent,
                             final FormToolkit tk,
                             final boolean isShowDescription,
                             final boolean isShowAdditionalActions) {

      _tk = tk;

      return createUI_10(parent, isShowDescription);
   }

   private Composite createUI_10(final Composite parent, final boolean isShowDescription) {

      final Composite container = _tk.createComposite(parent);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         // label: The computation of the speed (and pace) value for one ...
         final Label label = _tk.createLabel(container, Messages.TourChart_Smoothing_Label_NoSmoothingAlgorithm, SWT.WRAP);
         GridDataFactory.fillDefaults()
               .indent(0, 10)
               .hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .grab(true, false)
               .applyTo(label);
      }

      return container;
   }

   @Override
   public void dispose() {}

   @Override
   public void performDefaults(final boolean isFireModifications) {}

   @Override
   public void updateUIFromPrefStore() {}

}
