package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.DeliveryMode;

@Repository
public interface DeliveryModeRepository extends JpaRepository<DeliveryMode, Integer> {
}
