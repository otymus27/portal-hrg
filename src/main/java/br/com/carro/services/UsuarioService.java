package br.com.carro.services;

import br.com.carro.entities.Pasta;
import br.com.carro.entities.Role.Role;
import br.com.carro.entities.Role.RoleDto;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.entities.Usuario.UsuarioDto;
import br.com.carro.repositories.RoleRepository;
import br.com.carro.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    // Cadastrar um novo registro diretamente com a entidade sem DTO's
    public String cadastrar(Usuario usuario) {
        // Salva o carro no banco de dados
        this.usuarioRepository.save(usuario);
        return "Cadastro feito com sucesso!";
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

    // Excluir um carro
    public String excluir(Long id) throws Exception {
        this.usuarioRepository.deleteById(id);
        return "Exclusão feita com sucesso!";
    }


    public UsuarioDto buscarUsuarioLogado() {
        // Obtém o login (username) do usuário autenticado no contexto de segurança
        String login = SecurityContextHolder.getContext().getAuthentication().getName();

        Optional<Usuario> optionalUsuario = usuarioRepository.findByUsername(login);
        if (optionalUsuario.isPresent()) {
            Usuario usuario = optionalUsuario.get();
            Set<RoleDto> rolesDto = usuario.getRoles().stream()
                    .map(role -> new RoleDto(role.getId(), role.getNome()))
                    .collect(Collectors.toSet());

            // ✅ LÓGICA ATUALIZADA: Obtém os IDs das pastas principais
            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId)
                    .collect(Collectors.toSet());

            // ✅ AQUI ESTÁ A MUDANÇA: adicionando o ID do setor no DTO
            return new UsuarioDto(
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
