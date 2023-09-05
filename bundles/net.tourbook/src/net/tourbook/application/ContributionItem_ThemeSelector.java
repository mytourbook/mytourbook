/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.jface.viewers.LabelProvider.createTextProvider;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.ui.CustomControlContribution;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class ContributionItem_ThemeSelector extends CustomControlContribution {

   private static final String           ID                = "net.tourbook.application.ThemeSelectorContributionItem"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

   private IPropertyChangeListener       _prefChangeListener;

   private ITheme                        _currentTheme;
   private String                        _defaultThemeId;
   private IThemeEngine                  _themeEngine;

   private boolean                       _isInUpdate;

   /*
    * UI controls
    */
   private ComboViewer _comboThemeSelector;

   public ContributionItem_ThemeSelector() {

      super(ID);
   }

   /**
    * Listen for changes in the preferences
    */
   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.THEME_IS_THEME_MODIFIED)) {

            _isInUpdate = true;
            {
               updateUI();
            }
            _isInUpdate = false;
         }
      };

      // register the listener
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   protected Control createControl(final Composite parent) {

      initUI();

      final Composite ui = createUI(parent);

      addPrefListener();

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_CYAN);
      {
         _comboThemeSelector = new ComboViewer(container, SWT.READ_ONLY);
         _comboThemeSelector.setLabelProvider(createTextProvider(element -> ((ITheme) element).getLabel()));
         _comboThemeSelector.setContentProvider(ArrayContentProvider.getInstance());
         _comboThemeSelector.getCombo().setToolTipText(Messages.Pref_Appearance_Combo_Theme_Tooltip);
         _comboThemeSelector.addSelectionChangedListener(selectionChangedEvent -> onSelectTheme());
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_comboThemeSelector.getControl());

      }

      return container;
   }

   @Override
   public void dispose() {

      _prefStore_Common.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void initUI() {

      /*
       * Setup themes
       */
      final IWorkbench workbench = PlatformUI.getWorkbench();
      final MApplication application = workbench.getService(MApplication.class);
      final IEclipseContext context = application.getContext();

      // _defaultTheme = "org.eclipse.e4.ui.css.theme.e4_default"
      _defaultThemeId = (String) context.get(ThemeUtil.THEME_ID);
      _themeEngine = context.get(org.eclipse.e4.ui.css.swt.theme.IThemeEngine.class);
   }

   private void onSelectTheme() {

      if (_isInUpdate) {
         return;
      }

      final ITheme selectedTheme = (ITheme) (_comboThemeSelector.getStructuredSelection().getFirstElement());

      if (selectedTheme.equals(_currentTheme) == false) {

         // another theme is selected

         final boolean isDarkThemeSelected = ThemeUtil.E4_DARK_THEME_ID.equals(selectedTheme.getId());

         ThemeUtil.setDarkTheme(isDarkThemeSelected);

         // set theme and save it in the pref store (2nd parameter)
         _themeEngine.setTheme(selectedTheme, true);

         // an app restart is required for the theme change to take full effect

         if (new MessageDialog(

               _comboThemeSelector.getCombo().getShell(),

               Messages.App_Dialog_RestartApp_Title,
               null,

               Messages.Pref_Appearance_Dialog_RestartAfterThemeChange_Message,
               MessageDialog.QUESTION,

               // default index
               0,

               Messages.App_Action_RestartApp,
               Messages.App_Action_Cancel

         ).open() == IDialogConstants.OK_ID) {

            Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart());
         }
      }
   }

   private void restoreState() {

      // load combo content
      _comboThemeSelector.setInput(ThemeUtil.getAllThemes());

      updateUI();
   }

   private void updateUI() {

      _currentTheme = _themeEngine.getActiveTheme();

      if (_currentTheme == null) {

         // set default theme
         _themeEngine.setTheme(_defaultThemeId, true);

         _currentTheme = _themeEngine.getActiveTheme();
      }

      if (_currentTheme != null) {
         _comboThemeSelector.setSelection(new StructuredSelection(_currentTheme));
      }
   }

}
