/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal;

class FolderLoader {

	TVIFolderFolder	loaderFolderItem;
	boolean			isExpandFolder;

	FolderLoader(final TVIFolderFolder folderItem, final boolean isExpandFolder) {
		this.loaderFolderItem = folderItem;
		this.isExpandFolder = isExpandFolder;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FolderLoader)) {
			return false;
		}
		final FolderLoader other = (FolderLoader) obj;
		if (loaderFolderItem == null) {
			if (other.loaderFolderItem != null) {
				return false;
			}
		} else if (!loaderFolderItem.equals(other.loaderFolderItem)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((loaderFolderItem == null) ? 0 : loaderFolderItem.hashCode());
		return result;
	}
}
