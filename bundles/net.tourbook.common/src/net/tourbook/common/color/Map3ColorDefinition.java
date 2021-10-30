/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Contains all colors for one graph type (e.g. altitude) but only one is currently active.
 */
public class Map3ColorDefinition implements Comparable<Map3ColorDefinition> {

   private MapGraphId                                          _graphId;
   private String                                              _visibleName;

   private int                                                 _sortId;

   /**
    * Contains all color provider for one {@link MapGraphId}.
    */
   private ArrayList<Map3GradientColorProvider>                _colorProviders = new ArrayList<>();

   private final Comparator<? super Map3GradientColorProvider> _colorProviderComparator;

   {
      _colorProviderComparator = (map3GradientColorProvider1, map3GradientColorProvider2) -> {

         final String profileName1 = map3GradientColorProvider1.getMap3ColorProfile().getProfileName();
         final String profileName2 = map3GradientColorProvider2.getMap3ColorProfile().getProfileName();

         return profileName1.compareTo(profileName2);
      };

   }

   public Map3ColorDefinition(final MapGraphId graphId) {

      _graphId = graphId;

      _sortId = getSortId(graphId);
   }

   /**
    * Sets the color for the default, current and changes
    *
    * @param graphId
    * @param visibleName
    * @param colorProfile
    */
   protected Map3ColorDefinition(final MapGraphId graphId,
                                 final String visibleName,
                                 final ArrayList<Map3ColorProfile> colorProfiles) {

      this(graphId);

      _visibleName = visibleName;

      for (final Map3ColorProfile colorProfile : colorProfiles) {
         _colorProviders.add(new Map3GradientColorProvider(graphId, colorProfile));
      }
   }

   void addColorProvider(final Map3GradientColorProvider newColorProvider) {

      _colorProviders.add(newColorProvider);

      Collections.sort(_colorProviders, _colorProviderComparator);

      // ensure that only one profile is active
      final Map3ColorProfile newColorProfile = newColorProvider.getMap3ColorProfile();
      if (newColorProfile.isActiveColorProfile()) {

         // new color provider is active, set all other profiles to be inactive

         for (final Map3GradientColorProvider colorProvider : _colorProviders) {

            final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();

            if (colorProfile != newColorProfile) {
               colorProfile.setIsActiveColorProfile(false);
            }
         }
      }
   }

   void addProfile(final Map3ColorProfile newColorProfile) {

      // wrap into a new color provider
      final Map3GradientColorProvider newColorProvider = new Map3GradientColorProvider(_graphId, newColorProfile);

      addColorProvider(newColorProvider);
   }

   @Override
   public int compareTo(final Map3ColorDefinition otherDef) {

      // sort by sort id
      return _sortId - otherDef._sortId;
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }

      final Map3ColorDefinition other = (Map3ColorDefinition) obj;
      if (_graphId == null) {
         if (other._graphId != null) {
            return false;
         }
      } else if (!_graphId.equals(other._graphId)) {
         return false;
      }

      return true;
   }

   public List<Map3GradientColorProvider> getColorProviders() {
      return _colorProviders;
   }

   public MapGraphId getGraphId() {
      return _graphId;
   }

   private int getSortId(final MapGraphId graphId) {

      switch (graphId) {
      case Altitude:
         return 1;

      case Pulse:
         return 2;

      case Speed:
         return 3;

      case Pace:
         return 4;

      case Power:
         return 5;

      case Temperature:
         return 6;

      case Gradient:
         return 7;

      case Altimeter:
         return 8;

      case Cadence:
         return 9;

      case HrZone:
         return 10;

      default:
         return 1;
      }
   }

   public String getVisibleName() {
      return _visibleName;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((_graphId == null) ? 0 : _graphId.hashCode());
      return result;
   }

   public void removeColorProvider(final Map3GradientColorProvider removableColorProvider) {

      int removedIndex = -1;
      for (int providerIndex = 0; providerIndex < _colorProviders.size(); providerIndex++) {

         final Map3GradientColorProvider colorProvider = _colorProviders.get(providerIndex);

         if (colorProvider == removableColorProvider) {
            removedIndex = providerIndex;
            break;
         }
      }

      if (removedIndex == -1) {
         // this case should not happen
         return;
      }

      final Map3GradientColorProvider removedColorProvider = _colorProviders.remove(removedIndex);

      // check if the color provider is the active color provider
      if (removedColorProvider.getMap3ColorProfile().isActiveColorProfile()) {

         // set a new color provider as active which is at the current position

         int activeIndex = removedIndex;

         // ensure array bounds
         final int lastIndex = _colorProviders.size() - 1;
         if (activeIndex > lastIndex) {
            activeIndex = lastIndex;
         }

         _colorProviders.get(activeIndex).getMap3ColorProfile().setIsActiveColorProfile(true);
      }
   }

   public void setColorProvider(final List<Map3GradientColorProvider> colorProvider) {

      _colorProviders.clear();
      _colorProviders.addAll(colorProvider);
   }

   public void setGraphId(final MapGraphId graphId) {
      _graphId = graphId;
   }

   public void setVisibleName(final String visibleName) {
      _visibleName = visibleName;
   }

   @Override
   public String toString() {
      return String.format("Map3ColorDefinition [_graphId=%s]", _graphId); //$NON-NLS-1$
   }

}
