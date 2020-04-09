package io.nextpos.tablelayout.web.model;

import io.nextpos.tablelayout.data.TableLayout;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
public class UpdateTablePositionRequest {

    @NotBlank
    private String x;

    @NotBlank
    private String y;

    public TableLayout.TableDetails.ScreenPosition toScreenPosition() {
        return new TableLayout.TableDetails.ScreenPosition(x, y);
    }
}
