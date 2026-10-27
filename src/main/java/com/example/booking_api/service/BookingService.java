package com.example.booking_api.service;

import com.example.booking_api.model.Booking;
import com.example.booking_api.model.BookingStatus;
import com.example.booking_api.repository.BookingRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    // READ - Get all bookings
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // READ - Get booking by ID
    public Booking getBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Booking not found"
                ));
    }

    // CREATE - Create a booking
    public Booking createBooking(Booking booking) {
        booking.setId(null);

        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }

        return bookingRepository.save(booking);
    }

    // UPDATE - Update a booking
    public Booking updateBooking(UUID id, Booking updatedBooking) {
        Booking booking = getBookingById(id);

        booking.setName(updatedBooking.getName());
        booking.setEmail(updatedBooking.getEmail());
        booking.setDateTime(updatedBooking.getDateTime());
        booking.setNumberOfPeople(updatedBooking.getNumberOfPeople());

        booking.setStatus(updatedBooking.getStatus() == null
                ? BookingStatus.PENDING
                : updatedBooking.getStatus());

        return bookingRepository.save(booking);
    }

    // DELETE - Delete a booking
    public void deleteBooking(UUID id) {
        Booking booking = getBookingById(id);
        bookingRepository.delete(booking);
    }
}
