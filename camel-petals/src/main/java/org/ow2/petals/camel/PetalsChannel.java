/**
 * Copyright (c) 2015 Linagora
 * 
 * This program/library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This program/library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program/library; If not, see <http://www.gnu.org/licenses/>
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel;

import javax.jbi.messaging.MessagingException;

import org.ow2.petals.component.framework.api.exception.PEtALSCDKException;
import org.ow2.petals.component.framework.api.message.Exchange;

/**
 * Gives access to JBI messaging operations.
 * 
 * Used by the Camel Provider that needs to send message to Petals
 * 
 * @author vnoel
 *
 */
public interface PetalsChannel {
    
    /**
     * If timeout is less than 0 then we use the consumes or provides default timeout value, if equals to 0 then no
     * timeout.
     * 
     * @throws MessagingException
     * 
     */
    public boolean sendSync(Exchange exchange, long timeout) throws MessagingException;
    
    /**
     * If timeout is less than 0 then we use the consumes or provides default timeout value, if equals to 0 then no
     * timeout.
     * 
     * @throws MessagingException
     * 
     */
    public void sendAsync(Exchange exchange, long timeout, Runnable callback) throws MessagingException;

    public void send(Exchange exchange) throws MessagingException;

    public interface PetalsConsumesChannel extends PetalsChannel {

        public Exchange newExchange() throws MessagingException, PEtALSCDKException;

    }

    public interface PetalsProvidesChannel extends PetalsChannel {

    }
}