package plan.service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.pattern.Patterns;
import akka.util.Timeout;
import edu.fudan.common.entity.RoutePlanInfo;
import edu.fudan.common.entity.RoutePlanResultUnit;
import edu.fudan.common.entity.TripInfo;
import edu.fudan.common.entity.TripResponse;
import edu.fudan.common.util.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import plan.actor.HighSpeedTrainActor;
import plan.actor.NormalTrainActor;
import plan.actor.SortingActor;
import plan.integration.akka.SpringAkkaExtension;
import plan.message.GetStationsRequest;
import plan.message.GetTripFromHighSpeedTrainRequest;
import plan.message.GetTripFromNormalTrainRequest;
import plan.message.SortAllTripsRequest;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RoutePlanServiceImpl implements RoutePlanService {

    private final ActorSystem actorSystem;
    private final SpringAkkaExtension springAkkaExtension;
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private static final Logger log = LoggerFactory.getLogger(RoutePlanServiceImpl.class);
    private final Timeout timeout;

    @Autowired
    public RoutePlanServiceImpl(ActorSystem actorSystem, SpringAkkaExtension springAkkaExtension,
                                RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        this.actorSystem = actorSystem;
        this.springAkkaExtension = springAkkaExtension;
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
    }

    public Response searchCheapestResult(RoutePlanInfo info, HttpHeaders headers) {
        TripInfo tripInfo = new TripInfo();
        tripInfo.setStartPlace(info.getStartStation());
        tripInfo.setEndPlace(info.getEndStation());
        tripInfo.setDepartureTime(info.getTravelDate());

        try {
            ActorRef highSpeedTrainActor = actorSystem
                    .actorOf(springAkkaExtension
                            .props(SpringAkkaExtension.classNameToSpringName(HighSpeedTrainActor.class)));
            Future<Object> highSpeedTrainActorResponse = Patterns.ask(highSpeedTrainActor, new GetTripFromHighSpeedTrainRequest(tripInfo, headers), timeout);
            List<TripResponse> highSpeedTrainTripResponses = (List<TripResponse>) Await.result(highSpeedTrainActorResponse, timeout.duration());
            log.info("[searchCheapestResult][HighSpeedTrainTripResponses: {}]", highSpeedTrainTripResponses);

            ActorRef normalTrainActor = actorSystem
                    .actorOf(springAkkaExtension
                            .props(SpringAkkaExtension.classNameToSpringName(NormalTrainActor.class)));
            Future<Object> normalTrainActorResponse = Patterns.ask(normalTrainActor, new GetTripFromNormalTrainRequest(tripInfo, headers), timeout);
            List<TripResponse> normalTrainTripResponses = (List<TripResponse>) Await.result(normalTrainActorResponse, timeout.duration());
            log.info("[searchCheapestResult][HighSpeedTrainTripResponses: {}]", normalTrainTripResponses);

            ActorRef sortingActor = actorSystem
                    .actorOf(springAkkaExtension
                            .props(SpringAkkaExtension.classNameToSpringName(SortingActor.class)));
            Future<Object> sortingActorResponse = Patterns.ask(sortingActor, new SortAllTripsRequest(normalTrainTripResponses, highSpeedTrainTripResponses), timeout);
            List<TripResponse> sortingActorResponses = (List<TripResponse>) Await.result(sortingActorResponse, timeout.duration());
            log.info("[searchCheapestResult][SortingActorResponses: {}]", sortingActorResponses);

            List<RoutePlanResultUnit> units = mapToRoutePlanResultUnits(sortingActorResponses);

            return new Response<>(1, "Success", units);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<RoutePlanResultUnit> mapToRoutePlanResultUnits(List<TripResponse> tripResponses) throws Exception {
        List<RoutePlanResultUnit> units = new ArrayList<>();
        for (TripResponse tripResponse : tripResponses) {
            RoutePlanResultUnit unit = new RoutePlanResultUnit();
            unit.setTripId(tripResponse.getTripId().toString());
            unit.setTrainTypeName(tripResponse.getTrainTypeName());
            unit.setStartStation(tripResponse.getStartStation());
            unit.setEndStation(tripResponse.getTerminalStation());
            ActorRef stationActor = actorSystem
                    .actorOf(springAkkaExtension
                            .props(SpringAkkaExtension.classNameToSpringName(SortingActor.class)));
            Future<Object> stationActorResponse = Patterns.ask(stationActor, new GetStationsRequest(tripResponse.getTripId().toString()), timeout);
            List<String> stationActorResponses = (List<String>) Await.result(stationActorResponse, timeout.duration());
            log.info("[searchCheapestResult][StationActorResponses: {}]", stationActorResponses);
            unit.setStopStations(stationActorResponses);
            unit.setPriceForSecondClassSeat(tripResponse.getPriceForEconomyClass());
            unit.setPriceForFirstClassSeat(tripResponse.getPriceForConfortClass());
            unit.setStartTime(tripResponse.getStartTime());
            unit.setEndTime(tripResponse.getEndTime());
            units.add(unit);
        }
        return units;
    }

    public Response searchQuickestResult(RoutePlanInfo info, HttpHeaders headers) {
        return null;
    }

    public Response searchMinStopStations(RoutePlanInfo info, HttpHeaders headers) {
        return null;
    }
}
