package org.jgrapes.util.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jgrapes.util.events.ConfigurationUpdate;
import static org.junit.Assert.*;
import org.junit.Test;

public class PropertiesConvertion {

    @SuppressWarnings("unchecked")
    @Test
    public void testConversion() {
        // structured to flat
        var props = new HashMap<String, Object>();
        props.put("key1", "simpleProperty");
        props.put("key2", List.of("List", "of", "strings"));
        props.put("key3", Map.of("subkey1", "Value",
            "subkey2", List.of(1, 2, 3)));
        ConfigurationUpdate evt = new ConfigurationUpdate();
        evt.set("/", props);
        var flatProps = evt.values("/").get();
        assertEquals("simpleProperty", flatProps.get("key1"));
        assertEquals("List", flatProps.get("key2.0"));
        assertEquals("of", flatProps.get("key2.1"));
        assertEquals("strings", flatProps.get("key2.2"));
        assertEquals("1", flatProps.get("key3.subkey2.0"));
        assertEquals("2", flatProps.get("key3.subkey2.1"));
        assertEquals("3", flatProps.get("key3.subkey2.2"));
        assertEquals("Value", flatProps.get("key3.subkey1"));

        // Flat to structured
        evt.removePath("/");
        for (var prop : flatProps.entrySet()) {
            evt.add("/", prop.getKey(), prop.getValue());
        }
        var structured = evt.structured("/").get();
        assertEquals("simpleProperty", structured.get("key1"));
        List<String> list = (List<String>) structured.get("key2");
        assertEquals("List", list.get(0));
        assertEquals("of", list.get(1));
        assertEquals("strings", list.get(2));
        assertEquals("Value",
            ((Map<String, Map<String, String>>) structured.get("key3"))
                .get("subkey1"));
        list = ((Map<String, List<String>>) structured.get("key3"))
            .get("subkey2");
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertEquals("3", list.get(2));

        // Add one
        evt.add("/", "key3.subkey2.3", "4");
        structured = evt.structured("/").get();
        assertEquals("simpleProperty", structured.get("key1"));
        list = (List<String>) structured.get("key2");
        assertEquals("List", list.get(0));
        assertEquals("of", list.get(1));
        assertEquals("strings", list.get(2));
        assertEquals("Value",
            ((Map<String, Map<String, String>>) structured.get("key3"))
                .get("subkey1"));
        list = ((Map<String, List<String>>) structured.get("key3"))
            .get("subkey2");
        assertEquals("1", list.get(0));
        assertEquals("2", list.get(1));
        assertEquals("3", list.get(2));
        assertEquals("4", list.get(3));
    }

}
