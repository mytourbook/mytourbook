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
package net.tourbook.map3.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
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
import net.tourbook.common.tooltip.AnimatedToolTipShell;
import net.tourbook.common.util.Util;
import net.tourbook.map.MapManager;
import net.tourbook.map2.view.TourMapPainter;
import net.tourbook.map3.Messages;
import net.tourbook.map3.view.Map3View;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageMap25_Map3_Color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
 * Map3 tour track layer properties dialog.
 */
public class DialogSelectMap3Color extends AnimatedToolTipShell implements IMap3ColorUpdater {

   private static final int COLUMN_WITH_ABSOLUTE_RELATIVE = 4;
   private static final int COLUMN_WITH_COLOR_IMAGE       = 15;
   private static final int COLUMN_WITH_NAME              = 15;
   private static final int COLUMN_WITH_VALUE             = 8;

   private static int       PROFILE_IMAGE_HEIGHT          = -1;

   private IDialogSettings  _state;

   private int              _numVisibleRows               = MapManager.STATE_VISIBLE_COLOR_PROFILES_DEFAULT;

   // initialize with default values which are (should) never be used
   private Rectangle          _toolTipItemBounds = new Rectangle(0, 0, 50, 50);

   private final WaitTimer    _waitTimer         = new WaitTimer();

   private MapGraphId         _graphId;
   private Map3View           _map3View;

   private MouseWheelListener _defaultMouseWheelListener;
   private SelectionListener  _defaultSelectionListener;

   private boolean            _canOpenToolTip;
   private boolean            _isWaitTimerStarted;

   private boolean            _isInUIUpdate;
   private boolean            _isInFireEvent;

   private int                _columnIndexProfileImage;

   private Action             _actionAddColor;
   private Action             _actionEditAllColors;
   private Action             _actionEditSelectedColor;

   /*
    * UI resources
    */
   private PixelConverter                            _pc;
   private HashMap<Map3GradientColorProvider, Image> _profileImages = new HashMap<>();
   private CheckboxTableViewer                       _colorViewer;

   /*
    * UI controls
    */
   private Composite   _tableContainer;

   private Spinner     _spinnerNumVisibleProfiles;

   private TableColumn _tableColumn_ProfileImage;

   private final class WaitTimer implements Runnable {
      @Override
      public void run() {
         open_Runnable();
      }
   }

   public DialogSelectMap3Color(final Control ownerControl,
                                final ToolBar toolBar,
                                final Map3View map3View,
                                final MapGraphId graphId) {

      super(ownerControl);

      _graphId = graphId;
      _map3View = map3View;

      _state = map3View.getState();

      addListener(toolBar);

      setToolTipCreateStyle(AnimatedToolTipShell.TOOLTIP_STYLE_KEEP_CONTENT);
      setBehaviourOnMouseOver(AnimatedToolTipShell.MOUSE_OVER_BEHAVIOUR_IGNORE_OWNER);
      setIsKeepShellOpenWhenMoved(false);
      setFadeInSteps(1);
      setFadeOutSteps(10);
      setFadeOutDelaySteps(1);
   }

   private void actionAddColor() {

      final Object selectedItem = ((IStructuredSelection) _colorViewer.getSelection()).getFirstElement();

      final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;
      final Map3GradientColorProvider duplicatedColorProvider = selectedColorProvider.clone();

      // create a new profile name by setting it to the profile id which is unique
      duplicatedColorProvider.getMap3ColorProfile().setDuplicatedName();

      close();

      new DialogMap3ColorEditor(//
            _map3View.getShell(),
            duplicatedColorProvider,
            this,
            true).open();

   }

   private void actionEditAllColors() {

      close();

      PreferencesUtil.createPreferenceDialogOn(//
            _map3View.getShell(),
            PrefPageMap25_Map3_Color.ID,
            null,
            _graphId).open();
   }

   private void actionEditSelectedColor() {

      final IStructuredSelection selection = (IStructuredSelection) _colorViewer.getSelection();

      final Object selectedItem = selection.getFirstElement();
      final Map3GradientColorProvider selectedColorProvider = (Map3GradientColorProvider) selectedItem;

      close();

      new DialogMap3ColorEditor(//
            _map3View.getShell(),
            selectedColorProvider,
            this,
            false).open();
   }

