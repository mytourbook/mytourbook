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

import java.io.IOException;
import java.io.InputStream;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.ows.ServiceException;

import de.byteholder.geoclipse.Messages;
import de.byteholder.geoclipse.logging.GeoException;
import de.byteholder.geoclipse.logging.StatusUtil;
import de.byteholder.geoclipse.map.BoundingBoxEPSG4326;
import de.byteholder.geoclipse.map.ITileLoader;
import de.byteholder.geoclipse.map.Tile;
import de.byteholder.geoclipse.map.UI;
import de.byteholder.geoclipse.util.Util;

public class MPWms extends MP implements ITileLoader {

	private static final ReentrantLock	MP_WMS_LOCK			= new ReentrantLock();

	private static final String			SRS_EPSG_4326		= "EPSG:4326";				//$NON-NLS-1$

	/*
	 * this is spherical mercator (OSM projection) but is only rarely supported
	 */
//	private static final String			SRS_EPSG_3857		= "EPSG:3857";				//$NON-NLS-1$

	// this is depricated
//	private static final String			SRS_EPSG_3785		= "EPSG:3785";				//$NON-NLS-1$
//	private static final String			SRS_EPSG_900913		= "EPSG:900913";			//$NON-NLS-1$

	private static final char			COMMA				= ',';

	private boolean						_isWmsAvailable		= true;

	/**
	 * wms server {@link #_wmsServer} and wms caps {@link #_wmsCaps} are set when a connection to
	 * the server was successfull, otherwise they are <code>null</code>
	 */
	private WebMapServer				_wmsServer;

	private WMSCapabilities				_wmsCaps;

	private String						_capsUrl			= UI.EMPTY_STRING;
	private String						_mapUrl;

	private List<String>				_allImageFormats;

	/**
	 * contains a list with all layer names, this is necessary to load offline images from the
	 * filesystem
	 */
	private ArrayList<LayerOfflineData>	_offlineLayers;

	/**
	 * Contains all layers which the wms server provides, these
	 * layers are sorted in the sequence how they are requested from the server
	 */
	private ArrayList<MtLayer>			_mtLayers;

	private ArrayList<MtLayer>			_mtLayersReverse	= new ArrayList<MtLayer>();

	private boolean						_isLoadTransparentImages;

	MPWms() {}

	@Override
	public Object clone() throws CloneNotSupportedException {

		final MPWms mapProvider = (MPWms) super.clone();

		mapProvider._mtLayers = cloneMtLayer(mapProvider, _mtLayers);
		mapProvider._mtLayersReverse = cloneMtLayer(mapProvider, _mtLayersReverse);

//		if (fMtLayers != null) {
//			mapProvider.initializeLayers();
//		}

		mapProvider._offlineLayers = cloneOfflineLayer(mapProvider, _offlineLayers);

		return mapProvider;
	}

	private ArrayList<MtLayer> cloneMtLayer(final MPWms mapProvider, final ArrayList<MtLayer> allMtLayers)
			throws CloneNotSupportedException {

		if (allMtLayers == null) {
			// layers are not yet retrieved from the wms server
			return null;
		}

		final ArrayList<MtLayer> newMtLayers = new ArrayList<MtLayer>();

		for (final MtLayer mtLayer : allMtLayers) {
			newMtLayers.add((MtLayer) mtLayer.clone());
		}

		return newMtLayers;
	}

	private ArrayList<LayerOfflineData> cloneOfflineLayer(	final MPWms mapProvider,
															final ArrayList<LayerOfflineData> offlineDataList)
			throws CloneNotSupportedException {

		if (offlineDataList == null) {
			// layers are not loaded from the xml file
			return null;
		}

		final ArrayList<LayerOfflineData> newOfflineLayers = new ArrayList<LayerOfflineData>();

		for (final LayerOfflineData offlineData : offlineDataList) {
			newOfflineLayers.add((LayerOfflineData) offlineData.clone());
		}

		return newOfflineLayers;
	}

	/**
	 * creates the BBOX string
	 */
	private String createBBox(final Tile tile) {

		final BoundingBoxEPSG4326 bbox = tile.getBbox();

		final StringBuilder sb = new StringBuilder();
		sb.append(bbox.left);
		sb.append(COMMA);
		sb.append(bbox.bottom);
		sb.append(COMMA);
		sb.append(bbox.right);
		sb.append(COMMA);
		sb.append(bbox.top);

		return sb.toString();
	}

