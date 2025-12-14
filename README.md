# 某区域温度/湿度/气压数据发布订阅及分析处理系统

## 运行指南

- 启动docker服务

```bash
cd f:\IoT\docs\devops
docker-compose -f docker-compose-environment.yml up -d
```

- 创建MQTT用户

```bash
docker exec mqtt-broker mosquitto_passwd -c -b /mosquitto/config/pwfile iotuser iotpassword
```

- 启动SpringBoot应用

