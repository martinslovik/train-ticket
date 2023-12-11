package plan.actor;

import akka.actor.AbstractLoggingActor;
import akka.japi.pf.ReceiveBuilder;
import edu.fudan.common.entity.TripResponse;
import edu.fudan.common.util.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import plan.message.GetTripFromHighSpeedTrainRequest;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HighSpeedTrainActor extends AbstractLoggingActor {

    public static final String API_V_1_TRAVEL_SERVICE_TRIPS_LEFT = "/api/v1/travelservice/trips/left";
    private final RestTemplate restTemplate;

    @Autowired
    public HighSpeedTrainActor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getServiceUrl() {
        return "http://" + "ts-travel-service";
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder
                .create()
                .match(GetTripFromHighSpeedTrainRequest.class, request -> {
                    log().info("[getTripFromHighSpeedTravelService][trip info: {}]", request.getTripInfo());
                    String travelServiceUrl = getServiceUrl();
                    HttpEntity requestEntity = new HttpEntity(request.getTripInfo(), null);
                    ResponseEntity<Response<ArrayList<TripResponse>>> responseEntity = restTemplate.exchange(
                            travelServiceUrl + API_V_1_TRAVEL_SERVICE_TRIPS_LEFT,
                            HttpMethod.POST,
                            requestEntity,
                            new ParameterizedTypeReference<Response<ArrayList<TripResponse>>>() {
                            }
                    );

                    List<TripResponse> tripResponses = responseEntity.getBody().getData();
                    log().info("[getTripFromHighSpeedTravelService][Route Plan Get Trip][Size:{}]", tripResponses.size());
                    sender().tell(tripResponses, self());
                })
                .matchAny(msg -> {
                    log().warning("HighSpeedTrainActor: Unhandled message received: " + msg);
                    unhandled(msg);
                })
                .build();
    }
}
