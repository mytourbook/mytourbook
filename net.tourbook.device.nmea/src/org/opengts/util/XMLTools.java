// ----------------------------------------------------------------------------
// Copyright 2006-2009, GeoTelematic Solutions, Inc.
// All rights reserved
// ----------------------------------------------------------------------------
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ----------------------------------------------------------------------------
// Change History:
//  2006/02/19  Martin D. Flynn
//     -Initial release
//  2008/02/27  Martin D. Flynn
//     -Modified 'getNodeText' to include 'CDATA' sections
//     -Added 'getDocument(byt xml[])' methods
// ----------------------------------------------------------------------------
package org.opengts.util;

import java.io.*;
import java.util.*;
import java.awt.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class XMLTools
{

    // ------------------------------------------------------------------------

    public static class XMLErrorHandler
        implements ErrorHandler
    {
        public XMLErrorHandler() {
        }
        private void printError(String msg, SAXParseException spe) {
            int line = spe.getLineNumber();
            int col  = spe.getColumnNumber();
            System.out.println(msg + " [" + line + ":"+ col+ "] " + spe.getMessage());
        }
        public void error(SAXParseException spe) throws SAXException {
            printError("ERROR", spe);
        }
        public void fatalError(SAXParseException spe) throws SAXException {
            printError("FATAL", spe);
        }
        public void warning(SAXParseException spe) throws SAXException {
            printError("WARN ", spe);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* load XML document from file */
    public static Document getDocument(File xmlFile)
    {
        return XMLTools.getDocument(xmlFile, false);
    }

    /* load XML document from file */
    public static Document getDocument(File xmlFile, boolean checkErrors)
    {

        /* valid file? */
        if (xmlFile == null) {
            Print.logError("XML file is null!");
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (checkErrors) {
                dbf.setValidating(true);
                dbf.setIgnoringElementContentWhitespace(true);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (checkErrors) {
                db.setErrorHandler(new XMLErrorHandler());
            }
            doc = db.parse(xmlFile);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
        }

        /* return */
        return doc;

    }

    // ------------------------------------------------------------------------

    /* load XML document from input stream */
    public static Document getDocument(InputStream input)
    {
        return XMLTools.getDocument(input, false);
    }

    /* load XML document from input stream */
    public static Document getDocument(InputStream input, boolean checkErrors)
    {

        /* valid stream? */
        if (input == null) {
            Print.logError("XML stream is null!");
            return null;
        }

        /* create XML document */
        Document doc = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            if (checkErrors) {
                dbf.setValidating(true);
                dbf.setIgnoringElementContentWhitespace(true);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (checkErrors) {
                db.setErrorHandler(new XMLErrorHandler());
            }
            doc = db.parse(input);
        } catch (ParserConfigurationException pce) {
            Print.logError("Parse error: " + pce);
        } catch (SAXException se) {
            Print.logError("Parse error: " + se);
        } catch (IOException ioe) {
            Print.logError("IO error: " + ioe);
        }

        /* return */
        return doc;

    }

    // ------------------------------------------------------------------------

    /* load XML document from byte array */
    public static Document getDocument(byte xml[])
    {
        return XMLTools.getDocument(xml, false);
    }

    /* load XML document from byte array */
    public static Document getDocument(byte xml[], boolean checkErrors)
    {

        /* valid xml bytes? */
        if (xml == null) {
            Print.logError("XML data is null!");
            return null;
        }

        /* return */
        return XMLTools.getDocument(new ByteArrayInputStream(xml), checkErrors);

    }

    // ------------------------------------------------------------------------

    /* load XML document from String */
    public static Document getDocument(String xml)
    {
        return XMLTools.getDocument(StringTools.getBytes(xml), false);
    }

    /* load XML document from String */
    public static Document getDocument(String xml, boolean checkErrors)
    {
        return XMLTools.getDocument(StringTools.getBytes(xml), checkErrors);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /* parse text from node */
    public static String getNodeText(Node root)
    {
        return XMLTools.getNodeText(root, null, false);
    }

    /* parse text from node */
    public static String getNodeText(Node root, String repNewline)
    {
        return XMLTools.getNodeText(root, repNewline, false);
    }
    
    /* parse text from node */
    // does not return null
    public static String getNodeText(Node root, String repNewline, boolean resolveRT)
    {
        StringBuffer text = new StringBuffer();

        /* extract String */
        if (root != null) {
            NodeList list = root.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                Node n = list.item(i);
                if (n.getNodeType() == Node.CDATA_SECTION_NODE) { // CDATA Section
                    text.append(n.getNodeValue());
                } else
                if (n.getNodeType() == Node.TEXT_NODE) {
                    text.append(n.getNodeValue());
                } else {
                    //Print.logWarn("Unrecognized node type: " + n.getNodeType());
                }
            }
        }

        /* remove CR, and handle NL */
        if (repNewline != null) {
            // 'repNewline' contains text which is used to replace detected '\n' charaters
            StringBuffer sb = new StringBuffer();
            String s[] = StringTools.parseString(text.toString(),"\n\r");
            for (int i = 0; i < s.length; i++) {
                String line = s[i].trim();
                if (!line.equals("")) {
                    if (sb.length() > 0) {
                        sb.append(repNewline);
                    }
                    sb.append(line);
                }
            }
            text = sb;
        }
        
        /* return resulting text */
        if (resolveRT) {
            // resolve runtime property variables
            return RTConfig.insertKeyValues(text.toString());
        } else {
            // as-is
            return text.toString();
        }

    }

    /**
    *** Parse String into an array terminated by CR, NL, or CRNL
    *** @param text  The String text to parse
    *** @return An array of String lines
    **/
    public static String[] parseLines(String text)
    {
        return StringTools.parseString(text, "\r\n");
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    /**
    *** Returns the value for the specified attribute key, or null if the key is not
    *** defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @return The value of the key, or null if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key)
    {
        return XMLTools.getAttribute(elem, key, null, false);
    }

    /**
    *** Returns the value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default value to return if the key is not defined
    *** @return The value of the key, or the default value if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key, String dft)
    {
        return XMLTools.getAttribute(elem, key, dft, false);
    }

    /**
    *** Returns the value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The value of the key, or the default value if the key is not defined.
    **/
    public static String getAttribute(Element elem, String key, String dft, boolean resolveRT)
    {
        String rtn = dft;
        if ((elem != null) && !StringTools.isBlank(key)) {
            String val= elem.getAttribute(key);
            if (val != null) {
                rtn = val; // even if it's blank
            }
        }
        if (resolveRT) {
            // resolve runtime property variables
            return RTConfig.insertKeyValues(rtn);
        } else {
            // as-is
            return rtn;
        }
    }

    // ------------------------------------------------------------------------

    /**
    *** Returns the boolean value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default boolean value to return if the key is not defined
    *** @return The boolean value of the key, or the default value if the key is not defined.
    **/
    public static boolean getAttributeBoolean(Element elem, String key, boolean dft)
    {
        return StringTools.parseBoolean(XMLTools.getAttribute(elem,key,null,false),dft);
    }

    /**
    *** Returns the boolean value for the specified attribute key, or the default value if
    *** the key is not defined in the specified Element
    *** @param elem  The Element from which the key attribute will be returned
    *** @param key   The attribute key
    *** @param dft   The default boolean value to return if the key is not defined
    *** @param resolveRT If true, resolve any runtime config variables
    *** @return The boolean value of the key, or the default value if the key is not defined.
    **/
    public static boolean getAttributeBoolean(Element elem, String key, boolean dft, boolean resolveRT)
    {
        return StringTools.parseBoolean(XMLTools.getAttribute(elem,key,null,resolveRT),dft);
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static Element getChildElement(Node root, String name)
    {
        //print("Looking for " + name);
        NodeList list = root.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n    = list.item(i);
            //print(", checking " + n.getNodeName());
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if ((name == null) || n.getNodeName().equalsIgnoreCase(name)) {
                    //println(", found!");
                    return (Element)n;
                }
            }
        }
        //println(", not found");
        return null;
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static String getPathText(Element root, String nodes)
    {
        Element node = XMLTools.getPathElement(root, nodes);
        return (node != null)? XMLTools.getNodeText(node) : null;
    }

    public static Element getPathElement(Element root, String nodes)
    {
        return XMLTools.getPathElement(root, new StringTokenizer(nodes, "/"));
    }

    public static Element getPathElement(Element root, StringTokenizer nodes)
    {
        if (root == null) {
            return null;
        } else
        if (nodes == null) {
            return XMLTools.getChildElement(root, null);
        } else
        if (!nodes.hasMoreTokens()) {
            return root;
        } else {
            String nextName = nodes.nextToken();
            return XMLTools.getPathElement(XMLTools.getChildElement(root, nextName), nodes);
        }
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void printNodeTree(String indent, Node n)
    {
        Object objVal = n.getNodeValue();
        String strVal = (objVal != null)? objVal.toString().trim() : "null";
        Print.logInfo(indent + "Name: " + n.getNodeName() + " ['" + strVal + "']");
        NamedNodeMap attr = n.getAttributes();
        if (attr != null) {
            for (int i = 0; i < attr.getLength(); i++) {
                XMLTools.printNodeTree(indent + "   [A] ", attr.item(i));
            }
        }
        NodeList child = n.getChildNodes();
        if (child != null) {
            for (int i = 0; i < child.getLength(); i++) {
                XMLTools.printNodeTree(indent + "    ", child.item(i));
            }
        }
    }
   
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public static void main(String argv[])
    {

        /* start with blank line */
        System.out.println("");

        /* get file from command line */
        File xmlFile = (argv.length > 0)? new File(argv[0]) : null;
        System.out.println("Loading XML file: " + xmlFile);

        /* parse/validate XML */
        Document doc = XMLTools.getDocument(xmlFile, true);
        if (doc != null) {
            System.out.println("No fatal XML errors found");
        }

    }

}
