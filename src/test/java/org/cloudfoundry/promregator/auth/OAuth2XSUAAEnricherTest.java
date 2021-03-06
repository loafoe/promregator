package org.cloudfoundry.promregator.auth;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.methods.HttpGet;
import org.cloudfoundry.promregator.JUnitTestUtils;
import org.cloudfoundry.promregator.config.OAuth2XSUAAAuthenticationConfiguration;
import org.cloudfoundry.promregator.mockServer.AuthenticationMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class OAuth2XSUAAEnricherTest {
	private String oAuthServerResponse = "{\n" + 
			"    \"access_token\": \"someAccessToken\",\n" + 
			"    \"token_type\": \"bearer\",\n" + 
			"    \"expires_in\": 43199,\n" + 
			"    \"scope\": \"dummyScope.AdminOnboarding uaa.resource\",\n" + 
			"    \"jti\": \"01234567890\"\n" + 
			"}";
	
	private AuthenticationMockServer ams;
	
	@BeforeEach
	public void startUpAuthenticationServer() throws IOException {
		this.ams = new AuthenticationMockServer();
		this.ams.start();
	}
	
	@AfterEach
	public void tearDownAuthenticationServer() {
		this.ams.stop();
	}
	
	@AfterAll
	public static void cleanupEnvironment() {
		JUnitTestUtils.cleanUpAll();
	}

	@Test
	public void testAppropriateJWTCall() {
		this.ams.getOauthTokenHandler().setResponse(this.oAuthServerResponse);
		
		OAuth2XSUAAAuthenticationConfiguration authenticatorConfig = new OAuth2XSUAAAuthenticationConfiguration();
		authenticatorConfig.setClient_id("client_id");
		authenticatorConfig.setClient_secret("client_secret");
		authenticatorConfig.setTokenServiceURL("http://localhost:9001/oauth/token");
		
		OAuth2XSUAAEnricher subject = new OAuth2XSUAAEnricher(authenticatorConfig);
		
		HttpGet mockGet = Mockito.mock(HttpGet.class);
		Mockito.when(mockGet.getURI()).thenAnswer((Answer<URI>) invocation -> new URI("http://localhost/target"));
		
		subject.enrichWithAuthentication(mockGet);

		verify(mockGet).setHeader("Authorization", "Bearer someAccessToken");
	}

	@Test
	public void testJWTCallIsBuffered() {
		this.ams.getOauthTokenHandler().setResponse(this.oAuthServerResponse);
		
		OAuth2XSUAAAuthenticationConfiguration authenticatorConfig = new OAuth2XSUAAAuthenticationConfiguration();
		authenticatorConfig.setClient_id("client_id");
		authenticatorConfig.setClient_secret("client_secret");
		authenticatorConfig.setTokenServiceURL("http://localhost:9001/oauth/token");
		
		OAuth2XSUAAEnricher subject = new OAuth2XSUAAEnricher(authenticatorConfig);
		
		HttpGet mockGet = Mockito.mock(HttpGet.class);
		Mockito.when(mockGet.getURI()).thenAnswer(new Answer<URI>() {

			@Override
			public URI answer(InvocationOnMock invocation) throws Throwable {
				return new URI("http://localhost/target");
			}
			
		});
		
		// first call will trigger OAuth request
		subject.enrichWithAuthentication(mockGet);
		
		// second one should not
		subject.enrichWithAuthentication(mockGet);
		
		assertThat(this.ams.getOauthTokenHandler().getCounterCalled()).isEqualTo(1);
	}
	
}
