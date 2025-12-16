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

public class HelloIT{
	
	private static String port;
	
	@BeforeAll
	public static void findPort() {
		port = System.getProperty("servlet.port", "8080");
	}

	@Test
	public void hello() throws IOException {
		HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:" + port +"/hello").openConnection();
		{
			connection.connect();
			assertEquals(200, connection.getResponseCode());
			
			try (InputStream in = connection.getInputStream()) {
				String output = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
			}
		}
	}

}
