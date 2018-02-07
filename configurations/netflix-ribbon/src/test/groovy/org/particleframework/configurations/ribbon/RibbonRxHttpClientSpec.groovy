/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.configurations.ribbon

import com.netflix.client.config.CommonClientConfigKey
import org.particleframework.context.ApplicationContext
import org.particleframework.http.annotation.Controller
import org.particleframework.http.annotation.Get
import org.particleframework.http.client.Client
import org.particleframework.http.client.HttpClient
import org.particleframework.http.client.RxHttpClient
import org.particleframework.runtime.server.EmbeddedServer
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Inject

/**
 * @author graemerocher
 * @since 1.0
 */
class RibbonRxHttpClientSpec extends Specification {


    void "test basic ribbon load balancing configuration"() {

        given:"A discovery server, two micro services and a client"

        // the discovery server
        EmbeddedServer consulServer = ApplicationContext.run(EmbeddedServer)

        def serverConfig = [
                'consul.host'              : consulServer.host,
                'consul.port'              : consulServer.port,
                'particle.application.name': 'messageService'
        ]

        // the two micro services
        EmbeddedServer messageServer = ApplicationContext.run(EmbeddedServer, serverConfig)
        EmbeddedServer messageServer2 = ApplicationContext.run(EmbeddedServer, serverConfig)

        // the client
        ApplicationContext context = ApplicationContext.run([
                'consul.host': consulServer.host,
                'consul.port': consulServer.port,
                'ribbon.VipAddress': 'test',
                'ribbon.clients.messageService.VipAddress': 'bar'
        ])
        MessageService messageClient = context.getBean(MessageService)

        expect: "Different servers are called for each invocation of getMessage()"
        context.containsBean(RibbonRxHttpClient)
        messageClient.client instanceof RibbonRxHttpClient
        messageClient.client.loadBalancer.isPresent()
        ((RibbonLoadBalancer)messageClient.client.loadBalancer.get()).clientConfig
        ((RibbonLoadBalancer)messageClient.client.loadBalancer.get()).clientConfig.get(CommonClientConfigKey.VipAddress) == 'bar'
        messageClient.getMessage().startsWith("Server ")
        messageClient.getMessage() != messageClient.getMessage()


        cleanup:
        messageServer?.stop()
        messageServer2?.stop()
        context?.stop()
        consulServer?.stop()
    }

    void "test that load balancer evicts non available server after interval"() {
        given:"A discovery server, two micro services and a client"

        // the discovery server
        EmbeddedServer consulServer = ApplicationContext.run(EmbeddedServer)

        def serverConfig = [
                'consul.host'              : consulServer.host,
                'consul.port'              : consulServer.port,
                'particle.application.name': 'messageService'
        ]

        // the two micro services
        EmbeddedServer messageServer = ApplicationContext.run(EmbeddedServer, serverConfig)
        EmbeddedServer messageServer2 = ApplicationContext.run(EmbeddedServer, serverConfig)

        // the client with a short refresh interval
        ApplicationContext context = ApplicationContext.run([
                'consul.host': consulServer.host,
                'consul.port': consulServer.port,
                'ribbon.ServerListRefreshInterval':1000 // ms
        ])
        MessageService messageClient = context.getBean(MessageService)

        when:"The client is invoked"
        def msg1 = messageClient.getMessage()
        def msg2 = messageClient.getMessage()

        then:"Different servers are hit"
        msg1.startsWith("Server ")
        msg2.startsWith("Server ")
        msg1 != msg2

        when:"One of the servers is taken down"
        messageServer.stop()
        PollingConditions conditions = new PollingConditions(timeout: 3)

        then:"Eventually only one server is being used"
        conditions.eventually {
            messageClient.getMessage() == messageClient.getMessage()
        }

        cleanup:
        messageServer?.stop()
        messageServer2?.stop()
        context?.stop()
        consulServer?.stop()
    }


    static class MessageService {
        @Inject @Client('messageService') RxHttpClient client

        String getMessage() {
            client.retrieve('/message/value').blockingFirst()
        }
    }

    @Controller
    static class MessageController {
        @Inject EmbeddedServer embeddedServer

        @Get('/value')
        String value() {
            return "Server ${embeddedServer.port}"
        }
    }
}
