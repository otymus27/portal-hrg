//package br.com.carro.services;
//
//import br.com.carro.entities.Arquivo;
//import br.com.carro.entities.DTO.DashboardDTO;
//import br.com.carro.repositories.ArquivoRepository;
//import br.com.carro.repositories.PastaRepository;
//import br.com.carro.repositories.UsuarioRepository;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class EstatisticasService {
//
//    private final ArquivoRepository arquivoRepository;
//    private final PastaRepository pastaRepository;
//    private final UsuarioRepository usuarioRepository;
//
//    @Transactional
//    public DashboardDTO obterEstatisticas(LocalDate inicio, LocalDate fim) {
//
//        List<Arquivo> arquivos = arquivoRepository.findAll();
//
//        if (inicio != null && fim != null) {
//            LocalDateTime startDateTime = inicio.atStartOfDay();
//            LocalDateTime endDateTime = fim.plusDays(1).atStartOfDay().minusSeconds(1);
//
//            arquivos = arquivos.stream()
//                    .filter(a -> a.getDataUpload().isAfter(startDateTime) &&
//                            a.getDataUpload().isBefore(endDateTime))
//                    .toList();
//        }
//
//        long totalArquivos = arquivos.size();
//        long totalPastas = pastaRepository.count();
//        long totalEspacoBytes = arquivos.stream()
//                .mapToLong(Arquivo::getTamanho)
//                .sum();
//
//        double totalEspacoMB = bytesParaMB(totalEspacoBytes);
//        double totalEspacoGB = bytesParaGB(totalEspacoBytes);
//
//        Map<LocalDate, Long> uploadsPorDia = arquivos.stream()
//                .collect(Collectors.groupingBy(a -> a.getDataUpload().toLocalDate(), Collectors.counting()));
//
//        Map<String, Long> topUsuariosPorUpload = arquivos.stream()
//                .collect(Collectors.groupingBy(a -> a.getCriadoPor().getUsername(), Collectors.counting()))
//                .entrySet().stream()
//                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
//                .limit(5)
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//
//        Map<String, Long> topUsuariosPorEspaco = arquivos.stream()
//                .collect(Collectors.groupingBy(a -> a.getCriadoPor().getUsername(), Collectors.summingLong(Arquivo::getTamanho)))
//                .entrySet().stream()
//                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
//                .limit(5)
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//
//        Map<String, Long> distribuicaoPorTipo = arquivos.stream()
//                .collect(Collectors.groupingBy(Arquivo::getTipoMime, Collectors.counting()));
//
//        Map<String, Long> topTiposPorEspaco = arquivos.stream()
//                .collect(Collectors.groupingBy(Arquivo::getTipoMime, Collectors.summingLong(Arquivo::getTamanho)))
//                .entrySet().stream()
//                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
//                .limit(5)
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (e1, e2) -> e1,
//                        LinkedHashMap::new
//                ));
//
//        return new DashboardDTO(
//                totalArquivos,
//                totalPastas,
//                totalEspacoBytes,
//                totalEspacoMB,
//                totalEspacoGB,
//                uploadsPorDia,
//                topUsuariosPorUpload,
//                topUsuariosPorEspaco,
//                distribuicaoPorTipo,
//                topTiposPorEspaco,
//                usuariosAtivosAgora,
//                usuariosLogaramHoje
//
//        );
//    }
//
//    private double bytesParaMB(long bytes) {
//        return Math.round((bytes / 1024.0 / 1024.0) * 100.0) / 100.0;
//    }
//
//    private double bytesParaGB(long bytes) {
//        return Math.round((bytes / 1024.0 / 1024.0 / 1024.0) * 100.0) / 100.0;
//    }
//}
