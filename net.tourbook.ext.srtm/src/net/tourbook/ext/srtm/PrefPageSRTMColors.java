/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
/**
 * @author Wolfgang Schramm
 * @author Alfred Barten
 */

package net.tourbook.ext.srtm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int				COLUMN_PROFILE_NAME		= 0;
	private static final int				COLUMN_PROFILE_IMAGE	= 1;

	private final static IPreferenceStore	iPreferenceStore		= Activator.getDefault().getPreferenceStore();

	private static final int				maxProfiles				= 100;
	private static int						noProfiles				= 0;
	private static int						actualProfile			= 0;
	private static String					profiles				= null;

	private static RGBVertexList[]			rgbVertexList			= new RGBVertexList[maxProfiles];
	private static TableItem[]				fTableItems				= new TableItem[maxProfiles];

	private Composite						fTableContainer			= null;
	private Table							fTable					= null;
	private Button							addProfileButton		= null;
	private Button							removeProfileButton		= null;
	private TableColumn						fColumnImage;

	private int								fDefaultImageWidth		= 300;
	private int								fImageHeight			= 40;
	private int								fOldImageWidth			= -1;

	public static int getGrid() {
		// elevation is used at every grid-th pixel in both directions; 
		// the other values are interpolated
		// i.e. it gives the resolution of the image!
		final String srtmResolution = iPreferenceStore.getString(IPreferences.SRTM_RESOLUTION);
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_ROUGH))
			return 64;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_ROUGH))
			return 16;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_FINE))
			return 4;
		if (srtmResolution.equals(IPreferences.SRTM_RESOLUTION_VERY_FINE))
			return 1;
		return 4;
	}

	public static RGB getRGB(final int elev) {
		if (noProfiles == 0)
			initVertexLists();
		return rgbVertexList[actualProfile].getRGB(elev);
	}

	public static String getRGBVertexListString() {
		return rgbVertexList[actualProfile].toString();
	}

	public static void initVertexLists() {
		profiles = iPreferenceStore.getString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = iPreferenceStore.getInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);
		noProfiles = 0;
		final Pattern pattern = Pattern.compile("^([-,;0-9]*)X(.*)$"); //$NON-NLS-1$

		while (profiles.length() > 0) {
			final Matcher matcher = pattern.matcher(profiles);
			if (matcher.matches()) {
				final String profile = matcher.group(1);
				rgbVertexList[noProfiles] = new RGBVertexList();
				rgbVertexList[noProfiles].set(profile);

				profiles = matcher.group(2); // rest
				noProfiles++;
			} else
				break;
		}
	}

	public static boolean isShadowState() {
		return iPreferenceStore.getBoolean(IPreferences.SRTM_SHADOW);
	}

	private void addTableRow(final Composite parent) {

		rgbVertexList[noProfiles] = new RGBVertexList();
		rgbVertexList[noProfiles].init(); // a few default settings 

		final TableItem tableItem = fTableItems[noProfiles] = new TableItem(fTable, 0);

		tableItem.setChecked(false);
		final Image image = rgbVertexList[noProfiles].getImage(parent.getDisplay(), getImageWidth(), fImageHeight);
		tableItem.setData(image);

		fTable.setSelection(noProfiles);

		noProfiles++;
	}

	@Override
	protected Control createContents(final Composite parent) {

		final Composite container = createUI(parent);

//		fTableContainer.pack(true);
//		parent.pack(true);
//		fTable.pack(true);

		return container;
	}

	private Table createTableItems(final Composite parent) {

		int selectedIndex = -1;

		Table table;
		if (fTable == null) {
			table = new Table(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
			selectedIndex = actualProfile;
		} else {
			table = fTable;
			selectedIndex = table.getSelectionIndex();
			table.removeAll();
		}

		try {
			for (int ix = 0; ix < noProfiles; ix++) {

				final TableItem tableItem = fTableItems[ix] = new TableItem(table, 0);

				tableItem.setText(COLUMN_PROFILE_NAME, rgbVertexList[ix].toString());
				final Image image = rgbVertexList[ix].getImage(parent.getDisplay(), getImageWidth(), fImageHeight);
				tableItem.setData(image);
				tableItem.setChecked(false);
			}

		} catch (final Exception e) {
			e.printStackTrace();
		}

		if (selectedIndex != -1) {
			fTableItems[selectedIndex].setChecked(true);
		}

		if (removeProfileButton != null) {
			if (selectedIndex != -1) {
				removeProfileButton.setEnabled(true);
			} else {
				removeProfileButton.setEnabled(false);
			}
		}

		return table;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(false, false).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);

		createUIResolutionOption(container);
		createUIShadowOption(container);
		createUIProfileTable(container);
		createUIButtons(container);

		return container;
	}

	private void createUIButtons(final Composite parent) {

		final Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(buttonComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonComposite);

		addProfileButton = new Button(buttonComposite, SWT.NONE);
		addProfileButton.setText(Messages.prefPage_srtm_profile_add);
		setButtonLayoutData(addProfileButton);
		addProfileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				addTableRow(fTableContainer);
			}
		});

		removeProfileButton = new Button(buttonComposite, SWT.NONE);
		removeProfileButton.setText(Messages.prefPage_srtm_profile_remove);
		setButtonLayoutData(removeProfileButton);
		removeProfileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				removeTableRow(fTableContainer);
			}
		});
		removeProfileButton.setEnabled(false);
	}

	private void createUIProfileTable(final Composite parent) {

		final Label label = new Label(parent, SWT.LEFT);
		label.setText(Messages.prefPage_srtm_profile_title);

		fTableContainer = new Composite(parent, SWT.NONE);

		final TableColumnLayout tableLayout = new TableColumnLayout();
		fTableContainer.setLayout(tableLayout);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTableContainer);

		initVertexLists();

		fTable = createTableItems(fTableContainer);

		fTable.setHeaderVisible(true);
