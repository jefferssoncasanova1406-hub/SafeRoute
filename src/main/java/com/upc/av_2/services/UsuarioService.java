package com.upc.av_2.services;

import com.upc.av_2.dtos.UsuarioDTO;
import com.upc.av_2.entidades.Usuario;
import com.upc.av_2.repositories.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ModelMapper modelMapper;

    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioDTO.class))
                .toList();
    }

    public UsuarioDTO buscarPorId(Integer id) {
        Usuario usuario = obtenerEntidad(id);
        return modelMapper.map(usuario, UsuarioDTO.class);
    }

    public UsuarioDTO guardar(UsuarioDTO usuarioDTO) {
        Usuario usuario = modelMapper.map(usuarioDTO, Usuario.class);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        return modelMapper.map(usuarioGuardado, UsuarioDTO.class);
    }

    public UsuarioDTO actualizar(Integer id, UsuarioDTO usuarioDTO) {
        Usuario usuarioExistente = obtenerEntidad(id);
        modelMapper.map(usuarioDTO, usuarioExistente);
        usuarioExistente.setIdUsuario(id);
        Usuario usuarioActualizado = usuarioRepository.save(usuarioExistente);
        return modelMapper.map(usuarioActualizado, UsuarioDTO.class);
    }

    public void eliminar(Integer id) {
        Usuario usuario = obtenerEntidad(id);
        usuarioRepository.delete(usuario);
    }

    private Usuario obtenerEntidad(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id " + id));
    }
}
