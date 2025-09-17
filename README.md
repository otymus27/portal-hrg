# 🚀 Deploy - API Portal (Spring Boot + MySQL + Docker)

Este projeto contém a API do Portal, construída em **Spring Boot 3.5.4**, rodando em **Docker** com banco de dados **MySQL 5.7** e armazenamento persistente de arquivos.

---

## 📦 Requisitos

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/)

---

## ⚙️ Configuração

### 1. Arquivo `.env`

Crie um arquivo `.env` na raiz do projeto com as variáveis de ambiente:

```env
# Spring
SPRING_PROFILES_ACTIVE=prod

# JWT
JWT_SECRET=MinhaChaveSuperSecretaEmProd123

# Database
DB_HOST=mysql
DB_PORT=3306
DB_NAME=db_portal
DB_USER=portal
DB_PASSWORD=SenhaMuitoSegura
MYSQL_ROOT_PASSWORD=root

# Storage
STORAGE_PATH=/app/storage

/projeto
 ├── api/                 # Código da aplicação Spring Boot
 ├── storage/             # ⬅️ Arquivos/pastas enviados via API
 ├── docker-compose.yml   # Orquestração dos containers
 └── .env                 # Configurações de ambiente

Docker Compose

O docker-compose.yml já está preparado com:

API Spring Boot em http://localhost:8082

MySQL exposto em localhost:3307 (porta do host) → útil para Workbench/DBeaver

Armazenamento persistente em ./storage (no host)

▶️ Como rodar
1. Buildar e subir containers
docker-compose up --build -d

2. Verificar se está rodando
docker ps

3. Logs da aplicação
docker logs api-portal -f

4. Logs do banco de dados
docker logs mysql -f

🗂️ Armazenamento de Arquivos

Os arquivos enviados via API são gravados em /app/storage no container.

Essa pasta está mapeada para ./storage no host:

/projeto/storage/
 ├── uploads/
 ├── relatorios/
 └── ...


Se você remover os containers, os arquivos continuam no host.