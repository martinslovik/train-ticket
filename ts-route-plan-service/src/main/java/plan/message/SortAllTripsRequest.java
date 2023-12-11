package plan.message;

import edu.fudan.common.entity.TripResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SortAllTripsRequest {

    List<TripResponse> normalTrainTripResponses;

    List<TripResponse> highSpeedTrainTripResponses;
}
