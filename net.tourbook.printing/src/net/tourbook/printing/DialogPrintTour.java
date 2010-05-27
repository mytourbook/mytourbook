/*******************************************************************************
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
package net.tourbook.printing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.transform.TransformerException;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ImageComboLabel;
import net.tourbook.ui.UI;

import org.apache.fop.apps.FOPException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressIndicator;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.joda.time.DateTime;

public class DialogPrintTour extends TitleAreaDialog {

	private static final int				VERTICAL_SECTION_MARGIN		= 10;
	private static final int				SIZING_TEXT_FIELD_WIDTH		= 250;
	private static final int				COMBO_HISTORY_LENGTH		= 20;

	private static final String				STATE_PAPER_SIZE			= "printPaperSize";								//$NON-NLS-1$
	private static final String				STATE_PAPER_ORIENTATION		= "printPaperOrientation";						//$NON-NLS-1$
	
	private static final String[]			PAPER_SIZE_ITEMS;
	private static final String[]			PAPER_ORIENTATION_ITEMS;
	
	private static final String				STATE_IS_PRINT_MARKERS		= "isPrintMarkers";								//$NON-NLS-1$
	private static final String				STATE_IS_PRINT_NOTES		= "isPrintNotes";								//$NON-NLS-1$

	private static final String				STATE_PRINT_PATH_NAME		= "printPathName";								//$NON-NLS-1$
	private static final String				STATE_PRINT_FILE_NAME		= "printtFileName";								//$NON-NLS-1$
	private static final String				STATE_IS_OVERWRITE_FILES	= "isOverwriteFiles";							//$NON-NLS-1$

	private static final DecimalFormat		_intFormatter				= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat		_double2Formatter			= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final DecimalFormat		_double6Formatter			= (DecimalFormat) NumberFormat
																				.getInstance(Locale.US);
	private static final SimpleDateFormat	_dateFormat					= new SimpleDateFormat();
	private static final DateFormat			_timeFormatter				= DateFormat.getTimeInstance(DateFormat.MEDIUM);
	private static final NumberFormat		_numberFormatter			= NumberFormat.getNumberInstance();

	private static final String				PDF_FILE_EXTENSION			= "pdf";

	private static String					_dlgDefaultMessage;

	static {
		_intFormatter.applyPattern("000000"); //$NON-NLS-1$
		_double2Formatter.applyPattern("0.00"); //$NON-NLS-1$
		_double6Formatter.applyPattern("0.0000000"); //$NON-NLS-1$
		_dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
		
		PAPER_SIZE_ITEMS = new String[2];
		PAPER_SIZE_ITEMS[0] = Messages.Dialog_Print_Label_Paper_Size_A4;
		PAPER_SIZE_ITEMS[1] = Messages.Dialog_Print_Label_Paper_Size_Letter;

		PAPER_ORIENTATION_ITEMS = new String[2];
		PAPER_ORIENTATION_ITEMS[0] = Messages.Dialog_Print_Label_Paper_Orientation_Portrait;
		PAPER_ORIENTATION_ITEMS[1] = Messages.Dialog_Print_Label_Paper_Orientation_Landscape;
			
			
	}

	private final IDialogSettings			_state						= TourbookPlugin
																				.getDefault()
																				.getDialogSettingsSection(
																						"DialogPrintTour");				//$NON-NLS-1$

	private final PrintTourExtension		_printExtensionPoint;

	private final ArrayList<TourData>		_tourDataList;
	private final int						_tourStartIndex;
	private final int						_tourEndIndex;

	private Point							_shellDefaultSize;
	private Composite						_dlgContainer;

	private Button							_chkPrintMarkers;
	private Button							_chkPrintNotes;

	private Combo							_comboPaperSize;
	private Combo							_comboPaperOrientation;

	
	private Composite						_inputContainer;
	
	private Combo							_comboFile;
	private Combo							_comboPath;
	private Button							_btnSelectFile;
	private Button							_btnSelectDirectory;
	private Text							_txtFilePath;
	private Button							_chkOverwriteFiles;

	private ProgressIndicator				_progressIndicator;
	private ImageComboLabel					_lblPrintFilePath;

	private boolean							_isInit;

	public DialogPrintTour(	final Shell parentShell,
							final PrintTourExtension printExtensionPoint,
							final ArrayList<TourData> tourDataList,
							final int tourStartIndex,
							final int tourEndIndex) {

		super(parentShell);

		int shellStyle = getShellStyle();

		shellStyle = //
		SWT.NONE //
				| SWT.TITLE
				| SWT.CLOSE
				| SWT.MIN
//				| SWT.MAX
				| SWT.RESIZE
				| SWT.NONE;

		// make dialog resizable
		setShellStyle(shellStyle);

		_printExtensionPoint = printExtensionPoint;
		_tourDataList = tourDataList;
		_tourStartIndex = tourStartIndex;
		_tourEndIndex = tourEndIndex;

		_dlgDefaultMessage = NLS.bind(Messages.Dialog_Print_Dialog_Message, _printExtensionPoint.getVisibleName());

	}

	/**
	 * @return Returns <code>true</code> when a part of a tour can be printed
	 */
	private boolean canPrintTourPart() {
		return (_tourDataList.size() == 1) && (_tourStartIndex >= 0) && (_tourEndIndex > 0);
	}

	@Override
	public boolean close() {

		saveState();

		return super.close();
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_Print_Shell_Text);

		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(final Event event) {

				// allow resizing the width but not the height

				if (_shellDefaultSize == null) {
					_shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
				}

				final Point shellSize = shell.getSize();

				/*
				 * this is not working, the shell is flickering when the shell size is below min
				 * size and I found no way to prevent a resize :-(
				 */
//				if (shellSize.x < _shellDefaultSize.x) {
//					event.doit = false;
//				}

				shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
				shellSize.y = _shellDefaultSize.y;

				shell.setSize(shellSize);
			}
		});
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_Print_Dialog_Title);
		setMessage(_dlgDefaultMessage);

		_isInit = true;
		{
			restoreState();
		}
		_isInit = false;

		setFileName();
		validateFields();
		enableFields();
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.Dialog_Print_Btn_Print);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		_dlgContainer = (Composite) super.createDialogArea(parent);

		createUI(_dlgContainer);

		return _dlgContainer;
	}

	private void createUI(final Composite parent) {

		_inputContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
		GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);

		createUIPaperFormat(_inputContainer);
		createUIOption(_inputContainer);
		createUIDestination(_inputContainer);
		createUIProgress(parent);
	}

	private void createUIDestination(final Composite parent) {

		Label label;

		final ModifyListener filePathModifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				validateFields();
			}
		};

		/*
		 * group: filename
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Print_Group_PdfFileName);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		{
			/*
			 * label: filename
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_FileName);

			/*
			 * combo: path
			 */
			_comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
			((GridData) _comboFile.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboFile.setVisibleItemCount(20);
			_comboFile.addVerifyListener(net.tourbook.util.UI.verifyFilenameInput());
			_comboFile.addModifyListener(filePathModifyListener);
			_comboFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectFile = new Button(group, SWT.PUSH);
			_btnSelectFile.setText(Messages.app_btn_browse);
			_btnSelectFile.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseFile();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectFile);

			// -----------------------------------------------------------------------------

			/*
			 * label: path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_PrintFilePath);

			/*
			 * combo: path
			 */
			_comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
			((GridData) _comboPath.getLayoutData()).widthHint = SIZING_TEXT_FIELD_WIDTH;
			_comboPath.setVisibleItemCount(20);
			_comboPath.addModifyListener(filePathModifyListener);
			_comboPath.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					validateFields();
				}
			});

			/*
			 * button: browse
			 */
			_btnSelectDirectory = new Button(group, SWT.PUSH);
			_btnSelectDirectory.setText(Messages.app_btn_browse);
			_btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectBrowseDirectory();
					validateFields();
				}
			});
			setButtonLayoutData(_btnSelectDirectory);

			// -----------------------------------------------------------------------------

			/*
			 * checkbox: overwrite files
			 */
			_chkOverwriteFiles = new Button(group, SWT.CHECK);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).span(3, 1).applyTo(_chkOverwriteFiles);
			_chkOverwriteFiles.setText(Messages.Dialog_Print_Chk_OverwriteFiles);
			_chkOverwriteFiles.setToolTipText(Messages.Dialog_Print_Chk_OverwriteFiles_Tooltip);
			
			// -----------------------------------------------------------------------------

			/*
			 * label: file path
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Dialog_Print_Label_FilePath);

			/*
			 * text: filename
			 */
			_txtFilePath = new Text(group, /* SWT.BORDER | */SWT.READ_ONLY);
			GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
			_txtFilePath.setToolTipText(Messages.Dialog_Print_Txt_FilePath_Tooltip);
			_txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

			// spacer
