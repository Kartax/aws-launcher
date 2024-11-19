package de.kartax.awslauncher.aws;


import de.kartax.awslauncher.config.AwsConfig;
import de.kartax.awslauncher.dashboard.DashboardEventService;
import de.kartax.awslauncher.dashboard.DashboardUpdateEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.budgets.BudgetsClient;
import software.amazon.awssdk.services.budgets.model.DescribeBudgetsRequest;
import software.amazon.awssdk.services.budgets.model.DescribeBudgetsResponse;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AwsBackgroundTask {

    private final TaskScheduler taskScheduler;
    private final DashboardEventService eventService;
    private final Ec2Client ec2Client;
    private final AwsConfig awsConfig;
    private final BudgetsClient budgetsClient;

    Map<String, Double> instanceSpotPrices = Map.of(
            "g4dn.xlarge", 0.29,
            "g4dn.2xlarge", 0.50,
            "g6.2xlarge", 0.55,
            "g5.2xlarge", 0.57);

    private ScheduledFuture<?> scheduledFuture;


    public AwsBackgroundTask(TaskScheduler taskScheduler, DashboardEventService eventService, Ec2Client ec2Client, AwsConfig awsConfig, BudgetsClient budgetsClient) {
        this.taskScheduler = taskScheduler;
        this.eventService = eventService;
        this.ec2Client = ec2Client;
        this.awsConfig = awsConfig;
        this.budgetsClient = budgetsClient;
    }

    @PostConstruct
    public void start() {
        scheduleHourlyTask();
    }

    private void scheduleHourlyTask() {
        log.debug("scheduleHourlyTask");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::run, Duration.ofHours(1));
    }

    public void runOnceDelayed(Duration delay) {
        log.debug("runOnceDelayed");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }

        // Schedule the single delayed run
        scheduledFuture = taskScheduler.schedule(() -> {
            // Run the task once
            run();
            // Reschedule the regular task after the single run
            scheduleHourlyTask();
        }, Instant.now().plus(delay));
    }

    public void run() {
        log.debug("run");
        var event = new DashboardUpdateEvent(this);
        try {
            event.setInstances(getAllInstances());
        } catch (Ec2Exception e) {
            event.appendMessage(e.getMessage());
        }
        try {
            event.setVolumes(getAllVolumes());
        } catch (Ec2Exception e) {
            event.appendMessage(e.getMessage());
        }
        try {
            event.setSnapshots(getAllSnapshots());
        } catch (Ec2Exception e) {
            event.appendMessage(e.getMessage());
        }
        try {
            event.setCurrentMonthCost(getCurrentMonthCost());
        } catch (Exception e) {
            event.appendMessage(e.getMessage());
        }
        try {
            event.setInstanceTypesWithPrice(getInstanceTypesWithPrice());
        } catch (Exception e) {
            event.appendMessage(e.getMessage());
        }
        eventService.broadcastEvent(event);
    }

    public List<Instance> getAllInstances() {
        log.debug("getAllInstances");
        DescribeInstancesRequest request = DescribeInstancesRequest.builder().build();

        return ec2Client.describeInstancesPaginator(request)
                .stream()
                .flatMap(response -> response.reservations().stream())
                .flatMap(reservation -> reservation.instances().stream())
                .collect(Collectors.toList());
    }

    public List<Volume> getAllVolumes() {
        log.debug("getAllVolumes");
        DescribeVolumesRequest request = DescribeVolumesRequest.builder().build();

        return ec2Client.describeVolumesPaginator(request)
                .stream()
                .flatMap(response -> response.volumes().stream())
                .collect(Collectors.toList());
    }

    public List<Snapshot> getAllSnapshots() {
        log.debug("getAllSnapshots");
        DescribeSnapshotsRequest request = DescribeSnapshotsRequest.builder().ownerIds("self").build();

        return ec2Client.describeSnapshotsPaginator(request)
                .stream()
                .flatMap(response -> response.snapshots().stream())
                .collect(Collectors.toList());
    }

    public List<BigDecimal> getCurrentMonthCost() {
        log.debug("getCurrentMonthCost");
        DescribeBudgetsRequest request = DescribeBudgetsRequest.builder()
                .accountId(awsConfig.getBudgetAccountId())
                .build();

        DescribeBudgetsResponse response = budgetsClient.describeBudgets(request);

        return response.budgets().stream()
                .filter(budget -> budget.budgetName().equals(awsConfig.getBudgetName()))
                .findFirst().map(budget -> List.of(budget.calculatedSpend().actualSpend().amount(), budget.budgetLimit().amount()))
                .orElse(List.of(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    public Map<String, Double> getInstanceTypesWithPrice() {
        log.debug("getInstanceTypesWithPrice");
        Map<String, Double> spotPrices = new HashMap<>();

        DescribeSpotPriceHistoryRequest request = DescribeSpotPriceHistoryRequest.builder()
                .startTime(Instant.now().minus(Duration.ofHours(1)))
                .endTime(Instant.now())
                .instanceTypes(
                        InstanceType.G4_DN_XLARGE,
                        InstanceType.G4_DN_2_XLARGE,
                        InstanceType.G6_2_XLARGE,
                        InstanceType.G5_2_XLARGE)
                .build();

        ec2Client.describeSpotPriceHistoryPaginator(request)
                .stream()
                .flatMap(response -> response.spotPriceHistory().stream())
                .forEach(spotPrice -> {
                    BigDecimal price = new BigDecimal(spotPrice.spotPrice()).setScale(2, RoundingMode.HALF_UP);
                    spotPrices.put(spotPrice.instanceTypeAsString(), price.doubleValue());
                });

        return spotPrices;
    }
}
