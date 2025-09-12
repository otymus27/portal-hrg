package br.com.carro.exceptions;

import java.time.LocalDateTime;

public class ErrorMessage {
    private LocalDateTime timestamp;
    private int status;
    private String erro;
    private String mensagem;
    private String path;

    public ErrorMessage(int status, String erro, String mensagem, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getErro() {
        return erro;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getPath() {
        return path;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setErro(String erro) {
        this.erro = erro;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
