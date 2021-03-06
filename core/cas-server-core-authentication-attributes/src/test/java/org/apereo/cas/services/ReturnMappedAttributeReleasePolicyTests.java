package org.apereo.cas.services;

import org.apereo.cas.CoreAttributesTestUtils;
import org.apereo.cas.config.CasCoreUtilConfiguration;
import org.apereo.cas.util.CollectionUtils;
import org.apereo.cas.util.spring.ApplicationContextProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import lombok.val;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Tag("Simple")
@SpringBootTest(classes = {
    RefreshAutoConfiguration.class,
    CasCoreUtilConfiguration.class
})
public class ReturnMappedAttributeReleasePolicyTests {

    private static final File JSON_FILE = new File(FileUtils.getTempDirectoryPath(), "returnMappedAttributeReleasePolicy.json");

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    @Test
    public void verifyAttributeMappingWorksForCollections() throws IOException {
        val map = new TreeMap();
        map.put("test1", "newTest1");
        map.put("test2", Stream.of("newTest2", "DaTest2").collect(Collectors.toList()));
        val policyWritten = new ReturnMappedAttributeReleasePolicy(map);
        MAPPER.writeValue(JSON_FILE, policyWritten);
        val policyRead = MAPPER.readValue(JSON_FILE, ReturnMappedAttributeReleasePolicy.class);
        assertEquals(policyWritten, policyRead);

        val mapValues = new HashMap<String, List<Object>>();
        mapValues.put("test1", List.of("AttributeValue1"));
        mapValues.put("test2", List.of("AttributeValue2"));

        val principal = CoreAttributesTestUtils.getPrincipal("user", mapValues);
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policyRead);

        val attributes = policyRead.getAttributes(principal, CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(attributes.containsKey("DaTest2"));
        assertTrue(attributes.containsKey("newTest2"));
        assertTrue(attributes.containsKey("newTest1"));
    }

    @Test
    public void verifySerializeAndReturnMappedAttributeReleasePolicyToJson() throws IOException {
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        allowedAttributes.put("keyOne", "valueOne");
        val wrap = CollectionUtils.<String, Object>wrap(allowedAttributes);
        val policyWritten = new ReturnMappedAttributeReleasePolicy(wrap);

        MAPPER.writeValue(JSON_FILE, policyWritten);
        val policyRead = MAPPER.readValue(JSON_FILE, ReturnMappedAttributeReleasePolicy.class);
        assertEquals(policyWritten, policyRead);
    }

