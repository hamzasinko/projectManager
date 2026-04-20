package fr.uha.ensisa.gl.tarnished.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PathHelper Tests")
class PathHelperTest {

    private PathHelper pathHelper;

    @BeforeEach
    void setUp() {
        pathHelper = new PathHelper();
    }

    @Test
    @DisplayName("getContextPath should return '/' when no request context")
    void testGetContextPath_NoRequestContext() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);
            
            String result = pathHelper.getContextPath();
            
            assertEquals("/", result);
        }
    }

    @Test
    @DisplayName("getContextPath should return '/' for empty context path")
    void testGetContextPath_EmptyContextPath() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.getContextPath();
            
            assertEquals("/", result);
        }
    }

    @Test
    @DisplayName("getContextPath should return context path with trailing slash")
    void testGetContextPath_WithContextPath() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("/gl2526-tarnished");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.getContextPath();
            
            assertEquals("/gl2526-tarnished/", result);
        }
    }

    @Test
    @DisplayName("getContextPath should not add duplicate trailing slash")
    void testGetContextPath_AlreadyHasTrailingSlash() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("/gl2526-tarnished/");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.getContextPath();
            
            assertEquals("/gl2526-tarnished/", result);
        }
    }

    @Test
    @DisplayName("buildPath should handle null relative path")
    void testBuildPath_NullRelativePath() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);
            
            String result = pathHelper.buildPath(null);
            
            assertEquals("/", result);
        }
    }

    @Test
    @DisplayName("buildPath should handle empty relative path")
    void testBuildPath_EmptyRelativePath() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);
            
            String result = pathHelper.buildPath("");
            
            assertEquals("/", result);
        }
    }

    @Test
    @DisplayName("buildPath should handle relative path starting with slash")
    void testBuildPath_RelativePathStartsWithSlash() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("/gl2526-tarnished");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.buildPath("/project/list");
            
            assertEquals("/gl2526-tarnished/project/list", result);
        }
    }

    @Test
    @DisplayName("buildPath should add slash for relative path without leading slash")
    void testBuildPath_RelativePathWithoutLeadingSlash() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("/gl2526-tarnished");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.buildPath("project/list");
            
            assertEquals("/gl2526-tarnished/project/list", result);
        }
    }

    @Test
    @DisplayName("buildPath should handle root context path")
    void testBuildPath_RootContextPath() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContextPath("");
            ServletRequestAttributes attributes = new ServletRequestAttributes(request);
            
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            
            String result = pathHelper.buildPath("project/list");
            
            assertEquals("/project/list", result);
        }
    }

    @Test
    @DisplayName("redirect should return 'redirect:/' for null path")
    void testRedirect_NullPath() {
        String result = pathHelper.redirect(null);
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("redirect should return 'redirect:/' for empty path")
    void testRedirect_EmptyPath() {
        String result = pathHelper.redirect("");
        assertEquals("redirect:/", result);
    }

    @Test
    @DisplayName("redirect should add 'redirect:' prefix for path without slash")
    void testRedirect_PathWithoutSlash() {
        String result = pathHelper.redirect("project/list");
        assertEquals("redirect:/project/list", result);
    }

    @Test
    @DisplayName("redirect should handle path starting with slash")
    void testRedirect_PathWithSlash() {
        String result = pathHelper.redirect("/project/list");
        assertEquals("redirect:/project/list", result);
    }

    @Test
    @DisplayName("redirectView should return ModelAndView with redirect")
    void testRedirectView() {
        try (MockedStatic<RequestContextHolder> mockedStatic = mockStatic(RequestContextHolder.class)) {
            mockedStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(null);
            
            ModelAndView mav = pathHelper.redirectView("project/list");
            
            assertNotNull(mav);
            assertEquals("redirect:/project/list", mav.getViewName());
        }
    }

    @Test
    @DisplayName("redirectView should handle null path")
    void testRedirectView_NullPath() {
        ModelAndView mav = pathHelper.redirectView(null);
        
        assertNotNull(mav);
        assertEquals("redirect:/", mav.getViewName());
    }
}
