package com.alexanderaya.polizas.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador que envía los eventos al CORE (en esta prueba, al endpoint /core-mock/evento).
 * En producción apuntaría al servicio de edición expuesto en la capa media WebLogic.
 */
@Component
public class CoreRestAdapter implements CoreClient {

    private static final Logger log = LoggerFactory.getLogger(CoreRestAdapter.class);
    private static final String EVENTO_ACTUALIZACION = "ACTUALIZACION";

    private final RestClient restClient;
    private final String apiKey;

    public CoreRestAdapter(@Value("${app.core.base-url}") String baseUrl,
                           @Value("${app.security.api-key}") String apiKey) {
        this.restClient = RestClient.create(baseUrl);
        this.apiKey = apiKey;
    }

    @Override
    public void notificarActualizacion(Long polizaId) {
        try {
            restClient.post()
                    .uri("/core-mock/evento")
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EventoCoreRequest(EVENTO_ACTUALIZACION, polizaId))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento {} enviado al CORE para la póliza {}", EVENTO_ACTUALIZACION, polizaId);
        } catch (RestClientException ex) {
            log.error("No fue posible notificar al CORE la actualización de la póliza {}: {}",
                    polizaId, ex.getMessage());
        }
    }
}