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
package net.tourbook.statistic;

import java.util.ArrayList;
import java.util.regex.Pattern;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

public class StatisticManager {

// SET_FORMATTING_OFF

   private static final Pattern PATTERN_FIELDS           = Pattern.compile(",");                                            //$NON-NLS-1$
   private static final Pattern PATTERN_SPACES           = Pattern.compile("  *");                                          //$NON-NLS-1$
   private static final Pattern PATTERN_EMPTY_LINES      = Pattern.compile("^(?:[\t ]*(?:\r?\n|\r))+", Pattern.MULTILINE);  //$NON-NLS-1$

   private static final Pattern NUMBER_PATTERN_0         = Pattern.compile(" 0 ");                                          //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_END     = Pattern.compile(" 0$",     Pattern.MULTILINE);                   //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_0       = Pattern.compile(" 0.0 ");                                        //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_0_END   = Pattern.compile(" 0.0$",   Pattern.MULTILINE);                   //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_00      = Pattern.compile(" 0.00 ");                                       //$NON-NLS-1$
   private static final Pattern NUMBER_PATTERN_0_00_END  = Pattern.compile(" 0.00$",  Pattern.MULTILINE);                   //$NON-NLS-1$

//SET_FORMATTING_ON

   private static ArrayList<TourbookStatistic> _statisticExtensionPoints;

   /**
    * Keeps reference to the statistic view, is <code>null</code> when statistic view is closed.
    */
   private static StatisticView                _statisticView;

   /**
    * Join 2 header lines into 1 line
    */
   private static String convertHeaderLines(final String rawStatisticValues) {

      final Object[] allStatValueLines = rawStatisticValues.lines().toArray();

      final int numAllLines = allStatValueLines.length;

      final String line1 = numAllLines > 0 ? (String) allStatValueLines[0] : null;
      final String line2 = numAllLines > 1 ? (String) allStatValueLines[1] : null;

      if (line1 == null || line2 == null) {
         return rawStatisticValues;
      }

      final String line1_NoSpaces = PATTERN_SPACES.matcher(line1).replaceAll(UI.EMPTY_STRING);
      final String line2_NoSpaces = PATTERN_SPACES.matcher(line2).replaceAll(UI.EMPTY_STRING);

      final String[] allLine1_Fields = PATTERN_FIELDS.split(line1_NoSpaces);
      final String[] allLine2_Fields = PATTERN_FIELDS.split(line2_NoSpaces);

      final int numFields = allLine1_Fields.length;

      final StringBuilder sbHeader = new StringBuilder();

      for (int lineIndex = 0; lineIndex < numFields; lineIndex++) {

         final String line1_Field = allLine1_Fields[lineIndex];
         final String line2_Field = allLine2_Fields[lineIndex];

         sbHeader.append(line1_Field);

         if (line2_Field.length() > 0) {
            sbHeader.append(UI.SPACE);
            sbHeader.append(line2_Field);
         }

         if (lineIndex < numFields - 1) {
            sbHeader.append(UI.SYMBOL_COMMA);
         }
      }
      final String joinedHeader = sbHeader.toString();

      // create final stat value string
      final StringBuilder sbAll = new StringBuilder();

      sbAll.append(joinedHeader);
      sbAll.append(UI.NEW_LINE);

      for (int lineIndex = 2; lineIndex < numAllLines; lineIndex++) {
         sbAll.append(allStatValueLines[lineIndex]);
         sbAll.append(UI.NEW_LINE);
      }

      final String statValuesWithNewHeader = sbAll.toString();

      return statValuesWithNewHeader;
   }

   static void copyStatisticValuesToTheClipboard(final String rawStatisticValues) {

      final boolean isCSVFormat = true;
      final boolean isGroupValues = false; // remove empty lines
      final boolean isRemoveZeros = false;
      final boolean isShowRawData = false;

      final String statValues = formatStatValues(rawStatisticValues,
            isCSVFormat,
            isRemoveZeros,
            isGroupValues,
            isShowRawData);

      final Display display = Display.getDefault();

      if (statValues.length() > 0) {

         final TextTransfer textTransfer = TextTransfer.getInstance();

         final Clipboard clipBoard = new Clipboard(display);
         {
            clipBoard.setContents(

                  new Object[] { statValues },
                  new Transfer[] { textTransfer }

            );
         }
         clipBoard.dispose();

         final IStatusLineManager statusLineMgr = UI.getStatusLineManager();
         if (statusLineMgr != null) {

            // show info that data are copied
            statusLineMgr.setMessage(Messages.Tour_StatisticValues_Info_DataAreCopied);

            display.timerExec(2000,
                  () -> {

                     // cleanup message
                     statusLineMgr.setMessage(null);
                  });
         }
      }
   }

