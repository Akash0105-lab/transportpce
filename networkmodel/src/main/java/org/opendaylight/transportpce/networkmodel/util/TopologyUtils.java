/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.networkmodel.dto.TopologyShard;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to ease update of items in topology data-stores.
 */
public final class TopologyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyUtils.class);

    private TopologyUtils() {
    }

    /**
     * Create a {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
     *      .networks.network.LinkBuilder} object for a given source and destination.
     *
     * @param srcNode source node id
     * @param dstNode destination node id
     * @param srcTp source termination point
     * @param destTp destination termination point
     * @param otnPrefix OTN link type prefix
     * @return {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks
     *          .network.LinkBuilder}
     */
    public static LinkBuilder createLink(String srcNode, String dstNode, String srcTp, String destTp,
        String otnPrefix) {

        // Create Destination for link
        DestinationBuilder dstNodeBldr = new DestinationBuilder()
            .setDestTp(new TpId(destTp))
            .setDestNode(new NodeId(dstNode));

        // Create Source for the link
        SourceBuilder srcNodeBldr = new SourceBuilder()
            .setSourceNode(new NodeId(srcNode))
            .setSourceTp(new TpId(srcTp));

        LinkId linkId;
        LinkId oppositeLinkId;
        if (otnPrefix == null) {
            linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
            oppositeLinkId = LinkIdUtil.buildLinkId(dstNode, destTp, srcNode, srcTp);
        } else {
            linkId = LinkIdUtil.buildOtnLinkId(srcNode, srcTp, dstNode, destTp, otnPrefix);
            oppositeLinkId = LinkIdUtil.buildOtnLinkId(dstNode, destTp, srcNode, srcTp, otnPrefix);
        }
        //set opposite link
        Link1 lnk1 = new Link1Builder().setOppositeLink(oppositeLinkId).build();

        // set link builder attribute
        LinkBuilder lnkBldr = new LinkBuilder()
            .setDestination(dstNodeBldr.build())
            .setSource(srcNodeBldr.build())
            .setLinkId(linkId)
            .withKey(new LinkKey(linkId))
            .addAugmentation(lnk1);
        return lnkBldr;
    }

    /**
     * Delete a link specified by a given source and destination.
     *
     * @param srcNode source node id string
     * @param dstNode destination node id
     * @param srcTp source termination point
     * @param destTp destination termination point
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @return True if OK, False otherwise.
     */
    public static boolean deleteLink(String srcNode, String dstNode, String srcTp, String destTp,
                                     NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for {}-{}", srcNode, dstNode);
        LinkId linkId = LinkIdUtil.buildLinkId(srcNode, srcTp, dstNode, destTp);
        if (deleteLinkLinkId(linkId, networkTransactionService)) {
            LOG.debug("Link Id {} updated to have admin state down", linkId);
            return true;
        } else {
            LOG.debug("Link Id not found for Source {} and Dest {}", srcNode, dstNode);
            return false;
        }
    }

    /**
     * Delete a link specified by its linkId.
     *
     * @param linkId The link identifier
     * @param networkTransactionService Service that eases the transaction operations with data-stores
     * @return True if OK, False otherwise.
     */
    public static boolean deleteLinkLinkId(LinkId linkId , NetworkTransactionService networkTransactionService) {
        LOG.info("deleting link for LinkId: {}", linkId.getValue());
        try {
            DataObjectIdentifier.Builder<Link> linkIID = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));
            java.util.Optional<Link> link =
                networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,linkIID.build()).get();
            if (link.isPresent()) {
                LinkBuilder linkBuilder = new LinkBuilder(link.orElseThrow());
                Link1Builder link1Builder = new Link1Builder(linkBuilder.augmentation(Link1.class));
                linkBuilder.removeAugmentation(Link1.class);
                linkBuilder.addAugmentation(link1Builder.build());
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, linkIID.build(),
                    linkBuilder.build());
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                return true;
            } else {
                LOG.error("No link found for given LinkId: {}",
                    linkId);
                return false;
            }

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error deleting link {}", linkId.getValue(), e);
            return false;
        }
    }

    /**
     * Set the {@link org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates}
     *      according to string representation.
     *
     * @param adminState value of the AdminStates
     * @return {@link org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates}
     */
    public static AdminStates setNetworkAdminState(String adminState) {
        if (adminState == null) {
            return null;
        }
        return switch (adminState) {
            case "InService", "ENABLED" -> AdminStates.InService;
            case "OutOfService", "DISABLED" -> AdminStates.OutOfService;
            case "Maintenance" -> AdminStates.Maintenance;
            default -> null;
        };
    }

    /**
     * Set the {@link org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State} according to
     *      string representation.
     *
     * @param operState Value of the operational state
     * @return {@link org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State}
     */
    public static State setNetworkOperState(String operState) {
        if (operState == null) {
            return null;
        }
        return switch (operState) {
            case "InService", "ACTIVE" -> State.InService;
            case "OutOfService", "INACTIVE" -> State.OutOfService;
            case "Degraded" -> State.Degraded;
            default -> null;
        };
    }

    /**
     * Update topology components.
     *
     * @param abstractNodeid Node name
     * @param mapping mapping
     * @param nodes Map of topology nodes
     * @param links Map of topology links
     * @return Subset of the topology
     */
    public static TopologyShard updateTopologyShard(String abstractNodeid, Mapping mapping, Map<NodeKey, Node> nodes,
            Map<LinkKey, Link> links) {
        // update termination-point corresponding to the mapping
        List<TerminationPoint> topoTps = new ArrayList<>();
        TerminationPoint tp = nodes.get(new NodeKey(new NodeId(abstractNodeid))).augmentation(Node1.class)
            .getTerminationPoint().get(new TerminationPointKey(new TpId(mapping.getLogicalConnectionPoint())));
        TerminationPoint1Builder tp1Bldr = new TerminationPoint1Builder(tp.augmentation(TerminationPoint1.class));
        if (!tp1Bldr.getAdministrativeState().getName().equals(mapping.getPortAdminState())) {
            tp1Bldr.setAdministrativeState(AdminStates.valueOf(mapping.getPortAdminState()));
        }
        if (!tp1Bldr.getOperationalState().getName().equals(mapping.getPortOperState())) {
            tp1Bldr.setOperationalState(State.valueOf(mapping.getPortOperState()));
        }
        TerminationPointBuilder tpBldr = new TerminationPointBuilder(tp).addAugmentation(tp1Bldr.build());
        topoTps.add(tpBldr.build());

        if (links == null) {
            return new TopologyShard(null, null, topoTps);
        }
        String tpId = tpBldr.getTpId().getValue();
        // update links terminating on the given termination-point
        List<Link> filteredTopoLinks = links.values().stream()
            .filter(l1 -> (l1.getSource().getSourceNode().getValue().equals(abstractNodeid)
                && l1.getSource().getSourceTp().getValue().equals(tpId))
                || (l1.getDestination().getDestNode().getValue().equals(abstractNodeid)
                && l1.getDestination().getDestTp().getValue().equals(tpId)))
            .collect(Collectors.toList());
        List<Link> topoLinks = new ArrayList<>();
        for (Link link : filteredTopoLinks) {
            TerminationPoint otherLinkTp;
            if (link.getSource().getSourceNode().getValue().equals(abstractNodeid)) {
                otherLinkTp = nodes
                    .get(new NodeKey(new NodeId(link.getDestination().getDestNode().getValue())))
                    .augmentation(Node1.class)
                    .getTerminationPoint()
                    .get(new TerminationPointKey(new TpId(link.getDestination().getDestTp().getValue())));
            } else {
                otherLinkTp = nodes
                    .get(new NodeKey(new NodeId(link.getSource().getSourceNode().getValue())))
                    .augmentation(Node1.class)
                    .getTerminationPoint()
                    .get(new TerminationPointKey(new TpId(link.getSource().getSourceTp().getValue())));
            }
            Link1Builder link1Bldr = new Link1Builder(link.augmentation(Link1.class));
            if (tpBldr.augmentation(TerminationPoint1.class).getAdministrativeState().equals(AdminStates.InService)
                && otherLinkTp.augmentation(TerminationPoint1.class)
                    .getAdministrativeState().equals(AdminStates.InService)) {
                link1Bldr.setAdministrativeState(AdminStates.InService);
            } else {
                link1Bldr.setAdministrativeState(AdminStates.OutOfService);
            }
            if (tpBldr.augmentation(TerminationPoint1.class).getOperationalState().equals(State.InService)
                && otherLinkTp.augmentation(TerminationPoint1.class)
                    .getOperationalState().equals(State.InService)) {
                link1Bldr.setOperationalState(State.InService);
            } else {
                link1Bldr.setOperationalState(State.OutOfService);
            }
            topoLinks.add(new LinkBuilder(link).addAugmentation(link1Bldr.build()).build());
        }
        return new TopologyShard(null, topoLinks, topoTps);
    }
}
