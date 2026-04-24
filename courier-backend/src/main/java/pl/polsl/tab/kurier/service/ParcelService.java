package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.ParcelDTO;
import pl.polsl.tab.kurier.dto.ParcelCreateDTO;
import pl.polsl.tab.kurier.repository.ParcelRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pl.polsl.tab.kurier.model.Address;
import pl.polsl.tab.kurier.model.DeliveryMode;
import pl.polsl.tab.kurier.model.DeliveryUpdate;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Parcel;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.AddressRepository;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;
import pl.polsl.tab.kurier.repository.DeliveryUpdateRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.RegionRepository;
import pl.polsl.tab.kurier.repository.StatusRepository;

@Service
public class ParcelService {

    @Autowired private ParcelRepository parcelRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private StatusRepository statusRepository;
    @Autowired private DeliveryUpdateRepository deliveryUpdateRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private RegionRepository regionRepository;
    @Autowired private DeliveryModeRepository deliveryModeRepository;
    @Autowired private RouteService routeService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public List<ParcelDTO> getAllParcels() {
        return parcelRepository.findAll().stream()
                .map(ParcelDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<ParcelDTO> getParcelById(Integer id) {
        return parcelRepository.findById(id).map(ParcelDTO::fromEntity);
    }

    public Optional<ParcelDTO> getParcelByTrackingNumber(String trackingNumber) {
        return parcelRepository.findByTrackingNumber(trackingNumber).map(ParcelDTO::fromEntity);
    }

    /**
     * Returns parcels visible to a specific courier.
     *
     * A courier can see parcels whose nextRegion is one of their assigned regions
     * AND whose status indicates the parcel is actionable by a courier:
     *   - PENDING_PICKUP   → waiting at the sender's hub to be picked up
     *   - AT_HUB           → waiting at an intermediate hub for the next leg
     *   - IN_TRANSIT       → the courier already has it (their own in-progress deliveries)
     *   - OUT_FOR_DELIVERY → heading to the final recipient
     */
    public List<ParcelDTO> getParcelsForCourier(Integer courierId) {
        Employee courier = employeeRepository.findById(courierId)
                .orElseThrow(() -> new RuntimeException("Courier not found: " + courierId));

        if (courier.getRegions() == null || courier.getRegions().isEmpty()) {
            return List.of();
        }

        Set<Integer> regionIds = courier.getRegions().stream()
                .map(Region::getRegionId)
                .collect(Collectors.toSet());

        List<String> actionableStatuses = List.of(
                ParcelStatus.PENDING_PICKUP,
                ParcelStatus.AT_HUB,
                ParcelStatus.IN_TRANSIT,
                ParcelStatus.OUT_FOR_DELIVERY
        );

        return parcelRepository.findByNextRegionIdsAndStatusNames(regionIds, actionableStatuses)
                .stream()
                .map(ParcelDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    /**
     * Creates a new parcel. Uses RouteService to compute the first hop
     * from the sender's region toward the destination region.
     *
     * Status flow starts at REGISTERED (verified=false, Worker must verify before pickup).
     */
    public ParcelDTO createParcel(ParcelCreateDTO dto) {
        Region senderRegion = regionRepository.findById(dto.getSenderRegionId())
                .orElseThrow(() -> new RuntimeException("Sender region not found: " + dto.getSenderRegionId()));
        Region recipientRegion = regionRepository.findById(dto.getRecipientRegionId())
                .orElseThrow(() -> new RuntimeException("Recipient region not found: " + dto.getRecipientRegionId()));

        // Compute routing: first hop toward destination
        Integer nextRegionId = routeService.findNextRegionId(
                senderRegion.getRegionId(), recipientRegion.getRegionId()
        ).orElseThrow(() -> new RuntimeException(
                "No courier route from region " + senderRegion.getRegionId()
                + " to region " + recipientRegion.getRegionId()
                + ". Please ensure couriers connect these regions."
        ));

        Region nextRegion = regionRepository.findById(nextRegionId)
                .orElseThrow(() -> new RuntimeException("Next region not found: " + nextRegionId));

        Address senderAddress = buildAndSaveAddress(senderRegion, dto.getSenderStreet(),
                dto.getSenderBuildingNumber(), dto.getSenderPostalCode());
        Address recipientAddress = buildAndSaveAddress(recipientRegion, dto.getRecipientStreet(),
                dto.getRecipientBuildingNumber(), dto.getRecipientPostalCode());

        DeliveryMode deliveryMode = deliveryModeRepository.findById(dto.getDeliveryModeId())
                .orElseThrow(() -> new RuntimeException("Delivery mode not found: " + dto.getDeliveryModeId()));

        Parcel parcel = new Parcel();
        parcel.setPhoneNumber(dto.getPhoneNumber());
        parcel.setSenderAddress(senderAddress);
        parcel.setDestinationAddress(recipientAddress);
        parcel.setNextRegion(nextRegion);
        parcel.setDeliveryMode(deliveryMode);
        parcel.setStatus(resolveOrCreateStatus(ParcelStatus.REGISTERED));
        parcel.setVerified(false);
        parcel.setComment(dto.getComment());
        parcel.setFragility(dto.getFragility());
        if (dto.getWeight() != null) parcel.setWeight(BigDecimal.valueOf(dto.getWeight()));
        if (dto.getHeight() != null) parcel.setHeight(BigDecimal.valueOf(dto.getHeight()));
        if (dto.getWidth()  != null) parcel.setWidth(BigDecimal.valueOf(dto.getWidth()));
        if (dto.getLength() != null) parcel.setLength(BigDecimal.valueOf(dto.getLength()));

        boolean isExpress = deliveryMode.getName().toUpperCase().contains("EXPRESS");
        parcel.setExpectedTime(LocalDateTime.now().plusDays(isExpress ? 1 : 3));

        return ParcelDTO.fromEntity(parcelRepository.save(parcel));
    }

    // -------------------------------------------------------------------------
    // Status updates
    // -------------------------------------------------------------------------

    /**
     * General-purpose status update used by Workers for manual overrides,
     * e.g. marking LOST, DAMAGED, UNDELIVERED.
     */
    public Optional<ParcelDTO> updateParcelStatus(Integer id, String newStatusName, String comment, Integer employeeId) {
        return parcelRepository.findById(id).map(parcel -> {
            Status status = resolveOrCreateStatus(newStatusName);
            parcel.setStatus(status);
            parcelRepository.save(parcel);
            saveDeliveryUpdate(parcel, employeeId, status, comment);
            return ParcelDTO.fromEntity(parcel);
        });
    }

    /**
     * Called when a courier confirms delivery to the current nextRegion.
     *
     * Logic:
     *   - If nextRegion == destinationRegion → DELIVERED (terminal)
     *   - Otherwise → AT_HUB (or OUT_FOR_DELIVERY if one hop left),
     *     nextRegion updated to the next step via BFS
     */
    public Optional<ParcelDTO> advanceParcel(Integer id, Integer employeeId) {
        return parcelRepository.findById(id).map(parcel -> {
            Region currentNextRegion = parcel.getNextRegion();
            Region destinationRegion = parcel.getDestinationAddress().getRegion();

            Status newStatus;

            if (currentNextRegion.getRegionId().equals(destinationRegion.getRegionId())) {
                // Arrived at final destination
                newStatus = resolveOrCreateStatus(ParcelStatus.DELIVERED);
                parcel.setStatus(newStatus);
            } else {
                // Arrived at intermediate hub — compute next hop
                Integer newNextRegionId = routeService.findNextRegionId(
                        currentNextRegion.getRegionId(), destinationRegion.getRegionId()
                ).orElseThrow(() -> new RuntimeException(
                        "Route broken: no path from hub " + currentNextRegion.getRegionId()
                        + " to destination " + destinationRegion.getRegionId()
                ));

                Region newNextRegion = regionRepository.findById(newNextRegionId)
                        .orElseThrow(() -> new RuntimeException("Next region not found: " + newNextRegionId));

                // Last mile if the new nextRegion is already the destination
                boolean isLastMile = newNextRegionId.equals(destinationRegion.getRegionId());
                newStatus = resolveOrCreateStatus(
                        isLastMile ? ParcelStatus.OUT_FOR_DELIVERY : ParcelStatus.AT_HUB
                );

                parcel.setNextRegion(newNextRegion);
                parcel.setStatus(newStatus);
            }

            parcelRepository.save(parcel);
            saveDeliveryUpdate(parcel, employeeId, newStatus, null);
            return ParcelDTO.fromEntity(parcel);
        });
    }

    /**
     * Worker verifies the parcel. Upon successful verification status becomes
     * PENDING_PICKUP so couriers in the sender's region can pick it up.
     */
    public Optional<ParcelDTO> verifyParcel(Integer id, boolean verified) {
        return parcelRepository.findById(id).map(parcel -> {
            parcel.setVerified(verified);
            if (verified) {
                parcel.setStatus(resolveOrCreateStatus(ParcelStatus.PENDING_PICKUP));
            }
            return ParcelDTO.fromEntity(parcelRepository.save(parcel));
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Status resolveOrCreateStatus(String name) {
        return statusRepository.findByName(name).orElseGet(() -> {
            Status s = new Status();
            s.setName(name);
            return statusRepository.save(s);
        });
    }

    private Address buildAndSaveAddress(Region region, String street, String buildingNo, String postalCode) {
        Address address = new Address();
        address.setRegion(region);
        address.setStreet(street);
        address.setBuildingNumber(buildingNo);
        address.setPostalCode(postalCode);
        return addressRepository.save(address);
    }

    private void saveDeliveryUpdate(Parcel parcel, Integer employeeId, Status status, String comment) {
        if (employeeId == null) return;
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) return;

        DeliveryUpdate update = new DeliveryUpdate();
        update.setParcel(parcel);
        update.setEmployee(employee);
        update.setStatus(status);
        update.setTimeStamp(LocalDateTime.now());
        update.setComment(comment);

        if (parcel.getNextRegion() != null) {
            update.setRegion(parcel.getNextRegion());
        } else if (parcel.getDestinationAddress() != null) {
            update.setRegion(parcel.getDestinationAddress().getRegion());
        }

        if (update.getRegion() != null) {
            deliveryUpdateRepository.save(update);
        }
    }
}
