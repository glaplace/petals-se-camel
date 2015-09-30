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
package org.ow2.petals.camel.se;

import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.commons.io.input.ReaderInputStream;
import org.custommonkey.xmlunit.Diff;
import org.eclipse.jdt.annotation.Nullable;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation;
import org.ow2.petals.commons.log.PetalsExecutionContext;
import org.ow2.petals.component.framework.jbidescriptor.generated.MEPType;
import org.ow2.petals.component.framework.junit.Component;
import org.ow2.petals.component.framework.junit.Message;
import org.ow2.petals.component.framework.junit.RequestMessage;
import org.ow2.petals.component.framework.junit.ResponseMessage;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration;
import org.ow2.petals.component.framework.junit.impl.ServiceConfiguration.ServiceType;
import org.ow2.petals.component.framework.junit.impl.message.WrappedFaultToConsumerMessage;
import org.ow2.petals.component.framework.junit.impl.message.WrappedRequestToProviderMessage;
import org.ow2.petals.component.framework.junit.impl.message.WrappedResponseToConsumerMessage;
import org.ow2.petals.component.framework.junit.impl.message.WrappedStatusToConsumerMessage;
import org.ow2.petals.component.framework.junit.rule.ComponentUnderTest;
import org.ow2.petals.component.framework.junit.rule.ServiceConfigurationFactory;
import org.ow2.petals.junit.rules.log.handler.InMemoryLogHandler;

public abstract class AbstractComponentTest extends AbstractTest {

    /**
     * Converters from Camel
     */
    private static final XmlConverter CONVERTER = new XmlConverter();

    protected static final String SE_CAMEL_JBI_NS = "http://petals.ow2.org/components/petals-se-camel/jbi/version-1.0";

    protected static final String CDK_JBI_NS = "http://petals.ow2.org/components/extensions/version-5";

    protected static final URL WSDL11 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-1.1.wsdl");

    protected static final URL WSDL20 = Thread.currentThread().getContextClassLoader()
            .getResource("tests/service-2.0.wsdl");

    protected static final URL VALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-valid.xml");

    protected static final URL INVALID_ROUTES = Thread.currentThread().getContextClassLoader()
            .getResource("tests/routes-invalid.xml");

    protected static final String HELLO_NS = "http://petals.ow2.org";

    protected static final String EXTERNAL_CAMEL_SERVICE_ID = "theConsumesId";

    protected static final String SU_NAME = "su-name";

    protected static final QName HELLO_INTERFACE = new QName(HELLO_NS, "HelloInterface");

    protected static final QName HELLO_SERVICE = new QName(HELLO_NS, "HelloService");

    protected static final QName HELLO_OPERATION = new QName(HELLO_NS, "sayHello");

    protected static final String EXTERNAL_ENDPOINT_NAME = "externalHelloEndpoint";

    protected static final long DEFAULT_TIMEOUT_FOR_COMPONENT_SEND = 10000;

    protected static final long TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE = 1000;

    protected static final InMemoryLogHandler IN_MEMORY_LOG_HANDLER = new InMemoryLogHandler();

    protected static final Component COMPONENT_UNDER_TEST = new ComponentUnderTest();

    /**
     * We use a class rule (i.e. static) so that the component lives during all the tests, this enables to test also
     * that successive deploy and undeploy do not create problems.
     */
    @ClassRule
    public static final TestRule chain = RuleChain.outerRule(IN_MEMORY_LOG_HANDLER).around(COMPONENT_UNDER_TEST);

    @BeforeClass
    public static void registerExternalService() {
        COMPONENT_UNDER_TEST.registerExternalServiceProvider(HELLO_SERVICE, EXTERNAL_ENDPOINT_NAME);
    }

    @BeforeClass
    public static void attachInMemoryLoggerToLogger() {
        COMPONENT_UNDER_TEST.addLogHandler(IN_MEMORY_LOG_HANDLER.getHandler());
    }

    /**
     * All log traces must be cleared before starting a unit test (because the log handler is static and lives during
     * the whole suite of tests)
     */
    @Before
    public void clearLogTraces() {
        IN_MEMORY_LOG_HANDLER.clear();
        // we want to clear them inbetween tests
        COMPONENT_UNDER_TEST.clearRequestsFromConsumer();
        COMPONENT_UNDER_TEST.clearResponsesFromProvider();
        // note: incoming messages queue can't be cleared because it is the job of the tested component to well handle
        // any situation
        // JUnit is susceptible to reuse threads apparently
        PetalsExecutionContext.clear();
    }

    /**
     * We undeploy services after each test (because the component is static and lives during the whole suite of tests)
     */
    @After
    public void after() {

        COMPONENT_UNDER_TEST.undeployAllServices();

        // asserts are ALWAYS a bug!
        final Formatter formatter = new SimpleFormatter();
        for (final LogRecord r : IN_MEMORY_LOG_HANDLER.getAllRecords()) {
            assertFalse("Got a log with an assertion: " + formatter.format(r), r.getThrown() instanceof AssertionError
                    || r.getMessage().contains("AssertionError"));
        }
    }

