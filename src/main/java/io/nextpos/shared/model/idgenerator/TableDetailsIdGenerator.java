package io.nextpos.shared.model.idgenerator;

import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.tablelayout.data.TableLayout;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

public class TableDetailsIdGenerator implements IdentifierGenerator, Configurable {

    @Override
    public void configure(final Type type, final Properties params, final ServiceRegistry serviceRegistry) throws MappingException {

    }

    @Override
    public Serializable generate(final SharedSessionContractImplementor session, final Object object) throws HibernateException {

        if (!(object instanceof TableLayout.TableDetails)) {
            throw new GeneralApplicationException("This id generator is only suitable for " + TableLayout.TableDetails.class.getName());
        }

        final TableLayout.TableDetails tableDetails = (TableLayout.TableDetails) object;

        final Serializable parentId = tableDetails.getTableLayout().getId();

        return String.format("%s-%d-%d", parentId, tableDetails.getXCoordinate(), tableDetails.getYCoordinate());
    }
}
