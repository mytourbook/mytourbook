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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.Initializer;

public class TourMergerTests {

   //private static final String IMPORT_PATH          = FilesUtils.rootPath + "export/files/"; //$NON-NLS-1$

   private static TourData _targetTour;
   private TourMerger      _tourMerger;

   @BeforeAll
   static void initAll() {

      _targetTour = Initializer.importTour();
   }

   @AfterEach
   void afterEach() {

   }

   @Test
   void testMergeTours() {

      _tourMerger = new TourMerger(
            new TourData(), // _sourceTour,
            _targetTour,
            true, //_chkMergeSpeed.getSelection(),
            0, //_tourStartTimeSynchOffset,
            false, //_chkSynchStartTime.getSelection(),
            false, //_chkAdjustAltiFromSource.getSelection(),
            false);//_chkAdjustAltiSmoothly.getSelection(),

      final TourData mergedTour = _tourMerger.computeMergedData_NEWWIP();

      Comparison.compareTourDataAgainstControl(mergedTour, "filePath");
   }
}
