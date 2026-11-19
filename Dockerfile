# Etapa 1 (build): compila o projeto com Maven, gerando o .jar executável.
# Usa a imagem com o JDK completo (precisa do compilador), mas ela nunca vai pra imagem final.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copia só o necessário pro Maven resolver as dependências primeiro - assim, se só o
# código em src/ mudar (não o pom.xml), o Docker reaproveita essa camada já em cache,
# sem baixar tudo de novo a cada build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Etapa 2 (runtime): imagem final, só com o JRE (sem compilador, bem menor) e o .jar
# já pronto copiado da etapa de build - nada de código-fonte ou do Maven vai pra cá.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
