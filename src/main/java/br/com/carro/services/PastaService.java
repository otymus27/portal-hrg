package br.com.carro.services;

import br.com.carro.entities.DTO.*;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import br.com.carro.utils.AuthService;
import br.com.carro.utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PastaService {

    @Autowired
    private PastaRepository pastaRepository;
    private UsuarioRepository usuarioRepository;
    private AuthService authService;

    @Value("${storage.root-dir}")
    private String rootDirectory;

    // ✅ Use constructor injection
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository, AuthService authService) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
    }

    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO, Usuario usuarioLogado) {
        if (usuarioLogado == null) {
            throw new SecurityException("Usuário não autenticado.");
        }

        // Busca a pasta pai se existir
        Pasta pastaPai = null;
        if (pastaDTO.pastaPaiId() != null) {
            pastaPai = pastaRepository.findById(pastaDTO.pastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
        }

        // Validação de permissão
        validarPermissaoCriacao(usuarioLogado, pastaPai);

        // Determina caminho da nova pasta
        String caminhoPastaPai = (pastaPai != null) ? pastaPai.getCaminhoCompleto() : rootDirectory;
        Path caminhoPasta = Paths.get(caminhoPastaPai, FileUtils.sanitizeFileName(pastaDTO.nome()));

        if (Files.exists(caminhoPasta)) {
            throw new IllegalArgumentException("Uma pasta com este nome já existe neste local.");
        }

        try {
            Files.createDirectory(caminhoPasta);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar a pasta no sistema de arquivos.", e);
        }

        // Busca usuários com permissão
        Set<Usuario> usuariosComPermissao = usuarioRepository.findAllById(pastaDTO.usuariosComPermissaoIds())
                .stream().collect(Collectors.toSet());

        if (usuariosComPermissao.size() != pastaDTO.usuariosComPermissaoIds().size()) {
            throw new IllegalArgumentException("Um ou mais IDs de usuário fornecidos não são válidos.");
        }

        // Cria e salva a nova pasta
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.nome());
        novaPasta.setCaminhoCompleto(caminhoPasta.toString());
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setDataAtualizacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuarioLogado);
        novaPasta.setUsuariosComPermissao(usuariosComPermissao);
        if (pastaPai != null) {
            novaPasta.setPastaPai(pastaPai);
        }

        return pastaRepository.save(novaPasta);
    }

    // ✅ Método adicional para listar pastas raiz
    public java.util.List<Pasta> listarPastasRaiz(Usuario usuario) {
        if (usuario.isAdmin()) {
            return pastaRepository.findAllByPastaPaiIsNull();
        } else {
            return pastaRepository.findByUsuariosComPermissaoAndPastaPaiIsNull(usuario);
        }
    }

    @Transactional
    public List<PastaCompletaDTO> getTodasPastasCompletas(Usuario usuarioLogado, PastaFilterDTO filtro) {
        List<Pasta> pastasRaiz = pastaRepository.findByPastaPaiIsNull();

        return pastasRaiz.stream()
                .filter(p -> usuarioLogado.isAdmin() || p.getUsuariosComPermissao().contains(usuarioLogado))
                .map(p -> mapRecursivo(p, usuarioLogado, filtro, 0))
                .collect(Collectors.toList());
    }

    private PastaCompletaDTO mapRecursivo(Pasta pasta, Usuario usuarioLogado, PastaFilterDTO filtro, int nivelAtual) {
        // Limite de profundidade
        if (filtro.getProfundidadeMax() != null && nivelAtual >= filtro.getProfundidadeMax()) {
            return PastaCompletaDTO.fromEntity(pasta);
        }

        // Filtrar subpastas por permissão
        List<PastaCompletaDTO> subPastasDTO = pasta.getSubPastas().stream()
                .filter(sub -> usuarioLogado.isAdmin() || sub.getUsuariosComPermissao().contains(usuarioLogado))
                .map(sub -> mapRecursivo(sub, usuarioLogado, filtro, nivelAtual + 1))
                .collect(Collectors.toList());

        // Filtrar arquivos
        List<ArquivoDTO> arquivosFiltrados = pasta.getArquivos().stream()
                .filter(a -> filtro.getExtensaoArquivo() == null || a.getNomeArquivo().endsWith("." + filtro.getExtensaoArquivo()))
                .filter(a -> filtro.getTamanhoMinArquivo() == null || a.getTamanho() >= filtro.getTamanhoMinArquivo())
                .filter(a -> filtro.getTamanhoMaxArquivo() == null || a.getTamanho() <= filtro.getTamanhoMaxArquivo())
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());

        // Criar DTO
        PastaCompletaDTO dto = new PastaCompletaDTO(
                pasta.getId(),
                pasta.getNomePasta(),
                pasta.getCaminhoCompleto(),
                pasta.getDataCriacao(),
                pasta.getDataAtualizacao(),
                arquivosFiltrados,
                subPastasDTO
        );

        // Ordenação de subpastas
        Comparator<PastaCompletaDTO> comparator;
        switch (filtro.getOrdenarPor()) {
            case "data":
                comparator = Comparator.comparing(PastaCompletaDTO::dataCriacao);
                break;
            case "tamanho":
                comparator = Comparator.comparingInt(p -> p.arquivos().stream().mapToInt(a -> a.tamanho().intValue()).sum());
                break;
            case "nome":
            default:
                comparator = Comparator.comparing(PastaCompletaDTO::nomePasta);
        }
        if (!filtro.isOrdemAsc()) comparator = comparator.reversed();

        dto = dto.withSubPastas(
                dto.subPastas().stream().sorted(comparator).collect(Collectors.toList())
        );

        return dto;
    }



    /**
     * Valida se o usuário logado tem permissão para criar a pasta.
     */
    private void validarPermissaoCriacao(Usuario usuario, Pasta pastaPai) {
        if (usuario.isAdmin()) return; // Admin sempre pode

        // Se não for admin, deve existir pastaPai
        if (pastaPai == null) {
            throw new SecurityException("Gerentes devem criar pastas dentro de uma pasta existente.");
        }

        // Verifica se o gerente tem permissão na pastaPai
        if (!pastaPai.getUsuariosComPermissao().contains(usuario)) {
            throw new SecurityException("Você não tem permissão para criar pastas neste local.");
        }
    }


}