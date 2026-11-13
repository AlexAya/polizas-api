package com.alexanderaya.polizas.service;

import com.alexanderaya.polizas.domain.EstadoPoliza;
import com.alexanderaya.polizas.domain.EstadoRiesgo;
import com.alexanderaya.polizas.domain.Poliza;
import com.alexanderaya.polizas.domain.Riesgo;
import com.alexanderaya.polizas.domain.TipoPoliza;
import com.alexanderaya.polizas.dto.CrearRiesgoRequest;
import com.alexanderaya.polizas.dto.PolizaResponse;
import com.alexanderaya.polizas.dto.RiesgoResponse;
import com.alexanderaya.polizas.exception.RecursoNoEncontradoException;
import com.alexanderaya.polizas.exception.ReglaNegocioException;
import com.alexanderaya.polizas.integration.CoreClient;
import com.alexanderaya.polizas.repository.PolizaRepository;
import com.alexanderaya.polizas.repository.RiesgoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolizaServiceTest {

    private static final BigDecimal IPC = new BigDecimal("0.05");

    @Mock
    private PolizaRepository polizaRepository;

    @Mock
    private RiesgoRepository riesgoRepository;

    @Mock
    private CoreClient coreClient;

    private PolizaService polizaService;

    @BeforeEach
    void setUp() {
        polizaService = new PolizaService(polizaRepository, riesgoRepository, coreClient, IPC);
    }

    private Poliza nuevaPoliza(TipoPoliza tipo) {
        return new Poliza("POL-TEST-001", tipo, "Tomador de prueba",
                LocalDate.of(2026, 1, 1), 12, new BigDecimal("1500000.00"));
    }

    @Test
    @DisplayName("Renovar incrementa canon y prima según el IPC y extiende la vigencia")
    void renovarIncrementaCanonYPrimaSegunIpc() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        PolizaResponse resultado = polizaService.renovar(1L);

        assertThat(resultado.canonMensual()).isEqualByComparingTo("1575000.00");
        assertThat(resultado.prima()).isEqualByComparingTo("18900000.00");
        assertThat(resultado.estado()).isEqualTo(EstadoPoliza.RENOVADA);
        assertThat(resultado.fechaInicio()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(resultado.fechaFin()).isEqualTo(LocalDate.of(2027, 12, 31));
        verify(coreClient).notificarActualizacion(any());
    }

    @Test
    @DisplayName("No se puede renovar una póliza cancelada")
    void noSePuedeRenovarPolizaCancelada() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        poliza.setEstado(EstadoPoliza.CANCELADA);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        assertThatThrownBy(() -> polizaService.renovar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("cancelada");
        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("Cancelar una póliza cancela todos sus riesgos")
    void cancelarPolizaCancelaTodosSusRiesgos() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        poliza.getRiesgos().addAll(List.of(
                new Riesgo(poliza, "Calle 1 # 2-3", "Arrendatario 1"),
                new Riesgo(poliza, "Calle 4 # 5-6", "Arrendatario 2")
        ));
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        polizaService.cancelar(1L);

        assertThat(poliza.getEstado()).isEqualTo(EstadoPoliza.CANCELADA);
        assertThat(poliza.getRiesgos())
                .extracting(Riesgo::getEstado)
                .containsOnly(EstadoRiesgo.CANCELADO);
        verify(coreClient).notificarActualizacion(any());
    }

    @Test
    @DisplayName("No se pueden agregar riesgos a una póliza individual")
    void noSePuedeAgregarRiesgoAPolizaIndividual() {
        Poliza poliza = nuevaPoliza(TipoPoliza.INDIVIDUAL);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));

        assertThatThrownBy(() -> polizaService.agregarRiesgo(1L,
                new CrearRiesgoRequest("Calle 1 # 2-3", "Arrendatario")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("colectivas");
        verify(riesgoRepository, never()).save(any());
        verifyNoInteractions(coreClient);
    }

    @Test
    @DisplayName("Agregar riesgo a una póliza colectiva lo guarda como ACTIVO")
    void agregarRiesgoAPolizaColectiva() {
        Poliza poliza = nuevaPoliza(TipoPoliza.COLECTIVA);
        when(polizaRepository.findById(1L)).thenReturn(Optional.of(poliza));
        when(riesgoRepository.save(any(Riesgo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        RiesgoResponse resultado = polizaService.agregarRiesgo(1L,
                new CrearRiesgoRequest("Calle 26 # 50-10", "Sofia Ramirez"));

        assertThat(resultado.estado()).isEqualTo(EstadoRiesgo.ACTIVO);
        assertThat(resultado.arrendatario()).isEqualTo("Sofia Ramirez");
        assertThat(poliza.getRiesgos()).hasSize(1);
        verify(coreClient).notificarActualizacion(any());
    }

    @Test
    @DisplayName("Operar sobre una póliza inexistente lanza RecursoNoEncontradoException")
    void polizaInexistenteLanzaExcepcion() {
        when(polizaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> polizaService.renovar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }
}