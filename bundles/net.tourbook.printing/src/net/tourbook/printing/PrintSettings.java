/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

   private PaperSize        paperSize;
   private PaperOrientation paperOrientation;
   private String           completeFilePath;
   private boolean          isPrintMarkers;
   private boolean          isPrintDescription;
   private boolean          isOverwriteFiles;
   private boolean          isOpenFile;

   public String getCompleteFilePath() {
      return completeFilePath;
   }

   public PaperOrientation getPaperOrientation() {
      return paperOrientation;
   }

   public PaperSize getPaperSize() {
      return paperSize;
   }

   public boolean isOverwriteFiles() {
      return isOverwriteFiles;
   }

   public boolean isPrintDescription() {
      return isPrintDescription;
   }

   public boolean isPrintMarkers() {
      return isPrintMarkers;
   }

   public void setCompleteFilePath(final String completeFilePath) {
      this.completeFilePath = completeFilePath;
   }

   public void setOverwriteFiles(final boolean isOverwriteFiles) {
      this.isOverwriteFiles = isOverwriteFiles;
   }

   public void setPaperOrientation(final PaperOrientation paperOrientation) {
      this.paperOrientation = paperOrientation;
   }

   public void setPaperSize(final PaperSize paperSize) {
      this.paperSize = paperSize;
   }

   public void setPrintDescription(final boolean isPrintDescription) {
      this.isPrintDescription = isPrintDescription;
   }

   public void setPrintMarkers(final boolean isPrintMarkers) {
      this.isPrintMarkers = isPrintMarkers;
   }

   public boolean isOpenFile() {
      return isOpenFile;
   }

   public void setOpenFile(final boolean isOpenFile) {
      this.isOpenFile = isOpenFile;
   }

}
