package io.nextpos.announcement.data;

import io.nextpos.shared.model.MongoBaseObject;
import io.nextpos.shared.model.WithClientId;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class Announcement extends MongoBaseObject implements WithClientId {

    @Id
    private String id;

    private String clientId;

    private String titleIcon;

    private String titleText;

    private String markdownContent;

    @Indexed(name = "expireAt", expireAfterSeconds = 0)
    private Date expireAt;

    private int order;

    public Announcement(final String clientId, final String titleIcon, final String titleText, final String markdownContent) {
        this.clientId = clientId;
        this.titleIcon = titleIcon;
        this.titleText = titleText;
        this.markdownContent = markdownContent;
    }
}
