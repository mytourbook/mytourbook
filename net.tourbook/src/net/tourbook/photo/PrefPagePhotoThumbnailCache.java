/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.util.StatusUtil;
import net.tourbook.util.Util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoThumbnailCache extends PreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore		_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();

	private final String			_defaultThumbnailStorePath	= Platform.getInstanceLocation().getURL().getPath();

	private Composite				_containerLocation;
	private BooleanFieldEditor		_editorBoolUseDefaultLocation;
	private DirectoryFieldEditor	_editorDirThumbnailLocation;

	private Spinner					_spinnerNumberOfImages;

	@Override
	protected Control createContents(final Composite parent) {

		_prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final Composite ui = createUI(parent);

		restoreState();

		enableControls();

		/*
		 * hide error messages, it appears when the thumbnail location is invalid
		 */
		if (_editorBoolUseDefaultLocation.getBooleanValue() == false) {
			setErrorMessage(null);
		}

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.swtDefaults().applyTo(container);
		{
			createUI_10_ThumbnailCache(container);
			createUI_20_ImageCache(container);
		}

		return container;
	}

	private void createUI_10_ThumbnailCache(final Composite parent) {

		Text textDirEditor;

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.PrefPage_Photo_Cache_Group_ThumbnailCacheLocation);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			/*
			 * field: use default location
			 */
			_editorBoolUseDefaultLocation = new BooleanFieldEditor(
					ITourbookPreferences.PHOTO_USE_DEFAULT_THUMBNAIL_LOCATION,
					Messages.PrefPage_Photo_Cache_Label_UseDefaultThumbnailLocation,
					group);
			_editorBoolUseDefaultLocation.setPage(this);
			_editorBoolUseDefaultLocation.setPreferenceStore(_prefStore);
			_editorBoolUseDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
				public void propertyChange(final PropertyChangeEvent event) {
					enableControls();
				}
			});
			new Label(group, SWT.NONE);

			_containerLocation = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_containerLocation);
			{
				// field: path for the srtm data
				_editorDirThumbnailLocation = new DirectoryFieldEditor(
						ITourbookPreferences.PHOTO_CUSTOM_THUMBNAIL_LOCATION,
						Messages.PrefPage_Photo_Cache_Label_ThumbnailLocation,
						_containerLocation);

				_editorDirThumbnailLocation.setPage(this);
				_editorDirThumbnailLocation.setPreferenceStore(_prefStore);
				_editorDirThumbnailLocation.setEmptyStringAllowed(false);

				textDirEditor = _editorDirThumbnailLocation.getTextControl(_containerLocation);
				textDirEditor.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(final ModifyEvent e) {
						validateData();
					}
				});
			}
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);

		// set width for the text control that the pref dialog is not as wide as the full path
