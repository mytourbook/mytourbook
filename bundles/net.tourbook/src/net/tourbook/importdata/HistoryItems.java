/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import net.tourbook.Messages;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

/**
 * Manage combo box folder items.
 */
class HistoryItems {

	private static final String		NO_DEVICE_NAME			= "[?]";													//$NON-NLS-1$

	private static final int		COMBO_HISTORY_LENGTH	= 20;
	private static final String		COMBO_SEPARATOR			= "- - - - - - - - - - - - - - - - - - - - - - - - - - -";	//$NON-NLS-1$

	private boolean					_canShowDeviceName		= UI.IS_WIN;

	private LinkedHashSet<String>	_folderItems			= new LinkedHashSet<>();

	/** Contains paths with the device name and not the drive letter (only for Windows). */
	private LinkedHashSet<String>	_deviceNameItems		= new LinkedHashSet<>();

	/** Toggle history sorting. */
	private boolean					_isSortHistoryReversed;

	/*
	 * UI controls
	 */
	private Combo					_combo;
	private ControlDecoration		_comboError;

	private Label					_labelFolderInfo;

	private boolean					_isValidateFolder		= true;

	private String cleanupFolderDeviceName(final String deviceNameFolder) {

		final String cleanedDeviceNameFolder = deviceNameFolder.replace(
				Messages.Dialog_ImportConfig_Info_NoDeviceName,
				UI.EMPTY_STRING);

		return cleanedDeviceNameFolder;
	}

	private String convertTo_DeviceNameFolder(final String osFolder) {

		try {

			final Path newPath = Paths.get(osFolder);

			final String deviceName = getDeviceName(newPath);

			if (deviceName == null) {
				return null;
			}

			final String deviceFolderName = createDeviceNameFolder(newPath, deviceName);

			return deviceFolderName;

		} catch (final Exception e) {
			// folder can be invalid
		}

		return null;
	}

	private String createDeviceNameFolder(final Path folderPath, final String deviceName) {

		if (!_canShowDeviceName) {
			return folderPath.toString();
		}

		final int nameCount = folderPath.getNameCount();
		final Comparable<?> subPath = nameCount > 0 ? folderPath.subpath(0, nameCount) : UI.EMPTY_STRING;

		String deviceFolder = null;

		// construct device name folder
		if (deviceName == null) {

			deviceFolder = NO_DEVICE_NAME + File.separator + subPath;

		} else {

			if (deviceName.trim().length() == 0) {

				deviceFolder = '[' + Messages.Dialog_ImportConfig_Info_NoDeviceName + ']' + File.separator + subPath;

			} else {

				deviceFolder = '[' + deviceName + ']' + File.separator + subPath;
			}
		}

		return deviceFolder;
	}

	private void fillControls(final String newFolder, final String newDeviceNameFolder, final String selectedFolder) {

		// prevent to remove the combo text field
		_combo.removeAll();

		String folderText = UI.EMPTY_STRING;
		String folderInfo = UI.EMPTY_STRING;

		if (selectedFolder != null) {

			folderText = selectedFolder;

			folderInfo = NIO.isDeviceNameFolder(selectedFolder)
					? newFolder == null ? UI.EMPTY_STRING : newFolder
					: newDeviceNameFolder == null ? UI.EMPTY_STRING : newDeviceNameFolder;
		}

		_labelFolderInfo.setText(folderInfo);
		_combo.setText(folderText);

		boolean isAdded = false;

		/*
		 * Combo items
		 */
		if (newFolder != null && newFolder.length() > 0) {
			_combo.add(newFolder);
			isAdded = true;
		}

		if (_canShowDeviceName) {

			if (newDeviceNameFolder != null && newDeviceNameFolder.length() > 0) {
				_combo.add(newDeviceNameFolder);
				isAdded = true;
			}

			if (_deviceNameItems.size() > 0) {

				if (isAdded) {
					_combo.add(COMBO_SEPARATOR);
				}

				isAdded = true;

				for (final String deviceFolder : reverseHistory(_deviceNameItems)) {
					_combo.add(deviceFolder);
				}
			}
		}

		if (_folderItems.size() > 0) {

			if (isAdded) {
				_combo.add(COMBO_SEPARATOR);
			}

			isAdded = true;

			for (final String driveFolder : reverseHistory(_folderItems)) {
				_combo.add(driveFolder);
			}
		}
	}

