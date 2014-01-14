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
import java.util.Arrays;
import java.util.List;

import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.RGB;

/**
 * Provides colors to draw a tour or legend.
 * <p>
 * A color provider do not contain data values only min/max values needs to be set in the
 * configuration.
 */
public class Map3GradientColorProvider extends MapGradientColorProvider implements IGradientColorProvider, Cloneable {

	private MapGraphId			_graphId;

	private Map3ColorProfile	_colorProfile;

	private MapUnits			_mapUnits	= new MapUnits();

	private RGBVertex[]			_absoluteVertices;

	public Map3GradientColorProvider(final MapGraphId graphId, final Map3ColorProfile colorProfile) {

		_graphId = graphId;
		_colorProfile = colorProfile;
	}

	@Override
	public Map3GradientColorProvider clone() {

		Map3GradientColorProvider clonedObject = null;

		try {

			clonedObject = (Map3GradientColorProvider) super.clone();

			clonedObject._colorProfile = _colorProfile.clone();
			clonedObject._mapUnits = _mapUnits.clone();

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return clonedObject;
	}

	private RGBVertex[] cloneVertices(final RGBVertex[] relativeVertices) {

		final RGBVertex[] absoluteVertices = new RGBVertex[relativeVertices.length];

		for (int vertexIndex = 0; vertexIndex < relativeVertices.length; vertexIndex++) {
			absoluteVertices[vertexIndex] = relativeVertices[vertexIndex].clone();
		}

		return absoluteVertices;
	}

	public void configureColorProvider(	final int legendSize,
										final ArrayList<RGBVertex> rgbVertices,
										final boolean isDrawUnits) {

		String unitText = UI.EMPTY_STRING;

		if (isDrawUnits) {

			switch (_graphId) {
			case Altitude:
				unitText = UI.UNIT_LABEL_ALTITUDE;
				break;

			case Gradient:
				unitText = Messages.Graph_Label_Gradient_Unit;
				break;

			case Pace:
				unitText = UI.UNIT_LABEL_PACE;
				break;

			case Pulse:
				unitText = Messages.Graph_Label_Heartbeat_Unit;
				break;

			case Speed:
				unitText = UI.UNIT_LABEL_SPEED;
				break;

			default:
				break;
			}
		}

		/*
		 * Get min/max values from the values which are displayed
		 */
		float minValue = 0;
		float maxValue = 0;

		for (int vertexIndex = 0; vertexIndex < rgbVertices.size(); vertexIndex++) {

			final long value = rgbVertices.get(vertexIndex).getValue();

			if (vertexIndex == 0) {

				// initialize min/max values

				minValue = maxValue = value;

			} else {

				if (value < minValue) {
					minValue = value;
				} else if (value > maxValue) {
					maxValue = value;
				}
			}
		}

//		_absoluteVertices = rgbVertices.toArray(new RGBVertex[rgbVertices.size()]);

		configureColorProvider(//
				legendSize,
				minValue,
				maxValue,
				unitText,
				LegendUnitFormat.Number,
				true);

	}

	@Override
	public void configureColorProvider(	final int legendSize,
										final float minValue,
										float maxValue,
										final String unitText,
										final LegendUnitFormat unitFormat,
										final boolean isConvertIntoAbsoluteValues) {

//		// overwrite min value
//		if (_colorProfile.isMinValueOverwrite()) {
//
//			minValue = _colorProfile.getMinValueOverwrite();
//
//			if (unitFormat == LegendUnitFormat.Pace) {
//
//				// adjust value from minutes->seconds
//				minValue *= 60;
//			}
//		}
//
//		// overwrite max value
//		if (_colorProfile.isMaxValueOverwrite()) {
//
//			maxValue = _colorProfile.getMaxValueOverwrite();
//
//			if (unitFormat == LegendUnitFormat.Pace) {
//
//				// adjust value from minutes->seconds
//				maxValue *= 60;
//			}
//		}

		// ensure max is larger than min
		if (maxValue <= minValue) {
			maxValue = minValue + 1;
		}

		final List<Float> legendUnits = getLegendUnits(legendSize, minValue, maxValue, unitFormat);
		Assert.isTrue(legendUnits.size() > 0);

		final Float legendMinValue = legendUnits.get(0);
		final Float legendMaxValue = legendUnits.get(legendUnits.size() - 1);

		_mapUnits.units = legendUnits;
		_mapUnits.unitText = unitText;
		_mapUnits.legendMinValue = legendMinValue;
		_mapUnits.legendMaxValue = legendMaxValue;

		if (isConvertIntoAbsoluteValues) {
			configureColorProvider_SetVertices(minValue, maxValue);
		}
	}

	/**
	 * Convert relative vertices into absolute values.
	 * <p>
	 * minVertex ^ minValue<br>
	 * maxVertex ^ maxValue
	 * 
	 * @param minValue
	 *            Absolute minimum value.
	 * @param maxValue
	 *            Absolute maximum value.
	 */
	private void configureColorProvider_SetVertices(final float minValue, final float maxValue) {

		final RGBVertex[] relativeVertices = _colorProfile.getProfileImage().getRgbVerticesArray();
		final RGBVertex[] absoluteVertices = cloneVertices(relativeVertices);

		final RGBVertex minRelativeVertex = relativeVertices[0];
		final RGBVertex maxRelativeVertex = relativeVertices[relativeVertices.length - 1];

		final int relativeMinValue = minRelativeVertex.getValue();

		final int relativeDiff = maxRelativeVertex.getValue() - relativeMinValue;
		final int absoluteDiff = (int) (maxValue - minValue);

		final float diffRatio = (float) relativeDiff / (float) absoluteDiff;

		final int absoluteMinValue = (int) minValue;

		for (int vertexIndex = 0; vertexIndex < relativeVertices.length; vertexIndex++) {

			int absoluteValue;

			if (vertexIndex == 0) {

				// first value

				absoluteValue = absoluteMinValue;

			} else if (vertexIndex == relativeVertices.length - 1) {

				// last value

				absoluteValue = (int) maxValue;

			} else {

				// in between value

				final RGBVertex relativeVertex = relativeVertices[vertexIndex];

				final int relativeValue = relativeVertex.getValue();

				final int relative0Value = relativeValue - relativeMinValue;

				final int absolutInBetweenValue = (int) (relative0Value / diffRatio);

				absoluteValue = absoluteMinValue + absolutInBetweenValue;
			}

			absoluteVertices[vertexIndex].setValue(absoluteValue);
		}

		_absoluteVertices = absoluteVertices;

//		dumpVertices();
	}

	void dumpVertices() {

		final int maxLen = 20;

		final String dump = String.format(//
				"Map3GradientColorProvider [_absoluteVertices=%s]", //$NON-NLS-1$
				_absoluteVertices != null ? Arrays.asList(_absoluteVertices).subList(
						0,
						Math.min(_absoluteVertices.length, maxLen)) : null);

		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t" + dump) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Map3GradientColorProvider)) {
			return false;
		}
		final Map3GradientColorProvider other = (Map3GradientColorProvider) obj;
		if (_colorProfile == null) {
			if (other._colorProfile != null) {
				return false;
			}
		} else if (!_colorProfile.equals(other._colorProfile)) {
			return false;
		}
		return true;
	}

