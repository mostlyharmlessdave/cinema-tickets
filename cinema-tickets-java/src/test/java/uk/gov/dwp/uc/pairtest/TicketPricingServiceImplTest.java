package uk.gov.dwp.uc.pairtest;

import org.junit.Test;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;

import static org.junit.Assert.*;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

public class TicketPricingServiceImplTest {


    @Test
    public void testPriceTickets() {

        TicketPricingService ticketPricingService = new TicketPricingServiceImpl();

        // Test the three simple cases, there is no validation performed by the TicketPricingServiceImpl
        assertEquals("1 Adult Ticket should cost £20", 20, ticketPricingService.priceTickets(
                new TicketTypeRequest(ADULT, 1)));

        assertEquals("1 Child Ticket should cost £10", 10, ticketPricingService.priceTickets(
                new TicketTypeRequest(CHILD, 1)));

        assertEquals("1 Child Ticket should cost £0", 0, ticketPricingService.priceTickets(
                new TicketTypeRequest(INFANT, 1)));

        assertEquals("No tickets should cost £0", 0, ticketPricingService.priceTickets());

        // Negative ticket quantities are not priced.
        assertEquals("Checking the edge case", 0, ticketPricingService.priceTickets(
                new TicketTypeRequest(ADULT, -1),
                new TicketTypeRequest(CHILD, -1),
                new TicketTypeRequest(INFANT, -1)));

        // Test more complicated examples.
        assertEquals("10 Adult Tickets should cost £200", 200, ticketPricingService.priceTickets(
                new TicketTypeRequest(ADULT, 10)));

        assertEquals("13 Child Tickets should cost £130", 130, ticketPricingService.priceTickets(
                new TicketTypeRequest(CHILD, 13)));

        assertEquals("3 Adults and 13 Child Tickets should cost £190", 190, ticketPricingService.priceTickets(
                new TicketTypeRequest(ADULT, 3),
                new TicketTypeRequest(CHILD, 13)));

        assertEquals("3 Adults, 10 Child and 2 Infant Tickets should cost £160", 160, ticketPricingService.priceTickets(
                new TicketTypeRequest(ADULT, 3),
                new TicketTypeRequest(CHILD, 10),
                new TicketTypeRequest(INFANT, 2)));
    }
}