	/**
	 * @param deviceRoot
	 * @return Returns the device name for the drive or <code>null</code> when not available
	 */
	private String getDeviceName(final Path path) {

		/*
		 * This feature is available only for windows.
		 */
		if (!_canShowDeviceName) {
			return null;
		}

		final Path root = path.getRoot();

		if (root == null) {
			return null;
		}

		String deviceDrive = root.toString();
		deviceDrive = deviceDrive.substring(0, 2);

		final Iterable<FileStore> fileStores = NIO.getFileStores();

		for (final FileStore store : fileStores) {

			final String drive = NIO.parseDriveLetter(store);

			if (deviceDrive.equalsIgnoreCase(drive)) {

				return store.name();
			}
		}

		return null;
	}

	String getOSPath(final String defaultFolder, final String configFolder) {

		String osPath = null;

		if (defaultFolder != null) {
			osPath = NIO.convertToOSPath(defaultFolder);
		}

		if (osPath == null) {
			osPath = NIO.convertToOSPath(configFolder);
		}

		return osPath;
	}

	private void keepOldPathInHistory() {

		final String oldFolder = _combo.getText().trim();

		if (oldFolder.length() == 0) {
			return;
		}

		if (oldFolder.trim().startsWith(NIO.DEVICE_FOLDER_NAME_START)) {

			// this is a device name folder

			final String cleanHistoryItem = cleanupFolderDeviceName(oldFolder);
			_deviceNameItems.remove(cleanHistoryItem);
			_deviceNameItems.add(cleanHistoryItem);

		} else {

			_folderItems.remove(oldFolder);
			_folderItems.add(oldFolder);
		}
	}

	/**
	 * A new folder is selected in the system folder dialog.
	 * 
	 * @param newFolder
	 */
	void onSelectFolderInDialog(final String newFolder) {

		try {

			final Path newPath = Paths.get(newFolder);

			final String deviceName = getDeviceName(newPath);
			final String deviceNameFolder = createDeviceNameFolder(newPath, deviceName);

			updateModel(newFolder, deviceNameFolder);
			fillControls(newFolder, deviceNameFolder, newFolder);

		} catch (final Exception e) {
			// folder can be invalid
		}
	}

	/**
	 * Remove item from history.
	 * 
	 * @param text
	 */
	void removeFromHistory(final String itemText) {

		/*
		 * Remove from both histories because it could be in the wrong list
		 */
		_folderItems.remove(itemText);
		_deviceNameItems.remove(itemText);

		fillControls(null, null, null);
	}

	void restoreState(final String[] restoredFolderItems, final String[] restoredDeviceItems) {

		if (restoredFolderItems != null) {
			_folderItems.addAll(Arrays.asList(restoredFolderItems));
		}

		if (restoredDeviceItems != null) {
			_deviceNameItems.addAll(Arrays.asList(restoredDeviceItems));
		}

		// fill history
		fillControls(null, null, null);
	}

	private String[] reverseHistory(final LinkedHashSet<String> folderHistory) {

		final String[] folterItems = folderHistory.toArray(new String[folderHistory.size()]);
		final String[] reversedArray = (String[]) Util.arrayReverse(folterItems);

		return reversedArray;
	}

	/**
	 * Save history items.
	 * 
	 * @param state
	 * @param stateFolderHistoryItems
	 * @param stateDeviceHistoryItems
	 */
	void saveState(	final IDialogSettings state,
					final String stateFolderHistoryItems,
					final String stateDeviceHistoryItems) {

		state.put(stateFolderHistoryItems, _folderItems.toArray(new String[_folderItems.size()]));
		state.put(stateDeviceHistoryItems, _deviceNameItems.toArray(new String[_deviceNameItems.size()]));
	}

	void setControls(final Combo comboFolder, final Label lblFolderPath) {

		_combo = comboFolder;
		_labelFolderInfo = lblFolderPath;

		final Image image = FieldDecorationRegistry
				.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
				.getImage();

		_comboError = new ControlDecoration(_combo, SWT.LEFT | SWT.TOP);

		_comboError.setImage(image);
		_comboError.setDescriptionText(Messages.Dialog_ImportConfig_Error_FolderIsInvalid);
	}

	void setIsValidateFolder(final boolean isValidateFolder) {
		_isValidateFolder = isValidateFolder;
	}