   public static String formatStatValues(String statValues,
                                         final boolean isCSVFormat,
                                         final boolean isRemoveZeros,
                                         final boolean isGroupValues,
                                         final boolean isShowRawData) {

      if (isShowRawData) {

         // do not reformat values

      } else if (isCSVFormat) {

         final String statValuesWithNewHeader = convertHeaderLines(statValues);

         // always remove spaces
         statValues = PATTERN_SPACES.matcher(statValuesWithNewHeader).replaceAll(UI.EMPTY_STRING);

         // always remove empty lines
         statValues = PATTERN_EMPTY_LINES.matcher(statValues).replaceAll(UI.EMPTY_STRING);

      } else {

         // remove field separator
         statValues = PATTERN_FIELDS.matcher(statValues).replaceAll(UI.EMPTY_STRING);

         // remove empty lines
         if (isGroupValues == false) {
            statValues = PATTERN_EMPTY_LINES.matcher(statValues).replaceAll(UI.EMPTY_STRING);
         }

         // remove zeros
         if (isRemoveZeros) {

// SET_FORMATTING_OFF

            statValues = NUMBER_PATTERN_0.          matcher(statValues).replaceAll("   ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_END.      matcher(statValues).replaceAll("  ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_0.        matcher(statValues).replaceAll("     ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_0_END.    matcher(statValues).replaceAll("    ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_00.       matcher(statValues).replaceAll("      ");//$NON-NLS-1$
            statValues = NUMBER_PATTERN_0_00_END.   matcher(statValues).replaceAll("     ");//$NON-NLS-1$

// SET_FORMATTING_ON
         }
      }

      return statValues;
   }

   /**
    * This method is synchronized to conform to FindBugs
    *
    * @return Returns statistics from the extension registry in the sort order of the registry
    */
   public static synchronized ArrayList<TourbookStatistic> getStatisticExtensionPoints() {

      if (_statisticExtensionPoints != null) {
         return _statisticExtensionPoints;
      }

      _statisticExtensionPoints = new ArrayList<>();

      final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(
            TourbookPlugin.PLUGIN_ID,
            TourbookPlugin.EXT_POINT_STATISTIC_YEAR);

      if (extPoint != null) {

         for (final IExtension extension : extPoint.getExtensions()) {

            for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

               if (configElement.getName().equalsIgnoreCase("statistic")) { //$NON-NLS-1$

                  Object object;
                  try {
                     object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
                     if (object instanceof TourbookStatistic) {

                        final TourbookStatistic statisticItem = (TourbookStatistic) object;

                        statisticItem.plugin_StatisticId = configElement.getAttribute("id"); //$NON-NLS-1$
                        statisticItem.plugin_VisibleName = configElement.getAttribute("name"); //$NON-NLS-1$
                        statisticItem.plugin_Category_Data = configElement.getAttribute("category-data"); //$NON-NLS-1$
                        statisticItem.plugin_Category_Time = configElement.getAttribute("category-time"); //$NON-NLS-1$

                        _statisticExtensionPoints.add(statisticItem);
                     }
                  } catch (final CoreException e) {
                     e.printStackTrace();
                  }
               }
            }
         }
      }

      return _statisticExtensionPoints;
   }

   /**
    * @return Returns statistic providers with the custom sort order
    */
   public static ArrayList<TourbookStatistic> getStatisticProviders() {

      final ArrayList<TourbookStatistic> availableStatistics = getStatisticExtensionPoints();
      final ArrayList<TourbookStatistic> visibleStatistics = new ArrayList<>();

      final IPreferenceStore prefStore = TourbookPlugin.getPrefStore();
      final String providerIds = prefStore.getString(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS);

      final String[] prefStoreStatisticIds = StringToArrayConverter.convertStringToArray(providerIds);

      // get all statistics which are saved in the pref store
      for (final String statisticId : prefStoreStatisticIds) {

         // get statistic item from the id
         for (final TourbookStatistic tourbookStatistic : availableStatistics) {
            if (statisticId.equals(tourbookStatistic.plugin_StatisticId)) {
               visibleStatistics.add(tourbookStatistic);
               break;
            }
         }
      }

      // get statistics which are available but not saved in the prefstore
      for (final TourbookStatistic availableStatistic : availableStatistics) {

         if (visibleStatistics.contains(availableStatistic) == false) {
            visibleStatistics.add(availableStatistic);
         }
      }

      return visibleStatistics;
   }

   /**
    * @return the _statisticView
    */
   public static StatisticView getStatisticView() {
      return _statisticView;
   }

   /**
    * @param statisticView
    *           the _statisticView to set
    */
   public static void setStatisticView(final StatisticView statisticView) {
      _statisticView = statisticView;
   }
}
