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
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.p2.ui.RevertProfilePage;
import org.osgi.framework.Bundle;

public class RevertProfilePageExtensionFactory implements IExecutableExtensionFactory {

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtensionFactory#create()
	 */
	public Object create() {
		Bundle bundle = Platform.getBundle("org.eclipse.compare"); //$NON-NLS-1$
		if (bundle == null) {
			return new RevertProfilePage();
		}
		return new RevertProfilePageWithCompare();
	}
}