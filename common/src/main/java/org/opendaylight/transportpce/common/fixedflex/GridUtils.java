/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Util class for grid.
 * Thoses methods are used for pce spectrum assignment and topology update.
 * They use maximal precision of BigDecimal
 * For device configuration which needs precision (4 digits), dedicated methods are
 * located in FixedFlex and FrexGrid classes.
 *
 */
public final class GridUtils {
    private static final Logger LOG = LoggerFactory.getLogger(GridUtils.class);

    private GridUtils() {
    }

    public static Map<AvailFreqMapsKey, AvailFreqMaps> initFreqMaps4FixedGrid2Available() {
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        Map<AvailFreqMapsKey, AvailFreqMaps> waveMap = new HashMap<>();
        AvailFreqMaps availFreqMaps = new AvailFreqMapsBuilder().setMapName(GridConstant.C_BAND)
                .setFreqMapGranularity(
                    new FrequencyGHz(Decimal64.valueOf(BigDecimal.valueOf(GridConstant.GRANULARITY))))
                .setStartEdgeFreq(
                    new FrequencyTHz(Decimal64.valueOf(BigDecimal.valueOf(GridConstant.START_EDGE_FREQUENCY))))
                .setEffectiveBits(Uint16.valueOf(GridConstant.EFFECTIVE_BITS))
                .setFreqMap(byteArray)
                .build();
        waveMap.put(availFreqMaps.key(), availFreqMaps);
        return waveMap;
    }

    /**
     * Compute the wavelength index from Spectrum assignment begin index.
     * Only for fix grid and device 1.2.1.
     * @param index int
     * @return the wavelength number.
     */
    public static long getWaveLengthIndexFromSpectrumAssigment(int index) {
        return (GridConstant.EFFECTIVE_BITS - index) / GridConstant.NB_SLOTS_100G;
    }

    /**
     * Compute the start frequency in THz for the given index.
     * @param index int
     * @return the start frequency in THz for the provided index.
     */
    public static BigDecimal getStartFrequencyFromIndex(int index) {
        int nvalue = index - 284;

        return BigDecimal.valueOf(GridConstant.CENTRAL_FREQUENCY).add(
                        BigDecimal.valueOf(GridConstant.GRANULARITY)
                                .multiply(BigDecimal.valueOf(nvalue))
                                .divide(BigDecimal.valueOf(1000))
                );
    }

    /**
     * Compute the stop frequency in THz for the given index.
     * @param index int
     * @return the stop frequency in THz for the provided index.
     */
    public static BigDecimal getStopFrequencyFromIndex(int index) {
        return getStartFrequencyFromIndex(index)
                .add(BigDecimal.valueOf(GridConstant.GRANULARITY).divide(BigDecimal.valueOf(1000)));
    }

    /**
     * Get the bit index for the frequency.
     *
     * @param atozMinFrequency BigDecimal
     * @return the bit index of the frequency. Throw IllegalArgumentException if
     *         index not in range of 0 GridConstant.EFFECTIVE_BITS
     */
    public static int getIndexFromFrequency(Decimal64 atozMinFrequency) {

        BigDecimal nvalue = (BigDecimal.valueOf(atozMinFrequency.doubleValue())
                                .subtract(BigDecimal.valueOf(GridConstant.CENTRAL_FREQUENCY))
                            ).multiply(
                                    BigDecimal.valueOf(1000)
                                    .divide(BigDecimal.valueOf(GridConstant.GRANULARITY))
                            );

        int index = (int) Math.round(nvalue.add(BigDecimal.valueOf(284)).doubleValue());
        if (index < 0 || index > GridConstant.EFFECTIVE_BITS) {
            throw new IllegalArgumentException("Frequency not in range " + atozMinFrequency);
        }
        return index;
    }

    /**
     * Get the spectrum width for rate and modulation format.
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @return spectrum width in GHz compatible with models 10.1
     */
    public static FrequencyGHz getWidthFromRateAndModulationFormat(Uint32 rate,
            ModulationFormat modulationFormat) {
        String width = GridConstant.FREQUENCY_WIDTH_TABLE.get(rate, modulationFormat);
        if (width == null) {
            LOG.warn("No width found for service rate {} and modulation format {}, set width to 40", rate,
                    modulationFormat);
            width = String.valueOf(GridConstant.WIDTH_40);
        }
        return FrequencyGHz.getDefaultInstance(width);
    }

    /**
     * Get central frequency of spectrum.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @return central frequency in THz compatible with models 10.1
     */
    public static FrequencyTHz getCentralFrequency(BigDecimal minFrequency, BigDecimal maxFrequency) {
        return new FrequencyTHz(Decimal64.valueOf(computeCentralFrequency(minFrequency, maxFrequency)));
    }

