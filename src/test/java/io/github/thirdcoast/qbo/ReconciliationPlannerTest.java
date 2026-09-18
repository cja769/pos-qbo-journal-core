package io.github.thirdcoast.qbo;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationPlannerTest {
    private final ReconciliationPlanner planner = new ReconciliationPlanner();
    @Test void normalDayChecksYesterday() {
        assertEquals(java.util.Set.of(LocalDate.of(2026,9,17)), planner.datesToVerify(LocalDate.of(2026,9,18)));
    }
    @Test void sundayChecksPreviousSevenCompletedDays() {
        var dates = planner.datesToVerify(LocalDate.of(2026,9,20));
        assertEquals(7, dates.size());
        assertEquals(LocalDate.of(2026,9,13), dates.first());
        assertEquals(LocalDate.of(2026,9,19), dates.last());
    }
    @Test void firstOfMonthChecksPreviousCalendarMonth() {
        var dates = planner.datesToVerify(LocalDate.of(2026,11,1));
        assertEquals(31, dates.size());
        assertEquals(LocalDate.of(2026,10,1), dates.first());
        assertEquals(LocalDate.of(2026,10,31), dates.last());
    }
    @Test void firstOfYearDeduplicatesAllOverlappingWindows() {
        var dates = planner.datesToVerify(LocalDate.of(2027,1,1));
        assertEquals(365, dates.size());
        assertEquals(LocalDate.of(2026,1,1), dates.first());
        assertEquals(LocalDate.of(2026,12,31), dates.last());
    }
}
package io.github.thirdcoast.qbo;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationPlannerTest {
    private final ReconciliationPlanner planner = new ReconciliationPlanner();
    @Test void normalDayChecksYesterday() {
        assertEquals(java.util.Set.of(LocalDate.of(2026,9,17)), planner.datesToVerify(LocalDate.of(2026,9,18)));
    }
    @Test void sundayChecksPreviousSevenCompletedDays() {
        var dates = planner.datesToVerify(LocalDate.of(2026,9,20));
        assertEquals(7, dates.size());
        assertEquals(LocalDate.of(2026,9,13), dates.first());
        assertEquals(LocalDate.of(2026,9,19), dates.last());
    }
    @Test void firstOfMonthChecksPreviousCalendarMonth() {
        var dates = planner.datesToVerify(LocalDate.of(2026,10,1));
        assertEquals(30, dates.size());
        assertEquals(LocalDate.of(2026,9,1), dates.first());
        assertEquals(LocalDate.of(2026,9,30), dates.last());
    }
    @Test void firstOfYearDeduplicatesAllOverlappingWindows() {
        var dates = planner.datesToVerify(LocalDate.of(2027,1,1));
        assertEquals(365, dates.size());
        assertEquals(LocalDate.of(2026,1,1), dates.first());
        assertEquals(LocalDate.of(2026,12,31), dates.last());
    }
}
