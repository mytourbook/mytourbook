/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package de.byteholder.geoclipse.mapprovider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringToArrayConverter;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.Service;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.opengis.geometry.DirectPosition;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.byteholder.geoclipse.GeoclipseExtensions;
import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.GeoException;
import de.byteholder.geoclipse.map.TileImageCache;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.mapprovider.DialogMPCustom.PART_TYPE;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

/**
 * this will manage all map providers
 */
public class MapProviderManager {

	/**
	 * This prefix is used to sort the map providers at the end when the map provider is not a map
	 * profile
	 */
	private static final String			SINGLE_MAP_PROVIDER_NAME_PREFIX					= "_";								//$NON-NLS-1$

	private static final int			OSM_BACKGROUND_COLOR							= 0xE8EEF1;
	private static final int			DEFAULT_ALPHA									= 100;

	private static final String			URL_PREFIX_HTTP									= "http";							//$NON-NLS-1$
	private static final String			URL_PREFIX_HTTP_PROTOCOL						= "http://";						//$NON-NLS-1$

	private static final String			MAP_PROVIDER_TYPE_WMS							= "wms";							//$NON-NLS-1$
	private static final String			MAP_PROVIDER_TYPE_CUSTOM						= "custom";						//$NON-NLS-1$
	private static final String			MAP_PROVIDER_TYPE_MAP_PROFILE					= "profile";						//$NON-NLS-1$
	private static final String			MAP_PROVIDER_TYPE_PLUGIN						= "plugin";						//$NON-NLS-1$

	public static final String			MIME_PNG										= "image/png";						//$NON-NLS-1$
	public static final String			MIME_GIF										= "image/gif";						//$NON-NLS-1$
	public static final String			MIME_JPG										= "image/jpg";						//$NON-NLS-1$
	public static final String			MIME_JPEG										= "image/jpeg";					//$NON-NLS-1$

	public static final String			DEFAULT_IMAGE_FORMAT							= MIME_PNG;

	public static final String			FILE_EXTENSION_PNG								= "png";							//$NON-NLS-1$
	public static final String			FILE_EXTENSION_GIF								= "gif";							//$NON-NLS-1$
	public static final String			FILE_EXTENSION_JPG								= "jpg";							//$NON-NLS-1$

	/**
	 * This file name part is attached to saved tile images for profile map providers were only a
	 * part of the child images are available.
	 */
	public static final String			PART_IMAGE_FILE_NAME_SUFFIX						= "-part";							//$NON-NLS-1$

	/*
	 * map provider file and root tag
	 */
	private static final String			CUSTOM_MAP_PROVIDER_FILE_NAME					= "custom-map-provider.xml";		//$NON-NLS-1$

	private static final String			TAG_MAP_PROVIDER_LIST							= "MapProviderList";				//$NON-NLS-1$
	private static final String			ATTR_ROOT_DATETIME								= "Created";						//$NON-NLS-1$
	private static final String			ATTR_ROOT_VERSION_MAJOR							= "VersionMajor";					//$NON-NLS-1$
	private static final String			ATTR_ROOT_VERSION_MINOR							= "VersionMinor";					//$NON-NLS-1$
	private static final String			ATTR_ROOT_VERSION_MICRO							= "VersionMicro";					//$NON-NLS-1$
	private static final String			ATTR_ROOT_VERSION_QUALIFIER						= "VersionQualifier";				//$NON-NLS-1$
	private static final String			ATTR_ROOT_IS_MANUAL_EXPORT						= "IsExport";						//$NON-NLS-1$

	/*
	 * map provider common fields
	 */
	private static final String			ROOT_CHILD_TAG_MAP_PROVIDER						= "MapProvider";					//$NON-NLS-1$

	/**
	 * tag for map providers which are wrapped into a map profile
	 */
	private static final String			ROOT_CHILD_TAG_WRAPPED_MAP_PROVIDER				= "WrappedMapProvider";			//$NON-NLS-1$

	private static final String			ATTR_MP_NAME									= "Name";							//$NON-NLS-1$
	private static final String			ATTR_MP_ID										= "Id";							//$NON-NLS-1$
	private static final String			ATTR_MP_DESCRIPTION								= "Description";					//$NON-NLS-1$
	private static final String			ATTR_MP_OFFLINE_FOLDER							= "OfflineFolder";					//$NON-NLS-1$
	private static final String			ATTR_MP_TYPE									= "Type";							//$NON-NLS-1$
	private static final String			ATTR_MP_IMAGE_SIZE								= "ImageSize";						//$NON-NLS-1$
	private static final String			ATTR_MP_IMAGE_FORMAT							= "ImageFormat";					//$NON-NLS-1$
	private static final String			ATTR_MP_ZOOM_LEVEL_MIN							= "ZoomMin";						//$NON-NLS-1$
	private static final String			ATTR_MP_ZOOM_LEVEL_MAX							= "ZoomMax";						//$NON-NLS-1$
	private static final String			ATTR_MP_LAST_USED_ZOOM_LEVEL					= "LastUsedZoomLevel";				//$NON-NLS-1$
	private static final String			ATTR_MP_LAST_USED_LATITUDE						= "LastUsedLatitude";				//$NON-NLS-1$
	private static final String			ATTR_MP_LAST_USED_LONGITUDE						= "LastUsedLongitude";				//$NON-NLS-1$
	private static final String			ATTR_MP_FAVORITE_ZOOM_LEVEL						= "FavoriteZoomLevel";				//$NON-NLS-1$
	private static final String			ATTR_MP_FAVORITE_LATITUDE						= "FavoriteLatitude";				//$NON-NLS-1$
	private static final String			ATTR_MP_FAVORITE_LONGITUDE						= "FavoriteLongitude";				//$NON-NLS-1$

	/*
	 * custom map provider
	 */
	private static final String			ATTR_CUSTOM_CUSTOM_URL							= "CustomUrl";						//$NON-NLS-1$

	private static final String			TAG_URL_PART									= "UrlPart";						//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_TYPE							= "PartType";						//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_POSITION						= "Position";						//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_CONTENT_HTML					= "Html";							//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_START	= "RandomIntegerStart";			//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_END		= "RandomIntegerEnd";				//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_START		= "RandomAlphaStart";				//$NON-NLS-1$
	private static final String			ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_END		= "RandomAlphaEnd";				//$NON-NLS-1$

	private static final String			PART_TYPE_HTML									= "HTML";							//$NON-NLS-1$
	private static final String			PART_TYPE_RANDOM_INTEGER						= "RANDOM_INTEGER";				//$NON-NLS-1$
	private static final String			PART_TYPE_RANDOM_ALPHA							= "RANDOM_ALPHA";					//$NON-NLS-1$
	private static final String			PART_TYPE_X										= "X";								//$NON-NLS-1$;
	private static final String			PART_TYPE_Y										= "Y";								//$NON-NLS-1$;
	private static final String			PART_TYPE_ZOOM									= "ZOOM";							//$NON-NLS-1$;
//	private static final String				PART_TYPE_LAT_TOP								= "LATITUDE_TOP";					//$NON-NLS-1$
//	private static final String				PART_TYPE_LAT_BOTTOM							= "LATITUDE_BOTTOM";				//$NON-NLS-1$;
//	private static final String				PART_TYPE_LON_LEFT								= "LONGITUDE_LEFT";				//$NON-NLS-1$;
//	private static final String				PART_TYPE_LON_RIGHT								= "LONGITUDE_RIGHT";				//$NON-NLS-1$;

	/*
	 * wms map provider
	 */
	private static final String			ATTR_WMS_CAPS_URL								= "CapsUrl";						//$NON-NLS-1$
	private static final String			ATTR_WMS_MAP_URL								= "GetMapUrl";						//$NON-NLS-1$
	private static final String			ATTR_WMS_LOAD_TRANSPARENT_IMAGES				= "LoadTransparentImages";			//$NON-NLS-1$

	private static final String			TAG_LAYER										= "Layer";							//$NON-NLS-1$
	private static final String			ATTR_LAYER_NAME									= "Name";							//$NON-NLS-1$
	private static final String			ATTR_LAYER_TITLE								= "Title";							//$NON-NLS-1$
	private static final String			ATTR_LAYER_IS_DISPLAYED							= "IsDisplayed";					//$NON-NLS-1$
	private static final String			ATTR_LAYER_POSITION								= "Position";						//$NON-NLS-1$

	/*
	 * map profile
	 */
	private static final String			TAG_MAP_PROVIDER_WRAPPER						= "MapProviderWrapper";			//$NON-NLS-1$
	private static final String			ATTR_PMP_BACKGROUND_COLOR						= "BackgroundColor";				//$NON-NLS-1$

	// profile map provider settings
	private static final String			ATTR_PMP_MAP_PROVIDER_ID						= "Id";							//$NON-NLS-1$
	private static final String			ATTR_PMP_MAP_PROVIDER_TYPE						= "Type";							//$NON-NLS-1$
	private static final String			ATTR_PMP_POSITION								= "Position";						//$NON-NLS-1$
	private static final String			ATTR_PMP_IS_DISPLAYED							= "IsDisplayed";					//$NON-NLS-1$
	private static final String			ATTR_PMP_ALPHA									= "Alpha";							//$NON-NLS-1$
	private static final String			ATTR_PMP_IS_TRANSPARENT							= "IsTransparent";					//$NON-NLS-1$
	private static final String			ATTR_PMP_IS_BLACK_TRANSPARENT					= "IsBlackTransparent";			//$NON-NLS-1$
	private static final String			ATTR_PMP_IS_BRIGHTNESS_FOR_NEXT_MP				= "IsBrightnessForNextMP";			//$NON-NLS-1$
	private static final String			ATTR_PMP_BRIGHTNESS_FOR_NEXT_MP					= "BrightnessForNextMP";			//$NON-NLS-1$

	// transparent pixel
	private static final String			TAG_TRANSPARENT_COLOR							= "TransparentColor";				//$NON-NLS-1$
	private static final String			ATTR_TRANSPARENT_COLOR_VALUE					= "Value";							//$NON-NLS-1$

	/**
	 * Id for the default map provider
	 */
	public static String				DEFAULT_MAP_PROVIDER_ID							= OSMMapProvider.FACTORY_ID;

