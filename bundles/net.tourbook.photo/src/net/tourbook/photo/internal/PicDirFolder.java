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
package net.tourbook.photo.internal;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.photo.IPhotoPreferences;
import net.tourbook.photo.ImageUtils;
import net.tourbook.photo.PhotoCache;
import net.tourbook.photo.PhotoImageCache;
import net.tourbook.photo.PhotoLoadManager;
import net.tourbook.photo.PhotoUI;
import net.tourbook.photo.PicDirView;
import net.tourbook.photo.internal.manager.ExifCache;
import net.tourbook.photo.internal.manager.ThumbnailStore;
import net.tourbook.photo.internal.preferences.PrefPagePhotoExternalApp;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;

/**
 * This folder viewer is from org.eclipse.swt.examples.fileviewer but with many modifications.
 */
public class PicDirFolder {

// SET_FORMATTING_OFF

	private static final String	MENU_ID_PIC_DIR_VIEW_IN_FOLDER			= "menu.net.tourbook.photo.PicDirView.InFolder";	//$NON-NLS-1$

	static String						WIN_PROGRAMFILES								= System.getenv("programfiles");							//$NON-NLS-1$
	static String						FILE_SEPARATOR									= System.getProperty("file.separator");				//$NON-NLS-1$

	private static final String	STATE_SELECTED_FOLDER						= "STATE_SELECTED_FOLDER";									//$NON-NLS-1$
	private static final String	STATE_IS_SINGLE_CLICK_EXPAND				= "STATE_IS_SINGLE_CLICK_EXPAND";						//$NON-NLS-1$
	private static final String	STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS	= "STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS";			//$NON-NLS-1$

// SET_FORMATTING_ON

   private static final LinkedBlockingDeque<FolderLoader> _folderWaitingQueue = new LinkedBlockingDeque<>();

   /**
    * This executer is running only in one thread because accessing the file system with multiple
    * thread is slowing it down.
    */
   private static ThreadPoolExecutor                      _folderExecutor;

