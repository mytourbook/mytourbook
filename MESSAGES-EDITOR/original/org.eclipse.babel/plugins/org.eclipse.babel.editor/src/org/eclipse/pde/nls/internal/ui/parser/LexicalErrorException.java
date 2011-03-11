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

/**
 * Exception thrown by a scanner when encountering lexical errors.
 */
public class LexicalErrorException extends RuntimeException {

	private int lineNumber;
	private int columnNumber;

	/**
	 * Creates a <code>LexicalErrorException</code> without a detailed message. 
	 * 
	 * @param source the scanner source the error occured on  
	 */
	public LexicalErrorException(IScannerSource source) {
		this(source.getCurrentLineNumber(), source.getCurrentColumnNumber(), null);
	}

	/**
	 * @param source the scanner source the error occured on  
	 * @param message the error message 
	 */
	public LexicalErrorException(IScannerSource source, String message) {
		this(source.getCurrentLineNumber(), source.getCurrentColumnNumber(), message);
	}

	/**
	 * @param line the number of the line where the error occured  
	 * @param column the numer of the column where the error occured  
	 * @param message the error message 
	 */
	public LexicalErrorException(int line, int column, String message) {
		super("Lexical error (" + line + ", " + column + (message == null ? ")" : "): " + message)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		this.lineNumber = line;
		this.columnNumber = column;
	}

	/**
	 * Returns the line number where the error occured.
	 * 
	 * @return the line number where the error occured
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Returns the column number where the error occured.
	 * 
	 * @return the column number where the error occured
	 */
	public int getColumnNumber() {
		return columnNumber;
	}

	public static LexicalErrorException unexpectedCharacter(IScannerSource source, int c) {
		return new LexicalErrorException(source, "Unexpected character: '" //$NON-NLS-1$
			+ (char) c
			+ "' (0x" //$NON-NLS-1$
			+ hexDigit((c >> 4) & 0xf)
			+ hexDigit(c & 0xf)
			+ ")"); //$NON-NLS-1$
	}

	private static char hexDigit(int digit) {
		return "0123456789abcdef".charAt(digit); //$NON-NLS-1$
	}

}
