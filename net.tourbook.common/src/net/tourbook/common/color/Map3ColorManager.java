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
 * Manage colors which are displayed in a 3D map.
 */
public class Map3ColorManager {

	private static final int								FILE_VERSION					= 1;

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapGraphId, Map3ColorDefinition>	_map3ColorDefinitions			= new HashMap<MapGraphId, Map3ColorDefinition>();
	private static ArrayList<Map3ColorDefinition>			_sortedColorDefinitions;

	private static HashMap<MapGraphId, Map3ColorProfile>	DEFAULT_PROFILES				= new HashMap<MapGraphId, Map3ColorProfile>();

	private static final Map3ColorProfile					DEFAULT_PROFILE_ALTITUDE;
	private static final Map3ColorProfile					DEFAULT_PROFILE_GRADIENT;
	private static final Map3ColorProfile					DEFAULT_PROFILE_PACE;
	private static final Map3ColorProfile					DEFAULT_PROFILE_PULSE;
	private static final Map3ColorProfile					DEFAULT_PROFILE_SPEED;

	private static final String								ERROR_GRAPH_ID_IS_NOT_AVAILABLE	= "Requested graph id '%s' is not available.";		//$NON-NLS-1$

	private static final String								MAP3_COLOR_FILE					= "map3colors.xml";								//$NON-NLS-1$

	private static final String								TAG_ROOT						= "map3colors";									//$NON-NLS-1$

	private static final String								TAG_COLOR_DEFINITION			= "colorDefinition";								//$NON-NLS-1$
	private static final String								TAG_COLOR_PROFILE				= "colorProfile";									//$NON-NLS-1$
	private static final String								TAG_VERTICES					= "vertices";										//$NON-NLS-1$
	private static final String								TAG_VERTEX						= "vertex";										//$NON-NLS-1$

	// common attributes
	private static final String								ATTR_VERSION					= "version";										//$NON-NLS-1$
	private static final String								ATTR_GRAPH_ID					= "graphId";										//$NON-NLS-1$
	private static final String								ATTR_NAME						= "name";											//$NON-NLS-1$
	private static final String								ATTR_VALUE						= "value";											//$NON-NLS-1$
	private static final String								ATTR_RED						= "red";											//$NON-NLS-1$
	private static final String								ATTR_GREEN						= "green";											//$NON-NLS-1$
	private static final String								ATTR_BLUE						= "blue";											//$NON-NLS-1$

	private static final RGB								DEFAULT_RGB						= new RGB(0xFF, 0x8B, 0x8B);

