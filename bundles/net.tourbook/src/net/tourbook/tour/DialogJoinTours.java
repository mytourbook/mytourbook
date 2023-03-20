/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.data.TourWayPoint;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tag.TagManager;
import net.tourbook.tag.TagMenuManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.action.ActionSetTourTypeMenu;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.views.rawData.DialogUtils;
import net.tourbook.ui.views.tourBook.ActionDeleteTour;
import net.tourbook.ui.views.tourBook.TourBookView;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogJoinTours extends TitleAreaDialog implements ITourProvider2 {

   private static final String   STATE_TOUR_TITLE                       = "Title";                                    //$NON-NLS-1$
   private static final String   STATE_TOUR_TYPE_ID                     = "TourTypeId";                               //$NON-NLS-1$
   private static final String   STATE_PERSON_ID                        = "PersonId";                                 //$NON-NLS-1$

   private static final String   STATE_IS_INCLUDE_DESCRIPTION           = "isIncludeDescription";                     //$NON-NLS-1$
   private static final String   STATE_IS_INCLUDE_MARKER_WAYPOINTS      = "isIncludeMarkerWaypoints";                 //$NON-NLS-1$
   private static final String   STATE_IS_CREATE_TOUR_MARKER            = "isCreateTourMarker";                       //$NON-NLS-1$

   private static final String   STATE_JOIN_METHOD                      = "JoinMethod";                               //$NON-NLS-1$
   private static final String   STATE_JOIN_METHOD_ORIGINAL             = "original";                                 //$NON-NLS-1$
   private static final String   STATE_JOIN_METHOD_CONCATENATED         = "concatenated";                             //$NON-NLS-1$

   private static final String   STATE_TOUR_TITLE_SOURCE                = "TourTitleSource";                          //$NON-NLS-1$
   private static final String   STATE_TOUR_TITLE_SOURCE_FROM_TOUR      = "fromTour";                                 //$NON-NLS-1$
   private static final String   STATE_TOUR_TITLE_SOURCE_CUSTOM         = "custom";                                   //$NON-NLS-1$

   private static final String   STATE_TYPE_SOURCE                      = "TourTypeSource";                           //$NON-NLS-1$
   private static final String   STATE_TYPE_SOURCE_FROM_SELECTED_TOURS  = "fromTour";                                 //$NON-NLS-1$
   private static final String   STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR = "previous";                                 //$NON-NLS-1$
   private static final String   STATE_TYPE_SOURCE_CUSTOM               = "custom";                                   //$NON-NLS-1$

   private static final String   STATE_MARKER_TYPE                      = "TourMarkerType";                           //$NON-NLS-1$
   private static final String   STATE_MARKER_TYPE_SMALL                = "small";                                    //$NON-NLS-1$
   private static final String   STATE_MARKER_TYPE_MEDIUM               = "medium";                                   //$NON-NLS-1$
   private static final String   STATE_MARKER_TYPE_LARGE                = "large";                                    //$NON-NLS-1$

   /**
    * state: join method
    */
   private static final String[] ALL_STATES_JOIN_METHOD                 = new String[] {
         STATE_JOIN_METHOD_ORIGINAL,
         STATE_JOIN_METHOD_CONCATENATED                                                                               //
   };
   private static final String[] STATE_TEXT_JOIN_METHOD                 = new String[] {
         Messages.Dialog_JoinTours_ComboText_KeepTime,
         Messages.Dialog_JoinTours_ComboText_ConcatenateTime                                                          //
   };

   /**
    * state: tour title
    */
   private static final String[] ALL_STATES_TOUR_TILE_SOURCE            = new String[] {
         STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
         STATE_TOUR_TITLE_SOURCE_CUSTOM,                                                                              //
   };
   private static final String[] STATE_COMBO_TEXT_TOUR_TITLE_SOURCE     = new String[] {
         Messages.Dialog_JoinTours_ComboText_TourTitleFromTour,
         Messages.Dialog_JoinTours_ComboText_TourTitleCustom,
         //
   };

   /**
    * state: tour type
    */
   private static final String[] ALL_STATES_TOUR_TYPE                   = new String[] {
         STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
         STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR,
         STATE_TYPE_SOURCE_CUSTOM                                                                                     //
   };
   private static final String[] STATE_TEXT_TOUR_TYPE_SOURCE            = new String[] {
         Messages.Dialog_JoinTours_ComboText_TourTypeFromTour,
         Messages.Dialog_JoinTours_ComboText_TourTypePrevious,
         Messages.Dialog_JoinTours_ComboText_TourTypeCustom                                                           //
   };

   /**
    * state: tour marker
    */
   private static final String[] ALL_STATES_TOUR_MARKER                 = new String[] {
         STATE_MARKER_TYPE_SMALL,
         STATE_MARKER_TYPE_MEDIUM,
         STATE_MARKER_TYPE_LARGE                                                                                      //
   };

   private final IDialogSettings _state                                 = TourbookPlugin.getState("DialogJoinTours"); //$NON-NLS-1$

   private TagMenuManager        _tagMenuMgr;
   private ActionOpenPrefDialog  _actionOpenTourTypePrefs;

   private TourData              _joinedTourData;
   private ArrayList<TourData>   _joinedTourDataList;

   private final List<TourData>  _selectedTours;
   private TourPerson[]          _people;

   private long                  _tourTypeIdFromSelectedTours           = TourDatabase.ENTITY_IS_NOT_SAVED;
   private long                  _tourTypeIdPreviousJoinedTour          = TourDatabase.ENTITY_IS_NOT_SAVED;
   private long                  _tourTypeIdCustom                      = TourDatabase.ENTITY_IS_NOT_SAVED;

   private String                _tourTitleFromTour;
   private String                _tourTitleFromCustom;

   private ITourEventListener    _tourEventListener;
   private ITourProvider         _tourProvider;

   /*
    * UI resources
    */
   private Image _imageLock_Closed = CommonActivator.getThemedImageDescriptor(CommonImages.Lock_Closed).createImage();

   /*
    * UI controls
    */
   private Button    _btnUnlockDeleteSourceToursSelection;

   private Button    _chkCreateTourMarker;
   private Button    _chkDeleteSourceTours;
   private Button    _chkIncludeDescription;
   private Button    _chkIncludeMarkerWaypoints;

   private Combo     _cboPerson;
   private Combo     _cboTourMarker;
   private Combo     _cboJoinMethod;
   private Combo     _cboTourTitleSource;
   private Combo     _cboTourType;

   private Composite _dlgInnerContainer;
   private DateTime  _dtTourDate;
   private DateTime  _dtTourTime;

   private Label     _lblMarkerText;
   private Label     _lblTourStartDate;
   private Label     _lblTourStartTime;
   private Label     _lblTourTags;
   private CLabel    _lblTourType;

   private Link      _linkTag;
   private Link      _linkTourType;

   private Composite _parent;

   private Point     _shellDefaultSize;

   private Text      _txtTourTitle;

   public DialogJoinTours(final Shell parentShell,
                          final ITourProvider tourProvider,
                          final List<TourData> selectedTours) {

      super(parentShell);

      _tourProvider = tourProvider;
      // sort tours by date/time
      Collections.sort(selectedTours);

      _selectedTours = selectedTours;

      // make dialog resizable
      int shellStyle = getShellStyle();
      shellStyle = //
            SWT.NONE //
                  | SWT.TITLE
                  | SWT.CLOSE
                  | SWT.MIN
//            | SWT.MAX
                  | SWT.RESIZE;
      setShellStyle(shellStyle);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_JoinTours_DlgArea_Title);

      shell.addDisposeListener(disposeEvent -> onDispose());

      shell.addListener(SWT.Resize, event -> {

         // allow resizing the width but not the height

         if (_shellDefaultSize == null) {
            _shellDefaultSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
         }

         final Point shellSize = shell.getSize();

         /*
          * this is not working, the shell is flickering when the shell size is below min size
          * and I found no way to prevent a resize :-(
          */
//            if (shellSize.x < _shellDefaultSize.x) {
//               event.doit = false;
//            }

         shellSize.x = shellSize.x < _shellDefaultSize.x ? _shellDefaultSize.x : shellSize.x;
         shellSize.y = _shellDefaultSize.y;

         shell.setSize(shellSize);
      });
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_JoinTours_DlgArea_Title);
      setMessage(Messages.Dialog_JoinTours_DlgArea_Message);
   }

   private void createActions() {

      _tagMenuMgr = new TagMenuManager(this, false);

      _actionOpenTourTypePrefs = new ActionOpenPrefDialog(
            Messages.action_tourType_modify_tourTypes,
            ITourbookPreferences.PREF_PAGE_TOUR_TYPE);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _parent = parent;

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      initTourData();

      createUI(dlgContainer);

      restoreState();

      updateUITourTypeTags();
      updateUIMarker(true);
      updateUIFromModel();

      enableControls();

      createActions();
      createMenus();

      // must be run async because the dark theme is overwriting colors after calling createDialogArea()
      parent.getDisplay().asyncExec(this::updateUI_LockUnlockButton);

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */
   private void createMenus() {

      /*
       * tag menu
       */
      final MenuManager menuMgr = new MenuManager();

      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(menuManager -> {

         final Set<TourTag> joinedTourTags = _joinedTourData.getTourTags();
         final boolean isTagInTour = joinedTourTags != null && !joinedTourTags.isEmpty();

         _tagMenuMgr.fillTagMenu(menuManager, false);
         _tagMenuMgr.enableTagActions(true, isTagInTour, joinedTourTags);
      });

      // set menu for the tag item

      final Menu tagContextMenu = menuMgr.createContextMenu(_linkTag);
      tagContextMenu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuHidden(final MenuEvent e) {
            _tagMenuMgr.onHideMenu();
         }

         @Override
         public void menuShown(final MenuEvent menuEvent) {

            final Rectangle rect = _linkTag.getBounds();
            Point pt = new Point(rect.x, rect.y + rect.height);
            pt = _linkTag.getParent().toDisplay(pt);

            _tagMenuMgr.onShowMenu(menuEvent, _linkTag, pt, null);
         }
      });

      _linkTag.setMenu(tagContextMenu);

      /*
       * tour type menu
       */
      final MenuManager typeMenuMgr = new MenuManager();

      typeMenuMgr.setRemoveAllWhenShown(true);
      typeMenuMgr.addMenuListener(menuManager -> {

         // set menu items

         ActionSetTourTypeMenu.fillMenu(menuManager, DialogJoinTours.this, false);

         menuManager.add(new Separator());
         menuManager.add(_actionOpenTourTypePrefs);
      });

      // set menu for the tag item
      _linkTourType.setMenu(typeMenuMgr.createContextMenu(_linkTourType));
   }

   private void createUI(final Composite parent) {

      final SelectionListener defaultSelectionListener =
            widgetSelectedAdapter(selectionEvent -> enableControls());

      _dlgInnerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_dlgInnerContainer);
      GridLayoutFactory.swtDefaults().margins(10, 10).numColumns(3).spacing(10, 8).applyTo(_dlgInnerContainer);
