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
package net.tourbook.common.color;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

public class ThemeUtil {

   public static final String  DEFAULT_BACKGROUND_LIGHT_THEME = "fff";   //$NON-NLS-1$
   public static final String  DEFAULT_FOREGROUND_LIGHT_THEME = "333";   //$NON-NLS-1$

   public static final String  DEFAULT_FOREGROUND_DARK_THEME  = "ddd";   //$NON-NLS-1$
   public static final String  DEFAULT_BACKGROUND_DARK_THEME  = "333";   //$NON-NLS-1$

   /**
    * Currently only .png files are supported for themed images !!!
    */
   private static final String IMAGE_NAME_EXTENSION_PNG       = ".png";  //$NON-NLS-1$

   /**
    * All images for the dark theme should have this postfix before the file extension
    */
   public static final String  DARK_THEME_POSTFIX             = "-dark"; //$NON-NLS-1$

   /*
    * Copied from org.eclipse.e4.ui.internal.workbench.swt.E4Application
    */
   public static final String THEME_ID               = "cssTheme";                                  //$NON-NLS-1$
   public static final String HIGH_CONTRAST_THEME_ID = "org.eclipse.e4.ui.css.theme.high-contrast"; //$NON-NLS-1$

   /*
    * Copied from org.eclipse.e4.ui.css.swt.internal.theme.ThemeEngine
    */
   public static final String  E4_DARK_THEME_ID = "org.eclipse.e4.ui.css.theme.e4_dark"; //$NON-NLS-1$

   private static IThemeEngine _themeEngine;

   /**
    * The tour chart do not show a dark background color when displayed in a dialog, it shows the
    * background color from the shell.
    * <p>
    * This color is used to show the tour chart with a dark background color, to have a contrast to
    * the dialog background.
    *
    * <pre>
    *
    * W10 dark colors
    *
    * shell.getBackground()   Color {81, 86, 88, 255}
    * table.getBackground()   Color {47, 47, 47, 255}
    * </pre>
    */
   private static Color        _defaultBackgroundColor_Table;
   private static Color        _defaultBackgroundColor_TableHeader;

   /**
    * <pre>
    * W10 dark colors
    *
    * shell.getForeground() Color {238, 238, 238, 255}
    * table.getForeground() Color {238, 238, 238, 255}
    * </pre>
    */
   private static Color        _defaultForegroundColor_Table;
   private static Color        _defaultForegroundColor_TableHeader;

   private static Color        _defaultForegroundColor_Combo;
   private static Color        _defaultBackgroundColor_Combo;

   private static Color        _defaultForegroundColor_Shell;
   private static Color        _defaultBackgroundColor_Shell;

   /**
    * These are all Eclipse themes when using W10:
    *
    * <pre>
    *
    * org.eclipse.e4.ui.css.theme.e4_classic       - Classic
    * org.eclipse.e4.ui.css.theme.e4_dark          - Dark
    * org.eclipse.e4.ui.css.theme.high-contrast    - High Contrast
    * org.eclipse.e4.ui.css.theme.e4_default       - Light
    * org.eclipse.e4.ui.css.theme.e4_system        - System
    * </pre>
    *
    * @return
    */
   public static List<ITheme> getAllThemes() {

      setupTheme();

      final ArrayList<ITheme> allThemes = new ArrayList<>();

      for (final ITheme theme : _themeEngine.getThemes()) {

         /*
          * When we have Win32 OS - when the high contrast mode is enabled on the
          * platform, we display the 'high-contrast' special theme only. If not, we don't
          * want to mess the themes combo with the theme since it is the special
          * variation of the 'classic' one
          * When we have GTK - we have to display the entire list of the themes since we
          * are not able to figure out if the high contrast mode is enabled on the
          * platform. The user has to manually select the theme if they need it
          */
//         if (!highContrastMode && !Util.isGtk() && theme.getId().equals(E4Application.HIGH_CONTRAST_THEME_ID)) {
//            continue;
//         }

         // hide high contrast theme in all cases to have a clean combo
         if (theme.getId().equals(HIGH_CONTRAST_THEME_ID)) {
            continue;
         }

         allThemes.add(theme);
      }

      // sort themes by their name
      allThemes.sort((final ITheme t1, final ITheme t2) -> t1.getLabel().compareTo(t2.getLabel()));

      return allThemes;
   }

