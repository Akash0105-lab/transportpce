/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.rpc.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * This class is the implementation of the 'service-power-setup' RESTCONF service, which
 * is one of the external APIs into the olm application.
 *
 * <p>service-power-setup: This operation performs following steps:
 *    Step1: Calculate Spanloss on all links which are part of service.
 *    TODO Step2: Calculate power levels for each Tp-Id
 *    TODO Step3: Post power values on roadm connections
 *
 * <p>The signature for this method was generated by yang tools from the olm API model.
 */
public class ServicePowerSetupImpl implements ServicePowerSetup {
    private final OlmPowerService olmPowerService;

    public ServicePowerSetupImpl(final OlmPowerService olmPowerService) {
        this.olmPowerService = requireNonNull(olmPowerService);
    }

    @Override
    public ListenableFuture<RpcResult<ServicePowerSetupOutput>> invoke(ServicePowerSetupInput input) {
        return RpcResultBuilder.success(this.olmPowerService.servicePowerSetup(input)).buildFuture();
    }

}
