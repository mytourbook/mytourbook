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

/**
 * OUT states for the import of one file
 */
public class ImportState_File {

   /**
    * Is <code>true</code> when a file could be imported successfully
    */
   public boolean isImported;

   /**
    * File name which was imported
    */
   public String  importedFileName;

   /**
    * Is <code>true</code> when {@link #isImported} is <code>false</code> but the info/error is
    * logged why it failed
    */
   public boolean isLogged;

}
