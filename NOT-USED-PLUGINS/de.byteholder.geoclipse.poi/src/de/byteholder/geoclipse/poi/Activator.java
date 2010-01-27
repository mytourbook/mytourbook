package de.byteholder.geoclipse.poi;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String	PLUGIN_ID	= "de.byteholder.geoclipse.poi"; //$NON-NLS-1$

	public static final String	IMG_ANCHOR	= "anchor"; //$NON-NLS-1$
	public static final String	IMG_CAR		= "car"; //$NON-NLS-1$
	public static final String	IMG_CART	= "cart"; //$NON-NLS-1$
	public static final String	IMG_FLAG	= "flag"; //$NON-NLS-1$
	public static final String	IMG_HOUSE	= "house"; //$NON-NLS-1$
	public static final String	IMG_SOCCER	= "soccer"; //$NON-NLS-1$
	public static final String	IMG_STAR	= "star"; //$NON-NLS-1$

	// The shared instance
	private static Activator	plugin;

	/**
	 * The constructor
	 */
	public Activator() {
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);

		getImageRegistry().put(IMG_ANCHOR, getImageDescriptor("icons/anchor.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_CAR, getImageDescriptor("icons/car.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_CART, getImageDescriptor("icons/cart.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_FLAG, getImageDescriptor("icons/flag_green.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_HOUSE, getImageDescriptor("icons/house.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_SOCCER, getImageDescriptor("icons/sport_soccer.png")); //$NON-NLS-1$
		getImageRegistry().put(IMG_STAR, getImageDescriptor("icons/star.png")); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in relative path
	 * 
	 * @param path
	 *        the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
