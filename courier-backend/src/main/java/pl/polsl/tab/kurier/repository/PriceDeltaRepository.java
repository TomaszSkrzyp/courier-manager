package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.PriceDelta;

@Repository
public interface PriceDeltaRepository extends JpaRepository<PriceDelta, Integer> {
}
