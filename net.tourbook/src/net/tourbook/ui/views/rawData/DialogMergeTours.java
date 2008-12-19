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
package net.tourbook.ui.views.rawData;

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ISliderMoveListener;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.IDataModelListener;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.action.ActionOpenPrefDialog;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.tourChart.TourChartConfiguration;
import net.tourbook.util.PixelConverter;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;

public class DialogMergeTours extends TitleAreaDialog implements ITourProvider {

	private static final int		MAX_ADJUST_SECONDS		= 60;
	private static final int		MAX_ADJUST_MINUTES		= 60;								// x 60
	private static final int		MAX_ADJUST_HOURS		= 5;								// x 60 x 60
	private static final int		MAX_ADJUST_ALTITUDE_1	= 10;
	private static final int		MAX_ADJUST_ALTITUDE_10	= 50;								// x 10

	private Image					fShellImage;

	private final IDialogSettings	fDialogSettings;

	private TourData				fFromTourData;
	private TourData				fIntoTourData;

	private Composite				fDlgContainer;

	private TourChart				fTourChart;
	private TourChartConfiguration	fTourChartConfig;

	private Label					fLabelAltitudeDiff1;
	private Label					fLabelAltitudeDiff10;
	private Scale					fScaleAltitude1;
	private Scale					fScaleAltitude10;
	private Label					fLabelAdjustSecondsValue;

	private Label					fLabelAdjustMinuteValue;
	private Label					fLabelAdjustHourValue;
	private Scale					fScaleAdjustSeconds;
	private Scale					fScaleAdjustMinutes;
	private Scale					fScaleAdjustHours;
	private Button					fBtnResetAdjustment;

	private Button					fBtnResetValues;

	/*
	 * save actions
	 */
	private Button					fChkKeepHVAdjustments;
	private Button					fChkAdjustAltitudeFromSource;
	private Button					fChkAdjustStartAltitude;
	private Button					fChkMergeTemperature;

	private Label					fLblAdjustAltiValueTimeUnit;
	private Label					fLblAdjustAltiValueDistanceUnit;
	private Label					fLblAdjustAltiValueTime;
	private Label					fLblAdjustAltiValueDistance;

	private Button					fChkSetTourType;
	private Link					fTourTypeLink;

	private CLabel					fLblTourType;

	/*
	 * display actions
	 */
	private Button					fChkAltiDiffScaling;

	private Button					fChkPreviewChart;
	private boolean					fIsDirtyDisabled;

	private boolean					fIsTourDirty			= false;
	private boolean					fIsTourSaved			= false;
	private boolean					fIsMergeFromTourModified;
	private boolean					fIsChartUpdated;

	/*
	 * backup data
	 */
	private int[]					fBackupFromTimeSerie;

	private int[]					fBackupFromDistanceSerie;
	private int[]					fBackupFromAltitudeSerie;
	private TourType				fBackupFromTourType;
	private int[]					fBackupIntoTemperatureSerie;

	private int						fBackupIntoTimeOffset;
	private int						fBackupIntoAltitudeOffset;
	private ActionOpenPrefDialog	fActionOpenTourTypePrefs;

	private ITourEventListener		fTourEventListener;

	private NumberFormat			fNumberFormatter		= NumberFormat.getNumberInstance();
	{
		fNumberFormatter.setMinimumFractionDigits(3);
		fNumberFormatter.setMaximumFractionDigits(3);
	}

	/**
	 * creates a int array backup
	 * 
	 * @param source
	 * @return the backup array or <code>null</code> when the source is <code>null</code>
	 */
	private static int[] createDataSerieBackup(final int[] source) {

		int[] backup = null;

		if (source != null) {
			final int serieLength = source.length;
			backup = new int[serieLength];
			System.arraycopy(source, 0, backup, 0, serieLength);
		}

		return backup;
	}

	/**
	 * @param parentShell
	 * @param mergeFromTour
	 *            {@link TourData} for the tour which is merge into the other tour
	 * @param mergeIntoTour
	 *            {@link TourData} for the tour into which the other tour is merged
	 */
	public DialogMergeTours(final Shell parentShell, final TourData mergeFromTour, final TourData mergeIntoTour) {

		super(parentShell);

		// make dialog resizable and display maximize button
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);

		// set icon for the window 
		fShellImage = TourbookPlugin.getImageDescriptor(Messages.image__merge_tours).createImage();
		setDefaultImage(fShellImage);

		fFromTourData = mergeFromTour;
		fIntoTourData = mergeIntoTour;

