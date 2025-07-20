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

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

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

   public static final String   WORKBENCH_XMI                          = "workbench.xmi";                     //$NON-NLS-1$

   private static final String  WORKBENCH_XMI_BACKUP                   = "workbench-BACKUP.xmi";              //$NON-NLS-1$
   private static final String  WORKBENCH_XMI_ADJUSTED                 = "workbench-ADJUSTED.xmi";            //$NON-NLS-1$
   private static final String  TRUE                                   = "true";                              //$NON-NLS-1$

   private static final String  PART_ORG_ECLIPSE_UI_EDITORSS           = "org.eclipse.ui.editorss";           //$NON-NLS-1$

   private static final String  PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW = "org.eclipse.ui.internal.introview"; //$NON-NLS-1$
   private static final String  ATTR_CLOSEABLE                         = "closeable";                         //$NON-NLS-1$

   private static final String  ATTR_ELEMENT_ID                        = "elementId";                         //$NON-NLS-1$
   private static final String  ATTR_ICON_URI                          = "iconURI";                           //$NON-NLS-1$

   private static StringBuilder _logger                                = new StringBuilder();
   private static StringBuilder _stateLogger_CloseButtons              = new StringBuilder();
   private static StringBuilder _stateLogger_IconImages                = new StringBuilder();

   private static boolean       _isApplyToFile;

   public static class FixState {

      public String stateText_CloseButton;
      public String stateText_IconImages;

      public int    numFixed_CloseButtons;
      public int    numFixed_IconImages;

      public FixState(int numFixed_CloseButtons,
                      String stateText_CloseButton,
                      int numFixed_IconImages,
                      String stateText_IconImages) {

         this.numFixed_CloseButtons = numFixed_CloseButtons;
         this.stateText_CloseButton = stateText_CloseButton;

         this.numFixed_IconImages = numFixed_IconImages;
         this.stateText_IconImages = stateText_IconImages;

      }
   }

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
    * @param isApplyToFile
    *           When <code>false</code> then the fixes are not applied to workbench.xmi only the
    *           logs are created
    * @param workbenchFolderPath
    * @param isFixViewCloseButton
    * @param isFixViewIconImage
    *
    * @return
    */
   public static FixState fixAllIssues(boolean isApplyToFile,
                                       File workbenchFolderPath,
                                       boolean isFixViewCloseButton,
                                       boolean isFixViewIconImage) {

      _isApplyToFile = isApplyToFile;

      resetLogger();

      int numFixed_CloseButtons = 0;
      int numFixed_IconImages = 0;

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

         if (isFixViewCloseButton) {

            numFixed_CloseButtons = fixViewCloseButtons(domDocument, xpath, workbenchFolderPath);
         }

         if (isFixViewIconImage) {

            numFixed_IconImages = fixViewIconImage(domDocument, xpath, workbenchFolderPath);
         }

         if (isApplyToFile && (numFixed_CloseButtons > 0 || numFixed_IconImages > 0)) {

            replaceXmiFile(domDocument, fileWorkbenchXMI, fileWorkbenchXMI_Adjusted);
         }

      } catch (final Exception e) {

         logException(e);

      } finally {

         logInfo(_logger.toString());
      }

      return new FixState(

            numFixed_CloseButtons,
            _stateLogger_CloseButtons.toString(),

            numFixed_IconImages,
            _stateLogger_IconImages.toString());
   }

   /**
    * Fix "closable" attributes in workbench.xmi. Sometimes this attribute disappear and a view
    * cannot be closed anymore with the mouse.
    *
    * @param workbenchFolderPath
    * @param isUpdateWorkbenchXmiFile
    *
    * @return Returns the state
    */
   private static int fixViewCloseButtons(final Document domDocument,
                                          final XPath xpath,
                                          File workbenchFolderPath) throws XPathExpressionException {

      String xPathExpression = "//children[@xsi:type='advanced:Placeholder']"; //$NON-NLS-1$

      logIssue_CloseButtons("fixViewCloseButtons()"); //$NON-NLS-1$
      logIssue_CloseButtons("Set all views closeable=\"true\" in workbench.xmi"); //$NON-NLS-1$
      logIssue_CloseButtons("workbenchFolderPath: %s".formatted(workbenchFolderPath)); //$NON-NLS-1$
      logIssue_CloseButtons("XPath %s".formatted(xPathExpression)); //$NON-NLS-1$

      final NodeList allNodes = (NodeList) xpath.evaluate(xPathExpression, domDocument, XPathConstants.NODESET);

      final int numViews = allNodes.getLength();

      logIssue_CloseButtons("Number of views: %d".formatted(numViews)); //$NON-NLS-1$

      int numAdjustments = 0;

      // Updated the selected nodes
      for (int nodeIndex = 0; nodeIndex < numViews; nodeIndex++) {

         final Element domElement = (Element) allNodes.item(nodeIndex);

         final String attrCloseable = domElement.getAttribute(ATTR_CLOSEABLE);
         final String attrElementId = domElement.getAttribute(ATTR_ELEMENT_ID);

         if (PART_ORG_ECLIPSE_UI_EDITORSS.equals(attrElementId)
               || PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW.equals(attrElementId)) {

            // skip parts which do not have a closeable attribute

            logIssue_CloseButtons("Skipped view\t%s".formatted(attrElementId)); //$NON-NLS-1$

            continue;
         }

         if (TRUE.equals(attrCloseable) == false) {

            domElement.setAttribute(ATTR_CLOSEABLE, TRUE);

            numAdjustments++;

            logIssue_CloseButtons("Set closeable='true' in %s".formatted(attrElementId)); //$NON-NLS-1$
         }
      }

      if (_isApplyToFile) {
         logIssue_CloseButtons("closeable='true' is set in %d views".formatted(numAdjustments)); //$NON-NLS-1$
      }

      return numAdjustments;
   }

   private static int fixViewIconImage(final Document domDocument,
                                       final XPath xpath,
                                       final File workbenchFolderFilePath)

         throws XPathExpressionException {

      logIssue_IconImage("fixViewIconImage()"); //$NON-NLS-1$
      logIssue_IconImage("Fixing iconURI in all views in workbench.xmi"); //$NON-NLS-1$
      logIssue_IconImage("workbenchFolderPath:    %s".formatted(workbenchFolderFilePath)); //$NON-NLS-1$

      List<Workbench_SharedElement> allSharedElements = getAllSharedElements(domDocument, xpath);
      Map<String, Workbench_Descriptor> allDescriptors = getAllDescriptors(domDocument, xpath);

      Collections.sort(
            allSharedElements,
            (element1, element2) -> element1.elementID.compareTo(element2.elementID));

      logIssue_IconImage(UI.EMPTY_STRING);
      logIssue_IconImage("Replacing icons"); //$NON-NLS-1$

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

            String logText = "Replaced in %-70s from %-70s with %-70s".formatted( //$NON-NLS-1$
                  sharedElementID,
                  sharedIconURI,
                  descriptorIconURI);

            // limit log items otherwise they are not displayed in a tooltip
            if (numAdjustments <= 6) {

               if (numAdjustments == 6) {
                  _stateLogger_IconImages.append("...\n"); //$NON-NLS-1$
               } else {
                  logIssue_IconImage(logText);
               }

            } else {
               logDetails(logText);
            }
         }
      }

      if (_isApplyToFile) {
         logIssue_IconImage("Fixed %d images ".formatted(numAdjustments)); //$NON-NLS-1$
      }

      return numAdjustments;
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

      String xPathExpression = "//descriptors"; //$NON-NLS-1$

      logIssue_IconImage(UI.EMPTY_STRING);
      logIssue_IconImage("XPath: %s".formatted(xPathExpression)); //$NON-NLS-1$

      NodeList allNodes = (NodeList) xpath.evaluate(xPathExpression, domDocument, XPathConstants.NODESET);

      Map<String, Workbench_Descriptor> allDescriptors = new HashMap<>();

      for (int nodeIndex = 0; nodeIndex < allNodes.getLength(); nodeIndex++) {

         final Element domElement = (Element) allNodes.item(nodeIndex);

         final String attrIconURI = domElement.getAttribute(ATTR_ICON_URI);
         final String attrElementID = domElement.getAttribute(ATTR_ELEMENT_ID);

         if (attrIconURI != null && attrIconURI.length() > 0) {

            logDetails("%-70s %s".formatted(attrElementID, attrIconURI)); //$NON-NLS-1$

            allDescriptors.put(attrElementID, new Workbench_Descriptor(attrIconURI));
         }
      }

      logIssue_IconImage("Descriptors: %d".formatted(allDescriptors.size())); //$NON-NLS-1$

      return allDescriptors;
   }

   private static List<Workbench_SharedElement> getAllSharedElements(final Document domDocument,
                                                                     final XPath xpath)
         throws XPathExpressionException {

      String xPathExpression = "//sharedElements"; //$NON-NLS-1$

      logIssue_IconImage(UI.EMPTY_STRING);
      logIssue_IconImage("XPath: %s".formatted(xPathExpression)); //$NON-NLS-1$

      NodeList allNodes = (NodeList) xpath.evaluate(xPathExpression, domDocument, XPathConstants.NODESET);

      List<Workbench_SharedElement> allSharedElements = new ArrayList<>();

      for (int nodeIndex = 0; nodeIndex < allNodes.getLength(); nodeIndex++) {

         final Element domElement = (Element) allNodes.item(nodeIndex);

         final String attrIconURI = domElement.getAttribute(ATTR_ICON_URI);
         final String attrElementID = domElement.getAttribute(ATTR_ELEMENT_ID);

         if (attrIconURI != null && attrIconURI.length() > 0) {

            logDetails("%-70s %s".formatted(attrElementID, attrIconURI)); //$NON-NLS-1$

            allSharedElements.add(new Workbench_SharedElement(domElement, attrElementID, attrIconURI));
         }
      }

      logIssue_IconImage("SharedElements: %d".formatted(allSharedElements.size())); //$NON-NLS-1$

      return allSharedElements;
   }

   private static void logAll(String logText) {

      _stateLogger_CloseButtons.append("%s\n".formatted(logText)); //$NON-NLS-1$
      _stateLogger_IconImages.append("%s\n".formatted(logText)); //$NON-NLS-1$

      _logger.append("%-30s %s\n".formatted(LocalDateTime.now(), logText)); //$NON-NLS-1$
   }

   private static void logDetails(String logText) {

      _logger.append("%-30s %s\n".formatted(LocalDateTime.now(), logText)); //$NON-NLS-1$
   }

   private static void logException(Exception logText) {

//      System.out.println(logText);

      StatusUtil.log(logText);
   }

   private static void logInfo(String logText) {

//      System.out.println(logText);

      StatusUtil.logInfo(logText);
   }

   private static void logIssue_CloseButtons(String logText) {

      _stateLogger_CloseButtons.append("%s\n".formatted(logText)); //$NON-NLS-1$

      _logger.append("%-30s %s\n".formatted(LocalDateTime.now(), logText)); //$NON-NLS-1$
   }

   private static void logIssue_IconImage(String logText) {

      _stateLogger_IconImages.append("%s\n".formatted(logText)); //$NON-NLS-1$

      _logger.append("%-30s %s\n".formatted(LocalDateTime.now(), logText)); //$NON-NLS-1$
   }

   public static void main(final String[] args) {

      final String workbenchFolderPath = "C:/DAT/runtime-net.mytourbook/workspace/.metadata/.plugins/org.eclipse.e4.workbench"; //$NON-NLS-1$
      final File workbenchFolderFilePath = new File(workbenchFolderPath);

      boolean isUpdateFile = false;

//      fixViewCloseButtons(workbenchFolderFilePath, isUpdateFile);
//      fixViewIcons(workbenchFolderFilePath, isUpdateFile);
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
            OutputStreamWriter intoOutputStreamWriter = new OutputStreamWriter(intoFileOutputStream, Charset.forName("UTF-8")); //$NON-NLS-1$
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
         logAll("Renamed workbench.xmi -> workbench-BACKUP.xmi"); //$NON-NLS-1$

         // rename workbench-ADJUSTED.xmi -> workbench.xmi
         final Path newWorkbenchXMI = Paths.get(fileWorkbenchXMI_Adjusted.getAbsolutePath());
         Files.move(newWorkbenchXMI, newWorkbenchXMI.resolveSibling(WORKBENCH_XMI), StandardCopyOption.REPLACE_EXISTING);
         logAll("Replaced old workbench.xmi with new workbench.xmi"); //$NON-NLS-1$

      } catch (final IOException e) {

         logException(e);
      }
   }

   private static void resetLogger() {

      _logger.setLength(0);

      _stateLogger_CloseButtons.setLength(0);
      _stateLogger_IconImages.setLength(0);
   }

}
