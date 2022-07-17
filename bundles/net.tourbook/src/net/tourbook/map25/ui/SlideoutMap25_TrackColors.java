/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;
import net.tourbook.map3.ui.DialogMap3ColorEditor;
import net.tourbook.map3.ui.IMap3ColorUpdater;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap25_Map3_Color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Slideout for 2.5D tour track configuration
 */
public class SlideoutMap25_TrackColors extends ToolbarSlideout implements IMap3ColorUpdater {

// SET_FORMATTING_OFF

   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_ADD_COLOR_TOOLTIP    = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_AddColor_Tooltip;
   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_ALL_COLORS      = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_EditAllColors;
   private static final String MAP3_SELECT_COLOR_DIALOG_ACTION_EDIT_SELECTED_COLORS = net.tourbook.map3.Messages.Map3SelectColor_Dialog_Action_EditSelectedColors;
   private static final String PREF_MAP3_COLOR_COLUMN_LEGEND_MARKER                 = net.tourbook.map3.Messages.Pref_Map3Color_Column_Legend_Marker;
   private static final String PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_RELATIVE         = net.tourbook.map3.Messages.Pref_Map3Color_Column_ValueMarker_Relative;
   private static final String PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_ABSOLUTE         = net.tourbook.map3.Messages.Pref_Map3Color_Column_ValueMarker_Absolute;

// SET_FORMATTING_ON

   private static final IPreferenceStore _prefStore                     = TourbookPlugin.getPrefStore();

   private static final int              COLUMN_WIDTH_ABSOLUTE_RELATIVE = 4;
   private static final int              COLUMN_WIDTH_COLOR_IMAGE       = 15;
   private static final int              COLUMN_WIDTH_NAME              = 15;
   private static final int              COLUMN_WIDTH_VALUE             = 8;

   private static int                    NUMBER_OF_VISIBLE_ROWS         = 6;
   private static int                    PROFILE_IMAGE_HEIGHT           = -1;

   private Map25View                     _map25View;

   private CheckboxTableViewer           _colorViewer;
   private TableColumn                   _tcProfileImage;

   private Action                        _actionAddColor;
   private Action                        _actionEditSelectedColor;
   private Action                        _actionEditAllColors;

   private FocusListener                 _keepOpenListener;

   private boolean                       _isUpdateUI;
   private boolean                       _isInUIUpdate;
   private boolean                       _isInFireEvent;

   private int                           _columnIndexProfileImage;
   private MapGraphId                    _graphId;

   private PixelConverter                _pc;

   /*
    * UI resources
    */
   private HashMap<Map3GradientColorProvider, Image> _profileImages = new HashMap<>();

   /*
    * UI controls
    */
   private Composite _shellContainer;

