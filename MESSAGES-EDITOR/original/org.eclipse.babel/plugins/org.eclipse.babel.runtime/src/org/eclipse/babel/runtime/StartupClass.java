/*******************************************************************************
 * Copyright (c) 2008 Nigel Westbury and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Nigel Westbury - initial API and implementation
 *******************************************************************************/

package org.eclipse.babel.runtime;

import org.eclipse.babel.runtime.external.ITranslatableSet;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;

public class StartupClass implements IStartup {

	public void earlyStartup() {
		/*
		 * Build the text set for the menu. By doing this, the menu is updated.
		 * That is why this is an early start plug-in. The users will see the
		 * original text in the menu until this is done.
		 */
		new Thread(new Runnable() {

			public void run() {
				IWorkbench workbench = PlatformUI.getWorkbench();
				WorkbenchWindow window = null;
				do {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Display display = Display.getDefault();

					if (display != null) {
						display.asyncExec(new Runnable() {

							public void run() {
								if (Activator.getDefault().getTranslatableMenu() != null) {
									// Already done
									return;
								}

								IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
								if (window == null) {
									// Window not yet initialized, so we can't yet process the menu
									return;
								}

								MenuAnalyzer analyser = new MenuAnalyzer();
								TranslatableMenuItem translatableMenu = analyser.createTranslatableMenu();
								ITranslatableSet textSet = analyser.getTextSet();
								Activator.getDefault().setTranslatableMenu(translatableMenu, textSet);
							}
						});
					}
				} while (Activator.getDefault().getTranslatableMenu() == null);
			}
		}).start();
	}

}
