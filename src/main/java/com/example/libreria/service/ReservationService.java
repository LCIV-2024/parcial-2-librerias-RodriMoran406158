package com.example.libreria.service;

import com.example.libreria.dto.*;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private static final BigDecimal LATE_FEE_PERCENTAGE = new BigDecimal("0.15"); // 15% por día
    
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;
    private final ModelMapper modelMapper;
    
    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO requestDTO) {

        // TODO: Implementar la creación de una reserva
        // Validar que el usuario existe
        UserResponseDTO userDTO = userService.getUserById(requestDTO.getUserId());
        if (userDTO == null) {
            throw new IllegalArgumentException("El usuario con ID " + requestDTO.getUserId() + " no existe");
        }

        // Validar que el libro existe y está disponible
        BookResponseDTO bookDTO = bookService.getBookByExternalId(requestDTO.getBookExternalId());
        if (bookDTO == null) {
            throw new IllegalArgumentException("El libro con externalId " + requestDTO.getBookExternalId() + " no existe");
        }

        if (bookDTO.getAvailableQuantity() <= 0) {
            throw new IllegalStateException("El libro no está disponibke ");
        }
        User userEntity = modelMapper.map(userDTO, User.class);
        Book bookEntity = modelMapper.map(bookDTO, Book.class);
        // Crear la reserva
        Reservation reservation = new Reservation();
        reservation.setUser(userEntity);
        reservation.setBook(bookEntity);
        reservation.setRentalDays(requestDTO.getRentalDays());
        reservation.setStartDate(requestDTO.getStartDate());

        LocalDate expectedReturnDate = requestDTO.getStartDate().plusDays(requestDTO.getRentalDays());
        reservation.setExpectedReturnDate(expectedReturnDate);

        // Reducir la cantidad disponible
        reservation.setDailyRate(bookDTO.getPrice());
        reservation.setTotalFee(
                calculateTotalFee(bookDTO.getPrice(), requestDTO.getRentalDays())
        );
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);

        reservation = reservationRepository.save(reservation);

        bookDTO.setAvailableQuantity(bookDTO.getAvailableQuantity() - 1);
        bookRepository.save(modelMapper.map(bookDTO, Book.class));
        return convertToDTO(reservation);

    }
    
    @Transactional
    public ReservationResponseDTO returnBook(Long reservationId, ReturnBookRequestDTO returnRequest) {

        // TODO: Implementar la devolución de un libro
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new RuntimeException("La reserva ya fue devuelta");
        }
        
        LocalDate returnDate = returnRequest.getReturnDate();
        reservation.setActualReturnDate(returnDate);
        
        // Calcular tarifa por demora si hay retraso
        LocalDate expected = reservation.getExpectedReturnDate();
        long daysLate = 0;

        if (returnDate.isAfter(expected)) {
            daysLate = java.time.temporal.ChronoUnit.DAYS.between(expected, returnDate);
        }

        BigDecimal lateFee = BigDecimal.ZERO;

        if (daysLate > 0) {
            BigDecimal bookPrice = reservation.getBook().getPrice();
            lateFee = calculateLateFee(bookPrice, daysLate);
            reservation.setLateFee(lateFee);
        }
        reservation.setStatus(Reservation.ReservationStatus.RETURNED);

        // Aumentar la cantidad disponible
        Book book = reservation.getBook();
        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        bookRepository.save(book);

        reservation = reservationRepository.save(reservation);

        return convertToDTO(reservation);
    }
    
    @Transactional(readOnly = true)
    public ReservationResponseDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return convertToDTO(reservation);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getActiveReservations() {
        return reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getOverdueReservations() {
        return reservationRepository.findOverdueReservations().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private BigDecimal calculateTotalFee(BigDecimal dailyRate, Integer rentalDays) {
        // TODO: Implementar el cálculo del total de la reserva
        if (dailyRate == null || rentalDays == null || rentalDays <= 0) {
            throw new IllegalArgumentException("La tarifa diaria y los dias de alquiler deben ser vslidos");
        }

        return dailyRate
                .multiply(BigDecimal.valueOf(rentalDays));
    }
    
    private BigDecimal calculateLateFee(BigDecimal bookPrice, long daysLate) {
        // 15% del precio del libro por cada día de demora
        // TODO: Implementar el cálculo de la multa por demora
        if (daysLate <= 0) {
            return BigDecimal.ZERO;
        }

        return bookPrice
                .multiply(LATE_FEE_PERCENTAGE)
                .multiply(BigDecimal.valueOf(daysLate))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setUserId(reservation.getUser().getId());
        dto.setUserName(reservation.getUser().getName());
        dto.setBookExternalId(reservation.getBook().getExternalId());
        dto.setBookTitle(reservation.getBook().getTitle());
        dto.setRentalDays(reservation.getRentalDays());
        dto.setStartDate(reservation.getStartDate());
        dto.setExpectedReturnDate(reservation.getExpectedReturnDate());
        dto.setActualReturnDate(reservation.getActualReturnDate());
        dto.setDailyRate(reservation.getDailyRate());
        dto.setTotalFee(reservation.getTotalFee());
        dto.setLateFee(reservation.getLateFee());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        return dto;
    }
}

