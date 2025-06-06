/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opendaylight.mdsal.common.api.CommitInfo.emptyFluentFuture;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;


public class CrossConnectImpl121Test {
    private CrossConnectImpl121 crossConnectImpl121 = null;
    private DeviceTransactionManager deviceTransactionManager = null;

    private MountPointService mountPointServiceMock = mock(MountPointService.class);
    private MountPoint mountPointMock = mock(MountPoint.class);
    private DataBroker dataBrokerMock = mock(DataBroker.class);
    private ReadWriteTransaction rwTransactionMock = mock(ReadWriteTransaction.class);
    private DeviceTransaction deviceTransaction = mock(DeviceTransaction.class);

    @BeforeEach
    void setup() {
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);

        //mock responses for deviceTransactionManager calls
        DataObjectIdentifier<RoadmConnections> deviceIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey("1"))
            .build();
        when(deviceTransactionManager.getDataFromDevice("deviceId",
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(Optional.of(mock(RoadmConnections.class)));
    }

    @Test
    void getCrossConnectTest() {
        Optional<RoadmConnections> res =
                crossConnectImpl121.getCrossConnect("deviceId", "1");
        assertTrue(res.isPresent(), "Optional object should have a value");
    }

    @Test
    void postCrossConnectTest() {
        when(mountPointServiceMock.findMountPoint(any())).thenReturn(Optional.of(mountPointMock));
        when(mountPointMock.getService(any())).thenReturn(Optional.of(dataBrokerMock));
        when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock);
        when(rwTransactionMock.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointServiceMock, 3000);
        crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.ONE);
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Optional<String> res = crossConnectImpl121.postCrossConnect("deviceId", "srcTp", "destTp", spectrumInformation);
        assertEquals(res.orElseThrow(), "srcTp-destTp-761:768");
    }

    @Test
    void setPowerLevelTest() {
        DataObjectIdentifier<RoadmConnections> deviceIID = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey("1"))
            .build();
        when(deviceTransactionManager.getDataFromDevice("deviceId",
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(Optional.of(new RoadmConnectionsBuilder().setConnectionNumber("1").build()));
        when(deviceTransactionManager.getDeviceTransaction("deviceId"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(deviceTransaction)));
        doReturn(emptyFluentFuture()).when(deviceTransaction).commit(anyLong(), any());
        crossConnectImpl121.setPowerLevel("deviceId", OpticalControlMode.Power, Decimal64.valueOf("100"), "1");
        assertTrue(true, "set Level should be true");
    }
}