//		final GridData gd = (GridData) textDirEditor.getLayoutData();
//		gd.widthHint = 200;
	}

	private void createUI_20_ImageCache(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_Photo_Cache_Group_ThumbnailCacheSize);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * label: nof images
			 */
			Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_Cache_Label_NumberOfImages);

			/*
			 * spinner: nof images
			 */
			_spinnerNumberOfImages = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerNumberOfImages);
			_spinnerNumberOfImages.setMinimum(0);
			_spinnerNumberOfImages.setMaximum(Integer.MAX_VALUE);
			_spinnerNumberOfImages.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			/*
			 * button: get number of max images
			 */
			final Button buttonGetHandels = new Button(group, SWT.PUSH);
			GridDataFactory.fillDefaults()//
					.span(2, 1)
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(buttonGetHandels);
			buttonGetHandels.setText(Messages.PrefPage_Photo_Cache_Button_GetNumberOfImages);
			buttonGetHandels.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectGetImageHandels();
				}
			});

			/*
			 * label: info
			 */
			label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.grab(true, false)
					.span(2, 1)
					.hint(400, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_Cache_Label_ThumbnailCacheSizeInfo);
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return _prefStore;
	}

	private void enableControls() {

		final boolean useDefaultLocation = _editorBoolUseDefaultLocation.getBooleanValue();

		if (useDefaultLocation) {
			_editorDirThumbnailLocation.setEnabled(false, _containerLocation);
			_editorDirThumbnailLocation.setStringValue(_defaultThumbnailStorePath);
		} else {
			_editorDirThumbnailLocation.setEnabled(true, _containerLocation);
		}
	}

	public void init(final IWorkbench workbench) {}

	@Override
	public boolean okToLeave() {

		if (validateData() == false) {
			return false;
		}

		return super.okToLeave();
	}

	private void onSelectGetImageHandels() {

		final Display display = Display.getCurrent();
		final ArrayList<Image> imageHandels = new ArrayList<Image>();
		final int[] imageNo = { 0 };
		final int maxTest = 50000;

		BusyIndicator.showWhile(display, new Runnable() {
			public void run() {

				try {

					do {

						imageHandels.add(new Image(display, 10, 10));

						imageNo[0]++;
					}
					while (imageNo[0] < maxTest);

				} catch (final Exception e) {
					// ignore because it will happen
				} finally {

					for (final Image image : imageHandels) {
						image.dispose();
					}

					String message;
					if (imageNo[0] == maxTest) {
						message = NLS.bind(//
								Messages.PrefPage_Photo_Cache_Dialog_MaxHandle_NoError,
								Integer.toString(maxTest));
					} else {
						message = NLS.bind(
								Messages.PrefPage_Photo_Cache_Dialog_MaxHandle_CreatedImagesBeforeError,
								Integer.toString(imageNo[0]));
					}

					StatusUtil.log(message);

					MessageDialog.openInformation(getShell(), Messages.PrefPage_Photo_Cache_Dialog_MaxHandle_Title, message);
				}
			}
		});
	}

	@Override
	protected void performDefaults() {

		_editorBoolUseDefaultLocation.loadDefault();
		_editorDirThumbnailLocation.loadDefault();

		_spinnerNumberOfImages.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.PHOTO_IMAGE_CACHE_SIZE));

		enableControls();

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		if (_editorBoolUseDefaultLocation == null) {
			// page is not initialized this case happened and created an NPE
			return super.performOk();
		}

		if (validateData() == false) {
			return false;
		}

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		_spinnerNumberOfImages.setSelection(_prefStore.getInt(ITourbookPreferences.PHOTO_IMAGE_CACHE_SIZE));

		_editorBoolUseDefaultLocation.load();
		_editorDirThumbnailLocation.load();
	}

	private void saveState() {

		boolean isModified = false;

		final int newCacheSize = _spinnerNumberOfImages.getSelection();
		final int oldCacheSize = _prefStore.getInt(ITourbookPreferences.PHOTO_IMAGE_CACHE_SIZE);

		if (oldCacheSize != newCacheSize) {
			isModified = true;
			_prefStore.setValue(ITourbookPreferences.PHOTO_IMAGE_CACHE_SIZE, newCacheSize);
		}

		_editorBoolUseDefaultLocation.store();
		_editorDirThumbnailLocation.store();

		if (isModified) {
			PhotoImageCache.setCacheSize(newCacheSize);
		}
	}

	private boolean validateData() {

		boolean isValid = true;

		if (_editorBoolUseDefaultLocation.getBooleanValue() == false) {

			// use custom location

			final String customFilePath = _editorDirThumbnailLocation.getStringValue();

			if (Util.isDirectory(customFilePath) == false) {

				isValid = false;

				setErrorMessage(Messages.PrefPage_Photo_Cache_Error_ThumbnailLocation);
				_editorDirThumbnailLocation.setFocus();
			}
		}

		if (isValid) {
			setErrorMessage(null);
		}

		setValid(isValid);

		return isValid;
	}

}
