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

public interface IScannerSource {

	/**
	 * Returns the character at the specified position.
	 * 
	 * @param position the index of the character to be returned
	 * @return the character at the specified position (0-based)
	 */
	public int charAt(int position);

	/**
	 * Returns the character that will be returned by the next call
	 * to <code>readChar()</code>. Calling this method is equal to the
	 * following calls:
	 * <pre>
	 *     lookahead(1)
	 *     charAt(getPosition())
	 * </pre>
	 * 
	 * @return the character at the current position
	 */
	public int lookahead();

	/**
	 * Returns the character at the given lookahead position. Calling this
	 * method is equal to the following call:
	 * <pre>
	 *     charAt(currentPosition + n - 1)
	 * </pre>
	 * 
	 * @param n the number of characters to look ahead
	 * 
	 * @return the character
	 */
	public int lookahead(int n);

	/**
	 * Reads a single character.
	 * 
	 * @return the character read, or -1 if the end of the source
	 *     has been reached 
	 */
	public int readChar();
	
	/**
	 * Reads a single character.
	 * 
	 * @param expected the expected character; if the character read does not
	 *     match this character, a <code>LexcialErrorException</code> will be thrown
	 * @return the character read, or -1 if the end of the source
	 *     has been reached 
	 */
	public int readChar(int expected);
	
	/**
	 * Unreads a single character. The current position will be decreased
	 * by 1. If -1 has been read multiple times, it will be unread multiple
	 * times.
	 */
	public void unreadChar();

	/**
	 * Retruns the current position of the source.
	 * 
	 * @return the position (0-based)
	 */
	public int getPosition();

	/**
	 * Returns <code>true</code> if the current position is at the beginning of a line.
	 * 
	 * @return <code>true</code> if the current position is at the beginning of a line
	 */
	public boolean isAtLineBegin();

	/**
	 * Returns the current line number. 
	 * 
	 * @return the current line number (1-based)
	 */
	public int getCurrentLineNumber();
	
	/**
	 * Returns the current column number. 
	 * 
	 * @return the current column number (1-based)
	 */
	public int getCurrentColumnNumber(); 

	/**
	 * Records the next line end position. This method has to be called
	 * just after the line separator has been read.
	 * 
	 * @see IScannerSource#getLineEnds()
	 */
	public void pushLineSeparator();

	/**
	 * Returns an array of the line end positions recorded so far. Each value points
	 * to first character following the line end (and is thus an exclusive index
	 * to the line end). By definition the value <code>lineEnds[0]</code> is 0.
	 * <code>lineEnds[1]</code> contains the line end position of the first line.
	 * 
	 * @return an array containing the line end positions
	 * 
	 * @see IScannerSource#pushLineSeparator()
	 */
	public int[] getLineEnds();

	/**
	 * Returns <code>true</code> if more characters are available.
	 * 
	 * @return <code>true</code> if more characters are available
	 */
	public boolean hasMoreChars();

	/**
	 * Returns a String that contains the characters in the specified range
	 * of the source.
	 * 
	 * @param beginIndex the beginning index, inclusive
	 * @param endIndex the ending index, exclusive
	 * @return the newly created <code>String</code>
	 */
	public String toString(int beginIndex, int endIndex);

}
