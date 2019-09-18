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
package net.tourbook.ui.views.tagging;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.PrefPageTags;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class Slideout_TourTag_Options extends ToolbarSlideout {

   private IDialogSettings      _state;
   private TourTags_View        _tourTags_View;

   private SelectionAdapter     _defaultSelectionListener;

   private ActionOpenPrefDialog _action_PrefDialog;
   private Action               _action_RestoreDefaults;

   /*
    * UI controls
    */
//   private Button  _chkShowBreaktimeValues;

   private Control _ownerControl;

   /**
    * @param ownerControl
    * @param toolBar
    * @param tourTags_View
    * @param state
    * @param tourChart
    * @param gridPrefPrefix
    */
   public Slideout_TourTag_Options(final Control ownerControl,
                                   final ToolBar toolBar,
                                   final TourTags_View tourTags_View,
                                   final IDialogSettings state) {

      super(ownerControl, toolBar);

      _ownerControl = ownerControl;
      _tourTags_View = tourTags_View;
      _state = state;
   }

   private void createActions() {

      /*
       * Action: Restore default
       */
      _action_RestoreDefaults = new Action() {
         @Override
         public void run() {
            resetToDefaults();
         }
      };
      _action_RestoreDefaults.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
      _action_RestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

      _action_PrefDialog = new ActionOpenPrefDialog(Messages.action_tag_open_tagging_structure, PrefPageTags.ID);
      _action_PrefDialog.closeThisTooltip(this);
      _action_PrefDialog.setShell(_ownerControl.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

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
         GridLayoutFactory.fillDefaults()//
               .numColumns(2)
               .applyTo(container);
//         container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
            createUI_20_Controls(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TourTagOptions_Label_Title);
      label.setFont(JFaceResources.getBannerFont());

      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_action_RestoreDefaults);
      tbm.add(_action_PrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()//
            .grab(true, false)
            .span(2, 1)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
//         {
//            /*
//             * Show break time values
//             */
//            _chkShowBreaktimeValues = new Button(container, SWT.CHECK);
//            _chkShowBreaktimeValues.setText(Messages.Tour_Action_ShowBreaktimeValues);
//
//            GridDataFactory.fillDefaults()//
//                  .span(2, 1)
//                  .applyTo(_chkShowBreaktimeValues);
//
//            _chkShowBreaktimeValues.addSelectionListener(_defaultSelectionListener);
//         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   private void onChangeUI() {

      saveState();

   }

   private void resetToDefaults() {

//      final boolean isShowBreaktimeValues = _prefStore.getDefaultBoolean(//
//            ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE);
//
//      _chkShowBreaktimeValues.setSelection(isShowBreaktimeValues);

      onChangeUI();
   }

   private void restoreState() {

//      _chkShowBreaktimeValues.setSelection(tcc.isShowBreaktimeValues);

   }

   private void saveState() {

//      final boolean isShowBreaktimeValues = _chkShowBreaktimeValues.getSelection();
   }

}
