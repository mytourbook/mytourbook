/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.common.util.Util;
import net.tourbook.map2.Messages;
import net.tourbook.ui.UI;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.Bullet;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.IMappingPreferences;

public class DialogModifyMapProvider extends TitleAreaDialog {

	private final IDialogSettings	_state		= TourbookPlugin.getDefault()//
														.getDialogSettingsSection("DialogModifyMapProvider");	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getDefault().getPreferenceStore();

	private ArrayList<MP>			_allMp;

	private Button					_btnUp;
	private Button					_btnDown;
	private CheckboxTableViewer		_checkboxViewer;

	// DND support
	private Object[]				_dndCheckedElements;
	private long					_dndDragStartViewerLeft;

	public DialogModifyMapProvider(final Shell parentShell) {

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

		updateUI();

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
				_prefStore.getString(IMappingPreferences.MAP_PROVIDER_SORT_ORDER));

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

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		{
			createUI10MapProviderList(container);
			createUI20Buttons(container);

			createUI30Hints(container);
		}
	}

	private void createUI10MapProviderList(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		_checkboxViewer = CheckboxTableViewer.newCheckList(parent, SWT.SINGLE | SWT.TOP | SWT.BORDER);

		final Table table = _checkboxViewer.getTable();
		GridDataFactory.swtDefaults()//
				.grab(true, true)
				.hint(SWT.DEFAULT, pc.convertHeightInCharsToPixels(10))
				.align(SWT.FILL, SWT.FILL)
				.applyTo(table);

		_checkboxViewer.setContentProvider(new IStructuredContentProvider() {
			public void dispose() {}

			public Object[] getElements(final Object inputElement) {
				return _allMp.toArray();
			}

			public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
		});

		_checkboxViewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(final Object element) {
				return ((MP) element).getName();
			}
		});

		_checkboxViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {

				// keep the checked status
				final MP item = (MP) event.getElement();
				item.setCanBeToggled(event.getChecked());

				// select the checked item
				_checkboxViewer.setSelection(new StructuredSelection(item));
//
//				validateTab();
			}
		});

		_checkboxViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				enableUpDownButtons();
			}
		});

		/*
		 * set drag adapter
		 */
		_checkboxViewer.addDragSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				new DragSourceListener() {

					public void dragFinished(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

						if (event.doit == false) {
							return;
						}

						transfer.setSelection(null);
						transfer.setSelectionSetTime(0);
					}

					public void dragSetData(final DragSourceEvent event) {
						// data are set in LocalSelectionTransfer
					}

					public void dragStart(final DragSourceEvent event) {

						final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
						final ISelection selection = _checkboxViewer.getSelection();

						_dndCheckedElements = _checkboxViewer.getCheckedElements();

						transfer.setSelection(selection);
						transfer.setSelectionSetTime(_dndDragStartViewerLeft = event.time & 0xFFFFFFFFL);

						event.doit = !selection.isEmpty();
					}
				});

		/*
		 * set drop adapter
		 */
		final ViewerDropAdapter viewerDropAdapter = new ViewerDropAdapter(_checkboxViewer) {

			private Widget	_dragOverItem;

			@Override
			public void dragOver(final DropTargetEvent dropEvent) {

				// keep table item
				_dragOverItem = dropEvent.item;

				super.dragOver(dropEvent);
			}

			@Override
			public boolean performDrop(final Object data) {

				if (data instanceof StructuredSelection) {
					final StructuredSelection selection = (StructuredSelection) data;

					if (selection.getFirstElement() instanceof MP) {

						final MP mp = (MP) selection.getFirstElement();

						final int location = getCurrentLocation();
						final Table filterTable = _checkboxViewer.getTable();

						/*
						 * check if drag was startet from this item, remove the item before the new
						 * item is inserted
						 */
						if (LocalSelectionTransfer.getTransfer().getSelectionSetTime() == _dndDragStartViewerLeft) {
							_checkboxViewer.remove(mp);
						}

						int filterIndex;

						if (_dragOverItem == null) {

							_checkboxViewer.add(mp);
							filterIndex = filterTable.getItemCount() - 1;

						} else {

							// get index of the target in the table
							filterIndex = filterTable.indexOf((TableItem) _dragOverItem);
							if (filterIndex == -1) {
								return false;
							}

							if (location == LOCATION_BEFORE) {
								_checkboxViewer.insert(mp, filterIndex);
							} else if (location == LOCATION_AFTER || location == LOCATION_ON) {
								_checkboxViewer.insert(mp, ++filterIndex);
							}
						}

						// reselect filter item
						_checkboxViewer.setSelection(new StructuredSelection(mp));

						// set focus to selection
						filterTable.setSelection(filterIndex);
						filterTable.setFocus();

						// recheck items
						_checkboxViewer.setCheckedElements(_dndCheckedElements);

						enableUpDownButtons();

						return true;
					}
				}

				return false;
			}

			@Override
			public boolean validateDrop(final Object target, final int operation, final TransferData transferType) {

				final LocalSelectionTransfer transferData = LocalSelectionTransfer.getTransfer();

				// check if dragged item is the target item
				final ISelection selection = transferData.getSelection();
				if (selection instanceof StructuredSelection) {
					final Object dragFilter = ((StructuredSelection) selection).getFirstElement();
					if (target == dragFilter) {
						return false;
					}
				}

				if (transferData.isSupportedType(transferType) == false) {
					return false;
				}

				// check if target is between two items
				if (getCurrentLocation() == LOCATION_ON) {
					return false;
				}

				return true;
			}

		};

		_checkboxViewer.addDropSupport(
				DND.DROP_MOVE,
				new Transfer[] { LocalSelectionTransfer.getTransfer() },
				viewerDropAdapter);

	}

	private void createUI20Buttons(final Composite parent) {

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

	private void createUI30Hints(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			// use a bulleted list to display this info
			final StyleRange style = new StyleRange();
			style.metrics = new GlyphMetrics(0, 0, 10);
			final Bullet bullet = new Bullet(style);

			final String infoText = Messages.Modify_MapProvider_Label_Hints;
			final int lineCount = Util.countCharacter(infoText, '\n');

			final StyledText styledText = new StyledText(container, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(styledText);
			styledText.setText(infoText);
			styledText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			styledText.setLineBullet(1, lineCount, bullet);
			styledText.setLineWrapIndent(1, lineCount, 10);
		}
	}

	/**
	 * check if the up/down buttons are enabled
	 */
	private void enableUpDownButtons() {

		final Table table = _checkboxViewer.getTable();
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

		_checkboxViewer.insert(tileFactory, index);
		_checkboxViewer.setChecked(tileFactory, tileFactory.canBeToggled());
	}

	/**
	 * Move the current selection in the build list down.
	 */
	private void moveSelectionDown() {
		final Table table = _checkboxViewer.getTable();
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
		final Table table = _checkboxViewer.getTable();
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
		final Object[] mapProviders = _checkboxViewer.getCheckedElements();
		final String[] prefGraphsChecked = new String[mapProviders.length];

		for (int graphIndex = 0; graphIndex < mapProviders.length; graphIndex++) {
			final MP mp = (MP) mapProviders[graphIndex];
			prefGraphsChecked[graphIndex] = mp.getId();
		}

		_prefStore.setValue(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST, //
				StringToArrayConverter.convertArrayToString(prefGraphsChecked));

		/*
		 * save order of all map providers
		 */
		final TableItem[] items = _checkboxViewer.getTable().getItems();
		final String[] mapProviderIds = new String[items.length];

		for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
			mapProviderIds[itemIndex] = ((MP) items[itemIndex].getData()).getId();
		}

		_prefStore.setValue(IMappingPreferences.MAP_PROVIDER_SORT_ORDER, //
				StringToArrayConverter.convertArrayToString(mapProviderIds));
	}

	private void updateUI() {

		// first create the input, then check the map providers
		createMapProviderList();
		_checkboxViewer.setInput(this);

		/*
		 * check all map providers which are defined in the pref store
		 */
		final String[] storeProviderIds = StringToArrayConverter.convertStringToArray(//
				_prefStore.getString(IMappingPreferences.MAP_PROVIDER_TOGGLE_LIST));

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

		_checkboxViewer.setCheckedElements(checkedProviders.toArray());
	}
}
