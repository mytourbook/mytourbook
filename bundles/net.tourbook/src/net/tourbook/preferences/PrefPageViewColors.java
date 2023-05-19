/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageViewColors extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                                                   = "net.tourbook.preferences.PrefPageViewColors";  //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                                           = TourbookPlugin.getPrefStore();
   private static final IDialogSettings  _state                                               = TourbookPlugin.getState(ID);

   public static final String            STATE_VIEW_COLOR_CONTENT_CATEGORY_BRIGHT             = "STATE_VIEW_COLOR_CONTENT_CATEGORY_BRIGHT";     //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_CONTENT_CATEGORY_DARK               = "STATE_VIEW_COLOR_CONTENT_CATEGORY_DARK";       //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_BRIGHT         = "STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_BRIGHT"; //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DARK           = "STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DARK";   //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_DATE_CATEGORY_BRIGHT                = "STATE_VIEW_COLOR_DATE_CATEGORY_BRIGHT";        //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_DATE_CATEGORY_DARK                  = "STATE_VIEW_COLOR_DATE_CATEGORY_DARK";          //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_BRIGHT            = "STATE_VIEW_COLOR_DATE_SUB_CATEGORY_BRIGHT";    //$NON-NLS-1$
   public static final String            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DARK              = "STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DARK";      //$NON-NLS-1$

   public static final RGB               STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_BRIGHT     = new RGB(0x3c, 0x3c, 0x3c);
   public static final RGB               STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_BRIGHT = new RGB(0xf2, 0x5b, 0x0);
   public static final RGB               STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_BRIGHT        = new RGB(0x3c, 0x3c, 0x3c);
   public static final RGB               STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_BRIGHT    = new RGB(0xf2, 0x5b, 0x0);

   public static final RGB               STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_DARK       = new RGB(0xe8, 0xe8, 0xe8);
   public static final RGB               STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_DARK   = new RGB(0xff, 0x7a, 0x2b);
   public static final RGB               STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_DARK          = new RGB(0xe8, 0xe8, 0xe8);
   public static final RGB               STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_DARK      = new RGB(0xff, 0x7a, 0x2b);

   private boolean                       _isUIModified;

   /*
    * UI controls
    */
   private Button                _chkLiveUpdate;
   private Button                _chkViewGridLines;

   private ColorSelectorExtended _colorSelector_ContentCategory_Bright;
   private ColorSelectorExtended _colorSelector_ContentCategory_Dark;
   private ColorSelectorExtended _colorSelector_ContentSubCategory_Bright;
   private ColorSelectorExtended _colorSelector_ContentSubCategory_Dark;
   private ColorSelectorExtended _colorSelector_DateCategory_Bright;
   private ColorSelectorExtended _colorSelector_DateCategory_Dark;
   private ColorSelectorExtended _colorSelector_DateSubCategory_Bright;
   private ColorSelectorExtended _colorSelector_DateSubCategory_Dark;

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         createUI_10_Colors(container);
         createUI_20_Options(container);

         createUI_99_LiveUpdate(container);
      }

      return container;
   }

   private void createUI_10_Colors(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.pref_view_layout_label_color_group);
      GridDataFactory.fillDefaults().grab(false, false).applyTo(group);
      GridLayoutFactory.swtDefaults()
            .numColumns(3)
            .applyTo(group);
      {
         {
            UI.createSpacer_Horizontal(group, 1);

            final Label labelBright = UI.createLabel(group, OtherMessages.APP_THEME_BRIGHT_THEME);
            labelBright.setToolTipText(OtherMessages.APP_THEME_BRIGHT_THEME_TOOLTIP);
            GridDataFactory.fillDefaults()
                  .align(SWT.CENTER, SWT.FILL)
                  .applyTo(labelBright);

            final Label labelDark = UI.createLabel(group, OtherMessages.APP_THEME_DARK_THEME);
            labelDark.setToolTipText(OtherMessages.APP_THEME_DARK_THEME_TOOLTIP);
            GridDataFactory.fillDefaults()
                  .align(SWT.CENTER, SWT.FILL)
                  .applyTo(labelDark);
         }
         {
            /*
             * Color: Content category (e.g. tag category)
             */
            final Label label = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.pref_view_layout_label_category);

            _colorSelector_ContentCategory_Bright = new ColorSelectorExtended(group);
            _colorSelector_ContentCategory_Bright.addListener(event -> onModify(_colorSelector_ContentCategory_Bright));
            setButtonLayoutData(_colorSelector_ContentCategory_Bright.getButton());

            _colorSelector_ContentCategory_Dark = new ColorSelectorExtended(group);
            _colorSelector_ContentCategory_Dark.addListener(event -> onModify(_colorSelector_ContentCategory_Dark));
            setButtonLayoutData(_colorSelector_ContentCategory_Dark.getButton());
         }
         {
            /*
             * Color: Content subcategory (e.g. tag)
             */
            final Label label = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.pref_view_layout_label_title);

            _colorSelector_ContentSubCategory_Bright = new ColorSelectorExtended(group);
            _colorSelector_ContentSubCategory_Bright.addListener(event -> onModify(_colorSelector_ContentSubCategory_Bright));
            setButtonLayoutData(_colorSelector_ContentSubCategory_Bright.getButton());

            _colorSelector_ContentSubCategory_Dark = new ColorSelectorExtended(group);
            _colorSelector_ContentSubCategory_Dark.addListener(event -> onModify(_colorSelector_ContentSubCategory_Dark));
            setButtonLayoutData(_colorSelector_ContentSubCategory_Dark.getButton());
         }
         {
            /*
             * Color: Date category (e.g. year)
             */
            final Label label = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.pref_view_layout_label_sub);

            _colorSelector_DateCategory_Bright = new ColorSelectorExtended(group);
            _colorSelector_DateCategory_Bright.addListener(event -> onModify(_colorSelector_DateCategory_Bright));
            setButtonLayoutData(_colorSelector_DateCategory_Bright.getButton());

            _colorSelector_DateCategory_Dark = new ColorSelectorExtended(group);
            _colorSelector_DateCategory_Dark.addListener(event -> onModify(_colorSelector_DateCategory_Dark));
            setButtonLayoutData(_colorSelector_DateCategory_Dark.getButton());
         }
         {
            /*
             * Color: Date subcategory (e.g. month)
             */
            final Label label = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(label);
            label.setText(Messages.pref_view_layout_label_sub_sub);

            _colorSelector_DateSubCategory_Bright = new ColorSelectorExtended(group);
            _colorSelector_DateSubCategory_Bright.addListener(event -> onModify(_colorSelector_DateSubCategory_Bright));
            setButtonLayoutData(_colorSelector_DateSubCategory_Bright.getButton());

            _colorSelector_DateSubCategory_Dark = new ColorSelectorExtended(group);
            _colorSelector_DateSubCategory_Dark.addListener(event -> onModify(_colorSelector_DateSubCategory_Dark));
            setButtonLayoutData(_colorSelector_DateSubCategory_Dark.getButton());
         }
      }
   }

   private void createUI_20_Options(final Composite parent) {

      {
         /*
          * Checkbox: View grid lines
          */
         _chkViewGridLines = new Button(parent, SWT.CHECK);
         _chkViewGridLines.setText(Messages.pref_view_layout_display_lines);
         _chkViewGridLines.setToolTipText(Messages.pref_view_layout_display_lines_Tooltip);
         _chkViewGridLines.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModify(null)));
      }
   }

   private void createUI_99_LiveUpdate(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         /*
          * Checkbox: live update
          */
         _chkLiveUpdate = new Button(container, SWT.CHECK);
         _chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
         _chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
         _chkLiveUpdate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> doLiveUpdate()));
         GridDataFactory.fillDefaults().grab(true, true)
               .align(SWT.FILL, SWT.END)
               .applyTo(_chkLiveUpdate);
      }
   }

   private void doLiveUpdate() {

      if (_chkLiveUpdate.getSelection()) {

         performApply();
      }
   }

   private void fireModifyEvent() {

      if (_isUIModified) {

         _isUIModified = false;

         net.tourbook.ui.UI.setViewColorsFromState();

         // fire one event for all modified colors
         getPreferenceStore().setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
      }

   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }

   @Override
   public boolean okToLeave() {

      if (_isUIModified) {

         // save the colors in the pref store
         super.performOk();

         fireModifyEvent();
      }

      return super.okToLeave();
   }

   private void onModify(final Object eventControl) {

      _isUIModified = true;

      updateUI_CustomColors(eventControl);

      doLiveUpdate();
   }

   @Override
   protected void performApply() {

      performOk();
   }

   @Override
   protected void performDefaults() {

      _isUIModified = true;

      restoreDefaults();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      if (_isUIModified) {

         saveState();

         fireModifyEvent();
      }

      return true;
   }

