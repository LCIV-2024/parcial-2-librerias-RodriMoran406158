package com.example.libreria.service;

import com.example.libreria.dto.*;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    
    @Mock
    private ReservationRepository reservationRepository;
    
    @Mock
    private BookRepository bookRepository;
    
    @Mock
    private BookService bookService;
    
    @Mock
    private UserService userService;

    @Mock
    private ModelMapper modelMapper;

    @Spy
    @InjectMocks
    private ReservationService reservationService;
    
    private User testUser;
    private Book testBook;
    private Reservation testReservation;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        
        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);
        
        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }
    
    @Test
    void testCreateReservation_Success() {
        // TODO: Implementar el test de creación de reserva exitosa
        //given
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(258027L);
        requestDTO.setRentalDays(10);
        requestDTO.setStartDate(LocalDate.of(2025, 11, 14));

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(testUser.getId());
        userResponse.setName(testUser.getName());

        BookResponseDTO bookResponse = new BookResponseDTO();
        bookResponse.setExternalId(testBook.getExternalId());
        bookResponse.setPrice(testBook.getPrice());
        bookResponse.setAvailableQuantity(testBook.getAvailableQuantity());

        //when
        when(userService.getUserById(1L)).thenReturn(userResponse);
        when(bookService.getBookByExternalId(258027L)).thenReturn(bookResponse);
        when(modelMapper.map(userResponse, User.class)).thenReturn(testUser);
        when(modelMapper.map(bookResponse, Book.class)).thenReturn(testBook);

        //then
        Reservation saved = new Reservation();
        saved.setId(999L);
        saved.setUser(testUser);
        saved.setBook(testBook);
        saved.setRentalDays(10);
        saved.setStartDate(requestDTO.getStartDate());
        saved.setExpectedReturnDate(requestDTO.getStartDate().plusDays(10));
        saved.setDailyRate(testBook.getPrice());
        saved.setTotalFee(testBook.getPrice().multiply(BigDecimal.valueOf(10)));
        saved.setStatus(Reservation.ReservationStatus.ACTIVE);

        when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

        ReservationResponseDTO expectedResponse = new ReservationResponseDTO();
        expectedResponse.setId(999L);
        expectedResponse.setUserId(testUser.getId());
        expectedResponse.setBookExternalId(testBook.getExternalId());
        expectedResponse.setStartDate(requestDTO.getStartDate());
        expectedResponse.setExpectedReturnDate(requestDTO.getStartDate().plusDays(10));

        doReturn(expectedResponse).when(reservationService).convertToDTO(saved);

        ReservationResponseDTO result = reservationService.createReservation(requestDTO);

        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testBook.getExternalId(), result.getBookExternalId());
        assertEquals(requestDTO.getStartDate(), result.getStartDate());
        assertEquals(requestDTO.getStartDate().plusDays(10), result.getExpectedReturnDate());

        verify(userService).getUserById(1L);
        verify(bookService).getBookByExternalId(258027L);
        verify(reservationRepository).save(any(Reservation.class));
        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testCreateReservation_BookNotAvailable() {
        // TODO: Implementar el test de creación de reserva cuando el libro no está disponible
        //given
        ReservationRequestDTO requestDTO = new ReservationRequestDTO();
        requestDTO.setUserId(1L);
        requestDTO.setBookExternalId(258027L);

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setId(testUser.getId());
        userResponse.setName(testUser.getName());

        BookResponseDTO bookResponse = new BookResponseDTO();
        bookResponse.setExternalId(testBook.getExternalId());
        bookResponse.setPrice(testBook.getPrice());
        bookResponse.setAvailableQuantity(0);

        //when
        when(userService.getUserById(1L)).thenReturn(userResponse);
        when(bookService.getBookByExternalId(258027L)).thenReturn(bookResponse);

        //then


        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> {
                    ReservationResponseDTO result = reservationService.createReservation(requestDTO);
                });

        assertEquals("El libro no está disponibke ",exception.getMessage());

