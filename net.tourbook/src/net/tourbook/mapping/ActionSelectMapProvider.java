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

import de.byteholder.geoclipse.Activator;
import de.byteholder.geoclipse.map.Map;
import de.byteholder.geoclipse.mapprovider.IMapProviderListener;
import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

public class ActionSelectMapProvider extends Action implements IMenuCreator, IMapProviderListener {

	private static IPreferenceStore				_geoPrefStore	= Activator.getDefault().getPreferenceStore();

	private final TourMapView					_mapView;

	private Menu								_menu;
	private final ActionSetDefaultMapProviders	_actionSetDefaultMapProvider;
	private final ActionManageMapProviders		_actionModifyMapProvider;

	private final HashMap<String, MPAction>		_mpActions		= new HashMap<String, MPAction>();

	/**
	 * tile factory which is currently selected
	 */
	private MP									_selectedMP;

	private ArrayList<MP>						_sortedMapProviders;
	private ArrayList<MPAction>					_sortedMapProviderActions;

	private int									_mpToggleCounter;

	/**
	 * Action for a map provider
	 */
	private class MPAction extends Action {

		private final MP		_mp;

		private final String	_actionLabel;
		private boolean			_canBeToggled;

		public MPAction(final MP mp, final String label) {

			super(label, AS_RADIO_BUTTON);

			_mp = mp;
			_actionLabel = label;
		}

		boolean isCanBeToggled() {
			return _canBeToggled;
		}

		@Override
		public void run() {

			// select this map provider
			selectMapProviderInTheMap(_mp);
		}

		void setCanBeToggled(final boolean canBeToggled) {
			_canBeToggled = canBeToggled;
		}
	}

