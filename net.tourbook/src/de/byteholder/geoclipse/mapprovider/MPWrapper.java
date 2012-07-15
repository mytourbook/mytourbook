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

import java.util.ArrayList;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.osgi.util.NLS;

/**
 * This is a wrapper for a map provider ({@link MP}) within a map profile ({@link MPProfile})
 */
public class MPWrapper implements Cloneable {
 
	/**
	 * unique id which identifies the map provider, this is a copy from the map provider
	 */
	private String						_mapProviderId;

	private MP							_mp;

	/**
	 * position index is the sorting position within the map profile viewer
	 */
	private int							_positionIndex				= -1;

	/**
	 * is <code>true</code> when this map provider is displayed in a map profile
	 */
	private boolean						_isDisplayedInMap;

	private ArrayList<LayerOfflineData>	_wmsOfflineLayerList;

	/**
	 * Contains the type of the map provider which is defined in
	 * {@link MapProviderManager#MAP_PROVIDER_TYPE_xxx}
	 */
	private String						_type;

	private boolean						_isEnabled					= true;

	/**
	 * alpha values for the map provider, 100 is opaque, 0 is transparent
	 */
	private int							_alpha						= 100;

	private boolean						_isTransparentColors		= false;
	private int[]						_transparentColor			= null;

	/**
	 * when <code>true</code> the color black is transparent
	 */
	private boolean						_isBlackTransparent;

	private boolean						_isBrightnessForNextMp		= false;
	private int							_brightnessValueForNextMp	= 82;

//	@SuppressWarnings("unused")
//	private MPWrapper() {}

	MPWrapper(final MP mapProvider) {

		_mapProviderId = mapProvider.getId();

		setMP(mapProvider);
	}

