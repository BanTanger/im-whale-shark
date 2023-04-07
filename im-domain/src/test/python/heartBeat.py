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

import random
import string

special_char = ["#", "@", "$", "&"]  # 特殊符号列表
lst = []

# 生成随机字符串
for _ in range(110):
    char_type = random.choice(["digit", "lower", "upper", "special"])
    if char_type == "digit":
        lst.append(str(random.randint(0, 9)))
    elif char_type == "lower":
        lst.append(random.choice(string.ascii_lowercase))
    elif char_type == "upper":
        lst.append(random.choice(string.ascii_uppercase))
    elif char_type == "special":
        lst.append(random.choice(special_char))

s = "".join(lst)  # 转换为字符串
print(s)

char_list = list(s)  # 将字符串转换为字符列表
print(char_list)

from collections import Counter

char_count_dict = Counter(char_list)  # 计算每个字符的出现次数
print(char_count_dict)