   private void addListener(final ToolBar toolBar) {

      // prevent to open the tooltip
      toolBar.addMouseTrackListener(MouseTrackListener.mouseExitAdapter(mouseEvent -> _canOpenToolTip = false));

//		ownerControl.addDisposeListener(new DisposeListener() {
//			@Override
//			public void widgetDisposed(final DisposeEvent e) {
//				onDispose();
//			}
//		});
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

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   private void createActions() {

      /*
       * Action: Add color
       */
      _actionAddColor = new Action() {
         @Override
         public void run() {
            actionAddColor();
         }
      };
      _actionAddColor.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Add));
      _actionAddColor.setToolTipText(Messages.Map3SelectColor_Dialog_Action_AddColor_Tooltip);

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
      _actionEditSelectedColor.setToolTipText(Messages.Map3SelectColor_Dialog_Action_EditSelectedColors);

      /*
       * Action: Edit all colors.
       */
      _actionEditAllColors = new Action() {
         @Override
         public void run() {
            actionEditAllColors();
         }
      };
      _actionEditAllColors.setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Options));
      _actionEditAllColors.setToolTipText(Messages.Map3SelectColor_Dialog_Action_EditAllColors);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      restoreState_BeforeUI();

      final Composite ui = createUI(parent);

      restoreState();

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
//		_shellContainer.setBackground(UI.SYS_COLOR_RED);
      {
         createUI_10_Title(shellContainer);
         createUI_20_ColorViewer(shellContainer);
         createUI_30_Options(shellContainer);
      }

      // set color for all controls, the dark theme is already painting in dark colors
      if (UI.IS_DARK_THEME == false) {

         final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
         final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
         final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);

         UI.setChildColors(shellContainer, fgColor, bgColor);
      }

      shellContainer.addDisposeListener(disposeEvent -> onDispose());

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      {
         /*
          * Label: Title
          */
         final Label title = new Label(parent, SWT.LEAD);
         title.setText(NLS.bind(OtherMessages.SLIDEOUT_MAP_TRACK_COLORS_LABEL_TITLE, getSlideoutTitle()));
         MTFont.setBannerFont(title);
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(title);
      }
   }

   private void createUI_20_ColorViewer(final Composite parent) {

      final List<Map3GradientColorProvider> colorProviders = Map3GradientColorManager.getColorProviders(_graphId);

      int tableStyle;
      if (colorProviders.size() > _numVisibleRows) {

         tableStyle = SWT.CHECK
               | SWT.FULL_SELECTION
               | SWT.V_SCROLL
               | SWT.NO_SCROLL;
      } else {

         // table contains less than maximum entries, scroll is not necessary

         tableStyle = SWT.CHECK
               | SWT.FULL_SELECTION
               | SWT.NO_SCROLL;
      }

      _tableContainer = new Composite(parent, SWT.NONE);

      GridLayoutFactory.fillDefaults().applyTo(_tableContainer);

      setUI_TableLayout(_tableContainer);

      /*
       * create table
       */
      final Table table = new Table(_tableContainer, tableStyle);

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

      _colorViewer.addCheckStateListener(event -> onViewerCheckStateChange(event));

      _colorViewer.addSelectionChangedListener(selectionChangedEvent -> onViewerSelectColor());

      _colorViewer.addDoubleClickListener(doubleClickEvent -> actionEditSelectedColor());
   }

   private void createUI_30_Options(final Composite parent) {

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
               label.setText(OtherMessages.SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES);
               label.setToolTipText(OtherMessages.SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES_TOOLTIP);

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
    */
   private void defineColumn_10_Checkbox() {

      final TableViewerColumn tvc = new TableViewerColumn(_colorViewer, SWT.LEAD);

      final TableColumn tc = tvc.getColumn();
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_NAME));

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
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_VALUE));

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
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_COLOR_IMAGE));

      _tableColumn_ProfileImage = tc;
      _columnIndexProfileImage = _colorViewer.getTable().getColumnCount() - 1;

      tc.addControlListener(ControlListener.controlResizedAdapter(controlEvent -> onResizeImageColumn()));

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
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_VALUE));

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
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_ABSOLUTE_RELATIVE));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues()) {
                  cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Absolute);
               } else {
                  cell.setText(Messages.Pref_Map3Color_Column_ValueMarker_Relative);
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
      tc.setWidth(_pc.convertWidthInCharsToPixels(COLUMN_WITH_ABSOLUTE_RELATIVE));

      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof Map3GradientColorProvider) {

               final Map3ColorProfile colorProfile = ((Map3GradientColorProvider) (element)).getMap3ColorProfile();

               if (colorProfile.isAbsoluteValues() && colorProfile.isOverwriteLegendValues()) {
                  cell.setText(Messages.Pref_Map3Color_Column_Legend_Marker);
               } else {
                  cell.setText(UI.EMPTY_STRING);
               }

            } else {

               cell.setText(UI.EMPTY_STRING);
            }
         }
      });
   }

   /**
    * @return Returns <code>true</code> when the colors are disposed, otherwise <code>false</code>.
    */
   public boolean disposeColors() {

      if (_isInFireEvent) {

         // reload the viewer
         updateUI_ColorViewer();

         return false;
      }

      disposeProfileImages();

      return true;
   }

   private void disposeProfileImages() {

      for (final Image profileImage : _profileImages.values()) {
         profileImage.dispose();
      }

      _profileImages.clear();
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

         final int imageWidth = _tableColumn_ProfileImage.getWidth();
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

         UI.disposeResource(oldImage);
      }

      return image;
   }

   private String getSlideoutTitle() {

      switch (_graphId) {

      case Altitude:
         return OtherMessages.GRAPH_LABEL_ALTITUDE;

      case Gradient:
         return OtherMessages.GRAPH_LABEL_GRADIENT;

      case HrZone:
         return OtherMessages.GRAPH_LABEL_HR_ZONE;

      case Pace:
         return OtherMessages.GRAPH_LABEL_PACE;

      case Pulse:
         return OtherMessages.GRAPH_LABEL_HEARTBEAT;

      case Speed:
         return OtherMessages.GRAPH_LABEL_SPEED;

      default:
         break;
      }

      return UI.EMPTY_STRING;
   }

   @Override
   public Point getToolTipLocation(final Point tipSize) {

//		final int tipWidth = tipSize.x;
//
//		final int itemWidth = _toolTipItemBounds.width;
      final int itemHeight = _toolTipItemBounds.height;

      // center horizontally
      final int devX = _toolTipItemBounds.x;// + itemWidth / 2 - tipWidth / 2;
      final int devY = _toolTipItemBounds.y + itemHeight + 0;

      return new Point(devX, devY);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      PROFILE_IMAGE_HEIGHT = (int) (_pc.convertHeightInCharsToPixels(1) * 1.0);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   /**
    * @param image
    *
    * @return Returns <code>true</code> when the image is valid, returns <code>false</code> when
    *         the profile image must be created,
    */
   private boolean isProfileImageValid(final Image image) {

      if (image == null || image.isDisposed()) {

         return false;

      }

      return true;
   }

   @Override
   protected Rectangle noHideOnMouseMove() {

      return _toolTipItemBounds;
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
   public void onDispose() {

      disposeProfileImages();
   }

   private void onResizeImageColumn() {

      // recreate images
      disposeProfileImages();
   }

   private void onViewerCheckStateChange(final CheckStateChangedEvent event) {

      final Object viewerItem = event.getElement();

      if (viewerItem instanceof final Map3GradientColorProvider colorProvider) {

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

      if (element instanceof final Map3GradientColorProvider mgrColorProvider) {

         // set checked only active color providers

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

//			event.width += getImageColumnWidth();
//			event.height = PROFILE_IMAGE_HEIGHT;

            break;

         case SWT.PaintItem:

            final TableItem item = (TableItem) event.item;
            final Object itemData = item.getData();

            if (itemData instanceof final Map3GradientColorProvider colorProvider) {

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

      if (selectedItem instanceof final Map3GradientColorProvider selectedColorProvider) {

         setActiveColorProvider(selectedColorProvider);
      }
   }

   /**
    * @param toolTipItemBounds
    * @param isOpenDelayed
    */
   public void open(final Rectangle toolTipItemBounds, final boolean isOpenDelayed) {

      if (isToolTipVisible()) {
         return;
      }

      if (isOpenDelayed == false) {

         if (toolTipItemBounds != null) {

            _toolTipItemBounds = toolTipItemBounds;

            showToolTip();
         }

      } else {

         if (toolTipItemBounds == null) {

            // item is not hovered any more

            _canOpenToolTip = false;

            return;
         }

         _toolTipItemBounds = toolTipItemBounds;
         _canOpenToolTip = true;

         if (_isWaitTimerStarted == false) {

            _isWaitTimerStarted = true;

            Display.getCurrent().timerExec(50, _waitTimer);
         }
      }
   }

   private void open_Runnable() {

      _isWaitTimerStarted = false;

      if (_canOpenToolTip) {
         showToolTip();
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
    *
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
