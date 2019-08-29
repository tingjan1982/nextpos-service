package io.nextpos.shared.model.idgenerator;

import io.nextpos.client.data.Client;
import io.nextpos.shared.exception.GeneralApplicationException;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;

import java.io.Serializable;
import java.util.Properties;

/**
 * https://www.baeldung.com/hibernate-identifiers
 */
public class ClientIdGenerator implements IdentifierGenerator, Configurable {

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) throws MappingException {
        
    }

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object object) throws HibernateException {

        if (!object.getClass().isAssignableFrom(Client.class)) {
            throw new GeneralApplicationException("This generator can only be used on " + Client.class.getName());
        }

        final RandomValueStringGenerator randomValueStringGenerator = new RandomValueStringGenerator(28);
        return "cli-" + randomValueStringGenerator.generate();
    }
}
