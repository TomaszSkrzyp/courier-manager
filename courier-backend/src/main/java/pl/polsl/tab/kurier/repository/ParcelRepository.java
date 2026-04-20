
package pl.polsl.tab.kurier.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.polsl.tab.kurier.model.Parcel;

public interface ParcelRepository extends JpaRepository<Parcel, Long> {
    Optional<Parcel> findByTrackingNumber(String trackingNumber);
}