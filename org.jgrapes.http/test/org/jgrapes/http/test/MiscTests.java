package org.jgrapes.http.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import org.jgrapes.http.ResourcePattern;
import org.jgrapes.http.ResponseCreationSupport;
import org.jgrapes.http.ResponseCreationSupport.ResourceInfo;
import static org.junit.Assert.*;
import org.junit.Test;

public class MiscTests {

    @Test
    public void testPrefixRemoval() {
        assertEquals("rest",
            ResourcePattern.removeSegments("seg1/seg2/rest", 2));
    }

    @Test
    public void testResourceInfoForFiles() throws MalformedURLException {
        ResourceInfo info = ResponseCreationSupport.resourceInfo(
            Paths.get("test-resources").toUri().toURL());
        assertTrue(info.isDirectory());
        assertNotNull(info.getLastModifiedAt());
        info = ResponseCreationSupport.resourceInfo(Paths.get(
            "test-resources/static-content/index.html").toUri().toURL());
        assertFalse(info.isDirectory());
        assertNotNull(info.getLastModifiedAt());
    }

    @Test
    public void testResourceInfoForJars() throws MalformedURLException {
        @SuppressWarnings("resource")
        ClassLoader loader = new URLClassLoader(new URL[] {
            MiscTests.class.getResource("/static-content.jar") });
        URL resource = loader.getResource("only-in-jar/static-content");
        assertNotNull(resource);
        assertEquals("jar", resource.getProtocol());
        ResourceInfo info = ResponseCreationSupport.resourceInfo(resource);
        assertTrue(info.isDirectory());
        assertNotNull(info.getLastModifiedAt());
        resource = loader.getResource("only-in-jar/static-content/index.html");
        info = ResponseCreationSupport.resourceInfo(resource);
        assertFalse(info.isDirectory());
        assertNotNull(info.getLastModifiedAt());
    }
}
