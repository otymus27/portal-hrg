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
-- Adicionado 'data_criacao' e 'data_atualizacao' para rastreamento
CREATE TABLE IF NOT EXISTS tb_usuarios (
                                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                           username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE,
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
-- Adicionado 'data_atualizacao' e 'is_publica'
-- 'caminho_completo' teve o tamanho aumentado para acomodar hierarquias profundas
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
-- Adicionado 'data_atualizacao', 'hash_arquivo' e 'tipo_mime'
-- 'caminho_armazenamento' teve o tamanho aumentado para acomodar caminhos longos
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
    ON DUPLICATE KEY UPDATE nome = nome; -- Evita erro caso já existam


-- Inserção de usuários
INSERT INTO tb_usuarios (username, password, senha_provisoria) VALUES
                                                                   ('admin', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE),  -- senha: senha123
                                                                   ('gabriel', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE),  -- senha: senha123
                                                                   ('beatriz', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE), -- senha: senha123
                                                                   ('14329301', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE),   -- senha: senha123
                                                                   ('usuario5', '$2a$10$ADqjEwM1joxBvl0ivQiqK3odF2gGbzRslfvtnwTqfmRbx11P0RHgi', FALSE)  -- senha: senha202
    ON DUPLICATE KEY UPDATE password = password;


-- Inserção de relacionamento usuário ↔ role
INSERT INTO tb_usuarios_roles (user_id, role_id) VALUES
                                                     (1, (SELECT id FROM tb_roles WHERE nome = 'ADMIN')),  -- admin com ADMIN
                                                     (2, (SELECT id FROM tb_roles WHERE nome = 'BASIC')),  -- gabriel com BASIC
                                                     (3, (SELECT id FROM tb_roles WHERE nome = 'GERENTE')),  -- beatriz com GERENTE
                                                     (4, (SELECT id FROM tb_roles WHERE nome = 'GERENTE')),  -- usuario4 com GERENTE
                                                     (5, (SELECT id FROM tb_roles WHERE nome = 'BASIC'))  -- usuario5 com BASIC
    ON DUPLICATE KEY UPDATE user_id = user_id;


-- Inserção de pastas principais
-- Algumas pastas são marcadas como públicas
INSERT INTO tb_pasta (nome_pasta, caminho_completo, data_criacao, data_atualizacao, pasta_pai_id, criado_por_id, is_publico) VALUES
                                                                                                                                 ('Relatorios-Financeiros', '/Financeiro/Relatorios-Financeiros', NOW(), NOW(), NULL, 1, TRUE),
                                                                                                                                 ('Campanhas-2025', '/Marketing/Campanhas-2025', NOW(), NOW(), NULL, 1, TRUE),
                                                                                                                                 ('Docs-RH', '/RH/Docs-RH', NOW(), NOW(), NULL, 1, FALSE),
                                                                                                                                 ('Projetos-TI', '/TI/Projetos-TI', NOW(), NOW(), NULL, 1, FALSE),
                                                                                                                                 ('Contratos', '/Juridico/Contratos', NOW(), NOW(), NULL, 1, FALSE)
    ON DUPLICATE KEY UPDATE nome_pasta = nome_pasta;


-- Subpastas
INSERT INTO tb_pasta (nome_pasta, caminho_completo, data_criacao, data_atualizacao,is_publico, pasta_pai_id) VALUES
                                                                                                      ('Relatorio de Vendas - 2024', '/Financeiro/Relatorios-Financeiros/Relatorio de Vendas', NOW(), NOW(), true, 1),
                                                                                                      ('Relatorios Mensais', '/Financeiro/Relatorios-Financeiros/Relatorios Mensais', NOW(), NOW(), true, 1),
                                                                                                      ('Relatorios Anuais', '/Financeiro/Relatorios-Financeiros/Relatorios Anuais', NOW(), NOW(), true, 1),
                                                                                                      ('Relatorios', '/TI/Projetos-TI/Relatorios', NOW(), NOW(), false, 4),
                                                                                                      ('Documentacao', '/TI/Projetos-TI/Documentacao', NOW(), NOW(), true, 4),
                                                                                                      ('Backups', '/TI/Projetos-TI/Backups', NOW(), NOW(), false, 4),
                                                                                                      ('Manuais', '/RH/Docs-RH/Manuais', NOW(), NOW(), true,3),
                                                                                                      ('Procedimentos', '/RH/Docs-RH/Procedimentos', NOW(), NOW(), true,3),
                                                                                                      ('Formularios', '/RH/Docs-RH/Formularios', NOW(), NOW(), true, 3)
    ON DUPLICATE KEY UPDATE nome_pasta = nome_pasta;


-- Arquivos nas pastas principais
INSERT INTO tb_arquivo (nome_arquivo, caminho_armazenamento, tamanho_bytes, data_upload, data_atualizacao,is_publico, pasta_id, criado_por_id, hash_arquivo, tipo_mime) VALUES
                                                                                                                                                                 ('Relatorio_de_Vendas.pdf', '/caminho/servidor/vendas.pdf', 10240, NOW(), NOW(), true,1, 2, 'abc123456789def', 'application/pdf'),
                                                                                                                                                                 ('Plano_de_Midia.pdf', '/caminho/servidor/midia.pdf', 5120, NOW(), NOW(), true,2, 4, 'def987654321abc', 'application/pdf'),
                                                                                                                                                                 ('Manual_do_Funcionario.pdf', '/caminho/servidor/manual.pdf', 8192, NOW(), NOW(), false,3, 3, 'xyz123abc456def', 'application/pdf'),
                                                                                                                                                                 ('Especificacoes_Sistema.pdf', '/caminho/servidor/specs.pdf', 20480, NOW(), NOW(), true,4, 1, 'qwe123rty456uio', 'application/pdf'),
                                                                                                                                                                 ('Minuta_de_Acordo.pdf', '/caminho/servidor/acordo.pdf', 4096, NOW(), NOW(), true, 5, 5, 'lmn456opq789rst', 'application/pdf')
    ON DUPLICATE KEY UPDATE nome_arquivo = nome_arquivo;


-- Arquivos nas subpastas
INSERT INTO tb_arquivo (nome_arquivo, caminho_armazenamento, tamanho_bytes, data_upload, data_atualizacao, is_publico, pasta_id, criado_por_id, hash_arquivo, tipo_mime) VALUES
                                                                                                                                                                 ('Backup_DB.zip', '/caminho/servidor/backup_db.zip', 102400, NOW(), NOW(), true,11, 1, '6j7k8l9m0n1o2p', 'application/zip'),
                                                                                                                                                                 ('Manual_Recursos_Humanos.pdf', '/caminho/servidor/manual_rh.pdf', 4096, NOW(), NOW(),true, 12, 3, '7p8q9r0s1t2u3v', 'application/pdf'),
                                                                                                                                                                 ('Procedimentos_Internos.pdf', '/caminho/servidor/procedimentos.pdf', 2048, NOW(), NOW(), false,3, 3, '8w9x0y1z2a3b4c', 'application/pdf'),
                                                                                                                                                                 ('Formularios_Contratacao.pdf', '/caminho/servidor/formularios.pdf', 1024, NOW(), NOW(), true,14, 3, '9d0e1f2g3h4i5j', 'application/pdf')
    ON DUPLICATE KEY UPDATE nome_arquivo = nome_arquivo;


-- Define quais usuários têm acesso direto a quais pastas principais
INSERT INTO tb_permissao_pasta (usuario_id, pasta_id) VALUES
                                                          (1, 1), -- Admin tem acesso à pasta Financeiro
                                                          (1, 2), -- Admin tem acesso à pasta Marketing
                                                          (1, 3), -- Admin tem acesso à pasta RH
                                                          (1, 4), -- Admin tem acesso à pasta TI
                                                          (1, 5), -- Admin tem acesso à pasta Jurídico
                                                          (2, 1), -- Gabriel (BASIC) tem acesso à pasta Financeiro
                                                          (3, 2), -- Beatriz (GERENTE) tem acesso à pasta Marketing
                                                          (4, 3), -- Usuario4 (GERENTE) tem acesso à pasta RH
                                                          (5, 5)  -- Usuario5 (BASIC) tem acesso à pasta Jurídico
    ON DUPLICATE KEY UPDATE usuario_id = usuario_id;
