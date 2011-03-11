/*******************************************************************************
 * Copyright (c) 2001, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.build.core;

import java.io.File;

public class ResourceProxy {
	private File fileResource; 		/** From translation catalogue */
	private String relativePath;	/** From eclipse target */
	private String canonicalPath;
	
	public ResourceProxy(File fileResource) {
		this.fileResource = fileResource;
	}
	
	public ResourceProxy(String relativePath) {
		this.relativePath = relativePath;
		this.relativePath = this.relativePath.replace('/', File.separatorChar);
		this.canonicalPath = relativePath.replace(File.separatorChar, '/');
	}
	
	public ResourceProxy(File fileResource, String relativePath) {
		this.fileResource = fileResource;
		this.relativePath = relativePath.replace('/', File.separatorChar);
		this.canonicalPath = relativePath.replace(File.separatorChar, '/');
	}

	public File getFileResource() {
		return fileResource;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getCanonicalPath() {
		return canonicalPath;
	}
}