	/**
	 * @return Returns the number of layers which are available in the mtlayers or offline layers
	 */
	public int getAvailableLayers() {

		if (_mtLayers != null) {
			return _mtLayers.size();
		} else {
			return _offlineLayers.size();
		}
	}

	public String getCapabilitiesUrl() {
		return _capsUrl;
	}

	/**
	 * create unique key for all visible layers
	 */
	@Override
	String getCustomTileKey() {

		final StringBuilder sb = new StringBuilder();
		int layerIndex = 0;

		if (_mtLayers == null) {

			// layers are not loaded from wms server

			if (_offlineLayers == null) {

				// this case should not happen
				StatusUtil.log("map and offline layers are null", new Exception()); //$NON-NLS-1$
				return null;
			}

			// add all layer names to the unique key
			for (final LayerOfflineData offlineLayer : _offlineLayers) {

				if (offlineLayer.isDisplayedInMap) {

					if (layerIndex > 0) {
						sb.append('-');
					}

					sb.append(offlineLayer.name);

					layerIndex++;
				}
			}

		} else {

			// create unique key from all visible layers
			for (final MtLayer mtLayer : _mtLayers) {
				if (mtLayer.isDisplayedInMap()) {

					if (layerIndex > 0) {
						sb.append(UI.DASH);
					}

					sb.append(mtLayer.getGeoLayer().getName());

					layerIndex++;
				}
			}
		}

		// remove invalid characters from the key
		final String customTileKey = UI.createIdFromName(sb.toString(), 150);

		return customTileKey;
	}

	String getGetMapUrl() {
		return _mapUrl;
	}

	List<String> getImageFormats() {
		return _allImageFormats;
	}

	/**
	 * @return Returns all available layers or <code>null</code> when the caps are not loaded from
	 *         the wms
	 */
	ArrayList<MtLayer> getMtLayers() {
		return _mtLayers;
	}

	/**
	 * @return Returns a list with all layers which are used to get offline info, returns
	 *         <code>null</code> when the map provider is not yet saved
	 */
	ArrayList<LayerOfflineData> getOfflineLayers() {
		return _offlineLayers;
	}

	public InputStream getTileImageStream(final Tile tile) throws GeoException {

		if (_wmsServer == null) {

			MP_WMS_LOCK.lock();
			try {
				// recheck again
				if (_wmsServer == null) {

					// load wms caps

					if (MapProviderManager.checkWms(this, null) == null) {
						throw new GeoException();
					}

					initializeLayers();
				}
			} finally {
				MP_WMS_LOCK.unlock();
			}
		}

		int visibleLayers = 0;
		final GetMapRequest mapRequest = _wmsServer.createGetMapRequest();
		for (final MtLayer mtLayer : _mtLayersReverse) {
			if (mtLayer.isDisplayedInMap()) {
				mapRequest.addLayer(mtLayer.getGeoLayer());
				visibleLayers++;
			}
		}

		if (visibleLayers == 0) {
			throw new GeoException(NLS.bind(Messages.DBG043_Wms_Server_Error_CannotConnectToServer, getId()));
		}

		final int imageSize = getTileSize();

		mapRequest.setDimensions(imageSize, imageSize);
		mapRequest.setFormat(Util.encodeSpace(getImageFormat()));

		// only WGS84/EPSG:4326 is currently supported
		mapRequest.setSRS(SRS_EPSG_4326);
//		mapRequest.setSRS(SRS_EPSG_3857);
		mapRequest.setBBox(createBBox(tile));
		mapRequest.setTransparent(_isLoadTransparentImages);

		// keep url
		final String finalUrl = mapRequest.getFinalURL().toString();
		tile.setUrl(finalUrl);

		try {

			final GetMapResponse response = _wmsServer.issueRequest(mapRequest);

			// reset the offline file counter to reread the offline files
			setStateToReloadOfflineCounter();

			return response.getInputStream();

		} catch (final NoRouteToHostException e) {
			throw new GeoException(NLS.bind(
					Messages.DBG035_Wms_Server_Error_CannotConnectToServer,
					e.getMessage(),
					finalUrl), e);

		} catch (final IOException e) {
			throw new GeoException(NLS.bind(//
					Messages.DBG036_Wms_Server_Error_IoExeption,
					e.getMessage(),
					finalUrl), e);

		} catch (final ServiceException e) {
			throw new GeoException(NLS.bind(//
					Messages.DBG037_Wms_Server_Error_ServiceExeption,
					e.getMessage(),
					finalUrl), e);

		} catch (final Exception e) {
			throw new GeoException(NLS.bind(//
					Messages.DBG038_Wms_Server_Error_CannotLoadImage,
					finalUrl), e);
		}
	}

