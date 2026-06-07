package pl.polsl.tab.kurier;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import pl.polsl.tab.kurier.model.DeliveryMode;
import pl.polsl.tab.kurier.model.PriceDelta;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Role;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.PriceDeltaRepository;
import pl.polsl.tab.kurier.repository.RegionRepository;
import pl.polsl.tab.kurier.repository.RoleRepository;
import pl.polsl.tab.kurier.repository.StatusRepository;

import pl.polsl.tab.kurier.model.Address;
import pl.polsl.tab.kurier.model.Parcel;
import pl.polsl.tab.kurier.repository.AddressRepository;
import pl.polsl.tab.kurier.repository.ParcelRepository;
import pl.polsl.tab.kurier.service.ParcelStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import pl.polsl.tab.kurier.service.RouteService;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final StatusRepository statusRepository;
    private final DeliveryModeRepository deliveryModeRepository;
    private final PriceDeltaRepository priceDeltaRepository;
    private final EmployeeRepository employeeRepository;
    private final RegionRepository regionRepository;
    private final ParcelRepository parcelRepository;
    private final AddressRepository addressRepository;
    private final RouteService routeService;

    public DataSeeder(RoleRepository roleRepository,
                      StatusRepository statusRepository,
                      DeliveryModeRepository deliveryModeRepository,
                      PriceDeltaRepository priceDeltaRepository,
                      EmployeeRepository employeeRepository,
                      RegionRepository regionRepository,
                      ParcelRepository parcelRepository,
                      AddressRepository addressRepository,
                      RouteService routeService) {
        this.roleRepository = roleRepository;
        this.statusRepository = statusRepository;
        this.deliveryModeRepository = deliveryModeRepository;
        this.priceDeltaRepository = priceDeltaRepository;
        this.employeeRepository = employeeRepository;
        this.regionRepository = regionRepository;
        this.parcelRepository = parcelRepository;
        this.addressRepository = addressRepository;
        this.routeService = routeService;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedStatuses();
        seedRegions();
        seedDeliveryModes();
        seedPriceDelta();
        seedAdminUser();
        seedWorkerUser(); // New: seed worker
        seedCourierAndParcel();
    }

    private void seedWorkerUser() {
        if (employeeRepository.findByLogin("worker1").isEmpty()) {
            Role workerRole = roleRepository.findByName("WORKER")
                    .orElseThrow(() -> new RuntimeException("WORKER role not found"));

            Employee worker = new Employee();
            worker.setFirstName("Emma");
            worker.setLastName("Worker");
            worker.setLogin("worker1");
            worker.setPassword("admin");
            worker.setPesel("55555555555");
            worker.setRole(workerRole);
            employeeRepository.save(worker);
        }
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

    private void seedRegions() {
        if (regionRepository.count() == 0) {
            List<String> regions = List.of("KATOWICE", "RADOM", "WARSZAWA");
            for (String regionName : regions) {
                pl.polsl.tab.kurier.model.Region region = new pl.polsl.tab.kurier.model.Region();
                region.setName(regionName);
                regionRepository.save(region);
            }
        }
    }

    private void seedStatuses() {
        if (statusRepository.count() == 0) {
            List<String> statuses = List.of(
                    "REGISTERED", "PENDING_PICKUP", "AT_HUB", "IN_TRANSIT", 
                    "OUT_FOR_DELIVERY", "DELIVERED", "FAILED", "LOST", "DAMAGED", "UNDELIVERED"
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
            pd.setWeightDelta(BigDecimal.valueOf(2.5));
            pd.setLengthDelta(BigDecimal.valueOf(0.1));
            pd.setWidthDelta(BigDecimal.valueOf(0.1));
            pd.setHeightDelta(BigDecimal.valueOf(0.1));
            pd.setNormalModeDelta(BigDecimal.valueOf(10.0));
            pd.setExpressModeDelta(BigDecimal.valueOf(25.0));
            priceDeltaRepository.save(pd);
        }
    }

    private void seedAdminUser() {
        if (employeeRepository.findByLogin("admin").isEmpty()) {
            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            Employee admin = new Employee();
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setLogin("admin");
            admin.setPassword("admin");
            admin.setPesel("00000000000");
            admin.setRole(adminRole);
            admin.setAddress(null); // No default address/region
            
            employeeRepository.save(admin);
        }
    }

    private void seedCourierAndParcel() {
        if (employeeRepository.findByLogin("courier_kato").isEmpty()) {
            Role courierRole = roleRepository.findByName("COURIER")
                    .orElseThrow(() -> new RuntimeException("COURIER role not found"));

            pl.polsl.tab.kurier.model.Region katowice = regionRepository.findByName("KATOWICE")
                    .orElseThrow(() -> new RuntimeException("KATOWICE region not found"));
            pl.polsl.tab.kurier.model.Region radom = regionRepository.findByName("RADOM")
                    .orElseThrow(() -> new RuntimeException("RADOM region not found"));
            pl.polsl.tab.kurier.model.Region warszawa = regionRepository.findByName("WARSZAWA")
                    .orElseThrow(() -> new RuntimeException("WARSZAWA region not found"));

            // Local Couriers
            createCourier("courier_kato", "Kato", "Local", "10000000001", courierRole, Set.of(katowice));
            createCourier("courier_radom", "Radom", "Local", "10000000002", courierRole, Set.of(radom));
            createCourier("courier_waw", "Waw", "Local", "10000000003", courierRole, Set.of(warszawa));

            // Linehaul Couriers
            createCourier("linehaul_kato_radom", "KatoRadom", "Linehaul", "20000000001", courierRole, Set.of(katowice, radom));
            createCourier("linehaul_radom_waw", "RadomWaw", "Linehaul", "20000000002", courierRole, Set.of(radom, warszawa));

            if (parcelRepository.count() == 0) {
                DeliveryMode normal = deliveryModeRepository.findByName("NORMAL").get();
                Status registered = statusRepository.findByName(ParcelStatus.REGISTERED).get();

                // Parcel 1: KATOWICE -> WARSZAWA (Multi-hop)
                Address senderAddr = createAddress(katowice, "Korfantego", "10", "40-001");
                Address recipientAddr = createAddress(warszawa, "Marszałkowska", "50", "00-001");
                createParcel("555 123 456", BigDecimal.valueOf(5.0), normal, senderAddr, recipientAddr, katowice, katowice, registered);

                // Parcel 2: KATOWICE -> RADOM (Single linehaul hop)
                Address senderAddr2 = createAddress(katowice, "Chorzowska", "5", "40-101");
                Address recipientAddr2 = createAddress(radom, "Żeromskiego", "12", "26-600");
                createParcel("555 987 654", BigDecimal.valueOf(2.0), normal, senderAddr2, recipientAddr2, katowice, katowice, registered);

                // Parcel 3: KATOWICE -> KATOWICE (Pure local)
                Address senderAddr3 = createAddress(katowice, "Ligonia", "7", "40-036");
                Address recipientAddr3 = createAddress(katowice, "Mickiewicza", "15", "40-092");
                createParcel("555 111 222", BigDecimal.valueOf(1.0), normal, senderAddr3, recipientAddr3, katowice, katowice, registered);
            }
        }
    }

    private void createCourier(String login, String first, String last, String pesel, Role role, Set<pl.polsl.tab.kurier.model.Region> regions) {
        Employee courier = new Employee();
        courier.setFirstName(first);
        courier.setLastName(last);
        courier.setLogin(login);
        courier.setPassword("admin");
        courier.setPesel(pesel);
        courier.setRole(role);
        courier.setRegions(regions);
        employeeRepository.save(courier);
    }

    private Address createAddress(pl.polsl.tab.kurier.model.Region region, String street, String num, String zip) {
        Address addr = new Address();
        addr.setRegion(region);
        addr.setStreet(street);
        addr.setBuildingNumber(num);
        addr.setPostalCode(zip);
        return addressRepository.save(addr);
    }

    private void createParcel(String phone, BigDecimal weight, DeliveryMode mode, Address sender, Address recipient, pl.polsl.tab.kurier.model.Region curr, pl.polsl.tab.kurier.model.Region next, Status status) {
        Parcel p = new Parcel();
        p.setPhoneNumber(phone);
        p.setWeight(weight);
        
        int routeLength = routeService.findRouteLength(curr.getRegionId(), recipient.getRegion().getRegionId());
        if (routeLength == -1) routeLength = 1;
        boolean isExpress = mode.getName().equalsIgnoreCase("EXPRESS");
        double daysToDeliver = (routeLength + 1) * (isExpress ? 1.0 : 1.5);
        p.setExpectedTime(LocalDateTime.now().plusHours((long)(daysToDeliver * 24)));
        
        p.setDeliveryMode(mode);
        p.setSenderAddress(sender);
        p.setDestinationAddress(recipient);
        p.setCurrentRegion(curr);
        p.setNextRegion(next);
        p.setStatus(status);
        p.setVerified(false); // Force verification flow
        p.setFragility("no");
        p.setPrice(BigDecimal.valueOf(20.0));
        parcelRepository.save(p);
    }
}
