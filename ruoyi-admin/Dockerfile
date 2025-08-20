# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
#FROM bellsoft/liberica-openjdk-rocky:17.0.15-cds
#FROM bellsoft/liberica-openjdk-rocky:21.0.7-cds
#FROM findepi/graalvm:java17-native
FROM crpi-747o03p8n9vfx887.cn-shanghai.personal.cr.aliyuncs.com/mileso33/zhishifufei:jdk17-cds

LABEL maintainer="Lion Li"

#RUN mkdir -p /ruoyi/server/logs \
#    /ruoyi/server/temp \
#    /ruoyi/skywalking/agent

# 1. 创建目录并删除残留（确保路径绝对正确）
RUN mkdir -p /ruoyi/server/logs /ruoyi/server/temp && \
    rm -rf /ruoyi/server/app.jar  # 强制删除任何残留的 app.jar（文件/目录）

WORKDIR /ruoyi/server

ENV SERVER_PORT=8080 SNAIL_PORT=28080 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}
# 暴露 snail job 客户端端口 用于定时任务调度中心通信
EXPOSE ${SNAIL_PORT}
RUN rm -rf /ruoyi/server/app.jar
#ADD ./ruoyi-admin/target/ruoyi-admin.jar ./app.jar
#ADD ruoyi-admin/target/ruoyi-admin.jar /ruoyi/server/app.jar  

# 2. 用 COPY 替代 ADD，显式指定目标为文件（不加 /）
COPY ruoyi-admin/target/ruoyi-admin.jar /ruoyi/server/app.jar

# 3. 验证文件权限（可选，但确保可执行）
RUN chmod 644 /ruoyi/server/app.jar  # 赋予读权限


SHELL ["/bin/bash", "-c"]

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -Dsnail-job.port=${SNAIL_PORT} \
           # 应用名称 如果想区分集群节点监控 改成不同的名称即可
           #-Dskywalking.agent.service_name=ruoyi-server \
           #-javaagent:/ruoyi/skywalking/agent/skywalking-agent.jar \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar

