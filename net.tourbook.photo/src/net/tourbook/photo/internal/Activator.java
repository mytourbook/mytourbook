package net.tourbook.photo.internal;

import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoUI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String	PLUGIN_ID	= "net.tourbook.photo"; //$NON-NLS-1$

	// The shared instance
	private static Activator	plugin;

	/**
	 * The constructor
	 */
	public Activator() {}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for images in the plug-in path.
	 * 
	 * @param path
	 *            the path
	 * @return the axisImage descriptor
	 */
	public static ImageDescriptor getImageDescriptor(final String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, "icons/" + path); //$NON-NLS-1$
	}

	/**
	 * @param sectionName
	 * @return Returns the dialog setting section for the sectionName, a section is always returned
	 *         even when it's empty
	 */
	public IDialogSettings getDialogSettingsSection(final String sectionName) {

		final IDialogSettings dialogSettings = getDialogSettings();
		IDialogSettings section = dialogSettings.getSection(sectionName);

		if (section == null) {
			section = dialogSettings.addNewSection(sectionName);
		}

		return section;
	}

	@Override
	public void start(final BundleContext context) throws Exception {

		super.start(context);

		plugin = this;

		PhotoUI.init();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {

		PhotoLoadManager.stopImageLoading(true);
		PhotoLoadManager.removeInvalidImageFiles();

		PhotoImageCache.disposeAll();

		plugin = null;
		super.stop(context);
	}

}