    /**
     * Get central frequency of spectrum with precision.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param precision int
     * @return central frequency in THz with precision
     */
    public static org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz
        getCentralFrequencyWithPrecision(BigDecimal minFrequency,
            BigDecimal maxFrequency, int precision) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz(
                Decimal64.valueOf(computeCentralFrequency(minFrequency, maxFrequency)
                    .setScale(precision, RoundingMode.HALF_EVEN)));
    }

    /**
     * Compute central frequency from min and max frequency.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @return central frequency
     */
    private static BigDecimal computeCentralFrequency(BigDecimal minFrequency, BigDecimal maxFrequency) {
        return minFrequency.add(maxFrequency).divide(BigDecimal.valueOf(2));
    }

    /**
     * Get central frequency of spectrum with precision for open config models.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param precision int
     * @return central frequency in THz with precision
     */
    public static org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz
        getCentralFrequencyWithPrecisionForOpenConfig(BigDecimal minFrequency, BigDecimal maxFrequency,
                                                      int precision) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz(
                Decimal64.valueOf(computeCentralFrequencyForOpenConfig(minFrequency, maxFrequency)
                        .setScale(precision, RoundingMode.HALF_EVEN)));
    }

    /**
     * Compute central frequency from min and max frequency for open config models.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @return central frequency
     */
    public static BigDecimal computeCentralFrequencyForOpenConfig(BigDecimal minFrequency, BigDecimal maxFrequency) {
        return minFrequency.add(maxFrequency).divide(BigDecimal.valueOf(2)).movePointRight(6);
    }

    /**
     * Get the lower spectral index for the frequency.
     * @param frequency BigDecimal
     * @return the lower spectral index
     */
    public static int getLowerSpectralIndexFromFrequency(Decimal64 frequency) {
        return getIndexFromFrequency(frequency) + 1;
    }

    /**
     * Get the higher spectral index for the frequency.
     * @param frequency BigDecimal
     * @return the lower spectral index
     */
    public static int getHigherSpectralIndexFromFrequency(Decimal64 frequency) {
        return getIndexFromFrequency(frequency);
    }

    /**
     * Create spectrum information from service path input.
     * @param input ServicePathInput
     * @return SpectrumInformation
     */
    public static SpectrumInformation initSpectrumInformationFromServicePathInput(ServicePathInput input) {
        if (input.getLowerSpectralSlotNumber() == null || input.getHigherSpectralSlotNumber() == null) {
            LOG.error("low and higher spectral slot numbers cannot be null");
            throw new IllegalArgumentException("low and higher spectral slot numbers cannot be null");
        }
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setLowerSpectralSlotNumber(input.getLowerSpectralSlotNumber().intValue());
        spectrumInformation.setHigherSpectralSlotNumber(input.getHigherSpectralSlotNumber().intValue());
        if (input.getWaveNumber() != null) {
            spectrumInformation.setWaveLength(input.getWaveNumber());
        }
        if (input.getMinFreq() != null) {
            spectrumInformation.setMinFrequency(input.getMinFreq().getValue().decimalValue());
        } else {
            spectrumInformation.setMinFrequency(
                    GridUtils.getStartFrequencyFromIndex(input.getLowerSpectralSlotNumber().intValue() - 1));
        }
        if (input.getMaxFreq() != null) {
            spectrumInformation.setMaxFrequency(input.getMaxFreq().getValue().decimalValue());
        } else {
            spectrumInformation.setMaxFrequency(
                    GridUtils.getStopFrequencyFromIndex(input.getHigherSpectralSlotNumber().intValue() - 1));
        }
        if (input.getCenterFreq() != null) {
            spectrumInformation.setCenterFrequency(input.getCenterFreq().getValue().decimalValue());
        } else {
            spectrumInformation.setCenterFrequency(GridUtils.getCentralFrequency(
                    spectrumInformation.getMinFrequency(),
                    spectrumInformation.getMaxFrequency()).getValue().decimalValue());
        }
        if (input.getNmcWidth() != null) {
            spectrumInformation.setWidth(new BigDecimal(
                    input.getNmcWidth().getValue().decimalValue().stripTrailingZeros().toPlainString())
            );
        }
        if (input.getModulationFormat() != null) {
            spectrumInformation.setModulationFormat(input.getModulationFormat());
        }
        return spectrumInformation;
    }

    /**
     * Get the N value of range -284 +484 from frequency index array.
     * @param frequencyIndex the frequency index f range 0 768.
     * @return the N value
     */
    public static int getNFromFrequencyIndex(int frequencyIndex) {
        return frequencyIndex - 284;
    }

    /**
     * Convert the power from dBm to Watt.
     * @param  dbm power in dBm.
     * @return outputpower in Watt.
     */
    public static BigDecimal convertDbmW(double dbm) {
        return BigDecimal.valueOf(Math.pow(10, (dbm - 30) / 10));
    }

}
