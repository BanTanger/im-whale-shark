import socket
import json
import struct
import threading
import time
import uuid

def doPing(scoket):
## 基础数据
    command = 0x270f
    print(command)
    version = 1
    clientType = 5
    messageType = 0x0
    appId = 10000
    userId = 'bantanger'
    imei = str(uuid.uuid1())

    ## 数据转换为bytes
    commandByte = command.to_bytes(4,'big')
    versionByte = version.to_bytes(4,'big')
    messageTypeByte = messageType.to_bytes(4,'big')
    clientTypeByte = clientType.to_bytes(4,'big')
    appIdByte = appId.to_bytes(4,'big')
    clientTypeByte = clientType.to_bytes(4,'big')
    imeiBytes = bytes(imei,"utf-8");
    imeiLength = len(imeiBytes)
    imeiLengthByte = imeiLength.to_bytes(4,'big')
    data = {}
    jsonData = json.dumps(data)
    body = bytes(jsonData, 'utf-8')
    body_len = len(body)
    bodyLenBytes = body_len.to_bytes(4,'big')

    s.sendall(commandByte + versionByte + clientTypeByte + messageTypeByte + appIdByte + imeiLengthByte + bodyLenBytes + imeiBytes + body)

def ping(scoket):
    while True:
        time.sleep(10)
        doPing(scoket)

s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.connect(("127.0.0.1",9001))

# 创建一个线程专门接收服务端数据并且打印
t1 = threading.Thread(target=ping,args=(s,))
t1.start()