		fDialogSettings = TourbookPlugin.getDefault().getDialogSettingsSection(getClass().getName());
	}

	private void addTourEventListener() {

		fTourEventListener = new ITourEventListener() {
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

					final TourEvent tourEvent = (TourEvent) eventData;
					final ArrayList<TourData> modifiedTours = tourEvent.getModifiedTours();

					if (modifiedTours == null) {
						return;
					}

					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId() == fFromTourData.getTourId()) {

							// tour tag could be modified

							updateUIFromTourData();

							fIsMergeFromTourModified = true;

							setTourDirty();

							// nothing more to do
							return;
						}
					}
				}
			}
		};

		TourManager.getInstance().addTourEventListener(fTourEventListener);
	}

	/**
	 * set button width
	 */
	private void adjustButtonWidth() {

		final int btnResetAdj = fBtnResetAdjustment.getBounds().width;
		final int btnResetValues = fBtnResetValues.getBounds().width;
		final int newWidth = Math.max(btnResetAdj, btnResetValues);

		GridData gd;
		gd = (GridData) fBtnResetAdjustment.getLayoutData();
		gd.widthHint = newWidth;

		gd = (GridData) fBtnResetValues.getLayoutData();
		gd.widthHint = newWidth;
	}

	@Override
	public boolean close() {

		saveState();

		if (fIsTourSaved == false) {

			// tour is not saved, dialog is canceled, restore original values

			restoreDataBackup();
		}

		/**
		 * this is a tricky thing:
		 * <p>
		 * when the tour is not saved, the tour must be reverted
		 * <p>
		 * when the tour is saved, reverting the tour sets the editor to not dirty, tour data have
		 * already been saved
		 */
		if (fIsMergeFromTourModified) {

			// revert modified tour type in the merge from tour

			final TourEvent tourEvent = new TourEvent(fFromTourData);
			tourEvent.isReverted = true;

			TourManager.fireEvent(TourEventId.TOUR_CHANGED, tourEvent);
		}

		return super.close();
	}

	private void computeMergedData() {

		final int xMergeOffset = fIntoTourData.getMergedTourTimeOffset();
		final int yMergeOffset = fIntoTourData.getMergedAltitudeOffset();

		final int[] targetTimeSerie = fIntoTourData.timeSerie;
		final int[] targetDistanceSerie = fIntoTourData.distanceSerie;
		final int[] targetAltitudeSerie = fIntoTourData.altitudeSerie;

		final int[] sourceTimeSerie = fFromTourData.timeSerie;
		final int[] sourceAltitudeSerie = fFromTourData.altitudeSerie;
		final int[] sourceTemperatureSerie = fFromTourData.temperatureSerie;

		// check if the data series are available
		final boolean isIntoDistance = targetDistanceSerie != null;
		final boolean isFromTemperature = sourceTemperatureSerie != null;

		final int lastFromIndex = sourceTimeSerie.length - 1;
		final int serieLength = targetTimeSerie.length;

		final int[] newSourceTimeSerie = new int[serieLength];
		final int[] newSourceAltitudeSerie = new int[serieLength];
		final int[] newSourceAltiDiffSerie = new int[serieLength];

		final int[] newIntoTemperatureSerie = new int[serieLength];

		int sourceIndex = 0;

		int sourceTime = sourceTimeSerie[0] + xMergeOffset;
		int sourceAltitude = sourceAltitudeSerie[0] + yMergeOffset;

		int prevSourceTime = 0;
		int prevSourceAlti = sourceAltitude;
		int newSourceAltitude = sourceAltitude;

		int targetTime = targetTimeSerie[0];
		int targetAltitude = targetAltitudeSerie[0];

		/*
		 * create new time/distance serie for the source tour according to the time of the target
		 * tour
		 */
		for (int targetIndex = 0; targetIndex < serieLength; targetIndex++) {

			targetTime = targetTimeSerie[targetIndex];

			/*
			 * target tour is the leading data serie, move time forward for the source time
			 */
			while (sourceTime < targetTime) {

				sourceIndex++;

				// check array bounds
				sourceIndex = (sourceIndex <= lastFromIndex) ? sourceIndex : lastFromIndex;

				if (sourceIndex == lastFromIndex) {
					//prevent endless loops
					break;
				}

				prevSourceTime = sourceTime;
				prevSourceAlti = sourceAltitude;

				sourceTime = sourceTimeSerie[sourceIndex] + xMergeOffset;
				sourceAltitude = sourceAltitudeSerie[sourceIndex] + yMergeOffset;
			}

			targetAltitude = targetAltitudeSerie[targetIndex];

			/**
			 * do linear interpolation for the altitude
			 * <p>
			 * y2 = (x2-x1)(y3-y1)/(x3-x1) + y1
			 */
			final int x1 = prevSourceTime;
			final int x2 = targetTime;
			final int x3 = sourceTime;
			final int y1 = prevSourceAlti;
			final int y3 = sourceAltitude;

			final int xQ1 = x2 - x1;
			final int xQ2 = x3 - x1;
			final int yQ1 = y3 - y1;

			if (xQ2 == 0) {
				newSourceAltitude = prevSourceAlti;
			} else {
				newSourceAltitude = xQ1 * yQ1 / xQ2 + y1;
			}

			newSourceAltitudeSerie[targetIndex] = newSourceAltitude;
			newSourceAltiDiffSerie[targetIndex] = newSourceAltitude - targetAltitude;

			newSourceTimeSerie[targetIndex] = targetTime;

			if (isFromTemperature) {
				newIntoTemperatureSerie[targetIndex] = sourceTemperatureSerie[sourceIndex];
			}
		}

		fFromTourData.mergeAltitudeSerie = newSourceAltitudeSerie;
		fFromTourData.mergeAltitudeDiff = newSourceAltiDiffSerie;

		if (isFromTemperature) {
			fIntoTourData.temperatureSerie = newIntoTemperatureSerie;
		}

		float altiDiffTime = 0;
		float altiDiffDist = 0;

		if (fChkAdjustStartAltitude.getSelection() && isIntoDistance) {

			final int[] adjustedIntoAltitudeSerie = new int[serieLength];

			float startAltiDiff = newSourceAltiDiffSerie[0];
			final int endIndex = fTourChart.getXSliderPosition().getLeftSliderValueIndex();
			final float distanceDiff = targetDistanceSerie[endIndex];

			final int[] altitudeSerie = fIntoTourData.altitudeSerie;

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {

				if (serieIndex < endIndex) {

					// add adjusted altitude

					final float intoDistance = targetDistanceSerie[serieIndex];
					final float distanceScale = 1 - intoDistance / distanceDiff;
					final int adjustedAltiDiff = (int) (startAltiDiff * distanceScale);
					final int newAltitude = altitudeSerie[serieIndex] + adjustedAltiDiff;

					adjustedIntoAltitudeSerie[serieIndex] = newAltitude;
					newSourceAltiDiffSerie[serieIndex] = newSourceAltitudeSerie[serieIndex] - newAltitude;

				} else {

					// add altitude which are not adjusted

					adjustedIntoAltitudeSerie[serieIndex] = altitudeSerie[serieIndex];
				}
			}

			fFromTourData.mergeAdjustedAltitudeSerie = adjustedIntoAltitudeSerie;

			startAltiDiff /= UI.UNIT_VALUE_ALTITUDE;

			// meter/min
			altiDiffTime = startAltiDiff / (targetTimeSerie[endIndex] / 60);
			// meter/meter
			altiDiffDist = ((startAltiDiff * 1000) / targetDistanceSerie[endIndex]) / UI.UNIT_VALUE_DISTANCE;

		} else if (fChkAdjustAltitudeFromSource.getSelection() && isIntoDistance) {

			final int[] newTargetAltitudeSerie = new int[serieLength];

			for (int serieIndex = 0; serieIndex < serieLength; serieIndex++) {
				newTargetAltitudeSerie[serieIndex] = newSourceAltitudeSerie[serieIndex];
			}

			fFromTourData.mergeAdjustedAltitudeSerie = newTargetAltitudeSerie;

		} else {

			// disable adjusted altitude
			fFromTourData.mergeAdjustedAltitudeSerie = null;
		}

		updateUI(altiDiffTime, altiDiffDist);
	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.tour_merger_dialog_title);

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {
				onDispose();
			}
		});
	}

	@Override
	public void create() {

		// this will create the UI widgets
		super.create();

		setTitle(NLS.bind(Messages.tour_merger_dialog_header_title,
				TourManager.getTourTitle(fIntoTourData),
				fIntoTourData.getDeviceName()));

		setMessage(NLS.bind(Messages.tour_merger_dialog_header_message,
				TourManager.getTourTitle(fFromTourData),
				fFromTourData.getDeviceName()));

		createDataBackup();

		addTourEventListener();
		createActions();

		restoreState();

		computeMergedData();

		// set alti diff scaling
		fTourChartConfig.isRelativeAltiDiffScaling = fChkAltiDiffScaling.getSelection();
		fTourChart.updateMergeLayer(true);

		updateUIFromTourData();

		// update chart after the UI is updated from the tour
		updateTourChart();

		enableActions();

		adjustButtonWidth();
		fDlgContainer.layout(true, true);
	}

	private void createActions() {

		fActionOpenTourTypePrefs = new ActionOpenPrefDialog(Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// rename OK button
		getButton(IDialogConstants.OK_ID).setText(Messages.app_action_save);
	}

	private void createDataBackup() {

		/*
		 * keep a backup of the altitude data because these data will be changed in this dialog
		 */
		fBackupFromTimeSerie = createDataSerieBackup(fFromTourData.timeSerie);
		fBackupFromDistanceSerie = createDataSerieBackup(fFromTourData.distanceSerie);
		fBackupFromAltitudeSerie = createDataSerieBackup(fFromTourData.altitudeSerie);
		fBackupFromTourType = fFromTourData.getTourType();

		fBackupIntoTemperatureSerie = createDataSerieBackup(fIntoTourData.temperatureSerie);

		fBackupIntoTimeOffset = fIntoTourData.getMergedTourTimeOffset();
		fBackupIntoAltitudeOffset = fIntoTourData.getMergedAltitudeOffset();
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		fDlgContainer = (Composite) super.createDialogArea(parent);

		createUI(fDlgContainer);

		return fDlgContainer;
	}

	private void createUI(final Composite parent) {

		final Composite dlgContainer = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(dlgContainer);
		GridLayoutFactory.fillDefaults().margins(10, 0).numColumns(1).applyTo(dlgContainer);

		createUITourChart(dlgContainer);

		/*
		 * column: adjustments
		 */
		final Composite columnContainer = new Composite(dlgContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(columnContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).spacing(20, 0).applyTo(columnContainer);

		createUISectionAdjustments(columnContainer);

		/*
		 * column: options
		 */
		final Composite optionContainer = new Composite(columnContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(false, false).applyTo(optionContainer);
		GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(1).applyTo(optionContainer);

		createUISectionDisplayOptions(optionContainer);
		createUISectionSaveActions(optionContainer);
	}

	/**
	 * group: adjust time
	 */
	private void createUIGroupHorizAdjustment(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);
		final int valueWidth = pc.convertWidthInCharsToPixels(4);
		Label label;

		final Group groupTime = new Group(parent, SWT.NONE);
		groupTime.setText(Messages.tour_merger_group_adjust_time);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(groupTime);
		GridLayoutFactory.fillDefaults().numColumns(4).extendedMargins(5, 0, 0, 0).spacing(0, 0).applyTo(groupTime);

		/*
		 * scale: adjust seconds
		 */
		fLabelAdjustSecondsValue = new Label(groupTime, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustSecondsValue);

		label = new Label(groupTime, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(groupTime, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_seconds);

		fScaleAdjustSeconds = new Scale(groupTime, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustSeconds);

		fScaleAdjustSeconds.setMinimum(0);
		fScaleAdjustSeconds.setMaximum(MAX_ADJUST_SECONDS * 2);
//		fScaleAdjustSeconds.setPageIncrement(1);
		fScaleAdjustSeconds.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * container: minute and hour scale
		 */
		final Composite minContainer = new Composite(groupTime, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).span(4, 1).applyTo(minContainer);
		GridLayoutFactory.fillDefaults().numColumns(8).spacing(0, 0).applyTo(minContainer);

		/*
		 * scale: adjust minutes
		 */
		fLabelAdjustMinuteValue = new Label(minContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustMinuteValue);

		label = new Label(minContainer, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(minContainer, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_minutes);

		fScaleAdjustMinutes = new Scale(minContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustMinutes);
		fScaleAdjustMinutes.setMinimum(0);
		fScaleAdjustMinutes.setMaximum(MAX_ADJUST_MINUTES * 2);
		fScaleAdjustMinutes.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * scale: adjust hours
		 */
		fLabelAdjustHourValue = new Label(minContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(valueWidth, SWT.DEFAULT)
				.applyTo(fLabelAdjustHourValue);

		label = new Label(minContainer, SWT.NONE);
		label.setText(UI.SPACE);

		label = new Label(minContainer, SWT.NONE);
		label.setText(Messages.tour_merger_label_adjust_hours);

		fScaleAdjustHours = new Scale(minContainer, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAdjustHours);
		fScaleAdjustHours.setMinimum(0);
		fScaleAdjustHours.setMaximum(MAX_ADJUST_HOURS * 2);
		fScaleAdjustHours.setPageIncrement(1);
		fScaleAdjustHours.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
	}

	/**
	 * group: adjust altitude
	 */
	private void createUIGroupVertAdjustment(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		final Group groupAltitude = new Group(parent, SWT.NONE);
		groupAltitude.setText(Messages.tour_merger_group_adjust_altitude);
		GridDataFactory.swtDefaults().grab(true, false).align(SWT.FILL, SWT.BEGINNING).applyTo(groupAltitude);
		GridLayoutFactory.fillDefaults().numColumns(2).extendedMargins(0, 0, 0, 0).spacing(0, 0).applyTo(groupAltitude);
//		groupAltitude.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));

		/*
		 * scale: altitude 10m
		 */
		fLabelAltitudeDiff1 = new Label(groupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff1);

		fScaleAltitude1 = new Scale(groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude1);
		fScaleAltitude1.setMinimum(0);
		fScaleAltitude1.setMaximum(MAX_ADJUST_ALTITUDE_1 * 2);
		fScaleAltitude1.setPageIncrement(1);
		fScaleAltitude1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});

		/*
		 * scale: altitude 100m
		 */
		fLabelAltitudeDiff10 = new Label(groupAltitude, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.align(SWT.END, SWT.CENTER)
				.hint(pc.convertWidthInCharsToPixels(8), SWT.DEFAULT)
				.applyTo(fLabelAltitudeDiff10);

		fScaleAltitude10 = new Scale(groupAltitude, SWT.HORIZONTAL);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fScaleAltitude10);
		fScaleAltitude10.setMinimum(0);
		fScaleAltitude10.setMaximum(MAX_ADJUST_ALTITUDE_10 * 2);
		fScaleAltitude10.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModifyProperties();
			}
		});
	}

	private void createUISectionAdjustments(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

		createUIGroupHorizAdjustment(container);
		createUIGroupVertAdjustment(container);
		createUISectionResetButtons(container);
	}

	private void createUISectionDisplayOptions(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);

		/*
		 * checkbox: display relative or absolute scale
		 */
		fChkAltiDiffScaling = new Button(container, SWT.CHECK);
		fChkAltiDiffScaling.setText(Messages.tour_merger_chk_alti_diff_scaling);
		fChkAltiDiffScaling.setToolTipText(Messages.tour_merger_chk_alti_diff_scaling_tooltip);
		fChkAltiDiffScaling.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onModifyProperties();
				fIsDirtyDisabled = false;
			}
		});

		/*
		 * checkbox: preview chart
		 */
		fChkPreviewChart = new Button(container, SWT.CHECK);
		fChkPreviewChart.setText(Messages.tour_merger_chk_preview_graphs);
		fChkPreviewChart.setToolTipText(Messages.tour_merger_chk_preview_graphs_tooltip);
		fChkPreviewChart.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				fIsDirtyDisabled = true;
				onModifyProperties();
				fIsDirtyDisabled = false;
			}
		});
	}

	private void createUISectionResetButtons(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).indent(0, 0).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);

