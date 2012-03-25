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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.imgscalr.Scalr.Method;

public class PrefPagePhotoViewer extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPagePhotoViewerID"; //$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private boolean					_isEditorModified;

	/*
	 * UI controls
	 */
	private Composite				_containerFileFolder;

	private BooleanFieldEditor2		_chkEditorIsShowFileFolder;
	private ColorFieldEditor		_colorEditorFolder;
	private ColorFieldEditor		_colorEditorFile;
	private FileFieldEditor			_editorExternalPhotoViewer;

	private Composite				_containerDisplayImageQuality;
	private BooleanFieldEditor2		_chkEditorIsHighImageQuality;
	private Label					_lblThumbSize;
	private Label					_lblThumbSizeUnit;
	private Spinner					_spinnerThumbSize;

	private Composite				_containerResizeImageQuality;
	private Label					_lblResizeImageQuality;
	private Combo					_comboResizeQuality;

	@Override
	protected void createFieldEditors() {

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

			_containerFileFolder = new Composite(colorGroup, SWT.NONE);
			{
				/*
				 * checkbox: show file/folder number
				 */
				_chkEditorIsShowFileFolder = new BooleanFieldEditor2(
						ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER,
						Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView,
						_containerFileFolder);
				addField(_chkEditorIsShowFileFolder);

				final Button editorControl = _chkEditorIsShowFileFolder.getChangeControl(_containerFileFolder);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(editorControl);
				editorControl.setToolTipText(//
						Messages.PrefPage_Photo_Viewer_Checkbox_ShowNumbersInFolderView_Tooltip);
				{
					/*
					 * color: folder
					 */
					_colorEditorFolder = new ColorFieldEditor(
							ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER,
							Messages.PrefPage_Photo_Viewer_Label_FolderColor,
							_containerFileFolder);
					addField(_colorEditorFolder);

					// indent label
					Label labelControl = _colorEditorFolder.getLabelControl(_containerFileFolder);
					GridData gd = (GridData) labelControl.getLayoutData();
					gd.horizontalIndent = 16;

					/*
					 * color: file
					 */
					_colorEditorFile = new ColorFieldEditor(
							ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE,
							Messages.PrefPage_Photo_Viewer_Label_FileColor,
							_containerFileFolder);
					addField(_colorEditorFile);

					// indent label
					labelControl = _colorEditorFile.getLabelControl(_containerFileFolder);
					gd = (GridData) labelControl.getLayoutData();
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
			createUI_21_ResizeImageQuality(group);
			createUI_22_DisplayImageQuality(group);
		}
	}

	private void createUI_21_ResizeImageQuality(final Composite parent) {

		_containerResizeImageQuality = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerResizeImageQuality);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_containerResizeImageQuality);
		{
			/*
			 * label: load image quality
			 */
			_lblResizeImageQuality = new Label(_containerResizeImageQuality, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblResizeImageQuality);
			_lblResizeImageQuality.setText(Messages.PrefPage_Photo_Viewer_Label_ResizeImageQuality);
			_lblResizeImageQuality.setToolTipText(Messages.PrefPage_Photo_Viewer_Label_ResizeImageQuality_Tooltip);

			_comboResizeQuality = new Combo(_containerResizeImageQuality, SWT.READ_ONLY | SWT.DROP_DOWN);
			GridDataFactory.fillDefaults().applyTo(_comboResizeQuality);
			_comboResizeQuality.setVisibleItemCount(20);
			_comboResizeQuality.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_isEditorModified = true;
				}
			});

			// fill combobox
			for (final String quality : PhotoManager.SCALING_QUALITY_TEXT) {
				_comboResizeQuality.add(quality);
			}
		}
	}

	private void createUI_22_DisplayImageQuality(final Group group) {

		_containerDisplayImageQuality = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_containerDisplayImageQuality);
