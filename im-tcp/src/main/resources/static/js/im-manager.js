/**
 * IM管理模块
 * 整合消息处理器、IM客户端和API客户端功能
 */

/**
 * 消息处理器类
 * 负责解析和处理不同类型的WebSocket消息
 */
class MessageHandler {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     */
    constructor(options = {}) {
        this.options = Object.assign({
            onChatMessage: () => {},
            onGroupMessage: () => {},
            onSystemMessage: () => {},
            onMessageAck: () => {},
            onReadReceipt: () => {},
            onFriendRequest: () => {},
            onFriendStatusChange: () => {},
            onGroupStatusChange: () => {},
            wsClient: null,
            imClient: null
        }, options);
        
        // 存储IMClient实例引用
        this.imClient = this.options.imClient;
        
        // 消息命令码常量
        this.COMMAND = {
            CHAT_MESSAGE: 1103,
            GROUP_MESSAGE: 2104,
            CHAT_READ_RECEIPT: 1106,
            GROUP_READ_RECEIPT: 2106,
            MESSAGE_ACK: 1107,
            SYSTEM_MESSAGE: 9999,
            FRIEND_REQUEST: 3000,
            FRIEND_STATUS_CHANGE: 3100,
            GROUP_STATUS_CHANGE: 4000,
            CHAT_MSG_SYNC: 1046,            // 单聊消息 ACK（消息多端同步）
            CHAT_READ_NOTIFICATION: 1054    // 单聊消息已读回执
        };
        
        // 缓存已处理的消息ID，防止重复处理
        this.processedMessages = new Set();
        
        // 消息队列，用于处理消息顺序
        this.messageQueue = [];
        this.isProcessingQueue = false;
    }
    
