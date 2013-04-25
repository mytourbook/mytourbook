/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import org.eclipse.core.runtime.IPath;

public class ImagePathReplacement {

	public String	oldImageFilePathName;
	public IPath	newImageFilePathName;

	public ImagePathReplacement(final String oldImageFilePathName, final IPath newImageFilePathName) {

		this.oldImageFilePathName = oldImageFilePathName;
		this.newImageFilePathName = newImageFilePathName;
	}

	@Override
	public String toString() {
		return String.format(
				"\nImagePathReplacement\n   oldImageFilePathName=%s\n   newImageFilePathName=%s\n",
				oldImageFilePathName,
				newImageFilePathName.toOSString());
	}
}
