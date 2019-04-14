/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import de.byteholder.geoclipse.map.Map;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorSelectorExtended;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.util.ColumnDefinition;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.ITourViewer;
import net.tourbook.common.util.TableColumnDefinition;
import net.tourbook.common.util.Util;
import net.tourbook.map2.view.Map2View;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
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
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IViewPart;

/**
 * Slideout with the tour geo filters.
 */
public class Slideout_TourGeoFilter extends AdvancedSlideout implements ITourViewer, IColorSelectorListener {

   private static final String            COLUMN_CREATED_DATE_TIME   = "createdDateTime";                      //$NON-NLS-1$
   private static final String            COLUMN_FILTER_NAME         = "filterName";                           //$NON-NLS-1$
   private static final String            COLUMN_GEO_PARTS           = "geoParts";                             //$NON-NLS-1$
   private static final String            COLUMN_LATITUDE_1          = "latitude1";                            //$NON-NLS-1$
   private static final String            COLUMN_LATITUDE_2          = "latitude2";                            //$NON-NLS-1$
   private static final String            COLUMN_LONGITUDE_1         = "longitude1";                           //$NON-NLS-1$
   private static final String            COLUMN_LONGITUDE_2         = "longitude2";                           //$NON-NLS-1$
   private static final String            COLUMN_SEQUENCE            = "sequence";                             //$NON-NLS-1$
   private static final String            COLUMN_ZOOM_LEVEL          = "zoomLevel";                            //$NON-NLS-1$

   final static IPreferenceStore          _prefStore                 = TourbookPlugin.getPrefStore();
   private final static IDialogSettings   _state                     = TourGeoFilter_Manager.getState();

   private TableViewer                    _geoFilterViewer;
   private TableColumnDefinition          _colDef_FilterName;
   private ColumnManager                  _columnManager;
   private CompareResultComparator        _geoPartComparator         = new CompareResultComparator();

   private final ArrayList<TourGeoFilter> _allGeoFilter              = TourGeoFilter_Manager.getAllGeoFilter();
   private TourGeoFilter                  _selectedFilter;
   private boolean                        _isSelectPreviousGeoFilter = true;

   private ToolItem                       _tourFilterItem;

   private Action                         _actionRestoreDefaults;

   private SelectionAdapter               _columnSortListener;
   private IPropertyChangeListener        _defaultChangePropertyListener;
   private MouseWheelListener             _mouseWheelListener_WithUpdateUI_WithRepainting;
   private SelectionAdapter               _selectionListener_WithUpdateUI;
   private SelectionAdapter               _selectionListener_WithUpdateUI_WithRepainting;
   private SelectionAdapter               _selectionListener_OnlyStateUpdate;

   private final NumberFormat             _nf2                       = NumberFormat.getInstance();
   {
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
   }

   /*
    * UI controls
    */
   private PixelConverter        _pc;

   private ScrolledComposite     _optionsContainer;
   private Composite             _optionsContainer_Inner;
   private Composite             _viewerContainer;

   private Button                _btnDeleteGeoFilter;
   private Button                _btnDeleteGeoFilterAllWithoutName;

   private Button                _chkIsAutoOpenSlideout;
   private Button                _chkIsSyncMapPosition;
   private Button                _chkIsUseAppFilter;
   private Button                _chkIsFastMapPainting;

   private Button                _rdoGeoParts_Exclude;
   private Button                _rdoGeoParts_Include;

   private Label                 _lblGeoParts_IncludeExclude;

   private Spinner               _spinnerFastPainting_SkippedValues;

   private ColorSelectorExtended _colorGeoPart_HoverSelecting;
   private ColorSelectorExtended _colorGeoPart_Selected;

   private class CompareResultComparator extends ViewerComparator {

      private static final int ASCENDING       = 0;
      private static final int DESCENDING      = 1;

      private String           __sortColumnId  = COLUMN_LATITUDE_1;
      private int              __sortDirection = ASCENDING;

      @Override
      public int compare(final Viewer viewer, final Object e1, final Object e2) {

         final TourGeoFilter geoFilter1 = (TourGeoFilter) e1;
         final TourGeoFilter geoFilter2 = (TourGeoFilter) e2;

         boolean _isSortByTime = true;
         double rc = 0;

         // Determine which column and do the appropriate sort
         switch (__sortColumnId) {

         case COLUMN_GEO_PARTS:
            rc = geoFilter1.numGeoParts - geoFilter2.numGeoParts;
            break;

         case COLUMN_LATITUDE_1:
            rc = geoFilter1.geoLocation_TopLeft.latitude - geoFilter2.geoLocation_TopLeft.latitude;
            break;
         case COLUMN_LONGITUDE_1:
            rc = geoFilter1.geoLocation_TopLeft.longitude - geoFilter2.geoLocation_TopLeft.longitude;
            break;

         case COLUMN_LATITUDE_2:
            rc = geoFilter1.geoLocation_BottomRight.latitude - geoFilter2.geoLocation_BottomRight.latitude;
            break;
         case COLUMN_LONGITUDE_2:
            rc = geoFilter1.geoLocation_BottomRight.longitude - geoFilter2.geoLocation_BottomRight.longitude;
            break;

         case COLUMN_FILTER_NAME:
            rc = geoFilter1.filterName.compareTo(geoFilter2.filterName);
            break;

         case COLUMN_CREATED_DATE_TIME:

            // sorting by date is already set
            break;

         case COLUMN_ZOOM_LEVEL:
            rc = geoFilter1.mapZoomLevel - geoFilter2.mapZoomLevel;
            break;

         default:
            _isSortByTime = true;
         }

         if (rc == 0 && _isSortByTime) {
            rc = geoFilter1.createdMS - geoFilter2.createdMS;
         }

         // if descending order, flip the direction
         if (__sortDirection == DESCENDING) {
            rc = -rc;
         }

         /*
          * MUST return 1 or -1 otherwise long values are not sorted correctly.
          */
         return rc > 0 //
               ? 1
               : rc < 0 //
                     ? -1
                     : 0;
      }

