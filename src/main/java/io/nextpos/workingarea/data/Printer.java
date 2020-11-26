package io.nextpos.workingarea.data;

import io.nextpos.client.data.Client;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;


@Entity(name = "client_printer")
@Table(name = "client_printer", uniqueConstraints = @UniqueConstraint(columnNames = {"clientId", "ipAddress"}))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Printer extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Client client;

    private String name;

    private String ipAddress;

    @Deprecated
    private ServiceType serviceType;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(joinColumns = @JoinColumn(name = "printer_id"))
    @Column(name = "service_type")
    @Enumerated(EnumType.STRING)
    private Set<ServiceType> serviceTypes = new HashSet<>();

    @ManyToMany(mappedBy = "printers", fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Set<WorkingArea> workingAreas = new HashSet<>();

    public Printer(final Client client, final String name, final String ipAddress, final Set<ServiceType> serviceTypes) {
        this.client = client;
        this.name = name;
        this.ipAddress = ipAddress;
        this.serviceTypes.addAll(serviceTypes);
    }

    public void replaceServiceTypes(Set<ServiceType> serviceTypes) {
        this.serviceTypes.clear();
        this.serviceTypes.addAll(serviceTypes);
    }

    /**
     * This determines which printer will handle work order and receipt.
     */
    public enum ServiceType {
        WORKING_AREA, CHECKOUT
    }
}
