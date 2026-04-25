package pl.polsl.tab.kurier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pl.polsl.tab.kurier.model.DeliveryMode;
import pl.polsl.tab.kurier.model.PriceDelta;
import pl.polsl.tab.kurier.model.Role;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;
import pl.polsl.tab.kurier.repository.PriceDeltaRepository;
import pl.polsl.tab.kurier.repository.RoleRepository;
import pl.polsl.tab.kurier.repository.StatusRepository;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final StatusRepository statusRepository;
    private final DeliveryModeRepository deliveryModeRepository;
    private final PriceDeltaRepository priceDeltaRepository;

    public DataSeeder(RoleRepository roleRepository,
                      StatusRepository statusRepository,
                      DeliveryModeRepository deliveryModeRepository,
                      PriceDeltaRepository priceDeltaRepository) {
        this.roleRepository = roleRepository;
        this.statusRepository = statusRepository;
        this.deliveryModeRepository = deliveryModeRepository;
        this.priceDeltaRepository = priceDeltaRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedStatuses();
        seedDeliveryModes();
        seedPriceDelta();
    }

    private void seedRoles() {
        if (roleRepository.count() == 0) {
            List<String> roles = List.of("WORKER", "COURIER", "ADMIN", "CLIENT");
            for (String roleName : roles) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        }
    }

    private void seedStatuses() {
        if (statusRepository.count() == 0) {
            List<String> statuses = List.of(
                    "Created", "Collected", "In Transit", "Out for Delivery", 
                    "Delivered", "Failed", "Lost", "Damaged",
                    "REGISTERED", "PENDING_PICKUP", "AT_HUB", "UNDELIVERED"
            );
            for (String statusName : statuses) {
                Status status = new Status();
                status.setName(statusName);
                statusRepository.save(status);
            }
        }
    }

    private void seedDeliveryModes() {
        if (deliveryModeRepository.count() == 0) {
            List<String> modes = List.of("STANDARD", "EXPRESS", "ECONOMY");
            for (String modeName : modes) {
                DeliveryMode mode = new DeliveryMode();
                mode.setName(modeName);
                deliveryModeRepository.save(mode);
            }
        }
    }

    private void seedPriceDelta() {
        if (priceDeltaRepository.count() == 0) {
            PriceDelta pd = new PriceDelta();
            pd.setWeightDelta(BigDecimal.valueOf(2.5)); // $2.5 per kg
            pd.setLengthDelta(BigDecimal.valueOf(0.1)); // $0.1 per cm
            pd.setWidthDelta(BigDecimal.valueOf(0.1));  // $0.1 per cm
            pd.setHeightDelta(BigDecimal.valueOf(0.1)); // $0.1 per cm
            pd.setModeDelta(BigDecimal.valueOf(10.0));  // Base flat fee
            priceDeltaRepository.save(pd);
        }
    }
}
