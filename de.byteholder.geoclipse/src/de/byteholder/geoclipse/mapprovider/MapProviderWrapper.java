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

/**
 * This is a wrapper for a map provider ({@link MP}) within a map profile ({@link MPProfile})
 */
public class MapProviderWrapper implements Cloneable {

	/**
	 * unique id which identifies the map provider, this is a copy from the map provider
	 */
	private String						fMapProviderId;

	private MP							fMapProvider;

	/**
	 * position index is the sorting position within the map profile viewer
	 */
	private int							fPositionIndex			= -1;

	/**
	 * is <code>true</code> when this map provider is displayed in a map profile
	 */
	private boolean						fIsDisplayedInMap;

	private ArrayList<LayerOfflineData>	fWmsOfflineLayerList;

	/**
	 * Contains the type of the map provider which is defined in
	 * {@link MapProviderManager#MAP_PROVIDER_TYPE_xxx}
	 */
	private String						fType;

	/**
	 * alpha values for the map provider, 100 is opaque, 0 is transparent
	 */
	private int							fAlpha					= 100;

	private boolean						fIsTransparentColors	= false;
	private int[]						fTransparentColor		= null;

	/**
	 * when <code>true</code> the color black is transparent
	 */
	private boolean						fIsBlackTransparent;

	private boolean						fIsEnabled				= true;

	private boolean						fIsBrightness;
	private int							fBrightnessValue;

	@SuppressWarnings("unused")
	private MapProviderWrapper() {}

	public MapProviderWrapper(final MP mapProvider) {

		fMapProviderId = mapProvider.getId();

		setMapProvider(mapProvider);
	}

	public MapProviderWrapper(final String mapProviderId) {
		fMapProviderId = mapProviderId;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {

		final MapProviderWrapper clonedMpWrapper = (MapProviderWrapper) super.clone();

		final MP clonedMapProvider = (MP) fMapProvider.clone();

		clonedMpWrapper.fMapProvider = clonedMapProvider;

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
		if (!(obj instanceof MapProviderWrapper)) {
			return false;
		}
		final MapProviderWrapper other = (MapProviderWrapper) obj;
		if (fMapProviderId == null) {
			if (other.fMapProviderId != null) {
				return false;
			}
		} else if (!fMapProviderId.equals(other.fMapProviderId)) {
			return false;
		}
		return true;
	}

	public int getAlpha() {
		return fAlpha;
	}

	public int getBrightness() {
		return fBrightnessValue;
	}

	public MP getMapProvider() {

		// check map provider
//		if (fMapProvider == null) {
//			StatusUtil.showStatus("map provider is not set", new Exception());//$NON-NLS-1$
//		}

		return fMapProvider;
	}

	public String getMapProviderId() {
		return fMapProviderId;
	}

	public int getPositionIndex() {
		return fPositionIndex;
	}

	public int[] getTransparentColors() {
		return fTransparentColor;
	}

	public String getType() {
		return fType;
	}

	/**
	 * @return Returns layers which have been saved in the xml file
	 */
	public ArrayList<LayerOfflineData> getWmsOfflineLayerList() {
		return fWmsOfflineLayerList;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fMapProviderId == null) ? 0 : fMapProviderId.hashCode());
		return result;
	}

	public boolean isBrightness() {
		return fIsBrightness;
	}

	public boolean isDisplayedInMap() {
		return fIsDisplayedInMap;
	}

	/**
	 * @return Returns <code>true</code> when this map provider can be used, <code>false</code> when
	 *         a wms server is not available
	 */
	public boolean isEnabled() {
		return fIsEnabled;
	}

	public boolean isTransparentBlack() {
		return fIsBlackTransparent;
	}

	public boolean isTransparentColors() {
		return fIsTransparentColors;
	}

	public void setAlpha(final int alpha) {
		fAlpha = alpha;
	}

	public void setBrightness(final int brightnessValue) {
		fBrightnessValue = brightnessValue;
	}

	public void setEnabled(final boolean isEnabled) {
		fIsEnabled = isEnabled;
	}

	public void setIsBrightness(final boolean isBrightness) {
		fIsBrightness = isBrightness;
	}

	public void setIsDisplayedInMap(final boolean isDisplayed) {
		fIsDisplayedInMap = isDisplayed;
	}

	public void setIsTransparentBlack(final boolean isBlackTransparent) {
		fIsBlackTransparent = isBlackTransparent;
	}

	public void setIsTransparentColors(final boolean isTransColors) {
		fIsTransparentColors = isTransColors;
	}

	public void setMapProvider(final MP newMapProvider) {

		final MP oldMapProvider = fMapProvider;
		fMapProvider = newMapProvider;

		if (newMapProvider instanceof MPWms) {

			final MPWms newWmsMapProvider = (MPWms) newMapProvider;
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

			} else if (fWmsOfflineLayerList != null) {

				/*
				 * set layer state from offline data
				 */

				for (final MtLayer newMtLayer : newMtLayers) {

					final String newName = newMtLayer.getGeoLayer().getName();

					// search new layer within the offline layers
					for (final LayerOfflineData offlineLayer : fWmsOfflineLayerList) {

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

				for (final MtLayer newMtLayer : newMtLayers) {
					newMtLayer.setIsDisplayedInMap(false);
					newMtLayer.setPositionIndex(-1);
				}
			}
		}
	}

	/**
	 * Sets a new factory id, this happens when it is modified in the UI
	 * 
	 * @param newFactoryId
	 */
	public void setMapProviderId(final String newFactoryId) {
		fMapProviderId = newFactoryId;
		fMapProvider.setMapProviderId(newFactoryId);
	}

	public void setPositionIndex(final int positionIndex) {
		fPositionIndex = positionIndex;
	}

	public void setTransparentColors(final int[] transColors) {
		fTransparentColor = transColors;
	}

	public void setType(final String type) {
		fType = type;
	}

	public void setWmsOfflineLayerList(final ArrayList<LayerOfflineData> wmsOfflineLayerList) {
		fWmsOfflineLayerList = wmsOfflineLayerList;
	}

	@Override
	public String toString() {
		return fMapProviderId + " pos:" + fPositionIndex; //$NON-NLS-1$
	}

}
