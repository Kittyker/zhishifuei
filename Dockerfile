# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
#FROM bellsoft/liberica-openjdk-rocky:17.0.15-cds
#FROM bellsoft/liberica-openjdk-rocky:21.0.7-cds
#FROM findepi/graalvm:java17-native
FROM crpi-747o03p8n9vfx887.cn-shanghai.personal.cr.aliyuncs.com/mileso33/zhishifufei:jdk17-cds

LABEL maintainer="Lion Li"

# 创建工作目录（精简路径，避免多层嵌套）
WORKDIR /app

# 环境变量（集中定义，便于维护）
ENV SERVER_PORT=8080 \
    SNAIL_PORT=28080 \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_OPTS=""

# 暴露端口（直接使用变量，保持一致性）
EXPOSE ${SERVER_PORT} ${SNAIL_PORT}

# 复制 JAR 文件（核心步骤：路径必须相对于 Dockerfile 所在位置）
# 假设 Dockerfile 在项目根目录，JAR 路径为 ruoyi-admin/target/ruoyi-admin.jar
COPY ruoyi-admin/target/ruoyi-admin.jar /app/app.jar

# 验证文件存在性（构建时检测 JAR 是否复制成功，失败则中断构建）
RUN if [ ! -f "/app/app.jar" ]; then \
        echo "ERROR: ruoyi-admin.jar not found! Check COPY path."; \
        exit 1; \
    fi

# 启动命令（精简格式，确保使用 /app/app.jar）
ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom \
           -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar /app/app.jar
