package br.com.carro.controllers;

import br.com.carro.entities.DTO.AlterarSenhaRequestDTO;
import br.com.carro.entities.Senha.RecuperaSenhaRequestDto;
import br.com.carro.entities.Senha.ResetSenhaRequestDto;
import br.com.carro.exceptions.ErrorMessage;
import br.com.carro.services.RecuperarSenhaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;


// Endpoint para recuperar senha do usuário
@RestController
@RequestMapping("api/recuperar")
public class RecuperarSenhaController {

    private static final Logger logger = LoggerFactory.getLogger(RecuperarSenhaController.class);
    public record Mensagem(String mensagem) {}
    private final RecuperarSenhaService recuperarSenhaService;

    public RecuperarSenhaController(RecuperarSenhaService recuperarSenhaService) {
        this.recuperarSenhaService = recuperarSenhaService;
    }

    /**
     * Endpoint para o ADMIN gerar a senha provisória para o usuário.
     * @param dto com o id do usuário para quem será gerada a senha provisória
     * @return mensagem de sucesso ou erro caso usuário não exista
     */
    @PostMapping("/gerar-senha")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')") // CORRIGIDO: Era 'ROLE_ADMIN', agora é 'ADMIN'
    public ResponseEntity<Object> gerarSenhaProvisoria(@RequestBody RecuperaSenhaRequestDto dto, HttpServletRequest request) {
        logger.info("Iniciando solicitação de geração de senha provisória para ID: {}", dto.id());

        try {
            String senhaProvisoria = recuperarSenhaService.gerarSenhaProvisoria(dto.id());
            return ResponseEntity.ok(new Mensagem("Senha provisória gerada: " + senhaProvisoria));
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao gerar senha provisória: {}", e.getMessage());
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.NOT_FOUND.value(),
                    "Usuário não encontrado",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Endpoint para o usuário redefinir a senha usando a senha provisória.
     * @param dto com id, senha provisória e nova senha definitiva
     * @return mensagem de sucesso ou erro caso dados inválidos
     */
    @PostMapping("/redefinir-senha")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'BASIC')")
    public ResponseEntity<Object> confirmarRedefinicao(@RequestBody ResetSenhaRequestDto dto,
                                                       HttpServletRequest request) {
        logger.info("Tentando redefinir senha para username: {}", dto.username());

        try {
            recuperarSenhaService.atualizarSenha(dto.username(), dto.senhaProvisoria(), dto.novaSenha());
            return ResponseEntity.ok(new Mensagem("Senha redefinida com sucesso."));
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao redefinir senha: {}", e.getMessage());
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.BAD_REQUEST.value(),
                    "Erro de validação",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

//    Endpoint 3 - usuario refine sua propria senha.
    @PutMapping("/alterar-senha")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','BASIC')")
    public ResponseEntity<Object> alterarSenha(@RequestBody AlterarSenhaRequestDTO dto,
                                               @AuthenticationPrincipal Jwt jwt,
                                               HttpServletRequest request) {
        String username = jwt.getClaim("sub"); // pega usuário logado pelo token
        logger.info("Usuário {} solicitou alteração de senha", username);

        try {
            recuperarSenhaService.atualizarSenha(username, dto.senhaAtual(), dto.novaSenha());
            return ResponseEntity.ok(new Mensagem("Senha alterada com sucesso."));
        } catch (IllegalArgumentException e) {
            logger.error("Erro ao alterar senha: {}", e.getMessage());
            ErrorMessage error = new ErrorMessage(
                    HttpStatus.BAD_REQUEST.value(),
                    "Erro de validação",
                    e.getMessage(),
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }



}
