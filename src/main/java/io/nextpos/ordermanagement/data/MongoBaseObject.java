package io.nextpos.ordermanagement.data;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;

import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Data
abstract class MongoBaseObject implements Persistable {

    /**
     * https://stackoverflow.com/questions/25333711/what-is-the-use-of-the-temporal-annotation-in-hibernate
     */
    @Temporal(TemporalType.TIMESTAMP)
    @CreatedDate
    private Date createdDate;

    @Temporal(TemporalType.TIMESTAMP)
    @LastModifiedDate
    private Date modifiedDate;
}
