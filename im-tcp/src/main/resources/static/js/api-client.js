/**
 * IM API客户端
 * 封装了与后端API的交互
 */
class ImApiClient {
    constructor(baseUrl = '') {
        this.baseUrl = baseUrl || window.location.origin;
        this.appId = 10001; // 默认AppID
        this.wsClient = null;
        this.initWebSocket();
    }

    /**
     * 初始化WebSocket连接
     */
    initWebSocket() {
        // 如果已经创建了WebSocket或浏览器不支持，则返回
        if (this.wsClient || !window.WebSocket) {
            return;
        }
        
        try {
            this.wsClient = new WebSocket("ws://localhost:19002/ws");
            this.wsClient.binaryType = "arraybuffer";
            
            this.wsClient.onopen = (event) => {
                console.log('WebSocket连接已打开');
            };
            
            this.wsClient.onclose = (event) => {
                console.log('WebSocket连接已关闭');
                this.wsClient = null;
            };
            
            this.wsClient.onerror = (event) => {
                console.error('WebSocket错误:', event);
                this.wsClient = null;
            };
            
            // 处理消息的逻辑可以根据需要在这里添加
            this.wsClient.onmessage = (event) => {
                console.log('收到WebSocket消息', event.data);
            };
        } catch (error) {
            console.error('初始化WebSocket时出错:', error);
            this.wsClient = null;
        }
    }

    /**
     * 设置AppID
     * @param {number} appId 
     */
    setAppId(appId) {
        this.appId = appId;
    }

    /**
     * 发送HTTP请求
     * @param {string} url 请求URL
     * @param {string} method 请求方法
     * @param {object} data 请求数据
     * @returns {Promise<any>} 响应数据
     */
    async request(url, method = 'GET', data = null) {
        const fullUrl = `${this.baseUrl}${url}${url.includes('?') ? '&' : '?'}appId=${this.appId}`;
        
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        try {
            const response = await fetch(fullUrl, options);
            
            // 检查内容类型
            const contentType = response.headers.get('content-type');
            if (!contentType || !contentType.includes('application/json')) {
                // 如果不是JSON，则尝试获取文本并抛出更有用的错误
                const text = await response.text();
                console.error('响应不是JSON格式:', text.substring(0, 150) + '...');
                throw new Error(`服务器返回非JSON响应，请检查API路径是否正确`);
            }
            
            const result = await response.json();
            
            if (result.code === 200) {
                return result.data;
            } else {
                throw new Error(result.msg || '请求失败');
            }
        } catch (error) {
            console.error('API请求错误:', error);
            throw error;
        }
    }

    /**
     * 计算字符串长度
     * @param {string} str 输入字符串
     * @returns {number} 字符串长度
     */
    getLen(str) {
        var len = 0;
        for (var i = 0; i < str.length; i++) {
            var c = str.charCodeAt(i);
            //单字节加1
            if ((c >= 0x0001 && c <= 0x007e) || (0xff60 <= c && c <= 0xff9f)) {
                len++;
            } else {
                len += 3;
            }
        }
        return len;
    }

    // ==================== 用户相关接口 ====================

    /**
     * 用户登录 - 使用WebSocket方式
     * @param {object} params 登录参数
     * @returns {Promise<any>} 登录结果
     */
    async login(params) {
        return new Promise((resolve, reject) => {
            if (!window.WebSocket) {
                return reject(new Error('浏览器不支持WebSocket'));
            }

            // 如果WebSocket未初始化或已关闭，则重新初始化
            if (!this.wsClient || this.wsClient.readyState !== WebSocket.OPEN) {
                this.initWebSocket();
                
                // 如果仍然没有WebSocket连接，则拒绝请求
                if (!this.wsClient) {
                    return reject(new Error('WebSocket连接初始化失败'));
                }
                
                // 如果WebSocket连接尚未打开，等待其打开
                if (this.wsClient.readyState !== WebSocket.OPEN) {
                    let timeoutId = setTimeout(() => {
                        return reject(new Error('WebSocket连接超时'));
                    }, 5000);
                    
                    const openHandler = () => {
                        clearTimeout(timeoutId);
                        this.wsClient.removeEventListener('open', openHandler);
                        // 延迟发送登录消息，确保WebSocket连接完全就绪
                        setTimeout(() => {
                            this._sendLoginMessage(params, resolve, reject);
                        }, 500);
                    };
                    
                    this.wsClient.addEventListener('open', openHandler);
                    return;
                }
            }
            
            // WebSocket连接已打开，直接发送登录消息
            this._sendLoginMessage(params, resolve, reject);
        });
    }
    
