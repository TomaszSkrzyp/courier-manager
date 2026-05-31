package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.DeliveryUpdate;
import pl.polsl.tab.kurier.dto.CourierStatsDTO;
import pl.polsl.tab.kurier.dto.RegionStatsDTO;

import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DeliveryUpdateRepository extends JpaRepository<DeliveryUpdate, Integer> {

    @Query("SELECT new pl.polsl.tab.kurier.dto.CourierStatsDTO(e.firstName, e.lastName, COUNT(DISTINCT du.parcel.parcelId)) " +
           "FROM DeliveryUpdate du JOIN du.employee e JOIN du.status s " +
           "WHERE s.name = 'DELIVERED' AND du.timeStamp BETWEEN :startDate AND :endDate " +
           "AND (:regionId IS NULL OR du.region.regionId = :regionId) " +
           "GROUP BY e.employeeId, e.firstName, e.lastName ORDER BY COUNT(DISTINCT du.parcel.parcelId) DESC")
    List<CourierStatsDTO> countDeliveredParcelsByCourier(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("regionId") Integer regionId);

    @Query("SELECT new pl.polsl.tab.kurier.dto.RegionStatsDTO(r.name, COUNT(DISTINCT du.parcel.parcelId)) " +
           "FROM DeliveryUpdate du JOIN du.region r JOIN du.status s " +
           "WHERE s.name = 'DELIVERED' AND du.timeStamp BETWEEN :startDate AND :endDate " +
           "AND (:regionId IS NULL OR r.regionId = :regionId) " +
           "GROUP BY r.name ORDER BY COUNT(DISTINCT du.parcel.parcelId) DESC")
    List<RegionStatsDTO> countDeliveredParcelsByRegion(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("regionId") Integer regionId);
}