// SET_FORMATTING_OFF

   private void restoreDefaults() {

      _colorSelector_ContentCategory_Bright     .setColorValue(STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_BRIGHT);
      _colorSelector_ContentCategory_Dark       .setColorValue(STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_DARK);
      _colorSelector_ContentSubCategory_Bright  .setColorValue(STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_BRIGHT);
      _colorSelector_ContentSubCategory_Dark    .setColorValue(STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_DARK);
      _colorSelector_DateCategory_Bright        .setColorValue(STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_BRIGHT);
      _colorSelector_DateCategory_Dark          .setColorValue(STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_DARK);
      _colorSelector_DateSubCategory_Bright     .setColorValue(STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_BRIGHT);
      _colorSelector_DateSubCategory_Dark       .setColorValue(STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_DARK);

      _chkLiveUpdate       .setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE));
      _chkViewGridLines    .setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      doLiveUpdate();
   }

   private void restoreState() {

      _colorSelector_ContentCategory_Bright     .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_CONTENT_CATEGORY_BRIGHT,
            STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_BRIGHT));

      _colorSelector_ContentCategory_Dark       .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_CONTENT_CATEGORY_DARK,
            STATE_VIEW_COLOR_CONTENT_CATEGORY_DEFAULT_DARK));

      _colorSelector_ContentSubCategory_Bright  .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_BRIGHT,
            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_BRIGHT));

      _colorSelector_ContentSubCategory_Dark    .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DARK,
            STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DEFAULT_DARK));

      _colorSelector_DateCategory_Bright        .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_DATE_CATEGORY_BRIGHT,
            STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_BRIGHT));

      _colorSelector_DateCategory_Dark          .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_DATE_CATEGORY_DARK,
            STATE_VIEW_COLOR_DATE_CATEGORY_DEFAULT_DARK));

      _colorSelector_DateSubCategory_Bright     .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_BRIGHT,
            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_BRIGHT));

      _colorSelector_DateSubCategory_Dark       .setColorValue(Util.getStateRGB(_state,
            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DARK,
            STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DEFAULT_DARK));

      _colorSelector_ContentCategory_Bright     .restoreCustomColors(_state);
      _colorSelector_ContentCategory_Dark       .restoreCustomColors(_state);
      _colorSelector_ContentSubCategory_Bright  .restoreCustomColors(_state);
      _colorSelector_ContentSubCategory_Dark    .restoreCustomColors(_state);
      _colorSelector_DateCategory_Bright        .restoreCustomColors(_state);
      _colorSelector_DateCategory_Dark          .restoreCustomColors(_state);
      _colorSelector_DateSubCategory_Bright     .restoreCustomColors(_state);
      _colorSelector_DateSubCategory_Dark       .restoreCustomColors(_state);

      _chkLiveUpdate    .setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE));
      _chkViewGridLines .setSelection(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));
   }

   private void saveState() {

      Util.setState(_state, STATE_VIEW_COLOR_CONTENT_CATEGORY_BRIGHT,      _colorSelector_ContentCategory_Bright    .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_CONTENT_CATEGORY_DARK,        _colorSelector_ContentCategory_Dark      .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_BRIGHT,  _colorSelector_ContentSubCategory_Bright .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_CONTENT_SUB_CATEGORY_DARK,    _colorSelector_ContentSubCategory_Dark   .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_DATE_CATEGORY_BRIGHT,         _colorSelector_DateCategory_Bright       .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_DATE_CATEGORY_DARK,           _colorSelector_DateCategory_Dark         .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_DATE_SUB_CATEGORY_BRIGHT,     _colorSelector_DateSubCategory_Bright    .getColorValue());
      Util.setState(_state, STATE_VIEW_COLOR_DATE_SUB_CATEGORY_DARK,       _colorSelector_DateSubCategory_Dark      .getColorValue());

      // all color selectors have the same custom colors
      _colorSelector_ContentCategory_Bright.saveCustomColors(_state);

      _prefStore.setValue(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE,   _chkLiveUpdate.getSelection());
      _prefStore.setValue(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES,              _chkViewGridLines.getSelection());
   }

