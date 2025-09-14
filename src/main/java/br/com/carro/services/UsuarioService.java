package br.com.carro.services;

import br.com.carro.entities.Pasta;
import br.com.carro.entities.Role.Role;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.entities.DTO.UsuarioLogadoDTO;
import br.com.carro.exceptions.ResourceNotFoundException;
import br.com.carro.repositories.RoleRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.AccessDeniedException;
import java.util.HashSet;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private final RoleRepository roleRepository;
    @Autowired
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ Método de cadastro com roles
    @Transactional(noRollbackFor = EntityExistsException.class) // 👈 Evita rollback automático
    public Usuario cadastrar(Usuario usuario, Set<Long> roleIds,Usuario usuarioLogado) throws  AccessDeniedException {
        // Apenas admins podem excluir
        if (!usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"))) {
            throw new AccessDeniedException("Usuário não possui permissão para excluir outro usuário.");
        }
        // Verifica se já existe um usuário com o mesmo username
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new EntityExistsException("Usuário com este username já existe.");
        }

        // Busca as roles pelos IDs
        Set<Role> roles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            roles = roleRepository.findAllById(roleIds)
                    .stream()
                    .collect(Collectors.toSet());
            if (roles.size() != roleIds.size()) {
                throw new EntityNotFoundException("Uma ou mais roles informadas não existem.");
            }
        }

        usuario.setRoles(roles);

        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Captura erro de banco e converte para exceção mais amigável
            throw new EntityExistsException("Usuário com este username já existe.");
        }
    }

    // Buscar carro por ID
    public Usuario buscarPorId(Long id) throws Exception {
        Usuario usuario = this.usuarioRepository.findById(id).get();
        return usuario;
    }

    public Usuario atualizar(Long id, Usuario usuarioComNovosDados) {
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + id));

        usuarioExistente.setUsername(usuarioComNovosDados.getUsername());
        // ... (Atualizar outras propriedades como roles, se estiverem presentes em usuarioComNovosDados)

        // ✅ Lógica para ATUALIZAR as roles
        if (usuarioComNovosDados.getRoles() != null) {
            // Extrai os IDs das roles recebidas do frontend (o frontend envia objetos Role com ID)
            Set<Long> roleIds = usuarioComNovosDados.getRoles().stream()
                    .map(Role::getId) // Mapeia cada Role para seu ID
                    .collect(Collectors.toSet());

            // Busca as entidades Role completas (gerenciadas) do banco de dados pelos IDs
            Set<Role> rolesDoBanco = new HashSet<>(roleRepository.findAllById(roleIds));

            // Seta as roles no usuário existente (substituindo as antigas)
            usuarioExistente.setRoles(rolesDoBanco);
        } else {
            // Se nenhum role for fornecido, você pode optar por:
            // 1. Manter as roles existentes (não fazer nada)
            // 2. Limpar as roles: usuarioExistente.setRoles(new HashSet<>());
            // Para este caso, vamos manter as roles existentes se nenhum for fornecido no DTO
        }

        // ✅ Lógica vital no Service: SÓ ATUALIZA A SENHA SE ELA FOR FORNECIDA
        if (usuarioComNovosDados.getPassword() != null) { // Agora o Controller já fez o encode se for para atualizar
            usuarioExistente.setPassword(usuarioComNovosDados.getPassword());
        }

        return usuarioRepository.save(usuarioExistente);
    }

    @Transactional(rollbackFor = Exception.class, noRollbackFor = {ResourceNotFoundException.class})
    public void excluir(Long id, Usuario usuarioLogado) throws AccessDeniedException {
        // Apenas admins podem excluir
        if (!usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"))) {
            throw new AccessDeniedException("Usuário não possui permissão para excluir outro usuário.");
        }

        // Evita autoexclusão
        if (id.equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("Usuário não pode se auto-excluir.");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id " + id));

        // Exclui usuário
        usuario.getRoles().clear();

        usuarioRepository.delete(usuario);
    }


    public UsuarioLogadoDTO buscarUsuarioLogado() {
        String login = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Usuario> optionalUsuario = usuarioRepository.findByUsername(login);
        if (optionalUsuario.isPresent()) {
            Usuario usuario = optionalUsuario.get();

            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId)
                    .collect(Collectors.toSet());

            Set<UsuarioLogadoDTO.RoleDto> rolesDto = usuario.getRoles().stream()
                    .map(role -> new UsuarioLogadoDTO.RoleDto(role.getId(), role.getNome()))
                    .collect(Collectors.toSet());

            return new UsuarioLogadoDTO(
                    usuario.getId(),
                    usuario.getUsername(),
                    pastasIds,
                    rolesDto
            );
        }

        return null;
    }

    // Listar todas as marcas com paginação
    public Page<Usuario> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable);
    }

    // Listar registros filtrando por modelo (com paginação)
    public Page<Usuario> buscarPorNome(String username, Pageable pageable) {
        return usuarioRepository.findByUsernameContainingIgnoreCase(username,pageable);
    }



}
