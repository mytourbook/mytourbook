/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.ICommandIds;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.AdvancedMenuForActions;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.ToolTip;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageTagGroups;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.action.IActionProvider;
import net.tourbook.ui.action.TourActionCategory;
import net.tourbook.ui.action.TourActionManager;
import net.tourbook.ui.views.tagging.TourTags_View;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

public class TagMenuManager implements IActionProvider {

   private static final String            SETTINGS_SECTION_RECENT_TAGS = "TagManager.RecentTags";                              //$NON-NLS-1$
   private static final String            STATE_RECENT_TAGS            = "tagId";                                              //$NON-NLS-1$
   private static final String            STATE_PREVIOUS_TAGS          = UI.EMPTY_STRING;

   private static final IPreferenceStore  _prefStore                   = TourbookPlugin.getPrefStore();
   private static final IDialogSettings   _state                       = TourbookPlugin.getState(SETTINGS_SECTION_RECENT_TAGS);

   private static IPropertyChangeListener _prefChangeListener;

   private static TagMenuManager          _currentInstance;
   private static boolean                 _isAdvMenu;

   private static ActionRecentTag[]       _actionsRecentTags;
   private static ActionAllPreviousTags   _actionAllPreviousTags;

   /**
    * Number of tags which are displayed in the context menu or saved in the dialog settings, it's
    * max number is 9 to have a unique accelerator key
    */
   private static LinkedList<TourTag>     _recentTags                  = new LinkedList<>();

   /**
    * Contains all tags which are added by the last add action
    */
   private static HashMap<Long, TourTag>  _allPreviousTags             = new HashMap<>();

   private static int                     _maxRecentActions            = -1;

   /**
    * Contains all tag id's when only one tour is selected
    */
   private static HashSet<Long>           _allTagIds_OneTour;

   private static Map<Long, TourTag>      _allTags_WhenCopied;

   private static boolean                 _isEnableRecentTagActions;

   private static int                     _taggingAutoOpenDelay;
   private static boolean                 _isTaggingAutoOpen;
   private static boolean                 _isTaggingAnimation;
   private boolean                        _isSaveTour;

   private ITourProvider                  _tourProvider;

   private HashMap<String, Object>        _allTagActions;

   private ActionAddRecentTags            _actionAddRecentTags;
   private ActionAddTourTag_SubMenu       _actionAddTag;
   private ActionContributionItem         _actionAddTag_AutoOpen;
   private ActionTagGroups_SubMenu        _actionAddTagGroups;
   private ActionClipboard_CopyTags       _actionClipboard_CopyTags;
   private ActionClipboard_PasteTags      _actionClipboard_PasteTags;
   private Action_RemoveTourTag_SubMenu   _actionRemoveTag;
   private Action_RemoveAllTags           _actionRemoveAllTags;
   private ActionShowTourTagsView         _actionSetTags;
   private ActionOpenPrefDialog           _actionTagGroupPreferences;

   private AdvancedMenuForActions         _advancedMenuToAddTags;

   private TagTransfer                    _tagTransfer                 = new TagTransfer();

   /**
    * Removes all tags
    */
   public class Action_RemoveAllTags extends Action {

      public Action_RemoveAllTags() {

         super(Messages.action_tag_remove_all, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         BusyIndicator.showWhile(Display.getCurrent(), () -> runnableRemoveAllTags());
      }
   }

   /**
    *
    */
   private static class ActionAllPreviousTags extends Action {

      public ActionAllPreviousTags() {
         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         if (_isAdvMenu) {

            final ActionAddTourTag_SubMenu actionAddTagAdvanced = (ActionAddTourTag_SubMenu) _currentInstance._actionAddTag_AutoOpen.getAction();

            actionAddTagAdvanced.setTourTag(isChecked(), _allPreviousTags);

         } else {

            _currentInstance.saveTourTags(_allPreviousTags, isChecked());
         }
      }
   }

   public class ActionClipboard_CopyTags extends Action {

      public ActionClipboard_CopyTags() {

         super(Messages.Action_Tag_CopyTags, AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_Tag_CopyTags_Tooltip);
      }

