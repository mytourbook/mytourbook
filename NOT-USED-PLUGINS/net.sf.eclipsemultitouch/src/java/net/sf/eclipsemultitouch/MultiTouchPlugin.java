// <--------------------------------------------------------------------------------------------> //
// File name:       $RCSfile: MultiTouchPlugin.java $
// First created:   $Created: Dec 16, 2009 by mirko $
// Last modified:   $Date$ by $Author$
// Version:         $Revision$
//
// Copyright (c) 2009 Mirko Raner.
// All rights reserved. This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available
// at http://www.eclipse.org/legal/epl-v10.html

package net.sf.eclipsemultitouch;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
* The class {@link MultiTouchPlugin} is the main plug-in class and bundle activator for the
* Eclipse Multi-Touch plug-in.
*
* @author Mirko Raner
* @version $Revision: $
**/
public class MultiTouchPlugin extends AbstractUIPlugin
{
    /** The plug-in ID of the BugWizard Core plug-in. **/
    public static final String PLUGIN_ID = "net.sf.eclipsemultitouch"; //$NON-NLS-1$

    private static MultiTouchPlugin plugin;

    /**
    * @see AbstractUIPlugin#start(BundleContext)
    **/
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
    }

    /**
    * @see AbstractUIPlugin#stop(BundleContext)
    **/
    public void stop(BundleContext context) throws Exception
    {
        plugin = null;
        super.stop(context);
    }

    /**
    * Returns the shared instance.
    *
    * @return the shared {@link MultiTouchPlugin} instance
    **/
    public static MultiTouchPlugin getDefault()
    {
        return plugin;
    }
}
