/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.common.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.tourbook.common.UI;

import org.xml.sax.SAXException;

public class XmlUtils {

   public static SAXParser initializeParser() {

      try {

         final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, UI.EMPTY_STRING);
         parser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, UI.EMPTY_STRING);
         return parser;

      } catch (final ParserConfigurationException | SAXException e) {
         e.printStackTrace();
      }

      return null;
   }

}
