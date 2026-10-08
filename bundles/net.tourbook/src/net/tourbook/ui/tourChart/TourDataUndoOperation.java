/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import net.tourbook.data.TourData;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class TourDataUndoOperation extends AbstractOperation {

   private TourChartView _tourChartView;

   private TourData      _tourData_Cloned_WithRemovedTimeSliced;
   private TourData      _tourData_Cloned_Before;

   private int           _sliderFirstIndex;
   private int           _sliderLastIndex;

   /**
    * @param label
    *           This label will show in the menu (e.g., "Undo Modify Name")
    * @param tourChartView
    * @param tourData_Cloned_Before
    * @param tourData_Cloned_WithRemovedTimeSliced
    * @param sliderFirstIndex
    * @param sliderLastIndex
    */
   public TourDataUndoOperation(final String label,
                                final TourChartView tourChartView,

                                final TourData tourData_Cloned_Before,
                                final TourData tourData_Cloned_WithRemovedTimeSliced,

                                final int sliderFirstIndex,
                                final int sliderLastIndex) {

      super(label);

      _tourChartView = tourChartView;

      _tourData_Cloned_Before = tourData_Cloned_Before;
      _tourData_Cloned_WithRemovedTimeSliced = tourData_Cloned_WithRemovedTimeSliced;

      _sliderFirstIndex = sliderFirstIndex;
      _sliderLastIndex = sliderLastIndex;
   }

   @Override
   public IStatus execute(final IProgressMonitor monitor, final IAdaptable info) throws ExecutionException {

      _tourChartView.undoRedo_Execute(_sliderFirstIndex, _sliderLastIndex);

      return Status.OK_STATUS;
   }

   @Override
   public IStatus redo(final IProgressMonitor monitor, final IAdaptable info) throws ExecutionException {

      _tourChartView.undoRedo_Redo(_tourData_Cloned_WithRemovedTimeSliced, _sliderFirstIndex, _sliderLastIndex);

      return Status.OK_STATUS;
   }

   @Override
   public IStatus undo(final IProgressMonitor monitor, final IAdaptable info) throws ExecutionException {

      _tourChartView.undoRedo_Undo(_tourData_Cloned_Before, _sliderFirstIndex, _sliderLastIndex);

      return Status.OK_STATUS;
   }

   public int getSliderFirstIndex() {
      return _sliderFirstIndex;
   }

   public int getSliderLastIndex() {
      return _sliderLastIndex;
   }
}
