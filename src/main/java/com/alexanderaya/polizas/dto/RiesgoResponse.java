package com.alexanderaya.polizas.dto;

import com.alexanderaya.polizas.domain.EstadoRiesgo;
import com.alexanderaya.polizas.domain.Riesgo;

import java.time.LocalDate;

public record RiesgoResponse(
        Long id,
        Long polizaId,
        String direccionInmueble,
        String arrendatario,
        EstadoRiesgo estado,
        LocalDate fechaCreacion
) {
    public static RiesgoResponse desde(Riesgo riesgo) {
        return new RiesgoResponse(
                riesgo.getId(),
                riesgo.getPoliza().getId(),
                riesgo.getDireccionInmueble(),
                riesgo.getArrendatario(),
                riesgo.getEstado(),
                riesgo.getFechaCreacion()
        );
    }
}