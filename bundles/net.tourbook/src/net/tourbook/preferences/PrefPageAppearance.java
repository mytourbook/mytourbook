/*******************************************************************************
 * Copyright (C) 2008, 2023 Wolfgang Schramm and Contributors
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

import static org.eclipse.jface.viewers.LabelProvider.createTextProvider;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import de.byteholder.geoclipse.preferences.IMappingPreferences;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.font.FontFieldEditorExtended;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.statistic.StatisticValuesView;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageAppearance extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                = "net.tourbook.preferences.PrefPageAppearance"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore _prefStore_Common = CommonActivator.getPrefStore();

   private boolean                       _isShowInApp_MemoryMonitor;
   private boolean                       _isShowInApp_RestartApp;
   private boolean                       _isShowInApp_ThemeSelector;

   private int                           _hintDefaultSpinnerWidth;
   private PixelConverter                _pc;
   private SelectionListener             _defaultSelectionListener;
   private MouseWheelListener            _defaultMouseWheelListener;

   private ITheme                        _currentTheme;
   private String                        _defaultThemeId;
   private IThemeEngine                  _themeEngine;

   private ComboViewer                   _comboViewerTheme;
   private ControlDecoration             _comboDecorator_Theme;

   private FontFieldEditorExtended       _logMessageFontEditor;

   /*
    * UI controls
    */
   private Button  _btnResetAllToggleDialogs;

   private Button  _chkAutoOpenTagging;
   private Button  _chkShowInApp_MemoryMonitor;
   private Button  _chkShowInApp_RestartApp;
   private Button  _chkShowInApp_ThemeSelector;
   private Button  _chkTaggingAnimation;

   private Label   _lblAutoOpenMS;
   private Label   _lblAutoTagDelay;

   private Spinner _spinnerAutoOpenDelay;
   private Spinner _spinnerRecentTags;

   public PrefPageAppearance() {

// hide default button
//		noDefaultAndApplyButton();
   }

   @Override
   public void applyData(final Object data) {

      if (StatisticValuesView.ID.equals(data)) {

         // set focus to log font

         _logMessageFontEditor.setFocus();
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();
      enableControls();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(_uiContainer);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         createUI_10_Theme(container);
         createUI_20_Tagging(container);
         createUI_30_LogFont(container);
         createUI_40_OtherOptions(container);
      }

      return container;
   }

   private void createUI_10_Theme(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Appearance_Group_Theme);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)

            // more horizontal space is needed that the decorator is not clipped
            .spacing(10, 5)

            .applyTo(group);
      {
         UI.createLabel(group, Messages.Pref_Appearance_Label_Theme);

         _comboViewerTheme = new ComboViewer(group, SWT.READ_ONLY);

         final Combo combo = _comboViewerTheme.getCombo();
         combo.setToolTipText(Messages.Pref_Appearance_Combo_Theme_Tooltip);

         _comboViewerTheme.setLabelProvider(createTextProvider(element -> ((ITheme) element).getLabel()));
         _comboViewerTheme.setContentProvider(ArrayContentProvider.getInstance());
         _comboViewerTheme.setInput(ThemeUtil.getAllThemes());
         _comboViewerTheme.addSelectionChangedListener(selectionChangedEvent -> onSelectTheme());

         _comboDecorator_Theme = new ControlDecoration(combo, SWT.TOP | SWT.LEFT);
      }

      {
         /*
          * Checkbox: Show theme selector in app toolbar
          */
         _chkShowInApp_ThemeSelector = new Button(group, SWT.CHECK);
         _chkShowInApp_ThemeSelector.setText(Messages.Pref_Appearance_Check_ShowThemeSelectorInAppToolbar);
         GridDataFactory.fillDefaults()
               .span(2, 1)
               .applyTo(_chkShowInApp_ThemeSelector);
      }
   }

   private void createUI_20_Tagging(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.Pref_Appearance_Group_Tagging);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         /*
          * number of recent tags
          */
         final Label label = UI.createLabel(group, Messages.pref_appearance_number_of_recent_tags);
         label.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);

         // spinner
         _spinnerRecentTags = new Spinner(group, SWT.BORDER);
         _spinnerRecentTags.setToolTipText(Messages.pref_appearance_number_of_recent_tags_tooltip);
         _spinnerRecentTags.setMinimum(0);
         _spinnerRecentTags.setMaximum(9);
         _spinnerRecentTags.addSelectionListener(_defaultSelectionListener);
         _spinnerRecentTags.addMouseWheelListener(_defaultMouseWheelListener);
         GridDataFactory.fillDefaults()
               .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_spinnerRecentTags);

         /*
          * autoopen tagging
          */
