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
package org.eclipse.babel.core.message;

/**
 * Exception thrown when a message-related operation raised a problem. 
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public class MessageException extends RuntimeException {

    /** For serialisation. */
    private static final long serialVersionUID = 5702621096263524623L;

    /**
     * Creates a new <code>MessageException</code>.
     */
    public MessageException() {
        super();
    }

    /**
     * Creates a new <code>MessageException</code>.
     * @param message exception message
     */
    public MessageException(String message) {
        super(message);
    }

    /**
     * Creates a new <code>MessageException</code>.
     * @param message exception message
     * @param cause root cause
     */
    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new <code>MessageException</code>.
     * @param cause root cause
     */
    public MessageException(Throwable cause) {
        super(cause);
    }
}
