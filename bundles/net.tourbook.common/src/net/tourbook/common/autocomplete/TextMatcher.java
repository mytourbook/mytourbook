/*******************************************************************************
 * Copyright (c) 2020 Thomas Wolf<thomas.wolf@paranor.ch> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package net.tourbook.common.autocomplete;

// Source: package org.eclipse.ui.internal.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.tourbook.common.UI;

import org.eclipse.core.text.StringMatcher;

/**
 * Similar to {@link StringMatcher}, this {@code TextMatcher} matches a pattern
 * that may contain the wildcards '?' or '*' against a text. However, the
 * matching is not only done on the full text, but also on individual words from
 * the text, and if the pattern contains whitespace, the pattern is split into
 * sub-patterns and those are matched, too.
 * <p>
 * The precise rules are:
 * </p>
 * <ul>
 * <li>Leading and trailing whitespace in the pattern is ignored.</li>
 * <li>If the full pattern matches the full text, the match succeeds.</li>
 * <li>If the full pattern matches a single word of the text, the match
 * succeeds.</li>
 * <li>If all sub-patterns match a prefix of the whole text or any prefix of any
 * word, the match succeeds.</li>
 * <li>Otherwise, the match fails.</li>
 * </ul>
 * <p>
 * An empty pattern matches only the empty text.
 * </p>
 */
public final class TextMatcher {

   private static final Pattern      NON_WORD = Pattern.compile("\\W+", Pattern.UNICODE_CHARACTER_CLASS); //$NON-NLS-1$

   private final StringMatcher       _full;
   private final List<StringMatcher> _parts;

   /**
    * Creates a new {@link TextMatcher}.
    *
    * @param pattern
    *           to match
    * @param isIgnoreCase
    *           whether to do case-insensitive matching
    * @param isIgnoreWildCards
    *           whether to treat '?' and '*' as normal characters, not
    *           as wildcards
    *
    * @throws IllegalArgumentException
    *            if {@code pattern == null}
    */
   public TextMatcher(final String pattern, final boolean isIgnoreCase, final boolean isIgnoreWildCards) {

      _full = new StringMatcher(UI.SYMBOL_STAR + pattern.trim() + UI.SYMBOL_STAR,
            isIgnoreCase,
            isIgnoreWildCards);

      _parts = splitPattern(pattern, isIgnoreCase, isIgnoreWildCards);
   }

   /**
    * Splits a given text into words.
    *
    * @param text
    *           to split
    *
    * @return the words of the text
    */
   public static String[] getWords(final String text) {
      // Previous implementations (in the removed StringMatcher) used the ICU
      // BreakIterator to split the text. That worked well, but in 2020 it was decided
      // to drop the dependency to the ICU library due to its size. The JDK
      // BreakIterator splits differently, causing e.g.
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=563121 . The NON_WORD regexp
      // appears to work well for programming language text, but may give sub-optimal
      // results for natural languages. See also
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=90579 .
      return NON_WORD.split(text);
   }

   /**
    * Determines whether the given {@code text} matches the pattern.
    *
    * @param text
    *           String to match; must not be {@code null}
    *
    * @return {@code true} if the whole {@code text} matches the pattern;
    *         {@code false} otherwise
    *
    * @throws IllegalArgumentException
    *            if {@code text == null}
    */
   public boolean match(final String text) {
      if (text == null) {
         throw new IllegalArgumentException();
      }
      return match(text, 0, text.length());
   }

   /**
    * Determines whether the given sub-string of {@code text} from {@code start}
    * (inclusive) to {@code end} (exclusive) matches the pattern.
    *
    * @param text
    *           String to match in; must not be {@code null}
    * @param start
    *           start index (inclusive) within {@code text} of the sub-string to
    *           match
    * @param end
    *           end index (exclusive) within {@code text} of the sub-string to
    *           match
    *
    * @return {@code true} if the given slice of {@code text} matches the pattern;
    *         {@code false} otherwise
    *
    * @throws IllegalArgumentException
    *            if {@code text == null}
    */
   public boolean match(final String text, int start, int end) {

      if (text == null) {
         throw new IllegalArgumentException();
      }

      if (start > end) {
         return false;
      }

      final int tlen = text.length();
      start = Math.max(0, start);
      end = Math.min(end, tlen);
      if (_full.match(text, start, end)) {
         return true;
      }

      final String[] words = getWords(text.substring(start, end));
      if (match(_full, words)) {
         return true;
      }

      if (_parts.isEmpty()) {
         return false;
      }

      for (final StringMatcher subMatcher : _parts) {
         if (!subMatcher.match(text, start, end) && !match(subMatcher, words)) {
            return false;
         }
      }

      return true;
   }

   private boolean match(final StringMatcher matcher, final String[] words) {
      return Arrays.stream(words).filter(Objects::nonNull).anyMatch(matcher::match);
   }

   private List<StringMatcher> splitPattern(final String pattern,
                                            final boolean ignoreCase,
                                            final boolean ignoreWildCards) {

      final String patternTrimmed = pattern.trim();
      if (patternTrimmed.isEmpty()) {
         return Collections.emptyList();
      }

      final String[] allSubPatterns = patternTrimmed.split("\\s+"); //$NON-NLS-1$

      if (allSubPatterns.length <= 1) {
         return Collections.emptyList();
      }

      final List<StringMatcher> allMatchers = new ArrayList<>();

      for (final String subPattern : allSubPatterns) {

         if (subPattern == null || subPattern.isEmpty()) {
            continue;
         }

         final String matcherString = UI.SYMBOL_STAR + subPattern + UI.SYMBOL_STAR;

         final StringMatcher subPatternMatcher = new StringMatcher(matcherString,
               ignoreCase,
               ignoreWildCards);

         subPatternMatcher.usePrefixMatch();

         allMatchers.add(subPatternMatcher);
      }

      return allMatchers;
   }

   @Override
   public String toString() {
      return '[' + _full.toString() + ',' + _parts + ']';
   }
}
