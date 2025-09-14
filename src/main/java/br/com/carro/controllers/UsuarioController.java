package br.com.carro.controllers;

import br.com.carro.entities.DTO.UsuarioCreateDTO;
import br.com.carro.entities.DTO.UsuarioResponseDTO;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.entities.DTO.UsuarioLogadoDTO;
import br.com.carro.exceptions.ErrorMessage;
import br.com.carro.exceptions.ResourceNotFoundException;
import br.com.carro.services.UsuarioService;
import br.com.carro.utils.AuthService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
//import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Set;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/usuario") // Padroniza o caminho base da sua API
public class UsuarioController {
    private final UsuarioService usuarioService;
    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);
    private final PasswordEncoder passwordEncoder;
    private AuthService authService;

    public record Mensagem(String mensagem) {}

    public UsuarioController(UsuarioService usuarioService, PasswordEncoder passwordEncoder, AuthService authService) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    // Listar registros com paginação, filtros e ordenação
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    @GetMapping
    public ResponseEntity<Page<Usuario>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String username,
             @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sortObj = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Usuario> lista;

        if (username != null && !username.isBlank()) {
            lista = usuarioService.buscarPorNome(username, pageable);
        } else {
            lista = usuarioService.listar(pageable);
        }

        return ResponseEntity.ok(lista);
    }

    @PostMapping()
    @Transactional
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cadastrar(@Valid @RequestBody UsuarioCreateDTO usuarioCreateDTO, Authentication authentication, HttpServletRequest request) throws AccessDeniedException{

        try {
            // Busca o usuário logado
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);

            // Converte DTO para entidade
            Usuario usuario = new Usuario();
            usuario.setUsername(usuarioCreateDTO.username());
            usuario.setPassword(passwordEncoder.encode(usuarioCreateDTO.password()));

            // Extrai role IDs
            Set<Long> roleIds = usuarioCreateDTO.roles()
                    .stream()
                    .map(UsuarioCreateDTO.RoleIdDto::id)
                    .collect(Collectors.toSet());

            // Chama o service
            Usuario usuarioSalvo = usuarioService.cadastrar(usuario, roleIds, usuarioLogado);

            // Converte para DTO de resposta
            UsuarioResponseDTO resposta = UsuarioResponseDTO.fromEntity(usuarioSalvo);

            return ResponseEntity.status(HttpStatus.CREATED).body(resposta);

        } catch (EntityExistsException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.CONFLICT.value(),
                    "Usuário já existe",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);

        } catch (EntityNotFoundException | IllegalArgumentException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.BAD_REQUEST.value(),
                    "Dados inválidos",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (AccessDeniedException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.FORBIDDEN.value(),
                    "Acesso negado",
                    "Você não tem permissão para cadastrar usuários.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            logger.error("Erro inesperado ao cadastrar usuário", e);
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro ao cadastrar usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    // Buscar carro por ID
    @GetMapping("/{id}")
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            // Chama o service que retorna o objeto ou lança exceção se não existir
            Usuario usuario = usuarioService.buscarPorId(id);
            return new ResponseEntity<>(usuario, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    // Atualizar um carro
    @PatchMapping("/{id}")
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<Usuario> atualizar(@PathVariable Long id, @RequestBody Usuario usuario) { // ✅ Retorna Usuario
        try {
            // ✅ CORREÇÃO: Apenas codifica e define a senha se ela foi fornecida na requisição
            if (usuario.getPassword() != null && !usuario.getPassword().isBlank()) {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            } else {
                // Se a senha não foi fornecida, garantimos que ela não será atualizada.
                // Passamos null para o service indicar que a senha não deve ser alterada.
                // IMPORTANTE: O serviço DEVE lidar com essa lógica de não alterar senha se for null.
                usuario.setPassword(null);
            }

            // Atualiza o usuário usando o service
            Usuario usuarioAtualizado = this.usuarioService.atualizar(id, usuario); // ✅ Retorna o Usuario atualizado
            return new ResponseEntity<>(usuarioAtualizado, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Erro ao atualizar usuário com ID {}: {}", id, e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST); // Retornar null ou DTO de erro
        }
    }

    // Excluir um registro
    @DeleteMapping("/{id}")
    @Transactional
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<?> excluirUsuario(@PathVariable Long id,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            usuarioService.excluir(id, usuarioLogado);
            return ResponseEntity.noContent().build();

        }catch (ResourceNotFoundException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.NOT_FOUND.value(),
                    "Usuário não encontrado",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);

        } catch (IllegalArgumentException | EntityNotFoundException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.BAD_REQUEST.value(),
                    "Dados inválidos",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);

        } catch (AccessDeniedException e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.FORBIDDEN.value(),
                    "Acesso negado",
                    "Você não tem permissão para excluir este usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro ao excluir o usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Método para buscar o usuário logado
    @GetMapping("/logado")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUsuarioLogado(HttpServletRequest request) {
        UsuarioLogadoDTO usuario = usuarioService.buscarUsuarioLogado();
        if (usuario != null) {
            return ResponseEntity.ok(usuario);
        } else {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.NOT_FOUND.value(),
                    "Recurso não encontrado",
                    "Usuário autenticado não encontrado",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }





}
