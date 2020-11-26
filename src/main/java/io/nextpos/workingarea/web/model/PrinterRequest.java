package io.nextpos.workingarea.web.model;

import io.nextpos.shared.model.validator.ValidEnum;
import io.nextpos.shared.model.validator.ValidIpAddress;
import io.nextpos.workingarea.data.Printer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrinterRequest {

    @NotEmpty
    private String name;

    @ValidIpAddress
    private String ipAddress;

    /**
     * Use serviceTypes instead.
     */
    @Deprecated
    @ValidEnum(enumType = Printer.ServiceType.class)
    private String serviceType;

    private Set<Printer.ServiceType> serviceTypes = new HashSet<>();

}
