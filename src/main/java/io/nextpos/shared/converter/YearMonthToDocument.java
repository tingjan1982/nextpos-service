package io.nextpos.shared.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

import java.time.YearMonth;

public class YearMonthToDocument implements Converter<YearMonth, Document> {

    @Override
    public Document convert(YearMonth source) {
        Document document = new Document();
        document.put("year", source.getYear());
        document.put("month", source.getMonthValue());

        return document;
    }
}
