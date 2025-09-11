/*******************************************************************************
 * Copyright (C) 2012, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.IContextMenuProvider;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.photo.Camera;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.PicDirView;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tourType.TourTypeImage;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.TableColumnFactory;
import net.tourbook.ui.action.ActionEditTour;
import net.tourbook.ui.views.tourDataEditor.NewTourContext;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.RegistryToggleState;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class TourPhotoLinkView extends ViewPart implements ITourProvider, ITourViewer {

   public static final String               ID                                  = "net.tourbook.photo.PhotosAndToursView.ID"; //$NON-NLS-1$

   private static final String              ITEM_SEPARATOR                      = " | ";                                      //$NON-NLS-1$

   private static final String              STATE_FILTER_NOT_SAVED_PHOTOS       = "STATE_FILTER_NOT_SAVED_PHOTOS";            //$NON-NLS-1$
   private static final String              STATE_FILTER_TOUR_WITH_PHOTOS       = "STATE_FILTER_TOUR_WITH_PHOTOS";            //$NON-NLS-1$
   private static final String              STATE_SELECTED_CAMERA_NAME          = "STATE_SELECTED_CAMERA_NAME";               //$NON-NLS-1$
   private static final String              STATE_SELECTED_TIME_ADJUSTMENT_TYPE = "STATE_SELECTED_TIME_ADJUSTMENT_TYPE";      //$NON-NLS-1$

   public static final String               IMAGE_PIC_DIR_VIEW                  = "IMAGE_PIC_DIR_VIEW";                       //$NON-NLS-1$
   public static final String               IMAGE_PHOTO_PHOTO                   = "IMAGE_PHOTO_PHOTO";                        //$NON-NLS-1$

   private static final ComboEnumEntry<?>[] ALL_TIME_ADJUSTMENT_TYPES;

// SET_FORMATTING_OFF

   static {

      ALL_TIME_ADJUSTMENT_TYPES = new ComboEnumEntry<?>[] {

         new ComboEnumEntry<>(Messages.Photos_AndTours_AdjustmentType_SelectedAdjustment, TimeAdjustmentType.SELECT_AJUSTMENT),
         new ComboEnumEntry<>(Messages.Photos_AndTours_AdjustmentType_SavedAdjustment,    TimeAdjustmentType.SAVED_AJUSTMENT),
         new ComboEnumEntry<>(Messages.Photos_AndTours_AdjustmentType_NoAdjustment,       TimeAdjustmentType.NO_AJUSTMENT),
      };
   }

// SET_FORMATTING_ON

   private static final IPreferenceStore      _prefStore                      = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore      _prefStore_Common               = CommonActivator.getPrefStore();
   private static final IDialogSettings       _state                          = TourbookPlugin.getState(ID);
   //
   private static final TourPhotoManager      _photoMgr                       = TourPhotoManager.getInstance();
   //
   private TableViewer                        _tourViewer;
   private ColumnManager                      _columnManager;
   private MenuManager                        _viewerMenuManager;
   private IContextMenuProvider               _tableViewerContextMenuProvider = new TableContextMenuProvider();
   //
   private ArrayList<TourPhotoLink>           _allVisibleTourPhotoLinks       = new ArrayList<>();
   private ArrayList<Photo>                   _allPhotos                      = new ArrayList<>();
   private ArrayList<Photo>                   _allPrevPhotos;

   /**
    * Contains all cameras which are used in all displayed tours.
    */
   private HashMap<String, Camera>            _allTourCameras                 = new HashMap<>();

   /**
    * Contains all cameras sorted by camera name, they are displayed in the combobox
    */
   private Camera[]                           _allTourCamerasSorted;

   /**
    * Tour photo link which is currently selected in the tour/history viewer
    */
   private ArrayList<TourPhotoLink>           _allSelectedPhotoLinks          = new ArrayList<>();

   /**
    * Contains only tour photo links with real tours and which contain geo positions.
    */
   private List<TourPhotoLink>                _selectedTourPhotoLinksWithGps  = new ArrayList<>();

   private TourPhotoLinkSelection             _tourPhotoLinkSelection;

   private SelectionListener                  _defaultSelectionListener;
   private MouseWheelListener                 _defaultMouseWheelListener;
   private IPartListener2                     _partListener;
   private IPropertyChangeListener            _prefChangeListener;
   private IPropertyChangeListener            _prefChangeListener_Common;
   private ISelectionListener                 _postSelectionListener;
   private ITourEventListener                 _tourEventListener;
   //
   private ActionCreatePhotoTour              _actionCreatePhotoTour;
   private ActionEditTour                     _actionEditTour;
   private ActionFilterOneHistoryTour         _actionFilterOneHistory;
   private ActionFilterTourWithPhotos         _actionFilterTourWithPhotos;
   private ActionFilterTourWithoutSavedPhotos _actionFilterTourWithoutSavedPhotos;
   private ActionSavePhotosInTour             _actionSavePhotoInTour;
   private ActionSetToSavedAdjustment         _actionSetToSavedAdjustment;
   //
   private final PeriodFormatter              _durationFormatter;
   private final NumberFormat                 _nf_1_1;
   {
      _nf_1_1 = NumberFormat.getNumberInstance();
      _nf_1_1.setMinimumFractionDigits(1);
      _nf_1_1.setMaximumFractionDigits(1);

      _durationFormatter = new PeriodFormatterBuilder()
            .appendYears()
            .appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
            .appendMonths()
            .appendSuffix("m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
            .appendDays()
            .appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
            .appendHours()
            .appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
            .toFormatter();
   }

   /**
    * When <code>true</code>, only tours with photos are displayed.
    */
   private boolean                  _isShowToursWithPhotos           = true;

   /**
    * When <code>true</code>, only tours with <b>NOT</b> saved photos are displayed.
    */
   private boolean                  _isShowToursWithoutSavedPhotos;

   /**
    * It's dangerous when set to <code>true</code>, it will hide all tours which can confuses the
    * user, therefore this state is <b>NOT</b> saved.
    */
   private boolean                  _isFilterOneHistoryTour          = false;

   private ArrayList<TourPhotoLink> _selectionBackupBeforeOneHistory = new ArrayList<>();

   private ICommandService          _commandService;

   private PixelConverter           _pc;

   /*
    * UI controls
    */
   private PageBook  _pageBook;
   private Composite _pageNoImage;
   private Composite _pageViewer;

   private Composite _viewerContainer;

   private Combo     _comboAdjustTime;
   private Combo     _comboCamera;

   private Spinner   _spinnerHours;
   private Spinner   _spinnerMinutes;
   private Spinner   _spinnerSeconds;

   private Menu      _tableContextMenu;

   private Label     _lblAdjustTime;

   private class ActionCreatePhotoTour extends Action {

      public ActionCreatePhotoTour() {

         super(Messages.Photos_AndTours_Action_CreatePhotoTour);

         setToolTipText(Messages.Photos_AndTours_Action_CreatePhotoTour_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourNew));
      }

      @Override
      public void run() {
         actionCreatePhotoTour();
      }
   }

   private class ActionSavePhotosInTour extends Action {

      public ActionSavePhotosInTour() {

         super(Messages.Action_PhotosAndTours_SaveAllPhotos, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         actionSaveAllPhotosInTour();
      }
   }

   private class ActionSetToSavedAdjustment extends Action {

      /**
       * Common action to reset values to it's defaults
       *
       * @param restoreAction
       */
      public ActionSetToSavedAdjustment() {

         super();

         setToolTipText(Messages.Photos_AndTours_Action_SetToSavedAdjustment_Tooltip);

         setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.PhotoTimeAdjustment));
      }

      @Override
      public void run() {
         actionSetTimeToSavedAdjustment();
      }
   }

   private static class ContentComparator extends ViewerComparator {

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourPhotoLink mt1 = (TourPhotoLink) e1;
         final TourPhotoLink mt2 = (TourPhotoLink) e2;

         /*
          * sort by time
          */
         final long mt1Time = mt1.isHistoryTour ? mt1.historyStartTime : mt1.tourStartTime;
         final long mt2Time = mt2.isHistoryTour ? mt2.historyStartTime : mt2.tourStartTime;

         if (mt1Time != 0 && mt2Time != 0) {
            return mt1Time > mt2Time ? 1 : -1;
         }

         return mt1Time != 0 ? 1 : -1;
      }
   }

   private class ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allVisibleTourPhotoLinks.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public class TableContextMenuProvider implements IContextMenuProvider {

      @Override
      public void disposeContextMenu() {

         if (_tableContextMenu != null) {
            _tableContextMenu.dispose();
         }
      }

      @Override
      public Menu getContextMenu() {
         return _tableContextMenu;
      }

      @Override
      public Menu recreateContextMenu() {

         disposeContextMenu();

         _tableContextMenu = createUI_54_CreateViewerContextMenu();

         return _tableContextMenu;
      }

   }

   public static enum TimeAdjustmentType {

      NO_AJUSTMENT, //
      SAVED_AJUSTMENT, //
      SELECT_AJUSTMENT, //
   }

   public TourPhotoLinkView() {
      super();
   }

   private void actionCreatePhotoTour() {

      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourDataEditorView tourEditor = TourManager.openTourEditor(true);

      if (tourEditor == null) {
         return;
      }

      final TourPhotoLink selectedPhotoLink = _allSelectedPhotoLinks.get(0);

      final long tourStartTime = selectedPhotoLink.historyStartTime;
      final long tourEndTime = selectedPhotoLink.historyEndTime;

      final ArrayList<Photo> allLinkPhotos = selectedPhotoLink.linkPhotos;

      final int numPhotos = allLinkPhotos.size();
      final int[] timeSerie = new int[numPhotos];

      for (int photoIndex = 0; photoIndex < numPhotos; photoIndex++) {

         final Photo photo = allLinkPhotos.get(photoIndex);

         final long adjustedTime_Camera = photo.adjustedTime_Camera;

         final int relativeTime = (int) ((adjustedTime_Camera - tourStartTime) / 1000);

         timeSerie[photoIndex] = relativeTime;
      }

      final NewTourContext newTourContext = new NewTourContext();

      newTourContext.title = Messages.Photos_AndTours_TourTitle_PhotoTour;
      newTourContext.tourStartTime = tourStartTime;
      newTourContext.tourEndTime = tourEndTime;
      newTourContext.timeSerie = timeSerie;

      /*
       * Create a new tour
       */
      tourEditor.actionCreateTour(null, newTourContext);

      final TourData newTourData = newTourContext.newTourData;
      if (newTourData == null) {
         return;
      }

      /*
       * Set all photos into the new tour
       */

      // contains all photos, modified and not modified
      final HashSet<Photo> allPhotos = new HashSet<>();

      setAllPhotosInTour(newTourData, allLinkPhotos, allPhotos);

// I DON'T KNOW IF THIS IS NEEDED
//
//      PhotoManager.firePhotoEvent(this,
//            PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED,
//            new ArrayList<>(allPhotos));
   }

   void actionFilter_NotSavedPhotos() {

      _isShowToursWithoutSavedPhotos = _actionFilterTourWithoutSavedPhotos.isChecked();

      updateUI();
   }

   void actionFilter_OneHistoryTour() {

      _isFilterOneHistoryTour = _actionFilterOneHistory.isChecked();

      ArrayList<TourPhotoLink> links = null;

      if (_isFilterOneHistoryTour) {

         // backup current selection

         final ISelection selection = _tourViewer.getSelection();
         if (selection instanceof StructuredSelection) {

            _selectionBackupBeforeOneHistory.clear();

            for (final Object linkElement : ((StructuredSelection) selection).toArray()) {
               if (linkElement instanceof TourPhotoLink) {
                  _selectionBackupBeforeOneHistory.add((TourPhotoLink) linkElement);
               }
            }
         }

      } else {

         links = _selectionBackupBeforeOneHistory;
      }

      updateUI(null, links, false);

      enableControls();

      if (_isFilterOneHistoryTour == false) {

         final Table table = _tourViewer.getTable();

         table.setSelection(table.getSelectionIndices());
      }
   }

   void actionFilter_Photos() {

      _isShowToursWithPhotos = _actionFilterTourWithPhotos.isChecked();

      updateUI();
   }

   private void actionSaveAllPhotosInTour() {

      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourManager tourManager = TourManager.getInstance();

      // contains all photos, modified and not modified
      final HashSet<Photo> allPhotos = new HashSet<>();

      final ArrayList<TourData> allModifiedTours = new ArrayList<>();
      final ArrayList<TourPhotoLink> allModifiedLinks = new ArrayList<>();

      final Object[] allSelectedPhotoLinks = ((IStructuredSelection) _tourViewer.getSelection()).toArray();

      int numHistoryTours = 0;
      int numRealTours = 0;

      for (final Object selectedItem : allSelectedPhotoLinks) {

         if (selectedItem instanceof final TourPhotoLink photoLink) {

            final boolean isRealTour = photoLink.tourId != Long.MIN_VALUE;

            if (isRealTour) {

               final ArrayList<Photo> allLinkPhotos = photoLink.linkPhotos;

               if (allLinkPhotos.size() > 0) {

                  final TourData tourData = tourManager.getTourData(photoLink.tourId);

                  if (tourData != null) {

                     numRealTours++;

                     setAllPhotosInTour(tourData, allLinkPhotos, allPhotos);

                     allModifiedTours.add(tourData);
                     allModifiedLinks.add(photoLink);
                  }
               }

            } else {

               numHistoryTours++;
            }
         }
      }

      /*
       * It happened accidentally that the adjusted time was saved for all links :-((( -> a warning
       * is now displayed
       */
      if (numRealTours > 1) {

         if (new MessageDialog(

               Display.getCurrent().getActiveShell(),

               Messages.Photos_AndTours_Dialog_SavePhotos_Title,
               null, // no title image

               Messages.Photos_AndTours_Dialog_SavePhotos_Message.formatted(numRealTours),
               MessageDialog.CONFIRM,

               1, // default index

               Messages.App_Action_Save,
               Messages.App_Action_Cancel

         ).open() != IDialogConstants.OK_ID) {

            return;
         }
      }

      // show message that photos can be saved only in real tours
      if (numHistoryTours > 0) {

         if (_prefStore.getBoolean(ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING) == false) {

            final MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(
                  Display.getCurrent().getActiveShell(),
                  Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Title,
                  Messages.Photos_AndTours_Dialog_CannotSaveHistoryTour_Message,
                  Messages.App_ToggleState_DoNotShowAgain,
                  false, // toggle default state
                  null,
                  null);

            // save toggle state
            _prefStore.setValue(
                  ITourbookPreferences.TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING,
                  dialog.getToggleState());
         }

      }

      final ArrayList<TourData> savedTours = TourManager.saveModifiedTours(allModifiedTours);

      /*
       * After saving tour + photos, update the photos and put them into the photo cache
       */
      for (final TourData savedTourData : savedTours) {
         savedTourData.createGalleryPhotos();
      }

      // update viewer data
      for (final TourPhotoLink photoLink : allModifiedLinks) {

         final TourData tourData = tourManager.getTourData(photoLink.tourId);

         if (tourData != null) {

            photoLink.numTourPhotos = tourData.getTourPhotos().size();
            photoLink.photoTimeAdjustment = tourData.getPhotoTimeAdjustment();
            photoLink.updateAllTimeAdjustments();
         }
      }

      // update viewer UI
      _tourViewer.update(allModifiedLinks.toArray(), null);

      PhotoManager.firePhotoEvent(this,
            PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED,
            new ArrayList<>(allPhotos));
   }

   private void actionSetTimeToSavedAdjustment() {

      // set time adjustment into the selected camera
      final Camera camera = getSelectedCamera();
      if (camera == null) {
         return;
      }

      final TourPhotoLink selectedPhotoLink = _allSelectedPhotoLinks.get(0);

      camera.setTimeAdjustment(selectedPhotoLink.photoTimeAdjustment * 1000);

      updateUI();
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {

         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == TourPhotoLinkView.this) {
               onPartActivate();
            }
         }

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourPhotoLinkView.this) {

               // close sql connections
               _photoMgr.resetTourStartEnd();
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {}

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {}
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {

               // app filter is modified

               // sql filter is dirty, force reloading cached start/end
               _photoMgr.resetTourStartEnd();

               updateUI();

            } else if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

               _tourViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

               _tourViewer.refresh();

               /*
                * the tree must be redrawn because the styled text does not show with the new color
                */
               _tourViewer.getTable().redraw();
            }
         }
      };

      _prefChangeListener_Common = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

