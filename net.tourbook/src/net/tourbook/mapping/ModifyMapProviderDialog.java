/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class ModifyMapProviderDialog extends TitleAreaDialog {

	private final IDialogSettings		fDialogSettings;
	private final IPreferenceStore		fPrefStore;

	private MappingView					fMappingView;
	private ArrayList<MapProvider>	fMapProviders;
	private Button						fBtnUp;
	private Button						fBtnDown;
	private CheckboxTableViewer			fCheckboxList;

	{
		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
		fPrefStore = TourbookPlugin.getDefault().getPreferenceStore();
	}

	public ModifyMapProviderDialog(final Shell parentShell, MappingView mappingView) {

		super(parentShell);

		fMappingView = mappingView;

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(Messages.modify_mapprovider_dialog_title);
		setTitle(Messages.modify_mapprovider_dialog_area_title);

		enableUpDownButtons();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite dlgAreaContainer = (Composite) super.createDialogArea(parent);

		createUI(dlgAreaContainer);

		// trick to show the message
		setMessage(""); //$NON-NLS-1$
		setMessage(Messages.modify_mapprovider_dialog_area_message);

		return dlgAreaContainer;
	}

	/**
	 * create a list with all available map providers, sorted by preference settings
	 */
	private void createMapProviderList() {

		final List<MapProvider> mapProviders = fMappingView.getFactories();

		String[] storedProviderIds = StringToArrayConverter.convertStringToArray(//
		fPrefStore.getString(ITourbookPreferences.MAP_PROVIDERS_SORT_ORDER));

		fMapProviders = new ArrayList<MapProvider>();

		// put all map providers into the viewer which are defined in the pref store
		for (int providerIndex = 0; providerIndex < storedProviderIds.length; providerIndex++) {

			String storeMapProvider = storedProviderIds[providerIndex];

			// find the stored map provider in the available map providers
			for (MapProvider mapProvider : mapProviders) {
				if (mapProvider.getInfo().getFactoryID().equals(storeMapProvider)) {
					fMapProviders.add(mapProvider);
					break;
				}
			}
		}

		// make sure that all available map providers are in the viewer
		for (MapProvider tileFactory : mapProviders) {
			if (!fMapProviders.contains(tileFactory)) {
				fMapProviders.add(tileFactory);
			}
		}
	}

	private void createUI(final Composite parent) {

		/*
		 * dialog container
		 */
		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(dlgContainer);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
//		dlgContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));

		createUIMapProviderList(dlgContainer);
		createUIButtons(dlgContainer);

		Label label = new Label(dlgContainer, SWT.WRAP);
		label.setText(Messages.ModifyMapProviderDialog_modify_mapprovider_lbl_toggle_info);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

		// spacer
		new Label(dlgContainer, SWT.NONE);
	}

	private void createUIButtons(Composite parent) {

		// button container
		Composite buttonContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().applyTo(buttonContainer);
//		buttonContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		// button: up
		fBtnUp = new Button(buttonContainer, SWT.NONE);
		fBtnUp.setText(Messages.modify_mapprovider_btn_up);
		setButtonLayoutData(fBtnUp);
		fBtnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});

		// button: down
		fBtnDown = new Button(buttonContainer, SWT.NONE);
		fBtnDown.setText(Messages.modify_mapprovider_btn_down);
		setButtonLayoutData(fBtnDown);
		fBtnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
		});
	}

	private void createUIMapProviderList(final Composite parent) {

		fCheckboxList = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.TOP | SWT.BORDER);

		Table table = fCheckboxList.getTable();
		GridDataFactory.swtDefaults()//
				.grab(true, true)
				.align(SWT.FILL, SWT.FILL)
				.applyTo(table);

		fCheckboxList.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return fMapProviders.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		fCheckboxList.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				MapProvider tileFactory = (MapProvider) element;
				return tileFactory.getInfo().getFactoryName();
			}
		});

		fCheckboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final MapProvider item = (MapProvider) event.getElement();
				item.setCanBeToggled(event.getChecked());

				// select the checked item
				fCheckboxList.setSelection(new StructuredSelection(item));
