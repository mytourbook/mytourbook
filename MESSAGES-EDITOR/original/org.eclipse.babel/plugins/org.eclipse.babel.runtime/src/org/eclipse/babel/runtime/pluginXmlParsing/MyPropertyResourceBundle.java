 /*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/
package org.eclipse.babel.runtime.pluginXmlParsing;

import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

/**
 * Total waste of time class, needed just so we can set the protected
 * parent field.
 */
public class MyPropertyResourceBundle extends PropertyResourceBundle {

	public MyPropertyResourceBundle(InputStream resourceStream,
			PropertyResourceBundle parentBundle) throws IOException {
		super(resourceStream);
		setParent(parentBundle);
	}

}
