/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.preferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.byteholder.geoclipse.Activator;

/**
 * Category page for the map
 */
public class PrefPageMap extends PreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor	fChkShowTileInfo;
	private FontFieldEditor		fMonoFontEditor;

	public PrefPageMap() {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite container = createUI(parent);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final IPreferenceStore prefStore = getPreferenceStore();

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			final Composite infoContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(infoContainer);
			GridLayoutFactory.fillDefaults().applyTo(infoContainer);
			{

				// checkbox: show tile info
				fChkShowTileInfo = new BooleanFieldEditor(
						IMappingPreferences.SHOW_MAP_TILE_INFO,
						Messages.pref_map_show_tile_info,
						infoContainer);
				fChkShowTileInfo.setPreferenceStore(prefStore);
				fChkShowTileInfo.setPage(this);
				fChkShowTileInfo.load();
			}

			final Composite fontContainer = new Composite(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(fontContainer);
			GridLayoutFactory.fillDefaults().applyTo(fontContainer);
			{
				// font: mono space
				fMonoFontEditor = new FontFieldEditor(
						IMappingPreferences.THEME_FONT_LOGGING,
						Messages.Theme_Font_Logging,
						Messages.Theme_Font_Logging_PREVIEW_TEXT,
						fontContainer);
				fMonoFontEditor.setPreferenceStore(prefStore);
				fMonoFontEditor.setPage(this);
				fMonoFontEditor.load();
			}
			// force 2 columns
			final GridLayout gl = (GridLayout) fontContainer.getLayout();
			gl.numColumns = 2;
			gl.makeColumnsEqualWidth = true;
		}

		return container;
	}

	/**
	 * Returns preference store that belongs to this plugin.
	 * 
	 * @return IPreferenceStore the preference store for this plugin
	 */
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean performOk() {

		fChkShowTileInfo.store();
		fMonoFontEditor.store();

		return true;
	}

}
