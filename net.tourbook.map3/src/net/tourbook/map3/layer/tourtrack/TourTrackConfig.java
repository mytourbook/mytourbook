/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.layer.tourtrack;

import gov.nasa.worldwind.WorldWind;
import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;

public class TourTrackConfig {

	private static final String	STATE_ALTITUDE_MODE		= "STATE_ALTITUDE_MODE";		//$NON-NLS-1$
	private static final String	STATE_IS_FOLLOW_TERRAIN	= "STATE_IS_FOLLOW_TERRAIN";	//$NON-NLS-1$

	public int					altitudeMode;
	public boolean				isFollowTerrain;

	TourTrackConfig(final IDialogSettings state) {

		// set configuration from state, when not available set defaults

		altitudeMode = Util.getStateInt(state, STATE_ALTITUDE_MODE, WorldWind.CLAMP_TO_GROUND);
		isFollowTerrain = Util.getStateBoolean(state, STATE_IS_FOLLOW_TERRAIN, true);
	}

	void saveState(final IDialogSettings state) {

		state.put(STATE_ALTITUDE_MODE, altitudeMode);
		state.put(STATE_IS_FOLLOW_TERRAIN, isFollowTerrain);
	}

}
