/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map.model;

import java.util.ArrayList;

/**
 * Manage animated map models
 */
public class MapModelManager {

   public static final String         MAP_MODEL_FILE_EXTENTION = "gltf";           //$NON-NLS-1$

   /**
    * Contains all configurations which are loaded from a xml file.
    */
   private static ArrayList<MapModel> _allModels               = new ArrayList<>();

   public static ArrayList<MapModel> getAllModels() {

      return _allModels;
   }


}
