/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    https://github.com/kevinsawicki/eclipse-oauth2
 *****************************************************************************/
/*
 * Modified for MyTourbook by Frédéric Bard
 */
package net.tourbook.cloud.oauth2;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Plug-in class
 */
public class OAuth2Plugin extends AbstractUIPlugin {

   private static OAuth2Plugin INSTANCE = new OAuth2Plugin();

   /**
    * Get default plug-in
    *
    * @return plug-in
    */
   public static OAuth2Plugin getDefault() {
      return INSTANCE;
   }

   @Override
   public void start(final BundleContext context) throws Exception {
      super.start(context);
      INSTANCE = this;
   }

   @Override
   public void stop(final BundleContext context) throws Exception {
      super.stop(context);
      INSTANCE = null;
   }
}
