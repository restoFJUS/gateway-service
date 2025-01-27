#Imagen modelo
FROM eclipse-temurin:21.0.5_11-jdk

#Puerto que expondremos
EXPOSE 8080
#Directorio raiz de nuestro contenedor
WORKDIR /app
#Copiamos el archivo jar a la carpeta de trabajo
COPY ./pom.xml /app
COPY ./.mvn /app/.mvn
COPY ./mvnw /app

#Descargamos las dependencias
RUN ./mvnw dependency:go-offline

#Copiamos el resto de archivos
COPY ./src /app/src

#Construir nuestra app
RUN ./mvnw clean install -DskipTests

#Ejecutar nuestra app
ENTRYPOINT ["java","-jar","/app/target/gateway-service-0.0.1-SNAPSHOT.jar"]
