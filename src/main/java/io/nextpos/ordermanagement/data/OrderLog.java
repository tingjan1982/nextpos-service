package io.nextpos.ordermanagement.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@RequiredArgsConstructor
public class OrderLog {

     private final Date logDate;

     private final String who;

     private final String action;

     private List<OrderLogEntry> entries = new ArrayList<>();

     public void addOrderLogEntry(String key, String value) {
          this.addChangeOrderLogEntry(key, null, value);
     }

     public void addChangeOrderLogEntry(String key, String from, String to) {
          entries.add(new OrderLogEntry(key, from, to));
     }

     @Data
     @AllArgsConstructor
     public static class OrderLogEntry {

          private String name;

          private String from;

          private String to;
     }
}
