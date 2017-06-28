/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.map2.Messages;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25Manager;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25View;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

public class ActionSelectMap25Provider extends Action implements IMenuCreator, IMapProviderListener {

	private static IPreferenceStore						_prefStore			= TourbookPlugin.getPrefStore();

	private final Map25View								_map25View;

	private Menu										_menu;

	private Map25Provider								_selectedMapProvider;
	private ArrayList<Map25Provider>					_sortedMapProviders;

	private ArrayList<MapProviderAction>				_sortedMapProviderActions;
	private final HashMap<String, MapProviderAction>	_mapProviderActions	= new HashMap<>();

	private int											_mpToggleCounter;

	/**
	 * Action for a map provider
	 */
	private class MapProviderAction extends Action {

		private final Map25Provider	__mapProvider;

		private final String		__actionLabel;
		private boolean				__canBeToggled;

		public MapProviderAction(final Map25Provider mp, final String label) {

			super(label, AS_RADIO_BUTTON);

			__mapProvider = mp;
			__actionLabel = label;
		}

		boolean isCanBeToggled() {
			return __canBeToggled;
		}

		@Override
		public void run() {

			// select this map provider
			selectMapProvider_10(__mapProvider);
		}

		void setCanBeToggled(final boolean canBeToggled) {
			__canBeToggled = canBeToggled;
		}
	}

	public ActionSelectMap25Provider(final Map25View map25View) {

		super(null, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		_map25View = map25View;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		Map25Manager.addMapProviderListener(this);

		createMapProviderActions();
		updateMapProviders();
	}

	private void addActionToMenu(final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	/**
	 * check action for the selected map provider
	 * 
	 * @param selectedMapProvider
	 */
	private void checkSelectedMP(final Map25Provider selectedMapProvider) {

		final String selectedId = selectedMapProvider.getId();

		for (final MapProviderAction mapProviderAction : _sortedMapProviderActions) {

			if (mapProviderAction.__mapProvider.getId().equals(selectedId)) {
				mapProviderAction.setChecked(true);
			} else {
				mapProviderAction.setChecked(false);
			}
		}
	}

	/**
	 * create an action for each map provider
	 */
	private void createMapProviderActions() {

		_mapProviderActions.clear();

		final ArrayList<Map25Provider> allMapProviders = Map25Manager.getAllMapProviders();

		for (final Map25Provider mp : allMapProviders) {
			_mapProviderActions.put(mp.getId(), new MapProviderAction(mp, mp.name));
		}
	}

	@Override
	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}

		Map25Manager.removeMapProviderListener(this);
	}

	@Override
	public Menu getMenu(final Control parent) {

		// recreate menu each time
		if (_menu != null) {
			_menu.dispose();
		}

		_menu = new Menu(parent);

//		addActionToMenu(_actionSetDefaultMapProvider);
//		addActionToMenu(_actionModifyMapProvider);
//
//		(new Separator()).fill(_menu, -1);

		// add all map providers
		for (final Map25Provider mp : _sortedMapProviders) {

			final MapProviderAction mpAction = _mapProviderActions.get(mp.getId());

			updateActionToggleState(mp, mpAction);

			addActionToMenu(mpAction);
		}

		return _menu;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		return null;
	}

	/**
	 * @return Returns the map provider {@link Map25Provider} which is currently selected or OSM
	 *         when there is no selected map provider
	 */
	public Map25Provider getSelectedMapProvider() {

		if (_selectedMapProvider == null) {
			return Map25Manager.getDefaultMapProvider();
		}

		return _selectedMapProvider;
	}

	@Override
	public void mapProviderListChanged() {

//		if (_selectedMapProvider != null) {
//
//			// map profile tile offline images are deleted, reset state
//			_selectedMapProvider.resetTileImageAvailability();
//		}

		createMapProviderActions();
		updateMapProviders();

		selectMapProvider(_selectedMapProvider == null ? null : _selectedMapProvider.getId());
	}

	@Override
	public void run() {

		/*
		 * toggle map providers
		 */

		final ArrayList<MapProviderAction> sortedToggleMapProviderActions = new ArrayList<MapProviderAction>();

		// get all map provider actions which can be toggled
		for (final MapProviderAction mapProviderAction : _sortedMapProviderActions) {
			if (mapProviderAction.isCanBeToggled()) {
				sortedToggleMapProviderActions.add(mapProviderAction);
			}
		}

		Map25Provider newMp;

		if (sortedToggleMapProviderActions.size() == 1) {

			// open dialog to set default map provider

//			_map25View.actionOpenMapProviderDialog();

			return;

		} else if (sortedToggleMapProviderActions.size() > 0) {

			// use custom toggle mechanism

			/*
			 * find selected map provider in the toggle map providers, it is possible that the
			 * selected map provider is not found when a map provider was selected manually without
			 * toggle mechanism
			 */
			int toggleCounter = -1;
			int actionCounter = 0;

			for (final MapProviderAction mpAction : sortedToggleMapProviderActions) {
				if (_selectedMapProvider == mpAction.__mapProvider) {
					toggleCounter = actionCounter;
					break;
				}
				actionCounter++;
			}

			if (toggleCounter != -1) {

				// map provider action was found, toggle to the next map provider

				if (toggleCounter == sortedToggleMapProviderActions.size() - 1) {

					// last map provider was selected, get first map provider
					newMp = sortedToggleMapProviderActions.get(0).__mapProvider;

				} else {
					// get next map provider
					newMp = sortedToggleMapProviderActions.get(toggleCounter + 1).__mapProvider;
				}
			} else {

				// get first map provider
				newMp = sortedToggleMapProviderActions.get(0).__mapProvider;
			}

		} else {

			// toggle all available map factories

			// get next factory
			newMp = _sortedMapProviders.get(++_mpToggleCounter % _sortedMapProviders.size());
		}

		checkSelectedMP(newMp);

		// select map provider in the map
		selectMapProvider_10(newMp);
	}