	MPWrapper(final String mapProviderId) {
		_mapProviderId = mapProviderId;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		// clone wrapper
		final MPWrapper clonedMpWrapper = (MPWrapper) super.clone();

		// clone map provider
		final MP clonedMP = (MP) _mp.clone();

		clonedMpWrapper._mp = clonedMP;

		return clonedMpWrapper;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MPWrapper)) {
			return false;
		}
		final MPWrapper other = (MPWrapper) obj;
		if (_mapProviderId == null) {
			if (other._mapProviderId != null) {
				return false;
			}
		} else if (!_mapProviderId.equals(other._mapProviderId)) {
			return false;
		}
		return true;
	}

	int getAlpha() {
		return _alpha;
	}

	int getBrightnessValueForNextMp() {
		return _brightnessValueForNextMp;
	}

	public String getMapProviderId() {
		return _mapProviderId;
	}

	/**
	 * @return Returns the map provider for the wrapper or <code>null</code> when it's not yet set
	 */
	public MP getMP() {

		if (_mp == null) {

			// create map provider

			final ArrayList<MP> allMp = MapProviderManager.getInstance().getAllMapProviders(false);

			for (final MP mp : allMp) {
				if (mp.getId().equalsIgnoreCase(_mapProviderId)) {
					try {

						_mp = (MP) mp.clone();

						MPProfile.updateMpFromWrapper(_mp, this);

						break;

					} catch (final CloneNotSupportedException e) {
						StatusUtil.showStatus(e);
					}
				}
			}

			// check map provider
			if (_mp == null) {
				StatusUtil.showStatus(NLS.bind("A map provider cannot be created in the wrapper \"{0}\"",//$NON-NLS-1$
						_mapProviderId), new Exception());
			}
		}

		return _mp;
	}

	int getPositionIndex() {
		return _positionIndex;
	}

	int[] getTransparentColors() {
		return _transparentColor;
	}

	String getType() {
		return _type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_mapProviderId == null) ? 0 : _mapProviderId.hashCode());
		return result;
	}

	boolean isBrightnessForNextMp() {
		return _isBrightnessForNextMp;
	}

	public boolean isDisplayedInMap() {
		return _isDisplayedInMap;
	}
 
	/**
	 * @return Returns <code>true</code> when this map provider can be used, <code>false</code> when
	 *         a wms server is not available
	 */
	boolean isEnabled() {
		return _isEnabled;
	}

	boolean isTransparentBlack() {
		return _isBlackTransparent;
	}

	boolean isTransparentColors() {
		return _isTransparentColors;
	}

	void setAlpha(final int alpha) {
		_alpha = alpha;
	}

	void setBrightnessForNextMp(final int brightnessValue) {
		_brightnessValueForNextMp = brightnessValue;
	}

	void setEnabled(final boolean isEnabled) {
		_isEnabled = isEnabled;
	}

	void setIsBrightnessForNextMp(final boolean isBrightness) {

//		System.out.println("MPWrapper:setIsBrightnessForNextMp\t" + _mapProviderId + "\t" + isBrightness);
//		// TODO remove SYSTEM.OUT.PRINTLN

		_isBrightnessForNextMp = isBrightness;
	}

	void setIsDisplayedInMap(final boolean isDisplayed) {
		_isDisplayedInMap = isDisplayed;
	}

	void setIsTransparentBlack(final boolean isBlackTransparent) {
		_isBlackTransparent = isBlackTransparent;
	}

	void setIsTransparentColors(final boolean isTransColors) {
		_isTransparentColors = isTransColors;
	}

	/**
	 * Sets a new factory id, this happens when it is modified in the UI
	 * 
	 * @param newFactoryId
	 */
	public void setMapProviderId(final String newFactoryId) {
		_mapProviderId = newFactoryId;
		_mp.setId(newFactoryId);
	}

	/**
	 * Sets a map provider into the wrapper
	 * 
	 * @param newMP
	 */
	void setMP(final MP newMP) {

		final MP oldMapProvider = _mp;
		_mp = newMP;

		if (newMP instanceof MPWms) {

			final MPWms newWmsMapProvider = (MPWms) newMP;
			final ArrayList<MtLayer> newMtLayers = newWmsMapProvider.getMtLayers();

			if (newMtLayers == null) {
				// wms is not loaded
				return;
			}

			if (oldMapProvider instanceof MPWms) {

				/*
				 * copy layer state from old to new wms map provider
				 */

				final MPWms oldWmsMapProvider = (MPWms) oldMapProvider;
				final ArrayList<MtLayer> oldMtLayers = oldWmsMapProvider.getMtLayers();

				if (oldMtLayers != null && newMtLayers != null) {

					// update all new layers
					for (final MtLayer newMtLayer : newMtLayers) {

						// set default
						newMtLayer.setIsDisplayedInMap(false);
						newMtLayer.setPositionIndex(-1);

						final String newId = newMtLayer.getGeoLayer().getName();

						// search new layer within the old layers
						for (final MtLayer oldMtLayer : oldMtLayers) {

							final String oldId = oldMtLayer.getGeoLayer().getName();
							if (newId.equals(oldId)) {

								// set state from old layer into new layer
								newMtLayer.setIsDisplayedInMap(oldMtLayer.isDisplayedInMap());
								newMtLayer.setPositionIndex(oldMtLayer.getPositionIndex());

								break;
							}
						}
					}
				}

			} else if (_wmsOfflineLayerList != null) {

				/*
				 * set layer state from offline data
				 */

				for (final MtLayer newMtLayer : newMtLayers) {

					final String newName = newMtLayer.getGeoLayer().getName();

					// search new layer within the offline layers
					for (final LayerOfflineData offlineLayer : _wmsOfflineLayerList) {

						final String offlineName = offlineLayer.name;
						if (offlineName.equalsIgnoreCase(newName)) {

							// update state

							newMtLayer.setIsDisplayedInMap(offlineLayer.isDisplayedInMap);
							newMtLayer.setPositionIndex(offlineLayer.position);

							break;
						}
					}
				}

			} else {

				/*
				 * set all layers to be not displayed
				 */

//				for (final MtLayer newMtLayer : newMtLayers) {
//					newMtLayer.setIsDisplayedInMap(false);
//					newMtLayer.setPositionIndex(-1);
//				}
			}
		}

		MPProfile.updateWrapperFromMp(this, _mp);
	}

	void setPositionIndex(final int positionIndex) {
		_positionIndex = positionIndex;
	}

	void setTransparentColors(final int[] transColors) {
		_transparentColor = transColors;
	}

	void setType(final String type) {
		_type = type;
	}

	void setWmsOfflineLayerList(final ArrayList<LayerOfflineData> wmsOfflineLayerList) {
		_wmsOfflineLayerList = wmsOfflineLayerList;
	}

	@Override
	public String toString() {
		return _mapProviderId + " pos:" + _positionIndex; //$NON-NLS-1$
	}

}
