package com.upc.grupo3.entidades;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UsuarioPasswordTest {

    @Test
    void passwordAliasShouldReadAndWriteStoredCredential() {
        Usuario usuario = new Usuario();

        usuario.setPassword("hash-nuevo");

        assertEquals("hash-nuevo", usuario.getPassword());
        assertEquals("hash-nuevo", usuario.getContrasena());
    }
}
