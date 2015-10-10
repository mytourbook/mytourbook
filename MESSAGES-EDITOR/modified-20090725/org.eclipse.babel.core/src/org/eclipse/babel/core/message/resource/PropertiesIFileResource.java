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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.eclipse.babel.core.message.AbstractIFileChangeListener;
import org.eclipse.babel.core.message.AbstractIFileChangeListener.IFileChangeListenerRegistry;
import org.eclipse.babel.core.message.resource.ser.PropertiesDeserializer;
import org.eclipse.babel.core.message.resource.ser.PropertiesSerializer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;


/**
 * Properties file, where the underlying storage is a {@link IFile}.
 * When dealing with {@link File} as opposed to {@link IFile}, 
 * implementors should use {@link PropertiesFileResource}.
 * 
 * @author Pascal Essiembre
 * @see PropertiesFileResource
 */
public class PropertiesIFileResource extends AbstractPropertiesResource {

    private final IFile file;
    
    private final AbstractIFileChangeListener fileListener;
    private final IFileChangeListenerRegistry listenerRegistry;
    
    /**
     * Constructor.
     * @param locale the resource locale
     * @param serializer resource serializer
     * @param deserializer resource deserializer
     * @param file the underlying {@link IFile}
     * @param listenerRegistry It is the MessageEditorPlugin. 
     * Or null if we don't care for file changes.
     * We could replace it by an activator in this plugin.
     */
    public PropertiesIFileResource(
            Locale locale,
            PropertiesSerializer serializer,
            PropertiesDeserializer deserializer,
            IFile file, IFileChangeListenerRegistry listenerRegistry) {
        super(locale, serializer, deserializer);
        this.file = file;
        this.listenerRegistry = listenerRegistry;
        
        //[hugues] FIXME: this object is built at the beginning
        //of a build (no message editor)
        //it is disposed of at the end of the build.
        //during a build files are not changed.
        //so it is I believe never called.
        if (this.listenerRegistry != null) {
	        IResourceChangeListener rcl =
	            new IResourceChangeListener() {
	                public void resourceChanged(IResourceChangeEvent event) {
	                    //no need to check: it is always the case as this
	                	//is subscribed for a particular file.
//	                    if (event.getResource() != null
//	                       		&& PropertiesIFileResource.this.file.equals(event.getResource())) {
	                        fireResourceChange(PropertiesIFileResource.this);
//	                    }
	                }
             	};
	    	 fileListener = AbstractIFileChangeListener
	        		.wrapResourceChangeListener(rcl, file);
	    	 this.listenerRegistry.subscribe(fileListener);
        } else {
        	fileListener = null;
        }
    }

    /**
     * @see org.eclipse.babel.core.message.resource.AbstractPropertiesResource
     * 			#getText()
     */
    public String getText() {
        try {
            InputStream is = file.getContents();
            int byteCount = is.available();
            byte[] b = new byte[byteCount];
            is.read(b);
            String content = new String(b, file.getCharset());
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e); //TODO handle better
        } catch (CoreException e) {
            throw new RuntimeException(e); //TODO handle better
        }
    }

    /**
     * @see org.eclipse.babel.core.message.resource.TextResource#setText(
     *              java.lang.String)
     */
    public void setText(String text) {
        try {
        	String charset = file.getCharset();
        	ByteArrayInputStream is = new ByteArrayInputStream(
        			text.getBytes(charset));
            file.setContents(is, IFile.KEEP_HISTORY, null);
        } catch (Exception e) {
            //TODO handle better
            throw new RuntimeException(
                    "Cannot set content on properties file.", e); //$NON-NLS-1$
        }
    }
    
    /**
     * @see org.eclipse.babel.core.message.resource.IMessagesResource
     * 		#getSource()
     */
    public Object getSource() {
        return file;
    }    
    
    /**
     * @return The resource location label. or null if unknown.
     */
    public String getResourceLocationLabel() {
    	return file.getFullPath().toString();
    }
    
    /**
     * Called before this object will be discarded.
     * If this object was listening to file changes: then unsubscribe it.
     */
    public void dispose() {
    	if (this.listenerRegistry != null) {
    		this.listenerRegistry.unsubscribe(this.fileListener);
    	}
    }

    
}
