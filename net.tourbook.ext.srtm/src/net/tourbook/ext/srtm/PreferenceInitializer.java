package net.tourbook.ext.srtm;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Initialize preferences for the SRTM plugin
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		store.setDefault(IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH, true);
		
		// set srtm default data path to the working directory
		store.setDefault(IPreferences.SRTM_DATA_FILEPATH, Platform.getInstanceLocation().getURL().getPath());

	}
}
