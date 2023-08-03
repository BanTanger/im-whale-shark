# 1. docker
## 安装
```bash
curl -sSL https://get.daocloud.io/docker | sh
```
或者
```bash
curl -fsSL https://get.docker.com | bash -s docker --mirror Aliyun
```
## 启动、停止

### systemctl 方式

重启docker服务

```
sudo systemctl restart docker
```

关闭docker

```
sudo systemctl stop docker
```

### service 方式

重启docker服务

```bash
 sudo service docker restart
```

关闭docker

```bash
 sudo service docker stop
```

# 2. docker-compose安装

```bash
sudo curl -L "https://github.com/docker/compose/releases/download/1.24.1/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

```bash
chmod +x /usr/local/bin/docker-compose
```

# 3. 安装git
```
ubuntu系统上执行 apt install git
```
