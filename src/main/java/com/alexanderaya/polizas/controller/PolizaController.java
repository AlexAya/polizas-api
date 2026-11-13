package com.alexanderaya.polizas.controller;

import com.alexanderaya.polizas.domain.EstadoPoliza;
import com.alexanderaya.polizas.domain.TipoPoliza;
import com.alexanderaya.polizas.dto.CrearRiesgoRequest;
import com.alexanderaya.polizas.dto.PolizaResponse;
import com.alexanderaya.polizas.dto.RiesgoResponse;
import com.alexanderaya.polizas.service.PolizaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/polizas")
public class PolizaController {

    private final PolizaService polizaService;

    public PolizaController(PolizaService polizaService) {
        this.polizaService = polizaService;
    }

    @GetMapping
    public List<PolizaResponse> listar(@RequestParam(required = false) TipoPoliza tipo,
                                       @RequestParam(required = false) EstadoPoliza estado) {
        return polizaService.listar(tipo, estado);
    }

    @GetMapping("/{id}/riesgos")
    public List<RiesgoResponse> listarRiesgos(@PathVariable Long id) {
        return polizaService.listarRiesgos(id);
    }

    @PostMapping("/{id}/renovar")
    public PolizaResponse renovar(@PathVariable Long id) {
        return polizaService.renovar(id);
    }

    @PostMapping("/{id}/cancelar")
    public PolizaResponse cancelar(@PathVariable Long id) {
        return polizaService.cancelar(id);
    }

    @PostMapping("/{id}/riesgos")
    public ResponseEntity<RiesgoResponse> agregarRiesgo(@PathVariable Long id,
                                                        @Valid @RequestBody CrearRiesgoRequest request) {
        RiesgoResponse riesgo = polizaService.agregarRiesgo(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(riesgo);
    }
}