    /**
     * 处理WebSocket消息
     * @param {Object} message 消息对象
     * @returns {Object|null} 处理后的消息对象
     */
    handleMessage(message) {
        // 如果消息是ByteBuffer对象（检查常见属性）
        if (message && message._org_buf && message._list && message._littleEndian !== undefined) {
            console.error('接收到原始ByteBuffer对象，而不是解析后的消息对象');
            return null;
        }
        
        if (!message || !message.command) {
            console.error('无效的消息格式');
            return null;
        }
        
        try {
            // 将消息加入队列，确保按顺序处理
            this.messageQueue.push(message);
            this._processMessageQueue();
            
            // 直接返回加入队列的消息，实际处理在队列中进行
            return message;
        } catch (error) {
            console.error('处理消息时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理消息队列
     * @private
     */
    async _processMessageQueue() {
        if (this.isProcessingQueue) {
            return;
        }
        
        this.isProcessingQueue = true;
        
        try {
            while (this.messageQueue.length > 0) {
                const message = this.messageQueue.shift();
                await this._processMessage(message);
            }
        } finally {
            this.isProcessingQueue = false;
        }
    }
    
    /**
     * 处理单条消息
     * @param {Object} message 
     * @private
     */
    async _processMessage(message) {
        try {
            // 再次检查消息格式，确保安全性
            if (!message || typeof message !== 'object') {
                console.error('无效消息对象:', message);
                return null;
            }
            
            // 检查消息中是否有ByteBuffer实例
            if (message._org_buf && message._list) {
                console.error('尝试处理原始ByteBuffer对象，应该首先由WebSocketClient解析:', message);
                return null;
            }
            
            // 确保command是一个有效的数字
            let command = message.command;
            if (typeof command !== 'number') {
                if (command && !isNaN(Number(command))) {
                    command = Number(command);
                } else {
                    console.error('消息的command字段不是有效的数字:', command);
                    return null;
                }
            }
            
            switch (command) {
                case this.COMMAND.CHAT_MESSAGE:
                    return this._handleChatMessage(message);
                
                case this.COMMAND.GROUP_MESSAGE:
                    return this._handleGroupMessage(message);
                
                case this.COMMAND.CHAT_READ_RECEIPT:
                    return this._handleReadReceipt(message, false);
                
                case this.COMMAND.GROUP_READ_RECEIPT:
                    return this._handleReadReceipt(message, true);
                
                case this.COMMAND.MESSAGE_ACK:
                    return this._handleMessageAck(message);
                
                case this.COMMAND.CHAT_MSG_SYNC:
                    return this._handleMessageAck(message);
                
                case this.COMMAND.CHAT_READ_NOTIFICATION:
                    return this._handleReadReceipt(message, false);
                
                case this.COMMAND.SYSTEM_MESSAGE:
                    return this._handleSystemMessage(message);
                
                case this.COMMAND.FRIEND_REQUEST:
                    return this._handleFriendRequest(message);
                
                case this.COMMAND.FRIEND_STATUS_CHANGE:
                    return this._handleFriendStatusChange(message);
                
                case this.COMMAND.GROUP_STATUS_CHANGE:
                    return this._handleGroupStatusChange(message);
                
                default:
                    console.log('未知消息类型:', command);
                    return null;
            }
        } catch (error) {
            console.error('处理消息时出错:', error, '原始消息:', JSON.stringify(message, null, 2));
            return null;
        }
    }
    
    /**
     * 处理私聊消息
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleChatMessage(message) {
        try {
            const data = JSON.parse(message.body);
            
            // 检查是否已处理过该消息
            if (data.messageId && this.processedMessages.has(data.messageId)) {
                console.log('跳过重复消息:', data.messageId);
                return null;
            }
            
            // 处理消息体可能嵌套在data字段中的情况
            let messageData = data;
            if (data.data && typeof data.data === 'object') {
                messageData = data.data;
            }
            
            const processedMessage = {
                type: 'chat',
                id: messageData.messageId,
                fromId: messageData.fromId,
                toId: messageData.toId,
                content: messageData.messageBody,
                timestamp: messageData.messageTime || Date.now(),
                sequence: messageData.messageSequence,
                status: 'received',
                key: messageData.messageKey
            };
            
            // 如果有messageId，记录为已处理
            if (messageData.messageId) {
                this.processedMessages.add(messageData.messageId);
                
                // 清理过期的消息ID，避免内存泄漏
                if (this.processedMessages.size > 1000) {
                    this._cleanProcessedMessages();
                }
                
                // 使用IMClient的方法和属性
                if (this.imClient) {
                    // 检查缓存中是否已存在此消息
                    const conversationId = this.imClient._getConversationId('C2C', 
                        processedMessage.fromId === this.imClient.currentUserId ? 
                        processedMessage.toId : processedMessage.fromId);
                    
                    // 使用imClient的messageCache
                    const messageList = this.imClient.messageCache.get(conversationId);
                    if (messageList && messageList.some(m => m.id === processedMessage.id)) {
                        console.log('消息已在缓存中，跳过处理:', processedMessage.id);
                        return null;
                    }
                }
            }
            
            // 发送消息确认回执
            this._sendMessageAck(processedMessage);
            
            this.options.onChatMessage(processedMessage);
            return processedMessage;
        } catch (error) {
            console.error('处理私聊消息时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理群聊消息
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleGroupMessage(message) {
        try {
            console.log('群聊消息原始数据:', message);
            
            let data;
            try {
                data = JSON.parse(message.body);
                console.log('解析后的消息数据:', data);
            } catch (parseError) {
                console.error('解析群聊消息JSON失败:', parseError);
                // 如果不是JSON字符串，尝试直接使用
                data = message.body;
                console.log('使用原始消息体:', data);
            }
            
            // 检查是否已处理过该消息
            if (data.messageId && this.processedMessages.has(data.messageId)) {
                console.log('跳过重复消息:', data.messageId);
                return null;
            }
            
            // 处理消息体可能嵌套在data字段中的情况
            let messageData = data;
            if (data.data && typeof data.data === 'object') {
                messageData = data.data;
                console.log('使用嵌套data字段:', messageData);
            }
            
            console.log('最终处理的消息数据:', messageData);
            
            const processedMessage = {
                type: 'group',
                id: messageData.messageId || ('group_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)),
                fromId: messageData.fromId,
                toId: messageData.groupId,  // 增加toId字段以兼容UI显示
                groupId: messageData.groupId,
                content: messageData.messageBody || messageData.content || '',
                timestamp: messageData.messageTime || messageData.timestamp || Date.now(),
                sequence: messageData.messageSequence,
                status: 'received',
                key: messageData.messageKey
            };
            
            console.log('处理后的群聊消息对象:', processedMessage);
            
            // 如果有messageId，记录为已处理
            if (messageData.messageId) {
                this.processedMessages.add(messageData.messageId);
                
                // 清理过期的消息ID，避免内存泄漏
                if (this.processedMessages.size > 1000) {
                    this._cleanProcessedMessages();
                }
                
                // 使用IMClient的方法和属性
                if (this.imClient) {
                    // 检查缓存中是否已存在此消息
                    const conversationId = this.imClient._getConversationId('GROUP', processedMessage.groupId);
                    console.log('检查会话ID是否存在:', conversationId);
                    
                    // 使用imClient的messageCache
                    const messageList = this.imClient.messageCache.get(conversationId);
                    
                    // 如果会话不存在，尝试创建一个新的会话
                    if (!this.imClient.conversations.has(conversationId)) {
                        console.log(`群聊会话 ${conversationId} 不存在，尝试创建`);
                        
                        // 创建会话对象
                        const conversation = {
                            id: conversationId,
                            type: 'GROUP',
                            targetId: processedMessage.groupId,
                            name: `群组 ${processedMessage.groupId}`,
                            lastMessage: {
                                content: processedMessage.content,
                                timestamp: processedMessage.timestamp
                            },
                            unreadCount: 1
                        };
                        
                        // 保存会话
                        this.imClient.conversations.set(conversationId, conversation);
                        
                        // 触发会话创建回调
                        if (this.imClient.callbacks.onConversationsCreated) {
                            this.imClient.callbacks.onConversationsCreated([conversation]);
                        }
                        
                        // 触发会话列表更新回调
                        if (this.imClient.callbacks.onConversationsUpdated) {
                            const updatedConversations = Array.from(this.imClient.conversations.values());
                            this.imClient.callbacks.onConversationsUpdated(updatedConversations);
                        }
                    }
                    
                    // 再次检查消息是否在缓存中
                    if (messageList && messageList.some(m => m.id === processedMessage.id)) {
                        console.log('消息已在缓存中，跳过处理:', processedMessage.id);
                        return null;
                    }
                    
                    // 确保消息会被添加到缓存中
                    this.imClient._addToMessageCache(processedMessage);
                }
            }
            
            // 自动发送已读回执
            this._sendGroupReadReceipt(processedMessage);
            
            console.log('触发群聊消息回调:', processedMessage.id);
            this.options.onGroupMessage(processedMessage);
            return processedMessage;
        } catch (error) {
            console.error('处理群聊消息时出错:', error, '原始消息:', message);
            return null;
        }
    }
    
    /**
     * 处理已读回执
     * @param {Object} message 原始消息对象
     * @param {boolean} isGroup 是否是群聊已读回执
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleReadReceipt(message, isGroup) {
        try {
            const data = JSON.parse(message.body);
            
            // 处理不同命令类型
            const isReadNotification = message.command === this.COMMAND.CHAT_READ_NOTIFICATION;
            
            // 处理消息体可能嵌套在data字段中的情况
            let messageData = data;
            if (data.data && typeof data.data === 'object') {
                messageData = data.data;
            }
            
            const processedReceipt = {
                type: isGroup ? 'group_receipt' : 'chat_receipt',
                fromId: messageData.fromId,
                toId: messageData.toId,
                groupId: messageData.groupId,
                sequence: messageData.messageSequence,
                messageId: messageData.messageId, // 1054类型可能包含messageId
                timestamp: messageData.timestamp || Date.now()
            };
            
            // 标记特定类型
            if (isReadNotification) {
                processedReceipt.isReadNotification = true;
            }
            
            this.options.onReadReceipt(processedReceipt);
            return processedReceipt;
        } catch (error) {
            console.error('处理已读回执时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理消息确认回执
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleMessageAck(message) {
        try {
            const data = JSON.parse(message.body);
            
            // 判断是否是多端同步ACK
            const isMsgSync = message.command === this.COMMAND.CHAT_MSG_SYNC;
            
            // 处理消息体可能嵌套在data字段中的情况
            let messageData = data;
            if (data.data && typeof data.data === 'object') {
                messageData = data.data;
            }
            
            const processedAck = {
                type: 'ack',
                messageId: messageData.messageId,
                fromId: messageData.fromId,
                toId: messageData.toId,
                sequence: messageData.messageSequence,
                content: messageData.messageBody, // 多端同步包含消息内容
                timestamp: messageData.messageTime || messageData.timestamp || Date.now()
            };
            
            // 标记特定类型
            if (isMsgSync) {
                processedAck.isSync = true;
            }
            
            this.options.onMessageAck(processedAck);
            return processedAck;
        } catch (error) {
            console.error('处理消息确认回执时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理系统消息
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleSystemMessage(message) {
        try {
            let data;
            
            if (typeof message.body === 'string') {
                data = JSON.parse(message.body);
            } else {
                data = message.body || message;
            }
            
            // 心跳响应特殊处理
            if (data.type === 'ping' || data.type === 'pong') {
                return {
                    type: 'heartbeat',
                    timestamp: data.timestamp
                };
            }
            
            const processedMessage = {
                type: 'system',
                content: data.content || JSON.stringify(data),
                timestamp: data.timestamp || Date.now()
            };
            
            this.options.onSystemMessage(processedMessage);
            return processedMessage;
        } catch (error) {
            console.error('处理系统消息时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理好友请求
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleFriendRequest(message) {
        try {
            const data = JSON.parse(message.body);
            
            const processedRequest = {
                type: 'friend_request',
                fromId: data.fromId,
                toId: data.toId,
                remark: data.remark,
                addSource: data.addSource,
                addWording: data.addWording,
                timestamp: data.timestamp || Date.now()
            };
            
            this.options.onFriendRequest(processedRequest);
            return processedRequest;
        } catch (error) {
            console.error('处理好友请求时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理好友状态变更
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleFriendStatusChange(message) {
        try {
            const data = JSON.parse(message.body);
            
            const processedChange = {
                type: 'friend_status_change',
                fromId: data.fromId,
                toId: data.toId,
                status: data.status,
                timestamp: data.timestamp || Date.now()
            };
            
            this.options.onFriendStatusChange(processedChange);
            return processedChange;
        } catch (error) {
            console.error('处理好友状态变更时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理群组状态变更
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleGroupStatusChange(message) {
        try {
            const data = JSON.parse(message.body);
            
            const processedChange = {
                type: 'group_status_change',
                groupId: data.groupId,
                operatorId: data.operatorId,
                operation: data.operation,
                timestamp: data.timestamp || Date.now()
            };
            
            this.options.onGroupStatusChange(processedChange);
            return processedChange;
        } catch (error) {
            console.error('处理群组状态变更时出错:', error);
            return null;
        }
    }
    
    /**
     * 清理已处理的消息ID
     * @private
     */
    _cleanProcessedMessages() {
        if (this.processedMessages.size <= 1000) return;
        
        // 转换为数组，保留最近的500条
        const messageArray = Array.from(this.processedMessages);
        const newMessageSet = new Set(messageArray.slice(messageArray.length - 500));
        this.processedMessages = newMessageSet;
    }
    
    /**
     * 发送消息确认回执
     * @param {Object} message 消息对象
     * @private
     */
    _sendMessageAck(message) {
        if (!this.options.wsClient || !message.id) {
            return;
        }
        
        const userInfo = this.options.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            return;
        }
        
        this.options.wsClient.sendMessageAck({
            messageId: message.id,
            fromId: userInfo.userId,
            toId: message.fromId,
            messageSequence: message.sequence
        });
    }
    
    /**
     * 发送群聊已读回执
     * @param {Object} message 消息对象
     * @private
     */
    _sendGroupReadReceipt(message) {
        if (!this.options.wsClient || !message.id || !message.groupId) {
            return;
        }
        
        const userInfo = this.options.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            return;
        }
        
        this.options.wsClient.sendGroupReadReceipt({
            fromId: userInfo.userId,
            toId: message.fromId,
            groupId: message.groupId,
            messageSequence: message.sequence,
            appId: userInfo.appId,
            clientType: userInfo.clientType,
            imei: userInfo.imei
        });
    }
    
    /**
     * 创建消息对象
     * @param {string} type 消息类型
     * @param {Object} data 消息数据
     * @returns {Object} 消息对象
     */
    createMessage(type, data) {
        if (type === 'chat') {
            return {
                type: 'chat',
                id: this._generateId(),
                fromId: data.fromId,
                toId: data.toId,
                content: data.content,
                timestamp: Date.now(),
                status: 'sending'
            };
        } else if (type === 'group') {
            return {
                type: 'group',
                id: this._generateId(),
                fromId: data.fromId,
                groupId: data.groupId,
                content: data.content,
                timestamp: Date.now(),
                status: 'sending'
            };
        } else if (type === 'system') {
            return {
                type: 'system',
                id: this._generateId(),
                content: data.content,
                timestamp: Date.now()
            };
        }
        
        return null;
    }
    
    /**
     * 生成ID
     * @returns {string} 唯一ID
     * @private
     */
    _generateId() {
        return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
}

/**
 * IM API客户端类
 * 负责处理与后端API的HTTP通信
 */
class IMApiClient {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     */
    constructor(options = {}) {
        this.options = Object.assign({
            baseUrl: 'http://localhost:18000',
            appId: 10001
        }, options);
    }
    
    /**
     * 设置AppID
     * @param {number} appId 应用ID
     */
    setAppId(appId) {
        this.options.appId = appId;
    }
    
    /**
     * 设置基础URL
     * @param {string} baseUrl 基础URL
     */
    setBaseUrl(baseUrl) {
        this.options.baseUrl = baseUrl;
    }
    
    /**
     * 发送登录请求
     * @param {Object} data 登录数据
     * @returns {Promise<Object>} 登录结果
     */
    async login(data) {
        const req = {
            userId: data.userId,
            clientType: data.clientType || 1,
            appId: data.appId || this.options.appId,
            imei: data.imei || 'web',
            operater: data.userId
        };
        
        const response = await this._post('/v1/user/login', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '登录失败');
        }
        
        return response.data;
    }
    
    /**
     * 获取好友列表
     * @param {string} userId 用户ID
     * @returns {Promise<Array>} 好友列表
     */
    async getFriendList(userId) {
        const req = {
            fromId: userId,
            appId: this.options.appId,
            operater: userId
        };
        
        const response = await this._post('/v1/friendship/getAllFriendShip', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '获取好友列表失败');
        }
        
        return response.data || [];
    }
    
