/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.tourMarker;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage recent tour markers
 */
public class TourMarkerManager {

   private static final String              CONFIG_FILE_NAME             = "tour-marker.xml";                      //$NON-NLS-1$

   private static final Bundle              _bundle                      = TourbookPlugin.getDefault().getBundle();
   private static final IPath               _stateLocation               = Platform.getStateLocation(_bundle);

   /**
    * Version number is not yet used.
    */
   private static final int                 CONFIG_VERSION               = 1;
   private static final String              ATTR_CONFIG_VERSION          = "configVersion";                        //$NON-NLS-1$

   private static final String              RECENT_MARKER_ROOT           = "RecentMarkers";                        //$NON-NLS-1$
   private static final String              RECENT_MARKER                = "RecentMarker";                         //$NON-NLS-1$

   private static final String              ATTR_LABEL                   = "label";                                //$NON-NLS-1$

   public static final int                  MAX_NUMBER_OF_RECENT_MARKERS = 20;

   private static LinkedList<RecentMarker>  _allRecentMarker             = new LinkedList<>();

   /**
    * Key is the marker label
    */
   private static Map<String, RecentMarker> _allRecentMarkerMap          = new HashMap<>();

   static {

      restore();
   }

   /**
    * Keep recent updated marker
    *
    * @param tourMarker
    */
   public static void addRecentMarker(final String markerLabel) {

      final RecentMarker existingMarker = _allRecentMarkerMap.get(markerLabel);

      if (existingMarker == null) {

         // marker is new

         /*
          * Create a new marker
          */
         final RecentMarker newMarker = new RecentMarker(markerLabel);

         _allRecentMarker.addFirst(newMarker);

         _allRecentMarkerMap.put(markerLabel, newMarker);

         /*
          * Check number of max markers, remove last used marker
          */
         if (_allRecentMarker.size() > MAX_NUMBER_OF_RECENT_MARKERS) {

            final RecentMarker lastMarker = _allRecentMarker.removeLast();

            _allRecentMarkerMap.remove(lastMarker.label);
         }

      } else {

         // marker exists -> move it to the top

         _allRecentMarker.remove(existingMarker);
         _allRecentMarker.addFirst(existingMarker);
      }
   }

   private static XMLMemento createXML_WriteRoot() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(RECENT_MARKER_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // config version
      xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

      return xmlRoot;
   }

   public static LinkedList<RecentMarker> getRecentMarkers() {

      return _allRecentMarker;
   }

   private static File getXmlFile() {

      final File xmlFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   public static void removeRecentMarker(final RecentMarker recentMarker) {

      // update model
      _allRecentMarker.remove(recentMarker);
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static synchronized void restore() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get locations from saved xml file
         final File xmlFile = getXmlFile();
         final String absoluteFilePath = xmlFile.getAbsolutePath();
         final File inputFile = new File(absoluteFilePath);

         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         if (xmlRoot == null) {
            return;
         }

         restore_10_All(xmlRoot);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    */
   private static void restore_10_All(final XMLMemento xmlRoot) {

      for (final IMemento mementoRecentMarker : xmlRoot.getChildren()) {

         final XMLMemento xmlRecentMarker = (XMLMemento) mementoRecentMarker;

         try {

            final String xmlConfigType = xmlRecentMarker.getType();

            if (xmlConfigType.equals(RECENT_MARKER)) {

               // <RecentMarker>

               final RecentMarker recentMarker = restore_20_One(xmlRecentMarker);

               final String markerKey = recentMarker.label;

               // skip duplicates
               if (_allRecentMarkerMap.containsKey(markerKey)) {
                  continue;
               }

               _allRecentMarker.add(recentMarker);
               _allRecentMarkerMap.put(markerKey, recentMarker);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlRecentMarker), e);
         }
      }
   }

   private static RecentMarker restore_20_One(final XMLMemento xmlMarker) {

      final RecentMarker recentMarker = new RecentMarker();

      recentMarker.label = Util.getXmlString(xmlMarker, ATTR_LABEL, UI.EMPTY_STRING);

      return recentMarker;
   }

   public static void saveState() {

      final XMLMemento xmlRoot = createXML_WriteRoot();

      saveState_10_RecentMarker(xmlRoot);

      Util.writeXml(xmlRoot, getXmlFile());
   }

   private static void saveState_10_RecentMarker(final XMLMemento xmlRoot) {

      for (final RecentMarker recentMarker : _allRecentMarker) {

         // <RecentMarker>
         final IMemento xmlRecentMarker = xmlRoot.createChild(RECENT_MARKER);
         {
            xmlRecentMarker.putString(ATTR_LABEL, recentMarker.label);
         }
      }
   }

}
