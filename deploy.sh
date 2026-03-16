#!/bin/bash

# 配置
APP_NAME="spring-ai-rag"
REPO_URL="git@github.com:SuniaW/spring-ai-rag.git"
BRANCH="main"
PORT="8081"

echo "========================================"
echo ">>> Step 1: 拉取最新代码..."
echo "========================================"
if [ ! -d "$APP_NAME" ]; then
    git clone -b $BRANCH $REPO_URL $APP_NAME
    cd $APP_NAME
else
    cd $APP_NAME
    git checkout $BRANCH
    git pull origin $BRANCH
fi

echo ">>> Step 2: 构建 Java 21 镜像..."
docker build -t $APP_NAME:latest .

echo ">>> Step 3: 停止并删除旧容器..."
docker stop $APP_NAME || true
docker rm $APP_NAME || true

echo ">>> Step 4: 启动容器 (物理内存限制 256MB)..."
# --memory="256m": 宿主机强制限制该容器只能用 256MB
# --memory-reservation="128m": 软限制，尝试保持在 128MB
docker run -d \
  --name $APP_NAME \
  -p $PORT:8081 \
  --memory="256m" \
  --memory-swap="512m" \
  --restart always \
  $APP_NAME:latest

echo ">>> Step 5: 清理过期镜像..."
docker image prune -f

echo ">>> 部署成功！"
echo "当前容器内存实时状态:"
docker stats $APP_NAME --no-stream