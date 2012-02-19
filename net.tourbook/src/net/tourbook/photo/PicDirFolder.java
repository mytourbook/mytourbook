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
import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * The original source code is in org.eclipse.swt.examples.fileviewer
 */
class PicDirFolder {

	private PicDirImages		_picDirImages;

	private File				currentDirectory			= null;

	private boolean				initial						= true;

	// File: File associated with tree item
	private static final String	TREEITEMDATA_FILE			= "TreeItem.file";

	// Image: shown when item is expanded
	private static final String	TREEITEMDATA_IMAGEEXPANDED	= "TreeItem.imageExpanded";

	// Image: shown when item is collapsed
	private static final String	TREEITEMDATA_IMAGECOLLAPSED	= "TreeItem.imageCollapsed";

	// Object: if not present or null then the item has not been populated
	private static final String	TREEITEMDATA_STUB			= "TreeItem.stub";

	/*
	 * UI controls
	 */
	private Display				_display;
	private Tree				tree;

	PicDirFolder(final PicDirImages picDirImages) {
		_picDirImages = picDirImages;
	}

	/**
	 * Foreign method: removes all children of a TreeItem.
	 * 
	 * @param treeItem
	 *            the TreeItem
	 */
	private static void treeItemRemoveAll(final TreeItem treeItem) {
		final TreeItem[] children = treeItem.getItems();
		for (int i = 0; i < children.length; ++i) {
			children[i].dispose();
		}
	}

	void createUI(final Composite parent) {

		_display = parent.getDisplay();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
		{
			createUI_10_TreeView(container);
		}
	}

