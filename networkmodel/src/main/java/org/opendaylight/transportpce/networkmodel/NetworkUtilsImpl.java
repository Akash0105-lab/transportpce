/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.DeleteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.NetworkutilsService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtilsImpl implements NetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);
    private final DataBroker dataBroker;
    private final OpenRoadmTopology openRoadmTopology;

    public NetworkUtilsImpl(DataBroker dataBroker, OpenRoadmTopology openRoadmTopology) {
        this.dataBroker = dataBroker;
        this.openRoadmTopology = openRoadmTopology;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteLinkOutput>> deleteLink(DeleteLinkInput input) {

        LinkId linkId = new LinkId(input.getLinkId());
        // Building link instance identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Network.class,
            new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));

        //Check if link exists
        try {
            ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            Optional<Link> linkOptional = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, linkIID.build())
                .get();
            if (!linkOptional.isPresent()) {
                LOG.info("Link not present");
                return RpcResultBuilder
                    .success(new DeleteLinkOutputBuilder().setResult(
                        "Fail"))
                    .buildFuture();
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("readMdSal: Error reading link {}", input.getLinkId());
            return RpcResultBuilder
                .success(new DeleteLinkOutputBuilder().setResult(
                    "Fail"))
                .buildFuture();
        }

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, linkIID.build());
        try {
            writeTransaction.submit().get();
            LOG.info("Link with linkId: {} deleted from {} layer.",
                input.getLinkId(), NetworkUtils.OVERLAY_NETWORK_ID);
            return RpcResultBuilder
                .success(new DeleteLinkOutputBuilder().setResult(
                    "Link {} deleted successfully"))
                .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            return RpcResultBuilder.<DeleteLinkOutput>failed().buildFuture();
        }
    }

    public ListenableFuture<RpcResult<InitRoadmNodesOutput>> initRoadmNodes(InitRoadmNodesInput input) {
        boolean createRdmLinks = OrdLink.createRdm2RdmLinks(input,
                this.openRoadmTopology,this.dataBroker);
        if (createRdmLinks) {
            return RpcResultBuilder
                .success(new InitRoadmNodesOutputBuilder().setResult(
                    "Unidirectional Roadm-to-Roadm Link created successfully"))
                .buildFuture();
        } else {
            return RpcResultBuilder.<InitRoadmNodesOutput>failed().buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmLinksOutput>> initXpdrRdmLinks(InitXpdrRdmLinksInput input) {
        // Assigns user provided input in init-network-view RPC to nodeId
        boolean createXpdrRdmLinks = Rdm2XpdrLink.createXpdrRdmLinks(input.getLinksInput(),
                this.openRoadmTopology,this.dataBroker);
        if (createXpdrRdmLinks) {
            return RpcResultBuilder
                .success(new InitXpdrRdmLinksOutputBuilder().setResult("Xponder Roadm Link created successfully"))
                .buildFuture();
        } else {
            return RpcResultBuilder.<InitXpdrRdmLinksOutput>failed().buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<InitRdmXpdrLinksOutput>> initRdmXpdrLinks(InitRdmXpdrLinksInput input) {
        boolean createRdmXpdrLinks = Rdm2XpdrLink.createRdmXpdrLinks(input.getLinksInput(),
                this.openRoadmTopology,this.dataBroker);
        if (createRdmXpdrLinks) {
            return RpcResultBuilder
                .success(new InitRdmXpdrLinksOutputBuilder().setResult("Roadm Xponder links created successfully"))
                .buildFuture();
        } else {
            return RpcResultBuilder.<InitRdmXpdrLinksOutput>failed().buildFuture();
        }
    }
}