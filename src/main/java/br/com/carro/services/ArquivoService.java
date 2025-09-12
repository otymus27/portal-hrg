package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.exceptions.ArquivoNaoEncontradoException;
import br.com.carro.exceptions.PermissaoNegadaException;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.utils.ArquivoUtils;
import br.com.carro.utils.FileUtils;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArquivoService {

    @Autowired
    private ArquivoRepository arquivoRepository;
    private PastaRepository pastaRepository;
    private ArquivoUtils fileUtils;

    public ArquivoService(PastaRepository pastaRepository, ArquivoUtils fileUtils, ArquivoRepository arquivoRepository) {
        this.pastaRepository = pastaRepository;
        this.fileUtils = fileUtils;
        this.arquivoRepository = arquivoRepository;
    }

    // RF-016: Upload de arquivo
    public Arquivo uploadArquivo(
            MultipartFile file,
            Long pastaId,
            Usuario usuarioLogado,
            PastaRepository pastaRepository,
            ArquivoRepository arquivoRepository
    ) throws AccessDeniedException {

        if (usuarioLogado == null) {
            throw new SecurityException("Usuário não autenticado.");
        }

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo enviado está vazio.");
        }

        // 1. Buscar pasta de destino
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com ID: " + pastaId));

        // 2. Verificar permissões
        boolean temPermissao = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getNome().equals("ADMIN") || r.getNome().equals("GERENTE"));

        if (!temPermissao) {
            throw new AccessDeniedException("Usuário não tem permissão para enviar arquivos nesta pasta.");
        }

        // 3. Sanitizar nome do arquivo
        String nomeArquivo = FileUtils.sanitizeFileName(file.getOriginalFilename());
        Path destino = Paths.get(pasta.getCaminhoCompleto(), nomeArquivo);

        try {
            // 4. Criar diretório se não existir
            Files.createDirectories(destino.getParent());

            // 5. Salvar arquivo no filesystem
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar o arquivo no sistema de arquivos: " + nomeArquivo, e);
        }

        // 6. Criar registro no banco
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(nomeArquivo);
        arquivo.setCaminhoArmazenamento(destino.toString());
        arquivo.setTipoMime(file.getContentType());
        arquivo.setTamanho(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setDataAtualizacao(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuarioLogado);

        return arquivoRepository.save(arquivo);
    }


    // RF-017: Renomear arquivo
    public ArquivoDTO renomearArquivo(Long arquivoId, String novoNome, Usuario usuarioLogado) throws IOException {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O novo nome do arquivo não pode ser vazio.");
        }

        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com ID: " + arquivoId));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para renomear este arquivo.");
        }

        Path novoCaminho = fileUtils.renomearArquivo(arquivo.getCaminhoArmazenamento(), novoNome);
        arquivo.setNomeArquivo(novoNome);
        arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        arquivo.setDataAtualizacao(LocalDateTime.now());

        arquivo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivo);
    }

    public void excluirArquivo(Long arquivoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com ID: " + arquivoId));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para excluir este arquivo.");
        }

        fileUtils.deleteFile(arquivo.getCaminhoArmazenamento());
        arquivoRepository.delete(arquivo);
    }

    public ArquivoDTO moverArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com ID: " + arquivoId));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta destino não encontrada com ID: " + pastaDestinoId));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)
                || !pastaDestino.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para mover este arquivo.");
        }

        Path novoCaminho = fileUtils.moverArquivo(arquivo.getCaminhoArmazenamento(), pastaDestino.getCaminhoCompleto());
        arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        arquivo.setPasta(pastaDestino);
        arquivo.setDataAtualizacao(LocalDateTime.now());

        arquivo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivo);
    }

    public ArquivoDTO copiarArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com ID: " + arquivoId));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta destino não encontrada com ID: " + pastaDestinoId));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)
                || !pastaDestino.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para copiar este arquivo.");
        }

        Path destino = fileUtils.copiarArquivo(arquivo.getCaminhoArmazenamento(), pastaDestino.getCaminhoCompleto());

        Arquivo copia = new Arquivo();
        copia.setNomeArquivo(arquivo.getNomeArquivo());
        copia.setCaminhoArmazenamento(destino.toString());
        copia.setTipoMime(arquivo.getTipoMime());
        copia.setTamanho(arquivo.getTamanho());
        copia.setDataUpload(LocalDateTime.now());
        copia.setDataAtualizacao(LocalDateTime.now());
        copia.setCriadoPor(usuarioLogado);
        copia.setPasta(pastaDestino);

        copia = arquivoRepository.save(copia);
        return ArquivoDTO.fromEntity(copia);
    }

    @Transactional
    public ArquivoDTO substituirArquivo(Long arquivoId, MultipartFile novoArquivo, Usuario usuarioLogado) throws IOException {
        if (novoArquivo == null || novoArquivo.isEmpty()) {
            throw new IllegalArgumentException("Arquivo enviado está vazio.");
        }

        Arquivo arquivoExistente = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + arquivoId));

        if (!arquivoExistente.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não tem permissão para substituir este arquivo.");
        }

        Path caminhoArquivoAntigo = Paths.get(arquivoExistente.getCaminhoArmazenamento());
        Path diretorioDestino = caminhoArquivoAntigo.getParent();
        String novoNomeArquivo = novoArquivo.getOriginalFilename();
        Path caminhoNovoArquivo = diretorioDestino.resolve(novoNomeArquivo);

        try (InputStream inputStream = novoArquivo.getInputStream()) {
            Files.copy(inputStream, caminhoNovoArquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Erro ao salvar o novo arquivo: " + e.getMessage(), e);
        }

        if (!caminhoArquivoAntigo.equals(caminhoNovoArquivo) && Files.exists(caminhoArquivoAntigo)) {
            try {
                Files.delete(caminhoArquivoAntigo);
            } catch (IOException e) {
                System.err.println("Aviso: Não foi possível remover o arquivo antigo: " + caminhoArquivoAntigo);
            }
        }

        arquivoExistente.setNomeArquivo(novoNomeArquivo);
        arquivoExistente.setCaminhoArmazenamento(caminhoNovoArquivo.toString());
        arquivoExistente.setTamanho(novoArquivo.getSize());
        arquivoExistente.setTipoMime(novoArquivo.getContentType());
        arquivoExistente.setDataAtualizacao(LocalDateTime.now());

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivoExistente);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }


    @Transactional
    public List<ArquivoDTO> excluirArquivosDaPasta(Long pastaId, List<Long> arquivoIds, Usuario usuarioLogado) throws IOException {
        // 1️⃣ Buscar a pasta
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com ID: " + pastaId));

        // 2️⃣ Verificar permissão
        if (!pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para excluir arquivos desta pasta.");
        }

        // 3️⃣ Buscar arquivos para exclusão
        List<Arquivo> arquivosParaExcluir;
        if (arquivoIds == null || arquivoIds.isEmpty()) {
            arquivosParaExcluir = arquivoRepository.findByPastaId(pasta.getId());
        } else {
            arquivosParaExcluir = arquivoRepository.findAllById(arquivoIds)
                    .stream()
                    .filter(a -> a.getPasta().getId().equals(pastaId))
                    .collect(Collectors.toList());
        }

        // 4️⃣ Apagar arquivos do disco
        List<ArquivoDTO> arquivosExcluidos = new ArrayList<>();
        for (Arquivo arquivo : arquivosParaExcluir) {
            Path caminho = Path.of(arquivo.getCaminhoArmazenamento());
            if (Files.exists(caminho)) {
                Files.delete(caminho);
            }
            arquivosExcluidos.add(ArquivoDTO.fromEntity(arquivo));
        }

        // 5️⃣ Deletar registros do banco
        arquivoRepository.deleteAll(arquivosParaExcluir);

        return arquivosExcluidos;
    }

    @Transactional
    public List<ArquivoDTO> uploadArquivos(Long pastaId, List<MultipartFile> arquivos, Usuario usuarioLogado) throws IOException {
        if (arquivos == null || arquivos.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado para upload.");
        }

        // 1️⃣ Buscar a pasta
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com ID: " + pastaId));

        // 2️⃣ Verificar permissão
        if (!pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new AccessDeniedException("Usuário não possui permissão para enviar arquivos para esta pasta.");
        }

        List<ArquivoDTO> arquivosSalvos = new ArrayList<>();

        for (MultipartFile file : arquivos) {
            if (file.isEmpty()) continue;

            // 3️⃣ Salvar fisicamente
            Path destino = fileUtils.salvarArquivo(file, pasta.getCaminhoCompleto());

            // 4️⃣ Criar entidade Arquivo
            Arquivo novoArquivo = new Arquivo();
            novoArquivo.setNomeArquivo(file.getOriginalFilename());
            novoArquivo.setCaminhoArmazenamento(destino.toString());
            novoArquivo.setTipoMime(file.getContentType());
            novoArquivo.setTamanho(file.getSize());
            novoArquivo.setPasta(pasta);
            novoArquivo.setCriadoPor(usuarioLogado);
            novoArquivo.setDataUpload(LocalDateTime.now());
            novoArquivo.setDataAtualizacao(LocalDateTime.now());

            // 5️⃣ Salvar no banco
            novoArquivo = arquivoRepository.save(novoArquivo);

            arquivosSalvos.add(ArquivoDTO.fromEntity(novoArquivo));
        }

        return arquivosSalvos;
    }

    public List<ArquivoDTO> listarArquivos(Pasta pasta, String extensaoFiltro, String ordenarPor, boolean asc) {
        if (pasta == null) {
            throw new IllegalArgumentException("Pasta não pode ser nula.");
        }

        List<Arquivo> arquivos = (List<Arquivo>) pasta.getArquivos();

        return arquivos.stream()
                .filter(a -> extensaoFiltro == null || a.getNomeArquivo().toLowerCase().endsWith(extensaoFiltro.toLowerCase()))
                .sorted(getComparator(ordenarPor, asc))
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private Comparator<Arquivo> getComparator(String ordenarPor, boolean asc) {
        Comparator<Arquivo> comparator;
        switch (ordenarPor.toLowerCase()) {
            case "tamanho":
                comparator = Comparator.comparing(Arquivo::getTamanho);
                break;
            case "data":
                comparator = Comparator.comparing(Arquivo::getDataUpload);
                break;
            case "nome":
            default:
                comparator = Comparator.comparing(Arquivo::getNomeArquivo, String.CASE_INSENSITIVE_ORDER);
        }
        return asc ? comparator : comparator.reversed();
    }

    public Arquivo buscarPorId(Long id)  {
        // Busca o arquivo pelo ID
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com ID: " + id));

        // Retorna o DTO do arquivo
        return arquivo;
    }


    public Page<Arquivo> listarArquivosPorPasta(Long pastaId,
                                                int page,
                                                int size,
                                                String sortField,
                                                String sortDirection,
                                                String extensaoFiltro) {

        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com ID: " + pastaId));

        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, sortField);

        Pageable pageable = PageRequest.of(page, size, sort);

        if (extensaoFiltro != null && !extensaoFiltro.isBlank()) {
            return arquivoRepository.findByPastaAndExtensaoIgnoreCase(pasta, extensaoFiltro, pageable);
        }

        return arquivoRepository.findByPasta(pasta, pageable);
    }

}
