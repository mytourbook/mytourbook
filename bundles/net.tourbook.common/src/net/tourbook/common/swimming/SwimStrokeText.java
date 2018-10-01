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

import net.tourbook.common.Messages;

public class SwimStrokeText {

	private static final HashMap<SwimStroke, String>	_swimStrokeMap;
	private static final String[]								_allSwimStrokeText;

	private static StrokeStyle[]								_allStrokeStyles	= {

			new StrokeStyle(SwimStroke.FREESTYLE, Messages.Swim_Stroke_Freestyle),
			new StrokeStyle(SwimStroke.BACKSTROKE, Messages.Swim_Stroke_Backstroke),
			new StrokeStyle(SwimStroke.BREASTSTROKE, Messages.Swim_Stroke_Breaststroke),
			new StrokeStyle(SwimStroke.BUTTERFLY, Messages.Swim_Stroke_Butterfly),
			new StrokeStyle(SwimStroke.DRILL, Messages.Swim_Stroke_Drill),
			new StrokeStyle(SwimStroke.MIXED, Messages.Swim_Stroke_Mixed),

			// This race is either swum by one swimmer as individual medley (IM) or by four swimmers as a medley relay.
			new StrokeStyle(SwimStroke.IM, Messages.Swim_Stroke_IndividualMedley),

			// SwimStroke.INVALID is added later
	};

	static {

		/*
		 * Fill collections
		 */

		_swimStrokeMap = new HashMap<>();
		for (final StrokeStyle strokeStyle : _allStrokeStyles) {
			_swimStrokeMap.put(strokeStyle.__swimStroke, strokeStyle.__swimStrokeText);
		}

		_allSwimStrokeText = new String[_allStrokeStyles.length];
		for (int itemIndex = 0; itemIndex < _allStrokeStyles.length; itemIndex++) {
			_allSwimStrokeText[itemIndex] = _allStrokeStyles[itemIndex].__swimStrokeText;
		}

		// add an additional item which is not displayed in the combobox
		_swimStrokeMap.put(SwimStroke.INVALID, Messages.Swim_Stroke_Invalid);
	}

	private static class StrokeStyle {

		private SwimStroke	__swimStroke;
		private String			__swimStrokeText;

		public StrokeStyle(final SwimStroke swimStroke, final String swimStrokeText) {

			__swimStroke = swimStroke;
			__swimStrokeText = swimStrokeText;
		}
	}

	public static String[] getAllStrokeText() {
		return _allSwimStrokeText;
	}

	/**
	 * Retrieves the String Representation of the Value
	 *
	 * @return The string representation of the value
	 */
	public static String getStringFromValue(final SwimStroke stroke) {

		return _swimStrokeMap.getOrDefault(stroke, Messages.Swim_Stroke_Invalid);
	}

}