      @Override
      public void run() {

         clipboard_CopyTags();
      }
   }

   public class ActionClipboard_PasteTags extends Action {

      public ActionClipboard_PasteTags() {

         super(Messages.Action_Tag_PasteTags, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         clipboard_PasteTags();
      }
   }

   /**
    *
    */
   private static class ActionRecentTag extends Action {

      private TourTag _tag;

      public ActionRecentTag() {
         super(net.tourbook.ui.UI.IS_NOT_INITIALIZED, AS_CHECK_BOX);
      }

      @Override
      public void run() {

         final ActionAddTourTag_SubMenu actionAddTagAdvanced = (ActionAddTourTag_SubMenu) _currentInstance._actionAddTag_AutoOpen.getAction();

         if (_isAdvMenu) {
            actionAddTagAdvanced.setTourTag(isChecked(), _tag);
         } else {
            _currentInstance.saveTourTags(_tag, isChecked());
         }
      }

      private void setupTagAction(final TourTag tag, final String tagText) {
         setText(tagText);
         _tag = tag;
      }

      @Override
      public String toString() {
         return "ActionRecentTag [_tag=" + _tag + "]"; //$NON-NLS-1$ //$NON-NLS-2$
      }

   }

   public class ActionShowTourTagsView extends Action {

      public ActionShowTourTagsView() {

         super(Messages.Action_Tag_SetTags, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.TourTags));
      }

      @Override
      public void run() {

         Util.showView(TourTags_View.ID, true);
      }
   }

   private class ActionTagGroup extends Action {

      private final TagGroup __tagGroup;

      public ActionTagGroup(final TagGroup tagGroup) {

         super("%s  %d".formatted(tagGroup.name, tagGroup.tourTags.size()), AS_PUSH_BUTTON); //$NON-NLS-1$

         setToolTipText(TagGroupManager.createTagSortedList(tagGroup));

         __tagGroup = tagGroup;
      }

      @Override
      public void run() {

         saveTourTags(__tagGroup);
      }
   }

   public class ActionTagGroups_SubMenu extends SubMenu {

      List<ActionTagGroup> __allTagGroupActions = new ArrayList<>();

      public ActionTagGroups_SubMenu() {

         super(Messages.Action_Tag_AddGroupedTags, AS_DROP_DOWN_MENU);
      }

      @Override
      public void enableActions() {}

      @Override
      public void fillMenu(final Menu menu) {

         __allTagGroupActions.clear();

         final List<TagGroup> allTagGroups = TagGroupManager.getTagGroupsSorted();

         // create actions for each tag group
         for (final TagGroup tagGroup : allTagGroups) {

            final Set<TourTag> tourTags = tagGroup.tourTags;
            final boolean hasTags = tourTags.size() > 0;

            final ActionTagGroup tagGroupAction = new ActionTagGroup(tagGroup);

            tagGroupAction.setEnabled(hasTags);

            __allTagGroupActions.add(tagGroupAction);

            addActionToMenu(tagGroupAction);
         }

         if (allTagGroups.size() > 0) {

            addSeparatorToMenu();
         }

         addActionToMenu(_actionTagGroupPreferences);
      }
   }

   private class TagTransfer extends ByteArrayTransfer {

      private final String TYPE_NAME = "net.tourbook.tag.TagMenuManager.TagTransfer"; //$NON-NLS-1$
      private final int    TYPE_ID   = registerType(TYPE_NAME);

      private TagTransfer() {}

      @Override
      protected int[] getTypeIds() {
         return new int[] { TYPE_ID };
      }

      @Override
      protected String[] getTypeNames() {
         return new String[] { TYPE_NAME };
      }

      @Override
      protected void javaToNative(final Object data, final TransferData transferData) {

         try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
               final DataOutputStream dataOut = new DataOutputStream(out)) {

            if (_allTags_WhenCopied != null) {

               // write number of tags
               dataOut.writeInt(_allTags_WhenCopied.size());

               // write all tag ID's
               for (final Entry<Long, TourTag> entry : _allTags_WhenCopied.entrySet()) {
                  dataOut.writeLong(entry.getKey());
               }
            }

            super.javaToNative(out.toByteArray(), transferData);

         } catch (final IOException e) {

            StatusUtil.log(e);
         }
      }

