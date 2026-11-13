package com.alexanderaya.polizas.controller;

import com.alexanderaya.polizas.integration.EventoCoreRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simula el servicio agnóstico de edición del CORE (capa media WebLogic).
 * Su único propósito es registrar en logs que la operación se intentó enviar al CORE.
 */
@RestController
@RequestMapping("/core-mock")
public class CoreMockController {

    private static final Logger log = LoggerFactory.getLogger(CoreMockController.class);

    @PostMapping("/evento")
    public ResponseEntity<Map<String, String>> recibirEvento(@RequestBody EventoCoreRequest evento) {
        log.info("[CORE-MOCK] Evento recibido: evento={}, polizaId={}", evento.evento(), evento.polizaId());
        return ResponseEntity.accepted().body(Map.of("mensaje", "Evento registrado en CORE (mock)"));
    }
}