/**
 * WebSocket客户端类
 * 负责WebSocket连接的建立、维护和消息的收发
 */
class WebSocketClient {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     * @param {string} options.url WebSocket服务器URL
     * @param {Function} options.onOpen 连接打开时的回调
     * @param {Function} options.onMessage 收到消息时的回调
     * @param {Function} options.onClose 连接关闭时的回调
     * @param {Function} options.onError 发生错误时的回调
     * @param {number} options.heartbeatInterval 心跳间隔(毫秒)
     * @param {number} options.reconnectInterval 重连间隔(毫秒)
     * @param {string} options.apiBaseUrl 后端API基础URL
     */
    constructor(options = {}) {
        this.options = Object.assign({
            url: 'ws://localhost:19002/ws',
            onOpen: () => {},
            onMessage: () => {},
            onClose: () => {},
            onError: () => {},
            heartbeatInterval: 30000,
            reconnectInterval: 5000,
            apiBaseUrl: 'http://localhost:18000'
        }, options);
        
        this.connected = false;
        this.socket = null;
        this.heartbeatTimer = null;
        this.reconnectTimer = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 10;
        this.userInfo = null;
        
        // 确保全局ByteBuffer类已定义
        if (typeof ByteBuffer !== 'function') {
            console.error('ByteBuffer类未定义，请先引入byte-buffer.js');
        }
    }
    
    /**
     * 连接到WebSocket服务器
     */
    connect() {
        if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
            console.log('WebSocket连接已存在');
            return;
        }
        
