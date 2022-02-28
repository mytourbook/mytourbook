/*******************************************************************************
 * Copyright (C) 2019, 2022 Frédéric Bard
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

import net.tourbook.ui.views.WeatherProvidersUI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class PrefPageWeather extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String ID = "net.tourbook.preferences.PrefPageWeather"; //$NON-NLS-1$
   private int                DEFAULT_DESCRIPTION_WIDTH;

   private WeatherProvidersUI _weatherProvidersUI;
   private ScrolledComposite  _smoothingScrolledContainer;
   private Composite          _smoothingScrolledContent;

   private FormToolkit        _tk;
   private PixelConverter     _pc;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      return container;
   }

   private Composite createUI(final Composite parent) {

      _tk = new FormToolkit(parent.getDisplay());
      _weatherProvidersUI = new WeatherProvidersUI();

      _smoothingScrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_smoothingScrolledContainer);
      {
         _smoothingScrolledContent = _tk.createComposite(_smoothingScrolledContainer);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .hint(DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
               .applyTo(_smoothingScrolledContent);
         GridLayoutFactory.swtDefaults()
               .extendedMargins(5, 5, 10, 5)
               .numColumns(1)
               .applyTo(_smoothingScrolledContent);
//  _smoothingScrolledContent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
         {
            _weatherProvidersUI.createUI(_smoothingScrolledContent);
         }

         // setup scrolled container
         _smoothingScrolledContainer.setExpandVertical(true);
         _smoothingScrolledContainer.setExpandHorizontal(true);
         _smoothingScrolledContainer.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(final ControlEvent e) {
               _smoothingScrolledContainer.setMinSize(//
                     _smoothingScrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
         });

         _smoothingScrolledContainer.setContent(_smoothingScrolledContent);
      }

      return _smoothingScrolledContainer;
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {
      _pc = new PixelConverter(parent);

      DEFAULT_DESCRIPTION_WIDTH = _pc.convertWidthInCharsToPixels(70);
   }

}
