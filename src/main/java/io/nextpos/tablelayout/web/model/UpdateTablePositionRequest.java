package io.nextpos.tablelayout.web.model;

import io.nextpos.tablelayout.data.TableLayout;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
public class UpdateTablePositionRequest {

    private String x;

    private String y;

    public TableLayout.TableDetails.ScreenPosition toScreenPosition() {

        if (StringUtils.isNotEmpty(x) && StringUtils.isNotEmpty(y)) {
            return new TableLayout.TableDetails.ScreenPosition(x, y);
        }

        return null;
    }
}