	/**
	 * size for osm images
	 */
	public static final int				OSM_IMAGE_SIZE									= 256;
	public static final String			DEFAULT_IMAGE_SIZE								= Integer
																								.toString(OSM_IMAGE_SIZE);
	public static final String[]		IMAGE_SIZE										= { DEFAULT_IMAGE_SIZE, "300", //$NON-NLS-1$
			"400", //$NON-NLS-1$
			"500", //$NON-NLS-1$
			"512", //$NON-NLS-1$
			"600", //$NON-NLS-1$
			"700", //$NON-NLS-1$S
			"768", //$NON-NLS-1$
			"800", //$NON-NLS-1$
			"900", //$NON-NLS-1$
			"1000", //$NON-NLS-1$
			"1024", //$NON-NLS-1$
																						};

	private static final ReentrantLock	WMS_LOCK										= new ReentrantLock();

	private static MapProviderManager	_instance;

	/**
	 * contains all available map providers, including empty map provider and map profiles
	 */
	private static ArrayList<MP>		_allMapProviders;

	private MPPlugin					_mpDefault;

	private ArrayList<String>			_errorLog										= new ArrayList<String>();

	private static IPreferenceStore		_prefStore										= TourbookPlugin
																								.getDefault()
																								.getPreferenceStore();

	private static final ListenerList	_mapProviderListeners							= new ListenerList(
																								ListenerList.IDENTITY);

	private static boolean				_isDeleteError;
	private static long					_deleteUIUpdateTime;
	private static int					_deleteUIDeletedFiles;
	private static int					_deleteUICheckedFiles;

	private MapProviderManager() {}

	/**
	 * Checks if the WMS is initialized, if not it will be done (it is loading the WMS layers). It
	 * also creates the wms tile factory.
	 * 
	 * @param mpWms
	 *            can be <code>null</code> when capsUrl is <code>not null</code> to do an initial
	 *            loading
	 * @param capsUrl
	 * @return Returns a wms map provider or <code>null</code> when the wms cannot be initialized
	 *         which happens when an connection to the wms server cannot be established
	 */
	public static MPWms checkWms(final MPWms mpWms, final String capsUrl) {

		final MPWms[] returnMpWms = new MPWms[] { mpWms };

		if (mpWms == null || mpWms.getWmsCaps() == null) {

			WMS_LOCK.lock();
			try {

				// recheck again, it's possible tha another thread could have loaded the caps
				if (mpWms == null || mpWms.getWmsCaps() == null) {
					checkWmsRunnable(mpWms, capsUrl, returnMpWms);
				}

			} finally {
				WMS_LOCK.unlock();
			}
		}

		return returnMpWms[0];
	}

	/**
	 * @param mpWms
	 * @param capsUrl
	 * @param returnMpWms
	 *            Contains the checked wms map provider
	 */
	private static void checkWmsRunnable(final MPWms mpWms, final String capsUrl, final MPWms[] returnMpWms) {

		final IRunnableWithProgress progressRunnable = new IRunnableWithProgress() {

			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				final String capsUrlFinal = mpWms == null ? //
						capsUrl
						: mpWms.getCapabilitiesUrl();

				monitor.beginTask(Messages.MP_Manager_Task_GetWms, 1);
				monitor.subTask(capsUrlFinal);

				try {

					returnMpWms[0] = initializeWMS(mpWms, capsUrlFinal);

				} catch (final Exception e) {

					StatusUtil.showStatus(e.getMessage(), e);

					/*
					 * disable this wms map provider, it is possible that the server is currently
					 * not available
					 */
					for (final MP mapProvider : _allMapProviders) {

						if (mapProvider instanceof MPWms) {

							final MPWms wmsMp = (MPWms) mapProvider;

							if (wmsMp.getCapabilitiesUrl().equalsIgnoreCase(capsUrlFinal)) {
								wmsMp.setWmsEnabled(false);
							}

						} else if (mapProvider instanceof MPProfile) {

							final MPProfile mpProfile = (MPProfile) mapProvider;

							for (final MPWrapper mpWrapper : mpProfile.getAllWrappers()) {
								final MP mp = mpWrapper.getMP();
								if (mp instanceof MPWms) {
									final MPWms wmsMp = (MPWms) mp;
									if (wmsMp.getCapabilitiesUrl().equalsIgnoreCase(capsUrlFinal)) {
										wmsMp.setWmsEnabled(false);
									}
								}
							}
						}
					}

					returnMpWms[0] = null;
				}
			}
		};

		final Display display = Display.getDefault();

