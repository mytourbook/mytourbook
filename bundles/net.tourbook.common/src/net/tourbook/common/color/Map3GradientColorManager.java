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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Manage gradient colors which are displayed in a 3D map.
 */
/**
 * @author IBM_ADMIN
 *
 */
/**
 * @author IBM_ADMIN
 */
public class Map3GradientColorManager {

	private static final int								FILE_VERSION					= 1;

	/**
	 * Key is the color id.
	 * <p>
	 * Default colors are defined in the UI and then replaced with created code from
	 * {@link #createCodeForDefaultColors(ArrayList)}.
	 */
	private static HashMap<MapGraphId, Map3ColorDefinition>	_map3ColorDefinitions			= new HashMap<MapGraphId, Map3ColorDefinition>();
	private static ArrayList<Map3ColorDefinition>			_sortedColorDefinitions;

	private static HashMap<MapGraphId, Map3ColorProfile>	DEFAULT_PROFILES				= new HashMap<MapGraphId, Map3ColorProfile>();

	private static final Map3ColorProfile					DEFAULT_PROFILE;

	private static final String								ERROR_GRAPH_ID_IS_NOT_AVAILABLE	= "Requested graph id '%s' is not available.";		//$NON-NLS-1$

	private static final String								MAP3_COLOR_FILE					= "map3colors.xml";								//$NON-NLS-1$

	private static final String								TAG_ROOT						= "map3colors";									//$NON-NLS-1$

	private static final String								TAG_COLOR_DEFINITION			= "colorDefinition";								//$NON-NLS-1$
	private static final String								TAG_COLOR_PROFILE				= "colorProfile";									//$NON-NLS-1$
	private static final String								TAG_VERTICES					= "vertices";										//$NON-NLS-1$
	private static final String								TAG_VERTEX						= "vertex";										//$NON-NLS-1$

	// common attributes
	private static final String								ATTR_BLUE						= "blue";											//$NON-NLS-1$
	private static final String								ATTR_GRAPH_ID					= "graphId";										//$NON-NLS-1$
	private static final String								ATTR_GREEN						= "green";											//$NON-NLS-1$
	private static final String								ATTR_IS_ABSOLUTE_VALUES			= "isAbsoluteValues";								//$NON-NLS-1$
	private static final String								ATTR_IS_ACTIVE_PROFILE			= "isActiveProfile";								//$NON-NLS-1$
	private static final String								ATTR_IS_OVERWRITE_LEGEND_VALUES	= "isOverwriteLegendValues";						//$NON-NLS-1$
	private static final String								ATTR_NAME						= "name";											//$NON-NLS-1$
	private static final String								ATTR_OPACITY					= "opacity";										//$NON-NLS-1$
	private static final String								ATTR_RED						= "red";											//$NON-NLS-1$
	private static final String								ATTR_VALUE						= "value";											//$NON-NLS-1$
	private static final String								ATTR_VERSION					= "version";										//$NON-NLS-1$

	private static final RGB								DEFAULT_RGB						= new RGB(0xFF, 0x8B, 0x8B);

	// opacity
	public static final int									OPACITY_MIN						= 0;
	public static final int									OPACITY_MAX						= 100;
	public static final double								OPACITY_DIGITS_FACTOR			= 100.0;
	public static final int									OPACITY_DIGITS					= 2;
	public static final float								OPACITY_DEFAULT					= 1.0f;

