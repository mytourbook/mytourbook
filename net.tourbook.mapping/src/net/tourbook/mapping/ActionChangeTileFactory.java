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

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

import de.byteholder.geoclipse.map.TileFactory;

public class ActionChangeTileFactory extends Action {

	private static int	fFactoryCounter	= 0;

	private OSMView		fMapView;

	public ActionChangeTileFactory(OSMView mapView) {

		super(null, AS_PUSH_BUTTON);

		fMapView = mapView;

		setToolTipText(Messages.map_action_change_tile_factory_tooltip);
		setImageDescriptor(Activator.getIconImageDescriptor(Messages.image_action_change_tile_factory));
	}

	@Override
	public void run() {

		final List<TileFactory> mapFactories = fMapView.getFactories();

		TileFactory factory = mapFactories.get(fFactoryCounter++ % mapFactories.size());
		fMapView.getMap().setTileFactory(factory);
	}

	public void selectionChanged(IAction action, ISelection selection) {}
}
