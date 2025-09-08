# Etapa de build
# Estágio 1: Construir a aplicação Angular
FROM node:18 AS build

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos de definição de dependências
COPY package.json .
COPY package-lock.json .

# Instala as dependências do projeto
RUN npm install

# Copia todo o código-fonte
COPY . .

# Executa o build de produção do Angular
RUN npm run build -- --output-path=./dist --configuration=production

# Estágio 2: Servir a aplicação com um servidor web leve
FROM nginx:alpine

# Executa o build de produção do Angular
# Copia os arquivos estáticos de build do estágio anterior para o diretório de serviço do Nginx
COPY --from=build /app/dist/browser /usr/share/nginx/html
#COPY --from=build /app/dist /usr/share/nginx/html

# COPY nginx.conf /etc/nginx/nginx.conf

# Expõe a porta que o servidor Nginx vai usar
EXPOSE 80

# Comando padrão do Nginx para iniciar o servidor
CMD ["nginx", "-g", "daemon off;"]
