package br.com.carro.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_login_audit")
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDateTime dataLogin;

    public LoginAudit() {}

    public LoginAudit(String username) {
        this.username = username;
        this.dataLogin = LocalDateTime.now();
    }

    // Getters e Setters
}
