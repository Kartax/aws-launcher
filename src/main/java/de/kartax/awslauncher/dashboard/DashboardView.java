package de.kartax.awslauncher.dashboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.component.grid.Grid;
import de.kartax.awslauncher.aws.AwsBackgroundTask;
import de.kartax.awslauncher.aws.AwsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
@Route("")
public class DashboardView extends VerticalLayout {

    private static final int MAX_LOG_SIZE = 100;
    private static final String FILE_LOG_MESSAGES = "logMessages.txt";
    private static final String CURRENT_MONTH_COST_PREFIX = "Current month: ";


    public static Map<String, Double> typeSpotPriceMap() {
        Map<String, Double> instanceTypeMap = new HashMap<>();
        instanceTypeMap.put("g4dn.xlarge", 0.30);
        instanceTypeMap.put("g4dn.2xlarge", 0.50);
        instanceTypeMap.put("g5.2xlarge", 0.58);
        return instanceTypeMap;
    }

    private final LinkedList<String> logMessages = new LinkedList<>();
    private final TextArea logArea;

    private final DashboardEventService eventService;
    private final AwsBackgroundTask awsBackgroundTask;
    private final AwsService awsService;
    private final Grid<Instance> instances;
    private final Grid<Volume> volumes;
    private final Grid<Snapshot> snapshots;
    private final TextField nameInput = new TextField("Name","GamingRig","Name");
    private final NumberField maxSpotPrice = new NumberField("max Spot Price");
    private final Button launchButton = new Button("Launch");
    private final ComboBox<String> instanceTypeComboBox = new ComboBox<>("Instance Type");
    private final Span costBadge = new Span(CURRENT_MONTH_COST_PREFIX );

    public DashboardView(@Value("${BUILD_TIMESTAMP}") String buildTimestamp, DashboardEventService eventService, AwsBackgroundTask awsBackgroundTask, AwsService awsService) {
        this.eventService = eventService;
        this.awsBackgroundTask = awsBackgroundTask;
        this.awsService = awsService;

        var heading = new H1("Aws GamingRig Dashboard");

        Span buildBadge = new Span("Build: "+buildTimestamp);
        buildBadge.getElement().getThemeList().add("badge");
        costBadge.getElement().getThemeList().add("badge");
        var badges = new HorizontalLayout(buildBadge, costBadge);

        nameInput.setReadOnly(true);
        instanceTypeComboBox.setItems(typeSpotPriceMap().keySet());
        instanceTypeComboBox.setValue("g4dn.xlarge");
        instanceTypeComboBox.addValueChangeListener(event -> maxSpotPrice.setValue(typeSpotPriceMap().get(event.getValue())));
        maxSpotPrice.setMin(0.1);
        maxSpotPrice.setMax(1.0);
        maxSpotPrice.setStep(0.01);
        maxSpotPrice.setValue(0.3);
        launchButton.addClickListener(clickEvent -> launchInstance());
        HorizontalLayout controls = new HorizontalLayout(nameInput, instanceTypeComboBox, maxSpotPrice, launchButton);
        controls.setAlignItems(Alignment.BASELINE);


        logArea = new TextArea("Logs");
        logArea.setWidthFull();
        logArea.setHeight("150px");
        logArea.setReadOnly(true);


        instances = new Grid<>();
        instances.addColumn(DashboardView::getNameOrId).setHeader("Instance").setAutoWidth(true);
        instances.addColumn(Instance::instanceType).setHeader("Type").setAutoWidth(true);
        instances.addColumn(instance -> instance.state().nameAsString()).setHeader("State").setAutoWidth(true);
        volumes = new Grid<>();
        volumes.addColumn(DashboardView::getNameOrId).setHeader("Volume").setAutoWidth(true);
        volumes.addColumn(Volume::size).setHeader("Size").setAutoWidth(true);
        volumes.addColumn(Volume::stateAsString).setHeader("State").setAutoWidth(true);
        snapshots = new Grid<>();
        snapshots.addColumn(DashboardView::getNameOrId).setHeader("Snapshot").setAutoWidth(true);
        snapshots.addColumn(DashboardView::getStartDateTime).setHeader("Started").setAutoWidth(true);
        snapshots.addColumn(Snapshot::stateAsString).setHeader("State").setAutoWidth(true);
        snapshots.addColumn(Snapshot::progress).setHeader("State").setAutoWidth(true);
        HorizontalLayout lists = new HorizontalLayout(instances, volumes, snapshots);
        lists.setWidthFull();

        add(heading, badges, controls, logArea, lists);
        setSizeFull();
    }

