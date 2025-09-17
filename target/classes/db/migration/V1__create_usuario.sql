--
-- Script de Migração do Banco de Dados para o Gerenciador de Arquivos Web
--
-- NOTA: Este script é idempotente. Ele utiliza 'CREATE TABLE IF NOT EXISTS' para
-- evitar erros caso as tabelas já existam.
--

-- Criação da tabela de roles
CREATE TABLE IF NOT EXISTS tb_roles (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nome VARCHAR(255) NOT NULL UNIQUE
    );



-- Criação da tabela de usuários
-- Adicionado 'data_criacao', 'data_atualizacao' e 'nome_completo'
CREATE TABLE IF NOT EXISTS tb_usuarios (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE,
    nome_completo VARCHAR(150) NOT NULL,
    data_criacao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    data_atualizacao DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    );

-- Criação da tabela de relacionamento entre usuários e roles
CREATE TABLE IF NOT EXISTS tb_usuarios_roles (
                                                 user_id BIGINT NOT NULL,
                                                 role_id BIGINT NOT NULL,
                                                 PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES tb_usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
    );

-- Tabela de Pastas
CREATE TABLE IF NOT EXISTS tb_pasta (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        nome_pasta VARCHAR(255) NOT NULL,
    caminho_completo VARCHAR(1024) NOT NULL,
    data_criacao DATETIME(6) NOT NULL,
    data_atualizacao DATETIME(6) NOT NULL,
    is_publico BOOLEAN NOT NULL DEFAULT TRUE,
    pasta_pai_id BIGINT,
    criado_por_id BIGINT,
    CONSTRAINT fk_subpastas_pasta_pai FOREIGN KEY (pasta_pai_id) REFERENCES tb_pasta (id),
    CONSTRAINT fk_pastas_usuarios FOREIGN KEY (criado_por_id) REFERENCES tb_usuarios (id)
    );

-- Tabela de Arquivos
CREATE TABLE IF NOT EXISTS tb_arquivo (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          nome_arquivo VARCHAR(255) NOT NULL,
    caminho_armazenamento VARCHAR(1024) NOT NULL,
    tamanho_bytes BIGINT,
    data_upload DATETIME(6) NOT NULL,
    data_atualizacao DATETIME(6) NOT NULL,
    is_publico BOOLEAN NOT NULL DEFAULT TRUE,
    hash_arquivo VARCHAR(64),
    tipo_mime VARCHAR(100),
    pasta_id BIGINT,
    criado_por_id BIGINT,
    CONSTRAINT fk_arquivos_pastas FOREIGN KEY (pasta_id) REFERENCES tb_pasta (id),
    CONSTRAINT fk_arquivos_usuarios FOREIGN KEY (criado_por_id) REFERENCES tb_usuarios (id)
    );

-- Tabela de relacionamento entre usuários e as pastas principais que eles podem acessar
CREATE TABLE IF NOT EXISTS tb_permissao_pasta (
                                                  usuario_id BIGINT NOT NULL,
                                                  pasta_id BIGINT NOT NULL,
                                                  PRIMARY KEY (usuario_id, pasta_id),
    FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id),
    FOREIGN KEY (pasta_id) REFERENCES tb_pasta(id)
    );

--
-- Inserção de dados iniciais
--

-- Inserção de roles
INSERT INTO tb_roles (nome) VALUES
                                ('ADMIN'),
                                ('BASIC'),
                                ('GERENTE')
    ON DUPLICATE KEY UPDATE nome = nome;

-- Inserção de usuários
INSERT INTO tb_usuarios (username, password, senha_provisoria, nome_completo) VALUES
                                                                                  ('admin',    '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE, 'Administrador do Sistema'),
                                                                                  ('gabriel',  '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE,  'Gabriel Almeida'),
                                                                                  ('beatriz',  '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE, 'Beatriz Silva'),
                                                                                  ('14329301', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE,  'Usuário 14329301'),
                                                                                  ('usuario5', '$2a$10$ADqjEwM1joxBvl0ivQiqK3odF2gGbzRslfvtnwTqfmRbx11P0RHgi', FALSE, 'Usuário Cinco')
    ON DUPLICATE KEY UPDATE
                         password = VALUES(password),
                         nome_completo = VALUES(nome_completo);

-- Inserção de relacionamento usuário ↔ role
INSERT INTO tb_usuarios_roles (user_id, role_id) VALUES
                                                     (1, (SELECT id FROM tb_roles WHERE nome = 'ADMIN')),
                                                     (2, (SELECT id FROM tb_roles WHERE nome = 'BASIC')),
                                                     (3, (SELECT id FROM tb_roles WHERE nome = 'GERENTE')),
                                                     (4, (SELECT id FROM tb_roles WHERE nome = 'GERENTE')),
                                                     (5, (SELECT id FROM tb_roles WHERE nome = 'BASIC'))
    ON DUPLICATE KEY UPDATE user_id = user_id;

-- Criação da tabela de auditoria de usuarios logados
CREATE TABLE tb_login_audit (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                username VARCHAR(100) NOT NULL,
                                data_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);