# ==========================================
# Stage 1: Maven Builder
# ==========================================
FROM eclipse-temurin:21-jdk-jammy AS builder

# 安裝 Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

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
FROM eclipse-temurin:21-jre-jammy

# 設定工作目錄
WORKDIR /app

# 建立非 root 使用者
RUN groupadd -r appuser && useradd -r -g appuser -u 1001 appuser

# 從 builder 階段複製編譯好的 JAR
COPY --from=builder /build/target/*.jar app.jar

# 變更檔案擁有者
RUN chown -R appuser:appuser /app

# 切換到非 root 使用者
USER appuser

# 暴露應用程式端口
EXPOSE 8080

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 參數優化
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 啟動應用程式
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
