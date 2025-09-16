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
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
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


    // Buscar usuário por ID
    @GetMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // ✅ Apenas ADMIN pode buscar outro usuário
    public ResponseEntity<?> buscarUsuarioPorId(@PathVariable Long id,
                                                Authentication authentication,
                                                HttpServletRequest request) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);
            Usuario usuario = usuarioService.buscarPorId(id, usuarioLogado);
            return ResponseEntity.ok(usuario);

        } catch (ResourceNotFoundException e) {
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
                    "Você não tem permissão para visualizar este usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro inesperado ao buscar usuário por ID.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    // ✅ Atualizar um registro
    @PatchMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode atualizar usuários
    public ResponseEntity<?> atualizarUsuario(@PathVariable Long id,
                                              @RequestBody Usuario usuarioComNovosDados,
                                              Authentication authentication,
                                              HttpServletRequest request) {
        try {
            Usuario usuarioLogado = authService.getUsuarioLogado(authentication);

            // ⚠️ Aqui já deve vir com a senha encodada do Controller (se aplicável)
            Usuario usuarioAtualizado = usuarioService.atualizar(id, usuarioComNovosDados, usuarioLogado);
            return ResponseEntity.ok(usuarioAtualizado);

        } catch (ResourceNotFoundException e) {
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
                    "Você não tem permissão para atualizar este usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro inesperado ao atualizar o usuário.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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

    // ✅ ENDPOINT - Buscar usuário logado
    @GetMapping("/logado")
    @Transactional
    @PreAuthorize("isAuthenticated()") // Qualquer usuário autenticado pode acessar
    public ResponseEntity<?> getUsuarioLogado(Authentication authentication,
                                              HttpServletRequest request) {
        try {
            UsuarioLogadoDTO usuarioLogado = usuarioService.buscarUsuarioLogado();
            return ResponseEntity.ok(usuarioLogado);

        } catch (ResourceNotFoundException e) {
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
                    "Você não tem permissão para acessar este recurso.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);

        } catch (Exception e) {
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno no servidor",
                    "Erro inesperado ao buscar usuário logado.",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }






}
