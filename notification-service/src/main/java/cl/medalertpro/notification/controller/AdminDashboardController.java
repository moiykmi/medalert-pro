package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.dto.AdminDashboardEventDto;
import cl.medalertpro.notification.dto.AdminDashboardKpisResponse;
import cl.medalertpro.notification.service.AdminAuthGuard;
import cl.medalertpro.notification.service.AdminDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminAuthGuard authGuard;

    public AdminDashboardController(AdminDashboardService dashboardService, AdminAuthGuard authGuard) {
        this.dashboardService = dashboardService;
        this.authGuard = authGuard;
    }

    @GetMapping("/kpis")
    public AdminDashboardKpisResponse kpis(HttpServletRequest request) {
        authGuard.validar(request);
        return dashboardService.obtenerKpis();
    }

    @GetMapping("/eventos-recientes")
    public List<AdminDashboardEventDto> eventosRecientes(
            @RequestParam(defaultValue = "20") int limite,
            HttpServletRequest request) {
        authGuard.validar(request);
        return dashboardService.obtenerEventosRecientes(limite);
    }
}
