/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

	private static final String							TOGGLE_MARKER		= " (x)";						//$NON-NLS-1$

	private static final String							DEFAULT_FACTORY_ID	= "osm";						//$NON-NLS-1$

	private static IPreferenceStore						fPrefStore			= TourbookPlugin.getDefault()
																					.getPreferenceStore();

	private final TourMapView							fMappingView;

	private Menu										fMenu;
	private final ActionModifyMapProvider				fActionModifyMapProvider;

	private final HashMap<String, MapProviderAction>	fMapProviderActions;

	/**
	 * tile factory which is currently selected
	 */
	private TileFactory									fSelectedTileFactory;

	private ArrayList<MapProvider>						fSortedMapProviders;
	private ArrayList<MapProviderAction>				fSortedMapProviderActions;

	private int											fMapProviderToggleCounter;

	/**
	 * Action for a map provider
	 */
	private class MapProviderAction extends Action {

		private final MapProvider	fMapProvider;
		private final String		fActionLabel;
		private boolean				fCanBeToggled;

		public MapProviderAction(final MapProvider mapProvider, final String label) {

			super(label, AS_RADIO_BUTTON);

			this.fMapProvider = mapProvider;
			this.fActionLabel = label;
		}

		boolean isCanBeToggled() {
			return fCanBeToggled;
		}

		@Override
		public void run() {

			// select this map provider
			selectMapProviderInTheMap(fMapProvider.getTileFactory());
		}

		void setCanBeToggled(final boolean canBeToggled) {
			this.fCanBeToggled = canBeToggled;
		}
	}

	public ActionSelectMapProvider(final TourMapView mapView) {

		super(null, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fMappingView = mapView;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		/*
		 * create an action for each map provider
		 */
		fMapProviderActions = new HashMap<String, MapProviderAction>();
		for (final MapProvider mapProvider : mapView.getMapProviders()) {

			final TileFactory tileFactory = mapProvider.getTileFactory();

			final MapProviderAction mapProviderAction = new MapProviderAction(//
			new MapProvider(tileFactory, //
					tileFactory.getProjection()), //
					tileFactory.getInfo().getFactoryName());

			fMapProviderActions.put(tileFactory.getInfo().getFactoryID(), mapProviderAction);
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

			final MapProviderAction mapProviderAction = fMapProviderActions.get(mapProvider.getTileFactory()
					.getInfo()
					.getFactoryID());

			updateActionToggleState(mapProvider, mapProviderAction);

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
	 * @return Returns the map provider which is currently selected
	 */
	public TileFactory getSelectedFactory() {
		if (fSelectedTileFactory == null) {
			return fMapProviderActions.get(0).fMapProvider.getTileFactory();
		}
		return fSelectedTileFactory;
	}

	@Override
	public void run() {

		/*
		 * toggle map providers
		 */

		final ArrayList<MapProviderAction> sortedToggleMapProviderActions = new ArrayList<MapProviderAction>();

		// get all map provider actions which can be toggled
		for (final MapProviderAction mapProviderAction : fSortedMapProviderActions) {
			if (mapProviderAction.isCanBeToggled()) {
				sortedToggleMapProviderActions.add(mapProviderAction);
			}
		}

		MapProvider newMapProvider;

		if (sortedToggleMapProviderActions.size() > 0) {

			// use custom toggle mechanism

			/*
			 * find selected map provider in the toggle map providers, it is possible that the
			 * selected map provider is not found when a map provider was selected manually without
			 * toggle mechanism
			 */
			int toggleCounter = -1;
			int actionCounter = 0;

			for (final MapProviderAction mapProviderAction : sortedToggleMapProviderActions) {
				if (fSelectedTileFactory == mapProviderAction.fMapProvider.getTileFactory()) {
					toggleCounter = actionCounter;
					break;
				}
				actionCounter++;
			}

			if (toggleCounter != -1) {

				// map provider action was found, toggle to the next map provider

				if (toggleCounter == sortedToggleMapProviderActions.size() - 1) {

					// last map provider was selected, get first map provider
					newMapProvider = sortedToggleMapProviderActions.get(0).fMapProvider;

				} else {
					// get next map provider
					newMapProvider = sortedToggleMapProviderActions.get(toggleCounter + 1).fMapProvider;
				}
			} else {

				// get first map provider
				newMapProvider = sortedToggleMapProviderActions.get(0).fMapProvider;
			}

		} else {

			// toggle all available map factories

			// get next factory
			newMapProvider = fSortedMapProviders.get(++fMapProviderToggleCounter % fSortedMapProviders.size());
		}

		final String newMapProviderID = newMapProvider.getTileFactory().getInfo().getFactoryID();

		// check action for the selected map provider
		for (final MapProviderAction mapProviderAction : fSortedMapProviderActions) {
			if (mapProviderAction.fMapProvider.getTileFactory().getInfo().getFactoryID().equals(newMapProviderID)) {
				mapProviderAction.setChecked(true);
			} else {
				mapProviderAction.setChecked(false);
			}
		}

		// select map provider in the map
		selectMapProviderInTheMap(newMapProvider.getTileFactory());
	}

	/**
	 * Select a map provider by it's factory ID
	 * 
	 * @param mapProviderId
	 *            factory ID or <code>null</code> to select the default factory (OSM)
	 */
	public void selectMapProvider(final String mapProviderId) {

		if (mapProviderId != null) {

			for (final MapProviderAction mapProviderAction : fMapProviderActions.values()) {

				final TileFactory mapProvider = mapProviderAction.fMapProvider.getTileFactory();

				// check factory ID
				if (mapProvider.getInfo().getFactoryID().equals(mapProviderId)) {

					// map provider is available
					mapProviderAction.setChecked(true);
					selectMapProviderInTheMap(mapProvider);

					return;
				}
			}
		}

		/*
		 * if map provider is not set, get default map provider
		 */
		for (final MapProviderAction mapProviderAction : fMapProviderActions.values()) {
			final MapProvider mapProvider = mapProviderAction.fMapProvider;
			if (mapProvider.getTileFactory().getInfo().getFactoryID().equals(DEFAULT_FACTORY_ID)) {
				mapProviderAction.setChecked(true);
				selectMapProviderInTheMap(mapProvider.getTileFactory());
				return;
			}
		}

		/*
		 * if map provider is not set, get first one
		 */
		final MapProviderAction mapProviderAction = fMapProviderActions.values().iterator().next();
		if (mapProviderAction != null) {

			mapProviderAction.setChecked(true);
			selectMapProviderInTheMap(mapProviderAction.fMapProvider.getTileFactory());
		}
	}

	private void selectMapProviderInTheMap(final TileFactory mapProvider) {

		// check if a new map provider is selected
		if (fSelectedTileFactory != null && fSelectedTileFactory == mapProvider) {
			return;
		}

		fSelectedTileFactory = mapProvider;

		final Map map = fMappingView.getMap();
		map.setTileFactory(mapProvider);

		// reset overlays must be done after the new map provider is set
		map.resetOverlays();

		// set map dim level
		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
		final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);
		map.setDimLevel(fMappingView.getMapDimLevel(), dimColor);

		// update tooltip, show selected map provider
		setToolTipText(mapProvider.getInfo().getFactoryName());
	}

	/**
	 * Update the toggle state from the map provider into the action
	 * 
	 * @param mapProvider
	 * @param mapProviderAction
	 */
	private void updateActionToggleState(final MapProvider mapProvider, final MapProviderAction mapProviderAction) {

		final boolean canBeToggled = mapProvider.canBeToggled();

		// update action label
		if (canBeToggled) {
			mapProviderAction.setText(mapProviderAction.fActionLabel + TOGGLE_MARKER);
		} else {
			mapProviderAction.setText(mapProviderAction.fActionLabel);
		}

		mapProviderAction.setCanBeToggled(canBeToggled);
	}

	/**
	 * @return update the map providers with information from the pref store, the customized order
	 *         and the toggle status
	 */
	void updateMapProviders() {

		final List<MapProvider> allMapProviders = fMappingView.getMapProviders();

		final String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
		fPrefStore.getString(ITourbookPreferences.MAP_PROVIDERS_SORT_ORDER));

		final ArrayList<MapProvider> mapProviders = new ArrayList<MapProvider>();

		// get sorted map providers from the pref store
		for (final String storeMapProvider : storedProviderIds) {

			// find the stored map provider in the available map providers
			for (final MapProvider mapProvider : allMapProviders) {
				if (mapProvider.getTileFactory().getInfo().getFactoryID().equals(storeMapProvider)) {
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
			final String factoryId = mapProvider.getTileFactory().getInfo().getFactoryID();

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

			final MapProviderAction mapProviderAction = fMapProviderActions.get(mapProvider.getTileFactory()
					.getInfo()
					.getFactoryID());

			updateActionToggleState(mapProvider, mapProviderAction);

			sortedMapProviderActions.add(mapProviderAction);
		}

		fSortedMapProviderActions = sortedMapProviderActions;
	}
}
