package com.upc.av_2.controllers;

import com.upc.av_2.dtos.RutaDTO;
import com.upc.av_2.services.RutaService;
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
@RequestMapping("/api/rutas")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    @GetMapping
    public ResponseEntity<List<RutaDTO>> listar() {
        return ResponseEntity.ok(rutaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RutaDTO> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(rutaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<RutaDTO> guardar(@Valid @RequestBody RutaDTO rutaDTO) {
        RutaDTO rutaGuardada = rutaService.guardar(rutaDTO);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(rutaGuardada.getIdRuta())
                .toUri();
        return ResponseEntity.created(location).body(rutaGuardada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RutaDTO> actualizar(@PathVariable Integer id, @Valid @RequestBody RutaDTO rutaDTO) {
        return ResponseEntity.ok(rutaService.actualizar(id, rutaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        rutaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
