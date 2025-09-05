package br.com.carro.services;

import br.com.carro.controllers.PastaController;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import br.com.carro.utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import java.nio.file.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PastaService {

    @Autowired
    private PastaRepository pastaRepository;
    private UsuarioRepository usuarioRepository;

    @Value("${storage.root-dir}")
    private String rootDirectory;

    // ✅ Use constructor injection
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO, Usuario usuarioLogado) throws AccessDeniedException {
        if (usuarioLogado == null) {
            throw new AccessDeniedException("Usuário autenticado não foi encontrado.");
        }

        // Validação de segurança: Admins podem criar pastas em qualquer lugar.
        // Gerentes só podem criar em suas pastas ou subpastas com permissão.
        if (!usuarioLogado.isAdmin()) {
            if (pastaDTO.pastaPaiId() != null) {
                Pasta pastaPai = pastaRepository.findById(pastaDTO.pastaPaiId())
                        .orElseThrow(() -> new IllegalArgumentException("Pasta pai não encontrada."));
                if (!pastaPai.getUsuariosComPermissao().contains(usuarioLogado)) {
                    throw new AccessDeniedException("Você não tem permissão para criar pastas neste local.");
                }
            } else {
                throw new AccessDeniedException("Gerentes devem criar pastas dentro de uma pasta principal.");
            }
        }

        // Determina o caminho completo da nova pasta
        String caminhoPastaPai = (pastaDTO.pastaPaiId() != null) ?
                pastaRepository.findById(pastaDTO.pastaPaiId())
                        .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."))
                        .getCaminhoCompleto() :
                rootDirectory;

        Path caminhoPasta = Paths.get(caminhoPastaPai, FileUtils.sanitizeFileName(pastaDTO.nome()));

        if (Files.exists(caminhoPasta)) {
            throw new IllegalArgumentException("Uma pasta com este nome já existe neste local.");
        }

        try {
            Files.createDirectory(caminhoPasta);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar a pasta no sistema de arquivos.", e);
        }

        // Busca os usuários com permissão
        Set<Usuario> usuariosComPermissao = usuarioRepository.findAllById(pastaDTO.usuariosComPermissaoIds()).stream().collect(Collectors.toSet());
        if (usuariosComPermissao.size() != pastaDTO.usuariosComPermissaoIds().size()) {
            throw new IllegalArgumentException("Um ou mais IDs de usuário fornecidos não são válidos.");
        }

        // Cria e persiste a nova pasta no banco de dados
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.nome());
        novaPasta.setCaminhoCompleto(caminhoPasta.toString());
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setDataAtualizacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuarioLogado);
        novaPasta.setUsuariosComPermissao(usuariosComPermissao);

        if (pastaDTO.pastaPaiId() != null) {
            novaPasta.setPastaPai(pastaRepository.findById(pastaDTO.pastaPaiId()).get());
        }

        return pastaRepository.save(novaPasta);
    }

    // ✅ Novo método para listar as pastas raiz
    @Transactional(readOnly = true)
    public List<Pasta> listarPastasRaiz(Usuario usuarioLogado) {
        List<Pasta> pastas = pastaRepository.findByPastaPaiIsNull();

        if (usuarioLogado.isAdmin()) {
            return pastas;
        }

        // Se o usuário não é um admin, filtra as pastas para mostrar apenas aquelas
        // que ele tem permissão de visualizar
        return pastas.stream()
                .filter(pasta -> pasta.getUsuariosComPermissao().contains(usuarioLogado))
                .collect(Collectors.toList());
    }


}