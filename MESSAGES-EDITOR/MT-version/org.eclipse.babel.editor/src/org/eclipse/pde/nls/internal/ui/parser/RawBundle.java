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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

/**
 * A class used to manipulate resource bundle files.
 */
public class RawBundle {

	public static abstract class RawLine {
		String rawData;
		public RawLine(String rawData) {
			this.rawData = rawData;
		}
		public String getRawData() {
			return rawData;
		}
	}
	public static class CommentLine extends RawLine {
		public CommentLine(String line) {
			super(line);
		}
	}
	public static class EmptyLine extends RawLine {
		public EmptyLine(String line) {
			super(line);
		}
	}
	public static class EntryLine extends RawLine {
		String key;
		public EntryLine(String key, String lineData) {
			super(lineData);
			this.key = key;
		}
	}

	/**
	 * The logical lines of the resource bundle.
	 */
	private ArrayList<RawLine> lines = new ArrayList<RawLine>();

	public RawBundle() {
	}

	public EntryLine getEntryLine(String key) {
		for (RawLine line : lines) {
			if (line instanceof EntryLine) {
				EntryLine entryLine = (EntryLine) line;
				if (entryLine.key.equals(key))
					return entryLine;
			}
		}
		return null;
	}

	public void put(String key, String value) {

		// Find insertion position
		int size = lines.size();
		int pos = -1;
		for (int i = 0; i < size; i++) {
			RawLine line = lines.get(i);
			if (line instanceof EntryLine) {
				EntryLine entryLine = (EntryLine) line;
				int compare = key.compareToIgnoreCase(entryLine.key);
				if (compare < 0) {
					if (pos == -1) {
						pos = i; // possible insertion position
					}
				} else if (compare > 0) {
					continue;
				} else if (key.equals(entryLine.key)) {
					entryLine.rawData = key + "=" + escape(value) + "\r\n";
					return;
				} else {
					pos = i; // possible insertion position
				}
			}
		}
		if (pos == -1)
			pos = lines.size();

		// Append new entry
		lines.add(pos, new EntryLine(key, key + "=" + escape(value) + "\r\n"));
	}

