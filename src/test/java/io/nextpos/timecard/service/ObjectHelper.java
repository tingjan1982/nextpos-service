package io.nextpos.timecard.service;

import io.nextpos.timecard.data.UserTimeCard;

class ObjectHelper {

    public static UserTimeCard testTimeCard(String clientId, String username) {
        return new UserTimeCard(clientId, username, username, username);
    }
}