//		fTable.setLinesVisible(true);

		/*
		 * create table columns
		 */
		final TableColumn column = new TableColumn(fTable, SWT.CENTER);
		column.setText("Name");
		tableLayout.setColumnData(column, new ColumnPixelData(200, true));

		fColumnImage = new TableColumn(fTable, SWT.NONE);
		fColumnImage.setText("Image");
		fColumnImage.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeImageColumn();
			}
		});
		fColumnImage.setWidth(fDefaultImageWidth);
		tableLayout.setColumnData(fColumnImage, new ColumnWeightData(1, true));

		fTable.setToolTipText(Messages.PrefPage_srtm_colors_table_ttt);

		fTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				try {
					final int selectedIndex = fTable.getSelectionIndex();
					if (selectedIndex != -1) {

						// open color dialog

						final RGBVertexList rgbVertexListEdit = new RGBVertexList();
						rgbVertexListEdit.set(rgbVertexList[selectedIndex]);

						final DialogAdjustSRTMColors dialog = new DialogAdjustSRTMColors(Display.getCurrent()
								.getActiveShell());
						dialog.setRGBVertexList(rgbVertexListEdit);

						if (dialog.open() == Window.OK) {
							rgbVertexList[selectedIndex] = dialog.getRgbVertexList();
							final Image image = rgbVertexList[selectedIndex].getImage(fTableContainer.getDisplay(),
									getImageWidth(),
									fImageHeight);
							fTableItems[selectedIndex].setData(image);
						}
					}
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		fTable.addListener(SWT.Selection | SWT.Dispose, new Listener() {
			public void handleEvent(final Event e) {
				switch (e.type) {
				case SWT.Selection:
					final int selectedIndex = fTable.getSelectionIndex();
					for (int ix = 0; ix < noProfiles; ix++)
						fTableItems[ix].setChecked(false);
					if (selectedIndex != -1) {
						fTableItems[selectedIndex].setChecked(true);
						removeProfileButton.setEnabled(true);
						actualProfile = selectedIndex;
					}
					break;
				case SWT.Dispose:
					// xxx.dispose();
					break;
				}
			}
		});

		/*
		 * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
		 * critical for performance that these methods be as efficient as possible.
		 */
		final Listener paintListener = new Listener() {
			public void handleEvent(final Event event) {
				switch (event.type) {

				case SWT.MeasureItem: {

					if (event.index == COLUMN_PROFILE_IMAGE) {

						final TableItem item = (TableItem) event.item;
						final Image image = (Image) item.getData();
						final Rectangle rect = image.getBounds();

						event.width += rect.width;
						event.height = Math.max(event.height, rect.height + 2);
//					} else {
// is not working to center the checkbox
//						event.height = imageHeight;
					}
					break;
				}

				case SWT.PaintItem: {

					if (event.index == COLUMN_PROFILE_IMAGE) {

						final TableItem item = (TableItem) event.item;
						final Image image = (Image) item.getData();

						final int x = event.x + event.width;
						final Rectangle rect = image.getBounds();
						final int offset = Math.max(0, (event.height - rect.height) / 2);

						event.gc.drawImage(image, x, event.y + offset);
					}
					break;
				}
				}
			}
		};
		fTable.addListener(SWT.MeasureItem, paintListener);
		fTable.addListener(SWT.PaintItem, paintListener);
	}

	private void createUIResolutionOption(final Composite parent) {

//		final Group resolutionGroup = new Group(parent, SWT.NONE);
//		resolutionGroup.setText("Resolution");
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(resolutionGroup);

		final RadioGroupFieldEditor radioGroupFieldEditor = new RadioGroupFieldEditor(IPreferences.SRTM_RESOLUTION,
				Messages.prefPage_srtm_resolution_title,
				4,
				new String[][] {
						new String[] {
								Messages.prefPage_srtm_resolution_very_fine,
								IPreferences.SRTM_RESOLUTION_VERY_FINE },
						new String[] { Messages.prefPage_srtm_resolution_fine, IPreferences.SRTM_RESOLUTION_FINE },
						new String[] { Messages.prefPage_srtm_resolution_rough, IPreferences.SRTM_RESOLUTION_ROUGH },
						new String[] {
								Messages.prefPage_srtm_resolution_very_rough,
								IPreferences.SRTM_RESOLUTION_VERY_ROUGH }, },
				parent, // resolutionGroup,
				true);

		radioGroupFieldEditor.setPreferenceStore(iPreferenceStore);
		radioGroupFieldEditor.load();
		radioGroupFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				iPreferenceStore.setValue(IPreferences.SRTM_RESOLUTION, "" + e.getNewValue()); //$NON-NLS-1$
			}
		});

		// set margins after the editors are added
