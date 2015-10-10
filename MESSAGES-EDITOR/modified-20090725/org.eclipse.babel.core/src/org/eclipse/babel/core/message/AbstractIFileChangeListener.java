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
package org.eclipse.babel.core.message;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;

/**
 * Same functionality than an {@link IResourceChangeListener} but listens to
 * a single file at a time. Subscribed and unsubscribed directly on
 * the MessageEditorPlugin.
 * 
 * @author Hugues Malphettes
 */
public abstract class AbstractIFileChangeListener {
	
	/**
	 * Takes a IResourceChangeListener and wraps it into an AbstractIFileChangeListener.
	 * <p>
	 * Delegates the listenedFileChanged calls to the underlying
	 * IResourceChangeListener#resourceChanged.
	 * </p>
	 * @param rcl
	 * @param listenedFile
	 * @return
	 */
	public static AbstractIFileChangeListener wrapResourceChangeListener(
			final IResourceChangeListener rcl, IFile listenedFile) {
		return new AbstractIFileChangeListener(listenedFile) {
			public void listenedFileChanged(IResourceChangeEvent event) {
				rcl.resourceChanged(event);
			}
		};
	}
	

	//we use a string to be certain that no memory will leak from this class:
	//there is nothing to do a priori to dispose of this class.
	private final String listenedFileFullPath;
	
	/**
	 * @param listenedFile The file this object listens to. It is final.
	 */
	public AbstractIFileChangeListener(IFile listenedFile) {
		listenedFileFullPath = listenedFile.getFullPath().toString();
	}
	
	/**
	 * @return The file listened to. It is final.
	 */
	public final String getListenedFileFullPath() {
		return listenedFileFullPath;
	}
	
	/**
	 * @param event The change event. It is guaranteed that this
	 * event's getResource() method returns the file that this object listens to.
	 */
	public abstract void listenedFileChanged(IResourceChangeEvent event);
	
	/**
	 * Interface implemented by the MessageEditorPlugin.
	 * <p>
	 * Describes the registry of file change listeners.
	 * 
	 * </p>
	 */
	public interface IFileChangeListenerRegistry {
		/**
		 * @param rcl Adds a subscriber to a resource change event.
		 */
		public void subscribe(AbstractIFileChangeListener fileChangeListener);
		
		/**
		 * @param rcl Removes a subscriber to a resource change event.
		 */
		public void unsubscribe(AbstractIFileChangeListener fileChangeListener);
	}

}
