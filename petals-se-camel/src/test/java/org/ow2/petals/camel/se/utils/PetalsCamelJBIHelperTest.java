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
package org.ow2.petals.camel.se.utils;

import java.net.URI;
import java.util.List;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Test;
import org.ow2.easywsdl.extensions.wsdl4complexwsdl.api.Description;
import org.ow2.easywsdl.wsdl.api.WSDLException;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfDescription.WSDLVersionConstants;
import org.ow2.petals.camel.component.utils.Pair;
import org.ow2.petals.camel.se.utils.PetalsCamelJBIHelper;
import org.ow2.petals.component.framework.jbidescriptor.CDKJBIDescriptorBuilder;
import org.ow2.petals.component.framework.jbidescriptor.generated.Jbi;
import org.ow2.petals.component.framework.util.WSDLUtilImpl;
import org.w3c.dom.Document;

import com.google.common.collect.Lists;

public class PetalsCamelJBIHelperTest extends Assert {

    @Test
    public void testJbiJavaOk() throws Exception {
        final Jbi jbi = getJBI("/sus/valid-java/jbi.xml");

        testPopulateRouteLists(jbi, 1, 0);
    }

    @Test
    public void testJbiXmlOk() throws Exception {
        final Jbi jbi = getJBI("/sus/valid-xml-wsdl-1.1/jbi.xml");

        testPopulateRouteLists(jbi, 0, 1);
    }

    @Test
    public void testWsdl11Ok() throws Exception {

        final Document doc = getWSDL("/sus/valid-xml-wsdl-1.1/service.wsdl", WSDLVersionConstants.WSDL11);

        final List<Pair<Pair<QName, URI>, String>> res = PetalsCamelJBIHelper.getOperationsAndServiceId(doc,
                new QName("http://petals.ow2.org", "HelloInterface"));

        assertEquals(res.size(), 1);
        assertEquals(res.get(0).b, "theProvidesId");

    }

    @Test
    public void testWsdl20Ok() throws Exception {

        final Document doc = getWSDL("/sus/valid-xml-wsdl-2.0/service.wsdl", WSDLVersionConstants.WSDL20);

        final List<Pair<Pair<QName, URI>, String>> res = PetalsCamelJBIHelper.getOperationsAndServiceId(doc,
                new QName("http://petals.ow2.org", "HelloInterface"));

        assertEquals(res.size(), 1);
        assertEquals(res.get(0).b, "theProvidesId");

    }

    private Document getWSDL(final String localPath, final WSDLVersionConstants version) throws WSDLException {
        final Description desc = WSDLUtilImpl.createWsdlDescription(this.getClass().getResource(localPath));
        Assert.assertEquals(desc.getVersion(), version);
        return WSDLUtilImpl.convertDescriptionToDocument(desc);
    }

    public Jbi getJBI(final String localPath) throws Exception {
        return CDKJBIDescriptorBuilder.getInstance().buildJavaJBIDescriptor(
                this.getClass().getResourceAsStream(localPath));
    }

    public void testPopulateRouteLists(final Jbi jbi, final int classesSize, final int xmlSizes) throws Exception {
        final List<String> classNames = Lists.newArrayList();
        final List<String> xmlNames = Lists.newArrayList();

        PetalsCamelJBIHelper.populateRouteLists(jbi.getServices(), classNames, xmlNames);

        assertEquals(classNames.size(), classesSize);
        assertEquals(xmlNames.size(), xmlSizes);
    }
}