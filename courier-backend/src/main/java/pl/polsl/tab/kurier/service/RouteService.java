package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.repository.EmployeeRepository;

import java.util.*;

/**
 * Builds a region connectivity graph from courier assignments and uses BFS
 * to find the optimal next hop for parcel routing.
 *
 * Graph construction:
 *   - Each courier assigned to ≥2 regions represents a bidirectional edge between those regions.
 *   - Couriers assigned to exactly 1 region are "local" and can do last-mile delivery only.
 *
 * Example:
 *   Courier A: [Region1, Region2] → edge Region1 ↔ Region2
 *   Courier B: [Region2, Region3] → edge Region2 ↔ Region3
 *   BFS(Region1 → Region3) = [Region1, Region2, Region3], next hop = Region2
 */
@Service
public class RouteService {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Builds an adjacency map of region IDs from all courier region assignments.
     * Only couriers (role name "COURIER") with ≥2 regions contribute edges.
     *
     * @return Map of regionId → set of directly reachable regionIds
     */
    public Map<Integer, Set<Integer>> buildGraph() {
        Map<Integer, Set<Integer>> graph = new HashMap<>();

        List<Employee> couriers = employeeRepository.findCouriersWithRegions();
        System.out.println("Building graph. Found couriers: " + couriers.size());

        for (Employee courier : couriers) {
            if (courier.getRegions() == null || courier.getRegions().size() < 2) {
                continue;
            }

            List<Integer> regionIds = courier.getRegions().stream()
                    .map(r -> r.getRegionId())
                    .toList();
            
            System.out.println("Courier " + courier.getLogin() + " connects regions: " + regionIds);

            for (int i = 0; i < regionIds.size(); i++) {
                for (int j = i + 1; j < regionIds.size(); j++) {
                    int a = regionIds.get(i);
                    int b = regionIds.get(j);
                    graph.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    graph.computeIfAbsent(b, k -> new HashSet<>()).add(a);
                }
            }
        }
        System.out.println("Graph build complete. Nodes: " + graph.keySet());
        return graph;
    }

    /**
     * Finds the next region ID a parcel should travel to, given the current region
     * and the destination region, using BFS on the courier-derived graph.
     *
     * @param currentRegionId     the region where the parcel currently is
     * @param destinationRegionId the final destination region
     * @return Optional containing the next region ID, or empty if no route exists
     */
    public Optional<Integer> findNextRegionId(Integer currentRegionId, Integer destinationRegionId) {
        if (currentRegionId.equals(destinationRegionId)) {
            return Optional.of(destinationRegionId);
        }

        Map<Integer, Set<Integer>> graph = buildGraph();

        // BFS
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> parent = new HashMap<>(); // child → parent in BFS tree

        queue.add(currentRegionId);
        parent.put(currentRegionId, null);

        while (!queue.isEmpty()) {
            Integer current = queue.poll();

            if (current.equals(destinationRegionId)) {
                // Reconstruct path and return the first step after currentRegionId
                return Optional.of(firstStep(parent, currentRegionId, destinationRegionId));
            }

            Set<Integer> neighbours = graph.getOrDefault(current, Collections.emptySet());
            for (Integer neighbour : neighbours) {
                if (!parent.containsKey(neighbour)) {
                    parent.put(neighbour, current);
                    queue.add(neighbour);
                }
            }
        }

        return Optional.empty(); // no route found
    }

    /**
     * Walks the BFS parent map backwards from destination to find the first
     * step after the starting node.
     */
    private Integer firstStep(Map<Integer, Integer> parent, Integer start, Integer destination) {
        Integer current = destination;
        while (current != null && !start.equals(parent.get(current))) {
            current = parent.get(current);
        }
        return current; // this is the direct neighbour of start on the shortest path
    }

    /**
     * Calculates the number of hops (route length) between two regions.
     * Returns 0 if regions are the same, or -1 if no route exists.
     */
    public int findRouteLength(Integer startRegionId, Integer destinationRegionId) {
        if (startRegionId.equals(destinationRegionId)) {
            return 0;
        }

        Map<Integer, Set<Integer>> graph = buildGraph();
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distance = new HashMap<>();

        queue.add(startRegionId);
        distance.put(startRegionId, 0);

        while (!queue.isEmpty()) {
            Integer current = queue.poll();

            if (current.equals(destinationRegionId)) {
                return distance.get(current);
            }

            Set<Integer> neighbours = graph.getOrDefault(current, Collections.emptySet());
            for (Integer neighbour : neighbours) {
                if (!distance.containsKey(neighbour)) {
                    distance.put(neighbour, distance.get(current) + 1);
                    queue.add(neighbour);
                }
            }
        }

        return -1;
    }
}
