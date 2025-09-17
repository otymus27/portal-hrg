# Etapa de build
FROM node:20 AS build

WORKDIR /app

RUN npm cache clean --force

# Copia os arquivos de definição de dependências
COPY package.json package-lock.json ./

# Instala as dependências do projeto
RUN npm ci --legacy-peer-deps

# Copia todo o código-fonte
COPY . .

# Executa o build de produção do Angular
RUN npm run build -- --configuration=production

# Estágio 2: Servir a aplicação com um servidor web leve
FROM nginx:alpine

# Copia os arquivos estáticos de build do estágio anterior
COPY --from=build /app/dist/app-portal /usr/share/nginx/html

# Se quiser sobrescrever a config padrão do Nginx:
COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
