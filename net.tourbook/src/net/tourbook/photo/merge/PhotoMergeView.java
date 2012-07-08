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
package net.tourbook.photo.merge;

import java.util.ArrayList;
import java.util.Collection;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.photo.gallery.MT20.GalleryMT20Item;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class PhotoMergeView extends ViewPart {

	static public final String				ID			= "net.tourbook.photo.merge.PhotoMergeView.ID";		//$NON-NLS-1$

	private static final IDialogSettings	_state		= TourbookPlugin.getDefault()//
																.getDialogSettingsSection("PhotoMergeView");	//$NON-NLS-1$

	private static final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault()//
																.getPreferenceStore();

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;

	private PostSelectionProvider			_postSelectionProvider;

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoMergeView.this) {}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoMergeView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoMergeView.this) {}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == PhotoMergeView.this) {}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {


			}
		};

		_prefStore.addPropertyChangeListener(_prefChangeListener);
	}

	private void createActions() {
		// TODO Auto-generated method stub

	}

	@Override
	public void createPartControl(final Composite parent) {

		createUI(parent);
		createActions();

		fillActionBars();

		addPartListener();
		addPrefListener();

		// set selection provider
		getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider());

		restoreState();
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			final Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText("this is a test");

		}

	}

	@Override
	public void dispose() {

		getViewSite().getPage().removePartListener(_partListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActionBars() {
		// TODO Auto-generated method stub

	}

	private void restoreState() {

	}

	private void saveState() {

	}

	@Override
	public void setFocus() {}

	void updateUI(final Collection<GalleryMT20Item> selectedImages, final ArrayList<Long> selectedTours) {
		// TODO Auto-generated method stub

	}
}
