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
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public final class PrefPageSRTMColors extends PreferencePage implements IWorkbenchPreferencePage {

	private Composite						mainComposite		= null;
	private Composite						colorComposite		= null;
	private final static IPreferenceStore	iPreferenceStore	= Activator.getDefault().getPreferenceStore();
	private static final int				maxProfiles			= 100;
	private static RGBVertexList[]			rgbVertexList		= new RGBVertexList[maxProfiles];
	private Table							table				= null;
	private static TableItem[]				tableItem			= new TableItem[maxProfiles];
	private static int						noProfiles			= 0;
	private static int						actualProfile		= 0;
	private int								imageWidth			= 600;
	private int								imageHeight			= 40;
	private static String					profiles			= null;
	private Button							addProfileButton	= null;
	private Button							removeProfileButton	= null;

	@Override
	protected Control createContents(final Composite parent) {

		createUI(parent);

		return mainComposite;
	}

	private void createUI(final Composite parent) {

		mainComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.swtDefaults().grab(false, false).applyTo(mainComposite);
		GridLayoutFactory.fillDefaults().margins(0, 0).spacing(SWT.DEFAULT, 0).numColumns(1).applyTo(mainComposite);

		createResolutionOption(mainComposite);
		createShadowOption(mainComposite);
		createTableSettings(mainComposite);
		createButtons(mainComposite);
	}
	
	private void createResolutionOption(final Composite parent) {

//		final Group resolutionGroup = new Group(parent, SWT.NONE);
//		resolutionGroup.setText("Resolution");
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(resolutionGroup);

		final RadioGroupFieldEditor radioGroupFieldEditor = new RadioGroupFieldEditor(IPreferences.SRTM_RESOLUTION,
				Messages.prefPage_srtm_resolution_title,
				4,
				new String[][] {
				     new String[] {Messages.prefPage_srtm_resolution_very_fine, IPreferences.SRTM_RESOLUTION_VERY_FINE},
				     new String[] {Messages.prefPage_srtm_resolution_fine, IPreferences.SRTM_RESOLUTION_FINE},
				     new String[] {Messages.prefPage_srtm_resolution_rough, IPreferences.SRTM_RESOLUTION_ROUGH}, 
				     new String[] {Messages.prefPage_srtm_resolution_very_rough, IPreferences.SRTM_RESOLUTION_VERY_ROUGH}, 
					 		   },
				parent, // resolutionGroup,
				true);
		
		radioGroupFieldEditor.setPreferenceStore(iPreferenceStore);
		radioGroupFieldEditor.load();
		radioGroupFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				iPreferenceStore.setValue(IPreferences.SRTM_RESOLUTION, ""+e.getNewValue()); //$NON-NLS-1$
			}
		});
		
		// set margins after the editors are added
//		final GridLayout groupLayout = (GridLayout) resolutionGroup.getLayout();
//		groupLayout.marginWidth = 5;
//		groupLayout.marginHeight = 5;
	}
	
	private void createShadowOption(final Composite parent) {

		final BooleanFieldEditor booleanFieldEditor = new BooleanFieldEditor(IPreferences.SRTM_SHADOW,
				Messages.PrefPage_srtm_shadow_text,
				parent);
		
		booleanFieldEditor.setPreferenceStore(iPreferenceStore);
		booleanFieldEditor.load();
		booleanFieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(final PropertyChangeEvent e) {
				iPreferenceStore.setValue(IPreferences.SRTM_SHADOW, ""+e.getNewValue()); //$NON-NLS-1$
			}
		});
		
	}


	private void createButtons(final Composite parent) {

		final Composite buttonComposite = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().indent(5, 0).applyTo(buttonComposite);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(buttonComposite);

		addProfileButton = new Button(buttonComposite, SWT.NONE);
		addProfileButton.setText(Messages.prefPage_srtm_profile_add);
		setButtonLayoutData(addProfileButton);
		addProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				addTableRow(colorComposite);
			}
		});

		removeProfileButton = new Button(buttonComposite, SWT.NONE);
		removeProfileButton.setText(Messages.prefPage_srtm_profile_remove);
		setButtonLayoutData(removeProfileButton);
		removeProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				removeTableRow(colorComposite);
			}
		});
		removeProfileButton.setEnabled(false);
	}

	private void createTableSettings(final Composite parent) {

		Label label = new Label(parent, SWT.LEFT);
		label.setText(Messages.prefPage_srtm_profile_title);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1));

		colorComposite = new Composite(parent, SWT.NONE);
		colorComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));
		GridDataFactory.fillDefaults().grab(false, false).span(2, 1).applyTo(colorComposite);

		initVertexLists();
		createTable(colorComposite);

		table.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 1, 1));
		table.setToolTipText(Messages.PrefPage_srtm_colors_table_ttt);
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
							Image image = rgbVertexList[selectedIndex].getImage(colorComposite.getDisplay(),
									imageWidth,
									imageHeight);
							tableItem[selectedIndex].setImage(image);
						} 		
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
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(colorComposite);
	}

	public static void initVertexLists() {
		profiles = iPreferenceStore.getString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = iPreferenceStore.getInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);
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
		rgbVertexList[noProfiles].init(); // a few default settings 
		Image image = rgbVertexList[noProfiles].getImage(parent.getDisplay(), imageWidth, imageHeight);
		tableItem[noProfiles] = new TableItem(table, 0);
		tableItem[noProfiles].setImage(image);
		tableItem[noProfiles].setChecked(false);
		table.setSelection(noProfiles);
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

		profiles = iPreferenceStore.getDefaultString(IPreferences.SRTM_COLORS_PROFILES);
		actualProfile = iPreferenceStore.getDefaultInt(IPreferences.SRTM_COLORS_ACTUAL_PROFILE);

		createTable(colorComposite);

		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		profiles = ""; //$NON-NLS-1$
		for (int ix = 0; ix < noProfiles; ix++)
			profiles += rgbVertexList[ix].toString() + 'X';
		iPreferenceStore.setValue(IPreferences.SRTM_COLORS_PROFILES, profiles);
		iPreferenceStore.setValue(IPreferences.SRTM_COLORS_ACTUAL_PROFILE, actualProfile);
		createTable(colorComposite);

		return super.performOk();
	}

	public static RGB getRGB(int elev) {
		if (noProfiles == 0)
			initVertexLists();
		return rgbVertexList[actualProfile].getRGB(elev);
	}

	public static int getGrid() {
		// elevation is used at every grid-th pixel in both directions; 
		// the other values are interpolated
		// i.e. it gives the resolution of the image!
		String srtmResolution = iPreferenceStore.getString(IPreferences.SRTM_RESOLUTION);
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
	
	public static boolean isShadowState() {
		return iPreferenceStore.getBoolean(IPreferences.SRTM_SHADOW);
	}
	
	public static String getRGBVertexListString() {
		return rgbVertexList[actualProfile].toString();
	}
}
