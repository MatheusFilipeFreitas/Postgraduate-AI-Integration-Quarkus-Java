package com.mathffreitas.travel.repository.implementation;

import com.mathffreitas.travel.models.Booking;
import com.mathffreitas.travel.models.types.BookingStatus;
import com.mathffreitas.travel.repository.BookingRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class BookingRepositoryImpl implements BookingRepository {
    private final Map<Long, Booking> bookings = new HashMap<>();

    public BookingRepositoryImpl(boolean enableMock) {
        if (enableMock) {
            mockData();
        }
    }

    @Override
    public Optional<Booking> findById(long id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public Optional<Booking> cancelBooking(long id, String customerLastName) {
        Booking booking = findById(id).orElse(null);

        if(booking != null && booking.customerName().endsWith(customerLastName)) {
            Booking cancelledBooking = updateBookingStatus(booking, BookingStatus.CANCELLED);
            bookings.put(id, cancelledBooking);
            return Optional.of(cancelledBooking);
        }

        return Optional.empty();
    }

    private Booking updateBookingStatus(Booking booking, BookingStatus newStatus) {
        return new Booking(
                booking.id(),
                booking.customerName(),
                booking.destination(),
                booking.startDate(),
                booking.endDate(),
                newStatus
        );
    }

    private void mockData() {
        Booking mock1 = new Booking(
                12345L,
                "John Doe",
                "Egyptian Treasures",
                LocalDate.now().plusMonths(2),
                LocalDate.now().plusMonths(2),
                BookingStatus.CONFIRMED
        );
        bookings.put(mock1.id(), mock1);

        Booking mock2 = new Booking(
                67890L,
                "Jane Smith",
                "Amazon Adventure",
                LocalDate.now().plusMonths(3),
                LocalDate.now().plusMonths(4),
                BookingStatus.PENDING
        );
        bookings.put(mock2.id(), mock2);
    }

}