	/**
	 * Select a map provider by it's map provider ID
	 * 
	 * @param id
	 *            map provider id or <code>null</code> to select the default factory (OSM)
	 */
	public void selectMapProvider(final String id) {

		if (id != null) {

			for (final MapProviderAction mapProviderAction : _mapProviderActions.values()) {

				final Map25Provider mp = mapProviderAction.__mapProvider;

				if (mp.getId().equals(id)) {

					// map provider is available
					mapProviderAction.setChecked(true);
					selectMapProvider_10(mp);

					return;
				}
			}
		}

		/*
		 * When map provider is not set, get default map provider
		 */
		final String defaultId = Map25Manager.getDefaultMapProvider().getId();
		for (final MapProviderAction mapProviderAction : _mapProviderActions.values()) {

			final Map25Provider mp = mapProviderAction.__mapProvider;

			if (mp.getId().equals(defaultId)) {

				mapProviderAction.setChecked(true);
				selectMapProvider_10(mp);

				return;
			}
		}

		/*
		 * if map provider is not set, get first one
		 */
		final MapProviderAction mpAction = _mapProviderActions.values().iterator().next();
		if (mpAction != null) {

			mpAction.setChecked(true);
			selectMapProvider_10(mpAction.__mapProvider);
		}
	}

	private void selectMapProvider_10(final Map25Provider mapProivder) {

		// check if a new map provider is selected
		if (_selectedMapProvider != null && _selectedMapProvider == mapProivder) {
			return;
		}

		_selectedMapProvider = mapProivder;

		final Map25App mapApp = _map25View.getMapApp();

		mapApp.setMapProvider(mapProivder);

		// update tooltip, show selected map provider
		setToolTipText(mapProivder.name);
	}

	/**
	 * Update the toggle state from the map provider into the action
	 * 
	 * @param mapProvider
	 * @param mapProviderAction
	 */
	private void updateActionToggleState(final Map25Provider mapProvider, final MapProviderAction mapProviderAction) {

		final boolean canBeToggled = mapProvider.canBeToggled();

		// update action label
		if (canBeToggled) {
			mapProviderAction.setText(mapProviderAction.__actionLabel + Messages.Map_Action_ToggleMarker);
		} else {
			mapProviderAction.setText(mapProviderAction.__actionLabel);
		}

		mapProviderAction.setCanBeToggled(canBeToggled);
	}

	/**
	 * @return update the map providers with information from the pref store, the customized order
	 *         and the toggle status
	 */
	public void updateMapProviders() {

		final List<Map25Provider> allMapProviders = Map25Manager.getAllMapProviders();

		// get sorted map providers from the pref store
		final String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(ITourbookPreferences.MAP25_PROVIDER_SORT_ORDER));

		final ArrayList<Map25Provider> mapProviders = new ArrayList<>();
		final ArrayList<String> validMapProviderIds = new ArrayList<>();

		// set all map provider which are in the pref store
		for (final String storeProviderId : storedProviderIds) {

			/*
			 * ensure that a map provider is unique and not duplicated, this happend during
			 * debugging
			 */
			boolean ignoreMP = false;
			for (final Map25Provider mp : mapProviders) {
				if (mp.getId().equals(storeProviderId)) {
					ignoreMP = true;
					break;
				}
			}
			if (ignoreMP) {
				continue;
			}

			// find the stored map provider in the available map providers
			for (final Map25Provider mapProviderp : allMapProviders) {
				if (mapProviderp.getId().equals(storeProviderId)) {
					mapProviders.add(mapProviderp);
					validMapProviderIds.add(mapProviderp.getId());
					break;
				}
			}
		}

		// make sure that all available map providers are in the list
		for (final Map25Provider mapProvider : allMapProviders) {
			if (!mapProviders.contains(mapProvider)) {
				mapProviders.add(mapProvider);
			}
		}

		/*
		 * save valid mp id's
		 */
		_prefStore.setValue(
				ITourbookPreferences.MAP25_PROVIDER_SORT_ORDER, //
				StringToArrayConverter.convertArrayToString(//
						validMapProviderIds.toArray(new String[validMapProviderIds.size()])));

		/*
		 * set status if the map provider can be toggled with the map provider button
		 */
		final String[] toggleIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(ITourbookPreferences.MAP25_PROVIDER_TOGGLE_LIST));

		for (final Map25Provider mapProvider : allMapProviders) {

			final String mapProviderId = mapProvider.getId();

			for (final String toggleId : toggleIds) {

				if (mapProviderId == (toggleId)) {
					mapProvider.setCanBeToggled(true);
					break;
				}
			}
		}

		_sortedMapProviders = mapProviders;

		/*
		 * sort map provider actions
		 */
		final ArrayList<MapProviderAction> sortedMapProviderActions = new ArrayList<MapProviderAction>();

		// add all map providers
		for (final Map25Provider mp : _sortedMapProviders) {

			final MapProviderAction mapProviderAction = _mapProviderActions.get(mp.getId());

			updateActionToggleState(mp, mapProviderAction);

			sortedMapProviderActions.add(mapProviderAction);
		}

		_sortedMapProviderActions = sortedMapProviderActions;
	}
}
