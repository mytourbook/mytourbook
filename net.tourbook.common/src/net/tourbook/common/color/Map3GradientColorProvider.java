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

import java.util.List;

import net.tourbook.common.util.StatusUtil;

import org.eclipse.swt.graphics.RGB;

/**
 * Provides colors to draw a tour or legend.
 * <p>
 * A color provider do not contain data values only min/max values needs to be set in the
 * configuration.
 */
public class Map3GradientColorProvider extends MapGradientColorProvider implements IGradientColors {

	private MapGraphId			_graphId;

	private Map3ColorProfile	_colorProfile;

	private MapUnits			_mapUnits	= new MapUnits();

	public Map3GradientColorProvider(final Map3ColorProfile map3ColorProfile) {

		_graphId = map3ColorProfile.getGraphId();
		_colorProfile = map3ColorProfile;
	}

	public Map3GradientColorProvider(final MapGraphId graphId) {

		_graphId = graphId;
		_colorProfile = new Map3ColorProfile(graphId);
	}

	@Override
	public void configureColorProvider(	final int imageHeight,
										float minValue,
										float maxValue,
										final String unitText,
										final LegendUnitFormat unitFormat) {

		// overwrite min value
		if (_colorProfile.isMinValueOverwrite()) {

			minValue = _colorProfile.getMinValueOverwrite();

			if (unitFormat == LegendUnitFormat.Pace) {

				// adjust value from minutes->seconds
				minValue *= 60;
			}
		}

		// overwrite max value
		if (_colorProfile.isMaxValueOverwrite()) {

			maxValue = _colorProfile.getMaxValueOverwrite();

			if (unitFormat == LegendUnitFormat.Pace) {

				// adjust value from minutes->seconds
				maxValue *= 60;
			}
		}

		// ensure max is larger than min
		if (maxValue <= minValue) {
			maxValue = minValue + 1;
		}

		final List<Float> legendUnits = getLegendUnits(imageHeight, minValue, maxValue, unitFormat);

		if (legendUnits.size() > 0) {

			final Float legendMinValue = legendUnits.get(0);
			final Float legendMaxValue = legendUnits.get(legendUnits.size() - 1);

			_mapUnits.units = legendUnits;
			_mapUnits.unitText = unitText;
			_mapUnits.legendMinValue = legendMinValue;
			_mapUnits.legendMaxValue = legendMaxValue;
		}
	}

	@Override
	public int getColorValue(final float graphValue) {

		final RGBVertex[] rgbVerticies = _colorProfile.getProfileImage().getRgbVerticesArray();

		if (rgbVerticies.length == 0) {
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

		for (final RGBVertex rgbVertexFromArray : rgbVerticies) {

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

			rgbVertex = rgbVerticies[0];

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

			rgbVertex = rgbVerticies[rgbVerticies.length - 1];

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

	@Override
	public MapColorProfile getMapColorProfile() {
		return _colorProfile;
	}

	public MapUnits getMapUnits() {
		return _mapUnits;
	}

	@Override
	public void setColorProfile(final MapColorProfile colorProfile) {

		if (colorProfile instanceof Map3ColorProfile) {

			_colorProfile = (Map3ColorProfile) colorProfile;

		} else {

			StatusUtil.log(new Throwable(String.format(
					"Color profile '%s' is not of type '%s'",
					colorProfile,
					Map3ColorProfile.class.getSimpleName())));
		}
	}

}
