/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.extension.download;

public abstract class TourbookCloudDownloader {

   private String CLOUD_DOWNLOADER_ID;
   private String CLOUD_DOWNLOADER_NAME;
   private String CLOUD_DOWNLOADER_DESCRIPTION;
   private String CLOUD_DOWNLOADER_ICON_URL;

   public TourbookCloudDownloader(final String id, final String name, final String description, final String iconUrl) {
      CLOUD_DOWNLOADER_ID = id;
      CLOUD_DOWNLOADER_NAME = name;
      CLOUD_DOWNLOADER_DESCRIPTION = description;
      CLOUD_DOWNLOADER_ICON_URL = iconUrl;
   }

   public abstract void downloadTours();

   public String getDescription() {
      return CLOUD_DOWNLOADER_DESCRIPTION;
   }

   public String getIconUrl() {
      return CLOUD_DOWNLOADER_ICON_URL;
   }

   public String getId() {
      return CLOUD_DOWNLOADER_ID;
   }

   public String getName() {
      return CLOUD_DOWNLOADER_NAME;
   }

   protected abstract boolean isReady();
}
