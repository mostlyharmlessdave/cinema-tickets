package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;
import java.util.Map;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.*;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

public class TicketPricingServiceImpl implements TicketPricingService {

    /**
     * This is the simplest way of storing the pricing information. Ideally it
     * would be loaded from an external source so that the prices could be
     * altered without rebuilding and redeploying the application.
     */
    private Map<Type, Integer> pricing = Map.ofEntries(
            Map.entry(INFANT, 0),
            Map.entry(CHILD, 10),
            Map.entry(ADULT, 20));

    /**
     * This is a simple implementation of the pricing structure defined in the
     * Business Rules. It allows a more complicated pricing model, such as
     * discounting for larger orders, to be introduced in the future without
     * changes to the TicketService.
     *
     * It assumes the ticket quantities passed to it are valid and performs
     * no validation other than discarding 0 and negative quantities.
     *
     * @param ticketTypeRequests The array of TicketTypeRequests to price.
     * @return The total prices determined by the pricing model.
     */
    @Override
    public int priceTickets(TicketTypeRequest... ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).filter(ttr -> ttr.getNoOfTickets() > 0).mapToInt(ttr
                -> pricing.get(ttr.getTicketType()) * ttr.getNoOfTickets()).sum();
    }
}
