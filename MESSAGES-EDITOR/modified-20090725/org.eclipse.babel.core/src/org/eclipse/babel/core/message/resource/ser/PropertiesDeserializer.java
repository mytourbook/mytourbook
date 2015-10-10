/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.core.message.resource.ser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Properties;

import org.eclipse.babel.core.message.Message;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.util.BabelUtils;

/**
 * Class responsible for deserializing {@link Properties}-like text into
 * a {@link MessagesBundle}.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class PropertiesDeserializer {

    /** System line separator. */
    private static final String SYSTEM_LINE_SEPARATOR = 
            System.getProperty("line.separator"); //$NON-NLS-1$
    
    /** Characters accepted as key value separators. */
    private static final String KEY_VALUE_SEPARATORS = "=:"; //$NON-NLS-1$

    /** MessagesBundle deserializer configuration. */
    private IPropertiesDeserializerConfig config;
    
    /**
     * Constructor.
     */
    public PropertiesDeserializer(IPropertiesDeserializerConfig config) {
        super();
        this.config = config;
    }

    /**
     * Parses a string and populates a <code>MessagesBundle</code>.
     * The string is expected to match the documented structure of a properties
     * file.
     * @param messagesBundle the target {@link MessagesBundle}
     * @param properties the string containing the properties to parse
     */
    public void deserialize(MessagesBundle messagesBundle, String properties) {
        Locale locale = messagesBundle.getLocale();
        Collection<String> oldKeys =
        		new ArrayList<String>(Arrays.asList(messagesBundle.getKeys()));
        Collection<String> newKeys = new ArrayList<String>();
        
        String[] lines = properties.split("\r\n|\r|\n"); //$NON-NLS-1$
        
        boolean doneWithFileComment = false;
        StringBuffer fileComment = new StringBuffer();
        StringBuffer lineComment = new StringBuffer();
        StringBuffer lineBuf = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            lineBuf.setLength(0);
            lineBuf.append(line);
        
            int equalPosition = findKeyValueSeparator(line);
            boolean isRegularLine = line.matches("^[^#].*"); //$NON-NLS-1$
            boolean isCommentedLine = doneWithFileComment 
                    && line.matches("^##[^#].*"); //$NON-NLS-1$
            
            // parse regular and commented lines
            if (equalPosition >= 1 && (isRegularLine || isCommentedLine)) {
                doneWithFileComment = true;
                String comment = ""; //$NON-NLS-1$
                if (lineComment.length() > 0) {
                    comment = lineComment.toString();
                    lineComment.setLength(0);
                }

                if (isCommentedLine) {
                    lineBuf.delete(0, 2); // remove ##
                    equalPosition -= 2;
                }
                String backslash = "\\"; //$NON-NLS-1$
                while (lineBuf.lastIndexOf(backslash) == lineBuf.length() -1) {
                    int lineBreakPosition = lineBuf.lastIndexOf(backslash);
                    lineBuf.replace(
                            lineBreakPosition,
                            lineBreakPosition + 1, ""); //$NON-NLS-1$
                    if (++i < lines.length) {
                        String wrappedLine = lines[i].replaceFirst(
                                "^\\s*", ""); //$NON-NLS-1$ //$NON-NLS-2$
                        if (isCommentedLine) {
                            lineBuf.append(wrappedLine.replaceFirst(
                                    "^##", "")); //$NON-NLS-1$ //$NON-NLS-2$
                        } else {
                            lineBuf.append(wrappedLine);
                        }
                    }
                }
                String key = lineBuf.substring(0, equalPosition).trim();
                key = unescapeKey(key);
                
                String value = lineBuf.substring(equalPosition + 1)
                        .replaceFirst("^\\s*", "");  //$NON-NLS-1$//$NON-NLS-2$
                // Unescape leading spaces
                if (value.startsWith("\\ ")) { //$NON-NLS-1$
                    value = value.substring(1);
                }
                
                if (config.isUnicodeUnescapeEnabled()) {
                    key = convertEncodedToUnicode(key);
                    value = convertEncodedToUnicode(value);
                } else {
                    value = value.replaceAll(
                            "\\\\r", "\r"); //$NON-NLS-1$ //$NON-NLS-2$
                    value = value.replaceAll(
                            "\\\\n", "\n");  //$NON-NLS-1$//$NON-NLS-2$
                }
                Message entry = messagesBundle.getMessage(key);
                if (entry == null) {
                    entry = new Message(key, locale);
                    messagesBundle.addMessage(entry);
                }
                entry.setActive(!isCommentedLine);
                entry.setComment(comment);
                entry.setText(value);
                newKeys.add(key);
            // parse comment line
            } else if (lineBuf.indexOf("#") == 0) { //$NON-NLS-1$
                if (!doneWithFileComment) {
                    fileComment.append(lineBuf);
                    fileComment.append(SYSTEM_LINE_SEPARATOR);
                } else {
                    lineComment.append(lineBuf);
                    lineComment.append(SYSTEM_LINE_SEPARATOR);
                }
            // handle blank or unsupported line
            } else {
                doneWithFileComment = true;
            }
        }
        oldKeys.removeAll(newKeys);
        messagesBundle.removeMessages(
        		oldKeys.toArray(BabelUtils.EMPTY_STRINGS)); 
        messagesBundle.setComment(fileComment.toString());
    }
    
    
    /**
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     * @param str the string to convert
     * @return converted string
     * @see java.util.Properties
     */
    private String convertEncodedToUnicode(String str) {
        char aChar;
        int len = str.length();
        StringBuffer outBuffer = new StringBuffer(len);

        for (int x = 0; x < len;) {
            aChar = str.charAt(x++);
            if (aChar == '\\' && x + 1 <= len) {
                aChar = str.charAt(x++);
                if (aChar == 'u' && x + 4 <= len) {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = str.charAt(x++);
                        switch (aChar) {
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            value = (value << 4) + aChar - '0';
                            break;
                        case 'a': case 'b': case 'c':
                        case 'd': case 'e': case 'f':
                            value = (value << 4) + 10 + aChar - 'a';
                            break;
                        case 'A': case 'B': case 'C':
                        case 'D': case 'E': case 'F':
                            value = (value << 4) + 10 + aChar - 'A';
                            break;
                        default:
                            value = aChar;
                            System.err.println(
                                    "PropertiesDeserializer: " //$NON-NLS-1$
                                  + "bad character " //$NON-NLS-1$
                                  + "encoding for string:" //$NON-NLS-1$
                                  + str);
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't') {
                        aChar = '\t';
                    } else if (aChar == 'r') {
                        aChar = '\r';
                    } else if (aChar == 'n') {
                        aChar = '\n';
                    } else if (aChar == 'f') {
                        aChar = '\f';
                    } else if (aChar == 'u') {
                        outBuffer.append("\\"); //$NON-NLS-1$
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }
    
    /**
     * Finds the separator symbol that separates keys and values.
     * @param str the string on which to find seperator
     * @return the separator index or -1 if no separator was found
     */
    private int findKeyValueSeparator(String str) {
        int index = -1;
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char currentChar = str.charAt(i);
            if (currentChar == '\\') {
                i++;
            } else if (KEY_VALUE_SEPARATORS.indexOf(currentChar) != -1) {
                index = i;
                break;
            }
        }
        return index;
    }
    
    private String unescapeKey(String key) {
        int length = key.length();
        StringBuffer buf = new StringBuffer();
        for (int index = 0; index < length; index++) {
            char currentChar = key.charAt(index);
            if (currentChar != '\\') {
                buf.append(currentChar);
            }
        }
        return buf.toString();
    }
}