//				eclipse 3.7 supports this feature
//				if (UI.IS_OSX) {
//					// label: OSX is not supported, feature is not working
//					final Label label = new Label(container, SWT.WRAP);
//					GridDataFactory.fillDefaults().span(3, 1).applyTo(label);
//					label.setText(Messages.Pref_Appearance_Label_NoOSXSupport);
//				}
         _chkAutoOpenTagging = new Button(group, SWT.CHECK);
         _chkAutoOpenTagging.setText(Messages.Pref_Appearance_Check_AutoOpenTagging);
         _chkAutoOpenTagging.addSelectionListener(_defaultSelectionListener);
         _chkAutoOpenTagging.setToolTipText(Messages.Pref_Appearance_Label_AutoOpenTagging_Tooltip);
         GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkAutoOpenTagging);

         final Composite autoTagContainer = new Composite(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(false, false).indent(16, 0).span(2, 1).applyTo(autoTagContainer);
         GridLayoutFactory.fillDefaults().numColumns(3).applyTo(autoTagContainer);
         {

            // label: delay
            _lblAutoTagDelay = UI.createLabel(autoTagContainer, Messages.Pref_Appearance_Label_AutoOpenTaggingDelay);
            _lblAutoTagDelay.setToolTipText(Messages.Pref_Appearance_Label_AutoOpenTagging_Tooltip);

            // spinner
            _spinnerAutoOpenDelay = new Spinner(autoTagContainer, SWT.BORDER);
            _spinnerAutoOpenDelay.setMinimum(0);
            _spinnerAutoOpenDelay.setMaximum(3000);
            _spinnerAutoOpenDelay.addSelectionListener(_defaultSelectionListener);
            _spinnerAutoOpenDelay.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerAutoOpenDelay);

            // label: ms
            _lblAutoOpenMS = UI.createLabel(autoTagContainer, UI.UNIT_MS);

            // check: show animation
            _chkTaggingAnimation = new Button(autoTagContainer, SWT.CHECK);
            _chkTaggingAnimation.setText(Messages.Pref_Appearance_Check_TaggingAnimation);
            _chkTaggingAnimation.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkTaggingAnimation);
         }
      }
   }

   private void createUI_30_LogFont(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(OtherMessages.THEME_FONT_LOGGING);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         {
            /*
             * Font editor
             */
            final Composite fontContainer = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(fontContainer);
            GridLayoutFactory.swtDefaults().numColumns(1).applyTo(fontContainer);
//            fontContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
            {
               _logMessageFontEditor = new FontFieldEditorExtended(IMappingPreferences.THEME_FONT_LOGGING,
                     UI.EMPTY_STRING,
                     OtherMessages.THEME_FONT_LOGGING_PREVIEW_TEXT,
                     fontContainer);

               _logMessageFontEditor.setPropertyChangeListener(propertyChangeEvent -> onChangeFontInEditor());
            }
         }
      }
   }

   private void createUI_40_OtherOptions(final Composite parent) {

      {
         /*
          * Checkbox: Show restart app action in app toolbar
          */
         _chkShowInApp_RestartApp = new Button(parent, SWT.CHECK);
         _chkShowInApp_RestartApp.setText(Messages.Pref_Appearance_Check_RestartAppInAppToolbar);
      }
      {
         /*
          * Memory monitor
          */
         _chkShowInApp_MemoryMonitor = new Button(parent, SWT.CHECK);
         _chkShowInApp_MemoryMonitor.setText(Messages.pref_appearance_showMemoryMonitor);
//         GridDataFactory.fillDefaults().indent(0, 10).applyTo(_chkMemMonitor);
      }
      {
         /*
          * Reset all toggle dialogs
          */
         _btnResetAllToggleDialogs = new Button(parent, SWT.PUSH);
         _btnResetAllToggleDialogs.setText(Messages.Pref_Appearance_Button_ResetAllToggleDialogs);
         _btnResetAllToggleDialogs.setToolTipText(Messages.Pref_Appearance_Button_ResetAllToggleDialogs_Tooltip);
         _btnResetAllToggleDialogs.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onResetAllToggleDialogs()));
         GridDataFactory.fillDefaults()