//		final GridLayout groupLayout = (GridLayout) resolutionGroup.getLayout();
//		groupLayout.marginWidth = 5;
//		groupLayout.marginHeight = 5;
	}

	private void createUIShadowOption(final Composite parent) {

		final BooleanFieldEditor booleanFieldEditor = new BooleanFieldEditor(IPreferences.SRTM_SHADOW,
				Messages.PrefPage_srtm_shadow_text,
				parent);

		booleanFieldEditor.setPreferenceStore(iPreferenceStore);
		booleanFieldEditor.load();
		booleanFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				iPreferenceStore.setValue(IPreferences.SRTM_SHADOW, "" + e.getNewValue()); //$NON-NLS-1$
			}
		});

	}

	@Override
	public void dispose() {
		disposeImages();
		super.dispose();
	}

	private void disposeImages() {
		if (fTableItems != null) {
			for (final TableItem tableItem : fTableItems) {
				if (tableItem != null) {
					((Image) (tableItem.getData())).dispose();
				}
			}
		}
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	private int getImageWidth() {

		int width;
		if (fColumnImage == null) {
			width = fDefaultImageWidth;
		} else {
			width = fColumnImage.getWidth();
		}

		return width;
	}

	public void init(final IWorkbench workbench) {}

	private void onResizeImageColumn() {

		final int newImageWidth = getImageWidth();

		// check if the width has changed
		if (newImageWidth == fOldImageWidth) {
			return;
		}

		disposeImages();

		// create new images 
		int itemIndex = 0;
		for (final TableItem tableItem : fTableItems) {

			if (tableItem != null) {
				final Image image = rgbVertexList[itemIndex].getImage(Display.getCurrent(), newImageWidth, fImageHeight);
				tableItem.setData(image);
			}

			itemIndex++;
		}
	}

	@Override
	protected void performDefaults() {

		profiles = iPreferenceStore.getDefaultString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = iPreferenceStore.getDefaultInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);

		createTableItems(fTableContainer);

		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		profiles = ""; //$NON-NLS-1$
		for (int ix = 0; ix < noProfiles; ix++)
			profiles += rgbVertexList[ix].toString() + 'X';
		iPreferenceStore.setValue(IPreferences.SRTM_COLORS_PROFILES, profiles);
		iPreferenceStore.setValue(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, actualProfile);
		createTableItems(fTableContainer);

		return super.performOk();
	}

	private void removeTableRow(final Composite parent) {

		// confirm removal
		if (MessageDialog.openConfirm(parent.getShell(),
				Messages.dialog_adjust_srtm_colors_delete_profile_title,
				Messages.dialog_adjust_srtm_colors_delete_profile_msg) == false) {

			return;
		}

		final int removeIndex = fTable.getSelectionIndex();

		if (removeIndex >= 0 && removeIndex < noProfiles) {

			for (int ix = removeIndex; ix < noProfiles - 1; ix++)
				rgbVertexList[ix].set(rgbVertexList[ix + 1]);

			for (int ix = 0; ix < noProfiles; ix++)
				fTableItems[ix].setChecked(false);

			fTable.remove(removeIndex);
			noProfiles--;

			createTableItems(parent);
			parent.redraw();
		}

	}
}
