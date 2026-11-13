package com.alexanderaya.polizas.dto;

import com.alexanderaya.polizas.domain.EstadoPoliza;
import com.alexanderaya.polizas.domain.Poliza;
import com.alexanderaya.polizas.domain.TipoPoliza;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PolizaResponse(
        Long id,
        String numero,
        TipoPoliza tipo,
        EstadoPoliza estado,
        String tomador,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        Integer mesesVigencia,
        BigDecimal canonMensual,
        BigDecimal prima
) {
    public static PolizaResponse desde(Poliza poliza) {
        return new PolizaResponse(
                poliza.getId(),
                poliza.getNumero(),
                poliza.getTipo(),
                poliza.getEstado(),
                poliza.getTomador(),
                poliza.getFechaInicio(),
                poliza.getFechaFin(),
                poliza.getMesesVigencia(),
                poliza.getCanonMensual(),
                poliza.getPrima()
        );
    }
}