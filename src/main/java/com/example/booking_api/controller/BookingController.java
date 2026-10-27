package com.example.booking_api.controller;

import com.example.booking_api.model.Booking;
import com.example.booking_api.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // GET /bookings
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    // GET /bookings/{id}
    @GetMapping("/{id}")
    public Booking getBookingById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    // POST /bookings
    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody Booking booking) {

        Booking createdBooking = bookingService.createBooking(booking);

        URI location = URI.create("/bookings/" + createdBooking.getId());

        return ResponseEntity.created(location).body(createdBooking);
    }

    // PUT /bookings/{id}
    @PutMapping("/{id}")
    public Booking updateBooking(
            @PathVariable UUID id,
            @Valid @RequestBody Booking booking) {

        return bookingService.updateBooking(id, booking);
    }

    // DELETE /bookings/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
    }
}