//      _dlgInnerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI10JoinMethod(_dlgInnerContainer, defaultSelectionListener);
         createUI20TourTime(_dlgInnerContainer);
         createUI22Title(_dlgInnerContainer);
         createUI30TypeTags(_dlgInnerContainer);
         createUI40Person(_dlgInnerContainer);
         createUI50DescriptionMarker(_dlgInnerContainer, defaultSelectionListener);
         createUI60DeleteSourceTours(_dlgInnerContainer);
      }
   }

   /**
    * tour time
    */
   private void createUI10JoinMethod(final Composite parent, final SelectionListener defaultSelectionListener) {

      /*
       * join method
       */

      // label
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setText(Messages.Dialog_JoinTours_Label_JoinMethod);

      // combo
      _cboJoinMethod = new Combo(parent, SWT.READ_ONLY);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboJoinMethod);
      _cboJoinMethod.addSelectionListener(defaultSelectionListener);

      // fill combo
      Arrays.asList(STATE_TEXT_JOIN_METHOD).forEach(timeText -> _cboJoinMethod.add(timeText));
   }

   /**
    * tour time
    */
   private void createUI20TourTime(final Composite parent) {

      final SelectionListener dateTimeUpdateListener = widgetSelectedAdapter(selectionEvent -> enableControls());

      /*
       * tour start date/time
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setText(Messages.Dialog_SplitTour_Label_TourStartDateTime);

      final Composite dateContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .span(2, 1)
            .indent(0, -5)
            .applyTo(dateContainer);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(dateContainer);
      {
         /*
          * tour start: date
          */
         _lblTourStartDate = new Label(dateContainer, SWT.NONE);
         _lblTourStartDate.setText(Messages.Dialog_JoinTours_Label_TourDate);

         _dtTourDate = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.DATE | SWT.DROP_DOWN | SWT.BORDER);
         GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourDate);
         _dtTourDate.addSelectionListener(dateTimeUpdateListener);

         /*
          * tour start: time
          */
         _lblTourStartTime = new Label(dateContainer, SWT.NONE);
         GridDataFactory.fillDefaults().indent(20, 0).align(SWT.FILL, SWT.CENTER).applyTo(_lblTourStartTime);
         _lblTourStartTime.setText(Messages.Dialog_JoinTours_Label_TourTime);

         _dtTourTime = new org.eclipse.swt.widgets.DateTime(dateContainer, SWT.TIME | SWT.DROP_DOWN | SWT.BORDER);
         GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_dtTourTime);
         _dtTourTime.addSelectionListener(dateTimeUpdateListener);
      }
   }

   /**
    * tour title
    */
   private void createUI22Title(final Composite parent) {

      // label: title
      final Label label = new Label(parent, SWT.NONE);
      label.setText(Messages.Dialog_SplitTour_Label_TourTitle);
      label.setToolTipText(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip);

      // combo: title source
      _cboTourTitleSource = new Combo(parent, SWT.READ_ONLY);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(_cboTourTitleSource);
      _cboTourTitleSource.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectTourTitleSource()));

      // fill combo
      for (final String comboText : STATE_COMBO_TEXT_TOUR_TITLE_SOURCE) {
         _cboTourTitleSource.add(comboText);
      }

      // spacer
      new Label(parent, SWT.NONE);

      // text: title
      _txtTourTitle = new Text(parent, SWT.BORDER);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .indent(0, -5)
            .applyTo(_txtTourTitle);
      _txtTourTitle.setToolTipText(Messages.Dialog_SplitTour_Label_TourTitle_Tooltip);
   }

