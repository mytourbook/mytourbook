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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class LocaleProxy {
	public static final Set<LocaleProxy> ALL = new HashSet<LocaleProxy>();
	public static final Comparator<LocaleProxy> NAME_COMPARATOR = new Comparator<LocaleProxy>(){
		public int compare(LocaleProxy o1, LocaleProxy o2) {
			return o1.name.compareTo(o2.name);
		}
	};
	private String name;
	
	public LocaleProxy(String name) {
		this.name = name;
	}
	
	public String getName(){		
		return name;
	}
	
	public boolean equals(Object object) {
		LocaleProxy locale = (LocaleProxy)object;
		return locale.getName().equals(this.name);
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}
}
