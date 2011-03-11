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

import java.util.LinkedList;
import java.util.List;

public class CoverageReport {
	private final List<Ignore> ignores = new LinkedList<Ignore>();
	
	public Ignore createIgnore(){
		Ignore ignore = new Ignore();
		ignores.add(ignore);
		return ignore;
	}
	
	public List<Ignore> ignores(){
		return ignores;
	}

}
