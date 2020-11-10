package io.nextpos.shared.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

import java.time.YearMonth;

public class DocumentToYearMonth implements Converter<Document, YearMonth> {

    @Override
    public YearMonth convert(Document source) {
        return YearMonth.of(
                (int) source.get("year"),
                (int) source.get("month")
        );
    }
}