	/**
	 * Define 3D map default colors.
	 * <p>
	 * Default colors are defined in the UI and the default colors are then replaced with created
	 * code from {@link #createCodeForDefaultColors(ArrayList)}.
	 */
	static {

		final ArrayList<Map3ColorProfile> defaultAltitude = new ArrayList<Map3ColorProfile>();

		defaultAltitude.add(new Map3ColorProfile( //
				"0", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 181, 0, 0, 1.0f),
			new RGBVertex(10, 255, 106, 0, 1.0f),
			new RGBVertex(20, 255, 255, 255, 1.0f),
			new RGBVertex(30, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"10", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(400, 19, 77, 0, 1.0f),
			new RGBVertex(500, 255, 255, 0, 1.0f),
			new RGBVertex(600, 204, 0, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				50));

		defaultAltitude.add(new Map3ColorProfile( //
				"14", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(400, 98, 132, 0, 1.0f),
			new RGBVertex(500, 191, 255, 0, 1.0f),
			new RGBVertex(700, 255, 102, 0, 1.0f),
			new RGBVertex(900, 0, 180, 235, 1.0f),
			new RGBVertex(1000, 162, 218, 235, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"15", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(400, 38, 24, 0, 1.0f),
			new RGBVertex(600, 255, 255, 0, 1.0f),
			new RGBVertex(750, 255, 50, 0, 1.0f),
			new RGBVertex(1000, 0, 144, 255, 1.0f),
			new RGBVertex(1100, 87, 182, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				20));

		defaultAltitude.add(new Map3ColorProfile( //
				"30", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] { new RGBVertex(400, 255, 3, 158, 1.0f), new RGBVertex(500, 8, 255, 14, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"31", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 0, 0, 0, 1.0f),
			new RGBVertex(10, 0, 234, 255, 1.0f),
			new RGBVertex(15, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"50", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 0, 0, 0, 1.0f),
			new RGBVertex(200, 255, 255, 255, 1.0f),
			new RGBVertex(400, 0, 0, 0, 1.0f),
			new RGBVertex(600, 255, 255, 255, 1.0f),
			new RGBVertex(800, 0, 0, 0, 1.0f),
			new RGBVertex(1000, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				100,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"51", //$NON-NLS-1$
				false,
				false,
				new RGBVertex[] {
			new RGBVertex(0, 247, 71, 0, 1.0f),
			new RGBVertex(100, 0, 0, 0, 1.0f),
			new RGBVertex(200, 247, 71, 0, 1.0f),
			new RGBVertex(300, 0, 0, 0, 1.0f),
			new RGBVertex(400, 247, 71, 0, 1.0f),
			new RGBVertex(500, 237, 255, 167, 1.0f),
			new RGBVertex(600, 247, 71, 0, 1.0f),
			new RGBVertex(700, 0, 0, 0, 1.0f),
			new RGBVertex(800, 247, 71, 0, 1.0f),
			new RGBVertex(900, 0, 0, 0, 1.0f),
			new RGBVertex(1000, 247, 71, 0, 1.0f),
			new RGBVertex(1100, 215, 251, 255, 1.0f),
			new RGBVertex(1200, 247, 71, 0, 1.0f),
			new RGBVertex(1300, 0, 0, 0, 1.0f),
			new RGBVertex(1400, 247, 71, 0, 1.0f),
			new RGBVertex(2000, 0, 0, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_DIMMING,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"55", //$NON-NLS-1$
				false,
				false,
				new RGBVertex[] {
			new RGBVertex(0, 6, 217, 249, 1.0f),
			new RGBVertex(100, 0, 0, 0, 1.0f),
			new RGBVertex(200, 6, 217, 249, 1.0f),
			new RGBVertex(300, 0, 0, 0, 1.0f),
			new RGBVertex(400, 6, 217, 249, 1.0f),
			new RGBVertex(500, 0, 0, 0, 1.0f),
			new RGBVertex(600, 6, 217, 249, 1.0f),
			new RGBVertex(700, 0, 0, 0, 1.0f),
			new RGBVertex(800, 6, 217, 249, 1.0f),
			new RGBVertex(900, 0, 0, 0, 1.0f),
			new RGBVertex(1000, 6, 217, 249, 1.0f),
			new RGBVertex(1100, 0, 0, 0, 1.0f),
			new RGBVertex(1200, 6, 217, 249, 1.0f),
			new RGBVertex(1300, 0, 0, 0, 1.0f),
			new RGBVertex(1400, 6, 217, 249, 1.0f),
			new RGBVertex(2000, 0, 0, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_DIMMING,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"56", //$NON-NLS-1$
				false,
				false,
				new RGBVertex[] {
			new RGBVertex(0, 6, 217, 249, 1.0f),
			new RGBVertex(100, 255, 255, 255, 0.0f),
			new RGBVertex(200, 6, 217, 249, 1.0f),
			new RGBVertex(300, 255, 255, 255, 0.0f),
			new RGBVertex(400, 6, 217, 249, 1.0f),
			new RGBVertex(500, 255, 255, 255, 0.0f),
			new RGBVertex(600, 6, 217, 249, 1.0f),
			new RGBVertex(700, 255, 255, 255, 0.0f),
			new RGBVertex(800, 6, 217, 249, 1.0f),
			new RGBVertex(900, 255, 255, 255, 0.0f),
			new RGBVertex(1000, 6, 217, 249, 1.0f),
			new RGBVertex(1100, 255, 255, 255, 0.0f),
			new RGBVertex(1200, 6, 217, 249, 1.0f),
			new RGBVertex(1300, 255, 255, 255, 0.0f),
			new RGBVertex(1400, 6, 217, 249, 1.0f),
			new RGBVertex(2000, 255, 255, 255, 0.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_DIMMING,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"57", //$NON-NLS-1$
				false,
				false,
				new RGBVertex[] {
			new RGBVertex(0, 6, 217, 249, 1.0f),
			new RGBVertex(100, 255, 255, 255, 1.0f),
			new RGBVertex(200, 6, 217, 249, 1.0f),
			new RGBVertex(300, 255, 255, 255, 1.0f),
			new RGBVertex(400, 6, 217, 249, 1.0f),
			new RGBVertex(500, 255, 255, 255, 1.0f),
			new RGBVertex(600, 6, 217, 249, 1.0f),
			new RGBVertex(700, 255, 255, 255, 1.0f),
			new RGBVertex(800, 6, 217, 249, 1.0f),
			new RGBVertex(900, 255, 255, 255, 1.0f),
			new RGBVertex(1000, 6, 217, 249, 1.0f),
			new RGBVertex(1100, 255, 255, 255, 1.0f),
			new RGBVertex(1200, 6, 217, 249, 1.0f),
			new RGBVertex(1300, 255, 255, 255, 1.0f),
			new RGBVertex(1400, 6, 217, 249, 1.0f),
			new RGBVertex(2000, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_DIMMING,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"60", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(400, 246, 255, 3, 1.0f),
			new RGBVertex(450, 246, 255, 3, 1.0f),
			new RGBVertex(450, 167, 8, 255, 1.0f),
			new RGBVertex(500, 167, 8, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultAltitude.add(new Map3ColorProfile( //
				"61", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 246, 255, 3, 1.0f),
			new RGBVertex(200, 246, 255, 3, 1.0f),
			new RGBVertex(200, 167, 8, 255, 1.0f),
			new RGBVertex(400, 167, 8, 255, 1.0f),
			new RGBVertex(400, 255, 255, 255, 1.0f),
			new RGBVertex(600, 255, 255, 255, 1.0f),
			new RGBVertex(600, 251, 0, 109, 1.0f),
			new RGBVertex(800, 251, 0, 109, 1.0f),
			new RGBVertex(800, 0, 147, 255, 1.0f),
			new RGBVertex(1000, 0, 147, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				80));

		defaultAltitude.add(new Map3ColorProfile( //
				"32", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] { new RGBVertex(0, 0, 0, 0, 1.0f), new RGBVertex(1, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		final ArrayList<Map3ColorProfile> defaultPulse = new ArrayList<Map3ColorProfile>();

		defaultPulse.add(new Map3ColorProfile( //
				"0", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(100, 181, 0, 0, 1.0f),
			new RGBVertex(145, 255, 106, 0, 1.0f),
			new RGBVertex(155, 255, 255, 255, 1.0f),
			new RGBVertex(200, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultPulse.add(new Map3ColorProfile( //
				"10", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(60, 89, 255, 0, 1.0f),
			new RGBVertex(120, 255, 251, 0, 1.0f),
			new RGBVertex(200, 255, 36, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				50,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultPulse.add(new Map3ColorProfile( //
				"11", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(100, 28, 206, 6, 0.0f),
			new RGBVertex(110, 28, 206, 6, 0.0f),
			new RGBVertex(120, 28, 206, 6, 1.0f),
			new RGBVertex(140, 251, 240, 6, 1.0f),
			new RGBVertex(155, 247, 6, 1, 1.0f),
			new RGBVertex(160, 247, 6, 1, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				50,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				100));

		defaultPulse.add(new Map3ColorProfile( //
				"20", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(80, 201, 246, 0, 1.0f),
			new RGBVertex(100, 1, 237, 0, 1.0f),
			new RGBVertex(101, 0, 0, 0, 0.0f),
			new RGBVertex(140, 255, 255, 255, 0.0f),
			new RGBVertex(155, 0, 0, 0, 0.0f),
			new RGBVertex(155, 250, 8, 1, 1.0f),
			new RGBVertex(180, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				50,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultPulse.add(new Map3ColorProfile( //
				"22", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(80, 255, 255, 255, 0.0f),
			new RGBVertex(155, 255, 255, 255, 0.0f),
			new RGBVertex(155, 250, 8, 1, 1.0f),
			new RGBVertex(180, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				50,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				10));

		final ArrayList<Map3ColorProfile> defaultSpeed = new ArrayList<Map3ColorProfile>();

		defaultSpeed.add(new Map3ColorProfile( //
				"0", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 181, 0, 0, 1.0f),
			new RGBVertex(10, 255, 106, 0, 1.0f),
			new RGBVertex(20, 255, 255, 255, 1.0f),
			new RGBVertex(30, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultSpeed.add(new Map3ColorProfile( //
				"10", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 53, 91, 0, 1.0f),
			new RGBVertex(10, 166, 227, 0, 1.0f),
			new RGBVertex(20, 255, 64, 0, 1.0f),
			new RGBVertex(30, 161, 40, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultSpeed.add(new Map3ColorProfile( //
				"11", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 21, 36, 0, 1.0f),
			new RGBVertex(10, 94, 128, 0, 1.0f),
			new RGBVertex(20, 130, 30, 0, 1.0f),
			new RGBVertex(30, 51, 12, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultSpeed.add(new Map3ColorProfile( //
				"20", //$NON-NLS-1$
				false,
				true,
				new RGBVertex[] {
			new RGBVertex(9, 156, 213, 0, 1.0f),
			new RGBVertex(10, 156, 213, 0, 1.0f),
			new RGBVertex(15, 255, 255, 255, 0.0f),
			new RGBVertex(20, 0, 0, 0, 0.0f),
			new RGBVertex(25, 255, 255, 255, 0.0f),
			new RGBVertex(30, 231, 61, 0, 1.0f),
			new RGBVertex(31, 231, 61, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				100,
				MapColorProfile.BRIGHTNESS_DIMMING,
				30));

		defaultSpeed.add(new Map3ColorProfile( //
				"Breaktime", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 219, 251, 0, 1.0f),
			new RGBVertex(3, 219, 251, 0, 1.0f),
			new RGBVertex(5, 219, 251, 0, 0.0f),
			new RGBVertex(20, 255, 255, 255, 0.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultSpeed.add(new Map3ColorProfile( //
				"12", //$NON-NLS-1$
				false,
				false,
				new RGBVertex[] {
			new RGBVertex(5, 82, 0, 172, 1.0f),
			new RGBVertex(20, 249, 244, 0, 1.0f),
			new RGBVertex(25, 249, 244, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		final ArrayList<Map3ColorProfile> defaultPace = new ArrayList<Map3ColorProfile>();

		defaultPace.add(new Map3ColorProfile( //
				"0", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 181, 0, 0, 1.0f),
			new RGBVertex(150, 255, 106, 0, 1.0f),
			new RGBVertex(300, 255, 255, 255, 1.0f),
			new RGBVertex(450, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultPace.add(new Map3ColorProfile( //
				"1", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(0, 0, 0, 0, 1.0f),
			new RGBVertex(90, 43, 80, 255, 1.0f),
			new RGBVertex(240, 255, 255, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		final ArrayList<Map3ColorProfile> defaultGradient = new ArrayList<Map3ColorProfile>();

		defaultGradient.add(new Map3ColorProfile( //
				"10", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-10, 181, 0, 0, 1.0f),
			new RGBVertex(-5, 255, 106, 0, 1.0f),
			new RGBVertex(0, 255, 255, 255, 0.0f),
			new RGBVertex(10, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultGradient.add(new Map3ColorProfile( //
				"11", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-10, 181, 0, 0, 1.0f),
			new RGBVertex(-6, 181, 0, 0, 1.0f),
			new RGBVertex(-6, 255, 106, 0, 1.0f),
			new RGBVertex(-2, 255, 106, 0, 1.0f),
			new RGBVertex(-2, 255, 255, 255, 0.0f),
			new RGBVertex(2, 255, 255, 255, 0.0f),
			new RGBVertex(2, 140, 217, 255, 1.0f),
			new RGBVertex(6, 140, 217, 255, 1.0f),
			new RGBVertex(6, 0, 170, 255, 1.0f),
			new RGBVertex(10, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		defaultGradient.add(new Map3ColorProfile( //
				"12", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-10, 0, 255, 0, 1.0f),
			new RGBVertex(0, 0, 0, 0, 1.0f),
			new RGBVertex(10, 255, 0, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				20));

		defaultGradient.add(new Map3ColorProfile( //
				"13", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-7, 87, 219, 6, 1.0f),
			new RGBVertex(-4, 139, 255, 66, 1.0f),
			new RGBVertex(-4, 200, 255, 166, 0.0f),
			new RGBVertex(0, 255, 255, 255, 0.0f),
			new RGBVertex(3, 166, 0, 0, 0.0f),
			new RGBVertex(8, 255, 4, 0, 1.0f),
			new RGBVertex(11, 255, 4, 0, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				60,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				80));

		defaultGradient.add(new Map3ColorProfile( //
				"20", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-7, 0, 0, 0, 1.0f),
			new RGBVertex(0, 0, 171, 245, 1.0f),
			new RGBVertex(7, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				100,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				74));

		defaultGradient.add(new Map3ColorProfile( //
				"21", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-7, 0, 0, 0, 1.0f),
			new RGBVertex(0, 245, 0, 163, 1.0f),
			new RGBVertex(7, 255, 255, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DIMMING,
				100,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				74));

		defaultGradient.add(new Map3ColorProfile( //
				"1", //$NON-NLS-1$
				true,
				true,
				new RGBVertex[] {
			new RGBVertex(-10, 181, 0, 0, 1.0f),
			new RGBVertex(-5, 255, 106, 0, 1.0f),
			new RGBVertex(0, 255, 255, 255, 1.0f),
			new RGBVertex(10, 0, 170, 255, 1.0f), },
				MapColorProfile.BRIGHTNESS_DEFAULT,
				60,
				MapColorProfile.BRIGHTNESS_DEFAULT,
				100));

		/**
		 * ----------------------------------------------------------------------------------------
		 */

		// set default default value
		DEFAULT_PROFILE = defaultAltitude.get(0);

		DEFAULT_PROFILES.put(MapGraphId.Altitude, defaultAltitude.get(0).clone());
		DEFAULT_PROFILES.put(MapGraphId.Gradient, defaultGradient.get(0).clone());
		DEFAULT_PROFILES.put(MapGraphId.Pace, defaultPace.get(0).clone());
		DEFAULT_PROFILES.put(MapGraphId.Pulse, defaultPulse.get(0).clone());
		DEFAULT_PROFILES.put(MapGraphId.Speed, defaultSpeed.get(0).clone());

		/*
		 * Setup all color definitions with defaults.
		 */
		_map3ColorDefinitions.put(MapGraphId.Altitude, new Map3ColorDefinition(
				MapGraphId.Altitude,
				Messages.Graph_Label_Altitude,
				defaultAltitude));

		_map3ColorDefinitions.put(MapGraphId.Gradient, new Map3ColorDefinition(
				MapGraphId.Gradient,
				Messages.Graph_Label_Gradient,
				defaultGradient));

		_map3ColorDefinitions.put(MapGraphId.Pace, new Map3ColorDefinition(//
				MapGraphId.Pace,
				Messages.Graph_Label_Pace,
				defaultPace));

		_map3ColorDefinitions.put(MapGraphId.Pulse, new Map3ColorDefinition(//
				MapGraphId.Pulse,
				Messages.Graph_Label_Heartbeat,
				defaultPulse));

		_map3ColorDefinitions.put(MapGraphId.Speed, new Map3ColorDefinition(//
				MapGraphId.Speed,
				Messages.Graph_Label_Speed,
				defaultSpeed));

		_initColorDefinitions();
	}

	private Map3GradientColorManager() {}

	private static void _initColorDefinitions() {

		// overwrite default colors with saved colors
		readColors();

		// sort by name
		_sortedColorDefinitions = new ArrayList<Map3ColorDefinition>(_map3ColorDefinitions.values());
		Collections.sort(_sortedColorDefinitions);
	}

	public static void addColorProvider(final Map3GradientColorProvider newColorProvider) {

		final MapGraphId graphId = newColorProvider.getGraphId();
		final Map3ColorDefinition colorDefinition = getColorDefinition(graphId);

		colorDefinition.addColorProvider(newColorProvider);
	}

	private static void createCodeForDefaultColors(final ArrayList<Map3ColorDefinition> colorDefinitions) {

		for (final Map3ColorDefinition colorDefinition : colorDefinitions) {

			final ArrayList<Map3GradientColorProvider> colorProviders = colorDefinition.getColorProviders();

			final MapGraphId graphId = colorDefinition.getGraphId();

			final String dumpDef = UI.NEW_LINE //
					+ "final ArrayList<Map3ColorProfile> default" //$NON-NLS-1$
					+ graphId
					+ " = new ArrayList<Map3ColorProfile>();"; //$NON-NLS-1$

			System.out.println(dumpDef);

			for (int providerIndex = 0; providerIndex < colorProviders.size(); providerIndex++) {

				final Map3GradientColorProvider colorProvider = colorProviders.get(providerIndex);
				final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();

				final StringBuilder sbVertices = new StringBuilder();

				for (final RGBVertex vertex : colorProfile.getProfileImage().getRgbVertices()) {
					final RGB rgb = vertex.getRGB();
					sbVertices.append(//
							("new RGBVertex(" + UI.NEW_LINE) //$NON-NLS-1$
									+ (vertex.getValue() + ", ") //$NON-NLS-1$
									+ (rgb.red + ", ") //$NON-NLS-1$
									+ (rgb.green + ", ") //$NON-NLS-1$
									+ (rgb.blue + ", ") //$NON-NLS-1$
									+ (Float.toString(vertex.getOpacity()) + "f), ") //$NON-NLS-1$
									+ "\n"); //$NON-NLS-1$
				}

				final String vertices = "" + // //$NON-NLS-1$
						("new RGBVertex[] {" + UI.NEW_LINE) //$NON-NLS-1$
						+ sbVertices.toString()
						+ "},"; //$NON-NLS-1$

				final String dump = UI.NEW_LINE//
						+ ("default" + graphId + ".add(") //$NON-NLS-1$ //$NON-NLS-2$
						+ ("new Map3ColorProfile" + "( //" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
						+ ("\"" + colorProfile.getProfileName() + "\", //$NON-NLS-1$" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
						+ (Boolean.toString(colorProfile.isAbsoluteValues()) + "," + UI.NEW_LINE) //$NON-NLS-1$
						+ (Boolean.toString(colorProfile.isOverwriteLegendValues()) + "," + UI.NEW_LINE) //$NON-NLS-1$
						+ vertices
						+ (createCodeForDefaultColors_GetBrightness(colorProfile.minBrightness) + "," + UI.NEW_LINE) //$NON-NLS-1$
						+ (colorProfile.minBrightnessFactor + "," + UI.NEW_LINE) //$NON-NLS-1$
						+ (createCodeForDefaultColors_GetBrightness(colorProfile.maxBrightness) + "," + UI.NEW_LINE) //$NON-NLS-1$
						+ (colorProfile.maxBrightnessFactor + UI.NEW_LINE)
						+ "));" //$NON-NLS-1$
						+ UI.NEW_LINE
						+ UI.NEW_LINE;

				System.out.println(dump);
			}
		}
	}

	private static String createCodeForDefaultColors_GetBrightness(final int brightness) {

		switch (brightness) {
		case MapColorProfile.BRIGHTNESS_DIMMING:
			return "MapColorProfile.BRIGHTNESS_DIMMING"; //$NON-NLS-1$

		case MapColorProfile.BRIGHTNESS_LIGHTNING:
			return "MapColorProfile.BRIGHTNESS_LIGHTNING"; //$NON-NLS-1$

		default:
			return "MapColorProfile.BRIGHTNESS_DEFAULT"; //$NON-NLS-1$
		}
	}

	/**
	 * Disposes all profile images.
	 */
	public static void disposeProfileImages() {

		for (final Map3ColorDefinition colorDef : _map3ColorDefinitions.values()) {
			for (final Map3GradientColorProvider colorProvider : colorDef.getColorProviders()) {

				final Map3ColorProfile map3ColorProfile = colorProvider.getMap3ColorProfile();

				UI.disposeResource(map3ColorProfile.getProfileImage().getImage());
			}
		}
	}

	public static Map3GradientColorProvider getActiveMap3ColorProvider(final MapGraphId graphId) {

		Map3GradientColorProvider activeColorProvider = null;

		final ArrayList<Map3GradientColorProvider> colorProviders = getColorProviders(graphId);

		for (final Map3GradientColorProvider colorProvider : colorProviders) {
			if (colorProvider.getMap3ColorProfile().isActiveColorProfile()) {

				activeColorProvider = colorProvider;
				break;
			}
		}

		if (activeColorProvider == null) {

			// this case should not happen, set first as active

			activeColorProvider = colorProviders.get(0);
			activeColorProvider.getMap3ColorProfile().setIsActiveColorProfile(true);
		}

		return activeColorProvider;
	}

	public static Map3ColorDefinition getColorDefinition(final MapGraphId graphId) {

		final HashMap<MapGraphId, Map3ColorDefinition> mapColorDefinitions = getColorDefinitions();

		final Map3ColorDefinition colorDef = mapColorDefinitions.get(graphId);

		if (colorDef != null) {
			return colorDef;
		}

		// this case should not happen
		StatusUtil.logError(String.format(ERROR_GRAPH_ID_IS_NOT_AVAILABLE, graphId));

		return mapColorDefinitions.get(MapGraphId.Altitude);
	}

	/**
	 * @return Returns color definitions which are defined for a 3D map.
	 */
	private static HashMap<MapGraphId, Map3ColorDefinition> getColorDefinitions() {

		return _map3ColorDefinitions;
	}

	/**
	 * @param graphId
	 * @return Returns all color profiles for the requested {@link MapGraphId}.
	 */
	public static ArrayList<Map3GradientColorProvider> getColorProviders(final MapGraphId graphId) {

		return getColorDefinition(graphId).getColorProviders();
	}

	/**
	 * @param graphId
	 * @return Returns a clone from the default color profile of the requested {@link MapGraphId}.
	 */
	public static Map3ColorProfile getDefaultColorProfile(final MapGraphId graphId) {

		final Map3ColorProfile colorProfile = DEFAULT_PROFILES.get(graphId);

		if (colorProfile != null) {
			return colorProfile.clone();
		}

		// this case should not happen
		StatusUtil.logError(String.format(ERROR_GRAPH_ID_IS_NOT_AVAILABLE, graphId));

		return DEFAULT_PROFILE.clone();
	}

	public static ArrayList<Map3ColorDefinition> getSortedColorDefinitions() {

		return _sortedColorDefinitions;
	}

	private static void readColors() {

		// get loaded color definitions
		final ArrayList<Map3ColorDefinition> xmlColorDefinitions = readColors_0_FromXml();

		if (xmlColorDefinitions == null) {

			for (final Map3ColorDefinition colorDef : _map3ColorDefinitions.values()) {

				for (final Map3GradientColorProvider colorProvider : colorDef.getColorProviders()) {

					final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();

					colorProfile.setIsActiveColorProfile(true);

					// set only first default color profile as active
					break;
				}
			}

			return;
		}

		// replace existing color providers with loaded color providers
		for (final Map3ColorDefinition xmlColorDef : xmlColorDefinitions) {

			final ArrayList<Map3GradientColorProvider> xmlColorProvider = xmlColorDef.getColorProviders();
			if (xmlColorProvider.size() > 0) {

				final MapGraphId xmlGraphId = xmlColorDef.getGraphId();
				final Map3ColorDefinition defaultColorDef = getColorDefinition(xmlGraphId);

				defaultColorDef.setColorProvider(xmlColorProvider);
			}
		}

		// ensure that only one color provider for each graph id is active
		for (final Map3ColorDefinition colorDef : getColorDefinitions().values()) {

			final ArrayList<Map3GradientColorProvider> colorProviders = colorDef.getColorProviders();

			Map3GradientColorProvider activeColorProvider = null;

			for (final Map3GradientColorProvider colorProvider : colorProviders) {

				final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();

				final boolean isActiveColorProfile = colorProfile.isActiveColorProfile();

				if (activeColorProvider == null && isActiveColorProfile) {

					activeColorProvider = colorProvider;

				} else {

					// ensure that all other color providers are NOT active

					colorProfile.setIsActiveColorProfile(false);
				}
			}

			if (activeColorProvider == null) {

				// ensure that one active color provider is set, set first as active

				colorProviders.get(0).getMap3ColorProfile().setIsActiveColorProfile(true);
			}
		}
	}

	/**
	 * Read colors from a xml file.
	 * 
	 * @return
	 */
	private static ArrayList<Map3ColorDefinition> readColors_0_FromXml() {

		final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
		final File file = stateLocation.append(MAP3_COLOR_FILE).toFile();

		// check if file is available
		if (file.exists() == false) {
			return null;
		}

		InputStreamReader reader = null;

		ArrayList<Map3ColorDefinition> colorDefinitions = null;

		try {
			reader = new InputStreamReader(new FileInputStream(file), UI.UTF_8);

			final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

			colorDefinitions = readColors_10_MapColors(xmlRoot);

		} catch (final UnsupportedEncodingException e) {
			StatusUtil.log(e);
		} catch (final FileNotFoundException e) {
			StatusUtil.log(e);
		} catch (final WorkbenchException e) {
			StatusUtil.log(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}

		/*
		 * This code is only run when default colors are needed to be modified.
		 */
		final boolean isCreateDefaultCode = false;
		if (isCreateDefaultCode) {
			createCodeForDefaultColors(colorDefinitions);
		}

		return colorDefinitions;
	}

	private static ArrayList<Map3ColorDefinition> readColors_10_MapColors(final XMLMemento xmlRoot) {

		final ArrayList<Map3ColorDefinition> colorDefinitions = new ArrayList<Map3ColorDefinition>();

		final IMemento[] xmlColorDefinitions = xmlRoot.getChildren(TAG_COLOR_DEFINITION);

		for (final IMemento xmlColorDef : xmlColorDefinitions) {

			final String xmlGraphId = xmlColorDef.getString(ATTR_GRAPH_ID);

			if (xmlGraphId == null) {
				continue;
			}

			MapGraphId graphId;

			try {
				graphId = MapGraphId.valueOf(xmlGraphId);
			} catch (final Exception e) {
				// unknown graph id
				continue;
			}

			final Map3ColorDefinition colorDef = new Map3ColorDefinition(graphId);
			colorDefinitions.add(colorDef);

			final IMemento[] xmlProfiles = xmlColorDef.getChildren(TAG_COLOR_PROFILE);

			for (final IMemento xmlProfile : xmlProfiles) {

				final Map3ColorProfile colorProfile = new Map3ColorProfile();
				colorDef.addProfile(colorProfile);

				colorProfile.setProfileName(Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING));
				colorProfile.setIsActiveColorProfile(Util.getXmlBoolean(xmlProfile, ATTR_IS_ACTIVE_PROFILE, false));
				colorProfile.setIsAbsoluteValues(Util.getXmlBoolean(xmlProfile, ATTR_IS_ABSOLUTE_VALUES, false));
				colorProfile.setIsOverwriteLegendValues(Util.getXmlBoolean(xmlProfile,//
						ATTR_IS_OVERWRITE_LEGEND_VALUES,
						true));

				readColors_20_Brightness(xmlProfile, colorProfile);
				readColors_30_Vertices(xmlProfile, colorProfile);
			}
		}

		return colorDefinitions;
	}

	private static void readColors_20_Brightness(final IMemento xmlProfile, final Map3ColorProfile colorProfile) {

		final IMemento xmlBrightness = xmlProfile.getChild(GraphColorManager.MEMENTO_CHILD_BRIGHTNESS);

		if (xmlBrightness == null) {
			return;
		}

		colorProfile.setMinBrightness(Util.getXmlInteger(
				xmlBrightness,
				GraphColorManager.TAG_BRIGHTNESS_MIN,
				MapColorProfile.BRIGHTNESS_DEFAULT));

		colorProfile.setMaxBrightness(Util.getXmlInteger(
				xmlBrightness,
				GraphColorManager.TAG_BRIGHTNESS_MAX,
				MapColorProfile.BRIGHTNESS_DEFAULT));

		colorProfile.setMinBrightnessFactor(Util.getXmlInteger(
				xmlBrightness,
				GraphColorManager.TAG_BRIGHTNESS_MIN_FACTOR,
				MapColorProfile.BRIGHTNESS_FACTOR_DEFAULT));

		colorProfile.setMaxBrightnessFactor(Util.getXmlInteger(
				xmlBrightness,
				GraphColorManager.TAG_BRIGHTNESS_MAX_FACTOR,
				MapColorProfile.BRIGHTNESS_FACTOR_DEFAULT));
	}

	private static void readColors_30_Vertices(final IMemento xmlProfile, final Map3ColorProfile colorProfile) {

		final ArrayList<RGBVertex> vertices = new ArrayList<RGBVertex>();

		final IMemento xmlVertices = xmlProfile.getChild(TAG_VERTICES);

		if (xmlVertices == null) {
			return;
		}

		for (final IMemento xmlVertex : xmlVertices.getChildren()) {

			vertices.add(new RGBVertex(//
					Util.getXmlInteger(xmlVertex, ATTR_VALUE, 0),
					Util.getXmlRgb(xmlVertex, DEFAULT_RGB),
					Util.getXmlFloatInt(xmlVertex, ATTR_OPACITY, OPACITY_DEFAULT, OPACITY_MIN, OPACITY_MAX)));
		}

		colorProfile.getProfileImage().setVertices(vertices);
	}

	/**
	 * Replace a profile with another profile.
	 * 
	 * @param originalColorProvider
	 * @param modifiedColorProvider
	 */
	public static void replaceColorProvider(final Map3GradientColorProvider originalColorProvider,
											final Map3GradientColorProvider modifiedColorProvider) {

		final MapGraphId originalGraphId = originalColorProvider.getGraphId();
		final MapGraphId modifiedGraphId = modifiedColorProvider.getGraphId();

		// remove old profile
		final ArrayList<Map3GradientColorProvider> allOriginalProviders = getColorProviders(originalGraphId);

		// add new/modified profile
		if (originalGraphId == modifiedGraphId) {

			// graph id has not changed, replace original with modified color provider

			allOriginalProviders.remove(originalColorProvider);
			allOriginalProviders.add(modifiedColorProvider);

		} else {

			// graph id has changed, remove original and set new profile

			if (allOriginalProviders.size() < 2) {
				StatusUtil.log(new Throwable(
						"Color profile cannot be removed because the color definition contains only 1 color profile."));//$NON-NLS-1$
				return;
			}

			getColorDefinition(originalGraphId).removeColorProvider(originalColorProvider);
			getColorDefinition(modifiedGraphId).addColorProvider(modifiedColorProvider);
		}
	}

	/**
	 * Write map color data into a xml file.
	 */
	public static void saveColors() {

		BufferedWriter writer = null;

		try {

			final IPath stateLocation = Platform.getStateLocation(CommonActivator.getDefault().getBundle());
			final File file = stateLocation.append(MAP3_COLOR_FILE).toFile();

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), UI.UTF_8));

			final XMLMemento xmlRoot = saveColors_0_getXMLRoot();

			saveColors_10_MapColors(xmlRoot);

			xmlRoot.save(writer);

		} catch (final IOException e) {
			StatusUtil.log(e);
		} finally {

			if (writer != null) {
				try {
					writer.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}
	}

	private static XMLMemento saveColors_0_getXMLRoot() {

		Document document;
		try {

			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

			final Element element = document.createElement(TAG_ROOT);
			element.setAttribute(ATTR_VERSION, Integer.toString(FILE_VERSION));
			document.appendChild(element);

			return new XMLMemento(document, element);

		} catch (final ParserConfigurationException e) {
			throw new Error(e.getMessage());
		}
	}

	private static void saveColors_10_MapColors(final XMLMemento xmlRoot) {

		for (final Map3ColorDefinition colorDef : getSortedColorDefinitions()) {

			final IMemento xmlColor = xmlRoot.createChild(TAG_COLOR_DEFINITION);

			xmlColor.putString(ATTR_GRAPH_ID, colorDef.getGraphId().name());

			for (final Map3GradientColorProvider colorProvider : colorDef.getColorProviders()) {

				final MapColorProfile colorProfile = colorProvider.getColorProfile();
				if (colorProfile instanceof Map3ColorProfile) {

					final Map3ColorProfile map3ColorProfile = (Map3ColorProfile) colorProfile;

					final IMemento xmlProfile = xmlColor.createChild(TAG_COLOR_PROFILE);

					xmlProfile.putString(ATTR_NAME, map3ColorProfile.getProfileName());
					xmlProfile.putBoolean(ATTR_IS_ACTIVE_PROFILE, map3ColorProfile.isActiveColorProfile());
					xmlProfile.putBoolean(ATTR_IS_ABSOLUTE_VALUES, map3ColorProfile.isAbsoluteValues());
					xmlProfile.putBoolean(ATTR_IS_OVERWRITE_LEGEND_VALUES, map3ColorProfile.isOverwriteLegendValues());

					saveColors_20_Brightness(xmlProfile, map3ColorProfile);
					saveColors_30_Vertices(xmlProfile, map3ColorProfile);
				}
			}
		}
	}

	/**
	 * Brightness
	 */
	private static void saveColors_20_Brightness(final IMemento xmlColor, final Map3ColorProfile colorProfile) {

		final IMemento xmlBrightness = xmlColor.createChild(GraphColorManager.MEMENTO_CHILD_BRIGHTNESS);

		xmlBrightness.putInteger(GraphColorManager.TAG_BRIGHTNESS_MIN, colorProfile.getMinBrightness());
		xmlBrightness.putInteger(GraphColorManager.TAG_BRIGHTNESS_MIN_FACTOR, colorProfile.getMinBrightnessFactor());
		xmlBrightness.putInteger(GraphColorManager.TAG_BRIGHTNESS_MAX, colorProfile.getMaxBrightness());
		xmlBrightness.putInteger(GraphColorManager.TAG_BRIGHTNESS_MAX_FACTOR, colorProfile.getMaxBrightnessFactor());
	}

	private static void saveColors_30_Vertices(final IMemento xmlColor, final Map3ColorProfile colorProfile) {

		final IMemento xmlVertices = xmlColor.createChild(TAG_VERTICES);

		for (final RGBVertex vertex : colorProfile.getProfileImage().getRgbVertices()) {

			final IMemento xmlVertex = xmlVertices.createChild(TAG_VERTEX);

			final RGB rgb = vertex.getRGB();

			xmlVertex.putInteger(ATTR_VALUE, vertex.getValue());

			xmlVertex.putInteger(ATTR_RED, rgb.red);
			xmlVertex.putInteger(ATTR_GREEN, rgb.green);
			xmlVertex.putInteger(ATTR_BLUE, rgb.blue);

			xmlVertex.putFloat(ATTR_OPACITY, vertex.getOpacity());
		}
	}

	/**
	 * Set one color provider as active color provider and reset all others.
	 * 
	 * @param activeColorProvider
	 */
	public static void setActiveColorProvider(final Map3GradientColorProvider activeColorProvider) {

		final ArrayList<Map3GradientColorProvider> colorProviders = getColorProviders(activeColorProvider.getGraphId());

		for (final Map3GradientColorProvider colorProvider : colorProviders) {

			final boolean isActiveColorProfile = colorProvider == activeColorProvider;

			final Map3ColorProfile map3ColorProfile = colorProvider.getMap3ColorProfile();

			map3ColorProfile.setIsActiveColorProfile(isActiveColorProfile);
		}
	}
}
