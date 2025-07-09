/*******************************************************************************
 * Copyright (C) 2021, 2025 Wolfgang Schramm and Contributors
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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Fix workbench.xmi issues, these are similar issues
 * <p>
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=430090
 * https://github.com/mytourbook/mytourbook/issues/511
 */
public class ApplicationTools {

   private static final String  WORKBENCH_XMI                          = "workbench.xmi";                     //$NON-NLS-1$
   private static final String  WORKBENCH_XMI_BACKUP                   = "workbench-BACKUP.xmi";              //$NON-NLS-1$
   private static final String  WORKBENCH_XMI_ADJUSTED                 = "workbench-ADJUSTED.xmi";            //$NON-NLS-1$

   private static final String  TRUE                                   = "true";                              //$NON-NLS-1$

   private static final String  PART_ORG_ECLIPSE_UI_EDITORSS           = "org.eclipse.ui.editorss";           //$NON-NLS-1$
   private static final String  PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW = "org.eclipse.ui.internal.introview"; //$NON-NLS-1$

   private static final String  ATTR_CLOSEABLE                         = "closeable";                         //$NON-NLS-1$
   private static final String  ATTR_ELEMENT_ID                        = "elementId";                         //$NON-NLS-1$
   private static final String  ATTR_ICON_URI                          = "iconURI";                           //$NON-NLS-1$

   private static final boolean IS_DEBUGGING                           = true;

   private static StringBuilder _logger                                = new StringBuilder();

   /**
    * Source: https://howtodoinjava.com/java/xml/xpath-namespace-resolution-example/
    */
   private static class NamespaceResolver implements NamespaceContext {

      // Store the source document to search the namespaces
      private Document sourceDocument;

      public NamespaceResolver(final Document document) {

         sourceDocument = document;
      }

      // The lookup for the namespace uris is delegated to the stored document
      @Override
      public String getNamespaceURI(final String prefix) {

         if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {

            return sourceDocument.lookupNamespaceURI(null);

         } else {

            return sourceDocument.lookupNamespaceURI(prefix);
         }
      }

      @Override
      public String getPrefix(final String namespaceURI) {

         return sourceDocument.lookupPrefix(namespaceURI);
      }

      @Override
      @SuppressWarnings({ "rawtypes", "unchecked" })
      public Iterator getPrefixes(final String namespaceURI) {
         return null;
      }
   }

   private static class Workbench_Descriptor {

      String iconURI;

      public Workbench_Descriptor(String attrIconURI) {

         iconURI = attrIconURI;
      }
   }

   private static class Workbench_SharedElement {

      Element domElement;

      String  elementID;
      String  iconURI;

      public Workbench_SharedElement(Element domElement, String attrElementID, String attrIconURI) {

         this.domElement = domElement;

         elementID = attrElementID;
         iconURI = attrIconURI;
      }
   }

   /**
    * Fix "closable" attributes in workbench.xmi. Sometimes this attribute disappear and a view
    * cannot be closed anymore with the mouse.
    *
    * @param workbenchFolderPath
    */
   static void fixClosableAttribute(final File workbenchFolderPath) {

      log("fixClosableAttribute()"); //$NON-NLS-1$
      log("Setting all views closeable=\"true\" in workbench.xmi"); //$NON-NLS-1$
      log("workbenchFolderPath:    %s".formatted(workbenchFolderPath)); //$NON-NLS-1$

      final File fileWorkbenchXMI = new File(workbenchFolderPath, WORKBENCH_XMI);
      final File fileWorkbenchXMI_Adjusted = new File(workbenchFolderPath, WORKBENCH_XMI_ADJUSTED);

      try (FileInputStream fromInputStream = new FileInputStream(fileWorkbenchXMI)) {

         // Load the document
         final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
         domFactory.setNamespaceAware(true);
         final Document domDocument = domFactory.newDocumentBuilder().parse(fromInputStream);

         // Select the node(s) with XPath
         final XPath xpath = XPathFactory.newInstance().newXPath();

         // MUST set a name space context otherwise xsi:type is ignored !!!
         final NamespaceResolver nsContext = new NamespaceResolver(domDocument);
         xpath.setNamespaceContext(nsContext);

         final NodeList allNodes = (NodeList) xpath.evaluate(
               "//children[@xsi:type='advanced:Placeholder']", //$NON-NLS-1$
               domDocument,
               XPathConstants.NODESET);

         final int numViews = allNodes.getLength();

         log("numViews: %d".formatted(numViews)); //$NON-NLS-1$

         int numAdjustments = 0;

         // Updated the selected nodes
         for (int nodeIndex = 0; nodeIndex < numViews; nodeIndex++) {

            final Element domElement = (Element) allNodes.item(nodeIndex);

            final String attrCloseable = domElement.getAttribute(ATTR_CLOSEABLE);
            final String attrElementId = domElement.getAttribute(ATTR_ELEMENT_ID);

            if (PART_ORG_ECLIPSE_UI_EDITORSS.equals(attrElementId)
                  || PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW.equals(attrElementId)) {

               // skip parts which do not have a closeable attribute

               if (IS_DEBUGGING) {
                  log("Skipped view            %s".formatted(attrElementId)); //$NON-NLS-1$
               }

               continue;
            }

            if (TRUE.equals(attrCloseable) == false) {

               domElement.setAttribute(ATTR_CLOSEABLE, TRUE);

               numAdjustments++;

               log("closeable='true' in %s".formatted(attrElementId)); //$NON-NLS-1$
            }
         }

         log("closeable='true' is set in %d views".formatted(numAdjustments)); //$NON-NLS-1$

         if (numAdjustments > 0) {

            replaceXmiFile(domDocument, fileWorkbenchXMI, fileWorkbenchXMI_Adjusted);
         }

      } catch (final Exception e) {

         logException(e);

      } finally {

         logInfo(_logger.toString());
      }
   }

