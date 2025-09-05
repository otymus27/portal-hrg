package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.DTO.ArquivoUpdateDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final PastaRepository pastaRepository;

    @Value("${app.file.upload-dir}")
    private String fileStorageLocation;

    @Autowired
    public ArquivoService(ArquivoRepository arquivoRepository, PastaRepository pastaRepository) {
        this.arquivoRepository = arquivoRepository;
        this.pastaRepository = pastaRepository;
    }


    /**
     * Faz o upload de um único arquivo para uma pasta específica.
     * @param file O arquivo a ser enviado.
     * @param pastaId O ID da pasta de destino.
     * @param usuario O usuário que está fazendo o upload.
     * @return O objeto Arquivo salvo no banco de dados.
     * @throws IOException
     * @throws EntityNotFoundException
     */
    @Transactional
    public ArquivoDTO uploadArquivo(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        Path targetLocation = Paths.get(pasta.getCaminhoCompleto()).resolve(fileName);

        Files.createDirectories(targetLocation.getParent());

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(fileName);
        arquivo.setCaminhoArmazenamento(targetLocation.toAbsolutePath().normalize().toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);

        Arquivo arquivoSalvo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoSalvo);
    }

    /**
     * Faz o upload de múltiplos arquivos para uma pasta específica.
     * @param files A lista de arquivos a serem enviados.
     * @param pastaId O ID da pasta de destino.
     * @param usuario O usuário que está fazendo o upload.
     * @return Uma lista de DTOs dos arquivos salvos no banco de dados.
     * @throws IOException
     * @throws EntityNotFoundException
     */
    @Transactional
    public List<ArquivoDTO> uploadMultiplosArquivos(List<MultipartFile> files, Long pastaId, Usuario usuario) throws IOException {
        return files.stream()
                .map(file -> {
                    try {
                        return uploadArquivo(file, pastaId, usuario);
                    } catch (IOException e) {
                        throw new RuntimeException("Falha ao fazer upload do arquivo: " + file.getOriginalFilename(), e);
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Busca um arquivo por ID.
     * Retorna um Optional que pode conter o arquivo, ou ser vazio se não for encontrado.
     * @param id O ID do arquivo a ser buscado.
     * @return um Optional contendo o arquivo encontrado ou um Optional.empty().
     */
    public Optional<Arquivo> buscarArquivoPorId(Long id) {
        return arquivoRepository.findById(id);
    }

    /**
     * Lista todos os arquivos dentro de uma pasta específica.
     * @param pastaId O ID da pasta.
     * @return Uma lista de DTOs dos arquivos.
     * @throws EntityNotFoundException
     */
    public List<ArquivoDTO> listarArquivosPorPasta(Long pastaId) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));
        return pasta.getArquivos().stream()
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Baixa um arquivo com base no seu ID (para acesso privado/autenticado).
     * @param id O ID do arquivo.
     * @return O recurso de arquivo (Resource).
     * @throws EntityNotFoundException
     * @throws MalformedURLException
     */
    public Resource downloadArquivo(Long id) throws MalformedURLException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        Path filePath = Paths.get(arquivo.getCaminhoArmazenamento()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Não foi possível ler o arquivo!");
        }
    }

    /**
     * Baixa um arquivo com base no seu ID (para acesso público).
     * @param id O ID do arquivo.
     * @return O recurso de arquivo (Resource).
     * @throws EntityNotFoundException
     * @throws MalformedURLException
     */
    public Resource downloadArquivoPublico(Long id) throws MalformedURLException {
        // A lógica de segurança, se houver, deve ser implementada aqui.
        // Por exemplo, checar se o arquivo pertence a uma pasta pública.
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        Path filePath = Paths.get(arquivo.getCaminhoArmazenamento()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Não foi possível ler o arquivo!");
        }
    }

    /**
     * Exclui um arquivo físico e seu registro no banco de dados.
     * @param id O ID do arquivo a ser excluído.
     * @throws IOException
     * @throws EntityNotFoundException
     */
    @Transactional
    public void excluirArquivo(Long id) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        Path caminhoArquivo = Paths.get(arquivo.getCaminhoArmazenamento());
        Files.deleteIfExists(caminhoArquivo);

        arquivoRepository.delete(arquivo);
    }

    /**
     * Exclui múltiplos arquivos por uma lista de IDs.
     * @param arquivoIds Lista de IDs dos arquivos a serem excluídos.
     * @throws IOException
     */
    @Transactional
    public void excluirMultiplosArquivos(List<Long> arquivoIds) throws IOException {
        for (Long id : arquivoIds) {
            excluirArquivo(id);
        }
    }

    /**
     * Exclui todos os arquivos de uma pasta por ID.
     * @param pastaId ID da pasta.
     * @throws IOException
     */
    @Transactional
    public void excluirTodosArquivosNaPasta(Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        for (Arquivo arquivo : pasta.getArquivos()) {
            Path caminhoArquivo = Paths.get(arquivo.getCaminhoArmazenamento());
            Files.deleteIfExists(caminhoArquivo);
        }
        arquivoRepository.deleteAll(pasta.getArquivos());
    }

    /**
     * Atualiza os metadados de um arquivo por ID, incluindo o nome e o caminho físico.
     * @param id ID do arquivo.
     * @param dto DTO com os novos metadados.
     * @return O DTO do arquivo atualizado.
     * @throws IOException Se a operação de renomear o arquivo falhar.
     */
    @Transactional
    public ArquivoDTO atualizarMetadados(Long id, ArquivoUpdateDTO dto) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        if (dto.novoNome() != null && !dto.novoNome().equals(arquivo.getNomeArquivo())) {

            Path caminhoAntigo = Paths.get(arquivo.getCaminhoArmazenamento());
            Path novoCaminho = caminhoAntigo.getParent().resolve(dto.novoNome());

            Files.move(caminhoAntigo, novoCaminho, StandardCopyOption.REPLACE_EXISTING);

            arquivo.setNomeArquivo(dto.novoNome());
            arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        }

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    /**
     * Substitui o conteúdo de um arquivo por uma nova versão.
     * @param id ID do arquivo a ser substituído.
     * @param file O novo arquivo.
     * @return O DTO do arquivo atualizado.
     * @throws IOException
     */
    @Transactional
    public ArquivoDTO substituirArquivo(Long id, MultipartFile file) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        // Passo 1: Obtenha a pasta pai do arquivo original
        // Isso é mais seguro do que usar o caminho completo que pode ter o nome antigo
        Path diretorioPai = Paths.get(arquivo.getCaminhoArmazenamento()).getParent();

        // Passo 2: Construa o novo caminho do arquivo com base no diretório pai e no novo nome
        Path novoCaminhoArquivo = Paths.get(diretorioPai.toString(), file.getOriginalFilename());

        try {
            // Passo 3: Deletar o arquivo antigo, se ele existir
            Files.deleteIfExists(Paths.get(arquivo.getCaminhoArmazenamento()));

            // Passo 4: Copiar o novo arquivo para o novo caminho
            Files.copy(file.getInputStream(), novoCaminhoArquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Aqui você pode adicionar um log mais detalhado para o debug
            System.err.println("Erro ao copiar/excluir arquivo: " + e.getMessage());
            e.printStackTrace();
            throw e; // Relança a exceção para que o controller a capture e retorne 500
        }

        // Passo 5: Atualizar os metadados do arquivo no banco
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setCaminhoArmazenamento(novoCaminhoArquivo.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    /**
     * Move um arquivo para uma nova pasta.
     * @param arquivoId ID do arquivo a ser movido.
     * @param pastaDestinoId ID da pasta de destino.
     * @param usuario O usuário que está realizando a operação.
     * @throws IOException
     * @throws EntityNotFoundException
     */
    @Transactional
    public void moverArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuario) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + arquivoId));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta de destino não encontrada com o ID: " + pastaDestinoId));

        Path caminhoAntigo = Paths.get(arquivo.getCaminhoArmazenamento());
        Path novoCaminho = Paths.get(pastaDestino.getCaminhoCompleto()).resolve(arquivo.getNomeArquivo());

        Files.move(caminhoAntigo, novoCaminho, StandardCopyOption.REPLACE_EXISTING);

        arquivo.setPasta(pastaDestino);
        arquivo.setCaminhoArmazenamento(novoCaminho.toString());
        arquivoRepository.save(arquivo);
    }

    /**
     * Copia um arquivo para uma nova pasta.
     * @param arquivoId ID do arquivo a ser copiado.
     * @param pastaDestinoId ID da pasta de destino.
     * @param usuario O usuário que está realizando a operação.
     * @return O DTO do arquivo copiado.
     * @throws IOException
     * @throws EntityNotFoundException
     */
    @Transactional
    public ArquivoDTO copiarArquivo(Long arquivoId, Long pastaDestinoId, Usuario usuario) throws IOException {
        Arquivo arquivoOriginal = arquivoRepository.findById(arquivoId)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + arquivoId));

        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta de destino não encontrada com o ID: " + pastaDestinoId));

        Path caminhoOrigem = Paths.get(arquivoOriginal.getCaminhoArmazenamento());
        Path caminhoDestino = Paths.get(pastaDestino.getCaminhoCompleto()).resolve(arquivoOriginal.getNomeArquivo());

        Files.copy(caminhoOrigem, caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

        Arquivo novoArquivo = new Arquivo();
        novoArquivo.setNomeArquivo(arquivoOriginal.getNomeArquivo());
        novoArquivo.setCaminhoArmazenamento(caminhoDestino.toString());
        novoArquivo.setTamanhoBytes(arquivoOriginal.getTamanhoBytes());
        novoArquivo.setDataUpload(LocalDateTime.now());
        novoArquivo.setPasta(pastaDestino);
        novoArquivo.setCriadoPor(usuario);

        return ArquivoDTO.fromEntity(arquivoRepository.save(novoArquivo));
    }

    /**
     * Busca arquivos por nome em qualquer parte.
     * @param nome O nome a ser buscado.
     * @param pageable Objeto de paginação e ordenação.
     * @return Uma página de DTOs de arquivos.
     */
    public Page<ArquivoDTO> buscarPorNome(String nome, Pageable pageable) {
        Page<Arquivo> arquivos = arquivoRepository.findByNomeArquivoContainingIgnoreCase(nome, pageable);
        return arquivos.map(ArquivoDTO::fromEntity);
    }
}