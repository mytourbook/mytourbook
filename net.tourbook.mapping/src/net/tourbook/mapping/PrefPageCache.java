/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/

package net.tourbook.mapping;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageCache extends PreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor		fIsOffLineCache;
	private DirectoryFieldEditor	fOffLineCachePathEditor;

	private Composite				fOffLineContainer;
	private RadioGroupFieldEditor	fCacheLocation;

	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {

		Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		/*
		 * checkbox: is offline enabled
		 */
		Composite isOffLineContainer = new Composite(container, SWT.NONE);

		GridDataFactory.swtDefaults().applyTo(isOffLineContainer);
		fIsOffLineCache = new BooleanFieldEditor(IMappingPreferences.IS_OFFLINE_CACHE,
				Messages.pref_cache_use_offline_cache,
				isOffLineContainer);
		fIsOffLineCache.setPreferenceStore(getPreferenceStore());
		fIsOffLineCache.setPage(this);
		fIsOffLineCache.load();

		fCacheLocation = new RadioGroupFieldEditor(IMappingPreferences.OFFLINE_LOCATION,
				Messages.pref_cache_location,
				1,
				new String[][] {
						new String[] {
								Messages.pref_cache_location_default,
								IMappingPreferences.OFFLINE_LOCATION_INTERNAL },
						new String[] {
								Messages.pref_cache_location_selected_path,
								IMappingPreferences.OFFLINE_LOCATION_SELECTED_PATH }, },
				container,
				false);
		fCacheLocation.setPreferenceStore(getPreferenceStore());
		fCacheLocation.setPage(this);
		fCacheLocation.load();

		/*
		 * offline cache settings
		 */
		fOffLineContainer = new Composite(container, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15, 0).applyTo(fOffLineContainer);
//		fOffLineContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			/*
			 * field: path for the tile cache
			 */
			fOffLineCachePathEditor = new DirectoryFieldEditor(IMappingPreferences.OFFLINE_CACHE_PATH,
					Messages.pref_cache_selected_location,
					fOffLineContainer);
			fOffLineCachePathEditor.setPreferenceStore(getPreferenceStore());
			fOffLineCachePathEditor.setPage(this);
			fOffLineCachePathEditor.load();

		}

		/*
		 * set listeners which can't be done earlier
		 */
		fIsOffLineCache.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				enableControls();
			}
		});

		enableControls();

		return container;
	}

	private void enableControls() {
		final boolean isOffLineCache = fIsOffLineCache.getBooleanValue();
		fOffLineCachePathEditor.setEnabled(isOffLineCache, fOffLineContainer);
		fOffLineCachePathEditor.setEmptyStringAllowed(isOffLineCache == false);

	}

	@Override
	public boolean performOk() {

		// store field content into the pref store

		fIsOffLineCache.store();
		fOffLineCachePathEditor.store();
		fCacheLocation.store();

		return super.performOk();
	}

}