   static void fixIconURI(File workbenchFolderFilePath) {

      log("fixIconURI()"); //$NON-NLS-1$
      log("Fixing iconURI in all views in workbench.xmi"); //$NON-NLS-1$
      log("workbenchFolderPath:    %s".formatted(workbenchFolderFilePath)); //$NON-NLS-1$

      final File fileWorkbenchXMI = new File(workbenchFolderFilePath, WORKBENCH_XMI);
      final File fileWorkbenchXMI_Adjusted = new File(workbenchFolderFilePath, WORKBENCH_XMI_ADJUSTED);

      try (FileInputStream fromInputStream = new FileInputStream(fileWorkbenchXMI)) {

         // Load the document
         final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
         domFactory.setNamespaceAware(true);
         final Document domDocument = domFactory.newDocumentBuilder().parse(fromInputStream);

         // Select the node(s) with XPath
         final XPath xpath = XPathFactory.newInstance().newXPath();

         // MUST set a name space context otherwise xsi:type is ignored !!!
         final NamespaceResolver nsContext = new NamespaceResolver(domDocument);
         xpath.setNamespaceContext(nsContext);

         List<Workbench_SharedElement> allSharedElements = getAllSharedElements(domDocument, xpath);
         Map<String, Workbench_Descriptor> allDescriptors = getAllDescriptors(domDocument, xpath);

         Collections.sort(
               allSharedElements,
               (element1, element2) -> element1.elementID.compareTo(element2.elementID));

         int numAdjustments = 0;

         for (Workbench_SharedElement sharedElement : allSharedElements) {

            String sharedElementID = sharedElement.elementID;
            Workbench_Descriptor descriptor = allDescriptors.get(sharedElementID);

            if (descriptor == null) {
               continue;
            }

            String sharedIconURI = sharedElement.iconURI;
            String descriptorIconURI = descriptor.iconURI;

            if (sharedIconURI.equals(descriptorIconURI) == false) {

               // update shared element

               sharedElement.domElement.setAttribute(ATTR_ICON_URI, descriptorIconURI);

               numAdjustments++;

               log("Replaced in %-70s from %-70s with %-70s".formatted( //$NON-NLS-1$
                     sharedElementID,
                     sharedIconURI,
                     descriptorIconURI));
            }
         }

         log("Fixed %d images ".formatted(numAdjustments)); //$NON-NLS-1$

         if (numAdjustments > 0) {

            replaceXmiFile(domDocument, fileWorkbenchXMI, fileWorkbenchXMI_Adjusted);
         }

      } catch (final Exception e) {

         logException(e);

      } finally {

         logInfo(_logger.toString());
      }
   }

   /**
    * @param domDocument
    * @param xpath
    *
    * @return
    *
    * @throws XPathExpressionException
    */
   private static Map<String, Workbench_Descriptor> getAllDescriptors(final Document domDocument,
                                                                      final XPath xpath)
         throws XPathExpressionException {

      String xPathExpression = "//descriptors";

      log("");
      log("XPath: %s".formatted(xPathExpression));

      NodeList allNodes = (NodeList) xpath.evaluate(xPathExpression, domDocument, XPathConstants.NODESET);

      Map<String, Workbench_Descriptor> allDescriptors = new HashMap<>();

      for (int nodeIndex = 0; nodeIndex < allNodes.getLength(); nodeIndex++) {

         final Element domElement = (Element) allNodes.item(nodeIndex);

         final String attrIconURI = domElement.getAttribute(ATTR_ICON_URI);
         final String attrElementID = domElement.getAttribute(ATTR_ELEMENT_ID);

         if (attrIconURI != null && attrIconURI.length() > 0) {

            log("%-70s %s".formatted(attrElementID, attrIconURI)); //$NON-NLS-1$

            allDescriptors.put(attrElementID, new Workbench_Descriptor(attrIconURI));
         }
      }

      log("Descriptors: %d".formatted(allDescriptors.size())); //$NON-NLS-1$

      return allDescriptors;
   }

