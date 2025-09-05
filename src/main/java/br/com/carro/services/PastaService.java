package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.PastaDTO;
import br.com.carro.entities.DTO.PastaPublicaDTO;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
//import jakarta.transaction.Transactional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PastaService {

    private final PastaRepository pastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArquivoService arquivoService;
    private final ArquivoRepository arquivoRepository;

    @Value("${app.file.upload-dir}")
    private String fileStorageLocation;

    @Autowired
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository, ArquivoService arquivoService,  ArquivoRepository arquivoRepository) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.arquivoService = arquivoService;
        this.arquivoRepository = arquivoRepository;
    }



    public Page<PastaDTO> listarPastasPrincipais(boolean isAdmin, Usuario usuario, Pageable pageable) {
        Page<Pasta> pastas;
        if (isAdmin) {
            pastas = pastaRepository.findByPastaPaiIsNull(pageable);
        } else {
            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId)
                    .collect(Collectors.toSet());
            pastas = pastaRepository.findAllByIdIn(pastasIds, pageable);
        }

        List<PastaDTO> dtoList = pastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, pastas.getTotalElements());
    }

    public Page<PastaDTO> listarSubpastas(Long pastaPaiId, Pageable pageable) {
        pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));

        Page<Pasta> subpastas = pastaRepository.findByPastaPaiId(pastaPaiId, pageable);

        List<PastaDTO> dtoList = subpastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, subpastas.getTotalElements());
    }


    public Pasta buscarPorId(Long id) {
        return pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));
    }

    /**
     * ✅ MÉTODO PRINCIPAL: Cria uma nova pasta e o diretório físico no sistema.
     * Pode ser uma pasta raiz ou uma subpasta, dependendo se o pastaPaiId for fornecido.
     * @param pastaDTO O DTO com as informações da pasta (nome, id do pai, etc.).
     * @param usuario O usuário que está criando a pasta.
     * @return O objeto Pasta criado.
     */
    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO, Usuario usuario) throws AccessDeniedException {
        if (pastaDTO.getNomePasta() == null || pastaDTO.getNomePasta().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da pasta não pode ser vazio.");
        }

        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.getNomePasta());
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuario);

        // 1. Define o pai e o caminho físico
        Path caminhoCompleto;
        if (pastaDTO.getPastaPaiId() != null) {
            Pasta pastaPai = pastaRepository.findById(pastaDTO.getPastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
            // ✅ VERIFICA A PERMISSÃO DE ACESSO
            if (!podeAcessarPasta(pastaPai, usuario)) {
                throw new AccessDeniedException("Você não tem permissão para criar pastas neste local.");
            }
            novaPasta.setPastaPai(pastaPai);
            // Constrói o caminho completo da subpasta
            caminhoCompleto = Paths.get(pastaPai.getCaminhoCompleto()).resolve(pastaDTO.getNomePasta());
        } else {
            // Pasta principal
            caminhoCompleto = Paths.get(fileStorageLocation).resolve(pastaDTO.getNomePasta());
        }

        // 2. Salva o caminho no objeto e tenta criar o diretório físico
        try {
            novaPasta.setCaminhoCompleto(caminhoCompleto.toString());
            Files.createDirectories(caminhoCompleto);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar o diretório físico da pasta: " + e.getMessage(), e);
        }

        // 3. Adiciona permissões se fornecidas
        if (pastaDTO.getUsuariosComPermissaoIds() != null && !pastaDTO.getUsuariosComPermissaoIds().isEmpty()) {
            Set<Usuario> usuariosPermitidos = new HashSet<>(usuarioRepository.findAllById(pastaDTO.getUsuariosComPermissaoIds()));
            novaPasta.setUsuariosComPermissao(usuariosPermitidos);
        }

        // 4. Salva a entidade no banco de dados
        try {
            return pastaRepository.save(novaPasta);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Já existe uma pasta com este nome neste local.", e);
        }
    }

    @Transactional
    public Pasta atualizar(Long id, Pasta pastaAtualizada) throws IOException {
        // 1. Usa o novo método para carregar a pasta e todo o seu conteúdo
        Pasta pastaExistente = pastaRepository.findByIdWithChildrenAndFiles(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));

        // Apenas continua se o nome da pasta for alterado
        if (pastaAtualizada.getNomePasta() == null || pastaAtualizada.getNomePasta().equals(pastaExistente.getNomePasta())) {
            return pastaExistente;
        }

        String nomeAntigo = pastaExistente.getNomePasta();

        // 2. Renomeia o diretório físico PRIMEIRO
        Path caminhoAntigo = Paths.get(pastaExistente.getCaminhoCompleto());
        Path caminhoPai = caminhoAntigo.getParent();
        Path novoCaminho = caminhoPai.resolve(pastaAtualizada.getNomePasta());

        try {
            if (Files.exists(caminhoAntigo)) {
                Files.move(caminhoAntigo, novoCaminho);
            }
        } catch (IOException e) {
            System.err.println("Erro ao renomear a pasta física de '" + nomeAntigo + "' para '" + pastaAtualizada.getNomePasta() + "': " + e.getMessage());
            throw new IOException("Falha ao renomear o diretório físico. A transação será revertida.", e);
        }

        // 3. Atualiza os caminhos no banco de dados, começando pelo pai
        pastaExistente.setNomePasta(pastaAtualizada.getNomePasta());
        pastaExistente.setCaminhoCompleto(novoCaminho.toString());

        // 4. Inicia a atualização recursiva dos filhos no banco de dados
        atualizarCaminhos(pastaExistente);

        // O @Transactional garante que todas as alterações (inclusive dos filhos)
        // sejam salvas de uma vez.
        return pastaRepository.save(pastaExistente);
    }

    /**
     * ✅ Método auxiliar recursivo. Agora ele só precisa atualizar o caminho no banco de dados,
     * pois o diretório físico já foi renomeado de forma atômica no método principal.
     */
    private void atualizarCaminhos(Pasta pasta) {
        // Atualiza os caminhos das subpastas
        for (Pasta subpasta : pasta.getSubpastas()) {
            Path novoCaminho = Paths.get(pasta.getCaminhoCompleto()).resolve(subpasta.getNomePasta());
            subpasta.setCaminhoCompleto(novoCaminho.toString());
            // O `save` é desnecessário aqui, pois o @Transactional fará o "flush" no final
            // mas é bom para ilustrar a intenção.
            pastaRepository.save(subpasta);

            // Chama a si mesmo para as subpastas
            atualizarCaminhos(subpasta);
        }

        // Atualiza os caminhos dos arquivos
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path novoCaminho = Paths.get(pasta.getCaminhoCompleto()).resolve(arquivo.getNomeArquivo());
            arquivo.setCaminhoArmazenamento(novoCaminho.toString());
            // O `save` é desnecessário aqui, pois o @Transactional fará o "flush" no final
            arquivoRepository.save(arquivo);
        }
    }

    /**
     * ✅ NOVO MÉTODO: Deleta uma pasta e todo seu conteúdo (arquivos e subpastas) recursivamente.
     * @param pastaId O ID da pasta a ser deletada.
     */
    @Transactional
    public void excluir(Long pastaId) {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Deleta os arquivos físicos associados a esta pasta e suas subpastas.
        deletarArquivosDePasta(pasta);

        // Deleta a pasta e seus filhos do banco de dados.
        // O cascade type definido na entidade Pasta deve cuidar dos filhos.
        pastaRepository.delete(pasta);
    }

    // --- EXCLUSÃO ----

    /**
     * ✅ NOVO MÉTODO: Exclui uma pasta por ID, incluindo o conteúdo físico e do banco.
     * @param pastaId O ID da pasta a ser excluída.
     */
    @Transactional
    public void excluirPasta(Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // ✅ Adicione esta linha de log para depuração
        System.out.println("--- Pasta a ser excluída: ---");
        System.out.println("ID: " + pasta.getId());
        System.out.println("Nome: " + pasta.getNomePasta());
        System.out.println("Caminho completo: " + pasta.getCaminhoCompleto());
        System.out.println("--- Fim do diagnóstico da pasta ---");
        // ✅ TEMPORÁRIO: Chama o método de diagnóstico para verificar os arquivos
        diagnosticarDelecaoDePasta(pasta);
        // Primeiro, exclui todos os arquivos e subpastas fisicamente e do banco
        deletarConteudoPasta(pasta);

        // Em seguida, exclui a pasta principal do banco de dados
        pastaRepository.delete(pasta);

        // Passo 3: ✅ CORREÇÃO FINAL: Exclui o diretório físico da pasta principal.
        Path caminhoPasta = Paths.get(pasta.getCaminhoCompleto());
        if (Files.exists(caminhoPasta)) {
            Files.deleteIfExists(caminhoPasta);
        }
    }



    /**
     * Método auxiliar recursivo para deletar arquivos físicos e do banco.
     */
    private void deletarArquivosDePasta(Pasta pasta) {
        // Deleta os arquivos da pasta atual
        for (Arquivo arquivo : pasta.getArquivos()) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Apenas loga o erro, mas não interrompe o processo para tentar deletar o restante.
                System.err.println("Erro ao deletar arquivo físico: " + arquivo.getNomeArquivo() + " - " + e.getMessage());
            }
        }

        // Chama a si mesmo para as subpastas
        for (Pasta subpasta : pasta.getSubpastas()) {
            deletarArquivosDePasta(subpasta);
        }
    }



    /**
     * Cria subpastas e arquivos a partir de um upload de diretório.
     * @param files A lista de arquivos com seus caminhos relativos.
     * @param pastaPaiId O ID da pasta principal de destino.
     * @param usuario O usuário logado.
     * @throws IOException Se ocorrer um erro ao salvar o arquivo.
     */
    @Transactional
    public void criarSubpastasEArquivos(MultipartFile[] files, Long pastaPaiId, Usuario usuario) throws IOException {
        Pasta pastaPai = pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));

        for (MultipartFile file : files) {
            String caminhoRelativo = file.getOriginalFilename();

            // ✅ CORREÇÃO: Verifica se o caminho contém uma barra para saber se é uma subpasta
            int ultimoIndiceDaBarra = caminhoRelativo.lastIndexOf('/');

            if (ultimoIndiceDaBarra != -1) {
                // Se tiver uma barra, o arquivo está em uma subpasta.
                String caminhoSomentePasta = caminhoRelativo.substring(0, ultimoIndiceDaBarra);

                Pasta pastaAtual = pastaPai;
                String[] diretorios = caminhoSomentePasta.split("/");

                // Itera sobre o caminho e cria as pastas, se elas não existirem
                for (String nomeDiretorio : diretorios) {
                    pastaAtual = getOrCreatePasta(nomeDiretorio, pastaAtual);
                }

                // Salva o arquivo na subpasta final
                arquivoService.uploadArquivo(file, pastaAtual.getId(), usuario);

            } else {
                // Se não tiver barra, o arquivo está na pasta principal.
                arquivoService.uploadArquivo(file, pastaPai.getId(), usuario);
            }
        }
    }

    /**
     * Busca ou cria uma subpasta dentro de uma pasta pai.
     * @param nomePasta O nome da subpasta.
     * @param pastaPai A pasta pai.
     * @return A subpasta existente ou a recém-criada.
     */
    private Pasta getOrCreatePasta(String nomePasta, Pasta pastaPai) {
        // Tenta encontrar a pasta existente.
        Optional<Pasta> subpastaExistente = pastaRepository.findByNomePastaAndPastaPai(nomePasta, pastaPai);

        if (subpastaExistente.isPresent()) {
            return subpastaExistente.get();
        } else {
            Pasta novaPasta = new Pasta();
            novaPasta.setNomePasta(nomePasta);
            // ✅ CORREÇÃO: Constrói o caminho completo usando a API de Paths
            Path caminhoCompletoDaPastaPai = Paths.get(pastaPai.getCaminhoCompleto());
            Path novoCaminhoCompleto = caminhoCompletoDaPastaPai.resolve(nomePasta);
            novaPasta.setCaminhoCompleto(novoCaminhoCompleto.toAbsolutePath().normalize().toString());
            novaPasta.setPastaPai(pastaPai);
            novaPasta.setDataCriacao(LocalDateTime.now());


            try {
                // Tenta salvar a nova pasta.
                return pastaRepository.save(novaPasta);
            } catch (DataIntegrityViolationException e) {
                // Se a exceção de violação de integridade ocorrer (devido a uma pasta duplicada),
                // busca a pasta que já foi criada por outra thread ou por uma operação anterior.
                return pastaRepository.findByNomePastaAndPastaPai(nomePasta, pastaPai)
                        .orElseThrow(() -> new IllegalStateException("Falha ao criar ou encontrar a pasta após violação de integridade."));
            }
        }
    }

    /**
     * Busca pastas por nome com paginação.
     * @param nome O termo de busca.
     * @param pageable As informações de paginação (número da página, tamanho da página, etc.).
     * @return Uma página de PastaDTOs.
     */
    public Page<PastaDTO> buscarPastasPorNome(String nome, Pageable pageable) {
        Page<Pasta> pastas = pastaRepository.findByNomePastaContainingIgnoreCase(nome, pageable);

        // Converte a Page de Pastas para uma Page de PastaDTOs
        return pastas.map(PastaDTO::fromEntity);
    }


    /**
     * ✅ NOVO MÉTODO: Cria um arquivo ZIP de uma pasta e seus conteúdos.
     * @param pastaId O ID da pasta a ser compactada.
     * @return O caminho do arquivo ZIP temporário.
     * @throws IOException se ocorrer um erro de I/O.
     */
    public Path downloadPasta(Long pastaId) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Cria um arquivo ZIP temporário para o download
        Path tempZipFile = Files.createTempFile(pasta.getNomePasta() + "_", ".zip");

        // ✅ CORREÇÃO: Registra o arquivo temporário para ser deletado na saída da JVM
        tempZipFile.toFile().deleteOnExit();

        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(tempZipFile))) {
            adicionarPastaAoZip(pasta, zipOut, "");
        }

        return tempZipFile;
    }

    /**
     * Método recursivo para adicionar a pasta e seus conteúdos ao arquivo ZIP.
     * @param pasta A pasta atual a ser adicionada.
     * @param zipOut O stream de saída do arquivo ZIP.
     * @param caminhoRelativo O caminho relativo da pasta dentro do ZIP.
     * @throws IOException
     */
    private void adicionarPastaAoZip(Pasta pasta, ZipOutputStream zipOut, String caminhoRelativo) throws IOException {
        String nomeEntrada = caminhoRelativo.isEmpty() ? pasta.getNomePasta() + "/" : caminhoRelativo + pasta.getNomePasta() + "/";

        // Adiciona a entrada da própria pasta (útil para pastas vazias)
        ZipEntry zipEntry = new ZipEntry(nomeEntrada);
        zipOut.putNextEntry(zipEntry);
        zipOut.closeEntry();

        // Adiciona todos os arquivos da pasta ao ZIP
        for (Arquivo arquivo : pasta.getArquivos()) {
            Path arquivoPath = Paths.get(arquivo.getCaminhoArmazenamento());

            // ✅ CORREÇÃO: Verifica se o arquivo existe antes de tentar adicioná-lo
            if (Files.exists(arquivoPath)) {
                String nomeEntradaArquivo = nomeEntrada + arquivo.getNomeArquivo();
                zipOut.putNextEntry(new ZipEntry(nomeEntradaArquivo));
                Files.copy(arquivoPath, zipOut);
                zipOut.closeEntry();
            }
        }

        // Adiciona todas as subpastas de forma recursiva
        for (Pasta subpasta : pasta.getSubpastas()) {
            adicionarPastaAoZip(subpasta, zipOut, nomeEntrada);
        }
    }

    // ----UPLOAD DE PASTA E ARQUIVOS------


    @Transactional
    public void uploadDiretorioComArquivos(MultipartFile[] files, Long pastaPaiId, Usuario usuario) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Nenhum arquivo para upload.");
        }

        // 1. Encontra a pasta pai
        Pasta pastaPai = pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));

        // 2. Extrai o nome da pasta raiz do upload a partir do primeiro arquivo
        String caminhoRelativoPrimeiroArquivo = files[0].getOriginalFilename();
        Path caminhoRaiz = Paths.get(caminhoRelativoPrimeiroArquivo);
        String nomePastaRaiz = caminhoRaiz.getName(0).toString();

        // 3. Encontra ou cria a pasta raiz do upload no banco de dados
        Pasta pastaRaizUpload = encontrarOuCriarPastaAninhada(pastaPai, nomePastaRaiz, usuario);

        // 4. Itera sobre cada arquivo e salva na estrutura correta
        for (MultipartFile file : files) {
            String caminhoRelativoCompleto = file.getOriginalFilename();
            if (caminhoRelativoCompleto == null || caminhoRelativoCompleto.isEmpty()) {
                continue;
            }

            Path caminhoDoArquivo = Paths.get(caminhoRelativoCompleto);
            String nomeArquivo = caminhoDoArquivo.getFileName().toString();
            Path caminhoPastaRelativo = caminhoDoArquivo.getParent();

            // ✅ CORREÇÃO FINAL: Cria um loop seguro para navegar pela hierarquia de pastas.
            Pasta pastaParaSalvarArquivo = pastaRaizUpload;
            if (caminhoPastaRelativo != null) {
                // Itera sobre as partes do caminho, começando da segunda parte (índice 1)
                for (int i = 1; i < caminhoPastaRelativo.getNameCount(); i++) {
                    String nomeSubpasta = caminhoPastaRelativo.getName(i).toString();
                    pastaParaSalvarArquivo = encontrarOuCriarPastaAninhada(pastaParaSalvarArquivo, nomeSubpasta, usuario);
                }
            }

            // Salva o arquivo no sistema de arquivos e no banco de dados.
            salvarArquivo(file, pastaParaSalvarArquivo, nomeArquivo, usuario);
        }
    }

    /**
     * ✅ Sanitiza o nome do arquivo para evitar problemas com caracteres inválidos no Windows.
     */
    private String sanitizarNomeArquivo(String nomeArquivo) {
        // Substitui caracteres inválidos por "_"
        return nomeArquivo.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * ✅ Encontra ou cria a pasta correta para o arquivo.
     */
    private Pasta encontrarPastaParaArquivo(Pasta pastaPai, Path caminhoPastaRelativo, Usuario usuario) {
        Pasta pastaAtual = pastaPai;

        if (caminhoPastaRelativo != null) {
            for (Path parteDoCaminho : caminhoPastaRelativo) {
                pastaAtual = encontrarOuCriarPastaAninhada(pastaAtual, sanitizarNomeArquivo(parteDoCaminho.toString()), usuario);
            }
        }
        return pastaAtual;
    }

    /**
     * ✅ Salva um único arquivo no sistema de arquivos e no banco.
     */
    private void salvarArquivo(MultipartFile file, Pasta pastaParaSalvar, String nomeArquivo, Usuario usuario) throws IOException {
        Path caminhoFisicoCompleto = Paths.get(pastaParaSalvar.getCaminhoCompleto()).resolve(nomeArquivo);

        // Garante que a pasta existe antes de salvar
        Files.createDirectories(caminhoFisicoCompleto.getParent());

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, caminhoFisicoCompleto, StandardCopyOption.REPLACE_EXISTING);
        }

        // Salva a entidade do arquivo no banco
        Arquivo novoArquivo = new Arquivo();
        novoArquivo.setNomeArquivo(nomeArquivo);
        novoArquivo.setCaminhoArmazenamento(caminhoFisicoCompleto.toString());
        novoArquivo.setTamanhoBytes(file.getSize());
        novoArquivo.setDataUpload(LocalDateTime.now());
        novoArquivo.setPasta(pastaParaSalvar);
        novoArquivo.setCriadoPor(usuario);

        arquivoRepository.save(novoArquivo);
    }

    /**
     * ✅ Encontra ou cria uma subpasta no banco e no sistema de arquivos.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Pasta encontrarOuCriarPastaAninhada(Pasta pastaPai, String nomeSubpasta, Usuario usuario) {
        Optional<Pasta> subpastaOpt = pastaRepository.findByPastaPaiAndNomePasta(pastaPai, nomeSubpasta);
        if (subpastaOpt.isPresent()) {
            return subpastaOpt.get();
        }

        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(nomeSubpasta);
        novaPasta.setPastaPai(pastaPai);
        novaPasta.setDataCriacao(LocalDateTime.now());
        novaPasta.setCriadoPor(usuario);

        Path caminhoCompleto = Paths.get(pastaPai.getCaminhoCompleto(), nomeSubpasta);
        novaPasta.setCaminhoCompleto(caminhoCompleto.toString());

        try {
            Files.createDirectories(caminhoCompleto);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao criar o diretório físico para a subpasta.", e);
        }

        return pastaRepository.save(novaPasta);
    }

// ---FIM UPLOAD DE PASTA E ARQUIVOS----------

    // ---MOVER PASTA PARA OUTRO LOCAL

    @Transactional
    public PastaDTO moverPasta(Long pastaId, Long pastaDestinoId, Usuario usuario) throws IOException {
        // 1. Obtém a pasta de origem e verifica as permissões
        Pasta pastaParaMover = getPastaComAcesso(pastaId, usuario);

        // 2. Obtém a pasta de destino e verifica as permissões
        Pasta pastaDestino = getPastaComAcesso(pastaDestinoId, usuario);

        // 3. Verifica se a pasta de destino não é a pasta de origem ou uma subpasta dela
        if (isDescendantOf(pastaDestino, pastaParaMover)) {
            throw new IllegalArgumentException("Não é possível mover uma pasta para dentro de uma de suas subpastas.");
        }

        // 4. Define os caminhos físico da pasta de origem e de destino
        Path caminhoOrigem = Paths.get(pastaParaMover.getCaminhoCompleto());
        Path caminhoDestino = Paths.get(pastaDestino.getCaminhoCompleto(), pastaParaMover.getNomePasta());

        // 5. Mover fisicamente a pasta
        Files.move(caminhoOrigem, caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

        // 6. Atualiza a referência da pasta pai no banco de dados e o novo caminho
        pastaParaMover.setPastaPai(pastaDestino);
        pastaParaMover.setCaminhoCompleto(caminhoDestino.toString());

        // 7. Salva a atualização no banco de dados
        Pasta pastaAtualizada = pastaRepository.save(pastaParaMover);

        // Opcional: Atualizar caminhos de subpastas e arquivos, se necessário
        // (Isso pode ser complexo e depender de como você modelou o caminho)

        return PastaDTO.fromEntity(pastaAtualizada);
    }

    //--- Métodos Auxiliares ---

    // Método para obter a pasta com verificação de acesso
    private Pasta getPastaComAcesso(Long pastaId, Usuario usuario) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));
        verificarAcessoEPermitir(pasta, usuario);
        return pasta;
    }

    // Método para verificar se uma pasta é descendente de outra
    private boolean isDescendantOf(Pasta pasta, Pasta ancestral) {
        Pasta current = pasta;
        while (current != null) {
            if (current.equals(ancestral)) {
                return true;
            }
            current = current.getPastaPai();
        }
        return false;
    }

    // ---FIM MOVER PASTA PARA OUTRO LOCAL


    // --- COPIAR PASTA PARA OUTRA PASTA

    @Transactional
    public PastaDTO copiarPasta(Long pastaId, Long pastaDestinoId, Usuario usuario) throws IOException {
        // 1. Obtém a pasta de origem e a pasta de destino
        Pasta pastaParaCopiar = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta de origem não encontrada."));
        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta de destino não encontrada."));

        // 2. Cria a nova pasta no banco de dados e no disco
        Pasta novaPastaRaiz = new Pasta();
        novaPastaRaiz.setNomePasta(pastaParaCopiar.getNomePasta());
        novaPastaRaiz.setCriadoPor(usuario);
        novaPastaRaiz.setCaminhoCompleto(Paths.get(pastaDestino.getCaminhoCompleto(), pastaParaCopiar.getNomePasta()).toString());
        novaPastaRaiz.setPastaPai(pastaDestino);
        novaPastaRaiz.setDataCriacao(LocalDateTime.now());
        Pasta pastaSalva = pastaRepository.save(novaPastaRaiz);

        // 3. Inicia a cópia recursiva
        copiarConteudo(pastaParaCopiar, pastaSalva, usuario);

        return PastaDTO.fromEntity(pastaSalva);
    }

    // Método auxiliar recursivo
    private void copiarConteudo(Pasta pastaOrigem, Pasta pastaDestino, Usuario usuario) throws IOException {
        // 1. Copia arquivos
        List<Arquivo> arquivos = arquivoRepository.findByPasta(pastaOrigem);

        for (Arquivo arquivoOrigem : arquivos) {
            // --- LOGS PARA DEBUG INÍCIO ---
            System.out.println("DEBUG: Iniciando cópia do arquivo: " + arquivoOrigem.getNomeArquivo());
            Path caminhoOrigem = Paths.get(arquivoOrigem.getCaminhoArmazenamento());
            Path caminhoDestino = Paths.get(pastaDestino.getCaminhoCompleto(), arquivoOrigem.getNomeArquivo());
            System.out.println("DEBUG: Caminho de Origem: " + caminhoOrigem.toAbsolutePath());
            System.out.println("DEBUG: Caminho de Destino: " + caminhoDestino.toAbsolutePath());
            System.out.println("DEBUG: Arquivo de Origem existe? " + Files.exists(caminhoOrigem));
            // --- LOGS PARA DEBUG FIM ---

            try {
                // ADICIONE ESTA LINHA: Garante que a pasta de destino do arquivo exista.
                Files.createDirectories(caminhoDestino.getParent());

                // Garante que o arquivo de destino seja excluído se ele já existir e estiver acessível
                if (Files.exists(caminhoDestino)) {
                    System.out.println("DEBUG: Arquivo de destino já existe. Excluindo...");
                    Files.deleteIfExists(caminhoDestino);
                }

                Files.copy(caminhoOrigem, caminhoDestino, StandardCopyOption.REPLACE_EXISTING);

                // Cria um novo registro de arquivo no banco de dados
                Arquivo novoArquivo = new Arquivo();
                novoArquivo.setNomeArquivo(arquivoOrigem.getNomeArquivo());
                novoArquivo.setCaminhoArmazenamento(caminhoDestino.toString());
                novoArquivo.setTamanhoBytes(arquivoOrigem.getTamanhoBytes());
                novoArquivo.setDataUpload(LocalDateTime.now());
                novoArquivo.setPasta(pastaDestino);
                novoArquivo.setCriadoPor(usuario);
                arquivoRepository.save(novoArquivo);

            } catch (IOException e) {
                // MUDE A MENSAGEM DE ERRO para obter o detalhe
                System.err.println("ERRO CRÍTICO: Falha na operação de cópia do arquivo.");
                e.printStackTrace(); // Isso vai imprimir a stack trace completa
                throw new RuntimeException("Erro ao copiar o arquivo: " + arquivoOrigem.getNomeArquivo() + " - " + e.getMessage(), e);
            }
        }

        // 2. Copia subpastas (recursivamente)
        List<Pasta> subpastas = pastaRepository.findByPastaPai(pastaOrigem);

        for (Pasta subpastaOrigem : subpastas) {
            try {
                // Cria nova subpasta no disco
                Path caminhoDestinoSubpasta = Paths.get(pastaDestino.getCaminhoCompleto(), subpastaOrigem.getNomePasta());
                Files.createDirectories(caminhoDestinoSubpasta);

                // Cria novo registro de subpasta no banco de dados
                Pasta novaSubpasta = new Pasta();
                novaSubpasta.setNomePasta(subpastaOrigem.getNomePasta());
                novaSubpasta.setCriadoPor(usuario);
                novaSubpasta.setCaminhoCompleto(caminhoDestinoSubpasta.toString());
                novaSubpasta.setPastaPai(pastaDestino);
                novaSubpasta.setDataCriacao(LocalDateTime.now());
                Pasta subpastaSalva = pastaRepository.save(novaSubpasta);

                // Chama a função recursivamente para copiar o conteúdo da subpasta
                copiarConteudo(subpastaOrigem, subpastaSalva, usuario);
            } catch (IOException e) {
                throw new RuntimeException("Erro ao copiar a pasta: " + subpastaOrigem.getNomePasta(), e);
            }
        }

    }

    // --- FIM COPIAR PASTA PARA OUTRA PASTA






    /**
     * ✅ MÉTODO PRINCIPAL PARA DELETAR O CONTEÚDO DE UMA PASTA.
     * Esta é a versão final e funcional, garantindo a exclusão recursiva
     * de todos os arquivos e subpastas do sistema e do banco de dados.
     * @param pasta A pasta cujo conteúdo será deletado.
     */
    private void deletarConteudoPasta(Pasta pasta) throws IOException {
// Deleta todos os arquivos da pasta atual (entidades e físicos)
        for (Arquivo arquivo : List.copyOf(pasta.getArquivos())) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Erro ao deletar arquivo físico: " + arquivo.getNomeArquivo() + " - " + e.getMessage());
            }
            arquivoRepository.delete(arquivo);
        }

        // Deleta as subpastas de forma recursiva (da folha para a raiz)
        for (Pasta subpasta : List.copyOf(pasta.getSubpastas())) {
            deletarConteudoPasta(subpasta); // Chama recursivamente para deletar o conteúdo da subpasta

            // Depois de deletar o conteúdo, apaga a própria subpasta
            try {
                Path folderPath = Paths.get(subpasta.getCaminhoCompleto());
                Files.deleteIfExists(folderPath);
            } catch (IOException e) {
                System.err.println("Erro ao deletar pasta física: " + subpasta.getNomePasta() + " - " + e.getMessage());
            }
            pastaRepository.delete(subpasta);
        }

        // Limpa as coleções para evitar referências persistentes do Hibernate
        pasta.getArquivos().clear();
        pasta.getSubpastas().clear();
    }

    /**
     * ✅ NOVO MÉTODO AUXILIAR: Deleta arquivos físicos e suas entidades.
     */
    private void deletarArquivosEEntidades(Pasta pasta) {
        // Itera sobre uma cópia para evitar ConcurrentModificationException
        for (Arquivo arquivo : List.copyOf(pasta.getArquivos())) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Erro ao deletar arquivo físico: " + arquivo.getNomeArquivo() + " - " + e.getMessage());
            }
        }
        // Deleta todas as entidades de arquivos do banco de dados de uma vez
        arquivoRepository.deleteAll(pasta.getArquivos());
    }

    /**
     * ✅ MÉTODO PARA SUBSTITUIR UMA PASTA EXISTENTE POR UMA NOVA VERSÃO.
     * Esta é a versão corrigida para tratar o problema de caminho duplicado.
     *
     * @param pastaDestinoId O ID da pasta existente a ser substituída.
     * @param files Os novos arquivos enviados para a substituição.
     * @param usuario O usuário que está realizando a operação.
     */
    @Transactional
    public void substituirPasta(Long pastaDestinoId, MultipartFile[] files, Usuario usuario) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Nenhum arquivo para upload.");
        }

        // 1. Encontra a pasta que será substituída
        Pasta pastaDestino = pastaRepository.findById(pastaDestinoId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta de destino não encontrada."));

        // 2. Extrai o nome da pasta raiz do novo upload a partir do primeiro arquivo
        String caminhoRelativoPrimeiroArquivo = files[0].getOriginalFilename();
        Path caminhoRaiz = Paths.get(caminhoRelativoPrimeiroArquivo);
        String nomePastaRaiz = caminhoRaiz.getName(0).toString();

        // 3. Deleta o conteúdo da pasta existente no back-end
        deletarConteudoPasta(pastaDestino);

        // 4. Itera sobre os novos arquivos do upload para criar a nova estrutura.
        for (MultipartFile file : files) {
            String caminhoRelativoCompleto = file.getOriginalFilename();
            if (caminhoRelativoCompleto == null || caminhoRelativoCompleto.isEmpty()) {
                continue;
            }

            Path caminhoDoArquivo = Paths.get(caminhoRelativoCompleto);
            String nomeArquivo = caminhoDoArquivo.getFileName().toString();
            Path caminhoPastaRelativo = caminhoDoArquivo.getParent();

            // ✅ CORREÇÃO: Cria um loop seguro para navegar pela hierarquia de pastas.
            Pasta pastaParaSalvarArquivo = pastaDestino;
            if (caminhoPastaRelativo != null) {
                // Itera sobre as partes do caminho, começando da segunda parte (índice 1)
                for (int i = 1; i < caminhoPastaRelativo.getNameCount(); i++) {
                    String nomeSubpasta = caminhoPastaRelativo.getName(i).toString();
                    pastaParaSalvarArquivo = encontrarOuCriarPastaAninhada(pastaParaSalvarArquivo, nomeSubpasta, usuario);
                }
            }

            // Salva o arquivo (substitui se já existir, mas aqui estamos recriando)
            salvarArquivo(file, pastaParaSalvarArquivo, nomeArquivo, usuario);
        }
    }

    /**
     * ✅ MÉTODO DE DIAGNÓSTICO TEMPORÁRIO:
     * Verifica as propriedades do arquivo antes de tentar deletá-lo.
     * NÃO USE EM PRODUÇÃO.
     */
    private void diagnosticarDelecaoDePasta(Pasta pasta) {
        System.out.println("--- Diagnóstico de Deleção para a Pasta: " + pasta.getNomePasta() + " ---");

        for (Arquivo arquivo : List.copyOf(pasta.getArquivos())) {
            try {
                Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());

                System.out.println("  > Verificando arquivo: " + arquivo.getNomeArquivo());
                System.out.println("    - Caminho do banco de dados: " + arquivo.getCaminhoArmazenamento());
                System.out.println("    - Caminho normalizado: " + filePath.toAbsolutePath().normalize());
                System.out.println("    - Arquivo existe? " + Files.exists(filePath));
                System.out.println("    - Posso escrever/deletar? " + Files.isWritable(filePath));

                // Tenta a deleção e imprime o resultado
                boolean deletado = Files.deleteIfExists(filePath);
                System.out.println("    - Tentativa de deleção: " + (deletado ? "Sucesso" : "Falha"));

            } catch (Exception e) {
                System.out.println("    - ERRO AO DELETAR: " + e.getMessage());
            }
        }

        for (Pasta subpasta : pasta.getSubpastas()) {
            diagnosticarDelecaoDePasta(subpasta);
        }

        System.out.println("--- Fim do Diagnóstico ---");
    }


    // Métodos auxiliares de permissão
    private boolean podeAcessarPasta(Pasta pasta, Usuario usuario) {
        if (usuario.getRoles().stream().anyMatch(role -> role.getNome().equals("ADMIN"))) {
            return true;
        }
        if (pasta == null) {
            return false;
        }
        if (pasta.getUsuariosComPermissao().contains(usuario)) {
            return true;
        }
        if (pasta.getPastaPai() != null) {
            return podeAcessarPasta(pasta.getPastaPai(), usuario);
        }
        if (usuario.getPastasPrincipaisAcessadas().contains(pasta)) {
            return true;
        }
        return false;
    }

    private void verificarAcessoEPermitir(Pasta pasta, Usuario usuario) throws AccessDeniedException {
        if (!podeAcessarPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para acessar esta pasta.");
        }
    }


    //Métodos para testes locais apagar depois
    /**
     * ✅ MÉTODO DE TESTE TEMPORÁRIO E DEFINITIVO
     * Cria e tenta deletar um arquivo para verificar o caminho de deleção.
     */

    public void testarCaminhoDelecao() {
        System.out.println("--- Teste de Caminho de Deleção ---");

        Path pastaTemporaria = Paths.get(fileStorageLocation);
        Path arquivoTeste = pastaTemporaria.resolve(UUID.randomUUID().toString() + "_teste.txt");

        try {
            // Tenta criar o arquivo
            Files.createFile(arquivoTeste);
            System.out.println("1. Arquivo de teste criado com sucesso em: " + arquivoTeste.toAbsolutePath().normalize());

            // Tenta deletar o mesmo arquivo imediatamente
            boolean deletado = Files.deleteIfExists(arquivoTeste);
            System.out.println("2. O arquivo de teste foi deletado? " + (deletado ? "Sim" : "Não"));

            if (Files.exists(arquivoTeste)) {
                System.out.println("3. O arquivo AINDA EXISTE, algo está impedindo a deleção.");
            } else {
                System.out.println("3. O arquivo NÃO EXISTE, a deleção funcionou. O problema está no caminho do banco de dados.");
            }
        } catch (IOException e) {
            System.out.println("ERRO: Ocorreu uma exceção de I/O: " + e.getMessage());
        }

        System.out.println("--- Fim do Teste ---");
    }


    public List<PastaPublicaDTO> findPastasTopLevelDTO() {
        return pastaRepository.findByPastaPaiIsNull().stream()
                .map(pasta -> new PastaPublicaDTO(pasta.getId(), pasta.getNomePasta(), pasta.getCaminhoCompleto()))
                .collect(Collectors.toList());
    }

    public List<PastaPublicaDTO> findSubpastasByPastaPaiIdDTO(Long pastaPaiId) {
        return pastaRepository.findByPastaPaiId(pastaPaiId).stream()
                .map(pasta -> new PastaPublicaDTO(pasta.getId(), pasta.getNomePasta(), pasta.getCaminhoCompleto()))
                .collect(Collectors.toList());
    }
}