	@Override
	public MapColorProfile getColorProfile() {
		return _colorProfile;
	}

	@Override
	public int getColorValue(final float graphValue) {

		if (_absoluteVertices == null || _absoluteVertices.length == 0) {

			// color provider is not yet initialized, return a valid value
			return 0xff00ff;
		}

		final float minBrightnessFactor = _colorProfile.getMinBrightnessFactor() / 100.0f;
		final float maxBrightnessFactor = _colorProfile.getMaxBrightnessFactor() / 100.0f;

		/*
		 * find the ColorValue for the current value
		 */
		RGBVertex rgbVertex;
		RGBVertex minRgbVertex = null;
		RGBVertex maxRgbVertex = null;

		for (final RGBVertex rgbVertexFromArray : _absoluteVertices) {

			rgbVertex = rgbVertexFromArray;
			final long vertexValue = rgbVertex.getValue();

			if (graphValue >= vertexValue) {
				minRgbVertex = rgbVertex;
			}

			if (graphValue <= vertexValue) {
				maxRgbVertex = rgbVertex;
			}

			if (minRgbVertex != null && maxRgbVertex != null) {
				break;
			}
		}

		int red;
		int green;
		int blue;

		if (minRgbVertex == null) {

			// legend value is smaller than minimum value

			rgbVertex = _absoluteVertices[0];

			final RGB minRGB = rgbVertex.getRGB();

			red = minRGB.red;
			green = minRGB.green;
			blue = minRGB.blue;

			final float minValue = rgbVertex.getValue();
			final float minDiff = _mapUnits.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (graphValue - minValue) / minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (_colorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_colorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else if (maxRgbVertex == null) {

			// legend value is larger than maximum value

			rgbVertex = _absoluteVertices[_absoluteVertices.length - 1];

			final RGB maxRGB = rgbVertex.getRGB();

			red = maxRGB.red;
			green = maxRGB.green;
			blue = maxRGB.blue;

			final float maxValue = rgbVertex.getValue();
			final float maxDiff = _mapUnits.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (graphValue - maxValue) / maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (_colorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				red = red - (int) (dimmRatio * red);
				green = green - (int) (dimmRatio * green);
				blue = blue - (int) (dimmRatio * blue);

			} else if (_colorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				red = red + (int) (dimmRatio * (255 - red));
				green = green + (int) (dimmRatio * (255 - green));
				blue = blue + (int) (dimmRatio * (255 - blue));
			}

		} else {

			// legend value is in the min/max range

			final float minValue = minRgbVertex.getValue();
			final float maxValue = maxRgbVertex.getValue();

			final RGB minRGB = minRgbVertex.getRGB();

			final float minRed = minRGB.red;
			final float minGreen = minRGB.green;
			final float minBlue = minRGB.blue;

			final RGB maxRGB = maxRgbVertex.getRGB();

			final float redDiff = maxRGB.red - minRed;
			final float greenDiff = maxRGB.green - minGreen;
			final float blueDiff = maxRGB.blue - minBlue;

			final float ratioDiff = maxValue - minValue;
			final float ratio = ratioDiff == 0 ? 1 : (graphValue - minValue) / (ratioDiff);

			red = (int) (minRed + redDiff * ratio);
			green = (int) (minGreen + greenDiff * ratio);
			blue = (int) (minBlue + blueDiff * ratio);
		}

		// adjust color values to 0...255, this is optimized
		final int maxRed = (0 >= red) ? 0 : red;
		final int maxGreen = (0 >= green) ? 0 : green;
		final int maxBlue = (0 >= blue) ? 0 : blue;

		red = (255 <= maxRed) ? 255 : maxRed;
		green = (255 <= maxGreen) ? 255 : maxGreen;
		blue = (255 <= maxBlue) ? 255 : maxBlue;

		final int graphColor = ((red & 0xFF) << 0) | ((green & 0xFF) << 8) | ((blue & 0xFF) << 16);

		return graphColor;
	}

	public MapGraphId getGraphId() {
		return _graphId;
	}

	public Map3ColorProfile getMap3ColorProfile() {
		return _colorProfile;
	}

	public MapUnits getMapUnits() {
		return _mapUnits;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_colorProfile == null) ? 0 : _colorProfile.hashCode());
		return result;
	}

	@Override
	public void setColorProfile(final MapColorProfile colorProfile) {

		if (colorProfile instanceof Map3ColorProfile) {

			_colorProfile = (Map3ColorProfile) colorProfile;

		} else {

			StatusUtil.log(new Throwable(String.format("Color profile '%s' is not of type '%s'", //$NON-NLS-1$
					colorProfile,
					Map3ColorProfile.class.getName())));
		}
	}

	public void setGraphId(final MapGraphId graphId) {

		_graphId = graphId;
	}

	@Override
	public String toString() {
		return String.format("Map3GradientColorProvider [_graphId=%s, _colorProfile=%s]", _graphId, _colorProfile); //$NON-NLS-1$
	}

}