//			_containerImageQuality.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{

			/*
			 * checkbox: enable/disable high quality
			 */
			_chkEditorIsHighImageQuality = new BooleanFieldEditor2(
					ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY,
					Messages.PrefPage_Photo_Viewer_Checkbox_ShowHighQuality,
					_containerDisplayImageQuality);
			addField(_chkEditorIsHighImageQuality);

			final Button editorControl = _chkEditorIsHighImageQuality.getChangeControl(_containerDisplayImageQuality);
			GridDataFactory.fillDefaults()//
					.span(3, 1)
					.applyTo(editorControl);
			editorControl.setToolTipText(//
					Messages.PrefPage_Photo_Viewer_Checkbox_ShowHighQuality_Tooltip);

			/*
			 * label: thumbnail size
			 */
			_lblThumbSize = new Label(_containerDisplayImageQuality, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.indent(16, 0)
					.applyTo(_lblThumbSize);
			_lblThumbSize.setText(Messages.PrefPage_Photo_Viewer_Label_HQThumbnailSize);
			_lblThumbSize.setToolTipText(Messages.PrefPage_Photo_Viewer_Label_HQThumbnailSize_Tooltip);

			/*
			 * spinner: thumbnail size
			 */
			_spinnerThumbSize = new Spinner(_containerDisplayImageQuality, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerThumbSize);
			_spinnerThumbSize.setMinimum(PicDirImages.MIN_ITEM_HEIGHT);
			_spinnerThumbSize.setMaximum(PicDirImages.MAX_ITEM_HEIGHT);
			_spinnerThumbSize.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					_isEditorModified = true;
				}
			});
			_spinnerThumbSize.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
					_isEditorModified = true;
				}
			});
			/*
			 * label: thumbnail size
			 */
			_lblThumbSizeUnit = new Label(_containerDisplayImageQuality, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(_lblThumbSizeUnit);
			_lblThumbSizeUnit.setText(Messages.PrefPage_Photo_Viewer_Label_HQThumbnailSizeUnit);
		}

		// set layout after the fields are created
		GridLayoutFactory.fillDefaults() //
				.numColumns(3)
				.spacing(5, 0)
				.applyTo(_containerDisplayImageQuality);
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

		final boolean isShowFileFolder = _chkEditorIsShowFileFolder.getChangeControl(_containerFileFolder) //
				.getSelection();

		_colorEditorFile.getColorSelector().setEnabled(isShowFileFolder);
		_colorEditorFile.getLabelControl(_containerFileFolder).setEnabled(isShowFileFolder);

		_colorEditorFolder.getColorSelector().setEnabled(isShowFileFolder);
		_colorEditorFolder.getLabelControl(_containerFileFolder).setEnabled(isShowFileFolder);

		final boolean isHighQuality = _chkEditorIsHighImageQuality.getChangeControl(//
				_containerDisplayImageQuality).getSelection();

		_lblThumbSize.setEnabled(isHighQuality);
		_lblThumbSizeUnit.setEnabled(isHighQuality);
		_spinnerThumbSize.setEnabled(isHighQuality);
	}

	private void fireModifyEvent() {

		if (_isEditorModified) {

			_isEditorModified = false;

			UI.setPhotoColorsFromPrefStore();

			// fire one event for all modified values
			getPreferenceStore().setValue(ITourbookPreferences.PHOTO_VIEWER_PREF_STORE_EVENT, Math.random());
		}
	}

	private String getResizeQualityId() {

		int selectedIndex = _comboResizeQuality.getSelectionIndex();
		if (selectedIndex == -1 || selectedIndex >= PhotoManager.SCALING_QUALITY_ID.length) {
			// ensure valid selection
			selectedIndex = 0;
		}

		final Method selectedQualityId = PhotoManager.SCALING_QUALITY_ID[selectedIndex];

		return selectedQualityId.name();
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

	@Override
	protected void performDefaults() {

		_isEditorModified = true;

		_spinnerThumbSize.setSelection(//
				_prefStore.getDefaultInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

		setResizeQuality(_prefStore.getDefaultString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_RESIZE_QUALITY));

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

		final Object sourceEditor = event.getSource();

		if (sourceEditor == _chkEditorIsShowFileFolder || sourceEditor == _chkEditorIsHighImageQuality) {
			enableControls();
		}

		super.propertyChange(event);
	}

	private void restoreState() {

		_spinnerThumbSize.setSelection(//
				_prefStore.getInt(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE));

		setResizeQuality(_prefStore.getString(ITourbookPreferences.PHOTO_VIEWER_IMAGE_RESIZE_QUALITY));
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE, //
				_spinnerThumbSize.getSelection());

		_prefStore.setValue(ITourbookPreferences.PHOTO_VIEWER_IMAGE_RESIZE_QUALITY, //
				getResizeQualityId());
	}

	private void setResizeQuality(final String requestedResizeQualityId) {

		/*
		 * get combo index for a default id
		 */
		Method resizeQualityId = Method.SPEED;
		int comboIndex = 0;
		int qualityIndex = 0;
		for (final Method availableQuality : PhotoManager.SCALING_QUALITY_ID) {
			if (availableQuality.name().equals(resizeQualityId)) {
				resizeQualityId = availableQuality;
				comboIndex = qualityIndex;
				break;
			}

			qualityIndex++;
		}

		/*
		 * get combo index for requested id
		 */
		qualityIndex = 0;
		for (final Method quality : PhotoManager.SCALING_QUALITY_ID) {
			if (quality.name().equals(requestedResizeQualityId)) {
				resizeQualityId = quality;
				comboIndex = qualityIndex;
				break;
			}

			qualityIndex++;
		}

		_comboResizeQuality.select(comboIndex);
	}
}
