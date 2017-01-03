import controllers.shib.MockShibboleth;

import lombok.extern.slf4j.Slf4j;

import models.Person;

import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.Test;

import play.mvc.Http;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.test.FunctionalTest;

@Slf4j
public class ShibbolethTest extends FunctionalTest {


  /**
   * The basic test to authenticate as a user using Shibboleth.
   */
  @Test
  public void testShibbolethAuthentication() {
    assertThat(
        Person.find("SELECT p FROM Person p where email = ?" , "cristian.lucchesi@iit.cnr.it")
          .first(),
        IsNull.notNullValue());
    // Set up the mock shibboleth attributes that
    // will be used to authenticate the next user which
    // logins in.
    MockShibboleth.removeAll();
    MockShibboleth.set("eppn","cristian.lucchesi@iit.cnr.it");

    final String loginUrl = Router.reverse("shib.Shibboleth.login").url;
    Response response = httpGet(loginUrl,true);
    assertIsOk(response);
    log.debug("response.contentType = {}", response.contentType);
    assertContentType("text/html", response);
    assertTrue(response.cookies.get("PLAY_SESSION").value.contains("cristian.lucchesi"));

  }

  @AfterClass
  public static void cleanup() {
    MockShibboleth.reload();
  }


  /**
   * Fixed a bug in the default version of this method. It dosn't follow redirects properly.
   */
  public static Response httpGet(Object url, boolean followRedirect) {
    Response response = GET(url);
    if (Http.StatusCode.FOUND == response.status && followRedirect) {
      String redirectedTo = response.getHeader("Location");
      response = httpGet(redirectedTo,followRedirect);
    }
    return response;
  }
}
