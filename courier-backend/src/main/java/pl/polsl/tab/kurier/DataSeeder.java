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

    public DataSeeder(RoleRepository roleRepository,
                      StatusRepository statusRepository,
                      DeliveryModeRepository deliveryModeRepository,
                      PriceDeltaRepository priceDeltaRepository,
                      EmployeeRepository employeeRepository,
                      RegionRepository regionRepository,
                      ParcelRepository parcelRepository,
                      AddressRepository addressRepository) {
        this.roleRepository = roleRepository;
        this.statusRepository = statusRepository;
        this.deliveryModeRepository = deliveryModeRepository;
        this.priceDeltaRepository = priceDeltaRepository;
        this.employeeRepository = employeeRepository;
        this.regionRepository = regionRepository;
        this.parcelRepository = parcelRepository;
        this.addressRepository = addressRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedStatuses();
        seedRegions();
        seedDeliveryModes();
        seedPriceDelta();
        seedAdminUser();
        seedCourierAndParcel();
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
        if (employeeRepository.findByLogin("courier1").isEmpty()) {
            Role courierRole = roleRepository.findByName("COURIER")
                    .orElseThrow(() -> new RuntimeException("COURIER role not found"));

            pl.polsl.tab.kurier.model.Region katowice = regionRepository.findByName("KATOWICE")
                    .orElseThrow(() -> new RuntimeException("KATOWICE region not found"));
            pl.polsl.tab.kurier.model.Region radom = regionRepository.findByName("RADOM")
                    .orElseThrow(() -> new RuntimeException("RADOM region not found"));
            pl.polsl.tab.kurier.model.Region warszawa = regionRepository.findByName("WARSZAWA")
                    .orElseThrow(() -> new RuntimeException("WARSZAWA region not found"));

            Employee courier = new Employee();
            courier.setFirstName("John");
            courier.setLastName("Doe");
            courier.setLogin("courier1");
            courier.setPassword("admin");
            courier.setPesel("12345678901");
            courier.setRole(courierRole);
            courier.setRegions(Set.of(katowice, radom));
            employeeRepository.save(courier);

            // Add a second courier to bridge the gap between RADOM and WARSZAWA
            Employee courier2 = new Employee();
            courier2.setFirstName("Jane");
            courier2.setLastName("Smith");
            courier2.setLogin("courier2");
            courier2.setPassword("admin");
            courier2.setPesel("09876543210");
            courier2.setRole(courierRole);
            courier2.setRegions(Set.of(radom, warszawa));
            employeeRepository.save(courier2);

            if (parcelRepository.count() == 0) {
                DeliveryMode normal = deliveryModeRepository.findByName("NORMAL").get();
                Status pendingPickup = statusRepository.findByName(ParcelStatus.PENDING_PICKUP).get();

                Address senderAddr = new Address();
                senderAddr.setRegion(katowice);
                senderAddr.setStreet("Korfantego");
                senderAddr.setBuildingNumber("10");
                senderAddr.setPostalCode("40-001");
                addressRepository.save(senderAddr);

                Address recipientAddr = new Address();
                recipientAddr.setRegion(warszawa);
                recipientAddr.setStreet("Marszałkowska");
                recipientAddr.setBuildingNumber("50");
                recipientAddr.setPostalCode("00-001");
                addressRepository.save(recipientAddr);

                Parcel parcel = new Parcel();
                parcel.setPhoneNumber("555 123 456");
                parcel.setWeight(BigDecimal.valueOf(5.0));
                parcel.setExpectedTime(LocalDateTime.now().plusDays(2));
                parcel.setDeliveryMode(normal);
                parcel.setSenderAddress(senderAddr);
                parcel.setDestinationAddress(recipientAddr);
                parcel.setCurrentRegion(katowice);
                parcel.setNextRegion(katowice); // Parcel is in Katowice, waiting for pickup
                parcel.setStatus(pendingPickup);
                parcel.setVerified(true);
                parcel.setFragility("no");
                parcel.setLength(BigDecimal.valueOf(30.0));
                parcel.setWidth(BigDecimal.valueOf(20.0));
                parcel.setHeight(BigDecimal.valueOf(15.0));
                parcel.setPrice(BigDecimal.valueOf(25.0));
                parcelRepository.save(parcel);

                // Add a second parcel that only needs one courier (KATOWICE to RADOM)
                Address senderAddr2 = new Address();
                senderAddr2.setRegion(katowice);
                senderAddr2.setStreet("Chorzowska");
                senderAddr2.setBuildingNumber("5");
                senderAddr2.setPostalCode("40-101");
                addressRepository.save(senderAddr2);

                Address recipientAddr2 = new Address();
                recipientAddr2.setRegion(radom);
                recipientAddr2.setStreet("Żeromskiego");
                recipientAddr2.setBuildingNumber("12");
                recipientAddr2.setPostalCode("26-600");
                addressRepository.save(recipientAddr2);

                Parcel parcel2 = new Parcel();
                parcel2.setPhoneNumber("555 987 654");
                parcel2.setWeight(BigDecimal.valueOf(2.0));
                parcel2.setLength(BigDecimal.valueOf(10.0));
                parcel2.setWidth(BigDecimal.valueOf(10.0));
                parcel2.setHeight(BigDecimal.valueOf(5.0));
                parcel2.setExpectedTime(LocalDateTime.now().plusDays(1));
                parcel2.setDeliveryMode(normal);
                parcel2.setSenderAddress(senderAddr2);
                parcel2.setDestinationAddress(recipientAddr2);
                parcel2.setCurrentRegion(katowice);
                parcel2.setNextRegion(katowice);
                parcel2.setStatus(pendingPickup);
                parcel2.setVerified(true);
                parcel2.setFragility("no");
                parcel2.setPrice(BigDecimal.valueOf(15.0));
                parcelRepository.save(parcel2);
            }
        }
    }
}
