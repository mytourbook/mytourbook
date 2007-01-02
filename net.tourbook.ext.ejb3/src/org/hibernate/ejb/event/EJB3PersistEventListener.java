//$Id: EJB3PersistEventListener.java 9869 2006-05-04 03:17:03Z epbernard $
package org.hibernate.ejb.event;

import org.hibernate.event.EventSource;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Overrides the LifeCycle OnSave call to call the PrePersist operation
 *
 * @author Emmanuel Bernard
 */
public class EJB3PersistEventListener extends DefaultPersistEventListener implements CallbackHandlerConsumer {
	private EntityCallbackHandler callbackHandler;

	public void setCallbackHandler(EntityCallbackHandler callbackHandler) {
		this.callbackHandler = callbackHandler;
	}

	public EJB3PersistEventListener() {
		super();
	}

	public EJB3PersistEventListener(EntityCallbackHandler callbackHandler) {
		super();
		this.callbackHandler = callbackHandler;
	}

	@Override
	protected boolean invokeSaveLifecycle(Object entity, EntityPersister persister, EventSource source) {
		callbackHandler.preCreate( entity ); //always call the precreate event even if on safe vetoe it
		return super.invokeSaveLifecycle( entity, persister, source );
	}
}
