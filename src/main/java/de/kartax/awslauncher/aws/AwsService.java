package de.kartax.awslauncher.aws;

import de.kartax.awslauncher.dashboard.DashboardEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sfn.SfnClient;
import software.amazon.awssdk.services.sfn.model.StartExecutionRequest;
import software.amazon.awssdk.services.sfn.model.StartExecutionResponse;

import java.time.Duration;

@Slf4j
@Service
public class AwsService {

    String STATE_MACHINE_ARN = "arn:aws:states:eu-central-1:507136533040:stateMachine:LaunchRecentGamingRig";

    private final DashboardEventService eventService;
    private final SfnClient sfnClient;
    private final AwsBackgroundTask awsBackgroundTask;

    public AwsService(DashboardEventService eventService, SfnClient sfnClient, AwsBackgroundTask awsBackgroundTask) {
        this.eventService = eventService;
        this.sfnClient = sfnClient;
        this.awsBackgroundTask = awsBackgroundTask;
    }

    public void launch(String name, String instanceType, Double maxSpotPrice){
        log.debug("Launching EC2 instance: {} - {}", name, instanceType);
        eventService.broadcastMessageOnlyEvent(this, "Launching " + name + " of type " + instanceType + " with max spot price " + maxSpotPrice);

        try {
            String input = String.format("{\"instanceType\": \"%s\", \"maxSpotPrice\": \"%f\"}", instanceType, maxSpotPrice);

            StartExecutionRequest request = StartExecutionRequest.builder()
                    .stateMachineArn(STATE_MACHINE_ARN)
                    .input(input)
                    .build();

            StartExecutionResponse response = sfnClient.startExecution(request);
            log.debug("Started state machine execution with ARN: {}", response.executionArn());
            eventService.broadcastMessageOnlyEvent(this, "Started state machine execution with ARN: "+response.executionArn());

            awsBackgroundTask.runOnceDelayed(Duration.ofSeconds(5));


        } catch (Exception e) {
            log.debug("Error launching: ", e);
            eventService.broadcastMessageOnlyEvent(this, "ERROR:: "+e.getMessage());
        }
    }
}
