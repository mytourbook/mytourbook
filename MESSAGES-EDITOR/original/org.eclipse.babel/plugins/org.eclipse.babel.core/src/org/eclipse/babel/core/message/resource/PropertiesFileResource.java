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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;
import org.eclipse.babel.core.util.FileChangeListener;
import org.eclipse.babel.core.util.FileMonitor;


/**
 * Properties file, where the underlying storage is a regular {@link File}.
 * For files referenced through Eclipse workspace, implementors should
 * use {@link PropertiesIFileResource}.
 * 
 * @author Pascal Essiembre
 * @see PropertiesIFileResource
 */
public class PropertiesFileResource extends AbstractPropertiesResource {

    private File file;
    
    /**
     * Constructor.
     * @param locale the resource locale
     * @param serializer resource serializer
     * @param deserializer resource deserializer
     * @param file the underlying file
     * @throws FileNotFoundException 
     */
    public PropertiesFileResource(
            Locale locale,
            PropertiesSerializer serializer,
            PropertiesDeserializer deserializer,
            File file) throws FileNotFoundException {
        super(locale, serializer, deserializer);
        this.file = file;
        FileMonitor.getInstance().addFileChangeListener(new FileChangeListener() {
            public void fileChanged(final File changedFile) {
                fireResourceChange(PropertiesFileResource.this);
            }
        }, file, 2000);  //TODO make file scan delay configurable
    }

    
    /**
     * @see org.eclipse.babel.core.message.resource.AbstractPropertiesResource
     * 			#getText()
     */
    public String getText() {
        FileReader inputStream = null;
        StringWriter outputStream = null;
        try {
            inputStream = new FileReader(file);
            outputStream = new StringWriter();
            int c;
            while ((c = inputStream.read()) != -1) {
                outputStream.write(c);
            }
        } catch (IOException e) {
            //TODO handle better.
            throw new RuntimeException(
                    "Cannot get properties file text. Handle better.", e);
        } finally {
            closeReader(inputStream);
            closeWriter(outputStream);
        }
        return outputStream.toString();
    }

    /**
     * @see org.eclipse.babel.core.message.resource.AbstractPropertiesResource
     * 		#setText(java.lang.String)
     */
    public void setText(String content) {
        StringReader inputStream = null;
        FileWriter outputStream = null;
        try {
            inputStream = new StringReader(content);
            outputStream = new FileWriter(file);
            int c;
            while ((c = inputStream.read()) != -1) {
                outputStream.write(c);
            }
        } catch (IOException e) {
            //TODO handle better.
            throw new RuntimeException(
                    "Cannot get properties file text. Handle better.", e);
        } finally {
            closeReader(inputStream);
            closeWriter(outputStream);
        }
    }

    /**
     * @see org.eclipse.babel.core.message.resource
     * 			.IMessagesResource#getSource()
     */
    public Object getSource() {
        return file;
    }
    
    /**
     * @return The resource location label. or null if unknown.
     */
    public String getResourceLocationLabel() {
    	return file.getAbsolutePath();
    }
    
    //TODO move to util class for convinience???
    private void closeWriter(Writer writer) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                //TODO handle better.
                throw new RuntimeException("Cannot close writer stream.", e);
            }
        }
    }
    
    //TODO move to util class for convinience???
    public void closeReader(Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                //TODO handle better.
                throw new RuntimeException("Cannot close reader.", e);
            }
        }
    }
    
    /**
     * Called before this object will be discarded.
     * Nothing to do: we were not listening to changes to this file.
     */
    public void dispose() {
    	//nothing to do.
    }
}
