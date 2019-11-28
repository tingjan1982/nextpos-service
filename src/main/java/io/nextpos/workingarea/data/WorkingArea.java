package io.nextpos.workingarea.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity(name = "client_working_area")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class WorkingArea extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    private String name;

    private int noOfPrintCopies;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @Fetch(FetchMode.SUBSELECT)
    private List<Printer> printers = new ArrayList<>();

    public WorkingArea(final Client
                               client, final String name) {
        this.client = client;
        this.name = name;
    }

    public void addPrinter(Printer printer) {
        printers.add(printer);
    }
}
