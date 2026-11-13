package com.alexanderaya.polizas.integration;

/**
 * Puerto de salida hacia el CORE de seguros.
 * El servicio de negocio depende de esta interfaz, no de la tecnología de integración.
 */
public interface CoreClient {

    void notificarActualizacion(Long polizaId);
}