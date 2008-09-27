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
package net.tourbook.ui.views.tourDataEditor;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.ActionRemoveAllTags;
import net.tourbook.tag.ActionSetTourTag;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.ITourEditor;
import net.tourbook.tour.ITourPropertyListener;
import net.tourbook.tour.ITourSaveListener;
import net.tourbook.tour.SelectionActiveEditor;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourChart;
import net.tourbook.tour.TourEditor;
import net.tourbook.tour.TourEditorInput;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourProperties;
import net.tourbook.ui.ActionModifyColumns;
import net.tourbook.ui.ActionOpenPrefDialog;
import net.tourbook.ui.ActionSetTourType;
import net.tourbook.ui.ColumnManager;
import net.tourbook.ui.ISelectedTours;
import net.tourbook.ui.ITourViewer;
import net.tourbook.ui.TableColumnDefinition;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.tourCatalog.TVICatalogComparedTour;
import net.tourbook.util.PixelConverter;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Preferences.IPropertyChangeListener;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

// author: Wolfgang Schramm
// create: 24.08.2007

public class TourDataEditorView extends ViewPart implements ITourViewer, ISelectedTours, ITourEditor {

	private static final String		PART_NAME_IS_MODIFIED	= Messages.tour_editor_part_name_is_modified;
	private static final String		PART_NAME_TOUR_EDITOR	= Messages.tour_editor_part_name_tour_editor;

	private static final int		BUSY_INDICATOR_ITEMS	= 5000;

	private static final int		TAB_INDEX_TITLE			= 0;
	private static final int		TAB_INDEX_TIME_SLICES	= 1;

	public static final String		ID						= "net.tourbook.views.TourPropertiesView";		//$NON-NLS-1$

	private static final String		MEMENTO_SELECTED_TAB	= "tourProperties.selectedTab";				//$NON-NLS-1$
	private static final String		MEMENTO_ROW_EDIT_MODE	= "tourProperties.editMode";					//$NON-NLS-1$

	private static IMemento			fSessionMemento;

	private PageBook				fPageBook;
	private Label					fPageNoTour;
	private Form					fEditorForm;
	private CTabFolder				fTabFolder;

	private DateTime				fTourDate;
	private DateTime				fStartTime;

	private Label					fLblDatapoints;
	private Label					fLblDeviceName;
	private Label					fLblDrivingTime;
	private Label					fLblRecordingTime;
	private Label					fLblTourTags;
	private CLabel					fLblTourType;

	private Text					fTextTitle;
	private Text					fTextDescription;

	private Text					fTextStartLocation;
	private Text					fTextEndLocation;

	private Text					fTextTourDistance;
	private Label					fLblTourDistanceUnit;

	private Hyperlink				fTagLink;
	private Hyperlink				fTourTypeLink;

	private PostSelectionProvider	fPostSelectionProvider;
	private ISelectionListener		fPostSelectionListener;
	private IPartListener2			fPartListener;
	private IPropertyChangeListener	fPrefChangeListener;
	private ITourPropertyListener	fTourPropertyListener;
	private ITourSaveListener		fTourSaveListener;

	private Calendar				fCalendar				= GregorianCalendar.getInstance();

//	private DateFormat				fTimeFormatter			= DateFormat.getTimeInstance(DateFormat.SHORT);
	private DateFormat				fDurationFormatter		= DateFormat.getTimeInstance(DateFormat.SHORT,
																	Locale.GERMAN);
	private NumberFormat			fNumberFormatter		= NumberFormat.getNumberInstance();

	/**
	 * contains the tour editor when the tour is opened by an editor or <code>null</code> when the
	 * source of the tour is not from an editor
	 */
	private TourEditor				fTourEditor;
	private TourChart				fTourChart;
	private TourData				fTourData;

	private ScrolledComposite		fScrolledDataContainer;
	private Composite				fDataContainer;

	private Composite				fDataViewerContainer;
	private TableViewer				fDataViewer;

	private ColumnManager			fColumnManager;
	/*
	 * data series which are displayed in the viewer
	 */
	private int[]					fSerieTime;

	private int[]					fSerieDistance;

	private int[]					fSerieAltitude;
	private int[]					fSerieTemperature;
	private int[]					fSerieCadence;
	private int[]					fSerieGradient;
	private int[]					fSerieSpeed;
	private int[]					fSeriePace;
	private int[]					fSeriePower;
	private int[]					fSeriePulse;
	private double[]				fSerieLatitude;
	private double[]				fSerieLongitude;

	private TableColumnDefinition	fColDefAltitude;
	private TableColumnDefinition	fColDefCadence;
	private TableColumnDefinition	fColDefPulse;
	private TableColumnDefinition	fColDefTemperature;
	private TableColumnDefinition	fColDefLongitude;
	private TableColumnDefinition	fColDefLatitude;

	private ActionModifyColumns		fActionModifyColumns;
	private ActionSaveTour			fActionSaveTour;
	private ActionUndoChanges		fActionUndoChanges;
	private ActionEditRows			fActionEditRows;

	private ActionSetTourTag		fActionAddTag;
	private ActionSetTourTag		fActionRemoveTag;
	private ActionRemoveAllTags		fActionRemoveAllTags;

	private ActionOpenPrefDialog	fActionOpenTagPrefs;
	private ActionOpenPrefDialog	fActionOpenTourTypePrefs;

	/**
	 * <code>true</code>: rows can be selected in the viewer<br>
	 * <code>false</code>: cell can be selected in the viewer
	 */
	private boolean					fIsRowEditMode			= true;

	private long					fPostReloadViewerTourId;

	private boolean					fIsTourDirty			= false;

	/**
	 * is <code>true</code> when the tour is currently being saved
	 */
	private boolean					fIsSavingInProgress		= false;
	private boolean					fDisableModifyEvent		= false;

	private Long					fSelectionTourId;

//	private final class DoubleEditingSupport extends DoubleDataSerieEditingSupport {
//
//		private final TextCellEditor	fCellEditor;
//
//		private DoubleEditingSupport(final TextCellEditor cellEditor, final double[] dataSerie) {
//			super(fDataViewer);
//			fCellEditor = cellEditor;
//			fDataSerie = dataSerie;
//		}
//
//		@Override
//		protected boolean canEdit(final Object element) {
//			if (fDataSerie == null || isTourInDb() == false) {
//				return false;
//			}
//			return true;
//		}
//
//		@Override
//		protected CellEditor getCellEditor(final Object element) {
//			return fCellEditor;
//		}
//
//		@Override
//		protected Object getValue(final Object element) {
//			final TimeSlice timeSlice = (TimeSlice) element;
//			return new Double(fDataSerie[timeSlice.serieIndex]).toString();
//		}
//
//		@Override
//		protected void setValue(final Object element, final Object value) {
//
//			if (value instanceof String) {
//
//				try {
//					final TimeSlice timeSlice = (TimeSlice) element;
//					final double newValue = Double.parseDouble((String) value);
//
//					if (newValue != fDataSerie[timeSlice.serieIndex]) {
//
//						// value has changed
//
//						fIsTourDirty = true;
//
//						fDataSerie[timeSlice.serieIndex] = newValue;
//
//						updateTourData();
//					}
//
//				} catch (final Exception e) {
//					// ignore invalid characters
//				}
//			}
//		}
//	}

