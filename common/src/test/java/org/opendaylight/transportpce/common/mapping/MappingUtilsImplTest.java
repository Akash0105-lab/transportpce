/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.Network;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtilsImplTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImplTest.class);
    private static MappingUtils mappingUtils;

    @BeforeAll
    static void setUp() throws InterruptedException, ExecutionException {
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
                .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = Files.newBufferedReader(
                Path.of("src/test/resources/network.json"),
                StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                    .transformIntoNormalizedNode(reader).orElseThrow();
            Network network = (Network) getDataStoreContextUtil()
                    .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                            .of(Network.QNAME), normalizedNode).getValue();
            WriteTransaction writeNetworkTransaction = getDataBroker().newWriteOnlyTransaction();
            writeNetworkTransaction.put(
                    LogicalDatastoreType.CONFIGURATION,
                    DataObjectIdentifier.builder(Network.class).build(),
                    network);
            writeNetworkTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load network ", e);
            fail("Cannot load network");
        }
        mappingUtils = new MappingUtilsImpl(getDataBroker());
    }

    @Test
    void getOpenRoadmVersionTest() {
        assertEquals(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1, mappingUtils.getOpenRoadmVersion("ROADM-C1"),
            "NodeInfo with ROADM-C1 as id should be 1.2.1 version");
        assertEquals(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, mappingUtils.getOpenRoadmVersion("ROADM-A1"),
            "NodeInfo with ROADM-A1 as id should be 2.2.1 version");
        assertNull(mappingUtils.getOpenRoadmVersion("nodes3"), "NodeInfo with nodes3 as id should not exist");
    }

    @Test
    void getMcCapabilitiesForNodeTest() {
        assertEquals(2, mappingUtils.getMcCapabilitiesForNode("ROADM-A1").size(),
            "Mc capabilities list size should be 2");
        assertTrue(mappingUtils.getMcCapabilitiesForNode("ROADM-A2").isEmpty(),
            "Mc capabilities list size should be empty");
    }
}
