# Многостадийная сборка для Maven
FROM eclipse-temurin:21-jdk-alpine AS builder

# Установка Maven
RUN apk add --no-cache maven

# Установка рабочей директории
WORKDIR /app

# Копирование файлов сборки для кэширования зависимостей
COPY pom.xml ./

# Загрузка зависимостей (кэшируется отдельно от исходного кода)
RUN mvn dependency:go-offline -B

# Копирование исходного кода
COPY src/ src/

# Сборка приложения
RUN mvn clean package -DskipTests -B

# Извлечение слоев JAR для оптимизации
RUN java -Djarmode=layertools -jar target/*.jar extract

# Финальный образ
FROM eclipse-temurin:21-jre-alpine

# Создание пользователя для безопасности
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# Установка рабочей директории
WORKDIR /app

# Копирование слоев в порядке изменяемости (от редко к часто)
COPY --from=builder --chown=spring:spring app/dependencies/ ./
COPY --from=builder --chown=spring:spring app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring app/application/ ./

# Переключение на непривилегированного пользователя
USER spring:spring

# Настройка JVM для контейнера
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Открытие порта
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]