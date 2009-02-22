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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage {

	private Composite						fPrefContainer		= null;
	private Composite						fColorContainer		= null;
	private final static IPreferenceStore	fPrefStore			= Activator.getDefault().getPreferenceStore();
	private static final int				maxProfiles			= 100;
	private static RGBVertexList[]			rgbVertexList		= new RGBVertexList[maxProfiles];
	private Table							table				= null;
	private static TableItem[]				tableItem			= new TableItem[maxProfiles];
	private static int						noProfiles			= 0;
	private static int						actualProfile		= 0;
	private int								imageWidth			= 500;
	private int								imageHeight			= 40;
	private static String					profiles			= null;
	private Button							addProfileButton	= null;
	private Button							removeProfileButton	= null;

	@Override
	protected Control createContents(final Composite parent) {

		createUI(parent);

		return fPrefContainer;
	}

	private void createUI(final Composite parent) {

		fPrefContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fPrefContainer);
		GridLayoutFactory.fillDefaults().margins(0, 0).spacing(SWT.DEFAULT, 0).numColumns(3).applyTo(fPrefContainer);
		GridDataFactory.swtDefaults().applyTo(fPrefContainer);

		createTableSettings(fPrefContainer);
		createButtons(fPrefContainer);
	}

	private void createButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);

		addProfileButton = new Button(container, SWT.NONE);
		addProfileButton.setText(Messages.prefPage_srtm_add_profile);
		setButtonLayoutData(addProfileButton);
		addProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				addTableRow(fColorContainer);
			}
		});

		removeProfileButton = new Button(container, SWT.NONE);
		removeProfileButton.setText(Messages.prefPage_srtm_remove_profile);
		setButtonLayoutData(removeProfileButton);
		removeProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				removeTableRow(fColorContainer);
			}
		});
		removeProfileButton.setEnabled(false);
	}

	private void createTableSettings(final Composite parent) {

		fColorContainer = new Composite(parent, SWT.NONE);
		fColorContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

		GridDataFactory.fillDefaults().grab(false, false)
		//.hint(200, 100)
				.span(2, 1)
				.applyTo(fColorContainer);

		initVertexLists();
		createTable(fColorContainer);

		table.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));

		table.addMouseListener(new MouseListener() {
			public void mouseDoubleClick(MouseEvent e) {
				try {
					int selectedIndex = table.getSelectionIndex();
					if (selectedIndex != -1) {
						RGBVertexList rgbVertexListEdit = new RGBVertexList();
						rgbVertexListEdit.set(rgbVertexList[selectedIndex]);
						DialogAdjustSRTMColors dialog = new DialogAdjustSRTMColors(Display.getCurrent()
								.getActiveShell());
						dialog.setRGBVertexList(rgbVertexListEdit);
						if (dialog.open() == Window.OK) {
							rgbVertexList[selectedIndex] = dialog.getRgbVertexList();
							Image image = rgbVertexList[selectedIndex].getImage(fColorContainer.getDisplay(),
									imageWidth,
									imageHeight);
							tableItem[selectedIndex].setImage(image);
						} // else CANCEL			
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			public void mouseDown(MouseEvent e) {}

			public void mouseUp(MouseEvent e) {}
		});
		table.addListener(SWT.Selection | SWT.Dispose, new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Selection:
					int selectedIndex = table.getSelectionIndex();
					for (int ix = 0; ix < noProfiles; ix++)
						tableItem[ix].setChecked(false);
					if (selectedIndex != -1) {
						tableItem[selectedIndex].setChecked(true);
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

		// !!! set layout after the editor was created because the editor sets the parents layout
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(fColorContainer);
	}

	public static void initVertexLists() {
		profiles = fPrefStore.getString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = fPrefStore.getInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);
		noProfiles = 0;
		Pattern pattern = Pattern.compile("^([-,;0-9]*)X(.*)$"); //$NON-NLS-1$

		while (profiles.length() > 0) {
			Matcher matcher = pattern.matcher(profiles);
			if (matcher.matches()) {
				String profile = matcher.group(1);
				rgbVertexList[noProfiles] = new RGBVertexList();
				rgbVertexList[noProfiles].set(profile);

				profiles = matcher.group(2); // rest
				noProfiles++;
			} else
				break;
		}
	}

	private void createTable(final Composite parent) {

		int selectedIndex = -1;
		if (table != null) {
			selectedIndex = table.getSelectionIndex();
			table.removeAll();
		} else {
			table = new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.CHECK);
			selectedIndex = actualProfile;
		}
		try {
			for (int ix = 0; ix < noProfiles; ix++) {
				Image image = rgbVertexList[ix].getImage(parent.getDisplay(), imageWidth, imageHeight);
				tableItem[ix] = new TableItem(table, 0);
				tableItem[ix].setImage(image);
				tableItem[ix].setChecked(false);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (selectedIndex != -1)
			tableItem[selectedIndex].setChecked(true);
		if (removeProfileButton != null) {
			if (selectedIndex != -1)
				removeProfileButton.setEnabled(true);
			else
				removeProfileButton.setEnabled(false);
		}
	}

	private void addTableRow(final Composite parent) {
		rgbVertexList[noProfiles] = new RGBVertexList();
		Image image = rgbVertexList[noProfiles].getImage(parent.getDisplay(), imageWidth, imageHeight);
		tableItem[noProfiles] = new TableItem(table, 0);
		tableItem[noProfiles].setImage(image);
		tableItem[noProfiles].setChecked(false);
		noProfiles++;
	}

	private void removeTableRow(final Composite parent) {

		// confirm removal
		if (MessageDialog.openConfirm(parent.getShell(),
				Messages.dialog_adjust_srtm_colors_delete_profile_title,
				Messages.dialog_adjust_srtm_colors_delete_profile_msg) == false) {

			return;
		}

		int removeIndex = table.getSelectionIndex();

		if (removeIndex >= 0 && removeIndex < noProfiles) {

			for (int ix = removeIndex; ix < noProfiles - 1; ix++)
				rgbVertexList[ix].set(rgbVertexList[ix + 1]);

			for (int ix = 0; ix < noProfiles; ix++)
				tableItem[ix].setChecked(false);
			table.remove(removeIndex);
			noProfiles--;
			createTable(parent);
			parent.redraw();
		}

	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return Activator.getDefault().getPreferenceStore();
	}

	public void init(final IWorkbench workbench) {}

	@Override
	protected void performDefaults() {

		profiles = fPrefStore.getDefaultString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = fPrefStore.getDefaultInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);

		createTable(fColorContainer);

		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		profiles = ""; //$NON-NLS-1$
		for (int ix = 0; ix < noProfiles; ix++)
			profiles += rgbVertexList[ix].toString() + 'X';
		fPrefStore.setValue(IPreferences.SRTM_COLORS_PROFILES, profiles);
		fPrefStore.setValue(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, actualProfile);

		createTable(fColorContainer);

		return super.performOk();
	}

	public static RGB getRGB(int elev) {
		if (noProfiles == 0)
			initVertexLists();
		return rgbVertexList[actualProfile].getRGB(elev);
	}

}
