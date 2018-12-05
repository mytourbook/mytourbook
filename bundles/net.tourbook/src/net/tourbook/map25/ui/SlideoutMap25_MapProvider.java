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
import java.util.List;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.oscim.theme.VtmThemes;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.Map25View;
import net.tourbook.preferences.MapsforgeThemeStyle;
import net.tourbook.preferences.PrefPage_Map25Provider;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

/**
 * 2.5D map provider slideout
 */
public class SlideoutMap25_MapProvider extends ToolbarSlideout implements IMapProviderListener {

   private FocusListener            _keepOpenListener;

   private ActionOpenPrefDialog     _actionPrefDialog;

   private Map25View                _map25View;

   private ArrayList<Map25Provider> _allEnabledMapProvider;

   private boolean                  _isInUpdateUI;

   /*
    * UI controls
    */
   private Composite _parent;

   private Combo     _comboMapProvider;
   private Combo     _comboTheme;
   private Combo     _comboThemeStyle;

   private Label     _lblMapProvider;
   private Label     _lblTheme;
   private Label     _lblThemeStyle;

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

      _actionPrefDialog = new ActionOpenPrefDialog(
            Messages.Pref_Map25_Action_EditMapProviderPreferences_Tooltip,
            PrefPage_Map25Provider.ID);

      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_parent.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
         }
         createUI_20_Options(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Slideout_Map25Provider_Label_MapProvider_Title);
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Map provider
             */
            _lblMapProvider = new Label(container, SWT.NONE);
            _lblMapProvider.setText(Messages.Slideout_Map25Provider_Label_MapProvider);

            _comboMapProvider = new Combo(container, SWT.READ_ONLY);
            _comboMapProvider.addFocusListener(_keepOpenListener);
            _comboMapProvider.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_MapProvider();
               }
            });
         }
         {
            /*
             * Combo: Theme
             */
            _lblTheme = new Label(container, SWT.NONE);
            _lblTheme.setText(Messages.Slideout_Map25Provider_Label_DefaultTheme);

            _comboTheme = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
            _comboTheme.addFocusListener(_keepOpenListener);
            _comboTheme.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_Theme();
               }
            });
         }
         {
            /*
             * Theme style
             */
            _lblThemeStyle = new Label(container, SWT.NONE);
            _lblThemeStyle.setText(Messages.Slideout_Map25Provider_Label_ThemeStyle);

            _comboThemeStyle = new Combo(container, SWT.READ_ONLY);
            _comboThemeStyle.addFocusListener(_keepOpenListener);
            _comboThemeStyle.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelect_ThemeStyle();
               }
            });
         }
      }

      fillMapProvider();
   }

   private void fillMapProvider() {

      final ArrayList<Map25Provider> allMapProviders = Map25ProviderManager.getAllMapProviders();
      final ArrayList<Map25Provider> allEnabledMapProviders = new ArrayList<>();

      for (final Map25Provider map25Provider : allMapProviders) {

         // hide disabled map provider

         if (map25Provider.isEnabled) {
            allEnabledMapProviders.add(map25Provider);
         }
      }

      Collections.sort(allEnabledMapProviders, new Comparator<Map25Provider>() {

         @Override
         public int compare(final Map25Provider mp1, final Map25Provider mp2) {
            return mp1.name.compareTo(mp2.name);
         }
      });

      _allEnabledMapProvider = allEnabledMapProviders;

      _comboMapProvider.removeAll();

      for (final Map25Provider map25Provider : allEnabledMapProviders) {
         _comboMapProvider.add(map25Provider.name);
      }
   }

   private Map25Provider getSelectedMapProvider() {

      final int selectedIndex = _comboMapProvider.getSelectionIndex();

      if (selectedIndex < 0) {

         return _allEnabledMapProvider.get(0);

      } else {

         return _allEnabledMapProvider.get(selectedIndex);
      }
   }

   /**
    * @return Returns the map provider which is currently used in the map
    */
   private Map25Provider getUsedMapProvider() {

      return _map25View.getMapApp().getSelectedMapProvider();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

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

      if (_comboMapProvider == null) {

         // this can occure when dialog is closed and the event is still fired

         return;
      }

      fillMapProvider();
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
      if (selectedMapProvider == getUsedMapProvider()) {
         return;
      }

      updateUI_Theme(selectedMapProvider);
      updateUI_ThemeStyle(selectedMapProvider);
      updateUI_MapProvider_Tooltip(selectedMapProvider);

      // update UI otherwise the old theme style box is displayed until the next redraw
      _parent.update();

      // update UI
      updateMap(selectedMapProvider);
   }

   private void onSelect_Theme() {

      final Map25Provider mapProvider = getSelectedMapProvider();

      final int selectedIndex = _comboTheme.getSelectionIndex();

      mapProvider.offline_IsThemeFromFile = mapProvider.isOfflineMap && selectedIndex == 0;

      int themeIndex = selectedIndex;

      // adjust index for offline maps because the first item is not a theme
      if (mapProvider.isOfflineMap) {
         themeIndex--;
      }

      final VtmThemes[] themeValues = VtmThemes.values();
      mapProvider.theme = themeValues[themeIndex];

      // update UI
      updateMap(mapProvider);
   }

   private void onSelect_ThemeStyle() {

      if (_isInUpdateUI) {
         return;
      }

      final Map25Provider mapProvider = getSelectedMapProvider();
      final List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);

      final int selectedStyleIndex = _comboThemeStyle.getSelectionIndex();

      // update model
      mapProvider.offline_ThemeStyle = mfStyles.get(selectedStyleIndex).getXmlLayer();

      // update UI
      updateMap(mapProvider);
   }

   private void restoreState() {

      /*
       * Reselect map provider
       */
      final Map25Provider currentMapProvider = getUsedMapProvider();

      selectMapProvider(currentMapProvider);

      updateUI_Theme(currentMapProvider);
      updateUI_ThemeStyle(currentMapProvider);
      updateUI_MapProvider_Tooltip(currentMapProvider);
   }

   public void selectMapProvider(final Map25Provider mapProvider) {

      if (_allEnabledMapProvider == null) {

         // this can occure when not yet fully setup

         return;
      }

      for (int providerIndex = 0; providerIndex < _allEnabledMapProvider.size(); providerIndex++) {

         final Map25Provider map25Provider = _allEnabledMapProvider.get(providerIndex);
         if (mapProvider.equals(map25Provider)) {

            _isInUpdateUI = true;
            {
               _comboMapProvider.select(providerIndex);
            }
            _isInUpdateUI = false;

            break;
         }
      }
   }

   private void updateMap(final Map25Provider mapProvider) {

      _map25View.getMapApp().setMapProvider(mapProvider);
   }

   private void updateUI_MapProvider_Tooltip(final Map25Provider mapProvider) {

      String mpTooltip = UI.EMPTY_STRING;

      if (mapProvider.isOfflineMap) {

         // offline

         final List<MapsforgeThemeStyle> themeStyles = mapProvider.getThemeStyles(false);

         final CharSequence[] allThemeStyles = new CharSequence[themeStyles.size()];
         for (int styleIndex = 0; styleIndex < themeStyles.size(); styleIndex++) {
            final MapsforgeThemeStyle themeStyle = themeStyles.get(styleIndex);
            allThemeStyles[styleIndex] = UI.SPACE4 + themeStyle.getLocaleName() + UI.DASH_WITH_SPACE + themeStyle.getXmlLayer();
         }

         final String themeStyleText = String.join(UI.NEW_LINE1, allThemeStyles);

         mpTooltip = String.format(Messages.Slideout_Map25Provider_Combo_MapProvider_Offline_Tooltip,

               mapProvider.tileEncoding,
               mapProvider.offline_MapFilepath,
               mapProvider.offline_ThemeFilepath,
               themeStyleText

         );

      } else {

         // online

         mpTooltip = String.format(Messages.Slideout_Map25Provider_Combo_MapProvider_Online_Tooltip,

               mapProvider.tileEncoding,
               mapProvider.online_url + mapProvider.online_TilePath,
               mapProvider.online_ApiKey

         );
      }

      _lblMapProvider.setToolTipText(mpTooltip);
      _lblThemeStyle.setToolTipText(mpTooltip);
   }

   private void updateUI_Theme(final Map25Provider mapProvider) {

      /*
       * Fill theme combo
       */
      _comboTheme.removeAll();

      if (mapProvider != null && mapProvider.isOfflineMap) {

         // add an additional option to use the theme from the theme file

         _comboTheme.add(Messages.Pref_Map25_Provider_Theme_FromThemeFile);
      }

      // fill combobox
      for (final VtmThemes vtmTheme : VtmThemes.values()) {
         _comboTheme.add(vtmTheme.toString());
      }

      /*
       * Select theme
       */
      if (mapProvider == null) {
         _comboTheme.select(0);
         return;
      }

      if (mapProvider.isOfflineMap && mapProvider.offline_IsThemeFromFile) {

         // select: theme is from a file
         _comboTheme.select(0);
         return;
      }

      int themeIndex = Map25ProviderManager.getThemeIndex(mapProvider.theme, mapProvider.tileEncoding);
      if (mapProvider.isOfflineMap) {

         // adjust because of the offline additional item

         themeIndex++;
      }
      _comboTheme.select(themeIndex);
   }

   private void updateUI_ThemeStyle(final Map25Provider mapProvider) {

      _comboThemeStyle.removeAll();

      if (mapProvider.isOfflineMap == false) {

         // online map has no theme styles

         _comboThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_NotSupported);
         _comboThemeStyle.select(0);

         _lblThemeStyle.setEnabled(false);
         _comboThemeStyle.setEnabled(false);

         _comboThemeStyle.getParent().layout(true, true);

         return;
      }

      final List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);
      if (mfStyles == null) {

         // invalid style file

         _comboThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_InvalidThemeFilename);
         _comboThemeStyle.select(0);

         _lblThemeStyle.setEnabled(false);
         _comboThemeStyle.setEnabled(false);

         _comboThemeStyle.getParent().layout(true, true);

         return;
      }

      /*
       * Fill combo with all styles
       */

      _lblThemeStyle.setEnabled(true);
      _comboThemeStyle.setEnabled(true);

      int styleSelectIndex = 0;

      for (int styleIndex = 0; styleIndex < mfStyles.size(); styleIndex++) {

         final MapsforgeThemeStyle mfStyle = mfStyles.get(styleIndex);

         _comboThemeStyle.add(mfStyle.getLocaleName());

         if (mfStyle.getXmlLayer().equals(mapProvider.offline_ThemeStyle)) {
            styleSelectIndex = styleIndex;
         }
      }

      _comboThemeStyle.getParent().layout(true, true);

      // select map provider style
      _isInUpdateUI = true;
      {
         _comboThemeStyle.select(styleSelectIndex);
      }
      _isInUpdateUI = false;
   }

}
