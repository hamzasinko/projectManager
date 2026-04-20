package fr.uha.ensisa.gl;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

/**
 * Unit test for simple App (JUnit 5).
 */
public class AppTest {

    @Test
    public void testApp() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer));

        try {
            App.main(new String[]{});
        } finally {
            System.setOut(original);
        }

        assertTrue(buffer.toString().contains("Hello World!"));
    }
}
