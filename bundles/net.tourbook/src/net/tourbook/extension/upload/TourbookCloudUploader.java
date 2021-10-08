/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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
package net.tourbook.extension.upload;

import java.util.List;

import net.tourbook.data.TourData;

public abstract class TourbookCloudUploader {

   private String CLOUD_UPLOADER_ID;
   private String CLOUD_UPLOADER_NAME;

   protected TourbookCloudUploader(final String id, final String name) {
      CLOUD_UPLOADER_ID = id;
      CLOUD_UPLOADER_NAME = name;
   }

   public String getName() {
      return CLOUD_UPLOADER_NAME;
   }

   protected abstract boolean isReady();

   public abstract void uploadTours(List<TourData> selectedTours);
}
