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
import java.time.LocalDateTime;
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

        usuario.setNome(usuario.getNome());
        usuario.setRoles(roles);

        try {
            return usuarioRepository.save(usuario);
        } catch (DataIntegrityViolationException e) {
            // Captura erro de banco e converte para exceção mais amigável
            throw new EntityExistsException("Usuário com este username já existe.");
        }
    }

    // Buscar usuario por ID
    @Transactional(rollbackFor = Exception.class, noRollbackFor = {ResourceNotFoundException.class})
    public Usuario buscarPorId(Long id, Usuario usuarioLogado) throws AccessDeniedException {
        // Apenas admins podem consultar diretamente outro usuário
        if (!usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"))) {
            throw new AccessDeniedException("Usuário não possui permissão para consultar outro usuário.");
        }

        // Evita que o usuário busque a si mesmo de forma redundante (opcional, mas consistente com excluir)
        if (id.equals(usuarioLogado.getId())) {
            throw new IllegalArgumentException("Para consultar seus próprios dados, utilize o endpoint de usuário logado.");
        }

        // Busca o usuário no banco
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id " + id));
    }


    @Transactional(
            rollbackFor = Exception.class,
            noRollbackFor = {ResourceNotFoundException.class, IllegalArgumentException.class}
    )
    public Usuario atualizar(Long id, Usuario usuarioComNovosDados, Usuario usuarioLogado) throws AccessDeniedException {
        // ✅ Apenas admins podem atualizar usuários
        if (!usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN"))) {
            throw new AccessDeniedException("Usuário não possui permissão para atualizar outros usuários.");
        }

        // ✅ Não permitir que o usuário remova suas próprias permissões críticas
        if (id.equals(usuarioLogado.getId()) && usuarioComNovosDados.getRoles() != null) {
            boolean removeuAdmin = usuarioComNovosDados.getRoles().stream()
                    .noneMatch(role -> "ADMIN".equals(role.getNome()));
            if (removeuAdmin) {
                throw new IllegalArgumentException("Usuário não pode remover seu próprio papel de ADMIN.");
            }
        }

        // ✅ Buscar usuário no banco
        Usuario usuarioExistente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id " + id));

        // Atualizar username e nome
        usuarioExistente.setUsername(usuarioComNovosDados.getUsername());
        usuarioExistente.setNome(usuarioComNovosDados.getNome());

        // ✅ Atualizar roles
        if (usuarioComNovosDados.getRoles() != null) {
            Set<Long> roleIds = usuarioComNovosDados.getRoles().stream()
                    .map(Role::getId)
                    .collect(Collectors.toSet());

            Set<Role> rolesDoBanco = new HashSet<>(roleRepository.findAllById(roleIds));
            if (rolesDoBanco.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma role válida encontrada para atualizar o usuário.");
            }
            usuarioExistente.setRoles(rolesDoBanco);
        }

        // ✅ Atualizar senha apenas se for fornecida
        if (usuarioComNovosDados.getPassword() != null && !usuarioComNovosDados.getPassword().isBlank()) {
            usuarioExistente.setPassword(usuarioComNovosDados.getPassword()); // já deve estar encodada
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


    @Transactional(readOnly = true, rollbackFor = Exception.class, noRollbackFor = {ResourceNotFoundException.class})
    public UsuarioLogadoDTO buscarUsuarioLogado() throws AccessDeniedException {
        // 1️⃣ Recupera o usuário logado do contexto de segurança
        String login = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2️⃣ Busca usuário no banco
        Usuario usuario = usuarioRepository.findByUsername(login)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado."));

        // 3️⃣ Mapeia pastas acessadas
        Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                .map(Pasta::getId)
                .collect(Collectors.toSet());

        // 4️⃣ Mapeia roles para DTO
        Set<UsuarioLogadoDTO.RoleDto> rolesDto = usuario.getRoles().stream()
                .map(role -> new UsuarioLogadoDTO.RoleDto(role.getId(), role.getNome()))
                .collect(Collectors.toSet());

        // 5️⃣ Retorna DTO consolidado
        return new UsuarioLogadoDTO(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getNome(),
                pastasIds,
                rolesDto
        );
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
