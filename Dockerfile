# ==========================================
# Stage 1: Maven Builder
# ==========================================
FROM harbor.harbor.svc.cluster.local/dockerhub-proxy/library/maven:3.9-eclipse-temurin-21 AS builder

# 設定 Maven 使用 Nexus 代理
RUN mkdir -p /root/.m2 && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"><mirrors><mirror><id>nexus</id><mirrorOf>*</mirrorOf><url>http://nexus-nexus3.nexus.svc.cluster.local:8081/repository/maven-public/</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

WORKDIR /build

# 複製 Maven 配置檔案
COPY pom.xml .

# 下載依賴（利用 Docker 快取層）
RUN mvn dependency:go-offline -B || true

# 複製原始碼
COPY src ./src

# 編譯打包（跳過測試以加快建置速度）
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Production Runtime
# ==========================================
FROM harbor.harbor.svc.cluster.local/dockerhub-proxy/library/eclipse-temurin:21-jre-jammy

WORKDIR /app

# 建立非 root 使用者
RUN groupadd -r appuser && useradd -r -g appuser -u 1001 appuser

# 從 builder 階段複製編譯好的 JAR
COPY --from=builder /build/target/*.jar app.jar

# 變更檔案擁有者
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
