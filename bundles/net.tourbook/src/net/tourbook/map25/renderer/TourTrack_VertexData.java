/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.renderer;

import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

public class TourTrack_VertexData {

   ShortArrayList tourTrack_Vertices = new ShortArrayList(10_000);
   ByteArrayList  tourTrack_Colors   = new ByteArrayList(10_000);

   public void add(final short a,
                   final short b,
                   final short c,
                   final short d,

                   final int color) {

      tourTrack_Vertices.addAll(a, b, c, d);

      // set+convert color components: argb -> rgba
      tourTrack_Colors.addAll(
            (byte) ((color >>> 16) & 0xff), // red
            (byte) ((color >>> 8) & 0xff), // green
            (byte) ((color >>> 0) & 0xff), // blue
            (byte) ((color >>> 24) & 0xff)) // opacity / alpha
      ;
   }

}
