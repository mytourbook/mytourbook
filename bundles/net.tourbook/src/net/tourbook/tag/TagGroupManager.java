/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;

import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Manage tag groups
 */
public class TagGroupManager {

   private static final String         CONFIG_FILE_NAME    = "tag-groups.xml";                       //$NON-NLS-1$

   private static final Bundle         _bundle             = TourbookPlugin.getDefault().getBundle();
   private static final IPath          _stateLocation      = Platform.getStateLocation(_bundle);

   /**
    * Version number is not yet used.
    */
   private static final int            CONFIG_VERSION      = 1;
   private static final String         ATTR_CONFIG_VERSION = "configVersion";                        //$NON-NLS-1$
   //
   private static final String         TAG_ROOT            = "TagGroups";                            //$NON-NLS-1$
   private static final String         TAG_TAG_GROUP       = "TagGroup";                             //$NON-NLS-1$
   //
   private static final String         ATTR_NAME           = "name";                                 //$NON-NLS-1$
   private static final String         ATTR_TAG_IDS        = "tagIDs";                               //$NON-NLS-1$
   //
   private static final List<TagGroup> _allTagGroups       = new ArrayList<>();
   private static List<TagGroup>       _allTagGroupsSorted;

   static {

      // load locations
      readTagGroupsFromXml();
   }

   public static void addTagGroup(final TagGroup tagGroup) {

      // update model
      _allTagGroups.add(tagGroup);

      _allTagGroupsSorted = null;
   }

   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

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

   /**
    * @return Returns all {@link TagGroup}'s.
    *         <p>
    *         Do not modify this list, use {@link #addTagGroup(TagGroup)} or
    *         {@link #removeTagGroups(List)} to modify this list
    */
   public static List<TagGroup> getTagGroups() {

      return _allTagGroups;
   }

   /**
    * @return Returns a copy of all {@link TagGroup}'s sorted by name.
    */
   public static List<TagGroup> getTagGroupsSorted() {

      if (_allTagGroupsSorted != null) {
         return _allTagGroupsSorted;
      }

      _allTagGroupsSorted = new ArrayList<>(_allTagGroups);

      Collections.sort(
            _allTagGroupsSorted,
            (tagGroup1, tagGroup2) -> tagGroup1.name.compareTo(tagGroup2.name));

      return _allTagGroupsSorted;
   }

   private static File getXmlFile() {

      final File xmlFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return xmlFile;
   }

   /**
    * @param xmlRoot
    *           Can be <code>null</code> when not available
    * @param allTagGroups
    */
   private static void parse_10_TagGroups(final XMLMemento xmlRoot,
                                          final List<TagGroup> allTagGroups) {

      for (final IMemento mementoGroup : xmlRoot.getChildren()) {

         final XMLMemento xmlGroup = (XMLMemento) mementoGroup;

         try {

            final String xmlConfigType = xmlGroup.getType();

            if (xmlConfigType.equals(TAG_TAG_GROUP)) {

               // <TagGroup>

               allTagGroups.add(parse_22_TagGroups_One(xmlGroup));
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlGroup), e);
         }
      }
   }

   private static TagGroup parse_22_TagGroups_One(final XMLMemento xmlGroup) {

      final TagGroup tagGroup = new TagGroup();

      tagGroup.name = Util.getXmlString(xmlGroup, ATTR_NAME, null);

      final long[] tagIds = Util.getXmlLongArray(xmlGroup, ATTR_TAG_IDS);

      final Set<TourTag> allXmlTourTags = new HashSet<>();
      final HashMap<Long, TourTag> allDbTourTags = TourDatabase.getAllTourTags();

      for (final long tagID : tagIds) {

         final TourTag tourTag = allDbTourTags.get(tagID);

         if (tourTag != null) {
            allXmlTourTags.add(tourTag);
         }
      }

      tagGroup.tourTags = allXmlTourTags;

      return tagGroup;
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static synchronized void readTagGroupsFromXml() {

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

         parse_10_TagGroups(xmlRoot, _allTagGroups);

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void removeTagGroup(final TagGroup tagGroup) {

      // update model
      _allTagGroups.remove(tagGroup);

      _allTagGroupsSorted = null;
   }

   public static void saveState() {

      final XMLMemento xmlRoot = create_Root();

      saveState_10_TagGroups(xmlRoot);

      Util.writeXml(xmlRoot, getXmlFile());
   }

   private static void saveState_10_TagGroups(final XMLMemento xmlRoot) {

      for (final TagGroup tagGroup : _allTagGroups) {

         // <TagGroup>
         final IMemento xmlTagGroup = xmlRoot.createChild(TAG_TAG_GROUP);
         {
            // get all tag ID's
            final LongArrayList allTagIDs = new LongArrayList();
            final Set<TourTag> allTags = tagGroup.tourTags;

            if (allTags != null) {

               for (final TourTag tourTag : allTags) {
                  allTagIDs.add(tourTag.getTagId());
               }
            }

            xmlTagGroup.putString(ATTR_NAME, tagGroup.name);
            Util.setXmlLongArray(xmlTagGroup, ATTR_TAG_IDS, allTagIDs.toArray());
         }
      }
   }

}
