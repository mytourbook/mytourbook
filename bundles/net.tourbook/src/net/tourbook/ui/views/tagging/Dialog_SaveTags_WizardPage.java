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

import java.util.HashSet;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
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
   private Button _rdoRemoveAllTags;
   private Button _rdoAppendNewTags;
   private Button _rdoReplaceTags;

   private Label  _lblSelectedTags;

   protected Dialog_SaveTags_WizardPage(final HashSet<Long> allCheckedTagIds) {

      super(UI.EMPTY_STRING);

      _allCheckedTagIds = allCheckedTagIds;

      setTitle(Messages.Dialog_SaveTags_Wizard_Title);
   }

   @Override
   public void createControl(final Composite parent) {

      final Composite wizardPage = createUI(parent);

      enableControls();

      // set wizard page control
      setControl(wizardPage);

      restoreState();

      parent.addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent event) {
            parent.setSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
         }
      });
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
//      container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_YELLOW));
      GridDataFactory.fillDefaults()
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory.swtDefaults().applyTo(container);
      {
         createUI_10_Controls(container);
      }

      return container;
   }

   private void createUI_10_Controls(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Info
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_SaveTags_Label_Info);
         }
         {
            /*
             * Append the selected tags to the existing tags
             */

            // radio
            _rdoAppendNewTags = new Button(container, SWT.RADIO);
            _rdoAppendNewTags.setText(Messages.Dialog_SaveTags_Radio_AppendNewTags);
         }
         {
            /*
             * Replace existing tags with the selected tags
             */

            // radio
            _rdoReplaceTags = new Button(container, SWT.RADIO);
            _rdoReplaceTags.setText(Messages.Dialog_SaveTags_Radio_ReplaceTags);
         }
         {
            /*
             * Remove all tags
             */

            // radio
            _rdoRemoveAllTags = new Button(container, SWT.RADIO);
            _rdoRemoveAllTags.setText(Messages.Dialog_SaveTags_Radio_RemoveAllTags);
         }
         {
            /*
             * Selected tags
             */

            // label
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_SaveTags_Label_SelectedTags);
            GridDataFactory.fillDefaults().indent(0, 20).applyTo(label);

            // label
            _lblSelectedTags = new Label(container, SWT.WRAP);
            _lblSelectedTags.setText(UI.SPACE1);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .applyTo(_lblSelectedTags);
         }
      }
   }

   private void enableControls() {

      final boolean isTagsSelected = _allCheckedTagIds.size() > 0;

      _rdoAppendNewTags.setEnabled(isTagsSelected);
      _rdoReplaceTags.setEnabled(isTagsSelected);

      // remove all tags is only enabled when no tags are selected
      _rdoRemoveAllTags.setEnabled(isTagsSelected == false);
   }

   private void restoreState() {

      final int saveAction = _prefStore.getInt(ITourbookPreferences.DIALOG_SAVE_TAGS_ACTION);

      _rdoAppendNewTags.setSelection(saveAction == Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS);
      _rdoReplaceTags.setSelection(saveAction == Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS);
      _rdoRemoveAllTags.setSelection(saveAction == Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS);

      final String tagNames = TourDatabase.getTagNamesText(_allCheckedTagIds, true);
      _lblSelectedTags.setText(tagNames);
   }

   void saveState() {

      int saveAction = -1;

      if (_rdoAppendNewTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_APPEND_NEW_TAGS;

      } else if (_rdoReplaceTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_REPLACE_TAGS;

      } else if (_rdoRemoveAllTags.getSelection()) {
         saveAction = Dialog_SaveTags.SAVE_TAG_ACTION_REMOVE_ALL_TAGS;
      }

      _prefStore.setValue(ITourbookPreferences.DIALOG_SAVE_TAGS_ACTION, saveAction);
   }

}
