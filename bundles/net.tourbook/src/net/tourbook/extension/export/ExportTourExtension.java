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
package net.tourbook.extension.export;

import java.util.List;

import net.tourbook.data.TourData;

import org.eclipse.jface.resource.ImageDescriptor;

public abstract class ExportTourExtension {

   private String          _exportId;
   private String          _visibleName;
   private String          _fileExtension;
   private ImageDescriptor _imageDescriptor;

   /**
    * Exports the tour in the {@link TourData} list. If only one tour is exported, the values of
    * tourStartIndex and tourEndIndex is the range which points are exported, when the index is -1,
    * the whole tour is exported.
    *
    * @param tourDataList
    * @param tourStartIndex
    * @param tourEndIndex
    */
   public abstract void exportTours(List<TourData> tourDataList, int tourStartIndex, int tourEndIndex);

   public String getExportId() {
      return _exportId;
   }

   public String getFileExtension() {
      return _fileExtension;
   }

   public ImageDescriptor getImageDescriptor() {
      return _imageDescriptor;
   }

   public String getVisibleName() {
      return _visibleName;
   }

   public void setExportId(final String exportId) {
      _exportId = exportId;
   }

   public void setFileExtension(final String fileExtension) {
      _fileExtension = fileExtension;
   }

   public void setImageDescriptor(final ImageDescriptor imageDescriptor) {
      _imageDescriptor = imageDescriptor;
   }

   public void setVisibleName(final String visibleName) {
      _visibleName = visibleName;
   }

   @Override
   public String toString() {

      return new StringBuilder()//
            .append("id: ")//$NON-NLS-1$
            .append(_exportId)
            .append(" \t") //$NON-NLS-1$
            //
            .append("name: ") //$NON-NLS-1$
            .append(_visibleName)
            .append(" \t") //$NON-NLS-1$
            //
            .append("extension: ")//$NON-NLS-1$
            .append(_fileExtension)
            .append(" \t") //$NON-NLS-1$
            //
            .toString();
   }
}