	/**
	 * Creates the file tree view.
	 * 
	 * @param parent
	 *            the parent control
	 */
	private void createUI_10_TreeView(final Composite parent) {

		tree = new Tree(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);

		tree.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(final SelectionEvent event) {
				final TreeItem[] selection = tree.getSelection();
				if (selection != null && selection.length != 0) {
					final TreeItem item = selection[0];
					item.setExpanded(true);
					treeExpandItem(item);
				}
			}

			public void widgetSelected(final SelectionEvent event) {
				final TreeItem[] selection = tree.getSelection();
				if (selection != null && selection.length != 0) {
					final TreeItem item = selection[0];
					final File file = (File) item.getData(TREEITEMDATA_FILE);

					onSelectedDirectory(file);
				}
			}
		});

		tree.addTreeListener(new TreeAdapter() {
			@Override
			public void treeCollapsed(final TreeEvent event) {
				final TreeItem item = (TreeItem) event.item;
				final Image image = (Image) item.getData(TREEITEMDATA_IMAGECOLLAPSED);
				if (image != null) {
					item.setImage(image);
				}
			}

			@Override
			public void treeExpanded(final TreeEvent event) {
				final TreeItem item = (TreeItem) event.item;
				final Image image = (Image) item.getData(TREEITEMDATA_IMAGEEXPANDED);
				if (image != null) {
					item.setImage(image);
				}
				treeExpandItem(item);
			}

		});
	}

	/**
	 * Gets filesystem root entries
	 * 
	 * @return an array of Files corresponding to the root directories on the platform, may be empty
	 *         but not null
	 */
	private File[] getRoots() {
		/*
		 * On JDK 1.22 only...
		 */
		// return File.listRoots();

		/*
		 * On JDK 1.1.7 and beyond... -- PORTABILITY ISSUES HERE --
		 */
		if (System.getProperty("os.name").indexOf("Windows") != -1) {
			final Vector<File> list = new Vector<File>();
//			list.add(new File(DRIVE_A));
//			list.add(new File(DRIVE_B));
			for (char i = 'c'; i <= 'z'; ++i) {
				final File drive = new File(i + ":" + File.separator);
				if (drive.isDirectory() && drive.exists()) {
					list.add(drive);
					if (initial && i == 'c') {
						currentDirectory = drive;
						initial = false;
					}
				}
			}
			final File[] roots = list.toArray(new File[list.size()]);
			PicDirView.sortFiles(roots);
			return roots;
		}
		final File root = new File(File.separator);
		if (initial) {
			currentDirectory = root;
			initial = false;
		}
		return new File[] { root };
	}

	File getSelectedFolder() {
		return currentDirectory;
	}

	Tree getTree() {
		return tree;
	}

	void initialRefresh(final String folderPath) {

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				_picDirImages.showImages(currentDirectory);

				final File[] roots = getRoots();

				/*
				 * Tree view: Refreshes information about any files in the list and their children.
				 */
				treeRefresh(roots);

				// Remind everyone where we are in the filesystem
				File dir = currentDirectory;
				currentDirectory = null;

				if (folderPath != null) {
					final File folderFile = new File(folderPath);
					if (folderFile.isDirectory()) {
						dir = folderFile;
					}
				}

				onSelectedDirectory(dir);
			}
		});
	}

	/**
	 * Notifies the application components that a new current directory has been selected
	 * 
	 * @param dir
	 *            the directory that was selected, null is ignored
	 */
	private void onSelectedDirectory(final File dir) {

		if (dir == null) {
			return;
		}
		if (currentDirectory != null && dir.equals(currentDirectory)) {
			return;
		}

		currentDirectory = dir;

		/*
		 * image gallery: displays the contents of the selected directory
		 */
		_picDirImages.showImages(dir);

		/*
		 * Tree view: If not already expanded, recursively expands the parents of the specified
		 * directory until it is visible.
		 */

		final ArrayList<File> path = new ArrayList<File>();
		File dirRunnable = dir;

		// Build a stack of paths from the root of the tree
		while (dirRunnable != null) {
			path.add(dirRunnable);
			dirRunnable = dirRunnable.getParentFile();
		}
		// Recursively expand the tree to get to the specified directory
		TreeItem[] items = tree.getItems();
		TreeItem lastItem = null;
		for (int i = path.size() - 1; i >= 0; --i) {
			final File pathElement = path.get(i);

			// Search for a particular File in the array of tree items
			// No guarantee that the items are sorted in any recognizable fashion, so we'll
			// just sequential scan.  There shouldn't be more than a few thousand entries.
			TreeItem item = null;
			for (int k = 0; k < items.length; ++k) {
				item = items[k];
				if (item.isDisposed()) {
					continue;
				}
				final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
				if (itemFile != null && itemFile.equals(pathElement)) {
					break;
				}
			}
			if (item == null) {
				break;
			}
			lastItem = item;
			if (i != 0 && !item.getExpanded()) {
				treeExpandItem(item);
				item.setExpanded(true);
			}
			items = item.getItems();
		}

		tree.setSelection((lastItem != null) ? //
				new TreeItem[] { lastItem }
				: new TreeItem[0]);
	}

	void setColor(final Color fgColor, final Color bgColor) {

		tree.setForeground(fgColor);
		tree.setBackground(bgColor);
	}

	/*
	 * This worker updates the table with file information in the background. <p> Implementation
	 * notes: <ul> <li> It is designed such that it can be interrupted cleanly. <li> It uses
	 * asyncExec() in some places to ensure that SWT Widgets are manipulated in the right thread.
	 * Exclusive use of syncExec() would be inappropriate as it would require a pair of context
	 * switches between each table update operation. </ul> </p>
	 */

	/**
	 * Handles expand events on a tree item.
	 * 
	 * @param item
	 *            the TreeItem to fill in
	 */
	private void treeExpandItem(final TreeItem item) {

		final Object stub = item.getData(TREEITEMDATA_STUB);
		if (stub == null) {

			BusyIndicator.showWhile(_display, new Runnable() {
				public void run() {
					treeRefreshItem(item, true);
				}
			});
		}
	}

	/**
	 * Initializes a folder item.
	 * 
	 * @param item
	 *            the TreeItem to initialize
	 * @param folder
	 *            the File associated with this TreeItem
	 */
	private void treeInitFolder(final TreeItem item, final File folder) {
		item.setText(folder.getName());
//		item.setImage(iconCache.stockImages[iconCache.iconClosedFolder]);
		item.setData(TREEITEMDATA_FILE, folder);
//		item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenFolder]);
//		item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedFolder]);
	}

	/**
	 * Initializes a volume item.
	 * 
	 * @param item
	 *            the TreeItem to initialize
	 * @param volume
	 *            the File associated with this TreeItem
	 */
	private void treeInitVolume(final TreeItem item, final File volume) {
		item.setText(volume.getPath());
//		item.setImage(iconCache.stockImages[iconCache.iconClosedDrive]);
		item.setData(TREEITEMDATA_FILE, volume);
//		item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenDrive]);
//		item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedDrive]);
	}

	/**
	 * Traverse the entire tree and update only what has changed.
	 * 
	 * @param roots
	 *            the root directory listing
	 */
	private void treeRefresh(final File[] masterFiles) {

		final TreeItem[] items = tree.getItems();
		int masterIndex = 0;
		int itemIndex = 0;

		for (int i = 0; i < items.length; ++i) {
			final TreeItem item = items[i];
			final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
			if ((itemFile == null) || (masterIndex == masterFiles.length)) {
				// remove bad item or placeholder
				item.dispose();
				continue;
			}
			final File masterFile = masterFiles[masterIndex];
			final int compare = PicDirView.compareFiles(masterFile, itemFile);
			if (compare == 0) {
				// same file, update it
				treeRefreshItem(item, false);
				++itemIndex;
				++masterIndex;
			} else if (compare < 0) {
				// should appear before file, insert it
				final TreeItem newItem = new TreeItem(tree, SWT.NONE, itemIndex);
				treeInitVolume(newItem, masterFile);
				new TreeItem(newItem, SWT.NONE); // placeholder child item to get "expand" button
				++itemIndex;
				++masterIndex;
				--i;
			} else {
				// should appear after file, delete stale item
				item.dispose();
			}
		}

		for (; masterIndex < masterFiles.length; ++masterIndex) {
			final File masterFile = masterFiles[masterIndex];
			final TreeItem newItem = new TreeItem(tree, SWT.NONE);
			treeInitVolume(newItem, masterFile);
			new TreeItem(newItem, SWT.NONE); // placeholder child item to get "expand" button
		}
	}

	/**
	 * Traverse an item in the tree and update only what has changed.
	 * 
	 * @param dirItem
	 *            the tree item of the directory
	 * @param forcePopulate
	 *            true iff we should populate non-expanded items as well
	 */
	private void treeRefreshItem(final TreeItem dirItem, final boolean forcePopulate) {

		final File dir = (File) dirItem.getData(TREEITEMDATA_FILE);

		if (!forcePopulate && !dirItem.getExpanded()) {
			// Refresh non-expanded item
			if (dirItem.getData(TREEITEMDATA_STUB) != null) {
				treeItemRemoveAll(dirItem);
				new TreeItem(dirItem, SWT.NONE); // placeholder child item to get "expand" button
				dirItem.setData(TREEITEMDATA_STUB, null);
			}
			return;
		}
		// Refresh expanded item
		dirItem.setData(TREEITEMDATA_STUB, this); // clear stub flag

		/* Get directory listing */
		final File[] subFiles = (dir != null) ? PicDirView.getDirectoryList(dir) : null;

		if (subFiles == null || subFiles.length == 0) {
			/* Error or no contents */
			treeItemRemoveAll(dirItem);
			dirItem.setExpanded(false);
			return;
		}

		/* Refresh sub-items */
		final TreeItem[] items = dirItem.getItems();
		final File[] masterFiles = subFiles;
		int masterIndex = 0;
		int itemIndex = 0;
		File masterFile = null;
		for (int i = 0; i < items.length; ++i) {
			while ((masterFile == null) && (masterIndex < masterFiles.length)) {
				masterFile = masterFiles[masterIndex++];
				if (!masterFile.isDirectory()) {
					masterFile = null;
				}
			}

			final TreeItem item = items[i];
			final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
			if ((itemFile == null) || (masterFile == null)) {
				// remove bad item or placeholder
				item.dispose();
				continue;
			}
			final int compare = PicDirView.compareFiles(masterFile, itemFile);
			if (compare == 0) {
				// same file, update it
				treeRefreshItem(item, false);
				masterFile = null;
				++itemIndex;
			} else if (compare < 0) {
				// should appear before file, insert it
				final TreeItem newItem = new TreeItem(dirItem, SWT.NONE, itemIndex);
				treeInitFolder(newItem, masterFile);
				new TreeItem(newItem, SWT.NONE); // add a placeholder child item so we get the "expand" button
				masterFile = null;
				++itemIndex;
				--i;
			} else {
				// should appear after file, delete stale item
				item.dispose();
			}
		}
		while ((masterFile != null) || (masterIndex < masterFiles.length)) {
			if (masterFile != null) {
				final TreeItem newItem = new TreeItem(dirItem, SWT.NONE);
				treeInitFolder(newItem, masterFile);
				new TreeItem(newItem, SWT.NONE); // add a placeholder child item so we get the "expand" button
				if (masterIndex == masterFiles.length) {
					break;
				}
			}
			masterFile = masterFiles[masterIndex++];
			if (!masterFile.isDirectory()) {
				masterFile = null;
			}
		}
	}

}
