/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.common.swimming;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;

public class SwimStrokeManager {

	private static final String	SWIMMING_COLORS_FILE_NAME	= "swimming-colors.xml";							//$NON-NLS-1$
	private static final int		XML_VERSION						= 1;

	private static final String	TAG_ROOT							= "SwimmingStrokes";									//$NON-NLS-1$
	private static final String	TAG_SWIM_STROKE				= "SwimStroke";										//$NON-NLS-1$
	private static final String	TAG_BACKGROUND_COLOR			= "BackgroundColor";									//$NON-NLS-1$

	private static final String	ATTR_XML_VERSION				= "xmlVersion";										//$NON-NLS-1$
	private static final String	ATTR_SWIM_STROKE_NAME		= "name";												//$NON-NLS-1$

	private static final Bundle	_bundle							= CommonActivator.getDefault().getBundle();
	private static final IPath		_stateLocation					= Platform.getStateLocation(_bundle);

	static {}

	private static final HashMap<SwimStroke, String>	_swimStroke_Label	= new HashMap<>();
	private static final HashMap<SwimStroke, RGB>		_swimStroke_Rgb	= new HashMap<>();

	private static final String[]								_allSwimStrokeText;
	private static final StrokeStyle							_strokeStyle_Invalid;

// SET_FORMATTING_OFF


	public static StrokeStyle[]	DEFAULT_STROKE_STYLES = {

			new StrokeStyle(SwimStroke.FREESTYLE, 		Messages.Swim_Stroke_Freestyle, 			new RGB(0x0,  0xff, 0xff)),
			new StrokeStyle(SwimStroke.BREASTSTROKE, 	Messages.Swim_Stroke_Breaststroke, 		new RGB(0xff, 0xff, 0x0)),
			new StrokeStyle(SwimStroke.BACKSTROKE, 	Messages.Swim_Stroke_Backstroke, 		new RGB(0x0,  0xff, 0x0)),
			new StrokeStyle(SwimStroke.BUTTERFLY, 		Messages.Swim_Stroke_Butterfly, 			new RGB(0xff, 0x0,  0x0)),

			// This race is either swum by one swimmer as individual medley (IM) or by four swimmers as a medley relay.
			new StrokeStyle(SwimStroke.IM, 				Messages.Swim_Stroke_IndividualMedley, new RGB(0xff, 0x0 , 0xff)),

			new StrokeStyle(SwimStroke.MIXED, 			Messages.Swim_Stroke_Mixed, 				new RGB(0x99, 0x0,  0xcc)),
			new StrokeStyle(SwimStroke.DRILL, 			Messages.Swim_Stroke_Drill, 				new RGB(0xb3, 0xb3, 0xb3)),
	};

// SET_FORMATTING_ON

	static {

		_allSwimStrokeText = new String[DEFAULT_STROKE_STYLES.length];

		_strokeStyle_Invalid = new StrokeStyle(SwimStroke.IM, Messages.Swim_Stroke_Invalid, new RGB(0x0, 0x0, 0x0));

		setupDefaultValues();

		// replace rgb values from loaded values
		readSwimStyles();
	}

	public static String[] getAllStrokeText() {
		return _allSwimStrokeText;
	}

	/**
	 * @return Returns the graph background color of the stroke.
	 */
	public static RGB getColor(final SwimStroke stroke) {

		return _swimStroke_Rgb.getOrDefault(stroke, _strokeStyle_Invalid.getGraphBgColor());
	}

	/**
	 * Retrieves the String Representation of the Value
	 *
	 * @return The string representation of the value
	 */
	public static String getLabel(final SwimStroke stroke) {

		return _swimStroke_Label.getOrDefault(stroke, _strokeStyle_Invalid.swimStrokeLabel);
	}

	public static HashMap<SwimStroke, String> getSwimStroke_Label() {
		return _swimStroke_Label;
	}

	public static HashMap<SwimStroke, RGB> getSwimStroke_RGB() {
		return _swimStroke_Rgb;
	}

	private static File getXmlFile() {

		return _stateLocation.append(SWIMMING_COLORS_FILE_NAME).toFile();
	}

