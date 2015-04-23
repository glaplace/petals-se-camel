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
package org.ow2.petals.camel.component.mocks;

import java.util.Map;
import java.util.logging.Logger;

import org.apache.camel.CamelContext;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.ow2.petals.camel.PetalsCamelContext;
import org.ow2.petals.camel.PetalsChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsConsumesChannel;
import org.ow2.petals.camel.PetalsChannel.PetalsProvidesChannel;
import org.ow2.petals.camel.PetalsCamelRoute;
import org.ow2.petals.camel.ServiceEndpointOperation;
import org.ow2.petals.camel.ServiceEndpointOperation.ServiceType;
import org.ow2.petals.camel.exceptions.UnknownServiceException;
import org.ow2.petals.component.framework.util.EndpointOperationKey;

import com.google.common.collect.Maps;

public class PetalsCamelContextMock implements PetalsCamelContext {

    private final Map<EndpointOperationKey, PetalsChannel> channels = Maps.newHashMap();
    
    private final Map<String, ServiceEndpointOperation> seos = Maps.newHashMap();

    private final Map<EndpointOperationKey, PetalsCamelRoute> ppos = Maps.newHashMap();

    private final CamelContext context;

    private final Logger logger = Logger.getLogger(PetalsCamelContextMock.class.getName());

    public PetalsCamelContextMock(final CamelContext context) {
        this.context = context;
    }

    public void addMockService(final String serviceId, final ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel pC = this.channels.put(key,
                EasyMock.createMock(seo.getType() == ServiceType.CONSUMES ? PetalsConsumesChannel.class
                        : PetalsProvidesChannel.class));
        Assert.assertNull(pC);
        final ServiceEndpointOperation pS = this.seos.put(serviceId, seo);
        Assert.assertNull(pS);
    }

    @Override
    public ServiceEndpointOperation getService(final String serviceId) throws UnknownServiceException {
        final ServiceEndpointOperation seo = this.seos.get(serviceId);
        if (seo == null) {
            throw new UnknownServiceException(serviceId);
        }
        return seo;
    }

    @Override
    public PetalsConsumesChannel getConsumesChannel(final ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel channel = this.channels.get(key);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsConsumesChannel);
        return (PetalsConsumesChannel) channel;
    }

    @Override
    public PetalsProvidesChannel getProvidesChannel(final ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsChannel channel = this.channels.get(key);
        Assert.assertNotNull(channel);
        Assert.assertTrue(channel instanceof PetalsProvidesChannel);
        return (PetalsProvidesChannel) channel;
    }

    @Override
    public void registerRoute(final ServiceEndpointOperation seo, final PetalsCamelRoute ppo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsCamelRoute put = this.ppos.put(key, ppo);
        assert put == null;
    }

    @Override
    public void unregisterRoute(ServiceEndpointOperation seo) {
        final EndpointOperationKey key = new EndpointOperationKey(seo.getEndpoint(), seo.getInterface(),
                seo.getOperation());
        final PetalsCamelRoute removed = this.ppos.remove(key);
        assert removed == null;
    }

    @Override
    public CamelContext getCamelContext() {
        return this.context;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

}
