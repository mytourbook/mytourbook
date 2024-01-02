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

import net.tourbook.nutrition.openfoodfacts.Product;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class NutritionQuery implements Runnable {

   private List<Product>         _searchResult = new ArrayList<>();

   private Exception             _exception;

   private String                _query;

   private PropertyChangeSupport support;

   public NutritionQuery() {
      support = new PropertyChangeSupport(this);
   }

   public void addPropertyChangeListener(final PropertyChangeListener pcl) {
      support.addPropertyChangeListener(pcl);
   }

   public void asyncFind(final String productName) {

      _query = productName;

      final Job job = new Job("Messages.job_name_searchingPOI") {

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

      final var oldValue = List.copyOf(_searchResult);
      //todo fb
      //if the current one only contains the not found product then oldvalue should be empty
      try {

         _searchResult.clear();

         final List<Product> searchProductResults = NutritionUtils.searchProduct(_query);

         if (searchProductResults.isEmpty()) {
            searchProductResults.add(new Product("Not found", null, null, "not found"));
         }
         _searchResult.addAll(searchProductResults);

      } catch (final Exception e) {
         _exception = e;
      }

      support.firePropertyChange("_searchResult", oldValue, _searchResult);
   }
}
