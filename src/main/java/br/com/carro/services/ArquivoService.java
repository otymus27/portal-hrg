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
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    public Arquivo uploadArquivo(MultipartFile file, Long pastaId, Usuario usuarioLogado, PastaRepository pastaRepository, ArquivoRepository arquivoRepository) {
        try {
            // 1. Buscar pasta de destino
            Pasta pasta = pastaRepository.findById(pastaId)
                    .orElseThrow(() -> new RuntimeException("Pasta não encontrada com ID: " + pastaId));

            // 2. Verificar permissões
            if (!usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMIN") || r.getNome().equals("GERENTE"))) {
                throw new RuntimeException("Usuário não tem permissão para enviar arquivos nesta pasta.");
            }

            // 3. Sanitizar nome do arquivo
            String nomeArquivo = FileUtils.sanitizeFileName(file.getOriginalFilename());
            Path destino = Paths.get(pasta.getCaminhoCompleto(), nomeArquivo);

            // 4. Criar diretório se não existir
            Files.createDirectories(destino.getParent());

            // 5. Salvar arquivo no filesystem
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

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

        } catch (IOException e) {
            throw new RuntimeException("Erro ao fazer upload do arquivo: " + file.getOriginalFilename(), e);
        }
    }


    // RF-017: Renomear arquivo
    public ArquivoDTO renomearArquivo(Long arquivoId, String novoNome, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new RuntimeException("Usuário não possui permissão para este arquivo");
        }

        Path novoCaminho = fileUtils.renomearArquivo(arquivo.getCaminhoArmazenamento(), novoNome);
        arquivo.setNomeArquivo(novoNome);
        arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        arquivo.setDataAtualizacao(LocalDateTime.now());

        arquivo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivo);
    }

    // RF-018: Excluir arquivo
    public void excluirArquivo(Long arquivoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new RuntimeException("Usuário não possui permissão para excluir este arquivo");
        }

        fileUtils.deleteFile(arquivo.getCaminhoArmazenamento());
        arquivoRepository.delete(arquivo);
    }


    // RF-019: Mover arquivo
    public ArquivoDTO moverArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new RuntimeException("Pasta destino não encontrada"));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)
                || !pastaDestino.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new RuntimeException("Usuário não possui permissão para mover este arquivo");
        }

        Path novoCaminho = fileUtils.moverArquivo(arquivo.getCaminhoArmazenamento(), pastaDestino.getCaminhoCompleto());
        arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        arquivo.setPasta(pastaDestino);
        arquivo.setDataAtualizacao(LocalDateTime.now());

        arquivo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivo);
    }

    // RF-020: Copiar arquivo
    public ArquivoDTO copiarArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuarioLogado) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado"));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new RuntimeException("Pasta destino não encontrada"));

        if (!arquivo.getPasta().getUsuariosComPermissao().contains(usuarioLogado)
                || !pastaDestino.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new RuntimeException("Usuário não possui permissão para copiar este arquivo");
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


    // RF-021: Substituir arquivo por uma nova versão, atualizando nome se necessário
    @Transactional
    public ArquivoDTO substituirArquivo(Long arquivoId, MultipartFile novoArquivo, Usuario usuarioLogado) throws IOException {
        System.out.println("Service: iniciando substituição de arquivoId=" + arquivoId);

        // 1. Buscar arquivo existente
        Arquivo arquivoExistente = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new ArquivoNaoEncontradoException("Arquivo não encontrado com o ID: " + arquivoId));
        System.out.println("Arquivo encontrado: " + arquivoExistente.getNomeArquivo());

        // 2. Verificar permissão
        if (!arquivoExistente.getPasta().getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new PermissaoNegadaException("Usuário não tem permissão para substituir este arquivo.");
        }

        // 3. Obter o caminho completo do arquivo a ser substituído
        Path caminhoArquivoAntigo = Paths.get(arquivoExistente.getCaminhoArmazenamento());
        Path diretorioDestino = caminhoArquivoAntigo.getParent();

        // 4. Determinar o novo nome e o novo caminho
        String novoNomeArquivo = novoArquivo.getOriginalFilename();
        Path caminhoNovoArquivo = diretorioDestino.resolve(novoNomeArquivo);

        // 5. Mover o arquivo para o novo caminho, sobrescrevendo se já existir
        // Usamos Files.move para lidar com a possível mudança de nome
        try (InputStream inputStream = novoArquivo.getInputStream()) {
            Files.copy(inputStream, caminhoNovoArquivo, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Arquivo substituído com sucesso em: " + caminhoNovoArquivo);
        } catch (IOException e) {
            throw new IOException("Erro ao salvar o novo arquivo: " + e.getMessage(), e);
        }

        // 6. Se o nome do arquivo mudou, o arquivo antigo precisa ser removido
        if (!caminhoArquivoAntigo.equals(caminhoNovoArquivo) && Files.exists(caminhoArquivoAntigo)) {
            try {
                Files.delete(caminhoArquivoAntigo);
                System.out.println("Arquivo antigo removido: " + caminhoArquivoAntigo);
            } catch (IOException e) {
                System.err.println("Aviso: Não foi possível remover o arquivo antigo: " + caminhoArquivoAntigo);
                // Continua a operação mesmo se a remoção falhar
            }
        }

        // 7. Atualizar os metadados do arquivo no banco de dados
        arquivoExistente.setNomeArquivo(novoNomeArquivo); // Adicionado para atualizar o nome
        arquivoExistente.setCaminhoArmazenamento(caminhoNovoArquivo.toString()); // Adicionado para atualizar o caminho
        arquivoExistente.setTamanho(novoArquivo.getSize());
        arquivoExistente.setTipoMime(novoArquivo.getContentType());
        arquivoExistente.setDataAtualizacao(LocalDateTime.now());

        // 8. Salvar as alterações no banco de dados
        Arquivo arquivoAtualizado = arquivoRepository.save(arquivoExistente);
        System.out.println("Metadados do arquivo atualizados no banco de dados. ID: " + arquivoAtualizado.getId());

        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    /**
     * RF-022: Excluir múltiplos arquivos de uma pasta
     *
     * @param pastaId ID da pasta
     * @param arquivoIds Lista de IDs de arquivos a excluir. Se null ou vazia, exclui todos.
     * @param usuarioLogado Usuário que realiza a exclusão
     */
    @Transactional
    public List<ArquivoDTO> excluirArquivosDaPasta(Long pastaId, List<Long> arquivoIds, Usuario usuarioLogado) throws IOException {
        // 1. Buscar pasta
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new RuntimeException("Pasta não encontrada"));

        // 2. Verificar permissão do usuário na pasta
        if (!pasta.getUsuariosComPermissao().contains(usuarioLogado)) {
            throw new RuntimeException("Usuário não possui permissão para excluir arquivos desta pasta");
        }

        // 3. Determinar arquivos a excluir
        List<Arquivo> arquivosParaExcluir;
        if (arquivoIds == null || arquivoIds.isEmpty()) {
            arquivosParaExcluir = pasta.getArquivos().stream().toList(); // todos os arquivos da pasta
        } else {
            arquivosParaExcluir = new ArrayList<>();
            for (Long id : arquivoIds) {
                Optional<Arquivo> a = arquivoRepository.findById(id);
                a.ifPresent(arquivo -> {
                    if (arquivo.getPasta().getId().equals(pastaId)) {
                        arquivosParaExcluir.add(arquivo);
                    }
                });
            }
        }

        List<ArquivoDTO> arquivosExcluidos = new ArrayList<>();

        // 4. Excluir arquivos
        for (Arquivo arquivo : arquivosParaExcluir) {
            Path caminho = Path.of(arquivo.getCaminhoArmazenamento());
            try {
                if (Files.exists(caminho)) {
                    Files.delete(caminho);
                }
            } catch (IOException e) {
                throw new IOException("Erro ao excluir arquivo: " + arquivo.getNomeArquivo(), e);
            }

            arquivoRepository.delete(arquivo);
            arquivosExcluidos.add(ArquivoDTO.fromEntity(arquivo));
        }

        return arquivosExcluidos;
    }


    /**
     * Lista arquivos de uma pasta com filtro por extensão e ordenação.
     *
     * @param pasta pasta de onde listar os arquivos
     * @param extensaoFiltro extensão (ex: "pdf") ou null para todas
     * @param ordenarPor "nome", "tamanho" ou "data" (dataUpload)
     * @param asc true = ascendente, false = descendente
     * @return lista de ArquivoDTO
     */
    public List<ArquivoDTO> listarArquivos(Pasta pasta, String extensaoFiltro, String ordenarPor, boolean asc) {

        List<Arquivo> arquivos = (List<Arquivo>) pasta.getArquivos(); // pega a lista do banco

        return arquivos.stream()
                // filtro por extensão, se fornecida
                .filter(a -> extensaoFiltro == null
                        || a.getNomeArquivo().toLowerCase().endsWith(extensaoFiltro.toLowerCase()))
                // ordenação
                .sorted(getComparator(ordenarPor, asc))
                // converte para DTO
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

    // Método para buscar arquivo por ID
    public Arquivo buscarPorId(Long id) {
        return arquivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Arquivo não encontrado com ID: " + id));
    }
}
