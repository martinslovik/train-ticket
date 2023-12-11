package plan.actor;

import akka.actor.AbstractLoggingActor;
import akka.japi.pf.ReceiveBuilder;
import edu.fudan.common.entity.TripResponse;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import plan.message.SortAllTripsRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SortingActor extends AbstractLoggingActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder
                .create()
                .match(SortAllTripsRequest.class, request -> {
                    log().info("[SortingActor][SortAllTripsRequest][Start]");
                    List<TripResponse> finalResult = new ArrayList<>();
                    finalResult.addAll(request.getHighSpeedTrainTripResponses());
                    finalResult.addAll(request.getNormalTrainTripResponses());

                    List<TripResponse> sortedResult = finalResult.stream()
                            .sorted(Comparator.comparingDouble(trip -> Float.parseFloat(trip.getPriceForEconomyClass())))
                            .collect(Collectors.toList());

                    sender().tell(sortedResult, self());
                })
                .matchAny(msg -> {
                    log().warning("SortingActor: Unhandled message received: " + msg);
                    unhandled(msg);
                })
                .build();
    }
}
