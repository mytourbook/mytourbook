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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageCache extends PreferencePage implements IWorkbenchPreferencePage {

	final String					fDefaultCachePath	= Platform.getInstanceLocation().getURL().getPath();

	private Composite				fPrefContainer;
	private Composite				fOffLineContainer;
//	private Composite				fOffLineFolder;

	private BooleanFieldEditor		fUseOffLineCache;

	private Group					fGrpCacheDirectory;
	private Button					fRdoLocationDefault;
	private Text					fTxtDefaultPath;

	private Button					fRdoLocationSelectedPath;
	private DirectoryFieldEditor	fCachePathEditor;

	private IntegerFieldEditor		fPeriodOfValidityEditor;

	private Button					fBtnClearCache;

	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected Control createContents(Composite parent) {

		createUI(parent);

		enableControls();

		return fPrefContainer;
	}

	private void createUI(Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		fPrefContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fPrefContainer);
		GridLayoutFactory.fillDefaults().applyTo(fPrefContainer);
//		fPrefContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		// checkbox: is offline enabled
		GridDataFactory.swtDefaults().applyTo(fPrefContainer);
		fUseOffLineCache = new BooleanFieldEditor(IMappingPreferences.OFFLINE_CACH_IS_USED,
				Messages.pref_cache_use_offline_cache,
				fPrefContainer);
		fUseOffLineCache.setPreferenceStore(prefStore);
		fUseOffLineCache.setPage(this);
		fUseOffLineCache.load();
		fUseOffLineCache.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				enableControls();
			}
		});

		/*
		 * offline cache settings
		 */
		fOffLineContainer = new Composite(fPrefContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(15, 0).applyTo(fOffLineContainer);
//		fOffLineContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{

			// group: cache directory
			fGrpCacheDirectory = new Group(fOffLineContainer, SWT.NONE);
			fGrpCacheDirectory.setText("Directory for Map Files");

//			fOffLineFolder = new Composite(fOffLineContainer, SWT.NONE);
//			GridDataFactory.fillDefaults().grab(true, false).indent(15, 0).applyTo(fOffLineFolder);
			{
				// radio: default location
				fRdoLocationDefault = new Button(fGrpCacheDirectory, SWT.RADIO);
				fRdoLocationDefault.setText(Messages.pref_cache_location_default);
				fRdoLocationDefault.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						enableLocation();
					}
				});
				GridDataFactory.swtDefaults().span(3, 1).applyTo(fRdoLocationDefault);

				// label: default path
				fTxtDefaultPath = new Text(fGrpCacheDirectory, SWT.WRAP);
				fTxtDefaultPath.setText(fDefaultCachePath);
				fTxtDefaultPath.setEnabled(false);
				GridDataFactory.swtDefaults().span(3, 1).indent(15, 0).applyTo(fTxtDefaultPath);

				// radio: selected location
				fRdoLocationSelectedPath = new Button(fGrpCacheDirectory, SWT.RADIO);
				fRdoLocationSelectedPath.setText(Messages.pref_cache_location_selected_path);
				fRdoLocationSelectedPath.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						enableLocation();
					}
				});
				GridDataFactory.swtDefaults().span(3, 1).indent(0, 5).applyTo(fRdoLocationSelectedPath);

				// initialize radio buttons
				if (prefStore.getBoolean(IMappingPreferences.OFFLINE_CACH_USE_SELECTED_LOCATION)) {
					fRdoLocationSelectedPath.setSelection(true);
				} else {
					fRdoLocationDefault.setSelection(true);
				}

				// field: path for the tile cache
				fCachePathEditor = new DirectoryFieldEditor(IMappingPreferences.OFFLINE_CACHE_PATH,
						Messages.pref_cache_selected_location,
						fGrpCacheDirectory);
				fCachePathEditor.setPreferenceStore(prefStore);
				fCachePathEditor.setPage(this);
				fCachePathEditor.load();
				GridDataFactory.swtDefaults()
						.indent(15, 0)
						.applyTo(fCachePathEditor.getLabelControl(fGrpCacheDirectory));

			}

			// field: period of validity
			fPeriodOfValidityEditor = new IntegerFieldEditor(IMappingPreferences.OFFLINE_CACHE_PERIOD_OF_VALIDITY,
					Messages.pref_cache_period_of_validity,
					fOffLineContainer,
					2);
			fPeriodOfValidityEditor.getLabelControl(fOffLineContainer)
					.setToolTipText(Messages.pref_cache_period_of_validity_tooltip);

			fPeriodOfValidityEditor.setPreferenceStore(prefStore);
			fPeriodOfValidityEditor.setPage(this);
			fPeriodOfValidityEditor.load();
			GridDataFactory.swtDefaults()
					.hint(40, SWT.DEFAULT)
					.applyTo(fPeriodOfValidityEditor.getTextControl(fOffLineContainer));

			// spacer
			new Label(fOffLineContainer, SWT.NONE);

			// button: clear cache
			fBtnClearCache = new Button(fOffLineContainer, SWT.PUSH);
			fBtnClearCache.setText(Messages.pref_cache_clear_cache);
			fBtnClearCache.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					clearCache();
				}
			});

		}

		// !!! set layout after the editor was created because the editor sets the parents layout
//		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(fOffLineFolder);
	}

	private void clearCache() {
	// TODO Auto-generated method stub

	}

	private void enableLocation() {

		if (fUseOffLineCache.getBooleanValue()) {

			// use offline cache

			if (fRdoLocationDefault.getSelection() == true) {

				// use default location

				fCachePathEditor.setEnabled(false, fGrpCacheDirectory);

			} else {

				// use selected location

				fCachePathEditor.setEnabled(true, fGrpCacheDirectory);
			}

		} else {

			// disable offline cache

			fCachePathEditor.setEnabled(false, fGrpCacheDirectory);
		}
	}

	private void enableControls() {

		final boolean isOffLineCache = fUseOffLineCache.getBooleanValue();

		fRdoLocationDefault.setEnabled(isOffLineCache);
		fRdoLocationDefault.setEnabled(isOffLineCache);
		fRdoLocationSelectedPath.setEnabled(isOffLineCache);

		fCachePathEditor.setEnabled(isOffLineCache, fGrpCacheDirectory);
		fCachePathEditor.setEmptyStringAllowed(isOffLineCache == false);

		fPeriodOfValidityEditor.setEnabled(isOffLineCache, fOffLineContainer);

//		fBtnClearCache.setEnabled(isOffLineCache);
		fBtnClearCache.setEnabled(false);

		enableLocation();
	}

	@Override
	public boolean performOk() {

		// store field content into the pref store

		IPreferenceStore prefStore = getPreferenceStore();

		fUseOffLineCache.store();

		prefStore.setValue(IMappingPreferences.OFFLINE_CACH_USE_SELECTED_LOCATION,
				fRdoLocationSelectedPath.getSelection());

		fCachePathEditor.store();
		fPeriodOfValidityEditor.store();

		return super.performOk();
	}

}
