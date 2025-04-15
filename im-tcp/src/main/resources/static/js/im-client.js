/**
 * IM客户端类
 * 整合WebSocket通信和消息处理功能
 * 提供统一的接口给前端使用
 */
class IMClient {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     * @param {string} options.wsUrl WebSocket服务器URL
     * @param {string} options.apiBaseUrl 后端API基础URL
     * @param {Object} options.callbacks 回调函数集合
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
            onReadReceipt: () => {}
        }, this.options.callbacks);
        
        // 初始化WebSocket客户端
        this.wsClient = new WebSocketClient({
            url: this.options.wsUrl,
            apiBaseUrl: this.options.apiBaseUrl,
            onOpen: this._handleConnected.bind(this),
            onClose: this._handleDisconnected.bind(this),
            onError: this._handleError.bind(this),
            onMessage: this._handleMessage.bind(this)
        });
        
        // 初始化消息处理器
        this.messageHandler = new MessageHandler({
            wsClient: this.wsClient,
            onChatMessage: this.callbacks.onChatMessage,
            onGroupMessage: this.callbacks.onGroupMessage,
            onSystemMessage: this.callbacks.onSystemMessage,
            onFriendRequest: this.callbacks.onFriendRequest,
            onFriendStatusChange: this.callbacks.onFriendStatusChange,
            onGroupStatusChange: this.callbacks.onGroupStatusChange,
            onMessageAck: this.callbacks.onMessageAck,
            onReadReceipt: this.callbacks.onReadReceipt
        });
        
        // 会话管理
        this.conversations = new Map();
        
        // 消息缓存
        this.messageCache = new Map();
        
        // 当前用户ID
        this.currentUserId = null;
    }
    
    /**
     * 连接IM服务器
     */
    connect() {
        this.wsClient.connect();
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
            
            // 同步离线消息、好友列表和群组列表
            this._syncData();
            
            return { success: true };
        } catch (error) {
            console.error('登录失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 发送私聊消息
     * @param {string} toUserId 接收用户ID
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
            
            // 添加到会话列表
            this._updateConversation({
                type: 'C2C',
                targetId: toUserId,
                lastMessage: message
            });
            
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
            
            // 添加到会话列表
            this._updateConversation({
                type: 'GROUP',
                targetId: groupId,
                lastMessage: message
            });
            
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
        try {
            return await this.wsClient.getFriendshipList();
        } catch (error) {
            console.error('获取好友列表失败:', error);
            return [];
        }
    }
    
    /**
     * 获取群组列表
     * @returns {Promise<Array>} 群组列表
     */
    async getGroupList() {
        try {
            return await this.wsClient.getJoinedGroups();
        } catch (error) {
            console.error('获取群组列表失败:', error);
            return [];
        }
    }
    
    /**
     * 添加好友
     * @param {Object} params 添加好友参数
     * @returns {Promise<Object>} 添加结果
     */
    async addFriend(params) {
        try {
            return await this.wsClient.addFriend(params);
        } catch (error) {
            console.error('添加好友失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 创建群组
     * @param {Object} params 创建群组参数
     * @returns {Promise<Object>} 创建结果
     */
    async createGroup(params) {
        try {
            return await this.wsClient.createGroup(params);
        } catch (error) {
            console.error('创建群组失败:', error);
            return { success: false, error: error.message };
        }
    }
    
    /**
     * 获取会话列表
     * @returns {Array} 会话列表
     */
    getConversationList() {
        return Array.from(this.conversations.values())
            .sort((a, b) => b.lastMessage.timestamp - a.lastMessage.timestamp);
    }
    
    /**
     * 获取指定会话的消息列表
     * @param {string} conversationType 会话类型，'C2C'或'GROUP'
     * @param {string} targetId 目标ID，用户ID或群组ID
     * @returns {Array} 消息列表
     */
    getMessageList(conversationType, targetId) {
        const cacheKey = this._getConversationId(conversationType, targetId);
        return this.messageCache.has(cacheKey) 
            ? [...this.messageCache.get(cacheKey)].sort((a, b) => a.timestamp - b.timestamp)
            : [];
    }
    
    /**
     * 处理WebSocket连接成功
     * @param {Event} event 
     * @private
     */
    _handleConnected(event) {
        console.log('IM服务器连接成功');
        this.callbacks.onConnected(event);
    }
    
    /**
     * 处理WebSocket连接断开
     * @param {Event} event 
     * @private
     */
    _handleDisconnected(event) {
        console.log('IM服务器连接断开');
        this.callbacks.onDisconnected(event);
    }
    
    /**
     * 处理WebSocket错误
     * @param {Event} event 
     * @private
     */
    _handleError(event) {
        console.error('IM服务器连接错误:', event);
        this.callbacks.onError(event);
    }
    
    /**
     * 处理WebSocket消息
     * @param {Object} message 
     * @private
     */
    _handleMessage(message) {
        this.messageHandler.handleMessage(message);
    }
    
    /**
     * 同步数据（离线消息、好友列表、群组列表）
     * @private
     */
    async _syncData() {
        try {
            // 同步离线消息
            await this.wsClient.syncOfflineMessages();
            
            // 同步好友列表
            await this.wsClient.syncFriendshipList();
            
            // 同步群组列表
            await this.wsClient.syncJoinedGroupList();
            
            console.log('数据同步完成');
        } catch (error) {
            console.error('数据同步失败:', error);
        }
    }
    
    /**
     * 获取会话ID
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
        const conversationId = this._getConversationId(conversation.type, conversation.targetId);
        
        if (this.conversations.has(conversationId)) {
            // 更新现有会话
            const existingConversation = this.conversations.get(conversationId);
            existingConversation.lastMessage = conversation.lastMessage;
            existingConversation.unreadCount = existingConversation.unreadCount || 0;
            
            // 如果消息不是自己发的，增加未读数
            if (conversation.lastMessage.fromId !== this.currentUserId) {
                existingConversation.unreadCount++;
            }
        } else {
            // 创建新会话
            this.conversations.set(conversationId, {
                id: conversationId,
                type: conversation.type,
                targetId: conversation.targetId,
                lastMessage: conversation.lastMessage,
                unreadCount: conversation.lastMessage.fromId !== this.currentUserId ? 1 : 0
            });
        }
    }
    
    /**
     * 添加消息到缓存
     * @param {Object} message 消息对象
     * @private
     */
    _addToMessageCache(message) {
        let conversationType, targetId;
        
        if (message.type === 'chat') {
            conversationType = 'C2C';
            targetId = message.fromId === this.currentUserId ? message.toId : message.fromId;
        } else if (message.type === 'group') {
            conversationType = 'GROUP';
            targetId = message.groupId;
        } else {
            return; // 不缓存其他类型消息
        }
        
        const cacheKey = this._getConversationId(conversationType, targetId);
        
        if (!this.messageCache.has(cacheKey)) {
            this.messageCache.set(cacheKey, []);
        }
        
        // 添加消息到缓存
        this.messageCache.get(cacheKey).push(message);
        
        // 限制缓存大小
        const messages = this.messageCache.get(cacheKey);
        if (messages.length > 100) {
            messages.splice(0, messages.length - 100);
        }
        
        // 更新会话
        this._updateConversation({
            type: conversationType,
            targetId: targetId,
            lastMessage: message
        });
    }
    
    /**
     * 清空会话未读数
     * @param {string} conversationType 会话类型
     * @param {string} targetId 目标ID
     */
    clearUnreadCount(conversationType, targetId) {
        const conversationId = this._getConversationId(conversationType, targetId);
        
        if (this.conversations.has(conversationId)) {
            const conversation = this.conversations.get(conversationId);
            conversation.unreadCount = 0;
        }
    }
}

// 导出IMClient类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = IMClient;
} 