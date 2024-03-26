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
package net.tourbook.tour.location;

import net.tourbook.data.TourLocation;
import net.tourbook.ui.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class TourLocationUI {

   private TourLocationUI() {}

   /**
    * Create a UI to list all available tour location fields
    *
    * @param container
    *           This container requires 2 columns
    * @param tourLocation
    */
   public static void createUI(final Composite container, final TourLocation tourLocation) {

// SET_FORMATTING_OFF

         createUI_Content(container,   tourLocation.name,                  Messages.Tour_Location_Part_OsmName);

         createUI_Content(container,   tourLocation.country,               Messages.Tour_Location_Part_Country);
         createUI_Content(container,   tourLocation.country_code,          Messages.Tour_Location_Part_CountryCode);
         createUI_Content(container,   tourLocation.continent,             Messages.Tour_Location_Part_Continent);

         createUI_Content(container,   tourLocation.region,                Messages.Tour_Location_Part_Region);
         createUI_Content(container,   tourLocation.state,                 Messages.Tour_Location_Part_State);
         createUI_Content(container,   tourLocation.state_district,        Messages.Tour_Location_Part_StateDistrict);
         createUI_Content(container,   tourLocation.county,                Messages.Tour_Location_Part_County);

         createUI_Content(container,   tourLocation.municipality,          Messages.Tour_Location_Part_Municipality);
         createUI_Content(container,   tourLocation.city,                  Messages.Tour_Location_Part_City);
         createUI_Content(container,   tourLocation.town,                  Messages.Tour_Location_Part_Town);
         createUI_Content(container,   tourLocation.village,               Messages.Tour_Location_Part_Village);
         createUI_Content(container,   tourLocation.postcode,              Messages.Tour_Location_Part_Postcode);

         createUI_Content(container,   tourLocation.road,                  Messages.Tour_Location_Part_Road);
         createUI_Content(container,   tourLocation.house_number,          Messages.Tour_Location_Part_HouseNumber);
         createUI_Content(container,   tourLocation.house_name,            Messages.Tour_Location_Part_HouseName);

         createUI_Content(container,   tourLocation.city_district,         Messages.Tour_Location_Part_CityDistrict);
         createUI_Content(container,   tourLocation.district,              Messages.Tour_Location_Part_District);
         createUI_Content(container,   tourLocation.borough,               Messages.Tour_Location_Part_Borough);
         createUI_Content(container,   tourLocation.suburb,                Messages.Tour_Location_Part_Suburb);
         createUI_Content(container,   tourLocation.subdivision,           Messages.Tour_Location_Part_Subdivision);

         createUI_Content(container,   tourLocation.hamlet,                Messages.Tour_Location_Part_Hamlet);
         createUI_Content(container,   tourLocation.croft,                 Messages.Tour_Location_Part_Croft);
         createUI_Content(container,   tourLocation.isolated_dwelling,     Messages.Tour_Location_Part_IsolatedDwelling);

         createUI_Content(container,   tourLocation.neighbourhood,         Messages.Tour_Location_Part_Neighbourhood);
         createUI_Content(container,   tourLocation.allotments,            Messages.Tour_Location_Part_Allotments);
         createUI_Content(container,   tourLocation.quarter,               Messages.Tour_Location_Part_Quarter);

         createUI_Content(container,   tourLocation.city_block,            Messages.Tour_Location_Part_CityBlock);
         createUI_Content(container,   tourLocation.residential,           Messages.Tour_Location_Part_Residential);
         createUI_Content(container,   tourLocation.farm,                  Messages.Tour_Location_Part_Farm);
         createUI_Content(container,   tourLocation.farmyard,              Messages.Tour_Location_Part_Farmyard);
         createUI_Content(container,   tourLocation.industrial,            Messages.Tour_Location_Part_Industrial);
         createUI_Content(container,   tourLocation.commercial,            Messages.Tour_Location_Part_Commercial);
         createUI_Content(container,   tourLocation.retail,                Messages.Tour_Location_Part_Retail);

         createUI_Content(container,   tourLocation.aerialway,             Messages.Tour_Location_Part_Aerialway);
         createUI_Content(container,   tourLocation.aeroway,               Messages.Tour_Location_Part_Aeroway);
         createUI_Content(container,   tourLocation.amenity,               Messages.Tour_Location_Part_Amenity);
         createUI_Content(container,   tourLocation.boundary,              Messages.Tour_Location_Part_Boundary);
         createUI_Content(container,   tourLocation.bridge,                Messages.Tour_Location_Part_Bridge);
         createUI_Content(container,   tourLocation.club,                  Messages.Tour_Location_Part_Club);
         createUI_Content(container,   tourLocation.craft,                 Messages.Tour_Location_Part_Craft);
         createUI_Content(container,   tourLocation.emergency,             Messages.Tour_Location_Part_Emergency);
         createUI_Content(container,   tourLocation.historic,              Messages.Tour_Location_Part_Historic);
         createUI_Content(container,   tourLocation.landuse,               Messages.Tour_Location_Part_Landuse);
         createUI_Content(container,   tourLocation.leisure,               Messages.Tour_Location_Part_Leisure);
         createUI_Content(container,   tourLocation.man_made,              Messages.Tour_Location_Part_ManMade);
         createUI_Content(container,   tourLocation.military,              Messages.Tour_Location_Part_Military);
         createUI_Content(container,   tourLocation.mountain_pass,         Messages.Tour_Location_Part_MountainPass);
         createUI_Content(container,   tourLocation.natural2,              Messages.Tour_Location_Part_Natural);
         createUI_Content(container,   tourLocation.office,                Messages.Tour_Location_Part_Office);
         createUI_Content(container,   tourLocation.place,                 Messages.Tour_Location_Part_Place);
         createUI_Content(container,   tourLocation.railway,               Messages.Tour_Location_Part_Railway);
         createUI_Content(container,   tourLocation.shop,                  Messages.Tour_Location_Part_Shop);
         createUI_Content(container,   tourLocation.tourism,               Messages.Tour_Location_Part_Tourism);
         createUI_Content(container,   tourLocation.tunnel,                Messages.Tour_Location_Part_Tunnel);
         createUI_Content(container,   tourLocation.waterway,              Messages.Tour_Location_Part_Waterway);

// SET_FORMATTING_ON

   }

   private static Text createUI_Content(final Composite parent, final String contentValue, final String contentLabel) {

      if (contentValue == null || contentValue.length() == 0) {
         return null;
      }

      // label
      final Label label = new Label(parent, SWT.NONE);

      // text
      final Text text = new Text(parent, SWT.READ_ONLY | SWT.WRAP);

      label.setText(contentLabel);
      text.setText(contentValue);

      return text;
   }

}
