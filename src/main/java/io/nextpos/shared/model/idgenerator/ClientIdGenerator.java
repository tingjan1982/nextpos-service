package io.nextpos.shared.model.idgenerator;

import io.nextpos.client.data.Client;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

public class ClientIdGenerator implements IdentifierGenerator, Configurable {

    private static final int ID_LENGTH = 8;

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) throws MappingException {
        
    }

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object object) throws HibernateException {

        if (!object.getClass().isAssignableFrom(Client.class)) {
            throw new RuntimeException("This generator can only be used on " + Client.class.getName());
        }

        Client client = (Client) object;
        String id = client.getUsername().toUpperCase();
        final int emailSeparator = id.indexOf('@');

        if (emailSeparator != -1) {
            id = id.substring(0, emailSeparator);
        }

        if (id.length() > ID_LENGTH) {
            id = id.substring(0, ID_LENGTH);
        }

        return id;
    }
}