	@Override
	public IPath getTileOSPath(final String fullPath, final Tile tile) {

		IPath filePath = new Path(fullPath);
		filePath = filePath.append(getOfflineFolder());

		// add custom tile key
		final String customTileKey = getCustomTileKey();
		if (customTileKey != null) {
			filePath = filePath.append(customTileKey);
		}

		filePath = filePath//
				.append(Integer.toString(tile.getZoom()))
				.append(Integer.toString(tile.getX()))
				.append(Integer.toString(tile.getY()))
				.addFileExtension(MapProviderManager.getImageFileExtension(getImageFormat()));

		return filePath;

	}

	/**
	 * @return Returns wms capabilities or <code>null</code> when they are not loaded
	 */
	public WMSCapabilities getWmsCaps() {
		return _wmsCaps;
	}

	/**
	 * Initialize and sort layers which are loaded from the wms server<br>
	 * <br>
	 * Sort layers in reverse order because the sorting order is used when the map's are loaded
	 * from the server and is used as offline file path
	 */
	void initializeLayers() {

		if (_mtLayers == null) {
			// wms is not yet loaded
			StatusUtil.showStatus("wms is not initialized", new Exception());//$NON-NLS-1$
			return;
		}

		_mtLayersReverse.clear();
		_mtLayersReverse.addAll(_mtLayers);

		Collections.sort(_mtLayersReverse, new Comparator<MtLayer>() {

			public int compare(final MtLayer mt1, final MtLayer mt2) {

				if (mt1.getPositionIndex() == -1 || mt2.getPositionIndex() == -1) {

					// sort by name
					return mt1.getGeoLayer().compareTo(mt2.getGeoLayer());

				} else {

					// sort by position 
					return -mt1.getPositionIndex() - -mt2.getPositionIndex();
				}
			}
		});

		/*
		 * sort none reverse layers, this sorting is used when the custom tile key is created
		 */
		Collections.sort(_mtLayers, new Comparator<MtLayer>() {

			public int compare(final MtLayer mt1, final MtLayer mt2) {

				if (mt1.getPositionIndex() == -1 || mt2.getPositionIndex() == -1) {

					// sort by name
					return mt1.getGeoLayer().compareTo(mt2.getGeoLayer());

				} else {

					// sort by position 
					return mt1.getPositionIndex() - mt2.getPositionIndex();
				}
			}
		});
	}

	/**
	 * initialize wms server, set wms server and caps and create the tile factory
	 * 
	 * @param wmsServer
	 * @param wmsCaps
	 * @param allMtLayers
	 *            all layers which can be displayed
	 */
	void initializeWms(final WebMapServer wmsServer, final WMSCapabilities wmsCaps, final ArrayList<MtLayer> allMtLayers) {

		_wmsServer = wmsServer;
		_wmsCaps = wmsCaps;

		_mtLayers = allMtLayers;

		_allImageFormats = _wmsCaps.getRequest().getGetMap().getFormats();

		if (getImageFormat() == null) {

			// set png format as default

			String imageFormat = null;

			for (final String format : _allImageFormats) {
				if (format.equalsIgnoreCase(MapProviderManager.DEFAULT_IMAGE_FORMAT)) {
					imageFormat = format;
				}
			}

			setImageFormat(imageFormat == null ? _allImageFormats.get(0) : imageFormat);
		}
	}

	boolean isTransparent() {
		return _isLoadTransparentImages;
	}

	/**
	 * @return Returns <code>false</code> when a connection to the WMS server failed
	 */
	public boolean isWmsAvailable() {
		return _isWmsAvailable;
	}

	public void setCapabilitiesUrl(final String capabilitiesUrl) {
		if (capabilitiesUrl != null) {
			_capsUrl = capabilitiesUrl;
		}
	}

	public void setGetMapUrl(final String mapUrl) {
		_mapUrl = mapUrl;
	}

	void setOfflineLayers(final ArrayList<LayerOfflineData> offlineLayers) {
		_offlineLayers = offlineLayers;
	}

	void setTransparent(final boolean isTransparent) {
		_isLoadTransparentImages = isTransparent;
	}

	public void setWmsEnabled(final boolean isEnabled) {
		_isWmsAvailable = isEnabled;
	}

}