      @Override
      public boolean isSorterProperty(final Object element, final String property) {

         // force resorting when a name is renamed
         return true;
      }

      public void setSortColumn(final Widget widget) {

         final ColumnDefinition columnDefinition = (ColumnDefinition) widget.getData();
         final String columnId = columnDefinition.getColumnId();

         if (columnId.equals(__sortColumnId)) {

            // Same column as last sort; toggle the direction

            __sortDirection = 1 - __sortDirection;

         } else {

            // New column; do an ascent sorting

            __sortColumnId = columnId;
            __sortDirection = ASCENDING;
         }

         updateUI_SetSortDirection(__sortColumnId, __sortDirection);
      }
   }

   private class FilterName_EditingSupport extends EditingSupport {

      private final TableViewer __viewer;
      private final CellEditor  __editor;

      public FilterName_EditingSupport(final TableViewer viewer) {

         super(viewer);

         this.__viewer = viewer;
         this.__editor = new TextCellEditor(viewer.getTable());
      }

      @Override
      protected boolean canEdit(final Object element) {
         return true;
      }

      @Override
      protected CellEditor getCellEditor(final Object element) {
         return __editor;
      }

      @Override
      protected Object getValue(final Object element) {
         return ((TourGeoFilter) element).filterName;
      }

      @Override
      protected void setValue(final Object element, final Object userInputValue) {

         ((TourGeoFilter) element).filterName = (String) userInputValue;

         // update viewer
         __viewer.update(element, null);

         // update name in map
         getToolTipShell().getDisplay().asyncExec(() -> {

            onSelect_GeoFilter(_selectedFilter);
         });
      }
   }

   private class GeoFilterProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allGeoFilter.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public Slideout_TourGeoFilter(final ToolItem toolItem) {

      super(toolItem.getParent(), _state, new int[] { 700, 200, 700, 200 });

      _tourFilterItem = toolItem;

      setShellFadeOutDelaySteps(30);
      setTitleText(Messages.Slideout_TourGeoFilter_Label_Title);
   }

   @Override
   protected void beforeShellVisible(final boolean isVisible) {

      /**
       * Context menu must be set lately, otherwise an "Widget has the wrong parent" exception
       * occures
       */
      if (isVisible) {
         _columnManager.createHeaderContextMenu(_geoFilterViewer.getTable(), null, getRRShellWithResize());
      }
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {
      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new Action() {
         @Override
         public void run() {
            resetToDefaults();
         }
      };

      _actionRestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
      _actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);
   }

   @Override
   protected void createSlideoutContent(final Composite parent) {

      /*
       * Reset to a valid state when the slideout is opened again
       */
      _selectedFilter = null;

      initUI(parent);
      restoreState_BeforeUI();

      createActions();

      // define all columns for the viewer
      _columnManager = new ColumnManager(this, _state);
      defineAllColumns();

      initUI(parent);

      createUI(parent);

      // load viewer
      updateUI_Viewer();

      restoreState();
      enableControls();
   }

   private void createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(shellContainer);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(false, true).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
         {
            createUI_400_Options(container);
            createUI_490_Options_Actions(container);
         }

         createUI_600_FilterViewer(shellContainer);
      }
   }

   private void createUI_400_Options(final Composite parent) {

      /*
       * scrolled container
       */
      _optionsContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      _optionsContainer.setExpandVertical(true);
      _optionsContainer.setExpandHorizontal(true);
      _optionsContainer.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            onResize_Options();
         }
      });
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_optionsContainer);
      {
         _optionsContainer_Inner = new Composite(_optionsContainer, SWT.NONE);
         _optionsContainer_Inner.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_optionsContainer_Inner);
         GridLayoutFactory.fillDefaults().applyTo(_optionsContainer_Inner);
         {
            createUI_410_Options(_optionsContainer_Inner);
         }
      }

      // set content for scrolled composite
      _optionsContainer.setContent(_optionsContainer_Inner);
   }

   private void createUI_410_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
