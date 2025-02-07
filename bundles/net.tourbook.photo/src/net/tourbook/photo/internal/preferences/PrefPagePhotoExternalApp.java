/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.photo.internal.preferences;

import net.tourbook.common.UI;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.PhotoActivator;
import net.tourbook.photo.internal.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPagePhotoExternalApp extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPagePhotoExternalAppID"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = PhotoActivator.getPrefStore();

   /*
    * UI controls
    */

   public class FileFieldEditorNoValidation extends FileFieldEditor {

      public FileFieldEditorNoValidation(final String name, final String labelText, final Composite parent) {
         super(name, labelText, parent);
      }

      @Override
      protected boolean checkState() {
         return true;
      }
   }

   public PrefPagePhotoExternalApp() {
//		noDefaultAndApplyButton();
   }

   @Override
   protected void createFieldEditors() {
      createUI();
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
      GridLayoutFactory.fillDefaults().applyTo(parent);
//		parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_10_ExternalPhotoViewer(parent);
      }
   }

   /**
    * field: path to save raw tour data
    */
   private void createUI_10_ExternalPhotoViewer(final Composite parent) {

      FileFieldEditorNoValidation externalFolderViewer1;
      FileFieldEditorNoValidation externalFolderViewer2;
      FileFieldEditorNoValidation externalFolderViewer3;

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.PrefPage_Photo_ExtViewer_Group_ExternalApplication);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      {
         {
            /*
             * Label: info
             */
            final Label label = new Label(group, SWT.WRAP);
            label.setText(Messages.PrefPage_Photo_ExtViewer_Label_Info);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .indent(0, 5)
                  .hint(UI.DEFAULT_DESCRIPTION_WIDTH, SWT.DEFAULT)
                  .applyTo(label);
         }

         UI.createSpacer_Vertical(group, 10, 3);

         {
            /*
             * External photo folder viewer 1
             */
            externalFolderViewer1 = folderViewer(
                  group,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication1,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip,
                  IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FOLDER_VIEWER_1);
         }
         {
            /*
             * External photo folder viewer 2
             */
            externalFolderViewer2 = folderViewer(
                  group,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication2,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip,
                  IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FOLDER_VIEWER_2);
         }
         {
            /*
             * External photo folder viewer 3
             */
            externalFolderViewer3 = folderViewer(
                  group,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication3,
                  Messages.PrefPage_Photo_ExtViewer_Label_ExternalApplication_Tooltip,
                  IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_FOLDER_VIEWER_3);
         }
      }

      // set layout after the fields are created
      GridLayoutFactory.swtDefaults().numColumns(3).extendedMargins(0, 0, 0, 0).applyTo(group);

      /*
       * Set width for the text control that the pref dialog is not as wide as the full path
       */
      final int pathWidth = 200;

      GridData gd;

      Text rawPathControl = externalFolderViewer1.getTextControl(group);
      gd = (GridData) rawPathControl.getLayoutData();
      gd.widthHint = pathWidth;

      rawPathControl = externalFolderViewer2.getTextControl(group);
      gd = (GridData) rawPathControl.getLayoutData();
      gd.widthHint = pathWidth;

      rawPathControl = externalFolderViewer3.getTextControl(group);
      gd = (GridData) rawPathControl.getLayoutData();
      gd.widthHint = pathWidth;
   }

   private FileFieldEditorNoValidation folderViewer(final Group parent,
                                                    final String label,
                                                    final String tooltip,
                                                    final String prefKey) {

      final FileFieldEditorNoValidation field = new FileFieldEditorNoValidation(prefKey, label, parent);

      field.setEmptyStringAllowed(true);
      field.setValidateStrategy(StringFieldEditor.VALIDATE_ON_KEY_STROKE);

      final Label fieldLabel = field.getLabelControl(parent);
      fieldLabel.setToolTipText(tooltip);

      addField(field);

      return field;
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);
   }
}
