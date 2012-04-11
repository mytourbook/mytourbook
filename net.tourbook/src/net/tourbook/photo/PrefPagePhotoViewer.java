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
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoViewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPagePhotoViewerID"; //$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isEditorModified;

	private SelectionAdapter		_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private Button					_chkIsShowFileFolder;
	private ColorFieldEditor		_colorEditorFolder;
	private ColorFieldEditor		_colorEditorFile;
	private Label					_lblFileColor;
	private Label					_lblFolderColor;
	private FileFieldEditor			_editorExternalPhotoViewer;

	private Button					_rdoImageSystemSWT;
	private Button					_rdoImageSystemAWT;
	private Button					_chkIsHighImageQuality;
	private Combo					_comboHQImageSize;
	private Label					_lblThumbSize;
	private Label					_lblThumbSizeUnit;
	private Spinner					_spinnerThumbSize;

	@Override
	protected void createFieldEditors() {

		initUI();
		createUI();
	}

	private void createUI() {

		final Composite parent = getFieldEditorParent();
		{
			GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
			GridLayoutFactory.fillDefaults().applyTo(parent);

			createUI_10_Colors(parent);
			createUI_20_ImageQuality(parent);
			createUI_30_ExternalPhotoViewer(parent);
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
				addField(new ColorFieldEditor(ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
						Messages.PrefPage_Photo_Viewer_Label_BackgroundColor,
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
				_chkIsShowFileFolder.addSelectionListener(_defaultSelectionListener);
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
			_rdoImageSystemSWT.addSelectionListener(_defaultSelectionListener);

			_rdoImageSystemAWT = new Button(container, SWT.RADIO);
			_rdoImageSystemAWT.setText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_AWT);
			_rdoImageSystemAWT.setToolTipText(Messages.PrefPage_Photo_Viewer_Radio_ImageFramework_AWT_Tooltip);
			_rdoImageSystemAWT.addSelectionListener(_defaultSelectionListener);
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
			_comboHQImageSize.addSelectionListener(_defaultSelectionListener);

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
		_chkIsHighImageQuality.addSelectionListener(_defaultSelectionListener);

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

			_spinnerThumbSize.addSelectionListener(_defaultSelectionListener);
			_spinnerThumbSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					_isEditorModified = true;
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
//
//		// set layout after the fields are created
//		GridLayoutFactory.fillDefaults() //
//				.numColumns(3)
//				.spacing(5, 0)
//				.applyTo(container);
	}

	/**
	 * field: path to save raw tour data
	 */
	private void createUI_30_ExternalPhotoViewer(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(group);
		group.setText(Messages.PrefPage_Photo_ExtViewer_Group_ExternalApplication);
		{
			/*
			 * label: info
			 */
			Label label = new Label(group, SWT.WRAP);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.indent(0, 5)
					.hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_ExtViewer_Label_Info);

			/*
			 * editor: external file browser
			 */
			_editorExternalPhotoViewer = new FileFieldEditor(
					ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER,
					Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication,
					group);
			_editorExternalPhotoViewer.setEmptyStringAllowed(true);
			_editorExternalPhotoViewer.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

			label = _editorExternalPhotoViewer.getLabelControl(group);
			label.setToolTipText(Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip);

			addField(_editorExternalPhotoViewer);
		}

		// set layout after the fields are created
		GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 0, 0).applyTo(group);

		/*
		 * set width for the text control that the pref dialog is not as wide as the full path
		 */
		final Text rawPathControl = _editorExternalPhotoViewer.getTextControl(group);
		final GridData gd = (GridData) rawPathControl.getLayoutData();
		gd.widthHint = 200;
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

		if (_isEditorModified) {

			_isEditorModified = false;

			UI.setPhotoColorsFromPrefStore();

			final String imageFramework = _prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK);
			final int hqImageSize = PhotoManager.HQ_IMAGE_SIZES[_comboHQImageSize.getSelectionIndex()];
			PhotoManager.setFromPrefStore(imageFramework, hqImageSize);

			// fire one event for all modified values
			getPreferenceStore().setValue(ITourbookPreferences.PHOTO_VIEWER_PREF_STORE_EVENT, Math.random());
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

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify();
			}
		};
	}

	@Override
	public boolean okToLeave() {

		if (_isEditorModified) {

			saveState();

			// save the colors in the pref store
			super.performOk();

			fireModifyEvent();
		}

		return super.okToLeave();
	}

	private void onModify() {

		_isEditorModified = true;

		enableControls();
	}

	@Override
	protected void performDefaults() {

		_isEditorModified = true;

		_chkIsHighImageQuality.setSelection(//
				_prefStore.getDefaultBoolean(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY));

		_spinnerThumbSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

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

		enableControls();
	}

	@Override
	public boolean performOk() {

		saveState();

		final boolean isOK = super.performOk();

		if (isOK) {
			fireModifyEvent();
		}

		return isOK;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent event) {

		_isEditorModified = true;

		super.propertyChange(event);
	}

	private void restoreState() {

		_chkIsHighImageQuality.setSelection(//
				_prefStore.getBoolean(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY));

		_spinnerThumbSize.setSelection(//
				_prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

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

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE, //
				PhotoManager.HQ_IMAGE_SIZES[_comboHQImageSize.getSelectionIndex()]);
	}

}