//            .indent(10, 0)
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Radio: Include/exclude geo parts
             */
            // label
            _lblGeoParts_IncludeExclude = new Label(container, SWT.NONE);
            _lblGeoParts_IncludeExclude.setText(Messages.Slideout_TourGeoFilter_Label_FilterIncludeExclude);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblGeoParts_IncludeExclude);

            final Composite radioContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(radioContainer);
            GridLayoutFactory.fillDefaults().numColumns(1).applyTo(radioContainer);
            {
               // Radio: Line
               _rdoGeoParts_Include = new Button(radioContainer, SWT.RADIO);
               _rdoGeoParts_Include.setText(Messages.Slideout_TourGeoFilter_Radio_GeoParts_Include);
               _rdoGeoParts_Include.setToolTipText(Messages.Slideout_TourGeoFilter_Radio_GeoParts_Include_Tooltip);
               _rdoGeoParts_Include.addSelectionListener(_selectionListener_WithUpdateUI);

               // Radio: Dot
               _rdoGeoParts_Exclude = new Button(radioContainer, SWT.RADIO);
               _rdoGeoParts_Exclude.setText(Messages.Slideout_TourGeoFilter_Radio_GeoParts_Exclude);
               _rdoGeoParts_Exclude.setToolTipText(Messages.Slideout_TourGeoFilter_Radio_GeoParts_Exclude_Tooltip);
               _rdoGeoParts_Exclude.addSelectionListener(_selectionListener_WithUpdateUI);
            }
         }
         {
            /*
             * Color: Geo parts
             */
            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourGeoFilter_Label_GeoPartColor);

            // colors
            final Composite containerColors = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerColors);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerColors);
            {

               // hovering/selecting color
               _colorGeoPart_HoverSelecting = new ColorSelectorExtended(containerColors);
               _colorGeoPart_HoverSelecting.getButton().setToolTipText(Messages.Slideout_TourGeoFilter_Color_GeoPartHover_Tooltip);
               _colorGeoPart_HoverSelecting.addListener(_defaultChangePropertyListener);
               _colorGeoPart_HoverSelecting.addOpenListener(this);

               // selected color
               _colorGeoPart_Selected = new ColorSelectorExtended(containerColors);
               _colorGeoPart_Selected.getButton().setToolTipText(Messages.Slideout_TourGeoFilter_Color_GeoPartSelected_Tooltip);
               _colorGeoPart_Selected.addListener(_defaultChangePropertyListener);
               _colorGeoPart_Selected.addOpenListener(this);
            }
         }
         {
            /*
             * Use app filter
             */
            // checkbox
            _chkIsUseAppFilter = new Button(container, SWT.CHECK);
            _chkIsUseAppFilter.setText(Messages.Slideout_TourGeoFilter_Checkbox_UseAppFilter);
            _chkIsUseAppFilter.setToolTipText(Messages.GeoCompare_View_Action_AppFilter_Tooltip);
            _chkIsUseAppFilter.addSelectionListener(_selectionListener_WithUpdateUI);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsUseAppFilter);
         }
         {
            /*
             * Synch map position
             */
            // checkbox
            _chkIsSyncMapPosition = new Button(container, SWT.CHECK);
            _chkIsSyncMapPosition.setText(Messages.Slideout_TourGeoFilter_Checkbox_IsSyncMapPosition);
            _chkIsSyncMapPosition.setToolTipText(Messages.GeoCompare_View_Action_IsSyncMapPosition_Tooltip);
            _chkIsSyncMapPosition.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {

                  saveState_Options();

                  if (_chkIsSyncMapPosition.getSelection()) {
                     // reselect current geo filter
                     if (_selectedFilter != null) {
                        onSelect_GeoFilter(_selectedFilter);
                     }
                  }
               }
            });
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsSyncMapPosition);
         }
         {
            /*
             * Fast map painting
             */
            final Composite containerFastPainting = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .applyTo(containerFastPainting);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerFastPainting);
            {
               // checkbox: is fast painting
               _chkIsFastMapPainting = new Button(containerFastPainting, SWT.CHECK);
               _chkIsFastMapPainting.setText(Messages.Slideout_TourGeoFilter_Checkbox_IsUseFastMapPainting);
               _chkIsFastMapPainting.setToolTipText(Messages.Slideout_TourGeoFilter_Checkbox_IsUseFastMapPainting_Tooltip);
               _chkIsFastMapPainting.addSelectionListener(_selectionListener_WithUpdateUI_WithRepainting);

               // spinner: skipped values
               _spinnerFastPainting_SkippedValues = new Spinner(containerFastPainting, SWT.BORDER);
               _spinnerFastPainting_SkippedValues.setMinimum(0);
               _spinnerFastPainting_SkippedValues.setMaximum(100);
               _spinnerFastPainting_SkippedValues.setPageIncrement(10);
               _spinnerFastPainting_SkippedValues.setToolTipText(Messages.Slideout_TourGeoFilter_Spinner_FastMapPainting_SkippedValues_Tooltip);
               _spinnerFastPainting_SkippedValues.addSelectionListener(_selectionListener_WithUpdateUI_WithRepainting);
               _spinnerFastPainting_SkippedValues.addMouseWheelListener(_mouseWheelListener_WithUpdateUI_WithRepainting);

            }
         }
         {
            /*
             * Open slideout when starting geo search
             */
            // checkbox
            _chkIsAutoOpenSlideout = new Button(container, SWT.CHECK);
            _chkIsAutoOpenSlideout.setText(Messages.Slideout_TourGeoFilter_Checkbox_IsAutoOpenSlideout);
            _chkIsAutoOpenSlideout.setToolTipText(Messages.Slideout_TourGeoFilter_Checkbox_IsAutoOpenSlideout_Tooltip);
            _chkIsAutoOpenSlideout.addSelectionListener(_selectionListener_OnlyStateUpdate);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsAutoOpenSlideout);
         }
         {
            /*
             * Hint
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourGeoFilter_Label_Hint);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .span(2, 1)
                  .align(SWT.FILL, SWT.END)
                  .applyTo(label);
         }
      }
   }

   private void createUI_490_Options_Actions(final Composite parent) {

      {
         /*
          * Restore action
          */
         final ToolBar toolbarUI = new ToolBar(parent, SWT.FLAT);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).applyTo(toolbarUI);

         final ToolBarManager toolbarManager = new ToolBarManager(toolbarUI);

         toolbarManager.add(_actionRestoreDefaults);
         toolbarManager.update(true);
      }
   }

   private void createUI_600_FilterViewer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourGeoFilter_Label_History);
         }
         {
            _viewerContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewerContainer);
            GridLayoutFactory.fillDefaults().applyTo(_viewerContainer);
            {
               createUI_610_FilterViewer_Table(_viewerContainer);
            }
         }

         createUI_680_ViewerActions(container);
      }
   }

   private void createUI_610_FilterViewer_Table(final Composite parent) {

      /*
       * create table
       */
      final Table table = new Table(parent, SWT.FULL_SELECTION /* | SWT.MULTI /* | SWT.BORDER */);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(table);

      table.setHeaderVisible(true);
      table.setLinesVisible(false);

      /*
       * It took a while that the correct listener is set and also the checked item is fired and not
       * the wrong selection.
       */
      table.addListener(SWT.Selection, new Listener() {

         @Override
         public void handleEvent(final Event event) {
//            onGeoPart_Select(event);
         }
      });

      table.addKeyListener(new KeyListener() {

         @Override
         public void keyPressed(final KeyEvent e) {

            switch (e.keyCode) {

            case SWT.DEL:
               onGeoFilter_Delete();
               break;

            default:
               break;
            }
         }

         @Override
         public void keyReleased(final KeyEvent e) {}
      });

      /*
       * create table viewer
       */
      _geoFilterViewer = new TableViewer(table);

      // very important: the editing support must be set BEFORE the columns are created
      _colDef_FilterName.setEditingSupport(new FilterName_EditingSupport(_geoFilterViewer));

      UI.setCellEditSupport(_geoFilterViewer);

      _columnManager.createColumns(_geoFilterViewer);
      _columnManager.setSlideoutShell(this);

      _geoFilterViewer.setUseHashlookup(true);
      _geoFilterViewer.setContentProvider(new GeoFilterProvider());
      _geoFilterViewer.setComparator(_geoPartComparator);

      _geoFilterViewer.addSelectionChangedListener(new ISelectionChangedListener() {

         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onGeoFilter_Select(event);
         }
      });

      _geoFilterViewer.addDoubleClickListener(new IDoubleClickListener() {

         @Override
         public void doubleClick(final DoubleClickEvent event) {
//          onBookmark_Rename(true);
         }
      });

      updateUI_SetSortDirection(//
            _geoPartComparator.__sortColumnId,
            _geoPartComparator.__sortDirection);

      createUI_620_ContextMenu();
   }

   /**
    * Ceate the view context menus
    */
   private void createUI_620_ContextMenu() {

      final MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$

      menuMgr.setRemoveAllWhenShown(true);

      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
//          fillContextMenu(manager);
         }
      });

      /**
       * ColumnManager context menu is set in {@link #beforeHideToolTip()} but this works only until
       * the column manager is recreating the viewer.
       * <p>
       * The slideout must be reopened that the context menu is working again :-(
       */

      /*
       * Set context menu after a viewer reload, even more complicated
       */
      _columnManager.createHeaderContextMenu(_geoFilterViewer.getTable(), null, getRRShellWithResize());

   }

   /**
    * @param parent
    */
   private void createUI_680_ViewerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.FILL)
            .applyTo(container);
      GridLayoutFactory.fillDefaults()
            .margins(0, 0)
            .numColumns(2)
            .applyTo(container);
      {
         {
            /*
             * Button: delete geo filter
             */
            _btnDeleteGeoFilter = new Button(container, SWT.NONE);
            _btnDeleteGeoFilter.setText(Messages.App_Action_Delete);
            _btnDeleteGeoFilter.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onGeoFilter_Delete();
               }
            });
            UI.setButtonLayoutData(_btnDeleteGeoFilter);
         }
         {
            /*
             * Button: delete all geo filter
             */
            _btnDeleteGeoFilterAllWithoutName = new Button(container, SWT.NONE);
            _btnDeleteGeoFilterAllWithoutName.setText(Messages.Slideout_TourGeoFilter_Action_Delete_WithoutName);
            _btnDeleteGeoFilterAllWithoutName.setToolTipText(Messages.Slideout_TourGeoFilter_Action_Delete_WithoutName_Tooltip);
            _btnDeleteGeoFilterAllWithoutName.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onGeoFilter_Delete_AllWithoutName();
               }
            });
            UI.setButtonLayoutData(_btnDeleteGeoFilterAllWithoutName);
         }
      }
   }

   private void defineAllColumns() {

      defineColumn_00_SequenceNumber();
      defineColumn_05_FilterName();
      defineColumn_10_Created();
      defineColumn_20_NumGeoParts();
      defineColumn_30_Zoomlevel();

      defineColumn_50_Latitude1();
      defineColumn_52_Longitude1();

      defineColumn_60_Latitude2();
      defineColumn_62_Longitude2();
   }

   private void defineColumn_00_SequenceNumber() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_SEQUENCE, SWT.TRAIL);

      colDef.setColumnLabel(Messages.GeoCompare_View_Column_SequenceNumber_Label);
      colDef.setColumnHeaderText(Messages.GeoCompare_View_Column_SequenceNumber_Header);
      colDef.setColumnHeaderToolTipText(Messages.GeoCompare_View_Column_SequenceNumber_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(8));

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final int indexOf = _geoFilterViewer.getTable().indexOf((TableItem) cell.getItem());

            cell.setText(Integer.toString(indexOf + 1));
         }
      });

   }

   /**
    * Column: Number of geo parts
    */
   private void defineColumn_05_FilterName() {

      _colDef_FilterName = new TableColumnDefinition(_columnManager, COLUMN_FILTER_NAME, SWT.LEAD);

      _colDef_FilterName.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);
      _colDef_FilterName.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_FilterName_Label);

      _colDef_FilterName.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(12));

      _colDef_FilterName.setIsDefaultColumn();
      _colDef_FilterName.setCanModifyVisibility(false);
      _colDef_FilterName.setColumnSelectionListener(_columnSortListener);

      _colDef_FilterName.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(item.filterName);
         }
      });
   }

   /**
    * Column: Created
    */
   private void defineColumn_10_Created() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_CREATED_DATE_TIME, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Created_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Created_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(20));

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();
            final ZonedDateTime created = item.created;

            cell.setText(created.format(TimeTools.Formatter_DateTime_SM));
         }
      });
   }

   /**
    * Column: Number of geo parts
    */
   private void defineColumn_20_NumGeoParts() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_GEO_PARTS, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_NumGeoParts_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(6));

      colDef.setIsDefaultColumn();
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(Integer.toString(item.numGeoParts));
         }
      });
   }

   /**
    * Column: Zoomlevel
    */
   private void defineColumn_30_Zoomlevel() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_ZOOM_LEVEL, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Map_Bookmark_Column_ZoomLevel_Tooltip);
      colDef.setColumnHeaderText(Messages.Map_Bookmark_Column_ZoomLevel);
      colDef.setColumnHeaderToolTipText(Messages.Map_Bookmark_Column_ZoomLevel_Tooltip);

      colDef.setIsDefaultColumn();
      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(5));
      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(Integer.toString(item.mapZoomLevel + 1));
         }
      });
   }

   /**
    * Column: Latitude 1 - top/left
    */
   private void defineColumn_50_Latitude1() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE_1, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Latitude1_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Latitude1_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Latitude1_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(_nf2.format(item.geoLocation_TopLeft.latitude));
         }
      });
   }

   /**
    * Column: Longitude 1 - top/left
    */
   private void defineColumn_52_Longitude1() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE_1, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Longitude1_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Longitude1_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Longitude1_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(_nf2.format(item.geoLocation_TopLeft.longitude));
         }
      });
   }

   /**
    * Column: Latitude 2 - top/left
    */
   private void defineColumn_60_Latitude2() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LATITUDE_2, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Latitude2_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Latitude2_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Latitude2_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(_nf2.format(item.geoLocation_BottomRight.latitude));
         }
      });
   }

   /**
    * Column: Longitude 2 - top/left
    */
   private void defineColumn_62_Longitude2() {

      final TableColumnDefinition colDef = new TableColumnDefinition(_columnManager, COLUMN_LONGITUDE_2, SWT.TRAIL);

      colDef.setColumnLabel(Messages.Slideout_TourGeoFilter_Column_Longitude2_Label);
      colDef.setColumnHeaderText(Messages.Slideout_TourGeoFilter_Column_Longitude2_Header);
      colDef.setColumnHeaderToolTipText(Messages.Slideout_TourGeoFilter_Column_Longitude2_Label);

      colDef.setDefaultColumnWidth(_pc.convertWidthInCharsToPixels(10));

      colDef.setColumnSelectionListener(_columnSortListener);

      colDef.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final TourGeoFilter item = (TourGeoFilter) cell.getElement();

            cell.setText(_nf2.format(item.geoLocation_BottomRight.longitude));
         }
      });
   }

   private void disposeMapOverlayImages() {

      final IViewPart view = Util.getView(Map2View.ID);
      if (view instanceof Map2View) {

         final Map2View map2View = (Map2View) view;

         final Map map = map2View.getMap();

         map.disposeTiles();
         map.disposeOverlayImageCache();
         map.paint();
      }
   }

   private void enableControls() {

      final boolean isGeoFilterSelected = _selectedFilter != null;

      _btnDeleteGeoFilter.setEnabled(isGeoFilterSelected);
      _btnDeleteGeoFilterAllWithoutName.setEnabled(_allGeoFilter.size() > 0);
   }

   @Override
   public ColumnManager getColumnManager() {
      return _columnManager;
   }

   @Override
   protected Rectangle getParentBounds() {

      final Rectangle itemBounds = _tourFilterItem.getBounds();
      final Point itemDisplayPosition = _tourFilterItem.getParent().toDisplay(itemBounds.x, itemBounds.y);

      itemBounds.x = itemDisplayPosition.x;
      itemBounds.y = itemDisplayPosition.y;

      return itemBounds;
   }

   /**
    * @param sortColumnId
    * @return Returns the column widget by it's column id, when column id is not found then the
    *         first column is returned.
    */
   private TableColumn getSortColumn(final String sortColumnId) {

      final TableColumn[] allColumns = _geoFilterViewer.getTable().getColumns();

      for (final TableColumn column : allColumns) {

         final String columnId = ((ColumnDefinition) column.getData()).getColumnId();

         if (columnId.equals(sortColumnId)) {
            return column;
         }
      }

      return allColumns[0];
   }

   @Override
   public ColumnViewer getViewer() {
      return _geoFilterViewer;
   }

   private StructuredSelection getViewerSelection() {

      return (StructuredSelection) _geoFilterViewer.getSelection();
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _columnSortListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSelect_SortColumn(e);
         }
      };

      _selectionListener_WithUpdateUI = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _selectionListener_WithUpdateUI_WithRepainting = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {

            // state must be saved BEFORE overlay is reseted because this is reading the options
            saveState_Options();

            // force repainting
            disposeMapOverlayImages();

            if (_selectedFilter != null) {
               onSelect_GeoFilter(_selectedFilter);
            }
         }
      };

      _selectionListener_OnlyStateUpdate = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            saveState_Options();
         }
      };

      _defaultChangePropertyListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {
            onChangeUI();
         }
      };

      _mouseWheelListener_WithUpdateUI_WithRepainting = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {

            // force repainting
            disposeMapOverlayImages();

            UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeUI();
         }
      };
