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
import java.util.Properties;

import org.eclipse.babel.core.message.MessagesBundle;
import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;


/**
 * Based implementation of a text-based messages resource following
 * the conventions defined by the Java {@link Properties} class for
 * serialization and deserialization.
 * @author Pascal Essiembre
 */
public abstract class AbstractPropertiesResource
		extends AbstractMessagesResource {

    private PropertiesDeserializer deserializer;
    private PropertiesSerializer serializer;
    
    /**
     * Constructor.
     * @param locale properties locale
     * @param serializer properties serializer
     * @param deserializer properties deserializer
     */
    public AbstractPropertiesResource(
            Locale locale,
            PropertiesSerializer serializer,
            PropertiesDeserializer deserializer) {
        super(locale);
        this.deserializer = deserializer;
        this.serializer = serializer;
        //TODO initialises with configurations only... 
    }

    /**
     * @see org.eclipse.babel.core.message.resource.IMessagesResource#serialize(
     *              org.eclipse.babel.core.message.MessagesBundle)
     */
    public void serialize(MessagesBundle messagesBundle) {
        setText(serializer.serialize(messagesBundle));
    }

    /**
     * @see org.eclipse.babel.core.message.resource.IMessagesResource
     * 		#deserialize(org.eclipse.babel.core.message.MessagesBundle)
     */
    public void deserialize(MessagesBundle messagesBundle) {
        deserializer.deserialize(messagesBundle, getText());
    }

    /**
     * Gets the {@link Properties}-like formated text.
     * @return formated text
     */
    protected abstract String getText();
    /**
     * Sets the {@link Properties}-like formated text.
     * @param text formated text
     */
    protected abstract void setText(String text);
    
}