	private final class IntegerEditingSupport extends IntegerDataSerieEditingSupport {

		private final TextCellEditor	fCellEditor;

		private IntegerEditingSupport(final TextCellEditor cellEditor, final int[] dataSerie) {
			super(fDataViewer);
			fCellEditor = cellEditor;
			fDataSerie = dataSerie;
		}

		@Override
		protected boolean canEdit(final Object element) {
			if (fDataSerie == null || isTourInDb() == false) {
				return false;
			}
			return true;
		}

		@Override
		protected CellEditor getCellEditor(final Object element) {
			return fCellEditor;
		}

		@Override
		protected Object getValue(final Object element) {
			final TimeSlice timeSlice = (TimeSlice) element;
			return new Integer(fDataSerie[timeSlice.serieIndex]).toString();
		}

		@Override
		protected void setValue(final Object element, final Object value) {

			if (value instanceof String) {

				try {
					final TimeSlice timeSlice = (TimeSlice) element;
					final int newValue = Integer.parseInt((String) value);

					if (newValue != fDataSerie[timeSlice.serieIndex]) {

						// value has changed

						fIsTourDirty = true;

						fDataSerie[timeSlice.serieIndex] = newValue;

						updateTimeSlices();
					}

				} catch (final Exception e) {
					// ignore invalid characters
				}
			}
		}
	}

	private class TimeSlice {
		int	serieIndex;

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof TimeSlice)) {
				return false;
			}
			final TimeSlice other = (TimeSlice) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (serieIndex != other.serieIndex) {
				return false;
			}
			return true;
		}

		private TourDataEditorView getOuterType() {
			return TourDataEditorView.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + serieIndex;
			return result;
		}

	}

	private class TourDataContentProvider implements IStructuredContentProvider {

		public TourDataContentProvider() {}

		public void dispose() {}

		public Object[] getElements(final Object parent) {

			if (fTourData == null) {
				return new Object[0];
			}

			fSerieTime = fTourData.timeSerie;

			fSerieDistance = fTourData.getDistanceSerie();
			fSerieAltitude = fTourData.getAltitudeSerie();
			fSerieTemperature = fTourData.getTemperatureSerie();
			fSerieGradient = fTourData.getGradientSerie();
			fSerieSpeed = fTourData.getSpeedSerie();
			fSeriePace = fTourData.getPaceSerie();
			fSeriePower = fTourData.getPowerSerie();

			fSerieCadence = fTourData.cadenceSerie;
			fSeriePulse = fTourData.pulseSerie;

			fSerieLatitude = fTourData.latitudeSerie;
			fSerieLongitude = fTourData.longitudeSerie;

			fColDefAltitude.setEditorDataSerie(fSerieAltitude);
			fColDefTemperature.setEditorDataSerie(fSerieTemperature);
			fColDefPulse.setEditorDataSerie(fSeriePulse);
			fColDefCadence.setEditorDataSerie(fSerieCadence);

			/*
			 * create viewer elements (time slices)
			 */
			final TimeSlice[] dataElements = new TimeSlice[fTourData.timeSerie.length];
			TimeSlice timeSlice;
			for (int serieIndex = 0; serieIndex < dataElements.length; serieIndex++) {

				dataElements[serieIndex] = timeSlice = new TimeSlice();

				timeSlice.serieIndex = serieIndex;
			}

			return (dataElements);
		}

		public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {}
	}

	void actionEditRow() {

		fIsRowEditMode = fActionEditRows.isChecked();

		recreateViewer();
	}

	void actionSaveTour() {
		saveTourInternal();
	}

	void actionUndoChanges() {

		fIsTourDirty = false;

		fDisableModifyEvent = true;
		{
			fTourData = reloadTourData();
			updateUI(fTourData, true);
		}
		fDisableModifyEvent = false;

		fireRevertNotification();
	}

	private void addPartListener() {

		// set the part listener
		fPartListener = new IPartListener2() {
			public void partActivated(final IWorkbenchPartReference partRef) {

				/*
				 * fire a selection when this editor shows another tour as the currently selected
				 * tour in another part. This happens when the tour is modified, another tour is
				 * selected and saving this tour is canceled
				 */

				if (fTourData == null || fSelectionTourId == null) {
					return;
				}

				if (fTourData.getTourId().longValue() != fSelectionTourId.longValue()) {

					fPostSelectionProvider.setSelection(new SelectionTourData(null, fTourData, true));
				}
			}

			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			public void partClosed(final IWorkbenchPartReference partRef) {
				if (ID.equals(partRef.getId())) {

					// keep settings for this part
					saveSettings();

					saveTourConfirmed();
				}
			}

			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			public void partHidden(final IWorkbenchPartReference partRef) {}

			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			public void partOpened(final IWorkbenchPartReference partRef) {}

			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		// register the listener in the page
		getSite().getPage().addPartListener(fPartListener);
	}

	private void addPrefListener() {

		fPrefChangeListener = new Preferences.IPropertyChangeListener() {
			public void propertyChange(final Preferences.PropertyChangeEvent event) {

				if (fTourData == null) {
					return;
				}

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					fColumnManager.saveState(fSessionMemento);
					fColumnManager.clearColumns();
					defineViewerColumns(fDataViewerContainer);

					recreateViewer();
					updateUITabData();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					// reload tour data

					fTourData = TourManager.getInstance().getTourData(fTourData.getTourId());
					updateUI(fTourData, false);
				}
			}
		};
		TourbookPlugin.getDefault().getPluginPreferences().addPropertyChangeListener(fPrefChangeListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		fPostSelectionListener = new ISelectionListener() {
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == TourDataEditorView.this) {
					return;
				}

				onSelectionChanged(selection);
			}
		};
		getSite().getPage().addPostSelectionListener(fPostSelectionListener);
	}

	private void addTourPropertyListener() {

		fTourPropertyListener = new ITourPropertyListener() {
			public void propertyChanged(final IWorkbenchPart part, final int propertyId, final Object propertyData) {

				if (fTourData == null || part == TourDataEditorView.this) {
					return;
				}

				if (propertyId == TourManager.TOUR_PROPERTIES_CHANGED && propertyData instanceof TourProperties) {

					final TourProperties tourProperties = (TourProperties) propertyData;

					// get modified tours
					final ArrayList<TourData> modifiedTours = tourProperties.modifiedTours;
					final long viewTourId = fTourData.getTourId();

					// update modified tour
					for (final TourData tourData : modifiedTours) {
						if (tourData.getTourId() == viewTourId) {

							if (tourProperties.isReverted) {
								fIsTourDirty = false;
							}

							updateUI(tourData, true);
							return;
						}
					}

				} else if (propertyId == TourManager.TAG_STRUCTURE_CHANGED) {

					fTourData = TourManager.getInstance().getTourData(fTourData.getTourId());
					updateUI(fTourData, false);
				}
			}
		};

		TourManager.getInstance().addPropertyListener(fTourPropertyListener);
	}

	private void addTourSaveListener() {

		fTourSaveListener = new ITourSaveListener() {
			public boolean saveTour() {
				return saveTourConfirmed();
			}

		};

		TourManager.getInstance().addTourSaveListener(fTourSaveListener);
	}

	private void createActions() {

		fActionModifyColumns = new ActionModifyColumns(this);

		fActionSaveTour = new ActionSaveTour(this);
		fActionUndoChanges = new ActionUndoChanges(this);
		fActionEditRows = new ActionEditRows(this);

		fActionAddTag = new ActionSetTourTag(this, true, false);
		fActionRemoveTag = new ActionSetTourTag(this, false, false);
		fActionRemoveAllTags = new ActionRemoveAllTags(this, false);

		fActionOpenTagPrefs = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure,
				ITourbookPreferences.PREF_PAGE_TAGS);

		fActionOpenTourTypePrefs = new ActionOpenPrefDialog(Messages.action_tourType_modify_tourTypes,
				ITourbookPreferences.PREF_PAGE_TOUR_TYPE);

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(fActionSaveTour);
		tbm.add(fActionUndoChanges);

		tbm.add(new Separator());
		tbm.add(fActionEditRows);

		tbm.update(true);

		/*
		 * fill view menu
		 */
		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();
		menuMgr.add(fActionModifyColumns);
	}

	/**
	 * @param parent
	 */
	private void createDataViewer(final Composite parent) {

		// table
		final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		table.addTraverseListener(new TraverseListener() {
			public void keyTraversed(final TraverseEvent e) {
				e.doit = e.keyCode != SWT.CR; // vetoes all CR traversals
			}
		});

		fDataViewer = new TableViewer(table);

		if (fIsRowEditMode == false) {

			/*
			 * initialize cell editing
			 */
			final TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(fDataViewer,
					new FocusCellOwnerDrawHighlighter(fDataViewer));

			final ColumnViewerEditorActivationStrategy actSupport = new ColumnViewerEditorActivationStrategy(fDataViewer) {
				@Override
				protected boolean isEditorActivationEvent(final ColumnViewerEditorActivationEvent event) {
					return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
							|| event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
							|| (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == SWT.CR)
							|| event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC;
				}
			};

			TableViewerEditor.create(fDataViewer, //
					focusCellManager,
					actSupport,
					ColumnViewerEditor.TABBING_HORIZONTAL
							| ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR
							| ColumnViewerEditor.TABBING_VERTICAL
							| ColumnViewerEditor.KEYBOARD_ACTIVATION);
		}

		/*
		 * create editing support after the viewer is created but before the columns are created
		 */
		final TextCellEditor cellEditor = new TextCellEditor(fDataViewer.getTable());

		fColDefAltitude.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieAltitude));
		fColDefPulse.setEditingSupport(new IntegerEditingSupport(cellEditor, fSeriePulse));
		fColDefTemperature.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieTemperature));
		fColDefCadence.setEditingSupport(new IntegerEditingSupport(cellEditor, fSerieCadence));
