package io.nextpos.client.data;

import io.nextpos.shared.model.BaseObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity(name = "client_status")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClientStatus extends BaseObject {

    @Id
    private String id;

    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    private boolean noTableLayout;

    private boolean noTable;

    private boolean noCategory;

    private boolean noProduct;

    private boolean noWorkingArea;

    private boolean noPrinter;

    private boolean noElectronicInvoice;
}
