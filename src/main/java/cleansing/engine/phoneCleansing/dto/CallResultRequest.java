package cleansing.engine.phoneCleansing.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CallResultRequest {
    private String keyword;
    private String status;
    private String page;
    private String size;
}
