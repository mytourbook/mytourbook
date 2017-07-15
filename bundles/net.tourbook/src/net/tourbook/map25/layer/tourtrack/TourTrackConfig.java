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
package net.tourbook.map25.layer.tourtrack;

import net.tourbook.map25.Map25ConfigManager;

import org.eclipse.swt.graphics.RGB;

public class TourTrackConfig implements Cloneable {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

	public String	id				= Long.toString(System.nanoTime());
	public String	defaultId		= Map25ConfigManager.DEFAULT_ID_DEFAULT;
	public String	name			= Map25ConfigManager.CONFIG_NAME_UNKNOWN;

	// outline
	public float	outlineWidth	= Map25ConfigManager.OUTLINE_WIDTH_DEFAULT;
	public RGB		outlineColor;

	public TourTrackConfig() {}

	public void checkTrackRecreation(final TourTrackConfig trackConfig) {
		// TODO Auto-generated method stub

	}

	/**
	 * Create a copy of this object.
	 * 
	 * @return a copy of this <code>Insets</code> object.
	 */
	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (final CloneNotSupportedException e) {
			// this shouldn't happen, since we are Cloneable
			throw new InternalError();
		}
	}

}
