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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map2.Messages;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25Manager;
import net.tourbook.map25.Map25Provider;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.byteholder.geoclipse.mapprovider.IMapProviderListener;

public class ActionSelectMap25Provider extends Action implements IMenuCreator, IMapProviderListener {

	private final Map25View								_map25View;

	private final ActionManageMap25Providers			_actionModifyMapProvider;

	private Menu										_menu;

	private final HashMap<String, Map25ProviderAction>	_mapProviderActions	= new HashMap<>();
	private ArrayList<Map25ProviderAction>				_sortedMapProvidersActions;

	private int											_mpToggleCounter;

	/**
	 * Action for a map provider
	 */
	private class Map25ProviderAction extends Action {

		private final Map25Provider	__mapProvider;

		private boolean				__canBeToggled;

		public Map25ProviderAction(final Map25Provider mp, final String label) {

			super(label, AS_RADIO_BUTTON);

			__mapProvider = mp;
		}

		boolean isCanBeToggled() {
			return __canBeToggled;
		}

		@Override
		public void run() {

			// select this map provider
			selectMapProvider_10(__mapProvider);
		}

	}

	public ActionSelectMap25Provider(final Map25View map25View) {

		super(null, AS_DROP_DOWN_MENU);

		setMenuCreator(this);

		_map25View = map25View;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		Map25Manager.addMapProviderListener(this);

		_actionModifyMapProvider = new ActionManageMap25Providers();

		createMapProviderActions();
	}

	private void addActionToMenu(final Action action) {

		final ActionContributionItem item = new ActionContributionItem(action);
		item.fill(_menu, -1);
	}

	/**
	 * create an action for each map provider
	 */
	private void createMapProviderActions() {

		_mapProviderActions.clear();

		final ArrayList<Map25Provider> allMapProviders = Map25Manager.getAllMapProviders();

		Collections.sort(allMapProviders, new Comparator<Map25Provider>() {

			@Override
			public int compare(final Map25Provider mp1, final Map25Provider mp2) {
				return mp1.name.compareTo(mp2.name);
			}
		});

		_sortedMapProvidersActions = new ArrayList<>();

		// create an action for each map provider
		for (final Map25Provider mapProvider : allMapProviders) {

			if (mapProvider.isEnabled) {

				final Map25ProviderAction mpAction = new Map25ProviderAction(mapProvider, mapProvider.name);

				_mapProviderActions.put(mapProvider.getId(), mpAction);
				_sortedMapProvidersActions.add(mpAction);
			}
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

		final Map25Provider selectedMapProvider = getSelectedMapProvider();

		// add all map providers
		for (final Map25ProviderAction mapProviderAction : _sortedMapProvidersActions) {

			final Map25Provider mapProvider = mapProviderAction.__mapProvider;
			final Map25ProviderAction mpAction = _mapProviderActions.get(mapProvider.getId());

			addActionToMenu(mpAction);

			if (mapProvider == selectedMapProvider) {
				mpAction.setChecked(true);
			} else {
				mpAction.setChecked(false);
			}
		}

		(new Separator()).fill(_menu, -1);
		addActionToMenu(_actionModifyMapProvider);

		return _menu;
	}

	@Override
	public Menu getMenu(final Menu parent) {
		return null;
	}

	private Map25Provider getSelectedMapProvider() {

		return _map25View.getMapApp().getSelectedMapProvider();
	}

	@Override
	public void mapProviderListChanged() {

		createMapProviderActions();

		selectMapProvider(getSelectedMapProvider());
	}

	@Override
	public void run() {

		/*
		 * toggle map providers
		 */

		final ArrayList<Map25ProviderAction> sortedToggleMapProviderActions = new ArrayList<Map25ProviderAction>();

		// get all map provider actions which can be toggled
		for (final Map25ProviderAction mapProviderAction : _sortedMapProvidersActions) {
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
			final Map25Provider _selectedMapProvider = getSelectedMapProvider();

			for (final Map25ProviderAction mpAction : sortedToggleMapProviderActions) {
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
			final int nextIndex = ++_mpToggleCounter % _sortedMapProvidersActions.size();
			newMp = _sortedMapProvidersActions.get(nextIndex).__mapProvider;
		}

		// select map provider in the map
		selectMapProvider_10(newMp);
	}

	/**
	 * Select a map provider by it's map provider ID
	 * 
	 * @param selectedMapProvider
	 *            map provider id or <code>null</code> to select the default factory (OSM)
	 */
	public void selectMapProvider(final Map25Provider selectedMapProvider) {

		for (final Map25ProviderAction mapProviderAction : _mapProviderActions.values()) {

			final Map25Provider mp = mapProviderAction.__mapProvider;

			if (mp == selectedMapProvider) {

				// map provider is available
				mapProviderAction.setChecked(true);
				selectMapProvider_10(mp);

				return;
			}
		}
	}

	private void selectMapProvider_10(final Map25Provider newMapProvider) {

		// check if a new map provider is selected
		if (getSelectedMapProvider() == newMapProvider) {
			return;
		}

		final Map25App mapApp = _map25View.getMapApp();

		mapApp.setMapProvider(newMapProvider);

		updateUI_SelectedMapProvider(newMapProvider);
	}

	/**
	 * Update tooltip, show selected map provider.
	 * 
	 * @param selectedMapProvider
	 */
	public void updateUI_SelectedMapProvider(final Map25Provider selectedMapProvider) {

		setToolTipText(selectedMapProvider.name);
	}

}
