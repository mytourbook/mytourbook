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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoGalleryProvider;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.PhotosWithExifSelection;
import net.tourbook.photo.PicDirView;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;

/**
 */
public class PicDirImages implements IPhotoGalleryProvider {

	private static final int						MAX_HISTORY_ENTRIES			= 500;

	private static final String						STATE_FOLDER_HISTORY		= "STATE_FOLDER_HISTORY";		//$NON-NLS-1$
	private static final String						STATE_IS_SHOW_ONLY_PHOTOS	= "STATE_IS_SHOW_ONLY_PHOTOS";	//$NON-NLS-1$

	private IDialogSettings							_state;

	private int										_selectedHistoryIndex;
	private ArrayList<String>						_folderHistory				= new ArrayList<String>();

	private PicDirFolder							_picDirFolder;

	private boolean									_isComboKeyPressed;

	/**
	 * Is <code>true</code> when folders and gallery photos are displayed, is <code>false</code>
	 * when only photos are displayed.
	 */
	private boolean									_isShowOnlyPhotos;

	private ActionNavigateHistoryBackward			_actionNavigateBackward;
	private ActionNavigateHistoryForward			_actionNavigateForward;
	private ActionClearNavigationHistory			_actionClearNavigationHistory;
	private ActionRemoveInvalidFoldersFromHistory	_actionRemoveInvalidFoldersFromHistory;
	private ActionSortFolderHistory					_actionSortFolderHistory;
	private ActionToggleFolderGallery				_actionToggleFolderGallery;

	private PicDirView								_picDirView;

	/*
	 * UI controls
	 */
	private Display									_display;

	private Combo									_comboHistory;
	private ToolBar									_galleryToolbar;
	private PhotoGallery							_photoGallery;

	public PicDirImages(final PicDirView picDirView, final IDialogSettings state) {

		_picDirView = picDirView;
		_state = state;
	}

	void actionClearHistory() {

		final String selectedFolder = _comboHistory.getText();

		_comboHistory.removeAll();
		_comboHistory.add(selectedFolder);
		_comboHistory.select(0);

		_folderHistory.clear();
		_folderHistory.add(selectedFolder);

		_actionClearNavigationHistory.setEnabled(false);
		_actionRemoveInvalidFoldersFromHistory.setEnabled(false);
		_actionNavigateBackward.setEnabled(false);
		_actionNavigateForward.setEnabled(false);
	}

