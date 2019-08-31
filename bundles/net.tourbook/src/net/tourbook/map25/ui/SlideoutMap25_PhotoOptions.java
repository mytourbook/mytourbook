/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
 * Copyright (C) 2019 Thomas Theussing
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

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25View;

/**
 * Map 2.5D photo properties slideout.
 */
public class SlideoutMap25_PhotoOptions extends ToolbarSlideout {

   private SelectionAdapter   _defaultSelectionListener;

   private SelectionAdapter   _layerSelectionListener;
   
   private FocusListener      _keepOpenListener;
   private PixelConverter     _pc;
   
   private Map25View          _map25View;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkShowLayer_Photo;
   private Button    _chkIsShowPhotoTitle;
   private Button    _chkIsPhotoClustering;

   private Button    _chkUseDraggedKeyboardNavigation;


   /**
    * @param ownerControl
    * @param toolBar
    * @param map25View
    */
   public SlideoutMap25_PhotoOptions(final Control ownerControl,
                                   final ToolBar toolBar,
                                   final Map25View map25View) {

      super(ownerControl, toolBar);

      _map25View = map25View;
   }

   private void createActions() {

   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableActions();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory
               .fillDefaults()//
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);

            createUI_50_Layer(container);
            createUI_80_Other(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      //label.setText(Messages.Slideout_Map25MapOptions_Label_MapOptions);
      label.setText("Slideout_Map25PhotoOptions_Label_PhotoOptions");
      MTFont.setBannerFont(label);
      GridDataFactory
            .fillDefaults()//
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_50_Layer(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      //group.setText(Messages.Slideout_Map25MapOptions_Group_MapLayer);
      group.setText("Slideout_Map25PhotoOptions_Group_PhotoLayer");
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         
         {
            /*
             * Photo
             */
            _chkShowLayer_Photo = new Button(group, SWT.CHECK);
            //_chkShowLayer_Photo.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography);
            _chkShowLayer_Photo.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_Photo");
            //_chkShowLayer_Photo.setToolTipText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_Cartography_Tooltip);
            _chkShowLayer_Photo.setToolTipText("Slideout_Map25PhotoOptions_Checkbox_Layer_Photo_Tooltip");
            _chkShowLayer_Photo.addSelectionListener(_layerSelectionListener);
         }       
         {
            /*
             * Photo Clustering
             */
            _chkIsPhotoClustering = new Button(group, SWT.CHECK);
            //_chkIsPhotoClustering.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_LabelSymbol);
            _chkIsPhotoClustering.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_Clustering");
            _chkIsPhotoClustering.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Photo Title
             */
            _chkIsShowPhotoTitle = new Button(group, SWT.CHECK);
            //_chkIsShowPhotoTitle.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_3DBuilding);
            _chkIsShowPhotoTitle.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_ShowTitle");
            _chkIsShowPhotoTitle.addSelectionListener(_layerSelectionListener);
         }


      }
   }

   private void createUI_80_Other(final Composite parent) {

//      final Composite container = new Composite(parent, SWT.NONE);
//      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
//      GridLayoutFactory
//            .fillDefaults()
//            .numColumns(2)
//            .applyTo(container);
//      {
//         {
            /*
             * Keyboard navigation
             */

            // checkbox
//            _chkUseDraggedKeyboardNavigation = new Button(container, SWT.CHECK);
//            _chkUseDraggedKeyboardNavigation.setText(Messages.Slideout_Map25MapOptions_Checkbox_UseDraggedKeyNavigation);
//            _chkUseDraggedKeyboardNavigation.setToolTipText(Messages.Slideout_Map25MapOptions_Checkbox_UseDraggedKeyNavigation_Tooltip);
//            _chkUseDraggedKeyboardNavigation.addSelectionListener(_defaultSelectionListener);
//            GridDataFactory
//                  .fillDefaults()//
//                  .align(SWT.FILL, SWT.BEGINNING)
//                  .span(2, 1)
//                  .applyTo(_chkUseDraggedKeyboardNavigation);
//         }
//      }
      
   }

   private void enableActions() {

     // final boolean isHillShading = _chkShowLayer_Hillshading.getSelection();

     // _spinnerHillshadingOpacity.setEnabled(isHillShading);

      // force UI update otherwise the slideout UI update is done after the map is updated
      _parent.update();
   }

   private void initUI(final Composite parent) {

      _parent = parent;

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };



      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };

      _layerSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onModify_Layer();
         }
      };


   }

   private void onChangeUI() {

      saveState();

      enableActions();
   }



   private void onModify_Layer() {

      final Map25App mapApp = _map25View.getMapApp();

      mapApp.getLayer_Photo().setEnabled(_chkShowLayer_Photo.getSelection());

      mapApp.getLayer_Label().setEnabled(_chkIsPhotoClustering.getSelection());

      // switching off both building layers
      mapApp.getLayer_Building().setEnabled(_chkIsShowPhotoTitle.getSelection());
      mapApp.getLayer_S3DB().setEnabled(_chkIsShowPhotoTitle.getSelection());    
      
      enableActions();

      mapApp.getMap().updateMap(true);
   }

   private void restoreState() {

      final Map25App mapApp = _map25View.getMapApp();

      _chkShowLayer_Photo.setSelection(mapApp.getLayer_BaseMap().isEnabled());

      _chkIsPhotoClustering.setSelection(mapApp.getLayer_Label().isEnabled());

      _chkIsShowPhotoTitle.setSelection(mapApp.getLayer_Building().isEnabled());   
      
   }

   private void saveState() {
      ;
      //Map25ConfigManager.useDraggedKeyboardNavigation = _chkUseDraggedKeyboardNavigation.getSelection();
   }

}
