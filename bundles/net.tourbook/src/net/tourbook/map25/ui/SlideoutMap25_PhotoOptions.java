/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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

import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25ConfigManager;
import net.tourbook.map25.Map25View;
import net.tourbook.map25.layer.marker.MarkerConfig;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

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

   //private Button    _chkIsShowPhoto;
   private Button    _chkIsShowPhotoTitle;
   private Button    _chkIsPhotoClustered;

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
      label.setText("Slideout_Map25PhotoOptions_Label_PhotoOptions"); //$NON-NLS-1$
      MTFont.setBannerFont(label);
      GridDataFactory
            .fillDefaults()//
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_50_Layer(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Slideout_Map25PhotoOptions_Group_PhotoLayer"); //$NON-NLS-1$
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         
//         {
//            /*
//             * Photo
//             */
//            _chkIsShowPhoto = new Button(group, SWT.CHECK);
//            _chkIsShowPhoto.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_Photo");
//            _chkIsShowPhoto.setToolTipText("Slideout_Map25PhotoOptions_Checkbox_Layer_Photo_Tooltip");
//            _chkIsShowPhoto.addSelectionListener(_layerSelectionListener);
//         }       
         {
            /*
             * Photo Clustering
             */
            _chkIsPhotoClustered = new Button(group, SWT.CHECK);
            //_chkIsPhotoClustering.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_LabelSymbol);
            _chkIsPhotoClustered.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_Clustering"); //$NON-NLS-1$
            _chkIsPhotoClustered.addSelectionListener(_layerSelectionListener);
         }
         {
            /*
             * Photo Title
             */
            _chkIsShowPhotoTitle = new Button(group, SWT.CHECK);
            //_chkIsShowPhotoTitle.setText(Messages.Slideout_Map25MapOptions_Checkbox_Layer_3DBuilding);
            _chkIsShowPhotoTitle.setText("Slideout_Map25PhotoOptions_Checkbox_Layer_ShowTitle"); //$NON-NLS-1$
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
      Map25App.debugPrint("slideout: enableActions"); //$NON-NLS-1$
     // final boolean isHillShading = _chkShowLayer_Hillshading.getSelection();

     // _spinnerHillshadingOpacity.setEnabled(isHillShading);

      // force UI update otherwise the slideout UI update is done after the map is updated
      _parent.update();
   }

   private void initUI(final Composite parent) {
      Map25App.debugPrint("slideout: initUI");  //$NON-NLS-1$
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
           _map25View.getMapApp();
         Map25App.debugPrint("slideout: widget selected");  //$NON-NLS-1$
         }
      };


   }

   private void onChangeUI() {
      Map25App.debugPrint("slideout: onChangeUI");  //$NON-NLS-1$
      saveState();

      enableActions();
   }



   private void onModify_Layer() {
      Map25App.debugPrint("slideout: onModify_Layer");  //$NON-NLS-1$
      final Map25App mapApp = _map25View.getMapApp();

     // mapApp.getLayer_Photo().setEnabled(_chkIsShowPhoto.getSelection());

      mapApp.setIsPhotoClustered(_chkIsPhotoClustered.getSelection());
      
      mapApp.setIsPhotoShowTitle(_chkIsShowPhotoTitle.getSelection());

      enableActions();

      mapApp.getMap().updateMap(true);
      mapApp.updateUI_PhotoLayer();
   }

   private void restoreState() {

      final Map25App mapApp = _map25View.getMapApp();

     //_chkIsShowPhoto.setSelection(mapApp.getLayer_Photo().isEnabled());

      _chkIsPhotoClustered.setSelection(mapApp.getIsPhotoClustered());

      _chkIsShowPhotoTitle.setSelection(mapApp.getIsPhotoShowTitle());   
      
   }

   private void saveState() {
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
    //  config.isShowPhoto = _chkIsShowPhoto.getSelection();
      config.isShowPhotoTitle = _chkIsShowPhotoTitle.getSelection();
      config.isPhotoClustered = _chkIsPhotoClustered.getSelection();
      //Map25ConfigManager.useDraggedKeyboardNavigation = _chkUseDraggedKeyboardNavigation.getSelection();
   }

}