   /**
    * @return Returns themed color for displaying errors
    */
   public static Color getErrorColor() {

      return UI.IS_DARK_THEME

            // yellow is very bright in the dark theme compared with other colors
            ? UI.SYS_COLOR_YELLOW

            : UI.SYS_COLOR_RED;
   }

   /**
    * @return The tour chart do not show a dark background color when displayed in a dialog, it
    *         shows the background color from the shell.
    *         <p>
    *         Returns a color which is used to show the tour chart with a dark background color, to
    *         have a contrast to the dialog background.
    */
   public static Color getDarkestBackgroundColor() {

      return _defaultBackgroundColor_Table;
   }

   public static Color getDarkestForegroundColor() {

      return _defaultForegroundColor_Table;
   }

   public static Color getDefaultBackgroundColor_Combo() {
      return _defaultBackgroundColor_Combo;
   }

   public static Color getDefaultBackgroundColor_Shell() {
      return _defaultBackgroundColor_Shell;
   }

   /**
    * @return Returns the table default background color for light or dark theme
    */
   public static Color getDefaultBackgroundColor_Table() {
      return _defaultBackgroundColor_Table;
   }

   /**
    * @return Returns the table header default background color for light or dark theme
    */
   public static Color getDefaultBackgroundColor_TableHeader() {
      return _defaultBackgroundColor_TableHeader;
   }

   public static Color getDefaultForegroundColor_Combo() {
      return _defaultForegroundColor_Combo;
   }

   public static Color getDefaultForegroundColor_Shell() {
      return _defaultForegroundColor_Shell;
   }

   /**
    * @return Returns the table default foreground color for light or dark theme
    */
   public static Color getDefaultForegroundColor_Table() {
      return _defaultForegroundColor_Table;
   }

   /**
    * @return Returns the table header default foreground color for light or dark theme
    */
   public static Color getDefaultForegroundColor_TableHeader() {
      return _defaultForegroundColor_TableHeader;
   }

   public static String getThemedCss_DefaultBackground() {

      return UI.IS_DARK_THEME
            ? ThemeUtil.DEFAULT_BACKGROUND_DARK_THEME
            : ThemeUtil.DEFAULT_BACKGROUND_LIGHT_THEME;
   }

   public static String getThemedCss_DefaultForeground() {

      return UI.IS_DARK_THEME
            ? ThemeUtil.DEFAULT_FOREGROUND_DARK_THEME
            : ThemeUtil.DEFAULT_FOREGROUND_LIGHT_THEME;
   }

   /**
    * @param imageName
    * @return Returns the themed image name. The postfix {@value #DARK_THEME_POSTFIX} is
    *         appended to the image name when the dark theme image name is returned.
    */
   public static String getThemedImageName(final String imageName) {

      String imageNameThemed;

      if (UI.IS_DARK_THEME) {

         imageNameThemed = imageName.substring(0, imageName.length() - 4) + DARK_THEME_POSTFIX + IMAGE_NAME_EXTENSION_PNG;

      } else {
         imageNameThemed = imageName;
      }

      return imageNameThemed;
   }

   /**
    * In the dark theme the right aligned text in a tree control is just left of the column
    * separator which cannot be set hidden, it looks just awful
    */
   public static String getThemedTreeHeaderLabel(final String headerLabel) {

      if (UI.IS_DARK_THEME) {

         return headerLabel + UI.SPACE2;
      }

      return headerLabel;
   }

   public static IThemeEngine getThemeEngine() {

      setupTheme();

      return _themeEngine;
   }