//		fColDefLatitude.setEditingSupport(new DoubleEditingSupport(cellEditor, fSerieLatitude));
//		fColDefLongitude.setEditingSupport(new DoubleEditingSupport(cellEditor, fSerieLongitude));

		fColumnManager.createColumns();

		fDataViewer.setContentProvider(new TourDataContentProvider());
		fDataViewer.setUseHashlookup(true);

		fDataViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(final SelectionChangedEvent event) {
				final StructuredSelection selection = (StructuredSelection) event.getSelection();
				if (selection != null) {
					fireSliderPosition(selection);
				}
			}
		});

		// hide first column, this is a hack to align the "first" visible column to right
		table.getColumn(0).setWidth(0);
	}

	/**
	 * create the drop down menus, this must be created after the parent control is created
	 */
	private void createMenus() {

		/*
		 * tag menu
		 */
		MenuManager menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				final boolean isTagSet = fTourData.getTourTags().size() > 0;

				// enable actions
				fActionRemoveTag.setEnabled(isTagSet);
				fActionRemoveAllTags.setEnabled(isTagSet);

				// set menu items
				menuMgr.add(fActionAddTag);
				menuMgr.add(fActionRemoveTag);
				menuMgr.add(fActionRemoveAllTags);

				TagManager.fillRecentTagsIntoMenu(menuMgr, TourDataEditorView.this, true, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTagPrefs);
			}
		});

		// set menu for the tag item
		fTagLink.setMenu(menuMgr.createContextMenu(fTagLink));

		/*
		 * tour type menu
		 */
		menuMgr = new MenuManager();

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager menuMgr) {

				// set menu items

				ActionSetTourType.fillMenu(menuMgr, TourDataEditorView.this, false);

				menuMgr.add(new Separator());
				menuMgr.add(fActionOpenTourTypePrefs);
			}
		});

		// set menu for the tag item
		fTourTypeLink.setMenu(menuMgr.createContextMenu(fTourTypeLink));
	}

	@Override
	public void createPartControl(final Composite parent) {

		// define all columns
		fColumnManager = new ColumnManager(this, fSessionMemento);
		defineViewerColumns(parent);

		restoreStateBeforeUI(fSessionMemento);

		createUI(parent);
		createMenus();

		addSelectionListener();
		addPartListener();
		addPrefListener();
		addTourPropertyListener();
		addTourSaveListener();

		createActions();

		// this part is a selection provider
		getSite().setSelectionProvider(fPostSelectionProvider = new PostSelectionProvider());

		restoreStateWithUI(fSessionMemento);

		fPageBook.showPage(fPageNoTour);

		// show data from last selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		enableControls();
	}

	private void createUI(final Composite parent) {

		fPageBook = new PageBook(parent, SWT.NONE);
		fPageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		fPageNoTour = new Label(fPageBook, SWT.NONE);
		fPageNoTour.setText(Messages.UI_Label_no_chart_is_selected);

		final FormToolkit toolkit = new FormToolkit(parent.getDisplay());

		fEditorForm = toolkit.createForm(fPageBook);
		toolkit.decorateFormHeading(fEditorForm);

		final Composite formBody = fEditorForm.getBody();
		GridLayoutFactory.fillDefaults().applyTo(formBody);

		fTabFolder = new CTabFolder(formBody, SWT.FLAT | SWT.BOTTOM);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fTabFolder);
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				if (fPostReloadViewerTourId == -1L) {
					// load viewer when this is not done
					fPostReloadViewerTourId = fTourData.getTourId();
					reloadViewer();
				}

				if (fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES) {
					fDataViewer.getTable().setFocus();
				}

				enableControls();
			}
		});

		final CTabItem fTabItemLocation = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemLocation.setText(Messages.tour_editor_tabLabel_info);
		fTabItemLocation.setControl(createUITabData(fTabFolder, toolkit));

		final CTabItem fTabItemTimeSlices = new CTabItem(fTabFolder, SWT.FLAT);
		fTabItemTimeSlices.setText(Messages.tour_editor_tabLabel_tour_data);
		fTabItemTimeSlices.setControl(createUITabTimeSlices(fTabFolder));

	}

	private void createUISeparator(final FormToolkit tk) {

		final Label separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(separator);
//		separator.setText("x");
//		separator.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
	}

