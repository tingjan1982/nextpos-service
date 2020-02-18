package io.nextpos.timecard.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserTimeCardsResponse {

    private List<UserTimeCardResponse> timeCards;
}
