/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.photo.IPhotoEventListener;
import net.tourbook.photo.IPhotoGalleryProvider;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.Photo;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.PhotoEventId;
import net.tourbook.photo.PhotoGallery;
import net.tourbook.photo.PhotoManager;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.photo.internal.gallery.MT20.GalleryMT20Item;
import net.tourbook.photo.internal.preferences.PrefPagePhotoExternalApp;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.Path;
import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

public class TourPhotosView extends ViewPart implements IPhotoEventListener {

   public static final String             ID                              = "net.tourbook.photo.TourPhotosView.ID"; //$NON-NLS-1$

   private static final String            STATE_PHOTO_GALLERY_IS_VERTICAL = "STATE_PHOTO_GALLERY_IS_VERTICAL";      //$NON-NLS-1$

   private static final String            EXTERNAL_APP_ACTION             = "&%d   %s";                             //$NON-NLS-1$

   private static final IDialogSettings   _state                          = TourbookPlugin.getState(ID);
   private static final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private static final IPreferenceStore  _prefStore_Photo                = PhotoActivator.getPrefStore();

   private PostSelectionProvider          _postSelectionProvider;

   private ISelectionListener             _postSelectionListener;
   private IPropertyChangeListener        _prefChangeListener;
   private ITourEventListener             _tourEventListener;
   private IPartListener2                 _partListener;

   private boolean                        _isPartVisible;

   private ActionAddPhoto                 _actionAddPhoto;
   private ActionRemovePhoto              _actionRemovePhoto;
   private ActionToggleGalleryOrientation _actionToggleGalleryOrientation;
   private ActionRunExternalApp           _actionRunExternalApp1;
   private ActionRunExternalApp           _actionRunExternalApp2;
   private ActionRunExternalApp           _actionRunExternalApp3;
   private ActionRunExternalAppPrefPage   _actionRunExternalAppPrefPage;
   private ActionRunExternalAppTitle      _actionRunExternalAppTitle;

   /**
    * Contains selection which was set when the part is hidden
    */
   private TourPhotoLinkSelection         _selectionWhenHidden;

   private TourPhotoGallery               _photoGallery;

   private boolean                        _isVerticalGallery;
   public IToolBarManager                 _galleryToolbarManager;

   private int                            _galleryPositionKey;

   private long                           _photoStartTime;
   private long                           _photoEndTime;

   private boolean                        _isLinkPhotoDisplayed;

   /*
    * UI controls
    */
   private ToolBar _rightToolbar;
   private Label   _labelTitle;

   public class ActionRunExternalApp extends Action {

