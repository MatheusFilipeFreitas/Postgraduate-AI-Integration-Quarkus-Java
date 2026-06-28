package com.mathffreitas.travel.models;

import com.mathffreitas.travel.models.types.BookingStatus;

import java.time.LocalDate;

public record Booking(
        Long id,
        String customerName,
        String destination,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status
) {
}