// SET_FORMATTING_ON

   /**
    * Update custom colors in all color selectors
    *
    * @param eventControl
    */
   private void updateUI_CustomColors(final Object eventControl) {

      if (eventControl instanceof ColorSelectorExtended) {

         final ColorSelectorExtended colorSelectorExtended = (ColorSelectorExtended) eventControl;

         // log selected color as Java code
         System.out.println(UI.logRGB(colorSelectorExtended.getColorValue()));

         final RGB[] customColors = colorSelectorExtended.getCustomColors();

         if (customColors != null) {

            // update all color selectors with the same custom colors

// SET_FORMATTING_OFF

            _colorSelector_ContentCategory_Bright     .setCustomColors(customColors);
            _colorSelector_ContentCategory_Dark       .setCustomColors(customColors);
            _colorSelector_ContentSubCategory_Bright  .setCustomColors(customColors);
            _colorSelector_ContentSubCategory_Dark    .setCustomColors(customColors);
            _colorSelector_DateCategory_Bright        .setCustomColors(customColors);
            _colorSelector_DateCategory_Dark          .setCustomColors(customColors);
            _colorSelector_DateSubCategory_Bright     .setCustomColors(customColors);
            _colorSelector_DateSubCategory_Dark       .setCustomColors(customColors);

// SET_FORMATTING_ON
         }
      }
   }

}
