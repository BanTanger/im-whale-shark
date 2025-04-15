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
     */
    constructor(options = {}) {
        this.options = Object.assign({
            onChatMessage: () => {},
            onGroupMessage: () => {},
            onSystemMessage: () => {},
            onMessageAck: () => {},
            onReadReceipt: () => {}
        }, options);
        
        // 消息命令码常量
        this.COMMAND = {
            CHAT_MESSAGE: 1103,
            GROUP_MESSAGE: 2104,
            CHAT_READ_RECEIPT: 1106,
            GROUP_READ_RECEIPT: 2106,
            MESSAGE_ACK: 1107,
            SYSTEM_MESSAGE: 9999
        };
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
                    content: data.content,
                    userId: data.userId || 'system'
                };
                
            default:
                throw new Error(`未知的消息类型: ${type}`);
        }
    }
    
    /**
     * 生成消息ID
     * @returns {string} 消息ID
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