    private static String getStartDateTime(Snapshot snapshot) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(snapshot.startTime());
    }

    private static String getNameOrId(Instance instance) {
        return instance.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(Tag::value)
                .findFirst()
                .orElse(instance.instanceId());
    }

    private static String getNameOrId(Snapshot snapshot) {
        return snapshot.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(Tag::value)
                .findFirst()
                .orElse(snapshot.snapshotId());
    }

    private static String getNameOrId(Volume volume) {
        return volume.tags().stream()
                .filter(tag -> "Name".equals(tag.key()))
                .map(Tag::value)
                .findFirst()
                .orElse(volume.volumeId());
    }

    private void launchInstance(){
        log.debug("launchInstance");
        awsService.launch(nameInput.getValue(), instanceTypeComboBox.getValue(), maxSpotPrice.getValue());
    }

    public synchronized void logMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyy - HH:mm:ss"));
        String logEntry = String.format("%s : %s", timestamp, message);

        if (logMessages.size() >= MAX_LOG_SIZE) {
            logMessages.removeFirst();
        }
        logMessages.add(logEntry);
        saveLogMessages();
        updateLogArea();
    }

    private void saveLogMessages() {
        try {
            Path logFilePath = Paths.get(FILE_LOG_MESSAGES);
            Files.write(logFilePath, logMessages);
        } catch (IOException e) {
            log.error("Error saving log messages", e);
        }
    }

    private void loadLogMessages() {
        try {
            Path logFilePath = Paths.get("logMessages.txt");
            if (Files.exists(logFilePath)) {
                logMessages.clear();
                logMessages.addAll(Files.readAllLines(logFilePath));
                updateLogArea();
            }
        } catch (IOException e) {
            log.error("Error loading log messages", e);
        }
    }

    private void updateLogArea() {
        StringBuilder logContent = new StringBuilder();
        for (String logMessage : logMessages) {
            logContent.append(logMessage).append("\n");
        }
        logArea.setValue(logContent.toString());
        logArea.scrollToEnd();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        log.debug("onAttach");
        super.onAttach(attachEvent);
        loadLogMessages();
        eventService.register(this);
        awsBackgroundTask.run();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("onDetach");
        eventService.unregister(this);
        super.onDetach(detachEvent);
    }

    public void handleUpdate(DashboardUpdateEvent event) {
        log.debug("handleUpdate");
        getUI().ifPresent(ui -> ui.access(() -> {
            if(event.getMessage() != null){
                logMessage(event.getMessage());
            }
            if(event.getInstances() != null) {
                this.instances.setItems(event.getInstances());
                // check if any of the instances is running, starting or pending
                launchButton.setEnabled(event.getInstances().stream().noneMatch(instance ->
                                instance.state().nameAsString().startsWith("running") ||
                                instance.state().nameAsString().startsWith("pending")));
            }
            if(event.getVolumes() != null){
                this.volumes.setItems(event.getVolumes());
            }
            if(event.getSnapshots() != null) {
                this.snapshots.setItems(event.getSnapshots());
            }
            if(event.getCurrentMonthCost() != null){
                var cost = BigDecimal.valueOf(event.getCurrentMonthCost()).setScale(2, RoundingMode.HALF_UP);
                if(cost.doubleValue() > 40){
                    costBadge.getElement().getThemeList().add("error");
                    costBadge.getElement().getThemeList().remove("success");
                }else{
                    costBadge.getElement().getThemeList().add("success");
                    costBadge.getElement().getThemeList().remove("error");
                }
                costBadge.setText(CURRENT_MONTH_COST_PREFIX + cost + " $");
            }
            ui.push();
        }));
    }
}
