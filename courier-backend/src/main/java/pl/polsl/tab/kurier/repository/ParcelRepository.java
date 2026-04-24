package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.Parcel;
import pl.polsl.tab.kurier.dto.RegionStatsDTO;
import pl.polsl.tab.kurier.dto.DeliveryModeStatsDTO;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Integer> {
    Optional<Parcel> findByTrackingNumber(String trackingNumber);

    /**
     * Returns parcels whose nextRegion is in the courier's region set
     * and whose status indicates they are ready for courier action:
     *   PENDING_PICKUP — kurier odbiera paczkę z hub-u nadawcy
     *   AT_HUB         — paczka czeka w pośrednim hub-ie na dalszy transport
     *   OUT_FOR_DELIVERY — kurier jedzie do odbiorcy
     *   IN_TRANSIT     — kurier już ją ma (własne paczki widoczne na liście)
     */
    @Query("SELECT p FROM Parcel p " +
           "WHERE p.nextRegion.regionId IN :regionIds " +
           "AND p.status.name IN :statusNames")
    List<Parcel> findByNextRegionIdsAndStatusNames(
            @Param("regionIds") Set<Integer> regionIds,
            @Param("statusNames") List<String> statusNames);

    @Query("SELECT new pl.polsl.tab.kurier.dto.RegionStatsDTO(r.name, COUNT(p)) " +
           "FROM Parcel p JOIN p.destinationAddress a JOIN a.region r " +
           "GROUP BY r.name ORDER BY COUNT(p) DESC")
    List<RegionStatsDTO> countParcelsByDestinationRegion();

    @Query("SELECT new pl.polsl.tab.kurier.dto.DeliveryModeStatsDTO(m.name, COUNT(p)) " +
           "FROM Parcel p JOIN p.deliveryMode m " +
           "GROUP BY m.name ORDER BY COUNT(p) DESC")
    List<DeliveryModeStatsDTO> countParcelsByDeliveryMode();
}