//		// label for horizontal trail adjustment
//		final Label label = new Label(container, SWT.NONE);
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(label);

		/*
		 * button: reset all adjustment options
		 */
		fBtnResetAdjustment = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).applyTo(fBtnResetAdjustment);
		fBtnResetAdjustment.setText(Messages.tour_merger_btn_reset_adjustment);
		fBtnResetAdjustment.setToolTipText(Messages.tour_merger_btn_reset_adjustment_tooltip);
		fBtnResetAdjustment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetAdjustments();
			}
		});

		/*
		 * button: show original values
		 */
		fBtnResetValues = new Button(container, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.END).applyTo(fBtnResetValues);
		fBtnResetValues.setText(Messages.tour_merger_btn_reset_values);
		fBtnResetValues.setToolTipText(Messages.tour_merger_btn_reset_values_tooltip);
		fBtnResetValues.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectResetValues();
			}
		});
	}

	/**
	 * @param parent
	 */
	private void createUISectionSaveActions(final Composite parent) {

		final PixelConverter pc = new PixelConverter(parent);

		/*
		 * group: save options
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.tour_merger_group_save_actions);
		group.setToolTipText(Messages.tour_merger_group_save_actions_tooltip);
		GridDataFactory.swtDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.END)
				.indent(0, 10)
				.applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);

		/*
		 * checkbox: keep horiz. and vert. adjustments
		 */
		fChkKeepHVAdjustments = new Button(group, SWT.CHECK);
		fChkKeepHVAdjustments.setText(Messages.tour_merger_chk_keep_horiz_vert_adjustments);
		fChkKeepHVAdjustments.setToolTipText(Messages.tour_merger_chk_keep_horiz_vert_adjustments_tooltip);

		/*
		 * checkbox: merge temperature
		 */
		fChkMergeTemperature = new Button(group, SWT.CHECK);
		fChkMergeTemperature.setText(Messages.tour_merger_chk_merge_temperature);
		fChkMergeTemperature.setToolTipText(Messages.tour_merger_chk_merge_temperature_tooltip);
		fChkMergeTemperature.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (fChkMergeTemperature.getSelection()) {
					setTourDirty();
				}
			}
		});

		/*
		 * checkbox: adjust altitude from source
		 */
		fChkAdjustAltitudeFromSource = new Button(group, SWT.CHECK);
		fChkAdjustAltitudeFromSource.setText(Messages.tour_merger_chk_adjust_altitude_from_source);
		fChkAdjustAltitudeFromSource.setToolTipText(Messages.tour_merger_chk_adjust_altitude_from_source_tooltip);
		fChkAdjustAltitudeFromSource.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// only one altitude adjustment can be done
				fChkAdjustStartAltitude.setSelection(false);

				onModifyProperties();
			}
		});

		/*
		 * checkbox: adjust start altitude
		 */
		fChkAdjustStartAltitude = new Button(group, SWT.CHECK);
		fChkAdjustStartAltitude.setText(Messages.tour_merger_chk_adjust_start_altitude);
		fChkAdjustStartAltitude.setToolTipText(Messages.tour_merger_chk_adjust_start_altitude_tooltip);
		fChkAdjustStartAltitude.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				// only one altitude adjustment can be done
				fChkAdjustAltitudeFromSource.setSelection(false);

				final boolean isAdjustAltitude = fChkAdjustStartAltitude.getSelection();
				if (isAdjustAltitude) {

					// set relative scaling for altitude diff
					fChkAltiDiffScaling.setSelection(false);
				}

				onModifyProperties();
			}
		});

		/*
		 * altitude adjustment values
		 */
		final Composite aaContainer = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().indent(16, 0).applyTo(aaContainer);
		GridLayoutFactory.fillDefaults().numColumns(5).applyTo(aaContainer);
