package cleansing.engine.phoneCleansing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CallResultResponse<T> {
    private boolean success;
    private T data;
}
