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
package org.eclipse.babel.build.ui.wizard;

import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class ListFilter extends ViewerFilter{
	public static final String WILDCARD = "*"; //$NON-NLS-1$
	private Pattern fPattern;
	
	private final Map<Object, Object> fSelected;
	private final ILabelProvider fLabelProvider;

	public ListFilter(Map<Object, Object> selected, ILabelProvider labelProvider) {
		setPattern(WILDCARD);
		this.fSelected = selected;
		this.fLabelProvider = labelProvider;
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		// filter out any items that are currently selected
		// on a full refresh, these will have been added back to the list
		if (fSelected.containsKey(element))
			return false;

		String displayName = fLabelProvider.getText(element);
		return matches(element.toString()) || matches(displayName);
	}

	private boolean matches(String s) {
		return fPattern.matcher(s.toLowerCase()).matches();
	}

	public boolean setPattern(String pattern) {
		String newPattern = pattern.toLowerCase();

		if (!newPattern.endsWith(WILDCARD))
			newPattern += WILDCARD;
		if (!newPattern.startsWith(WILDCARD))
			newPattern = WILDCARD + newPattern;
		if (fPattern != null) {
			String oldPattern = fPattern.pattern();
			if (newPattern.equals(oldPattern))
				return false;
		}
		fPattern = PatternConstructor.createPattern(newPattern, true);
		return true;
	}

	private static class PatternConstructor {
		private static final Pattern PATTERN_BACK_SLASH = Pattern.compile("\\\\"); //$NON-NLS-1$
		private static final Pattern PATTERN_QUESTION = Pattern.compile("\\?"); //$NON-NLS-1$
		private static final Pattern PATTERN_STAR = Pattern.compile("\\*"); //$NON-NLS-1$
		private static final Pattern PATTERN_LBRACKET = Pattern.compile("\\("); //$NON-NLS-1$
		private static final Pattern PATTERN_RBRACKET = Pattern.compile("\\)"); //$NON-NLS-1$

		private static String asRegEx(String pattern, boolean group) {
			// Replace \ with \\, * with .* and ? with .
			// Quote remaining characters
			String result1 = PATTERN_BACK_SLASH.matcher(pattern).replaceAll("\\\\E\\\\\\\\\\\\Q"); //$NON-NLS-1$
			String result2 = PATTERN_STAR.matcher(result1).replaceAll("\\\\E.*\\\\Q"); //$NON-NLS-1$
			String result3 = PATTERN_QUESTION.matcher(result2).replaceAll("\\\\E.\\\\Q"); //$NON-NLS-1$
			if (group) {
				result3 = PATTERN_LBRACKET.matcher(result3).replaceAll("\\\\E(\\\\Q"); //$NON-NLS-1$
				result3 = PATTERN_RBRACKET.matcher(result3).replaceAll("\\\\E)\\\\Q"); //$NON-NLS-1$
			}
			return "\\Q" + result3 + "\\E"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		public static Pattern createPattern(String pattern, boolean isCaseSensitive) {
			if (isCaseSensitive)
				return Pattern.compile(asRegEx(pattern, false));
			return Pattern.compile(asRegEx(pattern, false), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}

		public static Pattern createGroupedPattern(String pattern, boolean isCaseSensitive) {
			if (isCaseSensitive)
				return Pattern.compile(asRegEx(pattern, true));
			return Pattern.compile(asRegEx(pattern, true), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
		}

		private PatternConstructor() {
		}
	}

}
