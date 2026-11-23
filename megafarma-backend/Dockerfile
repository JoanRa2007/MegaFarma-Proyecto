# Imagen del backend Megafarma para desplegar en Render, DigitalOcean u otro servicio.
# Etapa 1: compila el .jar con Maven (incluido en el proyecto como mvnw).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: imagen liviana solo con Java para ejecutar.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Perfil de produccion: la base y la clave JWT llegan por variables de entorno.
ENV SPRING_PROFILES_ACTIVE=prod
# Limita la memoria para planes pequenos (512 MB).
ENV JAVA_TOOL_OPTIONS="-Xmx300m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
