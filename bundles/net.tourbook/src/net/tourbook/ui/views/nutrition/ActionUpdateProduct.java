package net.tourbook.ui.views.nutrition;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.NutritionUtils;
import net.tourbook.nutrition.ProductSearchType;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourLogView;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionUpdateProduct extends Action {

   private String   _productCode;
   private TourData _tourData;

   public ActionUpdateProduct() {

      super(Messages.Tour_Nutrition_Button_UpdateProduct, AS_PUSH_BUTTON);
      setImageDescriptor(CommonActivator.getImageDescriptor(CommonImages.App_Refresh_All));
   }

   private void onUpdateProduct() {

      final Set<TourNutritionProduct> updatedTourNutritionProducts = new HashSet<>();

      // We skip the custom products
      if (net.tourbook.common.util.StringUtils.isNullOrEmpty(_productCode)) {
         return;
      }

      //get the most up-to-date product info from the api
      final List<Product> searchProductResults = NutritionUtils.searchProduct(
            _productCode,
            ProductSearchType.ByCode);

      if (searchProductResults.isEmpty()) {

         final TourNutritionProduct tourNutritionProduct =
               _tourData
                     .getTourNutritionProducts()
                     .stream()
                     .filter(product -> product.getProductCode().equals(_productCode))
                     .findFirst()
                     .orElse(null);

         TourLogManager.subLog_ERROR(NLS.bind(
               Messages.Log_Tour_Nutrition_ProductRetrieval_Error,
               new Object[] {
                     _productCode,
                     tourNutritionProduct.getName() }));

         return;
      }

      final Product updatedProduct = searchProductResults.get(0);

      final TourNutritionProduct updatedTourNutritionProduct = new TourNutritionProduct(
            _tourData,
            updatedProduct);

      updatedTourNutritionProducts.add(updatedTourNutritionProduct);

      final boolean tourNutritionProductsUpdated =
            _tourData.updateTourNutritionProducts(updatedTourNutritionProducts);

      if (tourNutritionProductsUpdated) {

         _tourData = TourManager.saveModifiedTour(_tourData);
      } else {
         TourLogManager.subLog_INFO(Messages.Log_ModifiedTour_No_New_Data);
      }
   }

   @Override
   public void run() {

      TourLogManager.showLogView(AutoOpenEvent.TOUR_ADJUSTMENTS);

      final String logMessage = NLS.bind(
            Messages.Log_ModifiedTour_Combined_Values,
            Messages.Log_Updating_Text);
      TourLogManager.addLog(
            TourLogState.DEFAULT,
            logMessage,
            TourLogView.CSS_LOG_TITLE);

      final long start = System.currentTimeMillis();

      // show busy indicator
      BusyIndicator.showWhile(Display.getCurrent(), () -> onUpdateProduct());

      TourLogManager.log_DEFAULT(String.format(
            Messages.Log_UpdateTour_End,
            (System.currentTimeMillis() - start) / 1000.0));
   }

   public void setTourData(final TourData tourData) {
      _tourData = tourData;
   }

   public void setTourNutritionProducts(final String productCode) {
      _productCode = productCode;
   }
}