      public ActionRunExternalApp() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         actionExternalApp_Run(this);
      }
   }

   public class ActionRunExternalAppPrefPage extends Action {

      public ActionRunExternalAppPrefPage() {

         super(OtherMessages.MAP_ACTION_EXTERNAL_APP_SETUP, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         actionExternalApp_PrefPage();
      }
   }

   private class ActionRunExternalAppTitle extends Action {

      public ActionRunExternalAppTitle() {

         super(OtherMessages.MAP_ACTION_EXTERNAL_APP_OPEN_PHOTO_IMAGE, AS_PUSH_BUTTON);

         setEnabled(false);
      }
   }

   private class ActionToggleGalleryOrientation extends Action {

      public ActionToggleGalleryOrientation() {

         super(null, Action.AS_PUSH_BUTTON);

         /**
          * VERY IMPORTANT
          * <p>
          * an image must be set in the constructor, otherwise the button is small when only ONE
          * action is in the toolbar
          */
         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.PhotoGallery_Horizontal));
      }

      @Override
      public void run() {
         actionToggleVerticalHorizontal();
      }
   }

   private final class PhotoGalleryProvider implements IPhotoGalleryProvider {

      @Override
      public IStatusLineManager getStatusLineManager() {
         return getViewSite().getActionBars().getStatusLineManager();
      }

      @Override
      public IToolBarManager getToolBarManager() {
         return getViewSite().getActionBars().getToolBarManager();
      }

      @Override
      public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

      @Override
      public void setSelection(final PhotoSelection photoSelection) {
         _postSelectionProvider.setSelection(photoSelection);
      }
   }

   private class TourPhotoGallery extends PhotoGallery {

      public TourPhotoGallery(final IDialogSettings state) {
         super(state);
      }

      @Override
      public void fillContextMenu(final IMenuManager menuMgr) {

         super.fillContextMenu(menuMgr);

         TourPhotosView.this.fillContextMenu(menuMgr);
      }
   }

   public TourPhotosView() {
      super();
   }

   private void actionExternalApp_PrefPage() {

      PreferencesUtil.createPreferenceDialogOn(
            Display.getCurrent().getActiveShell(),
            PrefPagePhotoExternalApp.ID,
            null,
            null)

            .open();
   }

   private void actionExternalApp_Run(final ActionRunExternalApp actionRunExternalApp) {

      final Photo photo = getSelectedPhoto();
      if (photo == null) {
         return;
      }

      /*
       * Run external app
       */
      String extApp = null;

      if (actionRunExternalApp == _actionRunExternalApp1) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_1).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp2) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_2).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp3) {
         extApp = _prefStore_Photo.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_3).trim();
      }

      final String imageFilePath = photo.imageFilePathName;

      String commands[] = null;
      if (UI.IS_WIN) {

         final String[] commandsWin = {

               UI.SYMBOL_QUOTATION_MARK + extApp + UI.SYMBOL_QUOTATION_MARK,
               UI.SYMBOL_QUOTATION_MARK + imageFilePath + UI.SYMBOL_QUOTATION_MARK
         };

         commands = commandsWin;

      } else if (UI.IS_OSX) {

         final String[] commandsOSX = { "/usr/bin/open", "-a", // //$NON-NLS-1$ //$NON-NLS-2$
               extApp,
               imageFilePath };

         commands = commandsOSX;

      } else if (UI.IS_LINUX) {

         final String[] commandsLinux = { extApp, imageFilePath };

         commands = commandsLinux;
      }

      if (commands != null) {

         try {

            // log command
            final StringBuilder sb = new StringBuilder();
            for (final String cmd : commands) {
               sb.append(cmd + UI.SPACE1);
            }

            StatusUtil.logInfo(sb.toString());

            Runtime.getRuntime().exec(commands);

         } catch (final Exception e) {

            StatusUtil.showStatus(e);
         }
      }
   }

   private void actionToggleVerticalHorizontal() {

      // keep state for current orientation
      _photoGallery.saveState();

      // toggle gallery
      _isVerticalGallery = !_isVerticalGallery;

      updateUI_ToogleAction();

      _photoGallery.setVertical(_isVerticalGallery);
   }

   private void addPartListener() {

      _partListener = new IPartListener2() {
         @Override
         public void partActivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

         @Override
         public void partClosed(final IWorkbenchPartReference partRef) {}

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == TourPhotosView.this) {
               _isPartVisible = false;
            }
         }

         @Override
         public void partInputChanged(final IWorkbenchPartReference partRef) {}

         @Override
         public void partOpened(final IWorkbenchPartReference partRef) {}

         @Override
         public void partVisible(final IWorkbenchPartReference partRef) {
            if (partRef.getPart(false) == TourPhotosView.this) {

               _isPartVisible = true;

               if (_selectionWhenHidden != null) {

                  onSelectionChanged(_selectionWhenHidden);

                  _selectionWhenHidden = null;
               }
            }
         }
      };
      getViewSite().getPage().addPartListener(_partListener);
   }

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            final String property = event.getProperty();

            if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)) {

               updateColors(false);
            }
         }
      };
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * listen for events when a tour is selected
    */
   private void addSelectionListener() {

      _postSelectionListener = new ISelectionListener() {
         @Override
         public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
            if (part == TourPhotosView.this) {
               return;
            }
            onSelectionChanged(selection);
         }
      };
      getViewSite().getPage().addPostSelectionListener(_postSelectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = new ITourEventListener() {
         @Override
         public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

            if (part == TourPhotosView.this) {
               return;
            }

            if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

               clearView();

               final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
               if (modifiedTours != null) {

                  // show modified tour

                  final ArrayList<Long> allTourIds = new ArrayList<>();

                  for (final TourData tourData : modifiedTours) {
                     allTourIds.add(tourData.getTourId());
                  }

                  onSelectionChanged(new SelectionTourIds(allTourIds));
               }

            } else if (eventId == TourEventId.UPDATE_UI) {

               // ensure that not the wrong data are displayed
               clearView();
//               showTourPhotos_FromDefaultSelection();

            } else if (eventId == TourEventId.MARKER_SELECTION && eventData instanceof SelectionTourMarker) {

               onSelectionChanged((SelectionTourMarker) eventData);

            } else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

               onSelectionChanged((ISelection) eventData);

            } else if (eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

               clearView();
            }
         }
      };

      TourManager.getInstance().addTourEventListener(_tourEventListener);
   }

   /**
    * Add photos from a tour.
    *
    * @param allPhotos
    * @param tourData
    *
    * @return
    */
   private int addTourPhotos(final ArrayList<Photo> allPhotos, final TourData tourData) {

      if (tourData == null) {
         return 0;
      }

      final ArrayList<Photo> galleryPhotos = tourData.getGalleryPhotos();

      if (galleryPhotos == null) {
         return 0;
      }

      allPhotos.addAll(galleryPhotos);

      _galleryPositionKey += galleryPhotos.hashCode();

      final int gallerySize = galleryPhotos.size();
      if (gallerySize > 0) {

         final long tourStartTime = galleryPhotos.get(0).adjustedTime_Tour;
         final long tourEndTime = galleryPhotos.get(gallerySize - 1).adjustedTime_Tour;

         if (tourStartTime < _photoStartTime) {
            _photoStartTime = tourStartTime;
         }
         if (tourEndTime > _photoEndTime) {
            _photoEndTime = tourEndTime;
         }
      }

      return galleryPhotos.size();
   }

   private void clearView() {

      // removed old tour data from the selection provider
      _postSelectionProvider.clearSelection();
   }

   private void createActions() {

// SET_FORMATTING_OFF

      _actionAddPhoto                  = new ActionAddPhoto(_photoGallery);
      _actionRemovePhoto               = new ActionRemovePhoto(_photoGallery);
      _actionToggleGalleryOrientation  = new ActionToggleGalleryOrientation();

      _actionRunExternalAppTitle       = new ActionRunExternalAppTitle();
      _actionRunExternalAppPrefPage    = new ActionRunExternalAppPrefPage();
      _actionRunExternalApp1           = new ActionRunExternalApp();
      _actionRunExternalApp2           = new ActionRunExternalApp();
      _actionRunExternalApp3           = new ActionRunExternalApp();

// SET_FORMATTING_ON
   }

   @Override
   public void createPartControl(final Composite parent) {

      createUI(parent);

      createActions();
      fillActionBar();

      addSelectionListener();
      addTourEventListener();
      addPrefListener();
      addPartListener();
      PhotoManager.addPhotoEventListener(this);

      restoreState();

      // this part is a selection provider
      getSite().setSelectionProvider(_postSelectionProvider = new PostSelectionProvider(ID));

      showTourPhotos_FromDefaultSelection();
   }

   private void createUI(final Composite parent) {

      createUI_10_Gallery(parent);
      createUI_20_ActionBar(_photoGallery.getCustomActionBarContainer());

      // must be called after the custom action bar is created
      _photoGallery.createActionBar();
   }

   private void createUI_10_Gallery(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         _photoGallery = new TourPhotoGallery(_state);

         _photoGallery.setShowCustomActionBar();
         _photoGallery.setShowThumbnailSize();

         _photoGallery.createPhotoGallery(
               container,
               SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI,
               new PhotoGalleryProvider());

         _photoGallery.setDefaultStatusMessage(Messages.Photo_Gallery_Label_NoTourWithPhoto);
      }
   }

   private void createUI_20_ActionBar(final Composite parent) {

      GridLayoutFactory.fillDefaults().applyTo(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         /*
          * label: title
          */
         _labelTitle = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_labelTitle);

         /*
          * Create toolbar for the buttons on the right side
          */
         _rightToolbar = new ToolBar(container, SWT.FLAT);
         GridDataFactory.fillDefaults()
               .align(SWT.FILL, SWT.CENTER)
               .applyTo(_rightToolbar);
      }
   }

   @Override
   public void dispose() {

      final IWorkbenchPage page = getViewSite().getPage();

      page.removePostSelectionListener(_postSelectionListener);
      page.removePartListener(_partListener);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      PhotoManager.removePhotoEventListener(this);

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableActions() {

      final Collection<GalleryMT20Item> selectedPhotos = _photoGallery.getGallerySelection();

// SET_FORMATTING_OFF

      final int numSelectedPhotos = selectedPhotos.size();
      final boolean isPhotoSelected = numSelectedPhotos > 0;

      _actionRemovePhoto            .setEnabled(isPhotoSelected);
      _actionAddPhoto               .setEnabled(isPhotoSelected && _isLinkPhotoDisplayed);
      _actionRunExternalAppTitle    .setEnabled(false);

// SET_FORMATTING_ON
   }

   private void fillActionBar() {

      /*
       * Fill gallery toolbar
       */
      _galleryToolbarManager = new ToolBarManager(_rightToolbar);

      _galleryToolbarManager.add(_actionToggleGalleryOrientation);

      _galleryToolbarManager.update(true);
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      if (_isLinkPhotoDisplayed) {
         menuMgr.add(_actionAddPhoto);
      }

      menuMgr.add(_actionRemovePhoto);

      menuMgr.add(new Separator());

      fillExternalApp(menuMgr);

      enableActions();
   }

   private void fillExternalApp(final IMenuManager menuMgr) {

      final Photo photo = getSelectedPhoto();

      if (photo == null) {
         return;
      }

      // set image file name into the external app title/tooltip
      _actionRunExternalAppTitle.setText(OtherMessages.MAP_ACTION_EXTERNAL_APP_OPEN_PHOTO_IMAGE.formatted(photo.imageFileName));
      _actionRunExternalAppTitle.setToolTipText(photo.imageFilePathName);

      menuMgr.add(_actionRunExternalAppTitle);

      fillExternalApp_One(1, _actionRunExternalApp1, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_1);
      fillExternalApp_One(2, _actionRunExternalApp2, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_2);
      fillExternalApp_One(3, _actionRunExternalApp3, menuMgr, IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FILE_VIEWER_3);

      menuMgr.add(_actionRunExternalAppPrefPage);
   }

   private void fillExternalApp_One(final int appIndex,
                                    final ActionRunExternalApp appAction,
                                    final IMenuManager menuMgr,
                                    final String prefStoreName) {

      final String extAppFilePath = _prefStore_Photo.getString(prefStoreName).trim();

      if (extAppFilePath.length() > 0) {

         appAction.setText(EXTERNAL_APP_ACTION.formatted(appIndex, new Path(extAppFilePath).lastSegment()));

         // set tooltip text
         if (appIndex == 1) {

            appAction.setToolTipText(OtherMessages.MAP_ACTION_EXTERNAL_APP_DOUBLE_CLICK_START.formatted(extAppFilePath));

         } else {

            appAction.setToolTipText(extAppFilePath);
         }

         menuMgr.add(appAction);
      }
   }

   private Photo getSelectedPhoto() {

      final Collection<GalleryMT20Item> selectedPhotos = _photoGallery.getGallerySelection();
      final Optional<GalleryMT20Item> galleryItem = selectedPhotos.stream().findFirst();

      if (galleryItem.isPresent()) {

         return galleryItem.get().photo;
      }

      return null;
   }

   private void onSelectionChanged(final ISelection selection) {

      if (_actionAddPhoto.isInModifyTour() || _actionRemovePhoto.isInModifyTour()) {

         // prevent that the saved tour is displayed instead of the tour photo link

         // update UI otherwise it is only updated e.g. when the mouse is hovering the photos
         _photoGallery.refreshUI();

         return;
      }

      final ArrayList<Photo> allPhotos = new ArrayList<>();

      _galleryPositionKey = 0;
      _photoStartTime = Long.MAX_VALUE;
      _photoEndTime = Long.MIN_VALUE;
      _isLinkPhotoDisplayed = false;

      if (selection instanceof TourPhotoLinkSelection) {

         _isLinkPhotoDisplayed = true;

         final TourPhotoLinkSelection tourPhotoSelection = (TourPhotoLinkSelection) selection;

         final ArrayList<TourPhotoLink> photoLinks = tourPhotoSelection.tourPhotoLinks;
         final HashSet<Long> allTourIds = new HashSet<>();
         int numHistoryTour = 0;

         for (final TourPhotoLink photoLink : photoLinks) {

            final ArrayList<Photo> linkPhotos = photoLink.linkPhotos;

            allPhotos.addAll(linkPhotos);

            _galleryPositionKey += photoLink.linkId;

            final long tourId = photoLink.tourId;
            if (tourId == Long.MIN_VALUE) {
               numHistoryTour++;
            } else {
               allTourIds.add(tourId);
            }

            // temporarily keep tour id in the photo, this is used when saving photo in tour
            for (final Photo linkPhoto : linkPhotos) {
               linkPhoto.setLinkTourId(tourId);
            }

            final long tourStartTime = photoLink.tourStartTime;
            final long tourEndTime = photoLink.tourEndTime;

            if (tourStartTime < _photoStartTime) {
               _photoStartTime = tourStartTime;
            }
            if (tourEndTime > _photoEndTime) {
               _photoEndTime = tourEndTime;
            }
         }

         showTourPhotos_FromCurrentSelection(allPhotos, allTourIds.size() + numHistoryTour);

      } else if (selection instanceof PhotoSelection) {

         final PhotoSelection photoSelection = (PhotoSelection) selection;

         final HashSet<Long> allTourIds = new HashSet<>();

         final ArrayList<Photo> allGalleryPhotos = photoSelection.galleryPhotos;

         // count number of tours
         for (final Photo photo : allGalleryPhotos) {

            // get all tour id's
            for (final Long tourId : photo.getTourPhotoReferences().keySet()) {
               allTourIds.add(tourId);
            }

            // set photo period time, from...until
            final long photoDateTime = photo.getPhotoTime();

            if (photoDateTime < _photoStartTime) {
               _photoStartTime = photoDateTime;
            }
            if (photoDateTime > _photoEndTime) {
               _photoEndTime = photoDateTime;
            }
         }

         showTourPhotos_FromCurrentSelection(allGalleryPhotos, allTourIds.size());

      } else if (selection instanceof SelectionTourMarker) {

         final TourData tourData = ((SelectionTourMarker) selection).getTourData();

         addTourPhotos(allPhotos, tourData);

         showTourPhotos_FromCurrentSelection(allPhotos, 1);

      } else if (selection instanceof SelectionTourData) {

         final TourData tourData = ((SelectionTourData) selection).getTourData();

         addTourPhotos(allPhotos, tourData);

         showTourPhotos_FromCurrentSelection(allPhotos, 1);

      } else if (selection instanceof SelectionTourId) {

         final SelectionTourId tourIdSelection = (SelectionTourId) selection;
         final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

         addTourPhotos(allPhotos, tourData);

         showTourPhotos_FromCurrentSelection(allPhotos, 1);

      } else if (selection instanceof SelectionTourIds) {

         // paint all selected tours

         final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();

         for (final Long tourId : tourIds) {

            final TourData tourData = TourManager.getInstance().getTourData(tourId);

            addTourPhotos(allPhotos, tourData);
         }

         showTourPhotos_FromCurrentSelection(allPhotos, tourIds.size());
      }

      /*
       * Ensure that the selection is set correctly and overwrite
       * PhotogalleryProvider.setSelection() which caused wrong behaviour
       */
      _postSelectionProvider.setSelectionNoFireEvent(selection);
   }

   @Override
   public void photoEvent(final IViewPart viewPart, final PhotoEventId photoEventId, final Object data) {

      if (photoEventId == PhotoEventId.PHOTO_SELECTION) {

         if (data instanceof TourPhotoLinkSelection) {

            final TourPhotoLinkSelection linkSelection = (TourPhotoLinkSelection) data;

            if (_isPartVisible == false) {

               _selectionWhenHidden = linkSelection;

            } else {

               onSelectionChanged(linkSelection);
            }
         }

      } else if (photoEventId == PhotoEventId.PHOTO_ATTRIBUTES_ARE_MODIFIED) {

         if (data instanceof ArrayList<?>) {

            final ArrayList<?> arrayList = (ArrayList<?>) data;

            _photoGallery.updatePhotos(arrayList);
         }

      } else if (photoEventId == PhotoEventId.PHOTO_IMAGE_PATH_IS_MODIFIED) {

         _photoGallery.refreshUI();
      }
   }

   private void restoreState() {

      updateColors(true);

      _photoGallery.restoreState();

      // set gallery orientation, default is horizontal
      _isVerticalGallery = Util.getStateBoolean(_state, STATE_PHOTO_GALLERY_IS_VERTICAL, false);
      _photoGallery.setVertical(_isVerticalGallery);

      updateUI_ToogleAction();
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_PHOTO_GALLERY_IS_VERTICAL, _isVerticalGallery);

      _photoGallery.saveState();
   }

   @Override
   public void setFocus() {

   }

   private void showTourPhotos_FromCurrentSelection(final ArrayList<Photo> allPhotos, final int numTours) {

      /*
       * Update photo gallery
       */
      _photoGallery.showImages(
            allPhotos,
            Long.toString(_galleryPositionKey) + "_TourPhotosView", //$NON-NLS-1$
            _isLinkPhotoDisplayed,
            false);

      /*
       * Set title
       */
      final int numPhotos = allPhotos.size();

      String labelText = UI.EMPTY_STRING;
      String labelTooltip = UI.EMPTY_STRING;

      if (numPhotos > 0) {

         final String labelTextFormat = _isLinkPhotoDisplayed
               ? Messages.Photos_AndTours_Label_Source_PhotoLink
               : Messages.Photos_AndTours_Label_Source_Tour;

         final String labelTooltipFormat = _isLinkPhotoDisplayed
               ? Messages.Photos_AndTours_Label_Source_PhotoLink_Tooltip
               : Messages.Photos_AndTours_Label_Source_Tour_Tooltip;

         String photoDateTime = UI.EMPTY_STRING;

         if (numPhotos == 1) {

            photoDateTime = TimeTools.getZonedDateTime(_photoStartTime).format(TimeTools.Formatter_DateTime_S);

         } else if (numPhotos > 1) {

            photoDateTime = TimeTools.getZonedDateTime(_photoStartTime).format(TimeTools.Formatter_DateTime_S)
                  + UI.NEW_LINE + UI.TAB + UI.TAB + UI.TAB
                  + TimeTools.getZonedDateTime(_photoEndTime).format(TimeTools.Formatter_DateTime_S);
         }

         labelText = String.format(labelTextFormat, numTours, numPhotos);
         labelTooltip = String.format(labelTooltipFormat, numTours, numPhotos, photoDateTime);
      }

      _labelTitle.setText(labelText);
      _labelTitle.setToolTipText(labelTooltip);
   }

   private void showTourPhotos_FromDefaultSelection() {

      Display.getCurrent().asyncExec(new Runnable() {
         @Override
         public void run() {

            // validate widget
            if (_photoGallery.isDisposed()) {
               return;
            }

            final ArrayList<TourData> selectedTours = TourManager.getSelectedTours();
            if (selectedTours != null && selectedTours.size() > 0) {
               onSelectionChanged(new SelectionTourData(selectedTours.get(0)));
            }
         }
      });
   }

   private void updateColors(final boolean isRestore) {

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
      final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

      final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

      _photoGallery.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, isRestore);
   }

   private void updateUI_ToogleAction() {

      if (_isVerticalGallery) {

         _actionToggleGalleryOrientation.setToolTipText(Messages.Photo_Gallery_Action_ToggleGalleryHorizontal_ToolTip);
         _actionToggleGalleryOrientation.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.PhotoGallery_Horizontal));

      } else {

         _actionToggleGalleryOrientation.setToolTipText(Messages.Photo_Gallery_Action_ToggleGalleryVertical_ToolTip);
         _actionToggleGalleryOrientation.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.PhotoGallery_Vertical));
      }
   }
}
