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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

public class AutoImportManager {

	private static final String			ID									= "net.tourbook.importdata.AutoImportManager";	//$NON-NLS-1$
	//
	private static final String			XML_STATE_AUTOMATED_IMPORT_CONFIG	= "XML_STATE_AUTOMATED_IMPORT_CONFIG";			//$NON-NLS-1$
	//
	private static final String			TAG_IMPORT_CONFIG					= "Config";									//$NON-NLS-1$
	private static final String			TAG_IMPORT_CONFIG_ROOT				= "AutomatedImportConfig";						//$NON-NLS-1$
	private static final String			TAG_SPEED_VERTEX					= "Speed";										//$NON-NLS-1$
	//
	private static final String			ATTR_ANIMATION_DURATION				= "animationDuration";							//$NON-NLS-1$
	private static final String			ATTR_AVG_SPEED						= "avgSpeed";									//$NON-NLS-1$
	private static final String			ATTR_BACKGROUND_OPACITY				= "backgroundOpacity";							//$NON-NLS-1$
	private static final String			ATTR_CONFIG_NAME					= "name";										//$NON-NLS-1$
	private static final String			ATTR_CONFIG_DESCRIPTION				= "description";								//$NON-NLS-1$
	private static final String			ATTR_CONFIG_BACKUP_FOLDER			= "backupFolder";								//$NON-NLS-1$
	private static final String			ATTR_CONFIG_DEVICE_FOLDER			= "deviceFolder";								//$NON-NLS-1$
	private static final String			ATTR_IS_LIVE_UPDATE					= "isLiveUpdate";								//$NON-NLS-1$
	private static final String			ATTR_NUM_UI_COLUMNS					= "uiColumns";									//$NON-NLS-1$
	private static final String			ATTR_TILE_SIZE						= "tileSize";									//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_CONFIG				= "tourTypeConfig";							//$NON-NLS-1$
	private static final String			ATTR_TOUR_TYPE_ID					= "tourTypeId";								//$NON-NLS-1$
	//
	static final int					CONFIG_SPEED_MIN					= 0;
	static final int					CONFIG_SPEED_MAX					= 3000;
	private static final int			CONFIG_SPEED_DEFAULT				= 0;
	//
	private static AutoImportManager	_instance;

	private final IDialogSettings		_state								= TourbookPlugin.getState(ID);

	private ImportConfig				_importConfig;

	private String						_fileStoresHash;

	public static AutoImportManager getInstance() {

		if (_instance == null) {
			_instance = new AutoImportManager();
		}

		return _instance;
	}

	public ImportConfig getAutoImportConfig() {

		if (_importConfig == null) {
			_importConfig = loadImportConfig();
		}

		return _importConfig;
	}

	/**
	 * @return Returns a list with a files which has not yet been imported
	 */
	private ArrayList<DeviceFile> getImportFiles() {

		final ArrayList<DeviceFile> notImportedFiles = new ArrayList<>();

		final String validDeviceFolder = runImport_10_GetDeviceFolder();

		if (validDeviceFolder == null) {
			return notImportedFiles;
		}

		final List<DeviceFile> deviceFileNames = runImport_12_GetDeviceFileNames(validDeviceFolder);

		if (deviceFileNames.size() == 0) {
			// there is nothing to be imported
			return notImportedFiles;
		}

		final HashSet<String> dbFileNames = runImport_14_GetDbFileNames(deviceFileNames);

		for (final DeviceFile deviceFileName : deviceFileNames) {

			if (dbFileNames.contains(deviceFileName.fileName) == false) {
				notImportedFiles.add(deviceFileName);
			}
		}
		
		// sort by date/time
		Collections.sort(notImportedFiles, new Comparator<DeviceFile>() {
			@Override
			public int compare(final DeviceFile file1, final DeviceFile file2) {
				return Long.compare(file1.modifiedTime, file2.modifiedTime);
			}
		});

		return notImportedFiles;
	}

	/**
	 * @param isForceUpdate
	 * @return Returns <code>true</code> when import files have newly retrieved, otherwise
	 *         <code>false</code>.
	 *         <p>
	 *         {@link ImportConfig#notImportedFiles} contains the files which are available in the
	 *         device folder but not available in the tour database.
	 */
	public boolean getNotImportedFiles(final boolean isForceUpdate) {

		final long start = System.nanoTime();

		/*
		 * Get hashcode vor all files stores
		 */
		final Iterable<FileStore> fileStores = FileSystems.getDefault().getFileStores();
		final StringBuilder sb = new StringBuilder();

		for (final FileStore store : fileStores) {
			sb.append(store);
			sb.append(' ');
		}
		final String fileStoresHash = sb.toString();

		final ImportConfig importConfig = getAutoImportConfig();

		/*
		 * Check if files stores has changed
		 */
		if (isForceUpdate == false && fileStoresHash.equals(_fileStoresHash)) {
			return false;
		}

		_fileStoresHash = fileStoresHash;

		/*
		 * Filestore has changed, a device was added/removed.
		 */
		importConfig.notImportedFiles = getImportFiles();

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
				+ (String.format("%7.1f ms", (float) (System.nanoTime() - start) / 1000000))
				+ ("\tnotImportedFiles: " + importConfig.notImportedFiles.size())
				+ ("\tdeviceFolder: " + importConfig.deviceFolder)
				+ ("\tbackupFolder: " + importConfig.backupFolder));
		// TODO remove SYSTEM.OUT.PRINTLN

