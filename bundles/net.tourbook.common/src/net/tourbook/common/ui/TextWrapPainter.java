/*******************************************************************************
 * Copyright (C) 2017, 2024 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.common.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;

/*
 * This is partly copied from Snippet133
 */
public class TextWrapPainter {

   private StringBuilder _wordbuffer = new StringBuilder();

   private int           _lineHeight;
   private int           _tabWidth;

   private int           _devLeftMargin;
   private int           _devRightMargin;
   private int           _devX;
   private int           _devY;

   private String        _tabText;

   private boolean       _is1stPainted;
   private int           _lastPaintedY;

   private boolean       _isTruncateText;
   private int           _maxTruncatedLines;
   private int           _numTruncatedLines;

   private int           _numPaintedLines;
   private int           _linePaintedCounter;

   {
      /*
       * Create a buffer for computing tab width.
       */
      final int tabSize = 4;
      final StringBuilder tabBuffer = new StringBuilder(tabSize);
      for (int i = 0; i < tabSize; i++) {
         tabBuffer.append(' ');
      }

      _tabText = tabBuffer.toString();
   }


   /**
    * @param gc
    * @param devX
    *           Top left position for the location icon
    * @param devY
    *
    * @param allLocationNameWords
    *
    * @param formattedTextSize
    * @param iconSize
    * @param foregroundColor
    * @param mapLocationLineHeight
    *
    * @return
    */
   public void drawFormattedText(final GC gc,

                                 final int devX,
                                 final int devY,

                                 final List<FormattedWord> allLocationNameWords,

                                 final Color foregroundColor) {

      // draw text foreground
      gc.setForeground(foregroundColor);

      for (final FormattedWord formattedWord : allLocationNameWords) {

         gc.drawString(formattedWord.word,
               devX + formattedWord.devX + formattedWord.lineHorizontalOffset,
               devY + formattedWord.devY,
               true);
      }
   }

   /**
    * @param gc
    * @param devX
    *           Top left position for the location icon
    * @param devY
    *
    * @param allLocationNameWords
    *
    * @param formattedTextSize
    * @param iconSize
    * @param isMapBackgroundDark
    * @param mapLocationLineHeight
    *
    * @return Returns the painted text rectangle
    */
   public Rectangle drawPreformattedText(final GC gc,

                                         final int devXTopLeft,
                                         final int devYTopLeft,

                                         final List<FormattedWord> allLocationNameWords,

                                         final Point formattedTextSize,
                                         final Rectangle iconSize,

                                         final int mapLocationLineHeight,

                                         final Color textColor,
                                         final Color shadowColor) {

      final int textWidth = formattedTextSize.x;
      final int textHeight = formattedTextSize.y;
      final int textHeight2 = textHeight / 2;

      final int iconWidth = iconSize.width;
      final int iconHeight = iconSize.height;
      final int iconHeight2 = iconHeight / 2;

      final int lineHeight2 = mapLocationLineHeight / 2;

      int devX = 0;
      int devY = 0;

      final int textIconOffset = 5;

      TextPosition textPosition;
      textPosition = TextPosition.Bottom;
      textPosition = TextPosition.Top;
      textPosition = TextPosition.Right;
      textPosition = TextPosition.Left;

      if (TextPosition.Top == textPosition) {

         devX = devXTopLeft - textWidth / 2 + iconSize.width / 2;
         devY = devYTopLeft - textHeight - textIconOffset;

      } else if (TextPosition.Bottom == textPosition) {

         devX = devXTopLeft - textWidth / 2 + iconSize.width / 2;
         devY = devYTopLeft + iconHeight - textIconOffset;

      } else if (TextPosition.Left == textPosition) {

         devX = devXTopLeft - textWidth - textIconOffset;
         devY = devYTopLeft + iconHeight2 - textHeight2 - lineHeight2;

      } else if (TextPosition.Right == textPosition) {

         devX = devXTopLeft + iconWidth + textIconOffset;
         devY = devYTopLeft + iconHeight2 - textHeight2 - lineHeight2;
      }

      // draw text shadow
      gc.setForeground(shadowColor);

      for (final FormattedWord formattedWord : allLocationNameWords) {

         gc.drawText(formattedWord.word,
               devX + formattedWord.devX + formattedWord.lineHorizontalOffset + 1,
               devY + formattedWord.devY + 1,
               true);
      }

      // draw text foreground
      gc.setForeground(textColor);

      for (final FormattedWord formattedWord : allLocationNameWords) {

         gc.drawString(formattedWord.word,
               devX + formattedWord.devX + formattedWord.lineHorizontalOffset,
               devY + formattedWord.devY,
               true);
      }

      final Rectangle paintedRectangle = new Rectangle(devX, devY, textWidth, textHeight);

      return paintedRectangle;
   }

