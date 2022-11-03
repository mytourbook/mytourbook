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

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;

import org.xml.sax.SAXException;

public class XmlUtils {

   public static XMLInputFactory initializeFactory() {

      try {

         final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
         // Address the following security issue: https://rules.sonarsource.com/java/RSPEC-2755
         inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
         return inputFactory;

      } catch (final FactoryConfigurationError e) {
         StatusUtil.log(e);
      }

      return null;
   }

   public static SAXParser initializeParser() {

      try {

         final SAXParserFactory factory = SAXParserFactory.newInstance();
         // Address the following security issue: https://rules.sonarsource.com/java/RSPEC-2755
         factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
         return factory.newSAXParser();

      } catch (final ParserConfigurationException | SAXException e) {
         StatusUtil.log(e);
      }

      return null;
   }
}
