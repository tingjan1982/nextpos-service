package io.nextpos.merchandising.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OffersResponse {

    private List<OfferResponse> results;
}
