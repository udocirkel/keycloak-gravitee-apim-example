/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.udocirkel.example.kcgravitee;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import de.udocirkel.example.kcgravitee.gravitee.policy.TokenExchangePolicy;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.Map;

import de.udocirkel.example.kcgravitee.gravitee.policy.TokenExchangePolicyConfiguration;
import io.gravitee.apim.gateway.tests.sdk.AbstractPolicyTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.fakes.Header1Policy;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.definition.model.Api;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@Disabled
@GatewayTest
@DeployApi("/de/udocirkel/example/kcgravitee/apis/TokenExchange.json")
public class TokenExchangePolicyIntegrationTest extends AbstractPolicyTest<TokenExchangePolicy, TokenExchangePolicyConfiguration> {

    // You can override configuration of the WebClient with the following lines.
    // @WebClientOptionsInject
    // public WebClientOptions options = new WebClientOptions().setDefaultHost("localhost").setDefaultPort(gatewayPort());

    @Override
    public void configureApi(Api api) {
        // Here you can override each api to deploy programmatically.
    }

    @Override
    public void configureWireMock(WireMockConfiguration configuration) {
        // Here you can override the wiremock configuration. Wiremock is the component mocking your backends.
    }

    @Override
    public void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        // Here you can override the gateway configuration if needed.
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        // Here you can register custom testing policies, for example:
        policies.put("header-policy1", PolicyBuilder.build("header-policy1", Header1Policy.class));

        // you have the same capabilities with:
        // - configureConnectors(Map<String, ConnectorPlugin> connectors)
        // - configureResources(Map<String, ResourcePlugin> resources)
        // - configureReporters(Map<String, Reporter> reporters)
    }

    @Test
    @DisplayName("Should test the policy deployed on an in memory gateway")
    public void shouldTestTokenExchangePolicyDeployedOnAnInMemoryGateway(WebClient client) throws Exception {
        wiremock.stubFor(get("/backend").willReturn(ok("response from backend")));

        final TestObserver<HttpResponse<Buffer>> obs = client.get("/test").rxSend().test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.bodyAsString()).isEqualTo("response from backend");
                return true;
            })
        .assertNoErrors();

        wiremock.verify(getRequestedFor(urlPathEqualTo("/backend")).withHeader("X-DummyHeader", equalTo("TokenExchange")));
    }
}