package io.nextpos.client.data;

import io.nextpos.merchandising.data.Offer;
import io.nextpos.product.data.Product;
import io.nextpos.product.data.ProductLabel;
import io.nextpos.product.data.ProductOption;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.tablelayout.data.TableLayout;
import io.nextpos.workingarea.data.Printer;
import io.nextpos.workingarea.data.WorkingArea;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The 1 to many associations here are declared in case of a force deletion of client that
 * would also cascade deletions of associated objects.
 */
@Entity(name = "client")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Client extends BaseObject {

    @Id
    @GenericGenerator(name = "clientid", strategy = "io.nextpos.shared.model.idgenerator.ClientIdGenerator")
    @GeneratedValue(generator = "clientid")
    private String id;

    private String clientName;

    private String username;

    private String masterPassword;

    private String roles;

    private String countryCode;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING_ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "attribute_key")
    @Column(name = "attribute_value")
    @CollectionTable(name = "client_attributes", joinColumns = @JoinColumn(name = "client_id"))
    private Map<String, String> attributes = new HashMap<>();

    /**
     * ClientSetting differs from attributes in that setting has typed value and enabled flag to control is the value is used.
     */
    @OneToMany(mappedBy = "client", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @MapKeyEnumerated(EnumType.STRING)
    private Map<ClientSetting.SettingName, ClientSetting> clientSettings = new HashMap<>();


    /**
     * The following associations exist so when client is deleted, all associated client objects are also removed via cascade operation.
     */

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Product> products;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductOption> productOptions;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ProductLabel> productLabels;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Offer> offers;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<TableLayout> tableLayouts;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WorkingArea> workingAreas;

    @OneToMany(mappedBy = "client", cascade = CascadeType.REMOVE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Printer> printers;


    public Client(final String clientName, final String username, final String masterPassword, final String countryCode) {
        this.clientName = clientName;
        this.username = username;
        this.masterPassword = masterPassword;
        this.countryCode = countryCode;
    }

    public Client addAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public void saveClientSettings(ClientSetting.SettingName settingName, String value, ClientSetting.ValueType valueType, boolean enabled) {

        ClientSetting clientSetting = clientSettings.get(settingName);

        if (clientSetting == null) {
            clientSetting = new ClientSetting(this, settingName, value, valueType, enabled);
            clientSettings.put(settingName, clientSetting);

        } else {
            clientSetting.setStoredValue(value);
            clientSetting.setEnabled(enabled);
            clientSetting.setValueType(valueType);
        }
    }

    public Optional<ClientSetting> getClientSettings(ClientSetting.SettingName settingName) {
        return Optional.ofNullable(clientSettings.get(settingName));
    }

    public enum Status {
        /**
         * New signed up client that hasn't been activated via email.
         */
        PENDING_ACTIVE,
        
        /**
         * Indicate client is active and eligible to full service capability.
         */
        ACTIVE,

        /**
         * Indicate client is denied access to service. It could be due to non-payment.
         */
        INACTIVE,

        /**
         * Marking client as deleted for client who wish to terminate service contract, but no actual records are deleted.
         */
        DELETED
    }

    /**
     * This enum identifies some common client attributes that have business meaning and are used in the frontend app.
     */
    public enum ClientAttributes {

        /**
         * Unified Business Number, which is a Taiwan specific attribute.
         */
        UBN,

        /**
         * Business address that displays on the receipt
         */
        ADDRESS
    }
}
