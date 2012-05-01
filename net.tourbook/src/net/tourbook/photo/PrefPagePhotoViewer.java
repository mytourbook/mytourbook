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
import net.tourbook.photo.manager.PhotoManager;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.BooleanFieldEditor2;
import net.tourbook.ui.UI;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoViewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPagePhotoViewerID"; //$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isImageViewerUIModified;
	private boolean					_isImageQualityModified;

	private SelectionAdapter		_viewerUISelectionListener;
	private SelectionAdapter		_imageQualitySelectionListener;

	/*
	 * UI controls
	 */
	private Button					_chkIsShowFileFolder;
	private ColorFieldEditor		_colorEditorFolder;
	private ColorFieldEditor		_colorEditorFile;
	private Label					_lblFileColor;
	private Label					_lblFolderColor;

	private Button					_rdoImageSystemSWT;
	private Button					_rdoImageSystemAWT;
	private Button					_chkIsHighImageQuality;
	private Spinner					_spinnerTextMinThumbSize;
	private Combo					_comboHQImageSize;
	private Label					_lblThumbSize;
	private Label					_lblThumbSizeUnit;
	private Spinner					_spinnerThumbSize;

	private FontFieldEditor			_galleryFontEditor;

	private class FileFieldEditorNoValidation extends FileFieldEditor {

		public FileFieldEditorNoValidation(final String name, final String labelText, final Composite parent) {
			super(name, labelText, parent);
		}

		@Override
		protected boolean checkState() {
			return true;
		}
	}

	@Override
	protected void createFieldEditors() {

		initUI();
		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
		GridLayoutFactory.fillDefaults().applyTo(parent);
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
			createUI_10_Colors(parent);
			createUI_12_Gallery(parent);
			createUI_20_ImageQuality(parent);
		}
	}

	private void createUI_10_Colors(final Composite parent) {

		final Group colorGroup = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(colorGroup);
		GridLayoutFactory.fillDefaults()//
				.margins(5, 5)
				.spacing(30, LayoutConstants.getSpacing().y)
				.numColumns(2)
				.applyTo(colorGroup);
		colorGroup.setText(Messages.PrefPage_Photo_Viewer_Group_Colors);
		{
			final Composite containerLeft = new Composite(colorGroup, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(containerLeft);
			{
				// color: foreground
				addField(new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND,
						Messages.PrefPage_Photo_Viewer_Label_ForgroundColor,
						containerLeft));

				// color: background
				addField(new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND,
						Messages.PrefPage_Photo_Viewer_Label_BackgroundColor,
						containerLeft));

				// color: selection forground
				addField(new ColorFieldEditor(
						ITourbookPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND,
						Messages.PrefPage_Photo_Viewer_Label_SelectionForegroundColor,
						containerLeft));
			}

			final Composite containerFileFolder = new Composite(colorGroup, SWT.NONE);
			{
				/*
				 * checkbox: show file/folder number
				 */
				final BooleanFieldEditor2 chkEditorIsShowFileFolder = new BooleanFieldEditor2(
						ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER,
						Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView,
						containerFileFolder);
				addField(chkEditorIsShowFileFolder);

				_chkIsShowFileFolder = chkEditorIsShowFileFolder.getChangeControl(containerFileFolder);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(_chkIsShowFileFolder);
				_chkIsShowFileFolder.setToolTipText(//
						Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView_Tooltip);
				_chkIsShowFileFolder.addSelectionListener(_viewerUISelectionListener);
				{
					/*
					 * color: folder
					 */
					_colorEditorFolder = new ColorFieldEditor(
							ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER,
							Messages.PrefPage_Photo_Viewer_Label_FolderColor,
							containerFileFolder);
					addField(_colorEditorFolder);

					// indent label
					_lblFolderColor = _colorEditorFolder.getLabelControl(containerFileFolder);
					GridData gd = (GridData) _lblFolderColor.getLayoutData();
					gd.horizontalIndent = 16;

					/*
					 * color: file
					 */
					_colorEditorFile = new ColorFieldEditor(
							ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE,
							Messages.PrefPage_Photo_Viewer_Label_FileColor,
							containerFileFolder);
					addField(_colorEditorFile);

					// indent label
					_lblFileColor = _colorEditorFile.getLabelControl(containerFileFolder);
					gd = (GridData) _lblFileColor.getLayoutData();
					gd.horizontalIndent = 16;
				}
			}
		}
	}

	private void createUI_12_Gallery(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		group.setText(Messages.PrefPage_Photo_Viewer_Group_PhotoGallery);
		{
			/*
			 * text min size
			 */
			createUI_14_TextMinSize(group);

			/*
			 * gallery font
			 */
			_galleryFontEditor = new FontFieldEditor(ITourbookPreferences.PHOTO_VIEWER_FONT, //
					Messages.PrefPage_Photo_Viewer_Label_FontEditor,
					Messages.PrefPage_Photo_Viewer_Label_FontExample,
					group);
			_galleryFontEditor.setPreferenceStore(_prefStore);
			_galleryFontEditor.setPage(this);
			_galleryFontEditor.load();
			_galleryFontEditor.setPropertyChangeListener(this);
		}

		// force 2 columns
		final GridLayout gl = (GridLayout) group.getLayout();
		gl.numColumns = 2;
		gl.marginWidth = 5;
		gl.marginHeight = 5;
		gl.makeColumnsEqualWidth = true;
	}

	private void createUI_14_TextMinSize(final Composite parent) {

		/*
		 * label: display text when thumbnail min size
		 */
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.PrefPage_Photo_Viewer_Label_TextMinThumbSize);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * spinner: thumbnail size
			 */
			_spinnerTextMinThumbSize = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerTextMinThumbSize);
			_spinnerTextMinThumbSize.setMinimum(PicDirImages.MIN_ITEM_WIDTH);
			_spinnerTextMinThumbSize.setMaximum(PicDirImages.MAX_ITEM_WIDTH);
			_spinnerTextMinThumbSize.setToolTipText(Messages.PrefPage_Photo_Viewer_Label_TextMinThumbSize_Tooltip);

			_spinnerTextMinThumbSize.addSelectionListener(_viewerUISelectionListener);
			_spinnerTextMinThumbSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					_isImageViewerUIModified = true;
				}
			});

			/*
			 * label: unit
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.App_Unit_Pixel);
		}
	}

	private void createUI_20_ImageQuality(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		group.setText(Messages.PrefPage_Photo_ExtViewer_Group_ImageQuality);
		{
			createUI_22_ImageFramework(group);
			createUI_24_HQImageSize(group);
			createUI_26_DisplayImageQuality(group);
		}

		// set group margin after the fields are created
		final GridLayout gl = (GridLayout) group.getLayout();
		gl.numColumns = 2;
		gl.marginHeight = 5;
		gl.marginWidth = 5;
	}

	private void createUI_22_ImageFramework(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.PrefPage_Photo_Viewer_Label_ImageFramework);

		// radio
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			_rdoImageSystemSWT = new Button(container, SWT.RADIO);
			_rdoImageSystemSWT.setText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_SWT);
			_rdoImageSystemSWT.setToolTipText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_SWT_Tooltip);
			_rdoImageSystemSWT.addSelectionListener(_imageQualitySelectionListener);

			_rdoImageSystemAWT = new Button(container, SWT.RADIO);
			_rdoImageSystemAWT.setText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_AWT);
			_rdoImageSystemAWT.setToolTipText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_AWT_Tooltip);
			_rdoImageSystemAWT.addSelectionListener(_imageQualitySelectionListener);
		}

	}

	private void createUI_24_HQImageSize(final Composite parent) {

		/*
		 * label: image size
		 */
		Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.applyTo(label);
		label.setText(Messages.PrefPage_Photo_Viewer_Label_HQImageSize);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		{
			/*
			 * combo: hq image size
			 */
			_comboHQImageSize = new Combo(container, SWT.READ_ONLY | SWT.DROP_DOWN);
			GridDataFactory.fillDefaults().align(SWT.END, SWT.CENTER).applyTo(_comboHQImageSize);
			_comboHQImageSize.setVisibleItemCount(20);
			_comboHQImageSize.setToolTipText(Messages.PrefPage_Photo_Viewer_Label_HQImageSize_Tooltip);
			_comboHQImageSize.addSelectionListener(_imageQualitySelectionListener);

			// fill combobox
			for (final int hqImageSize : PhotoManager.HQ_IMAGE_SIZES) {
				_comboHQImageSize.add(Integer.toString(hqImageSize));
			}

			/*
			 * label: image size unit
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.App_Unit_Pixel);
		}
	}

	private void createUI_26_DisplayImageQuality(final Composite parent) {

		/*
		 * checkbox: enable/disable high quality
		 */
		_chkIsHighImageQuality = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsHighImageQuality);
		_chkIsHighImageQuality.setText(Messages.PrefPage_Photo_Viewer_Checkbox_ShowHighQuality);
		_chkIsHighImageQuality.setToolTipText(Messages.PrefPage_Photo_Viewer_Checkbox_ShowHighQuality_Tooltip);
		_chkIsHighImageQuality.addSelectionListener(_viewerUISelectionListener);

		/*
		 * label: thumbnail size
		 */
		_lblThumbSize = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.align(SWT.BEGINNING, SWT.CENTER)
				.indent(16, 0)
				.applyTo(_lblThumbSize);
		_lblThumbSize.setText(Messages.PrefPage_Photo_Viewer_Label_HQThumbnailSize);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			/*
			 * spinner: thumbnail size
			 */
			_spinnerThumbSize = new Spinner(container, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerThumbSize);
			_spinnerThumbSize.setMinimum(PicDirImages.MIN_ITEM_WIDTH);
			_spinnerThumbSize.setMaximum(PicDirImages.MAX_ITEM_WIDTH);
			_spinnerThumbSize.setToolTipText(Messages.PrefPage_Photo_Viewer_Label_HQThumbnailSize_Tooltip);

			_spinnerThumbSize.addSelectionListener(_viewerUISelectionListener);
			_spinnerThumbSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					_isImageViewerUIModified = true;
				}
			});

			/*
			 * label: unit
			 */
			_lblThumbSizeUnit = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblThumbSizeUnit);
			_lblThumbSizeUnit.setText(Messages.App_Unit_Pixel);
		}
	}

	private void enableControls() {

		final boolean isShowFileFolder = _chkIsShowFileFolder.getSelection();

		_colorEditorFile.getColorSelector().setEnabled(isShowFileFolder);
		_lblFileColor.setEnabled(isShowFileFolder);

		_colorEditorFolder.getColorSelector().setEnabled(isShowFileFolder);
		_lblFolderColor.setEnabled(isShowFileFolder);

		final boolean isHighQuality = _chkIsHighImageQuality.getSelection();

		_lblThumbSize.setEnabled(isHighQuality);
		_lblThumbSizeUnit.setEnabled(isHighQuality);
		_spinnerThumbSize.setEnabled(isHighQuality);
	}

	private void fireModifyEvent() {

		if (_isImageQualityModified) {

			_isImageQualityModified = false;

			final String imageFramework = _prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK);
			final int hqImageSize = PhotoManager.HQ_IMAGE_SIZES[_comboHQImageSize.getSelectionIndex()];
			PhotoManager.setFromPrefStore(imageFramework, hqImageSize);

			getPreferenceStore().setValue(
					ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_QUALITY_IS_MODIFIED,
					Math.random());
		}

		if (_isImageViewerUIModified) {

			_isImageViewerUIModified = false;

			UI.setPhotoColorsFromPrefStore();

			// fire one event for all modified values
			getPreferenceStore().setValue(
					ITourbookPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED,
					Math.random());
		}
	}

	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	@Override
	protected void initialize() {

		super.initialize();

		restoreState();

		enableControls();
	}

	private void initUI() {

		_viewerUISelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				_isImageViewerUIModified = true;

				enableControls();
			}
		};

		_imageQualitySelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				_isImageQualityModified = true;
			}
		};
	}

	@Override
	public boolean okToLeave() {

		if (_isImageQualityModified) {

			saveState();

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	@Override
	protected void performDefaults() {

		_isImageQualityModified = true;
		_isImageViewerUIModified = true;

		_chkIsHighImageQuality.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY));

		_spinnerThumbSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

		_spinnerTextMinThumbSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE));

		/*
		 * image framework
		 */
		final boolean isSWT = _prefStore.getDefaultString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK)//
				.equals(PhotoManager.IMAGE_FRAMEWORK_SWT);
		_rdoImageSystemSWT.setSelection(isSWT);
		_rdoImageSystemAWT.setSelection(!isSWT);

		/*
		 * hq image size
		 */
		final int hqImageSize = _prefStore.getDefaultInt(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE);
		final int hqImageSizeIndex = PhotoManager.getHQImageSizeIndex(hqImageSize);
		_comboHQImageSize.select(hqImageSizeIndex);

		// set editor defaults
		super.performDefaults();

		// must be set here, is not set in super.performDefaults() on OSX
		_galleryFontEditor.loadDefault();

		enableControls();
	}

	@Override
	public boolean performOk() {

		saveState();

		_galleryFontEditor.store();

		// store editor fields
		final boolean isOK = super.performOk();

		if (isOK) {
			fireModifyEvent();
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

//		final String property = event.getProperty();

		final Object source = event.getSource();
		if (source instanceof FieldEditor) {

			final String prefName = ((FieldEditor) source).getPreferenceName();

			if (prefName.equals(ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE)
					|| prefName.equals(ITourbookPreferences.PHOTO_VIEWER_FONT)
			//
			) {

				_isImageViewerUIModified = true;
			}
		}

/////// show selected font as text
//		System.out.println(event.getNewValue());
//		// TODO remove SYSTEM.OUT.PRINTLN

		super.propertyChange(event);
	}

	private void restoreState() {

		_chkIsHighImageQuality.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY));

		_spinnerThumbSize.setSelection(//
				_prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

		_spinnerTextMinThumbSize.setSelection(//
				_prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE));

		/*
		 * image framework
		 */
		final String imageFramework = _prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK);
		final boolean isSWT = imageFramework.equals(PhotoManager.IMAGE_FRAMEWORK_SWT);
		_rdoImageSystemSWT.setSelection(isSWT);
		_rdoImageSystemAWT.setSelection(!isSWT);

		/*
		 * HQ image size
		 */
		final int hqImageSize = _prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE);
		final int hqImageSizeIndex = PhotoManager.getHQImageSizeIndex(hqImageSize);
		_comboHQImageSize.select(hqImageSizeIndex);
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK, _rdoImageSystemSWT.getSelection()
				? PhotoManager.IMAGE_FRAMEWORK_SWT
				: PhotoManager.IMAGE_FRAMEWORK_AWT);

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY, //
				_chkIsHighImageQuality.getSelection());

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE, //
				_spinnerThumbSize.getSelection());

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE, //
				_spinnerTextMinThumbSize.getSelection());

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE, //
				PhotoManager.HQ_IMAGE_SIZES[_comboHQImageSize.getSelectionIndex()]);
	}

}
