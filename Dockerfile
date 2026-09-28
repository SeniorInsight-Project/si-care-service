# ==============================================================================
# Multi-stage Dockerfile para SeniorInsight backend-core (Java 21 / Maven)
# ==============================================================================

# Estágio 1: Build da aplicação
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache inteligente de dependências do Maven
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Cópia do código-fonte e build do artefato (pulando testes unitários no empacotamento)
COPY src ./src
RUN mvn -B clean package -DskipTests

# Estágio 2: Execução com imagem JRE leve (Alpine Linux)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Criação de usuário não-root para execução segura do contêiner
RUN addgroup -S spring && adduser -S spring -G spring

# Cópia do JAR gerado no estágio anterior
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

# Define usuário não-root
USER spring:spring

# Flags de JVM otimizadas para contêineres e inicialização rápida
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Porta padrão de escuta
EXPOSE 8080

# Entrypoint compatível com substituição de variáveis e portas dinâmicas (Heroku $PORT)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