//   /**
//    * tour title
//    */
//   private void createUI20Title(final Composite parent) {
//
//      final Label label = new Label(parent, SWT.NONE);
//      label.setText(Messages.Dialog_JoinTours_Label_Title);
//      label.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);
//
//      _txtJoinedTitle = new Text(parent, SWT.BORDER);
//      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtJoinedTitle);
//      _txtJoinedTitle.setToolTipText(Messages.Dialog_JoinTours_Label_Title_Tooltip);
//   }

   /**
    * tour type & tags
    *
    * @param defaultSelectionAdapter
    */
   private void createUI30TypeTags(final Composite parent) {

      /*
       * tour type
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setText(Messages.Dialog_JoinTours_Label_TourType);

      _cboTourType = new Combo(parent, SWT.READ_ONLY);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboTourType);
      _cboTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectTourTypeSource()));

      // fill combo
      for (final String tourTypeText : STATE_TEXT_TOUR_TYPE_SOURCE) {
         _cboTourType.add(tourTypeText);
      }

      // spacer
      new Label(parent, SWT.NONE);

      final Composite tourTypeContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)//
            .indent(0, -8)
            .span(2, 1)
            .applyTo(tourTypeContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(tourTypeContainer);
      {
         _linkTourType = new Link(tourTypeContainer, SWT.NONE);
         _linkTourType.setText(Messages.Dialog_JoinTours_Link_TourType);
         _linkTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> net.tourbook.common.UI.openControlMenu(_linkTourType)));

         _lblTourType = new CLabel(tourTypeContainer, SWT.NONE);
         GridDataFactory.swtDefaults().grab(true, false).applyTo(_lblTourType);
      }

      /*
       * tags
       */
      _linkTag = new Link(parent, SWT.NONE);
      _linkTag.setText(Messages.tour_editor_label_tour_tag);
      GridDataFactory.fillDefaults()//
            .align(SWT.BEGINNING, SWT.FILL)
            .applyTo(_linkTag);
      _linkTag.addSelectionListener(widgetSelectedAdapter(
            selectionEvent -> UI.openControlMenu(_linkTag)));

      _lblTourTags = new Label(parent, SWT.WRAP);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            // hint is necessary that the width is not expanded when the text is long
            .hint(200, SWT.DEFAULT)
            .span(2, 1)
            .applyTo(_lblTourTags);
   }

   /**
    * person
    */
   private void createUI40Person(final Composite parent) {

      // label
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(label);
      label.setText(Messages.Dialog_SplitTour_Label_Person);
      label.setToolTipText(Messages.Dialog_SplitTour_Label_Person_Tooltip);

      // combo: person
      _cboPerson = new Combo(parent, SWT.READ_ONLY);
      GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_cboPerson);
      _cboPerson.setVisibleItemCount(20);
      _cboPerson.setToolTipText(Messages.Dialog_SplitTour_Label_Person_Tooltip);
   }

   /**
    * checkbox: set marker for each tour
    */
   private void createUI50DescriptionMarker(final Composite parent, final SelectionListener defaultSelectionListener) {

      /*
       * description
       */
//      Label label = new Label(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
//      label.setText(Messages.Dialog_JoinTours_Label_Description);

      // checkbox
      _chkIncludeDescription = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults().span(3, 1).indent(0, 10).applyTo(_chkIncludeDescription);
      _chkIncludeDescription.setText(Messages.Dialog_JoinTours_Checkbox_IncludeDescription);

      /*
       * include existing tour marker
       */
//      label = new Label(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);
//      label.setText(Messages.Dialog_JoinTours_Label_TourMarker);

      // checkbox
      _chkIncludeMarkerWaypoints = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkIncludeMarkerWaypoints);
      _chkIncludeMarkerWaypoints.setText(Messages.Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints);

      /*
       * create tour marker
       */
//      label = new Label(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);
//      label.setText(Messages.Dialog_JoinTours_Label_CreateTourMarker);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(3, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().spacing(0, 3).applyTo(container);
      {
         // checkbox
         _chkCreateTourMarker = new Button(container, SWT.CHECK);
         GridDataFactory.fillDefaults().applyTo(_chkCreateTourMarker);
         _chkCreateTourMarker.setText(Messages.Dialog_JoinTours_Checkbox_CreateTourMarker);
         _chkCreateTourMarker.addSelectionListener(defaultSelectionListener);

         final Composite markerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(markerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(markerContainer);
         {
            // label
            _lblMarkerText = new Label(markerContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .align(SWT.FILL, SWT.CENTER)
                  .indent(16, 0)
                  .applyTo(_lblMarkerText);
            _lblMarkerText.setText(Messages.Dialog_JoinTours_Label_TourMarkerText);

            // combo
            _cboTourMarker = new Combo(markerContainer, SWT.READ_ONLY);
            GridDataFactory.fillDefaults()//
                  .grab(true, false)
                  .applyTo(_cboTourMarker);
            _cboTourMarker.addSelectionListener(defaultSelectionListener);

            // !!! combo box is filled in updateUIMarker() !!!
         }
      }
   }

   /**
    * Checkbox to specify if the source tours should be deleted after they are
    * concatenated into a new tour
    */
   private void createUI60DeleteSourceTours(final Composite parent) {

      _chkDeleteSourceTours = new Button(parent, SWT.CHECK);
      _chkDeleteSourceTours.setText(Messages.Dialog_JoinTours_Checkbox_DeleteSourceTours);
      _chkDeleteSourceTours.setEnabled(false);
      GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).span(2, 1).applyTo(_chkDeleteSourceTours);

      _btnUnlockDeleteSourceToursSelection = new Button(parent, SWT.PUSH);
      _btnUnlockDeleteSourceToursSelection.setText(Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);
      _btnUnlockDeleteSourceToursSelection.setImage(_imageLock_Closed);
      _btnUnlockDeleteSourceToursSelection.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_Unlock_DeleteSourceTours()));
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .grab(true, false)
            .applyTo(_btnUnlockDeleteSourceToursSelection);
   }

   private void enableControls() {

      final boolean isCustomTime = getStateJoinMethod().equals(STATE_JOIN_METHOD_CONCATENATED);
      final boolean isCustomTourTitle = getStateTourTitleSource().equals(STATE_TOUR_TITLE_SOURCE_CUSTOM);
      final boolean isCustomTourType = getStateTourTypeSource().equals(STATE_TYPE_SOURCE_CUSTOM);
      final boolean isCreateMarker = _chkCreateTourMarker.getSelection();

      _txtTourTitle.setEnabled(isCustomTourTitle);

      _dtTourDate.setEnabled(isCustomTime);
      _dtTourTime.setEnabled(isCustomTime);
      _lblTourStartDate.setEnabled(isCustomTime);
      _lblTourStartTime.setEnabled(isCustomTime);

      if (!isCustomTime) {
         restoreFirstTourDateTime();
      }

      _cboTourMarker.setEnabled(isCreateMarker);
      _lblMarkerText.setEnabled(isCreateMarker);

      _linkTourType.setEnabled(isCustomTourType);
      _lblTourType.setEnabled(isCustomTourType);

      _btnUnlockDeleteSourceToursSelection.setEnabled(_tourProvider instanceof TourBookView);
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
//      return null;
   }