   static {

      final ThreadFactory threadFactoryFolder = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final String threadName = "LoadingFolder"; //$NON-NLS-1$

            final Thread thread = new Thread(r, threadName);

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      _folderExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1, threadFactoryFolder);
   }

   private IDialogSettings                  _state;
   private final IPreferenceStore           _prefStore       = Activator.getDefault().getPreferenceStore();

   private PicDirView                       _picDirView;
   private PicDirImages                     _picDirImages;

   private long                             _expandRunnableCounter;

   private boolean                          _isExpandingSelection;
   private boolean                          _isBehaviourAutoExpandCollapse;
   private boolean                          _isBehaviourSingleExpandedOthersCollapse;
   private boolean                          _isStateShowFileFolderInFolderItem;

   /**
    * Is true when the mouse click is for the context menu
    */
   private boolean                          _isMouseContextMenu;
   private boolean                          _isFromNavigationHistory;
   private boolean                          _doAutoCollapseExpand;

   private TVIFolderRoot                    _rootItem;
   private TVIFolderFolder                  _selectedTVIFolder;

   private FileFilter                       _imageFileFilter = ImageUtils.createImageFileFilter();
   private File                             _selectedFolder;

   private ActionRefreshFolder              _actionRefreshFolder;
   private ActionRunExternalAppTitle        _actionRunExternalAppTitle;
   private ActionRunExternalApp             _actionRunExternalAppDefault;
   private ActionRunExternalApp             _actionRunExternalApp1;
   private ActionRunExternalApp             _actionRunExternalApp2;
   private ActionRunExternalApp             _actionRunExternalApp3;
   private ActionPreferences                _actionPreferences;
   private ActionSingleClickExpand          _actionAutoExpandCollapse;
   private ActionSingleExpandCollapseOthers _actionSingleExpandCollapseOthers;

   /*
    * UI controls
    */
   private Display    _display;

   private TreeViewer _folderViewer;

   private class ActionRunExternalAppTitle extends Action {

      public ActionRunExternalAppTitle() {
         super(Messages.Pic_Dir_Action_RunExternalAppTitle, AS_PUSH_BUTTON);
         setEnabled(false);
      }

      @Override
      public void run() {}
   }

   private static final class FolderComparer implements IElementComparer {

      @Override
      public boolean equals(final Object a, final Object b) {

         if (a == b) {
            return true;
         }

         if (a instanceof TVIFolderFolder && b instanceof TVIFolderFolder) {

            final TVIFolderFolder item1 = (TVIFolderFolder) a;
            final TVIFolderFolder item2 = (TVIFolderFolder) b;

            final String folder1Name = item1._treeItemFolder.getName();
            final String folder2Name = item2._treeItemFolder.getName();

            return folder1Name.equals(folder2Name);
         }
         return false;
      }

      @Override
      public int hashCode(final Object element) {
         return element.hashCode();
      }
   }

   private class FolderContentProvider implements ITreeContentProvider {

      @Override
      public void dispose() {}

      @Override
      public Object[] getChildren(final Object parentElement) {

         /*
          * force to get children so that the user can see if a folder can be expanded or not
          */

         return ((TreeViewerItem) parentElement).getFetchedChildrenAsArray();
      }

      @Override
      public Object[] getElements(final Object inputElement) {
         return _rootItem.getFetchedChildrenAsArray();
      }

      @Override
      public Object getParent(final Object element) {
         return ((TreeViewerItem) element).getParentItem();
      }

      @Override
      public boolean hasChildren(final Object element) {

         boolean hasChildren = false;

         if (element instanceof TVIFolderFolder) {
            final TVIFolderFolder tviFolder = (TVIFolderFolder) element;

            if (tviFolder.isFolderLoaded()) {

               hasChildren = tviFolder.hasChildren();
            } else {

               putFolderInWaitingQueue(tviFolder, false);

               hasChildren = true;
            }

         } else {
            final TreeViewerItem treeViewerItem = (TreeViewerItem) element;

            hasChildren = treeViewerItem.hasChildren();
         }

         return hasChildren;
      }

      @Override
      public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
   }

   public PicDirFolder(final PicDirView picDirView, final PicDirImages picDirImages, final IDialogSettings state) {
      _picDirView = picDirView;
      _picDirImages = picDirImages;
      _state = state;
   }

   void actionAutoExpandCollapse() {
      _isBehaviourAutoExpandCollapse = _actionAutoExpandCollapse.isChecked();
      enableActions();
   }

   public void actionRefreshFolder() {

      if (_selectedTVIFolder == null) {
         return;
      }

      BusyIndicator.showWhile(_display, new Runnable() {
         @Override
         public void run() {

            final boolean isExpanded = _folderViewer.getExpandedState(_selectedTVIFolder);

            _folderViewer.collapseToLevel(_selectedTVIFolder, 1);

            final Tree tree = _folderViewer.getTree();
            tree.setRedraw(false);
            {
               final TreeItem topItem = tree.getTopItem();

               // remove children from viewer
               final ArrayList<TreeViewerItem> unfetchedChildren = _selectedTVIFolder.getUnfetchedChildren();
               if (unfetchedChildren != null) {
                  _folderViewer.remove(unfetchedChildren.toArray());
               }

               // remove children from model
               _selectedTVIFolder.clearChildren();

               // update folder viewer
               _folderViewer.refresh(_selectedTVIFolder);

               // remove cached metadata for this folder
               final String folderPath = _selectedTVIFolder._treeItemFolder.getAbsolutePath();
               ExifCache.remove(folderPath);

               // remove cached images
               PhotoImageCache.disposePath(folderPath);

               // remove errors
               PhotoLoadManager.removeInvalidImageFiles();

               // remove photos
               PhotoCache.removePhotosFromFolder(folderPath);

               // delete store files
               final File folder = new File(folderPath);
               final File[] imageFiles = folder.listFiles(_imageFileFilter);
               if (imageFiles != null) {
                  ThumbnailStore.cleanupStoreFiles(imageFiles);
               }

               // update images and force folder reload
               displayFolderImages(_selectedTVIFolder, false, true);

               // expand selected folder
               if (isExpanded) {
                  _folderViewer.setExpandedState(_selectedTVIFolder, true);
               }

               // position tree items to the old location
               tree.setTopItem(topItem);
            }
            tree.setRedraw(true);
         }
      });
   }

   void actionRunExternalApp(final ActionRunExternalApp actionRunExternalApp) {

      if (_selectedTVIFolder == null) {
         return;
      }

      // check if an app is defined
      if (actionRunExternalApp == _actionRunExternalAppDefault) {

         PreferencesUtil
               .createPreferenceDialogOn(
                     Display.getCurrent().getActiveShell(),
                     PrefPagePhotoExternalApp.ID,
                     null,
                     null)
               .open();
         return;
      }

      String extApp = null;
      if (actionRunExternalApp == _actionRunExternalApp1) {
         extApp = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp2) {
         extApp = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_2).trim();
      } else if (actionRunExternalApp == _actionRunExternalApp3) {
         extApp = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_3).trim();
      }

      final String folder = _selectedTVIFolder._treeItemFolder.getAbsolutePath();

      String commands[] = null;
      if (UI.IS_WIN) {

         final String[] commandsWin = { "\"" + extApp + "\"", //$NON-NLS-1$ //$NON-NLS-2$
               "\"" + folder + "\"" }; //$NON-NLS-1$ //$NON-NLS-2$

         commands = commandsWin;

      } else if (UI.IS_OSX) {

         final String[] commandsOSX = { "/usr/bin/open", "-a", // //$NON-NLS-1$ //$NON-NLS-2$
               extApp,
               folder };

         commands = commandsOSX;

      } else if (UI.IS_LINUX) {

         final String[] commandsLinux = { extApp, folder };

         commands = commandsLinux;
      }

      if (commands != null) {

         try {

            // log command
            final StringBuilder sb = new StringBuilder();
            for (final String cmd : commands) {
               sb.append(cmd + UI.SPACE1);
            }
            StatusUtil.logInfo(sb.toString());

            Runtime.getRuntime().exec(commands);

         } catch (final Exception e) {
            StatusUtil.showStatus(e);
         }
      }
   }

   void actionSingleExpandCollapseOthers() {
      _isBehaviourSingleExpandedOthersCollapse = _actionSingleExpandCollapseOthers.isChecked();
      enableActions();
   }

   private void createActions() {

      _actionPreferences = new ActionPreferences();
      _actionRefreshFolder = new ActionRefreshFolder(this);

      _actionRunExternalAppTitle = new ActionRunExternalAppTitle();
      _actionRunExternalAppDefault = new ActionRunExternalApp(this);
      _actionRunExternalApp1 = new ActionRunExternalApp(this);
      _actionRunExternalApp2 = new ActionRunExternalApp(this);
      _actionRunExternalApp3 = new ActionRunExternalApp(this);

      _actionAutoExpandCollapse = new ActionSingleClickExpand(this);
      _actionSingleExpandCollapseOthers = new ActionSingleExpandCollapseOthers(this);
   }

   /**
    * create the views context menu
    */
   private void createContextMenu() {

      final MenuManager menuMgr = new MenuManager();
      menuMgr.setRemoveAllWhenShown(true);
      menuMgr.addMenuListener(new IMenuListener() {
         @Override
         public void menuAboutToShow(final IMenuManager manager) {
            fillContextMenu(manager);
         }
      });

      final Tree tree = _folderViewer.getTree();
      final Menu contextMenu = menuMgr.createContextMenu(tree);

      tree.setMenu(contextMenu);

      _picDirView.registerContextMenu(MENU_ID_PIC_DIR_VIEW_IN_FOLDER, menuMgr);
   }

   public void createUI(final Composite parent) {

      createUI_0(parent);

      createActions();
      createContextMenu();

      // update UI from pref store
//		_folderViewer.getTree().setLinesVisible(_prefStore.getBoolean(IPhotoPreferences.VIEW_LAYOUT_DISPLAY_LINES));
   }

   private void createUI_0(final Composite parent) {

      _display = parent.getDisplay();

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
      {
         createUI_10_TreeView(container);
      }
   }

   private void createUI_10_TreeView(final Composite parent) {

      /*
       * Create tree layout
       */

      final Composite layoutContainer = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .hint(200, 100)
            .applyTo(layoutContainer);

      final TreeColumnLayout treeLayout = new TreeColumnLayout();
      layoutContainer.setLayout(treeLayout);

      /*
       * Create tree
       */
      final Tree tree = new Tree(layoutContainer, SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);

      tree.setHeaderVisible(false);

      tree.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseDown(final MouseEvent e) {
            _doAutoCollapseExpand = true;
            _isMouseContextMenu = e.button == 3;
         }
      });

      final Display display = parent.getDisplay();
      final Color listBackgroundColor = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);

      tree.addListener(SWT.EraseItem, event -> {

         if ((event.detail & SWT.SELECTED) != 0 && (event.detail & SWT.HOT) != 0) {

            // item is selected + hovered

            paintFolderBackground(event, tree, listBackgroundColor, 0xff);

         } else if ((event.detail & SWT.SELECTED) != 0) {

            // item is selected

            paintFolderBackground(event, tree, listBackgroundColor, 0xb0);

         } else if ((event.detail & SWT.HOT) != 0) {

            // item is hovered

            paintFolderBackground(event, tree, listBackgroundColor, 0x40);
         }
      });

      /*
       * Create viewer
       */
      _folderViewer = new TreeViewer(tree);

      _folderViewer.setContentProvider(new FolderContentProvider());
      _folderViewer.setComparer(new FolderComparer());
      _folderViewer.setUseHashlookup(true);

      _folderViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {

            // expand/collapse current item
            final Object selection = ((IStructuredSelection) _folderViewer.getSelection()).getFirstElement();

            final TVIFolderFolder treeItem = (TVIFolderFolder) selection;

            expandCollapseFolder(treeItem);
         }
      });

      _folderViewer.addSelectionChangedListener(new ISelectionChangedListener() {
         @Override
         public void selectionChanged(final SelectionChangedEvent event) {
            onSelectFolder((ITreeSelection) event.getSelection());
         }
      });

      /*
       * create columns
       */
      TreeViewerColumn tvc;
      TreeColumn tvcColumn;

      // column: os folder
      tvc = new TreeViewerColumn(_folderViewer, SWT.LEAD);
      tvcColumn = tvc.getColumn();
      tvc.setLabelProvider(new StyledCellLabelProvider() {

         @Override
         public void update(final ViewerCell cell) {

            final Object element = cell.getElement();

            if (element instanceof TVIFolderFolder) {
               final TVIFolderFolder folderItem = (TVIFolderFolder) element;

               final StyledString styledString = new StyledString();

               styledString.append(folderItem._folderName);

               if (_isStateShowFileFolderInFolderItem) {

                  if (folderItem.isFolderLoaded()) {

                     final int folderCounter = folderItem.getFolderCounter();
                     if (folderCounter > 0) {
                        styledString.append(UI.SPACE2);
                        styledString.append(Integer.toString(folderCounter), PhotoUI.PHOTO_FOLDER_STYLER);
                     }

                     final int fileCounter = folderItem.getFileCounter();
                     if (fileCounter > 0) {
                        styledString.append(net.tourbook.common.UI.SPACE2);
                        styledString.append(Integer.toString(fileCounter), PhotoUI.PHOTO_FILE_STYLER);
                     }

                  } else {

                     // force that file list is loaded and number of files is available

                     putFolderInWaitingQueue(folderItem, false);

                     styledString.append(
                           UI.SPACE2 + Messages.Pic_Dir_StatusLable_LoadingFolder_InFolderTree,
                           PhotoUI.PHOTO_FOLDER_STYLER);
                  }
               }

               cell.setText(styledString.getString());
               cell.setStyleRanges(styledString.getStyleRanges());
            }
         }
      });
      treeLayout.setColumnData(tvcColumn, new ColumnWeightData(100, true));
   }

   /**
    * @param tviFolder
    * @param isFromNavigationHistory
    * @param isReloadFolder
    */
   private void displayFolderImages(final TVIFolderFolder tviFolder,
                                    final boolean isFromNavigationHistory,
                                    final boolean isReloadFolder) {

      final File selectedFolder = tviFolder._treeItemFolder;

      // optimize, don't select the same folder again
      if (isReloadFolder == false && _selectedFolder != null && selectedFolder.equals(_selectedFolder)) {
         return;
      }

      _selectedFolder = selectedFolder;
      _selectedTVIFolder = tviFolder;

      // display images for the selected folder
      _picDirImages.showImages(selectedFolder, isFromNavigationHistory, isReloadFolder);
   }

   private void enableActions() {

      /*
       * auto expand is disabled when single expand is selected -> single expand always expands
       * (this is not the best solution but it is complicated to implement otherwise)
       */

      _actionAutoExpandCollapse.setEnabled(_isBehaviourSingleExpandedOthersCollapse == false);

//		_actionMergePhotosWithTours = new ActionMergeFolderPhotosWithTours(this);

   }

   private void expandCollapseFolder(final TVIFolderFolder treeItem) {

      if (_folderViewer.getExpandedState(treeItem)) {

         // collapse folder

         _folderViewer.collapseToLevel(treeItem, 1);

         removeFolderFromWaitingQueue(treeItem);

      } else {

         // expand folder

         if (treeItem.isFolderLoaded()) {

            _folderViewer.expandToLevel(treeItem, 1);

         } else {

            putFolderInWaitingQueue(treeItem, true);
         }
      }
   }

   private void fillContextMenu(final IMenuManager menuMgr) {

      menuMgr.add(new Separator(UI.MENU_SEPARATOR_ADDITIONS));

      fillExternalApp(menuMgr);

      menuMgr.add(new Separator());
      menuMgr.add(_actionRefreshFolder);

      menuMgr.add(new Separator());
      menuMgr.add(_actionAutoExpandCollapse);
      menuMgr.add(_actionSingleExpandCollapseOthers);

      menuMgr.add(new Separator());
      menuMgr.add(_actionPreferences);
   }

   private void fillExternalApp(final IMenuManager menuMgr) {

      menuMgr.add(_actionRunExternalAppTitle);

      /*
       * App1
       */
      final String prefExtApp1 = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1).trim();
      if (prefExtApp1.length() > 0) {

         _actionRunExternalApp1.setText(
               NLS.bind(
                     Messages.Pic_Dir_Label_ExternalApp, //
                     1,
                     new Path(prefExtApp1).lastSegment()
               //					prefExtApp1
               //
               ));

         menuMgr.add(_actionRunExternalApp1);
      }

      /*
       * App2
       */
      final String prefExtApp2 = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_2).trim();
      if (prefExtApp2.length() > 0) {

         _actionRunExternalApp2.setText(
               NLS.bind(
                     Messages.Pic_Dir_Label_ExternalApp, //
                     2,
                     new Path(prefExtApp2).lastSegment()
               //					prefExtApp2
               //
               ));

         menuMgr.add(_actionRunExternalApp2);
      }

      /*
       * App3
       */
      final String prefExtApp3 = _prefStore.getString(IPhotoPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_3).trim();
      if (prefExtApp3.length() > 0) {

         _actionRunExternalApp3.setText(
               NLS.bind(
                     Messages.Pic_Dir_Label_ExternalApp, //
                     3,
                     new Path(prefExtApp3).lastSegment()
               //					prefExtApp3
               //
               ));

         menuMgr.add(_actionRunExternalApp3);
      }

      // show default when app is not defined
      menuMgr.add(_actionRunExternalAppDefault);
   }

   /**
    * Gets filesystem root entries
    *
    * @return an array of Files corresponding to the root directories on the platform, may be empty
    *         but not null
    */
   private File[] getRootsSorted() {

      final File[] roots = File.listRoots();

      PicDirView.sortFiles(roots);

      return roots;

      /*
       * On JDK 1.22 only...
       */
      // return File.listRoots();

      /*
       * On JDK 1.1.7 and beyond... -- PORTABILITY ISSUES HERE --
       */
//		if (System.getProperty("os.name").indexOf("Windows") != -1) {
//
//			final ArrayList<File> list = new ArrayList<File>();
//
//			for (char i = 'c'; i <= 'z'; ++i) {
//
//				final File drive = new File(i + ":" + File.separator);
//
//				if (drive.isDirectory() && drive.exists()) {
//
//					list.add(drive);
//
//					if (initial && i == 'c') {
//						_selectedFolder = drive;
//						initial = false;
//					}
//				}
//			}
//
//			final File[] roots = list.toArray(new File[list.size()]);
//
//			PicDirView.sortFiles(roots);
//
//			return roots;
//
//		} else {
//
//			final File root = new File(File.separator);
//			if (initial) {
//				_selectedFolder = root;
//				initial = false;
//			}
//			return new File[] { root };
//		}
   }

   /**
    * @return Returns selected folder or <code>null</code> when a folder is not selected.
    */
   public File getSelectedFolder() {
      return _selectedFolder;
   }

   public Tree getTree() {
      return _folderViewer.getTree();
   }

   public void handlePrefStoreModifications(final PropertyChangeEvent event) {

      final String property = event.getProperty();
      boolean isViewerRefresh = false;

// !!! pref is set in another pref store !!!
//
//		if (property.equals(IPhotoPreferences.VIEW_LAYOUT_CHANGED)) {
//
//			_folderViewer.getTree().setLinesVisible(_prefStore.getBoolean(IPhotoPreferences.VIEW_LAYOUT_DISPLAY_LINES));
//
//			isViewerRefresh = true;
//
//		} else
      if (property.equals(IPhotoPreferences.PHOTO_VIEWER_PREF_EVENT_IMAGE_VIEWER_UI_IS_MODIFIED)) {

         updateColors(false);

         isViewerRefresh = true;
      }

      if (isViewerRefresh) {

         _folderViewer.refresh();

         /*
          * the tree must be redrawn because the styled text does not show with the new color
          */
         _folderViewer.getTree().redraw();
      }
   }

   /**
    * Do the actions when a folder is selected
    *
    * @param iSelection
    */
   private void onSelectFolder(final ITreeSelection treeSelection) {

      if (_isExpandingSelection) {
         // prevent endless loops
         return;
      }

      // keep & reset mouse event
      final boolean doAutoCollapseExpand = _doAutoCollapseExpand;
      _doAutoCollapseExpand = false;

      final TreePath[] selectedTreePaths = treeSelection.getPaths();
      if (selectedTreePaths.length == 0) {
         return;
      }
      final TreePath selectedTreePath = selectedTreePaths[0];
      if (selectedTreePath == null) {
         return;
      }

      final TVIFolderFolder tviFolder = (TVIFolderFolder) selectedTreePath.getLastSegment();

      if (_isMouseContextMenu) {

         // context menu has been opened, do no expand/collapse

         displayFolderImages(tviFolder, _isFromNavigationHistory, false);

      } else {

         if (doAutoCollapseExpand) {
            onSelectFolder_10_AutoExpandCollapse(treeSelection, selectedTreePath, tviFolder);
         } else {
            displayFolderImages(tviFolder, _isFromNavigationHistory, false);
         }
      }

      // reset navigation state, this is a bit of a complex behavior
      _isFromNavigationHistory = false;
   }

   /**
    * This is not yet working thoroughly because the expanded position moves up or down and all
    * expanded children are not visible (but they could) like when the triangle (+/-) icon in the
    * tree is clicked.
    *
    * @param treeSelection
    * @param selectedTreePath
    * @param tviFolder
    */
   private void onSelectFolder_10_AutoExpandCollapse(final ITreeSelection treeSelection,
                                                     final TreePath selectedTreePath,
                                                     final TVIFolderFolder tviFolder) {

      if (_isBehaviourSingleExpandedOthersCollapse) {

         /*
          * run async because this is doing a reselection which cannot be done within the current
          * selection event
          */
         Display.getCurrent().asyncExec(new Runnable() {

            private long            __expandRunnableCounter   = ++_expandRunnableCounter;

            private TVIFolderFolder __selectedFolderItem      = tviFolder;
            private ITreeSelection  __treeSelection           = treeSelection;
            private TreePath        __selectedTreePath        = selectedTreePath;
            private boolean         __isFromNavigationHistory = _isFromNavigationHistory;

            @Override
            public void run() {

               // check if a newer expand event occurred
               if (__expandRunnableCounter != _expandRunnableCounter) {
                  return;
               }

               onSelectFolder_20_AutoExpandCollapse_Runnable(
                     __selectedFolderItem,
                     __treeSelection,
                     __selectedTreePath,
                     __isFromNavigationHistory);
            }
         });

      } else {

         if (_isBehaviourAutoExpandCollapse) {

            // expand folder with one mouse click but not with the keyboard
            expandCollapseFolder(tviFolder);
         }

         displayFolderImages(tviFolder, _isFromNavigationHistory, false);
      }
   }

   /**
    * This behavior is complex and still have possible problems.
    *
    * @param selectedFolderItem
    * @param treeSelection
    * @param selectedTreePath
    * @param isFromNavigationHistory
    */
   private void onSelectFolder_20_AutoExpandCollapse_Runnable(final TVIFolderFolder selectedFolderItem,
                                                              final ITreeSelection treeSelection,
                                                              final TreePath selectedTreePath,
                                                              final boolean isFromNavigationHistory) {
      _isExpandingSelection = true;
      {
         final Tree tree = _folderViewer.getTree();

         tree.setRedraw(false);
         {
            final TreeItem topItem = tree.getTopItem();

            final boolean isExpanded = _folderViewer.getExpandedState(selectedTreePath);

            /*
             * collapse all tree paths
             */
            final TreePath[] allExpandedTreePaths = _folderViewer.getExpandedTreePaths();
            for (final TreePath treePath : allExpandedTreePaths) {
               _folderViewer.setExpandedState(treePath, false);
            }

            /*
             * expand and select selected folder
             */
            _folderViewer.setExpandedTreePaths(new TreePath[] { selectedTreePath });
            _folderViewer.setSelection(treeSelection, true);

            if (_isBehaviourAutoExpandCollapse && isExpanded) {

               // auto collapse expanded folder
               _folderViewer.setExpandedState(selectedTreePath, false);
            }

            /**
             * set top item to the previous top item, otherwise the expanded/collapse item is
             * positioned at the bottom and the UI is jumping all the time
             * <p>
             * win behavior: when an item is set to top which was collapsed before, it will be
             * expanded
             */
            if (topItem.isDisposed() == false) {
               tree.setTopItem(topItem);
            }
         }
         tree.setRedraw(true);
      }
      _isExpandingSelection = false;

      displayFolderImages(selectedFolderItem, isFromNavigationHistory, false);
   }

   private void paintFolderBackground(final Event event, final Tree tree, final Color listBackgroundColor, final int alpha) {

      final GC gc = event.gc;
      final Color bgColorBackup = gc.getBackground();

      final Rectangle clientArea = tree.getClientArea();
      final Rectangle eventBounds = event.getBounds();

      gc.setAlpha(alpha);
      gc.setBackground(listBackgroundColor);
      gc.fillRectangle(0, eventBounds.y, clientArea.width, eventBounds.height);

      // restore colors for subsequent drawing
      gc.setBackground(bgColorBackup);
   }

   private void putFolderInWaitingQueue(final TVIFolderFolder queueFolderItem, final boolean isExpandFolder) {

      // get and set queue state
      if (queueFolderItem._isInWaitingQueue.getAndSet(true)) {
         // folder is already in waiting queue
         return;
      }

      final FolderLoader treeFolderLoader = new FolderLoader(queueFolderItem, isExpandFolder);
      queueFolderItem._folderLoader = treeFolderLoader;

      _folderWaitingQueue.add(treeFolderLoader);

      final Runnable executorTask = new Runnable() {
         @Override
         public void run() {

            // get last added loader item
            final FolderLoader folderLoader = _folderWaitingQueue.pollFirst();

            if (folderLoader != null) {

               final TVIFolderFolder loaderFolderItem = folderLoader.loaderFolderItem;

               // load folder children
               loaderFolderItem.hasChildren();

               // must be outside of the UI thread that the number is correct
               final int queueSize = _folderWaitingQueue.size();

               // update UI
               _display.syncExec(new Runnable() {
                  @Override
                  public void run() {

                     if (_folderViewer.getTree().isDisposed()) {
                        return;
                     }

                     if (folderLoader.isExpandFolder) {
                        _folderViewer.expandToLevel(loaderFolderItem, 1);
                     } else {

                        /*
                         * update structural changes, also the triangle to expand/collapse, an
                         * update(...) is not sufficient because this will not remove the triangle
                         * when not necessary
                         */
                        _folderViewer.refresh(loaderFolderItem);
                     }

                     String statusMessage;
                     if (queueSize == 0) {
                        statusMessage = UI.EMPTY_STRING;
                     } else {
                        statusMessage = NLS.bind(Messages.Pic_Dir_StatusLable_LoadingFolder, queueSize);
                     }
                     _picDirImages.updateUI_StatusMessage(statusMessage);

                     // reset queue state
                     loaderFolderItem._isInWaitingQueue.set(false);
                     loaderFolderItem._folderLoader = null;
                  }
               });
            }
         }
      };
      _folderExecutor.submit(executorTask);

   }

   /**
    * this feature is not very simple to be implemented, stopped for implementing now 2012-07-18<br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    * recursive <br>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
    *
    * @param queueFolderItem
    */
   private void removeFolderFromWaitingQueue(final TVIFolderFolder queueFolderItem) {

//		final FolderLoader folderLoader = queueFolderItem._folderLoader;
//
//		// get queue state
//		if (queueFolderItem._isInWaitingQueue.getAndSet(false)) {
//			_folderWaitingQueue.remove(folderLoader);
//		}
//		// ensure it's reset
//		queueFolderItem._folderLoader = null;
//
//		final ArrayList<TreeViewerItem> folderChildren = queueFolderItem.getUnfetchedChildren();
//		if (folderChildren == null) {
//			return;
//		}
//
//		// remove all folder children
//		for (final TreeViewerItem treeViewerItem : folderChildren) {
//			if (treeViewerItem instanceof TVIFolderFolder) {
//				removeFolderFromWaitingQueue((TVIFolderFolder) treeViewerItem);
//			}
//		}
}

