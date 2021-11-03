/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.importdata;

import net.tourbook.common.UI;

public class TagWithNotes {

   private static final char NL = UI.NEW_LINE;

   public String             tagName;
   public String             tagNotes;
   public String             tagNotes_ContainedId;

   public TagWithNotes(final String tagName,
                       final String tagNotes,
                       final String tagNotes_ContainedId) {

      this.tagName = tagName;
      this.tagNotes = tagNotes;
      this.tagNotes_ContainedId = tagNotes_ContainedId;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TagWithNotes" + NL //                                    //$NON-NLS-1$

            + "[" + NL //                                               //$NON-NLS-1$

            + "tagName              =" + tagName + NL //                //$NON-NLS-1$
            + "tagNotes_ContainedId =" + tagNotes_ContainedId + NL //   //$NON-NLS-1$
            + "tagNotes             =" + tagNotes + NL //               //$NON-NLS-1$

            + "]" + NL //                                               //$NON-NLS-1$
      ;
   }

}