//		ttContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

		fLblAdjustAltiValueTime = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.hint(pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(fLblAdjustAltiValueTime);

		fLblAdjustAltiValueTimeUnit = new Label(aaContainer, SWT.NONE);
		fLblAdjustAltiValueTimeUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/min"); //$NON-NLS-1$

		fLblAdjustAltiValueDistance = new Label(aaContainer, SWT.TRAIL);
		GridDataFactory.fillDefaults()
				.hint(pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
				.applyTo(fLblAdjustAltiValueDistance);

		fLblAdjustAltiValueDistanceUnit = new Label(aaContainer, SWT.NONE);
		fLblAdjustAltiValueDistanceUnit.setText(UI.UNIT_LABEL_ALTITUDE + "/" + UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$

		/*
		 * checkbox: set tour type
		 */
		fChkSetTourType = new Button(group, SWT.CHECK);
		fChkSetTourType.setText(Messages.tour_merger_chk_set_tour_type);
		fChkSetTourType.setToolTipText(Messages.tour_merger_chk_set_tour_type_tooltip);
		fChkSetTourType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (fChkSetTourType.getSelection()) {
					setTourDirty();
				}

				enableActions();
			}
		});

		final Composite ttContainer = new Composite(group, SWT.NONE);
		GridDataFactory.fillDefaults().indent(16, 0).applyTo(ttContainer);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(ttContainer);

		/*
		 * tour type
		 */
		fTourTypeLink = new Link(ttContainer, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(fTourTypeLink);
		fTourTypeLink.setText(Messages.tour_editor_label_tour_type);
		fTourTypeLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				UI.openControlMenu(fTourTypeLink);
			}
		});

		/*
		 * tour type menu
		 */
		final MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourTypeMenu.fillMenu(menuMgr, DialogMergeTours.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		fTourTypeLink.setMenu(menuMgr.createContextMenu(fTourTypeLink));

		/*
		 * label: tour type icon and text
		 */
		fLblTourType = new CLabel(ttContainer, SWT.NONE);
		GridDataFactory.swtDefaults().grab(true, false).applyTo(fLblTourType);

	}

	private void createUITourChart(final Composite dlgContainer) {

		fTourChart = new TourChart(dlgContainer, SWT.BORDER, true);
		GridDataFactory.fillDefaults().grab(true, true).minSize(400, 200).applyTo(fTourChart);

		fTourChart.setShowZoomActions(true);
		fTourChart.setShowSlider(true);

		// set altitude visible
		fTourChartConfig = new TourChartConfiguration(true);
		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_ALTITUDE);
