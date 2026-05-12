package com.upc.av_2.services;

import com.upc.av_2.dtos.RutaDTO;
import com.upc.av_2.entidades.Ruta;
import com.upc.av_2.repositories.RutaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RutaService {

    private final RutaRepository rutaRepository;
    private final ModelMapper modelMapper;

    public List<RutaDTO> listar() {
        return rutaRepository.findAll()
                .stream()
                .map(ruta -> modelMapper.map(ruta, RutaDTO.class))
                .toList();
    }

    public RutaDTO buscarPorId(Integer id) {
        Ruta ruta = obtenerEntidad(id);
        return modelMapper.map(ruta, RutaDTO.class);
    }

    public RutaDTO guardar(RutaDTO rutaDTO) {
        Ruta ruta = modelMapper.map(rutaDTO, Ruta.class);
        Ruta rutaGuardada = rutaRepository.save(ruta);
        return modelMapper.map(rutaGuardada, RutaDTO.class);
    }

    public RutaDTO actualizar(Integer id, RutaDTO rutaDTO) {
        Ruta rutaExistente = obtenerEntidad(id);
        modelMapper.map(rutaDTO, rutaExistente);
        rutaExistente.setIdRuta(id);
        Ruta rutaActualizada = rutaRepository.save(rutaExistente);
        return modelMapper.map(rutaActualizada, RutaDTO.class);
    }

    public void eliminar(Integer id) {
        Ruta ruta = obtenerEntidad(id);
        rutaRepository.delete(ruta);
    }

    private Ruta obtenerEntidad(Integer id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ruta no encontrada con id " + id));
    }
}
