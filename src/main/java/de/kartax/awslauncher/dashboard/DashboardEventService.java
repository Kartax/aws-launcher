package de.kartax.awslauncher.dashboard;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DashboardEventService {
    private final Set<DashboardView> listeners = ConcurrentHashMap.newKeySet();

    public void register(DashboardView view) {
        listeners.add(view);
    }

    public void unregister(DashboardView view) {
        listeners.remove(view);
    }

    public void broadcastEvent(DashboardUpdateEvent event) {
        listeners.forEach(view -> view.handleUpdate(event));
    }

    public void broadcastMessageOnlyEvent(Object from, String message) {
        var event = new DashboardUpdateEvent(from);
        event.setMessage(message);
        listeners.forEach(view -> view.handleUpdate(event));
    }


}
