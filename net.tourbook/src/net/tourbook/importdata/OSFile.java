/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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

import java.nio.file.Path;

public class OSFile {

	private Path	path;
	private String	fileName;
	//
	public long		size;
	public long		modifiedTime;

	/** When <code>true</code>, this file is already moved and exists in the backup folder. */
	public boolean	isBackupImportFile;

	@SuppressWarnings("unused")
	private OSFile() {}

	public OSFile(final Path path) {

		this.path = path;
		this.fileName = path.getFileName().toString();
	}

	/**
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * <p>
	 * Equals is done only with the filename because files can be in different folders.
	 * <p>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 */
	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final OSFile other = (OSFile) obj;
		if (fileName == null) {
			if (other.fileName != null) {
				return false;
			}
		} else if (!fileName.equals(other.fileName)) {
			return false;
		}
		return true;
	}

	public String getFileName() {
		return fileName;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
		return result;
	}

	@Override
	public String toString() {

		return "OSFile [" //$NON-NLS-1$
				+ ("fileName=" + fileName + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("path=" + path + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("size=" + size + ", ") //$NON-NLS-1$ //$NON-NLS-2$
//				+ ("modifiedTime=" + modifiedTime + ", ") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("isBackupFile=" + isBackupImportFile) //$NON-NLS-1$
				+ "]"; //$NON-NLS-1$
	}

}