//		fTourChartConfig.addVisibleGraph(TourManager.GRAPH_TEMPERATURE);

		// overwrite x-axis from pref store
		fTourChartConfig.setIsShowTimeOnXAxis(TourbookPlugin.getDefault()
				.getPreferenceStore()
				.getString(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS)
				.equals(TourManager.X_AXIS_TIME));

		fTourChart.addDataModelListener(new IDataModelListener() {

			public void dataModelChanged(final ChartDataModel changedChartDataModel) {

				// set title
				changedChartDataModel.setTitle(TourManager.getTourTitleDetailed(fIntoTourData));
			}
		});

		fTourChart.addSliderMoveListener(new ISliderMoveListener() {
			public void sliderMoved(final SelectionChartInfo chartInfo) {

				if (fIsChartUpdated) {
					return;
				}

				fIsDirtyDisabled = true;
				onModifyProperties();
				fIsDirtyDisabled = false;
			}
		});
	}

	private void enableActions() {

		// enable/disable save button
		getButton(IDialogConstants.OK_ID).setEnabled(fIsTourDirty);

		if (fFromTourData.temperatureSerie == null) {
			fChkMergeTemperature.setSelection(false);
			fChkMergeTemperature.setEnabled(false);
		}

		if (fIntoTourData.distanceSerie == null) {
			fChkAdjustStartAltitude.setSelection(false);
			fChkAdjustStartAltitude.setEnabled(false);
		}

		final boolean isAdjustAltitude = fChkAdjustStartAltitude.getSelection();
		fLblAdjustAltiValueDistance.setEnabled(isAdjustAltitude);
		fLblAdjustAltiValueDistanceUnit.setEnabled(isAdjustAltitude);
		fLblAdjustAltiValueTime.setEnabled(isAdjustAltitude);
		fLblAdjustAltiValueTimeUnit.setEnabled(isAdjustAltitude);

		// this option can not be modified it's just for information
		fChkKeepHVAdjustments.setSelection(true);
		fChkKeepHVAdjustments.setEnabled(false);

		final boolean isSetTourType = fChkSetTourType.getSelection();

		fTourTypeLink.setEnabled(isSetTourType);
		fLblTourType.setEnabled(isSetTourType);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// keep window size and position
		return fDialogSettings;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
		selectedTours.add(fFromTourData);

		return selectedTours;
	}

	@Override
	protected void okPressed() {

		if (fIsTourDirty) {
			saveTour();
		}

		super.okPressed();
	}

	private void onDispose() {

		fShellImage.dispose();

		TourManager.getInstance().removeTourEventListener(fTourEventListener);
	}

	private void onModifyProperties() {

		// set dirty flag
		setTourDirty();

		final int altiDiff1 = fScaleAltitude1.getSelection() - MAX_ADJUST_ALTITUDE_1;
		final int altiDiff10 = (fScaleAltitude10.getSelection() - MAX_ADJUST_ALTITUDE_10) * 10;

		final int seconds = fScaleAdjustSeconds.getSelection() - MAX_ADJUST_SECONDS;
		final int minutes = fScaleAdjustMinutes.getSelection() - MAX_ADJUST_MINUTES;
		final int hours = fScaleAdjustHours.getSelection() - MAX_ADJUST_HOURS;

		final float localAltiDiff1 = altiDiff1 / UI.UNIT_VALUE_ALTITUDE;
		final float localAltiDiff10 = altiDiff10 / UI.UNIT_VALUE_ALTITUDE;

		fLabelAltitudeDiff1.setText(Integer.toString((int) localAltiDiff1) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);
		fLabelAltitudeDiff10.setText(Integer.toString((int) localAltiDiff10) + UI.SPACE + UI.UNIT_LABEL_ALTITUDE);

		fLabelAdjustSecondsValue.setText(Integer.toString(seconds));
		fLabelAdjustMinuteValue.setText(Integer.toString(minutes));
		fLabelAdjustHourValue.setText(Integer.toString(hours));

		fIntoTourData.setMergedTourTimeOffset(hours * 3600 + minutes * 60 + seconds);
		fIntoTourData.setMergedAltitudeOffset(altiDiff1 + altiDiff10);

		computeMergedData();

		fTourChartConfig.isRelativeAltiDiffScaling = fChkAltiDiffScaling.getSelection();

		if (fChkPreviewChart.getSelection()) {
			// update chart
			updateTourChart();
		} else {
			// update only the merge layer, this is much faster
			fTourChart.updateMergeLayer(true);
		}
	}

	private void onSelectResetAdjustments() {

		fScaleAdjustSeconds.setSelection(MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(MAX_ADJUST_HOURS);
		fScaleAltitude1.setSelection(MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(MAX_ADJUST_ALTITUDE_10);

		onModifyProperties();
	}

	private void onSelectResetValues() {

		/*
		 * get original data from the backuped data
		 */
		fFromTourData.timeSerie = createDataSerieBackup(fBackupFromTimeSerie);
		fFromTourData.distanceSerie = createDataSerieBackup(fBackupFromDistanceSerie);
		fFromTourData.altitudeSerie = createDataSerieBackup(fBackupFromAltitudeSerie);

		fIntoTourData.temperatureSerie = createDataSerieBackup(fBackupIntoTemperatureSerie);

		fIntoTourData.setMergedTourTimeOffset(fBackupIntoTimeOffset);
		fIntoTourData.setMergedAltitudeOffset(fBackupIntoAltitudeOffset);

		updateUIFromTourData();

		fIsTourDirty = false;

		enableActions();

	}

	/**
	 * Restore values which have been modified in the dialog
	 * 
	 * @param selectedTour
	 */
	private void restoreDataBackup() {

		fFromTourData.timeSerie = fBackupFromTimeSerie;
		fFromTourData.distanceSerie = fBackupFromDistanceSerie;
		fFromTourData.altitudeSerie = fBackupFromAltitudeSerie;
		fFromTourData.setTourType(fBackupFromTourType);

		fIntoTourData.temperatureSerie = fBackupIntoTemperatureSerie;

		fIntoTourData.setMergedTourTimeOffset(fBackupIntoTimeOffset);
		fIntoTourData.setMergedAltitudeOffset(fBackupIntoAltitudeOffset);
	}

	private void restoreState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		fChkPreviewChart.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART));
		fChkAltiDiffScaling.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING));

		fChkMergeTemperature.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_MERGE_TEMPERATURE));

