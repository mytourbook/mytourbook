/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.UI;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

public class ModifyMapProviderDialog extends TitleAreaDialog {

	private final IDialogSettings	_state;
	private final IPreferenceStore	_geoPrefStore;

	private ArrayList<MP>			_allMp;

	private Button					_btnUp;
	private Button					_btnDown;
	private CheckboxTableViewer		_checkboxList;

	{
		_state = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
		_geoPrefStore = TourbookPlugin.getDefault().getPreferenceStore();
	}

	public ModifyMapProviderDialog(final Shell parentShell) {

		super(parentShell);

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
		setMessage(UI.EMPTY_STRING);
		setMessage(Messages.modify_mapprovider_dialog_area_message);

		return dlgAreaContainer;
	}

	/**
	 * create a list with all available map providers, sorted by preference settings
	 */
	private void createMapProviderList() {

		final ArrayList<MP> allMapProvider = MapProviderManager.getInstance().getAllMapProviders(true);

		final String[] storedMpIds = StringToArrayConverter.convertStringToArray(//
				_geoPrefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

		_allMp = new ArrayList<MP>();

		// put all map providers into the viewer which are defined in the pref store
		for (final String storeMpId : storedMpIds) {

			// find the stored map provider in the available map providers
			for (final MP mp : allMapProvider) {
				if (mp.getId().equals(storeMpId)) {
					_allMp.add(mp);
					break;
				}
			}
		}

		// make sure that all available map providers are in the viewer
		for (final MP mp : allMapProvider) {
			if (!_allMp.contains(mp)) {
				_allMp.add(mp);
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

		final Label label = new Label(dlgContainer, SWT.WRAP);
		label.setText(Messages.modify_mapprovider_lbl_toggle_info);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

		// spacer
		new Label(dlgContainer, SWT.NONE);
	}

	private void createUIButtons(final Composite parent) {

		// button container
		final Composite buttonContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(false, false).align(SWT.FILL, SWT.FILL).applyTo(buttonContainer);
		GridLayoutFactory.fillDefaults().applyTo(buttonContainer);
//		buttonContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		// button: up
		_btnUp = new Button(buttonContainer, SWT.NONE);
		_btnUp.setText(Messages.modify_mapprovider_btn_up);
		setButtonLayoutData(_btnUp);
		_btnUp.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionUp();
				enableUpDownButtons();
			}
		});

		// button: down
		_btnDown = new Button(buttonContainer, SWT.NONE);
		_btnDown.setText(Messages.modify_mapprovider_btn_down);
		setButtonLayoutData(_btnDown);
		_btnDown.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				moveSelectionDown();
				enableUpDownButtons();
			}
		});
	}

	private void createUIMapProviderList(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		_checkboxList = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.TOP | SWT.BORDER);

		final Table table = _checkboxList.getTable();
		GridDataFactory.swtDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, pc.convertHeightInCharsToPixels(10))
				.align(SWT.FILL, SWT.FILL)
				.applyTo(table);

		_checkboxList.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _allMp.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_checkboxList.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				return ((MP) element).getName();
			}
		});

		_checkboxList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final MP item = (MP) event.getElement();
				item.setCanBeToggled(event.getChecked());

				// select the checked item
				_checkboxList.setSelection(new StructuredSelection(item));
//
//				validateTab();
			}
		});

		_checkboxList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});

		// first create the input, then check the map providers
		createMapProviderList();
		_checkboxList.setInput(this);

		/*
		 * check all map providers which are defined in the pref store
		 */
		final String[] storeProviderIds = StringToArrayConverter.convertStringToArray(//
				_geoPrefStore.getString(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST));

		final ArrayList<MP> checkedProviders = new ArrayList<MP>();

		for (final MP mapProvider : _allMp) {
			final String mpId = mapProvider.getId();
			for (final String storedProviderId : storeProviderIds) {
				if (mpId.equals(storedProviderId)) {
					mapProvider.setCanBeToggled(true);
					checkedProviders.add(mapProvider);
					break;
				}
			}
		}

		_checkboxList.setCheckedElements(checkedProviders.toArray());
	}

	/**
	 * check if the up/down button are enabled
	 */
	private void enableUpDownButtons() {

		final Table table = _checkboxList.getTable();
		final TableItem[] items = table.getSelection();

		final boolean validSelection = items != null && items.length > 0;
		boolean enableUp = validSelection;
		boolean enableDown = validSelection;

		if (validSelection) {
			final int indices[] = table.getSelectionIndices();
			final int max = table.getItemCount();
			enableUp = indices[0] != 0;
			enableDown = indices[indices.length - 1] < max - 1;
		}

		_btnUp.setEnabled(enableUp);
		_btnDown.setEnabled(enableDown);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return _state;
	}

	/**
	 * Moves an entry in the table to the given index.
	 */
	private void move(final TableItem item, final int index) {

		final MP tileFactory = (MP) item.getData();
		item.dispose();

		_checkboxList.insert(tileFactory, index);
		_checkboxList.setChecked(tileFactory, tileFactory.canBeToggled());
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		final Table table = _checkboxList.getTable();
		final int indices[] = table.getSelectionIndices();
		if (indices.length < 1) {
			return;
		}
		final int newSelection[] = new int[indices.length];
		final int max = table.getItemCount() - 1;
		for (int i = indices.length - 1; i >= 0; i--) {
			final int index = indices[i];
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
		final Table table = _checkboxList.getTable();
		final int indices[] = table.getSelectionIndices();
		final int newSelection[] = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			final int index = indices[i];
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
		final Object[] mapProviders = _checkboxList.getCheckedElements();
		final String[] prefGraphsChecked = new String[mapProviders.length];

		for (int graphIndex = 0; graphIndex < mapProviders.length; graphIndex++) {
			final MP mp = (MP) mapProviders[graphIndex];
			prefGraphsChecked[graphIndex] = mp.getId();
		}

		_geoPrefStore.setValue(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST, //
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		/*
		 * save order of all map providers
		 */
		final TableItem[] items = _checkboxList.getTable().getItems();
		final String[] mapProviderIds = new String[items.length];

		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			mapProviderIds[itemIndex] = ((MP) items[itemIndex].getData()).getId();
		}

		_geoPrefStore.setValue(IMappingPreferences.MAP_PROVIDER_SORT_ORDER, //
				StringToArrayConverter.convertArrayToString(mapProviderIds));
	}
}
