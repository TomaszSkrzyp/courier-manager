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
import pl.polsl.tab.kurier.model.PriceDelta;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.AddressRepository;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;
import pl.polsl.tab.kurier.repository.DeliveryUpdateRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.RegionRepository;
import pl.polsl.tab.kurier.repository.StatusRepository;
import pl.polsl.tab.kurier.repository.PriceDeltaRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParcelService {

    @Autowired private ParcelRepository parcelRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private StatusRepository statusRepository;
    @Autowired private DeliveryUpdateRepository deliveryUpdateRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private DeliveryModeRepository deliveryModeRepository;

    @Autowired
    private PriceDeltaRepository priceDeltaRepository;
    @Autowired private RouteService routeService;

    // -------------------------------------------------------------------------
    // Read
    // -------------------------------------------------------------------------

    public List<ParcelDTO> getAllParcels() {
        return parcelRepository.findAll().stream()
                .map(ParcelDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public org.springframework.data.domain.Page<ParcelDTO> getParcelsPage(org.springframework.data.domain.Pageable pageable) {
        return parcelRepository.findAll(pageable).map(ParcelDTO::fromEntity);
    }

    public org.springframework.data.domain.Page<ParcelDTO> searchParcels(String search, String status, org.springframework.data.domain.Pageable pageable) {
        String searchTerm = (search == null || search.trim().isEmpty()) ? null : "%" + search.trim().toLowerCase() + "%";
        String statusFilter = (status == null || status.equalsIgnoreCase("All")) ? "All" : status;
        return parcelRepository.searchParcels(searchTerm, statusFilter, pageable).map(ParcelDTO::fromEntity);
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
        // Initially, the parcel is in the sender's region and its first goal is the local hub.
        parcel.setCurrentRegion(senderRegion);
        parcel.setNextRegion(senderRegion);
        parcel.setDeliveryMode(deliveryMode);
        parcel.setStatus(resolveOrCreateStatus(ParcelStatus.REGISTERED));
        parcel.setVerified(false);
        parcel.setComment(dto.getComment());
        parcel.setFragility(dto.getFragility());
        if (dto.getWeight() != null) parcel.setWeight(BigDecimal.valueOf(dto.getWeight()));
        if (dto.getHeight() != null) parcel.setHeight(BigDecimal.valueOf(dto.getHeight()));
        if (dto.getWidth()  != null) parcel.setWidth(BigDecimal.valueOf(dto.getWidth()));
        if (dto.getLength() != null) parcel.setLength(BigDecimal.valueOf(dto.getLength()));

        int routeLength = routeService.findRouteLength(senderRegion.getRegionId(), recipientRegion.getRegionId());
        if (routeLength == -1) routeLength = 1; // Fallback if graph is not fully connected yet

        boolean isExpress = deliveryMode.getName().equalsIgnoreCase("EXPRESS");
        double daysToDeliver = (routeLength + 1) * (isExpress ? 1.0 : 1.5);
        parcel.setExpectedTime(LocalDateTime.now().plusHours((long)(daysToDeliver * 24)));

        // Calculate price using PriceDelta
        PriceDelta delta = priceDeltaRepository.findFirstByOrderByCreatedAtDesc()
                .orElseGet(() -> {
                    PriceDelta pd = new PriceDelta();
                    pd.setWeightDelta(BigDecimal.valueOf(2.5));
                    pd.setLengthDelta(BigDecimal.valueOf(0.1));
                    pd.setWidthDelta(BigDecimal.valueOf(0.1));
                    pd.setHeightDelta(BigDecimal.valueOf(0.1));
                    pd.setNormalModeDelta(BigDecimal.valueOf(10.0));
                    pd.setExpressModeDelta(BigDecimal.valueOf(25.0));
                    return priceDeltaRepository.save(pd);
                });

        BigDecimal price = BigDecimal.ZERO;
        if (dto.getWeight() != null) price = price.add(BigDecimal.valueOf(dto.getWeight()).multiply(delta.getWeightDelta()));
        if (dto.getHeight() != null) price = price.add(BigDecimal.valueOf(dto.getHeight()).multiply(delta.getHeightDelta()));
        if (dto.getWidth() != null) price = price.add(BigDecimal.valueOf(dto.getWidth()).multiply(delta.getWidthDelta()));
        if (dto.getLength() != null) price = price.add(BigDecimal.valueOf(dto.getLength()).multiply(delta.getLengthDelta()));
        
        if (isExpress) {
            price = price.add(delta.getExpressModeDelta());
        } else {
            price = price.add(delta.getNormalModeDelta());
        }
        parcel.setPrice(price);

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
     * Called when a courier picks up a parcel from sender or a hub.
     */
    public Optional<ParcelDTO> pickupParcel(Integer id, Integer employeeId) {
        return parcelRepository.findById(id).map(parcel -> {
            Region currentRegion = parcel.getCurrentRegion();
            Region destinationRegion = parcel.getDestinationAddress().getRegion();

            Status newStatus;
            // It becomes OUT_FOR_DELIVERY only if it's being picked up
            // FROM its final destination hub or directly from sender in the same region.
            if (currentRegion.getRegionId().equals(destinationRegion.getRegionId())) {
                newStatus = resolveOrCreateStatus(ParcelStatus.OUT_FOR_DELIVERY);
            } else {
                newStatus = resolveOrCreateStatus(ParcelStatus.IN_TRANSIT);
            }

            parcel.setStatus(newStatus);
            parcelRepository.save(parcel);
            saveDeliveryUpdate(parcel, employeeId, newStatus, "Package picked up by courier");
            return ParcelDTO.fromEntity(parcel);
        });
    }

    /**
     * Called when a courier confirms delivery of the parcel to the
     * current nextRegion.
     */
    public Optional<ParcelDTO> advanceParcel(Integer id, Integer employeeId) {
        return parcelRepository.findById(id).map(parcel -> {
            Region arrivedAtHub = parcel.getNextRegion();
            Region destinationRegion = parcel.getDestinationAddress().getRegion();

            // The parcel is now physically at the hub it was traveling to
            parcel.setCurrentRegion(arrivedAtHub);
            
            Status newStatus = resolveOrCreateStatus(ParcelStatus.AT_HUB);

            if (!arrivedAtHub.getRegionId().equals(destinationRegion.getRegionId())) {
                // Arrived at intermediate hub — compute next hop
                Integer newNextRegionId = routeService.findNextRegionId(
                        arrivedAtHub.getRegionId(), destinationRegion.getRegionId()
                ).orElseThrow(() -> new RuntimeException(
                        "Route broken: no path from hub " + arrivedAtHub.getRegionId() + " (" + arrivedAtHub.getName() + ")"
                        + " to destination " + destinationRegion.getRegionId() + " (" + destinationRegion.getName() + ")"
                ));

                Region newNextRegion = regionRepository.findById(newNextRegionId)
                        .orElseThrow(() -> new RuntimeException("Next region not found: " + newNextRegionId));

                parcel.setNextRegion(newNextRegion);
            }
            // If it's already at destination hub, we keep nextRegion as destinationRegion

            parcel.setStatus(newStatus);
            parcelRepository.save(parcel);
            saveDeliveryUpdate(parcel, employeeId, newStatus, "Package arrived at hub: " + arrivedAtHub.getName());
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
