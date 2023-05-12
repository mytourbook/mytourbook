/*******************************************************************************
 * Copyright (C) 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map25.ui;

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorProviderConfig;
import net.tourbook.common.color.Map3ColorDefinition;
import net.tourbook.common.color.Map3ColorProfile;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.Map3ProfileComparator;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.common.color.ProfileImage;
import net.tourbook.common.color.RGBVertex;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapManager;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map25.Map25View;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap25_Map3_Color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Slideout for 2.5D track colors
 */
public class SlideoutMap25_TrackColors extends ToolbarSlideout implements IMap3ColorUpdater {

// SET_FORMATTING_OFF

   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_ADD_COLOR_TOOLTIP    = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_AddColor_Tooltip;
   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_ALL_COLORS      = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_EditAllColors;
   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_SELECTED_COLORS = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_EditSelectedColors;
   private static final String PREF_MAP3_COLOR_COLUMN_OVERWRITE_LEGEND_MIN_MAX      = net.tourbook.map3.Messages.Pref_Map3Color_Column_OverwriteLegendMinMax_Label;
   private static final String PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_ABSOLUTE_DETAIL  = net.tourbook.map3.Messages.Pref_Map3Color_Column_ValueMarker_Absolute_Detail;
   private static final String PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_RELATIVE_DETAIL  = net.tourbook.map3.Messages.Pref_Map3Color_Column_ValueMarker_Relative_Detail;

// SET_FORMATTING_ON

   private static IDialogSettings _state;

   private static final int       COLUMN_WIDTH_COLOR_IMAGE = 15;
   private static final int       COLUMN_WIDTH_NAME        = 15;
   private static final int       COLUMN_WIDTH_VALUE       = 8;

   private static int             PROFILE_IMAGE_HEIGHT     = -1;

   private int                    _numVisibleRows          = MapManager.STATE_VISIBLE_COLOR_PROFILES_DEFAULT;

   private Map25View              _map25View;

   private CheckboxTableViewer    _colorViewer;
   private TableColumn            _tcProfileImage;

   private Action                 _actionAddColor;
   private Action                 _actionEditSelectedColor;
   private Action                 _actionEditAllColors;

   private SelectionAdapter       _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   private boolean                _isInUIUpdate;

   private int                    _columnIndexProfileImage;
   private MapGraphId             _graphId;

   private PixelConverter         _pc;

   /*
    * UI resources
    */
   private HashMap<Map3GradientColorProvider, Image> _allProfileImages = new HashMap<>();

   /*
    * UI controls
    */
   private Composite _tableContainer;

   private Spinner   _spinnerNumVisibleProfiles;

   public SlideoutMap25_TrackColors(final Composite ownerControl,
                                    final ToolBar toolbar,
                                    final Map25View map25View,
                                    final MapGraphId graphId,
                                    final IDialogSettings state) {

      super(ownerControl, toolbar);

      _map25View = map25View;
      _graphId = graphId;
      _state = state;
   }

   private void actionAddColor() {

      final Object selectedItem = ((IStructuredSelection) _colorViewer.getSelection()).getFirstElement();

      final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;
      final Map3GradientColorProvider duplicatedColorProvider = selectedColorProvider.clone();

      // create a new profile name by setting it to the profile id which is unique
      duplicatedColorProvider.getMap3ColorProfile().setDuplicatedName();

      close();

      new DialogMap3ColorEditor(
            _map25View.getShell(),
            duplicatedColorProvider,
            this,
            true).open();

   }

   private void actionEditAllColors() {

      close();

      PreferencesUtil.createPreferenceDialogOn(
            _map25View.getShell(),
            PrefPageMap25_Map3_Color.ID,
            null,
            _graphId).open();
   }

   private void actionEditSelectedColor() {

      final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

      final Object selectedItem = selection.getFirstElement();
      final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

      close();

      new DialogMap3ColorEditor(
            _map25View.getShell(),
            selectedColorProvider,
            this,
            false).open();
   }

   @Override
   public void applyMapColors(final Map3GradientColorProvider originalCP,
                              final Map3GradientColorProvider modifiedCP,
                              final boolean isNewColorProvider) {

      /*
       * Update model
       */
      if (isNewColorProvider) {

         // a new profile is edited
         Map3GradientColorManager.addColorProvider(modifiedCP);

      } else {

         // an existing profile is modified
         Map3GradientColorManager.replaceColorProvider(originalCP, modifiedCP);
      }

      // fire event that color has changed
      TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
   }