//               .indent(0, 10)
               .align(SWT.BEGINNING, SWT.FILL)
               .applyTo(_btnResetAllToggleDialogs);
      }
   }

   private void enableControls() {

      final boolean isTagAutoOpen = _chkAutoOpenTagging.getSelection();
      final boolean isEnabled = true; // eclipse 3.7 supports this feature in OSX

      _chkAutoOpenTagging.setEnabled(isEnabled);
      _chkTaggingAnimation.setEnabled(isEnabled && isTagAutoOpen);

      _lblAutoOpenMS.setEnabled(isEnabled && isTagAutoOpen);
      _lblAutoTagDelay.setEnabled(isEnabled && isTagAutoOpen);

      _spinnerAutoOpenDelay.setEnabled(isEnabled && isTagAutoOpen);
   }

   /**
    * @return the currently selected theme or null if there are no themes
    */
   private ITheme getSelectedTheme() {
      return (ITheme) (_comboViewerTheme.getStructuredSelection().getFirstElement());
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      /*
       * Setup themes
       */
      final MApplication application = workbench.getService(MApplication.class);
      final IEclipseContext context = application.getContext();

      // _defaultTheme = "org.eclipse.e4.ui.css.theme.e4_default"
      _defaultThemeId = (String) context.get(ThemeUtil.THEME_ID);
      _themeEngine = context.get(org.eclipse.e4.ui.css.swt.theme.IThemeEngine.class);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
      _hintDefaultSpinnerWidth = UI.IS_LINUX ? SWT.DEFAULT : _pc.convertWidthInCharsToPixels(UI.IS_OSX ? 10 : 5);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> {
         enableControls();
      });

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
      };
   }

   private void onChangeFontInEditor() {

      // update state, this will fire IMappingPreferences.THEME_FONT_LOGGING event which will recreate the font
      _logMessageFontEditor.store();
   }

   private void onResetAllToggleDialogs() {

      _prefStore.setValue(ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR, false);
      _prefStore.setValue(ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING, false);

      _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_GEO_FILTER_DELETE_ALL_WITHOUT_NAME, false);
      _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING, false);
      _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_SHOW_STAR_RATING_SAVE_WARNING, false);

      _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_REIMPORT_TOUR_VALUES, false);
      _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_DELETE_TOUR_VALUES, false);

      MessageDialog.openInformation(getShell(),

            Messages.Pref_Appearance_Dialog_ResetAllToggleDialogs_Title,
            Messages.Pref_Appearance_Dialog_ResetAllToggleDialogs_Message);
   }

   private void onSelectTheme() {

      final ITheme selectedTheme = getSelectedTheme();

      if (!selectedTheme.equals(_currentTheme)) {

         // another theme is selected

         final boolean isDarkThemeSelected = ThemeUtil.E4_DARK_THEME_ID.equals(selectedTheme.getId());

         ThemeUtil.setDarkTheme(isDarkThemeSelected);

         // set theme but do not save it in the pref store (2nd parameter)
         _themeEngine.setTheme(selectedTheme, false);

         final Image decorationImage = FieldDecorationRegistry.getDefault()
               .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING)
               .getImage();

         // a restart is required for the theme change to take full effect
         _comboDecorator_Theme.setDescriptionText(Messages.Pref_Appearance_Dialog_RestartAfterThemeChange_Message);
         _comboDecorator_Theme.setImage(decorationImage);
         _comboDecorator_Theme.show();

      } else {

         _comboDecorator_Theme.hide();
      }
   }

   @Override
   protected void performApply() {

      saveState();

      super.performApply();
   }

   @Override
   public boolean performCancel() {

      if (_themeEngine != null) {

         if (_currentTheme != null) {

            // set theme but do not save it in the pref store (2nd parameter)
            _themeEngine.setTheme(_currentTheme, false);
         }
      }

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      /*
       * Theme
       */
      if (_themeEngine != null) {

         // update model
         _themeEngine.setTheme(_defaultThemeId, true);

         // update UI
         final ITheme activeTheme = _themeEngine.getActiveTheme();
         if (activeTheme != null) {
            _comboViewerTheme.setSelection(new StructuredSelection(activeTheme));
         }
      }

      /*
       * Other
       */

// SET_FORMATTING_OFF

      _spinnerRecentTags            .setSelection(_prefStore.getDefaultInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS));

      _chkAutoOpenTagging           .setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN));
      _chkTaggingAnimation          .setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION));
      _spinnerAutoOpenDelay         .setSelection(_prefStore.getDefaultInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));

      _chkShowInApp_MemoryMonitor   .setSelection(false);
      _chkShowInApp_RestartApp      .setSelection(false);
      _chkShowInApp_ThemeSelector   .setSelection(false);

// SET_FORMATTING_ON

      // set font editor default values
      _logMessageFontEditor.loadDefault();
      _logMessageFontEditor.store();

      super.performDefaults();

      // this do not work, I have no idea why, but with the apply button it works :-(