    @Test
    public void verifyInlinedGroovyAttributes() {
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        allowedAttributes.put("attr1", "groovy { logger.debug('Running script...'); return 'DOMAIN\\\\' + attributes['uid'][0] }");
        val wrap = CollectionUtils.<String, Object>wrap(allowedAttributes);
        val policyWritten = new ReturnMappedAttributeReleasePolicy(wrap);
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policyWritten);
        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        val result = policyWritten.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.containsKey("attr1"));
        assertTrue(result.containsValue(List.of("DOMAIN\\" + CoreAttributesTestUtils.CONST_USERNAME)));
    }

    @Test
    public void verifyInlinedGroovyMultipleAttributes() {
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        allowedAttributes.put("attr1", "groovy { logger.debug('Running script...'); return ['one', 'two'] }");
        val wrap = CollectionUtils.<String, Object>wrap(allowedAttributes);
        val policyWritten = new ReturnMappedAttributeReleasePolicy(wrap);
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policyWritten);
        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        val result = policyWritten.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.containsKey("attr1"));
        assertEquals(2, Collection.class.cast(result.get("attr1")).size());
    }

    @Test
    public void verifyExternalGroovyAttributes() throws Exception {
        val file = new File(FileUtils.getTempDirectoryPath(), "script.groovy");
        val script = IOUtils.toString(new ClassPathResource("GroovyMappedAttribute.groovy").getInputStream(), StandardCharsets.UTF_8);
        FileUtils.write(file, script, StandardCharsets.UTF_8);
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        allowedAttributes.put("attr1", "file:" + file.getCanonicalPath());
        val wrap = CollectionUtils.<String, Object>wrap(allowedAttributes);
        val policyWritten = new ReturnMappedAttributeReleasePolicy(wrap);
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policyWritten);
        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        val result = policyWritten.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.containsKey("attr1"));
        val attr1 = result.get("attr1");
        assertTrue(attr1.contains("DOMAIN\\" + CoreAttributesTestUtils.CONST_USERNAME));
        assertTrue(attr1.contains("testing"));
    }


    @Test
    public void verifyMappingWithoutAttributeValue() {
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        val mappedAttribute = "urn:oid:0.9.2342.19200300.100.1.3";
        allowedAttributes.put("email", mappedAttribute);
        val policy = new ReturnMappedAttributeReleasePolicy(CollectionUtils.wrap(allowedAttributes));
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policy);
        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        var result = policy.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.isEmpty());

        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        principalAttributes.put("email", List.of("user@example.org"));
        result = policy.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.containsKey(mappedAttribute));
        assertEquals(List.of("user@example.org"), result.get(mappedAttribute));
    }

    @Test
    public void verifyClasspathGroovy() {
        val allowedAttributes = ArrayListMultimap.<String, Object>create();
        allowedAttributes.put("attr1", "classpath:GroovyMappedAttribute.groovy");
        val wrap = CollectionUtils.<String, Object>wrap(allowedAttributes);
        val policyWritten = new ReturnMappedAttributeReleasePolicy(wrap);
        val registeredService = CoreAttributesTestUtils.getRegisteredService();
        when(registeredService.getAttributeReleasePolicy()).thenReturn(policyWritten);
        val principalAttributes = new HashMap<String, List<Object>>();
        principalAttributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        val result = policyWritten.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, principalAttributes),
            CoreAttributesTestUtils.getService(), registeredService);
        assertTrue(result.containsKey("attr1"));
        val attr1 = result.get("attr1");
        assertTrue(attr1.contains("DOMAIN\\" + CoreAttributesTestUtils.CONST_USERNAME));
        assertTrue(attr1.contains("testing"));
    }

    @Test
    public void verifyInlinedGroovyWithCache() {
        val allowed1 = ArrayListMultimap.<String, Object>create();
        allowed1.put("attr1", "groovy { return 'v1' }");
        val p1 = new ReturnMappedAttributeReleasePolicy(CollectionUtils.wrap(allowed1));

        val service1 = CoreAttributesTestUtils.getRegisteredService();
        when(service1.getAttributeReleasePolicy()).thenReturn(p1);

        val attributes = new HashMap<String, List<Object>>();
        attributes.put("uid", List.of(CoreAttributesTestUtils.CONST_USERNAME));
        val result1 = p1.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, attributes),
            CoreAttributesTestUtils.getService(), service1);

        assertTrue(result1.containsKey("attr1"));
        assertTrue(result1.containsValue(List.of("v1")));

        val manager = ApplicationContextProvider.getScriptResourceCacheManager().get();
        val key = manager.getKeys().iterator().next();
        assertEquals("v1", manager.get(key).execute(ArrayUtils.EMPTY_OBJECT_ARRAY, String.class));

        val allowed2 = ArrayListMultimap.<String, Object>create();
        allowed2.put("attr1", "groovy { return 'v2' }");
        val p2 = new ReturnMappedAttributeReleasePolicy(CollectionUtils.wrap(allowed2));

        val service2 = CoreAttributesTestUtils.getRegisteredService();
        when(service2.getAttributeReleasePolicy()).thenReturn(p2);
        val result2 = p2.getAttributes(
            CoreAttributesTestUtils.getPrincipal(CoreAttributesTestUtils.CONST_USERNAME, attributes),
            CoreAttributesTestUtils.getService(), service2);
        assertTrue(result2.containsKey("attr1"));
        assertTrue(result2.containsValue(List.of("v2")));
    }
}