   private static List<Workbench_SharedElement> getAllSharedElements(final Document domDocument,
                                                                     final XPath xpath)
         throws XPathExpressionException {

      String xPathExpression = "//sharedElements";

      log("");
      log("XPath: %s".formatted(xPathExpression));

      NodeList allNodes = (NodeList) xpath.evaluate(xPathExpression, domDocument, XPathConstants.NODESET);

      List<Workbench_SharedElement> allSharedElements = new ArrayList<>();

      for (int nodeIndex = 0; nodeIndex < allNodes.getLength(); nodeIndex++) {

         final Element domElement = (Element) allNodes.item(nodeIndex);

         final String attrIconURI = domElement.getAttribute(ATTR_ICON_URI);
         final String attrElementID = domElement.getAttribute(ATTR_ELEMENT_ID);

         if (attrIconURI != null && attrIconURI.length() > 0) {

            log("%-70s %s".formatted(attrElementID, attrIconURI)); //$NON-NLS-1$

            allSharedElements.add(new Workbench_SharedElement(domElement, attrElementID, attrIconURI));
         }
      }

      log("SharedElements: %d".formatted(allSharedElements.size())); //$NON-NLS-1$

      return allSharedElements;
   }

   private static void log(String logText) {

      _logger.append("%-30s %s\n".formatted(LocalDateTime.now(), logText));
   }

   private static void logException(Exception logText) {

      System.out.println(logText);

//      StatusUtil.log(logText);
   }

   private static void logInfo(String logText) {

      System.out.println(logText);

//      StatusUtil.logInfo(logText);
   }

   public static void main(final String[] args) {

      final String workbenchFolderPath = "C:/DAT/runtime-net.mytourbook/workspace/.metadata/.plugins/org.eclipse.e4.workbench"; //$NON-NLS-1$
      final File workbenchFolderFilePath = new File(workbenchFolderPath);

//      ApplicationTools.fixClosableAttribute(workbenchFolderFilePath);

      ApplicationTools.fixIconURI(workbenchFolderFilePath);

   }

   /**
    * Replace original file with adjusted file
    *
    * @param domDocument
    * @param fileWorkbenchXMI
    * @param fileWorkbenchXMI_Adjusted
    *
    * @throws TransformerFactoryConfigurationError
    */
   private static void replaceXmiFile(final Document domDocument,
                                      final File fileWorkbenchXMI,
                                      final File fileWorkbenchXMI_Adjusted) throws TransformerFactoryConfigurationError {

      try (FileOutputStream intoFileOutputStream = new FileOutputStream(fileWorkbenchXMI_Adjusted);
            OutputStreamWriter intoOutputStreamWriter = new OutputStreamWriter(intoFileOutputStream, Charset.forName("UTF-8"));
            Writer intoCopyWriter = new BufferedWriter(intoOutputStreamWriter)) {

         // Write result into the output file
         final TransformerFactory transformFactory = TransformerFactory.newInstance();
         transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

         final Transformer xformer = transformFactory.newTransformer();
         xformer.transform(new DOMSource(domDocument), new StreamResult(intoCopyWriter));

      } catch (final Exception e) {

         logException(e);
      }

      // use a second try/catch to close files before they are renamed

      try {

         // rename original workbench.xmi -> workbench-BACKUP.xmi
         final Path originalWorkbenchXMI = Paths.get(fileWorkbenchXMI.getAbsolutePath());
         Files.move(originalWorkbenchXMI, originalWorkbenchXMI.resolveSibling(WORKBENCH_XMI_BACKUP), StandardCopyOption.REPLACE_EXISTING);
         log("Renamed workbench.xmi -> workbench-BACKUP.xmi"); //$NON-NLS-1$

         // rename workbench-ADJUSTED.xmi -> workbench.xmi
         final Path newWorkbenchXMI = Paths.get(fileWorkbenchXMI_Adjusted.getAbsolutePath());
         Files.move(newWorkbenchXMI, newWorkbenchXMI.resolveSibling(WORKBENCH_XMI), StandardCopyOption.REPLACE_EXISTING);
         log("Replaced old workbench.xmi with new workbench.xmi"); //$NON-NLS-1$

      } catch (final IOException e) {

         logException(e);
      }
   }

}