	void actionNavigateBackward() {

		final int historySize = _folderHistory.size();
		if (_selectedHistoryIndex >= historySize - 1) {

			// last entry is already selected

			_selectedHistoryIndex = historySize - 1;
			_actionNavigateBackward.setEnabled(false);

			return;
		}

		_selectedHistoryIndex++;

		// select combo history
		_comboHistory.select(_selectedHistoryIndex);

		// enabel/disable history navigation
		_actionNavigateBackward.setEnabled(_selectedHistoryIndex < historySize - 1);
		_actionNavigateForward.setEnabled(true);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				final String prevFolderPathName = _folderHistory.get(_selectedHistoryIndex);
				final boolean isFolderAvailable = _picDirFolder.selectFolder(prevFolderPathName, false, true, false);

				if (isFolderAvailable == false) {
					removeInvalidFolder(prevFolderPathName);
				}
			}
		});
	}

	void actionNavigateForward() {

		final int historySize = _folderHistory.size();
		if (_selectedHistoryIndex == 0) {

			// first entry is already selected

			_actionNavigateForward.setEnabled(false);

			return;
		}

		_selectedHistoryIndex--;

		// select combo history
		_comboHistory.select(_selectedHistoryIndex);

		// enabel/disable history navigation
		_actionNavigateBackward.setEnabled(historySize > 1);
		_actionNavigateForward.setEnabled(_selectedHistoryIndex > 0);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				final String prevFolderPathName = _folderHistory.get(_selectedHistoryIndex);
				final boolean isFolderAvailable = _picDirFolder.selectFolder(prevFolderPathName, false, true, false);

				if (isFolderAvailable == false) {
					removeInvalidFolder(prevFolderPathName);
				}
			}
		});
	}

	void actionRemoveInvalidFolders() {

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {
				removeInvalidFolders();
			}
		});
	}

	void actionShowNavigationHistory() {

		_comboHistory.setFocus();

		// this is not working with osx: https://bugs.eclipse.org/bugs/show_bug.cgi?id=300979
		_comboHistory.setListVisible(true);
	}

	void actionSortFolderHistory() {

		final int selectedIndex = _comboHistory.getSelectionIndex();
		String selectedFolder = null;

		if (selectedIndex != -1) {
			selectedFolder = _comboHistory.getItem(selectedIndex);
		}

		Collections.sort(_folderHistory);

		_comboHistory.removeAll();

		int newSelectedIndex = -1;

		for (int folderIndex = 0; folderIndex < _folderHistory.size(); folderIndex++) {

			final String folder = _folderHistory.get(folderIndex);

			if (newSelectedIndex == -1 && folder.equals(selectedFolder)) {
				newSelectedIndex = folderIndex;
			}

			_comboHistory.add(folder);
		}

		if (selectedIndex == -1) {
			_comboHistory.select(0);
		} else {
			_comboHistory.select(newSelectedIndex);
		}
	}

	void actionToggleFolderGallery() {

		_isShowOnlyPhotos = _isShowOnlyPhotos ? false : true;

		updateUI_Action_FolderGallery();
	}

	private void createActions() {

		_actionNavigateBackward = new ActionNavigateHistoryBackward(this, _picDirView);
		_actionNavigateForward = new ActionNavigateHistoryForward(this, _picDirView);

		// this action activates the shortcut key <Ctrl><Shift>H but the action is not displayed
		new ActionNavigateShowHistory(this, _picDirView);

		_actionClearNavigationHistory = new ActionClearNavigationHistory(this);
		_actionRemoveInvalidFoldersFromHistory = new ActionRemoveInvalidFoldersFromHistory(this);
		_actionSortFolderHistory = new ActionSortFolderHistory(this);

		_actionToggleFolderGallery = new ActionToggleFolderGallery(this);
	}

	public void createUI(final Composite parent, final PicDirFolder picDirFolder) {

		_display = parent.getDisplay();
		_picDirFolder = picDirFolder;

		_photoGallery = new PhotoGallery(_state);

		_photoGallery.setShowCustomActionBar();
		_photoGallery.setShowThumbnailSize();

		_photoGallery.createPhotoGallery(parent, SWT.V_SCROLL | SWT.MULTI, this);

		createActions();

		_photoGallery.createActionBar();

		final Composite galleryActionBarContainer = _photoGallery.getCustomActionBarContainer();
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(galleryActionBarContainer);
//		galleryActionBarContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			createUI_20_GalleryToolbars(galleryActionBarContainer);
			createUI_30_ComboHistory(galleryActionBarContainer);
		}
	}

	/**
	 * fill gallery actionbar
	 * 
	 * @param galleryActionBarContainer
	 */
	private void createUI_20_GalleryToolbars(final Composite galleryActionBarContainer) {

		/*
		 * toolbar actions
		 */
		_galleryToolbar = new ToolBar(galleryActionBarContainer, SWT.FLAT);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(_galleryToolbar);

		final ToolBarManager tbm = new ToolBarManager(_galleryToolbar);

		tbm.add(_actionToggleFolderGallery);
		tbm.add(_actionNavigateBackward);
		tbm.add(_actionNavigateForward);

		tbm.update(true);
	}

	/**
	 * combo: path history
	 */
	private void createUI_30_ComboHistory(final Composite parent) {

		_comboHistory = new Combo(parent, SWT.SIMPLE | SWT.DROP_DOWN);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.align(SWT.FILL, SWT.CENTER)
				.applyTo(_comboHistory);
		_comboHistory.setVisibleItemCount(60);

		_comboHistory.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(final MouseEvent e) {}

			@Override
			public void mouseDown(final MouseEvent e) {

				// show list
				_comboHistory.setListVisible(true);
			}

			@Override
			public void mouseUp(final MouseEvent e) {}
		});

		/**
		 * This combination of key and selection listener causes a folder selection only with the
		 * <Enter> key or with a selection with the mouse in the drop down box
		 */
		_comboHistory.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {

				_isComboKeyPressed = true;

				if (e.keyCode == SWT.CR) {
					onSelectHistoryFolder(_comboHistory.getText());
				}
			}
		});

		_comboHistory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				final boolean isKey = _isComboKeyPressed;
				_isComboKeyPressed = false;

				if (isKey == false) {
					onSelectHistoryFolder(_comboHistory.getText());
				}
			}
		});
	}

	private void enableControls() {

		_actionNavigateBackward.setEnabled(false);
		_actionNavigateForward.setEnabled(false);
	}

	public void fillViewMenu(final IMenuManager menuMgr) {

		menuMgr.add(_actionSortFolderHistory);
		menuMgr.add(_actionRemoveInvalidFoldersFromHistory);
		menuMgr.add(_actionClearNavigationHistory);
	}

	public PhotosWithExifSelection getSelectedPhotosWithExif(final boolean isAllImages) {
		return _photoGallery.getSelectedPhotosWithExif(isAllImages);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return _picDirView.getViewSite().getActionBars().getStatusLineManager();
	}

	@Override
	public IToolBarManager getToolBarManager() {
		return _picDirView.getViewSite().getActionBars().getToolBarManager();
	}

	public void handlePrefStoreModifications(final PropertyChangeEvent event) {
		_photoGallery.handlePrefStoreModifications(event);
	}

	private void onSelectHistoryFolder(final String selectedFolder) {

		updateHistory(selectedFolder);

		BusyIndicator.showWhile(_display, new Runnable() {
			public void run() {

				final boolean isFolderAvailable = _picDirFolder.selectFolder(selectedFolder, false, false, false);

				if (isFolderAvailable == false) {
					removeInvalidFolder(selectedFolder);
				}
			}
		});
	}

	public void refreshUI() {
		_photoGallery.refreshUI();
	}

	@Override
	public void registerContextMenu(final String menuId, final MenuManager menuManager) {
		_picDirView.registerContextMenu(menuId, menuManager);
	}

	private void removeInvalidFolder(final String invalidFolderPathName) {

		// search invalid folder in history
		int invalidIndex = -1;
		int historyIndex = 0;
		for (final String historyFolder : _folderHistory) {

			if (historyFolder.equals(invalidFolderPathName)) {
				invalidIndex = historyIndex;
				break;
			}

			historyIndex++;
		}

		if (invalidIndex == -1) {
			// this should not happen
			return;
		}

		// remove invalid folder
		_folderHistory.remove(invalidIndex);
		_comboHistory.remove(invalidIndex);

		// display previously successfully loaded folder
		final File photoFolder = _photoGallery.getPhotoFolder();
		if (photoFolder != null) {
			_comboHistory.setText(photoFolder.getAbsolutePath());
		}
	}

	/**
	 * Checks all folders in the history and removes all folders which are not available any more.
	 */
	private void removeInvalidFolders() {

		final ArrayList<String> invalidFolders = new ArrayList<String>();
		final ArrayList<Integer> invalidFolderIndexes = new ArrayList<Integer>();

		int folderIndex = 0;

		for (final String historyFolder : _folderHistory) {

			final File folder = new File(historyFolder);
			if (folder.isDirectory() == false) {
				invalidFolders.add(historyFolder);
				invalidFolderIndexes.add(folderIndex);
			}

			folderIndex++;
		}

		if (invalidFolders.size() == 0) {
			// nothing to do
			return;
		}

		_folderHistory.removeAll(invalidFolders);

		final Integer[] invalidIndexes = invalidFolderIndexes.toArray(new Integer[invalidFolderIndexes.size()]);

		// remove from the end that the index numbers do not disappear
		for (int index = invalidIndexes.length - 1; index >= 0; index--) {
			_comboHistory.remove(invalidIndexes[index]);
		}
	}

	public void restoreState() {

		_isShowOnlyPhotos = Util.getStateBoolean(_state, STATE_IS_SHOW_ONLY_PHOTOS, true);
		updateUI_Action_FolderGallery();

		/*
		 * history
		 */
		final String[] historyEntries = Util.getStateArray(_state, STATE_FOLDER_HISTORY, null);
		if (historyEntries != null) {

			// update history and combo
			for (final String history : historyEntries) {
				_folderHistory.add(history);
				_comboHistory.add(history);
			}
		}

		_photoGallery.restoreState();

		enableControls();
	}

	public void saveState() {

		_state.put(STATE_FOLDER_HISTORY, _folderHistory.toArray(new String[_folderHistory.size()]));

		_state.put(STATE_IS_SHOW_ONLY_PHOTOS, _isShowOnlyPhotos);

		_photoGallery.saveState();
	}

	public void setFocus() {
		_photoGallery.setFocus();
	}

	@Override
	public void setSelection(final PhotoSelection photoSelection) {
		_picDirView.setSelection(photoSelection);
	}

	void showImages(final File imageFolder, final boolean isFromNavigationHistory, final boolean isReloadFolder) {

		_photoGallery.showImages(imageFolder, isReloadFolder);

		if (imageFolder != null && isFromNavigationHistory == false) {

			/*
			 * don't update history when navigation in the history has caused to display the images
			 */
			updateHistory(imageFolder.getAbsolutePath());
		}
	}

	void showRestoreFolder(final String restoreFolderName) {
		_photoGallery.showRestoreFolder(restoreFolderName);
	}

	public void stopLoadingImages() {
		_photoGallery.stopLoadingImages();
	}

	void updateColors(	final Color fgColor,
						final Color bgColor,
						final Color selectionFgColor,
						final Color noFocusSelectionFgColor,
						final boolean isRestore) {

		_photoGallery.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, isRestore);

		// combobox list entries are almost invisible when colors are set on osx

		/*
		 * set color in action bar only for Linux & Windows, setting color in OSX looks not very
		 * good
		 */
		if (UI.IS_OSX == false) {

//			_comboHistory.setForeground(fgColor);
//			_comboHistory.setBackground(bgColor);
//
//			_galleryToolbar.setForeground(fgColor);
//			_galleryToolbar.setBackground(bgColor);
		}
	}

	private void updateHistory(final String newFolderPathName) {

		int historyIndex = -1;
		int historyCounter = 0;

		// check if new path is already in the history
		for (final String historyItem : _folderHistory) {
			if (historyItem.equals(newFolderPathName)) {
				historyIndex = historyCounter;
				break;
			}
			historyCounter++;
		}

		if (historyIndex != -1) {

			// this is an existing history entry, move it to the top

			// remove from history
			_folderHistory.remove(historyIndex);

			// remove from combo
			_comboHistory.remove(historyIndex);
		}

		// check max history size
		int historySize = _folderHistory.size();
		if (historySize > MAX_HISTORY_ENTRIES) {

			_folderHistory.remove(historySize - 1);
			_comboHistory.remove(historySize - 1);
		}

		// update history
		_folderHistory.add(0, newFolderPathName);

		// update combo
		_comboHistory.add(newFolderPathName, 0);

		// must be selected otherwise the text field can be empty when selected from the dropdown list
		_comboHistory.select(0);

		/*
		 * enabel/disable history navigation
		 */
		_selectedHistoryIndex = 0;
		historySize = _folderHistory.size();

		_actionNavigateBackward.setEnabled(historySize > 1);
		_actionNavigateForward.setEnabled(false);

		_actionClearNavigationHistory.setEnabled(historySize > 1);
		_actionRemoveInvalidFoldersFromHistory.setEnabled(historySize > 1);
	}

	private void updateUI_Action_FolderGallery() {

		if (_isShowOnlyPhotos) {

			// show folder and gallery

			_actionToggleFolderGallery.setText(Messages.Pic_Dir_Action_ToggleFolderGallery_OnlyPhotos);
			_actionToggleFolderGallery.setToolTipText(Messages.Pic_Dir_Action_ToggleFolderGallery_OnlyPhotos);
			_actionToggleFolderGallery.setImageDescriptor(Activator
					.getImageDescriptor(Messages.Image__PhotoFolderGallery_OnlyPhotos));

		} else {

			// show only photos

			_actionToggleFolderGallery.setText(Messages.Pic_Dir_Action_ToggleFolderGallery);
			_actionToggleFolderGallery.setToolTipText(Messages.Pic_Dir_Action_ToggleFolderGallery_Tooltip);
			_actionToggleFolderGallery.setImageDescriptor(Activator
					.getImageDescriptor(Messages.Image__PhotoFolderGallery));
		}

		_picDirView.setMaximizedControl(_isShowOnlyPhotos);
	}

	void updateUI_StatusMessage(final String statusMessage) {
		_photoGallery.updateUI_StatusMessage(statusMessage);
	}
}
