package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private TicketPaymentService ticketPaymentService;

    private SeatReservationService seatReservationService;

    private TicketPricingService ticketPricingService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService,
                             TicketPricingService ticketPricingService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
        this.ticketPricingService = ticketPricingService;
    }

    /**
     * Processes the ticket purchase by validating the request against the
     * business rules, requesting a payment and submitting a seat reservation.
     *
     * @param accountId          The id of the account that will purchase the tickets, valid ids are greater than zero.
     * @param ticketTypeRequests The array of TicketTypeRequests to validate and purchase.
     * @throws InvalidPurchaseException if the Business Rules have not been met or the account id is not positive.
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId < 1L) {
            throw new InvalidPurchaseException("Invalid Id, Id is not greater than 0.");
        }

        int numberOfAdults = countTicketsForType(ADULT, ticketTypeRequests);
        int numberOfChildren = countTicketsForType(CHILD, ticketTypeRequests);
        int numberOfInfants = countTicketsForType(INFANT, ticketTypeRequests);

        validateTicketRequest(numberOfAdults, numberOfChildren, numberOfInfants);

        ticketPaymentService.makePayment(accountId, ticketPricingService.priceTickets(ticketTypeRequests));
        seatReservationService.reserveSeat(accountId, numberOfAdults + numberOfChildren);
    }

    /**
     * Validates the supplied ticket quantities as specified in the Business Rules.
     * <pre>
     * 1) Checks that no negative numbers have been supplied.
     * 2) That the total number of tickets is not greater than 20.
     * 3) That bookings for children have an adult present.
     * 4) That all infants have a lap to sit on.
     * </pre> Throws an InvalidPurchaseException if the Business Rules are not met.
     *
     * @param numberOfAdults   The number of adult tickets requested.
     * @param numberOfChildren The number of child tickets requested.
     * @param numberOfInfants  The number of infant tickets requested.
     */
    private void validateTicketRequest(int numberOfAdults, int numberOfChildren, int numberOfInfants) {
        if (numberOfAdults < 0) {
            throw new InvalidPurchaseException("Negative number of adult tickets.");
        }
        if (numberOfChildren < 0) {
            throw new InvalidPurchaseException("Negative number of child tickets.");
        }
        if (numberOfInfants < 0) {
            throw new InvalidPurchaseException("Negative number of infant tickets.");
        }
        if ((numberOfAdults + numberOfChildren + numberOfInfants) > 20) {
            throw new InvalidPurchaseException("Attempted to purchase more than 20 tickets.");
        }
        if ((numberOfChildren + numberOfInfants) > 0 && numberOfAdults == 0) {
            throw new InvalidPurchaseException("Children and Infants require an Adult for booking.");
        }
        if (numberOfInfants > numberOfAdults) {
            throw new InvalidPurchaseException("More Infants than Adults, not enough laps.");
        }
    }

    /**
     * Collects the number of tickets for a given type.
     * <p>
     * It is assumed that if two TicketTypeRequests are supplied for a given
     * type that this is valid and both of them will be counted to give a total
     * for that type.
     *
     * @param type               The type of ticket to count for.
     * @param ticketTypeRequests The array of requests supplied to extact the count from.
     * @return The number of tickets for that type.
     */
    private int countTicketsForType(Type type, TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).filter(ttr
                -> ttr.getTicketType() == type).mapToInt(t -> t.getNoOfTickets()).sum();
    }
}
