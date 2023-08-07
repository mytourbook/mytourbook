/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;

public class SimpleColorStyler extends Styler {

   private final Color _foregroundColor;
   private final Color _backgroundColor;

   public SimpleColorStyler(final Color foregroundColor,
                            final Color backgroundColor) {

      _foregroundColor = foregroundColor;
      _backgroundColor = backgroundColor;
   }

   @Override
   public void applyStyles(final TextStyle textStyle) {

      if (_foregroundColor != null) {
         textStyle.foreground = _foregroundColor;
      }

      if (_backgroundColor != null) {
         textStyle.background = _backgroundColor;
      }
   }
}