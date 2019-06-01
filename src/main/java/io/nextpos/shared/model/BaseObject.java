package io.nextpos.shared.model;

import lombok.Data;

import java.util.Date;

@Data
public abstract class BaseObject {

    private Date createdTime;

    private Date updatedTime;
}
