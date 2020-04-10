package io.nextpos.reporting.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class ReportEnhancer {

    public static <T> void enhanceReportResult(IntStream dataRange,
                                               Supplier<Map<String, T>> mapProvider,
                                               Function<String, T> emptyResult,
                                               Consumer<List<T>> enhancedResultsConsumer) {

        final Map<String, T> resultMap = mapProvider.get();
        final int[] range = dataRange.toArray();

        if (range.length != resultMap.size()) {
            final List<T> enhancedResults = new ArrayList<>();

            for (final int i : range) {
                String id = String.valueOf(i);

                if (resultMap.containsKey(id)) {
                    enhancedResults.add(resultMap.get(id));
                } else {
                    enhancedResults.add(emptyResult.apply(id));
                }
            }

            enhancedResultsConsumer.accept(enhancedResults);
        }
    }

    public static <T, D> void enhanceReportResult(D[] dataType,
                                                  Supplier<Map<D, T>> mapProvider,
                                                  Function<D, T> emptyResult,
                                                  Consumer<List<T>> enhancedResultsConsumer) {

        final Map<D, T> resultMap = mapProvider.get();

        if (dataType.length != resultMap.size()) {
            final List<T> enhancedResults = new ArrayList<>();

            for (final D id : dataType) {

                if (resultMap.containsKey(id)) {
                    enhancedResults.add(resultMap.get(id));
                } else {
                    enhancedResults.add(emptyResult.apply(id));
                }
            }

            enhancedResultsConsumer.accept(enhancedResults);
        }
    }
}
