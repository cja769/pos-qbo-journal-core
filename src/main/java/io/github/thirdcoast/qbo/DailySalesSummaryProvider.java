package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.DailySalesSummary;
import java.time.LocalDate;

@FunctionalInterface
public interface DailySalesSummaryProvider {
    DailySalesSummary load(LocalDate businessDate) throws Exception;
}
