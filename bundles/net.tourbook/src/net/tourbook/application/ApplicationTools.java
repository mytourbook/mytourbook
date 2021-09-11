/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ApplicationTools {

   private static final char    NL                                     = UI.NEW_LINE;
   private static final String  WORKBENCH_XMI                          = "workbench.xmi";                     //$NON-NLS-1$
   private static final String  WORKBENCH_XMI_BACKUP                   = "workbench-BACKUP.xmi";              //$NON-NLS-1$
   private static final String  WORKBENCH_XMI_ADJUSTED                 = "workbench-ADJUSTED.xmi";            //$NON-NLS-1$

   private static final String  TRUE                                   = "true";                              //$NON-NLS-1$

   private static final String  PART_ORG_ECLIPSE_UI_EDITORSS           = "org.eclipse.ui.editorss";           //$NON-NLS-1$
   private static final String  PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW = "org.eclipse.ui.internal.introview"; //$NON-NLS-1$

   private static final String  ATTR_CLOSEABLE                         = "closeable";                         //$NON-NLS-1$
   private static final String  ATTR_ELEMENT_ID                        = "elementId";                         //$NON-NLS-1$

   private static final boolean IS_DEBUGGING                           = false;

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

   /**
    * Fix "closable" attributes in workbench.xmi. Sometime this attribute disappear and a view
    * cannot be closed anymore with the mouse.
    *
    * @param workbenchFolderPath
    */
   static void fixClosableAttribute(final File workbenchFolderPath) {

      final StringBuilder sb = new StringBuilder();

      sb.append(String.format("%-30s Setting all views closeable=\"true\" in workbench.xmi" + NL, LocalDateTime.now())); //$NON-NLS-1$
      sb.append(String.format("%-30s workbenchFolderPath:    %s" + NL, LocalDateTime.now(), workbenchFolderPath)); //$NON-NLS-1$

      final File fileWorkbenchXMI = new File(workbenchFolderPath, WORKBENCH_XMI);
      final File fileWorkbenchXMI_Adjusted = new File(workbenchFolderPath, WORKBENCH_XMI_ADJUSTED);

      int numAdjustments = 0;

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

         sb.append(String.format("%-30s numViews:               %d" + NL, LocalDateTime.now(), numViews)); //$NON-NLS-1$

         // Updated the selected nodes
         for (int nodeIndex = 0; nodeIndex < numViews; nodeIndex++) {

            final Element domElement = (Element) allNodes.item(nodeIndex);

            final String attrCloseable = domElement.getAttribute(ATTR_CLOSEABLE);
            final String attrElementId = domElement.getAttribute(ATTR_ELEMENT_ID);

            if (PART_ORG_ECLIPSE_UI_EDITORSS.equals(attrElementId)
                  || PART_ORG_ECLIPSE_UI_INTERNAL_INTROVIEW.equals(attrElementId)) {

               // skip parts which do not have a closeable attribute

               if (IS_DEBUGGING) {
                  sb.append(String.format("%-30s Skipped view            %s" + NL, LocalDateTime.now(), attrElementId)); //$NON-NLS-1$
               }

               continue;
            }

            if (TRUE.equals(attrCloseable) == false) {

               domElement.setAttribute(ATTR_CLOSEABLE, TRUE);

               numAdjustments++;

               sb.append(String.format("%-30s Set closeable='true' in %s" + NL, LocalDateTime.now(), attrElementId)); //$NON-NLS-1$
            }
         }

         sb.append(String.format("%-30s closeable='true' is set in %d views" + NL, LocalDateTime.now(), numAdjustments)); //$NON-NLS-1$

         if (numAdjustments > 0) {

            /*
             * Replace original file with adjusted file
             */

            try (FileOutputStream intoFileOutputStream = new FileOutputStream(fileWorkbenchXMI_Adjusted);
                  OutputStreamWriter intoOutputStreamWriter = new OutputStreamWriter(intoFileOutputStream, UI.UTF8_CHARSET);
                  Writer intoCopyWriter = new BufferedWriter(intoOutputStreamWriter)) {

               // Write result into the output file
               final TransformerFactory transformFactory = TransformerFactory.newInstance();
               transformFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

               final Transformer xformer = transformFactory.newTransformer();
               xformer.transform(new DOMSource(domDocument), new StreamResult(intoCopyWriter));

            } catch (final Exception e) {

               StatusUtil.log(e);
            }

            // use a secon try/catch to close files before they are renamed

            try {

               // rename original workbench.xmi -> workbench-BACKUP.xmi
               final Path originalWorkbenchXMI = Paths.get(fileWorkbenchXMI.getAbsolutePath());
               Files.move(originalWorkbenchXMI, originalWorkbenchXMI.resolveSibling(WORKBENCH_XMI_BACKUP), StandardCopyOption.REPLACE_EXISTING);
               sb.append(String.format("%-30s Renamed workbench.xmi -> workbench-BACKUP.xmi" + NL, LocalDateTime.now())); //$NON-NLS-1$

               // rename workbench-ADJUSTED.xmi -> workbench.xmi
               final Path newWorkbenchXMI = Paths.get(fileWorkbenchXMI_Adjusted.getAbsolutePath());
               Files.move(newWorkbenchXMI, newWorkbenchXMI.resolveSibling(WORKBENCH_XMI), StandardCopyOption.REPLACE_EXISTING);
               sb.append(String.format("%-30s Replaced old workbench.xmi with new workbench.xmi" + NL, LocalDateTime.now())); //$NON-NLS-1$

            } catch (final IOException e) {

               StatusUtil.log(e);
            }
         }

      } catch (final Exception e) {

         StatusUtil.log(e);

      } finally {

         StatusUtil.log(sb.toString());
      }
   }

   public static void main(final String[] args) {

      final String workbenchFolderPath = "C:/DAT/runtime-net.mytourbook/workspace/.metadata/.plugins/org.eclipse.e4.workbench"; //$NON-NLS-1$

      ApplicationTools.fixClosableAttribute(new File(workbenchFolderPath));

   }

}
