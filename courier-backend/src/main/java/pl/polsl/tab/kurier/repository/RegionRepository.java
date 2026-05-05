package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.Region;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Integer> {
    Optional<Region> findByName(String name);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r FROM Region r JOIN r.employeeSet e JOIN e.role role WHERE role.name = 'COURIER'")
    List<Region> findRegionsWithCouriers();
}

