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
package org.eclipse.babel.core.message.checks.proximity;

/**
 * Analyse the proximity of two objects (i.e., how similar they are) and return
 * a proximity level between zero and one.  The higher the return value is, 
 * the closer the two objects are to each other.  "One" does not need to mean 
 * "identical", but it has to be the closest match and analyser can 
 * potentially achieve.
 * @author Pascal Essiembre (pascal@essiembre.com)
 */
public interface IProximityAnalyzer {
    /**
     * Analyses two objects and return the proximity level.
     * @param obj1 first object to analyse
     * @param obj2 second object to analyse
     * @return proximity level
     */
    double analyse(Object obj1, Object obj2);
}
