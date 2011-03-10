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
package org.eclipse.babel.build.core.xml;

/**
 * An attribute pair on an Element in a Builder document tree. 
 */
public class Attribute {
	private final String name;
	private final String value;

	/**
	 * Create a new attribute.
	 *  
	 * @param name The name of the attribute.
	 * @param value The value of the attribute.
	 */
	public Attribute(String name, String value){
		this.name = name;
		this.value = value;
	}

	/**
	 * Retrieve the attribute's name.
	 * 
	 * @return The attribute's name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieve the attribute's value.
	 * 
	 * @return The attribute's value. 
	 */
	public String getValue() {
		return value;
	}
}