	public ActionSelectMapProvider(final TourMapView mapView) {

		super(null, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		_mapView = mapView;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		MapProviderManager.getInstance().addMapProviderListener(this);

		_actionSetDefaultMapProvider = new ActionSetDefaultMapProviders(mapView);
		_actionModifyMapProvider = new ActionManageMapProviders(_mapView);

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
	 * @param selectedMp
	 */
	private void checkSelectedMP(final MP selectedMp) {

		final String selectedMpId = selectedMp.getId();

		for (final MPAction mapProviderAction : _sortedMapProviderActions) {
			if (mapProviderAction._mp.getId().equals(selectedMpId)) {
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

		_mpActions.clear();

		final ArrayList<MP> allMapProviders = MapProviderManager.getInstance().getAllMapProviders(true);

		for (final MP mp : allMapProviders) {
			_mpActions.put(mp.getId(), new MPAction(mp, mp.getName()));
		}
	}

	public void dispose() {

		if (_menu != null) {
			_menu.dispose();
			_menu = null;
		}

		MapProviderManager.getInstance().removeMapProviderListener(this);
	}

	public Menu getMenu(final Control parent) {

		// recreate menu each time
		if (_menu != null) {
			_menu.dispose();
		}

		_menu = new Menu(parent);

		addActionToMenu(_actionSetDefaultMapProvider);
		addActionToMenu(_actionModifyMapProvider);

		(new Separator()).fill(_menu, -1);

		// add all map providers
		for (final MP mp : _sortedMapProviders) {

			final MPAction mpAction = _mpActions.get(mp.getId());

			updateActionToggleState(mp, mpAction);

			addActionToMenu(mpAction);
		}

		return _menu;
	}

	public Menu getMenu(final Menu parent) {
		return null;
	}

	/**
	 * @return Returns the map provider {@link MP} which is currently selected or OSM when there
	 *         is
	 *         no selected map provider
	 */
	public MP getSelectedMapProvider() {
		if (_selectedMP == null) {
			return _mpActions.get(0)._mp;
		}
		return _selectedMP;
	}

	public void mapProviderListChanged() {

		if (_selectedMP != null) {

			// map profile tile offline images are deleted, reset state  
			_selectedMP.resetTileImageAvailability();
		}

		createMapProviderActions();
		updateMapProviders();

		selectMapProvider(_selectedMP == null ? null : _selectedMP.getId());
	}

	@Override
	public void run() {

		/*
		 * toggle map providers
		 */

		final ArrayList<MPAction> sortedToggleMapProviderActions = new ArrayList<MPAction>();

		// get all map provider actions which can be toggled
		for (final MPAction mapProviderAction : _sortedMapProviderActions) {
			if (mapProviderAction.isCanBeToggled()) {
				sortedToggleMapProviderActions.add(mapProviderAction);
			}
		}

		MP newMp;

		if (sortedToggleMapProviderActions.size() == 1) {

			// open dialog to set default map provider

			_mapView.actionOpenMapProviderDialog();

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

			for (final MPAction mpAction : sortedToggleMapProviderActions) {
				if (_selectedMP == mpAction._mp) {
					toggleCounter = actionCounter;
					break;
				}
				actionCounter++;
			}

			if (toggleCounter != -1) {

				// map provider action was found, toggle to the next map provider

				if (toggleCounter == sortedToggleMapProviderActions.size() - 1) {

					// last map provider was selected, get first map provider
					newMp = sortedToggleMapProviderActions.get(0)._mp;

				} else {
					// get next map provider
					newMp = sortedToggleMapProviderActions.get(toggleCounter + 1)._mp;
				}
			} else {

				// get first map provider
				newMp = sortedToggleMapProviderActions.get(0)._mp;
			}

		} else {

			// toggle all available map factories

			// get next factory
			newMp = _sortedMapProviders.get(++_mpToggleCounter % _sortedMapProviders.size());
		}

		checkSelectedMP(newMp);

		// select map provider in the map
		selectMapProviderInTheMap(newMp);
	}

	/**
	 * Select a map provider by it's map provider ID
	 * 
	 * @param selectedMpId
	 *            map provider id or <code>null</code> to select the default factory (OSM)
	 */
	public void selectMapProvider(final String selectedMpId) {

		if (selectedMpId != null) {

			for (final MPAction mapProviderAction : _mpActions.values()) {

				final MP mp = mapProviderAction._mp;

				// check mp ID
				if (mp.getId().equals(selectedMpId)) {

					// map provider is available
					mapProviderAction.setChecked(true);
					selectMapProviderInTheMap(mp);

					return;
				}
			}
		}

		/*
		 * if map provider is not set, get default map provider
		 */
		for (final MPAction mapProviderAction : _mpActions.values()) {
			final MP mp = mapProviderAction._mp;
			if (mp.getId().equals(MapProviderManager.DEFAULT_MAP_PROVIDER_ID)) {
				mapProviderAction.setChecked(true);
				selectMapProviderInTheMap(mp);
				return;
			}
		}

		/*
		 * if map provider is not set, get first one
		 */
		final MPAction mpAction = _mpActions.values().iterator().next();
		if (mpAction != null) {

			mpAction.setChecked(true);
			selectMapProviderInTheMap(mpAction._mp);
		}
	}

	private void selectMapProviderInTheMap(final MP mp) {

		// check if a new map provider is selected
		if (_selectedMP != null && _selectedMP == mp) {
			return;
		}

		_selectedMP = mp;

		final Map map = _mapView.getMap();

		map.setMapProvider(mp);

		// reset overlays must be done after the new map provider is set
//		map.resetOverlays();

		// set map dim level
		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
		final RGB dimColor = PreferenceConverter.getColor(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR);
		map.setDimLevel(_mapView.getMapDimLevel(), dimColor);

		// update tooltip, show selected map provider
		setToolTipText(mp.getName());
	}

	/**
	 * Update the toggle state from the map provider into the action
	 * 
	 * @param mp
	 * @param mpAction
	 */
	private void updateActionToggleState(final MP mp, final MPAction mpAction) {

		final boolean canBeToggled = mp.canBeToggled();

		// update action label
		if (canBeToggled) {
			mpAction.setText(mpAction._actionLabel + Messages.Map_Action_ToggleMarker);
		} else {
			mpAction.setText(mpAction._actionLabel);
		}

		mpAction.setCanBeToggled(canBeToggled);
	}

	/**
	 * @return update the map providers with information from the pref store, the customized order
	 *         and the toggle status
	 */
	void updateMapProviders() {

		final List<MP> allMapProviders = MapProviderManager.getInstance().getAllMapProviders(true);

		// get sorted map providers from the pref store
		final String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
				_geoPrefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

		final ArrayList<MP> mapProviders = new ArrayList<MP>();

		// set all map provider which are in the pref store
		for (final String storeMpId : storedProviderIds) {

			// find the stored map provider in the available map providers
			for (final MP mp : allMapProviders) {
				if (mp.getId().equals(storeMpId)) {
					mapProviders.add(mp);
					break;
				}
			}
		}

		// make sure that all available map providers are in the list
		for (final MP mp : allMapProviders) {
			if (!mapProviders.contains(mp)) {
				mapProviders.add(mp);
			}
		}

		/*
		 * set status if the map provider can be toggled with the map provider button
		 */
		final String[] toggleIds = StringToArrayConverter.convertStringToArray(//
				_geoPrefStore.getString(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST));

		for (final MP mp : allMapProviders) {

			final String mpId = mp.getId();

			for (final String toggleId : toggleIds) {
				if (mpId.equals(toggleId)) {
					mp.setCanBeToggled(true);
					break;
				}
			}
		}

		_sortedMapProviders = mapProviders;

		/*
		 * sort map provider actions
		 */
		final ArrayList<MPAction> sortedMapProviderActions = new ArrayList<MPAction>();

		// add all map providers
		for (final MP mp : _sortedMapProviders) {

			final MPAction mapProviderAction = _mpActions.get(mp.getId());

			updateActionToggleState(mp, mapProviderAction);

			sortedMapProviderActions.add(mapProviderAction);
		}

		_sortedMapProviderActions = sortedMapProviderActions;
	}
}
