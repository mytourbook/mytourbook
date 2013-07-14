/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.map3;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import java.util.Map;

import javax.media.opengl.GL;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.map3.view.Map3Manager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ActionHandlerWorldWindDiagnostics extends AbstractHandler {

	private final IDialogSettings				_state		= TourbookPlugin.getStateSection(//
																	getClass().getCanonicalName());

	private static final WorldWindowGLCanvas	_wwCanvas	= Map3Manager.getWWCanvas();

	private static Attr[]						attrs		= new Attr[] {
			new Attr(GL.GL_STENCIL_BITS, "stencil bits"),
			new Attr(GL.GL_DEPTH_BITS, "depth bits"),
			new Attr(GL.GL_MAX_TEXTURE_UNITS, "max texture units"),
			new Attr(GL.GL_MAX_TEXTURE_IMAGE_UNITS_ARB, "max texture image units"),
			new Attr(GL.GL_MAX_TEXTURE_COORDS_ARB, "max texture coords"),
			new Attr(GL.GL_MAX_TEXTURE_SIZE, "max texture size"),
			new Attr(GL.GL_MAX_ELEMENTS_INDICES, "max elements indices"),
			new Attr(GL.GL_MAX_ELEMENTS_VERTICES, "max elements vertices"),
			new Attr(GL.GL_MAX_LIGHTS, "max lights")		};

	private static class Attr {

		private Object	attr;

		private String	name;

		private Attr(final Object attr, final String name) {
			this.attr = attr;
			this.name = name;
		}
	}

	private class DialogInfo extends Dialog {

		private Text	_txtInfo;

		protected DialogInfo(final Shell parentShell) {

			super(parentShell);

			// make dialog resizable
			setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
		}

		@Override
		protected void configureShell(final Shell shell) {

			super.configureShell(shell);

//			Dialog_WorldWind_Diagnostics=World Wind Diagnostics

			shell.setText("World Wind Diagnostics");
		}

		@Override
		protected void createButtonsForButtonBar(final Composite parent) {

			// create OK button
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		}

		@Override
		protected Control createDialogArea(final Composite parent) {

			final Control container = createUI(parent);

			BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
				public void run() {
					getDiagnosticsData();
				}
			});

			return container;
		}

		private Control createUI(final Composite parent) {
			final Composite container = new Composite(parent, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
			GridLayoutFactory.swtDefaults().spacing(20, 0).applyTo(container);
			{
				_txtInfo = new Text(container, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtInfo);
			}

			return container;
		}

		private void getDiagnosticsData() {

			final StringBuilder sb = new StringBuilder();

			sb.append(gov.nasa.worldwind.Version.getVersion() + "\n");

			sb.append("\nSystem Properties\n");
			sb.append("Processors: " + Runtime.getRuntime().availableProcessors() + "\n");
			sb.append("Free memory: " + Runtime.getRuntime().freeMemory() + " bytes\n");
			sb.append("Max memory: " + Runtime.getRuntime().maxMemory() + " bytes\n");
			sb.append("Total memory: " + Runtime.getRuntime().totalMemory() + " bytes\n");

			for (final Map.Entry<?, ?> prop : System.getProperties().entrySet()) {
				sb.append(prop.getKey() + " = " + prop.getValue() + "\n");
			}

//			final javax.media.opengl.GL gl = GLContext.getCurrent().getGL();

			final GL gl = _wwCanvas.getGL();

			sb.append("\nOpenGL Values\n");
			final String oglVersion = gl.glGetString(GL.GL_VERSION);
			sb.append("OpenGL version: " + oglVersion + "\n");

			String value = "";
			final int[] intVals = new int[1];
			for (final Attr attr : attrs) {
				if (attr.attr instanceof Integer) {
					gl.glGetIntegerv((Integer) attr.attr, intVals, 0);
					value = Integer.toString(intVals[0]);
				}

				sb.append(attr.name + ": " + value + "\n");
			}

			final String extensionString = gl.glGetString(GL.GL_EXTENSIONS);
			if (extensionString != null) {

				final String[] extensions = extensionString.split(" ");
				sb.append("Extensions\n");
				for (final String ext : extensions) {
					sb.append("    " + ext + "\n");
				}
			}

			sb.append("\nJOGL Values\n");
			final String pkgName = "javax.media.opengl";
			try {
				getClass().getClassLoader().loadClass(pkgName + ".GL");

				final Package p = Package.getPackage(pkgName);
				if (p == null) {
					sb.append("WARNING: Package.getPackage(" + pkgName + ") is null\n");
				} else {
					sb.append(p + "\n");
					sb.append("Specification Title = " + p.getSpecificationTitle() + "\n");
					sb.append("Specification Vendor = " + p.getSpecificationVendor() + "\n");
					sb.append("Specification Version = " + p.getSpecificationVersion() + "\n");
					sb.append("Implementation Vendor = " + p.getImplementationVendor() + "\n");
					sb.append("Implementation Version = " + p.getImplementationVersion() + "\n");
				}
			} catch (final ClassNotFoundException e) {
				sb.append("Unable to load " + pkgName + "\n");
			}

			_txtInfo.setText(sb.toString());
		}

		@Override
		protected IDialogSettings getDialogBoundsSettings() {
			// keep window size and position
			return _state;
		}

		@Override
		protected Point getInitialSize() {
			final Point calculatedSize = super.getInitialSize();
			if (calculatedSize.x < 600) {
				calculatedSize.x = 600;
			}
			if (calculatedSize.y < 600) {
				calculatedSize.y = 600;
			}
			return calculatedSize;
		}

	}

	public Object execute(final ExecutionEvent event) throws ExecutionException {

		new DialogInfo(Display.getCurrent().getActiveShell()).open();

		return null;
	}

}
