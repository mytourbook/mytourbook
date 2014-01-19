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
package net.tourbook.map2.view;

import java.util.ArrayList;

import net.tourbook.common.color.MapGraphId;
import net.tourbook.data.HrZoneContext;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPersonHRZone;
import net.tourbook.training.TrainingManager;

import org.eclipse.swt.graphics.RGB;

/**
 * Contains all data to draw the legend image for discrete colors.
 */
public class HrZonesColorProvider implements IDiscreteColorProvider {

	private MapGraphId					_graphId;

	/**
	 * {@link TourData} which are checked if they contain valid HR zone data
	 */
	private TourData					_checkedTourData;

	private boolean						_isValidHrZoneData;

	private ArrayList<TourPersonHRZone>	_personHrZones;

	private HrZoneContext				_hrZoneContext;
	private float[]						_pulseData;

	public HrZonesColorProvider(final MapGraphId graphId) {
		_graphId = graphId;
	}

	private void checkHrData(final TourData tourData) {

		if (tourData != _checkedTourData) {

			// get required data which are needed to get the HR zone color

			_checkedTourData = tourData;
			_isValidHrZoneData = TrainingManager.isRequiredHrZoneDataAvailable(tourData);

			if (_isValidHrZoneData) {
				_personHrZones = tourData.getTourPerson().getHrZonesSorted();
				_hrZoneContext = tourData.getHrZoneContext();
				_pulseData = tourData.pulseSerie;
			}
		}
	}

	@Override
	public int getColorValue(final TourData tourData, final int serieIndex) {

		checkHrData(tourData);

		if (_isValidHrZoneData == false) {
			return 0xffFF0AE3;
		}

		return getHrColor(serieIndex);
	}

	@Override
	public int getColorValue(final TourData tourData, final int valueIndex, final boolean isDrawLine) {

		checkHrData(tourData);

		if (_isValidHrZoneData == false) {
			return 0xFF0AE3;
		}

		/**
		 * Superhack :-) <br>
		 * Adjust the value index to use the previous data because when a tour is painted as a line,
		 * it will be painted "to" the value and not "from" the value.<br>
		 * This is not the best solution but adjusting the tour paint algorithm is much much more
		 * complex, really !!!
		 */
		final int adjustedValueIndex = valueIndex > 0 && isDrawLine ? //
				valueIndex - 1
				: valueIndex;

		return getHrColor(adjustedValueIndex);
	}

	@Override
	public MapGraphId getGraphId() {
		return _graphId;
	}

	private int getHrColor(final int serieIndex) {

		final float pulse = _pulseData[serieIndex];
		final int zoneIndex = TrainingManager.getZoneIndex(_hrZoneContext, pulse);

		final TourPersonHRZone hrZone = _personHrZones.get(zoneIndex);
		final RGB rgb = hrZone.getColor();

		final int rgbValue = ((rgb.red & 0xFF) << 0) //
				| ((rgb.green & 0xFF) << 8)
				| ((rgb.blue & 0xFF) << 16)
				| ((0xFF) << 24);

		return rgbValue;
	}

}
