package com.mathffreitas.travel.repository;

import com.mathffreitas.travel.models.Booking;

import java.util.Optional;

public interface BookingRepository {
    Optional<Booking> findById(long id);
    Optional<Booking> cancelBooking(long id, String customerLastName);
}