//   /**
//    * info
//    */
//   private void createUI50Info(final Composite container) {
//
//      final Label label = new Label(container, SWT.NONE);
//      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).indent(0, 10).applyTo(label);
//      label.setText(Messages.Dialog_JoinTours_Label_OtherFields);
//
//      // use a bulleted list to display this info
//      final StyleRange style = new StyleRange();
//      style.metrics = new GlyphMetrics(0, 0, 10);
//      final Bullet bullet = new Bullet(style);
//
//      final String infoText = Messages.Dialog_JoinTours_Label_OtherFieldsInfo;
//      final int lineCount = Util.countCharacter(infoText, '\n');
//
//      final StyledText styledText = new StyledText(container, SWT.READ_ONLY);
//      GridDataFactory.fillDefaults()//
//            .align(SWT.FILL, SWT.BEGINNING)
//            .indent(0, 10)
//            .span(2, 1)
//            .applyTo(styledText);
//      styledText.setText(infoText);
//      styledText.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
//      styledText.setLineBullet(0, lineCount + 1, bullet);
//   }

   private TourPerson getSelectedPerson() {

      final int selectedIndex = _cboPerson.getSelectionIndex();
      final TourPerson person = _people[selectedIndex];

      return person;
   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      // return joined tour

      return _joinedTourDataList;
   }

   private String getStateJoinMethod() {
      return Util.getStateFromCombo(_cboJoinMethod, ALL_STATES_JOIN_METHOD, STATE_JOIN_METHOD_ORIGINAL);
   }

   private String getStateTourMarker() {
      return Util.getStateFromCombo(_cboTourMarker, ALL_STATES_TOUR_MARKER, STATE_MARKER_TYPE_SMALL);
   }

   private String getStateTourTitleSource() {
      return Util.getStateFromCombo(
            _cboTourTitleSource,
            ALL_STATES_TOUR_TILE_SOURCE,
            STATE_TOUR_TITLE_SOURCE_FROM_TOUR);
   }

   private String getStateTourTypeSource() {
      return Util.getStateFromCombo(_cboTourType, ALL_STATES_TOUR_TYPE, STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);
   }

   private void initTourData() {

      _joinedTourData = new TourData();

      /*
       * create a dummy tour id because setting of the tags and tour type works requires it
       * otherwise it would cause a NPE when a tour has no id
       */
      _joinedTourData.createTourIdDummy();

      _joinedTourDataList = new ArrayList<>();
      _joinedTourDataList.add(_joinedTourData);

      final Set<TourTag> joinedTourTags = new HashSet<>();

      /*
       * set tour title
       */
      _tourTitleFromTour = UI.EMPTY_STRING;

      // get title from first tour which contains a title
      for (final TourData tourData : _selectedTours) {

         final String tourTitle = tourData.getTourTitle();
         if (tourTitle.length() > 0) {
            _tourTitleFromTour = tourTitle;
            break;
         }
      }

      /*
       * set tags into joined tour and get tour type from selected tours
       */
      for (final TourData tourData : _selectedTours) {

         // get tour type id from the first tour which has a tour type
         if (_tourTypeIdFromSelectedTours == TourDatabase.ENTITY_IS_NOT_SAVED) {
            final TourType tourType = tourData.getTourType();
            if (tourType != null) {
               _tourTypeIdFromSelectedTours = tourType.getTypeId();
            }
         }

         // get all tags
         final Set<TourTag> tourTags = tourData.getTourTags();
         joinedTourTags.addAll(tourTags);
      }

      /*
       * set tour type into joined tour
       */
      _tourTypeIdCustom = _tourTypeIdPreviousJoinedTour = Util.getStateLong(
            _state,
            STATE_TOUR_TYPE_ID,
            TourDatabase.ENTITY_IS_NOT_SAVED);

      final String stateTourTypeSource = Util.getStateString(
            _state,
            STATE_TYPE_SOURCE,
            STATE_TYPE_SOURCE_FROM_SELECTED_TOURS);

      long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

      if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {
         joinedTourTypeId = _tourTypeIdFromSelectedTours;
      } else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR)
            || stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
         joinedTourTypeId = _tourTypeIdPreviousJoinedTour;
      }

      _joinedTourData.setTourType(TourDatabase.getTourType(joinedTourTypeId));
      _joinedTourData.setTourTags(joinedTourTags);
   }

   private void joinPausedTimes(final boolean isOriginalTime,
                                final ArrayList<Long> joinedPausedTime_Start,
                                final ArrayList<Long> joinedPausedTime_End,
                                final ArrayList<Long> joinedPausedTime_Data,
                                final ZonedDateTime joinedTourStart,
                                final TourData previousTourData,
                                final TourData tourData) {

      final Long[] pausedTime_Start = ArrayUtils.toObject(tourData.getPausedTime_Start());
      if (pausedTime_Start != null) {

         //If a new tour start time is set, we need to offset the tour pause times
         if (!isOriginalTime) {

            offsetPausedTimes(
                  tourData.getTourStartTimeMS(),
                  joinedTourStart.toInstant().toEpochMilli(),
                  previousTourData,
                  pausedTime_Start);
         }

         joinedPausedTime_Start.addAll(Arrays.asList(pausedTime_Start));
      }
      final Long[] pausedTime_End = ArrayUtils.toObject(tourData.getPausedTime_End());
      if (pausedTime_End != null) {

         //If a new tour start time is set, we need to offset the tour pause times
         if (!isOriginalTime) {

            offsetPausedTimes(
                  tourData.getTourStartTimeMS(),
                  joinedTourStart.toInstant().toEpochMilli(),
                  previousTourData,
                  pausedTime_End);
         }
         joinedPausedTime_End.addAll(Arrays.asList(pausedTime_End));
      }
      final Long[] pausedTime_Data = ArrayUtils.toObject(tourData.getPausedTime_Data());
      if (pausedTime_Data != null) {

         joinedPausedTime_Data.addAll(Arrays.asList(pausedTime_Data));

      } else if (pausedTime_Start != null && pausedTime_End != null) {

         //The case can happen that a tour has pause data but no paused time
         //data (i.e.: All file formats except FIT imported prior to 22.1.0).
         //In this case, we need to add default paused time data

         Arrays.asList(pausedTime_Start).forEach(pausedTime -> joinedPausedTime_Data.add(0L));
      }
   }

   /**
    * Join the tours and create a new tour
    */
   private boolean joinTours() {

      final boolean isOriginalTime = getStateJoinMethod().equals(STATE_JOIN_METHOD_ORIGINAL);

      /**
       * number of slices, time series are already checked in ActionJoinTours
       */
      int joinedSliceCounter = 0;

      for (final TourData tourData : _selectedTours) {

         final int[] tourTimeSerie = tourData.timeSerie;
         final float[] tourDistanceSerie = tourData.distanceSerie;
         final double[] tourLatitudeSerie = tourData.latitudeSerie;

         final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);
         final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
         final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);

         if (isTourTime) {
            joinedSliceCounter += tourTimeSerie.length;
         } else if (isTourDistance) {
            joinedSliceCounter += tourDistanceSerie.length;
         } else if (isTourLat) {
            joinedSliceCounter += tourLatitudeSerie.length;
         }
      }

      boolean isJoinAltitude = false;
      boolean isJoinDistance = false;
      boolean isJoinCadence = false;
      boolean isJoinLat = false;
      boolean isJoinLon = false;
      boolean isJoinPower = false;
      boolean isJoinPulse = false;
      boolean isJoinSpeed = false;