//			new Label(group, SWT.NONE);
		}

	}

	private void createUIPaperFormat(final Composite parent) {

		// container
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Print_Group_Paper);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group); 
		{
			createUIPaperSize(group);
			createUIPaperOrientation(group);
		}
	}
	
	private void createUIPaperSize(final Composite parent){
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Dialog_Print_Label_Paper_Size);
		_comboPaperSize = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		_comboPaperSize.setVisibleItemCount(2);
		_comboPaperSize.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		_comboPaperSize.setItems(PAPER_SIZE_ITEMS);
	}
	
	private void createUIPaperOrientation(final Composite parent){
		final Label label = new Label(parent, SWT.NONE);
		label.setText(Messages.Dialog_Print_Label_Paper_Orientation);
		_comboPaperOrientation = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		_comboPaperOrientation.setVisibleItemCount(2);
		_comboPaperOrientation.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		_comboPaperOrientation.setItems(PAPER_ORIENTATION_ITEMS);
	}
	
	
	private void createUIOption(final Composite parent) {

		// container
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Dialog_Print_Group_Options);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
		{
			createUIOptionPrintMarkers(group);
			createUIOptionPrintNotes(group);
		}
	}

	private void createUIOptionPrintMarkers(final Composite parent) {

		/*
		 * checkbox: print markers
		 */
		_chkPrintMarkers = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkPrintMarkers);
		_chkPrintMarkers.setText(Messages.Dialog_Print_Chk_PrintMarkers);
		_chkPrintMarkers.setToolTipText(Messages.Dialog_Print_Chk_PrintMarkers_Tooltip);
	}

	private void createUIOptionPrintNotes(final Composite parent) {

		/*
		 * checkbox: print notes
		 */
		_chkPrintNotes = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_chkPrintNotes);
		_chkPrintNotes.setText(Messages.Dialog_Print_Chk_PrintNotes);
		_chkPrintNotes.setToolTipText(Messages.Dialog_Print_Chk_PrintNotes_Tooltip);
	}

	private void createUIProgress(final Composite parent) {

		final int selectedTours = _tourDataList.size();

		// hide progress bar when only one tour is printed
		if (selectedTours < 2) {
			return;
		}

		// container
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, VERTICAL_SECTION_MARGIN).applyTo(container);
		GridLayoutFactory.swtDefaults().margins(10, 5).numColumns(1).applyTo(container);
		{
			/*
			 * progress indicator
			 */
			_progressIndicator = new ProgressIndicator(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_progressIndicator);

			/*
			 * label: printed filename
			 */
			_lblPrintFilePath = new ImageComboLabel(container, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblPrintFilePath);
		}
	}

	private void doPrint() throws IOException {

		// disable button's
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);

		final String completeFilePath = _txtFilePath.getText();

		final PrintSettings printSettings = new PrintSettings();
		printSettings.setCompleteFilePath(completeFilePath);
		
		switch (_comboPaperSize.getSelectionIndex()) {
		case 0:
			printSettings.setPaperSize(PaperSize.A4);
			break;
		case 1:
			printSettings.setPaperSize(PaperSize.LETTER);
			break;

		default:
			break;
		}
		
		switch (_comboPaperOrientation.getSelectionIndex()) {
		case 0:
			printSettings.setPaperOrientation(PaperOrientation.PORTRAIT);
			break;
		case 1:
			printSettings.setPaperOrientation(PaperOrientation.LANDSCAPE);
			break;

		default:
			break;
		}
	
		printSettings.setOverwriteFiles(_chkOverwriteFiles.getSelection());
		printSettings.setPrintMarkers(_chkPrintMarkers.getSelection());
		printSettings.setPrintDescription(_chkPrintNotes.getSelection());
		
		
		if (_tourDataList.size() == 1) {
			// print one tour
			final TourData tourData = _tourDataList.get(0);
			
			if (_printExtensionPoint instanceof PrintTourPDF) {
				try {
					//TODO handle exception
					//System.out.println("tour id:"+tourData.getTourId());	
					((PrintTourPDF)_printExtensionPoint).printPDF(tourData, printSettings);
				} catch (FOPException e) {
					e.printStackTrace();
				} catch (TransformerException e) {
					e.printStackTrace();
				}
			}
		} else {
			/*
			 * print each tour separately
			 */

			final String printPathName = getPrintPathName();
			_progressIndicator.beginTask(_tourDataList.size());

			final Job printJob = new Job("print tours") { //$NON-NLS-1$
				@Override
				public IStatus run(final IProgressMonitor monitor) {

					monitor.beginTask(UI.EMPTY_STRING, _tourDataList.size());
					final IPath printFilePath = new Path(printPathName).addTrailingSeparator();

					for (final TourData tourData : _tourDataList) {

						// get filepath
						final IPath filePath = printFilePath
								.append(UI.format_yyyymmdd_hhmmss(tourData))
								.addFileExtension(PDF_FILE_EXTENSION);

						/*
						 *	print: update dialog progress monitor
						 */
						Display.getDefault().syncExec(new Runnable() {
							public void run() {

								// display printed filepath
								_lblPrintFilePath.setText(NLS.bind(Messages.Dialog_Print_Lbl_PdfFilePath, filePath
										.toOSString()));

								// !!! force label update !!!
								_lblPrintFilePath.update();

								_progressIndicator.worked(1);

							}
						});

						
						if (_printExtensionPoint instanceof PrintTourPDF) {
							try {
								//TODO handle exception
								printSettings.setCompleteFilePath(filePath.toOSString());
								((PrintTourPDF)_printExtensionPoint).printPDF(tourData, printSettings);
							} catch (FOPException e) {
								e.printStackTrace();
							} catch (TransformerException e) {
								e.printStackTrace();
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							}
						}			
					}

					return Status.OK_STATUS;
				}
			};

			printJob.schedule();
			try {
				printJob.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void enablePrintButton(final boolean isEnabled) {
		final Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton != null) {

			okButton.setEnabled(isEnabled);
		}
	}

	private void enableFields() {
		_comboFile.setEnabled(true);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}

	private String getPrintFileName() {
		return _comboFile.getText().trim();
	}

	private String getPrintPathName() {
		return _comboPath.getText().trim();
	}

	private String[] getUniqueItems(final String[] pathItems, final String currentItem) {

		final ArrayList<String> pathList = new ArrayList<String>();

		pathList.add(currentItem);

		for (final String pathItem : pathItems) {

			// ignore duplicate entries
			if (currentItem.equals(pathItem) == false) {
				pathList.add(pathItem);
			}

			if (pathList.size() >= COMBO_HISTORY_LENGTH) {
				break;
			}
		}

		return pathList.toArray(new String[pathList.size()]);
	}

	@Override
	protected void okPressed() {

		UI.disableAllControls(_inputContainer);

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				try {
					doPrint();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		});

		super.okPressed();
	}

	private void onSelectBrowseDirectory() {

		final DirectoryDialog dialog = new DirectoryDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.Dialog_Print_Dir_Dialog_Text);
		dialog.setMessage(Messages.Dialog_Print_Dir_Dialog_Message);

		dialog.setFilterPath(getPrintPathName());

		final String selectedDirectoryName = dialog.open();

		if (selectedDirectoryName != null) {
			setErrorMessage(null);
			_comboPath.setText(selectedDirectoryName);
		}
	}

	private void onSelectBrowseFile() {

		final String fileExtension = PDF_FILE_EXTENSION;

		final FileDialog dialog = new FileDialog(_dlgContainer.getShell(), SWT.SAVE);
		dialog.setText(Messages.Dialog_Print_File_Dialog_Text);

		dialog.setFilterPath(getPrintPathName());
		dialog.setFilterExtensions(new String[] { fileExtension });
		dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

		final String selectedFilePath = dialog.open();

		if (selectedFilePath != null) {
			setErrorMessage(null);
			_comboFile.setText(new Path(selectedFilePath).toFile().getName());
		}
	}

	private void restoreState() {
		try {
			_comboPaperSize.select(_state.getInt(STATE_PAPER_SIZE));
			_comboPaperOrientation.select(_state.getInt(STATE_PAPER_ORIENTATION));			
		} catch (NumberFormatException nfe){
			_comboPaperSize.select(0);
			_comboPaperOrientation.select(0);
		}
		
		_chkPrintMarkers.setSelection(_state.getBoolean(STATE_IS_PRINT_MARKERS));
		_chkPrintNotes.setSelection(_state.getBoolean(STATE_IS_PRINT_NOTES));

		// print file/path
		UI.restoreCombo(_comboFile, _state.getArray(STATE_PRINT_FILE_NAME));
		UI.restoreCombo(_comboPath, _state.getArray(STATE_PRINT_PATH_NAME));
		_chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_FILES));
	}

	private void saveState() {
		_state.put(STATE_PAPER_SIZE, _comboPaperSize.getSelectionIndex());
		_state.put(STATE_PAPER_ORIENTATION, _comboPaperOrientation.getSelectionIndex());
		
		// print file/path
		if (validateFilePath()) {
			_state.put(STATE_PRINT_PATH_NAME, getUniqueItems(_comboPath.getItems(), getPrintPathName()));
			_state.put(STATE_PRINT_FILE_NAME, getUniqueItems(_comboFile.getItems(), getPrintFileName()));
		}

		_state.put(STATE_IS_OVERWRITE_FILES, _chkOverwriteFiles.getSelection());
		_state.put(STATE_IS_PRINT_MARKERS, _chkPrintMarkers.getSelection());
		_state.put(STATE_IS_PRINT_NOTES, _chkPrintNotes.getSelection());
	}

	private void setError(final String message) {
		setErrorMessage(message);
		enablePrintButton(false);
	}

	/**
	 * Overwrite filename with the first tour date/time when the tour is not merged
	 */
	private void setFileName() {

		// search for the first tour
		TourData minTourData = null;
		final long minTourMillis = 0;

		for (final TourData tourData : _tourDataList) {
			final DateTime checkingTourDate = TourManager.getTourDateTime(tourData);

			if (minTourData == null) {
				minTourData = tourData;
			} else {

				final long tourMillis = checkingTourDate.getMillis();
				if (tourMillis < minTourMillis) {
					minTourData = tourData;
				}
			}
		}

		if ((_tourDataList.size() == 1) && (_tourStartIndex != -1) && (_tourEndIndex != -1)) {

			// display the start date/time

			final DateTime dtTour = new DateTime(minTourData.getStartYear(), minTourData.getStartMonth(), minTourData
					.getStartDay(), minTourData.getStartHour(), minTourData.getStartMinute(), minTourData
					.getStartSecond(), 0);

			final int startTime = minTourData.timeSerie[_tourStartIndex];
			final DateTime tourTime = dtTour.plusSeconds(startTime);

			_comboFile.setText(UI
					.format_yyyymmdd_hhmmss(
							tourTime.getYear(),
							tourTime.getMonthOfYear(),
							tourTime.getDayOfMonth(),
							tourTime.getHourOfDay(),
							tourTime.getMinuteOfHour(),
							tourTime.getSecondOfMinute()));
		} else {

			// display the tour date/time

			_comboFile.setText(UI.format_yyyymmdd_hhmmss(minTourData));
		}
	}

	private void validateFields() {

		if (_isInit) {
			return;
		}

		/*
		 * validate fields
		 */

		if (validateFilePath() == false) {
			return;
		}

		setErrorMessage(null);
		enablePrintButton(true);
	}

	private boolean validateFilePath() {

		// check path
		IPath filePath = new Path(getPrintPathName());
		if (new File(filePath.toOSString()).exists() == false) {

			// invalid path
			setError(NLS.bind(Messages.Dialog_Print_Msg_PathIsNotAvailable, filePath.toOSString()));
			return false;
		}

		boolean returnValue = false;

		String fileName = getPrintFileName();

		// remove extentions
		final int extPos = fileName.indexOf('.');
		if (extPos != -1) {
			fileName = fileName.substring(0, extPos);
		}

		// build file path with extension
		filePath = filePath.addTrailingSeparator().append(fileName).addFileExtension(PDF_FILE_EXTENSION);

		final File newFile = new File(filePath.toOSString());

		if ((fileName.length() == 0) || newFile.isDirectory()) {

			// invalid filename

			setError(Messages.Dialog_Print_Msg_FileNameIsInvalid);

		} else if (newFile.exists()) {
			
			// file already exists

			setMessage(
					NLS.bind(Messages.Dialog_Print_Msg_FileAlreadyExists, filePath.toOSString()),
					IMessageProvider.WARNING);
			returnValue = true;

		} else {

			setMessage(_dlgDefaultMessage);

			try {
				final boolean isFileCreated = newFile.createNewFile();

				// name is correct

				if (isFileCreated) {
					// delete file because the file is created for checking validity
					newFile.delete();
				}
				returnValue = true;

			} catch (final IOException ioe) {
				setError(Messages.Dialog_Print_Msg_FileNameIsInvalid);
			}

		}

		_txtFilePath.setText(filePath.toOSString());

		return returnValue;
	}
}
