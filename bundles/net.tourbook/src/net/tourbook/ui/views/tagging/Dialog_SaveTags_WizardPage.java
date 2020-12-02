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
package net.tourbook.ui.views.tagging;

import java.util.ArrayList;
import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class Dialog_SaveTags_WizardPage extends WizardPage {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private HashSet<Long>          _allCheckedTagIds;

   /*
    * UI controls
    */
   private Button              _rdoRemoveAllTags;
   private Button              _rdoRemoveSelectedTags;
   private Button              _rdoAppendNewTags;
   private Button              _rdoReplaceTags;

   private Label               _lblSelectedTags;

   private ArrayList<TourData> _allSelectedTours;

   protected Dialog_SaveTags_WizardPage(final ArrayList<TourData> selectedTours, final HashSet<Long> allCheckedTagIds) {

      super(UI.EMPTY_STRING);

      _allSelectedTours = selectedTours;
      _allCheckedTagIds = allCheckedTagIds;

      setTitle(Messages.Dialog_SaveTags_Wizard_Title);
   }

   @Override
   public void createControl(final Composite parent) {

      final Composite wizardPage = createUI(parent);

      enableControls();

      restoreState();

      parent.layout(true, true);

      // set wizard page control
      setControl(wizardPage);
   }

   private Composite createUI(final Composite parent) {

      final ScrolledComposite scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
      scrolledContainer.setExpandVertical(true);
      scrolledContainer.setExpandHorizontal(true);

      final Composite scrolledContent = new Composite(scrolledContainer, SWT.NONE);
      GridLayoutFactory.swtDefaults()//
//          .spacing(0, 0)
            .applyTo(scrolledContent);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         createUI_10_Controls(scrolledContent);
      }

      // setup scrolled container
      scrolledContainer.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent e) {
            scrolledContainer.setMinSize(scrolledContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         }
      });

      scrolledContainer.setContent(scrolledContent);

      return scrolledContainer;
   }

   private void createUI_10_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Label: Info
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(NLS.bind(Messages.Dialog_SaveTags_Label_Info, _allSelectedTours.size()));
         }
         {
            /*
             * Radio: Append the selected tags to the existing tags
             */
            _rdoAppendNewTags = new Button(container, SWT.RADIO);
            _rdoAppendNewTags.setText(Messages.Dialog_SaveTags_Radio_AppendNewTags);
            GridDataFactory.fillDefaults().indent(0, 10).applyTo(_rdoAppendNewTags);
         }
         {
            /*
             * Radio: Replace existing tags with the selected tags
             */
            _rdoReplaceTags = new Button(container, SWT.RADIO);
            _rdoReplaceTags.setText(Messages.Dialog_SaveTags_Radio_ReplaceTags);
         }
         {
            /*
             * Radio: Remove selected tags
             */
            _rdoRemoveSelectedTags = new Button(container, SWT.RADIO);
            _rdoRemoveSelectedTags.setText(Messages.Dialog_SaveTags_Radio_RemoveTags_Selected);
         }
         {
            /*
             * RadioL: Remove all tags
             */
            _rdoRemoveAllTags = new Button(container, SWT.RADIO);
            _rdoRemoveAllTags.setText(Messages.Dialog_SaveTags_Radio_RemoveTags_All);
         }

         if (_allCheckedTagIds.isEmpty() == false) {

            // tags are selected
            {
               /*
                * Label: Selected tags
                */

               // label: header
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Dialog_SaveTags_Label_SelectedTags);
               GridDataFactory.fillDefaults().indent(0, 10).applyTo(label);

               // label: tags
               _lblSelectedTags = new Label(container, SWT.WRAP);
            }
         }
      }
   }

   private void enableControls() {

      final boolean isTagSelected = _allCheckedTagIds.isEmpty() == false;

      _rdoAppendNewTags.setEnabled(isTagSelected);
      _rdoReplaceTags.setEnabled(isTagSelected);
      _rdoRemoveSelectedTags.setEnabled(isTagSelected);

      // remove all tags is only enabled when no tags are selected
      _rdoRemoveAllTags.setEnabled(isTagSelected == false);
   }

   private void restoreState() {

      final int saveAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SAVE_TAGS_ACTION);

// SET_FORMATTING_OFF

      _rdoAppendNewTags.setSelection(saveAction          == Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS);
      _rdoRemoveAllTags.setSelection(saveAction          == Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS);
      _rdoRemoveSelectedTags.setSelection(saveAction     == Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_SELECTED_TAGS);
      _rdoReplaceTags.setSelection(saveAction            == Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS);

// SET_FORMATTING_ON

      if (_allCheckedTagIds.isEmpty() == false) {
         _lblSelectedTags.setText(TourDatabase.getTagNamesText(_allCheckedTagIds, true));
      }
   }

   void saveState() {

      int saveAction = -1;

      if (_rdoAppendNewTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS;

      } else if (_rdoReplaceTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS;

      } else if (_rdoRemoveSelectedTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_SELECTED_TAGS;

      } else if (_rdoRemoveAllTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS;
      }

      _prefStore.setValue(ITourbookPreferences.DIALOG_SAVE_TAGS_ACTION, saveAction);
   }

}
