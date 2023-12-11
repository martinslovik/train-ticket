package plan.actor;

import akka.actor.AbstractLoggingActor;
import akka.japi.pf.ReceiveBuilder;
import edu.fudan.common.entity.Route;
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
import plan.message.GetStationsRequest;

import java.util.List;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StationActor extends AbstractLoggingActor {

    public static final String TS_TRAVEL_SERVICE = "ts-travel-service";
    public static final String TS_TRAVEL_2_SERVICE = "ts-travel2-service";
    public static final String API_V_1_TRAVEL_SERVICE_ROUTES = "/api/v1/travelservice/routes/";
    public static final String API_V_1_TRAVEL_2_SERVICE_ROUTES = "/api/v1/travel2service/routes/";

    private final RestTemplate restTemplate;

    @Autowired
    public StationActor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private String getServiceUrl(String url) {
        return "http://" + url;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder
                .create()
                .match(GetStationsRequest.class, request -> {
                    log().info("[StationActor][GetStationsRequest][Start]");
                    String travelServiceUrl = getServiceUrl(TS_TRAVEL_SERVICE);
                    String travel2ServiceUrl = getServiceUrl(TS_TRAVEL_2_SERVICE);
                    String path;

                    if (request.getTripId().charAt(0) == 'G' || request.getTripId().charAt(0) == 'D') {
                        path = travelServiceUrl + API_V_1_TRAVEL_SERVICE_ROUTES + request.getTripId();
                    } else {
                        path = travel2ServiceUrl + API_V_1_TRAVEL_2_SERVICE_ROUTES + request.getTripId();
                    }

                    HttpEntity requestEntity = new HttpEntity(null);
                    ResponseEntity<Response<Route>> responseEntity = restTemplate.exchange(
                            path,
                            HttpMethod.GET,
                            requestEntity,
                            new ParameterizedTypeReference<Response<Route>>() {
                            }
                    );

                    List<String> stations = responseEntity.getBody().getData().getStations();
                    log().info("[StationActor][GetStationsRequest][Size:{}]", stations.size());

                    sender().tell(stations, self());
                })
                .matchAny(msg -> {
                    log().warning("StationActor: Unhandled message received: " + msg);
                    unhandled(msg);
                })
                .build();
    }
}
