/*******************************************************************************
 * Copyright (c) 2007 Pascal Essiembre.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Pascal Essiembre - initial API and implementation
 ******************************************************************************/
package org.eclipse.babel.core.message.checks;

import org.eclipse.babel.core.message.Message;
import org.eclipse.babel.core.message.MessagesBundleGroup;

/**
 * Visitor for finding if a key has at least one corresponding bundle entry
 * with a missing value.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MissingValueCheck implements IMessageCheck {

	/** The singleton */
	public static MissingValueCheck MISSING_KEY = new MissingValueCheck();
	
    /**
     * Constructor.
     */
    private MissingValueCheck() {
        super();
    }

    /**
     * @see org.eclipse.babel.core.message.checks.IMessageCheck#checkKey(
     * 	        org.eclipse.babel.core.message.MessagesBundleGroup,
     * 		    org.eclipse.babel.core.message.Message)
     */
    public boolean checkKey(
            MessagesBundleGroup messagesBundleGroup, Message message) {
        if (message == null || message.getValue() == null
                || message.getValue().length() == 0) {
            return true;
        }
        return false;
    }
}
