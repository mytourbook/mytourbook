//$Id: EJB3PersistOnFlushEventListener.java 8970 2006-01-03 15:52:30Z epbernard $
package org.hibernate.ejb.event;

import org.hibernate.engine.CascadingAction;

/**
 * @author Emmanuel Bernard
 */
public class EJB3PersistOnFlushEventListener extends EJB3PersistEventListener {
	protected CascadingAction getCascadeAction() {
		return CascadingAction.PERSIST_ON_FLUSH;
	}
}
