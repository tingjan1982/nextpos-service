package io.nextpos.reporting.data;

import com.mongodb.BasicDBObject;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SalesReport {

    @Id
    private String id;

    private BigDecimal salesTotal;

    private LocalDate salesDate;

    private List<BasicDBObject> products;
}
