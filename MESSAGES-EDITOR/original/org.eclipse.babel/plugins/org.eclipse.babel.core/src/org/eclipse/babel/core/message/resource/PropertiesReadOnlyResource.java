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
package org.eclipse.babel.core.message.resource;

import java.util.Locale;

import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;


/**
 * Properties file, where the underlying storage is unknown and read-only.
 * This is the case when properties are located inside a jar or the target platform.
 * This resource is not suitable to build the editor itself.
 * It is used during the build only.
 * 
 * @author Pascal Essiembre
 * @author Hugues Malphettes
 * @see PropertiesFileResource
 */
public class PropertiesReadOnlyResource extends AbstractPropertiesResource{

    private final String contents;
    private final String resourceLocationLabel;
    
    /**
     * Constructor.
     * @param locale the resource locale
     * @param serializer resource serializer
     * @param deserializer resource deserializer
     * @param content The contents of the properties
     * @param resourceLocationLabel The label that explains to the user where
     * those properties are defined.
     */
    public PropertiesReadOnlyResource(
            Locale locale,
            PropertiesSerializer serializer,
            PropertiesDeserializer deserializer,
            String contents,
            String resourceLocationLabel) {
        super(locale, serializer, deserializer);
        this.contents = contents;
        this.resourceLocationLabel = resourceLocationLabel;
    }

    /**
     * @see org.eclipse.babel.core.message.resource.AbstractPropertiesResource
     * 			#getText()
     */
    public String getText() {
        return contents;
    }

    /**
     * Unsupported here. This is read-only.
     * @see org.eclipse.babel.core.message.resource.TextResource#setText(
     *              java.lang.String)
     */
    public void setText(String text) {
        throw new UnsupportedOperationException(getResourceLocationLabel()
        		+ " resource is read-only"); //$NON-NLS-1$ (just an error message)
    }
    
    /**
     * @see org.eclipse.babel.core.message.resource.IMessagesResource
     * 		#getSource()
     */
    public Object getSource() {
        return this;
    }    
    
    /**
     * @return The resource location label. or null if unknown.
     */
    public String getResourceLocationLabel() {
    	return resourceLocationLabel;
    }
        
    /**
     * Called before this object will be discarded.
     * Nothing to do: we were not listening to changes to this object.
     */
    public void dispose() {
    	//nothing to do.
    }
}
