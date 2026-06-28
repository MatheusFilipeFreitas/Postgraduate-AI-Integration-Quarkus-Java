package com.mathffreitas.travel.ai.tools;

import com.mathffreitas.travel.models.Booking;
import com.mathffreitas.travel.service.BookingService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// warn: some models does not support toolbox, such as gemma3:4b
@ApplicationScoped
public class BookingTools {

    @Inject
    BookingService bookingService;

    @Tool("""
            Looks up a travel booking by its numeric identifier.
            Use when the customer asks about reservation details, trip status, destination, or travel dates.
            Returns booking id, customer name, destination, start date, end date, and status (CONFIRMED, PENDING, or CANCELLED).
            If no booking exists for the given id, returns a not-found message.
            """)
    public String getBookingDetails(
            @P("Numeric booking identifier provided by the customer, e.g. 12345")
            long bookingId
    ) {
        return bookingService.getBookingDetails(bookingId)
                .map(Booking::toString)
                .orElse("Cound not find booking with id " + bookingId);
    }

    @Tool("""
            Cancels an existing travel booking after identity verification.
            Use only when the customer explicitly requests cancellation and provides both their booking id and last name.
            The last name must match the end of the name on the booking (e.g. 'Doe' for 'John Doe').
            Returns the updated booking with CANCELLED status on success.
            Returns a not-found message if the booking does not exist or the last name does not match.
            """)
    public String cancelBooking(
            @P("Numeric booking identifier to cancel")
            long bookingId,
            @P("Customer last name as registered on the booking, used to verify identity before cancellation")
            String customerLastName
    ) {
        return bookingService.cancelBooking(bookingId, customerLastName)
                .map(Booking::toString)
                .orElse("Cound not find booking with id " + bookingId);
    }
}
