/*
 * JBoss, the OpenSource EJB server
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.hibernate.ejb.event;

import org.hibernate.event.PostUpdateEvent;
import org.hibernate.event.PostUpdateEventListener;
import org.hibernate.engine.EntityEntry;
import org.hibernate.engine.Status;

/**
 * @author <a href="mailto:kabir.khan@jboss.org">Kabir Khan</a>
 * @version $Revision: 9964 $
 */
public class EJB3PostUpdateEventListener implements PostUpdateEventListener, CallbackHandlerConsumer {
	EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PostUpdateEventListener() {
		super();
	}

	;

	public EJB3PostUpdateEventListener(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public void onPostUpdate(PostUpdateEvent event) {
		Object entity = event.getEntity();
		EntityEntry entry = (EntityEntry) event.getSession().getPersistenceContext().getEntityEntries().get( entity );
		//mimic the preUpdate filter
		if ( Status.DELETED != entry.getStatus() ) {
			callbackHandler.postUpdate( entity );
		}
	}
}
