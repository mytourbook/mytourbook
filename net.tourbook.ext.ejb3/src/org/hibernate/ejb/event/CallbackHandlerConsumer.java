//$Id: CallbackHandlerConsumer.java 8717 2005-11-30 14:37:38Z epbernard $
package org.hibernate.ejb.event;

/**
 * @author Emmanuel Bernard
 */
public interface CallbackHandlerConsumer {
	void setCallbackHandler(EntityCallbackHandler callbackHandler);
}
