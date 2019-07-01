package io.nextpos.shared.model.idgenerator;

import io.nextpos.shared.model.ObjectVersioning;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

public class ObjectVersionIdGenerator implements IdentifierGenerator, Configurable {

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) throws MappingException {

    }

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object object) throws HibernateException {

        if (!(object instanceof ObjectVersioning)) {
            throw new RuntimeException("This id generator is only suitable for class that implements " + ObjectVersioning.class.getName());
        }

        final ObjectVersioning objectVersioning = (ObjectVersioning) object;
        final Serializable parentId = objectVersioning.getParent().getId();

        return parentId + "-" + objectVersioning.getState().name().toLowerCase();
    }
}
