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
package net.tourbook.application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.preferences.PrefPageGeneral;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

   @Override
   public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(final IWorkbenchWindowConfigurer configurer) {
      return new ApplicationWorkbenchWindowAdvisor(this, configurer);
   }

   /**
    * Copied from org.eclipse.e4.ui.internal.workbench.ResourceHandler
    *
    * @return
    */
   private File getBaseLocation() {

      File baseLocation;
      try {
         baseLocation = new File(URIUtil.toURI(Platform.getInstanceLocation().getURL()));
      } catch (final URISyntaxException e) {
         throw new RuntimeException(e);
      }

      baseLocation = new File(baseLocation, ".metadata"); //$NON-NLS-1$
      baseLocation = new File(baseLocation, ".plugins"); //$NON-NLS-1$

      return new File(baseLocation, "org.eclipse.e4.workbench"); //$NON-NLS-1$
   }

   @Override
   public String getInitialWindowPerspectiveId() {

      // set default perspective
      return PerspectiveFactoryTourBook.PERSPECTIVE_ID;
   }

   @Override
   public String getMainPreferencePageId() {

      // set default pref page
      return PrefPageGeneral.ID;
   }

   @Override
   public void initialize(final IWorkbenchConfigurer configurer) {

      configurer.setSaveAndRestore(true);
   }

   @Override
   public void postShutdown() {

      final File fileWorkbenchXMI = new File(getBaseLocation(), "workbench.xmi");
      final File fileWorkbenchXMI_Copy = new File(getBaseLocation(), "workbench-copy.xmi");

      try (FileInputStream fromInputStream = new FileInputStream(fileWorkbenchXMI);

            FileOutputStream intoFileOutputStream = new FileOutputStream(fileWorkbenchXMI_Copy);
            OutputStreamWriter intoOutputStreamWriter = new OutputStreamWriter(intoFileOutputStream, UI.UTF_8);
            Writer intoCopyWriter = new BufferedWriter(intoOutputStreamWriter)) {

         // Load the document
         final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
         domFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
         domFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
         final Document domDocument = domFactory.newDocumentBuilder().parse(fromInputStream);

         // Select the node(s) with XPath
         final XPath xpath = XPathFactory.newInstance().newXPath();
         final NodeList nodes = (NodeList) xpath.evaluate(
               "//children[@xsi:type='advanced:Placeholder']",
               domDocument,
               XPathConstants.NODESET);

         final int numNodes = nodes.getLength();

         System.out.println((System.currentTimeMillis() + " numNodes:" + numNodes));
         // TODO remove SYSTEM.OUT.PRINTLN

         // Updated the selected nodes
         for (int nodeIndex = 0; nodeIndex < numNodes; nodeIndex++) {

            System.out.println((System.currentTimeMillis() + " " + nodes.item(nodeIndex)));
            // TODO remove SYSTEM.OUT.PRINTLN

            final Element value = (Element) nodes.item(nodeIndex);
            value.setAttribute("closable", "true");
         }

         // Get the result as a String
         final TransformerFactory transformFactory = TransformerFactory.newInstance();
         transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

         final Transformer xformer = transformFactory.newTransformer();
         xformer.transform(new DOMSource(domDocument), new StreamResult(intoCopyWriter));

      } catch (ParserConfigurationException
            | SAXException
            | IOException
            | XPathExpressionException
            | TransformerException e) {

         StatusUtil.log(e);
      }
   }

   @Override
   public boolean preShutdown() {

      return TourManager.getInstance().saveTours();
   }
}
