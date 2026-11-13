package com.alexanderaya.polizas.repository;

import com.alexanderaya.polizas.domain.EstadoPoliza;
import com.alexanderaya.polizas.domain.Poliza;
import com.alexanderaya.polizas.domain.TipoPoliza;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolizaRepository extends JpaRepository<Poliza, Long> {

    List<Poliza> findByTipoAndEstadoOrderByIdAsc(TipoPoliza tipo, EstadoPoliza estado);

    List<Poliza> findByTipoOrderByIdAsc(TipoPoliza tipo);

    List<Poliza> findByEstadoOrderByIdAsc(EstadoPoliza estado);

    List<Poliza> findAllByOrderByIdAsc();
}