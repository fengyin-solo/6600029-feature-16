package com.drone.controller;

import com.drone.model.Waypoint;
import com.drone.service.RouteService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping("/route/plan")
    public List<Waypoint> planRoute(@RequestBody Map<String, Object> request) {
        double startLat = ((Number) request.get("startLat")).doubleValue();
        double startLng = ((Number) request.get("startLng")).doubleValue();
        double goalLat = ((Number) request.get("goalLat")).doubleValue();
        double goalLng = ((Number) request.get("goalLng")).doubleValue();
        String algorithm = (String) request.getOrDefault("algorithm", "astar");

        return routeService.planRoute(startLat, startLng, goalLat, goalLng, algorithm);
    }

    @GetMapping("/noflyzones")
    public List<Map<String, Object>> getNoFlyZones() {
        return routeService.getNoFlyZones();
    }

    @GetMapping("/terrain")
    public List<Map<String, Object>> getTerrain() {
        return routeService.getTerrain();
    }

    @PostMapping("/route/export")
    public Map<String, String> exportRoute(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wpData = (List<Map<String, Object>>) request.get("waypoints");
        String name = (String) request.getOrDefault("name", "Flight Plan");

        List<Waypoint> waypoints = new java.util.ArrayList<>();
        for (Map<String, Object> w : wpData) {
            waypoints.add(new Waypoint(
                (String) w.get("id"),
                ((Number) w.get("lat")).doubleValue(),
                ((Number) w.get("lng")).doubleValue(),
                ((Number) w.get("altitude")).doubleValue(),
                ((Number) w.get("speed")).doubleValue(),
                (String) w.get("action")
            ));
        }

        String kml = routeService.exportKML(waypoints, name);
        return Map.of("kml", kml);
    }

    @PostMapping("/route/validate")
    public Map<String, Object> validateRoute(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> wpData = (List<Map<String, Object>>) request.get("waypoints");
        double batteryUsage = ((Number) request.getOrDefault("batteryUsage", 0)).doubleValue();
        double maxAltitude = ((Number) request.getOrDefault("maxAltitude", 500)).doubleValue();
        double safeDistance = ((Number) request.getOrDefault("safeDistance", 30)).doubleValue();

        List<Waypoint> waypoints = new java.util.ArrayList<>();
        for (Map<String, Object> w : wpData) {
            waypoints.add(new Waypoint(
                (String) w.getOrDefault("id", ""),
                ((Number) w.get("lat")).doubleValue(),
                ((Number) w.get("lng")).doubleValue(),
                ((Number) w.getOrDefault("altitude", 100)).doubleValue(),
                ((Number) w.getOrDefault("speed", 10)).doubleValue(),
                (String) w.getOrDefault("action", "none")
            ));
        }

        return routeService.validateRoute(waypoints, batteryUsage, maxAltitude, safeDistance);
    }
}
