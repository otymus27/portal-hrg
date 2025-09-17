package br.com.carro.services;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsável por monitorar sessões e logins de usuários.
 */
@Service
public class SessionTrackerService {

    // Usuários com sessão ativa agora
    private final Set<String> usuariosAtivos = ConcurrentHashMap.newKeySet();

    // Usuários que logaram hoje
    private final Set<String> usuariosLogaramHoje = ConcurrentHashMap.newKeySet();

    // Registra login no momento em que o usuário autentica
    public void registrarLogin(String username) {
        usuariosAtivos.add(username);
        usuariosLogaramHoje.add(username);
    }

    // Remove quando o usuário sai (ou token expira)
    public void removerSessaoAtiva(String username) {
        usuariosAtivos.remove(username);
    }

    public int getUsuariosAtivosAgora() {
        return usuariosAtivos.size();
    }

    public int getUsuariosLogaramHoje() {
        return usuariosLogaramHoje.size();
    }

    // Reset diário (opcional, se quiser limpar a lista a cada dia)
    public void resetarLoginsDoDia() {
        usuariosLogaramHoje.clear();
    }
}