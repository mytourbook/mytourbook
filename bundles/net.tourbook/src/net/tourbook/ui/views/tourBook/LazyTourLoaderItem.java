/*******************************************************************************
 * Copyright (C) 2020, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.tourbook.common.UI;

public class LazyTourLoaderItem {

   private static final char NL               = UI.NEW_LINE;
   public int                sqlOffset;
   public int                fetchKey;

   List<Integer>             requestedIndices = Collections.synchronizedList(new ArrayList<Integer>());

   public LazyTourLoaderItem() {

   }

   @Override
   public String toString() {

      final int maxLen = 5;

      final List<Integer> list = requestedIndices != null
            ? requestedIndices.subList(0, Math.min(requestedIndices.size(), maxLen))
            : null;

      return UI.EMPTY_STRING

            + "LazyTourLoaderItem" + NL //                                    //$NON-NLS-1$

            + " sqlOffset        = " + sqlOffset + NL //                      //$NON-NLS-1$
            + " fetchKey         = " + fetchKey + NL //                       //$NON-NLS-1$
            + " requestedIndices = " + list + NL //                           //$NON-NLS-1$

      ;
   }

}