	/**
	 * Define 3D map default colors.
	 */
	static {

		/*
		 * Define all color profiles.
		 */

//		<valuecolor blue="0" green="64" red="128" value="10.0"/>
//		<valuecolor blue="0" green="255" red="255" value="50.0"/>
//		<valuecolor blue="0" green="0" red="255" value="100.0"/>
//		<valuecolor blue="255" green="128" red="0" value="150.0"/>
//		<valuecolor blue="255" green="128" red="0" value="190.0"/>

		DEFAULT_PROFILE_ALTITUDE = new Map3ColorProfile(MapGraphId.Altitude,//
				//
				new RGBVertex[] {
			new RGBVertex(0, 128, 64, 0),
			new RGBVertex(200, 255, 255, 0),
			new RGBVertex(500, 255, 0, 0),
			new RGBVertex(600, 0, 128, 255),
			new RGBVertex(1000, 0, 128, 255),
				//
				},
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				MapColorProfile.BRIGHTNESS_LIGHTNING,
				10);

//		<valuecolor blue="0" green="0" red="0" value="10.0"/>
//		<valuecolor blue="0" green="0" red="0" value="50.0"/>
//		<valuecolor blue="128" green="0" red="128" value="100.0"/>
//		<valuecolor blue="223" green="0" red="223" value="150.0"/>
//		<valuecolor blue="255" green="255" red="255" value="190.0"/>

		DEFAULT_PROFILE_GRADIENT = new Map3ColorProfile(MapGraphId.Gradient, //
				//
				new RGBVertex[] {
			new RGBVertex(-10, 0, 0, 0),
			new RGBVertex(0, 128, 0, 128),
			new RGBVertex(5, 223, 0, 223),
			new RGBVertex(10, 255, 255, 255), },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10);

//		<valuecolor blue="0" green="0" red="255" value="10.0"/>
//		<valuecolor blue="0" green="255" red="255" value="50.0"/>
//		<valuecolor blue="0" green="169" red="0" value="100.0"/>
//		<valuecolor blue="255" green="255" red="0" value="150.0"/>
//		<valuecolor blue="255" green="0" red="0" value="190.0"/>

		DEFAULT_PROFILE_PACE = new Map3ColorProfile(MapGraphId.Pace, //
				//
				new RGBVertex[] {
			new RGBVertex(0, 255, 0, 0),
			new RGBVertex(5, 0, 169, 0),
			new RGBVertex(10, 0, 255, 255) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10);

//		<valuecolor blue="0" green="203" red="0" value="10.0"/>
//		<valuecolor blue="0" green="255" red="57" value="50.0"/>
//		<valuecolor blue="0" green="0" red="255" value="100.0"/>
//		<valuecolor blue="0" green="0" red="204" value="150.0"/>
//		<valuecolor blue="255" green="0" red="0" value="190.0"/>

		DEFAULT_PROFILE_PULSE = new Map3ColorProfile(MapGraphId.Pulse, //
				//
				new RGBVertex[] {
			new RGBVertex(60, 0, 203, 0),
			new RGBVertex(100, 57, 255, 0),
			new RGBVertex(140, 255, 0, 0),
			new RGBVertex(180, 204, 0, 247) },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10);

//		<valuecolor blue="255" green="255" red="255" value="10.0"/>
//		<valuecolor blue="0" green="255" red="198" value="50.0"/>
//		<valuecolor blue="0" green="255" red="198" value="100.0"/>
//		<valuecolor blue="255" green="128" red="0" value="150.0"/>
//		<valuecolor blue="255" green="128" red="0" value="190.0"/>

		DEFAULT_PROFILE_SPEED = new Map3ColorProfile(MapGraphId.Speed, //
				//
				new RGBVertex[] {
			new RGBVertex(0, 198, 255, 0),
			new RGBVertex(25, 0, 255, 198),
			new RGBVertex(50, 255, 128, 0),
			new RGBVertex(100, 0, 128, 255), },
				//
				MapColorProfile.BRIGHTNESS_DIMMING,
				10,
				MapColorProfile.BRIGHTNESS_DIMMING,
				10);

		DEFAULT_PROFILES.put(MapGraphId.Altitude, DEFAULT_PROFILE_ALTITUDE);
		DEFAULT_PROFILES.put(MapGraphId.Gradient, DEFAULT_PROFILE_GRADIENT);
		DEFAULT_PROFILES.put(MapGraphId.Pace, DEFAULT_PROFILE_PACE);
		DEFAULT_PROFILES.put(MapGraphId.Pulse, DEFAULT_PROFILE_PULSE);
		DEFAULT_PROFILES.put(MapGraphId.Speed, DEFAULT_PROFILE_SPEED);

		/*
		 * Setup all color definitions with defaults.
		 */
		_map3ColorDefinitions.put(MapGraphId.Altitude, new Map3ColorDefinition(//
				MapGraphId.Altitude,
				Messages.Graph_Label_Altitude,
				DEFAULT_PROFILE_ALTITUDE.clone()));

		_map3ColorDefinitions.put(MapGraphId.Pulse, new Map3ColorDefinition(//
				MapGraphId.Pulse,
				Messages.Graph_Label_Heartbeat,
				DEFAULT_PROFILE_PULSE.clone()));

		_map3ColorDefinitions.put(MapGraphId.Speed, new Map3ColorDefinition(//
				MapGraphId.Speed,
				Messages.Graph_Label_Speed,
				DEFAULT_PROFILE_SPEED.clone()));

		_map3ColorDefinitions.put(MapGraphId.Pace, new Map3ColorDefinition(//
				MapGraphId.Pace,
				Messages.Graph_Label_Pace,
				DEFAULT_PROFILE_PACE.clone()));

		_map3ColorDefinitions.put(MapGraphId.Gradient, new Map3ColorDefinition(
				MapGraphId.Gradient,
				Messages.Graph_Label_Gradient,
				DEFAULT_PROFILE_GRADIENT.clone()));

		_initColorDefinitions();
	}

	private Map3ColorManager() {}

	private static void _initColorDefinitions() {

		// overwrite default colors with saved colors
		readColors();

		// sort by name
		_sortedColorDefinitions = new ArrayList<Map3ColorDefinition>(_map3ColorDefinitions.values());
		Collections.sort(_sortedColorDefinitions);
	}

	/**
	 * Disposes all profile images.
	 */
	public static void disposeProfileImages() {

		for (final Map3ColorDefinition colorDef : _map3ColorDefinitions.values()) {
			for (final Map3ColorProfile colorProfile : colorDef.getColorProfiles()) {
				UI.disposeResource(colorProfile.getProfileImage().getImage());
			}
		}
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
	public static ArrayList<Map3ColorProfile> getColorProfiles(final MapGraphId graphId) {

		return getColorDefinition(graphId).getColorProfiles();
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

		return DEFAULT_PROFILE_ALTITUDE.clone();
	}

	public static ArrayList<Map3ColorDefinition> getSortedColorDefinitions() {

		return _sortedColorDefinitions;
	}

	private static void readColors() {

		final ArrayList<Map3ColorDefinition> xmlColorDefinitions = readColors_0_FromXml();

		if (xmlColorDefinitions == null) {
			return;
		}

		for (final Map3ColorDefinition xmlColorDef : xmlColorDefinitions) {

			final MapGraphId xmlGraphId = xmlColorDef.getGraphId();

			final Map3ColorDefinition defaultColorDef = getColorDefinition(xmlGraphId);

			final ArrayList<Map3ColorProfile> xmlProfiles = xmlColorDef.getColorProfiles();
			if (xmlProfiles.size() > 0) {
				// replace existing profiles with loaded profiles
				defaultColorDef.setColorProfiles(xmlProfiles);
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

		ArrayList<Map3ColorDefinition> xmlColorDefinitions = null;

		try {
			reader = new InputStreamReader(new FileInputStream(file), "UTF-8"); //$NON-NLS-1$

			final XMLMemento mementoRoot = XMLMemento.createReadRoot(reader);

			xmlColorDefinitions = readColors_10(mementoRoot);

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

		return xmlColorDefinitions;
	}

	private static ArrayList<Map3ColorDefinition> readColors_10(final XMLMemento xmlRoot) {

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

				final Map3ColorProfile colorProfile = new Map3ColorProfile(graphId);
				colorDef.addProfile(colorProfile);

				colorProfile.setProfileName(Util.getXmlString(xmlProfile, ATTR_NAME, UI.EMPTY_STRING));

				readColors_20_Brightness(xmlProfile, colorProfile);
				readColors_22_MinMaxValue(xmlProfile, colorProfile);
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

	private static void readColors_22_MinMaxValue(final IMemento xmlProfile, final Map3ColorProfile colorProfile) {

		final IMemento xmlMinMaxValue = xmlProfile.getChild(GraphColorManager.MEMENTO_CHILD_MIN_MAX_VALUE);

		if (xmlMinMaxValue == null) {
			return;
		}

		colorProfile.setIsMinValueOverwrite(Util.getXmlBoolean(
				xmlMinMaxValue,
				GraphColorManager.TAG_IS_MIN_VALUE_OVERWRITE,
				false));

		colorProfile.setIsMaxValueOverwrite(Util.getXmlBoolean(
				xmlMinMaxValue,
				GraphColorManager.TAG_IS_MAX_VALUE_OVERWRITE,
				false));

		colorProfile.setMinValueOverwrite(Util.getXmlInteger(
				xmlMinMaxValue,
				GraphColorManager.TAG_MIN_VALUE_OVERWRITE,
				0));

		colorProfile.setMaxValueOverwrite(Util.getXmlInteger(
				xmlMinMaxValue,
				GraphColorManager.TAG_MAX_VALUE_OVERWRITE,
				0));
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
					Util.getXmlRgb(xmlVertex, DEFAULT_RGB)));
		}

		colorProfile.getProfileImage().setVertices(vertices);
	}

	/**
	 * Replace a profile with another profile.
	 * 
	 * @param originalProfile
	 * @param modifiedProfile
	 */
	public static void replaceColorProfile(	final Map3ColorProfile originalProfile,
											final Map3ColorProfile modifiedProfile) {

		final MapGraphId originalGraphId = originalProfile.getGraphId();
		final MapGraphId modifiedGraphId = modifiedProfile.getGraphId();

		// remove old profile
		final ArrayList<Map3ColorProfile> originalProfiles = getColorProfiles(originalGraphId);

		// add new/modified profile
		if (originalGraphId == modifiedGraphId) {

			// graph id has not changed, replace original with modified profile

			originalProfiles.remove(originalProfile);
			originalProfiles.add(modifiedProfile);

		} else {

			// graph id has changed, remove original and set new profile

			if (originalProfiles.size() < 2) {
				StatusUtil.log(new Throwable(
						"Color profile cannot be removed because the color definition contains only 1 color profile."));//$NON-NLS-1$
				return;
			}

			originalProfiles.remove(originalProfile);

			getColorProfiles(modifiedGraphId).add(modifiedProfile);
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

			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")); //$NON-NLS-1$

			final XMLMemento xmlRoot = saveColors_01_getXMLRoot();

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

	private static XMLMemento saveColors_01_getXMLRoot() {

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

			for (final Map3ColorProfile colorProfile : colorDef.getColorProfiles()) {

				final IMemento xmlProfile = xmlColor.createChild(TAG_COLOR_PROFILE);

				xmlProfile.putString(ATTR_NAME, colorProfile.getProfileName());

				saveColors_20_Brightness(xmlProfile, colorProfile);
				saveColors_22_MinMaxValue(xmlProfile, colorProfile);

				saveColors_50_Vertices(xmlProfile, colorProfile);
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

	/**
	 * Min/max value
	 */
	private static void saveColors_22_MinMaxValue(final IMemento xmlColor, final Map3ColorProfile colorProfile) {

		final IMemento xmlMinMaxValue = xmlColor.createChild(GraphColorManager.MEMENTO_CHILD_MIN_MAX_VALUE);

		xmlMinMaxValue.putInteger(GraphColorManager.TAG_IS_MIN_VALUE_OVERWRITE, colorProfile.isMinValueOverwrite()
				? 1
				: 0);

		xmlMinMaxValue.putInteger(//
				GraphColorManager.TAG_IS_MAX_VALUE_OVERWRITE,
				colorProfile.isMaxValueOverwrite() ? 1 : 0);

		xmlMinMaxValue.putInteger(GraphColorManager.TAG_MIN_VALUE_OVERWRITE, colorProfile.getMinValueOverwrite());
		xmlMinMaxValue.putInteger(GraphColorManager.TAG_MAX_VALUE_OVERWRITE, colorProfile.getMaxValueOverwrite());
	}

	private static void saveColors_50_Vertices(final IMemento xmlColor, final Map3ColorProfile colorProfile) {

		final IMemento xmlVertices = xmlColor.createChild(TAG_VERTICES);

		for (final RGBVertex vertex : colorProfile.getProfileImage().getRgbVertices()) {

			final IMemento xmlVertex = xmlVertices.createChild(TAG_VERTEX);

			final RGB rgb = vertex.getRGB();

			xmlVertex.putInteger(ATTR_VALUE, vertex.getValue());
			xmlVertex.putInteger(ATTR_RED, rgb.red);
			xmlVertex.putInteger(ATTR_GREEN, rgb.green);
			xmlVertex.putInteger(ATTR_BLUE, rgb.blue);
		}
	}
}
