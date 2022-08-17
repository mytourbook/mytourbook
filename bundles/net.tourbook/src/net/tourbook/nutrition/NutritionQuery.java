package net.tourbook.nutrition;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class NutritionQuery implements Runnable {

   private List<String>        _searchResult = new ArrayList<>();

   private Exception           _exception;

   private String              _query;

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

   public List<String> getSearchResult() {
      return _searchResult;
   }

   public void removePropertyChangeListener(final PropertyChangeListener pcl) {
         support.removePropertyChangeListener(pcl);
     }

     @Override
     public void run() {

      final var oldValue = List.copyOf(_searchResult);
      try {

         _searchResult.clear();
//
//			final String uri = SEARCH_URL + URLEncoder.encode(_query, "utf8"); //$NON-NLS-1$
//
//			SAXParserFactory.newInstance().newSAXParser().parse(uri, new GeoQuerySAXHandler(_searchResult));
         final var toto = NutritionUtils.searchProduct(_query);
         toto.stream().forEach(product -> _searchResult.add(product.getProductName()));
      } catch (final Exception e) {
         _exception = e;
      }

      support.firePropertyChange("_searchResult", oldValue, _searchResult);
   }


}