    /**
     * 获取群组列表
     * @param {string} userId 用户ID
     * @returns {Promise<Array>} 群组列表
     */
    async getGroupList(userId) {
        const req = {
            memberId: userId,
            appId: this.options.appId,
            operater: userId
        };
        
        const response = await this._post('/v1/group/getJoinedGroup', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '获取群组列表失败');
        }
        
        // 根据GetJoinedGroupResp结构解析
        if (response.data && response.data.groupList) {
            return response.data.groupList || [];
        }
        
        return response.data || [];
    }
    
    /**
     * 获取群组信息
     * @param {string} groupId 群组ID
     * @returns {Promise<Object>} 群组信息
     */
    async getGroupInfo(groupId) {
        const req = {
            groupId: groupId,
            appId: this.options.appId
        };
        
        const response = await this._post('/v1/group/getGroupInfo', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '获取群组信息失败');
        }
        
        // GetGroupResp可能包含额外信息，如memberList
        if (response.data) {
            return response.data;
        }
        
        return null;
    }
    
    /**
     * 创建群组
     * @param {Object} data 创建群组数据
     * @returns {Promise<Object>} 创建结果
     */
    async createGroup(data) {
        const req = {
            ownerId: data.userId,
            groupName: data.groupName,
            groupType: data.groupType || 1,
            introduction: data.introduction || '',
            notification: data.notification || '',
            member: data.member || [], // 成员列表，使用GroupMemberDto对象集合
            appId: this.options.appId,
            operater: data.userId
        };
        
        console.log('创建群组请求参数:', req);
        
        const response = await this._post('/v1/group/createGroup', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '创建群组失败');
        }
        
        return response.data;
    }
    
    /**
     * 添加好友
     * @param {Object} data 添加好友数据
     * @returns {Promise<Object>} 添加结果
     */
    async addFriend(data) {
        try {
            console.log('APIClient添加好友 - 接收参数:', data);
            
            if (!data.fromId) {
                throw new Error('缺少必要参数: fromId');
            }
            
            if (!data.toId) {
                throw new Error('缺少必要参数: toId');
            }
            
            const req = {
                fromId: data.fromId,
                toItem: {
                    toId: data.toId,
                    remark: data.remark || '',
                    addSource: data.addSource || '个人搜索',
                    addWording: data.addWording || '请求添加您为好友'
                },
                appId: this.options.appId,
                operater: data.fromId,
                clientType: data.clientType || 1,
                imei: data.imei || 'web'
            };
            
            console.log('APIClient添加好友 - 发送请求:', req);
            
            const response = await this._post('/v1/friendship/addFriend', req);
            
            console.log('APIClient添加好友 - 收到响应:', response);
            
            if (!response.isOk()) {
                throw new Error(response.msg || '添加好友失败');
            }
            
            // 处理data为null的情况
            if (response.data === null || response.data === undefined) {
                console.log('APIClient添加好友 - 响应成功但data为null，返回成功对象');
                return { 
                    success: true, 
                    message: '添加好友请求已发送',
                    code: response.code
                };
            }
            
            return response.data;
        } catch (error) {
            console.error('APIClient添加好友失败:', error);
            console.error('详细信息:', JSON.stringify(data));
            throw error;
        }
    }
    
    /**
     * 同步离线消息
     * @param {string} userId 用户ID
     * @param {number} lastSequence 上次同步序列号
     * @param {number} maxLimit 最大条数限制
     * @returns {Promise<Object>} 同步结果
     */
    async syncOfflineMessages(userId, lastSequence = 0, maxLimit = 100) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
            maxLimit: maxLimit,
            operater: userId
        };
        
        const response = await this._post('/v1/message/syncOfflineMessageList', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '同步离线消息失败');
        }
        
        return response.data;
    }
    
    /**
     * 同步好友列表
     * @param {string} userId 用户ID
     * @param {number} lastSequence 上次同步序列号
     * @param {number} maxLimit 最大条数限制
     * @returns {Promise<Object>} 同步结果
     */
    async syncFriendshipList(userId, lastSequence = 0, maxLimit = 100) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
            maxLimit: maxLimit,
            operater: userId
        };
        
        const response = await this._post('/v1/friendship/syncFriendShipList', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '同步好友列表失败');
        }
        
        return response.data;
    }
    
    /**
     * 同步群组列表
     * @param {string} userId 用户ID
     * @param {number} lastSequence 上次同步序列号
     * @param {number} maxLimit 最大条数限制
     * @returns {Promise<Object>} 同步结果
     */
    async syncJoinedGroupList(userId, lastSequence = 0, maxLimit = 100) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
            maxLimit: maxLimit,
            operater: userId
        };
        
        const response = await this._post('/v1/group/syncJoinedGroup', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '同步群组列表失败');
        }
        
        return response.data;
    }
    
    /**
     * 同步会话列表
     * @param {string} userId 用户ID
     * @param {number} lastSequence 上次同步序列号
     * @param {number} maxLimit 最大条数限制
     * @returns {Promise<Object>} 同步结果
     */
    async syncConversationList(userId, lastSequence = 0, maxLimit = 100) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
            maxLimit: maxLimit,
            operater: userId
        };
        
        const response = await this._post('/v1/conversation/syncConversationList', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '同步会话列表失败');
        }
        
        return response.data;
    }
    
    /**
     * 批量同步多个会话的消息
     * @param {string} userId 用户ID
     * @param {Array<Object>} conversations 会话同步信息列表，每个元素包含conversationId、conversationType和sequence
     * @param {number} maxCount 单个会话最大拉取消息数量
     * @returns {Promise<Object>} 同步结果
     */
    async batchSyncMessage(userId, conversations, maxCount = 10) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            conversations: conversations,
            maxCount: maxCount,
            operater: userId
        };
        
        console.log('批量同步消息 - 请求参数:', req);
        
        // 使用专门的18001端口进行批量同步消息请求
        const messageStoreUrl = 'http://localhost:18001';
        const response = await this._postToSpecificUrl(messageStoreUrl, '/v1/message/batchSyncMessage', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '批量同步消息失败');
        }
        
        console.log('批量同步消息 - 响应结果:', response.data);
        return response.data;
    }
    
    /**
     * 向指定URL发送HTTP POST请求
     * @param {string} baseUrl 基础URL
     * @param {string} endpoint 接口路径
     * @param {Object} data 请求数据
     * @returns {Promise<Object>} 响应结果
     * @private
     */
    async _postToSpecificUrl(baseUrl, endpoint, data) {
        try {
            const url = `${baseUrl}${endpoint}`;
            console.log('向特定URL发送请求:', url);
            
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            
            // 为响应对象添加一个便捷的isOk方法
            result.isOk = function() {
                return this.code === 200 || this.code === 0;
            };
            
            return result;
        } catch (error) {
            console.error('HTTP请求失败:', error, '请求URL:', baseUrl + endpoint);
            return { 
                code: -1, 
                msg: error.message,
                isOk: function() { return false; }
            };
        }
    }
    
    /**
     * 发送HTTP POST请求
     * @param {string} endpoint 接口路径
     * @param {Object} data 请求数据
     * @returns {Promise<Object>} 响应结果
     * @private
     */
    async _post(endpoint, data) {
        try {
            const url = `${this.options.baseUrl}${endpoint}`;
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            
            // 为响应对象添加一个便捷的isOk方法
            result.isOk = function() {
                return this.code === 200 || this.code === 0;
            };
            
            return result;
        } catch (error) {
            console.error('HTTP请求失败:', error);
            return { 
                code: -1, 
                msg: error.message,
                isOk: function() { return false; }
            };
        }
    }
}

