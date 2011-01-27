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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.eclipse.pde.nls.internal.ui.parser.RawBundle;
import org.eclipse.pde.nls.internal.ui.parser.RawBundle.EntryLine;

public class RawBundleTest extends TestCase {

	private RawBundle rawBundle;

	public void test() throws IOException {
		String input = "   # a comment line\r\n"
				+ "key1=value1\r\n"
				+ "key2=a value \\\r\n"
				+ "     on two lines\r\n"
				+ "key3=a value \\\r\n"
				+ "     "
				+ "     with an empty line in between\r\n";
		rawBundle = readRawBundle(input);
		assertRawData("key1=value1\r\n", "key1");
		assertRawData("key2=a value \\\r\n" + "     on two lines\r\n", "key2");
		assertRawData("key3=a value \\\r\n" + "     " + "     with an empty line in between\r\n", "key3");
	}

	public void testKeysWithWhitespace() throws IOException {
		String input = ""
				+ "key1\t=key with tab\r\n"
				+ "key\\ 2 =key with escaped space\r\n"
				+ "key 3 =key with space\r\n";
		rawBundle = readRawBundle(input);
		assertRawData("key1\t=key with tab\r\n", "key1");
		assertRawData("key\\ 2 =key with escaped space\r\n", "key 2");
		assertRawData("key 3 =key with space\r\n", "key");
	}

	public void testKeysWithMissingValue() throws IOException {
		String input = "keyWithoutValue\r\n" + "key=value\r\n";
		rawBundle = readRawBundle(input);
		assertRawData("keyWithoutValue\r\n", "keyWithoutValue");
		assertRawData("key=value\r\n", "key");
	}

	public void testPut() throws IOException {
		String input = "" + "key1=value1\r\n" + "key3=value3\r\n";
		rawBundle = readRawBundle(input);
		rawBundle.put("key2", "value2");
		rawBundle.put("key4", "value4");
		rawBundle.put("key0", "value0\\\t");
		StringWriter stringWriter = new StringWriter();
		rawBundle.writeTo(stringWriter);
		assertEquals(""
				+ "key0=value0\\\\\\t\r\n"
				+ "key1=value1\r\n"
				+ "key2=value2\r\n"
				+ "key3=value3\r\n"
				+ "key4=value4\r\n", stringWriter.toString());

	}
	
	private void assertRawData(String expected, String key) {
		EntryLine entryLine = rawBundle.getEntryLine(key);
		assertEquals(expected, entryLine.getRawData());
	}

	private RawBundle readRawBundle(String input) throws IOException {
		return RawBundle.createFrom(new StringReader(input));
	}

}
