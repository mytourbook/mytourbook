package org.eclipse.babel.core.message.plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public class BabelCorePlugin implements BundleActivator {

   private static BabelCorePlugin _plugin;
   private Version                 _version;

   public static BabelCorePlugin getInstance() {

      return _plugin;
   }

   public Version getVersion() {

      return _version;
   }

   @Override
   public void start(final BundleContext context) throws Exception {

      _plugin = this;

      _version = context.getBundle().getVersion();
   }

   @Override
   public void stop(final BundleContext context) throws Exception {

   }

}
