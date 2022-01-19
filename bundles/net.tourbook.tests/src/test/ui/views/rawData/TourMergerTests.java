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
   void testMergeTours_Basic() {

      final String sourceFilePath = FilesUtils.getAbsoluteFilePath(FILES_PATH + "Move_2011_07_03_08_01_04_Trail+running.fit");
      _sourceTour = Initializer.importTour_FIT(sourceFilePath);

      final String targetFilePath = FilesUtils.getAbsoluteFilePath(FILES_PATH + "2011-07-03_KiliansClassik.gpx");
      _targetTour = Initializer.importTour_GPX(targetFilePath);

      _tourMerger = new TourMerger(
            _sourceTour,
            _targetTour,
            false, // Merge time
            false, //_chkAdjustAltiFromSource.getSelection(),
            false, //_chkAdjustAltiSmoothly.getSelection(),
            false, //_chkSynchStartTime.getSelection(),
            0); //_tourStartTimeSynchOffset,

      //Comparing the original tour before the merge
      String controlFilePath = FILES_PATH + "2011-07-03_KiliansClassik"; //$NON-NLS-1$

      Comparison.compareTourDataAgainstControl(_targetTour, controlFilePath);

      final TourData mergedTour = _tourMerger.computeMergedData();

      //Comparing the merged tour
      controlFilePath = FILES_PATH + "2011-07-03_KiliansClassik-MergedWith-Move_2011_07_03_08_01_04_Trail+running"; //$NON-NLS-1$

      Comparison.compareTourDataAgainstControl(mergedTour, controlFilePath);
   }
}
