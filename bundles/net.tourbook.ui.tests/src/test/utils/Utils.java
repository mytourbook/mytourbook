/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
package utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import net.tourbook.Messages;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class Utils {

   public static final String DIRECTORY               = "Directory";                    //$NON-NLS-1$
   public static final String SAVE_MODIFIED_TOUR      = "Save modified tour (Ctrl+S)";  //$NON-NLS-1$
   public static final String STATISTICS_VIEW_NAME    = "Statistics";                   //$NON-NLS-1$
   public static final String TOOLS                   = "Tools ";                       //$NON-NLS-1$
   public static final String TOURBOOK_VIEW_NAME      = "Tour Book";                    //$NON-NLS-1$
   public static final String TOUREDITOR_VIEW_NAME    = "Tour Editor";                  //$NON-NLS-1$
   public static final String TOURMARKERS_VIEW_NAME   = "Tour Markers";                 //$NON-NLS-1$
   public static final String TOURPAUSES_VIEW_NAME    = "Tour Pauses";                  //$NON-NLS-1$
   public static final String TOURSEGMENTER_VIEW_NAME = "Tour Segmenter";               //$NON-NLS-1$

   public static final String workingDirectory        = System.getProperty("user.dir"); //$NON-NLS-1$

   public static void changeMeasurementSystem(final SWTWorkbenchBot bot, final String measurementSystem) {

      Utils.openPreferences(bot);
      bot.tree().getTreeItem("General").select(); //$NON-NLS-1$
      bot.cTabItem(Messages.Pref_general_system_measurement).activate();
      bot.comboBox(0).setSelection(measurementSystem);
      Utils.clickApplyAndCloseButton(bot);
   }

   public static void clickApplyAndCloseButton(final SWTWorkbenchBot bot) {

      clickButton("Apply and Close", bot); //$NON-NLS-1$
   }

   private static SWTBotButton clickButton(final String mnemonicText, final SWTWorkbenchBot bot) {

      return bot.button(mnemonicText).click();
   }

   public static void clickCancelButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.CANCEL_LABEL, bot);
   }

   public static void clickCloseButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.CLOSE_LABEL, bot);
   }

   public static void clickOkButton(final SWTWorkbenchBot bot) {

      clickButton(IDialogConstants.OK_LABEL, bot);
   }

   public static SWTBotTreeItem getTour(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2021   3").expand() //$NON-NLS-1$
            .getNode("Jan   3").expand().select().getNode("31").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   /**
    * Select a tour for which we have SRTM3 data
    */
   public static SWTBotTreeItem getTourWithSRTM(final SWTWorkbenchBot bot) {

      showTourBookView(bot);

      final SWTBotTreeItem tour = bot.tree().getTreeItem("2013   1").expand() //$NON-NLS-1$
            .getNode("May   1").expand().select().getNode("18").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);

      return tour;
   }

   public static boolean isUrlReachable(final String url) {

      final HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .build();

      try {
         final HttpResponse<String> response = OAuth2Utils.httpClient.send(request, BodyHandlers.ofString());

         return response.statusCode() == HttpURLConnection.HTTP_OK;
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return false;
   }

   public static void openOtherMenu(final SWTWorkbenchBot bot) {

      final SWTBotMenu otherMenu = bot.menu(TOOLS).menu("All Views").menu("Other...").click(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(otherMenu);
   }

   public static void openPreferences(final SWTWorkbenchBot bot) {

      final String preferencesTooltip = UI.IS_LINUX ? "Preferences (Shift+Ctrl+P)" //$NON-NLS-1$
            : "Preferences (Ctrl+Shift+P)"; //$NON-NLS-1$
      bot.toolbarButtonWithTooltip(preferencesTooltip).click();
   }

   public static void openVendorPage(final SWTBotTreeItem treeItem, final String vendorName) {

      treeItem.getNode(vendorName).select();
   }

   public static SWTBotView showTourBookView(final SWTWorkbenchBot bot) {

      return showView(bot, TOURBOOK_VIEW_NAME);
   }

   public static SWTBotView showView(final SWTWorkbenchBot bot, final String viewName) {

      final SWTBotView view = bot.viewByTitle(viewName);
      assertNotNull(view);
      view.show();

      return view;
   }

   public static void showViewFromMenu(final SWTWorkbenchBot bot, final String menuName, final String viewName) {

      final SWTBotMenu viewMenu = bot.menu(menuName).menu(viewName).click();
      assertNotNull(viewMenu);
   }
}
