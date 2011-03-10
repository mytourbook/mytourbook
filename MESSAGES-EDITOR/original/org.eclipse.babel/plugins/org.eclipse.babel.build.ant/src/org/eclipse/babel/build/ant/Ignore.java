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
package org.eclipse.babel.build.ant;

import org.eclipse.babel.build.core.Filter;

public class Ignore {
	private String pattern;
	
	public void setPattern(String pattern){
		this.pattern = pattern;
	}
	
	public Filter toFilter(){
		return new Filter(pattern);
	}
}
