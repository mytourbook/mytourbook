/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

public class Map25TrackConfig {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

	public String	id						= Long.toString(System.nanoTime());
	public String	defaultId				= Map25ConfigManager.CONFIG_DEFAULT_ID_1;
	public String	name					= Map25ConfigManager.CONFIG_DEFAULT_ID_1;

	// outline
	public float	outlineWidth			= Map25ConfigManager.DEFAULT_OUTLINE_WIDTH;
	public RGB		outlineColor			= Map25ConfigManager.DEFAULT_OUTLINE_COLOR;
	public int		outlineOpacity			= Map25ConfigManager.DEFAULT_OUTLINE_OPACITY;

	// chart slider path
	public boolean	isShowSliderLocation		= Map25ConfigManager.DEFAULT_IS_SHOW_CHART_SLIDER;
	public boolean	isShowSliderPath		= Map25ConfigManager.DEFAULT_IS_SHOW_SLIDER_PATH;
	public RGB		sliderPath_Color		= Map25ConfigManager.DEFAULT_SLIDER_PATH_COLOR;
	public float	sliderPath_LineWidth	= Map25ConfigManager.DEFAULT_SLIDER_PATH_LINE_WIDTH;
	public int		sliderPath_Opacity		= Map25ConfigManager.DEFAULT_SLIDER_PATH_OPACITY;

	/**
	 * Animation time in milli seconds when a tour is synched with the map, default is 2000 ms
	 */
	public int		animationTime			= Map25ConfigManager.DEFAULT_ANIMATION_TIME;

	public Map25TrackConfig() {}

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

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final Map25TrackConfig other = (Map25TrackConfig) obj;

		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());

		return result;
	}

}
