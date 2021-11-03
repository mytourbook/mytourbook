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

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.UI;

/**
 * OUT states for the import of one file
 */
public class ImportState_File {

   private static final char NL = UI.NEW_LINE;

   /**
    * Is <code>true</code> when the import for this file is done, successfully or not which is set
    * in {@link #isFileImportedWithValidData}
    */
   public boolean            isImportDone;

   /**
    * Is <code>true</code> when a file could be imported successfully
    */
   public boolean            isFileImportedWithValidData;

   /**
    * Is <code>true</code> when the import is logged for this file, this prevents additional default
    * logging
    */
   public boolean            isImportLogged;

   /**
    * File name which was imported
    */
   public String             importedFileName;

   /*
    * Polar specific fields
    */
   private ArrayList<String>      _additionalImportedFiles = new ArrayList<>();
   private HashMap<Long, Integer> _tourSportMap            = new HashMap<>();

   /**
    * @return Returns a list of files which are also imported additional to the selected imported
    *         file or <code>null</code> otherwise.
    */
   public ArrayList<String> getAdditionalImportedFiles() {

      return _additionalImportedFiles;
   }

   public HashMap<Long, Integer> getTourSportMap() {
      return _tourSportMap;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "ImportState_File" + NL //                                                     //$NON-NLS-1$

            + "[" + NL //                                                                    //$NON-NLS-1$

            + "isImportDone                  =" + isImportDone + NL //                       //$NON-NLS-1$
            + "isFileImportedWithValidData   =" + isFileImportedWithValidData + NL //        //$NON-NLS-1$
            + "importedFileName              =" + importedFileName + NL //                   //$NON-NLS-1$
            + "_additionalImportedFiles      =" + _additionalImportedFiles + NL //           //$NON-NLS-1$

            + "]"; //                                                                        //$NON-NLS-1$
   }

}
