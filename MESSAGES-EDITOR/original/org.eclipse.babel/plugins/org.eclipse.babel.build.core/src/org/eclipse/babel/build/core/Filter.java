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

import java.util.regex.Pattern;

public class Filter {
	private final Pattern pattern;
	private final String specifier;

	public Filter(String pattern) {
		specifier = pattern;
		this.pattern = Pattern.compile(pattern);
	}

	public String getPattern(){
		return specifier;
	}

	public boolean matches(PluginProxy plugin, ResourceProxy resource){
		String relativePath = resource.getRelativePath();
		boolean relative = pattern.matcher(relativePath).matches();
		boolean absolute = pattern.matcher(plugin.getName() + "/" + relativePath).matches();
		return relative || absolute;
	}
	
	public boolean isInclusive(){
		return false;
	}
}
