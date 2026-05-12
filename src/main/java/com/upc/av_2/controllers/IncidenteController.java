package com.upc.av_2.controllers;

import com.upc.av_2.dtos.IncidenteDTO;
import com.upc.av_2.services.IncidenteService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/incidentes")
@RequiredArgsConstructor
public class IncidenteController {

    private final IncidenteService incidenteService;

    @GetMapping
    public ResponseEntity<List<IncidenteDTO>> listar() {
        return ResponseEntity.ok(incidenteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidenteDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(incidenteService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<IncidenteDTO> guardar(@Valid @RequestBody IncidenteDTO incidenteDTO) {
        IncidenteDTO incidenteGuardado = incidenteService.guardar(incidenteDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(incidenteGuardado.getIdIncidente())
                .toUri();
        return ResponseEntity.created(location).body(incidenteGuardado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidenteDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody IncidenteDTO incidenteDTO) {
        return ResponseEntity.ok(incidenteService.actualizar(id, incidenteDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        incidenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