//        assertNotNull(result);
//        assertEquals(999L, result.getId());
//        assertEquals(testUser.getId(), result.getUserId());
//        assertEquals(testBook.getExternalId(), result.getBookExternalId());
//        assertEquals(requestDTO.getStartDate(), result.getStartDate());
//        assertEquals(requestDTO.getStartDate().plusDays(10), result.getExpectedReturnDate());

//        verify(userService).getUserById(1L);
//        verify(bookService).getBookByExternalId(258027L);
//        verify(reservationRepository).save(any(Reservation.class));
//        verify(bookRepository).save(any(Book.class));
    }
    
    @Test
    void testReturnBook_OnTime() {
        // TODO: Implementar el test de devolución de libro en tiempo
        //given
        Long reservationId = 1L;
        testReservation.setExpectedReturnDate(LocalDate.of(2025, 11, 20));
        testReservation.setStartDate(LocalDate.of(2025, 11, 13));
        testReservation.setRentalDays(7);
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setLateFee(BigDecimal.ZERO);

        testBook.setAvailableQuantity(5);

        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(LocalDate.of(2025, 11, 20));

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationResponseDTO expectedDTO = new ReservationResponseDTO();
        expectedDTO.setId(testReservation.getId());
        expectedDTO.setUserId(testReservation.getUser().getId());
        expectedDTO.setBookExternalId(testReservation.getBook().getExternalId());
        expectedDTO.setLateFee(BigDecimal.ZERO);
        expectedDTO.setStatus(Reservation.ReservationStatus.RETURNED);

        doReturn(expectedDTO).when(reservationService).convertToDTO(testReservation);

        //when
        ReservationResponseDTO result = reservationService.returnBook(reservationId, returnRequest);


        //then
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testBook.getExternalId(), result.getBookExternalId());

        assertEquals(BigDecimal.ZERO, result.getLateFee());

        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(6, testBook.getAvailableQuantity());
        verify(reservationRepository).findById(reservationId);
        verify(reservationRepository).save(testReservation);
        verify(bookRepository).save(testBook);

    }
    
    @Test
    void testReturnBook_Overdue() {
        // TODO: Implementar el test de devolución de libro con retraso
        //given
        Long reservationId = 1L;
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setExpectedReturnDate(LocalDate.of(2025, 11, 20));
        testReservation.setStartDate(LocalDate.of(2025, 11, 10));
        testReservation.setRentalDays(10);

        testBook.setAvailableQuantity(5);
        ReturnBookRequestDTO returnRequest = new ReturnBookRequestDTO();
        returnRequest.setReturnDate(LocalDate.of(2025, 11, 23));
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        long daysLate = 3;

        BigDecimal expectedLateFee =
                testBook.getPrice()
                        .multiply(new BigDecimal("0.15"))
                        .multiply(BigDecimal.valueOf(daysLate));
        ReservationResponseDTO expectedDTO = new ReservationResponseDTO();
        expectedDTO.setId(testReservation.getId());
        expectedDTO.setUserId(testUser.getId());
        expectedDTO.setBookExternalId(testBook.getExternalId());
        expectedDTO.setLateFee(expectedLateFee);
        expectedDTO.setStatus(Reservation.ReservationStatus.RETURNED);

        doReturn(expectedDTO).when(reservationService).convertToDTO(testReservation);

        ReservationResponseDTO result = reservationService.returnBook(reservationId, returnRequest);
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testBook.getExternalId(), result.getBookExternalId());
        assertEquals(expectedLateFee, result.getLateFee());
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(6, testBook.getAvailableQuantity());

        verify(reservationRepository).findById(reservationId);
        verify(reservationRepository).save(testReservation);
        verify(bookRepository).save(testBook);
    }
    
    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
        
        ReservationResponseDTO result = reservationService.getReservationById(1L);
        
        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }
    
    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        
        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));
        
        List<ReservationResponseDTO> result = reservationService.getAllReservations();
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
    
    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));
        
        List<ReservationResponseDTO> result = reservationService.getActiveReservations();
        
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}

