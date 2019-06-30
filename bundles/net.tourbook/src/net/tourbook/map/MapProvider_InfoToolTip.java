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
import net.tourbook.common.util.ToolTip;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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

   private MP             _mp;

   private Control        _ttControl;

   private ColumnViewer   _tableViewer;
   private ViewerCell     _viewerCell;

   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Button _chkIsIncludesHillshading;
   private Button _chkIsTransparentLayer;

   private Label  _lblDescription;
   private Label  _lblOfflineFolderInfo;
   private Label  _lblLayers;

   private Text   _txtDescription;
   private Text   _txtMapProviderName;
   private Text   _txtMapProviderId;
   private Text   _txtMapProviderType;
   private Text   _txtOfflineFolder;
   private Text   _txtLayers;

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

      _pc = new PixelConverter(parent);

      final Composite container = createUI(parent);

      parent.addDisposeListener(new DisposeListener() {
         @Override
         public void widgetDisposed(final DisposeEvent e) {
            container.dispose();
         }
      });

      updateUI_MapProviderInfo(_mp);

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         createUI_10_MPInfo(container);
      }

      return container;
   }

   private void createUI_10_MPInfo(final Composite parent) {

      final int secondColumnIndent = 20;

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
      {
         {
            /*
             * Map provider name
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Lable_MapProvider);

            // text: map provider
            _txtMapProviderName = new Text(container, SWT.None);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_txtMapProviderName);
         }

         {
            /*
             * Description
             */

            // label: description
            _lblDescription = new Label(container, SWT.NONE);
            _lblDescription.setText(Messages.Pref_Map_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(_lblDescription);

            // text: description
            _txtDescription = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(5))
                  .grab(true, false)
                  .applyTo(_txtDescription);
         }
         {
            /*
             * Layers
             */

            // label
            _lblLayers = new Label(container, SWT.NONE);
            _lblLayers.setText(Messages.Pref_Map_Label_Layers);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.TOP).applyTo(_lblLayers);

            // text
            _txtLayers = new Text(container, SWT.READ_ONLY | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .hint(_pc.convertWidthInCharsToPixels(50), _pc.convertHeightInCharsToPixels(5))
                  .grab(true, false)
                  .applyTo(_txtLayers);
         }

         {
            /*
             * Offline folder
             */

            // label: offline folder
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Lable_OfflineFolder);

            // text: offline folder
            _txtOfflineFolder = new Text(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT)
                  .applyTo(_txtOfflineFolder);

            // label: offline info
            _lblOfflineFolderInfo = new Label(container, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .grab(true, false)
                  .align(SWT.END, SWT.CENTER)
                  .applyTo(_lblOfflineFolderInfo);
         }

         {
            /*
             * Unique id
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Lable_MapProviderId);

            // text: map provider id
            _txtMapProviderId = new Text(container, SWT.NONE);
            GridDataFactory.fillDefaults().applyTo(_txtMapProviderId);

         }

         {
            /*
             * Checkbox: Includes topo
             */

            _chkIsIncludesHillshading = new Button(container, SWT.CHECK);
            _chkIsIncludesHillshading.setText(Messages.Pref_Map_Checkbox_IncludeHillshading);
            _chkIsIncludesHillshading.setToolTipText(Messages.Pref_Map_Checkbox_IncludeHillshading_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsIncludesHillshading);
         }

         {
            /*
             * Map provider type
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Pref_Map_Lable_MapProviderType);

            // text: map provider type
            _txtMapProviderType = new Text(container, SWT.READ_ONLY);
            _txtMapProviderType.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            GridDataFactory.fillDefaults().applyTo(_txtMapProviderType);
         }
         {
            /*
             * Checkbox: Is Layer
             */

            _chkIsTransparentLayer = new Button(container, SWT.CHECK);
            _chkIsTransparentLayer.setText(Messages.Pref_Map_Checkbox_IsTransparentLayer);
            _chkIsTransparentLayer.setToolTipText(Messages.Pref_Map_Checkbox_IsTransparentLayer_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(secondColumnIndent, 0)
                  .applyTo(_chkIsTransparentLayer);
         }
      }
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

   private void updateUI_MapProviderInfo(final MP mapProvider) {

      // common fields
      _chkIsIncludesHillshading.setSelection(mapProvider.isIncludesHillshading());
      _chkIsTransparentLayer.setSelection(mapProvider.isTransparentLayer());
      _txtDescription.setText(mapProvider.getDescription());
      _txtMapProviderId.setText(mapProvider.getId());
      _txtMapProviderName.setText(mapProvider.getName());
      _txtLayers.setText(MapProviderManager.getTileLayerInfo(mapProvider));

      // offline folder
      final String tileOSFolder = mapProvider.getOfflineFolder();
      if (tileOSFolder == null) {
         _txtOfflineFolder.setText(UI.EMPTY_STRING);
      } else {
         _txtOfflineFolder.setText(tileOSFolder);
      }

      if (mapProvider instanceof MPWms) {

         // wms map provider

         _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Wms);

      } else if (mapProvider instanceof MPCustom) {

         // custom map provider

         _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Custom);

      } else if (mapProvider instanceof MPProfile) {

         // map profile

         _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_MapProfile);

      } else if (mapProvider instanceof MPPlugin) {

         // plugin map provider

         _txtMapProviderType.setText(Messages.Pref_Map_ProviderType_Plugin);
      }
   }
}
