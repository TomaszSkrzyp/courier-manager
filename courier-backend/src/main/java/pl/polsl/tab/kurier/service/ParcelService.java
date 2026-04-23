package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.ParcelDTO;
import pl.polsl.tab.kurier.repository.ParcelRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.polsl.tab.kurier.model.DeliveryUpdate;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Parcel;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.model.Status;
import pl.polsl.tab.kurier.repository.DeliveryUpdateRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.StatusRepository;

@Service
public class ParcelService {

    @Autowired
    private ParcelRepository parcelRepository;

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

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private DeliveryUpdateRepository deliveryUpdateRepository;

    public List<ParcelDTO> getParcelsForCourier(Integer courierId) {
        // For demonstration, we just fetch all parcels and return them.
        // In a real scenario, this would filter by the courier's assigned regions
        // and parcel's nextRegion or status.
        return getAllParcels();
    }

    public Optional<ParcelDTO> updateParcelStatus(Integer id, String newStatusName, String comment, Integer employeeId) {
        return parcelRepository.findById(id).map(parcel -> {
            Status status = statusRepository.findByName(newStatusName)
                    .orElseGet(() -> {
                        Status newStatus = new Status();
                        newStatus.setName(newStatusName);
                        return statusRepository.save(newStatus);
                    });
            parcel.setStatus(status);
            parcelRepository.save(parcel);

            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            
            if (employee != null) {
                DeliveryUpdate update = new DeliveryUpdate();
                update.setParcel(parcel);
                update.setEmployee(employee);
                update.setStatus(status);
                update.setTimeStamp(LocalDateTime.now());
                update.setComment(comment);
                
                // Set region to the parcel's next region or destination region
                if (parcel.getNextRegion() != null) {
                    update.setRegion(parcel.getNextRegion());
                } else if (parcel.getDestinationAddress() != null) {
                    update.setRegion(parcel.getDestinationAddress().getRegion());
                }
                
                if (update.getRegion() != null) {
                    deliveryUpdateRepository.save(update);
                }
            }

            return ParcelDTO.fromEntity(parcel);
        });
    }

    public Optional<ParcelDTO> verifyParcel(Integer id, boolean verified) {
        return parcelRepository.findById(id).map(parcel -> {
            parcel.setVerified(verified);
            return ParcelDTO.fromEntity(parcelRepository.save(parcel));
        });
    }
}