// this is disabled because it can confuse the user
//		fChkAdjustStartAltitude.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_ADJUST_START_ALTITUDE));

		/*
		 * set tour type
		 */
		fChkSetTourType.setSelection(prefStore.getBoolean(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE));
		if (fFromTourData.getTourPerson() == null) {

			// tour is not saved, used tour type id from pref store

			final long tourTypeId = prefStore.getLong(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID);

			final ArrayList<TourType> tourTypes = TourDatabase.getAllTourTypes();

			for (final TourType tourType : tourTypes) {
				if (tourType.getTypeId() == tourTypeId) {

					fFromTourData.setTourType(tourType);

					fIsMergeFromTourModified = true;

					break;
				}
			}
		}

	}

	private void saveState() {

		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, fTourChartConfig.showTimeOnXAxis
				? TourManager.X_AXIS_TIME
				: TourManager.X_AXIS_DISTANCE);

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, fChkPreviewChart.getSelection());
		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, fChkAltiDiffScaling.getSelection());

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_MERGE_TEMPERATURE, fChkMergeTemperature.getSelection());

		prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE, fChkSetTourType.getSelection());

		// save tour type id
		final TourType fromTourType = fFromTourData.getTourType();
		if (fromTourType != null) {
			prefStore.setValue(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID, fromTourType.getTypeId());
		}
	}

	private void saveTour() {

		if (fChkMergeTemperature.getSelection()) {
			// temperature is already merged
		} else {
			// restore temperature values because temperature should not be saved
			fIntoTourData.temperatureSerie = fBackupIntoTemperatureSerie;
		}

		if (fChkAdjustStartAltitude.getSelection() && fFromTourData.mergeAdjustedAltitudeSerie != null) {

			/*
			 * put the adjusted altitude into the tour which is merged from the start until the
			 * first slider
			 */

			fIntoTourData.altitudeSerie = fFromTourData.mergeAdjustedAltitudeSerie;

			// adjust altitude up/down values
			fIntoTourData.computeAltitudeUpDown();
		}

		if (fChkSetTourType.getSelection() == false) {

			// restore backup values

			fFromTourData.setTourType(fBackupFromTourType);
		}

		// set tour id into which the tour is merged
		fFromTourData.setTourPerson(fIntoTourData.getTourPerson());
		fFromTourData.setMergeIntoTourId(fIntoTourData.getTourId());

		// save modified tours
		final ArrayList<TourData> modifiedTours = new ArrayList<TourData>();
		modifiedTours.add(fIntoTourData);
		modifiedTours.add(fFromTourData);

		TourManager.saveModifiedTours(modifiedTours);

		fIsTourSaved = true;
	}

	private void setTourDirty() {

		if (fIsDirtyDisabled) {
			return;
		}

		fIsTourDirty = true;

		enableActions();
	}

	private void updateTourChart() {

		fIsChartUpdated = true;

		fTourChart.updateTourChart(fIntoTourData, fTourChartConfig, true);

		fIsChartUpdated = false;
	}

	private void updateUI(final float altiDiffTime, final float altiDiffDist) {

		if (fFromTourData.mergeAdjustedAltitudeSerie == null) {

			// adjusted alti is disabled

			fLblAdjustAltiValueTime.setText("N/A"); //$NON-NLS-1$
			fLblAdjustAltiValueDistance.setText("N/A"); //$NON-NLS-1$

		} else {

			fLblAdjustAltiValueTime.setText(fNumberFormatter.format(altiDiffTime));
			fLblAdjustAltiValueDistance.setText(fNumberFormatter.format(altiDiffDist));
		}
	}

	/**
	 * set data from the tour into the UI
	 */
	private void updateUIFromTourData() {

		fIsDirtyDisabled = true;

		/*
		 * show time offset
		 */
		final int mergedTourTimeOffset = fIntoTourData.getMergedTourTimeOffset();
		final int mergedMetricAltitudeOffset = fIntoTourData.getMergedAltitudeOffset();

		final int seconds = mergedTourTimeOffset % 60;
		final int minutes = mergedTourTimeOffset / 60 % 60;
		final int hours = mergedTourTimeOffset / 3600;

		fScaleAdjustSeconds.setSelection(seconds + MAX_ADJUST_SECONDS);
		fScaleAdjustMinutes.setSelection(minutes + MAX_ADJUST_MINUTES);
		fScaleAdjustHours.setSelection(hours + MAX_ADJUST_HOURS);

		/*
		 * show altitude offset
		 */
		final float altitudeOffset = mergedMetricAltitudeOffset;
		final int altitudeOffset1 = (int) (altitudeOffset % 10);
		final int altitudeOffset10 = (int) (altitudeOffset / 10);

		fScaleAltitude1.setSelection(altitudeOffset1 + MAX_ADJUST_ALTITUDE_1);
		fScaleAltitude10.setSelection(altitudeOffset10 + MAX_ADJUST_ALTITUDE_10);

		UI.updateUITourType(fFromTourData.getTourType(), fLblTourType);

		onModifyProperties();

		fIsDirtyDisabled = false;
	}

}
