package io.fabric8.kubernetes.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InlineTest {

    @Test
    public void testIntOrString() throws JsonProcessingException {
        ServicePort port = new ServicePortBuilder().withNewTargetPort(2181).build();
        assertEquals(2181, port.getTargetPort().getIntVal().intValue());

        port = new ServicePortBuilder().withNewTargetPort("2181").build();
        assertEquals("2181", port.getTargetPort().getStrVal());
    }
}