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
package de.byteholder.geoclipse.mapprovider;

import de.byteholder.geoclipse.map.Tile;

import java.util.ArrayList;

import net.tourbook.common.UI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class MPCustom extends MP {

   private String             _customUrl = UI.EMPTY_STRING;

   private ArrayList<UrlPart> _urlParts  = new ArrayList<>();

   public MPCustom() {

   }

   @Override
   public Object clone() throws CloneNotSupportedException {

      final MPCustom mapProvider = (MPCustom) super.clone();

      mapProvider._customUrl = new String(_customUrl);

      // clone url parts
      final ArrayList<UrlPart> newUrlParts = new ArrayList<>();
      for (final UrlPart urlPart : _urlParts) {
         newUrlParts.add((UrlPart) urlPart.clone());
      }
      mapProvider._urlParts = newUrlParts;

      return mapProvider;
   }

   public String getCustomUrl() {
      return _customUrl;
   }

   @Override
   public IPath getTileOSPath(final String fullPath, final Tile tile) {

      IPath filePath = new Path(fullPath);

      filePath = filePath.append(getOfflineFolder());

      filePath = filePath//
            .append(Integer.toString(tile.getZoom()))
            .append(Integer.toString(tile.getX()))
            .append(Integer.toString(tile.getY()))
            .addFileExtension(MapProviderManager.getImageFileExtension(getImageFormat()));

      return filePath;
   }

   @Override
   public String getTileUrl(final Tile tile) {

      if (_urlParts.size() == 0) {

         // url parts are not set yet, display openstreetmap

         return MapProviderManager.getDefaultMapProvider().getTileUrl(tile);

      } else {

         final StringBuilder sb = new StringBuilder();

         for (final UrlPart urlPart : _urlParts) {
            switch (urlPart.getPartType()) {

            case HTML:
               sb.append(urlPart.getHtml());
               break;

            case X:
               sb.append(Integer.toString(tile.getX()));
               break;

            case Y:
               sb.append(Integer.toString(tile.getY()));
               break;

//				case LAT_TOP:
//					sb.append(Double.toString(tile.getBboxLatitudeTop()));
//					break;
//
//				case LAT_BOTTOM:
//					sb.append(Double.toString(tile.getBboxLatitudeBottom()));
//					break;
//
//				case LON_LEFT:
//					sb.append(Double.toString(tile.getBboxLongitudeLeft()));
//					break;
//
//				case LON_RIGHT:
//					sb.append(Double.toString(tile.getBboxLongitudeRight()));
//					break;

            case ZOOM:
               sb.append(Integer.toString(tile.getZoom()));
               break;

            case RANDOM_INTEGER:

               final int startValue = urlPart.getRandomIntegerStart();
               final int endValue = urlPart.getRandomIntegerEnd();
               final int randomDiff = endValue - startValue + 1;

               final double random = Math.random() * randomDiff + startValue;
               final int randomInt = (int) random;

               sb.append(randomInt);

               break;

            default:
               break;
            }
         }

         return sb.toString();
      }
   }

   public ArrayList<UrlPart> getUrlParts() {
      return _urlParts;
   }

   public void setCustomUrl(final String customUrl) {
      _customUrl = customUrl;
   }

   public void setUrlParts(final ArrayList<UrlPart> urlParts) {

      _urlParts.clear();

      _urlParts = urlParts;
   }

}