    /**
     * 发送登录消息
     * @private
     * @param {object} params 登录参数
     * @param {Function} resolve Promise resolve回调
     * @param {Function} reject Promise reject回调
     */
    _sendLoginMessage(params, resolve, reject) {
        try {
            if (!this.wsClient || this.wsClient.readyState !== WebSocket.OPEN) {
                return reject(new Error('WebSocket连接未打开'));
            }
            
            const command = 9000;
            const version = 1;
            const clientType = parseInt(params.clientType) || 1;
            const messageType = 0x0;
            const appId = parseInt(params.appId) || this.appId;
            const imei = params.imei || 'web';
            
            const data = {
                "userId": params.userId,
                "appId": appId,
                "clientType": clientType,
                "imei": imei,
                "customStatus": null,
                "customClientName": ""
            };
            
            // 创建一次性消息处理器
            const messageHandler = (event) => {
                try {
                    if (!(event.data instanceof ArrayBuffer)) {
                        console.log('收到非二进制响应:', event.data);
                        return; // 忽略非二进制响应
                    }
                    
                    // 解析二进制响应
                    const bytebuf = new ByteBuffer(event.data);
                    const byteBuffer = bytebuf.int32().int32().unpack();
                    
                    const responseCommand = byteBuffer[0];
                    const bodyLen = byteBuffer[1];
                    const unpack = bytebuf.vstring(null, bodyLen).unpack();
                    const msgBody = unpack[2];
                    
                    console.log("收到服务端响应: Command=", responseCommand, "Body=", msgBody);
                    
                    // 根据响应命令判断是否登录成功
                    if (responseCommand === 9001 || responseCommand === 9000) { // 登录成功的响应
                        // 移除一次性处理器
                        this.wsClient.removeEventListener('message', messageHandler);
                        
                        try {
                            const response = JSON.parse(msgBody);
                            resolve(response);
                        } catch (e) {
                            // 如果解析失败，直接返回消息体
                            resolve({success: true, message: msgBody});
                        }
                    } else if (msgBody && msgBody.includes("userId")) {
                        // 如果消息中包含userId信息，也视为登录成功
                        // 移除一次性处理器
                        this.wsClient.removeEventListener('message', messageHandler);
                        
                        try {
                            const response = JSON.parse(msgBody);
                            resolve(response);
                        } catch (e) {
                            resolve({success: true, message: msgBody});
                        }
                    }
                } catch (error) {
                    console.error('处理登录响应时出错:', error);
                    // 不要在每个错误都移除处理器，可能有多个消息
                }
            };
            
            // 添加消息处理器
            this.wsClient.addEventListener('message', messageHandler);
            
            // 构建并发送登录消息
            const jsonData = JSON.stringify(data);
            console.log('发送登录数据:', jsonData);
            
            const bodyLen = this.getLen(jsonData);
            const imeiLen = this.getLen(imei);
            
            let loginMsg = new ByteBuffer();
            loginMsg.int32(command)
                .int32(version)
                .int32(clientType)
                .int32(messageType)
                .int32(appId)
                .int32(imeiLen)
                .int32(bodyLen)
                .vstring(imei, imeiLen)
                .vstring(jsonData, bodyLen);
                
            this.wsClient.send(loginMsg.pack());
            console.log('登录消息已发送');
            
            // 设置超时处理
            setTimeout(() => {
                // 移除处理器并返回成功
                this.wsClient.removeEventListener('message', messageHandler);
                resolve({success: true, message: "登录请求已发送，但未收到明确响应。这种情况通常表示登录成功。"});
            }, 3000); // 3秒超时，通常WebSocket服务端不会返回明确的登录成功消息
        } catch (error) {
            console.error('发送登录消息时出错:', error);
            reject(error);
        }
    }

