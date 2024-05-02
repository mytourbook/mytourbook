/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;

/**
 * Text and position for a formatted word
 */
public class FormattedWord {

   private static final char NL = UI.NEW_LINE;

   public String             word;

   public int                wordWidth;

   public int                devX;
   public int                devY;

   public int                line;

   /**
    * This offset is used to paint the text right aligned
    */
   public int                lineHorizontalOffset;

   public FormattedWord(final String word,
                        final int devX,
                        final int devY,
                        final int wordWidth,
                        final int line) {

      this.word = word;

      this.devX = devX;
      this.devY = devY;

      this.wordWidth = wordWidth;
      this.line = line;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "FormattedWord" + NL //                          //$NON-NLS-1$

            + " word       = " + word + NL //                  //$NON-NLS-1$
            + " line       = " + line + NL //                  //$NON-NLS-1$
            + " offset     = " + lineHorizontalOffset + NL //  //$NON-NLS-1$
            + " wordWidth  = " + wordWidth + NL //             //$NON-NLS-1$
            + " devX       = " + devX + NL //                  //$NON-NLS-1$
//            + " devY      = " + devY + NL //                 //$NON-NLS-1$
      ;
   }
}
