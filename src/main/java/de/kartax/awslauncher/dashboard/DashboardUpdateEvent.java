package de.kartax.awslauncher.dashboard;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Snapshot;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DashboardUpdateEvent extends ApplicationEvent {
    private String message;
    private List<Instance> instances;
    private List<Volume> volumes;
    private List<Snapshot> snapshots;
    private List<BigDecimal> currentMonthCost;

    public DashboardUpdateEvent(Object source) {
        super(source);
    }

    public void appendMessage(String message){
        if(this.getMessage() == null) {
            this.message = message;
        }else{
            this.message += ", "+message;
        }

    }
}
