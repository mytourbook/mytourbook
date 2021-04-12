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
package net.tourbook.map25.ui;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25ProviderManager;
import net.tourbook.map25.Map25View;
import net.tourbook.preferences.MapsforgeThemeStyle;
import net.tourbook.preferences.PrefPage_Map25Provider;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.ToolBar;
import org.oscim.theme.VtmThemes;

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

   private List      _listMapProvider;
   private List      _listTheme;
   private List      _listThemeStyle;

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
         //createUI_30_Info(shellContainer);
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
      GridDataFactory.fillDefaults()
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
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblMapProvider);

            _listMapProvider = new List(container, SWT.SINGLE);
            _listMapProvider.addFocusListener(_keepOpenListener);
            _listMapProvider.addSelectionListener(new SelectionAdapter() {
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
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblTheme);

            _listTheme = new List(container, SWT.SINGLE);
            _listTheme.addFocusListener(_keepOpenListener);
            _listTheme.addSelectionListener(new SelectionAdapter() {
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
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblThemeStyle);

            _listThemeStyle = new List(container, SWT.SINGLE);
            _listThemeStyle.addFocusListener(_keepOpenListener);
            _listThemeStyle.addSelectionListener(new SelectionAdapter() {
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

      _listMapProvider.removeAll();

      for (final Map25Provider map25Provider : allEnabledMapProviders) {
         _listMapProvider.add(map25Provider.name);
      }

      /*
       * Reselect map provider
       */
      final Map25Provider currentMapProvider = getUsedMapProvider();
      selectMapProvider(currentMapProvider);
   }

   private Map25Provider getSelectedMapProvider() {

      final int selectedIndex = _listMapProvider.getSelectionIndex();

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

      if (_listMapProvider == null) {

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
      updateUI_Theme_Style(selectedMapProvider);
      updateUI_MapProvider_Tooltip(selectedMapProvider);

      // update UI otherwise the old theme style box is displayed until the next redraw
      _parent.update();

      // update UI
      updateMap(selectedMapProvider);

      // set map provider which should be selected when the pref dialog is opened
      _actionPrefDialog.setPrefData(selectedMapProvider);
   }

   private void onSelect_Theme() {

      final Map25Provider mapProvider = getSelectedMapProvider();

      final int selectedIndex = _listTheme.getSelectionIndex();

      final boolean isThemeFromFile = mapProvider.is_mf_Map && selectedIndex == 0;

      mapProvider.mf_IsThemeFromFile = isThemeFromFile;

      if (isThemeFromFile) {

         mapProvider.theme = null;

      } else {

         int themeIndex = selectedIndex;

         // adjust index for offline maps because the first item is not a theme
         if (mapProvider.is_mf_Map) {
            themeIndex--;
         }

         //update model
         final VtmThemes[] themeValues = VtmThemes.values();
         mapProvider.theme = themeValues[themeIndex];
      }

      Map25ProviderManager.saveMapProvider();

      // update UI
      updateUI_Theme_Style(mapProvider);

      // update UI otherwise the old theme style box is displayed until the next redraw
      _parent.update();

      updateMap(mapProvider);
   }

   private void onSelect_ThemeStyle() {

      if (_isInUpdateUI) {
         return;
      }

      final Map25Provider mapProvider = getSelectedMapProvider();

      final java.util.List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);

      int selectedStyleIndex = _listThemeStyle.getSelectionIndex();
      String selectedThemeStyle;

      if (selectedStyleIndex == 0) {

         // first item is "All Styles"
         selectedThemeStyle = Map25App.THEME_STYLE_ALL;

      } else {

         // mfStyles do not contain "All Styles" !!!
         selectedStyleIndex--;

         selectedThemeStyle = mfStyles.get(selectedStyleIndex).getXmlLayer();
      }

      // update model
      mapProvider.mf_ThemeStyle = selectedThemeStyle;

      Map25ProviderManager.saveMapProvider();

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
      updateUI_Theme_Style(currentMapProvider);
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
               _listMapProvider.select(providerIndex);
            }
            _isInUpdateUI = false;

            return;
         }
      }

   }

   private void updateMap(final Map25Provider mapProvider) {

      _map25View.getMapApp().setMapProvider(mapProvider);
   }

   private void updateUI_MapProvider_Tooltip(final Map25Provider mapProvider) {

      String mpTooltip = UI.EMPTY_STRING;

      if (mapProvider.is_mf_Map) {

         // offline

         final java.util.List<MapsforgeThemeStyle> themeStyles = mapProvider.getThemeStyles(false);

         final CharSequence[] allThemeStyles = new CharSequence[themeStyles.size()];
         for (int styleIndex = 0; styleIndex < themeStyles.size(); styleIndex++) {
            final MapsforgeThemeStyle themeStyle = themeStyles.get(styleIndex);
            allThemeStyles[styleIndex] = UI.SPACE4 + themeStyle.getLocaleName() + UI.DASH_WITH_SPACE + themeStyle.getXmlLayer();
         }

         final String themeStyleText = String.join(UI.NEW_LINE1, allThemeStyles);

         mpTooltip = String.format(Messages.Slideout_Map25Provider_Combo_MapProvider_Offline_Tooltip,

               mapProvider.tileEncoding,
               mapProvider.mf_MapFilepath,
               mapProvider.mf_ThemeFilepath,
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
      _listTheme.removeAll();

      if (mapProvider != null && mapProvider.is_mf_Map) {

         // add an additional option to use the theme from the theme file

         _listTheme.add(Messages.Pref_Map25_Provider_Theme_FromThemeFile);
      }

      // fill combobox with all themes
      for (final VtmThemes vtmTheme : VtmThemes.values()) {
         _listTheme.add(vtmTheme.toString());
      }

      /*
       * Select theme
       */
      if (mapProvider == null) {
         _listTheme.select(0);
         return;
      }

      if (mapProvider.is_mf_Map && mapProvider.mf_IsThemeFromFile) {

         // select: theme is from a file
         _listTheme.select(0);
         return;
      }

      int themeIndex = Map25ProviderManager.getThemeIndex(mapProvider.theme, mapProvider.tileEncoding);
      if (mapProvider.is_mf_Map) {

         // adjust because of the offline additional item

         themeIndex++;
      }
      _listTheme.select(themeIndex);
   }

   /**
    * This must be called <b>AFTER</b> {@link #updateUI_Theme(Map25Provider)} because it depends on
    * the {@link #_listTheme}
    *
    * @param mapProvider
    */
   private void updateUI_Theme_Style(final Map25Provider mapProvider) {

      _listThemeStyle.removeAll();

      if (mapProvider.is_mf_Map == false) {

         // online map has no theme styles

         _listThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_NotSupported);

         _lblThemeStyle.setEnabled(false);
         _listThemeStyle.setEnabled(false);

         _listThemeStyle.getParent().layout(true, true);

         return;
      }

      final java.util.List<MapsforgeThemeStyle> mfStyles = mapProvider.getThemeStyles(false);
      if (mfStyles == null) {

         // invalid style file

         _listThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_InvalidThemeFilename);

         _lblThemeStyle.setEnabled(false);
         _listThemeStyle.setEnabled(false);

         _listThemeStyle.getParent().layout(true, true);

         return;
      }

      if (mapProvider.is_mf_Map && _listTheme.getSelectionIndex() > 0) {

         /*
          * When it's an offline map and the themes are NOT from the a file then there are no theme
          * styles
          */

         _listThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_NoStyles);

         _lblThemeStyle.setEnabled(false);
         _listThemeStyle.setEnabled(false);

         _listThemeStyle.getParent().layout(true, true);

         return;
      }

      /*
       * Fill combo with all available styles
       */

      // first item is "All Styles"
      _listThemeStyle.add(Messages.Pref_Map25_Provider_ThemeStyle_Info_All);

      int styleSelectIndex = 0;

      for (int styleIndex = 0; styleIndex < mfStyles.size(); styleIndex++) {

         final MapsforgeThemeStyle mfStyle = mfStyles.get(styleIndex);

         _listThemeStyle.add(mfStyle.getLocaleName());

         if (mfStyle.getXmlLayer().equals(mapProvider.mf_ThemeStyle)) {
            styleSelectIndex = styleIndex + 1;
         }
      }

      _lblThemeStyle.setEnabled(true);
      _listThemeStyle.setEnabled(true);

      _listThemeStyle.getParent().layout(true, true);

      // select map provider style
      _isInUpdateUI = true;
      {
         _listThemeStyle.select(styleSelectIndex);
      }
      _isInUpdateUI = false;
   }

}
