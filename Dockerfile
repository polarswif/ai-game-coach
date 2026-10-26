# How the server is packaged for hosting (Render builds this). Two stages: the first has Maven and
# the full JDK to build the jar; the second keeps only a Java runtime and the jar, so the image the
# host runs is small.

# ---- Build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---- Run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/gamecoach-*.jar app.jar
# Free hosts give about 512 MB. Let the JVM use most of it for the heap, with small thread stacks and
# the simplest garbage collector (best for one small CPU). AWT runs headless for the image code.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -Djava.awt.headless=true"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
