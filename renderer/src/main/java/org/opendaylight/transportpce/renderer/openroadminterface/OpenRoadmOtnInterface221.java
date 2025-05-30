/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev171215.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OduFunctionIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.Opu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.parent.odu.allocation.ParentOduAllocation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmOtnInterface221 {

    private static final String MAPPING_ERROR_EXCEPTION_MESSAGE =
        "Unable to get mapping from PortMapping for node % and logical connection port %s";
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory
            .getLogger(OpenRoadmOtnInterface221.class);

    public OpenRoadmOtnInterface221(PortMapping portMapping,
            OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEth1GInterface(String nodeId,
            String logicalConnPoint) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
                .setSpeed(Uint32.valueOf(1000));
        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.VALUE,
                logicalConnPoint + "-ETHERNET1G");
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build()).build());
        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postOTNEquipmentState(nodeId,
                portMap.getSupportingCircuitPackName(), true);
        this.portMapping.updateMapping(nodeId, portMap);
        String ethernetInterfaceName = ethInterfaceBldr.getName();

        return ethernetInterfaceName;
    }

    private void throwException(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        throw new OpenRoadmInterfaceException(String.format(
                "Unable to get mapping from PortMapping for node % and logical connection port %s",
                nodeId, logicalConnPoint));
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, InterfaceType type, String key) {
        return new InterfaceBuilder()
                // .setDescription(" TBD ")
                // .setCircuitId(" TBD ")
                .setSupportingCircuitPackName(
                        portMap.getSupportingCircuitPackName())
                .setSupportingPort(portMap.getSupportingPort())
                .setAdministrativeState(AdminStates.InService)
                // TODO get rid of unchecked cast warning
                .setType(type).setName(key).withKey(new InterfaceKey(key));
    }

    public String createOpenRoadmEth10GInterface(String nodeId,
            String logicalConnPoint) throws OpenRoadmInterfaceException {
        Mapping portMap = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throwException(nodeId, logicalConnPoint);
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
                // .setAutoNegotiation(EthAttributes.AutoNegotiation.Disabled)
                .setSpeed(Uint32.valueOf(10000));
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.VALUE,
                logicalConnPoint + "-ETHERNET10G").addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build())
                        .build());
        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        this.openRoadmInterfaces.postOTNEquipmentState(nodeId,
                portMap.getSupportingCircuitPackName(), true);
        this.portMapping.updateMapping(nodeId, portMap);
        String ethernetInterfaceName = ethInterfaceBldr.getName();

        return ethernetInterfaceName;
    }

    public String createOpenRoadmOdu2eInterface(String nodeId, String logicalConnPoint, String serviceName,
            boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ,
            String payloadType) throws OpenRoadmInterfaceException {

        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(mapping, OtnOdu.VALUE,
            logicalConnPoint + "-ODU2e" + ":" + serviceName);
        if (mapping.getSupportingOdu4() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingOdu4());
        }
        if (mapping.getSupportingEthernet() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingEthernet());
        }

        OduFunctionIdentity oduFunction;
        MonitoringMode monitoringMode;
        Opu opu = null;
        ParentOduAllocation parentOduAllocation = null;
        if (isCTP) {
            oduFunction = ODUCTP.VALUE;
            monitoringMode = MonitoringMode.Monitored;
            Set<Uint16> tribSlots = new HashSet<>();
            Uint16 newIdx = Uint16.valueOf(tribSlotIndex);
            tribSlots.add(newIdx);
            IntStream.range(tribSlotIndex, tribSlotIndex + 8)
                    .forEach(nbr -> tribSlots.add(Uint16.valueOf(nbr)));
            parentOduAllocation = new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots)
                    .build();
        } else {
            oduFunction = ODUTTPCTP.VALUE;
            monitoringMode = MonitoringMode.Terminated;
            opu = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .build();
        }
        OduBuilder oduIfBuilder = new OduBuilder()
            .setRate(ODU2e.VALUE)
            .setOduFunction(oduFunction)
            .setMonitoringMode(monitoringMode)
            .setOpu(opu)
            .setParentOduAllocation(parentOduAllocation);
        if (apiInfoA != null) {
            oduIfBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            oduIfBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        if (!isCTP) {
            LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
            this.portMapping.updateMapping(nodeId, mapping);
        }
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu0Interface(String nodeId, String logicalConnPoint, String servicename,
        boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ,
            String payloadType) throws OpenRoadmInterfaceException {

        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(mapping, OtnOdu.VALUE,
            logicalConnPoint + "-ODU0" + ":" + servicename);
        if (mapping.getSupportingOdu4() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingOdu4());
        }
        if (mapping.getSupportingEthernet() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingEthernet());
        }

        OduFunctionIdentity oduFunction;
        MonitoringMode monitoringMode;
        Opu opu = null;
        ParentOduAllocation parentOduAllocation = null;
        if (isCTP) {
            oduFunction = ODUCTP.VALUE;
            monitoringMode = MonitoringMode.Monitored;
            Set<Uint16> tribSlots = new HashSet<>();
            Uint16 newIdx = Uint16.valueOf(tribSlotIndex);
            tribSlots.add(newIdx);
            IntStream.range(tribSlotIndex, tribSlotIndex + 8)
                    .forEach(nbr -> tribSlots.add(Uint16.valueOf(nbr)));
            parentOduAllocation = new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots)
                    .build();
        } else {
            oduFunction = ODUTTPCTP.VALUE;
            monitoringMode = MonitoringMode.Terminated;
            opu = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .build();
        }
        OduBuilder oduIfBuilder = new OduBuilder()
            .setRate(ODU0.VALUE)
            .setOduFunction(oduFunction)
            .setMonitoringMode(monitoringMode)
            .setOpu(opu)
            .setParentOduAllocation(parentOduAllocation);
        if (apiInfoA != null) {
            oduIfBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            oduIfBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        if (!isCTP) {
            LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
            this.portMapping.updateMapping(nodeId, mapping);
        }
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu2Interface(String nodeId, String logicalConnPoint,
            String servicename, boolean isCTP, int tribPortNumber, int tribSlotIndex, AEndApiInfo apiInfoA,
            ZEndApiInfo apiInfoZ, String payloadType) throws OpenRoadmInterfaceException {

        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(mapping, OtnOdu.VALUE,
            logicalConnPoint + "-ODU2" + ":" + servicename);
        if (mapping.getSupportingOdu4() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingOdu4());
        }
        if (mapping.getSupportingEthernet() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingEthernet());
        }

        OduFunctionIdentity oduFunction;
        MonitoringMode monitoringMode;
        Opu opu = null;
        ParentOduAllocation parentOduAllocation = null;
        if (isCTP) {
            oduFunction = ODUCTP.VALUE;
            monitoringMode = MonitoringMode.Monitored;
            Set<Uint16> tribSlots = new HashSet<>();
            Uint16 newIdx = Uint16.valueOf(tribSlotIndex);
            tribSlots.add(newIdx);
            IntStream.range(tribSlotIndex, tribSlotIndex + 8)
                    .forEach(nbr -> tribSlots.add(Uint16.valueOf(nbr)));
            parentOduAllocation = new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(tribPortNumber))
                    .setTribSlots(tribSlots)
                    .build();
        } else {
            oduFunction = ODUTTPCTP.VALUE;
            monitoringMode = MonitoringMode.Terminated;
            opu = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                .build();
        }
        OduBuilder oduIfBuilder = new OduBuilder()
            .setRate(ODU2.VALUE)
            .setOduFunction(oduFunction)
            .setMonitoringMode(monitoringMode)
            .setOpu(opu)
            .setParentOduAllocation(parentOduAllocation);
        if (apiInfoA != null) {
            oduIfBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            oduIfBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder
            oduIf1Builder = new
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        this.openRoadmInterfaces.postOTNInterface(nodeId, oduInterfaceBldr);
        if (!isCTP) {
            LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
            this.portMapping.updateMapping(nodeId, mapping);
        }
        return oduInterfaceBldr.getName();
    }
}
