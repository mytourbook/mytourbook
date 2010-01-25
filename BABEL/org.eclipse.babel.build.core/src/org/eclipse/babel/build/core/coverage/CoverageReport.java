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
package org.eclipse.babel.build.core.coverage;

import java.io.InputStream;
import java.io.OutputStream;

public interface CoverageReport {
	public static class helper{
		public static void serialize(OutputStream out){
			// TODO: stub
		}
		
		public static CoverageReport parse(InputStream in){
			// TODO: stub
			return null; 
		}
	}
	/**
	public List<LocaleProxy> locales();
	public List<PluginProxy> plugins();
	public Date timestamp();
	public EclipseArchive archive();
	public TranslationArchive translations();
	public LanguagePack catalogue();
	*/
}