        try {
            this.socket = new WebSocket(this.options.url);
            this.socket.binaryType = 'arraybuffer';
            
            this.socket.onopen = (event) => {
                console.log('WebSocket连接已打开');
                this.connected = true;
                this.reconnectAttempts = 0;
                this._startHeartbeat();
                this.options.onOpen(event);
            };
            
            this.socket.onmessage = (event) => {
                this._handleMessage(event);
            };
            
            this.socket.onclose = (event) => {
                console.log('WebSocket连接已关闭');
                this.connected = false;
                this._stopHeartbeat();
                this.options.onClose(event);
                this._reconnect();
            };
            
            this.socket.onerror = (event) => {
                console.error('WebSocket错误:', event);
                this.options.onError(event);
            };
        } catch (error) {
            console.error('连接WebSocket时出错:', error);
            this._reconnect();
        }
    }
    
    /**
     * 关闭WebSocket连接
     */
    disconnect() {
        this._stopHeartbeat();
        clearTimeout(this.reconnectTimer);
        
        if (this.socket) {
            this.socket.onclose = null; // 防止触发自动重连
            this.socket.close();
            this.socket = null;
        }
        
        this.connected = false;
    }
    
    /**
     * 发送消息
     * @param {number} command 命令号
     * @param {Object} data 消息数据
     * @param {Object} options 发送选项
     */
    sendMessage(command, data, options = {}) {
        if (!this.connected || !this.socket || this.socket.readyState !== WebSocket.OPEN) {
            console.error('WebSocket未连接，无法发送消息');
            return false;
        }
        
        try {
            const defaults = {
                version: 1,
                clientType: 1,
                messageType: 0,
                appId: 10001,
                imei: 'web'
            };
            
            const config = Object.assign({}, defaults, options);
            const jsonData = typeof data === 'string' ? data : JSON.stringify(data);
            
            const bodyLen = this._getLen(jsonData);
            const imeiLen = this._getLen(config.imei);
            
            let message = new ByteBuffer();
            message.int32(command)
                .int32(config.version)
                .int32(config.clientType)
                .int32(config.messageType)
                .int32(config.appId)
                .int32(imeiLen)
                .int32(bodyLen)
                .vstring(config.imei, imeiLen)
                .vstring(jsonData, bodyLen);
            
            this.socket.send(message.pack());
            return true;
        } catch (error) {
            console.error('发送消息时出错:', error);
            return false;
        }
    }
    
    /**
     * 发送登录消息
     * @param {Object} data 登录信息
     * @returns {boolean} 是否成功发送
     */
    sendLoginMessage(data) {
        const loginData = {
            userId: data.userId,
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web',
            customStatus: null,
            customClientName: ""
        };
        
        return this.sendMessage(9000, loginData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 发送私聊消息
     * @param {Object} data 消息数据
     * @returns {boolean} 是否成功发送
     */
    sendChatMessage(data) {
        const messageData = {
            messageId: this._generateUuid(),
            fromId: data.fromId,
            toId: data.toId,
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web',
            messageBody: data.content
        };
        
        return this.sendMessage(1103, messageData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 发送群聊消息
     * @param {Object} data 消息数据
     * @returns {boolean} 是否成功发送
     */
    sendGroupMessage(data) {
        const messageData = {
            messageId: this._generateUuid(),
            fromId: data.fromId,
            groupId: data.groupId,
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web',
            messageBody: data.content
        };
        
        return this.sendMessage(2104, messageData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 发送私聊已读回执
     * @param {Object} data 回执数据
     * @returns {boolean} 是否成功发送
     */
    sendP2PReadReceipt(data) {
        const readData = {
            fromId: data.fromId,
            toId: data.toId,
            conversationType: 0,
            messageSequence: data.messageSequence
        };
        
        return this.sendMessage(1106, readData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 发送群聊已读回执
     * @param {Object} data 回执数据
     * @returns {boolean} 是否成功发送
     */
    sendGroupReadReceipt(data) {
        const readData = {
            fromId: data.fromId,
            toId: data.toId,
            groupId: data.groupId,
            conversationType: 1,
            messageSequence: data.messageSequence
        };
        
        return this.sendMessage(2106, readData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 发送消息确认回执
     * @param {Object} data 回执数据
     * @returns {boolean} 是否成功发送
     */
    sendMessageAck(data) {
        const ackData = {
            fromId: data.fromId,
            toId: data.toId,
            messageKey: data.messageKey,
            messageId: data.messageId,
            messageSequence: data.messageSequence
        };
        
        return this.sendMessage(1107, ackData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 处理收到的WebSocket消息
     * @param {MessageEvent} event 
     * @private
     */
    _handleMessage(event) {
        try {
            const data = event.data;
            if (!(data instanceof ArrayBuffer)) {
                console.log('收到非二进制消息:', data);
                return;
            }
            
            const bytebuf = new ByteBuffer(data);
            const byteBuffer = bytebuf.int32().int32().unpack();
            
            const command = byteBuffer[0];
            const bodyLen = byteBuffer[1];
            const unpack = bytebuf.vstring(null, bodyLen).unpack();
            const msgBody = unpack[2];
            
            console.log('收到消息 Command=', command, 'Body=', msgBody);
            
            // 处理心跳响应
            if (command === 9999) {
                this._handleHeartbeatResponse();
                return;
            }
            
            this.options.onMessage({
                command,
                body: msgBody
            });
        } catch (error) {
            console.error('处理消息时出错:', error);
        }
    }
    
    /**
     * 处理心跳响应
     * @private
     */
    _handleHeartbeatResponse() {
        console.log('收到心跳响应');
    }
    
    /**
     * 开始心跳
     * @private
     */
    _startHeartbeat() {
        this._stopHeartbeat();
        
        this.heartbeatTimer = setInterval(() => {
            if (this.connected && this.socket && this.socket.readyState === WebSocket.OPEN) {
                try {
                    this.sendMessage(9999, { userId: 'system', data: 'ping' });
                } catch (error) {
                    console.error('发送心跳时出错:', error);
                }
            } else {
                this._stopHeartbeat();
            }
        }, this.options.heartbeatInterval);
    }
    
    /**
     * 停止心跳
     * @private
     */
    _stopHeartbeat() {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = null;
        }
    }
    
    /**
     * 尝试重新连接
     * @private
     */
    _reconnect() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }
        
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.log('达到最大重连次数，停止重连');
            return;
        }
        
        this.reconnectAttempts++;
        
        this.reconnectTimer = setTimeout(() => {
            console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            this.connect();
        }, this.options.reconnectInterval);
    }
    
    /**
     * 计算字符串长度
     * @param {string} str 
     * @returns {number}
     * @private
     */
    _getLen(str) {
        let len = 0;
        for (let i = 0; i < str.length; i++) {
            const c = str.charCodeAt(i);
            if ((c >= 0x0001 && c <= 0x007e) || (0xff60 <= c && c <= 0xff9f)) {
                len++;
            } else {
                len += 3;
            }
        }
        return len;
    }
    
    /**
     * 生成UUID
     * @returns {string}
     * @private
     */
    _generateUuid() {
        const s = [];
        const hexDigits = "0123456789abcdef";
        for (let i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = "4"; 
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
        s[8] = s[13] = s[18] = s[23] = "-";
        
        return s.join("");
    }
    
    /**
     * 发送HTTP请求到后端REST API
     * @param {string} endpoint API端点
     * @param {string} method HTTP方法
     * @param {Object} data 请求数据
     * @returns {Promise<Object>} 响应数据
     * @private
     */
    async _httpRequest(endpoint, method = 'GET', data = null) {
        try {
            const url = `${this.options.apiBaseUrl}${endpoint}`;
            const headers = {
                'Content-Type': 'application/json'
            };
            
            const config = {
                method,
                headers
            };
            
            if (data && (method === 'POST' || method === 'PUT')) {
                config.body = JSON.stringify(data);
            }
            
            const response = await fetch(url, config);
            const result = await response.json();
            
            if (!result.ok) {
                throw new Error(result.msg || '请求失败');
            }
            
            return result;
        } catch (error) {
            console.error(`API请求失败 [${endpoint}]:`, error);
            throw error;
        }
    }
    
    /**
     * 设置当前用户信息
     * @param {Object} userInfo 用户信息
     */
    setUserInfo(userInfo) {
        this.userInfo = userInfo;
    }
    
    /**
     * 获取当前用户信息
     * @returns {Object|null} 用户信息
     */
    getUserInfo() {
        return this.userInfo;
    }
    
    /**
     * 获取好友列表
     * @returns {Promise<Array>} 好友列表
     */
    async getFriendshipList() {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            fromId: this.userInfo.userId,
            appId: this.userInfo.appId || 10001
        };
        
        const response = await this._httpRequest('/v1/friendship/getAllFriendShip', 'POST', reqData);
        return response.data || [];
    }
    
    /**
     * 添加好友
     * @param {Object} params 添加好友参数
     * @returns {Promise<Object>} 添加结果
     */
    async addFriend(params) {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            fromId: this.userInfo.userId,
            toItem: {
                toId: params.toId,
                remark: params.remark || '',
                addSource: params.addSource || '个人搜索',
                addWording: params.addWording || '请求添加您为好友'
            },
            appId: this.userInfo.appId || 10001,
            operater: this.userInfo.userId,
            clientType: this.userInfo.clientType || 1,
            imei: this.userInfo.imei || 'web'
        };
        
        return this._httpRequest('/v1/friendship/addFriend', 'POST', reqData);
    }
    
    /**
     * 获取加入的群组列表
     * @returns {Promise<Array>} 群组列表
     */
    async getJoinedGroups() {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            userId: this.userInfo.userId,
            appId: this.userInfo.appId || 10001,
            operater: this.userInfo.userId
        };
        
        const response = await this._httpRequest('/v1/group/getJoinedGroup', 'POST', reqData);
        return response.data || [];
    }
    
    /**
     * 获取群组信息
     * @param {string} groupId 群组ID
     * @returns {Promise<Object>} 群组信息
     */
    async getGroupInfo(groupId) {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            groupId: groupId,
            appId: this.userInfo.appId || 10001
        };
        
        const response = await this._httpRequest('/v1/group/getGroupInfo', 'POST', reqData);
        return response.data || null;
    }
    
    /**
     * 创建群组
     * @param {Object} params 创建群组参数
     * @returns {Promise<Object>} 创建结果
     */
    async createGroup(params) {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            ownerId: this.userInfo.userId,
            groupName: params.groupName,
            groupType: params.groupType || 1,
            memberIds: params.memberIds || [],
            appId: this.userInfo.appId || 10001,
            operater: this.userInfo.userId
        };
        
        return this._httpRequest('/v1/group/createGroup', 'POST', reqData);
    }
    
    /**
     * 发送同步请求获取离线消息
     * @returns {Promise<Object>} 同步结果
     */
    async syncOfflineMessages() {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            userId: this.userInfo.userId,
            appId: this.userInfo.appId || 10001,
            lastSequence: 0
        };
        
        return this._httpRequest('/v1/message/syncOfflineMessageList', 'POST', reqData);
    }
    
    /**
     * 同步好友列表
     * @param {number} lastSequence 上次同步序列号
     * @returns {Promise<Object>} 同步结果
     */
    async syncFriendshipList(lastSequence = 0) {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            userId: this.userInfo.userId,
            appId: this.userInfo.appId || 10001,
            lastSequence: lastSequence
        };
        
        return this._httpRequest('/v1/friendship/syncFriendShipList', 'POST', reqData);
    }
    
    /**
     * 同步群组列表
     * @param {number} lastSequence 上次同步序列号
     * @returns {Promise<Object>} 同步结果
     */
    async syncJoinedGroupList(lastSequence = 0) {
        if (!this.userInfo) {
            throw new Error('用户未登录');
        }
        
        const reqData = {
            userId: this.userInfo.userId,
            appId: this.userInfo.appId || 10001,
            lastSequence: lastSequence
        };
        
        return this._httpRequest('/v1/group/syncJoinedGroup', 'POST', reqData);
    }
}

// 导出WebSocketClient类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WebSocketClient;
} 