   private void createActions() {

      {
         /*
          * Action: Add color
          */
         _actionAddColor = new Action() {
            @Override
            public void run() {
               actionAddColor();
            }
         };

         _actionAddColor.setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Add));
         _actionAddColor.setToolTipText(MAP3_SELECT_COLOR_DIALOG_ACTION_ADD_COLOR_TOOLTIP);
      }
      {
         /*
          * Action: Edit selected color
          */
         _actionEditSelectedColor = new Action() {
            @Override
            public void run() {
               actionEditSelectedColor();
            }
         };

         _actionEditSelectedColor.setImageDescriptor(net.tourbook.ui.UI.getGraphImageDescriptor(_graphId));
         _actionEditSelectedColor.setToolTipText(MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_SELECTED_COLORS);
      }
      {
         /*
          * Action: Edit all colors
          */
         _actionEditAllColors = new Action() {
            @Override
            public void run() {
               actionEditAllColors();
            }
         };

         _actionEditAllColors.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Options));
         _actionEditAllColors.setToolTipText(MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_ALL_COLORS);
      }
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      restoreState_BeforeUI();

      final Composite ui = createUI(parent);

      restoreState();

      // must be run async otherwise the image width is 0 -> exception
      parent.getDisplay().asyncExec(() -> {
         updateUI_ColorViewer();
      });

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(2, 2)
            .spacing(0, 3)
            .applyTo(shellContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_RED);
      {
         createUI_00_Title(shellContainer);
         createUI_10_ColorViewer(shellContainer);
         createUI_20_Options(shellContainer);
      }

      // set color for all controls, the dark theme is already painting in dark colors
      if (UI.IS_DARK_THEME == false) {

         final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
         final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
         final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

         net.tourbook.common.UI.setChildColors(shellContainer, fgColor, bgColor);
      }

      shellContainer.addDisposeListener(disposeEvent -> onDispose());

      return shellContainer;
   }

   private void createUI_00_Title(final Composite parent) {

      {
         /*
          * Label: Title
          */
         final Label title = new Label(parent, SWT.LEAD);
         title.setText(NLS.bind(Messages.Slideout_Map_TrackColors_Label_Title, getSlideoutTitle()));
         MTFont.setBannerFont(title);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(title);
      }
   }

   private void createUI_10_ColorViewer(final Composite parent) {

      final int numColorProviders = Map3GradientColorManager.getColorProviders(_graphId).size();

      int tableStyle;
      if (numColorProviders > _numVisibleRows) {

         tableStyle = SWT.CHECK
               | SWT.FULL_SELECTION
               | SWT.V_SCROLL
               | SWT.NO_SCROLL;
      } else {

         // table contains less than or equal maximum entries, scroll is not necessary

         tableStyle = SWT.CHECK
               | SWT.FULL_SELECTION
               | SWT.NO_SCROLL;
      }

      final TableLayout tableLayout = new TableLayout();
      final TableColumnLayout columnLayout = new TableColumnLayout();

      _tableContainer = new Composite(parent, SWT.NONE);
      _tableContainer.setLayout(columnLayout);

      setUI_TableLayout(_tableContainer);

      /*
       * create table
       */
      final Table table = new Table(_tableContainer, tableStyle);
      table.setLayout(tableLayout);

      /*
       * NOTE: MeasureItem, PaintItem and EraseItem are called repeatedly. Therefore, it is
       * critical for performance that these methods be as efficient as possible.
       */
      final Listener paintListener = event -> {

         if (event.type == SWT.MeasureItem || event.type == SWT.PaintItem) {
            onViewerPaint(event);
         }
      };
      table.addListener(SWT.MeasureItem, paintListener);
      table.addListener(SWT.PaintItem, paintListener);

      _colorViewer = new CheckboxTableViewer(table);

      /*
       * create columns
       */
      defineColumn_10_Checkbox(columnLayout);
      defineColumn_20_MinValue(columnLayout);
      defineColumn_30_ColorImage(columnLayout);
      defineColumn_40_MaxValue(columnLayout);
      defineColumn_50_RelativeAbsolute(columnLayout);
      defineColumn_52_OverwriteLegendMinMax(columnLayout);

      _colorViewer.setComparator(new Map3ProfileComparator());

      _colorViewer.setContentProvider(new IStructuredContentProvider() {

         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {

            return Map3GradientColorManager.getColorProviders(_graphId).toArray(new Map3GradientColorProvider[numColorProviders]);
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _colorViewer.setCheckStateProvider(new ICheckStateProvider() {

         @Override
         public boolean isChecked(final Object element) {
            return onViewerIsChecked(element);
         }

         @Override
         public boolean isGrayed(final Object element) {
            return onViewerIsGrayed(element);
         }
      });

      _colorViewer.addCheckStateListener(this::onViewerCheckStateChange);
      _colorViewer.addSelectionChangedListener(selectionChangedEvent -> onViewerSelectColor());
      _colorViewer.addDoubleClickListener(doubleClickEvent -> actionEditSelectedColor());
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .extendedMargins(2, 0, 3, 2)
            .applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {

         final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

         final ToolBarManager tbm = new ToolBarManager(toolbar);

         tbm.add(_actionAddColor);
         tbm.add(_actionEditSelectedColor);
         tbm.add(_actionEditAllColors);

         tbm.update(true);
      }
      {
         final Composite containerOptions = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).align(SWT.END, SWT.FILL).applyTo(containerOptions);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerOptions);
         {
            {
               /*
                * Number of visible color profiles
                */

               // label
               final Label label = new Label(containerOptions, SWT.NONE);
               label.setText(Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles);
               label.setToolTipText(Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles_Tooltip);

               // spinner
               _spinnerNumVisibleProfiles = new Spinner(containerOptions, SWT.BORDER);
               _spinnerNumVisibleProfiles.setMinimum(0);
               _spinnerNumVisibleProfiles.setMaximum(100);
               _spinnerNumVisibleProfiles.setPageIncrement(5);
               _spinnerNumVisibleProfiles.addSelectionListener(_defaultSelectionListener);
               _spinnerNumVisibleProfiles.addMouseWheelListener(_defaultMouseWheelListener);
            }
         }
      }
   }

   /**
    * Column: Show only the checkbox
    *
    * @param tableLayout
    */
   private void defineColumn_10_Checkbox(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               cell.setText(colorProfile.getProfileName());
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_NAME), true));
   }

   /**
    * Column: Min value
    */
   private void defineColumn_20_MinValue(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               final ProfileImage profileImage = colorProfile.getProfileImage();

               final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
               final RGBVertex firstVertex = vertices.get(0);

               final String minValueText = Integer.toString(firstVertex.getValue());

               cell.setText(minValueText);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_VALUE), true));
   }

   /**
    * Column: Color image
    */
   private void defineColumn_30_ColorImage(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();

      _tcProfileImage = tc;
      _columnIndexProfileImage = _colorViewer.getTable().getColumnCount() - 1;

      tc.addControlListener(controlResizedAdapter(controlEvent -> onResizeImageColumn()));

      tvc.setLabelProvider(new CellLabelProvider() {

         // !!! set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });

      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_COLOR_IMAGE), true));
   }

   /**
    * Column: Max value
    */
   private void defineColumn_40_MaxValue(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final String maxValueText;
               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               final ProfileImage profileImage = colorProfile.getProfileImage();

               final ArrayList<RGBVertex> vertices = profileImage.getRgbVertices();
               final RGBVertex lastVertex = vertices.get(vertices.size() - 1);

               maxValueText = Integer.toString(lastVertex.getValue());

               cell.setText(maxValueText);

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_VALUE), true));
   }

   /**
    * Column: Relative/absolute values
    */
   private void defineColumn_50_RelativeAbsolute(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues()) {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_ABSOLUTE_DETAIL);
               } else {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_RELATIVE_DETAIL);
               }

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(10), true));
   }

   /**
    * Column: Legend overwrite min/max
    */
   private void defineColumn_52_OverwriteLegendMinMax(final TableColumnLayout tableLayout) {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues() && colorProfile.isOverwriteLegendValues()) {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_OVERWRITE_LEGEND_MIN_MAX);
               } else {
                  cell.setText(UI.EMPTY_STRING);
               }

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
      tableLayout.setColumnData(tc, new ColumnPixelData(_pc.convertWidthInCharsToPixels(10), true));
   }

   private void disposeProfileImages() {

      for (final Image profileImage : _allProfileImages.values()) {
         profileImage.dispose();
      }

      _allProfileImages.clear();
   }

   /**
    * Fire event that 3D map colors have changed.
    */
   private void fireModifyEvent() {

//      _isInFireEvent = true;
      {
         TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
      }
//      _isInFireEvent = false;
   }

   private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

      Image image = _allProfileImages.get(colorProvider);

      if (isProfileImageValid(image)) {

         // image is OK

      } else {

         // create new image

         final int imageWidth = _tcProfileImage.getWidth();
         final int imageHeight = PROFILE_IMAGE_HEIGHT - 1;

         final Map3ColorProfile colorProfile = colorProvider.getMap3ColorProfile();
         final ArrayList<RGBVertex> rgbVertices = colorProfile.getProfileImage().getRgbVertices();

         colorProvider.configureColorProvider(
               ColorProviderConfig.MAP3_PROFILE,
               imageWidth,
               rgbVertices,
               false);

         image = TourMapPainter.createMap3_LegendImage(
               colorProvider,
               ColorProviderConfig.MAP3_PROFILE,
               imageWidth,
               imageHeight,
               false, // horizontal
               false, // no unit
               net.tourbook.common.UI.IS_DARK_THEME, // is dark background
               false // no shadow
         );

         final Image oldImage = _allProfileImages.put(colorProvider, image);

         UI.disposeResource(oldImage);
      }

      return image;
   }

   private String getSlideoutTitle() {

      switch (_graphId) {

      case Altitude:
         return Messages.Graph_Label_Altitude;

      case Gradient:
         return Messages.Graph_Label_Gradient;

      case HrZone:
         return Messages.Graph_Label_HrZone;

      case Pace:
         return Messages.Graph_Label_Pace;

      case Pulse:
         return Messages.Graph_Label_Heartbeat;

      case Speed:
         return Messages.Graph_Label_Speed;

      default:
         break;
      }

      return UI.EMPTY_STRING;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      PROFILE_IMAGE_HEIGHT = _pc.convertHeightInCharsToPixels(1);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = new MouseWheelListener() {
         @Override
         public void mouseScrolled(final MouseEvent event) {
            UI.adjustSpinnerValueOnMouseScroll(event);
            onChangeUI();
         }
      };
   }

   /**
    * @param image
    * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
    *         the profile image must be created,
    */
   private boolean isProfileImageValid(final Image image) {

      if (image == null || image.isDisposed()) {

         return false;

      }

      return true;
   }

   private void onChangeUI() {

      saveState();

      /*
       * Update UI with new number of visible rows
       */
      restoreState_BeforeUI();

      setUI_TableLayout(_tableContainer);

      final Shell shell = _colorViewer.getTable().getShell();
      shell.pack(true);
   }

   @Override
   protected void onDispose() {

      disposeProfileImages();

      super.onDispose();
   }

   private void onResizeImageColumn() {

      // recreate images
      disposeProfileImages();
   }

   private void onViewerCheckStateChange(final CheckStateChangedEvent event) {

      final Object viewerItem = event.getElement();

      if (viewerItem instanceof Map3GradientColorProvider) {

         final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) viewerItem;

         if (event.getChecked()) {

            // set as active color provider

            setActiveColorProvider(colorProvider);

         } else {

            // a color provider cannot be unchecked, to be unchecked, another color provider must be checked

            _colorViewer.setChecked(colorProvider, true);
         }
      }

   }

   private boolean onViewerIsChecked(final Object element) {

      if (element instanceof Map3GradientColorProvider) {

         // set checked only active color providers

         final Map3GradientColorProvider mgrColorProvider = (Map3GradientColorProvider) element;
         final boolean isActiveColorProfile = mgrColorProvider.getMap3ColorProfile().isActiveColorProfile();

         return isActiveColorProfile;
      }

      return false;
   }

   private boolean onViewerIsGrayed(final Object element) {

      if (element instanceof Map3ColorDefinition) {
         return true;
      }

      return false;
   }

   private void onViewerPaint(final Event event) {

      // paint images at the correct column
      if (event.index == _columnIndexProfileImage) {

         switch (event.type) {
         case SWT.MeasureItem:

//       event.width += getImageColumnWidth();
//       event.height = PROFILE_IMAGE_HEIGHT;

            break;

         case SWT.PaintItem:

            final TableItem item = (TableItem) event.item;
            final Object itemData = item.getData();

            if (itemData instanceof Map3GradientColorProvider) {

               final Map3GradientColorProvider colorProvider = (Map3GradientColorProvider) itemData;

               final Image image = getProfileImage(colorProvider);

               if (image != null) {

                  final Rectangle rect = image.getBounds();

                  final int x = event.x + event.width;
                  final int yOffset = Math.max(0, (event.height - rect.height) / 2);

                  event.gc.drawImage(image, x, event.y + yOffset);
               }
            }

            break;
         }
      }
   }

   /**
    * Is called when a color in the color viewer is selected.
    */
   private void onViewerSelectColor() {

      if (_isInUIUpdate) {
         return;
      }

      final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();
      final Object selectedItem = selection.getFirstElement();

      if (selectedItem instanceof Map3GradientColorProvider) {

         final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

         setActiveColorProvider(selectedColorProvider);
      }
   }

   private void restoreState() {

      _spinnerNumVisibleProfiles.setSelection(_numVisibleRows);
   }

   private void restoreState_BeforeUI() {

      _numVisibleRows = Util.getStateInt(_state, MapManager.STATE_VISIBLE_COLOR_PROFILES, MapManager.STATE_VISIBLE_COLOR_PROFILES_DEFAULT);
   }

   private void saveState() {

      _state.put(MapManager.STATE_VISIBLE_COLOR_PROFILES, _spinnerNumVisibleProfiles.getSelection());
   }

   /**
    * @param selectedColorProvider
    * @return Returns <code>true</code> when a new color provider is set, otherwise
    *         <code>false</code>.
    */
   private boolean setActiveColorProvider(final Map3GradientColorProvider selectedColorProvider) {

      final Map3ColorProfile selectedColorProfile = selectedColorProvider.getMap3ColorProfile();

      // check if the selected color provider is already the active color provider
      if (selectedColorProfile.isActiveColorProfile()) {
         return false;
      }

      final MapGraphId graphId = selectedColorProvider.getGraphId();
      final Map3ColorDefinition colorDefinition = Map3GradientColorManager.getColorDefinition(graphId);

      final List<Map3GradientColorProvider> allGraphIdColorProvider = colorDefinition.getColorProviders();

      if (allGraphIdColorProvider.size() < 2) {

         // this case should need no attention

      } else {

         // set selected color provider as active color provider

         // reset state for previous color provider
         final Map3GradientColorProvider oldActiveColorProvider = Map3GradientColorManager
               .getActiveMap3ColorProvider(graphId);
         _colorViewer.setChecked(oldActiveColorProvider, false);

         // set state for selected color provider
         _colorViewer.setChecked(selectedColorProvider, true);

         // set new active color provider
         Map3GradientColorManager.setActiveColorProvider(selectedColorProvider);

         _isInUIUpdate = true;
         {
            // also select the active (checked) color provider
            _colorViewer.setSelection(new StructuredSelection(selectedColorProvider));
         }
         _isInUIUpdate = false;

         fireModifyEvent();

         return true;
      }

      return false;
   }

   private void setUI_TableLayout(final Composite tableLayoutContainer) {

      final int numColorProviders = Map3GradientColorManager.getColorProviders(_graphId).size();

      final int numVisibleRows = _numVisibleRows == 0

            // show all color provider
            ? numColorProviders

            : Math.min(numColorProviders, _numVisibleRows);

      GridDataFactory.fillDefaults()

            .hint(SWT.DEFAULT,
                  (int) (_pc.convertHeightInCharsToPixels(numVisibleRows) * 1.35))

            .applyTo(tableLayoutContainer);
   }

   private void updateUI_ColorViewer() {

      if (_colorViewer.getTable().isDisposed()) {
         return;
      }

      _colorViewer.setInput(this);

      /*
       * Select checked color provider that the actions can always be enabled
       */
      for (final Map3GradientColorProvider colorProvider : Map3GradientColorManager.getColorProviders(_graphId)) {

         if (colorProvider.getMap3ColorProfile().isActiveColorProfile()) {

            /**
             * !!! Reveal and table.showSelection() do NOT work !!!
             */
            _colorViewer.setSelection(new StructuredSelection(colorProvider), true);

            _colorViewer.getTable().showSelection();

            break;
         }
      }
   }

}
