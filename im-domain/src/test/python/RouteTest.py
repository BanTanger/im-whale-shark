import requests
import json
import random
import string

url = "http://127.0.0.1:8000/v1/user/login" # 替换成你的接口地址
headers = {'Content-Type': 'application/json'}

success_count = 0
error_count = 0
port_9001_count = 0
port_9002_count = 0
for i in range(10):
    data = {
        "userId": ''.join(random.sample(string.ascii_letters + string.digits, 8)),
        "appId": random.randint(1, 100),
        "clientType": random.choice([0, 1, 2])
    }
    response = requests.post(url, headers=headers, data=json.dumps(data))
    # print(response.status_code == 200)
    if response.status_code == 200:
        json_data = response.json()
        # print(json_data)
        # print(json_data.get("msg"))
        # print(json_data.get("success"))
        # if json_data.get("success") == True:
        if json_data.get("msg") == "success":
            success_count += 1
            server_info = json_data.get("data")
            if server_info.get("port") == 9001:
                port_9001_count += 1
            elif server_info.get("port") == 9002:
                port_9002_count += 1
        else:
            error_count += 1
    else:
        error_count += 1

print("总共执行了 %d 次，成功 %d 次，失败 %d 次。" % (success_count + error_count, success_count, error_count))
print("端口号 9001 出现次数：%d" % port_9001_count)
print("端口号 9002 出现次数：%d" % port_9002_count)

# print("hello world")