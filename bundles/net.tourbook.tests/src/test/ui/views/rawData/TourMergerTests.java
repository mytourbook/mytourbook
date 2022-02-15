/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package ui.views.rawData;

import net.tourbook.data.TourData;
import net.tourbook.ui.views.rawData.TourMerger;

import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class TourMergerTests {

   private static final String FILES_PATH = FilesUtils.rootPath + "ui/views/rawData/files/"; //$NON-NLS-1$

   private TourData            _sourceTour;
   private TourData            _targetTour;
   private TourMerger          _tourMerger;

   @Test
   void testMergeTours() {

      final String sourceFilePath = FilesUtils.getAbsoluteFilePath(FILES_PATH + "Move_2011_07_03_08_01_04_Trail+running.fit"); //$NON-NLS-1$
      _sourceTour = Initializer.importTour_FIT(sourceFilePath);

      final String targetFilePath = FilesUtils.getAbsoluteFilePath(FILES_PATH + "2011-07-03_KiliansClassik.gpx"); //$NON-NLS-1$
      _targetTour = Initializer.importTour_GPX(targetFilePath);

      _tourMerger = new TourMerger(_sourceTour, _targetTour, false, false, false, 0);

      //Comparing the original tour before the merge
      String controlFilePath = FILES_PATH + "2011-07-03_KiliansClassik"; //$NON-NLS-1$

      Comparison.compareTourDataAgainstControl(_targetTour, controlFilePath);

      //Merge the altitude and pulse
      _tourMerger.computeMergedData(false);
      _targetTour.altitudeSerie = _tourMerger.getNewSourceAltitudeSerie();
      _targetTour.pulseSerie = _tourMerger.getNewTargetPulseSerie();

      //Comparing the merged tour
      controlFilePath = FILES_PATH + "2011-07-03_KiliansClassik-PulseAndAltitudeMergeWith-Move_2011_07_03_08_01_04_Trail+running"; //$NON-NLS-1$

      Comparison.compareTourDataAgainstControl(_targetTour, controlFilePath);

      //Merge the pulse, temperature and speed
      _tourMerger.computeMergedData(true);
      _targetTour.altitudeSerie = _tourMerger.getNewSourceAltitudeSerie();
      _targetTour.pulseSerie = _tourMerger.getNewTargetPulseSerie();
      _targetTour.timeSerie = _tourMerger.getNewTargetTimeSerie();

      //Comparing the merged tour
      controlFilePath = FILES_PATH + "2011-07-03_KiliansClassik-PulseAndAltitudeAndSpeedMergeWith-Move_2011_07_03_08_01_04_Trail+running"; //$NON-NLS-1$

      Comparison.compareTourDataAgainstControl(_targetTour, controlFilePath);
   }
}
