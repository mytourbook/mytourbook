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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class DeviceImportManager {

	private static final String			ID								= "net.tourbook.importdata.DeviceImportManager";	//$NON-NLS-1$
	//
	private static final String			XML_STATE_DEVICE_IMPORT_CONFIG	= "XML_STATE_DEVICE_IMPORT_CONFIG";				//$NON-NLS-1$
	//
	private static final String			TAG_IMPORT_CONFIG				= "Config";										//$NON-NLS-1$
	private static final String			TAG_IMPORT_CONFIG_ROOT			= "DeviceImportConfig";							//$NON-NLS-1$
	private static final String			TAG_SPEED_VERTEX				= "Speed";											//$NON-NLS-1$
	//
	private static final String			ATTR_ANIMATION_CRAZY_FACTOR		= "animationCrazyFactor";							//$NON-NLS-1$
	private static final String			ATTR_ANIMATION_DURATION			= "animationDuration";								//$NON-NLS-1$
	private static final String			ATTR_AVG_SPEED					= "avgSpeed";										//$NON-NLS-1$
	private static final String			ATTR_BACKGROUND_OPACITY			= "backgroundOpacity";								//$NON-NLS-1$
	private static final String			ATTR_CONFIG_NAME				= "name";											//$NON-NLS-1$
	private static final String			ATTR_CONFIG_DESCRIPTION			= "description";									//$NON-NLS-1$
	private static final String			ATTR_CONFIG_BACKUP_FOLDER		= "backupFolder";									//$NON-NLS-1$
	private static final String			ATTR_CONFIG_DEVICE_FOLDER		= "deviceFolder";									//$NON-NLS-1$
	private static final String			ATTR_IS_CREATE_BACKUP			= "isCreateBackup";								//$NON-NLS-1$
	private static final String			ATTR_IS_LIVE_UPDATE				= "isLiveUpdate";									//$NON-NLS-1$
	private static final String			ATTR_NUM_UI_COLUMNS				= "uiColumns";										//$NON-NLS-1$
	private static final String			ATTR_TILE_SIZE					= "tileSize";										//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_CONFIG			= "tourTypeConfig";								//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_ID				= "tourTypeId";									//$NON-NLS-1$
	//
	static final int					CONFIG_SPEED_MIN				= 0;
	static final int					CONFIG_SPEED_MAX				= 3000;
	private static final int			CONFIG_SPEED_DEFAULT			= 0;
	//
	private static DeviceImportManager	_instance;

	private final IDialogSettings		_state							= TourbookPlugin.getState(ID);

	private ImportConfig				_importConfig;

	private String						_fileStoresHash;

	private ReentrantLock				STORE_LOCK						= new ReentrantLock();

	public static DeviceImportManager getInstance() {

		if (_instance == null) {
			_instance = new DeviceImportManager();
		}

		return _instance;
	}

	/**
	 * @param isForceRetrieveFiles
	 *            When <code>true</code> files will be retrieved even when the stores have not
	 *            changed.
	 * @return Returns <code>true</code> when import files have been retrieved, otherwise
	 *         <code>false</code>.
	 *         <p>
	 *         {@link ImportConfig#notImportedFiles} contains the files which are available in the
	 *         device folder but not available in the tour database.
	 */
	public DeviceImportState checkImportedFiles(final boolean isForceRetrieveFiles) {

		final DeviceImportState returnState = new DeviceImportState();

		// this is called from multiple threads and propably cause problems
		STORE_LOCK.lock();
		{
			try {

				/*
				 * Create hashcode for all file stores
				 */
				final Iterable<FileStore> fileStores = FileSystems.getDefault().getFileStores();
				final StringBuilder sb = new StringBuilder();

				for (final FileStore store : fileStores) {
					sb.append(store);
					sb.append(' ');
				}
				final String fileStoresHash = sb.toString();

				/*
				 * Check if stores has changed
				 */
				final boolean areTheSameStores = fileStoresHash.equals(_fileStoresHash);
				returnState.areTheSameStores = areTheSameStores;

				if (areTheSameStores && isForceRetrieveFiles == false) {

					returnState.areFilesRetrieved = false;

					return returnState;
				}

				/*
				 * Filestore has changed, a device was added/removed.
				 */
				_fileStoresHash = fileStoresHash;
				getImportFiles();

			} finally {
				STORE_LOCK.unlock();
			}
		}

		returnState.areFilesRetrieved = true;

		return returnState;
	}

	private HashSet<String> getBackupFiles(final String folder) {

		final HashSet<String> backupFiles = new HashSet<>();

		final Path validPath = getValidPath(folder);
		if (validPath == null) {
			return backupFiles;
		}

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(validPath)) {

			for (final Path path : directoryStream) {

				try {

					final BasicFileAttributeView fileAttributesView = Files.getFileAttributeView(
							path,
							BasicFileAttributeView.class);

					final BasicFileAttributes fileAttributes = fileAttributesView.readAttributes();

					// ignore not regular files
					if (fileAttributes.isRegularFile()) {

						backupFiles.add(path.getFileName().toString());
					}

				} catch (final Exception e) {
// this can occure too often
//					StatusUtil.log(e);
				}

			}

		} catch (final IOException ex) {
			StatusUtil.log(ex);
		}

		return backupFiles;
	}

	private HashSet<String> getDbFileNames(final List<OSFile> deviceFileNames) {

		final HashSet<String> dbFileNames = new HashSet<>();

		/*
		 * Create a IN list with all device file names which are searched in the db.
		 */
		final StringBuilder sb = new StringBuilder();

		for (int fileIndex = 0; fileIndex < deviceFileNames.size(); fileIndex++) {

			final OSFile deviceFile = deviceFileNames.get(fileIndex);
			final String fileName = deviceFile.fileName;

			if (fileIndex > 0) {
				sb.append(',');
			}

			sb.append('\'');

			// escape single quotes
			sb.append(fileName.replace("\'", "\\\'")); //$NON-NLS-1$ //$NON-NLS-2$

			sb.append('\'');
		}

		final String deviceFileNameINList = sb.toString();

		try (Connection conn = TourDatabase.getInstance().getConnection(); //
				Statement stmt = conn.createStatement()) {

			final String sqlQuery = ""// 													//$NON-NLS-1$
					+ "SELECT" //															//$NON-NLS-1$
					+ " TourImportFileName" //												//$NON-NLS-1$
					+ " FROM " + TourDatabase.TABLE_TOUR_DATA //							//$NON-NLS-1$
					+ (" WHERE TourImportFileName IN (" + deviceFileNameINList + ")") //	//$NON-NLS-1$ //$NON-NLS-2$
					+ " ORDER BY TourImportFileName"; //									//$NON-NLS-1$

			final ResultSet result = stmt.executeQuery(sqlQuery);

			while (result.next()) {

				final String dbFileName = result.getString(1);

				dbFileNames.add(dbFileName);
			}

		} catch (final SQLException e) {
			SQL.showException(e);
		}

		return dbFileNames;
	}

	public ImportConfig getDeviceImportConfig() {

		if (_importConfig == null) {
			_importConfig = loadImportConfig();
		}

		return _importConfig;
	}

	/**
	 */
	private void getImportFiles() {

		final ArrayList<OSFile> notImportedFiles = new ArrayList<>();
		final ArrayList<String> notBackedUpFiles = new ArrayList<>();

		final ImportConfig importConfig = getDeviceImportConfig();
		importConfig.notImportedFiles = notImportedFiles;
		importConfig.notBackedUpFiles = notBackedUpFiles;

		/*
		 * Get backup files
		 */
		HashSet<String> existingBackupFiles = null;
		if (importConfig.isCreateBackup) {

			existingBackupFiles = getBackupFiles(importConfig.getBackupOSFolder());
		}

		/*
		 * Get device files
		 */
		final List<OSFile> existingDeviceFiles = getOSFiles(importConfig.getDeviceOSFolder());
		importConfig.numDeviceFiles = existingDeviceFiles.size();

		/*
		 * Get files which are not yet backed up
		 */
		if (existingBackupFiles != null) {

			for (final OSFile deviceFile : existingDeviceFiles) {

				final String deviceFileName = deviceFile.fileName;

				if (existingBackupFiles.contains(deviceFileName) == false) {
					notBackedUpFiles.add(deviceFileName);
				}
			}
		}

		if (existingDeviceFiles.size() == 0) {
			// there is nothing to be imported
			return;
		}

		/*
		 * Get files which are not yet imported
		 */
		final HashSet<String> dbFileNames = getDbFileNames(existingDeviceFiles);

		for (final OSFile deviceFile : existingDeviceFiles) {

			if (dbFileNames.contains(deviceFile.fileName) == false) {
				notImportedFiles.add(deviceFile);
			}
		}

		// sort by date/time
		Collections.sort(notImportedFiles, new Comparator<OSFile>() {
			@Override
			public int compare(final OSFile file1, final OSFile file2) {
//				return Long.compare(file1.modifiedTime, file2.modifiedTime);
				return file1.fileName.compareTo(file2.fileName);
			}
		});
	}

	private List<OSFile> getOSFiles(final String folder) {

		final List<OSFile> osFiles = new ArrayList<>();

		final Path validPath = getValidPath(folder);
		if (validPath == null) {
			return osFiles;
		}

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(validPath)) {

			for (final Path path : directoryStream) {

				try {

					final BasicFileAttributeView fileAttributesView = Files.getFileAttributeView(
							path,
							BasicFileAttributeView.class);

					final BasicFileAttributes fileAttributes = fileAttributesView.readAttributes();

					// ignore not regular files
					if (fileAttributes.isRegularFile()) {

						final OSFile deviceFile = new OSFile();

						deviceFile.path = path;
						deviceFile.fileName = path.getFileName().toString();
						deviceFile.size = fileAttributes.size();
						deviceFile.modifiedTime = fileAttributes.lastModifiedTime().toMillis();

						osFiles.add(deviceFile);
					}

				} catch (final Exception e) {
// this can occure too often
//					StatusUtil.log(e);
				}

			}

		} catch (final IOException ex) {
			StatusUtil.log(ex);
		}

		return osFiles;
	}

	/**
	 * @param osFolder
	 * @return Returns the device OS path or <code>null</code> when this folder is not valid.
	 */
	private Path getValidPath(final String osFolder) {

		if (osFolder != null && osFolder.trim().length() > 0) {

			try {

				final Path devicePath = Paths.get(osFolder);

				if (Files.exists(devicePath)) {
					return devicePath;
				}

			} catch (final Exception e) {}
		}

		return null;
	}

	private boolean isFolderValid(final String deviceOSFolder, final String invalidMessage) {

		boolean isFolderValid = false;

		if (deviceOSFolder != null && deviceOSFolder.trim().length() > 0) {

			// check file
			try {

				final Path deviceFolderPath = Paths.get(deviceOSFolder);
				if (Files.exists(deviceFolderPath)) {
					isFolderValid = true;
				}

			} catch (final Exception e) {
				// path can be invalid
			}
		}

		if (!isFolderValid) {

			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					Messages.Import_Data_Error_DeviceImport_Title,
					NLS.bind(invalidMessage, deviceOSFolder));
		}

		return isFolderValid;
	}

	private ImportConfig loadImportConfig() {

		final ImportConfig importConfig = new ImportConfig();

		final String stateValue = Util.getStateString(_state, XML_STATE_DEVICE_IMPORT_CONFIG, null);

		if ((stateValue != null) && (stateValue.length() > 0)) {

			try {

				final Reader reader = new StringReader(stateValue);

				loadImportConfig_Data(XMLMemento.createReadRoot(reader), importConfig);

			} catch (final WorkbenchException e) {
				// ignore
			}
		}

		return importConfig;
	}

	private void loadImportConfig_Data(final XMLMemento xmlMemento, final ImportConfig importConfig) {

		importConfig.isLiveUpdate = Util.getXmlBoolean(xmlMemento, ATTR_IS_LIVE_UPDATE, true);

		importConfig.animationCrazinessFactor = Util.getXmlInteger(
				xmlMemento,
				ATTR_ANIMATION_CRAZY_FACTOR,
				3,
				-100,
				100);
		importConfig.animationDuration = Util.getXmlInteger(xmlMemento, ATTR_ANIMATION_DURATION, 40, 0, 100);
		importConfig.backgroundOpacity = Util.getXmlInteger(xmlMemento, ATTR_BACKGROUND_OPACITY, 5, 0, 100);

		importConfig.numHorizontalTiles = Util.getXmlInteger(
				xmlMemento,
				ATTR_NUM_UI_COLUMNS,
				RawDataView.NUM_HORIZONTAL_TILES_DEFAULT,
				RawDataView.NUM_HORIZONTAL_TILES_MIN,
				RawDataView.NUM_HORIZONTAL_TILES_MAX);

		importConfig.tileSize = Util.getXmlInteger(
				xmlMemento,
				ATTR_TILE_SIZE,
				RawDataView.TILE_SIZE_DEFAULT,
				RawDataView.TILE_SIZE_MIN,
				RawDataView.TILE_SIZE_MAX);

		importConfig.isCreateBackup = Util.getXmlBoolean(xmlMemento, ATTR_IS_CREATE_BACKUP, true);
		importConfig.backupFolder = Util.getXmlString(xmlMemento, ATTR_CONFIG_BACKUP_FOLDER, UI.EMPTY_STRING);
		importConfig.deviceFolder = Util.getXmlString(xmlMemento, ATTR_CONFIG_DEVICE_FOLDER, UI.EMPTY_STRING);

		for (final IMemento xmlConfig : xmlMemento.getChildren()) {

			final DeviceImportLauncher configItem = new DeviceImportLauncher();

			configItem.name = Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME, UI.EMPTY_STRING);
			configItem.description = Util.getXmlString(xmlConfig, ATTR_CONFIG_DESCRIPTION, UI.EMPTY_STRING);

			final Enum<TourTypeConfig> ttConfig = Util.getXmlEnum(
					xmlConfig,
					ATTR_TOUR_TYPE_CONFIG,
					TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL);

			configItem.tourTypeConfig = ttConfig;

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				final ArrayList<SpeedTourType> speedVertices = configItem.speedTourTypes;

				for (final IMemento memento : xmlConfig.getChildren()) {

					if (memento instanceof XMLMemento) {

						final XMLMemento xmlSpeed = (XMLMemento) memento;

						final Long xmlTourTypeId = Util.getXmlLong(xmlSpeed, ATTR_TOUR_TYPE_ID, null);

						/*
						 * Check if the loaded tour type id is valid
						 */
						final TourType tourType = TourDatabase.getTourType(xmlTourTypeId);

						if (tourType != null) {

							final SpeedTourType speedVertex = new SpeedTourType();

							speedVertex.tourTypeId = xmlTourTypeId;

							speedVertex.avgSpeed = Util.getXmlFloatFloat(
									xmlSpeed,
									ATTR_AVG_SPEED,
									CONFIG_SPEED_DEFAULT,
									CONFIG_SPEED_MIN,
									CONFIG_SPEED_MAX);

							speedVertices.add(speedVertex);
						}
					}
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final Long xmlTourTypeId = Util.getXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, null);

				configItem.oneTourType = TourDatabase.getTourType(xmlTourTypeId);

			} else {

				// this is the default, tour type is not set

			}

			configItem.setupItemImage();

			importConfig.deviceImportLaunchers.add(configItem);
		}
	}

	/**
	 * Reset stored values.
	 */
	public void reset() {

		// force that it will be reloaded
		_fileStoresHash = null;
	}

	public ImportState runImport(final DeviceImportLauncher aiLauncher) {

		final ImportState importState = new ImportState();

		final ImportConfig importConfig = getDeviceImportConfig();

		/*
		 * Check device folder
		 */
		final String deviceOSFolder = importConfig.getDeviceOSFolder();
		if (!isFolderValid(deviceOSFolder, Messages.Import_Data_Error_DeviceImport_InvalidDeviceFolder_Message)) {

			importState.isOpenSetup = true;

			return importState;
		}

		/*
		 * Check backup folder
		 */
		if (importConfig.isCreateBackup) {

			final String backupOSFolder = importConfig.getBackupOSFolder();
			if (!isFolderValid(backupOSFolder, Messages.Import_Data_Error_DeviceImport_InvalidBackupFolder_Message)) {

				importState.isOpenSetup = true;

				return importState;
			}
		}

		final ArrayList<OSFile> notImportedPaths = importConfig.notImportedFiles;
		if (notImportedPaths.size() == 0) {

			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					Messages.Import_Data_Error_DeviceImport_Title,
					NLS.bind(Messages.Import_Data_Error_DeviceImport_NoImportFiles_Message, deviceOSFolder));

			return importState;
		}

		final ArrayList<String> notImportedFileNames = new ArrayList<>();

		for (final OSFile deviceFile : notImportedPaths) {
			notImportedFileNames.add(deviceFile.fileName);
		}

		final String firstFilePathName = notImportedPaths.get(0).path.toString();
		final String[] fileNames = notImportedFileNames.toArray(new String[notImportedFileNames.size()]);

		RawDataManager.getInstance().actionImportFromFile_DoTheImport(firstFilePathName, fileNames);

		return importState;
	}

	public void saveImportConfig(final ImportConfig importConfig) {

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(TAG_IMPORT_CONFIG_ROOT);

		saveImportConfig_Data(xmlMemento, importConfig);

		// Write the XML block to the state store.
		final Writer writer = new StringWriter();
		try {

			xmlMemento.save(writer);
			_state.put(XML_STATE_DEVICE_IMPORT_CONFIG, writer.toString());

		} catch (final IOException e) {

			StatusUtil.log(e);

		} finally {

			try {
				writer.close();
			} catch (final IOException e) {
				StatusUtil.log(e);
			}
		}
	}

	private void saveImportConfig_Data(final XMLMemento xmlMemento, final ImportConfig importConfig) {

		xmlMemento.putBoolean(ATTR_IS_LIVE_UPDATE, importConfig.isLiveUpdate);

		xmlMemento.putInteger(ATTR_ANIMATION_CRAZY_FACTOR, importConfig.animationCrazinessFactor);
		xmlMemento.putInteger(ATTR_ANIMATION_DURATION, importConfig.animationDuration);
		xmlMemento.putInteger(ATTR_BACKGROUND_OPACITY, importConfig.backgroundOpacity);
		xmlMemento.putInteger(ATTR_NUM_UI_COLUMNS, importConfig.numHorizontalTiles);
		xmlMemento.putInteger(ATTR_TILE_SIZE, importConfig.tileSize);

		xmlMemento.putBoolean(ATTR_IS_CREATE_BACKUP, importConfig.isCreateBackup);
		xmlMemento.putString(ATTR_CONFIG_BACKUP_FOLDER, importConfig.backupFolder);
		xmlMemento.putString(ATTR_CONFIG_DEVICE_FOLDER, importConfig.deviceFolder);

		for (final DeviceImportLauncher configItem : importConfig.deviceImportLaunchers) {

			final IMemento xmlConfig = xmlMemento.createChild(TAG_IMPORT_CONFIG);

			xmlConfig.putString(ATTR_CONFIG_NAME, configItem.name);
			xmlConfig.putString(ATTR_CONFIG_DESCRIPTION, configItem.description);

			final Enum<TourTypeConfig> ttConfig = configItem.tourTypeConfig;
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_TYPE_CONFIG, ttConfig);

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				for (final SpeedTourType speedVertex : configItem.speedTourTypes) {

					final IMemento memento = xmlConfig.createChild(TAG_SPEED_VERTEX);

					if (memento instanceof XMLMemento) {

						final XMLMemento xmlSpeedVertex = (XMLMemento) memento;

						Util.setXmlLong(xmlSpeedVertex, ATTR_TOUR_TYPE_ID, speedVertex.tourTypeId);
						xmlSpeedVertex.putFloat(ATTR_AVG_SPEED, speedVertex.avgSpeed);
					}
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final TourType oneTourType = configItem.oneTourType;

				if (oneTourType != null) {
					Util.setXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, oneTourType.getTypeId());
				}

			} else {

				// this is the default, a tour type is not set
			}
		}
	}
}