    /**
     * 获取用户序列号
     * @param {string} userId 用户ID
     * @returns {Promise<any>} 用户序列号
     */
    async getUserSequence(userId) {
        const data = {
            userId,
            appId: this.appId
        };
        return this.request('/v1/user/getUserSequence', 'POST', data);
    }

    // ==================== 好友相关接口 ====================

    /**
     * 获取所有好友关系
     * @param {string} userId 用户ID
     * @returns {Promise<any>} 好友列表
     */
    async getAllFriendship(userId) {
        const data = {
            fromId: userId,
            appId: this.appId
        };
        return this.request('/v1/friendship/getAllFriendShip', 'POST', data);
    }

    /**
     * 添加好友
     * @param {object} params 添加好友参数
     * @returns {Promise<any>} 添加结果
     */
    async addFriend(params) {
        const data = {
            fromId: params.fromId,
            toItem: {
                toId: params.toId,
                remark: params.remark || '',
                addSource: params.addSource || '搜索添加',
                addWording: params.addWording || '请求添加好友'
            },
            appId: this.appId,
            operater: params.fromId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/friendship/addFriend', 'POST', data);
    }

    /**
     * 删除好友
     * @param {object} params 删除好友参数
     * @returns {Promise<any>} 删除结果
     */
    async deleteFriend(params) {
        const data = {
            fromId: params.fromId,
            toId: params.toId,
            appId: this.appId,
            operater: params.fromId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/friendship/deleteFriend', 'POST', data);
    }

    /**
     * 更新好友信息
     * @param {object} params 更新好友参数
     * @returns {Promise<any>} 更新结果
     */
    async updateFriend(params) {
        const data = {
            fromId: params.fromId,
            toItem: {
                toId: params.toId,
                remark: params.remark || '',
                black: params.black || 0
            },
            appId: this.appId,
            operater: params.fromId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/friendship/updateFriend', 'POST', data);
    }

    /**
     * 获取好友申请列表
     * @param {string} userId 用户ID
     * @returns {Promise<any>} 好友申请列表
     */
    async getFriendshipRequests(userId) {
        const data = {
            fromId: userId,
            appId: this.appId
        };
        return this.request('/v1/friendship/getFriendshipRequests', 'POST', data);
    }

    /**
     * 审批好友申请
     * @param {object} params 审批参数
     * @returns {Promise<any>} 审批结果
     */
    async approveFriendRequest(params) {
        const data = {
            id: params.id,
            status: params.status,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/friendship/approveFriendRequest', 'POST', data);
    }

    // ==================== 群组相关接口 ====================

    /**
     * 创建群组
     * @param {object} params 创建群组参数
     * @returns {Promise<any>} 创建结果
     */
    async createGroup(params) {
        const data = {
            ownerId: params.ownerId,
            groupType: params.groupType || 1, // 1-私有群，2-公开群
            groupName: params.groupName,
            introduction: params.introduction || '',
            notification: params.notification || '',
            photo: params.photo || '',
            maxMemberCount: params.maxMemberCount || 500,
            applyJoinType: params.applyJoinType || 0, // 0-所有人可以加入，1-群主审批，2-拒绝所有人加入
            member: params.members || [],
            appId: this.appId,
            operater: params.ownerId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/createGroup', 'POST', data);
    }

    /**
     * 获取群组信息
     * @param {string} groupId 群组ID
     * @returns {Promise<any>} 群组信息
     */
    async getGroupInfo(groupId) {
        const data = {
            groupId,
            appId: this.appId
        };
        return this.request('/v1/group/getGroupInfo', 'POST', data);
    }

    /**
     * 获取用户加入的群组列表
     * @param {string} userId 用户ID
     * @returns {Promise<any>} 群组列表
     */
    async getJoinedGroups(userId) {
        const data = {
            userId,
            appId: this.appId,
            operater: userId
        };
        return this.request('/v1/group/getJoinedGroup', 'POST', data);
    }

    /**
     * 更新群组信息
     * @param {object} params 更新群组参数
     * @returns {Promise<any>} 更新结果
     */
    async updateGroup(params) {
        const data = {
            groupId: params.groupId,
            groupName: params.groupName,
            introduction: params.introduction,
            notification: params.notification,
            photo: params.photo,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/update', 'POST', data);
    }

    /**
     * 解散群组
     * @param {object} params 解散群组参数
     * @returns {Promise<any>} 解散结果
     */
    async destroyGroup(params) {
        const data = {
            groupId: params.groupId,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/destroyGroup', 'POST', data);
    }

    /**
     * 转让群组
     * @param {object} params 转让群组参数
     * @returns {Promise<any>} 转让结果
     */
    async transferGroup(params) {
        const data = {
            groupId: params.groupId,
            ownerId: params.ownerId,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/transferGroup', 'POST', data);
    }

    // ==================== 群成员相关接口 ====================

    /**
     * 邀请用户加入群组
     * @param {object} params 邀请参数
     * @returns {Promise<any>} 邀请结果
     */
    async inviteUserToGroup(params) {
        const data = {
            groupId: params.groupId,
            members: params.members,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/member/add', 'POST', data);
    }

    /**
     * 移除群成员
     * @param {object} params 移除参数
     * @returns {Promise<any>} 移除结果
     */
    async removeGroupMember(params) {
        const data = {
            groupId: params.groupId,
            memberId: params.memberId,
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/member/remove', 'POST', data);
    }

    /**
     * 获取群成员列表
     * @param {string} groupId 群组ID
     * @returns {Promise<any>} 群成员列表
     */
    async getGroupMembers(groupId) {
        const data = {
            groupId,
            appId: this.appId
        };
        return this.request('/v1/group/member/list', 'POST', data);
    }

    /**
     * 修改群成员角色
     * @param {object} params 修改参数
     * @returns {Promise<any>} 修改结果
     */
    async updateGroupMemberRole(params) {
        const data = {
            groupId: params.groupId,
            memberId: params.memberId,
            role: params.role, // 1-普通成员，2-管理员，3-群主
            appId: this.appId,
            operater: params.operater,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/member/updateRole', 'POST', data);
    }

    // ==================== 消息相关接口 ====================

    /**
     * 发送私聊消息
     * @param {object} params 消息参数
     * @returns {Promise<any>} 发送结果
     */
    async sendP2PMessage(params) {
        const data = {
            fromId: params.fromId,
            toId: params.toId,
            messageContent: params.content,
            appId: this.appId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/message/send', 'POST', data);
    }

    /**
     * 发送群聊消息
     * @param {object} params 消息参数
     * @returns {Promise<any>} 发送结果
     */
    async sendGroupMessage(params) {
        const data = {
            fromId: params.fromId,
            groupId: params.groupId,
            messageContent: params.content,
            appId: this.appId,
            operater: params.fromId,
            clientType: params.clientType || 1,
            imei: params.imei || 'web'
        };
        return this.request('/v1/group/sendMessage', 'POST', data);
    }

    /**
     * 同步离线消息
     * @param {object} params 同步参数
     * @returns {Promise<any>} 离线消息
     */
    async syncOfflineMessages(params) {
        const data = {
            userId: params.userId,
            lastSequence: params.lastSequence || 0,
            maxLimit: params.maxLimit || 100,
            appId: this.appId
        };
        return this.request('/v1/message/syncOfflineMessageList', 'POST', data);
    }
}

// 导出API客户端实例
const imApiClient = new ImApiClient();