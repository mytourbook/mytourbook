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
import net.tourbook.common.UI;

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

		/**
		 * <code>
						<legendcolor prefname="altitude">
							<valuecolor blue="0" green="64" red="128" value="10.0"/>
							<valuecolor blue="0" green="255" red="255" value="50.0"/>
							<valuecolor blue="0" green="0" red="255" value="100.0"/>
							<valuecolor blue="255" green="128" red="0" value="150.0"/>
							<valuecolor blue="255" green="128" red="0" value="190.0"/>
							<brightness max="2" maxFactor="14" min="1" minFactor="38"/>
							<minmaxValue isMaxOverwrite="1" isMinOverwrite="0" maxValueOverwrite="750" minValueOverwrite="350"/>
						</legendcolor>

						<legendcolor prefname="heartbeat">
							<valuecolor blue="0" green="203" red="0" value="10.0"/>
							<valuecolor blue="0" green="255" red="57" value="50.0"/>
							<valuecolor blue="0" green="0" red="255" value="100.0"/>
							<valuecolor blue="0" green="0" red="204" value="150.0"/>
							<valuecolor blue="255" green="0" red="0" value="190.0"/>
							<brightness max="2" maxFactor="10" min="1" minFactor="11"/>
							<minmaxValue isMaxOverwrite="1" isMinOverwrite="1" maxValueOverwrite="170" minValueOverwrite="110"/>
						</legendcolor>
						
						<legendcolor prefname="speed">
							<valuecolor blue="255" green="255" red="255" value="10.0"/>
							<valuecolor blue="0" green="255" red="198" value="50.0"/>
							<valuecolor blue="0" green="255" red="198" value="100.0"/>
							<valuecolor blue="255" green="128" red="0" value="150.0"/>
							<valuecolor blue="255" green="128" red="0" value="190.0"/>
							<brightness max="1" maxFactor="48" min="0" minFactor="16"/>
							<minmaxValue isMaxOverwrite="1" isMinOverwrite="1" maxValueOverwrite="30" minValueOverwrite="0"/>
						</legendcolor>
						
						<legendcolor prefname="pace">
							<valuecolor blue="0" green="0" red="255" value="10.0"/>
							<valuecolor blue="0" green="255" red="255" value="50.0"/>
							<valuecolor blue="0" green="169" red="0" value="100.0"/>
							<valuecolor blue="255" green="255" red="0" value="150.0"/>
							<valuecolor blue="255" green="0" red="0" value="190.0"/>
							<brightness max="1" maxFactor="8" min="1" minFactor="17"/>
							<minmaxValue isMaxOverwrite="0" isMinOverwrite="0" maxValueOverwrite="0" minValueOverwrite="0"/>
						</legendcolor>
						
						<legendcolor prefname="gradient">
							<valuecolor blue="0" green="0" red="0" value="10.0"/>
							<valuecolor blue="0" green="0" red="0" value="50.0"/>
							<valuecolor blue="128" green="0" red="128" value="100.0"/>
							<valuecolor blue="223" green="0" red="223" value="150.0"/>
							<valuecolor blue="255" green="255" red="255" value="190.0"/>
							<brightness max="2" maxFactor="20" min="1" minFactor="29"/>
							<minmaxValue isMaxOverwrite="1" isMinOverwrite="1" maxValueOverwrite="14" minValueOverwrite="-6"/>
						</legendcolor>
		 * </code>
		 */

		MAP3_DEFAULT_COLOR_ALTITUDE = new Map3ColorProfile(//
				//
				new RGBVertex[] {
			new RGBVertex(0, 0xEC, 0x20, 0x20),
			new RGBVertex(25, 255, 85, 13),
			new RGBVertex(50, 255, 255, 0),
			new RGBVertex(75, 0, 170, 9),
			new RGBVertex(100, 23, 163, 255) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				38,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				39);

		MAP3_DEFAULT_COLOR_GRADIENT = new Map3ColorProfile(//
				//
				new RGBVertex[] {
			new RGBVertex(0, 0, 0, 255),
			new RGBVertex(25, 0, 255, 255),
			new RGBVertex(50, 0, 237, 0),
			new RGBVertex(75, 255, 255, 0),
			new RGBVertex(100, 255, 0, 0) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				23,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				//
				// overwrite min/max values
				true,
				-10,
				true,
				10);

		MAP3_DEFAULT_COLOR_PACE = new Map3ColorProfile(//
				//
				new RGBVertex[] {
			new RGBVertex(0, 255, 0, 0),
			new RGBVertex(25, 255, 255, 0),
			new RGBVertex(50, 0, 169, 0),
			new RGBVertex(75, 0, 255, 255),
			new RGBVertex(100, 0, 0, 255) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				17,
				MapColorProfile.BRIGHTNESS_DIMMING,
				8);

		MAP3_DEFAULT_COLOR_PULSE = new Map3ColorProfile(//
				//
				new RGBVertex[] {
			new RGBVertex(0, 0, 203, 0),
			new RGBVertex(25, 57, 255, 0),
			new RGBVertex(50, 255, 255, 0),
			new RGBVertex(75, 255, 0, 0),
			new RGBVertex(100, 255, 0, 247) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				11,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10);

		MAP3_DEFAULT_COLOR_SPEED = new Map3ColorProfile(//
				//
				new RGBVertex[] {
			new RGBVertex(0, 0, 0, 255),
			new RGBVertex(25, 0, 255, 255),
			new RGBVertex(50, 0, 169, 0),
			new RGBVertex(75, 255, 255, 0),
			new RGBVertex(100, 255, 0, 0) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				17,
				MapColorProfile.BRIGHTNESS_DIMMING,
				8);
	}

	private Map3ColorManager() {}

	private static ArrayList<Map3ColorDefinition> createDefaultColors() {

		final ArrayList<Map3ColorDefinition> allColorDef = new ArrayList<Map3ColorDefinition>();

		allColorDef.add(new Map3ColorDefinition(
				MapColorId.Altitude,
				Messages.Graph_Label_Altitude,
				MAP3_DEFAULT_COLOR_ALTITUDE));

		allColorDef.add(new Map3ColorDefinition(
				MapColorId.Pulse,
				Messages.Graph_Label_Heartbeat,
				MAP3_DEFAULT_COLOR_PULSE));

		allColorDef.add(new Map3ColorDefinition(//
				MapColorId.Speed,
				Messages.Graph_Label_Speed,
				MAP3_DEFAULT_COLOR_SPEED));

		allColorDef.add(new Map3ColorDefinition(//
				MapColorId.Pace,
				Messages.Graph_Label_Pace,
				MAP3_DEFAULT_COLOR_PACE));

		allColorDef.add(new Map3ColorDefinition(
				MapColorId.Gradient,
				Messages.Graph_Label_Gradient,
				MAP3_DEFAULT_COLOR_GRADIENT));

		return allColorDef;
	}

	/**
	 * Disposes all profile images.
	 */
	public static void disposeProfileImages() {

		for (final Map3ColorDefinition colorDef : _map3ColorDefinitions) {
			for (final Map3ColorProfile colorProfile : colorDef.getColorProfiles()) {
				UI.disposeResource(colorProfile.getProfileImage().getImage());
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