public void restoreState() {

   _isBehaviourAutoExpandCollapse = Util.getStateBoolean(_state, STATE_IS_SINGLE_CLICK_EXPAND, false);
   _actionAutoExpandCollapse.setChecked(_isBehaviourAutoExpandCollapse);

   _isBehaviourSingleExpandedOthersCollapse = Util.getStateBoolean(
         _state,
         STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS,
         false);
   _actionSingleExpandCollapseOthers.setChecked(_isBehaviourSingleExpandedOthersCollapse);

   updateColors(true);
   enableActions();

   /*
    * delay folder retrieval so that the UI can be updated immediately
    */
   final Job folderJob = new UIJob(UI.EMPTY_STRING) {

      @Override
      public IStatus runInUIThread(final IProgressMonitor monitor) {

         final String previousSelectedFolder = Util.getStateString(_state, STATE_SELECTED_FOLDER, null);

         restoreStateFolder(previousSelectedFolder);

         return Status.OK_STATUS;
      }
   };

   folderJob.setSystem(true);
   folderJob.schedule();
}

private void restoreStateFolder(final String restoreFolderName) {

   BusyIndicator.showWhile(_display, new Runnable() {
      @Override
      public void run() {

         // set root item
         _rootItem = new TVIFolderRoot(PicDirFolder.this, _folderViewer, getRootsSorted());

         _folderViewer.setInput(new Object());

         _selectedFolder = null;
         _selectedTVIFolder = null;

         _picDirImages.showRestoreFolder(restoreFolderName);

//				/*
//				 * first select only the root item because there is an effect, that the restored
//				 * folder is expanded and ms later the last device is displayed multiple times (for
//				 * each root entry)
//				 */
         selectFolder(restoreFolderName, true, false, false);

//				_display.asyncExec(new Runnable() {
//					public void run() {
//						selectFolder(restoreFolderName, true, false, false);
//					}
//				});
      }
   });
}