		display.syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					new ProgressMonitorDialog(display.getActiveShell()).run(false, false, progressRunnable);
				} catch (final InvocationTargetException e1) {
					StatusUtil.showStatus(e1.getMessage(), e1);
				} catch (final InterruptedException e1) {
					StatusUtil.showStatus(e1.getMessage(), e1);
				}
			}
		});
	}

	/**
	 * Deletes offline map image files
	 * 
	 * @param mp
	 *            MapProvider
	 * @param isDeletePartImages
	 *            When <code>true</code> only the part images are deleted, otherwise all images are
	 *            deleted.
	 * @return Returns <code>true</code> when image files are deleted.
	 */
	public static boolean deleteOfflineMap(final MP mp, final boolean isDeletePartImages) {

		// reset state that offline images are available
		mp.resetTileImageAvailability();

		// check base path
		IPath tileCacheBasePath = getTileCachePath();
		if (tileCacheBasePath == null) {
			return false;
		}

		// check map provider offline folder
		final String tileOSFolder = mp.getOfflineFolder();
		if (tileOSFolder == null) {
			return false;
		}

		tileCacheBasePath = tileCacheBasePath.addTrailingSeparator();

		boolean isDeleted = false;

		// delete map provider files
		final File tileCacheDir = tileCacheBasePath.append(tileOSFolder).toFile();
		if (tileCacheDir.exists()) {
			deleteOfflineMapFiles(tileCacheDir, isDeletePartImages);
			isDeleted = true;
		}

		// delete profile wms files
		final File wmsPath = tileCacheBasePath.append(MPProfile.WMS_CUSTOM_TILE_PATH).append(tileOSFolder).toFile();
		if (wmsPath.exists()) {
			deleteOfflineMapFiles(wmsPath, isDeletePartImages);
			isDeleted = true;
		}

		return isDeleted;
	}

	private static void deleteOfflineMapFiles(final File offlineFolder, final boolean isDeletePartImages) {

		_isDeleteError = false;
		_deleteUIUpdateTime = System.currentTimeMillis();
		_deleteUIDeletedFiles = 0;
		_deleteUICheckedFiles = 0;

		try {

			final IRunnableWithProgress runnable = new IRunnableWithProgress() {

				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					final String taskName = isDeletePartImages ? //
							Messages.MP_Manager_DeletedOfflineImagesParts_TaskNameParts
							: Messages.MP_Manager_DeletedOfflineImagesParts_TaskName;

					monitor.beginTask(taskName, IProgressMonitor.UNKNOWN);

					deleteOfflineMapFilesFolder(offlineFolder, isDeletePartImages, monitor);
				}
			};

			new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, runnable);

		} catch (final InvocationTargetException e) {
			e.printStackTrace();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		if (_isDeleteError) {
			StatusUtil.showStatus(
					NLS.bind(Messages.MP_Manager_DeleteOfflineImages_CannotDeleteFolder, offlineFolder),
					new Exception());
		}
	}

	/**
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!<br>
	 * <br>
	 * Deletes all files and subdirectories. If a deletion fails, the method stops attempting to
	 * delete and returns false. <br>
	 * <br>
	 * !!!!!!!!!!!!!! RECURSIVE !!!!!!!!!!!!!!!!!
	 * 
	 * @param fileFolder
	 * @param isDeletePartImages
	 * @param monitor
	 * @return Returns <code>true</code> if all deletions were successful
	 */
	private static void deleteOfflineMapFilesFolder(final File fileFolder,
													final boolean isDeletePartImages,
													final IProgressMonitor monitor) {

		if (monitor.isCanceled()) {
			return;
		}

		boolean doDeleteFileFolder = true;

		if (fileFolder.isDirectory()) {

			// file is a folder

			final String[] allFileFolder = fileFolder.list();

			for (final String fileFolder2 : allFileFolder) {
				deleteOfflineMapFilesFolder(new File(fileFolder, fileFolder2), isDeletePartImages, monitor);
			}

			// update monitor every 200ms
			final long time = System.currentTimeMillis();
			if (time > _deleteUIUpdateTime + 200) {

				_deleteUIUpdateTime = time;

				final String fileFolderName = fileFolder.toString();
				final int endIndex = fileFolderName.length();
				int beginIndex = endIndex - 100;
				beginIndex = beginIndex < 0 ? 0 : beginIndex;

				monitor.subTask(NLS.bind(Messages.MP_Manager_DeletedOfflineImagesParts_SubTask, //
						new Object[] {
								_deleteUICheckedFiles,
								_deleteUIDeletedFiles,
								fileFolderName.substring(beginIndex, endIndex) }));
			}

			if (isDeletePartImages) {
				// don't delete folders when only part images are deleted
				doDeleteFileFolder = false;
			}

		} else {

			// file is a file

			// check if only part images should be deleted
			if (isDeletePartImages) {
				final String fileName = fileFolder.getName();
				if (fileName.contains(PART_IMAGE_FILE_NAME_SUFFIX) == false) {
					// this is not a part image -> DON'T delete
					doDeleteFileFolder = false;
				}
			}
		}

		boolean isFileFolderDeleted = false;
		if (doDeleteFileFolder) {

			// the folder is now empty so delete it
			isFileFolderDeleted = fileFolder.delete();

			_deleteUIDeletedFiles++;
		}

		_deleteUICheckedFiles++;

		/*
		 * !!! canceled must be checked before isFileFolderDeleted is checked because this returns
		 * false when the monitor is canceled !!!
		 */
		if (monitor.isCanceled()) {
			return;
		}

		if (doDeleteFileFolder && isFileFolderDeleted == false) {
			_isDeleteError = true;
			monitor.setCanceled(true);
		}
	}

	private static void fireChangeEvent() {

		final Object[] allListeners = _mapProviderListeners.getListeners();
		for (final Object listener : allListeners) {
			((IMapProviderListener) listener).mapProviderListChanged();
		}
	}

	/**
	 * Convert {@link DirectPosition} into a {@link GeoPosition}
	 * 
	 * @param position
	 * @return Returns a {@link GeoPosition}
	 */
	private static GeoPosition getGeoPosition(final DirectPosition position) {

		double latitude = 0;
		double longitude = 0;
		final int dimension = position.getDimension();

		for (int dimensionIndex = 0; dimensionIndex < dimension; dimensionIndex++) {

			if (dimensionIndex == 0) {
				longitude = position.getOrdinate(dimensionIndex);
			} else if (dimensionIndex == 1) {
				latitude = position.getOrdinate(dimensionIndex);
			}
		}

		return new GeoPosition(latitude, longitude);
	}

	/**
	 * @param imageType
	 *            SWT image format like {@link SWT#IMAGE_PNG}
	 * @return Returns <code>null</code> when the image type cannot be recognized
	 */
	public static String getImageFileExtension(final int imageType) {

		switch (imageType) {
		case SWT.IMAGE_JPEG:
			return FILE_EXTENSION_JPG;

		case SWT.IMAGE_GIF:
			return FILE_EXTENSION_GIF;

		case SWT.IMAGE_PNG:
			return FILE_EXTENSION_PNG;

		default:
		}

		return null;
	}

	public static String getImageFileExtension(final String mimeImageFormat) {

		if (mimeImageFormat.equalsIgnoreCase(MIME_JPG) || mimeImageFormat.equalsIgnoreCase(MIME_JPEG)) {
			return FILE_EXTENSION_JPG;
		} else if (mimeImageFormat.equalsIgnoreCase(MIME_GIF)) {
			return FILE_EXTENSION_GIF;
		} else {
			// use png as default
			return FILE_EXTENSION_PNG;
		}
	}

	/**
	 * @param imageType
	 *            SWT image format like {@link SWT#IMAGE_PNG}
	 * @return Returns <code>null</code> when the image type cannot be recognized
	 */
	public static String getImageMimeType(final int imageType) {

		switch (imageType) {
		case SWT.IMAGE_JPEG:
			return MIME_JPG;

		case SWT.IMAGE_GIF:
			return MIME_GIF;

		case SWT.IMAGE_PNG:
			return MIME_PNG;

		default:
		}

		return null;
	}

	public static MapProviderManager getInstance() {

		if (_instance == null) {
			_instance = new MapProviderManager();
		}

		return _instance;
	}

	private static String getMapProviderType(final MP mapProvider) {

		if (mapProvider instanceof MPCustom) {
			return MAP_PROVIDER_TYPE_CUSTOM;
		} else if (mapProvider instanceof MPWms) {
			return MAP_PROVIDER_TYPE_WMS;
		} else if (mapProvider instanceof MPPlugin) {
			return MAP_PROVIDER_TYPE_PLUGIN;
		}

		return null;
	}

	/**
	 * @return Returns file path for the offline maps or <code>null</code> when offline is not used
	 *         or the path is not valid
	 */
	public static IPath getTileCachePath() {

		// get status if the tile is offline cache is activated
		final boolean useOffLineCache = _prefStore.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_OFFLINE);

		if (useOffLineCache == false) {
			return null;
		}

		if (useOffLineCache) {

			// check tile cache path
			String workingDirectory;

			final boolean useDefaultLocation = _prefStore
					.getBoolean(IMappingPreferences.OFFLINE_CACHE_USE_DEFAULT_LOCATION);
			if (useDefaultLocation) {
				workingDirectory = Platform.getInstanceLocation().getURL().getPath();
			} else {
				workingDirectory = _prefStore.getString(IMappingPreferences.OFFLINE_CACHE_PATH);
			}

			if (new File(workingDirectory).exists() == false) {
				StatusUtil.showStatus("Map image offline folder is not available: " + workingDirectory); //$NON-NLS-1$
				return null;
			}

			// append a unique path so that deleting tiles is not doing it in the wrong directory
			final IPath tileCachePath = new Path(workingDirectory).append(TileImageCache.TILE_OFFLINE_CACHE_OS_PATH);
			if (tileCachePath.toFile().exists() == false) {
				return null;
			}

			return tileCachePath;
		}

		return null;
	}

	/**
	 * Load WMS capabilities, update the map provider by setting the UI state for the layers<br>
	 * <br>
	 * 
	 * @param oldWmsMapProvider
	 *            when a map provider is set, this wms map provider will be updated, when
	 *            <code>null</code> a new map provider is created
	 * @param capsUrl
	 *            capabilities url for the wms server
	 * @return Returns a wms map provider when the data are loaded from the wms server or
	 *         <code>null</code> otherwise
	 * @throws Exception
	 *             when the wms server cannot be accessed
	 */
	private static MPWms initializeWMS(final MPWms oldWmsMapProvider, String capsUrl) throws Exception {

		ArrayList<MtLayer> oldMtLayers = null;
		if (oldWmsMapProvider != null) {
			oldMtLayers = oldWmsMapProvider.getMtLayers();
		}

		/*
		 * load wms server
		 */
		WebMapServer wmsServer;

		try {
			wmsServer = WmsServerWrapper.getWmsServer(capsUrl);
		} catch (final Exception e) {

			// try it with http prefix
			if (e.getCause() instanceof MalformedURLException && capsUrl.startsWith(URL_PREFIX_HTTP) == false) {

				capsUrl = URL_PREFIX_HTTP_PROTOCOL + capsUrl;
				wmsServer = WmsServerWrapper.getWmsServer(capsUrl);

			} else {
				throw e;
			}
		}

		/*
		 * get capabilities
		 */
		WMSCapabilities wmsCaps = null;
		String capsError = null;
		Service wmsService = null;
		Exception ex = null;
		try {
			wmsCaps = wmsServer.getCapabilities();
			wmsService = wmsCaps.getService();
		} catch (final Exception e) {
			capsError = e.getMessage();
			ex = e;
		}
		if (wmsCaps == null || wmsService == null || capsError != null) {
			throw new GeoException(ex);
		}

		/*
		 * create mt layers and check wms layers
		 */
		final ArrayList<MtLayer> loadedMtLayers = new ArrayList<MtLayer>();
		final List<Layer> allGeoLayer = wmsCaps.getLayerList();
		for (final Layer geoLayer : allGeoLayer) {

			final String layerName = geoLayer.getName();
			if (layerName != null) {

				/*
				 * get lat/lon from envelope
				 */
				final CRSEnvelope envelope = geoLayer.getLatLonBoundingBox();
				if (envelope == null) {

					String wmsName = wmsService.getTitle();
					if (wmsName == null || wmsName.length() == 0) {
						wmsName = wmsService.getName();
					}

					StatusUtil.log(
							NLS.bind(Messages.DBG001_Error_Wms_LatLonBboxIsNotDefined, layerName, wmsName),
							new Exception());
					continue;
				}

				/*
				 * create new layer
				 */
				final GeoPosition lowerGeoPosition = getGeoPosition(envelope.getLowerCorner());
				final GeoPosition upperGeoPosition = getGeoPosition(envelope.getUpperCorner());
				final MtLayer mtLayer = new MtLayer(geoLayer, lowerGeoPosition, upperGeoPosition);

				loadedMtLayers.add(mtLayer);
			}
		}

		if (loadedMtLayers.size() == 0) {
			MessageDialog.openError(
					Display.getCurrent().getActiveShell(),
					Messages.MP_Error_DialogTitle_Wms,
					Messages.DBG002_Error_Wms_DialogMessage_InvalidLayers);
			return null;
		}

		MPWms updatedWmsMapProvider;

		if (oldWmsMapProvider == null) {
			// create WMS map provider
			updatedWmsMapProvider = new MPWms();
		} else {
			// use existing
			updatedWmsMapProvider = oldWmsMapProvider;
		}

		// inizialize map provider by setting none UI data
		updatedWmsMapProvider.initializeWms(wmsServer, wmsCaps, loadedMtLayers);

		/*
		 * update UI state
		 */
		if (oldWmsMapProvider != null) {

			// update UI state from old map provider

			if (oldMtLayers != null) {

				// update layers from old layers

				for (final MtLayer loadedMtLayer : loadedMtLayers) {

					final String mtLayerName = loadedMtLayer.getGeoLayer().getName();

					for (final MtLayer oldMtLayer : oldMtLayers) {

						if (oldMtLayer.getGeoLayer().getName().equals(mtLayerName)) {

							// update state

							loadedMtLayer.setIsDisplayedInMap(oldMtLayer.isDisplayedInMap());
							loadedMtLayer.setPositionIndex(oldMtLayer.getPositionIndex());

							break;
						}
					}
				}

			} else {

				// update layers from offline data

				final ArrayList<LayerOfflineData> allOfflineLayer = oldWmsMapProvider.getOfflineLayers();
				if (allOfflineLayer != null) {

					for (final MtLayer mtLayer : loadedMtLayers) {

						final String mtLayerName = mtLayer.getGeoLayer().getName();

						for (final LayerOfflineData offlineLayer : allOfflineLayer) {

							if (offlineLayer.name.equals(mtLayerName)) {

								// update state

								mtLayer.setIsDisplayedInMap(offlineLayer.isDisplayedInMap);
								mtLayer.setPositionIndex(offlineLayer.position);

								break;
							}
						}
					}
				}
			}
		}

		updatedWmsMapProvider.initializeLayers();

		// replace wms map provider in the list of map providers
		if (oldWmsMapProvider != null) {
			replaceMapProvider(updatedWmsMapProvider);
		}

		return updatedWmsMapProvider;
	}

	public static boolean isInstanciated() {
		return _instance != null;
	}

	/**
	 * updates a map provider in the model
	 * 
	 * @param allMapProviders
	 */
	public static void replaceMapProvider(final MP mapProviderReplacement) {

		final String replaceMapProviderId = mapProviderReplacement.getId();
		boolean isMapProviderReplaced = false;
		int mpIndex = 0;

		/*
		 * replace map provider in the model
		 */
		for (final MP mapProvider : _allMapProviders) {

			if (replaceMapProviderId.equals(mapProvider.getId())) {

				isMapProviderReplaced = true;

				// replace map provider
				_allMapProviders.set(mpIndex, mapProviderReplacement);

				// a map provider exists only once
				break;

			} else {
				mpIndex++;
			}
		}

		if (mapProviderReplacement instanceof MPProfile) {
			/*
			 * a map profile does not contain another map profile
			 */
			return;
		}

		if (isMapProviderReplaced) {

			/*
			 * replace map provider within the map profiles
			 */

			// loop: all profiles
			for (final MP mapProvider : _allMapProviders) {
				if (mapProvider instanceof MPProfile) {

					final MPProfile mapProfile = (MPProfile) mapProvider;
					final ArrayList<MPWrapper> mpWrapperList = mapProfile.getAllWrappers();

					// loop: all map providers which are set within the profiles
					for (final MPWrapper mpWrapper : mpWrapperList) {

						final MP profileMapProvider = mpWrapper.getMP();

						if (profileMapProvider != null && profileMapProvider.getId().equals(replaceMapProviderId)) {

							// replace map provider with a clone of the original map provider

							try {
								mpWrapper.setMP((MP) mapProviderReplacement.clone());
							} catch (final CloneNotSupportedException e) {
								StatusUtil.showStatus(e.getMessage(), e);
							}
						}
					}
				}
			}
		}
	}

	public void addMapProvider(final MP mp) {
		_allMapProviders.add(mp);
		updateMpSorting(mp);
	}

	public void addMapProviderListener(final IMapProviderListener listener) {
		_mapProviderListeners.add(listener);
	}

	private void checkMapProviders() {

		if (_allMapProviders != null) {
			return;
		}

		createAllMapProviders();
	}

	/**
	 * Create all map providers, osm as internal mp, all plugin mp's and the imported mp's from an
	 * xml file
	 */
	private void createAllMapProviders() {

		_allMapProviders = new ArrayList<MP>();

		// create default tile factories
		_mpDefault = new OSMMapProvider();

		_allMapProviders.add(_mpDefault);

		/*
		 * add plugin map providers
		 */
		final List<MPPlugin> allPluginMp = GeoclipseExtensions.getInstance().readFactories();
		for (final MPPlugin pluginMp : allPluginMp) {

			final String pluginFactoryId = pluginMp.getId();

			boolean isValid = true;

			for (final MP checkedMapProvider : _allMapProviders) {

				// check factory id
				if (checkedMapProvider.getId().equalsIgnoreCase(pluginFactoryId)) {

					StatusUtil.showStatus(
							NLS.bind(
									Messages.DBG003_Error_InvalidFactoryId,
									new Object[] { pluginFactoryId, pluginMp.getName(), checkedMapProvider.getName() //
									}),
							new Exception());

					isValid = false;
					break;
				}

				// check offline folder
				final String pluginOfflineFolder = pluginMp.getOfflineFolder();
				final String checkedOfflineFolder = checkedMapProvider.getOfflineFolder();
				if (pluginOfflineFolder != null
						&& checkedOfflineFolder != null
						&& checkedOfflineFolder.equalsIgnoreCase(pluginOfflineFolder)) {

					StatusUtil.showStatus(
							NLS.bind(Messages.DBG004_Error_InvalidOfflineFolder, new Object[] {
									pluginOfflineFolder,
									pluginMp.getName(),
									checkedMapProvider.getName() //
									}),
							new Exception());

					isValid = false;
					break;
				}
			}

			if (isValid) {

				// add valid map providers

				_allMapProviders.add(pluginMp);
			}
		}

		/*
		 * add external map providers which are defined in a xml file
		 */
		final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
		final File mapProviderFile = stateLocation.append(CUSTOM_MAP_PROVIDER_FILE_NAME).toFile();
		String absolutePath = mapProviderFile.getAbsolutePath();

		if (mapProviderFile.exists() == false) {

			/*
			 * check if the file exists in the old location from version <= 10.3
			 */

			final IPath stateRoot = stateLocation.removeLastSegments(1);
			final IPath oldFilePath = stateRoot.append("de.byteholder.geoclipse"); //$NON-NLS-1$
			final File oldFile = oldFilePath.append(CUSTOM_MAP_PROVIDER_FILE_NAME).toFile();

			if (oldFile.exists()) {
				absolutePath = oldFile.getAbsolutePath();
			}
		}

		final ArrayList<MP> importedMapProviders = readXml1(absolutePath, false, false);
		for (final MP mp : importedMapProviders) {

			/*
			 * ignore plugin map providers, they should be already in the list but can occure in the
			 * import file as a map profile wrapper
			 */
			if ((mp instanceof MPPlugin) == false) {
				_allMapProviders.add(mp);
			}
		}

		/*
		 * initialize map profiles, this MUST be done AFTER all external map providers are read from
		 * the xml file because they are referenced in the profile map provider
		 */
		for (final MP mapProvider : _allMapProviders) {
			if (mapProvider instanceof MPProfile) {
				((MPProfile) mapProvider).synchronizeMPWrapper();
			}
		}

		// sort by name
		Collections.sort(_allMapProviders);
	}

	private XMLMemento createXmlRoot(final boolean isManualExport) {

		try {

			final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			// create root element
			final Element rootElement = document.createElement(TAG_MAP_PROVIDER_LIST);
			document.appendChild(rootElement);

			// create tag from element
			final XMLMemento tagRoot = new XMLMemento(document, rootElement);

			// date/time
			tagRoot.putString(ATTR_ROOT_DATETIME, TimeTools.now().toString());

			// plugin version
			final Version version = TourbookPlugin.getDefault().getBundle().getVersion();
			tagRoot.putInteger(ATTR_ROOT_VERSION_MAJOR, version.getMajor());
			tagRoot.putInteger(ATTR_ROOT_VERSION_MINOR, version.getMinor());
			tagRoot.putInteger(ATTR_ROOT_VERSION_MICRO, version.getMicro());
			tagRoot.putString(ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

			// export flag
			if (isManualExport) {
				tagRoot.putBoolean(ATTR_ROOT_IS_MANUAL_EXPORT, true);
			}

			return tagRoot;

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	private void displayError(final String filename) {

		if (_errorLog.size() == 0) {
			return;
		}

		// log filename, that it's visible in the log
		logError(filename, new Exception());

		final StringBuilder sb = new StringBuilder();
		sb.append(Messages.MP_Error_Title_ErrorInXmlFile);
		sb.append(UI.NEW_LINE);
		sb.append(UI.NEW_LINE);
		sb.append(filename);
		sb.append(UI.NEW_LINE);
		sb.append(UI.NEW_LINE);
		for (final String log : _errorLog) {
			sb.append(log);
			sb.append(UI.NEW_LINE);
		}

		MessageDialog.openError(
				Display.getDefault().getActiveShell(),
				Messages.MP_Error_DialogTitle_ConfigurationError,
				sb.toString());
	}

	public void exportMapProvider(final MP mapProvider, final File file) {

		BufferedWriter writer = null;

		try {

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UI.UTF_8));

			final XMLMemento xmlMemento = createXmlRoot(true);

			writeXml(mapProvider, xmlMemento.createChild(ROOT_CHILD_TAG_MAP_PROVIDER));

			if (mapProvider instanceof MPProfile) {

				final MPProfile mpProfile = (MPProfile) mapProvider;

				// create xml for each map provider which is visible
				for (final MPWrapper mpWrapper : mpProfile.getAllWrappers()) {
					if (mpWrapper.isDisplayedInMap()) {

						writeXml(//
								mpWrapper.getMP(),
								xmlMemento.createChild(ROOT_CHILD_TAG_WRAPPED_MAP_PROVIDER));
					}
				}
			}

			xmlMemento.save(writer);

		} catch (final IOException e) {
			StatusUtil.log(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	/**
	 * @return Returns the backend list with all available map providers, including empty map
	 *         provider and map profiles
	 */
	public ArrayList<MP> getAllMapProviders() {

		checkMapProviders();

		return _allMapProviders;
	}

	/**
	 * @param withMapProfile
	 *            when <code>true</code> the map profiles are included otherwise they are ignored
	 * @return Returns a list with all available map providers but without empty map providers
	 */
	public ArrayList<MP> getAllMapProviders(final boolean withMapProfile) {

		checkMapProviders();

		final ArrayList<MP> mapProviders = new ArrayList<MP>();

		for (final MP mapProvider : _allMapProviders) {

			boolean isValid = true;

			if (mapProvider instanceof MPProfile) {
				isValid = withMapProfile;
			}

			if (isValid) {
				mapProviders.add(mapProvider);
			}
		}

		return mapProviders;
	}

	/**
	 * @return Return the default map provider which is currently OpenstreetMap
	 */
	public MPPlugin getDefaultMapProvider() {
		return _mpDefault;
	}

	/**
	 * convert {@link PART_TYPE} into a string which can be saved in xml file
	 * 
	 * @param partType
	 * @return String representation of the {@link PART_TYPE} enum
	 */
	private String getPartType(final PART_TYPE partType) {

		switch (partType) {

		case HTML:
			return PART_TYPE_HTML;

		case X:
			return PART_TYPE_X;
		case Y:
			return PART_TYPE_Y;

//		case LAT_TOP:
//			return PART_TYPE_LAT_TOP;
//		case LAT_BOTTOM:
//			return PART_TYPE_LAT_BOTTOM;
//
//		case LON_LEFT:
//			return PART_TYPE_LON_LEFT;
//		case LON_RIGHT:
//			return PART_TYPE_LON_RIGHT;

		case ZOOM:
			return PART_TYPE_ZOOM;

		case RANDOM_INTEGER:
			return PART_TYPE_RANDOM_INTEGER;
		case RANDOM_ALPHA:
			return PART_TYPE_RANDOM_ALPHA;

		}

		return null;
	}

	/**
	 * convert part type from string into {@link PART_TYPE} enum
	 * 
	 * @param partTypeText
	 * @return
	 */
	private PART_TYPE getPartType(final String partTypeText) {

		if (partTypeText.equalsIgnoreCase(PART_TYPE_HTML)) {
			return PART_TYPE.HTML;

		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_X)) {
			return PART_TYPE.X;
		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_Y)) {
			return PART_TYPE.Y;

//		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_LAT_TOP)) {
//			return PART_TYPE.LAT_TOP;
//		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_LAT_BOTTOM)) {
//			return PART_TYPE.LAT_BOTTOM;
//
//		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_LON_LEFT)) {
//			return PART_TYPE.LON_LEFT;
//		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_LON_RIGHT)) {
//			return PART_TYPE.LON_RIGHT;

		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_ZOOM)) {
			return PART_TYPE.ZOOM;

		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_RANDOM_INTEGER)) {
			return PART_TYPE.RANDOM_INTEGER;
		} else if (partTypeText.equalsIgnoreCase(PART_TYPE_RANDOM_ALPHA)) {
			return PART_TYPE.RANDOM_ALPHA;
		}

		StatusUtil.showStatus(NLS.bind(Messages.DBG006_Error_Custom_InvalidPartType, partTypeText), new Exception());
		return null;
	}

	/**
	 * @param importFilePath
	 * @return Returns the imported map provider or <code>null</code> when an import error occured<br>
	 * <br>
	 *         Multiple map providers are returned when a map profile contains map providers which
	 *         do not yet exists
	 */
	public ArrayList<MP> importMapProvider(final String importFilePath) {

		final ArrayList<MP> importedMPList = readXml1(importFilePath, true, true);
		if (importedMPList.size() > 0) {

			// validate map provider
			return validateImportedMP(importedMPList);
		}

		return null;
	}

	private void logError(final String errorText, final Exception exception) {
		StatusUtil.log(errorText, exception);
		_errorLog.add(errorText);
	}

	/**
	 * Read map provider list from a xml file
	 * 
	 * @param filename
	 * @param isShowExistError
	 * @param isMpImport
	 *            is <code>true</code> when a map provider should be imported
	 * @return Returns a list with all map providers from a xml file including wrapped plugin map
	 *         provider
	 */
	private ArrayList<MP> readXml1(final String filename, final boolean isShowExistError, final boolean isMpImport) {

		final ArrayList<MP> validMapProviders = new ArrayList<MP>();
		InputStreamReader reader = null;
		_errorLog.clear();

		try {

			// check if file is available
			final File inputFile = new File(filename);
			if (inputFile.exists() == false) {

				if (isShowExistError) {
					logError(Messages.DBG007_Error_FileIsNotAvailable, new Exception());
				}

				return validMapProviders;
			}

			reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
			final XMLMemento mementoRoot = XMLMemento.createReadRoot(reader);

			// check if this is an exported map provider
			if (isMpImport) {
				final Boolean isExport = mementoRoot.getBoolean(ATTR_ROOT_IS_MANUAL_EXPORT);
				if (isExport == null || isExport == false) {
					logError(Messages.DBG039_Error_FileIsNotExported, new Exception());

					return validMapProviders;
				}
			}

			readXml2(validMapProviders, mementoRoot, ROOT_CHILD_TAG_MAP_PROVIDER);

			if (isMpImport) {
				readXml2(validMapProviders, mementoRoot, ROOT_CHILD_TAG_WRAPPED_MAP_PROVIDER);
			}

		} catch (final UnsupportedEncodingException e) {
			logError(e.getMessage(), e);
		} catch (final FileNotFoundException e) {
			logError(e.getMessage(), e);
		} catch (final WorkbenchException e) {
			logError(e.getMessage(), e);
		} catch (final NumberFormatException e) {
			logError(e.getMessage(), e);
		} catch (final Exception e) {
			logError(e.getMessage(), e);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}

			displayError(filename);
		}

		return validMapProviders;
	}

	/**
	 * @param validMapProviders
	 * @param mementoRoot
	 * @param tagNameRootChildren
	 */
	private void readXml2(	final ArrayList<MP> validMapProviders,
							final XMLMemento mementoRoot,
							final String tagNameRootChildren) {

		final ArrayList<MP> allMapProviders = getAllMapProviders();
		final IMemento[] tagMapProviderList = mementoRoot.getChildren(tagNameRootChildren);

		for (final IMemento tagMapProvider : tagMapProviderList) {

			/*
			 * get common fields
			 */
			final String mapProviderId = tagMapProvider.getString(ATTR_MP_ID);
			final String mapProviderName = tagMapProvider.getString(ATTR_MP_NAME);
			final String mapProviderType = tagMapProvider.getString(ATTR_MP_TYPE);

			final String offlineFolder = tagMapProvider.getString(ATTR_MP_OFFLINE_FOLDER);
			final String description = tagMapProvider.getString(ATTR_MP_DESCRIPTION);

			final Integer imageSize = tagMapProvider.getInteger(ATTR_MP_IMAGE_SIZE);
			final String imageFormat = tagMapProvider.getString(ATTR_MP_IMAGE_FORMAT);

			// zoom level
			final Integer zoomMin = tagMapProvider.getInteger(ATTR_MP_ZOOM_LEVEL_MIN);
			final Integer zoomMax = tagMapProvider.getInteger(ATTR_MP_ZOOM_LEVEL_MAX);

			// favorite position
			final Integer favoriteZoom = tagMapProvider.getInteger(ATTR_MP_FAVORITE_ZOOM_LEVEL);
			final Float favoriteLatitude = tagMapProvider.getFloat(ATTR_MP_FAVORITE_LATITUDE);
			final Float favoriteLongitude = tagMapProvider.getFloat(ATTR_MP_FAVORITE_LONGITUDE);

			// last used position
			final Integer lastUsedZoom = tagMapProvider.getInteger(ATTR_MP_LAST_USED_ZOOM_LEVEL);
			final Float lastUsedLatitude = tagMapProvider.getFloat(ATTR_MP_LAST_USED_LATITUDE);
			final Float lastUsedLongitude = tagMapProvider.getFloat(ATTR_MP_LAST_USED_LONGITUDE);

			// check common fields
			if (mapProviderId == null || mapProviderName == null || offlineFolder == null || mapProviderType == null) {

				logError(NLS.bind(//
						Messages.DBG008_Error_TagIsInvalid,
						mapProviderId,
						tagNameRootChildren), new Exception());
				continue;
			}

			// check if the factory id is already used, ignore the duplicated factory
			boolean isValid = true;
			for (final MP checkMapProvider : validMapProviders) {
				if (checkMapProvider.getId().equalsIgnoreCase(mapProviderId)) {
					logError(NLS.bind(//
							Messages.DBG009_Error_MapProfileDuplicate,
							mapProviderId), new Exception());

					isValid = false;
					break;
				}
			}
			if (isValid == false) {
				continue;
			}

			// check plugin map provider, they are also added to the list
			if (mapProviderType.equals(MAP_PROVIDER_TYPE_PLUGIN)) {

				for (final MP mp : allMapProviders) {
					if (mp.getId().equalsIgnoreCase(mapProviderId) && mp instanceof MPPlugin) {

						validMapProviders.add(mp);
						isValid = true;

						break;
					}
				}

				if (isValid == false) {
					logError(NLS.bind(//
							Messages.DBG027_ImportError_InvalidPlugin,
							mapProviderId), new Exception());
				}

				continue;
			}

			// check map provider type
			if (mapProviderType.equals(MAP_PROVIDER_TYPE_CUSTOM) == false
					&& mapProviderType.equals(MAP_PROVIDER_TYPE_WMS) == false
					&& mapProviderType.equals(MAP_PROVIDER_TYPE_MAP_PROFILE) == false) {
				logError(NLS.bind(//
						Messages.DBG010_Error_InvalidType,
						mapProviderId,
						mapProviderType), new Exception());
				continue;
			}

			/*
			 * read map provider specific fields
			 */
			MP mapProvider = null;

			if (mapProviderType.equals(MAP_PROVIDER_TYPE_CUSTOM)) {

				// custom map provider
				mapProvider = readXmlCustom(tagMapProvider, mapProviderId);

			} else if (mapProviderType.equals(MAP_PROVIDER_TYPE_WMS)) {

				// wms map provider
				mapProvider = readXmlWms(tagMapProvider, mapProviderId, tagNameRootChildren);

			} else if (mapProviderType.equals(MAP_PROVIDER_TYPE_MAP_PROFILE)) {

				// map profile
				mapProvider = readXmlProfile(tagMapProvider, mapProviderId);
			}

			/*
			 * set common fields
			 */
			if (mapProvider != null) {

				// id
				mapProvider.setId(mapProviderId);
				mapProvider.setName(mapProviderName);
				mapProvider.setDescription(description == null ? UI.EMPTY_STRING : description);
				mapProvider.setOfflineFolder(offlineFolder);

				// image
				mapProvider.setTileSize(imageSize == null ? Integer.parseInt(DEFAULT_IMAGE_SIZE) : imageSize);
				mapProvider.setImageFormat(imageFormat == null ? DEFAULT_IMAGE_FORMAT : imageFormat);

				// zoom level
				final int minZoom = zoomMin == null ? 0 : zoomMin;
				final int maxZoom = zoomMax == null ? 17 : zoomMax;
				mapProvider.setZoomLevel(minZoom, maxZoom);

				// favorite position
				mapProvider.setFavoriteZoom(favoriteZoom == null ? 0 : favoriteZoom);
				mapProvider.setFavoritePosition(new GeoPosition(
						favoriteLatitude == null ? 0.0 : favoriteLatitude,
						favoriteLongitude == null ? 0.0 : favoriteLongitude));

				// last used position
				mapProvider.setLastUsedZoom(lastUsedZoom == null ? 0 : lastUsedZoom);
				mapProvider.setLastUsedPosition(new GeoPosition(
						lastUsedLatitude == null ? 0.0 : lastUsedLatitude,
						lastUsedLongitude == null ? 0.0 : lastUsedLongitude));

				validMapProviders.add(mapProvider);
			}
		}
	}

	/**
	 * @param mementoMapProvider
	 * @param mapProviderId
	 * @return
	 */
	private MPCustom readXmlCustom(final IMemento mementoMapProvider, final String mapProviderId) {

		final MPCustom mapProvider = new MPCustom();

		/*
		 * custom map provider specific fields
		 */
		final String customUrl = mementoMapProvider.getString(ATTR_CUSTOM_CUSTOM_URL);

		/*
		 * url parts
		 */
		final ArrayList<UrlPart> urlParts = new ArrayList<UrlPart>();

		// loop: all url parts which are defined in the xml file
		final IMemento[] tagParts = mementoMapProvider.getChildren(TAG_URL_PART);
		for (final IMemento partTag : tagParts) {

			final String partTypeText = partTag.getString(ATTR_CUSTOM_PART_TYPE);
			final Integer partPosition = partTag.getInteger(ATTR_CUSTOM_PART_POSITION);

			// check tag attributes
			if (partTypeText == null || partPosition == null) {
				logError(
						NLS.bind(Messages.DBG011_Error_Custom_InvalidPropertied, mapProviderId, TAG_URL_PART),
						new Exception());
			}

			final UrlPart urlPart = new UrlPart();
			final PART_TYPE partType = getPartType(partTypeText);

			urlPart.setPartType(partType);
			urlPart.setPosition(partPosition);

			// get part type specific fields
			switch (partType) {

			case HTML:
				final String html = partTag.getString(ATTR_CUSTOM_PART_CONTENT_HTML);
				if (html == null) {
					continue;
				}
				urlPart.setHtml(html);
				break;

			case RANDOM_INTEGER:
				final Integer randIntStart = partTag.getInteger(ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_START);
				final Integer randIntEnd = partTag.getInteger(ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_END);
				if (randIntStart == null || randIntEnd == null) {
					continue;
				}
				urlPart.setRandomIntegerStart(randIntStart);
				urlPart.setRandomIntegerStart(randIntEnd);
				break;

			case RANDOM_ALPHA:
				final String randAlphaStart = partTag.getString(ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_START);
				final String randAlphaEnd = partTag.getString(ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_END);
				if (randAlphaStart == null || randAlphaEnd == null) {
					continue;
				}
				urlPart.setRandomAlphaStart(randAlphaStart);
				urlPart.setRandomAlphaStart(randAlphaEnd);
				break;

			default:
				break;
			}

			// add validated part
			urlParts.add(urlPart);
		}

		// sort parts by position
		Collections.sort(urlParts, new Comparator<UrlPart>() {
			@Override
			public int compare(final UrlPart p1, final UrlPart p2) {
				return p1.getPosition() - p2.getPosition();
			}
		});

		/*
		 * update model
		 */
		mapProvider.setCustomUrl(customUrl == null ? UI.EMPTY_STRING : customUrl);
		mapProvider.setUrlParts(urlParts);

		return mapProvider;
	}

	/**
	 * Reads the map profile items from the xml file. The contained map providers cannot be set
	 * because they are not all loaded at the current time. <br>
	 * <br>
	 * The method {@link MPProfile#synchronizeMapProviders(MapProviderManager)} must be called to
	 * initialize the {@link MPProfile}
	 * 
	 * @param tagMapProvider
	 * @param mapProviderId
	 * @return
	 */
	private MPProfile readXmlProfile(final IMemento tagMapProvider, final String mapProviderId) {

		final ArrayList<MPWrapper> mpWrapperList = new ArrayList<MPWrapper>();
		final MPProfile mapProfile = new MPProfile(mpWrapperList);

		final Integer backgroundColor = tagMapProvider.getInteger(ATTR_PMP_BACKGROUND_COLOR);
		mapProfile.setBackgroundColor(backgroundColor == null ? 0xFFFFFF : backgroundColor);

		final IMemento[] tagProfileMapProviderList = tagMapProvider.getChildren(TAG_MAP_PROVIDER_WRAPPER);

		// loop: all map provider wrapper within a map profile
		for (final IMemento tagProfileMapProvider : tagProfileMapProviderList) {

			final String mpType = tagProfileMapProvider.getString(ATTR_PMP_MAP_PROVIDER_TYPE);
			final String mpId = tagProfileMapProvider.getString(ATTR_PMP_MAP_PROVIDER_ID);
			final Integer positionIndex = tagProfileMapProvider.getInteger(ATTR_PMP_POSITION);
			final Boolean isDisplayed = tagProfileMapProvider.getBoolean(ATTR_PMP_IS_DISPLAYED);
			final Integer alpha = tagProfileMapProvider.getInteger(ATTR_PMP_ALPHA);

			final Boolean isTransparent = tagProfileMapProvider.getBoolean(ATTR_PMP_IS_TRANSPARENT);
			final Boolean isTransBlack = tagProfileMapProvider.getBoolean(ATTR_PMP_IS_BLACK_TRANSPARENT);
			final Boolean isBrightnessForNextMp = tagProfileMapProvider.getBoolean(ATTR_PMP_IS_BRIGHTNESS_FOR_NEXT_MP);
			final Integer brightnessForNextMp = tagProfileMapProvider.getInteger(ATTR_PMP_BRIGHTNESS_FOR_NEXT_MP);

			// transparent colors
			final IMemento[] tagTransColor = tagProfileMapProvider.getChildren(TAG_TRANSPARENT_COLOR);
			final int[] transColors = new int[tagTransColor.length];
			int colorIndex = 0;
			for (final IMemento mementoTransColor : tagTransColor) {
				final Integer colorValue = mementoTransColor.getInteger(ATTR_TRANSPARENT_COLOR_VALUE);
				transColors[colorIndex++] = colorValue == null ? 0 : colorValue;
			}

			// validate fields
			if (mpId == null || mpType == null || positionIndex == null || isDisplayed == null) {
				logError(NLS.bind(
						Messages.DBG012_Error_Profile_InvalidAttributes,
						mapProviderId,
						TAG_MAP_PROVIDER_WRAPPER), new Exception());
				continue;
			}
			// check if a map provider id is already in the list
			boolean isIdValid = true;
			for (final MPWrapper mpWrapper : mpWrapperList) {
				if (mpWrapper.getMapProviderId().equalsIgnoreCase(mpId)) {
					isIdValid = false;
					break;
				}
			}
			if (isIdValid == false) {
				logError(NLS.bind(//
						Messages.DBG013_Error_Profile_DuplicateMP,
						new Object[] { mapProviderId, mpId, TAG_MAP_PROVIDER_WRAPPER }), new Exception());
				continue;
			}

			// check map provider type
			if (mpType.equals(MAP_PROVIDER_TYPE_CUSTOM) == false
					&& mpType.equals(MAP_PROVIDER_TYPE_WMS) == false
					&& mpType.equals(MAP_PROVIDER_TYPE_MAP_PROFILE) == false
					&& mpType.equals(MAP_PROVIDER_TYPE_PLUGIN) == false) {

				logError(NLS.bind(Messages.DBG014_Error_Profile_InvalidMPType, mapProviderId, mpType), new Exception());

				continue;
			}

			/*
			 * initial data are valid, create map provider wrapper
			 */
			final MPWrapper mpWrapper = new MPWrapper(mpId);

			mpWrapper.setType(mpType);
			mpWrapper.setIsDisplayedInMap(isDisplayed);
			mpWrapper.setPositionIndex(positionIndex);
			mpWrapper.setAlpha(alpha == null ? DEFAULT_ALPHA : alpha);
			mpWrapper.setIsTransparentColors(isTransparent == null ? false : isTransparent);
			mpWrapper.setIsTransparentBlack(isTransBlack == null ? false : isTransBlack);
			mpWrapper.setTransparentColors(transColors.length == 0 ? new int[] { OSM_BACKGROUND_COLOR } : transColors);
			mpWrapper.setIsBrightnessForNextMp(isBrightnessForNextMp == null ? false : isBrightnessForNextMp);
			mpWrapper.setBrightnessForNextMp(brightnessForNextMp == null ? 88 : brightnessForNextMp);

			mpWrapperList.add(mpWrapper);

			// set map provider specific fields

			if (mpType.equals(MAP_PROVIDER_TYPE_WMS)) {

				/*
				 * read wms layer state
				 */

				int displayedLayers = 0;
				final ArrayList<LayerOfflineData> wmsOfflineLayers = new ArrayList<LayerOfflineData>();

				for (final IMemento tagLayer : tagProfileMapProvider.getChildren(TAG_LAYER)) {

					final String layerName = tagLayer.getString(ATTR_LAYER_NAME);
					final String layerTitle = tagLayer.getString(ATTR_LAYER_TITLE);
					final Boolean layerIsDisplayed = tagLayer.getBoolean(ATTR_LAYER_IS_DISPLAYED);
					final Integer layerPosition = tagLayer.getInteger(ATTR_LAYER_POSITION);

					// validate properties
					if (layerName == null || layerTitle == null || layerIsDisplayed == null) {
						logError(
								NLS.bind(Messages.DBG015_Error_Profile_LayerInvalidProperties, new Object[] {
										mapProviderId,
										TAG_LAYER,
										mapProviderId }),
								new Exception());
						continue;
					}

					// check if a layer with the same name is already in the list, a layer MUST be unique
					boolean isLayerValid = true;
					{
						for (final LayerOfflineData layerOfflineData : wmsOfflineLayers) {
							if (layerOfflineData.name.equalsIgnoreCase(layerName)) {
								logError(NLS.bind(
										Messages.DBG016_Error_Profile_OfflineLayerInvalidProperties,
										new Object[] { mapProviderId, TAG_LAYER, mapProviderId }), new Exception());

								isLayerValid = false;
								break;
							}
						}
					}
					if (isLayerValid == false) {
						logError(
								NLS.bind(Messages.DBG017_Error_Profile_DuplicateLayer, new Object[] {
										mapProviderId,
										layerName,
										mpId }),
								new Exception());
						continue;
					}

					final LayerOfflineData offlineLayer = new LayerOfflineData();

					offlineLayer.name = layerName;
					offlineLayer.title = layerTitle;
					offlineLayer.isDisplayedInMap = layerIsDisplayed;
					offlineLayer.position = layerPosition == null ? -1 : layerPosition;

					wmsOfflineLayers.add(offlineLayer);

					if (layerIsDisplayed) {
						displayedLayers++;
					}
				}

				mpWrapper.setWmsOfflineLayerList(wmsOfflineLayers);
			}
		}

		return mapProfile;
	}

	/**
	 * @param mementoMapProvider
	 * @param mapProviderId
	 * @param tagNameRootChildren
	 * @return
	 */
	private MPWms readXmlWms(	final IMemento mementoMapProvider,
								final String mapProviderId,
								final String tagNameRootChildren) {

		final MPWms mapProvider = new MPWms();

		/*
		 * caps & maps url
		 */
		final String capsUrl = mementoMapProvider.getString(ATTR_WMS_CAPS_URL);
		final String mapUrl = mementoMapProvider.getString(ATTR_WMS_MAP_URL);

		if (capsUrl == null || mapUrl == null) {
			logError(
					NLS.bind(Messages.DBG018_Error_Wms_InvalidAttributes, mapProviderId, tagNameRootChildren),
					new Exception());
			return null;
		}

		mapProvider.setCapabilitiesUrl(capsUrl);
		mapProvider.setGetMapUrl(mapUrl);

		/*
		 * load transparent images
		 */
		Boolean isTransparent = mementoMapProvider.getBoolean(ATTR_WMS_LOAD_TRANSPARENT_IMAGES);
		if (isTransparent == null) {
			// set default
			isTransparent = false;
		}
		mapProvider.setTransparent(isTransparent);

		/*
		 * layer
		 */
		final IMemento[] tagLayers = mementoMapProvider.getChildren(TAG_LAYER);

//		int displayedLayers = 0;
		final ArrayList<LayerOfflineData> offlineLayerList = new ArrayList<LayerOfflineData>();
		for (final IMemento tagLayer : tagLayers) {

			final String layerName = tagLayer.getString(ATTR_LAYER_NAME);
			final String layerTitle = tagLayer.getString(ATTR_LAYER_TITLE);
			final Boolean layerIsDisplayed = tagLayer.getBoolean(ATTR_LAYER_IS_DISPLAYED);
			final Integer layerPosition = tagLayer.getInteger(ATTR_LAYER_POSITION);

			// validate properties
			if (layerName == null || layerTitle == null || layerIsDisplayed == null) {
				logError(NLS.bind(Messages.DBG019_Error_Wms_InvalidLayer, mapProviderId, TAG_LAYER), new Exception());
				continue;
			}

			// check if a layer with the same name is already in the list, a layer MUST be unique
			boolean isLayerValid = true;
			{
				for (final LayerOfflineData layerOfflineData : offlineLayerList) {
					if (layerOfflineData.name.equalsIgnoreCase(layerName)) {
						logError(
								NLS.bind(Messages.DBG020_Error_Wms_DuplicateLayer, mapProviderId, layerName),
								new Exception());
						isLayerValid = false;
						break;
					}
				}
			}
			if (isLayerValid == false) {
				continue;
			}

			final LayerOfflineData offlineLayer = new LayerOfflineData();

			offlineLayer.name = layerName;
			offlineLayer.title = layerTitle;
			offlineLayer.isDisplayedInMap = layerIsDisplayed;
			offlineLayer.position = layerPosition == null ? -1 : layerPosition;

			offlineLayerList.add(offlineLayer);

//			if (layerIsDisplayed) {
//				displayedLayers++;
//			}
		}
		mapProvider.setOfflineLayers(offlineLayerList);
//		mapProvider.setDisplayedLayers(displayedLayers);

		return mapProvider;
	}

	public void remove(final MP mapProvider) {
		_allMapProviders.remove(mapProvider);
	}

	public void removeMapProviderListener(final IMapProviderListener listener) {
		if (listener != null) {
			_mapProviderListeners.remove(listener);
		}
	}

	private void updateMpSorting(final MP newMP) {

		final String[] storedMpIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

		// check if the new mp is already in the list
		for (final String storedMpId : storedMpIds) {
			if (storedMpId.equals(newMP)) {
				// new mp is already in the list, this case should not happen
				return;
			}
		}

		final String[] newMpIds = new String[storedMpIds.length + 1];
		final String newMpName = newMP.getName();

		if (newMpName.startsWith(SINGLE_MAP_PROVIDER_NAME_PREFIX)) {

			// append at the end

			System.arraycopy(storedMpIds, 0, newMpIds, 0, storedMpIds.length);
			newMpIds[newMpIds.length - 1] = newMP.getId();

		} else {

			// append at the start

			newMpIds[0] = newMP.getId();
			System.arraycopy(storedMpIds, 0, newMpIds, 1, storedMpIds.length);
		}

		_prefStore.setValue(
				IMappingPreferences.MAP_PROVIDER_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(newMpIds));
	}

	/**
	 * Validates an imported map provider
	 * 
	 * @param importedMPList
	 *            contains all imported map provider including the plugin map provider
	 * @return Returns the valid map provider or profile map providers which are not yet created. <br>
	 *         Returns <code>null</code> when the map provider is not valid.
	 */
	private ArrayList<MP> validateImportedMP(final ArrayList<MP> importedMPList) {

		final ArrayList<MP> newMPs = new ArrayList<MP>();
		final ArrayList<MP> existingMPs = MapProviderManager.getInstance().getAllMapProviders(true);

		/*
		 * first map provider is the main map provider, the others are wrapped map providers
		 */
		final MP importedMP = importedMPList.get(0);

		// check imported mp against existing mp's
		for (final MP mp : existingMPs) {

			// check ID
			if (mp.equals(importedMP)) {

				// duplicate ID
				MessageDialog.openError(
						Display.getDefault().getActiveShell(),
						Messages.Import_Error_Dialog_Title,
						NLS.bind(Messages.DBG021_Import_Error_DuplicateId, mp.getId()));

				return null;
			}

			// check offline folder
			final String importedOfflineFolder = importedMP.getOfflineFolder();
			if (mp.getOfflineFolder().equalsIgnoreCase(importedOfflineFolder)) {

				// duplicate folder
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.Import_Error_Dialog_Title, NLS
						.bind(Messages.DBG022_Import_Error_DuplicateOfflineFolder, mp.getId(), mp.getOfflineFolder()));

				return null;
			}
		}

		// update model with the imported MP
		_allMapProviders.add(importedMP);

		updateMpSorting(importedMP);

		/*
		 * the imported map provider will be the first in the list, imported wrapped mp's come
		 * afterwards
		 */
		newMPs.add(importedMP);

		if (importedMP instanceof MPCustom) {

			return newMPs;

		} else if (importedMP instanceof MPWms) {

			return newMPs;

		} else if (importedMP instanceof MPProfile) {

			return validateImportedProfile(importedMPList, newMPs);
		}

		return null;
	}

	/**
	 * @param importedMP
	 * @param newMPs
	 * @return
	 */
	private ArrayList<MP> validateImportedProfile(final ArrayList<MP> importedMPList, final ArrayList<MP> newMPs) {

		final MPProfile importedMP = (MPProfile) importedMPList.get(0);

		// remove profile mp
		importedMPList.remove(0);

		/*
		 * this list contains map providers which are wrapped in the profile but must be defined as
		 * a ROOT_CHILD_TAG_WRAPPED_MAP_PROVIDER, this includes plugin map provider
		 */
		final ArrayList<MP> importedMpWrapperList = new ArrayList<MP>(importedMPList);

		final ArrayList<MP> existingMPs = MapProviderManager.getInstance().getAllMapProviders(true);
		final MPProfile importedMpProfile = importedMP;
		final ArrayList<MPWrapper> importedProfileWrappers = importedMpProfile.getAllWrappers();

		// check all wrappers
		for (final MPWrapper importedWrapper : importedProfileWrappers) {

			// check wrapper map provider

			MP wrappedMP = null;

			// get map provider by id, wrapper and and map provider are linked with the id
			for (final MP importedWrappedMp : importedMpWrapperList) {
				if (importedWrappedMp.getId().equalsIgnoreCase(importedWrapper.getMapProviderId())) {
					wrappedMP = importedWrappedMp;
					break;
				}
			}

			if (wrappedMP == null) {

				// mp wrapper is not defined in the export file
				MessageDialog.openError(Display.getDefault().getActiveShell(),//
						Messages.Import_Error_Dialog_Title,
						NLS.bind(Messages.DBG023_Import_Error_WrappedMpIsNotDefined, new Object[] {
								importedMP.getId(),
								importedWrapper.getMapProviderId() }));
				return null;
			}

			final String importedWrapperMPClassName = wrappedMP.getClass().getName();

			MP checkedMP = null;

			// check if the imported wrapper mp is available in the existing mp's
			for (final MP existingMp : existingMPs) {

				// check ID/class/folder

				if (existingMp.getId().equalsIgnoreCase(wrappedMP.getId())) {

					// same ID

					// check class
					if (existingMp.getClass().getName().equals(importedWrapperMPClassName)) {

						// same class

						// check folder for none Wms map provider, profile wms map providers are using another folder structure
						if (wrappedMP instanceof MPWms) {

							// the imported wrapper MP is an existing map provider
							checkedMP = wrappedMP;

							break;

						} else {

							// check offline folder
							if (existingMp.getOfflineFolder().equalsIgnoreCase(wrappedMP.getOfflineFolder())) {

								// same offline folder

								// the imported wrapper MP is an existing map provider
								checkedMP = wrappedMP;

								break;

							} else {

								// same ID/class but other offline folder

								MessageDialog.openError(
										Display.getDefault().getActiveShell(),
										Messages.Import_Error_Dialog_Title,
										NLS.bind(
												Messages.DBG024_Import_Error_ProfileDuplicateOfflineFolder,
												new Object[] {
														importedMP.getId(),
														wrappedMP.getId(),
														wrappedMP.getOfflineFolder(),
														existingMp.getId() }));
								return null;
							}
						}

					} else {

						// same ID but another class

						MessageDialog.openError(
								Display.getDefault().getActiveShell(),
								Messages.Import_Error_Dialog_Title,
								NLS.bind(Messages.DBG025_Import_Error_DifferentClass, new Object[] {
										importedMP.getId(),
										wrappedMP.getId(),
										importedWrapperMPClassName,
										existingMp.getClass().getName() }));

						return null;
					}
				}
			}

			if (checkedMP == null) {

				// imported wrapper mp do not yet exist and will be created

				// a plugin wrapper map provider must exist in the application
				if (wrappedMP instanceof MPPlugin) {

					MessageDialog.openError(
							Display.getDefault().getActiveShell(),
							Messages.Import_Error_Dialog_Title,
							NLS.bind(
									Messages.DBG040_Import_Error_PluginMPIsNotAvailable,
									new Object[] { importedMP.getId(), wrappedMP.getId() }));

					return null;
				}

				if ((wrappedMP instanceof MPWms) == false) {

					// check folder for none Wms map provider, profile wms map providers are using another folder structure

					for (final MP existingMp : existingMPs) {

						if (existingMp.getOfflineFolder().equalsIgnoreCase(wrappedMP.getOfflineFolder())) {

							// folder is already used by another mp

							MessageDialog.openError(
									Display.getDefault().getActiveShell(),
									Messages.Import_Error_Dialog_Title,
									NLS.bind(Messages.DBG026_Import_Error_ProfileDuplicateOfflineFolder, new Object[] {
											importedMP.getId(),
											wrappedMP.getId(),
											wrappedMP.getOfflineFolder(),
											existingMp.getId() }));

							return null;
						}
					}
				}

				newMPs.add(wrappedMP);

				// update model with the wrapped map provider
				_allMapProviders.add(wrappedMP);

				updateMpSorting(wrappedMP);
			}
		}

		return newMPs;
	}

	public void writeMapProviderXml() {

		BufferedWriter writer = null;

		try {

			final IPath stateLocation = Platform.getStateLocation(TourbookPlugin.getDefault().getBundle());
			final File file = stateLocation.append(CUSTOM_MAP_PROVIDER_FILE_NAME).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UI.UTF_8));

			final XMLMemento xmlMemento = createXmlRoot(false);

			for (final MP mapProvider : _allMapProviders) {

				if (mapProvider instanceof MPWms || mapProvider instanceof MPCustom || mapProvider instanceof MPProfile) {

					// a plugin map provider cannot be modified, save only none plugin map provider

					final IMemento tagMapProvider = xmlMemento.createChild(ROOT_CHILD_TAG_MAP_PROVIDER);

					writeXml(mapProvider, tagMapProvider);
				}
			}

			xmlMemento.save(writer);

		} catch (final IOException e) {
			StatusUtil.log(e);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}

		fireChangeEvent();
	}

	private void writeXml(final MP mp, final IMemento tagMapProvider) {

		final GeoPosition lastUsedPosition = mp.getLastUsedPosition();
		final GeoPosition favoritePosition = mp.getFavoritePosition();

		/*
		 * set common fields
		 */
		tagMapProvider.putString(ATTR_MP_ID, mp.getId());
		tagMapProvider.putString(ATTR_MP_NAME, mp.getName());
		tagMapProvider.putString(ATTR_MP_DESCRIPTION, mp.getDescription());
		tagMapProvider.putString(ATTR_MP_OFFLINE_FOLDER, mp.getOfflineFolder());

		// image
		tagMapProvider.putInteger(ATTR_MP_IMAGE_SIZE, mp.getTileSize());
		tagMapProvider.putString(ATTR_MP_IMAGE_FORMAT, mp.getImageFormat());

		// zoom level
		tagMapProvider.putInteger(ATTR_MP_ZOOM_LEVEL_MIN, mp.getMinZoomLevel());
		tagMapProvider.putInteger(ATTR_MP_ZOOM_LEVEL_MAX, mp.getMaxZoomLevel());

		// favorite position
		tagMapProvider.putInteger(ATTR_MP_FAVORITE_ZOOM_LEVEL, mp.getFavoriteZoom());
		tagMapProvider.putFloat(ATTR_MP_FAVORITE_LATITUDE, favoritePosition == null
				? 0.0f
				: (float) favoritePosition.latitude);
		tagMapProvider.putFloat(ATTR_MP_FAVORITE_LONGITUDE, favoritePosition == null
				? 0.0f
				: (float) favoritePosition.longitude);

		// last used position
		tagMapProvider.putInteger(ATTR_MP_LAST_USED_ZOOM_LEVEL, mp.getLastUsedZoom());
		tagMapProvider.putFloat(ATTR_MP_LAST_USED_LATITUDE, lastUsedPosition == null
				? 0.0f
				: (float) lastUsedPosition.latitude);
		tagMapProvider.putFloat(ATTR_MP_LAST_USED_LONGITUDE, lastUsedPosition == null
				? 0.0f
				: (float) lastUsedPosition.longitude);

		/*
		 * add special fields for each map provider
		 */
		if (mp instanceof MPWms) {

			writeXmlWms((MPWms) mp, tagMapProvider);

		} else if (mp instanceof MPCustom) {

			writeXmlCustom((MPCustom) mp, tagMapProvider);

		} else if (mp instanceof MPProfile) {

			writeXmlProfile((MPProfile) mp, tagMapProvider);

		} else if (mp instanceof MPPlugin) {

			// plugin mp can be exported in the map profile
			tagMapProvider.putString(ATTR_MP_TYPE, MAP_PROVIDER_TYPE_PLUGIN);
		}
	}

	private void writeXmlCustom(final MPCustom customMapProvider, final IMemento tagMapProvider) {

		tagMapProvider.putString(ATTR_MP_TYPE, MAP_PROVIDER_TYPE_CUSTOM);

		/*
		 * custom map provider specific fields
		 */
		tagMapProvider.putString(ATTR_CUSTOM_CUSTOM_URL, customMapProvider.getCustomUrl());

		/*
		 * url parts
		 */
		for (final UrlPart urlPart : customMapProvider.getUrlParts()) {

			final IMemento partTag = tagMapProvider.createChild(TAG_URL_PART);

			final PART_TYPE partType = urlPart.getPartType();

			partTag.putString(ATTR_CUSTOM_PART_TYPE, getPartType(partType));
			partTag.putInteger(ATTR_CUSTOM_PART_POSITION, urlPart.getPosition());

			switch (partType) {

			case HTML:
				partTag.putString(ATTR_CUSTOM_PART_CONTENT_HTML, urlPart.getHtml());
				break;

			case RANDOM_INTEGER:
				partTag.putInteger(ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_START, urlPart.getRandomIntegerStart());
				partTag.putInteger(ATTR_CUSTOM_PART_CONTENT_RANDOM_INTEGER_END, urlPart.getRandomIntegerEnd());
				break;

			case RANDOM_ALPHA:
				partTag.putString(ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_START, urlPart.getRandomAlphaStart());
				partTag.putString(ATTR_CUSTOM_PART_CONTENT_RANDOM_ALPHA_END, urlPart.getRandomAlphaEnd());
				break;

			default:
				break;
			}
		}
	}

	private void writeXmlProfile(final MPProfile mapProfile, final IMemento tagProfile) {

		tagProfile.putString(ATTR_MP_TYPE, MAP_PROVIDER_TYPE_MAP_PROFILE);

		tagProfile.putInteger(ATTR_PMP_BACKGROUND_COLOR, mapProfile.getBackgroundColor());

		/*
		 * map provider specific fields
		 */

		for (final MPWrapper mpWrapper : mapProfile.getAllWrappers()) {

			final MP mp = mpWrapper.getMP();

			final String mpType = getMapProviderType(mp);
			if (mpType == null) {
				continue;
			}

			final boolean isDisplayedInMap = mpWrapper.isDisplayedInMap();
			if (isDisplayedInMap == false) {
				// save only displayed map providers to reduce space
				continue;
			}

			final IMemento tagProfileMapProvider = tagProfile.createChild(TAG_MAP_PROVIDER_WRAPPER);

			tagProfileMapProvider.putString(ATTR_PMP_MAP_PROVIDER_TYPE, mpType);
			tagProfileMapProvider.putString(ATTR_PMP_MAP_PROVIDER_ID, mp.getId());
			tagProfileMapProvider.putInteger(ATTR_PMP_POSITION, mpWrapper.getPositionIndex());
			tagProfileMapProvider.putBoolean(ATTR_PMP_IS_DISPLAYED, isDisplayedInMap);
			tagProfileMapProvider.putInteger(ATTR_PMP_ALPHA, mpWrapper.getAlpha());
			tagProfileMapProvider.putBoolean(ATTR_PMP_IS_TRANSPARENT, mpWrapper.isTransparentColors());
			tagProfileMapProvider.putBoolean(ATTR_PMP_IS_BLACK_TRANSPARENT, mpWrapper.isTransparentBlack());
			tagProfileMapProvider.putBoolean(ATTR_PMP_IS_BRIGHTNESS_FOR_NEXT_MP, mpWrapper.isBrightnessForNextMp());
			tagProfileMapProvider.putInteger(ATTR_PMP_BRIGHTNESS_FOR_NEXT_MP, mpWrapper.getBrightnessValueForNextMp());

			// transparent colors
			final int[] transparentColors = mpWrapper.getTransparentColors();
			if (transparentColors != null) {

				// create a child for each color
				for (final int color : transparentColors) {

					// don't write black color this is with the attribute ATTR_PMP_IS_TRANSPARENT_BLACK
					if (color > 0) {
						tagProfileMapProvider//
								.createChild(TAG_TRANSPARENT_COLOR)
								.putInteger(ATTR_TRANSPARENT_COLOR_VALUE, color);
					}
				}
			}

			// wms layers
			if (mp instanceof MPWms) {
				writeXmlWmsLayers(tagProfileMapProvider, (MPWms) mp);
			}
		}

	}

	private void writeXmlWms(final MPWms wmsMapProvider, final IMemento tagMapProvider) {

		tagMapProvider.putString(ATTR_MP_TYPE, MAP_PROVIDER_TYPE_WMS);
		tagMapProvider.putString(ATTR_WMS_CAPS_URL, wmsMapProvider.getCapabilitiesUrl());
		tagMapProvider.putString(ATTR_WMS_MAP_URL, wmsMapProvider.getGetMapUrl());
		tagMapProvider.putBoolean(ATTR_WMS_LOAD_TRANSPARENT_IMAGES, wmsMapProvider.isTransparent());

		writeXmlWmsLayers(tagMapProvider, wmsMapProvider);

	}

	/**
	 * write all layers from the wms caps
	 */
	private void writeXmlWmsLayers(final IMemento parentTag, final MPWms wmsMapProvider) {

		final ArrayList<MtLayer> mtLayers = wmsMapProvider.getMtLayers();

		if (mtLayers == null) {

			// caps are not loaded from the server

			// write all layers from the offline info
			final ArrayList<LayerOfflineData> offlineLayers = wmsMapProvider.getOfflineLayers();
			if (offlineLayers != null) {

				for (final LayerOfflineData offlineLayer : offlineLayers) {

					final IMemento layerTag = parentTag.createChild(TAG_LAYER);

					layerTag.putString(ATTR_LAYER_NAME, offlineLayer.name);
					layerTag.putString(ATTR_LAYER_TITLE, offlineLayer.title);
					layerTag.putBoolean(ATTR_LAYER_IS_DISPLAYED, offlineLayer.isDisplayedInMap);
					layerTag.putInteger(ATTR_LAYER_POSITION, offlineLayer.position);
				}
			}

		} else {

			// caps are loaded from the wms server

			for (final MtLayer mtLayer : mtLayers) {

				final Layer layer = mtLayer.getGeoLayer();

				// check if the layer is drawable
				final String layerName = layer.getName();
				if (layerName != null && layerName.trim().length() > 0) {

					final IMemento layerTag = parentTag.createChild(TAG_LAYER);

					layerTag.putString(ATTR_LAYER_NAME, layerName);
					layerTag.putString(ATTR_LAYER_TITLE, layer.getTitle());
					layerTag.putBoolean(ATTR_LAYER_IS_DISPLAYED, mtLayer.isDisplayedInMap());
					layerTag.putInteger(ATTR_LAYER_POSITION, mtLayer.getPositionIndex());
				}
			}
		}
	}

}
