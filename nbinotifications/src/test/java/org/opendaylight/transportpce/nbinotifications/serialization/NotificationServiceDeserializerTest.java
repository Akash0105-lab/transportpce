/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.serialization;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.get.notifications.process.service.output.NotificationsProcessService;

public class NotificationServiceDeserializerTest extends AbstractTest {

    @Test
    void deserializeTest() throws IOException {
        JsonStringConverter<NotificationProcessService> converter = new JsonStringConverter<>(
                getDataStoreContextUtil().getBindingDOMCodecServices());
        NotificationServiceDeserializer deserializer = new NotificationServiceDeserializer();
        Map<String, Object> configs = Map.of(ConfigConstants.CONVERTER, converter);
        deserializer.configure(configs, false);
        NotificationsProcessService readEvent = deserializer.deserialize("Test",
                Files.readAllBytes(Path.of("src/test/resources/event.json")));
        deserializer.close();
        assertEquals("service1", readEvent.getServiceName(), "Service name should be service1");
    }
}
