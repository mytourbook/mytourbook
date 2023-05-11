/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.printing;

public class PrintSettings {

   private PaperSize        _paperSize;
   private PaperOrientation _paperOrientation;
   private String           _completeFilePath;
   private boolean          _isPrintMarkers;
   private boolean          _isPrintDescription;
   private boolean          _isOverwriteFiles;
   private boolean          _isOpenFile;

   public String getCompleteFilePath() {
      return _completeFilePath;
   }

   public PaperOrientation getPaperOrientation() {
      return _paperOrientation;
   }

   public PaperSize getPaperSize() {
      return _paperSize;
   }

   public boolean isOpenFile() {
      return _isOpenFile;
   }

   public boolean isOverwriteFiles() {
      return _isOverwriteFiles;
   }

   public boolean isPrintDescription() {
      return _isPrintDescription;
   }

   public boolean isPrintMarkers() {
      return _isPrintMarkers;
   }

   public void setCompleteFilePath(final String completeFilePath) {
      _completeFilePath = completeFilePath;
   }

   public void setOpenFile(final boolean isOpenFile) {
      _isOpenFile = isOpenFile;
   }

   public void setOverwriteFiles(final boolean isOverwriteFiles) {
      _isOverwriteFiles = isOverwriteFiles;
   }

   public void setPaperOrientation(final PaperOrientation paperOrientation) {
      _paperOrientation = paperOrientation;
   }

   public void setPaperSize(final PaperSize paperSize) {
      _paperSize = paperSize;
   }

   public void setPrintDescription(final boolean isPrintDescription) {
      _isPrintDescription = isPrintDescription;
   }

   public void setPrintMarkers(final boolean isPrintMarkers) {
      _isPrintMarkers = isPrintMarkers;
   }

}
