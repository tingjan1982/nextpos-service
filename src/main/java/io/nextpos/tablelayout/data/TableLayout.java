package io.nextpos.tablelayout.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "client_table_layout")
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

    private int gridSizeX;

    private int gridSizeY;

    @OneToMany(mappedBy = "tableLayout", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<TableDetails> tables = new ArrayList<>();

    public TableLayout(final Client client, final String layoutName, final int gridSizeX, final int gridSizeY) {
        this.client = client;
        this.layoutName = layoutName;
        this.gridSizeX = gridSizeX;
        this.gridSizeY = gridSizeY;
    }

    public int getTotalCapacity() {
        return tables.stream()
                .mapToInt(TableDetails::getCapacity)
                .sum();
    }

    public TableLayout addTableDetails(TableDetails tableDetails) {

        if (tableDetails.getXCoordinate() < 0 || tableDetails.getXCoordinate() >= this.gridSizeX) {
            throw new GeneralApplicationException(String.format("Table's X coordinate needs to be within table layout's defined grid. x=%d, y=%d", this.gridSizeX, this.gridSizeY));
        }

        if (tableDetails.getYCoordinate() < 0 || tableDetails.getYCoordinate() >= this.gridSizeY) {
            throw new GeneralApplicationException(String.format("Table's Y coordinate needs to be within table layout's defined grid. x=%d, y=%d", this.gridSizeX, this.gridSizeY));
        }

        tableDetails.setTableLayout(this);
        tables.add(tableDetails);

        return this;
    }

    @Entity(name = "client_table_details")
    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor(access = AccessLevel.PACKAGE)
    public static class TableDetails extends BaseObject {

        @Id
        @GenericGenerator(name = "tableDetailsId", strategy = "io.nextpos.shared.model.idgenerator.TableDetailsIdGenerator")
        @GeneratedValue(generator = "tableDetailsId")
        private String id;

        @ManyToOne
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private TableLayout tableLayout;

        private String tableName;

        private int xCoordinate;

        private int yCoordinate;

        private int capacity;

        public TableDetails(final String tableName, final int xCoordinate, final int yCoordinate) {
            this.tableName = tableName;
            this.xCoordinate = xCoordinate;
            this.yCoordinate = yCoordinate;
        }
    }
}