package net.tourbook.ui.views.nutrition;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.nutrition.NutritionUtils;

import org.eclipse.jface.action.Action;

public class ActionOpenProductsWebsite extends Action {

   private List<String> _productCodes = new ArrayList<>();

   public ActionOpenProductsWebsite() {

      super(Messages.Tour_Nutrition_Button_OpenProductsWebsite, AS_PUSH_BUTTON);
   }

   @Override
   public void run() {

      _productCodes.forEach(productCode -> NutritionUtils.openProductWebPage(productCode));
   }

   public void setTourNutritionProducts(final List<String> productCodes) {
      _productCodes = productCodes;
   }
}
