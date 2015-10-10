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
package org.eclipse.babel.core.message.strategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.eclipse.babel.core.message.MessageException;
import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.resource.PropertiesFileResource;
import org.eclipse.babel.core.message.resource.ser.IPropertiesDeserializerConfig;
import org.eclipse.babel.core.message.resource.ser.IPropertiesSerializerConfig;
import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;
import org.eclipse.babel.core.util.BabelUtils;


/**
 * MessagesBundleGroup strategy for standard Java properties file structure.
 * That is, all *.properties files of the same base name within the same
 * directory.  This implementation works on files outside the Eclipse
 * workspace.
 * @author Pascal Essiembre
 */
public class PropertiesFileGroupStrategy implements IMessagesBundleGroupStrategy {

    /** Empty bundle array. */
    private static final MessagesBundle[] EMPTY_MESSAGES =
    		new MessagesBundle[] {};
    
    /** File being open, triggering the creation of a bundle group. */
    private File file;
    /** MessagesBundle group base name. */
    private final String baseName;
    /** File extension. */
    private final String fileExtension;
    /** Pattern used to match files in this strategy. */
    private final String fileMatchPattern;
    /** Properties file serializer configuration. */
    private final IPropertiesSerializerConfig serializerConfig;
    /** Properties file deserializer configuration. */
    private final IPropertiesDeserializerConfig deserializerConfig;
    
    
    /**
     * Constructor.
     * @param file file from which to derive the group
     */
    public PropertiesFileGroupStrategy(
            File file,
            IPropertiesSerializerConfig serializerConfig,
            IPropertiesDeserializerConfig deserializerConfig) {
        super();
        this.serializerConfig = serializerConfig;
        this.deserializerConfig = deserializerConfig;
        this.file = file;
        this.fileExtension = file.getName().replaceFirst(
                "(.*\\.)(.*)", "$2"); //$NON-NLS-1$ //$NON-NLS-2$

        String patternCore =
                "((_[a-z]{2,3})|(_[a-z]{2,3}_[A-Z]{2})" //$NON-NLS-1$
              + "|(_[a-z]{2,3}_[A-Z]{2}_\\w*))?(\\." //$NON-NLS-1$
              + fileExtension + ")$"; //$NON-NLS-1$

        // Compute and cache name
        String namePattern = "^(.*?)" + patternCore; //$NON-NLS-1$
        this.baseName = file.getName().replaceFirst(
                namePattern, "$1"); //$NON-NLS-1$
        
        // File matching pattern
        this.fileMatchPattern =
                "^(" + baseName + ")" + patternCore;  //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * @see org.eclipse.babel.core.bundle.IMessagesBundleGroupStrategy
     * 			#getMessagesBundleGroupName()
     */
    public String createMessagesBundleGroupName() {
        return baseName + "[...]." + fileExtension; //$NON-NLS-1$
    }

    /**
     * @see org.eclipse.babel.core.bundle.IMessagesBundleGroupStrategy
     *          #loadMessagesBundles()
     */
    public MessagesBundle[] loadMessagesBundles() throws MessageException {
        File[] resources = null;
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            resources = parentDir.listFiles();
        }
        Collection<MessagesBundle> bundles = new ArrayList<MessagesBundle>();
        if (resources != null) {
            for (int i = 0; i < resources.length; i++) {
                File resource = resources[i];
                String resourceName = resource.getName();
                if (resource.isFile()
                        && resourceName.matches(fileMatchPattern)) {
                    // Build local title
                    String localeText = resourceName.replaceFirst(
                            fileMatchPattern, "$2"); //$NON-NLS-1$
                    Locale locale = BabelUtils.parseLocale(localeText);
                    bundles.add(createBundle(locale, resource));
                }
            }
        }
        return bundles.toArray(EMPTY_MESSAGES);
    }

    /**
     * @see org.eclipse.babel.core.bundle.IBundleGroupStrategy
     *          #createBundle(java.util.Locale)
     */
    public MessagesBundle createMessagesBundle(Locale locale) {
        // TODO Implement me (code exists in SourceForge version)
        return null;
    }
    
    /**
     * Creates a resource bundle for an existing resource.
     * @param locale locale for which to create a bundle
     * @param resource resource used to create bundle
     * @return an initialized bundle
     */
    protected MessagesBundle createBundle(Locale locale, File resource)
            throws MessageException {
        try {
            //TODO have the text de/serializer tied to Eclipse preferences,
            //singleton per project, and listening for changes
            return new MessagesBundle(new PropertiesFileResource(
                    locale,
                    new PropertiesSerializer(serializerConfig),
                    new PropertiesDeserializer(deserializerConfig),
                    resource));
        } catch (FileNotFoundException e) {
            throw new MessageException(
                    "Cannot create bundle for locale " //$NON-NLS-1$
                  + locale + " and resource " + resource, e); //$NON-NLS-1$
        }
    }
}
