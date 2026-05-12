package com.upc.av_2.services;

import com.upc.av_2.dtos.IncidenteDTO;
import com.upc.av_2.entidades.Incidente;
import com.upc.av_2.repositories.IncidenteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class IncidenteService {

    private final IncidenteRepository incidenteRepository;
    private final ModelMapper modelMapper;

    public List<IncidenteDTO> listar() {
        return incidenteRepository.findAll()
                .stream()
                .map(incidente -> modelMapper.map(incidente, IncidenteDTO.class))
                .toList();
    }

    public IncidenteDTO buscarPorId(Integer id) {
        Incidente incidente = obtenerEntidad(id);
        return modelMapper.map(incidente, IncidenteDTO.class);
    }

    public IncidenteDTO guardar(IncidenteDTO incidenteDTO) {
        Incidente incidente = modelMapper.map(incidenteDTO, Incidente.class);
        Incidente incidenteGuardado = incidenteRepository.save(incidente);
        return modelMapper.map(incidenteGuardado, IncidenteDTO.class);
    }

    public IncidenteDTO actualizar(Integer id, IncidenteDTO incidenteDTO) {
        Incidente incidenteExistente = obtenerEntidad(id);
        modelMapper.map(incidenteDTO, incidenteExistente);
        incidenteExistente.setIdIncidente(id);
        Incidente incidenteActualizado = incidenteRepository.save(incidenteExistente);
        return modelMapper.map(incidenteActualizado, IncidenteDTO.class);
    }

    public void eliminar(Integer id) {
        Incidente incidente = obtenerEntidad(id);
        incidenteRepository.delete(incidente);
    }

    private Incidente obtenerEntidad(Integer id) {
        return incidenteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incidente no encontrado con id " + id));
    }
}
