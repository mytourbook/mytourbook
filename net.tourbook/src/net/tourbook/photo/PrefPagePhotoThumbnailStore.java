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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.manager.PhotoImageCache;
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.photo.manager.ThumbnailStore;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.UI;
import net.tourbook.util.Util;

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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class PrefPagePhotoThumbnailStore extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore		_prefStore					= TourbookPlugin.getDefault().getPreferenceStore();

	private final String			_defaultThumbnailStorePath	= Platform.getInstanceLocation().getURL().getPath();

	private final DateTimeFormatter	_dateFormatter				= DateTimeFormat.fullDateTime();

	/*
	 * UI controls
	 */
	private Composite				_containerLocation;
	private BooleanFieldEditor		_editorBoolUseDefaultLocation;
	private BooleanFieldEditor		_editorBoolDoCleanup;
	private DirectoryFieldEditor	_editorDirThumbnailLocation;

	private Label					_lblCleanup;
	private Spinner					_spinnerNumberOfDaysToKeepImages;
	private Label					_lblCleanupDays;

	private Label					_lblCheckStore;
	private Spinner					_spinnerCleanupPeriod;
	private Label					_lblCheckStoreDays;

	private Button					_btnCleanupNow;
	private Button					_btnCleanupAll;
	private Label					_lblLastCleanup;

	@Override
	protected void createFieldEditors() {

		createUI(getFieldEditorParent());

		// content is set in initialize();
	}

	private void createUI(final Composite parent) {

		GridLayoutFactory.fillDefaults().applyTo(parent);
		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			createUI_10_StoreLocation(container);
			createUI_20_Cleanup(container);
		}
	}

	private void createUI_10_StoreLocation(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.PrefPage_Photo_ThumbStore_Group_ThumbnailStoreLocation);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			/*
			 * field: use default location
			 */
			_editorBoolUseDefaultLocation = new BooleanFieldEditor(
					ITourbookPreferences.PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION,
					Messages.PrefPage_Photo_ThumbStore_Checkbox_UseDefaultLocation,
					group);
			_editorBoolUseDefaultLocation.setPage(this);
			_editorBoolUseDefaultLocation.setPreferenceStore(_prefStore);

			addField(_editorBoolUseDefaultLocation);

			// spacer
			new Label(group, SWT.NONE);

			_containerLocation = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_containerLocation);
			{
				/*
				 * editor: thumbnail location
				 */
				_editorDirThumbnailLocation = new DirectoryFieldEditor(
						ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION,
						Messages.PrefPage_Photo_ThumbStore_Text_Location,
						_containerLocation);

				_editorDirThumbnailLocation.setPage(this);
				_editorDirThumbnailLocation.setPreferenceStore(_prefStore);
				_editorDirThumbnailLocation.setEmptyStringAllowed(false);

				final Text textDirEditor = _editorDirThumbnailLocation.getTextControl(_containerLocation);
				textDirEditor.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(final ModifyEvent e) {
						isDataValid();
					}
				});

				addField(_editorDirThumbnailLocation);
			}
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
	}

	private void createUI_20_Cleanup(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.PrefPage_Photo_ThumbStore_Group_Cleanup);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			/*
			 * field: use default location
			 */
			_editorBoolDoCleanup = new BooleanFieldEditor(
					ITourbookPreferences.PHOTO_THUMBNAIL_STORE_IS_CLEANUP,
					Messages.PrefPage_Photo_ThumbStore_Checkbox_Cleanup,
					group);
			_editorBoolDoCleanup.setPage(this);
			_editorBoolDoCleanup.setPreferenceStore(_prefStore);
			addField(_editorBoolDoCleanup);

			createUI_30_CleanupOptions(group);
			createUI_40_CleanupNow(group);
			createUI_50_LastCleanup(group);
		}

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
	}

	private void createUI_30_CleanupOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * label: cleanup perion
			 */
			_lblCheckStore = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.indent(16, 0)
					.applyTo(_lblCheckStore);
			_lblCheckStore.setText(Messages.PrefPage_Photo_Thumbstore_Label_CleanupPeriod);

			/*
			 * spinner: cleanup perion
			 */
			_spinnerCleanupPeriod = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerCleanupPeriod);
			_spinnerCleanupPeriod.setToolTipText(Messages.PrefPage_Photo_Thumbstore_Spinner_CleanupPeriod_Tooltip);
			_spinnerCleanupPeriod.setMinimum(0);
			_spinnerCleanupPeriod.setMaximum(9999);
			_spinnerCleanupPeriod.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			/*
			 * label: days
			 */
			_lblCheckStoreDays = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblCheckStoreDays);
			_lblCheckStoreDays.setText(Messages.PrefPage_Photo_Thumbstore_Label_UnitDays);

			// #########################################################################

			/*
			 * label: keep images for x days
			 */
			_lblCleanup = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.indent(16, 0)
					.applyTo(_lblCleanup);
			_lblCleanup.setText(Messages.PrefPage_Photo_Thumbstore_Label_KeepImagesNumberOfDays);

			/*
			 * spinner: keep images for x days
			 */
			_spinnerNumberOfDaysToKeepImages = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerNumberOfDaysToKeepImages);
			_spinnerNumberOfDaysToKeepImages.setToolTipText(//
					Messages.PrefPage_Photo_Thumbstore_Spinner_KeepImagesNumberOfDays_Tooltip);
			_spinnerNumberOfDaysToKeepImages.setMinimum(0);
			_spinnerNumberOfDaysToKeepImages.setMaximum(9999);
			_spinnerNumberOfDaysToKeepImages.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
				}
			});

			/*
			 * label: days
			 */
			_lblCleanupDays = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_lblCleanupDays);
			_lblCleanupDays.setText(Messages.PrefPage_Photo_Thumbstore_Label_UnitDays);
		}

	}

	private void createUI_40_CleanupNow(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.align(SWT.BEGINNING, SWT.FILL)
				.indent(0, 20)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_btnCleanupNow = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
//					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_btnCleanupNow);
			_btnCleanupNow.setText(Messages.PrefPage_Photo_ThumbStore_Button_CleanupNow);
			_btnCleanupNow.setToolTipText(Messages.PrefPage_Photo_ThumbStore_Button_CleanupNow_Tooltip);
			_btnCleanupNow.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					doCleanupNow(false);
				}
			});

			_btnCleanupAll = new Button(container, SWT.PUSH);
			GridDataFactory.fillDefaults()//
