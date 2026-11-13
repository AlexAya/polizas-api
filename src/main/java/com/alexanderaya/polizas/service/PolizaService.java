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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class PolizaService {

    private static final Logger log = LoggerFactory.getLogger(PolizaService.class);

    private final PolizaRepository polizaRepository;
    private final RiesgoRepository riesgoRepository;
    private final CoreClient coreClient;
    private final BigDecimal ipc;

    public PolizaService(PolizaRepository polizaRepository,
                         RiesgoRepository riesgoRepository,
                         CoreClient coreClient,
                         @Value("${app.negocio.ipc}") BigDecimal ipc) {
        this.polizaRepository = polizaRepository;
        this.riesgoRepository = riesgoRepository;
        this.coreClient = coreClient;
        this.ipc = ipc;
    }

    @Transactional(readOnly = true)
    public List<PolizaResponse> listar(TipoPoliza tipo, EstadoPoliza estado) {
        List<Poliza> polizas;
        if (tipo != null && estado != null) {
            polizas = polizaRepository.findByTipoAndEstadoOrderByIdAsc(tipo, estado);
        } else if (tipo != null) {
            polizas = polizaRepository.findByTipoOrderByIdAsc(tipo);
        } else if (estado != null) {
            polizas = polizaRepository.findByEstadoOrderByIdAsc(estado);
        } else {
            polizas = polizaRepository.findAllByOrderByIdAsc();
        }
        return polizas.stream().map(PolizaResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<RiesgoResponse> listarRiesgos(Long polizaId) {
        buscarPoliza(polizaId);
        return riesgoRepository.findByPolizaIdOrderByIdAsc(polizaId).stream()
                .map(RiesgoResponse::desde)
                .toList();
    }

    @Transactional
    public PolizaResponse renovar(Long polizaId) {
        Poliza poliza = buscarPoliza(polizaId);

        // Regla: no se puede renovar una póliza cancelada
        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("No se puede renovar una póliza cancelada");
        }

        // Canon y prima se incrementan según el IPC; la prima = canon x meses de vigencia
        BigDecimal factor = BigDecimal.ONE.add(ipc);
        BigDecimal nuevoCanon = poliza.getCanonMensual().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        BigDecimal nuevaPrima = nuevoCanon.multiply(BigDecimal.valueOf(poliza.getMesesVigencia()))
                .setScale(2, RoundingMode.HALF_UP);

        // Se renueva por el mismo periodo de vigencia inicial
        LocalDate nuevoInicio = poliza.getFechaFin().plusDays(1);
        LocalDate nuevoFin = nuevoInicio.plusMonths(poliza.getMesesVigencia()).minusDays(1);

        poliza.setCanonMensual(nuevoCanon);
        poliza.setPrima(nuevaPrima);
        poliza.setFechaInicio(nuevoInicio);
        poliza.setFechaFin(nuevoFin);
        poliza.setEstado(EstadoPoliza.RENOVADA);

        log.info("Póliza {} renovada. Nuevo canon: {}, nueva prima: {}", poliza.getId(), nuevoCanon, nuevaPrima);
        coreClient.notificarActualizacion(poliza.getId());
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public PolizaResponse cancelar(Long polizaId) {
        Poliza poliza = buscarPoliza(polizaId);

        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("La póliza ya se encuentra cancelada");
        }

        // Regla: la cancelación de una póliza cancela todos sus riesgos
        poliza.setEstado(EstadoPoliza.CANCELADA);
        poliza.getRiesgos().stream()
                .filter(Riesgo::estaActivo)
                .forEach(riesgo -> riesgo.setEstado(EstadoRiesgo.CANCELADO));

        log.info("Póliza {} cancelada junto con sus riesgos activos", poliza.getId());
        coreClient.notificarActualizacion(poliza.getId());
        return PolizaResponse.desde(poliza);
    }

    @Transactional
    public RiesgoResponse agregarRiesgo(Long polizaId, CrearRiesgoRequest request) {
        Poliza poliza = buscarPoliza(polizaId);

        if (poliza.estaCancelada()) {
            throw new ReglaNegocioException("No se pueden agregar riesgos a una póliza cancelada");
        }

        // Reglas: agregar riesgo exige validar el tipo; una póliza individual solo puede tener 1 riesgo
        if (poliza.esIndividual()) {
            throw new ReglaNegocioException(
                    "Solo se pueden agregar riesgos a pólizas colectivas: una póliza individual solo puede tener 1 riesgo");
        }

        Riesgo riesgo = new Riesgo(poliza, request.direccionInmueble(), request.arrendatario());
        poliza.getRiesgos().add(riesgo);
        Riesgo guardado = riesgoRepository.save(riesgo);

        log.info("Riesgo {} agregado a la póliza colectiva {}", guardado.getId(), poliza.getId());
        coreClient.notificarActualizacion(poliza.getId());
        return RiesgoResponse.desde(guardado);
    }

    @Transactional
    public RiesgoResponse cancelarRiesgo(Long riesgoId) {
        Riesgo riesgo = riesgoRepository.findById(riesgoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el riesgo con id " + riesgoId));

        if (!riesgo.estaActivo()) {
            throw new ReglaNegocioException("El riesgo ya se encuentra cancelado");
        }

        riesgo.setEstado(EstadoRiesgo.CANCELADO);

        log.info("Riesgo {} cancelado", riesgo.getId());
        coreClient.notificarActualizacion(riesgo.getPoliza().getId());
        return RiesgoResponse.desde(riesgo);
    }

    private Poliza buscarPoliza(Long polizaId) {
        return polizaRepository.findById(polizaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la póliza con id " + polizaId));
    }
}