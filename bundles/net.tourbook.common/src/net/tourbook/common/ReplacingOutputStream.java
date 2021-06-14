/**
 * Source: http://stackoverflow.com/questions/1624830/java-search-replace-in-a-stream<p>
 *
 * Date: 2015-01-28
 */
package net.tourbook.common;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

/**
 * I got some good ideas from the link provided and ended up writing a small class to handle
 * replacement of $VAR$ variables in a stream.
 */
public class ReplacingOutputStream extends OutputStream {

   private static final int          DOLLAR_SIGN      = "$".codePointAt(0);  //$NON-NLS-1$
   private static final int          BACKSLASH        = "\\".codePointAt(0); //$NON-NLS-1$

   private final OutputStream        _outputStream;
   private final Map<String, Object> _replacementValues;

   private int                       _previousValue   = Integer.MIN_VALUE;
   private boolean                   _isReplacing     = false;

   private ArrayList<Integer>        _allReplacements = new ArrayList<>();

   public ReplacingOutputStream(final OutputStream delegate, final Map<String, Object> replacementValues) {

      _outputStream = delegate;
      _replacementValues = replacementValues;
   }

   private void doReplacement() throws IOException {

      final StringBuilder sb = new StringBuilder();

      for (final Integer intvalue : _allReplacements) {
         final char[] chars = Character.toChars(intvalue);
         sb.append(chars);
      }

      _allReplacements.clear();

      final String oldValue = sb.toString();
      final Object replaceValue = _replacementValues.get(oldValue);

      if (replaceValue == null) {
         throw new RuntimeException("Could not find replacement variable for value '" + oldValue + "'."); //$NON-NLS-1$ //$NON-NLS-2$
      }

      if (replaceValue instanceof byte[]) {

         final byte[] newValue = (byte[]) replaceValue;

         _outputStream.write(newValue);

      } else {

         final String newValue = replaceValue.toString();

         for (int i = 0; i < newValue.length(); ++i) {

            final int value = newValue.codePointAt(i);

            _outputStream.write(value);
         }
      }
   }

   public @Override void write(final int byteValue) throws IOException {

      if (byteValue == DOLLAR_SIGN && _previousValue != BACKSLASH) {

         if (_isReplacing) {

            doReplacement();

            _isReplacing = false;

         } else {

            _isReplacing = true;
         }

      } else {

         if (_isReplacing) {

            _allReplacements.add(byteValue);

         } else {

            _outputStream.write(byteValue);
         }
      }

      _previousValue = byteValue;

   }
}
