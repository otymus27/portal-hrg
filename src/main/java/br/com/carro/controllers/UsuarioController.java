package br.com.carro.controllers;

import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.entities.Usuario.UsuarioDto;
import br.com.carro.exceptions.ErrorMessage;
import br.com.carro.services.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/usuario") // Padroniza o caminho base da sua API
public class UsuarioController {
    private final UsuarioService usuarioService;
    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);
    private final PasswordEncoder passwordEncoder;

    public record Mensagem(String mensagem) {}

    public UsuarioController(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
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
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<String> cadastrar(@RequestBody Usuario usuario) {
        try {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            String mensagem = this.usuarioService.cadastrar(usuario);
            return new ResponseEntity<>(mensagem, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao cadastrar registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
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

    // Excluir um carro
    @DeleteMapping("/{id}")
    @Transactional
    // ✅ Apenas usuários com a role 'ADMIN' podem acessar este método para gerenciar usuários.
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<String> excluir(@PathVariable Long id) {
        try {
            // Chama o service que já verifica se o carro existe e lança exceção se não existir
            String mensagem = this.usuarioService.excluir(id);
            return new ResponseEntity<>(mensagem, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erro ao excluir registro: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }


    // Método para buscar o usuário logado
    @GetMapping("/logado")
    @Transactional
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUsuarioLogado(HttpServletRequest request) {
        UsuarioDto usuario = usuarioService.buscarUsuarioLogado();
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
