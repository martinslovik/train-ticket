package plan.message;

import edu.fudan.common.entity.TripInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpHeaders;

@Data
@AllArgsConstructor
public class GetTripFromHighSpeedTrainRequest {

    private TripInfo tripInfo;

    private HttpHeaders headers;
}
