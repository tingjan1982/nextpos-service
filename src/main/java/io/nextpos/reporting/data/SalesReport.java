package io.nextpos.reporting.data;

import lombok.Data;
import org.bson.Document;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesReport {

    @Id
    private String id;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

    private int orderCount;

    private BigDecimal salesTotal;

    private List<Document> salesByProducts;
}
