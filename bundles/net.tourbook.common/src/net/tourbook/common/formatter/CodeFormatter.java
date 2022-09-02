/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.common.formatter;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.RGBA;

/**
 * Centralize code formatting
 */
public class CodeFormatter {

   public static String RGB(final RGB rgb) {

      return String.format("new RGB(0x%x, 0x%x, 0x%x)", //$NON-NLS-1$

            rgb.red,
            rgb.green,
            rgb.blue);
   }

   public static String RGBA(final RGBA rgba) {

      return String.format("new RGBA(0x%x, 0x%x, 0x%x, 0x%x)", //$NON-NLS-1$

            rgba.rgb.red,
            rgba.rgb.green,
            rgba.rgb.blue,
            rgba.alpha);
   }

}
