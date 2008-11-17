/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.swt.Map;

public class ActionSelectMapProvider extends Action implements IMenuCreator {

	private static final String							TOOLTIP_DASH		= " - ";

	private static final String							TOGGLE_MARKER		= " (x)";						//$NON-NLS-1$

	private static final String							DEFAULT_FACTORY_ID	= "osm";

	private static int									fFactoryCounter		= 0;

	private static IPreferenceStore						fPrefStore			= TourbookPlugin.getDefault()
																					.getPreferenceStore();

	private final MapView							fMappingView;

	private Menu										fMenu;
	private final ActionModifyMapProvider				fActionModifyMapProvider;

	private final HashMap<String, MapProviderAction>	fMapProviderActions;

	/**
	 * tile factory which is currently selected
	 */
	private TileFactory									fSelectedTileFactory;

	private ArrayList<MapProvider>						fSortedMapProviders;
	private ArrayList<MapProviderAction>				fSortedMapProviderActions;

	/**
	 * Action for a map provider
	 */
	private class MapProviderAction extends Action {

		private final MapProvider	mapProvider;
		private final String		label;
		private boolean				canBeToggled;

		public MapProviderAction(final MapProvider mapProvider, final String label) {

			super(label, AS_RADIO_BUTTON);

			this.mapProvider = mapProvider;
			this.label = label;
		}

		boolean isCanBeToggled() {
			return canBeToggled;
		}

		@Override
		public void run() {
			selectMapProviderInTheMap(mapProvider);
		}

		void setCanBeToggled(final boolean canBeToggled) {
			this.canBeToggled = canBeToggled;
		}
	}

