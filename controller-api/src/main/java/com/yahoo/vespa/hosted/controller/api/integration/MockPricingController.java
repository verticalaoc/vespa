package com.yahoo.vespa.hosted.controller.api.integration;

import com.yahoo.config.provision.ClusterResources;
import com.yahoo.vespa.hosted.controller.api.integration.billing.Plan;
import com.yahoo.vespa.hosted.controller.api.integration.pricing.PriceInformation;
import com.yahoo.vespa.hosted.controller.api.integration.pricing.PricingController;
import com.yahoo.vespa.hosted.controller.api.integration.pricing.PricingInfo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class MockPricingController implements PricingController {

    @Override
    public PriceInformation price(List<ClusterResources> clusterResources, PricingInfo pricingInfo, Plan plan) {
        BigDecimal listPrice = BigDecimal.valueOf(clusterResources.stream()
                                                          .mapToDouble(resources -> resources.nodes() *
                                                                  (resources.nodeResources().vcpu() * 1000 +
                                                                          resources.nodeResources().memoryGb() * 100 +
                                                                          resources.nodeResources().diskGb() * 10))
                                                          .sum())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal volumeDiscount = new BigDecimal("5.00");
        BigDecimal committedAmountDiscount = new BigDecimal("0.00");
        BigDecimal enclaveDiscount = new BigDecimal("0.00");
        BigDecimal totalAmount = listPrice.subtract(volumeDiscount);
        return new PriceInformation(listPrice, volumeDiscount, committedAmountDiscount, enclaveDiscount, totalAmount);
    }

}
