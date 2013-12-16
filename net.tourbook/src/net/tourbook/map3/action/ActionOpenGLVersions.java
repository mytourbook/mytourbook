/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.action;

import gov.nasa.worldwind.Version;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.map3.Messages;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.opengl.JoglVersion;

public class ActionOpenGLVersions extends Action {

	private final IDialogSettings	_state	= TourbookPlugin.getStateSection(//
													getClass().getCanonicalName());

	private GLProfile				_glProfile;
	private GLCapabilities			_glCaps;

	/*
	 * UI controls
	 */
	private FormToolkit				_tk;

	private Composite				_mapContainer;

	private GLCanvas				_glCanvas;
	private Frame					_awtFrame;

	private Text					_txtInfo;

	private class DialogOpenGLVersions extends Dialog {

		private static final int	DIALOG_MINIMUM_SIZE	= 600;

		protected DialogOpenGLVersions(final Shell parentShell) {

			super(parentShell);

			// make dialog resizable
			setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		}

		@Override
		public boolean close() {

			final boolean isClosed = super.close();

			if (isClosed) {
				onDispose();
			}

			return isClosed;
		}

		@Override
		protected void configureShell(final Shell shell) {

			super.configureShell(shell);

			shell.setText(Messages.Map3_Dialog_OpenGLVersion_Title);
		}

		@Override
		protected void createButtonsForButtonBar(final Composite parent) {

			// create OK button
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}

		@Override
		protected Control createDialogArea(final Composite parent) {
			return createUI(parent);
		}

		@Override
		protected IDialogSettings getDialogBoundsSettings() {
			// keep window size and position
			return _state;
		}

		@Override
		protected Point getInitialSize() {

			final Point calculatedSize = super.getInitialSize();

			final int minWidth = DIALOG_MINIMUM_SIZE;
			final int minHeight = DIALOG_MINIMUM_SIZE;

			// ensure minimum size
			if (calculatedSize.x < minWidth) {
				calculatedSize.x = minWidth;
			}
			if (calculatedSize.y < minHeight) {
				calculatedSize.y = minHeight;
			}

			return calculatedSize;
		}
	}

	private class GLInfo implements GLEventListener {

		public void display(final GLAutoDrawable drawable) {}

		public void dispose(final GLAutoDrawable drawable) {}

		public void init(final GLAutoDrawable drawable) {

			// update UI in UI thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					updateUI(drawable.getGL());
				}
			});
		}

		public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
	}

	public ActionOpenGLVersions() {

		super(Messages.Map3_Action_ShowOpenGLVersion_Tooltip, AS_PUSH_BUTTON);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_Map3_OpenGL_Version));
	}

	private Control createUI(final Composite parent) {

		_glProfile = GLProfile.getDefault();
		_glCaps = new GLCapabilities(_glProfile);

		final Composite container = createUI_10_Content(parent);

		// this will also update the UI
		_glCanvas.setVisible(true);

		return container;
	}

	private Composite createUI_10_Content(final Composite parent) {

		_tk = new FormToolkit(parent.getDisplay());

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().spacing(20, 0).applyTo(container);
		{
			_txtInfo = _tk.createText(container, UI.EMPTY_STRING, 0 //
					| SWT.MULTI
//					| SWT.BORDER
					| SWT.READ_ONLY
					| SWT.V_SCROLL
//					| SWT.H_SCROLL
					//
					);
			GridDataFactory.fillDefaults()//
					.grab(true, true)
					.applyTo(_txtInfo);

			// set mono spaced font
			_txtInfo.setFont(JFaceResources.getTextFont());

			createUI_90_OpenGL(container);
		}

		return container;
	}

	private void createUI_90_OpenGL(final Composite parent) {

		_glCanvas = new GLCanvas(_glCaps);
		_glCanvas.addGLEventListener(new GLInfo());
		_glCanvas.setSize(10, 10);

		// set parent griddata, this must be done AFTER the content is created, otherwise it fails !!!
//		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

		// build GUI: container(SWT) -> Frame(AWT) -> Panel(AWT) -> WorldWindowGLCanvas(AWT)
		_mapContainer = _tk.createComposite(parent, SWT.EMBEDDED);
		GridDataFactory.fillDefaults().hint(1, 2).applyTo(_mapContainer);
		{
			_awtFrame = SWT_AWT.new_Frame(_mapContainer);
			final java.awt.Panel awtPanel = new java.awt.Panel(new java.awt.BorderLayout());

			_awtFrame.add(awtPanel);
			awtPanel.add(_glCanvas, BorderLayout.CENTER);
		}

		_mapContainer.setVisible(false);

		parent.layout();
	}

	private void onDispose() {

		_tk.dispose();

		_glCanvas.destroy();
		_glCanvas = null;
	}

	@Override
	public void run() {
		new DialogOpenGLVersions(Display.getCurrent().getActiveShell()).open();
	}

	private void updateUI(final GL gl) {

		final StringBuilder sb = new StringBuilder();

		// WW version
		sb.append(VersionUtil.SEPERATOR);
		sb.append(UI.NEW_LINE);
		sb.append(Version.getVersionName() + UI.DASH_WITH_SPACE + Version.getVersionNumber());
		sb.append(UI.NEW_LINE);
		sb.append(VersionUtil.SEPERATOR);
		sb.append(UI.NEW_LINE3);

		// Device info
		sb.append(VersionUtil.getPlatformInfo());
		sb.append(UI.NEW_LINE3);

		sb.append(JoglVersion.getGLInfo(gl, null).toString());
		sb.append(UI.NEW_LINE3);

		sb.append(GlueGenVersion.getInstance().toString());
		sb.append(UI.NEW_LINE3);

		sb.append(JoglVersion.getInstance().toString());
		sb.append(UI.NEW_LINE3);

		/*
		 * All capabilities
		 */
		final GLDrawableFactory factory = GLDrawableFactory.getFactory(_glProfile);
		final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(null);

		for (int i = 0; i < availCaps.size(); i++) {
			sb.append(availCaps.get(i).toString());
			sb.append(UI.NEW_LINE);
		}

		_txtInfo.setText(sb.toString());
	}

}
