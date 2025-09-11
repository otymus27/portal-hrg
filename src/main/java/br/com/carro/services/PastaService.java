package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.*;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.exceptions.ResourceNotFoundException;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import br.com.carro.utils.AuthService;
import br.com.carro.utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class PastaService {
    private static final Logger logger = LoggerFactory.getLogger(PastaService.class);

    @Autowired
    private ArquivoRepository arquivoRepository;
    private PastaRepository pastaRepository;
    private UsuarioRepository usuarioRepository;
    private AuthService authService;

    @Value("${storage.root-dir}")
    private String rootDirectory;

    // ✅ Use constructor injection
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository, AuthService authService, ArquivoRepository arquivoRepository) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.authService = authService;
        this.arquivoRepository = arquivoRepository;
    }

    // ✅ ENDPOINT 01 - Service para criar pasta raiz ou subpastas
    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO, Usuario usuarioLogado) throws AccessDeniedException {
        if (usuarioLogado == null) {
            throw new SecurityException("Usuário não autenticado.");
        }

        // Busca a pasta pai se existir
        Pasta pastaPai = null;
        if (pastaDTO.pastaPaiId() != null) {
            pastaPai = pastaRepository.findById(pastaDTO.pastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
        }

        // valida permissões (lança AccessDeniedException se não permitido)
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

        Set<Usuario> usuariosComPermissao = new HashSet<>();

        if (pastaDTO.usuariosComPermissaoIds() != null && !pastaDTO.usuariosComPermissaoIds().isEmpty()) {
            // Busca usuários com permissão
            usuariosComPermissao = usuarioRepository.findAllById(pastaDTO.usuariosComPermissaoIds())
                    .stream().collect(Collectors.toSet());

            if (usuariosComPermissao.size() != pastaDTO.usuariosComPermissaoIds().size()) {
                throw new IllegalArgumentException("Um ou mais IDs de usuário fornecidos não são válidos.");
            }
        } else {
            // Nenhum usuário informado → adiciona o usuário logado como padrão
            usuariosComPermissao.add(usuarioLogado);
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

    // ENDPOINT 02 - Método para busca de pastas e arquivos por id
    @Transactional(readOnly = true)
    public PastaCompletaDTO getPastaCompletaPorId(Long idPasta, Usuario usuarioLogado, PastaFilterDTO filtro) {
        Pasta pasta = pastaRepository.findById(idPasta)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        // Permissão: admin ou usuário listado
        if (!usuarioLogado.isAdmin() && !pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new SecurityException("Você não tem permissão para acessar esta pasta.");
        }

        return mapRecursivo(pasta, usuarioLogado, filtro, 0);
    }

    /**
     * Lista todas as pastas visíveis para o usuário logado,
     * aplicando filtros e mapeando recursivamente subpastas e arquivos.
     */
    // ENDPOINT 07 - Lista todas as pastas visíveis para o usuário logado
    @Transactional(readOnly = true)
    public List<PastaCompletaDTO> listarPastasPorUsuario(Usuario usuarioLogado, PastaFilterDTO filtro) {
        // Admin vê todas as pastas raiz, outros apenas as pastas onde tem permissão
        List<Pasta> pastasRaiz;
        if (usuarioLogado.isAdmin()) {
            pastasRaiz = pastaRepository.findByPastaPaiIsNull();
        } else {
            pastasRaiz = pastaRepository.findByPastaPaiIsNullAndUsuariosComPermissaoContains(usuarioLogado);
        }

        return pastasRaiz.stream()
                .map(pasta -> mapRecursivo(pasta, usuarioLogado, filtro, 0))
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


    // --- Métodos para exclusão de pastas e subpastas por id
    @Transactional
    public void excluirPasta(Long pastaId, Usuario usuarioLogado) throws AccessDeniedException {
        if (usuarioLogado == null) {
            throw new AccessDeniedException("Usuário autenticado não foi encontrado.");
        }

        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        // Verificação de permissão
        if (!usuarioLogado.isAdmin()) {
            if (!pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
                throw new AccessDeniedException("Você não tem permissão para excluir esta pasta.");
            }
        }

        // Excluir subpastas recursivamente
        excluirSubPastasRecursivo(pasta);

        // Excluir arquivos da pasta
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path arquivoPath = Paths.get(arquivo.getCaminhoArmazenamento());
            try {
                Files.deleteIfExists(arquivoPath);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao excluir arquivo: " + arquivo.getNomeArquivo(), e);
            }
            // Remover do banco
            arquivoRepository.delete(arquivo);
        }

        // Excluir a pasta do filesystem
        Path caminhoPasta = Paths.get(pasta.getCaminhoCompleto());
        try {
            Files.deleteIfExists(caminhoPasta);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir a pasta: " + pasta.getNomePasta(), e);
        }

        // Excluir a pasta do banco
        pastaRepository.delete(pasta);
    }

    // Método auxiliar para exclusão recursiva
    private void excluirSubPastasRecursivo(Pasta pasta) {
        for (Pasta sub : pasta.getSubPastas()) {
            excluirSubPastasRecursivo(sub);

            // Excluir arquivos da subpasta
            for (Arquivo arquivo : sub.getArquivos()) {
                Path arquivoPath = Paths.get(arquivo.getCaminhoArmazenamento());
                try {
                    Files.deleteIfExists(arquivoPath);
                } catch (IOException e) {
                    throw new RuntimeException("Erro ao excluir arquivo: " + arquivo.getNomeArquivo(), e);
                }
                arquivoRepository.delete(arquivo);
            }

            // Excluir subpasta do filesystem
            Path caminhoSub = Paths.get(sub.getCaminhoCompleto());
            try {
                Files.deleteIfExists(caminhoSub);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao excluir a subpasta: " + sub.getNomePasta(), e);
            }

            pastaRepository.delete(sub);
        }

    }


    @Transactional
    public Pasta renomearPasta(Long pastaId, String novoNome, Usuario usuarioLogado) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        if (!usuarioLogado.isAdmin() && !pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Você não tem permissão para renomear esta pasta.");
        }

        // Caminho atual e novo caminho
        Path caminhoAtual = Paths.get(pasta.getCaminhoCompleto());
        Path caminhoNovo = caminhoAtual.getParent().resolve(FileUtils.sanitizeFileName(novoNome));

        if (Files.exists(caminhoNovo)) {
            throw new IllegalArgumentException("Já existe uma pasta com este nome neste local.");
        }

        try {
            Files.move(caminhoAtual, caminhoNovo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao renomear a pasta no sistema de arquivos.", e);
        }

        // Atualiza banco
        pasta.setNomePasta(novoNome);
        pasta.setCaminhoCompleto(caminhoNovo.toString());
        pasta.setDataAtualizacao(LocalDateTime.now());

        // Atualiza caminhos das subpastas e arquivos recursivamente
        atualizarCaminhoRecursivo(pasta, caminhoNovo);

        return pastaRepository.save(pasta);
    }

    // ✅ ENDPOINT 06 - Service para atualizar campos da pasta raiz ou subpastas
    @Transactional
    public Pasta atualizarPasta(Long pastaId, PastaUpdateDTO pastaDTO, Usuario usuarioLogado) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        // 🔐 Verifica permissão
        if (!usuarioLogado.isAdmin() && !pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Você não tem permissão para atualizar esta pasta.");
        }

        // 📝 Atualiza nome se foi enviado
        if (pastaDTO.nome() != null && !pastaDTO.nome().isBlank()
                && !pastaDTO.nome().equals(pasta.getNomePasta())) {

            Path caminhoAtual = Paths.get(pasta.getCaminhoCompleto());
            Path caminhoNovo = caminhoAtual.getParent().resolve(FileUtils.sanitizeFileName(pastaDTO.nome()));

            if (Files.exists(caminhoNovo)) {
                throw new IllegalArgumentException("Já existe uma pasta com este nome neste local.");
            }

            try {
                Files.move(caminhoAtual, caminhoNovo);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao renomear a pasta no sistema de arquivos.", e);
            }

            pasta.setNomePasta(pastaDTO.nome());
            pasta.setCaminhoCompleto(caminhoNovo.toString());

            // Atualiza caminhos das subpastas e arquivos recursivamente
            atualizarCaminhoRecursivo(pasta, caminhoNovo);
        }

        // 🧑‍🤝‍🧑 Atualiza usuários com permissão (se informado no DTO)
        if (pastaDTO.usuariosComPermissaoIds() != null) {
            Set<Usuario> usuarios = usuarioRepository.findAllById(pastaDTO.usuariosComPermissaoIds())
                    .stream().collect(Collectors.toSet());

            if (usuarios.size() != pastaDTO.usuariosComPermissaoIds().size()) {
                throw new IllegalArgumentException("Um ou mais IDs de usuário fornecidos não são válidos.");
            }

            pasta.setUsuariosComPermissao(usuarios);
        }

        // 🔑 Garante pelo menos um usuário com permissão
        if (pasta.getUsuariosComPermissao() == null || pasta.getUsuariosComPermissao().isEmpty()) {
            pasta.setUsuariosComPermissao(Set.of(usuarioLogado));
        }

        // 📌 Atualiza data de modificação
        pasta.setDataAtualizacao(LocalDateTime.now());

        return pastaRepository.save(pasta);
    }


    private void atualizarCaminhoRecursivo(Pasta pasta, Path novoCaminho) {
        // Atualiza subpastas
        if (pasta.getSubPastas() != null) {
            for (Pasta sub : pasta.getSubPastas()) {
                Path subNovoCaminho = novoCaminho.resolve(sub.getNomePasta());
                sub.setCaminhoCompleto(subNovoCaminho.toString());
                atualizarCaminhoRecursivo(sub, subNovoCaminho);
            }
        }

        // Atualiza arquivos dentro da pasta
        if (pasta.getArquivos() != null) {
            for (Arquivo arq : pasta.getArquivos()) {
                Path arqNovoCaminho = novoCaminho.resolve(arq.getNomeArquivo());
                arq.setCaminhoArmazenamento(arqNovoCaminho.toString());
            }
        }
    }


    @Transactional
    public Pasta moverPasta(Long pastaId, Long novaPastaPaiId, Usuario usuarioLogado) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada."));

        // Permissões
        if (!usuarioLogado.isAdmin() && !pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Você não tem permissão para mover esta pasta.");
        }

        String novoCaminhoPai;
        if (novaPastaPaiId == null) {
            // Tornar a pasta raiz
            pasta.setPastaPai(null);
            novoCaminhoPai = rootDirectory;
        } else {
            Pasta novaPastaPai = pastaRepository.findById(novaPastaPaiId)
                    .orElseThrow(() -> new EntityNotFoundException("Nova pasta pai não encontrada."));

            if (!usuarioLogado.isAdmin() && !novaPastaPai.getUsuariosComPermissao().contains(usuarioLogado)) {
                throw new AccessDeniedException("Você não tem permissão para mover a pasta para este destino.");
            }
            pasta.setPastaPai(novaPastaPai);
            novoCaminhoPai = novaPastaPai.getCaminhoCompleto();
        }

        Path novoCaminho = Paths.get(novoCaminhoPai, FileUtils.sanitizeFileName(pasta.getNomePasta()));
        try {
            Files.move(Paths.get(pasta.getCaminhoCompleto()), novoCaminho, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao mover a pasta no sistema de arquivos.", e);
        }

        pasta.setCaminhoCompleto(novoCaminho.toString());
        pasta.setDataAtualizacao(LocalDateTime.now());

        return pastaRepository.save(pasta);
    }


    private void atualizarCaminhoRecursivo(Pasta pasta, String novoCaminho) {
        pasta.setCaminhoCompleto(novoCaminho);
        if (pasta.getSubPastas() != null) {
            for (Pasta sub : pasta.getSubPastas()) {
                atualizarCaminhoRecursivo(sub, Paths.get(novoCaminho, sub.getNomePasta()).toString());
            }
        }
    }



    // --- COPIAR PASTA --- //

    @Transactional
    public Pasta copiarPasta(Long id, Long idDestino, Usuario usuarioLogado) throws AccessDeniedException {
        logger.info("copiarPasta called: pastaId={}, idDestino={}, usuario={}", id, idDestino, usuarioLogado != null ? usuarioLogado.getUsername() : null);

        Pasta pastaOriginal = pastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pasta original não encontrada"));

        Pasta pastaPaiDestino = null;
        Path caminhoDestino;

        if (idDestino != null) {
            pastaPaiDestino = pastaRepository.findById(idDestino)
                    .orElseThrow(() -> new ResourceNotFoundException("Pasta destino não encontrada"));
            logger.debug("pastaPaiDestino encontrado: id={}, caminhoCompleto={}", pastaPaiDestino.getId(), pastaPaiDestino.getCaminhoCompleto());

            // Normaliza o caminho de destino: se for relativo, resolve contra rootDirectory
            Path possivel = Paths.get(pastaPaiDestino.getCaminhoCompleto());
            if (!possivel.isAbsolute()) {
                caminhoDestino = Paths.get(rootDirectory).resolve(possivel).normalize();
            } else {
                caminhoDestino = possivel.normalize();
            }
        } else {
            // Se não foi informada pasta destino, usa a raiz
            caminhoDestino = Paths.get(rootDirectory).normalize();
            logger.debug("idDestino não informado. Usando rootDirectory: {}", caminhoDestino);
        }

        // Segurança: garante que destino está dentro do rootDirectory
        Path rootPath = Paths.get(rootDirectory).toAbsolutePath().normalize();
        if (!caminhoDestino.toAbsolutePath().normalize().startsWith(rootPath)) {
            throw new IllegalArgumentException("Caminho de destino fora do diretório raiz configurado.");
        }

        // Gera um nome válido para a nova pasta
        String nomeNovaPasta = gerarNomeCopiaDisponivel(pastaOriginal.getNomePasta(), caminhoDestino);
        Path caminhoNovaPasta = caminhoDestino.resolve(FileUtils.sanitizeFileName(nomeNovaPasta)).normalize();

        logger.info("Criando pasta de copia: nomeNovaPasta='{}', caminhoDestino='{}', caminhoNovaPasta='{}'",
                nomeNovaPasta, caminhoDestino, caminhoNovaPasta);

        try {
            Files.createDirectories(caminhoNovaPasta); // cria recursivamente, mais seguro
        } catch (IOException e) {
            logger.error("Erro ao criar nova pasta no FS: {}", caminhoNovaPasta, e);
            throw new RuntimeException("Erro ao criar nova pasta", e);
        }

        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(nomeNovaPasta);
        novaPasta.setCaminhoCompleto(caminhoNovaPasta.toString());
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setDataAtualizacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuarioLogado);
        novaPasta.setUsuariosComPermissao(new HashSet<>(Optional.ofNullable(pastaOriginal.getUsuariosComPermissao()).orElse(Set.of())));
        novaPasta.setPastaPai(pastaPaiDestino);
        novaPasta = pastaRepository.save(novaPasta);

        // Chamada recursiva para copiar conteúdo
        copiarSubpastasEArquivos(pastaOriginal, novaPasta, caminhoNovaPasta, usuarioLogado);

        logger.info("Cópia concluída: novaPasta.id={}, caminho='{}'", novaPasta.getId(), novaPasta.getCaminhoCompleto());
        return novaPasta;
    }

    private void copiarSubpastasEArquivos(Pasta pastaOriginal, Pasta pastaDestino, Path caminhoDestino, Usuario usuarioLogado) {
        logger.debug("copiarSubpastasEArquivos: originalId={}, destinoId={}, caminhoDestino={}",
                pastaOriginal.getId(), pastaDestino.getId(), caminhoDestino);

        // 1) Copiar arquivos da pasta atual
        if (pastaOriginal.getArquivos() != null) {
            for (Arquivo arquivo : pastaOriginal.getArquivos()) {
                Path origemArquivo = Paths.get(arquivo.getCaminhoArmazenamento()).normalize();
                Path destinoArquivo = caminhoDestino.resolve(arquivo.getNomeArquivo()).normalize();

                logger.debug("Copiando arquivo: origem='{}' -> destino='{}'", origemArquivo, destinoArquivo);
                try {
                    // garante diretório pai
                    Files.createDirectories(destinoArquivo.getParent());
                    Files.copy(origemArquivo, destinoArquivo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    logger.error("Erro ao copiar arquivo {} -> {}", origemArquivo, destinoArquivo, e);
                    throw new RuntimeException("Erro ao copiar arquivo " + arquivo.getNomeArquivo(), e);
                }

                // Persistir registro no banco (preenchendo campos obrigatórios)
                Arquivo novoArquivo = new Arquivo();
                novoArquivo.setNomeArquivo(arquivo.getNomeArquivo());
                novoArquivo.setCaminhoArmazenamento(destinoArquivo.toString());
                novoArquivo.setPasta(pastaDestino);
                novoArquivo.setCriadoPor(usuarioLogado);
                novoArquivo.setDataUpload(LocalDateTime.now());
                novoArquivo.setDataAtualizacao(LocalDateTime.now());

                arquivoRepository.save(novoArquivo);
                logger.debug("Arquivo salvo no banco: {}", novoArquivo.getNomeArquivo());
            }
        }

        // 2) Copiar subpastas recursivamente
        if (pastaOriginal.getSubPastas() != null) {
            for (Pasta sub : pastaOriginal.getSubPastas()) {
                // Gera nome disponível no mesmo caminhoDestino (evita colisão)
                String nomeBaseSub = sub.getNomePasta();
                String novoNomeSub = gerarNomeCopiaDisponivel(nomeBaseSub, caminhoDestino);
                Path caminhoSubDestino = caminhoDestino.resolve(FileUtils.sanitizeFileName(novoNomeSub)).normalize();

                logger.debug("Criando subpasta destino: {} -> {}", sub.getNomePasta(), caminhoSubDestino);
                try {
                    Files.createDirectories(caminhoSubDestino);
                } catch (IOException e) {
                    logger.error("Erro ao criar subpasta {}", caminhoSubDestino, e);
                    throw new RuntimeException("Erro ao criar subpasta " + novoNomeSub, e);
                }

                // Persistir subpasta no banco
                Pasta novaSub = new Pasta();
                novaSub.setNomePasta(novoNomeSub);
                novaSub.setCaminhoCompleto(caminhoSubDestino.toString());
                novaSub.setDataCriacao(LocalDateTime.now());
                novaSub.setDataAtualizacao(LocalDateTime.now());
                novaSub.setCriadoPor(usuarioLogado);
                novaSub.setUsuariosComPermissao(new HashSet<>(Optional.ofNullable(sub.getUsuariosComPermissao()).orElse(Set.of())));
                novaSub.setPastaPai(pastaDestino);
                novaSub = pastaRepository.save(novaSub);

                // chamada recursiva
                copiarSubpastasEArquivos(sub, novaSub, caminhoSubDestino, usuarioLogado);
            }
        }
    }




    private boolean temPermissao(Usuario u, Pasta p) {
        return u != null && (u.isAdmin() || p.getUsuariosComPermissao().contains(u));
    }

    private boolean isDescendente(Pasta candidato, Pasta ancestral) {
        Pasta atual = candidato;
        while (atual != null) {
            if (atual.getId().equals(ancestral.getId())) return true;
            atual = atual.getPastaPai();
        }
        return false;
    }

    private String gerarNomeCopiaDisponivel(String baseNome, Path dirPai) {
        String nome = baseNome;
        int i = 1;
        while (Files.exists(dirPai.resolve(FileUtils.sanitizeFileName(nome)))) {
            i++;
            nome = baseNome + " (" + i + ")";
        }
        return nome;
    }



    // ----------------------------------------------------------------//


    // ---EXCLUSÃO DE VARIOS OU TODOS ITENS DA PASTA------------------//
    @Transactional
    public void excluirPastasEmLote(List<Long> idsPastas, boolean excluirConteudo, Usuario usuarioLogado) throws AccessDeniedException {
        if (idsPastas == null || idsPastas.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma pasta foi selecionada para exclusão.");
        }

        for (Long idPasta : idsPastas) {
            Pasta pasta = pastaRepository.findById(idPasta)
                    .orElseThrow(() -> new EntityNotFoundException("Pasta com ID " + idPasta + " não encontrada."));

            // Verificar permissão
            if (!usuarioLogado.isAdmin() && !pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
                throw new AccessDeniedException("Você não tem permissão para excluir a pasta " + pasta.getNomePasta());
            }

            if (excluirConteudo) {
                excluirPastaRecursiva(pasta); // já apaga tudo
            } else {
                if (!pasta.getSubPastas().isEmpty() || !pasta.getArquivos().isEmpty()) {
                    throw new IllegalArgumentException("A pasta '" + pasta.getNomePasta() + "' contém itens. "
                            + "Ative 'excluirConteudo=true' para excluir tudo junto.");
                }
                excluirSomentePasta(pasta);
            }
        }
    }

    /**
     * Exclui recursivamente a pasta, subpastas e arquivos.
     */
    private void excluirPastaRecursiva(Pasta pasta) {
        // Primeiro apagar subpastas
        for (Pasta sub : pasta.getSubPastas()) {
            excluirPastaRecursiva(sub);
        }

        // Apagar arquivos
        for (Arquivo arquivo : pasta.getArquivos()) {
            try {
                Files.deleteIfExists(Paths.get(arquivo.getCaminhoArmazenamento()));
            } catch (IOException e) {
                throw new RuntimeException("Erro ao excluir arquivo: " + arquivo.getNomeArquivo(), e);
            }
            arquivoRepository.delete(arquivo);
        }

        // Excluir diretório físico
        try {
            Files.deleteIfExists(Paths.get(pasta.getCaminhoCompleto()));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir pasta do sistema de arquivos: " + pasta.getNomePasta(), e);
        }

        pastaRepository.delete(pasta);
    }

    /**
     * Exclui apenas a pasta (se estiver vazia).
     */
    private void excluirSomentePasta(Pasta pasta) {
        try {
            Files.deleteIfExists(Paths.get(pasta.getCaminhoCompleto()));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao excluir pasta do sistema de arquivos: " + pasta.getNomePasta(), e);
        }
        pastaRepository.delete(pasta);
    }




    //----------------------------------------------------------------//


    // --- SUBSTITUIÇÃO DE PASTAS ----------------------------------//

    @Transactional
    public Pasta substituirConteudoPasta(Long idOrigem, Long idDestino, Usuario usuarioLogado) throws IOException {
        Pasta pastaOrigem = pastaRepository.findById(idOrigem)
                .orElseThrow(() -> new EntityNotFoundException("Pasta origem não encontrada."));
        Pasta pastaDestino = pastaRepository.findById(idDestino)
                .orElseThrow(() -> new EntityNotFoundException("Pasta destino não encontrada."));

        // Validação de permissão
        if (!usuarioLogado.isAdmin() && !pastaDestino.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Você não tem permissão para substituir esta pasta.");
        }

        // Limpar conteúdo da pasta destino
        FileUtils.deleteDirectory(Paths.get(pastaDestino.getCaminhoCompleto()));
        pastaDestino.getArquivos().clear();
        pastaDestino.getSubPastas().clear();
        pastaRepository.save(pastaDestino);

        // Copiar conteúdo da pasta origem para a pasta destino
        for (Arquivo arquivo : pastaOrigem.getArquivos()) {
            Path destinoArquivo = Paths.get(pastaDestino.getCaminhoCompleto(), arquivo.getNomeArquivo());
            try {
                Files.copy(Paths.get(arquivo.getCaminhoArmazenamento()), destinoArquivo);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao copiar arquivo: " + arquivo.getNomeArquivo(), e);
            }

            // Persistir arquivo no banco, associando à pasta destino
            Arquivo novoArquivo = new Arquivo();
            novoArquivo.setNomeArquivo(arquivo.getNomeArquivo());
            novoArquivo.setCaminhoArmazenamento(destinoArquivo.toString());
            novoArquivo.setDataUpload(LocalDateTime.now());
            novoArquivo.setDataAtualizacao(LocalDateTime.now());
            novoArquivo.setCriadoPor(usuarioLogado);
            novoArquivo.setPasta(pastaDestino);
            arquivoRepository.save(novoArquivo);
            pastaDestino.getArquivos().add(novoArquivo);
        }

        // Copiar subpastas recursivamente
        for (Pasta sub : pastaOrigem.getSubPastas()) {
            Pasta copiaSub = copiarSubPastaRecursiva(sub, pastaDestino, usuarioLogado);
            pastaDestino.getSubPastas().add(copiaSub);
        }

        return pastaRepository.save(pastaDestino);
    }

    private Pasta copiarSubPastaRecursiva(Pasta original, Pasta novaPastaPai, Usuario usuarioLogado) {
        // Cria o nome da nova subpasta
        String nomeNovaSub = original.getNomePasta() + "_copy";
        Path caminhoNovaSub = Paths.get(novaPastaPai.getCaminhoCompleto(), FileUtils.sanitizeFileName(nomeNovaSub));

        try {
            Files.createDirectory(caminhoNovaSub);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar subpasta no sistema de arquivos.", e);
        }

        // Cria a subpasta no banco
        Pasta novaSub = new Pasta();
        novaSub.setNomePasta(nomeNovaSub);
        novaSub.setCaminhoCompleto(caminhoNovaSub.toString());
        novaSub.setDataCriacao(LocalDateTime.now());
        novaSub.setDataAtualizacao(LocalDateTime.now());
        novaSub.setCriadoPor(usuarioLogado);
        novaSub.setUsuariosComPermissao(new HashSet<>(original.getUsuariosComPermissao()));
        novaSub.setPastaPai(novaPastaPai);
        novaSub = pastaRepository.save(novaSub);

        // Copiar arquivos da subpasta
        for (Arquivo arquivo : original.getArquivos()) {
            Path destinoArquivo = caminhoNovaSub.resolve(arquivo.getNomeArquivo());
            try {
                Files.copy(Paths.get(arquivo.getCaminhoArmazenamento()), destinoArquivo);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao copiar arquivo da subpasta: " + arquivo.getNomeArquivo(), e);
            }

            // Persistir arquivo no banco de dados
            Arquivo novoArquivo = new Arquivo();
            novoArquivo.setNomeArquivo(arquivo.getNomeArquivo());
            novoArquivo.setCaminhoArmazenamento(destinoArquivo.toString());
            novoArquivo.setDataUpload(LocalDateTime.now());
            novoArquivo.setDataAtualizacao(LocalDateTime.now());
            novoArquivo.setCriadoPor(usuarioLogado);
            novoArquivo.setPasta(novaSub);
            arquivoRepository.save(novoArquivo);
            novaSub.getArquivos().add(novoArquivo);
        }

        // Recursão para subpastas
        for (Pasta sub : original.getSubPastas()) {
            Pasta copiaSub = copiarSubPastaRecursiva(sub, novaSub, usuarioLogado);
            novaSub.getSubPastas().add(copiaSub);
        }

        return pastaRepository.save(novaSub);
    }


    // ✅ ENDPOINT  - Adicionar e Remover permissão a pastas para usuario
    @Transactional
    public void atualizarPermissoesAcao(Long pastaId, Set<Long> adicionarIds, Set<Long> removerIds, Usuario usuarioLogado)throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada"));

        // Adicionar novos usuários
        if (adicionarIds != null && !adicionarIds.isEmpty()) {
            Set<Usuario> usuariosParaAdicionar = new HashSet<>(usuarioRepository.findAllById(adicionarIds));
            pasta.getUsuariosComPermissao().addAll(usuariosParaAdicionar);
        }

        // Remover usuários
        if (removerIds != null && !removerIds.isEmpty()) {
            Set<Usuario> usuariosParaRemover = new HashSet<>(usuarioRepository.findAllById(removerIds));
            pasta.getUsuariosComPermissao().removeAll(usuariosParaRemover);
        }
    }

    //----------------------------------------------------------------------//





    private void validarPermissaoCriacao(Usuario usuario, Pasta pastaPai) throws AccessDeniedException {
        logger.debug("validarPermissaoCriacao: usuarioId={}, pastaPaiId={}",
                usuario != null ? usuario.getId() : null,
                pastaPai != null ? pastaPai.getId() : null);

        if (usuario == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        // Admin sempre pode
        if (Boolean.TRUE.equals(usuario.isAdmin())) {
            logger.debug("Usuário é admin — permissão concedida.");
            return;
        }

        // Se não for admin, não pode criar na raiz (pastaPai == null)
        if (pastaPai == null) {
            logger.warn("Usuário {} tentou criar pasta raiz sem ser admin", usuario.getUsername());
            throw new AccessDeniedException("Gerentes devem criar pastas dentro de uma pasta existente.");
        }

        // Verifica permissões comparando pelo ID (mais robusto)
        boolean temPermissao = false;
        if (pastaPai.getUsuariosComPermissao() != null) {
            temPermissao = pastaPai.getUsuariosComPermissao().stream()
                    .anyMatch(u -> u != null && u.getId() != null && u.getId().equals(usuario.getId()));
        }

        if (!temPermissao) {
            logger.warn("Usuário {} não tem permissão na pastaPai id={}", usuario.getUsername(), pastaPai.getId());
            throw new AccessDeniedException("Você não tem permissão para criar pastas neste local.");
        }

        logger.debug("Permissão validada: usuário {} pode criar na pasta {}", usuario.getUsername(), pastaPai.getId());
    }


}