/*******************************************************************************
 * Copyright (c) 2008 Stefan Mücke and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Mücke - initial API and implementation
 *******************************************************************************/
package org.eclipse.nls.ui.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import junit.framework.TestCase;

public class PropertiesTest extends TestCase {

	private Properties properties;

	public void test() throws IOException {
		String input = "   # a comment line\r\n"
				+ "key1=value1\r\n"
				+ "key2=a value \\\r\n"
				+ "     on two lines\r\n"
				+ "key3=a value \\\r\n"
				+ "     "
				+ "     with an empty line in between\r\n";
		properties = readProperties(input);
		assertValue("value1", "key1");
		assertValue("a value on two lines", "key2");
		assertValue("a value with an empty line in between", "key3");
	}

	public void testKeysWithWhitespace() throws IOException {
		String input = ""
				+ "key1\t=key with tab\r\n"
				+ "key\\ 2 =key with escaped space\r\n"
				+ "key 3 =key with space\r\n";
		properties = readProperties(input);
		assertValue("key with tab", "key1");
		assertValue("key with escaped space", "key 2");
		assertValue("3 =key with space", "key");
	}

	public void testKeysWithMissingValue() throws IOException {
		String input = "" + "keyWithoutValue\r\n" + "key=value\r\n";
		properties = readProperties(input);
		assertValue("", "keyWithoutValue");
		assertValue("value", "key");
	}

	private void assertValue(String expected, String key) {
		assertEquals(expected, properties.get(key));
	}

	private Properties readProperties(String input) throws IOException {
		Properties properties = new Properties();
		properties.load(new ByteArrayInputStream(input.getBytes()));
		return properties;
	}

}
