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

import java.util.HashMap;

import org.eclipse.swt.graphics.RGB;

import net.tourbook.common.Messages;

public class SwimStrokeConfig {

	private static final HashMap<SwimStroke, StrokeStyle>	_swimStrokeMap;
	private static final String[]									_allSwimStrokeText;

	private static final StrokeStyle								_strokeStyle_Invalid	= new StrokeStyle(SwimStroke.IM,
			Messages.Swim_Stroke_Invalid,
			new RGB(0x0, 0x0, 0x0));

	// SET_FORMATTING_OFF

	private static StrokeStyle[]	_allStrokeStyles	= {

			new StrokeStyle(SwimStroke.FREESTYLE, 		Messages.Swim_Stroke_Freestyle, 				new RGB(0x80, 0x0, 0xff)),
			new StrokeStyle(SwimStroke.BACKSTROKE, 	Messages.Swim_Stroke_Backstroke, 			new RGB(0x80, 0x80, 0xff)),
			new StrokeStyle(SwimStroke.BREASTSTROKE, 	Messages.Swim_Stroke_Breaststroke, 			new RGB(0x0, 0x80, 0xff)),
			new StrokeStyle(SwimStroke.BUTTERFLY, 		Messages.Swim_Stroke_Butterfly, 				new RGB(0x0, 0xff, 0xff)),
			new StrokeStyle(SwimStroke.DRILL, 			Messages.Swim_Stroke_Drill, 					new RGB(0xff, 0xff, 0x0)),
			new StrokeStyle(SwimStroke.MIXED, 			Messages.Swim_Stroke_Mixed, 					new RGB(0x80, 0x0, 0x0)),

			// This race is either swum by one swimmer as individual medley (IM) or by four swimmers as a medley relay.
			new StrokeStyle(SwimStroke.IM, 				Messages.Swim_Stroke_IndividualMedley, 	new RGB(0x0, 0xff, 0x0)),

			// SwimStroke.INVALID is added later
	};

// SET_FORMATTING_ON

	static {

		/*
		 * Fill collections
		 */

		_swimStrokeMap = new HashMap<>();
		for (final StrokeStyle strokeStyle : _allStrokeStyles) {
			_swimStrokeMap.put(strokeStyle.swimStroke, strokeStyle);
		}

		_allSwimStrokeText = new String[_allStrokeStyles.length];
		for (int itemIndex = 0; itemIndex < _allStrokeStyles.length; itemIndex++) {
			_allSwimStrokeText[itemIndex] = _allStrokeStyles[itemIndex].swimStrokeText;
		}

		// add an additional item which is not displayed in the combobox
		_swimStrokeMap.put(SwimStroke.INVALID, _strokeStyle_Invalid);
	}

	public static HashMap<SwimStroke, StrokeStyle> getAllStrokeStyles() {
		return _swimStrokeMap;
	}

	public static String[] getAllStrokeText() {
		return _allSwimStrokeText;
	}

	/**
	 * @return Returns the graph background color of the stroke.
	 */
	public static RGB getColor(final SwimStroke stroke) {

		return _swimStrokeMap.getOrDefault(stroke, _strokeStyle_Invalid).graphBgColor;
	}

	/**
	 * Retrieves the String Representation of the Value
	 *
	 * @return The string representation of the value
	 */
	public static String getLabel(final SwimStroke stroke) {

		return _swimStrokeMap.getOrDefault(stroke, _strokeStyle_Invalid).swimStrokeText;
	}

}
