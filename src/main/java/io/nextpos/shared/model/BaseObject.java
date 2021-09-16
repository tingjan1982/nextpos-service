package io.nextpos.shared.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import java.util.Date;


@MappedSuperclass
@Getter
@Setter
@ToString
@RequiredArgsConstructor
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