//					// measurement system has changed
//
//					UI.updateUnits();
//					updateInternalUnitValues();
//
//					_columnManager.saveState(_state);
//					_columnManager.clearColumns();
//					defineAllColumns(_viewerContainer);
//
//					_tourViewer = (TableViewer) recreateViewer(_tourViewer);
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            if (part == TourPhotoLinkView.this) {
               return;
            }
            onSelectionChanged(selection, part);
         }
      };
      getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourPhotoLinkView.this) {
               return;
            }

            if (part instanceof TourDataEditorView) {

               // a tour date/time could be changed -> recomputed view

               showPhotosAndTours(_allPrevPhotos, false);

            } else {

               if (eventId == TourEventId.TOUR_CHANGED && eventData instanceof TourEvent) {

                  // get modified tours
                  final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
                  if (modifiedTours != null) {

                     onTourChanged(modifiedTours);
                  }

               } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

                  clearView();
               }
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   private void clearView() {

      if (_pageBook.isDisposed()) {
         return;
      }

      _allVisibleTourPhotoLinks.clear();
      _allPhotos.clear();
      _allSelectedPhotoLinks.clear();
      _selectedTourPhotoLinksWithGps.clear();
      _tourPhotoLinkSelection = null;

      _tourViewer.setInput(new Object[0]);

      enableControls();

      _pageBook.showPage(_pageNoImage);
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionCreatePhotoTour              = new ActionCreatePhotoTour();
      _actionEditTour                     = new ActionEditTour(this);
      _actionFilterOneHistory             = new ActionFilterOneHistoryTour(this);
      _actionFilterTourWithoutSavedPhotos = new ActionFilterTourWithoutSavedPhotos(this);
      _actionFilterTourWithPhotos         = new ActionFilterTourWithPhotos(this);
      _actionSavePhotoInTour              = new ActionSavePhotosInTour();
      _actionSetToSavedAdjustment         = new ActionSetToSavedAdjustment();

// SET_FORMATTING_ON
   }

   private void createMenuManager() {

      _viewerMenuManager = new MenuManager();
      _viewerMenuManager.setRemoveAllWhenShown(true);
      _viewerMenuManager.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager menuManager) {
            fillContextMenu(menuManager);
         }
      });
   }

   @Override
   public void createPartControl(final Composite parent) {

      initUI(parent);

      createMenuManager();

      _columnManager = new ColumnManager(this, _state);
      defineAllColumns(parent);

      createActions();

      createUI(parent);

      fillToolbar();

      addSelectionListener();
      addPrefListener();
      addPartListener();
      addTourEventListener();

      restoreState();

      enableControls();

      _commandService = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(ICommandService.class);

      // show default page
      _pageBook.showPage(_pageNoImage);
   }

   private void createUI(final Composite parent) {

      _pageBook = new PageBook(parent, SWT.NONE);
      {
         _pageViewer = new Composite(_pageBook, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_pageViewer);
         GridLayoutFactory.fillDefaults().applyTo(_pageViewer);
//			_pageViewer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
         {
            createUI_10_Container(_pageViewer);
         }

         _pageNoImage = createUI_90_PageNoImage(_pageBook);
      }
   }

   private void createUI_10_Container(final Composite parent) {

      _viewerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
      GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_viewerContainer);
      {
         createUI_40_Header(_viewerContainer);
         createUI_50_TourViewer(_viewerContainer);
      }
   }

   private void createUI_40_Header(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(7)
            .margins(2, 2)
            .applyTo(container);
      {
         {
            /*
             * Label: Adjust time
             */
            _lblAdjustTime = new Label(container, SWT.NONE);
            _lblAdjustTime.setText(Messages.Photos_AndTours_Label_AdjustTime);
            _lblAdjustTime.setToolTipText(Messages.Photos_AndTours_Label_AdjustTime_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_lblAdjustTime);
         }
         {
            /*
             * Combo: Adjust time by
             */
            _comboAdjustTime = new Combo(container, SWT.READ_ONLY);
            _comboAdjustTime.setVisibleItemCount(33);
            _comboAdjustTime.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
//                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_comboAdjustTime);

            // fill static combo
            for (final ComboEnumEntry<?> option : ALL_TIME_ADJUSTMENT_TYPES) {
               _comboAdjustTime.add(option.label);
            }
         }
         {
            /*
             * Spinner: Adjust hours
             */
            _spinnerHours = new Spinner(container, SWT.BORDER);
            _spinnerHours.setMinimum(-100);
            _spinnerHours.setMaximum(100);
            _spinnerHours.setIncrement(1);
            _spinnerHours.setPageIncrement(24);
            _spinnerHours.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustHours_Tooltip);
            _spinnerHours.addSelectionListener(_defaultSelectionListener);
            _spinnerHours.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerHours);
         }
         {
            /*
             * Spinner: Adjust minutes
             */
            _spinnerMinutes = new Spinner(container, SWT.BORDER);
            _spinnerMinutes.setMinimum(-100);
            _spinnerMinutes.setMaximum(100);
            _spinnerMinutes.setIncrement(1);
            _spinnerMinutes.setPageIncrement(10);
            _spinnerMinutes.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustMinutes_Tooltip);
            _spinnerMinutes.addSelectionListener(_defaultSelectionListener);
            _spinnerMinutes.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerMinutes);
         }
         {
            /*
             * Spinner: adjust seconds
             */
            _spinnerSeconds = new Spinner(container, SWT.BORDER);
            _spinnerSeconds.setMinimum(-100);
            _spinnerSeconds.setMaximum(100);
            _spinnerSeconds.setIncrement(1);
            _spinnerSeconds.setPageIncrement(10);
            _spinnerSeconds.setToolTipText(Messages.Photos_AndTours_Spinner_AdjustSeconds_Tooltip);
            _spinnerSeconds.addSelectionListener(_defaultSelectionListener);
            _spinnerSeconds.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults().applyTo(_spinnerSeconds);
         }
         {
            /*
             * Action: Set to saved adjustment
             */
            final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
//            GridDataFactory.fillDefaults()
//                  .grab(true, false)
//                  .align(SWT.END, SWT.BEGINNING)
//                  .applyTo(toolbar);

            final ToolBarManager tbm = new ToolBarManager(toolbar);

            tbm.add(_actionSetToSavedAdjustment);

            tbm.update(true);
         }
         {
            /*
             * Combo: Camera
             */
            _comboCamera = new Combo(container, SWT.READ_ONLY);
            _comboCamera.setVisibleItemCount(33);
            _comboCamera.setToolTipText(Messages.Photos_AndTours_Combo_Camera_Tooltip);
            _comboCamera.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectCamera()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_comboCamera);
         }
      }
   }

   private void createUI_50_TourViewer(final Composite parent) {

      final Table table = new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);

      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
      table.setHeaderVisible(true);
      table.setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

      /*
       * create table viewer
       */
      _tourViewer = new TableViewer(table);
      _columnManager.createColumns(_tourViewer);

      _tourViewer.setUseHashlookup(true);
      _tourViewer.setContentProvider(new ContentProvider());
      _tourViewer.setComparator(new ContentComparator());

      _tourViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            final ISelection eventSelection = event.getSelection();
            if (eventSelection instanceof StructuredSelection) {
               onSelectTourLink(((StructuredSelection) eventSelection).toArray());
            }
         }
      });

      createUI_52_ContextMenu();
   }

   /**
    * create the views context menu
    */
   private void createUI_52_ContextMenu() {

      _tableContextMenu = createUI_54_CreateViewerContextMenu();

      final Table table = _tourViewer.getTable();

      _columnManager.createHeaderContextMenu(table, _tableViewerContextMenuProvider);
   }

   /**
    * create the views context menu
    */
   private Menu createUI_54_CreateViewerContextMenu() {

      final Table table = _tourViewer.getTable();
      final Menu tableContextMenu = _viewerMenuManager.createContextMenu(table);

      table.setMenu(tableContextMenu);

      return tableContextMenu;
   }

   private Composite createUI_90_PageNoImage(final Composite parent) {

      final int defaultWidth = 200;

      final Composite page = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(page);
      {
         final Composite container = new Composite(page, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
         {
            final Label label = new Label(container, SWT.WRAP);
            label.setText(Messages.Photos_AndTours_Label_NoSelectedPhoto);
            GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(label);

            /*
             * Link: import
             */
            final Image picDirIcon = net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PIC_DIR_VIEW);

            final CLabel iconPicDirView = new CLabel(container, SWT.NONE);
            GridDataFactory.fillDefaults().indent(0, 10).applyTo(iconPicDirView);
            iconPicDirView.setImage(picDirIcon);
            iconPicDirView.setText(UI.EMPTY_STRING);

            final Link linkImport = new Link(container, SWT.NONE);
            linkImport.setText(Messages.Photos_AndTours_Link_PhotoDirectory);
            linkImport.addSelectionListener(widgetSelectedAdapter(selectionEvent -> Util.showView(PicDirView.ID, true)));
            GridDataFactory.fillDefaults()
                  .hint(defaultWidth, SWT.DEFAULT)
                  .align(SWT.FILL, SWT.CENTER)
                  .grab(true, false)
                  .indent(0, 10)
                  .applyTo(linkImport);
         }
      }

      return page;
   }

   private void defineAllColumns(final Composite parent) {

      defineColumn_Tour_TypeImage();
      defineColumn_Photo_NumberOfTourPhotos();
      defineColumn_Photo_TimeAdjustment();
      defineColumn_Photo_TimeAdjustment_All();
      defineColumn_Photo_NumberOfGPSPhotos();
      defineColumn_Photo_NumberOfNoGPSPhotos();

      defineColumn_Time_TourStartDate();
      defineColumn_Time_TourDurationTime();

      defineColumn_Photo_TourCameras();
      defineColumn_Photo_FilePath();

      defineColumn_Time_TourStartTime();
      defineColumn_Time_TourEndDate();
      defineColumn_Time_TourEndTime();

      defineColumn_Tour_TypeText();
   }

   /**
    * column: number of photos which are saved in the tour
    */
   private void defineColumn_Photo_FilePath() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_FILE_PATH.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final String photoFilePath = photoLink.photoFilePath;

            cell.setText(photoFilePath == null ? UI.EMPTY_STRING : photoFilePath);

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: number of photos which contain gps data
    */
   private void defineColumn_Photo_NumberOfGPSPhotos() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_GPS_PHOTOS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final int numberOfGPSPhotos = photoLink.numGPSPhotos;

            cell.setText(numberOfGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfGPSPhotos));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: number of photos which contain gps data
    */
   private void defineColumn_Photo_NumberOfNoGPSPhotos() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_NO_GPS_PHOTOS.createColumn(
            _columnManager,
            _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final int numberOfNoGPSPhotos = photoLink.numNoGPSPhotos;

            cell.setText(numberOfNoGPSPhotos == 0 ? UI.EMPTY_STRING : Long.toString(numberOfNoGPSPhotos));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: number of photos which are saved in the tour
    */
   private void defineColumn_Photo_NumberOfTourPhotos() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_NUMBER_OF_PHOTOS.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final int numberOfPhotos = photoLink.numTourPhotos;

            cell.setText(numberOfPhotos == 0 ? UI.EMPTY_STRING : Integer.toString(numberOfPhotos));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * Column: Time difference between time in photo (EXIF time) and displayed time. This is an
    * average value for all photos.
    */
   private void defineColumn_Photo_TimeAdjustment() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_TIME_ADJUSTMENT.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final int numTourPhotos = photoLink.numTourPhotos;
            final int timeAdjustment = photoLink.photoTimeAdjustment;

            cell.setText(numTourPhotos == 0
                  ? UI.EMPTY_STRING
                  : UI.formatHhMmSs(timeAdjustment));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * Column: Time difference between time in photo (EXIF time) and displayed time
    */
   private void defineColumn_Photo_TimeAdjustment_All() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_TIME_ADJUSTMENT_ALL.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final int numTourPhotos = photoLink.numTourPhotos;

            if (numTourPhotos == 0) {

               cell.setText(UI.EMPTY_STRING);

            } else {

               final Map<Long, Integer> allTimeAdjustments = photoLink.getAllPhotoTimeAdjustments();

               final StringBuilder sb = new StringBuilder();
               int numEntries = 0;

               for (final Entry<Long, Integer> entrySet : allTimeAdjustments.entrySet()) {

                  // append spacer
                  if (numEntries++ > 0) {
                     sb.append(ITEM_SEPARATOR);
                  }

                  sb.append(entrySet.getValue() + UI.COLON_SPACE + UI.formatHhMmSs(entrySet.getKey()));
               }

               cell.setText(sb.toString());
            }

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour type text
    */
   private void defineColumn_Photo_TourCameras() {

      final ColumnDefinition colDef = TableColumnFactory.PHOTO_TOUR_CAMERA.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();

            cell.setText(photoLink.tourCameras);

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: duration time
    */
   private void defineColumn_Time_TourDurationTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_DURATION_TIME.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();

            final Period period = photoLink.tourPeriod;

            int periodSum = 0;
            for (final int value : period.getValues()) {
               periodSum += value;
            }

            if (periodSum == 0) {
               // < 1 h
               cell.setText(Messages.Photos_AndTours_Label_DurationLess1Hour);
            } else {
               // > 1 h
               cell.setText(period.toString(_durationFormatter));
            }

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour end date
    */
   private void defineColumn_Time_TourEndDate() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_END_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
//		colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final long historyTime = photoLink.historyEndTime;

            ZonedDateTime zonedDateTime;

            if (historyTime != Long.MIN_VALUE) {

               // this is a history tour

               zonedDateTime = TimeTools.getZonedDateTime(historyTime);

            } else {

               // this is a real tour

               final ZonedDateTime tourEndDateTime_WithZoneID = photoLink.getTourEndDateTime_WithZoneID();

               zonedDateTime = tourEndDateTime_WithZoneID != null

                     ? tourEndDateTime_WithZoneID
                     : TimeTools.getZonedDateTime(photoLink.tourEndTime);
            }

            cell.setText(zonedDateTime.format(TimeTools.Formatter_Date_S));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour end time
    */
   private void defineColumn_Time_TourEndTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_END_TIME.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final long historyTime = photoLink.historyEndTime;

            ZonedDateTime zonedDateTime;

            if (historyTime != Long.MIN_VALUE) {

               // this is a history tour

               zonedDateTime = TimeTools.getZonedDateTime(historyTime);

            } else {

               // this is a real tour

               final ZonedDateTime tourEndDateTime_WithZoneID = photoLink.getTourEndDateTime_WithZoneID();

               zonedDateTime = tourEndDateTime_WithZoneID != null

                     ? tourEndDateTime_WithZoneID
                     : TimeTools.getZonedDateTime(photoLink.tourEndTime);
            }

            cell.setText(zonedDateTime.format(TimeTools.Formatter_Time_M));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour start date
    */
   private void defineColumn_Time_TourStartDate() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_DATE.createColumn(_columnManager, _pc);
//		colDef.setCanModifyVisibility(false);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final long historyTime = photoLink.historyStartTime;

            ZonedDateTime zonedDateTime;

            if (historyTime != Long.MIN_VALUE) {

               // this is a history tour

               zonedDateTime = TimeTools.getZonedDateTime(historyTime);

            } else {

               // this is a real tour

               final ZonedDateTime tourStartDateTime_WithZoneID = photoLink.getTourStartDateTime_WithZoneID();

               zonedDateTime = tourStartDateTime_WithZoneID != null

                     ? tourStartDateTime_WithZoneID
                     : TimeTools.getZonedDateTime(photoLink.tourStartTime);
            }

            cell.setText(zonedDateTime.format(TimeTools.Formatter_Date_S));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour start time
    */
   private void defineColumn_Time_TourStartTime() {

      final ColumnDefinition colDef = TableColumnFactory.TIME_TOUR_START_TIME.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourPhotoLink photoLink = (TourPhotoLink) cell.getElement();
            final long historyTime = photoLink.historyStartTime;

            ZonedDateTime zonedDateTime;

            if (historyTime != Long.MIN_VALUE) {

               // this is a history tour

               zonedDateTime = TimeTools.getZonedDateTime(historyTime);

            } else {

               // this is a real tour

               final ZonedDateTime tourStartDateTime_WithZoneID = photoLink.getTourStartDateTime_WithZoneID();

               zonedDateTime = tourStartDateTime_WithZoneID != null

                     ? tourStartDateTime_WithZoneID
                     : TimeTools.getZonedDateTime(photoLink.tourStartTime);
            }

            cell.setText(zonedDateTime.format(TimeTools.Formatter_Time_M));

            setBgColor(cell, photoLink);
         }
      });
   }

   /**
    * column: tour type image
    */
   private void defineColumn_Tour_TypeImage() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE.createColumn(_columnManager, _pc);
      colDef.setIsDefaultColumn();
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TourPhotoLink) {

               final TourPhotoLink photoLink = (TourPhotoLink) element;

               if (photoLink.isHistoryTour) {

                  cell.setImage(net.tourbook.ui.UI.IMAGE_REGISTRY.get(IMAGE_PHOTO_PHOTO));

               } else {

                  final long tourTypeId = photoLink.tourTypeId;
                  if (tourTypeId == -1) {

                     cell.setImage(null);

                  } else {

                     final Image tourTypeImage = TourTypeImage.getTourTypeImage(tourTypeId);

                     /*
                      * when a tour type image is modified, it will keep the same image resource
                      * only the content is modified but in the rawDataView the modified image is
                      * not displayed compared with the tourBookView which displays the correct
                      * image
                      */
                     cell.setImage(tourTypeImage);
                  }
               }
            }
         }
      });
   }

   /**
    * column: tour type text
    */
   private void defineColumn_Tour_TypeText() {

      final ColumnDefinition colDef = TableColumnFactory.TOUR_TYPE_TEXT.createColumn(_columnManager, _pc);
      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            if (element instanceof TourPhotoLink) {

               final TourPhotoLink photoLink = (TourPhotoLink) element;
               if (photoLink.isHistoryTour) {

                  cell.setText(Messages.Photos_AndTours_Label_HistoryTour);

               } else {

                  final long tourTypeId = photoLink.tourTypeId;
                  if (tourTypeId == -1) {
                     cell.setText(UI.EMPTY_STRING);
                  } else {
                     cell.setText(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
                  }
               }

               setBgColor(cell, photoLink);
            }
         }
      });
   }

   @Override
   public void dispose() {

      final IWorkbenchPage page = getViewSite().getPage();

      page.removePostSelectionListener(_postSelectionListener);
      page.removePartListener(_partListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   @SuppressWarnings("unused")
   private void dumpPhotos() {

      System.out.println();
      System.out.println();
      System.out.println();

      for (final Photo photo : _allPhotos) {

//         final LocalDateTime exifDateTime = photo.getExifDateTime();
         final LocalDateTime exifDateTime = photo.getOriginalDateTime();

         final long imageExifTime = photo.imageExifTime;

         final long adjustedTime_Camera = photo.adjustedTime_Camera;

         final ZonedDateTime zonedAdjustedTime = TimeTools.getZonedDateTime(adjustedTime_Camera);
         final ZonedDateTime zonedImageExif = TimeTools.getZonedDateTime(imageExifTime);

         final int month = exifDateTime.getMonthValue();
         final int day = exifDateTime.getDayOfMonth();
         final int hour = exifDateTime.getHour();

         if (true

               && month == 9
               && day == 27
               && hour < 12

         ) {

            System.out.println("%s   %4d.%2d.%2d   %2d:%2d:%2d    %d   %s   %d   %s".formatted( //$NON-NLS-1$

                  photo.imageFileName,

                  exifDateTime.getYear(),
                  month,
                  day,

                  hour,
                  exifDateTime.getMinute(),
                  exifDateTime.getSecond(),

                  adjustedTime_Camera,
                  TimeTools.Formatter_Time_M.format(zonedAdjustedTime),

                  imageExifTime,
                  TimeTools.Formatter_Time_M.format(zonedImageExif)

            ));
         }
      }
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isPhotoAvailable            = _allPhotos.size() > 0;
      final boolean isOneHistoryFilter          = _actionFilterOneHistory.isChecked();
      final boolean isNoHistoryFilter           = !isOneHistoryFilter;
      final boolean isPhotoWithRealTour         = isPhotoAvailable && isNoHistoryFilter;
      final boolean isOneLinkSelected           = _allSelectedPhotoLinks.size() == 1;
      final boolean isSelectedOneRealTour       = isOneLinkSelected && _allSelectedPhotoLinks.get(0).isHistoryTour() == false;
      final boolean isSelectedOneHistoryTour    = isOneLinkSelected && _allSelectedPhotoLinks.get(0).isHistoryTour() ;

      boolean canSelectTime               = getSelectedTimeAdjustmentType().equals(TimeAdjustmentType.SELECT_AJUSTMENT);

      canSelectTime = true;

      _comboAdjustTime                    .setEnabled(isPhotoWithRealTour);
      _comboCamera                        .setEnabled(isPhotoWithRealTour && canSelectTime);

      _lblAdjustTime                      .setEnabled(isPhotoWithRealTour);

      _spinnerHours                       .setEnabled(isPhotoWithRealTour && canSelectTime);
      _spinnerMinutes                     .setEnabled(isPhotoWithRealTour && canSelectTime);
      _spinnerSeconds                     .setEnabled(isPhotoWithRealTour && canSelectTime);

      _actionCreatePhotoTour              .setEnabled(isSelectedOneHistoryTour);
      _actionEditTour                     .setEnabled(isPhotoWithRealTour && isSelectedOneRealTour);
      _actionFilterTourWithPhotos         .setEnabled(isPhotoWithRealTour && _isShowToursWithoutSavedPhotos == false);
      _actionFilterTourWithoutSavedPhotos .setEnabled(isPhotoWithRealTour);
      _actionFilterOneHistory             .setEnabled(isPhotoAvailable);
      _actionSavePhotoInTour              .setEnabled(isPhotoAvailable);
      _actionSetToSavedAdjustment         .setEnabled(isPhotoWithRealTour && isSelectedOneRealTour && canSelectTime);

// SET_FORMATTING_ON
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(_actionSavePhotoInTour);

      menuMgr.add(new Separator());

      menuMgr.add(_actionEditTour);
      menuMgr.add(_actionCreatePhotoTour);
   }

   private void fillToolbar() {

      /*
       * fill view toolbar
       */
      final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

      tbm.add(_actionFilterTourWithPhotos);
      tbm.add(_actionFilterTourWithoutSavedPhotos);
      tbm.add(_actionFilterOneHistory);
      tbm.add(new Separator());
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   private String getPhotoFilePath(final TourData tourData) {

      final Set<TourPhoto> allTourPhotos = tourData.getTourPhotos();

      if (allTourPhotos.size() == 0) {
         return null;
      }

      final TourPhoto[] allPhotos = allTourPhotos.toArray(new TourPhoto[allTourPhotos.size()]);

      return allPhotos[0].getImageFilePath();
   }

   private Camera getSelectedCamera() {

      final int selectedIndex = _comboCamera.getSelectionIndex();
      if (selectedIndex == -1) {
         return null;
      }

      return _allTourCamerasSorted[selectedIndex];
   }

   private TimeAdjustmentType getSelectedTimeAdjustmentType() {

      int selectedIndex = _comboAdjustTime.getSelectionIndex();

      if (selectedIndex == -1) {
         selectedIndex = 0;
      }

      return (TimeAdjustmentType) ALL_TIME_ADJUSTMENT_TYPES[selectedIndex].value;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> allSelectedTours = new ArrayList<>();

      final boolean isOneLinkSelected = _allSelectedPhotoLinks.size() == 1;
      final boolean isSelectedOneRealTour = isOneLinkSelected && _allSelectedPhotoLinks.get(0).isHistoryTour() == false;

      if (isSelectedOneRealTour) {

         final long tourId = _allSelectedPhotoLinks.get(0).tourId;
         final TourData tourData = TourManager.getInstance().getTourData(tourId);

         allSelectedTours.add(tourData);
      }

      return allSelectedTours;
   }

   private int getTimeAdjustmentTypeIndex(final Enum<TimeAdjustmentType> timeAdjustmentType) {

      if (timeAdjustmentType == null) {

         // this case should not happen
         return -1;
      }

      for (int itemIndex = 0; itemIndex < ALL_TIME_ADJUSTMENT_TYPES.length; itemIndex++) {

         final ComboEnumEntry<?> enumItem = ALL_TIME_ADJUSTMENT_TYPES[itemIndex];

         if (enumItem.value.equals(timeAdjustmentType)) {
            return itemIndex;
         }
      }

      return -1;
   }

   @Override
   public ColumnViewer getViewer() {
      return _tourViewer;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onSelectTimeAdjustment());

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onSelectTimeAdjustment();
      };
   }

   private void onPartActivate() {

      // fire selection
      if (_tourPhotoLinkSelection != null) {
         PhotoManager.firePhotoEvent(this, PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
      }
   }

   private void onSelectCamera() {

      final Camera camera = getSelectedCamera();
      if (camera == null) {
         return;
      }

      // update UI
      updateUI_TimeAdjustment(camera.getTimeAdjustment() / 1000);
   }

   private void onSelectionChanged(final ISelection selection, final IWorkbenchPart part) {

      if (selection instanceof SyncSelection) {

         final ISelection originalSelection = ((SyncSelection) selection).getSelection();

         if (originalSelection instanceof PhotoSelection) {
            showPhotosAndTours(((PhotoSelection) originalSelection).galleryPhotos, false);
         }

      } else if (selection instanceof PhotoSelection && part instanceof PicDirView) {

         /**
          * Accept photo selection ONLY from the pic dir view, otherwise other photo selections will
          * cause a view update
          */

         final PhotoSelection photoSelection = (PhotoSelection) selection;

         final Command command = _commandService.getCommand(ActionHandler_SyncPhotoWithTour.COMMAND_ID);
         final State state = command.getState(RegistryToggleState.STATE_ID);
         final boolean isSync = (Boolean) state.getValue();

         if (isSync) {
            showPhotosAndTours(photoSelection.galleryPhotos, false);
         }

      } else if (selection instanceof SelectionDeletedTours) {

         clearView();

         _photoMgr.resetTourStartEnd();
      }
   }

   private void onSelectTimeAdjustment() {

      if (_allSelectedPhotoLinks.isEmpty()) {
         // a tour is not selected
         return;
      }

      /*
       * Set time adjustment into the selected camera
       */
      final Camera camera = getSelectedCamera();
      if (camera == null) {
         return;
      }

      camera.setTimeAdjustment(
            _spinnerHours.getSelection(),
            _spinnerMinutes.getSelection(),
            _spinnerSeconds.getSelection());

      updateUI();
   }

   /**
    * Creates a {@link TourPhotoLinkSelection}
    *
    * @param allSelectedLinks
    *           All elements of type {@link TourPhotoLink}
    */
   private void onSelectTourLink(final Object[] allSelectedLinks) {

      // get all real tours with geo positions
      _selectedTourPhotoLinksWithGps.clear();

      // contains tour id's for all real tours
      final ArrayList<Long> allSelectedTourIds = new ArrayList<>();

      final ArrayList<TourPhotoLink> allSelectedPhotoLinks = new ArrayList<>();

      String firstLinkCamera = null;

      for (final Object linkElement : allSelectedLinks) {

         if (linkElement instanceof TourPhotoLink) {

            final TourPhotoLink selectedLink = (TourPhotoLink) linkElement;

            allSelectedPhotoLinks.add(selectedLink);

            final boolean isRealTour = selectedLink.tourId != Long.MIN_VALUE;

            if (isRealTour) {
               allSelectedTourIds.add(selectedLink.tourId);
            }

            if (selectedLink.linkPhotos.size() > 0) {

               final TourData tourData = TourManager.getInstance().getTourData(selectedLink.tourId);

               if (tourData != null && tourData.latitudeSerie != null) {
                  _selectedTourPhotoLinksWithGps.add(selectedLink);
               }

               // get camera which is used to preselect the camera in the view toolbar
               if (firstLinkCamera == null) {

                  final String[] tourCameras = selectedLink.tourCameras.split(UI.COMMA_SPACE);

                  if (tourCameras != null

                        /*
                         * When a tour contains photos from multiple cameras -> do not set a camera
                         * otherwise the adjusted time for a camera can switch to another camera
                         */
                        && tourCameras.length == 1) {

                     firstLinkCamera = tourCameras[0];
                  }
               }
            }
         }
      }

      if (_allSelectedPhotoLinks.equals(allSelectedPhotoLinks)) {

         // currently selected tour is already selected and selection is fired
         return;
      }

      // select camera of the first tour in the combobox
      if (firstLinkCamera != null) {

         for (int cameraIndex = 0; cameraIndex < _allTourCamerasSorted.length; cameraIndex++) {

            final Camera camera = _allTourCamerasSorted[cameraIndex];

            if (firstLinkCamera.equals(camera.cameraName)) {

               _comboCamera.select(cameraIndex);

               // update UI
               onSelectCamera();

               break;
            }
         }
      }

      _allSelectedPhotoLinks.clear();
      _allSelectedPhotoLinks.addAll(allSelectedPhotoLinks);

      enableControls();

      // create tour selection
      _tourPhotoLinkSelection = new TourPhotoLinkSelection(_allSelectedPhotoLinks, allSelectedTourIds);

      PhotoManager.firePhotoEvent(this, PhotoEventId.PHOTO_SELECTION, _tourPhotoLinkSelection);
   }

   private void onTourChanged(final ArrayList<TourData> modifiedTours) {

      final TourManager tourManager = TourManager.getInstance();

      final ArrayList<TourPhotoLink> allModifiedLinks = new ArrayList<>();

      // update viewer data
      for (final TourPhotoLink photoLink : _allVisibleTourPhotoLinks) {

         final long photoLink_TourId = photoLink.tourId;

         for (final TourData modifiedTourData : modifiedTours) {

            if (modifiedTourData.getTourId() == photoLink_TourId) {

               final TourData tourData = tourManager.getTourData(photoLink_TourId);

               if (tourData != null) {

                  photoLink.numTourPhotos = tourData.getTourPhotos().size();
                  photoLink.photoTimeAdjustment = tourData.getPhotoTimeAdjustment();
                  photoLink.photoFilePath = getPhotoFilePath(tourData);

                  allModifiedLinks.add(photoLink);

                  // proceed with the next photo link
                  break;
               }
            }
         }
      }

      // update viewer UI
      if (allModifiedLinks.size() > 0) {

         // update number of photos with/without GPS
         TourPhotoManager.getInstance().setTourGpsIntoPhotos(allModifiedLinks);

         _tourViewer.update(allModifiedLinks.toArray(), null);
      }
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _tourViewer.getTable().dispose();

         createUI_50_TourViewer(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _tourViewer;
   }

   @Override
   public void reloadViewer() {

      _tourViewer.setInput(new Object[0]);
   }

   private void restoreState() {

      // photo filter
      _isShowToursWithPhotos = Util.getStateBoolean(_state, STATE_FILTER_TOUR_WITH_PHOTOS, true);
      _isShowToursWithoutSavedPhotos = Util.getStateBoolean(_state, STATE_FILTER_NOT_SAVED_PHOTOS, false);

      _actionFilterOneHistory.setChecked(_isFilterOneHistoryTour);
      _actionFilterTourWithoutSavedPhotos.setChecked(_isShowToursWithoutSavedPhotos);
      _actionFilterTourWithPhotos.setChecked(_isShowToursWithPhotos);

      // time adjustment type
      final Enum<TimeAdjustmentType> timeAdjustmentType = Util.getStateEnum(_state,
            STATE_SELECTED_TIME_ADJUSTMENT_TYPE,
            TimeAdjustmentType.SAVED_AJUSTMENT);
      _comboAdjustTime.select(getTimeAdjustmentTypeIndex(timeAdjustmentType));

      // camera
      final String prevCameraName = Util.getStateString(_state, STATE_SELECTED_CAMERA_NAME, null);
      updateUI_Cameras(prevCameraName);
   }

   @PersistState
   private void saveState() {

      // check if UI is disposed
      final Table table = _tourViewer.getTable();
      if (table.isDisposed()) {
         return;
      }

      // camera
      final Camera selectedCamera = getSelectedCamera();
      if (selectedCamera != null) {

         final String cameraName = selectedCamera.cameraName;

         if (cameraName != null) {
            _state.put(STATE_SELECTED_CAMERA_NAME, cameraName);
         }
      }

      // photo filter
      _state.put(STATE_FILTER_NOT_SAVED_PHOTOS, _actionFilterTourWithoutSavedPhotos.isChecked());
      _state.put(STATE_FILTER_TOUR_WITH_PHOTOS, _actionFilterTourWithPhotos.isChecked());

      // time adjustment type
      Util.setStateEnum(_state, STATE_SELECTED_TIME_ADJUSTMENT_TYPE, getSelectedTimeAdjustmentType());

      _columnManager.saveState(_state);
   }

   /**
    * @param prevTourPhotoLink
    *           Previously selected link, can be <code>null</code>.
    */
   private void selectTour(final TourPhotoLink prevTourPhotoLink) {

      if (_allVisibleTourPhotoLinks.isEmpty()) {
         return;
      }

      TourPhotoLink selectedTour = null;

      /*
       * 1st try to select a tour
       */
      if (prevTourPhotoLink == null) {

         // select first tour
         selectedTour = _allVisibleTourPhotoLinks.get(0);

      } else if (prevTourPhotoLink.isHistoryTour == false) {

         // select a real tour by tour id
         selectedTour = prevTourPhotoLink;
      }

      ISelection newSelection = null;
      if (selectedTour != null) {
         _tourViewer.setSelection(new StructuredSelection(selectedTour), true);
         newSelection = _tourViewer.getSelection();
      }

      if (prevTourPhotoLink == null) {
         // there is nothing which can be compared in equals()
         return;
      }

      /*
       * 2nd try to select a tour
       */
      // check if tour is selected
      if (newSelection == null || newSelection.isEmpty()) {

         TourPhotoLink linkSelection = null;

         final ArrayList<Photo> tourPhotos = prevTourPhotoLink.linkPhotos;
         if (tourPhotos.size() > 0) {

            // get tour for the first photo

            final long tourPhotoTime = tourPhotos.get(0).adjustedTime_Camera;

            for (final TourPhotoLink link : _allVisibleTourPhotoLinks) {

               final long linkStartTime = link.isHistoryTour
                     ? link.historyStartTime
                     : link.tourStartTime;

               final long linkEndTime = link.isHistoryTour
                     ? link.historyEndTime
                     : link.tourEndTime;

               if (tourPhotoTime >= linkStartTime && tourPhotoTime <= linkEndTime) {
                  linkSelection = link;
                  break;
               }
            }

         } else {

            // get tour by checking intersection

            final long requestedStartTime = prevTourPhotoLink.isHistoryTour
                  ? prevTourPhotoLink.historyStartTime
                  : prevTourPhotoLink.tourStartTime;

            final long requestedEndTime = prevTourPhotoLink.isHistoryTour
                  ? prevTourPhotoLink.historyEndTime
                  : prevTourPhotoLink.tourEndTime;

            final long requestedTime = requestedStartTime + ((requestedEndTime - requestedStartTime) / 2);

            for (final TourPhotoLink link : _allVisibleTourPhotoLinks) {

               final long linkStartTime = link.isHistoryTour
                     ? link.historyStartTime
                     : link.tourStartTime;

               final long linkEndTime = link.isHistoryTour
                     ? link.historyEndTime
                     : link.tourEndTime;

               final boolean isIntersects = requestedTime > linkStartTime && requestedTime < linkEndTime;

               if (isIntersects) {
                  linkSelection = link;
                  break;
               }
            }
         }

         if (linkSelection != null) {

            _tourViewer.setSelection(new StructuredSelection(linkSelection), false);
            newSelection = _tourViewer.getSelection();
         }
      }

      /*
       * 3rd try to select a tour
       */
      if (newSelection == null || newSelection.isEmpty()) {

         // previous selections failed, select first tour
         final TourPhotoLink firstTour = _allVisibleTourPhotoLinks.get(0);

         _tourViewer.setSelection(new StructuredSelection(firstTour), true);
      }

      // set focus rubberband to selected item, most of the time it is not at the correct position
      final Table table = _tourViewer.getTable();
      table.setSelection(table.getSelectionIndex());
   }

   /**
    * @param tourData
    * @param allLinkPhotos
    * @param allPhotos
    *           Out: Contains all photos which are "touched"
    */
   private void setAllPhotosInTour(final TourData tourData,
                                   final ArrayList<Photo> allLinkPhotos,
                                   final HashSet<Photo> allPhotos) {

      final HashMap<String, TourPhoto> allOldTourPhotos = new HashMap<>();
      final Set<TourPhoto> allExistingTourPhotos = tourData.getTourPhotos();

      for (final TourPhoto tourPhoto : allExistingTourPhotos) {
         allOldTourPhotos.put(tourPhoto.getImageFilePathName(), tourPhoto);
      }

      // keep existing photos
      final ArrayList<Photo> allOldGalleryPhotos = tourData.getGalleryPhotos();
      if (allOldGalleryPhotos != null) {
         allPhotos.addAll(allOldGalleryPhotos);
      }

      final HashSet<TourPhoto> allNewTourPhotos = new HashSet<>();

      for (final Photo linkPhoto : allLinkPhotos) {

         // get existing tour photo
         TourPhoto tourPhoto = allOldTourPhotos.get(linkPhoto.imageFilePathName);

         if (tourPhoto == null) {

            // gallery photo is not in tour -> create new tour photo

            tourPhoto = new TourPhoto(tourData, linkPhoto);
         }

         final double linkLatitude = linkPhoto.getLinkLatitude();
         final double linkLongitude = linkPhoto.getLinkLongitude();

         // set adjusted time / geo location
         tourPhoto.setAdjustedTime(linkPhoto.adjustedTime_Camera);
         tourPhoto.setGeoLocation(linkLatitude, linkLongitude);

         allNewTourPhotos.add(tourPhoto);

         // add new/old photos
         allPhotos.add(linkPhoto);
      }

      tourData.setTourPhotos(allNewTourPhotos, allLinkPhotos);
   }

   private void setBgColor(final ViewerCell cell, final TourPhotoLink linkTour) {

//		if (linkTour.isHistoryTour()) {
//			cell.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
//		} else {
//			cell.setBackground(JFaceResources.getColorRegistry().get(net.tourbook.ui.UI.VIEW_COLOR_BG_HISTORY_TOUR));
//		}
   }

   @Override
   public void setFocus() {
      _tourViewer.getTable().setFocus();
   }

   private void setPhotoTimeAdjustment() {

      for (final Photo photo : _allPhotos) {

         final long exifTime = photo.imageExifTime;
         final long cameraTimeAdjustment = photo.camera.getTimeAdjustment();

         photo.adjustedTime_Camera = exifTime + cameraTimeAdjustment;

         // force that the position is updated
         photo.resetLinkWorldPosition();
      }

      // sort photos by time
      Collections.sort(_allPhotos, TourPhotoManager.AdjustTimeComparator_Link);

//    dumpPhotos();
   }

   /**
    * Entry point in this view to show tours for the provided photos
    *
    * @param allPhotos
    * @param isSelectAllPhotos
    */
   void showPhotosAndTours(final ArrayList<Photo> allPhotos, final boolean isSelectAllPhotos) {

      _allPrevPhotos = allPhotos;

      if (allPhotos == null) {
         return;
      }

      final int numPhotos = allPhotos.size();

      if (numPhotos == 0) {
         clearView();
         return;
      }

      _allPhotos.clear();
      _allPhotos.addAll(allPhotos);

      _allTourCameras.clear();

      // ensure camera is set in all photos
      for (final Photo photo : _allPhotos) {
         if (photo.camera == null) {
            _photoMgr.setCamera(photo, _allTourCameras);
         }
      }

      if (numPhotos > 100) {

         BusyIndicator.showWhile(_pageBook.getDisplay(), new Runnable() {
            @Override
            public void run() {
               updateUI(null, _allVisibleTourPhotoLinks, isSelectAllPhotos);
            }
         });

      } else {

         updateUI(null, _allVisibleTourPhotoLinks, isSelectAllPhotos);
      }
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Update UI with default settings
    */
   private void updateUI() {

      updateUI(_allSelectedPhotoLinks, null, false);
   }

   private void updateUI(final ArrayList<TourPhotoLink> tourPhotoLinksWhichShouldBeSelected,
                         final ArrayList<TourPhotoLink> allLinksWhichShouldBeSelected,
                         final boolean isSelectAllPhotos) {

      if (_allPhotos.isEmpty()) {

         // view is not fully initialized, this happend in the pref listener
         return;
      }

      // get previous selected tour
      final TourPhotoLink prevTourPhotoLink[] = { null };
      if (tourPhotoLinksWhichShouldBeSelected != null && tourPhotoLinksWhichShouldBeSelected.size() > 0) {
         prevTourPhotoLink[0] = tourPhotoLinksWhichShouldBeSelected.get(0);
      }

      // this must be called BEFORE start/end date are set
      setPhotoTimeAdjustment();

      _allVisibleTourPhotoLinks.clear();
      _allSelectedPhotoLinks.clear();
      _selectedTourPhotoLinksWithGps.clear();

      if (_isFilterOneHistoryTour) {

         _photoMgr.createTourPhotoLinks_OneHistoryTour(

               _allPhotos,
               _allVisibleTourPhotoLinks,
               _allTourCameras);
      } else {

         _photoMgr.createTourPhotoLinks(

               _allPhotos,
               _allVisibleTourPhotoLinks,
               _allTourCameras,
               _isShowToursWithPhotos,
               _isShowToursWithoutSavedPhotos,
               getSelectedTimeAdjustmentType());
      }

      updateUI_Cameras(null);

      enableControls();

      // tour viewer update can be a longer task, update other UI element before
      _pageBook.getDisplay().asyncExec(() -> {

         if (_tourViewer.getTable().isDisposed()) {
            return;
         }

         _tourViewer.setInput(new Object[0]);
         _pageBook.showPage(_pageViewer);

         // update annotations in PicDirView
         PhotoManager.updatePicDirGallery();

         if (isSelectAllPhotos == false) {

            /*
             * Prevent that all tour photo links are selected -> this can cause performance and
             * other issues when the user thinks that not all tour photo links are selected, in the
             * dark theme mode this selection is not easy visible
             */

            if (allLinksWhichShouldBeSelected != null && allLinksWhichShouldBeSelected.size() > 0) {

               _tourViewer.setSelection(new StructuredSelection(allLinksWhichShouldBeSelected), true);

            } else {

               selectTour(prevTourPhotoLink[0]);
            }
         }
      });
   }

   /**
    * fill camera combo and select previous selection
    *
    * @param defaultCameraName
    */
   private void updateUI_Cameras(final String defaultCameraName) {

      // get previous camera
      String currentSelectedCameraName = null;
      if (defaultCameraName == null) {

         final int currentSelectedCameraIndex = _comboCamera.getSelectionIndex();
         if (currentSelectedCameraIndex != -1) {
            currentSelectedCameraName = _comboCamera.getItem(currentSelectedCameraIndex);
         }

      } else {
         currentSelectedCameraName = defaultCameraName;
      }

      _comboCamera.removeAll();

      // sort cameras
      final Collection<Camera> cameraValues = _allTourCameras.values();
      _allTourCamerasSorted = cameraValues.toArray(new Camera[cameraValues.size()]);
      Arrays.sort(_allTourCamerasSorted);

      int cameraComboIndex = -1;

      for (int cameraIndex = 0; cameraIndex < _allTourCamerasSorted.length; cameraIndex++) {

         final Camera camera = _allTourCamerasSorted[cameraIndex];
         _comboCamera.add(camera.cameraName);

         // get index for the last selected camera
         if (cameraComboIndex == -1
               && currentSelectedCameraName != null
               && currentSelectedCameraName.equals(camera.cameraName)) {
            cameraComboIndex = cameraIndex;
         }
      }

      _comboCamera.getParent().layout();

      // select previous camera
      _comboCamera.select(cameraComboIndex == -1 ? 0 : cameraComboIndex);

      // update spinners for camera time adjustment
      onSelectCamera();
   }

   /**
    * @param timeAdjustment
    *           Time adjustment in seconds
    */
   private void updateUI_TimeAdjustment(final long timeAdjustment) {

      final int hours = (int) (timeAdjustment / 3600);
      final int minutes = (int) ((timeAdjustment % 3600) / 60);
      final int seconds = (int) ((timeAdjustment % 3600) % 60);

      _spinnerHours.setSelection(hours);
      _spinnerMinutes.setSelection(minutes);
      _spinnerSeconds.setSelection(seconds);
   }
}