    protected static ServiceConfiguration createHelloConsumes() {
        final ServiceConfiguration consumes = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE,
                EXTERNAL_ENDPOINT_NAME, ServiceType.CONSUME);
        consumes.setOperation(HELLO_OPERATION.getLocalPart());
        consumes.setMEP(MEPType.IN_OUT);
        // let's use a smaller timeout time by default
        consumes.setTimeout(DEFAULT_TIMEOUT_FOR_COMPONENT_SEND);
        consumes.setParameter(new QName(SE_CAMEL_JBI_NS, "service-id"), EXTERNAL_CAMEL_SERVICE_ID);
        return consumes;
    }

    protected static ServiceConfigurationFactory createHelloService(final URL wsdl, final @Nullable Class<?> clazz,
            final @Nullable URL routes) throws Exception {

        final ServiceConfiguration provides = new ServiceConfiguration(HELLO_INTERFACE, HELLO_SERVICE, "autogenerate",
                ServiceType.PROVIDE, wsdl);

        provides.addServiceConfigurationDependency(createHelloConsumes());

        if (clazz != null) {
            provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "java-routes"), clazz.getName());
        }

        if (routes != null) {
            provides.setServicesSectionParameter(new QName(SE_CAMEL_JBI_NS, "xml-routes"),
                    new File(routes.toURI()).getName());
            provides.addResource(routes);
        }

        return new ServiceConfigurationFactory() {
            @Override
            public ServiceConfiguration create() {
                return provides;
            }
        };
    }

    protected static void deployHello(final String suName, final URL wsdl, final Class<?> clazz) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, clazz, null));
    }

    protected static void deployHello(final String suName, final URL wsdl, final URL routes) throws Exception {
        COMPONENT_UNDER_TEST.deployService(suName, createHelloService(wsdl, null, routes));
    }

    protected static ResponseMessage sendAndCheckConsumer(final RequestMessage request,
            final ExternalServiceImplementation consumer, final MessageChecks serviceChecks) throws Exception {
        return sendAndCheck(request, consumer, serviceChecks, TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE,
                MessageChecks.none(), TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE);
    }

    protected static ResponseMessage sendHelloIdentity(final String suName) throws Exception {
        return sendHelloIdentity(suName, MessageChecks.none());
    }

    protected static ResponseMessage sendHelloIdentity(final String suName, final MessageChecks serviceChecks)
            throws Exception {
        final String requestContent = "<aaa/>";
        final String responseContent = "<bbb/>";

        return sendHello(suName, requestContent, requestContent, responseContent, responseContent, true, true,
                serviceChecks);
    }

    protected static ResponseMessage sendHello(final String suName, @Nullable final String request,
            @Nullable final String expectedRequest, final String response, @Nullable final String expectedResponse,
            final boolean checkResponseNoError, final boolean checkResponseNoFault, final MessageChecks serviceChecks)
            throws Exception {

        MessageChecks reqChecks = isHelloRequest();
        if (expectedRequest != null) {
            reqChecks = reqChecks.andThen(hasXmlContent(expectedRequest));
        }
        reqChecks = reqChecks.andThen(serviceChecks);

        MessageChecks respChecks = MessageChecks.none();
        if (checkResponseNoError) {
            respChecks = respChecks.andThen(MessageChecks.errorExists());
        }
        if (checkResponseNoFault) {
            respChecks = respChecks.andThen(MessageChecks.faultExists());
        }
        if (expectedResponse != null) {
            respChecks = respChecks.andThen(hasXmlContent(expectedResponse));
        }

        return sendAndCheck(helloRequest(suName, request), ExternalServiceImplementation.outMessage(response),
                reqChecks, TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE, respChecks, TIMEOUTS_FOR_TESTS_SEND_AND_RECEIVE);
    }

    protected static MessageChecks isHelloRequest() {
        return new MessageChecks() {
            @Override
            public void checks(Message request) throws Exception {
                final MessageExchange exchange = request.getMessageExchange();
                assertEquals(exchange.getInterfaceName(), HELLO_INTERFACE);
                assertEquals(exchange.getService(), HELLO_SERVICE);
                assertEquals(exchange.getOperation(), HELLO_OPERATION);
                assertEquals(exchange.getEndpoint().getEndpointName(), EXTERNAL_ENDPOINT_NAME);
            }
        };
    }

    protected static MessageChecks hasXmlContent(final String expectedContent) {
        return new MessageChecks() {
            @Override
            public void checks(final Message request) throws Exception {
                final Diff diff = new Diff(CONVERTER.toDOMSource(request.getPayload(), null),
                        CONVERTER.toDOMSource(expectedContent));

                assertTrue(diff.similar());
            }
        };
    }

    protected static RequestMessage helloRequest(final String suName, final @Nullable String requestContent) {
        return new WrappedRequestToProviderMessage(COMPONENT_UNDER_TEST.getServiceConfiguration(suName),
                HELLO_OPERATION, AbsItfOperation.MEPPatternConstants.IN_OUT.value(), requestContent == null ? null
                        : new ReaderInputStream(new StringReader(requestContent)));
    }

    // /////////////////// GENERIC METHODS /////////////////////////

    protected static ResponseMessage send(final RequestMessage request, final ExternalServiceImplementation service,
            final long timeoutForth, final long timeoutBack) throws Exception {

        COMPONENT_UNDER_TEST.pushRequestToProvider(request);

        receiveAsExternalProvider(service, timeoutForth);

        return COMPONENT_UNDER_TEST.pollResponseFromProvider(timeoutBack);
    }

    protected static void receiveAsExternalProvider(final ExternalServiceImplementation service, final long timeout)
            throws Exception {
        final RequestMessage requestFromConsumer = COMPONENT_UNDER_TEST.pollRequestFromConsumer(timeout);
        if (requestFromConsumer != null) {
            final ResponseMessage responseToConsumer = service.provides(requestFromConsumer);
            COMPONENT_UNDER_TEST.pushResponseToConsumer(responseToConsumer);
        }
    }

    protected static ResponseMessage sendAndCheck(final RequestMessage request,
            final ExternalServiceImplementation service, final MessageChecks serviceChecks, final long serviceTimeout,
            final MessageChecks clientChecks, final long clientTimeout) throws Exception {

        final ResponseMessage responseMessage = send(request, service.with(serviceChecks), serviceTimeout,
                clientTimeout);

        clientChecks.checks(responseMessage);

        return responseMessage;
    }

    public static abstract class MessageChecks {

        /**
         * Checks to apply on a message.
         * 
         * @param message
         * @throws Exception
         */
        public abstract void checks(final Message message) throws Exception;

        public final MessageChecks andThen(final MessageChecks checks) {
            final MessageChecks me = this;
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    me.checks(message);
                    checks.checks(message);
                }
            };
        }

        public static MessageChecks none() {
            return new MessageChecks() {
                @Override
                public void checks(Message message) throws Exception {
                }
            };
        }

        public static MessageChecks matches(final Matcher<Message> matcher) {
            return new MessageChecks() {
                @Override
                public void checks(Message message) throws Exception {
                    assertThat(message, matcher);
                }
            };
        }

        public static MessageChecks faultExists() {
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    assertNull(message.getMessageExchange().getFault());
                }
            };
        }

        public static MessageChecks errorExists() {
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    assertNull(message.getMessageExchange().getError());
                }
            };
        }

        public static MessageChecks propertyNotExists(final String property) {
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    assertNull(message.getMessageExchange().getProperty(property));
                }
            };
        }

        public static MessageChecks propertyExists(final String property) {
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    assertNotNull(message.getMessageExchange().getProperty(property));
                }
            };
        }

        public static MessageChecks propertyEquals(final String property, final Object value) {
            return new MessageChecks() {
                @Override
                public void checks(final Message message) throws Exception {
                    assertEquals(message.getMessageExchange().getProperty(property), value);
                }
            };
        }
    }

    public static abstract class ExternalServiceImplementation {

        /**
         * The implementation of an external service.
         * 
         * @param request
         * @return
         * @throws Exception
         */
        public abstract ResponseMessage provides(final RequestMessage request) throws Exception;

        /**
         * Checks are executed before executing the consumer implementation
         * 
         * @param checks
         * @return
         */
        public final ExternalServiceImplementation with(final MessageChecks checks) {
            final ExternalServiceImplementation me = this;
            return new ExternalServiceImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    checks.checks(request);
                    return me.provides(request);
                }
            };
        }

        /**
         * A service that returns a ResponseMessage with the givent content as out message.
         * 
         * @param content
         * @return
         */
        public static ExternalServiceImplementation outMessage(final String content) {
            return new ExternalServiceImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    return new WrappedResponseToConsumerMessage(request.getMessageExchange(), new ReaderInputStream(
                            new StringReader(content)));
                }
            };
        }

        /**
         * A service that returns a ResponseMessage with the given content as fault.
         * 
         * @param content
         * @return
         */
        public static ExternalServiceImplementation faultMessage(final String content) {
            return new ExternalServiceImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    return new WrappedFaultToConsumerMessage(request.getMessageExchange(), new ReaderInputStream(
                            new StringReader(content)));
                }
            };
        }

        /**
         * A service that returns a ResponseMessage with the given status.
         * 
         * @param status
         * @return
         */
        public static ExternalServiceImplementation statusMessage(final ExchangeStatus status) {
            return new ExternalServiceImplementation() {
                @Override
                public ResponseMessage provides(final RequestMessage request) throws Exception {
                    return new WrappedStatusToConsumerMessage(request.getMessageExchange(), status);
                }
            };
        }
    }
}