//
//				validateTab();
			}
		});

		fCheckboxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});

		// first create the input, then check the map providers
		createMapProviderList();
		fCheckboxList.setInput(this);

		/*
		 * check all map providers which are defined in the pref store
		 */
		String[] storeProviderIds = StringToArrayConverter.convertStringToArray(//
		fPrefStore.getString(ITourbookPreferences.MAP_PROVIDERS_TOGGLE_LIST));

		ArrayList<MapProvider> checkedProviders = new ArrayList<MapProvider>();

		for (MapProvider mapProvider : fMapProviders) {
			final String factoryId = mapProvider.getInfo().getFactoryID();
			for (String storedProviderId : storeProviderIds) {
				if (factoryId.equals(storedProviderId)) {
					mapProvider.setCanBeToggled(true);
					checkedProviders.add(mapProvider);
					break;
				}
			}
		}

		fCheckboxList.setCheckedElements(checkedProviders.toArray());
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		Table table = fCheckboxList.getTable();
		TableItem[] items = table.getSelection();

		boolean validSelection = items != null && items.length > 0;
		boolean enableUp = validSelection;
		boolean enableDown = validSelection;

		if (validSelection) {
			int indices[] = table.getSelectionIndices();
			int max = table.getItemCount();
			enableUp = indices[0] != 0;
			enableDown = indices[indices.length - 1] < max - 1;
		}

		fBtnUp.setEnabled(enableUp);
		fBtnDown.setEnabled(enableDown);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(TableItem item, int index) {

		final MapProvider tileFactory = (MapProvider) item.getData();
		item.dispose();

		fCheckboxList.insert(tileFactory, index);
		fCheckboxList.setChecked(tileFactory, tileFactory.canBeToggled());
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		Table table = fCheckboxList.getTable();
		int indices[] = table.getSelectionIndices();
		if (indices.length < 1) {
			return;
		}
		int newSelection[] = new int[indices.length];
		int max = table.getItemCount() - 1;
		for (int i = indices.length - 1; i >= 0; i--) {
			int index = indices[i];
			if (index < max) {
				move(table.getItem(index), index + 1);
				newSelection[i] = index + 1;
			}
		}
		table.setSelection(newSelection);
	}

	/**
	 * Move the current selection in the build list up.
	 */
	private void moveSelectionUp() {
		Table table = fCheckboxList.getTable();
		int indices[] = table.getSelectionIndices();
		int newSelection[] = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			int index = indices[i];
			if (index > 0) {
				move(table.getItem(index), index - 1);
				newSelection[i] = index - 1;
			}
		}
		table.setSelection(newSelection);
	}

	@Override
	protected void okPressed() {

		saveMapProviders();

		super.okPressed();
	}

	/**
	 * save the ckeck state and order of the map providers
	 */
	private void saveMapProviders() {

		/*
		 * save all checked map providers
		 */
		Object[] mapProviders = fCheckboxList.getCheckedElements();
		String[] prefGraphsChecked = new String[mapProviders.length];

		for (int graphIndex = 0; graphIndex < mapProviders.length; graphIndex++) {
			final MapProvider mapTileFactory = (MapProvider) mapProviders[graphIndex];
			prefGraphsChecked[graphIndex] = mapTileFactory.getInfo().getFactoryID();
		}

		fPrefStore.setValue(ITourbookPreferences.MAP_PROVIDERS_TOGGLE_LIST,
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		/*
		 * save order of all map providers
		 */
		TableItem[] items = fCheckboxList.getTable().getItems();
		String[] mapProviderIds = new String[items.length];

		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			mapProviderIds[itemIndex] = ((MapProvider) items[itemIndex].getData()).getInfo().getFactoryID();
		}

		fPrefStore.setValue(ITourbookPreferences.MAP_PROVIDERS_SORT_ORDER,
				StringToArrayConverter.convertArrayToString(mapProviderIds));
	}
}