//      boolean isJoinTemperature = false;
      boolean isJoinTime = false;

      final float[] joinedAltitudeSerie = new float[joinedSliceCounter];
      final float[] joinedCadenceSerie = new float[joinedSliceCounter];
      final float[] joinedDistanceSerie = new float[joinedSliceCounter];
      final double[] joinedLatitudeSerie = new double[joinedSliceCounter];
      final double[] joinedLongitudeSerie = new double[joinedSliceCounter];
      final float[] joinedPowerSerie = new float[joinedSliceCounter];
      final float[] joinedPulseSerie = new float[joinedSliceCounter];
      final float[] joinedSpeedSerie = new float[joinedSliceCounter];
      final float[] joinedTemperatureSerie = new float[joinedSliceCounter];
      final int[] joinedTimeSerie = new int[joinedSliceCounter];

      final StringBuilder joinedDescription = new StringBuilder();
      final HashSet<TourMarker> joinedTourMarker = new HashSet<>();
      final ArrayList<TourWayPoint> joinedWayPoints = new ArrayList<>();

      int joinedSerieIndex = 0;
      int joinedTourStartIndex = 0;
      int joinedTourStartDistance = 0;
      int joinedRecordedTime = 0;
      int joinedPausedTime = 0;
      final ArrayList<Long> joinedPausedTime_Start = new ArrayList<>();
      final ArrayList<Long> joinedPausedTime_End = new ArrayList<>();
      final ArrayList<Long> joinedPausedTime_Data = new ArrayList<>();
      int joinedMovingTime = 0;
      float joinedDistance = 0;
      int joinedCalories = 0;
      float joinedCadenceMultiplier = 0;
      boolean isJoinedDistanceFromSensor = false;
      boolean isJoinedPowerFromSensor = false;
      boolean isJoinedPulseFromSensor = false;
      short joinedDeviceTimeInterval = -1;
      String joinedWeatherClouds = UI.EMPTY_STRING;
      String joinedWeather = UI.EMPTY_STRING;
      int joinedWeatherWindDirection = 0;
      int joinedWeatherWindSpeed = 0;
      int joinedRestPulse = 0;

      int relTourTime = 0;
      long relTourTimeOffset = 0;
      long absFirstTourStartTimeSec = 0;
      long absJoinedTourStartTimeSec = 0;
      ZonedDateTime joinedTourStart = null;

      TourData previousTourData = null;

      boolean isFirstTour = true;

      /*
       * copy tour data series into joined data series
       */
      for (final TourData tourData : _selectedTours) {

         final float[] tourAltitudeSerie = tourData.altitudeSerie;
         final float[] tourCadenceSerie = tourData.getCadenceSerie();
         final float[] tourDistanceSerie = tourData.distanceSerie;
         final double[] tourLatitudeSerie = tourData.latitudeSerie;
         final double[] tourLongitudeSerie = tourData.longitudeSerie;
         final float[] tourPulseSerie = tourData.pulseSerie;
         final float[] tourTemperatureSerie = tourData.temperatureSerie;
         final int[] tourTimeSerie = tourData.timeSerie;

         final boolean isTourAltitude = (tourAltitudeSerie != null) && (tourAltitudeSerie.length > 0);
         final boolean isTourCadence = (tourCadenceSerie != null) && (tourCadenceSerie.length > 0);
         final boolean isTourDistance = (tourDistanceSerie != null) && (tourDistanceSerie.length > 0);
         final boolean isTourLat = (tourLatitudeSerie != null) && (tourLatitudeSerie.length > 0);
         final boolean isTourLon = (tourLongitudeSerie != null) && (tourLongitudeSerie.length > 0);
         final boolean isTourPulse = (tourPulseSerie != null) && (tourPulseSerie.length > 0);
         final boolean isTourTemperature = (tourTemperatureSerie != null) && (tourTemperatureSerie.length > 0);
         final boolean isTourTime = (tourTimeSerie != null) && (tourTimeSerie.length > 0);

         /*
          * get speed/power data when it's from the device
          */
         float[] tourPowerSerie = null;
         float[] tourSpeedSerie = null;
         final boolean isTourPower = tourData.isPowerSerieFromDevice();
         final boolean isTourSpeed = tourData.isSpeedSerieFromDevice();
         if (isTourPower) {
            tourPowerSerie = tourData.getPowerSerie();
         }
         if (isTourSpeed) {
            tourSpeedSerie = tourData.getSpeedSerie();
         }

         /*
          * set tour time
          */
         final ZonedDateTime tourStartTime = tourData.getTourStartTime();

         if (isFirstTour) {

            // get start date/time

            if (isOriginalTime) {

               joinedTourStart = tourStartTime;

            } else {

               joinedTourStart = ZonedDateTime.of(
                     _dtTourDate.getYear(),
                     _dtTourDate.getMonth() + 1,
                     _dtTourDate.getDay(),
                     _dtTourTime.getHours(),
                     _dtTourTime.getMinutes(),
                     _dtTourTime.getSeconds(),
                     0,
                     TimeTools.getDefaultTimeZone());
            }

            // tour start in absolute seconds
            absJoinedTourStartTimeSec = joinedTourStart.toInstant().getEpochSecond();
            absFirstTourStartTimeSec = absJoinedTourStartTimeSec;

         } else {

            // get relative time offset

            if (isOriginalTime) {

               final long absTourStartTimeSec = tourStartTime.toInstant().getEpochSecond();

               // keep original time
               relTourTimeOffset = absTourStartTimeSec - absFirstTourStartTimeSec;

            } else {

               /*
                * remove time gaps between tours, add relative time from the last tour and add 1
                * second for the start of the next tour
                */
               relTourTimeOffset += relTourTime + 1;
            }
         }

         /*
          * get number of slices
          */
         int tourSliceCounter = 0;
         if (isTourTime) {
            tourSliceCounter = tourTimeSerie.length;
         } else if (isTourDistance) {
            tourSliceCounter = tourDistanceSerie.length;
         } else if (isTourLat) {
            tourSliceCounter = tourLatitudeSerie.length;
         }

         float relTourDistance = 0;

         /*
          * copy data series
          */
         for (int tourSerieIndex = 0; tourSerieIndex < tourSliceCounter; tourSerieIndex++) {

            if (isTourTime) {

               relTourTime = tourTimeSerie[tourSerieIndex];

               joinedTimeSerie[joinedSerieIndex] = (int) (relTourTimeOffset + relTourTime);

               isJoinTime = true;
            }

            if (isTourAltitude) {
               joinedAltitudeSerie[joinedSerieIndex] = tourAltitudeSerie[tourSerieIndex];
               isJoinAltitude = true;
            }
            if (isTourCadence) {
               joinedCadenceSerie[joinedSerieIndex] = tourCadenceSerie[tourSerieIndex];
               isJoinCadence = true;
            }

            if (isTourDistance) {

               relTourDistance = tourDistanceSerie[tourSerieIndex];

               joinedDistanceSerie[joinedSerieIndex] = joinedTourStartDistance + relTourDistance;
               isJoinDistance = true;
            }

            if (isTourPulse) {
               joinedPulseSerie[joinedSerieIndex] = tourPulseSerie[tourSerieIndex];
               isJoinPulse = true;
            }
            if (isTourLat) {
               joinedLatitudeSerie[joinedSerieIndex] = tourLatitudeSerie[tourSerieIndex];
               isJoinLat = true;
            }
            if (isTourLon) {
               joinedLongitudeSerie[joinedSerieIndex] = tourLongitudeSerie[tourSerieIndex];
               isJoinLon = true;
            }

            if (isTourTemperature) {
               joinedTemperatureSerie[joinedSerieIndex] = tourTemperatureSerie[tourSerieIndex];
            } else {
               // set temperature to temporarily value
               joinedTemperatureSerie[joinedSerieIndex] = Float.MIN_VALUE;
            }

            if (isTourPower) {
               joinedPowerSerie[joinedSerieIndex] = tourPowerSerie[tourSerieIndex];
               isJoinPower = true;
            }
            if (isTourSpeed) {
               joinedSpeedSerie[joinedSerieIndex] = tourSpeedSerie[tourSerieIndex];
               isJoinSpeed = true;
            }

            joinedSerieIndex++;
         }

         final Set<TourMarker> tourMarkers = tourData.getTourMarkers();

         if (_chkIncludeMarkerWaypoints.getSelection()) {

            /*
             * copy tour markers
             */
            for (final TourMarker tourMarker : tourMarkers) {

               final TourMarker clonedMarker = tourMarker.clone(_joinedTourData);

               int joinMarkerIndex = joinedTourStartIndex + clonedMarker.getSerieIndex();
               if (joinMarkerIndex >= joinedSliceCounter) {
                  joinMarkerIndex = joinedSliceCounter - 1;
               }

               // adjust marker position, position is relative to the tour start
               clonedMarker.setSerieIndex(joinMarkerIndex);

               if (isJoinTime) {
                  final int relativeTourTime = joinedTimeSerie[joinMarkerIndex];
                  clonedMarker.setTime(//
                        relativeTourTime,
                        joinedTourStart.toInstant().toEpochMilli() + (relativeTourTime * 1000));
               }
               if (isJoinDistance) {
                  clonedMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
               }

               if (isJoinAltitude) {
                  clonedMarker.setAltitude(joinedAltitudeSerie[joinMarkerIndex]);
               }

               if (isJoinLat && isJoinLon) {
                  clonedMarker.setGeoPosition(
                        joinedLatitudeSerie[joinMarkerIndex],
                        joinedLongitudeSerie[joinMarkerIndex]);
               }

               joinedTourMarker.add(clonedMarker);
            }

            /*
             * copy way points
             */
            for (final TourWayPoint wayPoint : tourData.getTourWayPoints()) {
               joinedWayPoints.add(wayPoint.clone(_joinedTourData));
            }
         }

         if (_chkCreateTourMarker.getSelection()) {

            /*
             * create a tour marker
             */

            // first find a free marker position in the tour
            int tourMarkerIndex = -1;
            for (int tourSerieIndex = 0; tourSerieIndex < tourSliceCounter; tourSerieIndex++) {

               boolean isIndexAvailable = true;

               // check if a marker occupies the current index
               for (final TourMarker tourMarker : tourMarkers) {
                  if (tourMarker.getSerieIndex() == tourSerieIndex) {
                     isIndexAvailable = false;
                     break;
                  }
               }

               if (isIndexAvailable) {
                  // free index was found
                  tourMarkerIndex = tourSerieIndex;
                  break;
               }
            }

            if (tourMarkerIndex != -1) {

               // create tour marker label
               final String stateTourMarker = getStateTourMarker();

               String markerLabel = UI.EMPTY_STRING;

               if (stateTourMarker.equals(STATE_MARKER_TYPE_SMALL)) {

                  markerLabel = tourStartTime.format(TimeTools.Formatter_Date_S);

               } else if (stateTourMarker.equals(STATE_MARKER_TYPE_MEDIUM)) {

                  markerLabel = tourStartTime.format(TimeTools.Formatter_DateTime_S);

               } else if (stateTourMarker.equals(STATE_MARKER_TYPE_LARGE)) {

                  markerLabel = tourStartTime.format(TimeTools.Formatter_DateTime_F);
               }

               final int joinMarkerIndex = joinedTourStartIndex + tourMarkerIndex;

               final TourMarker tourMarker = new TourMarker(_joinedTourData, ChartLabelMarker.MARKER_TYPE_CUSTOM);

               tourMarker.setSerieIndex(joinMarkerIndex);
               tourMarker.setLabel(markerLabel);
               tourMarker.setLabelPosition(TourMarker.LABEL_POS_VERTICAL_ABOVE_GRAPH);

               if (isJoinTime) {

                  final int relativeTourTime = joinedTimeSerie[joinMarkerIndex];

                  tourMarker.setTime(//
                        relativeTourTime,
                        joinedTourStart.toInstant().toEpochMilli() + (relativeTourTime * 1000));
               }

               if (isJoinDistance) {
                  tourMarker.setDistance(joinedDistanceSerie[joinMarkerIndex]);
               }

               if (isJoinAltitude) {
                  tourMarker.setAltitude(joinedAltitudeSerie[joinMarkerIndex]);
               }

               if (isJoinLat && isJoinLon) {
                  tourMarker.setGeoPosition(
                        joinedLatitudeSerie[joinMarkerIndex],
                        joinedLongitudeSerie[joinMarkerIndex]);
               }

               joinedTourMarker.add(tourMarker);
            }
         }

         /*
          * create description
          */
         if (_chkIncludeDescription.getSelection()) {

            if (joinedDescription.length() > 0) {
               // set space between two tours
               joinedDescription.append(UI.NEW_LINE2);
            }

            joinedDescription.append(Messages.Dialog_JoinTours_Label_Tour + net.tourbook.ui.UI.COLON_SPACE);
            joinedDescription.append(TourManager.getTourTitleDetailed(tourData));

            final String tourDescription = tourData.getTourDescription();
            if (StringUtils.hasContent(tourDescription)) {
               joinedDescription.append(UI.NEW_LINE);
               joinedDescription.append(tourDescription);
            }
         }

         /*
          * other tour values
          */
         if (isFirstTour) {

            isJoinedDistanceFromSensor = tourData.isDistanceSensorPresent();
            isJoinedPowerFromSensor = tourData.isPowerSensorPresent();
            isJoinedPulseFromSensor = tourData.isPulseSensorPresent();

            joinedCadenceMultiplier = tourData.getCadenceMultiplier();

            joinedDeviceTimeInterval = tourData.getDeviceTimeInterval();

            joinedWeather = tourData.getWeather();
            joinedWeatherClouds = tourData.getWeather_Clouds();
            joinedWeatherWindDirection = tourData.getWeather_Wind_Direction();
            joinedWeatherWindSpeed = tourData.getWeather_Wind_Speed();

            joinedRestPulse = tourData.getRestPulse();

         } else {
            if (isJoinedDistanceFromSensor && tourData.isDistanceSensorPresent()) {
               // keep TRUE state
            } else {
               isJoinedDistanceFromSensor = false;
            }
            if (isJoinedPowerFromSensor && tourData.isPowerSensorPresent()) {
               // keep TRUE state
            } else {
               isJoinedPowerFromSensor = false;
            }
            if (isJoinedPulseFromSensor && tourData.isPulseSensorPresent()) {
               // keep TRUE state
            } else {
               isJoinedPulseFromSensor = false;
            }

            if (joinedDeviceTimeInterval == tourData.getDeviceTimeInterval()) {
               // keep value
            } else {
               joinedDeviceTimeInterval = -1;
            }

            if (isOriginalTime) {

               // As it's not the first tour, we add the time difference between this tour's start time
               // and the previous tour end time as a pause.

               final long previousTourEndTime = previousTourData.getTourEndTimeMS();
               final long currentTourStartTime = tourData.getTourStartTimeMS();

               if (previousTourEndTime < currentTourStartTime) {

                  joinedPausedTime_Start.add(previousTourEndTime);
                  joinedPausedTime_End.add(currentTourStartTime);

                  // set this pause as a manual pause, it's not an auto-pause
                  joinedPausedTime_Data.add(0L);

                  joinedPausedTime += (currentTourStartTime - previousTourEndTime) / 1000;
               }
            }
         }

         joinPausedTimes(isOriginalTime,
               joinedPausedTime_Start,
               joinedPausedTime_End,
               joinedPausedTime_Data,
               joinedTourStart,
               previousTourData,
               tourData);

         joinedPausedTime += tourData.getTourDeviceTime_Paused();

         /*
          * summarize other fields
          */
         tourData.computeTourMovingTime();
         joinedRecordedTime += tourData.getTourDeviceTime_Recorded();

         joinedMovingTime += tourData.getTourComputedTime_Moving();

         joinedDistance += tourData.getTourDistance();
         joinedCalories += tourData.getCalories();

         /*
          * init next tour
          */
         isFirstTour = false;
         joinedTourStartIndex = joinedSerieIndex;
         joinedTourStartDistance += relTourDistance;

         previousTourData = tourData;
      }

      /*
       * setup tour data
       */
      _joinedTourData.setTourStartTime(joinedTourStart);
      _joinedTourData.setTimeZoneId(joinedTourStart.getZone().getId());

      // tour id must be created after the tour date/time is set
      _joinedTourData.createTourId();

      _joinedTourData.setTourTitle(_txtTourTitle.getText());
      _joinedTourData.setTourDescription(joinedDescription.toString());

      _joinedTourData.setTourMarkers(joinedTourMarker);
      _joinedTourData.setWayPoints(joinedWayPoints);
      _joinedTourData.setDeviceName(Messages.Dialog_JoinTours_Label_DeviceName);

      _joinedTourData.setIsDistanceFromSensor(isJoinedDistanceFromSensor);
      _joinedTourData.setIsPowerSensorPresent(isJoinedPowerFromSensor);
      _joinedTourData.setIsPulseSensorPresent(isJoinedPulseFromSensor);

      _joinedTourData.setDeviceTimeInterval(joinedDeviceTimeInterval);

      _joinedTourData.setCalories(joinedCalories);
      _joinedTourData.setCadenceMultiplier(joinedCadenceMultiplier);
      _joinedTourData.setRestPulse(joinedRestPulse);

      _joinedTourData.setWeather(joinedWeather);
      _joinedTourData.setWeather_Clouds(joinedWeatherClouds);
      _joinedTourData.setWeather_Wind_Direction(joinedWeatherWindDirection);
      _joinedTourData.setWeather_Wind_Speed(joinedWeatherWindSpeed);

      _joinedTourData.setTourDeviceTime_Elapsed(joinedRecordedTime + joinedPausedTime);
      _joinedTourData.setTourDeviceTime_Recorded(joinedRecordedTime);
      _joinedTourData.setTourDeviceTime_Paused(joinedPausedTime);
      _joinedTourData.setPausedTime_Start(joinedPausedTime_Start.stream().mapToLong(l -> l).toArray());
      _joinedTourData.setPausedTime_End(joinedPausedTime_End.stream().mapToLong(l -> l).toArray());
      _joinedTourData.setPausedTime_Data(joinedPausedTime_Data.stream().mapToLong(l -> l).toArray());
      _joinedTourData.setTourComputedTime_Moving(joinedMovingTime);
      _joinedTourData.setTourDistance(joinedDistance);

      // !! tour type and tour tags are already set !!

      if (isJoinAltitude) {
         _joinedTourData.altitudeSerie = joinedAltitudeSerie;
      }
      if (isJoinDistance) {
         _joinedTourData.distanceSerie = joinedDistanceSerie;
      }
      if (isJoinCadence) {
         _joinedTourData.setCadenceSerie(joinedCadenceSerie);
      }
      if (isJoinLat) {
         _joinedTourData.latitudeSerie = joinedLatitudeSerie;
      }
      if (isJoinLon) {
         _joinedTourData.longitudeSerie = joinedLongitudeSerie;
      }
      if (isJoinPower) {
         _joinedTourData.setPowerSerie(joinedPowerSerie);
      }
      if (isJoinPulse) {
         _joinedTourData.pulseSerie = joinedPulseSerie;
      }
      if (isJoinSpeed) {
         _joinedTourData.setSpeedSerie(joinedSpeedSerie);
      }

      _joinedTourData.temperatureSerie = joinedTemperatureSerie;

      if (isJoinTime) {
         _joinedTourData.timeSerie = joinedTimeSerie;
      }

      _joinedTourData.computeAltitudeUpDown();
      _joinedTourData.computeComputedValues();

      // set person which is required to save a tour
      _joinedTourData.setTourPerson(getSelectedPerson());

      /*
       * check size of the fields
       */
      if (_joinedTourData.isValidForSave() == false) {
         return false;
      }

      /*
       * TourData.computeComputedValues() creates speed data serie which must be removed that
       * cleanupDataSeries() works correctly
       */
      _joinedTourData.setSpeedSerie(null);

      _joinedTourData.cleanupDataSeries();

      // set joined speed again otherwise it was killed 2 lines above, highly complicated
      if (isJoinSpeed) {
         _joinedTourData.setSpeedSerie(joinedSpeedSerie);
      }

      _joinedTourData = TourManager.saveModifiedTour(_joinedTourData);

      return true;
   }

   private void offsetPausedTimes(final long previousTourStartTime,
                                  final long newTourStartTime,
                                  final TourData previousTourData,
                                  final Long[] pausedTime) {

      for (int index = 0; index < pausedTime.length; ++index) {

         long relativePausedTimeStart = pausedTime[index] - previousTourStartTime;
         if (previousTourData != null) {
            relativePausedTimeStart += previousTourData.getTourDeviceTime_Elapsed() * 1000;
         }
         pausedTime[index] = newTourStartTime + relativePausedTimeStart;
      }
   }

   @Override
   protected void okPressed() {

      if (joinTours() == false) {
         return;
      }

      // state must be set after the tour is saved because the tour type id is set when the tour is saved
      saveState();

      if (_chkDeleteSourceTours.isEnabled() &&
            _chkDeleteSourceTours.getSelection() &&
            _tourProvider instanceof TourBookView) {

         super.close();

         final ActionDeleteTour actionDeleteTours = new ActionDeleteTour((TourBookView) _tourProvider);
         actionDeleteTours.run();
      }

      super.okPressed();
   }

   private void onDispose() {

      UI.disposeResource(_imageLock_Closed);

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
   }

   private void onSelect_Unlock_DeleteSourceTours() {

      _chkDeleteSourceTours.setEnabled(!_chkDeleteSourceTours.isEnabled());

      final boolean isEnabled = _chkDeleteSourceTours.isEnabled();

      _btnUnlockDeleteSourceToursSelection.setText(isEnabled
            ? Messages.Dialog_ModifyTours_Button_LockMultipleToursSelection_Text
            : Messages.Dialog_ModifyTours_Button_UnlockMultipleToursSelection_Text);

      if (!isEnabled) {
         _chkDeleteSourceTours.setSelection(false);
      }

      updateUI_LockUnlockButton();
   }

   private void onSelectTourTitleSource() {

      updateUIFromModel();
      enableControls();
   }

   private void onSelectTourTypeSource() {

      final String stateTourTypeSource = getStateTourTypeSource();

      long joinedTourTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

      if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_FROM_SELECTED_TOURS)) {

         joinedTourTypeId = _tourTypeIdFromSelectedTours;

      } else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_PREVIOUS_JOINED_TOUR)) {

         joinedTourTypeId = _tourTypeIdPreviousJoinedTour;

      } else if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {

         joinedTourTypeId = _tourTypeIdCustom;
      }

      _joinedTourData.setTourType(TourDatabase.getTourType(joinedTourTypeId));

      updateUITourTypeTags();

      enableControls();
   }

   private void restoreFirstTourDateTime() {
      final TourData firstTour = _selectedTours.get(0);
      final ZonedDateTime firstTourStart = firstTour.getTourStartTime();

      _dtTourDate.setDate(
            firstTourStart.getYear(),
            firstTourStart.getMonthValue() - 1,
            firstTourStart.getDayOfMonth());

      _dtTourTime.setTime(
            firstTourStart.getHour(),
            firstTourStart.getMinute(),
            firstTourStart.getSecond());
   }

   private void restoreState() {

      _tourTitleFromCustom = Util.getStateString(
            _state,
            STATE_TOUR_TITLE,
            Messages.Dialog_JoinTours_Label_DefaultTitle);

      // tour title source
      Util.selectStateInCombo(
            _state,
            STATE_TOUR_TITLE_SOURCE,
            ALL_STATES_TOUR_TILE_SOURCE,
            STATE_TOUR_TITLE_SOURCE_FROM_TOUR,
            _cboTourTitleSource);

      // tour type source
      Util.selectStateInCombo(
            _state,
            STATE_TYPE_SOURCE,
            ALL_STATES_TOUR_TYPE,
            STATE_TYPE_SOURCE_FROM_SELECTED_TOURS,
            _cboTourType);

      // join method
      Util.selectStateInCombo(
            _state,
            STATE_JOIN_METHOD,
            ALL_STATES_JOIN_METHOD,
            STATE_JOIN_METHOD_ORIGINAL,
            _cboJoinMethod);

      // description/marker/waypoints
      _chkIncludeDescription.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_DESCRIPTION, true));
      _chkIncludeMarkerWaypoints.setSelection(Util.getStateBoolean(_state, STATE_IS_INCLUDE_MARKER_WAYPOINTS, true));
      _chkCreateTourMarker.setSelection(Util.getStateBoolean(_state, STATE_IS_CREATE_TOUR_MARKER, false));

      /*
       * update UI from selected tours
       */

      restoreFirstTourDateTime();

      /*
       * fill person combo and reselect previous person
       */
      final long statePersonId = Util.getStateLong(_state, STATE_PERSON_ID, -1);

      if (_people == null) {
         final ArrayList<TourPerson> people = PersonManager.getTourPeople();
         _people = people.toArray(new TourPerson[people.size()]);
      }

      int index = 0;
      int personIndex = 0;

      for (final TourPerson person : _people) {

         _cboPerson.add(person.getName());

         if (person.getPersonId() == statePersonId) {
            personIndex = index;
         }

         index++;
      }

      _cboPerson.select(personIndex);

   }

   private void saveState() {

      // tour title
      _state.put(STATE_TOUR_TITLE, _txtTourTitle.getText());
      _state.put(STATE_TOUR_TITLE_SOURCE, getStateTourTitleSource());

      // tour type
      final TourType tourType = _joinedTourData.getTourType();
      _state.put(STATE_TOUR_TYPE_ID, tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId());
      _state.put(STATE_TYPE_SOURCE, getStateTourTypeSource());

      // join method
      _state.put(STATE_JOIN_METHOD, getStateJoinMethod());

      // description/marker
      _state.put(STATE_IS_INCLUDE_DESCRIPTION, _chkIncludeDescription.getSelection());
      _state.put(STATE_IS_INCLUDE_MARKER_WAYPOINTS, _chkIncludeMarkerWaypoints.getSelection());
      _state.put(STATE_IS_CREATE_TOUR_MARKER, _chkCreateTourMarker.getSelection());
      _state.put(STATE_MARKER_TYPE, getStateTourMarker());

      // person
      _state.put(STATE_PERSON_ID, getSelectedPerson().getPersonId());
   }

   @Override
   public void toursAreModified(final ArrayList<TourData> modifiedTours) {

      // check if it's not the correct tour
      if (modifiedTours == null || modifiedTours.isEmpty() || _joinedTourData != modifiedTours.get(0)) {
         return;
      }

      // update custom tour type id
      final String stateTourTypeSource = getStateTourTypeSource();

      if (stateTourTypeSource.equals(STATE_TYPE_SOURCE_CUSTOM)) {
         final TourType tourType = _joinedTourData.getTourType();
         _tourTypeIdCustom = tourType == null ? TourDatabase.ENTITY_IS_NOT_SAVED : tourType.getTypeId();
      }

      // tour type or tags can have been changed within this dialog
      updateUITourTypeTags();
   }

   private void updateUI_LockUnlockButton() {

      final boolean isDarkTheme = net.tourbook.common.UI.isDarkTheme();

      // get default foreground color
      final Color unlockColor = _parent.getForeground();
      final Color lockColor = isDarkTheme ? DialogUtils.LOCK_COLOR_DARK : DialogUtils.LOCK_COLOR_LIGHT;

      _btnUnlockDeleteSourceToursSelection.setForeground(_chkDeleteSourceTours.isEnabled()
            ? unlockColor
            : lockColor);

      // ensure the modified text is fully visible
      _dlgInnerContainer.layout(true, true);
   }

   private void updateUIFromModel() {

      /*
       * tour title
       */
      final String stateTourTitleSource = getStateTourTitleSource();

      String tourTitle = _txtTourTitle.getText();

      if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_FROM_TOUR)) {
         tourTitle = _tourTitleFromTour;
      } else if (stateTourTitleSource.equals(STATE_TOUR_TITLE_SOURCE_CUSTOM)) {
         tourTitle = _tourTitleFromCustom;
      }

      // update ui
      _txtTourTitle.setText(tourTitle);
   }

   /**
    * updates marker which requires that the tour date/time control is set
    *
    * @param isRestoreState
    */
   private void updateUIMarker(final boolean isRestoreState) {

      final ZonedDateTime joinedTourStart = ZonedDateTime.of(
            _dtTourDate.getYear(),
            _dtTourDate.getMonth() + 1,
            _dtTourDate.getDay(),
            _dtTourTime.getHours(),
            _dtTourTime.getMinutes(),
            _dtTourTime.getSeconds(),
            0,
            TimeTools.getDefaultTimeZone());

      /**
       * !!! this list must correspond to the states {@link #ALL_STATES_TOUR_MARKER} !!!
       */
      final String[] markerItems = new String[3];
      markerItems[0] = NLS.bind(
            Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
            joinedTourStart.format(TimeTools.Formatter_Date_S));

      markerItems[1] = NLS.bind(
            Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
            joinedTourStart.format(TimeTools.Formatter_DateTime_S));

      markerItems[2] = NLS.bind(
            Messages.Dialog_JoinTours_ComboText_MarkerTourTime,
            joinedTourStart.format(TimeTools.Formatter_DateTime_F));

      final int selectedMarkerIndex = _cboTourMarker.getSelectionIndex();

      _cboTourMarker.setItems(markerItems);

      if (isRestoreState) {

         // restore from state

         Util.selectStateInCombo(
               _state,
               STATE_MARKER_TYPE,
               ALL_STATES_TOUR_MARKER,
               STATE_MARKER_TYPE_SMALL,
               _cboTourMarker);

      } else {

         // restore selection

         _cboTourMarker.select(selectedMarkerIndex);
      }
   }

   private void updateUITourTypeTags() {

      // tour type/tags
      net.tourbook.ui.UI.updateUI_TourType(_joinedTourData, _lblTourType, true);
      TagManager.updateUI_Tags(_joinedTourData, _lblTourTags);

      // reflow layout that the tags are aligned correctly
      _dlgInnerContainer.layout(true);
   }
}
