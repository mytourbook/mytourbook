/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.byteholder.geoclipse.preferences.Messages;

/**
 * Cache for offline map tiles
 */
public class PrefPageMap25OfflineMap extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore		_prefStore					= TourbookPlugin.getPrefStore();

	private final String			_defaultOfflineMapFolder	= Platform.getInstanceLocation().getURL().getPath();

	/*
	 * UI controls
	 */
	private Composite				_editorContainerLocation;

	private BooleanFieldEditor		_editorBool_UseDefaultLocation;
	private DirectoryFieldEditor	_editorDir_ThumbnailLocation;

	@Override
	protected void createFieldEditors() {

		createUI(getFieldEditorParent());

		// content is set in initialize() !!!;
	}

	private Control createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			createUI_10_OfflineFolder(container);
		}

		return container;
	}

	private void createUI_10_OfflineFolder(final Composite parent) {

		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().applyTo(label);
		label.setText("text");

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_Map25_Group_OfflineMap);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			/*
			 * field: use default location
			 */
			_editorBool_UseDefaultLocation = new BooleanFieldEditor(
					ITourbookPreferences.MAP25_OFFLINE_MAP_IS_DEFAULT_LOCATION,
					Messages.Pref_Map25_Label_UseDefaultLocation,
					group);
			_editorBool_UseDefaultLocation.setPage(this);
			_editorBool_UseDefaultLocation.setPreferenceStore(_prefStore);

			addField(_editorBool_UseDefaultLocation);

			// spacer
			new Label(group, SWT.NONE);

			_editorContainerLocation = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_editorContainerLocation);
			{
				/*
				 * editor: thumbnail location
				 */
				_editorDir_ThumbnailLocation = new DirectoryFieldEditor(
						ITourbookPreferences.MAP25_OFFLINE_MAP_CUSTOM_LOCATION,
						Messages.Pref_Map25_Label_Location,
						_editorContainerLocation);

				_editorDir_ThumbnailLocation.setPage(this);
				_editorDir_ThumbnailLocation.setPreferenceStore(_prefStore);
				_editorDir_ThumbnailLocation.setEmptyStringAllowed(false);

				final Text textDirEditor = _editorDir_ThumbnailLocation.getTextControl(_editorContainerLocation);
				textDirEditor.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(final ModifyEvent e) {
						isDataValid();
					}
				});

				addField(_editorDir_ThumbnailLocation);
			}
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
//		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
	}

	private void enableControls() {

		/*
		 * location
		 */
		final boolean useDefaultLocation = _editorBool_UseDefaultLocation.getBooleanValue();

		if (useDefaultLocation) {
			_editorDir_ThumbnailLocation.setEnabled(false, _editorContainerLocation);
			_editorDir_ThumbnailLocation.setStringValue(_defaultOfflineMapFolder);
		} else {
			_editorDir_ThumbnailLocation.setEnabled(true, _editorContainerLocation);
		}
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void initialize() {

		super.initialize();

		// #####################################################################################
		//
		// must be done after the initialize() method because this will overwrite prop listener
		//
		// it took me several hours to figure out this problem
		//
		// #####################################################################################

		final IPropertyChangeListener propListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {
				enableControls();
			}
		};
		_editorBool_UseDefaultLocation.setPropertyChangeListener(propListener);

		restoreState();

		/*
		 * hide error messages, it appears when the thumbnail location is invalid
		 */
		if (_editorBool_UseDefaultLocation.getBooleanValue() == false) {
			setErrorMessage(null);
		}
	}

	private boolean isDataValid() {

		boolean isValid = true;

		if (_editorBool_UseDefaultLocation.getBooleanValue() == false) {

			// use custom location

			final String customFilePath = _editorDir_ThumbnailLocation.getStringValue();

			if (Util.isDirectory(customFilePath) == false) {

				isValid = false;

				setErrorMessage(Messages.Pref_Map25_Error_Location);
				_editorDir_ThumbnailLocation.setFocus();
			}
		}

		if (isValid) {
			setErrorMessage(null);
			setErrorMessage("fehler");
		}

		setValid(isValid);

		return isValid;
	}

	@Override
	public boolean okToLeave() {

		if (isDataValid() == false) {
			return false;
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();

		enableControls();
	}

	@Override
	public boolean performOk() {

		if (_editorBool_UseDefaultLocation == null) {
			// page is not initialized this case happened and created an NPE
			return false;
		}

		if (isDataValid() == false) {
			return false;
		}

		// set pref store values
		final boolean isOk = super.performOk();

		saveState();

		return isOk;
	}

	private void restoreState() {

		enableControls();
	}

	private void saveState() {

//		Map25Manager.updateOfflineMapLocation();
	}

}
