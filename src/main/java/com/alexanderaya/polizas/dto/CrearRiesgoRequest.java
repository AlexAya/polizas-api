package com.alexanderaya.polizas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRiesgoRequest(
        @NotBlank(message = "La dirección del inmueble es obligatoria")
        @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
        String direccionInmueble,

        @NotBlank(message = "El arrendatario es obligatorio")
        @Size(max = 120, message = "El arrendatario no puede superar 120 caracteres")
        String arrendatario
) {
}