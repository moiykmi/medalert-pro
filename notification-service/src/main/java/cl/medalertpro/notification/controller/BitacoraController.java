package cl.medalertpro.notification.controller;

import cl.medalertpro.notification.dto.BitacoraResponse;
import cl.medalertpro.notification.service.AdminAuthGuard;
import cl.medalertpro.notification.service.BitacoraService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/bitacora")
public class BitacoraController {

    private final BitacoraService bitacoraService;
    private final AdminAuthGuard authGuard;

    public BitacoraController(BitacoraService bitacoraService, AdminAuthGuard authGuard) {
        this.bitacoraService = bitacoraService;
        this.authGuard = authGuard;
    }

    @GetMapping
    public BitacoraResponse obtener(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            HttpServletRequest request) {
        authGuard.validar(request);
        return bitacoraService.obtener(fecha != null ? fecha : LocalDate.now());
    }
}
