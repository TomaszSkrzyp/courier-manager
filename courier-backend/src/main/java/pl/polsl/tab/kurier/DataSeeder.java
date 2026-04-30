package pl.polsl.tab.kurier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pl.polsl.tab.kurier.model.DeliveryMode;
import pl.polsl.tab.kurier.model.PriceDelta;
import pl.polsl.tab.kurier.model.Address;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.model.Role;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.AddressRepository;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.PriceDeltaRepository;
import pl.polsl.tab.kurier.repository.RegionRepository;
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
    private final EmployeeRepository employeeRepository;
    private final AddressRepository addressRepository;
    private final RegionRepository regionRepository;

    public DataSeeder(RoleRepository roleRepository,
                      StatusRepository statusRepository,
                      DeliveryModeRepository deliveryModeRepository,
                      PriceDeltaRepository priceDeltaRepository,
                      EmployeeRepository employeeRepository,
                      AddressRepository addressRepository,
                      RegionRepository regionRepository) {
        this.roleRepository = roleRepository;
        this.statusRepository = statusRepository;
        this.deliveryModeRepository = deliveryModeRepository;
        this.priceDeltaRepository = priceDeltaRepository;
        this.employeeRepository = employeeRepository;
        this.addressRepository = addressRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedStatuses();
        seedDeliveryModes();
        seedPriceDelta();
        seedAdminUser();
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

    private void seedAdminUser() {
        if (employeeRepository.count() == 0) {
            Region defaultRegion = regionRepository.findByName("Warsaw")
                    .orElseGet(() -> {
                        Region region = new Region();
                        region.setName("Warsaw");
                        return regionRepository.save(region);
                    });

            Address defaultAddress = new Address();
            defaultAddress.setStreet("Main Street");
            defaultAddress.setBuildingNumber("1");
            defaultAddress.setPostalCode("00-001");
            defaultAddress.setRegion(defaultRegion);
            defaultAddress = addressRepository.save(defaultAddress);

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            Employee admin = new Employee();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setLogin("admin");
            admin.setPassword("admin");
            admin.setPesel("00000000000");
            admin.setRole(adminRole);
            admin.setAddress(defaultAddress);
            
            employeeRepository.save(admin);
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
            List<String> modes = List.of("NORMAL", "EXPRESS");
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
            pd.setNormalModeDelta(BigDecimal.valueOf(10.0));
            pd.setExpressModeDelta(BigDecimal.valueOf(25.0));
            priceDeltaRepository.save(pd);
        }
    }
}