/**
 * IM客户端类
 * 整合WebSocket通信和消息处理功能
 * 提供统一的接口给前端使用
 */
class IMClient {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     */
    constructor(options = {}) {
        this.options = Object.assign({
            wsUrl: 'ws://localhost:19002/ws',
            apiBaseUrl: 'http://localhost:18000',
            callbacks: {}
        }, options);
        
        // 初始化回调函数
        this.callbacks = Object.assign({
            onConnected: () => {},
            onDisconnected: () => {},
            onError: () => {},
            onChatMessage: () => {},
            onGroupMessage: () => {},
            onSystemMessage: () => {},
            onFriendRequest: () => {},
            onFriendStatusChange: () => {},
            onGroupStatusChange: () => {},
            onMessageAck: () => {},
            onReadReceipt: () => {},
            onConversationsUpdated: () => {}, // 添加会话列表更新回调
            onConversationsCreated: () => {}, // 添加会话创建回调
            onMessageSyncComplete: () => {} // 消息同步完成回调
        }, this.options.callbacks);
        
        // 会话管理
        this.conversations = new Map();
        
        // 会话序列号管理 - 用于记录每个会话的序列号
        this.conversationSequences = new Map();
        
        // 消息缓存
        this.messageCache = new Map();
        
        // 已处理的消息ID集合，用于消息去重
        this.processedMessageIds = new Set();
        
        // 当前用户ID
        this.currentUserId = null;
        
        // 消息同步状态
        this.isMessageSyncInProgress = false;
        
        // 初始化WebSocket客户端
        this.wsClient = new WebSocketClient({
            url: this.options.wsUrl,
            apiBaseUrl: this.options.apiBaseUrl,
            onOpen: this._handleConnected.bind(this),
            onClose: this._handleDisconnected.bind(this),
            onError: this._handleError.bind(this),
            onMessage: this._handleMessage.bind(this)
        });
        
        // 初始化消息处理器，传入this引用
        this.messageHandler = new MessageHandler({
            wsClient: this.wsClient,
            imClient: this,  // 传入this引用
            onChatMessage: this.callbacks.onChatMessage,
            onGroupMessage: this.callbacks.onGroupMessage,
            onSystemMessage: this.callbacks.onSystemMessage,
            onFriendRequest: this.callbacks.onFriendRequest,
            onFriendStatusChange: this.callbacks.onFriendStatusChange,
            onGroupStatusChange: this.callbacks.onGroupStatusChange,
            onMessageAck: this.callbacks.onMessageAck,
            onReadReceipt: this.callbacks.onReadReceipt
        });
        
        // 初始化API客户端
        this.apiClient = new IMApiClient({
            baseUrl: this.options.apiBaseUrl
        });
    }
    
    /**
     * 连接IM服务器
     * @returns {Promise} 连接完成的Promise
     */
    connect() {
        return this.wsClient.connect();
    }
    
    /**
     * 断开连接
     */
    disconnect() {
        this.wsClient.disconnect();
    }
    
