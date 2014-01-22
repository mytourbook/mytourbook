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

	private MapUnits			_mapUnitsProfile	= new MapUnits();
	private MapUnits			_mapUnitsTour		= new MapUnits();

	private RGBVertex[]			_verticesProfile;
	private RGBVertex[]			_verticesTour;

//	/**
//	 * Reference of the original color provider when this color provider is a clone.
//	 */
//	private Map3GradientColorProvider	_originalColorProvider;

	public Map3GradientColorProvider(final MapGraphId graphId, final Map3ColorProfile colorProfile) {

		_graphId = graphId;
		_colorProfile = colorProfile;
	}

	@Override
	public Map3GradientColorProvider clone() {

		try {

			final Map3GradientColorProvider clonedObject = (Map3GradientColorProvider) super.clone();

			clonedObject._colorProfile = _colorProfile.clone();

			clonedObject._mapUnitsProfile = _mapUnitsProfile.clone();
			clonedObject._mapUnitsTour = _mapUnitsTour.clone();

			return clonedObject;

		} catch (final CloneNotSupportedException e) {
			StatusUtil.log(e);
		}

		return null;
	}

	private RGBVertex[] cloneVertices(final RGBVertex[] relativeVertices) {

		final RGBVertex[] absoluteVertices = new RGBVertex[relativeVertices.length];

		for (int vertexIndex = 0; vertexIndex < relativeVertices.length; vertexIndex++) {
			absoluteVertices[vertexIndex] = relativeVertices[vertexIndex].clone();
		}

		return absoluteVertices;
	}

	public void configureColorProvider(	final ColorProviderConfig config,
										final int legendSize,
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

			final int value = rgbVertices.get(vertexIndex).getValue();

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

		configureColorProvider(//
				config,
				legendSize,
				minValue,
				maxValue,
				unitText,
				LegendUnitFormat.Number);

	}

	@Override
	public void configureColorProvider(	final ColorProviderConfig config,
										final int legendSize,
										final float dataMinValue,
										final float dataMaxValue,
										final String unitText,
										final LegendUnitFormat unitFormat) {

		float dataMaxValueAdjusted = dataMaxValue;

		// ensure max is larger than min
		if (dataMaxValue <= dataMinValue) {
			dataMaxValueAdjusted = dataMinValue + 1;
		}

		float legendMinValue = dataMinValue;
		float legendMaxValue = dataMaxValueAdjusted;

		final RGBVertex[] vertexValues = _colorProfile.getProfileImage().getRgbVerticesArray();

		if (_colorProfile.isAbsoluteValues() && _colorProfile.isOverwriteLegendValues()) {

			final RGBVertex minVertexValue = vertexValues[0];
			final RGBVertex maxVertexValue = vertexValues[vertexValues.length - 1];

			legendMinValue = minVertexValue.getValue();
			legendMaxValue = maxVertexValue.getValue();
		}

		final List<Float> dataUnits = getLegendUnits(legendSize, legendMinValue, legendMaxValue, unitFormat);
		Assert.isTrue(dataUnits.size() > 0);

		final Float adjustedLegendMinValue = dataUnits.get(0);
		final Float adjustedLegendMaxValue = dataUnits.get(dataUnits.size() - 1);

		final MapUnits mapUnits = getMapUnits(config);
		mapUnits.units = dataUnits;
		mapUnits.unitText = unitText;
		mapUnits.legendMinValue = adjustedLegendMinValue;
		mapUnits.legendMaxValue = adjustedLegendMaxValue;

		configureColorProvider_SetVertices(config, legendMinValue, legendMaxValue);
	}

	/**
	 * Convert relative vertices into absolute values.
	 * <p>
	 * minVertex ^ minValue<br>
	 * maxVertex ^ maxValue
	 * 
	 * @param config
	 * @param dataMinValue
	 *            Absolute minimum value.
	 * @param dataMaxValue
	 *            Absolute maximum value.
	 */
	private void configureColorProvider_SetVertices(final ColorProviderConfig config,
													final float dataMinValue,
													final float dataMaxValue) {

		final RGBVertex[] profileVertices = _colorProfile.getProfileImage().getRgbVerticesArray();
		final RGBVertex[] dataVertices = cloneVertices(profileVertices);

		if (_colorProfile.isAbsoluteValues()) {

			setVertices(config, dataVertices);

			return;
		}

		final RGBVertex minRelativeVertex = profileVertices[0];
		final RGBVertex maxRelativeVertex = profileVertices[profileVertices.length - 1];

		final int relativeMinValue = minRelativeVertex.getValue();

		final int relativeDiff = maxRelativeVertex.getValue() - relativeMinValue;
		final int absoluteDiff = (int) (dataMaxValue - dataMinValue);

		final float diffRatio = (float) relativeDiff / (float) absoluteDiff;

		final int absoluteMinValue = (int) dataMinValue;

		for (int vertexIndex = 0; vertexIndex < profileVertices.length; vertexIndex++) {

			int absoluteValue;

			if (vertexIndex == 0) {

				// set first value

				absoluteValue = absoluteMinValue;

			} else if (vertexIndex == profileVertices.length - 1) {

				// set last value

				absoluteValue = (int) dataMaxValue;

			} else {

				// set in between value

				final RGBVertex relativeVertex = profileVertices[vertexIndex];

				final int relativeValue = relativeVertex.getValue();

				final int relative0Value = relativeValue - relativeMinValue;

				final int absolutInBetweenValue = (int) (relative0Value / diffRatio);

				absoluteValue = absoluteMinValue + absolutInBetweenValue;
			}

			dataVertices[vertexIndex].setValue(absoluteValue);
		}

		setVertices(config, dataVertices);

//		dumpVertices();
	}

	void dumpVertices() {

//		final int maxLen = 20;
//
//		final String dump = String.format(//
//				"Map3GradientColorProvider [_absoluteVertices=%s]", //$NON-NLS-1$
//				_absoluteVertices != null ? Arrays.asList(_absoluteVertices).subList(
//						0,
//						Math.min(_absoluteVertices.length, maxLen)) : null);
//
//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") + ("\t" + dump) + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//		// TODO remove SYSTEM.OUT.PRINTLN
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

	public MapGraphId getGraphId() {
		return _graphId;
	}

	public Map3ColorProfile getMap3ColorProfile() {
		return _colorProfile;
	}

	public MapUnits getMapUnits(final ColorProviderConfig config) {

		switch (config) {
		case MAP3_TOUR:
			return _mapUnitsTour;

		default:
			return _mapUnitsProfile;
		}
	}

	/**
	 * Returns a RGB value with opacity.
	 */
	@Override
	public int getRGBValue(final ColorProviderConfig config, final float graphValue) {

		final RGBVertex[] vertices = getVertices(config);

		if (vertices == null || vertices.length == 0) {

			// color provider is not yet initialized, return a valid value
			return 0xffff00ff;
		}

		final MapUnits mapUnits = getMapUnits(config);

		final float minBrightnessFactor = _colorProfile.getMinBrightnessFactor() / 100.0f;
		final float maxBrightnessFactor = _colorProfile.getMaxBrightnessFactor() / 100.0f;

		/*
		 * find the ColorValue for the current value
		 */
		RGBVertex rgbVertex;
		RGBVertex minRgbVertex = null;
		RGBVertex maxRgbVertex = null;

		for (final RGBVertex rgbVertexFromArray : vertices) {

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

		int r;
		int g;
		int b;
		int o;

		if (minRgbVertex == null) {

			// legend value is smaller than minimum value

			rgbVertex = vertices[0];

			final RGB minRGB = rgbVertex.getRGB();

			r = minRGB.red;
			g = minRGB.green;
			b = minRGB.blue;

			final float minValue = rgbVertex.getValue();
			final float minDiff = mapUnits.legendMinValue - minValue;

			final float ratio = minDiff == 0 ? 1 : (graphValue - minValue) / minDiff;
			final float dimmRatio = minBrightnessFactor * ratio;

			if (_colorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				r = r - (int) (dimmRatio * r);
				g = g - (int) (dimmRatio * g);
				b = b - (int) (dimmRatio * b);

			} else if (_colorProfile.getMinBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				r = r + (int) (dimmRatio * (255 - r));
				g = g + (int) (dimmRatio * (255 - g));
				b = b + (int) (dimmRatio * (255 - b));
			}

			o = (int) (rgbVertex.getOpacity() * 0xff);

		} else if (maxRgbVertex == null) {

			// legend value is larger than maximum value

			rgbVertex = vertices[vertices.length - 1];

			final RGB maxRGB = rgbVertex.getRGB();

			r = maxRGB.red;
			g = maxRGB.green;
			b = maxRGB.blue;

			final float maxValue = rgbVertex.getValue();
			final float maxDiff = mapUnits.legendMaxValue - maxValue;

			final float ratio = maxDiff == 0 ? 1 : (graphValue - maxValue) / maxDiff;
			final float dimmRatio = maxBrightnessFactor * ratio;

			if (_colorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_DIMMING) {

				r = r - (int) (dimmRatio * r);
				g = g - (int) (dimmRatio * g);
				b = b - (int) (dimmRatio * b);

			} else if (_colorProfile.getMaxBrightness() == MapColorProfile.BRIGHTNESS_LIGHTNING) {

				r = r + (int) (dimmRatio * (255 - r));
				g = g + (int) (dimmRatio * (255 - g));
				b = b + (int) (dimmRatio * (255 - b));
			}

			o = (int) (rgbVertex.getOpacity() * 0xff);

		} else {

			// legend value is in the min/max range

			final float minValue = minRgbVertex.getValue();
			final float maxValue = maxRgbVertex.getValue();

			final RGB minRGB = minRgbVertex.getRGB();

			final float minR = minRGB.red;
			final float minG = minRGB.green;
			final float minB = minRGB.blue;
			final float minO = minRgbVertex.getOpacity();

			final RGB maxRGB = maxRgbVertex.getRGB();
			final float maxO = maxRgbVertex.getOpacity();

			final float rDiff = maxRGB.red - minR;
			final float gDiff = maxRGB.green - minG;
			final float bDiff = maxRGB.blue - minB;
			final float oDiff = maxO - minO;

			final float ratioDiff = maxValue - minValue;
			final float ratio = ratioDiff == 0 ? 1 : (graphValue - minValue) / (ratioDiff);

			r = (int) (minR + rDiff * ratio);
			g = (int) (minG + gDiff * ratio);
			b = (int) (minB + bDiff * ratio);

			final float a = (minO + oDiff * ratio);
			o = (int) (a * 0xff);
		}

		// adjust color values to 0...255, this is optimized
		final int maxR = (0 >= r) ? 0 : r;
		final int maxG = (0 >= g) ? 0 : g;
		final int maxB = (0 >= b) ? 0 : b;
		final int maxO = (0 >= o) ? 0 : o;

		r = (255 <= maxR) ? 255 : maxR;
		g = (255 <= maxG) ? 255 : maxG;
		b = (255 <= maxB) ? 255 : maxB;
		o = (255 <= maxO) ? 255 : maxO;

		final int rgba = (r & 0xFF) << 0 //
				| (g & 0xFF) << 8
				| (b & 0xFF) << 16
				| (o & 0xff) << 24
		//
		;

		return rgba;
	}

	private RGBVertex[] getVertices(final ColorProviderConfig config) {

		if (config == ColorProviderConfig.MAP3_TOUR) {

			return _verticesTour;

		} else {

			return _verticesProfile;
		}
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

	private void setVertices(final ColorProviderConfig config, final RGBVertex[] vertices) {

		if (config == ColorProviderConfig.MAP3_TOUR) {

			_verticesTour = vertices;

		} else {
			_verticesProfile = vertices;
		}
	}

	@Override
	public String toString() {
		return String.format("Map3GradientColorProvider [_graphId=%s, _colorProfile=%s]", _graphId, _colorProfile); //$NON-NLS-1$
	}

}