   public SlideoutMap25_TrackColors(final Composite ownerControl,
                                    final ToolBar toolbar,
                                    final Map25View map25View,
                                    final MapGraphId graphId) {

      super(ownerControl, toolbar);

      _map25View = map25View;
      _graphId = graphId;
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

      final Composite ui = createUI(parent);

      fillUI();
      restoreState();
      enableControls();

      updateUI_colorViewer();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .margins(UI.SHELL_MARGIN, UI.SHELL_MARGIN)
            .spacing(0, 5)
            .applyTo(_shellContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_RED);
      {
         createUI_00_Title(_shellContainer);
         createUI_10_ColorViewer(_shellContainer);
         createUI_20_Actions(_shellContainer);
      }

      // set color for all controls
      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

      net.tourbook.common.UI.setChildColors(_shellContainer, fgColor, bgColor);

      _shellContainer.addDisposeListener(disposeEvent -> onDispose());

      return _shellContainer;
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

      final List<Map3GradientColorProvider> colorProviders = Map3GradientColorManager.getColorProviders(_graphId);

      int tableStyle;
      if (colorProviders.size() > NUMBER_OF_VISIBLE_ROWS) {

         tableStyle = SWT.CHECK //
               | SWT.FULL_SELECTION
               //          | SWT.H_SCROLL
               | SWT.V_SCROLL
               | SWT.NO_SCROLL;
      } else {

         // table contains less than maximum entries, scroll is not necessary

         tableStyle = SWT.CHECK //
               | SWT.FULL_SELECTION
               | SWT.NO_SCROLL;
      }

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * create table
          */
         final Table table = new Table(container, tableStyle);

         GridDataFactory.fillDefaults().grab(true, true).applyTo(table);
         table.setHeaderVisible(false);
         table.setLinesVisible(false);

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

         /*
          * Set maximum number of visible rows
          */
         table.addControlListener(controlResizedAdapter(controlEvent -> {

            final int itemHeight = table.getItemHeight();
            final int maxHeight = itemHeight * NUMBER_OF_VISIBLE_ROWS;

            final int defaultHeight = table.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

            if (defaultHeight > maxHeight) {

               final GridData gd = (GridData) container.getLayoutData();
               gd.heightHint = maxHeight;

               container.layout(true, true);
            }
         }));

         _colorViewer = new CheckboxTableViewer(table);

         /*
          * create columns
          */
         defineColumn_10_Checkbox();
         defineColumn_20_MinValue();
         defineColumn_30_ColorImage();
         defineColumn_40_MaxValue();
         defineColumn_50_RelativeAbsolute();
         defineColumn_52_OverwriteLegendMinMax();

         _colorViewer.setComparator(new Map3ProfileComparator());

         _colorViewer.setContentProvider(new IStructuredContentProvider() {

            @Override
            public void dispose() {}

            @Override
            public Object[] getElements(final Object inputElement) {

               return colorProviders.toArray(new Map3GradientColorProvider[colorProviders.size()]);
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
   }

   private void createUI_20_Actions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .extendedMargins(2, 0, 3, 2)
            .applyTo(container);
      {

         final ToolBar toolbar = new ToolBar(container, SWT.FLAT);

         final ToolBarManager tbm = new ToolBarManager(toolbar);

         tbm.add(_actionAddColor);
         tbm.add(_actionEditSelectedColor);
         tbm.add(_actionEditAllColors);

         tbm.update(true);
      }
   }

   /**
    * Column: Show only the checkbox
    */
   private void defineColumn_10_Checkbox() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_NAME));

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
   }

   /**
    * Column: Min value
    */
   private void defineColumn_20_MinValue() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_VALUE));

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
   }

   /**
    * Column: Color image
    */
   private void defineColumn_30_ColorImage() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_COLOR_IMAGE));

      _tcProfileImage = tc;
      _columnIndexProfileImage = _colorViewer.getTable().getColumnCount() - 1;

      tc.addControlListener(controlResizedAdapter(controlEvent -> onResizeImageColumn()));

      tvc.setLabelProvider(new CellLabelProvider() {

         // !!! set dummy label provider, otherwise an error occurs !!!
         @Override
         public void update(final ViewerCell cell) {}
      });
   }

   /**
    * Column: Max value
    */
   private void defineColumn_40_MaxValue() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_VALUE));

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
   }

   /**
    * Column: Relative/absolute values
    */
   private void defineColumn_50_RelativeAbsolute() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_ABSOLUTE_RELATIVE));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues()) {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_ABSOLUTE);
               } else {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_VALUE_MARKER_RELATIVE);
               }

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * Column: Legend overwrite marker
    */
   private void defineColumn_52_OverwriteLegendMinMax() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.TRAIL);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WIDTH_ABSOLUTE_RELATIVE));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues() && colorProfile.isOverwriteLegendValues()) {
                  cell.setText(PREF_MAP3_COLOR_COLUMN_LEGEND_MARKER);
               } else {
                  cell.setText(UI.EMPTY_STRING);
               }

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   private void disposeProfileImages() {

      for (final Image profileImage : _profileImages.values()) {
         profileImage.dispose();
      }

      _profileImages.clear();
   }

   private void enableControls() {

   }

   private void fillUI() {

      final boolean backupIsUpdateUI = _isUpdateUI;
      _isUpdateUI = true;
      {

      }
      _isUpdateUI = backupIsUpdateUI;
   }

   /**
    * Fire event that 3D map colors have changed.
    */
   private void fireModifyEvent() {

      _isInFireEvent = true;
      {
         TourbookPlugin.getPrefStore().setValue(ITourbookPreferences.MAP3_COLOR_IS_MODIFIED, Math.random());
      }
      _isInFireEvent = false;
   }

   private Image getProfileImage(final Map3GradientColorProvider colorProvider) {

      Image image = _profileImages.get(colorProvider);

      if (isProfileImageValid(image)) {

         // image is OK

      } else {

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

         final Image oldImage = _profileImages.put(colorProvider, image);

         Util.disposeResource(oldImage);
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

      PROFILE_IMAGE_HEIGHT = (int) (_pc.convertHeightInCharsToPixels(1) * 1.0);
      NUMBER_OF_VISIBLE_ROWS = _prefStore.getInt(ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS);

      /*
       * This will fix the problem that when the list of a combobox is displayed, then the
       * slideout will disappear :-(((
       */
      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
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

   /**
    * Restores state values from the tour track configuration and update the UI.
    */
   private void restoreState() {

      _isUpdateUI = true;

      final Map25TrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

      // get active config AFTER getting the index because this could change the active config
      final int activeConfigIndex = Map25ConfigManager.getActiveTourTrackConfigIndex();

// SET_FORMATTING_OFF


// SET_FORMATTING_ON

      _isUpdateUI = false;
   }

   private void saveState() {

      // update config

      final Map25TrackConfig config = Map25ConfigManager.getActiveTourTrackConfig();

// SET_FORMATTING_OFF


// SET_FORMATTING_ON
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

   private void updateUI_colorViewer() {

      _colorViewer.setInput(this);

      /*
       * Select checked color provider that the actions can always be enabled.
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
