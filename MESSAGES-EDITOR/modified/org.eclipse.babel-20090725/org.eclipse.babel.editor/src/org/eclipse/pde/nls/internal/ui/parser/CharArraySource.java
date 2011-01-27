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
package org.eclipse.pde.nls.internal.ui.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * A scanner source that is backed by a character array.
 */
public class CharArraySource implements IScannerSource {

	private char[] cbuf;

	/** The end position of this source. */
	private int end;

	private int[] lineEnds = new int[2048];

	/**
	 * The current position at which the next character will be read.
	 * The value <code>Integer.MAX_VALUE</code> indicates, that the end
	 * of the source has been reached (the EOF character has been returned).
	 */
	int currentPosition = 0;

	/** The number of the current line. (Line numbers are one-based.) */
	int currentLineNumber = 1;

	protected CharArraySource() {
	}

	/**
	 * Constructs a scanner source from a char array.
	 */
	public CharArraySource(char[] cbuf) {
		this.cbuf = cbuf;
		this.end = cbuf.length;
	}

	/**
	 * Resets this source on the given array.
	 * 
	 * @param cbuf the array to read from
	 * @param begin where to begin reading
	 * @param end where to end reading
	 */
	protected void reset(char[] cbuf, int begin, int end) {
		if (cbuf == null) {
			this.cbuf = null;
			this.end = -1;
			currentPosition = -1;
			currentLineNumber = -1;
			lineEnds = null;
		} else {
			this.cbuf = cbuf;
			this.end = end;
			currentPosition = begin;
			currentLineNumber = 1;
			lineEnds = new int[2];
		}
	}

	/*
	 * @see scanner.IScannerSource#charAt(int)
	 */
	public int charAt(int index) {
		if (index < end) {
			return cbuf[index];
		} else {
			return -1;
		}
	}

	/*
	 * @see scanner.IScannerSource#currentChar()
	 */
	public int lookahead() {
		if (currentPosition < end) {
			return cbuf[currentPosition];
		} else {
			return -1;
		}
	}

	/*
	 * @see scanner.IScannerSource#lookahead(int)
	 */
	public int lookahead(int n) {
		int pos = currentPosition + n - 1;
		if (pos < end) {
			return cbuf[pos];
		} else {
			return -1;
		}
	}

	/*
	 * @see core.IScannerSource#readChar()
	 */
	public int readChar() {
		if (currentPosition < end) {
			return cbuf[currentPosition++];
		} else {
			currentPosition++;
			return -1;
		}
	}

	/*
	 * @see core.IScannerSource#readChar(int)
	 */
	public int readChar(int expected) {
		int c = readChar();
		if (c == expected) {
			return c;
		} else {
			String message = "Expected char '"
					+ (char) expected
					+ "' (0x"
					+ hexDigit((expected >> 4) & 0xf)
					+ hexDigit(expected & 0xf)
					+ ") but got '" + (char) c + "' (0x" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					+ hexDigit((c >> 4) & 0xf)
					+ hexDigit(c & 0xf)
					+ ")"; //$NON-NLS-1$
			throw new LexicalErrorException(this, message);
		}
	}

	/*
	 * @see scanner.IScannerSource#unreadChar()
	 */
	public void unreadChar() {
		currentPosition--;
	}

	/*
	 * @see core.IScannerSource#hasMoreChars()
	 */
	public boolean hasMoreChars() {
		return currentPosition < end;
	}

	/*
	 * @see scanner.IScannerSource#getPosition()
	 */
	public int getPosition() {
		if (currentPosition < end)
			return currentPosition;
		else
			return end;
	}

	/*
	 * @see core.IScannerSource#isAtLineBegin()
	 */
	public boolean isAtLineBegin() {
		return currentPosition == lineEnds[currentLineNumber - 1];
	}

	/*
	 * @see scanner.IScannerSource#getCurrentLineNumber()
	 */
	public int getCurrentLineNumber() {
		return currentLineNumber;
	}

	/*
	 * @see scanner.IScannerSource#getCurrentColumnNumber()
	 */
	public int getCurrentColumnNumber() {
		return currentPosition - lineEnds[currentLineNumber - 1] + 1;
	}

	/*
	 * @see scanner.IScannerSource#getLineEnds()
	 */
	public int[] getLineEnds() {
		return lineEnds;
	}
	/*
	 * @see scanner.IScannerSource#pushLineSeparator()
	 */
	public void pushLineSeparator() {
		if (currentLineNumber >= lineEnds.length) {
			int[] newLineEnds = new int[lineEnds.length * 2];
			System.arraycopy(lineEnds, 0, newLineEnds, 0, lineEnds.length);
			lineEnds = newLineEnds;
		}
		lineEnds[currentLineNumber++] = currentPosition;
	}

	/*
	 * @see scanner.IScannerSource#length()
	 */
	public int length() {
		return cbuf.length;
	}

	/**
	 * Returns a string that contains the characters of the source specified
	 * by the range <code>beginIndex</code> and the current position as the
	 * end index.
	 * 
	 * @param beginIndex
	 * @return the String
	 */
	public String toString(int beginIndex) {
		return toString(beginIndex, currentPosition);
	}

	/*
	 * @see scanner.IScannerSource#toString(int, int)
	 */
	public String toString(int beginIndex, int endIndex) {
		return new String(cbuf, beginIndex, endIndex - beginIndex);
	}

	/**
	 * Returns the original character array that backs the scanner source.
	 * All subsequent changes to the returned array will affect the scanner
	 * source.
	 * 
	 * @return the array
	 */
	public char[] getArray() {
		return cbuf;
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Line=" + getCurrentLineNumber() + ", Column=" + getCurrentColumnNumber(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static char hexDigit(int digit) {
		return "0123456789abcdef".charAt(digit); //$NON-NLS-1$
	}

	public static CharArraySource createFrom(Reader reader) throws IOException {
		StringBuffer buffer = new StringBuffer();
		int BUF_SIZE = 4096;
		char[] array = new char[BUF_SIZE];
		for (int read = 0; (read = reader.read(array, 0, BUF_SIZE)) > 0;) {
			buffer.append(array, 0, read);
		}
		char[] result = new char[buffer.length()];
		buffer.getChars(0, buffer.length(), result, 0);
		return new CharArraySource(result);
	}

}