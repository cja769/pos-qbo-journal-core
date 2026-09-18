package io.github.thirdcoast.qbo;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public final class ReconciliationPlanner {
    public SortedSet<LocalDate> datesToVerify(LocalDate runDate) {
        var dates = new TreeSet<LocalDate>();
        dates.add(runDate.minusDays(1));
        if (runDate.getDayOfWeek() == DayOfWeek.SUNDAY) addRange(dates, runDate.minusDays(7), runDate.minusDays(1));
        if (runDate.getDayOfMonth() == 1) {
            var previousMonthEnd = runDate.minusDays(1);
            addRange(dates, previousMonthEnd.with(TemporalAdjusters.firstDayOfMonth()), previousMonthEnd);
        }
        if (isQuarterStart(runDate)) {
            var previousQuarterEnd = runDate.minusDays(1);
            var quarterStartMonth = ((previousQuarterEnd.getMonthValue() - 1) / 3) * 3 + 1;
            addRange(dates, LocalDate.of(previousQuarterEnd.getYear(), quarterStartMonth, 1), previousQuarterEnd);
        }
        if (runDate.getMonth() == Month.JANUARY && runDate.getDayOfMonth() == 1) {
            addRange(dates, LocalDate.of(runDate.getYear() - 1, 1, 1), LocalDate.of(runDate.getYear() - 1, 12, 31));
        }
        return Collections.unmodifiableSortedSet(dates);
    }
    private boolean isQuarterStart(LocalDate d) { return d.getDayOfMonth() == 1 && Set.of(1, 4, 7, 10).contains(d.getMonthValue()); }
    private void addRange(Set<LocalDate> dates, LocalDate start, LocalDate end) {
        for (var date = start; !date.isAfter(end); date = date.plusDays(1)) dates.add(date);
    }
}
