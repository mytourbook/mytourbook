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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoImageCache extends PreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private Spinner				_spinnerNumberOfImages;
	private Spinner				_spinnerNumberOfOriginalImages;

	@Override
	protected Control createContents(final Composite parent) {

		_prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		GridDataFactory.swtDefaults().applyTo(container);
		{
			createUI_20_ImageCache(container);
			createUI_30_OriginalImageCache(container);
		}

		return container;
	}

	private void createUI_20_ImageCache(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_Photo_Cache_Group_ThumbnailCache);
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

	private void createUI_30_OriginalImageCache(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_Photo_Cache_Group_OriginalImageCacheSize);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * label: nof images
			 */
			Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults()//
					.align(SWT.BEGINNING, SWT.CENTER)
					.applyTo(label);
			label.setText(Messages.PrefPage_Photo_Cache_Label_NumberOfOriginalImages);

			/*
			 * spinner: nof original images
			 */
			_spinnerNumberOfOriginalImages = new Spinner(group, SWT.BORDER);
			GridDataFactory.fillDefaults() //
					.align(SWT.BEGINNING, SWT.FILL)
					.applyTo(_spinnerNumberOfOriginalImages);
			_spinnerNumberOfOriginalImages.setMinimum(1);
			_spinnerNumberOfOriginalImages.setMaximum(50);
			_spinnerNumberOfOriginalImages.addMouseWheelListener(new MouseWheelListener() {
				public void mouseScrolled(final MouseEvent event) {
					UI.adjustSpinnerValueOnMouseScroll(event);
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
			label.setText(Messages.PrefPage_Photo_Cache_Label_OriginalCacheSizeInfo);
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return _prefStore;
	}

	public void init(final IWorkbench workbench) {}

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

					MessageDialog.openInformation(
							getShell(),
							Messages.PrefPage_Photo_Cache_Dialog_MaxHandle_Title,
							message);
				}
			}
		});
	}

	@Override
	protected void performDefaults() {

		_spinnerNumberOfImages.setSelection(_prefStore.getDefaultInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE));

		_spinnerNumberOfOriginalImages.setSelection(_prefStore.getDefaultInt(//
				ITourbookPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE));

		super.performDefaults();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		_spinnerNumberOfImages.setSelection(_prefStore.getInt(//
				ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE));

		_spinnerNumberOfOriginalImages.setSelection(_prefStore.getInt(//
				ITourbookPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE));
	}

	private void saveState() {

		/*
		 * set thumb cache size
		 */
		int newCacheSize = _spinnerNumberOfImages.getSelection();
		int oldCacheSize = _prefStore.getInt(ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE);

		if (oldCacheSize != newCacheSize) {
			_prefStore.setValue(ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE, newCacheSize);
			PhotoImageCache.setThumbCacheSize(newCacheSize);
		}

		/*
		 * set original image cache size
		 */
		newCacheSize = _spinnerNumberOfOriginalImages.getSelection();
		oldCacheSize = _prefStore.getInt(ITourbookPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE);

		if (oldCacheSize != newCacheSize) {
			_prefStore.setValue(ITourbookPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE, newCacheSize);
			PhotoImageCache.setOriginalImageCacheSize(newCacheSize);
		}

	}

}
