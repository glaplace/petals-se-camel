/**
 * Copyright (c) 2015-2016 Linagora
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
 * along with this program/library; If not, see http://www.gnu.org/licenses/
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel.se.exceptions;


import org.ow2.petals.component.framework.util.ServiceEndpointOperationKey;

public class NotImplementedRouteException extends PetalsCamelSEException {

    private static final long serialVersionUID = -7084276404020652784L;

    private static final String MESSAGE_PATTERN = "There is no route for the operation %s of the service endpoint %s";

    public NotImplementedRouteException(final ServiceEndpointOperationKey eo) {
        super(String.format(MESSAGE_PATTERN, eo.getOperation(), eo.getEndpointName()));
    }

}