	void sortHistory() {

		// toggle sorting
		_isSortHistoryReversed = !_isSortHistoryReversed;

		/*
		 * Sort folder items
		 */
		final ArrayList<String> folderItems = new ArrayList<String>(_folderItems);
		Collections.sort(folderItems);
		if (_isSortHistoryReversed) {
			Collections.reverse(folderItems);
		}
		_folderItems.clear();
		_folderItems.addAll(folderItems);

		/*
		 * Sort named folder items
		 */
		final ArrayList<String> namedFolderItems = new ArrayList<String>(_deviceNameItems);
		Collections.sort(namedFolderItems);
		if (_isSortHistoryReversed) {
			Collections.reverse(namedFolderItems);
		}
		_deviceNameItems.clear();
		_deviceNameItems.addAll(namedFolderItems);

		// update UI
		final String selectedFolderRaw = _combo.getText();
		fillControls(null, null, selectedFolderRaw);
	}

	/**
	 * Set selected/entered folder in the combo box into the history. This maintains the history
	 * with manually created paths.
	 */
	void updateHistory() {

		final String selectedFolderRaw = _combo.getText();

		String selectedFolder = null;

		if (NIO.isDeviceNameFolder(selectedFolderRaw)) {
			selectedFolder = NIO.convertToOSPath(selectedFolderRaw);
		} else {
			selectedFolder = selectedFolderRaw;
		}

		if (selectedFolder == null || selectedFolder.trim().length() == 0) {
			return;
		}

		try {

			final Path newPath = Paths.get(selectedFolder);

			final String deviceName = getDeviceName(newPath);
			final String deviceNameFolder = createDeviceNameFolder(newPath, deviceName);

			updateModel(selectedFolder, deviceNameFolder);
			fillControls(selectedFolder, deviceNameFolder, selectedFolderRaw);

		} catch (final Exception e) {
			// this can occure when the entered path is totally invalid
		}
	}

	private void updateHistory(final LinkedHashSet<String> historyItems, final String newItem) {

		if (newItem == null || newItem.trim().length() == 0) {
			// there is no new item
			return;
		}

		// move the new folder path to the top of the history
		final String cleanHistoryItem = cleanupFolderDeviceName(newItem);
		historyItems.remove(cleanHistoryItem);
		historyItems.add(cleanHistoryItem);

		if (historyItems.size() < COMBO_HISTORY_LENGTH) {
			return;
		}

		// force history length
		final ArrayList<String> removedItems = new ArrayList<>();

		int numFolder = 0;

		for (final String folderItem : historyItems) {
			if (++numFolder < COMBO_HISTORY_LENGTH) {
				continue;
			} else {
				removedItems.add(folderItem);
			}
		}

		historyItems.removeAll(removedItems);
	}

	private void updateModel(final String folderPath, final String deviceNamePath) {

		keepOldPathInHistory();

		updateHistory(_folderItems, folderPath);
		updateHistory(_deviceNameItems, deviceNamePath);
	}

	/**
	 */
	void validateModifiedPath() {

		if (_isValidateFolder == false) {

			_comboError.hide();
			_labelFolderInfo.setText(UI.EMPTY_STRING);

			return;
		}

		boolean isFolderValid = false;

		final String modifiedFolder = _combo.getText().trim();

		if (COMBO_SEPARATOR.equals(modifiedFolder)) {

			// ignore special texts

			isFolderValid = true;
			_labelFolderInfo.setText(UI.EMPTY_STRING);

		} else {

			final String cleanedFolderName = cleanupFolderDeviceName(modifiedFolder);

			final String osFolder = NIO.convertToOSPath(cleanedFolderName);

			if (osFolder != null) {

				try {

					final Path osPath = Paths.get(osFolder);

					isFolderValid = Files.exists(osPath);

					if (isFolderValid) {

						if (_canShowDeviceName) {

							if (NIO.isDeviceNameFolder(cleanedFolderName)) {

								// this is a device folder name

								_labelFolderInfo.setText(osFolder);

							} else {

								final String deviceFolder = convertTo_DeviceNameFolder(osFolder);

								if (deviceFolder == null) {
									isFolderValid = false;
								} else {

									_labelFolderInfo.setText(deviceFolder);
								}
							}

						} else {

							_labelFolderInfo.setText(UI.EMPTY_STRING);
						}
					}

				} catch (final Exception e) {
					isFolderValid = false;
				}
			}
		}

		if (isFolderValid) {

			_comboError.hide();
			_labelFolderInfo.setForeground(null);

		} else {

			_comboError.show();
			_labelFolderInfo.setText(Messages.Dialog_ImportConfig_Error_FolderIsInvalid);
			_labelFolderInfo.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}
	}
}