	/**
	 * Read swim style xml file.
	 *
	 * @return
	 */
	private static void readSwimStyles() {

		final HashMap<SwimStroke, StrokeStyle> loadedStrokeStyles = new HashMap<>();

		final File xmlFile = getXmlFile();

		if (xmlFile.exists()) {

			try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

				final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);
				for (final IMemento mementoChild : xmlRoot.getChildren()) {

					final XMLMemento xmlSwimStyle = (XMLMemento) mementoChild;
					if (TAG_SWIM_STROKE.equals(xmlSwimStyle.getType())) {

						final String swimStrokeName = Util.getXmlString(xmlSwimStyle, ATTR_SWIM_STROKE_NAME, null);
						final SwimStroke swimStrokeEnum = (SwimStroke) Util.getEnumValue(swimStrokeName, SwimStroke.INVALID);
						final RGB swimStrokeRGB = Util.getXmlRgb(xmlSwimStyle, TAG_BACKGROUND_COLOR, _strokeStyle_Invalid.getGraphBgColor());

						if (SwimStroke.INVALID.equals(swimStrokeEnum) == false && swimStrokeRGB != null) {

							final StrokeStyle strokeStyle = new StrokeStyle();

							final String swimStrokeLabel = _swimStroke_Label.get(swimStrokeEnum);

							strokeStyle.swimStroke = swimStrokeEnum;
							strokeStyle.swimStrokeLabel = swimStrokeLabel;
							strokeStyle.setGraphBgColor(swimStrokeRGB);

							loadedStrokeStyles.put(swimStrokeEnum, strokeStyle);
						}
					}
				}

			} catch (final Exception e) {
				StatusUtil.log(e);
			}
		}

		/*
		 * Update stroke styles from loaded styles
		 */
		for (final StrokeStyle defaultStrokeStyle : DEFAULT_STROKE_STYLES) {

			final StrokeStyle loadedStrokeStyle = loadedStrokeStyles.get(defaultStrokeStyle.swimStroke);

			if (loadedStrokeStyle != null) {

				// overwrite defaults
				_swimStroke_Rgb.put(loadedStrokeStyle.swimStroke, loadedStrokeStyle.getGraphBgColor());
			}
		}
	}

	public static void restoreDefaults() {

		setupDefaultValues();
	}

	public static void saveState() {

		final XMLMemento xmlRoot = writeSwimStyles();
		final File xmlFile = getXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	/**
	 * Set color for a swim stroke.
	 *
	 * @param swimStroke
	 * @param rgb
	 */
	public static void setRgb(final SwimStroke swimStroke, final RGB rgb) {

		_swimStroke_Rgb.put(swimStroke, rgb);
	}

	/**
	 * Fill collections with default values
	 */
	private static void setupDefaultValues() {

		_swimStroke_Rgb.clear();
		for (final StrokeStyle strokeStyle : DEFAULT_STROKE_STYLES) {
			_swimStroke_Rgb.put(strokeStyle.swimStroke, strokeStyle.getGraphBgColor());
		}
		_swimStroke_Label.clear();
		for (final StrokeStyle strokeStyle : DEFAULT_STROKE_STYLES) {
			_swimStroke_Label.put(strokeStyle.swimStroke, strokeStyle.swimStrokeLabel);
		}

		// create texts (for user selection) without the invalid item
		for (int itemIndex = 0; itemIndex < DEFAULT_STROKE_STYLES.length; itemIndex++) {
			_allSwimStrokeText[itemIndex] = DEFAULT_STROKE_STYLES[itemIndex].swimStrokeLabel;
		}

		// add the invalid item which is not displayed in the combobox
		_swimStroke_Rgb.put(SwimStroke.INVALID, _strokeStyle_Invalid.getGraphBgColor());
		_swimStroke_Label.put(SwimStroke.INVALID, _strokeStyle_Invalid.swimStrokeLabel);
	}

	private static XMLMemento writeSwimStyle_10_Root() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// layer structure version
		xmlRoot.putInteger(ATTR_XML_VERSION, XML_VERSION);

		return xmlRoot;
	}

	/**
	 * @return
	 */
	private static XMLMemento writeSwimStyles() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = writeSwimStyle_10_Root();

			// loop: stroke style
			for (final StrokeStyle strokeStyle : DEFAULT_STROKE_STYLES) {

				final IMemento xmlSwimStroke = xmlRoot.createChild(TAG_SWIM_STROKE);

				xmlSwimStroke.putString(ATTR_SWIM_STROKE_NAME, strokeStyle.swimStroke.name());

				Util.setXmlRgb(xmlSwimStroke, TAG_BACKGROUND_COLOR, _swimStroke_Rgb.get(strokeStyle.swimStroke));
			}

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

}
