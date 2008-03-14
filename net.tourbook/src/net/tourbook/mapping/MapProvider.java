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

import de.byteholder.geoclipse.map.DefaultTileFactory;
import de.byteholder.geoclipse.map.TileFactory;
import de.byteholder.geoclipse.util.Projection;

public class MapProvider extends DefaultTileFactory {

	/**
	 * status if this map provider can be toggled with in the map provider button
	 */
	private boolean	fCanBeToggled	= false;

	public MapProvider(TileFactory tileFactory, Projection projection) {
		super(tileFactory.getInfo());
		this.projection = projection;
	}

	boolean canBeToggled() {
		return fCanBeToggled;
	}

	void setCanBeToggled(boolean canBeToggled) {
		fCanBeToggled = canBeToggled;
	}

}
