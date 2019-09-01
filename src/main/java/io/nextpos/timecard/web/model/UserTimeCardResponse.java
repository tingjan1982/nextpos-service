package io.nextpos.timecard.web.model;

import io.nextpos.timecard.data.UserTimeCard;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class UserTimeCardResponse {

    private String id;

    private String clientId;

    private String username;

    private String nickname;

    private Date clockIn;

    private Date clockOut;

    private UserTimeCard.TimeCardStatus timeCardStatus;
}