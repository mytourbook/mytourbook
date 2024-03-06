/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;

import org.eclipse.swt.SWT;

/**
 * Contains customized table/tree column properties, a column is identified by the
 * {@link #columnId}.
 *
 * @since 16.5
 */
public class ColumnProperties {

   private static final char NL = UI.NEW_LINE;

   String                    columnId;

   ValueFormat               valueFormat_Category;
   ValueFormat               valueFormat_Detail;

   /**
    * SWT.* constant for the column alignment
    *
    * <li>{@link SWT#LEAD} - 16384</li>
    * <li>{@link SWT#CENTER} - 16777216</li>
    * <li>{@link SWT#TRAIL} - 131072</li>
    */
   int                       alignment;

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof ColumnProperties)) {
         return false;
      }
      final ColumnProperties other = (ColumnProperties) obj;
      if (columnId == null) {
         if (other.columnId != null) {
            return false;
         }
      } else if (!columnId.equals(other.columnId)) {
         return false;
      }
      return true;
   }



   @Override
   public int hashCode() {

      final int prime = 31;
      int result = 1;
      result = prime * result + ((columnId == null) ? 0 : columnId.hashCode());

      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "ColumnProperties" + NL //                                      //$NON-NLS-1$

            + " columnId               = " + columnId + NL //                 //$NON-NLS-1$
            + " valueFormat_Category   = " + valueFormat_Category + NL //     //$NON-NLS-1$
            + " valueFormat_Detail     = " + valueFormat_Detail + NL //       //$NON-NLS-1$
            + " alignment              = " + ColumnManager.getAlignmentText(alignment) + NL //       //$NON-NLS-1$
      ;
   }
}
