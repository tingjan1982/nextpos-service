package io.nextpos.shared.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import java.util.Date;


@MappedSuperclass
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseObject {

    protected Date createdTime;

    protected Date updatedTime;

    @PrePersist
    public void prePersist() {

        if (createdTime == null) {
            this.createdTime = new Date();
        }

        this.updatedTime = new Date();
    }
}
