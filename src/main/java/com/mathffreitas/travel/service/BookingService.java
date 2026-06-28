package com.mathffreitas.travel.service;

import com.mathffreitas.travel.models.Booking;

import java.util.Optional;

public interface BookingService {
    Optional<Booking> getBookingDetails(long id);
    Optional<Booking> cancelBooking(long id, String customerLastName);
}
