

import socket
import json
import struct
import threading
import time
import uuid


def task(s):
    print("task开始")
    while True:
        # datab = scoket.recv(4)
        command = struct.unpack('>I', s.recv(4))[0] # 接受command并且解析
        num = struct.unpack('>I', s.recv(4))[0] # 接受包大小并且解析
        print(command)
        if command == 0x232a :
            print("收到下线通知,退出登录")
            s.close()
            # exit;
            # break


imei = str(uuid.uuid1())

s=socket.socket(socket.AF_INET,socket.SOCK_STREAM)
s.connect(("127.0.0.1",9001))

t1 = threading.Thread(target=task,args=(s,))
t1.start()

## 基础数据
# command = 0x232b
command = 0x2328

    # WEB(1,"web"),
    # IOS(2,"ios"),
    # ANDROID(3,"android"),
    # WINDOWS(4,"windows"),
    # MAC(5,"mac"),

version = 2
clientType = 5
print(clientType)
messageType = 0x0
appId = 10000
userId = 'bantanger'

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
data = {"userId": userId}
jsonData = json.dumps(data)
body = bytes(jsonData, 'utf-8')
body_len = len(body)
bodyLenBytes = body_len.to_bytes(4,'big')

s.sendall(commandByte + versionByte + clientTypeByte + messageTypeByte + appIdByte + imeiLengthByte + bodyLenBytes + imeiBytes + body)

while(True):
    i = 1+1


