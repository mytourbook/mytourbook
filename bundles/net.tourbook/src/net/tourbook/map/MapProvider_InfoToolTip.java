/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.map;

import de.byteholder.geoclipse.mapprovider.MP;
import de.byteholder.geoclipse.mapprovider.MPCustom;
import de.byteholder.geoclipse.mapprovider.MPPlugin;
import de.byteholder.geoclipse.mapprovider.MPProfile;
import de.byteholder.geoclipse.mapprovider.MPWms;
import de.byteholder.geoclipse.mapprovider.MapProviderManager;
import de.byteholder.geoclipse.preferences.Messages;

import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.util.ToolTip;
import net.tourbook.map2.view.SlideoutMap2_MapProvider;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapProvider_InfoToolTip extends ToolTip {

   private static final int          SHELL_MARGIN = 5;

   private SlideoutMap2_MapProvider _slideout_Map2_MapProvider;

   private MP                        _mp;

   private Control                   _ttControl;

   private ColumnViewer              _tableViewer;
   private ViewerCell                _viewerCell;

   private boolean                   _hasDescription;
   private boolean                   _hasOnlineMap;
   private boolean                   _hasTileLayerInfo;
   private String                    _tileLayerInfo;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Button    _chkIsIncludesHillshading;
   private Button    _chkIsTransparentLayer;

   private Text      _txtDescription;
   private Text      _txtLayers;
   private Text      _txtMapProviderId;
   private Text      _txtMapProviderName;
   private Text      _txtMapProviderType;
   private Text      _txtOfflineFolder;

   private Link      _linkOnlineMap;

   public MapProvider_InfoToolTip(final SlideoutMap2_MapProvider slideout_Map2_MapProvider, final TableViewer tableViewer) {

      super(tableViewer.getTable(), NO_RECREATE, false);

      _slideout_Map2_MapProvider = slideout_Map2_MapProvider;
      _ttControl = tableViewer.getTable();
      _tableViewer = tableViewer;

      setHideOnMouseDown(false);
   }

   public static String getMapProviderType(final MP mapProvider) {

      if (mapProvider instanceof MPWms) {

         // wms map provider

         return Messages.Pref_Map_ProviderType_Wms;

      } else if (mapProvider instanceof MPCustom) {

         // custom map provider

         return Messages.Pref_Map_ProviderType_Custom;

      } else if (mapProvider instanceof MPProfile) {

         // map profile

         return Messages.Pref_Map_ProviderType_MapProfile;

      } else if (mapProvider instanceof MPPlugin) {

         // plugin map provider

         return Messages.Pref_Map_ProviderType_Plugin;
      }

      return UI.EMPTY_STRING;
   }

   @Override
   protected void afterHideToolTip(final Event event) {

      super.afterHideToolTip(event);

      _slideout_Map2_MapProvider.setIsAnotherDialogOpened(false);

      _viewerCell = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      setupContent();

      initUI(parent);

      final Composite container = createUI(parent);

      updateUI_Content(_mp);
      updateUI_Layout();

      /*
       * Prevent that this tooltip is NOT closed when the mouse is hovering the tooltip and this
       * tooltip is not covered by the underlaying slideout, very complicated !!! (but is seams to
       * work :-)
       */
      _slideout_Map2_MapProvider.setIsAnotherDialogOpened(true);

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory.fillDefaults() //
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(_ttContainer);
         {
            createUI_10_MPInfo(_ttContainer);
         }
      }

      final Display display = parent.getDisplay();

      final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      UI.setChildColors(shellContainer.getShell(), fgColor, bgColor);

      return shellContainer;
   }

   private void createUI_10_MPInfo(final Composite parent) {

      final int secondColumnIndent = 20;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).spacing(5, 3).applyTo(container);
      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Map provider name
             */

            // using text control that & is not displayed as mnemonic
            _txtMapProviderName = new Text(container, SWT.READ_ONLY);
            GridDataFactory.fillDefaults()
                  .span(4, 1)

                  // indent to the left that this text is aligned with the labels
                  .indent(-4, 0)

                  .applyTo(_txtMapProviderName);

            MTFont.setBannerFont(_txtMapProviderName);

            // spacer
            final Label spacer = createUI_Label(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().span(4, 1).grab(true, false).hint(1, 10).applyTo(spacer);
         }
         {
            /*
             * Online map
             */
            if (_hasOnlineMap) {

               // label
               final Label label = createUI_Label(container, Messages.Map2Provider_Tooltip_Label_OnlineMap);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(label);

               // link
               _linkOnlineMap = new Link(container, SWT.NONE);
               _linkOnlineMap.addSelectionListener(new SelectionAdapter() {
                  @Override
                  public void widgetSelected(final SelectionEvent e) {
                     WEB.openUrl(_mp.getOnlineMapUrl());
                  }
               });
               GridDataFactory.fillDefaults()
                     .span(3, 1)
                     .grab(true, false)
                     .applyTo(_linkOnlineMap);
            }
         }
         {
            /*
             * Description
             */
            if (_hasDescription) {

               // label
               final Label label = createUI_Label(container, Messages.Map2Provider_Tooltip_Label_Description);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(label);

               // text
               _txtDescription = new Text(container, SWT.READ_ONLY | SWT.WRAP);
               GridDataFactory.fillDefaults()
                     .span(3, 1)
                     .grab(true, false)
                     .applyTo(_txtDescription);

               // spacer
               final Label spacer = createUI_Label(container, UI.EMPTY_STRING);
               GridDataFactory.fillDefaults().span(4, 1).grab(true, false).hint(1, 5).applyTo(spacer);
            }
         }
         {
            /*
             * Layers
             */
            if (_hasTileLayerInfo) {

               // label
               final Label label = createUI_Label(container, Messages.Map2Provider_Tooltip_Label_Layers);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(label);

               // text
               _txtLayers = new Text(container, SWT.READ_ONLY | SWT.WRAP);
               GridDataFactory.fillDefaults()
                     .span(3, 1)
                     .grab(true, false)
                     .applyTo(_txtLayers);
            }
         }
         {
            /*
             * Map provider type
             */

            createUI_Label(container, Messages.Map2Provider_Tooltip_Label_MapProviderType);

            _txtMapProviderType = createUI_TextValue(container);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(3, 1)
                  .applyTo(_txtMapProviderType);
         }
         {
            /*
             * Offline folder
             */

            // label
            createUI_Label(container, Messages.Map2Provider_Tooltip_Label_OfflineFolder);

            // text
            _txtOfflineFolder = createUI_TextValue(container, SWT.LEAD);
         }
         {
            /*
             * Checkbox: Includes topo
             */

            _chkIsIncludesHillshading = new Button(container, SWT.CHECK);
            _chkIsIncludesHillshading.setText(Messages.Map2Provider_Tooltip_Checkbox_IncludeHillshading);
            _chkIsIncludesHillshading.setEnabled(false);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsIncludesHillshading);
         }
         {
            /*
             * Unique id
             */

            createUI_Label(container, Messages.Map2Provider_Tooltip_Label_MapProviderId);

            _txtMapProviderId = createUI_TextValue(container, SWT.LEAD);
         }
         {
            /*
             * Checkbox: Is Layer
             */

            _chkIsTransparentLayer = new Button(container, SWT.CHECK);
            _chkIsTransparentLayer.setText(Messages.Map2Provider_Tooltip_Checkbox_IsTransparentLayer);
            _chkIsTransparentLayer.setEnabled(false);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsTransparentLayer);
         }
      }
   }

   private Label createUI_Label(final Composite parent, final String labelText) {

      final Label label = new Label(parent, SWT.NONE);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Text createUI_TextValue(final Composite parent) {

      return new Text(parent, SWT.READ_ONLY);
   }

   private Text createUI_TextValue(final Composite parent, final int style) {

      return new Text(parent, style);
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      final int mouseX = event.x;

      final Point mousePosition = new Point(mouseX, event.y);

      // try to position the tooltip at the bottom of the cell
      final ViewerCell cell = _tableViewer.getCell(mousePosition);

      if (cell != null) {

         final Rectangle cellBounds = cell.getBounds();
         final int cellWidth2 = (int) (cellBounds.width * 0.5);
         final int cellHeight = cellBounds.height;

         final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
         final int devY = cellBounds.y + cellHeight;

         /*
          * Check if the tooltip is outside of the tree, this can happen when the column is very
          * wide and partly hidden
          */
         final Rectangle treeBounds = _ttControl.getBounds();
         boolean isDevXAdjusted = false;
         int devX = devXDefault;

         if (devXDefault >= treeBounds.width) {
            devX = treeBounds.width - 40;
            isDevXAdjusted = true;
         }

         final Rectangle displayBounds = _ttControl.getDisplay().getBounds();

         Point ttDisplayLocation = _ttControl.toDisplay(devX, devY);
         final int tipSizeWidth = tipSize.x;
         final int tipSizeHeight = tipSize.y;

         if (ttDisplayLocation.x + tipSizeWidth > displayBounds.width) {

            /*
             * adjust horizontal position, it is outside of the display, prevent default
             * repositioning
             */

            if (isDevXAdjusted) {

               final int devXAdjusted = devXDefault - cellWidth2 + 20 - tipSizeWidth;

               ttDisplayLocation = _ttControl.toDisplay(devXAdjusted, devY);

            } else {

               int devXAdjusted = ttDisplayLocation.x - tipSizeWidth;

               if (devXAdjusted + tipSizeWidth + 10 > mouseX) {

                  // prevent that the tooltip of the adjusted x position is below the mouse

                  final Point mouseDisplay = _ttControl.toDisplay(mouseX, devY);

                  devXAdjusted = mouseDisplay.x - tipSizeWidth - 10;
               }

               ttDisplayLocation.x = devXAdjusted;
            }
         }

         if (ttDisplayLocation.y + tipSizeHeight > displayBounds.height) {

            /*
             * adjust vertical position, it is outside of the display, prevent default
             * repositioning
             */

            ttDisplayLocation.y = ttDisplayLocation.y - tipSizeHeight - cellHeight;
         }

         return fixupDisplayBoundsWithMonitor(tipSize, ttDisplayLocation);
      }

      return super.getLocation(tipSize, event);
   }

   @Override
   protected Object getToolTipArea(final Event event) {

      _viewerCell = _tableViewer.getCell(new Point(event.x, event.y));

      if (_viewerCell != null) {

         final CellLabelProvider labelProvider = _tableViewer.getLabelProvider(_viewerCell.getColumnIndex());

         if (labelProvider instanceof SlideoutMap2_MapProvider.TooltipLabelProvider) {

            // show tooltip for this cell

            final Object cellElement = _viewerCell.getElement();

            if (cellElement instanceof MP) {
               _mp = (MP) cellElement;
            }

         } else {

            // tooltip is not dispalyed for this cell

            _viewerCell = null;
         }
      }

      return _viewerCell;
   }

   private void initUI(final Composite parent) {

   }

   private void setMaxContentWidth(final Control control) {

      final int maxContentWidth = 400;

      final GridData gd = (GridData) control.getLayoutData();
      final Point contentSize = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);

      if (contentSize.x > maxContentWidth) {

         // adjust max width
         gd.widthHint = maxContentWidth;

      } else {

         // reset layout width
         gd.widthHint = SWT.DEFAULT;
      }
   }

   private void setupContent() {

      _tileLayerInfo = MapProviderManager.getTileLayerInfo(_mp);

      _hasDescription = _mp.getDescription().trim().length() > 0;
      _hasOnlineMap = _mp.getOnlineMapUrl().trim().length() > 0;
      _hasTileLayerInfo = _tileLayerInfo.length() > 0;
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (!super.shouldCreateToolTip(event)) {
         return false;
      }

      if (_viewerCell == null) {

         // show default tooltip
         _ttControl.setToolTipText(null);

         return false;
      }

      boolean isShowTooltip = false;

      if (_mp == null) {

         // show default tooltip
         _ttControl.setToolTipText(null);

      } else {

         // hide default tooltip and display the custom tooltip
         _ttControl.setToolTipText(UI.EMPTY_STRING);

         isShowTooltip = true;
      }

      return isShowTooltip;
   }

   private void updateUI_Content(final MP mapProvider) {

      // common fields
      _chkIsIncludesHillshading.setSelection(mapProvider.isIncludesHillshading());
      _chkIsTransparentLayer.setSelection(mapProvider.isTransparentLayer());
      _txtMapProviderId.setText(mapProvider.getId());
      _txtMapProviderName.setText(mapProvider.getName());

      if (_hasDescription) {
         _txtDescription.setText(mapProvider.getDescription().trim());
         setMaxContentWidth(_txtDescription);
      }

      if (_hasOnlineMap) {
         _linkOnlineMap.setText(net.tourbook.common.UI.getLinkFromText(mapProvider.getOnlineMapUrl()));
         setMaxContentWidth(_linkOnlineMap);
      }

      if (_hasTileLayerInfo) {
         _txtLayers.setText(MapProviderManager.getTileLayerInfo(mapProvider));
         setMaxContentWidth(_txtLayers);
      }

      // offline folder
      final String tileOSFolder = mapProvider.getOfflineFolder();
      if (tileOSFolder == null) {
         _txtOfflineFolder.setText(UI.EMPTY_STRING);
      } else {
         _txtOfflineFolder.setText(tileOSFolder);
      }

      _txtMapProviderType.setText(MapProviderManager.getMapProvider_TypeLabel(mapProvider));
   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }
}
