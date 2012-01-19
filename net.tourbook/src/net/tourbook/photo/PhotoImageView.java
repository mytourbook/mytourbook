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

import java.io.File;

import net.tourbook.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class PhotoImageView extends ViewPart {

	static public final String	ID	= "net.tourbook.photo.photoImageView";	//$NON-NLS-1$

	private ISelectionListener	_postSelectionListener;

	/*
	 * UI controls
	 */
	private PageBook			_pageBook;
	private Label				_pageNoImage;
	private Composite			_pageImageContainer;

	private PhotoCanvas			_photoCanvas;

	private ImageLoader			_imageLoader;

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				if (part == PhotoImageView.this) {
					return;
				}
				onSelectionChanged(selection);
			}
		};
		getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		addSelectionListener();

		// show default page
		_pageBook.showPage(_pageNoImage);

		// show marker from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);

		_pageNoImage = new Label(_pageBook, SWT.NONE);
		_pageNoImage.setText(Messages.Photo_View_Label_AnImageIsNotSelected);

		_pageImageContainer = new Composite(_pageBook, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(_pageImageContainer);
		_pageImageContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			_photoCanvas = new PhotoCanvas(_pageImageContainer, SWT.NO_BACKGROUND);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_photoCanvas);
		}

		_imageLoader = new ImageLoader();
	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getViewSite().getPage();

		page.removePostSelectionListener(_postSelectionListener);

		super.dispose();
	}

	private void onSelectionChanged(final ISelection selection) {

		if (selection instanceof IStructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();

			if (firstElement instanceof Photo) {
				final Photo photo = (Photo) firstElement;

				final File imageFile = photo.getImageFile();

				final Image photoImage = new Image(_pageBook.getDisplay(), imageFile.getAbsolutePath());

				if (photo.getWidth() == Integer.MIN_VALUE) {

					// images size is not yet set

					final Rectangle imageSize = photoImage.getBounds();

					photo.setSize(imageSize.width, imageSize.height);
				}

				_photoCanvas.setImage(photoImage, photo);

				_pageBook.showPage(_pageImageContainer);
			}
		}
	}

	@Override
	public void setFocus() {}

}
