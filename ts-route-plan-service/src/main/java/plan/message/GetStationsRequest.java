package plan.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetStationsRequest {

    private String tripId;
}
