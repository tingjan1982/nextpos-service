package io.nextpos.client.data;

import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.shared.model.BaseObject;
import io.nextpos.shared.model.ClientObject;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * ClientSetting differs from attributes in that setting has typed value and enabled flag to control is the value is used.
 *
 * Hibernate custom type:
 * https://www.baeldung.com/hibernate-custom-types
 *
 * JPA attribute converter:
 * https://www.baeldung.com/jpa-attribute-converters
 */
@Entity(name = "client_settings")
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "clientId"})})
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ClientSetting extends BaseObject implements ClientObject {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;

    @ManyToOne
    @JoinColumn(name = "clientId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @Enumerated(EnumType.STRING)
    private SettingName name;

    private String storedValue;

    @Enumerated(EnumType.STRING)
    private ValueType valueType;

    private boolean enabled;

    public ClientSetting(final Client client, final SettingName name, final String storedValue, final ValueType valueType, final boolean enabled) {
        this.client = client;
        this.name = name;
        this.storedValue = storedValue;
        this.valueType = valueType;
        this.enabled = enabled;
    }

    /**
     * This method is not intended to be used but rather document the fact that ClientSettingsService
     * offers the method to get the actual value intended for this ClientSettings object.
     */
    public <T> T getActualStoredValue(ClientSettingsService clientSettingsService, Class<T> targetType) {
        return clientSettingsService.getActualStoredValue(this, targetType);
    }

    public enum SettingName {

        SERVICE_CHARGE(ValueType.BIG_DECIMAL),

        TAX_INCLUSIVE(ValueType.BOOLEAN),

        /**
         * This is a placeholder to customize offer application behavior, to determine whether offer discount can be stacked or is exclusive only.
         */
        STACKABLE_OFFER(ValueType.BOOLEAN);

        private ValueType valueType;

        SettingName(final ValueType valueType) {
            this.valueType = valueType;
        }

        public ValueType getValueType() {
            return valueType;
        }
    }

    public enum ValueType {

        STRING(String.class),

        BOOLEAN(Boolean.class),

        BIG_DECIMAL(BigDecimal.class),

        INTEGER(Integer.class),

        DATE(Date.class);

        private Class<?> classType;

        ValueType(final Class<?> classType) {
            this.classType = classType;
        }

        public Class<?> getClassType() {
            return classType;
        }
    }
}
