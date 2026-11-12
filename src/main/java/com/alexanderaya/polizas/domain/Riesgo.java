package com.alexanderaya.polizas.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "riesgo")
public class Riesgo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Column(name = "direccion_inmueble", nullable = false)
    private String direccionInmueble;

    @Column(nullable = false)
    private String arrendatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRiesgo estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    protected Riesgo() {
        // Requerido por JPA
    }

    public Riesgo(Poliza poliza, String direccionInmueble, String arrendatario) {
        this.poliza = poliza;
        this.direccionInmueble = direccionInmueble;
        this.arrendatario = arrendatario;
        this.estado = EstadoRiesgo.ACTIVO;
        this.fechaCreacion = LocalDate.now();
    }

    public boolean estaActivo() {
        return estado == EstadoRiesgo.ACTIVO;
    }

    public Long getId() { return id; }
    public Poliza getPoliza() { return poliza; }
    public String getDireccionInmueble() { return direccionInmueble; }
    public String getArrendatario() { return arrendatario; }
    public EstadoRiesgo getEstado() { return estado; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }

    public void setEstado(EstadoRiesgo estado) { this.estado = estado; }
}