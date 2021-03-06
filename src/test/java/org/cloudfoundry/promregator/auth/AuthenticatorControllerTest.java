package org.cloudfoundry.promregator.auth;

import java.util.List;

import org.cloudfoundry.promregator.config.PromregatorConfiguration;
import org.cloudfoundry.promregator.config.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AuthenticatorControllerSpringApplication.class)
@TestPropertySource(locations="default.properties")
public class AuthenticatorControllerTest {

	@Autowired
	private AuthenticatorController subject;
	
	@Autowired
	private PromregatorConfiguration promregatorConfiguration;
	
	@Test
	public void testDefaultConfigurationCheck() {
		assertThat(subject).isNotNull();
		
		AuthenticationEnricher auth0 = subject.getAuthenticationEnricherById("unittestAuth0");
		assertThat(auth0).isInstanceOf(BasicAuthenticationEnricher.class);
		
		AuthenticationEnricher auth1 = subject.getAuthenticationEnricherById("unittestAuth1");
		assertThat(auth1).isInstanceOf(OAuth2XSUAAEnricher.class);

		AuthenticationEnricher auth2 = subject.getAuthenticationEnricherById("somethingInvalid");
		assertThat(auth2).isNull();

		AuthenticationEnricher auth3 = subject.getAuthenticationEnricherById(null);
		assertThat(auth3).isNull();
		
		List<Target> targets = this.promregatorConfiguration.getTargets();
		Target target0 = targets.get(0);
		
		assertThat("testapp").isEqualTo(target0.getApplicationName()); // only as safety for this test (not really a test subject)
		assertThat(auth0).isEqualTo(subject.getAuthenticationEnricherByTarget(target0));
		
		Target target1 = targets.get(1);
		
		assertThat("testapp2").isEqualTo(target1.getApplicationName()); // only as safety for this test (not really a test subject)
		assertThat(auth1).isEqualTo(subject.getAuthenticationEnricherByTarget(target1));
		
		assertThat(subject.getAuthenticationEnricherByTarget(new Target())).isInstanceOf(NullEnricher.class);
		assertThat(subject.getAuthenticationEnricherByTarget(null)).isInstanceOf(NullEnricher.class);
	}

}
