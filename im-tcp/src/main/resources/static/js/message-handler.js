/**
 * 消息处理器类
 * 负责解析和处理不同类型的WebSocket消息
 */
class MessageHandler {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     * @param {Function} options.onChatMessage 收到聊天消息的回调
     * @param {Function} options.onGroupMessage 收到群聊消息的回调
     * @param {Function} options.onSystemMessage 收到系统消息的回调
     * @param {Function} options.onMessageAck 收到消息确认的回调
     * @param {Function} options.onReadReceipt 收到已读回执的回调
     * @param {Function} options.onFriendRequest 收到好友请求的回调
     * @param {Function} options.onFriendStatusChange 好友状态变更的回调
     * @param {Function} options.onGroupStatusChange 群组状态变更的回调
     * @param {WebSocketClient} options.wsClient WebSocket客户端实例
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
            wsClient: null
        }, options);
        
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
            GROUP_STATUS_CHANGE: 4000
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
            switch (message.command) {
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
                
                case this.COMMAND.SYSTEM_MESSAGE:
                    return this._handleSystemMessage(message);
                
                case this.COMMAND.FRIEND_REQUEST:
                    return this._handleFriendRequest(message);
                
                case this.COMMAND.FRIEND_STATUS_CHANGE:
                    return this._handleFriendStatusChange(message);
                
                case this.COMMAND.GROUP_STATUS_CHANGE:
                    return this._handleGroupStatusChange(message);
                
                default:
                    console.log('未知消息类型:', message.command);
                    return null;
            }
        } catch (error) {
            console.error('处理消息时出错:', error);
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
            
            const processedMessage = {
                type: 'chat',
                id: data.messageId,
                fromId: data.fromId,
                toId: data.toId,
                content: data.messageBody,
                timestamp: data.messageTime || Date.now(),
                sequence: data.messageSequence,
                status: 'received',
                key: data.messageKey
            };
            
            // 如果有messageId，记录为已处理
            if (data.messageId) {
                this.processedMessages.add(data.messageId);
                
                // 清理过期的消息ID，避免内存泄漏
                if (this.processedMessages.size > 1000) {
                    this._cleanProcessedMessages();
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
            const data = JSON.parse(message.body);
            
            // 检查是否已处理过该消息
            if (data.messageId && this.processedMessages.has(data.messageId)) {
                console.log('跳过重复消息:', data.messageId);
                return null;
            }
            
            const processedMessage = {
                type: 'group',
                id: data.messageId,
                fromId: data.fromId,
                groupId: data.groupId,
                content: data.messageBody,
                timestamp: data.messageTime || Date.now(),
                sequence: data.messageSequence,
                status: 'received',
                key: data.messageKey
            };
            
            // 如果有messageId，记录为已处理
            if (data.messageId) {
                this.processedMessages.add(data.messageId);
                
                // 清理过期的消息ID，避免内存泄漏
                if (this.processedMessages.size > 1000) {
                    this._cleanProcessedMessages();
                }
            }
            
            // 自动发送已读回执
            this._sendGroupReadReceipt(processedMessage);
            
            this.options.onGroupMessage(processedMessage);
            return processedMessage;
        } catch (error) {
            console.error('处理群聊消息时出错:', error);
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
            const processedReceipt = {
                type: isGroup ? 'groupReadReceipt' : 'chatReadReceipt',
                fromId: data.fromId,
                toId: data.toId,
                conversationType: data.conversationType,
                messageSequence: data.messageSequence,
                timestamp: Date.now()
            };
            
            if (isGroup) {
                processedReceipt.groupId = data.groupId;
            }
            
            this.options.onReadReceipt(processedReceipt);
            return processedReceipt;
        } catch (error) {
            console.error('处理已读回执时出错:', error);
            return null;
        }
    }
    
    /**
     * 处理消息确认
     * @param {Object} message 原始消息对象
     * @returns {Object} 处理后的消息对象
     * @private
     */
    _handleMessageAck(message) {
        try {
            const data = JSON.parse(message.body);
            const processedAck = {
                type: 'messageAck',
                fromId: data.fromId,
                toId: data.toId,
                messageKey: data.messageKey,
                messageId: data.messageId,
                messageSequence: data.messageSequence,
                timestamp: Date.now()
            };
            
            this.options.onMessageAck(processedAck);
            return processedAck;
        } catch (error) {
            console.error('处理消息确认时出错:', error);
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
            try {
                data = JSON.parse(message.body);
            } catch (e) {
                // 如果不是JSON格式，尝试其他格式
                if (typeof message.body === 'string' && message.body.includes('system')) {
                    const matches = message.body.match(/userId":"(.*?)".*data":"(.*?)"/);
                    if (matches && matches.length >= 3) {
                        data = {
                            userId: matches[1],
                            data: matches[2]
                        };
                    } else {
                        data = { data: message.body };
                    }
                } else {
                    data = { data: message.body };
                }
            }
            
            const processedMessage = {
                type: 'system',
                userId: data.userId || 'system',
                content: data.data || data,
                timestamp: Date.now()
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
                type: 'friendRequest',
                fromId: data.fromId,
                addSource: data.addSource,
                addWording: data.addWording,
                timestamp: Date.now()
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
                type: 'friendStatusChange',
                fromId: data.fromId,
                toId: data.toId,
                status: data.status,
                timestamp: Date.now()
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
                type: 'groupStatusChange',
                groupId: data.groupId,
                operationType: data.operationType,
                operator: data.operator,
                timestamp: Date.now()
            };
            
            this.options.onGroupStatusChange(processedChange);
            return processedChange;
        } catch (error) {
            console.error('处理群组状态变更时出错:', error);
            return null;
        }
    }
    
    /**
     * 清理已处理的消息ID集合
     * @private
     */
    _cleanProcessedMessages() {
        // 保留最近500条消息ID
        if (this.processedMessages.size > 500) {
            const toRemove = this.processedMessages.size - 500;
            const iterator = this.processedMessages.values();
            for (let i = 0; i < toRemove; i++) {
                this.processedMessages.delete(iterator.next().value);
            }
        }
    }
    
    /**
     * 发送消息确认回执
     * @param {Object} message 处理后的消息对象 
     * @private
     */
    _sendMessageAck(message) {
        if (!this.options.wsClient || message.fromId === this.options.wsClient.getUserInfo()?.userId) {
            // 不给自己发送的消息发送确认回执
            return;
        }
        
        const userInfo = this.options.wsClient.getUserInfo();
        if (!userInfo) {
            console.error('用户未登录，无法发送消息确认');
            return;
        }
        
        this.options.wsClient.sendMessageAck({
            fromId: userInfo.userId,
            toId: message.fromId,
            messageKey: message.key,
            messageId: message.id,
            messageSequence: message.sequence,
            appId: userInfo.appId,
            clientType: userInfo.clientType,
            imei: userInfo.imei
        });
    }
    
    /**
     * 发送群聊消息已读回执
     * @param {Object} message 处理后的消息对象
     * @private
     */
    _sendGroupReadReceipt(message) {
        if (!this.options.wsClient || message.fromId === this.options.wsClient.getUserInfo()?.userId) {
            // 不给自己发送的消息发送已读回执
            return;
        }
        
        const userInfo = this.options.wsClient.getUserInfo();
        if (!userInfo) {
            console.error('用户未登录，无法发送已读回执');
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
        const baseMessage = {
            id: this._generateId(),
            timestamp: Date.now(),
            status: 'sending'
        };
        
        switch (type) {
            case 'chat':
                return {
                    ...baseMessage,
                    type: 'chat',
                    fromId: data.fromId,
                    toId: data.toId,
                    content: data.content
                };
                
            case 'group':
                return {
                    ...baseMessage,
                    type: 'group',
                    fromId: data.fromId,
                    groupId: data.groupId,
                    content: data.content
                };
                
            case 'system':
                return {
                    ...baseMessage,
                    type: 'system',
                    userId: data.userId || 'system',
                    content: data.content
                };
                
            default:
                console.error('未知消息类型:', type);
                return null;
        }
    }
    
    /**
     * 生成唯一ID
     * @returns {string} 唯一ID
     * @private
     */
    _generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substr(2, 5);
    }
}

// 导出MessageHandler类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = MessageHandler;
} 