//
//      _keepOpenListener = new FocusListener() {
//
//         @Override
//         public void focusGained(final FocusEvent e) {
//
//            /*
//             * This will fix the problem that when the list of a combobox is displayed, then the
//             * slideout will disappear :-(((
//             */
//            setIsKeepOpenInternally(true);
//         }
//
//         @Override
//         public void focusLost(final FocusEvent e) {
//            setIsKeepOpenInternally(false);
//         }
//      };
   }

   private void onChangeUI() {

      saveState_Options();

      if (_selectedFilter != null) {
         onSelect_GeoFilter(_selectedFilter);
      }
   }

   @Override
   protected void onFocus() {

//      if (_selectedProfile != null
//            && _selectedProfile.name != null
//            && _selectedProfile.name.equals(Messages.Tour_Filter_Default_ProfileName)) {
//
//         // default profile is selected, make it easy to rename it
//
//         _txtProfileName.selectAll();
//         _txtProfileName.setFocus();
//
//      } else {
//
//         _profileViewer.getTable().setFocus();
//      }
      _geoFilterViewer.getTable().setFocus();
   }

   private void onGeoFilter_Delete() {

      final Object filterItem = _geoFilterViewer.getStructuredSelection().getFirstElement();

      if (filterItem == null) {
         return;
      }

      // update model
      _allGeoFilter.remove(filterItem);

      // update UI
      final Table filterTable = _geoFilterViewer.getTable();
      final int selectionIndex = filterTable.getSelectionIndex();

      _geoFilterViewer.remove(filterItem);

      // select next filter item
      final int nextIndex = Math.min(filterTable.getItemCount() - 1, selectionIndex);
      if (nextIndex < 0) {

         _selectedFilter = null;

         // fire event to hide geo grid
         TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, null, null);

      } else {

         // set new selection this will also fire the event

         _geoFilterViewer.setSelection(new StructuredSelection(_geoFilterViewer.getElementAt(nextIndex)));
      }

      enableControls();

      // set focus back to table
      _geoFilterViewer.getTable().setFocus();
   }

   private void onGeoFilter_Delete_AllWithoutName() {

      if (_allGeoFilter.size() == 0) {
         return;
      }

//      setIsKeepOpenInternally(true);
//      int confirmDialogResult;
//      {
//         confirmDialogResult = new MessageDialog(
//               getToolTipShell(),
//               Messages.Slideout_TourGeoFilter_Dialog_DeleteAllFilter_Title,
//               null,
//               Messages.Slideout_TourGeoFilter_Dialog_DeleteAllFilter_Message,
//               MessageDialog.QUESTION,
//
//               0, // default index
//
//               Messages.Slideout_TourGeoFilter_Action_Delete_AllWithoutName,
//               IDialogConstants.CANCEL_LABEL
//
//         ).open();
//
//      }
//      setIsKeepOpenInternally(false);
//
//      if (confirmDialogResult != 0) {
//         return;
//      }

      // check if deletion must be confirmed
      if (_prefStore.getBoolean(ITourbookPreferences.TOGGLE_STATE_GEO_FILTER_DELETE_ALL_WITHOUT_NAME) == false) {

         // confirm deletion

         final LinkedHashMap<String, Integer> buttonLabelToIdMap = new LinkedHashMap<>();
         buttonLabelToIdMap.put(Messages.Slideout_TourGeoFilter_Action_Delete_AllWithoutName, IDialogConstants.OK_ID);
         buttonLabelToIdMap.put(Messages.App_Action_Cancel, IDialogConstants.CANCEL_ID);

         final MessageDialogWithToggle dialog = new MessageDialogWithToggle(

               getToolTipShell(),

               Messages.Slideout_TourGeoFilter_Dialog_DeleteAllFilter_Title,
               null,

               Messages.Slideout_TourGeoFilter_Dialog_DeleteAllFilter_Message,
               MessageDialog.QUESTION,

               buttonLabelToIdMap,
               0, // default index

               Messages.App_ToggleState_DoNotShowAgain,
               false // toggle default state
         );

         int dialogReturnCode;

         setIsAnotherDialogOpened(true);
         {
            dialogReturnCode = dialog.open();
         }
         setIsAnotherDialogOpened(false);

         // save toggle state
         _prefStore.setValue(ITourbookPreferences.TOGGLE_STATE_GEO_FILTER_DELETE_ALL_WITHOUT_NAME, dialog.getToggleState());

         if (dialogReturnCode != IDialogConstants.OK_ID) {
            return;
         }
      }

      // update model

      // get all geo filter with name
      final ArrayList<TourGeoFilter> allGeoFilter_WithName = new ArrayList<>();
      for (final TourGeoFilter tourGeoFilter : _allGeoFilter) {
         if (tourGeoFilter.filterName.length() > 0) {
            allGeoFilter_WithName.add(tourGeoFilter);
         }
      }
      _allGeoFilter.clear();
      _allGeoFilter.addAll(allGeoFilter_WithName);

      // update UI
      _geoFilterViewer.refresh();

      // fire event to hide geo grid
      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, null, null);

      enableControls();

      // set focus back to table
      _geoFilterViewer.getTable().setFocus();
   }

   private void onGeoFilter_Select(final SelectionChangedEvent event) {

      final Object selectedItem = event.getStructuredSelection().getFirstElement();
      final TourGeoFilter selectedGeoFilter = (TourGeoFilter) selectedItem;

      onSelect_GeoFilter(selectedGeoFilter);
   }

   private void onResize_Options() {

      // horizontal scroll bar ishidden, only the vertical scrollbar can be displayed
      int infoContainerWidth = _optionsContainer.getBounds().width;
      final ScrollBar vertBar = _optionsContainer.getVerticalBar();

      if (vertBar != null && vertBar.isVisible()) {

         // vertical bar is displayed
         infoContainerWidth -= vertBar.getSize().x;
      }

      final Point minSize = _optionsContainer_Inner.computeSize(infoContainerWidth, SWT.DEFAULT);

      _optionsContainer.setMinSize(minSize);
   }

   private void onSelect_GeoFilter(final TourGeoFilter selectedGeoFilter) {

      _selectedFilter = selectedGeoFilter;

      // show geo grid in the map
      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID, _selectedFilter, null);

      // run tour filter for all opened tour directory views
      TourGeoFilter_Manager.selectFilter(_selectedFilter);

      enableControls();
   }

   private void onSelect_SortColumn(final SelectionEvent e) {

      _viewerContainer.setRedraw(false);
      {
         // keep selection
         final ISelection selectionBackup = getViewerSelection();
         {
            // update viewer with new sorting
            _geoPartComparator.setSortColumn(e.widget);
            _geoFilterViewer.refresh();
         }
         updateUI_SelectGeoFilterItem(selectionBackup);
      }
      _viewerContainer.setRedraw(true);
   }

   @Override
   public ColumnViewer recreateViewer(final ColumnViewer columnViewer) {

      _viewerContainer.setRedraw(false);
      {
         _geoFilterViewer.getTable().dispose();

         createUI_610_FilterViewer_Table(_viewerContainer);
         _viewerContainer.layout();

         // update the viewer
         reloadViewer();
      }
      _viewerContainer.setRedraw(true);

      return _geoFilterViewer;
   }

   public void refreshViewer(final TourGeoFilter selectedFilter) {

      _geoFilterViewer.refresh();

      _geoFilterViewer.setSelection(new StructuredSelection(selectedFilter), true);
      _geoFilterViewer.getTable().showSelection();
   }

   @Override
   public void reloadViewer() {

      updateUI_Viewer();
   }

   private void resetToDefaults() {

   // SET_FORMATTING_OFF

      _rdoGeoParts_Include.setSelection(                 TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS_DEFAULT);
      _rdoGeoParts_Exclude.setSelection(                 TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS_DEFAULT == false);

      _chkIsAutoOpenSlideout.setSelection(               TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT_DEFAULT);
      _chkIsFastMapPainting.setSelection(                TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING_DEFAULT);
      _chkIsSyncMapPosition.setSelection(                TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION_DEFAULT);
      _chkIsUseAppFilter.setSelection(                   TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS_DEFAULT);

      _spinnerFastPainting_SkippedValues.setSelection(   TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES_DEFAULT);

      // colors
      _colorGeoPart_HoverSelecting.setColorValue(        TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER_DEFAULT);
      _colorGeoPart_Selected.setColorValue(              TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED_DEFAULT);

// SET_FORMATTING_ON

   }

   private void restoreState() {

// SET_FORMATTING_OFF

      // options
//      _spinnerGridBoxSize.setSelection(      Util.getStateInt(_state,         TourGeoFilterManager.STATE_GRID_BOX_SIZE,          TourGeoFilterManager.STATE_GRID_BOX_SIZE_DEFAULT));

      final boolean isIncludeGeoParts =      Util.getStateBoolean(_state,     TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS,     TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS_DEFAULT);
      _rdoGeoParts_Include.setSelection(isIncludeGeoParts);
      _rdoGeoParts_Exclude.setSelection(isIncludeGeoParts == false);

      _chkIsAutoOpenSlideout.setSelection(   Util.getStateBoolean(_state,     TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT,    TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT_DEFAULT));
      _chkIsFastMapPainting.setSelection(    Util.getStateBoolean(_state,     TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING,     TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING_DEFAULT));
      _chkIsSyncMapPosition.setSelection(    Util.getStateBoolean(_state,     TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION,     TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION_DEFAULT));
      _chkIsUseAppFilter.setSelection(       Util.getStateBoolean(_state,     TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS,       TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS_DEFAULT));

      _spinnerFastPainting_SkippedValues.setSelection(
            Util.getStateInt(_state,
                  TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES,
                  TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES_DEFAULT));

      // colors
      _colorGeoPart_HoverSelecting.setColorValue(Util.getStateRGB(_state,     TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER,      TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER_DEFAULT));
      _colorGeoPart_Selected.setColorValue(  Util.getStateRGB(_state,         TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED,   TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED_DEFAULT));

// SET_FORMATTING_ON

      if (_isSelectPreviousGeoFilter) {

         // reselect filter item
         final String selectedFilterId = Util.getStateString(_state, TourGeoFilter_Manager.STATE_SELECTED_GEO_FILTER_ID, null);
         if (selectedFilterId != null) {

            for (final TourGeoFilter tourGeoFilterItem : _allGeoFilter) {

               if (tourGeoFilterItem.id.equals(selectedFilterId)) {

                  _geoFilterViewer.setSelection(new StructuredSelection(tourGeoFilterItem), true);
                  _geoFilterViewer.getTable().showSelection();

                  break;
               }
            }
         }
      }
   }

   private void restoreState_BeforeUI() {

      // sorting
      final String sortColumnId = Util.getStateString(_state, TourGeoFilter_Manager.STATE_SORT_COLUMN_ID, COLUMN_CREATED_DATE_TIME);
      final int sortDirection = Util.getStateInt(_state, TourGeoFilter_Manager.STATE_SORT_COLUMN_DIRECTION, CompareResultComparator.DESCENDING);

      // update comparator
      _geoPartComparator.__sortColumnId = sortColumnId;
      _geoPartComparator.__sortDirection = sortDirection;
   }

   @Override
   protected void saveState() {

      // save UI

      if (_selectedFilter != null) {
         _state.put(TourGeoFilter_Manager.STATE_SELECTED_GEO_FILTER_ID, _selectedFilter.id);
      }

      // viewer columns
      _state.put(TourGeoFilter_Manager.STATE_SORT_COLUMN_ID, _geoPartComparator.__sortColumnId);
      _state.put(TourGeoFilter_Manager.STATE_SORT_COLUMN_DIRECTION, _geoPartComparator.__sortDirection);

      _columnManager.saveState(_state);

      super.saveState();
   }

   private void saveState_Options() {

      // save options

      _state.put(TourGeoFilter_Manager.STATE_IS_AUTO_OPEN_SLIDEOUT, _chkIsAutoOpenSlideout.getSelection());
      _state.put(TourGeoFilter_Manager.STATE_IS_FAST_MAP_PAINTING, _chkIsFastMapPainting.getSelection());
      _state.put(TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS, _rdoGeoParts_Include.getSelection());
      _state.put(TourGeoFilter_Manager.STATE_IS_SYNC_MAP_POSITION, _chkIsSyncMapPosition.getSelection());
      _state.put(TourGeoFilter_Manager.STATE_IS_USE_APP_FILTERS, _chkIsUseAppFilter.getSelection());

      _state.put(TourGeoFilter_Manager.STATE_FAST_MAP_PAINTING_SKIPPED_VALUES, _spinnerFastPainting_SkippedValues.getSelection());

      // colors
      Util.setState(_state, TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_HOVER, _colorGeoPart_HoverSelecting.getColorValue());
      Util.setState(_state, TourGeoFilter_Manager.STATE_RGB_GEO_PARTS_SELECTED, _colorGeoPart_Selected.getColorValue());
   }

   public void setIsSelectPreviousGeoFilter(final boolean isSelectPreviousGeoFilter) {
      _isSelectPreviousGeoFilter = isSelectPreviousGeoFilter;
   }

   @Override
   public void updateColumnHeader(final ColumnDefinition colDef) {}

   /**
    * Select and reveal a compare item item.
    *
    * @param selection
    */
   private void updateUI_SelectGeoFilterItem(final ISelection selection) {

//      _isInUpdate = true;
      {
         _geoFilterViewer.setSelection(selection, true);
         _geoFilterViewer.getTable().showSelection();

//       // focus can have changed when resorted, set focus to the selected item
//       int selectedIndex = 0;
//       final Table table = _geoFilterViewer.getTable();
//       final TableItem[] items = table.getItems();
//       for (int itemIndex = 0; itemIndex < items.length; itemIndex++) {
//
//          final TableItem tableItem = items[itemIndex];
//
//          if (tableItem.getData() == selectedProfile) {
//             selectedIndex = itemIndex;
//          }
//       }
//       table.setSelection(selectedIndex);
//       table.showSelection();

      }
//      _isInUpdate = false;
   }

   /**
    * Set the sort column direction indicator for a column.
    *
    * @param sortColumnId
    * @param isAscendingSort
    */
   private void updateUI_SetSortDirection(final String sortColumnId, final int sortDirection) {

      final Table table = _geoFilterViewer.getTable();
      final TableColumn tc = getSortColumn(sortColumnId);

      table.setSortColumn(tc);
      table.setSortDirection(sortDirection == CompareResultComparator.ASCENDING ? SWT.UP : SWT.DOWN);
   }

   private void updateUI_Viewer() {

      _geoFilterViewer.setInput(new Object[0]);
   }

}
