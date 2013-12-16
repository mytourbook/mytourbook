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

import java.util.ArrayList;

import net.tourbook.common.Messages;

/**
 * Manage colors which are displayed in 3D map.
 */
public class Map3ColorManager {

	private static ArrayList<Map3ColorDefinition>	_map3ColorDefinitions;

	private static final Map3ColorProfile			MAP3_DEFAULT_COLOR_ALTITUDE;
	private static final Map3ColorProfile			MAP3_DEFAULT_COLOR_GRADIENT;
	private static final Map3ColorProfile			MAP3_DEFAULT_COLOR_PACE;
	private static final Map3ColorProfile			MAP3_DEFAULT_COLOR_PULSE;
	private static final Map3ColorProfile			MAP3_DEFAULT_COLOR_SPEED;

	/**
	 * Define 3D map default colors.
	 */
	static {

		MAP3_DEFAULT_COLOR_ALTITUDE = new Map3ColorProfile(//
				//
				new ColorValue[] {
			new ColorValue(10, 0xEC, 0x20, 0x20),
			new ColorValue(50, 255, 85, 13),
			new ColorValue(100, 255, 255, 0),
			new ColorValue(150, 0, 170, 9),
			new ColorValue(190, 23, 163, 255) },
				//
				MapColor.BRIGHTNESS_DIMMING,
				38,
				MapColor.BRIGHTNESS_LIGHTNING,
				39);

		MAP3_DEFAULT_COLOR_GRADIENT = new Map3ColorProfile(//
				//
				new ColorValue[] {
			new ColorValue(10, 0, 0, 255),
			new ColorValue(50, 0, 255, 255),
			new ColorValue(100, 0, 237, 0),
			new ColorValue(150, 255, 255, 0),
			new ColorValue(190, 255, 0, 0) },
				//
				MapColor.BRIGHTNESS_DIMMING,
				23,
				MapColor.BRIGHTNESS_DIMMING,
				10,
				//
				// overwrite min/max values
				true,
				-10,
				true,
				10);

		MAP3_DEFAULT_COLOR_PACE = new Map3ColorProfile(//
				//
				new ColorValue[] {
			new ColorValue(10, 255, 0, 0),
			new ColorValue(50, 255, 255, 0),
			new ColorValue(100, 0, 169, 0),
			new ColorValue(150, 0, 255, 255),
			new ColorValue(190, 0, 0, 255) },
				//
				MapColor.BRIGHTNESS_DIMMING,
				17,
				MapColor.BRIGHTNESS_DIMMING,
				8);

		MAP3_DEFAULT_COLOR_PULSE = new Map3ColorProfile(//
				//
				new ColorValue[] {
			new ColorValue(10, 0, 203, 0),
			new ColorValue(50, 57, 255, 0),
			new ColorValue(100, 255, 255, 0),
			new ColorValue(150, 255, 0, 0),
			new ColorValue(190, 255, 0, 247) },
				//
				MapColor.BRIGHTNESS_DIMMING,
				11,
				MapColor.BRIGHTNESS_DIMMING,
				10);

		MAP3_DEFAULT_COLOR_SPEED = new Map3ColorProfile(//
				//
				new ColorValue[] {
			new ColorValue(10, 0, 0, 255),
			new ColorValue(50, 0, 255, 255),
			new ColorValue(100, 0, 169, 0),
			new ColorValue(150, 255, 255, 0),
			new ColorValue(190, 255, 0, 0) },
				//
				MapColor.BRIGHTNESS_DIMMING,
				17,
				MapColor.BRIGHTNESS_DIMMING,
				8);
	}

	private Map3ColorManager() {}

	private static ArrayList<Map3ColorDefinition> createDefaultColors() {

		final ArrayList<Map3ColorDefinition> allColorDef = new ArrayList<Map3ColorDefinition>();

		allColorDef.add(new Map3ColorDefinition(MapColorId.Altitude, //
				Messages.Graph_Label_Altitude,
				MAP3_DEFAULT_COLOR_ALTITUDE));

		allColorDef.add(new Map3ColorDefinition(MapColorId.Pulse, //
				Messages.Graph_Label_Heartbeat,
				MAP3_DEFAULT_COLOR_PULSE));

		allColorDef.add(new Map3ColorDefinition(MapColorId.Speed,//
				Messages.Graph_Label_Speed,
				MAP3_DEFAULT_COLOR_SPEED));

		allColorDef.add(new Map3ColorDefinition(MapColorId.Pace,//
				Messages.Graph_Label_Pace,
				MAP3_DEFAULT_COLOR_PACE));

		allColorDef.add(new Map3ColorDefinition(MapColorId.Gradient, //
				Messages.Graph_Label_Gradient,
				MAP3_DEFAULT_COLOR_GRADIENT));

		return allColorDef;
	}

	public static void disposeProfileImages() {

		for (final Map3ColorDefinition colorDef : _map3ColorDefinitions) {
			for (final Map3ColorProfile colorProfile : colorDef.getColorProfiles()) {

			}
		}
	}

	/**
	 * @return Returns color definitions which are defined for the 3D map.
	 */
	public static ArrayList<Map3ColorDefinition> getMapColorDefinitions() {

		if (_map3ColorDefinitions != null) {
			return _map3ColorDefinitions;
		}

		// create and set default colors
		_map3ColorDefinitions = createDefaultColors();

		// overwrite default colors with saved colors
//		readXmlMapColors();
//		setMapColors();

		return _map3ColorDefinitions;
	}
}