public void saveState() {

   // selected folder
   if (_selectedFolder != null) {
      _state.put(STATE_SELECTED_FOLDER, _selectedFolder.getAbsolutePath());
      }

      _state.put(STATE_IS_SINGLE_CLICK_EXPAND, _actionAutoExpandCollapse.isChecked());
      _state.put(STATE_IS_SINGLE_EXPAND_COLLAPSE_OTHERS, _actionSingleExpandCollapseOthers.isChecked());
   }

   /**
    * @param requestedFolderName
    * @param isMoveUpHierarchyWhenFolderIsInvalid
    * @param isFromNavigationHistory
    *           Set <code>true</code> when the folder was selected from the navigations history
    *           which prevents that the navigation history is updated.
    * @param isRootItem
    * @return Return <code>false</code> when the folder which should be selected is not available
    */
   boolean selectFolder(final String requestedFolderName,
                        final boolean isMoveUpHierarchyWhenFolderIsInvalid,
                        final boolean isFromNavigationHistory,
                        final boolean isRootItem) {

      _isFromNavigationHistory = isFromNavigationHistory;

      boolean isRequestedFolderAvailable = false;
      File selectedFolder = null;

      if (requestedFolderName != null) {

         final File folderFile = new File(requestedFolderName);
         if (folderFile.isDirectory()) {
            selectedFolder = folderFile;
            isRequestedFolderAvailable = true;
         }

         if (selectedFolder == null && isMoveUpHierarchyWhenFolderIsInvalid) {

            // previously selected folder is not available, try to move up the hierarchy

            IPath folderPath = new Path(requestedFolderName);

            final int segmentCount = folderPath.segmentCount();
            for (int segmentIndex = segmentCount; segmentIndex > 0; segmentIndex--) {

               folderPath = folderPath.removeLastSegments(1);

               final File folderPathFile = new File(folderPath.toOSString());
               if (folderPathFile.isDirectory()) {
                  selectedFolder = folderPathFile;
                  break;
               }
            }
         }
      }

      if (requestedFolderName != null && isRequestedFolderAvailable == false) {

         // restored folder is not available

         _picDirImages.updateUI_StatusMessage(
               NLS.bind(
                     Messages.Pic_Dir_StatusLable_LoadingFolder_FolderIsNotAvailable,
                     requestedFolderName));
      }

      if (selectedFolder == null) {

         // previously selected folder is not available
         return false;
      }

      final String restorePathName = selectedFolder.getAbsolutePath();

      final ArrayList<String> allFolderSegments = new ArrayList<>();

      if (UI.IS_WIN) {
         // add device, e.g. c:, z:
         final IPath restoreRoot = new Path(restorePathName).removeFirstSegments(9999);
         allFolderSegments.add(restoreRoot.toOSString());
      } else {
         // add root
         allFolderSegments.add("/"); //$NON-NLS-1$
      }

      final IPath restorePath = new Path(restorePathName);
      final String[] folderSegments = restorePath.segments();

      for (final String folderSegmentName : folderSegments) {
         allFolderSegments.add(folderSegmentName);
      }

      final ArrayList<TVIFolder> treePathItems = new ArrayList<>();
      TVIFolder folderSegmentItem = _rootItem;
      treePathItems.add(folderSegmentItem);

      // create tree path for each folder segment
      for (final String folderSegmentName : allFolderSegments) {

         boolean isPathSegmentAvailable = false;

         final ArrayList<TreeViewerItem> tviChildren = folderSegmentItem.getFetchedChildren();
         for (final TreeViewerItem tviChild : tviChildren) {

            final TVIFolderFolder childFolder = (TVIFolderFolder) tviChild;
            String childFolderName;

            if (childFolder._isRootFolder) {

               if (UI.IS_WIN) {
                  // remove \ from device name
                  childFolderName = childFolder._folderName.substring(0, 2);
               } else {
                  childFolderName = childFolder._folderName;
               }

            } else {

               childFolderName = childFolder._folderName;
            }

            if (folderSegmentName.equals(childFolderName)) {

               isPathSegmentAvailable = true;

               treePathItems.add(childFolder);
               folderSegmentItem = childFolder;

               break;
            }
         }

         if (isPathSegmentAvailable == false) {
            // requested path is not available, select partial path in the viewer
            break;
         }

         if (isRootItem) {
            // read only the root item
            break;
         }
      }

      if (treePathItems.size() == 0) {
         // there is nothing which can be selected
         return false;
      }

      final TVIFolder[] treePathArray = treePathItems.toArray(new TVIFolder[treePathItems.size()]);
      final TreePath treePath = new TreePath(treePathArray);
      final ITreeSelection treeSelection = new TreeSelection(treePath);

      _doAutoCollapseExpand = true;

      // select folder in the tree viewer, this triggers onSelectFolder(...)
      _folderViewer.setSelection(treeSelection, true);

      return true;
   }

   private void updateColors(final boolean isRestore) {

      _isStateShowFileFolderInFolderItem = _prefStore.getBoolean(//
            IPhotoPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER);

      final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
      final Color fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
      final Color bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
      final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

      final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

      final Tree tree = _folderViewer.getTree();
      tree.setForeground(fgColor);
      tree.setBackground(bgColor);

      _picDirImages.updateColors(fgColor, bgColor, selectionFgColor, noFocusSelectionFgColor, isRestore);
   }

}
