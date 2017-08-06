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
package net.tourbook.map25.layer.marker;

import net.tourbook.map25.Map25ConfigManager;

import org.eclipse.swt.graphics.RGB;

public class MarkerConfig {

	/*
	 * Set default values also here to ensure that a valid value is set. A default value would not
	 * be set when an xml tag is not available.
	 */

	public String					id						= Long.toString(System.nanoTime());
	public String					defaultId				= Map25ConfigManager.CONFIG_DEFAULT_ID_1;
	public String					name					= Map25ConfigManager.CONFIG_DEFAULT_ID_1;

	/*
	 * Marker
	 */
	public boolean					isShowMarkerLabel		= true;
	public boolean					isShowMarkerPoint		= true;
	public RGB						markerFill_Color		= Map25ConfigManager.DEFAULT_MARKER_FILL_COLOR;
	public int						markerFill_Opacity		= Map25ConfigManager.DEFAULT_MARKER_OPACITY;
	public RGB						markerOutline_Color		= Map25ConfigManager.DEFAULT_MARKER_OUTLINE_COLOR;
	public int						markerOutline_Opacity	= Map25ConfigManager.DEFAULT_MARKER_OPACITY;
	public int						markerOrientation		= Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;
	public int						markerSymbolSize		= Map25ConfigManager.DEFAULT_MARKER_SYMBOL_SIZE;

	/*
	 * Cluster
	 */
	public boolean					isMarkerClustered		= true;
	public Enum<ClusterAlgorithm>	clusterAlgorithm		= ClusterAlgorithm.FirstMarker_Grid;
	public int						clusterFill_Opacity		= Map25ConfigManager.DEFAULT_CLUSTER_OPACITY;
	public RGB						clusterFill_Color		= Map25ConfigManager.DEFAULT_CLUSTER_FILL_COLOR;
	public int						clusterOutline_Opacity	= Map25ConfigManager.DEFAULT_CLUSTER_OPACITY;
	public RGB						clusterOutline_Color	= Map25ConfigManager.DEFAULT_CLUSTER_OUTLINE_COLOR;
	public int						clusterGridSize			= Map25ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
	public int						clusterOrientation		= Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;
	public int						clusterSymbolSize		= Map25ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;

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

		final MarkerConfig other = (MarkerConfig) obj;

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

	@Override
	public String toString() {
		return "Map25MarkerConfig ["

				+ "id=" + id + ", "
				+ "name=" + name + ", "

				+ "iconClusterSizeDP=" + clusterSymbolSize + ", "
				+ "iconMarkerSizeDP=" + markerSymbolSize + ", "

				+ "clusterColorForeground=" + clusterOutline_Color + ", "
				+ "clusterColorBackground=" + clusterFill_Color +

				"]\n";
	}
}
