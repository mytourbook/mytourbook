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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Tour info tooltip, implemented custom tooltip similar like
 * {@link org.eclipse.ui.internal.dialogs.CustomizePerspectiveDialog} and
 * {@link org.eclipse.jface.viewers.ColumnViewerToolTipSupport}
 */
public class MapProvider_InfoToolTip extends ToolTip {

   private static final int SHELL_MARGIN = 5;

   private MP               _mp;

   private Control          _ttControl;

   private ColumnViewer     _tableViewer;
   private ViewerCell       _viewerCell;

   private boolean          _hasDescription;
   private boolean          _hasTileLayerInfo;
   private String           _tileLayerInfo;

   /*
    * UI resources
    */
   private Color _bgColor;
   private Color _fgColor;

   /*
    * UI controls
    */
   private Composite _ttContainer;

   private Button    _chkIsIncludesHillshading;
   private Button    _chkIsTransparentLayer;

   private Label     _lblMapProviderName;
   private Label     _lblMapProviderId;
   private Label     _lblMapProviderType;
   private Label     _lblOfflineFolder;

   private Text      _txtDescription;
   private Text      _txtLayers;

   public MapProvider_InfoToolTip(final TableViewer tableViewer) {

      super(tableViewer.getTable(), NO_RECREATE, false);

      _ttControl = tableViewer.getTable();
      _tableViewer = tableViewer;

      setHideOnMouseDown(false);
   }

   @Override
   protected void afterHideToolTip(final Event event) {

      super.afterHideToolTip(event);

      _viewerCell = null;
   }