   /**
    * @param gc
    * @param textToPrint
    *           Text which is printed
    * @param devX
    *           Left margin
    * @param devY
    *           Top margin
    * @param viewportWidth
    *           Viewport width
    * @param viewportHeight
    *           Viewport height
    * @param fontHeight
    * @param noOverlapRect
    * @param isTruncateText
    * @param truncatedLines
    */
   public void drawText(final GC gc,
                        final String textToPrint,
                        final int devX,
                        final int devY,
                        final int viewportWidth,
                        final int viewportHeight,
                        final int fontHeight,
                        final Rectangle noOverlapRect,
                        final boolean isTruncateText,
                        final int truncatedLines) {

      drawText(

            gc,
            textToPrint,

            devX,
            devY,

            viewportWidth,
            viewportHeight,

            fontHeight,
            noOverlapRect,

            isTruncateText,
            truncatedLines,

            null);
   }

   private void drawText(final GC gc,
                         final String textToPrint,

                         final int devX,
                         final int devY,
                         final int viewportWidth,
                         final int viewportHeight,

                         final int fontHeight,
                         final Rectangle noOverlapRect,

                         final boolean isTruncateText,
                         final int truncatedLines,

                         final List<FormattedWord> allFormattedWords) {

      _is1stPainted = false;

      if (viewportWidth < 0 || viewportHeight < 0) {
         return;
      }

      _tabWidth = gc.stringExtent(_tabText).x;
      _lineHeight = fontHeight;

      _devX = _devLeftMargin = devX;
      _devY = devY;

      // fix problem when an empty string is painted
      _lastPaintedY = _devY;

      _numPaintedLines = 0;
      _linePaintedCounter = -1;

      _isTruncateText = isTruncateText;
      _maxTruncatedLines = truncatedLines;
      _numTruncatedLines = 2;

      _devRightMargin = devX + viewportWidth;
      final int bottom = devY + viewportHeight;

      // truncate buffer
      _wordbuffer.setLength(0);

      final Rectangle textRect = new Rectangle(devX, devY, viewportWidth, viewportHeight);
      if (noOverlapRect == null || noOverlapRect.width < 1 || noOverlapRect.height < 1) {

         gc.setClipping(textRect);

      } else {

         final Region region = new Region();
         {
            region.add(textRect);
            region.subtract(noOverlapRect);

            gc.setClipping(region);
         }
         region.dispose();
      }

      int index = 0;
      final int end = textToPrint.length();

      while (index < end) {

         final char c = textToPrint.charAt(index);

         index++;

         if (c != 0) {

            if (c == 0x0a || c == 0x0d) {

               // print line + new line

               // if this is cr-lf, skip the lf
               if (c == 0x0d && index < end && textToPrint.charAt(index) == 0x0a) {
                  index++;
               }

               drawText_WordBuffer(gc, noOverlapRect, allFormattedWords);

               if (_isTruncateText && _numTruncatedLines > _maxTruncatedLines) {
                  return;
               } else {
                  newline();
               }

               if (_devY > bottom) {
                  break;
               }

            } else {

               if (c != '\t') {
                  _wordbuffer.append(c);
               }

               if (Character.isWhitespace(c) || c == '/' || c == ',' || c == '&' || c == '-') {

                  // print word

                  drawText_WordBuffer(gc, noOverlapRect, allFormattedWords);

                  if (c == '\t') {
                     _devX += _tabWidth;
                  }
               }
            }
         }
      }

      // print final buffer
      drawText_WordBuffer(gc, noOverlapRect, allFormattedWords);
   }

