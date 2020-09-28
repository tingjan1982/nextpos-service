package io.nextpos.tablelayout.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.ObjectAlreadyExistsException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Entity(name = "client_table_layout")
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"layoutName", "clientId"})})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class TableLayout extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    private String layoutName;

    private AtomicInteger internalCounter;


    @OneToMany(mappedBy = "tableLayout", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("tableName ASC")
    private List<TableDetails> tables = new ArrayList<>();

    public TableLayout(final Client client, final String layoutName) {
        this.client = client;
        this.layoutName = layoutName;
        this.internalCounter = new AtomicInteger(1);
    }

    public int getTotalCapacity() {
        return tables.stream()
                .mapToInt(TableDetails::getCapacity)
                .sum();
    }

    public TableLayout addTableDetails(TableDetails tableDetails) {

        tableDetails.setId(this.id + "-" + internalCounter.getAndIncrement());
        tableDetails.setTableLayout(this);
        tables.add(tableDetails);

        return this;
    }

    public TableDetails getTableDetails(String tableId) {

        return tables.stream().filter(t -> t.getId().equals(tableId)).findFirst().orElseThrow(() -> {
            throw new ObjectNotFoundException(tableId, TableDetails.class);
        });
    }

    public void checkForDuplicateTableName() {

        Set<String> tableNames = new HashSet<>();

        tables.forEach(td -> {
            if (!tableNames.add(td.getTableName())) {
                throw new ObjectAlreadyExistsException(td.getTableName(), TableDetails.class);
            }
        });
    }

    public void deleteTableDetails(String tableId) {
        tables.removeIf(t -> t.getId().equals(tableId));
    }

    @Entity(name = "client_table_details")
    @Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"tableName", "tableLayoutId"})})
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class TableDetails extends BaseObject {

        @Id
        private String id;

        @ManyToOne
        @JoinColumn(name = "tableLayoutId")
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private TableLayout tableLayout;

        private String tableName;

        private int capacity;

        @Embedded
        private ScreenPosition screenPosition;

        public TableDetails(final String tableName, int capacity) {
            this.tableName = tableName;
            this.capacity = capacity;
        }

        @Embeddable
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ScreenPosition {

            private String x;

            private String y;
        }
    }
}
