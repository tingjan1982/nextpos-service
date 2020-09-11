package io.nextpos.invoicenumber.web;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class InvoiceNumberRequest {

    @NotBlank
    @Size(min = 8, max = 8)
    private String ubn;

    @NotBlank
    @Size(min = 6, max = 7)
    private String rangeIdentifier;

    @Valid
    private NumberRangeRequest numberRange;

    @Data
    @NoArgsConstructor
    public static class NumberRangeRequest {

        @NotBlank
        @Size(min = 2, max = 2)
        private String prefix;

        @NotBlank
        @Size(min = 8, max = 8)
        private String rangeFrom;

        @NotBlank
        @Size(min = 8, max = 8)
        private String rangeTo;

    }
}
