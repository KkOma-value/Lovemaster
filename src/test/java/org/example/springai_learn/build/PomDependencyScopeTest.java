package org.example.springai_learn.build;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PomDependencyScopeTest {

    @Test
    void fontAsian_mustNotBeTestScope() throws Exception {
        Path pomPath = Path.of(System.getProperty("user.dir"), "pom.xml");
        assertTrue(Files.exists(pomPath), "pom.xml should exist at project root: " + pomPath);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        Document doc = factory.newDocumentBuilder().parse(Files.newInputStream(pomPath));

        NodeList dependencies = doc.getElementsByTagName("dependency");
        boolean found = false;
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element dep = (Element) dependencies.item(i);
            String groupId = text(dep, "groupId");
            String artifactId = text(dep, "artifactId");
            if ("com.itextpdf".equals(groupId) && "font-asian".equals(artifactId)) {
                found = true;
                String scope = text(dep, "scope");
                assertFalse("test".equalsIgnoreCase(scope), "com.itextpdf:font-asian must be available at runtime (scope must not be test)");
            }
        }

        assertTrue(found, "Expected dependency com.itextpdf:font-asian to be declared in pom.xml");
    }

    private static String text(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            return null;
        }
        String value = list.item(0).getTextContent();
        return value == null ? null : value.trim();
    }
}
