//package br.com.carro.controllers;
//
//import br.com.carro.entities.DTO.DashboardDTO;
//import br.com.carro.services.EstatisticasService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDate;
//
//@RestController
//@RequestMapping("/api/estatisticassss")
////@RequiredArgsConstructor
//public class EstatisticasController {
//
//    private final EstatisticasService estatisticasService;
//
//    @GetMapping
//    public ResponseEntity<DashboardDTO> getEstatisticas(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim
//    ) {
//        return ResponseEntity.ok(estatisticasService.obterEstatisticas(inicio, fim));
//    }
//}