	private String escape(String str) {
		StringBuilder builder = new StringBuilder();
		int len = str.length();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case ' ' :
					if (i == 0) {
						builder.append("\\ ");
					} else {
						builder.append(c);
					}
					break;
				case '\t' :
					builder.append("\\t");
					break;
				case '=' :
				case ':' :
				case '#' :
				case '!' :
				case '\\' :
					builder.append('\\').append(c);
					break;
				default :
					if (31 <= c && c <= 255) {
						builder.append(c);
					} else {
						builder.append("\\u");
						builder.append(hexDigit((c >> 12) & 0x0f));
						builder.append(hexDigit((c >> 8) & 0x0f));
						builder.append(hexDigit((c >> 4) & 0x0f));
						builder.append(hexDigit(c & 0x0f));
					}
					break;
			}
		}
		return builder.toString();
	}

	private static char hexDigit(int digit) {
		return "0123456789ABCDEF".charAt(digit); //$NON-NLS-1$
	}

	public void writeTo(OutputStream out) throws IOException {
		OutputStreamWriter writer = new OutputStreamWriter(out, "ISO-8859-1");
		writeTo(writer);
	}

	public void writeTo(Writer writer) throws IOException {
		for (RawLine line : lines) {
			writer.write(line.rawData);
		}
	}

	public static RawBundle createFrom(InputStream in) throws IOException {
		IScannerSource source = CharArraySource.createFrom(new InputStreamReader(in, "ISO-8859-1"));
		return RawBundle.createFrom(source);
	}

	public static RawBundle createFrom(Reader reader) throws IOException {
		IScannerSource source = CharArraySource.createFrom(reader);
		return RawBundle.createFrom(source);
	}

	public static RawBundle createFrom(IScannerSource source) throws IOException {
		RawBundle rawBundle = new RawBundle();
		StringBuilder builder = new StringBuilder();

		while (source.hasMoreChars()) {
			int begin = source.getPosition();
			skipAllOf(" \t\u000c", source);

			// Comment line
			if (source.lookahead() == '#' || source.lookahead() == '!') {
				skipToOneOf("\r\n", false, source);
				consumeLineSeparator(source);
				int end = source.getPosition();
				String line = source.toString(begin, end);
				rawBundle.lines.add(new CommentLine(line));
				continue;
			}

			// Empty line
			if (isAtLineEnd(source)) {
				consumeLineSeparator(source);
				int end = source.getPosition();
				String line = source.toString(begin, end);
				rawBundle.lines.add(new EmptyLine(line));
				continue;
			}

			// Entry line
			{
				// Key
				builder.setLength(0);
				loop : while (source.hasMoreChars()) {
					char c = (char) source.readChar();
					switch (c) {
						case ' ' :
						case '\t' :
						case '\u000c' :
						case '=' :
						case '\r' :
						case '\n' :
							break loop;
						case '\\' :
							source.unreadChar();
							builder.append(readEscapedChar(source));
							break;
						default :
							builder.append(c);
							break;
					}
				}
				String key = builder.toString();

				// Value
				int end = 0;
				loop : while (source.hasMoreChars()) {
					char c = (char) source.readChar();
					switch (c) {
						case '\r' :
						case '\n' :
							consumeLineSeparator(source);
							end = source.getPosition();
							break loop;
						case '\\' :
							if (isAtLineEnd(source)) {
								consumeLineSeparator(source);
							} else {
								source.unreadChar();
								readEscapedChar(source);
							}
							break;
						default :
							break;
					}
				}
				if (end == 0)
					end = source.getPosition();

				String lineData = source.toString(begin, end);
				EntryLine entryLine = new EntryLine(key, lineData);
				rawBundle.lines.add(entryLine);
			}
		}

		return rawBundle;
	}

	private static char readEscapedChar(IScannerSource source) {
		source.readChar('\\');
		char c = (char) source.readChar();
		switch (c) {
			case ' ' :
			case '=' :
			case ':' :
			case '#' :
			case '!' :
			case '\\' :
				return c;
			case 't' :
				return '\t';
			case 'n' :
				return '\n';
			case 'u' :
				int d1 = Character.digit(source.readChar(), 16);
				int d2 = Character.digit(source.readChar(), 16);
				int d3 = Character.digit(source.readChar(), 16);
				int d4 = Character.digit(source.readChar(), 16);
				if (d1 == -1 || d2 == -1 || d3 == -1 || d4 == -1)
					throw new LexicalErrorException(source, "Illegal escape sequence");
				return (char) (d1 << 12 | d2 << 8 | d3 << 4 | d4);
			default :
				throw new LexicalErrorException(source, "Unknown escape sequence");
		}
	}

	private static boolean isAtLineEnd(IScannerSource source) {
		return source.lookahead() == '\r' || source.lookahead() == '\n';
	}

	private static void consumeLineSeparator(IScannerSource source) {
		if (source.lookahead() == '\n') {
			source.readChar();
			source.pushLineSeparator();
		} else if (source.lookahead() == '\r') {
			source.readChar();
			if (source.lookahead() == '\n') {
				source.readChar();
			}
			source.pushLineSeparator();
		}
	}
	
	private static void skipToOneOf(String delimiters, boolean readDelimiter, IScannerSource source) {
		loop : while (source.hasMoreChars()) {
			int c = source.readChar();
			if (delimiters.indexOf(c) != -1) {
				if (!readDelimiter) {
					source.unreadChar();
				}
				break loop;
			}
			if (c == '\r') {
				source.readChar('\n');
				source.pushLineSeparator();
			}
		}
	}

	private static void skipAllOf(String string, IScannerSource source) {
		while (source.hasMoreChars() && string.indexOf(source.lookahead()) != -1) {
			source.readChar();
		}
	}
	

}