	public ActionSelectMapProvider(final MapView mapView) {

		super(null, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fMappingView = mapView;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		/*
		 * create an action for each factory
		 */
		fMapProviderActions = new HashMap<String, MapProviderAction>();
		final List<MapProvider> factories = mapView.getFactories();

		for (final TileFactory tileFactory : factories) {
			fMapProviderActions.put(tileFactory.getInfo().getFactoryID(),
					new MapProviderAction(new MapProvider(tileFactory, tileFactory.getProjection()),
							tileFactory.getInfo().getFactoryName()));
		}

		fActionModifyMapProvider = new ActionModifyMapProvider(mapView);

		updateMapProviders();
	}

	private void addActionToMenu(final Action action) {
		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(final Control parent) {

		// recreate menu each time
		dispose();
		fMenu = new Menu(parent);

		// add all map providers
		for (final MapProvider mapProvider : fSortedMapProviders) {

			final MapProviderAction mapProviderAction = fMapProviderActions.get(mapProvider.getInfo().getFactoryID());

			updateToggleStateIntoAction(mapProvider, mapProviderAction);

			addActionToMenu(mapProviderAction);
		}

		(new Separator()).fill(fMenu, -1);
		addActionToMenu(fActionModifyMapProvider);

		return fMenu;
	}

	public Menu getMenu(final Menu parent) {
		return null;
	}

	/**
	 * @return Returns the tile factory which is currently selected
	 */
	public TileFactory getSelectedFactory() {
		if (fSelectedTileFactory == null) {
			return fMappingView.getFactories().get(0);
		}
		return fSelectedTileFactory;
	}

	@Override
	public void run() {

		/*
		 * toggle map providers
		 */

		// get a list of all map provider actions which can be toggled
		final ArrayList<MapProviderAction> toggleMapProviderActions = new ArrayList<MapProviderAction>();
		for (final MapProviderAction mapProviderAction : fSortedMapProviderActions) {
			if (mapProviderAction.isCanBeToggled()) {
				toggleMapProviderActions.add(mapProviderAction);
			}
		}

		MapProvider newMapProvider;

		if (toggleMapProviderActions.size() > 0) {

			// use custom toggle mechanism

			// get next factory
			final MapProviderAction action = toggleMapProviderActions.get(++fFactoryCounter
					% toggleMapProviderActions.size());
			newMapProvider = action.mapProvider;

		} else {

			// toggle all available map factories

			// get next factory
			final List<MapProvider> mapFactories = fMappingView.getFactories();
			newMapProvider = mapFactories.get(++fFactoryCounter % mapFactories.size());
		}

		// check map provider action for the selected map provider
		for (final MapProviderAction factoryAction : fSortedMapProviderActions) {
			if (factoryAction.mapProvider.getInfo().getFactoryID().equals(newMapProvider.getInfo().getFactoryID())) {
				factoryAction.setChecked(true);
			} else {
				factoryAction.setChecked(false);
			}
		}

		// select map provider in the map
		selectMapProviderInTheMap(newMapProvider);

		// update tooltip, show selected map provider
		setToolTipText(newMapProvider.getInfo().getFactoryName()
				+ TOOLTIP_DASH
				+ Messages.map_action_change_tile_factory_tooltip);
	}

	private void selectMapProviderInTheMap(final TileFactory mapProvider) {

		fSelectedTileFactory = mapProvider;

		final Map map = fMappingView.getMap();

//		map.disposeOverlayImageCache();

		map.setTileFactory(mapProvider);

		// reset overlays must be done after the new map provider is set
		map.resetOverlays();

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
		final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);

		map.setDimLevel(fMappingView.getMapDimLevel(), dimColor);
	}

	/**
	 * Select a tile factory by it's factory ID
	 * 
	 * @param factoryId
	 *            factory ID or <code>null</code> to select the default factory (OSM)
	 */
	public void setSelectedFactory(final String factoryId) {

		if (factoryId != null) {

			for (final MapProviderAction factoryAction : fMapProviderActions.values()) {

				final TileFactory mapProvider = factoryAction.mapProvider;
				fFactoryCounter++;

				// check facory ID
				if (mapProvider.getInfo().getFactoryID().equals(factoryId)) {

					// factory is available
					factoryAction.setChecked(true);
					selectMapProviderInTheMap(mapProvider);
					return;
				}
			}
		}

		/*
		 * factory is not available, get default map provider
		 */
		for (final MapProviderAction mapProviderAction : fMapProviderActions.values()) {
			final MapProvider mapProvider = mapProviderAction.mapProvider;
			if (mapProvider.getInfo().getFactoryID().equals(DEFAULT_FACTORY_ID)) {
				selectMapProviderInTheMap(mapProvider);
				return;
			}
		}

		/*
		 * factory is not available, get first factory
		 */
		final MapProviderAction mapProviderAction = fMapProviderActions.values().iterator().next();
		if (mapProviderAction != null) {

			mapProviderAction.setChecked(true);

			selectMapProviderInTheMap(mapProviderAction.mapProvider);
		}
	}

	/**
	 * @return update the map providers with information from the pref store, the customized order
	 *         and the toggle status
	 */
	void updateMapProviders() {

		final List<MapProvider> allMapProviders = fMappingView.getFactories();

		final String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
		fPrefStore.getString(ITourbookPreferences.MAP_PROVIDERS_SORT_ORDER));

		final ArrayList<MapProvider> mapProviders = new ArrayList<MapProvider>();

		// put all map providers into the viewer which are defined in the pref store
		for (final String storeMapProvider : storedProviderIds) {

			// find the stored map provider in the available map providers
			for (final MapProvider mapProvider : allMapProviders) {
				if (mapProvider.getInfo().getFactoryID().equals(storeMapProvider)) {
					mapProviders.add(mapProvider);
					break;
				}
			}
		}

		// make sure that all available map providers are in the viewer
		for (final MapProvider tileFactory : allMapProviders) {
			if (!mapProviders.contains(tileFactory)) {
				mapProviders.add(tileFactory);
			}
		}

		/*
		 * set status if the map provider can be toggled with the map provider button
		 */
		final String[] toggleIds = StringToArrayConverter.convertStringToArray(//
		fPrefStore.getString(ITourbookPreferences.MAP_PROVIDERS_TOGGLE_LIST));

		for (final MapProvider mapProvider : allMapProviders) {
			final String factoryId = mapProvider.getInfo().getFactoryID();

			for (final String toggleId : toggleIds) {
				if (factoryId.equals(toggleId)) {
					mapProvider.setCanBeToggled(true);
					break;
				}
			}
		}

		fSortedMapProviders = mapProviders;

		/*
		 * sort map provider actions
		 */
		final ArrayList<MapProviderAction> sortedMapProviderActions = new ArrayList<MapProviderAction>();

		// add all map providers
		for (final MapProvider mapProvider : fSortedMapProviders) {

			final MapProviderAction mapProviderAction = fMapProviderActions.get(mapProvider.getInfo().getFactoryID());

			updateToggleStateIntoAction(mapProvider, mapProviderAction);

			sortedMapProviderActions.add(mapProviderAction);
		}

		fSortedMapProviderActions = sortedMapProviderActions;
	}

	/**
	 * Update the toggle state from the map provider into the action
	 * 
	 * @param mapProvider
	 * @param mapProviderAction
	 */
	private void updateToggleStateIntoAction(final MapProvider mapProvider, final MapProviderAction mapProviderAction) {

		final boolean canBeToggled = mapProvider.canBeToggled();

		// update action label
		if (canBeToggled) {
			mapProviderAction.setText(mapProviderAction.label + TOGGLE_MARKER);
		} else {
			mapProviderAction.setText(mapProviderAction.label);
		}

		mapProviderAction.setCanBeToggled(canBeToggled);
	}
}
