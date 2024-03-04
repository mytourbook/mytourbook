/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.nutrition;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.nutrition.openfoodfacts.Product;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class NutritionQuery implements Runnable {

   private List<Product>         _searchResult = new ArrayList<>();

   private Exception             _exception;

   private String                _searchText;
   private ProductSearchType     _productSearchType;

   private PropertyChangeSupport support;

   public NutritionQuery() {
      support = new PropertyChangeSupport(this);
   }

   public void addPropertyChangeListener(final PropertyChangeListener pcl) {
      support.addPropertyChangeListener(pcl);
   }

   public void asyncFind(final String searchText, final ProductSearchType productSearchType) {

      _searchText = searchText;
      _productSearchType = productSearchType;

      final Job job = new Job(Messages.Tour_Nutrition_Job_SearchingProducts) {

         @Override
         protected IStatus run(final IProgressMonitor arg0) {
            NutritionQuery.this.run();
            return Status.OK_STATUS;
         }
      };

      job.schedule();
   }

   public Exception getException() {
      return _exception;
   }

   public List<Product> getSearchResult() {
      return _searchResult;
   }

   public void removePropertyChangeListener(final PropertyChangeListener pcl) {
      support.removePropertyChangeListener(pcl);
   }

   @Override
   public void run() {

      final List<Product> oldValue = List.copyOf(_searchResult);
      try {

         _searchResult.clear();

         final List<Product> searchProductResults = NutritionUtils.searchProduct(_searchText, _productSearchType);

         _searchResult.addAll(searchProductResults);

      } catch (final Exception e) {
         _exception = e;
      }

      // Sending an old value of null will trigger the firing.
      // Otherwise, empty values for both old and new values will not and as
      // a consequence, will leave the POIView waiting for a response forever.
      final String searchResultName = "_searchResult";//$NON-NLS-1$
      if (oldValue.isEmpty() && _searchResult.isEmpty()) {
         support.firePropertyChange(searchResultName, null, null);
      }
      support.firePropertyChange(searchResultName, oldValue, _searchResult);
   }
}
