package io.nextpos.workingarea.data;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity(name = "client_working_area")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
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

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(joinColumns = @JoinColumn(name = "working_area_id"), inverseJoinColumns = @JoinColumn(name = "printer_id"))
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<Printer> printers = new HashSet<>();

    @ManyToMany(mappedBy = "workingAreas", fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<ClientUser> clientUsers = new HashSet<>();

    public WorkingArea(final Client client, final String name) {
        this.client = client;
        this.name = name;
        this.visibility = Visibility.ALL;
    }

    public void clearPrinters() {
        // todo: this is workaround because Hibernate's PersistentSet doesn't perform remove(obj) properly.
        printers.forEach(p -> p.getWorkingAreas().removeIf(next -> StringUtils.equals(next.getId(), this.getId())));
        printers.clear();
    }

    public void addPrinter(Printer printer) {
        printers.add(printer);
        printer.getWorkingAreas().add(this);
    }

    public enum Visibility {
        ALL, PRODUCT, ROSTER
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkingArea that = (WorkingArea) o;
        return Objects.equals(id, that.id) && client.equals(that.client) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, client, name);
    }
}
