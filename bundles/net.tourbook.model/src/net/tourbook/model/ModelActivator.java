package net.tourbook.model;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class ModelActivator extends AbstractUIPlugin {

   // The plug-in ID
   public static final String PLUGIN_ID = "net.tourbook.model"; //$NON-NLS-1$

   // The shared instance
   private static ModelActivator plugin;

   /**
    * The constructor
    */
   public ModelActivator() {}

   /**
    * Returns the shared instance
    *
    * @return the shared instance
    */
   public static ModelActivator getDefault() {
      return plugin;
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      plugin = this;
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      plugin = null;
      super.stop(context);
   }

}
