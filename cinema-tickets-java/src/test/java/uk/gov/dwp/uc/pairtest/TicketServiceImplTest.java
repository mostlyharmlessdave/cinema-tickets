package uk.gov.dwp.uc.pairtest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {
    @Mock
    TicketPaymentService ticketPaymentService;

    @Mock
    SeatReservationService seatReservationService;

    @Mock
    TicketPricingService ticketPricingService;

    @InjectMocks
    TicketServiceImpl ticketService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testPurchaseTicketsPassesTheCorrectPaymentInformation() {

        // The TicketPricingService handles the calculation of the ticket price (see implementation and test),
        // this test is only assures that the correct price is passed to the TicketPaymentService.
        when(ticketPricingService.priceTickets(any())).thenReturn(999);

        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> amountCaptor = ArgumentCaptor.forClass(Integer.class);

        ticketService.purchaseTickets(100L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2));
        verify(ticketPaymentService, times(1)).makePayment(idCaptor.capture(), amountCaptor.capture());
        assertEquals("The id of 100 is passed to the TicketPaymentService", 100L, idCaptor.getValue().longValue());
        assertEquals("The price of 999 is passed to the TicketPaymentService", 999, amountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTicketsMakesTheCorrectSeatReservation() {

        ArgumentCaptor<Integer> seatsRequiredCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> idCaptor = ArgumentCaptor.forClass(Long.class);

        ticketService.purchaseTickets(100L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2));
        verify(seatReservationService, times(1)).reserveSeat(idCaptor.capture(), seatsRequiredCaptor.capture());

        assertEquals("The id of 100 is passed to the SeatReservationService", 100L, idCaptor.getValue().longValue());
        assertEquals("2 seats are passed to the SeatReservationService", 2, seatsRequiredCaptor.getValue().intValue());

        reset(seatReservationService);
        ticketService.purchaseTickets(100L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2));
        verify(seatReservationService, times(1)).reserveSeat(idCaptor.capture(), seatsRequiredCaptor.capture());

        assertEquals("The id of 100 is passed to the SeatReservationService", 100L, idCaptor.getValue().longValue());
        assertEquals("4 seats are passed to the SeatReservationService", 4, seatsRequiredCaptor.getValue().intValue());

        reset(seatReservationService);
        ticketService.purchaseTickets(100L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 4),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2));
        verify(seatReservationService, times(1)).reserveSeat(idCaptor.capture(), seatsRequiredCaptor.capture());

        assertEquals("The id of 100 is passed to the SeatReservationService", 100L, idCaptor.getValue().longValue());
        assertEquals("6 seats are passed to the SeatReservationService", 6, seatsRequiredCaptor.getValue().intValue());

        reset(seatReservationService);
        ticketService.purchaseTickets(100L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 4),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2));
        verify(seatReservationService, times(1)).reserveSeat(idCaptor.capture(), seatsRequiredCaptor.capture());

        assertEquals("The id of 100 is passed to the SeatReservationService", 100L, idCaptor.getValue().longValue());
        assertEquals("9 seats are passed to the SeatReservationService", 9, seatsRequiredCaptor.getValue().intValue());
    }

    @Test
    public void testOnlyCorrectRatiosOfDependantsAreProcessed() {

        ticketService.purchaseTickets(3L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1));
        assertEquals("No exception is thrown as this is valid.",
                ExpectedException.none().getClass(), thrown.getClass());

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Children and Infants require an Adult for booking.");
        ticketService.purchaseTickets(3L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("More Infants than Adults, not enough laps.");
        ticketService.purchaseTickets(3L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2));
    }


    @Test
    public void testAccountIdsThatAreLessThanZeroAreNotProcessed() {
        ticketService.purchaseTickets(3L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4));
        assertEquals("Positive id is valid.", ExpectedException.none().getClass(), thrown.getClass());

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Invalid Id, Id is not greater than 0.");
        ticketService.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Invalid Id, Id is not greater than 0.");
        ticketService.purchaseTickets(-30L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4));
    }

    @Test
    public void testThatNegativeTicketCountsAreNotProcessed() {

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Negative number of adult tickets.");
        ticketService.purchaseTickets(3L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -4));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Negative number of child tickets.");
        ticketService.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.CHILD, -5));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Negative number of infant tickets.");
        ticketService.purchaseTickets(0L, new TicketTypeRequest(TicketTypeRequest.Type.INFANT, -3));
    }

    @Test
    public void testThatTwoTicketsOfTheSameTypeAreProcessedCorrectly() {

        ticketService.purchaseTickets(3L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 6),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10));
        assertEquals("No exception is thrown as 16 tickets is valid.",
                ExpectedException.none().getClass(), thrown.getClass());

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(3L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 16),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10));
    }

    @Test
    public void testAttemptToPurchaseMoreThan20Tickets() {

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 21));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 32));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(1L, new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 26));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 16),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15));

        thrown.expect(InvalidPurchaseException.class);
        thrown.expectMessage("Attempted to purchase more than 20 tickets.");
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 8),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6));
    }
}