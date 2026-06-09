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
                ParcelStatus.REGISTERED, // New: pickup from sender
                ParcelStatus.PENDING_PICKUP, // Ready after worker acceptance at hub
                ParcelStatus.IN_TRANSIT,
                ParcelStatus.OUT_FOR_DELIVERY
        );

        // Fetch parcels where nextRegion is one of the courier's assigned regions
        List<Parcel> potentialParcels = parcelRepository.findByNextRegionIdsAndStatusNames(regionIds, actionableStatuses);
        
        List<Parcel> filtered = potentialParcels.stream().filter(p -> {
            Integer currId = p.getCurrentRegion().getRegionId();
            Integer nextId = p.getNextRegion().getRegionId();
            
            if (regionIds.size() == 1) {
                // Local Courier: handles only parcels within their single region (Sender->Hub or Hub->Recipient)
                // This matches when currentRegion == nextRegion
                return currId.equals(nextId) && regionIds.contains(currId);
            } else {
                // Linehaul Courier: handles only parcels moving between two of their assigned regions (Hub->Hub)
                // This matches when currentRegion != nextRegion AND both are in their region set
                return !currId.equals(nextId) && regionIds.contains(currId) && regionIds.contains(nextId);
            }
        }).collect(Collectors.toList());

        return filtered.stream()
                .sorted((p1, p2) -> {
                    String s1 = p1.getStatus().getName();
                    String s2 = p2.getStatus().getName();
                    boolean inPoss1 = s1.equals(ParcelStatus.IN_TRANSIT) || s1.equals(ParcelStatus.OUT_FOR_DELIVERY);
                    boolean inPoss2 = s2.equals(ParcelStatus.IN_TRANSIT) || s2.equals(ParcelStatus.OUT_FOR_DELIVERY);
                    
                    if (inPoss1 && !inPoss2) return -1;
                    if (!inPoss1 && inPoss2) return 1;

                    boolean isExp1 = p1.getDeliveryMode().getName().equalsIgnoreCase("EXPRESS");
                    boolean isExp2 = p2.getDeliveryMode().getName().equalsIgnoreCase("EXPRESS");
                    if (isExp1 && !isExp2) return -1;
                    if (!isExp1 && isExp2) return 1;

                    // Secondary sort by expected time (soonest first)
                    return p1.getExpectedTime().compareTo(p2.getExpectedTime());
                })
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
            // Business Rule: UNDELIVERED can only be set if OUT_FOR_DELIVERY
            if (ParcelStatus.UNDELIVERED.equals(newStatusName) && !ParcelStatus.OUT_FOR_DELIVERY.equals(parcel.getStatus().getName())) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Cannot report client absent: package must be out for delivery.");
            }

            // Business Rule: LOST or DAMAGED can only be set if already picked up or at hub
            if ((ParcelStatus.LOST.equals(newStatusName) || ParcelStatus.DAMAGED.equals(newStatusName)) 
                && (ParcelStatus.REGISTERED.equals(parcel.getStatus().getName()) || ParcelStatus.PENDING_PICKUP.equals(parcel.getStatus().getName()))) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Cannot report issue: package not yet picked up by any courier.");
            }

            // Comment validation: avoid "random signs" or too short
            if (comment == null || comment.trim().length() < 10) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Comment is too short. Please provide at least 10 meaningful characters.");
            }
            if (!comment.matches(".*[a-zA-Z0-9].*")) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Comment must contain alphanumeric characters.");
            }

            Status status = resolveOrCreateStatus(newStatusName);
            parcel.setStatus(status);
            
            if (ParcelStatus.DELIVERED.equals(newStatusName)) {
                parcel.setExpectedTime(LocalDateTime.now());
            }

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
            // If from sender (REGISTERED), always go to Hub first via IN_TRANSIT
            if (ParcelStatus.REGISTERED.equals(parcel.getStatus().getName())) {
                newStatus = resolveOrCreateStatus(ParcelStatus.IN_TRANSIT);
            } 
            // If from Hub (PENDING_PICKUP), decide if local delivery or next hub
            else if (currentRegion.getRegionId().equals(destinationRegion.getRegionId())) {
                newStatus = resolveOrCreateStatus(ParcelStatus.OUT_FOR_DELIVERY);
            } else {
                newStatus = resolveOrCreateStatus(ParcelStatus.IN_TRANSIT);
            }

            parcel.setStatus(newStatus);
            if (ParcelStatus.DELIVERED.equals(newStatus.getName())) {
                parcel.setExpectedTime(LocalDateTime.now());
            }
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
            // Only unverify if this is the very first intake (from sender)
            // We know it's the first intake if currentRegion was the same as sender city during the trip
            Status newStatus;
            if (parcel.getSenderAddress().getRegion().getRegionId().equals(arrivedAtHub.getRegionId()) 
                && (parcel.getVerified() == null || !parcel.getVerified())) {
                parcel.setVerified(false);
                newStatus = resolveOrCreateStatus(ParcelStatus.AT_HUB);
            } else {
                // If it was already verified at a previous hub, it stays verified and ready for pickup
                parcel.setVerified(true);
                newStatus = resolveOrCreateStatus(ParcelStatus.PENDING_PICKUP);
            }

            parcel.setStatus(newStatus);

            if (!arrivedAtHub.getRegionId().equals(destinationRegion.getRegionId())) {
                // Arrived at intermediate hub — compute next hop
                Integer newNextRegionId = routeService.findNextRegionId(
                        arrivedAtHub.getRegionId(), destinationRegion.getRegionId()
                ).orElseThrow(() -> new pl.polsl.tab.kurier.exception.ResourceBusyException(
                        "Route broken: no path from hub " + arrivedAtHub.getName()
                        + " to destination " + destinationRegion.getName()
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
            if (parcel.getVerified() != null && parcel.getVerified()) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Parcel is already verified.");
            }
            if (!verified) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Parcels cannot be unverified.");
            }
            
            parcel.setVerified(true);
            parcel.setStatus(resolveOrCreateStatus(ParcelStatus.PENDING_PICKUP));
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
