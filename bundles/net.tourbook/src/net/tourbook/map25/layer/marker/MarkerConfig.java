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

// SET_FORMATTING_OFF
// SET_FORMATTING_ON

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
	public int						markerOrientation		= Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;

	public RGB						markerFill_Color		= Map25ConfigManager.DEFAULT_MARKER_FILL_COLOR;
	public int						markerFill_Opacity		= Map25ConfigManager.DEFAULT_MARKER_FILL_OPACITY;
	public RGB						markerOutline_Color		= Map25ConfigManager.DEFAULT_MARKER_OUTLINE_COLOR;
	public int						markerOutline_Opacity	= Map25ConfigManager.DEFAULT_MARKER_OUTLINE_OPACITY;
	public float					markerOutline_Size		= Map25ConfigManager.DEFAULT_MARKER_OUTLINE_SIZE;
	public int						markerSymbol_Size		= Map25ConfigManager.DEFAULT_MARKER_SYMBOL_SIZE;

	/*
	 * Cluster
	 */
	public boolean					isMarkerClustered		= true;
	public Enum<ClusterAlgorithm>	clusterAlgorithm		= ClusterAlgorithm.FirstMarker_Grid;
	public int						clusterGrid_Size		= Map25ConfigManager.DEFAULT_CLUSTER_GRID_SIZE;
	public int						clusterOrientation		= Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD;
	
	public RGB						clusterFill_Color		= Map25ConfigManager.DEFAULT_CLUSTER_FILL_COLOR;
	public int						clusterFill_Opacity		= Map25ConfigManager.DEFAULT_CLUSTER_FILL_OPACITY;
	public RGB						clusterOutline_Color	= Map25ConfigManager.DEFAULT_CLUSTER_OUTLINE_COLOR;
	public int						clusterOutline_Opacity	= Map25ConfigManager.DEFAULT_CLUSTER_OUTLINE_OPACITY;
	public float					clusterOutline_Size		= Map25ConfigManager.DEFAULT_CLUSTER_OUTLINE_SIZE;
	public int						clusterSymbol_Size		= Map25ConfigManager.DEFAULT_CLUSTER_SYMBOL_SIZE;
	public int						clusterSymbol_Weight	= Map25ConfigManager.DEFAULT_CLUSTER_SYMBOL_WEIGHT;

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
		return "Map25MarkerConfig [" //$NON-NLS-1$

				+ "id=" + id + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "name=" + name + ", " //$NON-NLS-1$ //$NON-NLS-2$

				+ "iconClusterSizeDP=" + clusterSymbol_Size + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "iconMarkerSizeDP=" + markerSymbol_Size + ", " //$NON-NLS-1$ //$NON-NLS-2$

				+ "clusterColorForeground=" + clusterOutline_Color + ", " //$NON-NLS-1$ //$NON-NLS-2$
				+ "clusterColorBackground=" + clusterFill_Color + //$NON-NLS-1$

				"]\n"; //$NON-NLS-1$
	}
}
