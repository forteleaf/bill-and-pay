package com.korpay.billpay.service.settlement;

import com.korpay.billpay.domain.enums.SettlementCycle;
import com.korpay.billpay.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BusinessDayCalculator {

    private final HolidayRepository holidayRepository;

    public LocalDate calculateSettlementDate(LocalDate eventDate, SettlementCycle cycle) {
        if (cycle == SettlementCycle.REALTIME) {
            return eventDate;
        }
        return addBusinessDays(eventDate, cycle.getBusinessDays());
    }

    public LocalDate addBusinessDays(LocalDate from, int days) {
        LocalDate date = from;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (isBusinessDay(date)) {
                added++;
            }
        }
        return date;
    }

    public boolean isBusinessDay(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayRepository.existsByHolidayDateAndCountryCode(date, "KR");
    }
}