//		fireModificationEvent();

      enableControls();
   }

   @Override
   public boolean performOk() {

      boolean isRestartNow = false;

      /*
       * Theme
       */
      if (_themeEngine != null) {

         final ITheme selectedTheme = getSelectedTheme();
         final boolean isThemeChanged = selectedTheme != null && !selectedTheme.equals(_currentTheme);

         if (selectedTheme != null) {

            // set theme and save it in the pref store (2nd parameter)
            _themeEngine.setTheme(selectedTheme, true);

            _currentTheme = selectedTheme;
         }

         _comboDecorator_Theme.hide();

         if (isThemeChanged) {

            // fire modify event to update the app toolbar
            _prefStore_Common.setValue(ICommonPreferences.THEME_IS_THEME_MODIFIED, Math.random());

            // an app restart is required for the theme change to take full effect

            isRestartNow = requestForRestart(Messages.Pref_Appearance_Dialog_RestartAfterThemeChange_Message);
         }
      }

      /*
       * Theme
       */
      if (isRestartNow == false && _chkShowInApp_RestartApp.getSelection() != _isShowInApp_ThemeSelector) {

         // field is modified, ask for restart

         isRestartNow = requestForRestart(Messages.Pref_Appearance_Dialog_RestartAfterRestartApp_Message);
      }

      /*
       * Restart app
       */
      if (isRestartNow == false && _chkShowInApp_ThemeSelector.getSelection() != _isShowInApp_RestartApp) {

         // field is modified, ask for restart

         isRestartNow = requestForRestart(Messages.Pref_Appearance_Dialog_RestartAfterThemeSelectorIsInToolbar_Message);
      }

      /*
       * Memory monitor
       */
      if (isRestartNow == false && _chkShowInApp_MemoryMonitor.getSelection() != _isShowInApp_MemoryMonitor) {

         isRestartNow = requestForRestart(Messages.pref_appearance_showMemoryMonitor_message);
      }

      saveState();

      if (isRestartNow) {
         Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart());
      }

      return true;
   }

   private boolean requestForRestart(final String message) {

      if (new MessageDialog(

            getShell(),

            Messages.App_Dialog_RestartApp_Title,
            null,

            message,
            MessageDialog.QUESTION,

            // default index
            0,

            Messages.App_Action_RestartApp,
            Messages.App_Action_Cancel

      ).open() == IDialogConstants.OK_ID) {

         return true;
      }

      return false;
   }

   private void restoreState() {

      /*
       * Theme
       */
      _currentTheme = _themeEngine.getActiveTheme();
      if (_currentTheme != null) {
         _comboViewerTheme.setSelection(new StructuredSelection(_currentTheme));
      }

      /*
       * Other
       */
// SET_FORMATTING_OFF

      _isShowInApp_MemoryMonitor = _prefStore_Common.getBoolean(ICommonPreferences.APPEARANCE_IS_SHOW_MEMORY_MONITOR_IN_APP);
      _isShowInApp_RestartApp    = _prefStore_Common.getBoolean(ICommonPreferences.APPEARANCE_IS_SHOW_RESTART_APP_ACTION_IN_APP);
      _isShowInApp_ThemeSelector = _prefStore_Common.getBoolean(ICommonPreferences.THEME_IS_SHOW_THEME_SELECTOR_IN_APP);

      _chkShowInApp_MemoryMonitor.setSelection(_isShowInApp_MemoryMonitor);
      _chkShowInApp_RestartApp   .setSelection(_isShowInApp_RestartApp);
      _chkShowInApp_ThemeSelector.setSelection(_isShowInApp_ThemeSelector);

      _chkAutoOpenTagging        .setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN));
      _chkTaggingAnimation       .setSelection(_prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION));
      _spinnerAutoOpenDelay      .setSelection(_prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY));
      _spinnerRecentTags         .setSelection(_prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS));

      _logMessageFontEditor.setPreferenceStore(_prefStore);
      _logMessageFontEditor.load();
   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS,    _spinnerRecentTags.getSelection());

      _prefStore.setValue(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN,     _chkAutoOpenTagging.getSelection());
      _prefStore.setValue(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION,     _chkTaggingAnimation.getSelection());
      _prefStore.setValue(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY,  _spinnerAutoOpenDelay.getSelection());

      _prefStore_Common.setValue(ICommonPreferences.APPEARANCE_IS_SHOW_MEMORY_MONITOR_IN_APP,      _chkShowInApp_MemoryMonitor.getSelection());
      _prefStore_Common.setValue(ICommonPreferences.APPEARANCE_IS_SHOW_RESTART_APP_ACTION_IN_APP,  _chkShowInApp_RestartApp.getSelection());
      _prefStore_Common.setValue(ICommonPreferences.THEME_IS_SHOW_THEME_SELECTOR_IN_APP,           _chkShowInApp_ThemeSelector.getSelection());

// SET_FORMATTING_ON
   }
}