		return true;
	}

	private ImportConfig loadImportConfig() {

		final ImportConfig importConfig = new ImportConfig();

		final String stateValue = Util.getStateString(_state, XML_STATE_AUTOMATED_IMPORT_CONFIG, null);

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

		importConfig.backupFolder = Util.getXmlString(xmlMemento, ATTR_CONFIG_BACKUP_FOLDER, UI.EMPTY_STRING);
		importConfig.deviceFolder = Util.getXmlString(xmlMemento, ATTR_CONFIG_DEVICE_FOLDER, UI.EMPTY_STRING);

		for (final IMemento xmlConfig : xmlMemento.getChildren()) {

			final TourTypeItem configItem = new TourTypeItem();

			configItem.name = Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME, UI.EMPTY_STRING);
			configItem.description = Util.getXmlString(xmlConfig, ATTR_CONFIG_DESCRIPTION, UI.EMPTY_STRING);

			final Enum<TourTypeConfig> ttConfig = Util.getXmlEnum(
					xmlConfig,
					ATTR_TOUR_TYPE_CONFIG,
					TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED);

			configItem.configType = ttConfig;

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				final ArrayList<SpeedVertex> speedVertices = configItem.speedVertices;

				for (final IMemento xmlSpeed : xmlConfig.getChildren()) {

					final Long xmlTourTypeId = Util.getXmlLong(xmlSpeed, ATTR_TOUR_TYPE_ID, null);

					/*
					 * Check if the loaded tour type id is valid
					 */
					final TourType tourType = TourDatabase.getTourType(xmlTourTypeId);

					if (tourType != null) {

						final SpeedVertex speedVertex = new SpeedVertex();

						speedVertex.tourTypeId = xmlTourTypeId;

						speedVertex.avgSpeed = Util.getXmlInteger(
								xmlSpeed,
								ATTR_AVG_SPEED,
								CONFIG_SPEED_DEFAULT,
								CONFIG_SPEED_MIN,
								CONFIG_SPEED_MAX);

						speedVertices.add(speedVertex);
					}
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final Long xmlTourTypeId = Util.getXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, null);

				configItem.oneTourType = TourDatabase.getTourType(xmlTourTypeId);

			} else {

				// this is the default, TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			}

			configItem.setupItemImage();

			importConfig.tourTypeItems.add(configItem);
		}
	}

	/**
	 * Reset stored values.
	 */
	public void reset() {

		// force that it will be reloaded
		_fileStoresHash = null;
	}

	public void runImport(final TourTypeItem tourTypeItem) {

//
//						(\\[^\\]*\\[^\\]*$)
//
//						2015-11-04 18:34:57.307'790 [] 	C:\DAT\_DEVICE data\705\2014\2014-07-03-10-47-25.tcx
//						2015-11-04 18:34:57.308'029 [] 	C:\DAT\_DEVICE data\705\2014\2014-07-06-13-55-18.fit
//						2015-11-04 18:34:57.387'047 [] 	C:\DAT\_DEVICE data\HAC4\2012-01-14.dat
//						2015-11-04 18:34:57.387'271 [] 	C:\DAT\_DEVICE data\HAC4\2012-01-14.dat
//						2015-11-04 18:34:57.473'234 [] 	M:\DEVICE data\DAUM Ergometer\0198  06_01_2010 19_48_48   12min    5_1km  Manuelles Training (Watt).csv
//						2015-11-04 18:34:57.473'457 [] 	M:\DEVICE data\DAUM Ergometer\0200  08_01_2010 19_12_47   40min   15_9km  Coaching - 009 - 2_4.csv
//
//		final IRunnableWithProgress importRunnable = new IRunnableWithProgress() {
//
//			@Override
//			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//
//				final int imported = 0;
//
//				monitor.beginTask(Messages.Import_Data_AutoImport_Task, 2);
//
//				monitor.worked(1);
//				monitor.subTask(NLS.bind(Messages.Import_Data_AutoImport_SubTask, //
//						0));
//				if (monitor.isCanceled()) {
//					// stop autoimport
////					break;
//				}
//
//
//			}
//		};
//
//		try {
//			new ProgressMonitorDialog(Display.getDefault().getActiveShell()).run(true, true, importRunnable);
//		} catch (final Exception e) {
//			StatusUtil.log(e);
//		}
	}

	/**
	 * @return Returns the device OS path or <code>null</code> when this folder is not valid.
	 */
	private String runImport_10_GetDeviceFolder() {

		final ImportConfig importConfig = getAutoImportConfig();

		final String deviceFolderRaw = importConfig.deviceFolder;
		final String[] deviceFolder = { null };

		if (NIO.isDeviceNameFolder(deviceFolderRaw)) {

			deviceFolder[0] = NIO.convertToOSPath(deviceFolderRaw);

		} else {

			deviceFolder[0] = deviceFolderRaw;
		}

		boolean isFolderValid = false;

		try {

			final Path devicePath = Paths.get(deviceFolder[0]);

			if (Files.exists(devicePath)) {
				isFolderValid = true;
			}

		} catch (final Exception e) {}

		if (isFolderValid == false) {
			return null;
		}

		return deviceFolder[0];
	}

	private List<DeviceFile> runImport_12_GetDeviceFileNames(final String validDeviceFolder) {

		final List<DeviceFile> deviceFileNames = new ArrayList<>();

		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(validDeviceFolder))) {

			for (final Path path : directoryStream) {

				final BasicFileAttributeView fileAttributesView = Files.getFileAttributeView(
						path,
						BasicFileAttributeView.class);

				final BasicFileAttributes fileAttributes = fileAttributesView.readAttributes();

				final DeviceFile deviceFile = new DeviceFile();
				deviceFile.path = path;
				deviceFile.fileName = path.getFileName().toString();
				deviceFile.size = fileAttributes.size();
				deviceFile.modifiedTime = fileAttributes.lastModifiedTime().toMillis();

				deviceFileNames.add(deviceFile);
			}

		} catch (final IOException ex) {
			StatusUtil.log(ex);
		}

		return deviceFileNames;
	}

	private HashSet<String> runImport_14_GetDbFileNames(final List<DeviceFile> deviceFileNames) {

		final HashSet<String> dbFileNames = new HashSet<>();

		/*
		 * Create a IN list with all device file names which are searched in the db.
		 */
		final StringBuilder sb = new StringBuilder();

		for (int fileIndex = 0; fileIndex < deviceFileNames.size(); fileIndex++) {

			final DeviceFile deviceFile = deviceFileNames.get(fileIndex);
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

	private void runImport_50_Backup(final ImportConfig importConfig) {

		final String backupFolder = importConfig.backupFolder;
		final boolean isBackup = backupFolder != null && backupFolder.trim().length() > 0;

		if (isBackup) {

			final Path backupPath = Paths.get(backupFolder);

			Files.exists(backupPath);
		}
	}

	public void saveImportConfig(final ImportConfig importConfig) {

		// Build the XML block for writing the bindings and active scheme.
		final XMLMemento xmlMemento = XMLMemento.createWriteRoot(TAG_IMPORT_CONFIG_ROOT);

		saveImportConfig_Data(xmlMemento, importConfig);

		// Write the XML block to the state store.
		final Writer writer = new StringWriter();
		try {

			xmlMemento.save(writer);
			_state.put(XML_STATE_AUTOMATED_IMPORT_CONFIG, writer.toString());

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

		xmlMemento.putInteger(ATTR_ANIMATION_DURATION, importConfig.animationDuration);
		xmlMemento.putInteger(ATTR_BACKGROUND_OPACITY, importConfig.backgroundOpacity);
		xmlMemento.putInteger(ATTR_NUM_UI_COLUMNS, importConfig.numHorizontalTiles);
		xmlMemento.putInteger(ATTR_TILE_SIZE, importConfig.tileSize);

		xmlMemento.putString(ATTR_CONFIG_BACKUP_FOLDER, importConfig.backupFolder);
		xmlMemento.putString(ATTR_CONFIG_DEVICE_FOLDER, importConfig.deviceFolder);

		for (final TourTypeItem configItem : importConfig.tourTypeItems) {

			final IMemento xmlConfig = xmlMemento.createChild(TAG_IMPORT_CONFIG);

			xmlConfig.putString(ATTR_CONFIG_NAME, configItem.name);
			xmlConfig.putString(ATTR_CONFIG_DESCRIPTION, configItem.description);

			final Enum<TourTypeConfig> ttConfig = configItem.configType;
			Util.setXmlEnum(xmlConfig, ATTR_TOUR_TYPE_CONFIG, ttConfig);

			if (TourTypeConfig.TOUR_TYPE_CONFIG_BY_SPEED.equals(ttConfig)) {

				for (final SpeedVertex speedVertex : configItem.speedVertices) {

					final IMemento xmlSpeedVertex = xmlConfig.createChild(TAG_SPEED_VERTEX);

					Util.setXmlLong(xmlSpeedVertex, ATTR_TOUR_TYPE_ID, speedVertex.tourTypeId);
					xmlSpeedVertex.putInteger(ATTR_AVG_SPEED, speedVertex.avgSpeed);
				}

			} else if (TourTypeConfig.TOUR_TYPE_CONFIG_ONE_FOR_ALL.equals(ttConfig)) {

				final TourType oneTourType = configItem.oneTourType;

				if (oneTourType != null) {
					Util.setXmlLong(xmlConfig, ATTR_TOUR_TYPE_ID, oneTourType.getTypeId());
				}

			} else {

				// this is the default or TourTypeConfig.TOUR_TYPE_CONFIG_NOT_USED

			}
		}
	}
}