   @Override
   public Composite createToolTipContentArea(final Event event, final Composite parent) {

      setupContent();

      initUI(parent);

      final Composite container = createUI(parent);

      updateUI(_mp);
      updateUI_Layout();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(_fgColor);
      shellContainer.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         _ttContainer.setForeground(_fgColor);
         _ttContainer.setBackground(_bgColor);
         GridLayoutFactory
               .fillDefaults() //
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(_ttContainer);
         {
            createUI_10_MPInfo(_ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI_10_MPInfo(final Composite parent) {

      final int secondColumnIndent = 20;

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).spacing(5, 0).applyTo(container);
      {
         {
            /*
             * Map provider name
             */

            _lblMapProviderName = createUI_LabelValue(container, SWT.LEAD);
            GridDataFactory.fillDefaults().span(4, 1).applyTo(_lblMapProviderName);
            MTFont.setBannerFont(_lblMapProviderName);

            // spacer
            final Label spacer = createUI_Label(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().span(4, 1).grab(true, false).hint(1, 10).applyTo(spacer);
         }

         /*
          * Description
          */
         if (_hasDescription) {

            // label: description
            final Label label = createUI_Label(container, Messages.Map2Provider_Tooltip_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(label);

            // text: description
            _txtDescription = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            _txtDescription.setForeground(_fgColor);
            _txtDescription.setBackground(_bgColor);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .grab(true, false)
                  .applyTo(_txtDescription);

            // spacer
            final Label spacer = createUI_Label(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().span(4, 1).grab(true, false).hint(1, 5).applyTo(spacer);
         }

         /*
          * Layers
          */
         if (_hasTileLayerInfo) {

            // label
            final Label label = createUI_Label(container, Messages.Map2Provider_Tooltip_Label_Layers);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(label);

            // text
            _txtLayers = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            _txtLayers.setForeground(_fgColor);
            _txtLayers.setBackground(_bgColor);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .grab(true, false)
                  .applyTo(_txtLayers);

            // spacer
            final Label spacer = createUI_Label(container, UI.EMPTY_STRING);
            GridDataFactory.fillDefaults().span(4, 1).grab(true, false).hint(1, 5).applyTo(spacer);
         }

         {
            /*
             * Offline folder
             */

            // label: offline folder
            createUI_Label(container, Messages.Map2Provider_Tooltip_Lable_OfflineFolder);

            // text: offline folder
            _lblOfflineFolder = createUI_LabelValue(container, SWT.LEAD);
            GridDataFactory.fillDefaults().applyTo(_lblOfflineFolder);
         }
         {
            /*
             * Checkbox: Includes topo
             */

            _chkIsIncludesHillshading = new Button(container, SWT.CHECK);
            _chkIsIncludesHillshading.setText(Messages.Map2Provider_Tooltip_Checkbox_IncludeHillshading);
            _chkIsIncludesHillshading.setForeground(_fgColor);
            _chkIsIncludesHillshading.setBackground(_bgColor);
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

            createUI_Label(container, Messages.Map2Provider_Tooltip_Lable_MapProviderId);

            _lblMapProviderId = createUI_LabelValue(container, SWT.LEAD);
            GridDataFactory.fillDefaults().applyTo(_lblMapProviderId);

         }
         {
            /*
             * Checkbox: Is Layer
             */

            _chkIsTransparentLayer = new Button(container, SWT.CHECK);
            _chkIsTransparentLayer.setText(Messages.Map2Provider_Tooltip_Checkbox_IsTransparentLayer);
            _chkIsTransparentLayer.setForeground(_fgColor);
            _chkIsTransparentLayer.setBackground(_bgColor);

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsTransparentLayer);
         }
         {
            /*
             * Map provider type
             */

            createUI_Label(container, Messages.Map2Provider_Tooltip_Lable_MapProviderType);

            _lblMapProviderType = createUI_LabelValue(container, SWT.LEAD);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(3, 1)
                  .applyTo(_lblMapProviderType);
         }
      }
   }

   private Label createUI_Label(final Composite parent, final String labelText) {

      final Label label = new Label(parent, SWT.NONE);
      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Label createUI_LabelValue(final Composite parent, final int style) {

      final Label label = new Label(parent, style);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      return label;
   }

   @Override
   public Point getLocation(final Point tipSize, final Event event) {

      // try to position the tooltip at the bottom of the cell
      final ViewerCell cell = _tableViewer.getCell(new Point(event.x, event.y));

      if (cell != null) {

         final Rectangle cellBounds = cell.getBounds();
         final int cellWidth2 = cellBounds.width / 2;
         final int cellHeight = cellBounds.height;

         final int devXDefault = cellBounds.x + cellWidth2;// + cellBounds.width; //event.x;
         final int devY = cellBounds.y + cellHeight;

         /*
          * check if the tooltip is outside of the tree, this can happen when the column is very
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

               ttDisplayLocation = _ttControl.toDisplay(devXDefault - cellWidth2 + 20 - tipSizeWidth, devY);

            } else {
               ttDisplayLocation.x = ttDisplayLocation.x - tipSizeWidth;
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

         final Object cellElement = _viewerCell.getElement();

         if (cellElement instanceof MP) {
            _mp = (MP) cellElement;
         }
      }

      return _viewerCell;
   }

   private void initUI(final Composite parent) {

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
   }

   private void setupContent() {

      _tileLayerInfo = MapProviderManager.getTileLayerInfo(_mp);

      _hasDescription = _mp.getDescription().trim().length() > 0;
      _hasTileLayerInfo = _tileLayerInfo.length() > 0;
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (!super.shouldCreateToolTip(event)) {
         return false;
      }

      if (_viewerCell == null) {
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

   private void updateUI(final MP mapProvider) {

      // common fields
      _chkIsIncludesHillshading.setSelection(mapProvider.isIncludesHillshading());
      _chkIsTransparentLayer.setSelection(mapProvider.isTransparentLayer());
      _lblMapProviderId.setText(mapProvider.getId());
      _lblMapProviderName.setText(mapProvider.getName());

      if (_hasDescription) {
         _txtDescription.setText(mapProvider.getDescription());
      }
      if (_hasTileLayerInfo) {
         _txtLayers.setText(MapProviderManager.getTileLayerInfo(mapProvider));
      }

      // offline folder
      final String tileOSFolder = mapProvider.getOfflineFolder();
      if (tileOSFolder == null) {
         _lblOfflineFolder.setText(UI.EMPTY_STRING);
      } else {
         _lblOfflineFolder.setText(tileOSFolder);
      }

      if (mapProvider instanceof MPWms) {

         // wms map provider

         _lblMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);

      } else if (mapProvider instanceof MPCustom) {

         // custom map provider

         _lblMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);

      } else if (mapProvider instanceof MPProfile) {

         // map profile

         _lblMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);

      } else if (mapProvider instanceof MPPlugin) {

         // plugin map provider

         _lblMapProviderType.setText(Messages.Pref_Map_ProviderType_Plugin);
      }
   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }
}
