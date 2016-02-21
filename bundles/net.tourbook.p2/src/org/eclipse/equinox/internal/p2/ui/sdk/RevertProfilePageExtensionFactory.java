/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.p2.ui.sdk;

import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.equinox.p2.ui.RevertProfilePage;

public class RevertProfilePageExtensionFactory implements IExecutableExtensionFactory {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtensionFactory#create()
	 */
	public Object create() {
		return new RevertProfilePage();
	}
}