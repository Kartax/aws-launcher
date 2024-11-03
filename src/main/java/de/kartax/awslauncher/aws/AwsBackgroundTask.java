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
import java.time.Duration;
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
        scheduleTask();
    }

    private void scheduleTask() {
        log.debug("scheduling task");
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = taskScheduler.scheduleAtFixedRate(this::run, Duration.ofHours(1));
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
}
