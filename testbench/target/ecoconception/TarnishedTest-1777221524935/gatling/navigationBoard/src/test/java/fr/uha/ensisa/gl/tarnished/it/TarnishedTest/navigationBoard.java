package fr.uha.ensisa.gl.tarnished.it.TarnishedTest;

import java.time.Duration;
import java.util.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import io.gatling.javaapi.jdbc.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
import static io.gatling.javaapi.jdbc.JdbcDsl.*;

public class navigationBoard extends Simulation {

  private HttpProtocolBuilder httpProtocol = http
    .baseUrl("http://127.0.1.1:8091")
    .inferHtmlResources()
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.9")
    .userAgentHeader("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) HeadlessChrome/136.0.0.0 Safari/537.36");
  
  private Map<CharSequence, String> headers_0 = Map.ofEntries(
    Map.entry("Proxy-Connection", "keep-alive"),
    Map.entry("Upgrade-Insecure-Requests", "1")
  );
  
  private Map<CharSequence, String> headers_1 = Map.ofEntries(
    Map.entry("Accept", "*/*"),
    Map.entry("Proxy-Connection", "keep-alive")
  );
  
  private Map<CharSequence, String> headers_2 = Map.ofEntries(
    Map.entry("Accept", "text/css,*/*;q=0.1"),
    Map.entry("Proxy-Connection", "keep-alive")
  );
  
  private Map<CharSequence, String> headers_3 = Map.ofEntries(
    Map.entry("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"),
    Map.entry("Proxy-Connection", "keep-alive")
  );


  private ScenarioBuilder scn = scenario("navigationBoard")
    .exec(
      http("request_0")
        .get("/gl2526-tarnished/")
        .headers(headers_0)
        .resources(
          http("request_1")
            .get("/gl2526-tarnished/libs/bootstrap/bootstrap.bundle.min.js")
            .headers(headers_1),
          http("request_2")
            .get("/gl2526-tarnished/libs/bootstrap/bootstrap.min.css")
            .headers(headers_2)
        ),
      pause(1),
      http("request_3")
        .get("/favicon.ico")
        .headers(headers_3),
      pause(2),
      http("request_4")
        .get("/gl2526-tarnished/projects")
        .headers(headers_0)
        .check(status().is(404)),
      pause(20),
      http("request_5")
        .get("/gl2526-tarnished/")
        .headers(headers_0)
    );

  {
	  setUp(scn.injectOpen(rampUsers(20).during(10))).protocols(httpProtocol);
  }
}