      @Override
      protected Object nativeToJava(final TransferData transferData) {

         final byte[] bytes = (byte[]) super.nativeToJava(transferData);

         try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes);
               final DataInputStream dataIn = new DataInputStream(in)) {

            final HashSet<Long> allTagIDs = new HashSet<>();

            // read number of tags
            final int numTags = dataIn.readInt();

            for (int tagIndex = 0; tagIndex < numTags; tagIndex++) {

               // read tag ID
               final long tagID = dataIn.readLong();

               allTagIDs.add(tagID);
            }

            return allTagIDs;

         } catch (final IOException e) {

            StatusUtil.log(e);
         }

         return null;
      }
   }

   /**
    * @param tourProvider
    * @param isSaveTour
    */
   public TagMenuManager(final ITourProvider tourProvider, final boolean isSaveTour) {

      _tourProvider = tourProvider;
      _isSaveTour = isSaveTour;

      if (_prefChangeListener == null) {

         // static var's are not yet initialized
         addPrefListener();
         setupRecentActions();
         restoreAutoOpen();
      }

      createActions();
   }

   private static void addPrefListener() {

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         // check if the number of recent tags has changed
         if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS)) {

            setupRecentActions();

         } else if (property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN)
               || property.equals(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION)
               || property.equals(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY)) {

            restoreAutoOpen();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public static void clearRecentTags() {

      _allPreviousTags.clear();
      _recentTags.clear();
   }

   static void enableRecentTagActions(final boolean isAddTagEnabled, final Set<Long> existingTagIds) {

      if (_actionsRecentTags == null) {
         return;
      }

      final boolean isExistingTagIds = _allTagIds_OneTour != null && _allTagIds_OneTour.size() > 0;

      for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

         final TourTag actionTag = actionRecentTag._tag;

         if (actionTag == null) {
            actionRecentTag.setEnabled(false);
            continue;
         }

         final long recentTagId = actionTag.getTagId();
         boolean isTagEnabled;
         boolean isTagChecked;

         if (isExistingTagIds && _isEnableRecentTagActions) {

            // disable action when it's tag id is contained in allExistingTagIds

            boolean isExistTagId = false;

            for (final long existingTagId : _allTagIds_OneTour) {
               if (recentTagId == existingTagId) {
                  isExistTagId = true;
                  break;
               }
            }

            isTagEnabled = isExistTagId == false;
            isTagChecked = isExistTagId;

         } else {
            isTagEnabled = _isEnableRecentTagActions;
            isTagChecked = false;
         }

         if (isTagEnabled && existingTagIds.contains(recentTagId)) {
            isTagChecked = true;
         }

         actionRecentTag.setEnabled(isTagEnabled);
         actionRecentTag.setChecked(isTagChecked);
      }

      if (_allPreviousTags != null && isAddTagEnabled) {

         boolean isTagChecked = true;

         for (final TourTag previousTag : _allPreviousTags.values()) {
            if (_allTagIds_OneTour.contains(previousTag.getTagId()) == false) {
               isTagChecked = false;
               break;
            }
         }

         _actionAllPreviousTags.setChecked(isTagChecked);
         _actionAllPreviousTags.setEnabled(isTagChecked == false);

      } else {

         _actionAllPreviousTags.setChecked(false);
         _actionAllPreviousTags.setEnabled(false);
      }

   }

   private static void restoreAutoOpen() {

      _isTaggingAutoOpen = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN);
      _isTaggingAnimation = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION);
      _taggingAutoOpenDelay = _prefStore.getInt(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY);
   }

   public static void restoreTagState() {

      final HashMap<Long, TourTag> allTags = TourDatabase.getAllTourTags();

      final String[] recentTagIds = _state.getArray(STATE_RECENT_TAGS);
      if (recentTagIds != null) {

         for (final String tagId : recentTagIds) {
            try {
               final TourTag tag = allTags.get(Long.valueOf(tagId));
               if (tag != null) {
                  _recentTags.add(tag);
               }
            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }

      final String[] previousTagIds = _state.getArray(STATE_PREVIOUS_TAGS);
      if (previousTagIds != null) {

         for (final String tagId : previousTagIds) {
            try {
               final TourTag tag = allTags.get(Long.valueOf(tagId));
               if (tag != null) {
                  _allPreviousTags.put(tag.getTagId(), tag);
               }
            } catch (final NumberFormatException e) {
               // ignore
            }
         }
      }
   }

   public static void saveTagState() {

      if (_maxRecentActions > 0) {

         final String[] tagIds = new String[Math.min(_maxRecentActions, _recentTags.size())];
         int tagIndex = 0;

         for (final TourTag tag : _recentTags) {

            tagIds[tagIndex++] = Long.toString(tag.getTagId());

            if (tagIndex == _maxRecentActions) {
               break;
            }
         }

         _state.put(STATE_RECENT_TAGS, tagIds);
      }

      if (_allPreviousTags.size() > 0) {

         final String[] tagIds = new String[_allPreviousTags.size()];
         int tagIndex = 0;

         for (final TourTag tag : _allPreviousTags.values()) {
            tagIds[tagIndex++] = Long.toString(tag.getTagId());
         }

         _state.put(STATE_PREVIOUS_TAGS, tagIds);
      }
   }

   /**
    * create actions for recent tags
    */
   private static void setupRecentActions() {

      _maxRecentActions = _prefStore.getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS);

      _actionsRecentTags = new ActionRecentTag[_maxRecentActions];

      for (int actionIndex = 0; actionIndex < _actionsRecentTags.length; actionIndex++) {
         _actionsRecentTags[actionIndex] = new ActionRecentTag();
      }

      _actionAllPreviousTags = new ActionAllPreviousTags();
   }

   static void updatePreviousTagState(final HashMap<Long, TourTag> modifiedTags) {

      // check if all previous tags are contained in the modified tags
      for (final TourTag previousTag : _allPreviousTags.values()) {
         if (modifiedTags.containsKey(previousTag.getTagId()) == false) {
            _actionAllPreviousTags.setChecked(false);
            return;
         }
      }
   }

   /**
    * Update names of all recent tags
    */
   public static void updateRecentTagNames() {

      final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();

      for (final TourTag recentTag : _recentTags) {

         final TourTag tourTag = allTourTags.get(recentTag.getTagId());
         if (tourTag != null) {
            recentTag.setTagName(tourTag.getTagName());
         }
      }

      for (final TourTag previousTag : _allPreviousTags.values()) {

         final TourTag tourTag = allTourTags.get(previousTag.getTagId());
         if (tourTag != null) {
            previousTag.setTagName(tourTag.getTagName());
         }
      }
   }

   private void clipboard_CopyTags() {

      _allTags_WhenCopied = getSelectedTourTags();

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         clipboard.setContents(

               new Object[] { new Object() },
               new Transfer[] { _tagTransfer });
      }
      clipboard.dispose();

      UI.showStatusLineMessage(Messages.Action_Tag_StatusLine_PasteInfo.formatted(_allTags_WhenCopied.size()));
   }

   private void clipboard_PasteTags() {

      Object contents;

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         contents = clipboard.getContents(_tagTransfer);
      }
      clipboard.dispose();

      if (contents instanceof final HashSet allTagIDs) {

         // get all tags from the tag ID's

         final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();
         final HashMap<Long, TourTag> allClipboardTags = new HashMap<>();

         for (final Object tagID : allTagIDs) {

            final TourTag tourTag = allTourTags.get(tagID);

            if (tourTag != null) {

               allClipboardTags.put(tourTag.getTagId(), tourTag);
            }
         }

         if (allClipboardTags.size() > 0) {

            saveTourTags(allClipboardTags, true);
         }
      }
   }

   private void createActions() {

// SET_FORMATTING_OFF

      final ActionAddTourTag_SubMenu actionAddTag_AutoOpen = new ActionAddTourTag_SubMenu(this, null);

      _actionAddTag_AutoOpen     = new ActionContributionItem(actionAddTag_AutoOpen);

      /**
       * VERY IMPORTANT: Without an ID, the auto open do NOT work
       */
      _actionAddTag_AutoOpen.setId(ICommandIds.ACTION_ADD_TAG);

      _actionAddRecentTags       = new ActionAddRecentTags(this);
      _actionAddTag              = new ActionAddTourTag_SubMenu(this);
      _actionAddTagGroups        = new ActionTagGroups_SubMenu();
      _actionClipboard_CopyTags  = new ActionClipboard_CopyTags();
      _actionClipboard_PasteTags = new ActionClipboard_PasteTags();
      _actionRemoveTag           = new Action_RemoveTourTag_SubMenu(this);
      _actionRemoveAllTags       = new Action_RemoveAllTags();
      _actionSetTags             = new ActionShowTourTagsView();
      _actionTagGroupPreferences = new ActionOpenPrefDialog(Messages.Action_Tag_ManageTagGroups,PrefPageTagGroups.ID);

      _advancedMenuToAddTags     = new AdvancedMenuForActions(_actionAddTag_AutoOpen);

      _allTagActions       = new HashMap<>();

      _allTagActions.put(_actionAddRecentTags         .getClass().getName(),  _actionAddRecentTags);
      _allTagActions.put(_actionAddTag                .getClass().getName(),  _actionAddTag);
      _allTagActions.put(_actionAddTagGroups          .getClass().getName(),  _actionAddTagGroups);
      _allTagActions.put(_actionClipboard_CopyTags    .getClass().getName(),  _actionClipboard_CopyTags);
      _allTagActions.put(_actionClipboard_PasteTags   .getClass().getName(),  _actionClipboard_PasteTags);
      _allTagActions.put(_actionRemoveAllTags         .getClass().getName(),  _actionRemoveAllTags);
      _allTagActions.put(_actionRemoveTag             .getClass().getName(),  _actionRemoveTag);
      _allTagActions.put(_actionSetTags               .getClass().getName(),  _actionSetTags);

      _allTagActions.put(actionAddTag_AutoOpen        .getClass().getName() + TourActionManager.AUTO_OPEN,   _actionAddTag_AutoOpen);

// SET_FORMATTING_ON
   }

   /**
    * @param isAddTagEnabled
    * @param isRemoveTagEnabled
    */
   private void enableTagActions(final boolean isAddTagEnabled, final boolean isRemoveTagEnabled) {

      _currentInstance = this;

      final ActionAddTourTag_SubMenu actionAddTagAdvanced = (ActionAddTourTag_SubMenu) _actionAddTag_AutoOpen.getAction();

      final List<TourTag> allTagsInClipboard = getTagsFromClipboard();
      final int numTags = allTagsInClipboard != null ? allTagsInClipboard.size() : 0;

      if (numTags > 0) {

         _actionClipboard_PasteTags.setToolTipText(Messages.Action_Tag_PasteTags_Tooltip
               .formatted(TagGroupManager.createTagSortedList(null, allTagsInClipboard)));
      }

// SET_FORMATTING_OFF

      _actionAddTag              .setEnabled(isAddTagEnabled);
      _actionAddTagGroups        .setEnabled(isAddTagEnabled);
      actionAddTagAdvanced       .setEnabled(isAddTagEnabled);

      _actionRemoveTag           .setEnabled(isRemoveTagEnabled);
      _actionRemoveAllTags       .setEnabled(isRemoveTagEnabled);
      _actionSetTags             .setEnabled(isAddTagEnabled || isRemoveTagEnabled);

      _actionClipboard_CopyTags  .setEnabled(isRemoveTagEnabled);
      _actionClipboard_PasteTags .setEnabled(isAddTagEnabled && numTags > 0);

// SET_FORMATTING_ON

      enableRecentTagActions(isAddTagEnabled, _allTagIds_OneTour);
   }

   /**
    * Add/remove tags
    *
    * @param isTourSelected
    *           Is <code>true</code> when at least one tour is selected
    * @param isOneTour
    *           Is <code>true</code> when one single tour is selected
    * @param oneTourTagIds
    *           Contains {@link TourTag} ids when one tour is selected, is <code>null</code> when
    *           multiple tours are selected
    */
   public void enableTagActions(final boolean isTourSelected,
                                final boolean isOneTour,
                                final List<Long> oneTourTagIds) {

      final boolean isAddTagEnabled = isTourSelected;
      final boolean isRemoveTagEnabled;

      final HashSet<Long> allTourTagIds = new HashSet<>();

      if (isOneTour) {

         // one tour is selected

         if (oneTourTagIds != null && oneTourTagIds.size() > 0) {

            // at least one tag is within the tour

            isRemoveTagEnabled = true;

            if (oneTourTagIds != null) {

               for (final Long tagId : oneTourTagIds) {
                  allTourTagIds.add(tagId);
               }
            }

         } else {

            // tags are not available

            isRemoveTagEnabled = false;
         }

      } else {

         // multiple tours are selected

         isRemoveTagEnabled = isTourSelected;
      }

      _isEnableRecentTagActions = isAddTagEnabled;
      _allTagIds_OneTour = allTourTagIds;

      enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
   }

   /**
    * @param isAddTagEnabled
    * @param isRemoveTagEnabled
    * @param tourTags
    */
   public void enableTagActions(final boolean isAddTagEnabled,
                                final boolean isRemoveTagEnabled,
                                final Set<TourTag> tourTags) {

      // get tag id's from all tags
      final HashSet<Long> allExistingTagIds = new HashSet<>();
      if (tourTags != null) {
         for (final TourTag tourTag : tourTags) {
            allExistingTagIds.add(tourTag.getTagId());
         }
      }

      _isEnableRecentTagActions = isAddTagEnabled;
      _allTagIds_OneTour = allExistingTagIds;

      enableTagActions(isAddTagEnabled, isRemoveTagEnabled);
   }

   @Override
   public void fillActions(final IMenuManager menuMgr,
                           final ITourProvider tourProvider) {

      fillTagMenu_WithRecentTags(menuMgr, null);
   }

   /**
    * @param menuMgr
    */
   public void fillTagMenu(final IMenuManager menuMgr) {

      // all all tour tag actions
      menuMgr.add(new Separator());
      {
         menuMgr.add(_actionAddTag_AutoOpen);
         menuMgr.add(_actionAddTagGroups);
         menuMgr.add(_actionAddTag);

         fillTagMenu_WithRecentTags(menuMgr, null);

         menuMgr.add(_actionRemoveTag);
         menuMgr.add(_actionRemoveAllTags);

         menuMgr.add(_actionClipboard_CopyTags);
         menuMgr.add(_actionClipboard_PasteTags);
      }

      _isAdvMenu = false;
   }

   public void fillTagMenu_WithActiveActions(final IMenuManager menuMgr,
                                             final ITourProvider tourProvider) {

      menuMgr.add(new Separator());

      TourActionManager.fillContextMenu(menuMgr, TourActionCategory.TAG, _allTagActions, tourProvider);

      _isAdvMenu = false;
   }

   /**
    * @param menuMgr
    * @param menu
    */
   void fillTagMenu_WithRecentTags(final IMenuManager menuMgr, final Menu menu) {

      if (_recentTags.isEmpty()) {
         return;
      }

      if (_maxRecentActions < 1) {
         return;
      }

      // add all previous tags
      final int numPreviousTags = _allPreviousTags.size();
      if (numPreviousTags > 0) {

         final Collection<TourTag> allPreviousTags = _allPreviousTags.values();

         // check if the first previous tag is the same as the first recent tag
         if (numPreviousTags > 1 || allPreviousTags.iterator().next().equals(_recentTags.get(0)) == false) {

            final StringBuilder sb = new StringBuilder();
            boolean isFirst = true;

            for (final TourTag recentTag : allPreviousTags) {

               if (isFirst) {
                  isFirst = false;
               } else {
                  sb.append(UI.COMMA_SPACE);
               }
               sb.append(recentTag.getTagName());
            }

            String tagText = sb.toString();

            if (UI.IS_SCRAMBLE_DATA) {

               tagText = UI.scrambleText(tagText);
            }

            final int maxTextWidth = 40;

            if (tagText.length() > maxTextWidth) {

               tagText = UI.shortenText(tagText, maxTextWidth, true);
            }

            final ActionContributionItem actionContributionItem = new ActionContributionItem(_actionAllPreviousTags);

            if (menu == null) {

               _actionAllPreviousTags.setText(UI.SPACE4 + UI.MNEMONIC + 0 + UI.SPACE2 + tagText);

               menuMgr.add(actionContributionItem);

            } else {

               _actionAllPreviousTags.setText(UI.MNEMONIC + 0 + UI.SPACE2 + tagText);

               actionContributionItem.fill(menu, -1);
            }
         }
      }

      // add all recent tags
      int tagIndex = 0;
      for (final ActionRecentTag actionRecentTag : _actionsRecentTags) {

         if (tagIndex >= _recentTags.size()) {

            // there are no more recent tags

            break;
         }

         final TourTag tag = _recentTags.get(tagIndex);

         String tagText = tag.getTagName();

         if (UI.IS_SCRAMBLE_DATA) {

            tagText = UI.scrambleText(tagText);
         }

         if (menu == null) {

            actionRecentTag.setupTagAction(
                  tag,
                  (UI.SPACE4 + UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tagText));

            menuMgr.add(new ActionContributionItem(actionRecentTag));

         } else {

            actionRecentTag.setupTagAction(
                  tag,
                  (UI.MNEMONIC + (tagIndex + 1) + UI.SPACE2 + tagText));

            new ActionContributionItem(actionRecentTag).fill(menu, -1);
         }

         tagIndex++;
      }
   }

   public HashMap<String, Object> getAllTagActions() {
      return _allTagActions;
   }

   private Map<Long, TourTag> getSelectedTourTags() {

      final Map<Long, TourTag> allTags = new HashMap<>();

      final List<TourData> allSelectedTours = _tourProvider.getSelectedTours();

      if (allSelectedTours != null) {

         // get all tag's from all tours
         for (final TourData tourData : allSelectedTours) {

            final Set<TourTag> tourTags = tourData.getTourTags();

            for (final TourTag tourTag : tourTags) {

               allTags.put(tourTag.getTagId(), tourTag);
            }
         }
      }

      return allTags;
   }

   private List<TourTag> getTagsFromClipboard() {

      Object contents;

      final Clipboard clipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());
      {
         contents = clipboard.getContents(_tagTransfer);
      }
      clipboard.dispose();

      if (contents instanceof final HashSet allTagIDs) {

         // get all tags from the tag ID's

         final HashMap<Long, TourTag> allTourTags = TourDatabase.getAllTourTags();
         final List<TourTag> allClipboardTags = new ArrayList<>();

         for (final Object tagID : allTagIDs) {

            final TourTag tourTag = allTourTags.get(tagID);

            if (tourTag != null) {

               allClipboardTags.add(tourTag);
            }
         }

         return allClipboardTags;
      }

      return null;
   }

   ITourProvider getTourProvider() {
      return _tourProvider;
   }

   /**
    * This is called when the menu is hidden which contains the tag actions.
    */
   public void onHideMenu() {

      _advancedMenuToAddTags.onHideParentMenu();
   }

   /**
    * This is called when the menu is displayed which contains the tag actions.
    *
    * @param menuEvent
    * @param menuParentControl
    * @param menuPosition
    * @param toolTip
    */
   public void onShowMenu(final MenuEvent menuEvent,
                          final Control menuParentControl,
                          final Point menuPosition,
                          final ToolTip toolTip) {

      _advancedMenuToAddTags.onShowParentMenu(
            menuEvent,
            menuParentControl,
            _isTaggingAutoOpen,
            _isTaggingAnimation,
            _taggingAutoOpenDelay,
            menuPosition,
            toolTip);
   }

   private void runnableRemoveAllTags() {

      // get tours which tour type should be changed
      final ArrayList<TourData> modifiedTours = _tourProvider.getSelectedTours();

      if (modifiedTours == null || modifiedTours.isEmpty()) {
         return;
      }

      final HashMap<Long, TourTag> modifiedTags = new HashMap<>();

      // remove tag in all tours (without tours from an editor)
      for (final TourData tourData : modifiedTours) {

         // get all tag's which will be removed
         final Set<TourTag> tourTags = tourData.getTourTags();

         for (final TourTag tourTag : tourTags) {
            modifiedTags.put(tourTag.getTagId(), tourTag);
         }

         // remove all tour tags
         tourTags.clear();
      }

      saveAndNotify(modifiedTags, modifiedTours);
   }

   /**
    * Save modified tours and notify tour provider
    *
    * @param modifiedTags
    * @param modifiedTours
    */
   private void saveAndNotify(final HashMap<Long, TourTag> modifiedTags, ArrayList<TourData> modifiedTours) {

      if (_isSaveTour) {

         // save all tours with the removed tags

         modifiedTours = TourManager.saveModifiedTours(modifiedTours);

      } else {

         // tours are not saved but the tour provider must be notified that tours has changed

         if (_tourProvider instanceof ITourProvider2) {

            ((ITourProvider2) _tourProvider).toursAreModified(modifiedTours);

         } else {

            TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(modifiedTours));
         }
      }

      TourManager.fireEventWithCustomData(TourEventId.NOTIFY_TAG_VIEW,
            new ChangedTags(modifiedTags, modifiedTours, false),
            null);
   }

   /**
    * Set/Save for multiple tour tags
    *
    * @param mapWithAllModifiedTags
    * @param isAddMode
    *           When <code>true</code> then tags are added otherwise they are removed
    */
   void saveTourTags(final HashMap<Long, TourTag> mapWithAllModifiedTags, final boolean isAddMode) {

      final Runnable runnable = () -> {

         final ArrayList<TourData> allSelectedTours = _tourProvider.getSelectedTours();

         // get tours which tag should be changed
         if (allSelectedTours == null || allSelectedTours.isEmpty()) {
            return;
         }

         final Collection<TourTag> allModifiedTags = mapWithAllModifiedTags.values();

         // add the tag into all selected tours
         for (final TourData tourData : allSelectedTours) {

            // set tags into a tour
            final Set<TourTag> tourTags = tourData.getTourTags();

            if (isAddMode) {

               // add tag to the tour
               tourTags.addAll(allModifiedTags);

            } else {

               // remove tag from tour
               tourTags.removeAll(allModifiedTags);
            }
         }

         // update recent tags
         for (final TourTag tag : allModifiedTags) {

            _recentTags.remove(tag);
            _recentTags.addFirst(tag);
         }

         // it's possible that both hash maps are the same when previous tags has been added as last
         if (_allPreviousTags != mapWithAllModifiedTags) {

            _allPreviousTags.clear();
            _allPreviousTags.putAll(mapWithAllModifiedTags);
         }

         saveAndNotify(mapWithAllModifiedTags, allSelectedTours);
      };

      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   /**
    * Set and save all tour tags from a group
    *
    * @param tagGroup
    */
   private void saveTourTags(final TagGroup tagGroup) {

      final HashMap<Long, TourTag> allTags = new HashMap<>();

      for (final TourTag tourTag : tagGroup.tourTags) {

         allTags.put(tourTag.getTagId(), tourTag);
      }

      saveTourTags(allTags, true);
   }

   /**
    * Set/Save one tour tag
    *
    * @param tag
    * @param isAddMode
    */
   void saveTourTags(final TourTag tag, final boolean isAddMode) {

      final HashMap<Long, TourTag> tags = new HashMap<>();
      tags.put(tag.getTagId(), tag);

      saveTourTags(tags, isAddMode);
   }

   void setIsAdvanceMenu() {

      _isAdvMenu = true;
   }
}
