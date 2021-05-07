/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class ThemeUtil {

   /**
    * Currently only .png files are supported for themed images !!!
    */
   private static final String IMAGE_NAME_EXTENSION_PNG = ".png";

   /**
    * All images for the dark theme should have this postfix before the file extension
    */
   private static final String DARK_THEME_POSTFIX       = "-dark"; //$NON-NLS-1$

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
    * @param imageName
    * @return Returns the themed image name. The postfix {@value #DARK_THEME_POSTFIX} is
    *         appended to
    *         the image name when the dark theme image name is returned.
    */
   public static String getThemedImageName(final String imageName) {

      String imageNameThemed;

      if (UI.isDarkTheme()) {

         imageNameThemed = imageName.substring(0, imageName.length() - 4) + DARK_THEME_POSTFIX + IMAGE_NAME_EXTENSION_PNG;

      } else {
         imageNameThemed = imageName;
      }

      return imageNameThemed;
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

         display.setData("org.eclipse.swt.internal.win32.useDarkModeExplorerTheme",       isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.menuBarForegroundColor",         isDarkTheme ? menuBarForegroundColor : null);
         display.setData("org.eclipse.swt.internal.win32.menuBarBackgroundColor",         isDarkTheme ? menuBarBackgroundColor : null);
         display.setData("org.eclipse.swt.internal.win32.menuBarBorderColor",             isDarkTheme ? menuBarBorderColor : null);
         display.setData("org.eclipse.swt.internal.win32.Canvas.use_WS_BORDER",           isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.List.use_WS_BORDER",             isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.Table.use_WS_BORDER",            isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.Text.use_WS_BORDER",             isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.Tree.use_WS_BORDER",             isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.Table.headerLineColor",          isDarkTheme ? table_HeaderLineColor : null);
         display.setData("org.eclipse.swt.internal.win32.Label.disabledForegroundColor",  isDarkTheme ? label_DisabledForegroundColor : null);
         display.setData("org.eclipse.swt.internal.win32.Combo.useDarkTheme",             isDarkTheme);
         display.setData("org.eclipse.swt.internal.win32.ProgressBar.useColors",          isDarkTheme);

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
   }
}
