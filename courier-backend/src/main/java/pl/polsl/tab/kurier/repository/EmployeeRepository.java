package pl.polsl.tab.kurier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.polsl.tab.kurier.model.Employee;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    /** Fetches all couriers with their regions eagerly loaded (needed by RouteService). */
    @Query("SELECT DISTINCT e FROM Employee e JOIN FETCH e.regions WHERE e.role.name = 'COURIER'")
    List<Employee> findCouriersWithRegions();

    /** Fetches all employees with the given role name. */
    List<Employee> findByRole_Name(String roleName);
}
