package fr.uha.ensisa.gl.tarnished.it;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HelloIT {

    private static String host;
    private static String port;

    @BeforeAll
    public static void init() {
        host = System.getProperty("host", "localhost");
        port = System.getProperty("servlet.port", "8080");
    }

    @Test
    public void hello() throws IOException {
        String contextPath = System.getProperty("jetty.context.path", "/gl2526-tarnished");
        String url = "http://" + host + ":" + port + contextPath + "/hello";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

        connection.connect();
        assertEquals(200, connection.getResponseCode());

        try (InputStream in = connection.getInputStream()) {
            String output = new BufferedReader(new InputStreamReader(in))
                    .lines()
                    .collect(Collectors.joining("\n"));
            //on ne teste pas le contenu ici, juste que ça répond 200
        }
    }
}
