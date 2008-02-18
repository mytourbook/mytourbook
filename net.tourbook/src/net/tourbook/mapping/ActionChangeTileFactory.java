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
import java.util.List;

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import de.byteholder.geoclipse.map.TileFactory;

public class ActionChangeTileFactory extends Action implements IMenuCreator {

	private static int					fFactoryCounter	= 0;

	private Menu						fMenu;
	private MappingView					fMapView;

	private ArrayList<FactoryAction>	fFactoryActions;

	/**
	 * tile factory which is currently selected
	 */
	private TileFactory					fSelectedTileFactory;

	private class FactoryAction extends Action {

		private TileFactory	fTileFactory;

		public FactoryAction(TileFactory tileFactory, String label) {
			super(label, AS_RADIO_BUTTON);
			fTileFactory = tileFactory;
		}

		@Override
		public void run() {
			fSelectedTileFactory = fTileFactory;
			selectTileFactoryInMap();
		}
	}

	public ActionChangeTileFactory(MappingView mapView) {

		super(null, AS_DROP_DOWN_MENU);
		setMenuCreator(this);

		fMapView = mapView;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.image_action_change_tile_factory));

		/*
		 * create an action for each factory
		 */
		fFactoryActions = new ArrayList<FactoryAction>();
		List<TileFactory> factories = mapView.getFactories();

		for (TileFactory tileFactory : factories) {
			fFactoryActions.add(new FactoryAction(tileFactory, tileFactory.getInfo().getFactoryName()));
		}
	}

	private void addActionToMenu(Action action) {
		ActionContributionItem item = new ActionContributionItem(action);
		item.fill(fMenu, -1);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu = null;
		}
	}

	public Menu getMenu(Control parent) {

		fMenu = new Menu(parent);

		for (FactoryAction action : fFactoryActions) {
			addActionToMenu(action);
		}

		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	/**
	 * @return Returns the tile factory which is currently selected
	 */
	public TileFactory getSelectedFactory() {
		if (fSelectedTileFactory == null) {
			return fMapView.getFactories().get(0);
		}
		return fSelectedTileFactory;
	}

	@Override
	public void run() {

		// get next factory
		final List<TileFactory> mapFactories = fMapView.getFactories();
		fSelectedTileFactory = mapFactories.get(fFactoryCounter++ % mapFactories.size());

		// select factory action for the new factory
		for (FactoryAction factoryAction : fFactoryActions) {
			if (factoryAction.fTileFactory == fSelectedTileFactory) {
				factoryAction.setChecked(true);
			} else {
				factoryAction.setChecked(false);
			}
		}

		// select factory in the map
		selectTileFactoryInMap();
	}

	private void selectTileFactoryInMap() {
		fMapView.getMap().setTileFactory(fSelectedTileFactory);
	}

	/**
	 * Select a tile factory by it's factory ID
	 * 
	 * @param factoryId
	 *        factory ID or <code>null</code> to select the first tile factory
	 */
	public void setSelectedFactory(String factoryId) {

		if (factoryId != null) {

			for (FactoryAction factoryAction : fFactoryActions) {
				final TileFactory tileFactory = factoryAction.fTileFactory;
				fFactoryCounter++;

				// check facory ID
				if (tileFactory.getInfo().getFactoryID().equals(factoryId)) {

					// factory is available
					factoryAction.setChecked(true);
					fSelectedTileFactory = tileFactory;
					selectTileFactoryInMap();
					return;
				}
			}
		}

		/*
		 * factory is not available, select first factory
		 */
		fFactoryActions.get(0).setChecked(true);

		TileFactory tileFactory = fMapView.getFactories().get(0);
		fSelectedTileFactory = tileFactory;
		selectTileFactoryInMap();
	}
}
