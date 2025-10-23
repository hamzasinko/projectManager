package fr.uha.ensisa.gl.tarnished.projest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class IssueTest {
    fr.uha.ensisa.gl.tarnished.projest.Issue sut;

    @BeforeEach
    void createIssue() {
        sut = new fr.uha.ensisa.gl.tarnished.projest.Issue();
    }

    @Test
    @DisplayName("An issue should have a name")
    void setName() {
        assertNull(sut.getName());
        String name = "A sample issue name";
        sut.setName(name);
        assertEquals(name, sut.getName());
    }
}
package fr.uha.ensisa.gl.tarnished.projest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class IssueTest {
    fr.uha.ensisa.gl.tarnished.projest.Issue sut;

    @BeforeEach
    void createIssue() {
        sut = new fr.uha.ensisa.gl.tarnished.projest.Issue();
    }

    @Test
    @DisplayName("An issue should have a name")
    void setName() {
        assertNull(sut.getName());
        String name = "A sample issue name";
        sut.setName(name);
        assertEquals(name, sut.getName());
    }
}
