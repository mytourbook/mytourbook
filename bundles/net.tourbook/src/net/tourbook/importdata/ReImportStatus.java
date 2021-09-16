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

import java.util.concurrent.atomic.AtomicBoolean;

import net.tourbook.common.UI;

/**
 * Keeps the different states which can occur when a re-import is performed.
 */
public class ReImportStatus {

   private static final char  NL                       = UI.NEW_LINE;

   /**
    * Is <code>true</code> when at least one tour was reimported
    */
   public final AtomicBoolean isAnyTourReImported      = new AtomicBoolean(false);

   public final AtomicBoolean isCanceled_WholeReimport = new AtomicBoolean(false);

   /**
    * Is <code>true</code> when the invalid import file location is canceled by the user.
    */
   public boolean             isCanceled_ByUser_TheFileLocationDialog;

   /**
    * Is <code>true</code> when the invalid import file location is canceled by the user also for
    * any subsequent files.
    */
   public boolean             isCanceled_ByUser_SkipAllInvalidFiles;

   boolean                    isCanceled_Auto_ImportFilePathIsEmpty;
   boolean                    isCanceled_Auto_TheFileLocationDialog;

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "ReImportStatus [" + NL //$NON-NLS-1$

            + "isAnyReImported                           =" + isAnyTourReImported + NL //$NON-NLS-1$
            + "isCanceled_WholeReimport                  =" + isCanceled_WholeReimport + NL //$NON-NLS-1$

            + "isCanceled_ByUser_TheFileLocationDialog   =" + isCanceled_ByUser_TheFileLocationDialog + NL //$NON-NLS-1$
            + "isCanceled_Auto_ImportFilePathIsEmpty     =" + isCanceled_Auto_ImportFilePathIsEmpty + NL //$NON-NLS-1$
            + "isCanceled_Auto_TheFileLocationDialog     =" + isCanceled_Auto_TheFileLocationDialog + NL //$NON-NLS-1$

            + "]"; //$NON-NLS-1$
   }
}
