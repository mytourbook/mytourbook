/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TableLayoutComposite;
import net.tourbook.ui.action.TourAction;
import net.tourbook.ui.action.TourActionManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class PrefPageAppearance_TourActions extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID                 = "net.tourbook.preferences.PrefPageAppearance_TourActions"; //$NON-NLS-1$

   private static final String           LINK_ID_TAGS       = "tags";                                                    //$NON-NLS-1$
   private static final String           LINK_ID_TOUR_TYPES = "tourTypes";                                               //$NON-NLS-1$

   private static final IPreferenceStore _prefStore         = TourbookPlugin.getPrefStore();

   private List<TourAction>              _allSortedActions  = new ArrayList<>();
   private Set<String>                   _allViewActionIDs;

   private CheckboxTableViewer           _tourActionViewer;
   private ActionFilter                  _actionFilter      = new ActionFilter();

   private SelectionListener             _defaultSelectionListener;
   private IPropertyChangeListener       _prefChangeListener;

   private PixelConverter                _pc;

   /*
    * UI controls
    */
   private Button _btnCheckAll;
   private Button _btnUncheckAll;
   private Button _btnUp;
   private Button _btnDown;

   private Button _chkShowOnlyAvailableActions;

   private Button _rdoShowAllActions;
   private Button _rdoShowCustomActions;

   private Label  _lblOptions;

   private Link   _linkOptions_Tags;
   private Link   _linkOptions_TourTypes;

   private class ActionFilter extends ViewerFilter {

      @Override
      public boolean select(final Viewer viewer, final Object parentElement, final Object element) {

         if (element instanceof final TourAction tourAction) {

            if (tourAction.isCategory

                  // show only available actions
                  || _allViewActionIDs != null && _allViewActionIDs.contains(tourAction.actionClassName)) {

               return true;
            }
         }

         return false;
      }
   }

   private final class ActionViewer_ContentProvider implements IStructuredContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getElements(final Object inputElement) {
         return _allSortedActions.toArray();
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.VIEW_LAYOUT_CHANGED)) {

            _tourActionViewer.getTable().setLinesVisible(_prefStore.getBoolean(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES));

            _tourActionViewer.refresh();

            /*
             * the tree must be redrawn because the styled text does not display the new color
             */
            _tourActionViewer.getTable().redraw();
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   @Override
   public void applyData(final Object data) {

      if (data instanceof final String viewID) {

         /*
          * Make actions more visible which are available in a view
          */
         _allViewActionIDs = TourActionManager.getAllViewActions().get(viewID);

         if (_allViewActionIDs != null) {

            _tourActionViewer.refresh();
         }
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Control ui = createUI(parent);

      addPrefListener();

      restoreState();

      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         /*
          * Info
          */
         final Label label = new Label(container, SWT.WRAP);
         label.setText("Customize the tour contextual menu actions, some actions are not available in all context menus.");
         GridDataFactory.fillDefaults()
               .grab(true, false)
               .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
               .applyTo(label);

         {
            /*
             * Show all values
             */
            _rdoShowAllActions = new Button(container, SWT.RADIO);
            _rdoShowAllActions.setText("Show &all actions");
            _rdoShowAllActions.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show custom values
             */
            _rdoShowCustomActions = new Button(container, SWT.RADIO);
            _rdoShowCustomActions.setText("&Customize actions");
            _rdoShowCustomActions.addSelectionListener(_defaultSelectionListener);
         }

         /*
          * Viewer & Actions
          */
         final Composite viewerContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults()
               .indent(8, 0)
               .grab(true, true)
               .applyTo(viewerContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(viewerContainer);
//         viewerContainer.setBackground(UI.SYS_COLOR_BLUE);
         {
            {
               /*
                * Checkbox: Show only available actions
                */
               _chkShowOnlyAvailableActions = new Button(viewerContainer, SWT.CHECK);
               _chkShowOnlyAvailableActions.setText("Show only a&vailable actions");
               _chkShowOnlyAvailableActions.setToolTipText(
                     "When this preferences dialog is opened from a view context menu, then the available actions within this context menu can be filtered.");
               _chkShowOnlyAvailableActions.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> updateUI_ActionViewerFilter()));

               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .span(2, 1)
                     .applyTo(_chkShowOnlyAvailableActions);
            }

            createUI_10_ActionViewer(viewerContainer);
            createUI_20_ViewerActions(viewerContainer);
            createUI_30_Options(viewerContainer);
         }
      }

      return container;
   }

   private void createUI_10_ActionViewer(final Composite parent) {

      final TableLayoutComposite tableLayouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .hint(50, 100)
            .applyTo(tableLayouter);

      final Table table = new Table(
            tableLayouter,
            (SWT.CHECK
                  | SWT.SINGLE
                  | SWT.FULL_SELECTION));

      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _tourActionViewer = new CheckboxTableViewer(table);

      _tourActionViewer.setContentProvider(new ActionViewer_ContentProvider());

      _tourActionViewer.addCheckStateListener(event -> onAction_Check(event));
      _tourActionViewer.addDoubleClickListener(event -> onAction_DoubleClick(event));
      _tourActionViewer.addSelectionChangedListener(event -> onAction_Select(event));

      defineAllColumn(tableLayouter);
   }

   private void createUI_20_ViewerActions(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Button: Up
             */
            _btnUp = new Button(container, SWT.NONE);
            _btnUp.setText(Messages.Pref_Graphs_Button_up);
            _btnUp.setEnabled(false);
            _btnUp.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

               moveSelection_Up();
               enableUpDownActions();
            }));

            setButtonLayoutData(_btnUp);
         }
         {
            /*
             * Button: Down
             */
            _btnDown = new Button(container, SWT.NONE);
            _btnDown.setText(Messages.Pref_Graphs_Button_down);
            _btnDown.setEnabled(false);
            _btnDown.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> {

               moveSelection_Down();
               enableUpDownActions();

            }));

            setButtonLayoutData(_btnDown);
         }
         {
            /*
             * Button: Check all
             */
            _btnCheckAll = new Button(container, SWT.PUSH);
            _btnCheckAll.setText(Messages.App_Action_CheckAll);
            _btnCheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(true)));
            setButtonLayoutData(_btnCheckAll);
         }
         {
            /*
             * Button: Uncheck all
             */
            _btnUncheckAll = new Button(container, SWT.PUSH);
            _btnUncheckAll.setText(Messages.App_Action_UncheckAll);
            _btnUncheckAll.addSelectionListener(SelectionListener.widgetSelectedAdapter(selectionEvent -> onCheckAll(false)));
            setButtonLayoutData(_btnUncheckAll);
         }
      }
   }

   private void createUI_30_Options(final Composite parent) {

      final String tooltipText = "e.g. the number of recent tags or tour types can be adjusted";

      final GridDataFactory gdLink = GridDataFactory.fillDefaults().indent(6, 0);
      final SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(event -> onSelectOptions(event));

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(0, 2)
            .applyTo(container);
      {
         {
            _lblOptions = new Label(container, SWT.WRAP);
            _lblOptions.setText("&Options for");
            _lblOptions.setToolTipText(tooltipText);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(_lblOptions);
         }
         {
            /*
             * Link to tag and tour type options
             */
            _linkOptions_Tags = new Link(container, SWT.WRAP);
            _linkOptions_Tags.setText("<a href=\"%s\">Tags</a>".formatted(LINK_ID_TAGS));
            _linkOptions_Tags.setToolTipText(tooltipText);
            _linkOptions_Tags.addSelectionListener(selectionListener);
            gdLink.applyTo(_linkOptions_Tags);
         }
         UI.createSpacer_Horizontal(container);
         {
            /*
             * Link to tag and tour type options
             */
            _linkOptions_TourTypes = new Link(container, SWT.WRAP);
            _linkOptions_TourTypes.setText("<a href=\"%s\">Tour Types</a>".formatted(LINK_ID_TOUR_TYPES));
            _linkOptions_TourTypes.setToolTipText(tooltipText);
            _linkOptions_TourTypes.addSelectionListener(selectionListener);
            gdLink.applyTo(_linkOptions_TourTypes);
         }
      }
   }

   private void defineAllColumn(final TableLayoutComposite tableLayouter) {

      defineColumn_10_ActionName(tableLayouter);
//    defineColumn_20_ActionID(tableLayouter);
   }

   private void defineColumn_10_ActionName(final TableLayoutComposite tableLayouter) {

      TableViewerColumn tvc;

      tvc = new TableViewerColumn(_tourActionViewer, SWT.NONE);

      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourAction tourAction = ((TourAction) cell.getElement());

            if (tourAction.isCategory) {

               cell.setText(tourAction.actionText);

            } else {

               final String actionText = UI.SPACE4 + tourAction.actionText;

               final StyledString styledString = new StyledString();

               if (_allViewActionIDs != null && _allViewActionIDs.contains(tourAction.actionClassName)) {

                  // action is available in the related view

                  if (tourAction.isChecked) {

                     styledString.append(actionText, net.tourbook.ui.UI.TOTAL_STYLER);

                  } else {

                     styledString.append(actionText);
                  }

               } else {

                  styledString.append(actionText, net.tourbook.ui.UI.DISABLED_STYLER);
               }

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());

               if (_rdoShowCustomActions.getSelection()) {

                  // actions are customized -> display enabled image

                  cell.setImage(tourAction.getImage());

               } else {

                  cell.setImage(tourAction.getImageDisabled());
               }
            }
         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(20));
   }

   @SuppressWarnings("unused")
   private void defineColumn_20_ActionID(final TableLayoutComposite tableLayouter) {

      TableViewerColumn tvc;

      tvc = new TableViewerColumn(_tourActionViewer, SWT.NONE);

      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final TourAction tourAction = ((TourAction) cell.getElement());

            if (tourAction.isCategory) {

               cell.setText(UI.EMPTY_STRING);

            } else {

               final String actionText = tourAction.actionClassName;

               final StyledString styledString = new StyledString();

               if (_allViewActionIDs != null && _allViewActionIDs.contains(tourAction.actionClassName)) {

                  // action is available in the related view

                  if (tourAction.isChecked) {

                     styledString.append(actionText, net.tourbook.ui.UI.TOTAL_STYLER);

                  } else {

                     styledString.append(actionText);
                  }

               } else {

                  styledString.append(actionText, net.tourbook.ui.UI.DISABLED_STYLER);
               }

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());
            }
         }
      });

      tableLayouter.addColumnData(new ColumnWeightData(20));
   }

   @Override
   public void dispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      super.dispose();
   }

   private void enableControls() {

      final boolean isCustomizeActions = _rdoShowCustomActions.getSelection();

      final IStructuredSelection structuredSelection = _tourActionViewer.getStructuredSelection();
      final TourAction selectedAction = (TourAction) structuredSelection.getFirstElement();
      final boolean isActionCategory = selectedAction != null && selectedAction.isCategory;

      // update icons
      _tourActionViewer.refresh();

      _tourActionViewer.getTable().setEnabled(isCustomizeActions);

// SET_FORMATTING_OFF

      _btnCheckAll                  .setEnabled(isCustomizeActions);
      _btnUncheckAll                .setEnabled(isCustomizeActions);

      _chkShowOnlyAvailableActions  .setEnabled(isCustomizeActions);

      _lblOptions                   .setEnabled(isCustomizeActions);

      _linkOptions_Tags             .setEnabled(isCustomizeActions);
      _linkOptions_TourTypes        .setEnabled(isCustomizeActions);

// SET_FORMATTING_ON

      if (isCustomizeActions && isActionCategory == false) {

         enableUpDownActions();

      } else {

         _btnUp.setEnabled(false);
         _btnDown.setEnabled(false);
      }
   }

   /**
    * check if the up/down button are enabled
    */

   private void enableUpDownActions() {

      final Table table = _tourActionViewer.getTable();
      final TableItem[] items = table.getSelection();

      final boolean isValidSelection = items.length > 0;

      boolean isEnableUp = isValidSelection;
      boolean isEnableDown = isValidSelection;

      if (isValidSelection) {

         final int[] allIndices = table.getSelectionIndices();
         final int numItems = table.getItemCount();

         isEnableUp = allIndices[0] != 0;
         isEnableDown = allIndices[allIndices.length - 1] < numItems - 1;
      }

      _btnUp.setEnabled(isEnableUp);
      _btnDown.setEnabled(isEnableDown);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Control parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(selectionEvent -> onModified());
   }

   /**
    * Move the current selection in the build list down
    */
   private void moveSelection_Down() {

      final Table table = _tourActionViewer.getTable();
      final int[] allSelectedIndices = table.getSelectionIndices();

      if (allSelectedIndices.length > 0) {

         final int selectedIndex = allSelectedIndices[0];
         Collections.swap(_allSortedActions, selectedIndex, selectedIndex + 1);

         _tourActionViewer.refresh();
      }
   }

   /**
    * Move the current selection in the build list up
    */
   private void moveSelection_Up() {

      final Table table = _tourActionViewer.getTable();
      final int[] allSelectedIndices = table.getSelectionIndices();

      if (allSelectedIndices.length > 0) {

         final int selectedIndex = allSelectedIndices[0];

         Collections.swap(_allSortedActions, selectedIndex, selectedIndex - 1);

         _tourActionViewer.refresh();
      }
   }

   private void onAction_Check(final CheckStateChangedEvent event) {

      // keep the checked status
      final TourAction tourAction = (TourAction) event.getElement();
      tourAction.isChecked = event.getChecked();

      // select the checked item
      _tourActionViewer.setSelection(new StructuredSelection(tourAction));

      // update UI
      _tourActionViewer.update(tourAction, null);
   }

   private void onAction_DoubleClick(final DoubleClickEvent event) {

      // toggle checkbox

      final IStructuredSelection structuredSelection = _tourActionViewer.getStructuredSelection();
      final TourAction selectedAction = (TourAction) structuredSelection.getFirstElement();

      // update model
      selectedAction.isChecked = !selectedAction.isChecked;

      // update UI
      _tourActionViewer.setChecked(selectedAction, selectedAction.isChecked);
   }

   private void onAction_Select(final SelectionChangedEvent event) {

      enableControls();
   }

   private void onCheckAll(final boolean isChecked) {

      final List<TourAction> allActions = TourActionManager.getSortedActions();

      // update model
      for (final TourAction tourAction : allActions) {
         tourAction.isChecked = isChecked;
      }

      // update UI
      final Object[] allActionsArray = allActions.toArray();

      if (isChecked) {

         _tourActionViewer.setCheckedElements(allActionsArray);

      } else {

         _tourActionViewer.setCheckedElements(new Object[] {});
      }

      _tourActionViewer.refresh();
   }

   private void onModified() {

      enableControls();
   }

   private void onSelectOptions(final SelectionEvent selectionEvent) {

      if (LINK_ID_TAGS.equals(selectionEvent.text)) {

         PreferencesUtil.createPreferenceDialogOn(getShell(),
               PrefPageAppearance.ID,
               null,
               null);

      } else if (LINK_ID_TOUR_TYPES.equals(selectionEvent.text)) {

         PreferencesUtil.createPreferenceDialogOn(getShell(),
               PrefPageTourTypeFilterList.ID,
               null,
               null);
      }
   }

   @Override
   public boolean performCancel() {

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      super.performDefaults();

      _allSortedActions.clear();
      _allSortedActions.addAll(TourActionManager.getDefinedActions());

      // load viewer
      _tourActionViewer.setInput(this);

      enableControls();
   }

   @Override
   public boolean performOk() {

      saveState();

      return super.performOk();
   }

   private void restoreState() {

      final boolean isCustomizeActions = TourActionManager.isCustomizeActions();

      _rdoShowAllActions.setSelection(isCustomizeActions == false);
      _rdoShowCustomActions.setSelection(isCustomizeActions);

      _chkShowOnlyAvailableActions.setSelection(TourActionManager.isShowOnlyAvailableActions());

      // get viewer content
      _allSortedActions.clear();
      _allSortedActions.addAll(TourActionManager.getSortedActions());

      // load viewer
      _tourActionViewer.setInput(this);

      // check only the visible actions
      _tourActionViewer.setCheckedElements(TourActionManager.getVisibleActions().toArray());

      updateUI_ActionViewerFilter();
   }

   private void saveState() {

      /*
       * Get sorting
       */
      final TableItem[] tableItems = _tourActionViewer.getTable().getItems();
      final int numActions = tableItems.length;
      final String[] stateAllSortedActions = new String[numActions];

      for (int actionIndex = 0; actionIndex < numActions; actionIndex++) {

         final TourAction tourAction = (TourAction) tableItems[actionIndex].getData();

         if (tourAction.isCategory) {

            stateAllSortedActions[actionIndex] = tourAction.getCategoryClassName();

         } else {

            stateAllSortedActions[actionIndex] = tourAction.actionClassName;
         }
      }

      /*
       * Get all checked actions
       */
      final Object[] allCheckedActions = _tourActionViewer.getCheckedElements();
      final String[] stateAllCheckedActions = new String[allCheckedActions.length];

      for (int actionIndex = 0; actionIndex < allCheckedActions.length; actionIndex++) {

         final TourAction tourAction = (TourAction) allCheckedActions[actionIndex];

         stateAllCheckedActions[actionIndex] = tourAction.actionClassName;
      }

      TourActionManager.saveActions(

            _rdoShowCustomActions.getSelection(),
            _chkShowOnlyAvailableActions.getSelection(),

            stateAllSortedActions,
            stateAllCheckedActions);
   }

   private void updateUI_ActionViewerFilter() {

      if (_chkShowOnlyAvailableActions.getSelection()) {

         _tourActionViewer.setFilters(_actionFilter);

      } else {

         _tourActionViewer.setFilters();
      }
   }

}