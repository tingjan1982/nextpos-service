package io.nextpos.reservation.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AvailableTablesResponse {

    private List<String> results;
}
