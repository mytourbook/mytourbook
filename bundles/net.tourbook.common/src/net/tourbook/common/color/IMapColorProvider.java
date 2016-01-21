/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.common.color;

public interface IMapColorProvider {

	public static final int	DEFAULT_LEGEND_GRAPHIC_WIDTH	= 20;

	public static final int	LEGEND_IMAGE_BORDER_WIDTH		= 1;

	public static final int	DEFAULT_LEGEND_WIDTH			= 150;
	public static final int	DEFAULT_LEGEND_HEIGHT			= 300;

	public static final int	LEGEND_TOP_MARGIN				= 20;
	public static final int	LEGEND_MARGIN_TOP_BOTTOM		= 10;

	public static final int	LEGEND_UNIT_DISTANCE			= 100;

	/**
	 * @return Returns an id to identify the map color provider.
	 */
	public abstract MapGraphId getGraphId();

}