//					.align(SWT.BEGINNING, SWT.FILL)
//					.indent(10, 0)
					.applyTo(_btnCleanupAll);
			_btnCleanupAll.setText(Messages.PrefPage_Photo_ThumbStore_Button_CleanupAll);
			_btnCleanupAll.setToolTipText(Messages.PrefPage_Photo_ThumbStore_Button_CleanupAll_Tooltip);
			_btnCleanupAll.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					doCleanupNow(true);
				}
			});
		}
	}

	private void createUI_50_LastCleanup(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * label: cleanup perion
			 */
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.FILL)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_Thumbstore_Label_LastCleanup);

			/*
			 * label: cleanup perion
			 */
			_lblLastCleanup = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.FILL, SWT.FILL)
					.grab(true, false)
					.applyTo(_lblLastCleanup);
		}
	}

	private void doCleanupNow(final boolean isDeleteAllImages) {

		// update pref store values
		if (performOk()) {

			PhotoManager.stopImageLoading(true);

			// remove store files
			ThumbnailStore.cleanupStoreFiles(isDeleteAllImages, true);

			// dispose cached images
			PhotoImageCache.dispose();

			updateUILastCleanup();
		}
	}

	private void enableControls() {

		/*
		 * location
		 */
		final boolean useDefaultLocation = _editorBoolUseDefaultLocation.getBooleanValue();

		if (useDefaultLocation) {
			_editorDirThumbnailLocation.setEnabled(false, _containerLocation);
			_editorDirThumbnailLocation.setStringValue(_defaultThumbnailStorePath);
		} else {
			_editorDirThumbnailLocation.setEnabled(true, _containerLocation);
		}

		/*
		 * cleanup
		 */
		final boolean isCleanup = _editorBoolDoCleanup.getBooleanValue();

		_spinnerNumberOfDaysToKeepImages.setEnabled(isCleanup);
		_lblCleanup.setEnabled(isCleanup);
		_lblCleanupDays.setEnabled(isCleanup);

		_spinnerCleanupPeriod.setEnabled(isCleanup);
		_lblCheckStore.setEnabled(isCleanup);
		_lblCheckStoreDays.setEnabled(isCleanup);
	}

	private void enableValidControls(final boolean isValid) {

		_btnCleanupAll.setEnabled(isValid);
		_btnCleanupNow.setEnabled(isValid);
	}

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
			public void propertyChange(final PropertyChangeEvent event) {
				enableControls();
			}
		};
		_editorBoolUseDefaultLocation.setPropertyChangeListener(propListener);
		_editorBoolDoCleanup.setPropertyChangeListener(propListener);

		restoreState();

		/*
		 * hide error messages, it appears when the thumbnail location is invalid
		 */
		if (_editorBoolUseDefaultLocation.getBooleanValue() == false) {
			setErrorMessage(null);
		}
	}

	private boolean isDataValid() {

		boolean isValid = true;

		if (_editorBoolUseDefaultLocation.getBooleanValue() == false) {

			// use custom location

			final String customFilePath = _editorDirThumbnailLocation.getStringValue();

			if (Util.isDirectory(customFilePath) == false) {

				isValid = false;

				setErrorMessage(Messages.PrefPage_Photo_ThumbStore_Error_Location);
				_editorDirThumbnailLocation.setFocus();
			}
		}

		if (isValid) {
			setErrorMessage(null);
		}

		setValid(isValid);
		enableValidControls(isValid);

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

		_spinnerNumberOfDaysToKeepImages.setSelection(_prefStore.getDefaultInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES));

		_spinnerCleanupPeriod.setSelection(_prefStore.getDefaultInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD));

		enableControls();
	}

	@Override
	public boolean performOk() {

		if (_editorBoolUseDefaultLocation == null) {
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

		_spinnerNumberOfDaysToKeepImages.setSelection(_prefStore.getInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES));
		_spinnerCleanupPeriod.setSelection(_prefStore.getInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD));

		updateUILastCleanup();

		enableControls();
	}

	private void saveState() {

		_prefStore.setValue(
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES,
				_spinnerNumberOfDaysToKeepImages.getSelection());

		_prefStore.setValue(
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD,
				_spinnerCleanupPeriod.getSelection());

		ThumbnailStore.updateStoreLocation();
	}

	private void updateUILastCleanup() {

		final long lastCleanup = _prefStore.getLong(//
				ITourbookPreferences.PHOTO_THUMBNAIL_STORE_LAST_CLEANUP_DATE_TIME);

		_lblLastCleanup.setText(lastCleanup == 0 ? UI.EMPTY_STRING : _dateFormatter.print(lastCleanup));
	}
}
