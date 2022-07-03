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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.map25.Map25App;
import net.tourbook.map25.Map25View;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * 2.5D map photo properties slideout
 */
public class SlideoutMap25_PhotoOptions extends ToolbarSlideout {

   public static final int   IMAGE_SIZE_MINIMUM = 20;

   private SelectionListener _defaultSelectionListener;

   private Map25View         _map25View;

   /*
    * UI controls
    */
   private Button  _chkShowPhoto_Title;
   private Button  _chkShowPhoto_Scaled;

   private Spinner _spinnerPhoto_Size;

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

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_20_Photo(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText("Photo Options"); //$NON-NLS-1$
      MTFont.setBannerFont(label);
      GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.CENTER)
            .applyTo(label);
   }

   private void createUI_20_Photo(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText("Photo"); //$NON-NLS-1$
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
      {
         /*
          * Photo Size
          */
         final Composite containerPhotoSize = new Composite(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerPhotoSize);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerPhotoSize);
         {
            {
               _chkShowPhoto_Scaled = new Button(containerPhotoSize, SWT.CHECK);
               _chkShowPhoto_Scaled.setText(Messages.Slideout_Map25PhotoOptions_Checkbox_Layer_Photo_Size);
               _chkShowPhoto_Scaled.addSelectionListener(_defaultSelectionListener);
            }
            {
               /*
                * PhotoSize
                */
               // spinner: fill
               _spinnerPhoto_Size = new Spinner(containerPhotoSize, SWT.BORDER);
               _spinnerPhoto_Size.setMinimum(IMAGE_SIZE_MINIMUM);
               _spinnerPhoto_Size.setMaximum(1000);
               _spinnerPhoto_Size.setIncrement(10);
               _spinnerPhoto_Size.setPageIncrement(50);
               _spinnerPhoto_Size.setToolTipText(Messages.Slideout_Map25PhotoOptions_Spinner_Layer_Photo_Size);

               _spinnerPhoto_Size.addSelectionListener(_defaultSelectionListener);
               _spinnerPhoto_Size.addMouseWheelListener(mouseEvent -> {
                  UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
                  onChangeUI();
               });
            }
         }
      }

      {
         /*
          * Photo Title
          */
         _chkShowPhoto_Title = new Button(group, SWT.CHECK);
         _chkShowPhoto_Title.setText(Messages.Slideout_Map25PhotoOptions_Checkbox_Photo_Title);
         _chkShowPhoto_Title.addSelectionListener(_defaultSelectionListener);
      }
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      // updade model
      saveState();

      // update UI
      updateUI();
   }

   private void restoreState() {

      final Map25App mapApp = _map25View.getMapApp();

      _chkShowPhoto_Title.setSelection(mapApp.isPhoto_ShowTitle());
      _chkShowPhoto_Scaled.setSelection(mapApp.isPhoto_Scaled());

      _spinnerPhoto_Size.setSelection(Math.max(IMAGE_SIZE_MINIMUM, mapApp.getPhoto_Size()));
   }

   private void saveState() {

      final Map25App mapApp = _map25View.getMapApp();

      mapApp.setPhoto_IsShowTitle(_chkShowPhoto_Title.getSelection());
      mapApp.setPhoto_IsScaled(_chkShowPhoto_Scaled.getSelection());
      mapApp.setPhoto_Size(_spinnerPhoto_Size.getSelection());
   }

   private void updateUI() {

      final Map25App mapApp = _map25View.getMapApp();

      mapApp.updateLayer_Photos();
      mapApp.updateMap();
   }

}