    /**
     * 用户登录
     * @param {Object} loginInfo 登录信息
     * @returns {Promise<Object>} 登录结果
     */
    async login(loginInfo) {
        try {
            console.log('开始登录流程:', loginInfo.userId);
            
            // 发送登录消息
            const loginResult = this.wsClient.sendLoginMessage({
                userId: loginInfo.userId,
                appId: loginInfo.appId || 10001,
                clientType: loginInfo.clientType || 1,
                imei: loginInfo.imei || 'web'
            });
            
            if (!loginResult) {
                throw new Error('发送登录消息失败');
            }
            
            // 保存用户信息
            this.wsClient.setUserInfo({
                userId: loginInfo.userId,
                appId: loginInfo.appId || 10001,
                clientType: loginInfo.clientType || 1,
                imei: loginInfo.imei || 'web'
            });
            
            this.currentUserId = loginInfo.userId;
            
            console.log('用户信息已保存，开始同步数据');
            
            // 等待同步数据完成
            try {
                // 同步离线消息、好友列表和群组列表，强制同步所有会话
                await this._syncData(true);
                console.log('数据同步完成，登录流程结束');
            } catch (syncError) {
                console.warn('数据同步部分失败，但不影响登录:', syncError);
                // 同步失败不阻止登录
            }
            
            return { success: true };
        } catch (error) {
            console.error('登录失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 同步数据（离线消息、好友列表、群组列表等）
     * @param {boolean} [forceSyncMessages=false] 是否强制同步消息，即使没有会话
     * @returns {Promise<void>}
     * @private
     */
    async _syncData(forceSyncMessages = false) {
        const userInfo = this.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            console.error('没有用户信息，无法同步数据');
            return;
        }
        
        console.log('开始数据同步流程, 强制同步消息:', forceSyncMessages);
        let syncErrors = [];
        
        // 同步离线消息
        try {
            await this.apiClient.syncOfflineMessages(userInfo.userId);
            console.log('同步离线消息成功');
        } catch (error) {
            console.error('同步离线消息失败:', error);
            syncErrors.push('离线消息同步失败');
        }
        
        // 同步好友列表
        try {
            await this.apiClient.syncFriendshipList(userInfo.userId);
            console.log('同步好友列表成功');
        } catch (error) {
            console.error('同步好友列表失败:', error);
            syncErrors.push('好友列表同步失败');
        }
        
        // 同步群组列表
        try {
            await this.apiClient.syncJoinedGroupList(userInfo.userId);
            console.log('同步群组列表成功');
        } catch (error) {
            console.error('同步群组列表失败:', error);
            syncErrors.push('群组列表同步失败');
        }
        
        // 同步会话消息 - 传入强制同步标志
        try {
            await this._syncConversationMessages(null, forceSyncMessages);
            console.log('同步会话消息成功, 强制同步:', forceSyncMessages);
        } catch (error) {
            console.error('同步会话消息失败:', error);
            syncErrors.push('会话消息同步失败');
        }
        
        // 如果有同步错误，记录但不阻止程序继续运行
        if (syncErrors.length > 0) {
            console.error('同步数据部分失败:', syncErrors.join(', '));
        } else {
            console.log('所有数据同步成功');
        }
    }
    
    /**
     * 发送私聊消息
     * @param {string} toUserId 接收者ID
     * @param {string} content 消息内容
     * @returns {Promise<Object>} 发送结果
     */
    async sendChatMessage(toUserId, content) {
        try {
            // 检查用户登录状态
            const userInfo = this.wsClient.getUserInfo();
            if (!userInfo || !userInfo.userId) {
                throw new Error('用户未登录');
            }
            
            // 检查内容是否为空
            if (!content || content.trim() === '') {
                throw new Error('消息内容不能为空');
            }
            
            // 检查接收者是否为空
            if (!toUserId || toUserId.trim() === '') {
                throw new Error('接收者不能为空');
            }
            
            // 创建消息对象
            const message = this.messageHandler.createMessage('chat', {
                fromId: userInfo.userId,
                toId: toUserId,
                content: content
            });
            
            // 添加到消息缓存
            this._addToMessageCache(message);
            
            // 发送消息
            const result = this.wsClient.sendChatMessage({
                fromId: userInfo.userId,
                toId: toUserId,
                content: content,
                appId: userInfo.appId,
                clientType: userInfo.clientType,
                imei: userInfo.imei
            });
            
            if (!result) {
                throw new Error('发送消息失败');
            }
            
            return { success: true, message };
        } catch (error) {
            console.error('发送私聊消息失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 发送群聊消息
     * @param {string} groupId 群组ID
     * @param {string} content 消息内容
     * @returns {Promise<Object>} 发送结果
     */
    async sendGroupMessage(groupId, content) {
        try {
            // 检查用户登录状态
            const userInfo = this.wsClient.getUserInfo();
            if (!userInfo || !userInfo.userId) {
                throw new Error('用户未登录');
            }
            
            // 检查内容是否为空
            if (!content || content.trim() === '') {
                throw new Error('消息内容不能为空');
            }
            
            // 检查群组ID是否为空
            if (!groupId || groupId.trim() === '') {
                throw new Error('群组ID不能为空');
            }
            
            // 创建消息对象
            const message = this.messageHandler.createMessage('group', {
                fromId: userInfo.userId,
                groupId: groupId,
                content: content
            });
            
            // 添加到消息缓存
            this._addToMessageCache(message);
            
            // 发送消息
            const result = this.wsClient.sendGroupMessage({
                fromId: userInfo.userId,
                groupId: groupId,
                content: content,
                appId: userInfo.appId,
                clientType: userInfo.clientType,
                imei: userInfo.imei
            });
            
            if (!result) {
                throw new Error('发送消息失败');
            }
            
            return { success: true, message };
        } catch (error) {
            console.error('发送群聊消息失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 获取好友列表
     * @returns {Promise<Array>} 好友列表
     */
    async getFriendList() {
        const userInfo = this.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            throw new Error('用户未登录');
        }
        
        return this.apiClient.getFriendList(userInfo.userId);
    }
    
    /**
     * 获取群组列表
     * @returns {Promise<Array>} 群组列表
     */
    async getGroupList() {
        const userInfo = this.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            throw new Error('用户未登录');
        }
        
        return this.apiClient.getGroupList(userInfo.userId);
    }
    
    /**
     * 创建群组
     * @param {Object} params 创建群组参数
     * @returns {Promise<Object>} 创建结果
     */
    async createGroup(params) {
        const userInfo = this.wsClient.getUserInfo();
        if (!userInfo || !userInfo.userId) {
            throw new Error('用户未登录');
        }
        
        return this.apiClient.createGroup({
            ...params,
            userId: userInfo.userId
        });
    }
    
    /**
     * 获取会话列表
     * @returns {Array} 会话列表
     */
    getConversationList() {
        return Array.from(this.conversations.values()).sort((a, b) => {
            return (b.lastMessage?.timestamp || 0) - (a.lastMessage?.timestamp || 0);
        });
    }
    
    /**
     * 获取消息列表
     * @param {string} conversationType 会话类型
     * @param {string} targetId 目标ID
     * @returns {Array} 消息列表
     */
    getMessageList(conversationType, targetId) {
        const conversationId = this._getConversationId(conversationType, targetId);
        return this.messageCache.get(conversationId) || [];
    }
    
    /**
     * 处理连接事件
     * @param {Event} event 事件对象
     * @private
     */
    _handleConnected(event) {
        console.log('WebSocket连接已建立');
        this.callbacks.onConnected(event);
    }
    
    /**
     * 处理断开连接事件
     * @param {Event} event 事件对象
     * @private
     */
    _handleDisconnected(event) {
        console.log('WebSocket连接已断开');
        this.callbacks.onDisconnected(event);
    }
    
    /**
     * 处理错误事件
     * @param {Event} event 事件对象
     * @private
     */
    _handleError(event) {
        console.error('WebSocket连接错误');
        this.callbacks.onError(event);
    }
    
    /**
     * 处理消息事件
     * @param {Object} message 消息对象
     * @private
     */
    _handleMessage(message) {
        try {
            // 检查消息是否为 null 或 undefined
            if (!message) {
                console.error('收到空消息');
                return;
            }
            
            // 检查消息是否为ByteBuffer实例或包含ByteBuffer特有的属性
            if (message._org_buf && message._list && message._littleEndian !== undefined) {
                console.error('收到未解析的ByteBuffer对象，跳过处理');
                return;
            }
            
            // 检查消息是否为原始事件对象（来自WebSocket）
            if (message instanceof Event && message.data) {
                console.error('收到原始WebSocket事件，而不是解析后的消息对象');
                return;
            }
            
            // 检查command属性是否存在且为数字
            if (typeof message.command !== 'number') {
                console.warn('消息的command属性不是数字:', message.command);
                
                // 尝试转换成数字
                if (message.command && !isNaN(Number(message.command))) {
                    message.command = Number(message.command);
                } else {
                    console.error('无法处理消息：无效的command属性', message);
                    return;
                }
            }
            
            // 检查是否有消息ID，如果有则进行去重检查
            if (message.body) {
                try {
                    const bodyObj = typeof message.body === 'string' ? 
                        JSON.parse(message.body) : message.body;
                    
                    let messageId = bodyObj.messageId;
                    
                    // 尝试从嵌套的data对象中获取messageId
                    if (!messageId && bodyObj.data && bodyObj.data.messageId) {
                        messageId = bodyObj.data.messageId;
                    }
                    
                    if (messageId && this.processedMessageIds) {
                        if (this.processedMessageIds.has(messageId)) {
                            console.log('跳过重复消息:', messageId);
                            return;
                        }
                        
                        // 记录消息ID
                        this.processedMessageIds.add(messageId);
                        
                        // 限制集合大小
                        if (this.processedMessageIds.size > 1000) {
                            // 移除最早加入的100个元素
                            const toRemove = Array.from(this.processedMessageIds).slice(0, 100);
                            toRemove.forEach(id => this.processedMessageIds.delete(id));
                        }
                    }
                } catch (e) {
                    // 解析失败，继续处理消息
                    console.warn('解析消息body时出错:', e);
                }
            }
            
            // 处理消息
            const processed = this.messageHandler.handleMessage(message);
            
            // 如果消息被成功处理，记录
            if (processed && processed.id) {
                // 确保集合已初始化
                if (!this.processedMessageIds) {
                    this.processedMessageIds = new Set();
                }
                this.processedMessageIds.add(processed.id);
            }
        } catch (error) {
            console.error('处理消息时出错:', error, message);
        }
    }
    
    /**
     * 同步当前可见会话的消息
     * @param {Array} [specificConversations] 指定需要同步的会话，如果为空则同步所有会话
     * @param {boolean} [forceSync=false] 是否强制同步，即使没有会话
     * @returns {Promise<void>}
     * @private
     */
    async _syncConversationMessages(specificConversations, forceSync = false) {
        // 设置同步状态为进行中
        this.isMessageSyncInProgress = true;
        console.log('消息同步开始, 强制同步:', forceSync);
        
        try {
            const userInfo = this.wsClient.getUserInfo();
            if (!userInfo || !userInfo.userId) {
                console.warn('用户未登录，无法同步会话消息');
                this.isMessageSyncInProgress = false;
                this.callbacks.onMessageSyncComplete(false, '用户未登录');
                return;
            }
            
            // 获取需要同步的会话列表
            let conversationsToSync = [];
            
            if (specificConversations && Array.isArray(specificConversations)) {
                // 使用指定的会话列表
                conversationsToSync = specificConversations;
            } else {
                // 使用当前所有会话
                conversationsToSync = Array.from(this.conversations.values());
            }
            
            // 如果会话列表为空且不强制同步，直接返回
            if (conversationsToSync.length === 0 && !forceSync) {
                console.log('没有会话需要同步，且未设置强制同步');
                this.isMessageSyncInProgress = false;
                this.callbacks.onMessageSyncComplete(true, '没有会话需要同步');
                return;
            }
            
            // 如果强制同步但没有会话，使用空会话列表的同步请求
            // 这会让服务器返回所有相关会话的最新消息
            let syncConversations = [];
            
            if (conversationsToSync.length === 0 && forceSync) {
                console.log('没有会话但设置了强制同步，使用默认同步请求');
                // 在这种情况下，我们发送一个空的同步请求列表
                // 服务器会根据用户ID自动返回相关的会话和消息
                
                // 调用批量同步API，使用较大的消息数量限制
                const result = await this.apiClient.batchSyncMessage(
                    userInfo.userId,
                    syncConversations,
                    30  // 每个会话拉取更多消息
                );
                
                // 处理同步结果（与正常流程相同）
                await this._processSyncMessageResult(result);
                return;
            }
            
            // 构建同步请求
            syncConversations = conversationsToSync.map(conv => {
                // 确定会话ID格式
                let conversationId = '';
                if (conv.type === 'C2C') {
                    conversationId = conv.targetId;
                } else if (conv.type === 'GROUP') {
                    conversationId = conv.targetId;
                } else {
                    console.warn('未知会话类型:', conv.type);
                    return null;
                }
                
                // 转换会话类型
                let conversationType;
                if (conv.type === 'C2C') {
                    conversationType = 1; // 单聊
                } else if (conv.type === 'GROUP') {
                    conversationType = 2; // 群聊
                } else {
                    conversationType = 0; // 未知类型
                }
                
                // 获取会话的当前序列号，如果没有则使用0
                const sequence = this.conversationSequences.get(conv.id) || 0;
                
                return {
                    conversationId: conversationId,
                    conversationType: conversationType,
                    sequence: sequence
                };
            }).filter(item => item !== null);  // 过滤掉无效的会话
            
            console.log('准备同步以下会话的消息:', syncConversations);
            
            // 调用批量同步API
            const result = await this.apiClient.batchSyncMessage(
                userInfo.userId,
                syncConversations,
                20  // 每个会话默认拉取20条消息，提高初始加载数量
            );
            
            // 处理同步结果
            await this._processSyncMessageResult(result);
        } catch (error) {
            console.error('同步会话消息时出错:', error);
            this.isMessageSyncInProgress = false;
            this.callbacks.onMessageSyncComplete(false, error.message);
            throw error;
        }
    }
    
    /**
     * 处理同步消息的结果
     * @param {Object} result 同步消息的结果
     * @returns {Promise<void>}
     * @private
     */
    async _processSyncMessageResult(result) {
        try {
            if (result && result.conversationList && Array.isArray(result.conversationList)) {
                console.log('同步会话消息成功，处理消息数据');
                
                // 创建新的会话列表（如果有消息返回但没有对应的会话）
                const newConversations = [];
                
                // 处理每个会话的消息
                result.conversationList.forEach(convResp => {
                    if (!convResp.conversationId || !convResp.conversationType) {
                        console.warn('会话响应缺少必要字段:', convResp);
                        return;
                    }
                    
                    // 确定会话类型
                    const type = convResp.conversationType === 1 ? 'C2C' : 'GROUP';
                    
                    // 根据会话类型和ID构建会话标识
                    const conversationId = this._getConversationId(type, convResp.conversationId);
                    
                    // 获取消息列表
                    const messages = convResp.dataList || [];
                    if (messages.length === 0) {
                        console.log(`会话 ${conversationId} 没有新消息`);
                        return;
                    }
                    
                    console.log(`会话 ${conversationId} 收到 ${messages.length} 条消息`);
                    
                    // 检查会话是否存在，如果不存在则创建
                    let conversation = this.conversations.get(conversationId);
                    if (!conversation) {
                        console.log(`会话 ${conversationId} 不存在，创建新会话`);
                        // 创建会话对象
                        conversation = {
                            id: conversationId,
                            type: type,
                            targetId: convResp.conversationId,
                            name: type === 'C2C' ? `用户 ${convResp.conversationId}` : `群组 ${convResp.conversationId}`,
                            lastMessage: {
                                content: '新消息',
                                timestamp: Date.now()
                            }
                        };
                        // 保存会话
                        this.conversations.set(conversationId, conversation);
                        // 添加到新创建的会话列表
                        newConversations.push(conversation);
                    }
                    
                    // 处理消息列表
                    let maxSequence = 0;
                    messages.forEach(msg => {
                        // 转换消息格式
                        const message = this._convertMessageFormat(msg, type);
                        
                        // 添加消息到缓存
                        this._addToMessageCache(message);
                        
                        // 记录最大序列号
                        if (msg.sequence && msg.sequence > maxSequence) {
                            maxSequence = msg.sequence;
                        }
                        
                        // 不论当前是否选中会话，都触发消息回调
                        if (type === 'C2C' && this.callbacks.onChatMessage) {
                            this.callbacks.onChatMessage(message);
                        } else if (type === 'GROUP' && this.callbacks.onGroupMessage) {
                            this.callbacks.onGroupMessage(message);
                        }
                    });
                    
                    // 更新会话序列号
                    if (maxSequence > 0) {
                        console.log(`更新会话 ${conversationId} 的序列号为 ${maxSequence}`);
                        this.conversationSequences.set(conversationId, maxSequence);
                    }
                    
                    // 更新会话的最后一条消息
                    if (messages.length > 0) {
                        // 按照序列号排序，获取最新的消息
                        const lastMsg = messages.sort((a, b) => b.sequence - a.sequence)[0];
                        const conversation = this.conversations.get(conversationId);
                        if (conversation) {
                            conversation.lastMessage = {
                                content: lastMsg.content,
                                timestamp: lastMsg.timestamp || Date.now()
                            };
                            this.conversations.set(conversationId, conversation);
                        }
                    }
                });
                
                // 如果有新创建的会话，通知UI更新
                if (newConversations.length > 0) {
                    console.log(`创建了 ${newConversations.length} 个新会话`);
                    if (this.callbacks.onConversationsCreated) {
                        this.callbacks.onConversationsCreated(newConversations);
                    }
                }
                
                // 更新UI
                if (this.callbacks.onConversationsUpdated) {
                    this.callbacks.onConversationsUpdated(Array.from(this.conversations.values()));
                }

                // 同步完成，设置状态并触发回调
                this.isMessageSyncInProgress = false;
                console.log('消息同步完成');
                this.callbacks.onMessageSyncComplete(true, '消息同步完成');
            } else {
                console.warn('同步会话消息返回数据格式不正确:', result);
                this.isMessageSyncInProgress = false;
                this.callbacks.onMessageSyncComplete(false, '同步返回数据格式不正确');
            }
        } catch (error) {
            console.error('处理同步消息结果时出错:', error);
            this.isMessageSyncInProgress = false;
            this.callbacks.onMessageSyncComplete(false, error.message);
            throw error;
        }
    }
    
    /**
     * 处理滚动加载历史消息
     * @param {string} conversationId 会话ID
     * @returns {Promise<boolean>} 是否成功加载更多消息
     */
    async handleScrollLoadMessages(conversationId) {
        try {
            // 检查是否有有效的会话ID
            if (!conversationId) {
                console.warn('滚动加载消息时未提供会话ID');
                return false;
            }
            
            // 解析会话ID
            const parts = conversationId.split('_');
            if (parts.length < 2) {
                console.warn('无效的会话ID格式:', conversationId);
                return false;
            }
            
            const type = parts[0];
            const targetId = parts[1];
            
            // 获取会话当前序列号
            const sequence = this.conversationSequences.get(conversationId) || 0;
            
            // 构建同步请求
            const conversationType = type === 'C2C' ? 1 : (type === 'GROUP' ? 2 : 0);
            const syncRequest = [{
                conversationId: targetId,
                conversationType: conversationType,
                sequence: sequence
            }];
            
            // 获取用户信息
            const userInfo = this.wsClient.getUserInfo();
            if (!userInfo || !userInfo.userId) {
                console.warn('用户未登录，无法加载更多消息');
                return false;
            }
            
            console.log(`准备加载会话 ${conversationId} 的更多消息，当前序列号: ${sequence}`);
            
            // 调用批量同步API
            const result = await this.apiClient.batchSyncMessage(
                userInfo.userId,
                syncRequest,
                20  // 滚动加载时拉取更多消息
            );
            
            // 处理同步结果
            if (result && result.conversationList && Array.isArray(result.conversationList) && result.conversationList.length > 0) {
                const convResp = result.conversationList[0];
                
                // 获取消息列表
                const messages = convResp.dataList || [];
                if (messages.length === 0) {
                    console.log(`会话 ${conversationId} 没有更多历史消息`);
                    return false;
                }
                
                console.log(`会话 ${conversationId} 加载到 ${messages.length} 条历史消息`);
                
                // 处理消息列表
                let maxSequence = 0;
                const newMessages = [];
                
                messages.forEach(msg => {
                    // 转换消息格式
                    const message = this._convertMessageFormat(msg, type);
                    
                    // 添加消息到缓存
                    this._addToMessageCache(message);
                    
                    // 记录最大序列号
                    if (msg.sequence && msg.sequence > maxSequence) {
                        maxSequence = msg.sequence;
                    }
                    
                    newMessages.push(message);
                });
                
                // 触发历史消息加载回调
                if (this.callbacks.onHistoryMessagesLoaded) {
                    this.callbacks.onHistoryMessagesLoaded(conversationId, newMessages);
                }
                
                // 更新会话序列号（如果加载到更早的消息，不更新序列号）
                if (maxSequence > sequence) {
                    console.log(`更新会话 ${conversationId} 的序列号为 ${maxSequence}`);
                    this.conversationSequences.set(conversationId, maxSequence);
                }
                
                return true;
        } else {
                console.warn('加载更多消息返回数据格式不正确:', result);
                return false;
            }
        } catch (error) {
            console.error('滚动加载历史消息时出错:', error);
            return false;
        }
    }
    
    /**
     * 转换消息格式
     * @param {Object} msg 服务器返回的消息对象
     * @param {string} type 会话类型
     * @returns {Object} 转换后的消息对象
     * @private
     */
    _convertMessageFormat(msg, type) {
        // 基础消息结构
        const message = {
            id: msg.messageId || msg.messageKey || `msg_${Date.now()}_${Math.random().toString(36).slice(2)}`,
            fromId: msg.fromId,
            content: msg.content || '',
            timestamp: msg.createTime || msg.timestamp || Date.now(),
            status: 'sent',
            type: type === 'C2C' ? 'chat' : 'group'
        };
        
        // 根据会话类型添加其他字段
        if (type === 'C2C') {
            message.toId = msg.toId;
        } else if (type === 'GROUP') {
            message.groupId = msg.groupId || msg.toId;
            message.toId = msg.groupId || msg.toId;
        }
        
        // 判断是否自己发送的消息
        if (message.fromId === this.currentUserId) {
            message.isSelf = true;
        }
        
        return message;
    }
    
    /**
     * 生成会话ID
     * @param {string} type 会话类型
     * @param {string} targetId 目标ID
     * @returns {string} 会话ID
     * @private
     */
    _getConversationId(type, targetId) {
        return `${type}_${targetId}`;
    }
    
    /**
     * 更新会话
     * @param {Object} conversation 会话对象
     * @private
     */
    _updateConversation(conversation) {
        const id = this._getConversationId(conversation.type, conversation.targetId);
        
        // 检查会话是否已存在
        let existingConversation = this.conversations.get(id);
        
        if (existingConversation) {
            // 更新现有会话
            existingConversation.lastMessage = conversation.lastMessage;
            
            // 增加未读消息计数
            if (conversation.lastMessage && conversation.lastMessage.fromId !== this.currentUserId) {
                existingConversation.unreadCount = (existingConversation.unreadCount || 0) + 1;
            }
        } else {
            // 创建新会话
            existingConversation = {
                id,
                type: conversation.type,
                targetId: conversation.targetId,
                lastMessage: conversation.lastMessage,
                unreadCount: conversation.lastMessage && conversation.lastMessage.fromId !== this.currentUserId ? 1 : 0
            };
        }
        
        // 保存会话
        this.conversations.set(id, existingConversation);
        
        return existingConversation;
    }
    
    /**
     * 添加消息到缓存
     * @param {Object} message 消息对象
     * @private
     */
    _addToMessageCache(message) {
        let conversationType, targetId;
        
        console.log('添加消息到缓存:', message.type, message.id);
        
        if (message.type === 'chat') {
            conversationType = 'C2C';
            
            // 确定会话ID（私聊消息的目标ID取决于发送方向）
            if (message.fromId === this.currentUserId) {
                targetId = message.toId;
            } else {
                targetId = message.fromId;
            }
        } else if (message.type === 'group') {
            conversationType = 'GROUP';
            targetId = message.groupId;
        } else {
            // 不缓存其他类型的消息
            console.log('不缓存该类型消息:', message.type);
            return;
        }
        
        const conversationId = this._getConversationId(conversationType, targetId);
        console.log('使用会话ID:', conversationId);
        
        // 如果消息是由当前用户发送的，更新会话（不删除缓存）
        if (message.fromId === this.currentUserId) {
            console.log('当前用户发送的消息，更新会话:', conversationId);
            // 获取现有消息列表或创建新列表
            let messageList = this.messageCache.get(conversationId);
            if (!messageList) {
                messageList = [];
                this.messageCache.set(conversationId, messageList);
            }
            
            // 检查是否是已存在的消息，如果是则更新状态
            const existingMessageIndex = messageList.findIndex(m => m.id === message.id);
            if (existingMessageIndex !== -1) {
                // 更新现有消息，保留原始状态除非新状态更高级
                const existingMessage = messageList[existingMessageIndex];
                const statusPriority = {
                    'sending': 1,
                    'sent': 2,
                    'delivered': 3,
                    'read': 4
                };
                
                // 只有当新状态优先级更高时才更新
                if (message.status && statusPriority[message.status] > (statusPriority[existingMessage.status] || 0)) {
                    console.log(`更新消息(${message.id})状态: ${existingMessage.status} -> ${message.status}`);
                    existingMessage.status = message.status;
                } else if (message.status && !existingMessage.status) {
                    // 如果原消息没有状态但新消息有状态
                    existingMessage.status = message.status;
                }
                
                // 更新其他属性
                Object.assign(existingMessage, message);
            } else {
                // 添加新消息到列表
                messageList.push(message);
                console.log(`添加新消息到缓存: ${message.id}, 当前会话消息数: ${messageList.length}`);
                
                // 限制缓存大小（每个会话最多保留100条消息）
                if (messageList.length > 100) {
                    messageList.shift();
                }
            }
            
            // 更新会话
            this._updateConversation({
                type: conversationType,
                targetId,
                lastMessage: message
            });
            
            return messageList;
        }
        
        // 检查是否已存在相同ID的消息
        if (message.id) {
            let messageList = this.messageCache.get(conversationId);
            if (messageList) {
                const existingMessageIndex = messageList.findIndex(m => m.id === message.id);
                if (existingMessageIndex !== -1) {
                    console.log('更新已存在的消息:', message.id);
                    // 更新现有消息的状态和内容
                    const existingMessage = messageList[existingMessageIndex];
                    
                    // 如果新消息有状态且原消息没有状态，或者新状态不是"sending"，则更新状态
                    if ((message.status && !existingMessage.status) || 
                        (message.status && message.status !== 'sending')) {
                        existingMessage.status = message.status;
                        console.log(`设置消息(${message.id})状态为: ${message.status}`);
                    }
                    
                    // 更新其他属性，但保留状态
                    const originalStatus = existingMessage.status;
                    Object.assign(existingMessage, message);
                    if (originalStatus && originalStatus !== 'sending') {
                        existingMessage.status = originalStatus;
                    }
                    
                    // 更新会话的最后一条消息
                    let existingConversation = this.conversations.get(conversationId);
                    if (existingConversation) {
                        existingConversation.lastMessage = message;
                        this.conversations.set(conversationId, existingConversation);
                    } else {
                        // 会话不存在，创建一个新会话
                        console.log(`会话 ${conversationId} 不存在，创建新会话`);
                        this._createNewConversation(conversationType, targetId, message);
                    }
                    
                    return messageList;
                }
            }
        }
        
        // 获取现有消息列表或创建新列表
        let messageList = this.messageCache.get(conversationId);
        if (!messageList) {
            messageList = [];
            this.messageCache.set(conversationId, messageList);
            console.log(`为会话 ${conversationId} 创建消息列表`);
        }
        
        // 添加消息
        messageList.push(message);
        console.log(`添加新消息到会话 ${conversationId}, 消息ID: ${message.id}, 当前会话消息数: ${messageList.length}`);
        
        // 限制缓存大小（每个会话最多保留100条消息）
        if (messageList.length > 100) {
            messageList.shift();
        }
        
        // 检查会话是否存在
        let existingConversation = this.conversations.get(conversationId);
        if (!existingConversation) {
            // 会话不存在，创建新会话
            console.log(`会话 ${conversationId} 不存在，创建新会话`);
            existingConversation = this._createNewConversation(conversationType, targetId, message);
        } else {
            // 更新现有会话
            existingConversation.lastMessage = {
                content: message.content,
                timestamp: message.timestamp
            };
            
            // 消息不是当前用户发送的，增加未读计数
            if (message.fromId !== this.currentUserId) {
                existingConversation.unreadCount = (existingConversation.unreadCount || 0) + 1;
                console.log(`增加会话 ${conversationId} 未读计数: ${existingConversation.unreadCount}`);
            }
            
            this.conversations.set(conversationId, existingConversation);
        }
        
        // 触发会话更新回调
        if (this.callbacks.onConversationsUpdated) {
            this.callbacks.onConversationsUpdated(Array.from(this.conversations.values()));
        }
        
        return messageList;
    }
    
    /**
     * 创建新会话
     * @param {string} conversationType 会话类型
     * @param {string} targetId 目标ID
     * @param {Object} message 消息对象
     * @returns {Object} 创建的会话对象
     * @private
     */
    _createNewConversation(conversationType, targetId, message) {
        const conversationId = this._getConversationId(conversationType, targetId);
        
        // 创建会话对象
        const conversation = {
            id: conversationId,
            type: conversationType,
            targetId: targetId,
            name: conversationType === 'C2C' ? `用户 ${targetId}` : `群组 ${targetId}`,
            lastMessage: {
                content: message.content,
                timestamp: message.timestamp
            },
            unreadCount: message.fromId !== this.currentUserId ? 1 : 0
        };
        
        // 保存会话
        this.conversations.set(conversationId, conversation);
        console.log(`创建新会话: ${conversationId}, 类型: ${conversationType}`);
        
        // 触发会话创建回调
        if (this.callbacks.onConversationsCreated) {
            this.callbacks.onConversationsCreated([conversation]);
        }
        
        return conversation;
    }
    
    /**
     * 清除会话未读计数
     * @param {string} conversationType 会话类型
     * @param {string} targetId 目标ID
     * @returns {Object|null} 更新后的会话，如果不存在则返回null
     */
    clearUnreadCount(conversationType, targetId) {
        const conversationId = this._getConversationId(conversationType, targetId);
        const conversation = this.conversations.get(conversationId);
        
        if (conversation) {
            conversation.unreadCount = 0;
            this.conversations.set(conversationId, conversation);
            return conversation;
        }
        
        return null;
    }
    
    /**
     * 添加好友
     * @param {Object} params 添加好友参数
     * @returns {Promise<Object>} 添加结果
     */
    async addFriend(params) {
        try {
            // 检查WebSocket客户端是否初始化
            if (!this.wsClient) {
                throw new Error('WebSocket客户端未初始化');
            }
            
            // 安全获取用户信息
            if (typeof this.wsClient.getUserInfo !== 'function') {
                throw new Error('WebSocket客户端不支持getUserInfo方法');
            }
            
            const userInfo = this.wsClient.getUserInfo();
            console.log('添加好友 - 当前用户信息:', userInfo);
            
            if (!userInfo || !userInfo.userId) {
                throw new Error('用户未登录');
            }
            
            if (!params.toId) {
                throw new Error('缺少必要参数: toId');
            }
            
            const requestParams = {
                ...params,
                fromId: userInfo.userId,
                clientType: userInfo.clientType || 1,
                imei: userInfo.imei || 'web'
            };
            
            console.log('添加好友 - 发送完整参数:', requestParams);
            
            // 检查apiClient是否存在
            if (!this.apiClient) {
                throw new Error('API客户端未初始化');
            }
            
            // 检查apiClient.addFriend方法是否存在
            if (typeof this.apiClient.addFriend !== 'function') {
                throw new Error('API客户端不支持addFriend方法');
            }
            
            const result = await this.apiClient.addFriend(requestParams);
            console.log('添加好友 - API返回结果:', result);
            
            // 若API返回null但未抛出异常，说明操作可能已成功
            // 后端API可能不返回任何数据，但请求本身是成功的
            if (result === null || result === undefined) {
                console.log('添加好友 - API返回null，但请求已成功处理，返回成功对象');
                return {
                    success: true,
                    message: '添加好友请求已发送',
                    fromId: userInfo.userId,
                    toId: params.toId
                };
            }
            
            return result;
        } catch (error) {
            console.error('添加好友失败:', error);
            
            // 安全记录错误详情
            const errorDetails = {
                message: error.message,
                stack: error.stack,
                params: JSON.stringify(params)
            };
            
            // 使用已有的用户信息，而不是尝试从wsClient获取
            if (userInfo) {
                try {
                    errorDetails.userInfo = JSON.stringify(userInfo);
                } catch (e) {
                    errorDetails.userInfoError = e.message;
                }
            } else {
                errorDetails.userInfo = '无法获取用户信息';
            }
            
            console.error('添加好友失败 - 详细信息:', errorDetails);
            throw error;
        }
    }
}

// 导出为全局变量，以便在不支持ES6模块的环境中使用
window.MessageHandler = MessageHandler;
window.IMApiClient = IMApiClient;
window.IMClient = IMClient; 