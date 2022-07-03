/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package net.tourbook.device.sporttracks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.common.util.StringUtils;
import net.tourbook.device.sporttracks.FitLog_SAXHandler.Equipment;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * For FitLogEx files only:
 * We parse and save the CustomDataFieldDefinitions and equipments
 */
public class FitLogEx_SAXHandler extends DefaultHandler {

   private static final String            TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION   = FitLog_SAXHandler.TAG_ACTIVITY_CUSTOM_DATA_FIELD
         + "Definition";                                                                                                                 //$NON-NLS-1$
   private static final String            TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS  = TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION + "s"; //$NON-NLS-1$
   private static final String            TAG_ACTIVITY_EQUIPMENT                      = "Equipment";                                     //$NON-NLS-1$
   private static final String            TAG_EQUIPMENT_MODEL                         = "Model";                                         //$NON-NLS-1$

   private static final String            ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_NAME    = "Name";                                          //$NON-NLS-1$
   private static final String            ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_OPTIONS = "Options";                                       //$NON-NLS-1$
   private static final String            ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_TRIMP   = "TRIMP";                                         //$NON-NLS-1$

   private static final String            TAG_EQUIPMENT_BRAND                         = "Brand";                                         //$NON-NLS-1$

   private LinkedHashMap<String, Integer> _customDataFieldDefinitions;
   private ArrayList<Equipment>           _equipments;

   private boolean                        _isInCustomDataFieldDefinitions;

   private boolean                        _isInEquipment;
   private boolean                        _isInBrand;
   private boolean                        _isInModel;
   private boolean                        _isInDatePurchased;
   private boolean                        _isInExpectedLifeKilometers;
   private boolean                        _isInInUse;
   private boolean                        _isInNotes;
   private boolean                        _isInPurchaseLocation;
   private boolean                        _isInPurchasePrice;
   private boolean                        _isInType;
   private boolean                        _isInWeightKilograms;

   private StringBuilder                  _characters                                 = new StringBuilder();

   public FitLogEx_SAXHandler() {

      _customDataFieldDefinitions = new LinkedHashMap<>();
      _equipments = new ArrayList<>();
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInEquipment ||
            _isInBrand ||
            _isInModel ||
            _isInDatePurchased ||
            _isInExpectedLifeKilometers ||
            _isInInUse ||
            _isInNotes ||
            _isInPurchaseLocation ||
            _isInPurchasePrice ||
            _isInType ||
            _isInWeightKilograms) {

         _characters.append(chars, startIndex, length);
      }
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      if (_isInEquipment) {

         endElement_InEquipment(name);

      }

      if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS)) {

         _isInCustomDataFieldDefinitions = false;

      } else if (name.equals(TAG_ACTIVITY_EQUIPMENT)) {

         _isInEquipment = false;

      }
   }

   private void endElement_InEquipment(final String name) {

      final int numberOfEquipments = _equipments.size();
      if (numberOfEquipments == 0) {
         return;
      }

      final Equipment currentEquipment = _equipments.get(numberOfEquipments - 1);

      if (name.equals(TAG_EQUIPMENT_BRAND)) {

         _isInBrand = false;

         currentEquipment.Brand = _characters.toString();

      } else if (name.equals(TAG_EQUIPMENT_MODEL)) {

         _isInModel = false;
         currentEquipment.Model = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_DATE_PURCHASED)) {

         _isInDatePurchased = false;
         currentEquipment.DatePurchased = _characters.toString().substring(0, 10);

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_EXPECTED_LIFE_KILOMETERS)) {

         _isInExpectedLifeKilometers = false;
         currentEquipment.ExpectedLifeKilometers = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_IN_USE)) {

         _isInInUse = false;
         currentEquipment.InUse = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_NOTES)) {

         _isInNotes = false;
         currentEquipment.Notes = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_PURCHASE_LOCATION)) {

         _isInPurchaseLocation = false;
         currentEquipment.PurchaseLocation = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_PURCHASE_PRICE)) {

         _isInPurchasePrice = false;
         currentEquipment.PurchasePrice = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_TYPE)) {

         _isInType = false;
         currentEquipment.Type = _characters.toString();

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_WEIGHT_KILOGRAMS)) {

         _isInWeightKilograms = false;
         currentEquipment.WeightKilograms = _characters.toString();
      }
   }

   public Map<String, Integer> getCustomDataFieldDefinitions() {
      return _customDataFieldDefinitions;
   }

   public List<Equipment> getEquipments() {
      return _equipments;
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

      if (_isInCustomDataFieldDefinitions) {

         if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITION)) {

            final String customFieldName = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_NAME);

            final String customFieldOptions = attributes.getValue(ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_OPTIONS);

            /*
             * We parse and save the field format in order to be able to format the double values
             */
            if (StringUtils.hasContent(customFieldOptions)) {

               final String[] tokens = customFieldOptions.split("\\|"); //$NON-NLS-1$
               if (tokens.length < 2) {
                  return;
               }

               final int numberOfDecimals = Integer.parseInt(tokens[1]);

               _customDataFieldDefinitions.put(customFieldName, numberOfDecimals);

            } else if (customFieldName.equals(ATTRIB_CUSTOM_DATA_FIELD_DEFINITION_TRIMP)) {
               // We make an exception for the TRIMP custom data field as its definition doesn't specify any specific number of decimals :
               // <.... Name="TRIMP" GroupAggregation="Sum" Options="">
               // However, this is how a TRIMP value is being exported
               // <... . name="TRIMP" v="81.8000717163086" />
               _customDataFieldDefinitions.put(customFieldName, 0);
            }
         }
      } else if (name.equals(TAG_ACTIVITY_CUSTOM_DATA_FIELD_DEFINITIONS)) {

         _isInCustomDataFieldDefinitions = true;

      } else if (name.equals(TAG_ACTIVITY_EQUIPMENT)) {

         // It's very important to test if we are in an <Equipment>
         // element as there can be equipments within equipments as below:
         // <Equipment Id="1">
         //    <Equipment Id="2" partOf="1">
         //    </Equipment>
         // </Equipment>
         //
         _isInEquipment = true;

         final Equipment newEquipment = new Equipment();
         newEquipment.Id = attributes.getValue(FitLog_SAXHandler.ATTRIB_EQUIPMENT_ID);

         _equipments.add(newEquipment);

      } else if (_isInEquipment) {

         startElement_InEquipment(name);

      }
   }

   private void startElement_InEquipment(final String name) {

      boolean isData = true;

      if (name.equals(TAG_EQUIPMENT_BRAND)) {

         _isInBrand = true;

      } else if (name.equals(TAG_EQUIPMENT_MODEL)) {

         _isInModel = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_DATE_PURCHASED)) {

         _isInDatePurchased = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_EXPECTED_LIFE_KILOMETERS)) {

         _isInExpectedLifeKilometers = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_IN_USE)) {

         _isInInUse = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_NOTES)) {

         _isInNotes = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_PURCHASE_LOCATION)) {

         _isInPurchaseLocation = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_PURCHASE_PRICE)) {

         _isInPurchasePrice = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_TYPE)) {

         _isInType = true;

      } else if (name.equals(FitLog_SAXHandler.TAG_EQUIPMENT_WEIGHT_KILOGRAMS)) {

         _isInWeightKilograms = true;

      } else {
         isData = false;
      }

      if (isData) {

         // clear char buffer
         _characters.delete(0, _characters.length());
      }
   }
}
