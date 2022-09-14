/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 * Copyright (C) 2018, 2021 Thomas Theussing
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

/*
 * Original: org.oscim.layers.PathLayer
 */
package net.tourbook.map25.layer.tourtrack;

import net.tourbook.map25.renderer.TourTrack_LayerRenderer;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.oscim.core.GeoPoint;
import org.oscim.layers.Layer;
import org.oscim.map.Map;

/**
 * This class draws a path line in given color or texture.
 * <p>
 * Example to handle track hover/selection
 * org.oscim.layers.marker.ItemizedLayer.activateSelectedItems(MotionEvent, ActiveItem)
 * <p>
 * Original code: org.oscim.layers.PathLayer
 */
public class TourTrack_Layer extends Layer {

   private TourTrack_LayerRenderer _tourTrackRenderer;

   public TourTrack_Layer(final Map map) {

      super(map);

      mRenderer = _tourTrackRenderer = new TourTrack_LayerRenderer(this, map);
   }

   public void onModifyConfig(final boolean isVerticesModified) {

      _tourTrackRenderer.onModifyConfig(isVerticesModified);
   }

   public void setupTourPositions(final GeoPoint[] allGeoPoints, final int[] allGeoPointColors, final IntArrayList allTourStarts) {

      _tourTrackRenderer.setupTourPositions(allGeoPoints, allGeoPointColors, allTourStarts);
   }
}
