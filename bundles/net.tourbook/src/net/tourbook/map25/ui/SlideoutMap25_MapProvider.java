/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.Map25View;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

/**
 * 2.5D map provider slideout
 */
public class SlideoutMap25_MapProvider extends ToolbarSlideout implements IMapProviderListener {

   private SelectionAdapter         _defaultSelectionListener;
   private MouseWheelListener       _defaultMouseWheelListener;
   private FocusListener            _keepOpenListener;

   private PixelConverter           _pc;

   private Map25View                _map25View;

   private ArrayList<Map25Provider> _allMapProvider;

   private boolean                  _isInUpdateUI;

   /*
    * UI controls
    */
   private Combo _comboMapProvider;

   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_MapProvider(final Control ownerControl,
                                    final ToolBar toolBar,
                                    final Map25View map25View) {

      super(ownerControl, toolBar);

      _map25View = map25View;

      Map25ProviderManager.addMapProviderListener(this);

   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableActions();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory
               .fillDefaults()//
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_MapProvider(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25Provider_Label_MapProvider);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
   }

   private void createUI_20_MapProvider(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            _comboMapProvider = new Combo(container, SWT.READ_ONLY);
            _comboMapProvider.addFocusListener(_keepOpenListener);
            _comboMapProvider.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_MapProvider();
               }
            });
         }
      }

      fillMapProvider();
   }

   private void enableActions() {

   }

   private void fillMapProvider() {

      final ArrayList<Map25Provider> allMapProviders = Map25ProviderManager.getAllMapProviders();

      Collections.sort(allMapProviders, new Comparator<Map25Provider>() {

         @Override
         public int compare(final Map25Provider mp1, final Map25Provider mp2) {
            return mp1.name.compareTo(mp2.name);
         }
      });

      _allMapProvider = allMapProviders;

      _comboMapProvider.removeAll();

      for (final Map25Provider map25Provider : allMapProviders) {
         _comboMapProvider.add(map25Provider.name);
      }
   }

   /**
    * @return Returns the used map provider in the map
    */
   private Map25Provider getCurrentMapProvider() {

      return _map25View.getMapApp().getSelectedMapProvider();
   }

   private Map25Provider getSelectedMapProvider() {

      final int selectedIndex = _comboMapProvider.getSelectionIndex();

      if (selectedIndex < 0) {

         return _allMapProvider.get(0);

      } else {

         return _allMapProvider.get(selectedIndex);
      }
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeUI();
         }
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   @Override
   public void mapProviderListChanged() {

      fillMapProvider();
   }

   private void onChangeUI() {

      saveState();

      enableActions();
   }

   @Override
   protected void onDispose() {

      Map25ProviderManager.removeMapProviderListener(this);

      super.onDispose();
   }

   private void onSelect_MapProvider() {

      if (_isInUpdateUI) {
         return;
      }

      final Map25Provider selectedMapProvider = getSelectedMapProvider();

      // check if a new map provider is selected
      if (selectedMapProvider == getCurrentMapProvider()) {
         return;
      }

      _map25View.getMapApp().setMapProvider(selectedMapProvider);
   }

   private void restoreState() {

      /*
       * Reselect map provider
       */
      final Map25Provider currentMapProvider = getCurrentMapProvider();
      for (int providerIndex = 0; providerIndex < _allMapProvider.size(); providerIndex++) {

         final Map25Provider map25Provider = _allMapProvider.get(providerIndex);
         if (currentMapProvider.equals(map25Provider)) {

            _isInUpdateUI = true;
            {
               _comboMapProvider.select(providerIndex);
            }
            _isInUpdateUI = false;

            break;
         }
      }

   }

   private void saveState() {

   }

}
