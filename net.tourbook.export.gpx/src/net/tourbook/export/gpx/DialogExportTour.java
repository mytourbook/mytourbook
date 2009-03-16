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
package net.tourbook.export.gpx;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.export.ExportTourExtension;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.plugin.TourbookPlugin;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.dinopolis.gpstool.gpsinput.garmin.GarminTrack;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogExportTour extends TitleAreaDialog {

	private static final String		SETTINGS_EXPORT_PATH	= "ExportPath";

	private static final int		SIZING_TEXT_FIELD_WIDTH	= 250;
	private static final int		COMBO_HISTORY_LENGTH	= 20;

	private ExportTourExtension		fExportExtensionPoint;
	private ArrayList<TourData>		fTourDataList;

	private final IDialogSettings	fDialogSettings;

	private Combo					fComboPath;
	private Button					fBtnSelectDirectory;

	private Composite				fDlgContainer;

	public DialogExportTour(final Shell parentShell,
							final ExportTourExtension exportExtensionPoint,
							final ArrayList<TourData> tourDataList) {

		super(parentShell);

		// make dialog resizable
		setShellStyle(getShellStyle() | SWT.RESIZE);

//		setDefaultImage(TourbookPlugin.getImageDescriptor(Messages.Image__quick_edit).createImage());

		fTourDataList = tourDataList;
		fExportExtensionPoint = exportExtensionPoint;

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.dialog_export_shell_text);
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.dialog_export_dialog_title);
		setMessage(NLS.bind(Messages.dialog_export_dialog_message, fExportExtensionPoint.getVisibleName()));
		
		restoreState();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
//		String okText = null;
//
//		final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
//		if (tourDataEditor != null && tourDataEditor.isDirty() && tourDataEditor.getTourData() == fTourDataList) {
//			okText = Messages.app_action_update;
//		} else {
//			okText = Messages.app_action_save;
//		}
//
//		getButton(IDialogConstants.OK_ID).setText(okText);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createUI(final Composite parent) {

//		final Label label;
//
//		final PixelConverter pixelConverter = new PixelConverter(parent);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);

		createUIDestination(container);
	}

	/**
	 * Create the export destination specification widgets
	 * 
	 * @param parent
	 *            org.eclipse.swt.widgets.Composite
	 */
	private void createUIDestination(final Composite parent) {

		final Font font = parent.getFont();

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
		container.setFont(font);

		/*
		 * label
		 */
		final Label label = new Label(container, SWT.NONE);
		label.setText(Messages.dialog_export_label_destination);
		label.setFont(font);

		/*
		 * combo: path
		 */
		fComboPath = new Combo(container, SWT.SINGLE | SWT.BORDER);
		fComboPath.setFont(font);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fComboPath);
		final GridData layoutData = (GridData) fComboPath.getLayoutData();
		layoutData.widthHint = SIZING_TEXT_FIELD_WIDTH;
		fComboPath.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				validatePath();
			}
		});
		fComboPath.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validatePath();
			}
		});

		/*
		 * button: browse
		 */
		// destination browse button
		fBtnSelectDirectory = new Button(container, SWT.PUSH);
		fBtnSelectDirectory.setText(Messages.dialog_export_btn_selectDirectory);
		fBtnSelectDirectory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectBrowseDirectory();
			}
		});
		fBtnSelectDirectory.setFont(font);
		setButtonLayoutData(fBtnSelectDirectory);
	}

	private void doExport() {

		// create context
		final VelocityContext context = new VelocityContext();

		// prepare context
		final ArrayList<GarminTrack> tList = new ArrayList<GarminTrack>();
		tList.add(track);
		context.put("tracks", tList); //$NON-NLS-1$
		context.put("printtracks", new Boolean(true)); //$NON-NLS-1$
		context.put("printwaypoints", new Boolean(false)); //$NON-NLS-1$
		context.put("printroutes", new Boolean(false)); //$NON-NLS-1$

		final File receivedFile = new File(RawDataManager.getTempDir()
				+ File.separator
				+ track.getIdentification()
				+ ".gpx"); //$NON-NLS-1$

		final Reader reader = new InputStreamReader(this.getClass()
				.getResourceAsStream("/format-templates/gpx-1.0.vm")); //$NON-NLS-1$
		final Writer writer = new FileWriter(receivedFile);

		addValuesToContext(context, productInfo);

		Velocity.evaluate(context, writer, "MyTourbook", reader); //$NON-NLS-1$
		writer.close();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return fDialogSettings;
	}

	/**
	 * @return <code>true</code> when the path is valid in the destination field
	 */
	private boolean isPathValid() {
		return new Path(fComboPath.getText()).toFile().exists();
	}

	@Override
	protected void okPressed() {

		/*
		 * do export
		 */
		fExportExtensionPoint.exportTours(fTourDataList, fComboPath.getText());

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(fDlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.dialog_export_dir_dialog_text);
		dialog.setMessage(Messages.dialog_export_dir_dialog_message);

		dialog.setFilterPath(fComboPath.getText());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			fComboPath.setText(selectedDirectoryName);
		}

	}

	private void restoreState() {
		
		final String[] pathItems = fDialogSettings.getArray(SETTINGS_EXPORT_PATH);

		if (pathItems != null && pathItems.length > 0) {
			for (final String pathItem : pathItems) {
				fComboPath.add(pathItem);
			}
			
			// restore last used path
			fComboPath.setText(pathItems[0]);
		}
	}

	private void saveState() {

		String[] pathItems = fComboPath.getItems();

		
		if (isPathValid()) {
		
			/*
			 * add current path to the path history
			 */
			
			final String currentPath = fComboPath.getText();

			final ArrayList<String> pathList = new ArrayList<String>();

			pathList.add(currentPath);

			for (final String pathItem : pathItems) {
				// ignore duplicate entries
				if (currentPath.equals(pathItem) == false) {
					pathList.add(pathItem);
				}
				
				if (pathList.size() >= COMBO_HISTORY_LENGTH) {
					break;
				}
			}

			pathItems = pathList.toArray(new String[pathList.size()]);
		}
		
		if (pathItems.length > 0) {
			fDialogSettings.put(SETTINGS_EXPORT_PATH, pathItems);
		}
	}

	private void validatePath() {

		if (isPathValid()) {
			setErrorMessage(null);
		} else {
			setErrorMessage(Messages.dialog_export_invalid_path);
		}

	}

}
