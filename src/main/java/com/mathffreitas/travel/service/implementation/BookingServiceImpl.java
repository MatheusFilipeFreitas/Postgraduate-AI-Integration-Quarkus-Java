package com.mathffreitas.travel.service.implementation;

import com.mathffreitas.travel.models.Booking;
import com.mathffreitas.travel.repository.BookingRepository;
import com.mathffreitas.travel.repository.implementation.BookingRepositoryImpl;
import com.mathffreitas.travel.service.BookingService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class BookingServiceImpl implements BookingService {

    private BookingRepository bookingRepository;

    public BookingServiceImpl() {
        initService();
    }

    @Override
    public Optional<Booking> getBookingDetails(long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public Optional<Booking> cancelBooking(long id, String customerLastName) {
        return bookingRepository.cancelBooking(id, customerLastName);
    }

    private void initService() {
        bookingRepository = new BookingRepositoryImpl(true);
    }
}
