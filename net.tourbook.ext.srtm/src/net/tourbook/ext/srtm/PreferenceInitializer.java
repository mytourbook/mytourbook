package net.tourbook.ext.srtm;

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
		store.setDefault(IPreferences.SRTM_DATA_FILEPATH, "");//$NON-NLS-1$

	}
}
