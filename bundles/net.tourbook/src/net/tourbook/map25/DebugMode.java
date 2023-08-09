/*******************************************************************************
 * Copyright (C) 2019 Wolfgang Schramm and Contributors
 * Copyright 2019, 2023 Thomas Theussing
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
package net.tourbook.map25;

import net.tourbook.map25.layer.marker.PhotoToolkit;

/**
 * Tile encoding format. {@link Map25App} and {@link Map25View} and {@link PhotoToolkit} depends on
 * this.
 */
public enum DebugMode {

   /**
    * no debug output
    */
   OFF,

   /**
    * writing debug output to screen
    */
   ON,

}