   /**
    * @param gc
    * @param noOverlapRect
    * @param allFormattedWords
    *           Can be <code>null</code>
    */
   private void drawText_WordBuffer(final GC gc,
                                    final Rectangle noOverlapRect,
                                    final List<FormattedWord> allFormattedWords) {

      if (_wordbuffer.length() == 0) {
         return;
      }

      final String word = _wordbuffer.toString();
      final Point wordExtent = gc.stringExtent(word);

      final int wordWidth = wordExtent.x;
      final int wordHeight = wordExtent.y;

      boolean isSkipNewLine = false;

      if (noOverlapRect != null) {

         final int max = 5;
         int current = 0;

         while (current++ < max) {

            final Rectangle wordRect = new Rectangle(_devX, _devY, wordWidth, wordHeight);

            if (wordRect.intersects(noOverlapRect)) {

               if (_linePaintedCounter < _numPaintedLines) {

                  // this line is not yet painted -> skip newline

                  _linePaintedCounter++;
                  isSkipNewLine = true;

                  break;

               } else {

                  if (_isTruncateText && _numTruncatedLines > _maxTruncatedLines) {
                     return;
                  } else {

                     newline();
                  }
               }
            }
         }

      }

      if (_devX + wordWidth > _devRightMargin) {

         // do not draw a newline on the 1st line
         if (_is1stPainted && isSkipNewLine == false) {

            // word doesn't fit on current line, so wrap
            if (_isTruncateText && _numTruncatedLines > _maxTruncatedLines) {
               return;
            } else {
               newline();
            }
         }
      }

      if (allFormattedWords != null) {

         // just keep the formatted word, painter is later on

         allFormattedWords.add(new FormattedWord(word, _devX, _devY, wordWidth, _numPaintedLines));

      } else {

         // draw formatted word

         gc.drawString(word, _devX, _devY, true);
      }

      _lastPaintedY = _devY;

      _is1stPainted = true;

      _devX += wordWidth;

      // truncate buffer
      _wordbuffer.setLength(0);
   }

   /**
    *
    * Formats the text which should be printed and returns a list with all words and there positions
    *
    * @param gc
    * @param textToPrint
    *           Text which is printed
    * @param viewportWidth
    *           Viewport width
    * @param viewportHeight
    *           Viewport height
    * @param fontHeight
    * @param noOverlapRect
    * @param isTruncateText
    * @param truncatedLines
    *
    * @return Returns a list with all formatted words
    */
   public List<FormattedWord> formatText(final GC gc,
                                         final String textToPrint,
                                         final int viewportWidth,
                                         final int viewportHeight,
                                         final int fontHeight,
                                         final Rectangle noOverlapRect,
                                         final boolean isTruncateText,
                                         final int truncatedLines) {

      _is1stPainted = false;

      final List<FormattedWord> allFormattedWords = new ArrayList<>();

      drawText(

            gc,
            textToPrint,

            0, // devX,
            0, // devY,

            viewportWidth,
            viewportHeight,
            fontHeight,
            noOverlapRect,
            isTruncateText,
            truncatedLines,
            allFormattedWords);

      return allFormattedWords;
   }

   /**
    * @return Returns the y position of the last painted text when {@link #isPainted} is
    *         <code>true</code>.
    */
   public int getLastPaintedY() {
      return _lastPaintedY;
   }

   /**
    * @return Returns <code>true</code> when the last {@link #drawText} has painted text, otherwise
    *         <code>false</code>.
    */
   public boolean isPainted() {
      return _is1stPainted;
   }

   private void newline() {

      _devX = _devLeftMargin;
      _devY += _lineHeight;

      _numPaintedLines++;
      _numTruncatedLines++;
   }

}