   /**
    * W10 do not set all controls into a dark mode, e.g. menu bar
    * <p>
    * Copied from org.eclipse.swt.internal.win32.OS.setTheme(boolean)
    * <p>
    * See also {@link "https://www.eclipse.org/eclipse/news/4.16/platform_isv.html#win-dark-tweaks"}
    * <p>
    * Experimental API for dark theme.
    * <p>
    * On Windows, there is no OS API for dark theme yet, and this method only
    * configures various tweaks. Some of these tweaks have drawbacks. The tweaks
    * are configured with defaults that fit Eclipse. Non-Eclipse applications are
    * expected to configure individual tweaks instead of calling this method.
    * Please see <code>Display#setData()</code> and documentation for string keys
    * used there.
    *
    * @param isDarkTheme
    *           <code>true</code> for dark theme
    */
   public static final void setDarkTheme(final boolean isDarkTheme) {

      UI.setIsDarkTheme(isDarkTheme);

      if (UI.IS_WIN) {

         // this hack is only for windows

         final Display display = Display.getDefault();

// SET_FORMATTING_OFF

         // menu colors
         final Color menuBarForegroundColor        = new Color(0xD0, 0xD0, 0xD0);
         final Color menuBarBackgroundColor        = new Color(0x30, 0x30, 0x30);
         final Color menuBarBorderColor            = new Color(0x50, 0x50, 0x50);

         // table header color: 38 3D 3F
         final Color table_HeaderLineColor         = new Color(0x50, 0x50, 0x50);
         final Color label_DisabledForegroundColor = new Color(0x80, 0x80, 0x80);

         display.setData("org.eclipse.swt.internal.win32.useDarkModeExplorerTheme",       isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.menuBarForegroundColor",         isDarkTheme ? menuBarForegroundColor : null);         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.menuBarBackgroundColor",         isDarkTheme ? menuBarBackgroundColor : null);         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.menuBarBorderColor",             isDarkTheme ? menuBarBorderColor     : null);         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Canvas.use_WS_BORDER",           isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.List.use_WS_BORDER",             isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Table.use_WS_BORDER",            isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Text.use_WS_BORDER",             isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Tree.use_WS_BORDER",             isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Table.headerLineColor",          isDarkTheme ? table_HeaderLineColor         : null);  //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Label.disabledForegroundColor",  isDarkTheme ? label_DisabledForegroundColor : null);  //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.Combo.useDarkTheme",             isDarkTheme);                                         //$NON-NLS-1$
         display.setData("org.eclipse.swt.internal.win32.ProgressBar.useColors",          isDarkTheme);                                         //$NON-NLS-1$

// SET_FORMATTING_ON

      }
   }

   public static final void setupTheme() {

      if (_themeEngine != null) {
         return;
      }

      final MApplication application = PlatformUI.getWorkbench().getService(MApplication.class);
      final IEclipseContext context = application.getContext();

      _themeEngine = context.get(org.eclipse.e4.ui.css.swt.theme.IThemeEngine.class);

      final ITheme activeTheme = _themeEngine.getActiveTheme();
      if (activeTheme != null) {

         final boolean isDarkThemeSelected = E4_DARK_THEME_ID.equals(activeTheme.getId());

         setDarkTheme(isDarkThemeSelected);
      }

      final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

      final Combo combo = new Combo(shell, SWT.READ_ONLY);
      final Table table = new Table(shell, SWT.BORDER);

//      System.out.println((System.currentTimeMillis() + " shell.getBackground()      1 " + shell.getBackground()));
//      System.out.println((System.currentTimeMillis() + " table.getBackground()      1 " + table.getBackground()));
//      System.out.println((System.currentTimeMillis() + " shell.getForeground()      1 " + shell.getForeground()));
//      System.out.println((System.currentTimeMillis() + " table.getForeground()      1 " + table.getForeground()));

      Display.getDefault().asyncExec(() -> {

//         System.out.println((System.currentTimeMillis() + " shell.getBackground()      2 " + shell.getBackground()));
//         System.out.println((System.currentTimeMillis() + " table.getBackground()      2 " + table.getBackground()));
//         System.out.println((System.currentTimeMillis() + " shell.getForeground()      2 " + shell.getForeground()));
//         System.out.println((System.currentTimeMillis() + " table.getForeground()      2 " + table.getForeground()));

         _defaultForegroundColor_Shell = shell.getForeground();
         _defaultBackgroundColor_Shell = shell.getBackground();

         _defaultForegroundColor_Combo = combo.getForeground();
         _defaultBackgroundColor_Combo = combo.getBackground();

         // I found, that a table do have a darker background color than the shell

         _defaultForegroundColor_Table = table.getForeground();
         _defaultBackgroundColor_Table = table.getBackground();

         _defaultForegroundColor_TableHeader = table.getHeaderForeground();
         _defaultBackgroundColor_TableHeader = table.getHeaderBackground();

         combo.dispose();
         table.dispose();
      });

   }
}
