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
package net.tourbook.photo;

import java.io.File;
import java.io.FileFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIFolderFolder extends TVIFolder {

	File						_treeItemFolder;

	private File[]				_folderList;

	boolean						_isVolume;

	int							_folderCounter;
	int							_fileCounter;

	private final FileFilter	_folderFilter;

	private boolean				_isFolderChecked;

	{
		_folderFilter = new FileFilter() {
			@Override
			public boolean accept(final File pathname) {

				// get only visible folder
				if (pathname.isDirectory() && pathname.isHidden() == false) {

					// count folder
					_folderCounter++;

					return true;

				} else {
					// count files
					_fileCounter++;
				}

				return false;
			}
		};
	}

	public TVIFolderFolder(final TreeViewer tagViewer, final File folder, final boolean isVolume) {

		super(tagViewer);

		_treeItemFolder = folder;
		_isVolume = isVolume;
	}

	@Override
	protected void fetchChildren() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		if (_folderList != null) {

			PicDirView.sortFiles(_folderList);

			for (final File childFolder : _folderList) {
				addChild(new TVIFolderFolder(_folderViewer, childFolder, false));
			}
		}
	}

	int getFileCounter() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _fileCounter;
	}

	public int getFolderCounter() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _folderCounter;
	}

	@Override
	public boolean hasChildren() {

		if (_isFolderChecked == false) {
			readFolderList();
		}

		return _folderList == null ? false : _folderList.length > 0;
	}

	private void readFolderList() {

		_folderList = _treeItemFolder.listFiles(_folderFilter);

		_isFolderChecked = true;
	}

	@Override
	protected void remove() {}

	@Override
	public String toString() {
		return "TVIFolderFolder: " + _treeItemFolder; //$NON-NLS-1$
	}

}