//	private void createUITourType(Composite parent, FormToolkit tk) {
//
//		Composite container = tk.createComposite(parent);
//		GridDataFactory.fillDefaults().applyTo(container);
//		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		
//		fImageTourType = new Image(Display.getCurrent(),0,0);
//		
//		UI.getInstance().getTourTypeImage(-1);
//		
//		fLblTourType = tk.createLabel(container, UI.EMPTY_STRING);
//
//	}

	private Composite createUITabData(final Composite parent, final FormToolkit tk) {

		final PixelConverter pixelConverter = new PixelConverter(parent);
		Label label;

		final ModifyListener modifyListener = new ModifyListener() {
			public void modifyText(final ModifyEvent e) {

				if (fDisableModifyEvent || fIsSavingInProgress) {
					return;
				}

				setTourDirty();
			}
		};

		final KeyAdapter keyListener = new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent e) {
				updateContentOnKeyUp();
			}
		};

		final VerifyListener verifyIntValue = new VerifyListener() {
			public void verifyText(final VerifyEvent e) {
				try {
					Integer.parseInt(e.text);
				} catch (final NumberFormatException nfe) {
					// ignore wrong characters
					e.doit = false;
				}
			}
		};

		final VerifyListener verifyFloatValue = new VerifyListener() {
			public void verifyText(final VerifyEvent e) {
				try {
					Float.parseFloat(e.text);
				} catch (final NumberFormatException nfe) {
					
					// ignore wrong characters
					
					fEditorForm.setMessage("invalid value " + e.text, IMessageProvider.ERROR);
					e.doit = false;
				}
			}
		};

		/*
		 * scrolled container
		 */
		fScrolledDataContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		fScrolledDataContainer.setExpandVertical(true);
		fScrolledDataContainer.setExpandHorizontal(true);

		fDataContainer = new Composite(fScrolledDataContainer, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(fDataContainer);
		GridLayoutFactory.swtDefaults()//
				.numColumns(6)
//				.spacing(5, 5)
				.applyTo(fDataContainer);
		tk.adapt(fDataContainer);

		fScrolledDataContainer.setContent(fDataContainer);
		fScrolledDataContainer.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				onResizeContainer();
			}
		});

		/*
		 * title
		 */
		tk.createLabel(fDataContainer, Messages.tour_editor_label_tour_title);

		fTextTitle = tk.createText(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).span(5, 1).applyTo(fTextTitle);
		fTextTitle.addKeyListener(keyListener);
		fTextTitle.addModifyListener(modifyListener);

		/*
		 * description
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_description);
		GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

		fTextDescription = tk.createText(fDataContainer, UI.EMPTY_STRING, SWT.BORDER
				| SWT.WRAP
				| SWT.MULTI
				| SWT.V_SCROLL
				| SWT.H_SCROLL);

		GridDataFactory.fillDefaults()//
				.grab(true, true)
				.span(5, 1)
//				.hint(400, pixelConverter.convertHeightInCharsToPixels(3))
				.hint(SWT.DEFAULT, pixelConverter.convertHeightInCharsToPixels(3))
				.applyTo(fTextDescription);

		fTextDescription.addModifyListener(modifyListener);

		/*
		 * tags
		 */
		fTagLink = tk.createHyperlink(fDataContainer, Messages.tour_editor_label_tour_tag, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).applyTo(fTagLink);
		fTagLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				openControlMenu(fTagLink);
			}
		});

		fLblTourTags = tk.createLabel(fDataContainer, UI.EMPTY_STRING, SWT.WRAP);
		GridDataFactory.fillDefaults()
				.grab(true, false)
				.span(5, 1)
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(fLblTourTags);
		tk.adapt(fLblTourTags, false, false);

		/*
		 * tour type
		 */
		fTourTypeLink = tk.createHyperlink(fDataContainer, Messages.tour_editor_label_tour_type, SWT.NONE);
		fTourTypeLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(final HyperlinkEvent e) {
				openControlMenu(fTourTypeLink);
			}
		});
		fLblTourType = new CLabel(fDataContainer, SWT.NONE);
		GridDataFactory.swtDefaults().span(5, 1).applyTo(fLblTourType);
		tk.adapt(fLblTourType);

		/*
		 * date
		 */
		tk.createLabel(fDataContainer, Messages.tour_editor_label_tour_date);

		fTourDate = new DateTime(fDataContainer, SWT.DATE | SWT.MEDIUM);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(false, false).applyTo(fTourDate);
		tk.adapt(fTourDate, true, true);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * recording time
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_recording_time);

		fLblRecordingTime = tk.createLabel(fDataContainer, UI.EMPTY_STRING);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * start time
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_start_time);

		fStartTime = new DateTime(fDataContainer, SWT.TIME | SWT.SHORT /* | SWT.BORDER */);
		GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(false, false).applyTo(fStartTime);
		tk.adapt(fStartTime, true, true);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * driving time
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_driving_time);

		fLblDrivingTime = tk.createLabel(fDataContainer, UI.EMPTY_STRING);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * data points
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_datapoints);

		fLblDatapoints = tk.createLabel(fDataContainer, UI.EMPTY_STRING, SWT.TRAIL);
		GridDataFactory.fillDefaults().applyTo(fLblDatapoints);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * device name
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_device_name);

		fLblDeviceName = tk.createLabel(fDataContainer, UI.EMPTY_STRING);

		///////////////////
		createUISeparator(tk);
		///////////////////

		/*
		 * tour distance
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_tour_distance);

		fTextTourDistance = tk.createText(fDataContainer, UI.EMPTY_STRING, SWT.TRAIL);
		GridDataFactory.fillDefaults().applyTo(fTextTourDistance);
		fTextTourDistance.addModifyListener(modifyListener);
		fTextTourDistance.addVerifyListener(verifyFloatValue);

		fLblTourDistanceUnit = tk.createLabel(fDataContainer, UI.UNIT_LABEL_DISTANCE);

		///////////////////
		createUISeparator(tk);
		createUISeparator(tk);
		createUISeparator(tk);
		///////////////////

		/*
		 * start location
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_start_location);

		fTextStartLocation = tk.createText(fDataContainer, UI.EMPTY_STRING);
		fTextStartLocation.addModifyListener(modifyListener);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(fTextStartLocation);

		/*
		 * end location
		 */
		label = tk.createLabel(fDataContainer, Messages.tour_editor_label_end_location);

		fTextEndLocation = tk.createText(fDataContainer, UI.EMPTY_STRING);
		fTextEndLocation.addModifyListener(modifyListener);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(fTextEndLocation);

		/*
		 * force column width
		 */
		Label separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(separator);

		separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(separator);

		separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(1000, SWT.DEFAULT).applyTo(separator);

		separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(separator);

		separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(1000, SWT.DEFAULT).applyTo(separator);

		separator = tk.createLabel(fDataContainer, UI.EMPTY_STRING);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(separator);

		return fScrolledDataContainer;
	}

	private Control createUITabTimeSlices(final Composite parent) {

		fDataViewerContainer = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(fDataViewerContainer);

		createDataViewer(fDataViewerContainer);

		return fDataViewerContainer;
	}

	private void defineViewerColumns(final Composite parent) {

		final PixelConverter pixelConverter = new PixelConverter(parent);

		TableColumnDefinition colDef;

		/*
		 * 1. column will be hidden because the alignment for the first column is always to the left
		 */
		colDef = TableColumnFactory.FIRST_COLUMN.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setHideColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {}
		});

		/*
		 * column: #
		 */
		colDef = TableColumnFactory.SEQUENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setCanModifyVisibility(false);
		colDef.setIsColumnMoveable(false);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				cell.setText(Integer.toString(((TimeSlice) cell.getElement()).serieIndex));

				cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
			}
		});

		/*
		 * column: time
		 */
		colDef = TableColumnFactory.TOUR_TIME.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (fSerieTime != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieTime[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: distance
		 */
		colDef = TableColumnFactory.DISTANCE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {

				if (fSerieDistance != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					final int distance = fSerieDistance[timeSlice.serieIndex];
					fNumberFormatter.setMinimumFractionDigits(3);
					fNumberFormatter.setMaximumFractionDigits(3);
					cell.setText(fNumberFormatter.format((float) distance / 1000));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: altitude
		 */
		fColDefAltitude = colDef = TableColumnFactory.ALTITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieAltitude != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieAltitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pulse
		 */
		fColDefPulse = colDef = TableColumnFactory.PULSE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSeriePulse != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSeriePulse[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: temperature
		 */
		fColDefTemperature = colDef = TableColumnFactory.TEMPERATURE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieTemperature != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieTemperature[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: cadence
		 */
		fColDefCadence = colDef = TableColumnFactory.CADENCE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieCadence != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSerieCadence[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: gradient
		 */
		colDef = TableColumnFactory.GRADIENT.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSerieGradient[timeSlice.serieIndex] / 10));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: speed
		 */
		colDef = TableColumnFactory.SPEED.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSerieSpeed[timeSlice.serieIndex] / 10));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: pace
		 */
		colDef = TableColumnFactory.PACE.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					fNumberFormatter.setMinimumFractionDigits(1);
					fNumberFormatter.setMaximumFractionDigits(1);

					cell.setText(fNumberFormatter.format((float) fSeriePace[timeSlice.serieIndex] / 10));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: power
		 */
		colDef = TableColumnFactory.POWER.createColumn(fColumnManager, pixelConverter);
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieGradient != null) {
					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Integer.toString(fSeriePower[timeSlice.serieIndex]));

				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: longitude
		 */
		fColDefLongitude = colDef = TableColumnFactory.LONGITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieLongitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(fSerieLongitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

		/*
		 * column: latitude
		 */
		fColDefLatitude = colDef = TableColumnFactory.LATITUDE.createColumn(fColumnManager, pixelConverter);
		colDef.setIsDefaultColumn();
		colDef.setLabelProvider(new CellLabelProvider() {
			@Override
			public void update(final ViewerCell cell) {
				if (fSerieLatitude != null) {

					final TimeSlice timeSlice = (TimeSlice) cell.getElement();
					cell.setText(Double.toString(fSerieLatitude[timeSlice.serieIndex]));
				} else {
					cell.setText(UI.EMPTY_STRING);
				}
			}
		});

	}

	@Override
	public void dispose() {

		final IWorkbenchPage page = getSite().getPage();

		page.removePostSelectionListener(fPostSelectionListener);
		page.removePartListener(fPartListener);

		TourbookPlugin.getDefault().getPluginPreferences().removePropertyChangeListener(fPrefChangeListener);

		TourManager.getInstance().removePropertyListener(fTourPropertyListener);
		TourManager.getInstance().removeTourSaveListener(fTourSaveListener);

		super.dispose();
	}

	private void enableActions() {
		fActionSaveTour.setEnabled(fIsTourDirty);
		fActionUndoChanges.setEnabled(fIsTourDirty);
	}

	private void enableControls() {

		final boolean canEditTour = isTourInDb();

		fTextTitle.setEnabled(canEditTour);
		fTextDescription.setEnabled(canEditTour);
		fTextStartLocation.setEnabled(canEditTour);
		fTextEndLocation.setEnabled(canEditTour);

		final boolean isTimeSliceTab = fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES;
		fActionModifyColumns.setEnabled(isTimeSliceTab);
		fActionEditRows.setEnabled(isTimeSliceTab);

		enableActions();

		// update partname
		String partName = fIsTourDirty ? PART_NAME_IS_MODIFIED : UI.EMPTY_STRING;
		partName += PART_NAME_TOUR_EDITOR;
		setPartName(partName);
	}

	/**
	 * fire notification for changed tour data
	 */
	private void fireModifyNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		final TourProperties propertyData = new TourProperties(modifiedTour);
		propertyData.isTourEdited = true;

		TourManager.firePropertyChange(TourDataEditorView.this, TourManager.TOUR_PROPERTIES_CHANGED, propertyData);
	}

	/**
	 * fire notification for the reverted tour data
	 */
	private void fireRevertNotification() {

		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		final TourProperties tourProperties = new TourProperties(modifiedTour);
		tourProperties.isReverted = true;

		TourManager.firePropertyChange(TourDataEditorView.this, TourManager.TOUR_PROPERTIES_CHANGED, tourProperties);
	}

	/**
	 * select the chart slider(s) according to the selected marker(s)
	 */
	private void fireSliderPosition(final StructuredSelection selection) {

		if (fTourChart == null) {

			final TourChart tourChart = TourManager.getInstance().getActiveTourChart();

			if (tourChart == null || tourChart.isDisposed()) {
				return;
			} else {
				fTourChart = tourChart;
			}
		}

		final Object[] selectedData = selection.toArray();

		if (selectedData.length > 1) {

			// two or more data are selected, set the 2 sliders to the first and last selected data

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TimeSlice) selectedData[0]).serieIndex,
					((TimeSlice) selectedData[selectedData.length - 1]).serieIndex));

		} else if (selectedData.length > 0) {

			// one data is selected

			fPostSelectionProvider.setSelection(new SelectionChartXSliderPosition(fTourChart,
					((TimeSlice) selectedData[0]).serieIndex,
					SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {

		if (adapter == ColumnViewer.class) {
			return fDataViewer;
		}

		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	public ColumnManager getColumnManager() {
		return fColumnManager;
	}

	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> tourDataList = new ArrayList<TourData>();
		tourDataList.add(fTourData);

		return tourDataList;
	}

	/**
	 * get data for this tour data editor from a {@link TourEditor}
	 * 
	 * @param editor
	 */
	private void getTourEditorData(final TourEditor editor) {

		fTourEditor = editor;
		fTourChart = fTourEditor.getTourChart();

		// update dirty state from the editor
		fIsTourDirty = fTourEditor.isDirty();
	}

	public ColumnViewer getViewer() {
		return fDataViewer;
	}

	@Override
	public void init(final IViewSite site, final IMemento memento) throws PartInitException {

		super.init(site, memento);

		// set the session memento if it's not yet set
		if (fSessionMemento == null) {
			fSessionMemento = memento;
		}
	}

	/**
	 * @return Returns <code>true</code> when the data have been modified and not saved
	 */
	public boolean isDirty() {
		return fIsTourDirty;
	}

	public boolean isFromTourEditor() {
		return false;
	}

	/**
	 * @return Returns <code>true</code> when the tour is saved in the database
	 */
	private boolean isTourInDb() {

		if (fTourData == null || fTourData.getTourPerson() == null) {
			return false;
		}

		return true;
	}

	/**
	 * Checks the selection if it contains the current tour
	 * 
	 * @param selection
	 * @return Returns <code>true</code> when the current tour is within the selection
	 */
	private boolean isTourInSelection(final ISelection selection) {

		boolean isCurrentTourSelected = false;

		if (fTourData == null) {
			return false;
		}

		final long currentTourId = fTourData.getTourId();
		fSelectionTourId = null;

		if (selection instanceof SelectionTourData) {

			final TourData tourData = ((SelectionTourData) selection).getTourData();
			fSelectionTourId = tourData.getTourId();

			if (tourData != null && currentTourId == fSelectionTourId) {
				isCurrentTourSelected = true;
			}

		} else if (selection instanceof SelectionTourId) {

			fSelectionTourId = ((SelectionTourId) selection).getTourId();

			if (currentTourId == fSelectionTourId) {
				isCurrentTourSelected = true;
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

				fSelectionTourId = tourData.getTourId();
				if (currentTourId == fSelectionTourId) {
					isCurrentTourSelected = true;
				}
			}

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor == fTourEditor) {
				// the same editor is selected as the current
				isCurrentTourSelected = true;

			} else if (editor instanceof TourEditor) {

				// check tour id in the editor
				final TourEditor tourEditor = (TourEditor) editor;
				fSelectionTourId = tourEditor.getTourChart().getTourData().getTourId();
				if (currentTourId == fSelectionTourId) {

					// get editor data when tour data are set but not yet the tour editor
					getTourEditorData(tourEditor);
					isCurrentTourSelected = true;
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				fSelectionTourId = ((TVICatalogComparedTour) firstElement).getTourId();
				if (currentTourId == fSelectionTourId) {
					isCurrentTourSelected = true;
				}
			}
		}

		return isCurrentTourSelected;
	}

	private void onResizeContainer() {

		fScrolledDataContainer.setMinSize(fDataContainer.computeSize(fScrolledDataContainer.getClientArea().width,
				SWT.DEFAULT));

	}

	private void onSelectionChanged(final ISelection selection) {

		if (fIsSavingInProgress) {
			return;
		}

		// save current tour when another tour is selected
		if (isTourInSelection(selection)) {
			// tour in the selection is already displayed
			return;
		} else {
			// a new tour is selected, save modified tour
			if (saveTourConfirmed() == false) {
				return;
			}
		}

		fDisableModifyEvent = true;

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();
			if (tourData == null) {
				fTourEditor = null;
				fTourChart = null;
			} else {

				final TourChart tourChart = selectionTourData.getTourChart();

				fTourEditor = null;
				fTourChart = tourChart;
				updateUI(tourData, false);
			}

		} else if (selection instanceof SelectionTourId) {

			final Long tourId = ((SelectionTourId) selection).getTourId();

			onSelectTourId(tourId);

		} else if (selection instanceof SelectionActiveEditor) {

			final IEditorPart editor = ((SelectionActiveEditor) selection).getEditor();

			if (editor instanceof TourEditor) {

				/*
				 * prevent loading the data from the same editor when data have not been modified
				 */
				if (editor == fTourEditor && editor.isDirty() == false) {
					return;
				}

				getTourEditorData(((TourEditor) editor));

				final TourData tourData = fTourChart.getTourData();
				updateUI(tourData, false);
			}

		} else if (selection instanceof SelectionChartInfo) {

			final ChartDataModel chartDataModel = ((SelectionChartInfo) selection).chartDataModel;
			if (chartDataModel != null) {

				final TourData tourData = (TourData) chartDataModel.getCustomData(TourManager.CUSTOM_DATA_TOUR_DATA);

				if (fTourData == null) {
					fTourData = tourData;
					fTourEditor = null;
					fTourChart = null;
					updateUI(tourData, false);
				} else {

					if (fTourData.getTourId() != tourData.getTourId()) {

						// a new tour id is in the selection
						fTourData = tourData;
						fTourEditor = null;
						fTourChart = null;
						updateUI(tourData, false);
					}
				}
			}

		} else if (selection instanceof StructuredSelection) {

			final Object firstElement = ((StructuredSelection) selection).getFirstElement();
			if (firstElement instanceof TVICatalogComparedTour) {
				onSelectTourId(((TVICatalogComparedTour) firstElement).getTourId());
			}
		}

		fDisableModifyEvent = false;
	}

	private void onSelectTourId(final Long tourId) {

		// don't reload the same tour
		if (fTourData != null) {
			if (fTourData.getTourId().equals(tourId)) {
				return;
			}
		}

		final TourData tourData = TourManager.getInstance().getTourData(tourId);

		if (tourData != null) {
			fTourEditor = null;
			fTourChart = null;
			updateUI(tourData, false);
		}
	}

	/**
	 * Opens the menu for a control aligned below the control on the left side
	 * 
	 * @param control
	 */
	private void openControlMenu(final Control control) {

		final Rectangle rect = control.getBounds();
		Point pt = new Point(rect.x, rect.y + rect.height);
		pt = control.getParent().toDisplay(pt);

		final Menu menu = control.getMenu();
		menu.setLocation(pt.x, pt.y);
		menu.setVisible(true);
	}

	public void recreateViewer() {

		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {

				// preserve column width, selection and focus
				final ISelection selection = fDataViewer.getSelection();

				final Table table = fDataViewer.getTable();
				final boolean isFocus = table.isFocusControl();

// disabled: new columns are not displayed 
//				fColumnManager.saveState(fSessionMemento);

				fDataViewerContainer.setRedraw(false);
				{
					table.dispose();

					createDataViewer(fDataViewerContainer);
					fDataViewerContainer.layout();

					// update the viewer
					fDataViewer.setInput(new Object());
				}
				fDataViewerContainer.setRedraw(true);

//				// restore selection and focus
//				Display.getCurrent().asyncExec(new Runnable() {
//					public void run() {
//						final Table table = fDataViewer.getTable();
//						table.setRedraw(false);
//						{
				fDataViewer.setSelection(selection, true);
				if (isFocus) {
					fDataViewer.getTable().setFocus();
				}
//						}
//						table.setRedraw(true);
//					}
//				});
			}
		});
	}

	private TourData reloadTourData() {

		final Long tourId = fTourData.getTourId();
		final TourManager tourManager = TourManager.getInstance();

		tourManager.removeTourFromCache(tourId);

		return tourManager.getTourData(tourId);
	}

	/**
	 * reload the content of the viewer
	 */
	public void reloadViewer() {

		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {

				final ISelection selection = fDataViewer.getSelection();

				final Table table = fDataViewer.getTable();
				if (table.isDisposed()) {
					return;
				}

				table.setRedraw(false);
				{

					/*
					 * update the viewer, show busy indicator when it's a large tour or the previous
					 * tour was large because it takes time to remove the old items
					 */
					if (fTourData != null
							&& fTourData.timeSerie != null
							&& fTourData.timeSerie.length > BUSY_INDICATOR_ITEMS
							|| table.getItemCount() > BUSY_INDICATOR_ITEMS) {

						BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
							public void run() {
								fDataViewer.setInput(new Object());
							}
						});
					} else {
						fDataViewer.setInput(new Object());
					}

					fDataViewer.setSelection(selection, true);
				}
				table.setRedraw(true);
			}
		});
	}

	private void restoreStateBeforeUI(final IMemento memento) {

		if (memento != null) {

			// restore from memento

			final Integer mementoRowEditMode = memento.getInteger(MEMENTO_ROW_EDIT_MODE);
			if (mementoRowEditMode != null) {
				fIsRowEditMode = mementoRowEditMode == 1 ? true : false;
			}
		}
	}

	private void restoreStateWithUI(final IMemento memento) {

		if (memento == null) {

			// memento is not set, set defaults

			fTabFolder.setSelection(TAB_INDEX_TITLE);

		} else {

			// restore from memento

			// select tab
			final Integer selectedTab = memento.getInteger(MEMENTO_SELECTED_TAB);
			if (selectedTab != null) {
				fTabFolder.setSelection(selectedTab);
			} else {
				fTabFolder.setSelection(TAB_INDEX_TITLE);
			}

		}

		fActionEditRows.setChecked(fIsRowEditMode);
	}

	private void saveSettings() {
		fSessionMemento = XMLMemento.createWriteRoot("TourPropertiesView"); //$NON-NLS-1$
		saveState(fSessionMemento);
	}

	@Override
	public void saveState(final IMemento memento) {

		// save selected tab
		memento.putInteger(MEMENTO_SELECTED_TAB, fTabFolder.getSelectionIndex());

		memento.putInteger(MEMENTO_ROW_EDIT_MODE, fActionEditRows.isChecked() ? 1 : 0);

		fColumnManager.saveState(memento);
	}

	/**
	 * saves the tour in the {@link TourDataEditorView}
	 */
	public void saveTour() {

		fIsSavingInProgress = true;

		updateTourDataFromUI();

		TourDatabase.saveTour(fTourData);

		fIsTourDirty = false;
		enableControls();

		fIsSavingInProgress = false;
	}

	/**
	 * @return Returns <code>true</code> when the tour was saved or false when saving the tour was
	 *         canceled
	 */
	private boolean saveTourConfirmed() {

		if (fIsTourDirty == false) {
			return true;
		}

		final MessageDialog dialog = new MessageDialog(Display.getCurrent().getActiveShell(),
				Messages.tour_editor_dlg_save_tour_title,
				null,
				NLS.bind(Messages.tour_editor_dlg_save_tour_message, TourManager.getTourDateFull(fTourData)),
				MessageDialog.QUESTION,
				new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL },
				0);

		final int result = dialog.open();

		if (result == 2 || result == -1) {
			// button CANCEL or dialog is canceled
			return false;
		}

		if (result == 0) {

			// button YES
			saveTourInternal();

		} else {

			// button NO

			fIsTourDirty = false;

			fTourData = reloadTourData();

			enableControls();

			fireRevertNotification();
		}

		return true;
	}

	private void saveTourInternal() {

		fIsSavingInProgress = true;

		updateTourDataFromUI();

		if (saveTourInTourEditor()) {
			return;
		}

		// tour was not found in an editor

		TourDatabase.saveTour(fTourData);

		fIsTourDirty = false;
		enableControls();

		TourDatabase.getInstance().firePropertyChange(TourDatabase.TOUR_IS_CHANGED_AND_PERSISTED);

		// notify all views which display the tour type
		final ArrayList<TourData> modifiedTour = new ArrayList<TourData>();
		modifiedTour.add(fTourData);

		TourManager.firePropertyChange(TourManager.TOUR_PROPERTIES_CHANGED, new TourProperties(modifiedTour));

		fIsSavingInProgress = false;
	}

	/**
	 * @return Returns <code>true</code> when the tour was saved in a {@link TourEditor}
	 */
	private boolean saveTourInTourEditor() {

		final Long viewTourId = fTourData.getTourId();

		// check if a tour is opened in the tour editor
		for (final IEditorPart editorPart : UI.getOpenedEditors()) {
			if (editorPart instanceof TourEditor) {

				final IEditorInput editorInput = editorPart.getEditorInput();
				if (editorInput instanceof TourEditorInput) {

					final TourEditor tourEditor = (TourEditor) editorPart;
					final long editorTourId = ((TourEditorInput) editorInput).getTourId();

					if (editorTourId == viewTourId) {

						// a tour editor was found containing the current tour

						if (tourEditor.isDirty()) {

							// save tour in the editor
							editorPart.doSave(null);

							fIsTourDirty = false;
							enableControls();

							fIsSavingInProgress = false;

							// there can be only one editor for a tour
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	@Override
	public void setFocus() {
		fTabFolder.setFocus();
	}

	private void setTourDirty() {

		fIsTourDirty = true;

		enableActions();
	}

	public void tourIsModified() {

		updateUI(fTourData, false);

		setTourDirty();
	}

	private void updateContentOnKeyUp() {

		// set changed data
		fTourData.setTourTitle(fTextTitle.getText());

		enableControls();

		fireModifyNotification();
	}

	/**
	 * display modified time slices in this editor and in other views/editors
	 */
	private void updateTimeSlices() {

		fTourData.clearComputedSeries();

		reloadViewer();

		enableControls();

		fireModifyNotification();
	}

	/**
	 * update {@link TourData} from the ui fields
	 */
	private void updateTourDataFromUI() {

		try {

			fTourData.setTourTitle(fTextTitle.getText());
			fTourData.setTourDescription(fTextDescription.getText());

			fTourData.setTourStartPlace(fTextStartLocation.getText());
			fTourData.setTourEndPlace(fTextEndLocation.getText());

			fTourData.setStartYear((short) fTourDate.getYear());
			fTourData.setStartMonth((short) (fTourDate.getMonth() + 1));
			fTourData.setStartDay((short) fTourDate.getDay());

			fTourData.setStartHour((short) fStartTime.getHours());
			fTourData.setStartMinute((short) fStartTime.getMinutes());

			// set week of year
			fCalendar.set(fTourData.getStartYear(), fTourData.getStartMonth() - 1, fTourData.getStartDay());
			fTourData.setStartWeek((short) fCalendar.get(Calendar.WEEK_OF_YEAR));

			float distanceValue = Float.parseFloat(fTextTourDistance.getText());
			distanceValue = distanceValue * UI.UNIT_VALUE_DISTANCE * 1000;
			fTourData.setTourDistance((int) distanceValue);

		} catch (final NumberFormatException e) {
			// this should not happen
			e.printStackTrace();
		}
	}

	/**
	 * updates the fields in the tour data editor and enables actions and controls
	 * 
	 * @param tourData
	 * @param forceReload
	 */
	private void updateUI(final TourData tourData, final boolean forceReload) {

		// keep tour data
		fTourData = tourData;

		// show tour type image when tour type is set
		final TourType tourType = tourData.getTourType();
		if (tourType == null) {
			fEditorForm.setImage(null);
		} else {
			fEditorForm.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
		}

		fEditorForm.setText(TourManager.getTourTitle(tourData));

		updateUITabData();
		updateUITabTimeSlices(forceReload);

		enableControls();

		fPageBook.showPage(fEditorForm);
	}

	private void updateUITabData() {

		fTextTitle.setText(fTourData.getTourTitle());
		fTextDescription.setText(fTourData.getTourDescription());

		fTextStartLocation.setText(fTourData.getTourStartPlace());
		fTextEndLocation.setText(fTourData.getTourEndPlace());

		// tour date
		fTourDate.setDate(fTourData.getStartYear(), fTourData.getStartMonth() - 1, fTourData.getStartDay());

		// start time
		fStartTime.setTime(fTourData.getStartHour(), fTourData.getStartMinute(), 0);

		// recording time
		final int recordingTime = fTourData.getTourRecordingTime();
		if (recordingTime == 0) {
			fLblRecordingTime.setText(UI.EMPTY_STRING);
		} else {
			fCalendar.set(0, 0, 0, recordingTime / 3600, ((recordingTime % 3600) / 60), ((recordingTime % 3600) % 60));

			fLblRecordingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// driving time
		final int drivingTime = fTourData.getTourDrivingTime();
		if (drivingTime == 0) {
			fLblDrivingTime.setText(UI.EMPTY_STRING);
		} else {
			fCalendar.set(0, 0, 0, drivingTime / 3600, ((drivingTime % 3600) / 60), ((drivingTime % 3600) % 60));

			fLblDrivingTime.setText(fDurationFormatter.format(fCalendar.getTime()));
		}

		// data points
		final int[] timeSerie = fTourData.timeSerie;
		if (timeSerie == null) {
			fLblDatapoints.setText(UI.EMPTY_STRING);
		} else {
			final int dataPoints = timeSerie.length;
			fLblDatapoints.setText(Integer.toString(dataPoints));
		}

		// tour type
		final TourType tourType = fTourData.getTourType();
		if (tourType == null) {
			fLblTourType.setText(UI.EMPTY_STRING);
			fLblTourType.setImage(null);
		} else {
			fLblTourType.setImage(UI.getInstance().getTourTypeImage(tourType.getTypeId()));
			fLblTourType.setText(tourType.getName());
		}

		// tour tags
		final Set<TourTag> tourTags = fTourData.getTourTags();

		if (tourTags == null || tourTags.size() == 0) {

			fLblTourTags.setText(UI.EMPTY_STRING);

		} else {

			// sort tour tags by name
			final ArrayList<TourTag> tourTagList = new ArrayList<TourTag>(tourTags);
			Collections.sort(tourTagList, new Comparator<TourTag>() {
				public int compare(final TourTag tt1, final TourTag tt2) {
					return tt1.getTagName().compareTo(tt2.getTagName());
				}
			});

			final StringBuilder sb = new StringBuilder();
			int index = 0;
			for (final TourTag tourTag : tourTagList) {

				if (index > 0) {
					sb.append(", "); //$NON-NLS-1$
				}

				sb.append(tourTag.getTagName());

				index++;
			}
			fLblTourTags.setText(sb.toString());
		}

		// device name
		fLblDeviceName.setText(fTourData.getDeviceName());

		// tour distance
		final int tourDistance = fTourData.getTourDistance();
		if (tourDistance == 0) {
			fTextTourDistance.setText(Integer.toString(tourDistance));
		} else {

			fNumberFormatter.setMinimumFractionDigits(3);
			fNumberFormatter.setMaximumFractionDigits(3);

			fTextTourDistance.setText(fNumberFormatter.format(((float) tourDistance) / 1000 / UI.UNIT_VALUE_DISTANCE));
		}
		fLblTourDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

//		onResizeContainer();
		fDataContainer.layout(true);
	}

	private void updateUITabTimeSlices(final boolean forceReload) {

		if (forceReload) {
			fPostReloadViewerTourId = -1L;
		}

		if (fTabFolder.getSelectionIndex() == TAB_INDEX_TIME_SLICES //
				&& fPostReloadViewerTourId != fTourData.getTourId()) {

			// load the table only when the viewer is displayed and not yet loaded

			reloadViewer();
			fPostReloadViewerTourId = fTourData.getTourId();

		} else {

			if (fPostReloadViewerTourId != fTourData.getTourId()) {
				// force reload when it's not yet loaded
				fPostReloadViewerTourId = -1L;
			}
		}
	}

}
