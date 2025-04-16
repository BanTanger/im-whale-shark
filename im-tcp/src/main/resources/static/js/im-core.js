/**
 * IM核心模块
 * 整合 ByteBuffer 和 WebSocketClient 功能
 */

/**
 * ByteBuffer类
 * 用于处理二进制数据的读写
 */
class ByteBuffer {
    constructor(arrayBuf, offset) {
        const Type_Byte = 1;
        const Type_Short = 2;
        const Type_UShort = 3;
        const Type_Int32 = 4;
        const Type_UInt32 = 5;
        const Type_String = 6; // 变长字符串，前两个字节表示长度
        const Type_VString = 7; // 定长字符串
        const Type_Int64 = 8;
        const Type_Float = 9;
        const Type_Double = 10;
        const Type_ByteArray = 11;

        this._org_buf = arrayBuf ? (arrayBuf.constructor == DataView ? arrayBuf : (arrayBuf.constructor == Uint8Array ? new DataView(arrayBuf.buffer, offset) : new DataView(arrayBuf, offset))) : new DataView(new Uint8Array([]).buffer);
        this._offset = offset || 0;
        this._list = [];
        this._littleEndian = false;
        
        // 兼容性处理
        if (!ArrayBuffer.prototype.slice) {
            ArrayBuffer.prototype.slice = function (start, end) {
                const that = new Uint8Array(this);
                if (end == undefined) end = that.length;
                const result = new ArrayBuffer(end - start);
                const resultArray = new Uint8Array(result);
                for (let i = 0; i < resultArray.length; i++)
                    resultArray[i] = that[i + start];
                return result;
            };
        }
    }

    // 指定字节序 为BigEndian
    bigEndian() {
        this._littleEndian = false;
        return this;
    }

    // 指定字节序 为LittleEndian
    littleEndian() {
        this._littleEndian = true;
        return this;
    }

    // 获取bytebuffer的长度
    blength() {
        return this._offset;
    }

    // 字节操作
    byte(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getUint8(this._offset, this._littleEndian));
            this._offset += 1;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 1, d: val, l: 1 });
            this._offset += 1;
        }
        return this;
    }

    // 短整数操作
    short(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getInt16(this._offset, this._littleEndian));
            this._offset += 2;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 2, d: val, l: 2 });
            this._offset += 2;
        }
        return this;
    }

    // 无符号短整数操作
    ushort(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getUint16(this._offset, this._littleEndian));
            this._offset += 2;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 3, d: val, l: 2 });
            this._offset += 2;
        }
        return this;
    }

    // 32位整数操作
    int32(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getInt32(this._offset, this._littleEndian));
            this._offset += 4;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 4, d: val, l: 4 });
            this._offset += 4;
        }
        return this;
    }

    // 无符号32位整数操作
    uint32(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getUint32(this._offset, this._littleEndian));
            this._offset += 4;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 5, d: val, l: 4 });
            this._offset += 4;
        }
        return this;
    }

    // 变长字符串 前4个字节表示字符串长度
    string(val, index) {
        if (arguments.length == 0) {
            const len = this._org_buf.getInt32(this._offset, this._littleEndian);
            this._offset += 4;
            this._list.push(ByteBuffer.utf8Read(this._org_buf, this._offset, len));
            this._offset += len;
        } else {
            let len = 0;
            if (val) {
                len = ByteBuffer.utf8Length(val);
            }
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 6, d: val, l: len });
            this._offset += len + 4;
        }
        return this;
    }

    // 定长字符串
    vstring(val, len, index) {
        if (!len) {
            throw new Error('vstring must got len argument');
            return this;
        }
        if (val == undefined || val == null) {
            let vlen = 0; // 实际长度
            for (let i = this._offset; i < this._offset + len; i++) {
                if (this._org_buf.getUint8(i) > 0) vlen++;
            }
            this._list.push(ByteBuffer.utf8Read(this._org_buf, this._offset, vlen));
            this._offset += len;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 7, d: val, l: len });
            this._offset += len;
        }
        return this;
    }

    // 64位整数操作
    int64(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getFloat64(this._offset, this._littleEndian));
            this._offset += 8;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 8, d: val, l: 8 });
            this._offset += 8;
        }
        return this;
    }

    // 浮点数操作
    float(val, index) {
        if (arguments.length == 0) {
            this._list.push(this._org_buf.getFloat32(this._offset, this._littleEndian));
            this._offset += 4;
        } else {
            this._list.splice(index != undefined ? index : this._list.length, 0, { t: 9, d: val, l: 4 });
            this._offset += 4;
        }
        return this;
    }

    // 解包成数据数组
    unpack() {
        return this._list;
    }

    // 打包成二进制数据
    pack() {
        const listLen = this._list.length;
        let len = 0;
        for (let i = 0; i < listLen; i++) {
            let n = this._list[i];
            if (typeof n === 'object') {
                len += n.l;
                if (n.t === 6) len += 4; // string长度
            } else if (typeof n === 'string') {
                len += ByteBuffer.utf8Length(n);
                len += 4;
            }
        }
        
        const arrayBuf = new Uint8Array(len).buffer;
        const dataView = new DataView(arrayBuf);
        let offset = 0;
        
        for (let i = 0; i < listLen; i++) {
            let n = this._list[i];
            if (typeof n === 'object') {
                if (n.t == 1) dataView.setUint8(offset, n.d);
                else if (n.t == 2) dataView.setInt16(offset, n.d, this._littleEndian);
                else if (n.t == 3) dataView.setUint16(offset, n.d, this._littleEndian);
                else if (n.t == 4) dataView.setInt32(offset, n.d, this._littleEndian);
                else if (n.t == 5) dataView.setUint32(offset, n.d, this._littleEndian);
                else if (n.t == 6) {
                    dataView.setInt32(offset, ByteBuffer.utf8Length(n.d), this._littleEndian);
                    offset += 4;
                    ByteBuffer.utf8Write(dataView, offset, n.d);
                } else if (n.t == 7) {
                    ByteBuffer.utf8Write(dataView, offset, n.d);
                } else if (n.t == 8) dataView.setFloat64(offset, n.d, this._littleEndian);
                else if (n.t == 9) dataView.setFloat32(offset, n.d, this._littleEndian);
                offset += n.l;
            } else if (typeof n === 'string') {
                dataView.setInt32(offset, ByteBuffer.utf8Length(n), this._littleEndian);
                offset += 4;
                ByteBuffer.utf8Write(dataView, offset, n);
                offset += ByteBuffer.utf8Length(n);
            }
        }
        
        return arrayBuf;
    }

    // 静态方法 - UTF8编码写入
    static utf8Write(view, offset, str) {
        let c = 0;
        for (let i = 0, l = str.length; i < l; i++) {
            c = str.charCodeAt(i);
            if (c < 0x80) {
                view.setUint8(offset++, c);
            } else if (c < 0x800) {
                view.setUint8(offset++, 0xc0 | (c >> 6));
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            } else if (c < 0xd800 || c >= 0xe000) {
                view.setUint8(offset++, 0xe0 | (c >> 12));
                view.setUint8(offset++, 0x80 | (c >> 6) & 0x3f);
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            } else {
                i++;
                c = 0x10000 + (((c & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
                view.setUint8(offset++, 0xf0 | (c >> 18));
                view.setUint8(offset++, 0x80 | (c >> 12) & 0x3f);
                view.setUint8(offset++, 0x80 | (c >> 6) & 0x3f);
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            }
        }
    }

    // 静态方法 - UTF8解码读取
    static utf8Read(view, offset, length) {
        let string = '', chr = 0;
        for (let i = offset, end = offset + length; i < end; i++) {
            const byte = view.getUint8(i);
            if ((byte & 0x80) === 0x00) {
                string += String.fromCharCode(byte);
                continue;
            }
            if ((byte & 0xe0) === 0xc0) {
                string += String.fromCharCode(
                    ((byte & 0x0f) << 6) |
                    (view.getUint8(++i) & 0x3f)
                );
                continue;
            }
            if ((byte & 0xf0) === 0xe0) {
                string += String.fromCharCode(
                    ((byte & 0x0f) << 12) |
                    ((view.getUint8(++i) & 0x3f) << 6) |
                    ((view.getUint8(++i) & 0x3f) << 0)
                );
                continue;
            }
            if ((byte & 0xf8) === 0xf0) {
                chr = ((byte & 0x07) << 18) |
                    ((view.getUint8(++i) & 0x3f) << 12) |
                    ((view.getUint8(++i) & 0x3f) << 6) |
                    ((view.getUint8(++i) & 0x3f) << 0);
                if (chr >= 0x010000) { // surrogate pair
                    chr -= 0x010000;
                    string += String.fromCharCode((chr >>> 10) + 0xD800, (chr & 0x3FF) + 0xDC00);
                } else {
                    string += String.fromCharCode(chr);
                }
                continue;
            }
            throw new Error('Invalid byte ' + byte.toString(16));
        }
        return string;
    }

    // 静态方法 - 计算UTF8字符串长度
    static utf8Length(str) {
        let c = 0, length = 0;
        for (let i = 0, l = str.length; i < l; i++) {
            c = str.charCodeAt(i);
            if (c < 0x80) {
                length += 1;
            } else if (c < 0x800) {
                length += 2;
            } else if (c < 0xd800 || c >= 0xe000) {
                length += 3;
            } else {
                i++;
                length += 4;
            }
        }
        return length;
    }
}

/**
 * WebSocket客户端类
 * 负责WebSocket连接的建立、维护和消息的收发
 */
class WebSocketClient {
    /**
     * 构造函数
     * @param {Object} options 配置选项
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
    }
    
    /**
     * 连接到WebSocket服务器
     * @returns {Promise} 连接完成的Promise
     */
    connect() {
        if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
            console.log('WebSocket连接已存在');
            return Promise.resolve();
        }
        
        return new Promise((resolve, reject) => {
            try {
                this.socket = new WebSocket(this.options.url);
                this.socket.binaryType = 'arraybuffer';
                
                this.socket.onopen = (event) => {
                    console.log('WebSocket连接已打开');
                    this.connected = true;
                    this.reconnectAttempts = 0;
                    this._startHeartbeat();
                    this.options.onOpen(event);
                    resolve(event);
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
                    reject(new Error('WebSocket连接关闭'));
                };
                
                this.socket.onerror = (event) => {
                    console.error('WebSocket错误:', event);
                    this.options.onError(event);
                    reject(new Error('WebSocket连接错误'));
                };
            } catch (error) {
                console.error('连接WebSocket时出错:', error);
                this._reconnect();
                reject(error);
            }
        });
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
     * @param {Object} data 消息数据
     * @returns {boolean} 是否成功发送
     */
    sendMessageAck(data) {
        const ackData = {
            messageId: data.messageId,
            fromId: data.fromId,
            toId: data.toId,
            messageSequence: data.messageSequence
        };
        
        return this.sendMessage(1107, ackData, {
            appId: data.appId || 10001,
            clientType: data.clientType || 1,
            imei: data.imei || 'web'
        });
    }
    
    /**
     * 处理WebSocket消息
     * @param {MessageEvent} event WebSocket消息事件
     * @private
     */
    _handleMessage(event) {
        try {
            // 处理二进制消息
            if (event.data instanceof ArrayBuffer) {
                const dataView = new DataView(event.data);
                let offset = 0;
                
                // 读取命令号
                const command = dataView.getInt32(offset);
                offset += 4;
                
                // 读取消息体长度
                const bodyLength = dataView.getInt32(offset);
                offset += 4;
                
                // 读取消息体
                let body = "";
                if (bodyLength > 0) {
                    body = ByteBuffer.utf8Read(dataView, offset, bodyLength);
                }
                
                console.log("收到服务端发来的消息: " + body);
                
                // 创建消息对象
                const message = {
                    command,
                    body,
                    version: 1,
                    clientType: 1,
                    messageType: 0,
                    appId: 10001,
                    imei: 'web'
                };
                
                // 处理不同类型的命令
                if (command === 1103) { 
                    // 私聊消息
                    this._handleChatMessage(message);
                } else if (command === 2104) {
                    // 群聊消息
                    this._handleGroupMessage(message);
                } else if (command === 9999) {
                    // 系统消息
                    this._handleSystemMessage(message);
                }
                
                // 转发消息给回调处理
                this.options.onMessage(message);
            } else if (typeof event.data === 'string') {
                // 尝试解析JSON字符串
                try {
                    const jsonData = JSON.parse(event.data);
                    
                    // 确保有基本的消息结构
                    const message = {
                        command: jsonData.command || 9999, // 默认为系统消息
                        body: typeof jsonData === 'string' ? jsonData : JSON.stringify(jsonData),
                        version: 1,
                        clientType: 1,
                        messageType: 0,
                        appId: 10001,
                        imei: 'web'
                    };
                    
                    this.options.onMessage(message);
                } catch (jsonError) {
                    // 如果不是JSON，创建一个系统消息对象
                    const message = {
                        command: 9999, // 系统消息
                        body: event.data,
                        version: 1,
                        clientType: 1,
                        messageType: 0,
                        appId: 10001,
                        imei: 'web'
                    };
                    
                    this.options.onMessage(message);
                }
            } else {
                console.warn('未知的WebSocket消息类型:', typeof event.data);
                
                // 创建一个错误消息对象
                const errorMessage = {
                    command: 9999, // 系统消息
                    body: JSON.stringify({
                        type: 'error',
                        message: '未知的WebSocket消息类型',
                        dataType: typeof event.data
                    }),
                    version: 1,
                    clientType: 1,
                    messageType: 0,
                    appId: 10001,
                    imei: 'web'
                };
                
                this.options.onMessage(errorMessage);
            }
        } catch (error) {
            console.error('处理WebSocket消息时出错:', error);
            
            // 报告错误但不阻止程序继续运行
            try {
                const errorMessage = {
                    command: 9999, // 系统消息
                    body: JSON.stringify({
                        type: 'error',
                        message: '处理WebSocket消息时出错',
                        error: error.message
                    }),
                    version: 1,
                    clientType: 1,
                    messageType: 0,
                    appId: 10001,
                    imei: 'web'
                };
                
                this.options.onMessage(errorMessage);
            } catch (e) {
                console.error('报告消息处理错误时出错:', e);
            }
        }
    }
    
    /**
     * 处理私聊消息
     * @param {Object} message 消息对象
     * @private
     */
    _handleChatMessage(message) {
        try {
            let data;
            if (typeof message.body === 'string') {
                try {
                    // 尝试解析 JSON 字符串
                    data = JSON.parse(message.body);
                    
                    // 兼容不同格式的消息体
                    if (data.data && typeof data.data === 'object') {
                        // 如果消息体嵌套在 data 字段中
                        data = data.data;
                    }
                } catch (e) {
                    console.error('解析私聊消息 JSON 失败:', e);
                    return;
                }
            } else if (typeof message.body === 'object') {
                data = message.body;
            } else {
                console.error('未知的消息体格式:', typeof message.body);
                return;
            }
            
            // 如果有用户信息，则处理私聊消息回执
            if (this.userInfo && this.userInfo.userId) {
                // 如果消息不是自己发的，则发送消息确认回执
                if (data.fromId !== this.userInfo.userId) {
                    const ackData = {
                        fromId: this.userInfo.userId,
                        toId: data.fromId,
                        messageKey: data.messageKey,
                        messageId: data.messageId,
                        messageSequence: data.messageSequence
                    };
                    
                    this.sendMessageAck(ackData);
                    
                    // 如果客户端类型为1（Web），发送已读回执
                    if (this.userInfo.clientType === 1) {
                        const toId = data.fromId;
                        const readData = {
                            fromId: this.userInfo.userId,
                            toId: toId,
                            conversationType: 0,
                            messageSequence: data.messageSequence
                        };
                        
                        this.sendP2PReadReceipt(readData);
                    }
                }
            }
        } catch (error) {
            console.error('处理私聊消息时出错:', error);
        }
    }
    
    /**
     * 处理群聊消息
     * @param {Object} message 消息对象
     * @private
     */
    _handleGroupMessage(message) {
        try {
            let data;
            if (typeof message.body === 'string') {
                try {
                    // 尝试解析 JSON 字符串
                    data = JSON.parse(message.body);
                    
                    // 兼容不同格式的消息体
                    if (data.data && typeof data.data === 'object') {
                        // 如果消息体嵌套在 data 字段中
                        data = data.data;
                    }
                } catch (e) {
                    console.error('解析群聊消息 JSON 失败:', e);
                    return;
                }
            } else if (typeof message.body === 'object') {
                data = message.body;
            } else {
                console.error('未知的消息体格式:', typeof message.body);
                return;
            }
            
            // 如果有用户信息且客户端类型为1（Web），则发送群聊已读回执
            if (this.userInfo && this.userInfo.userId && this.userInfo.clientType === 1) {
                const toId = data.fromId;
                const readData = {
                    fromId: this.userInfo.userId,
                    toId: data.fromId,
                    groupId: data.groupId,
                    conversationType: 1,
                    messageSequence: data.messageSequence
                };
                
                this.sendGroupReadReceipt(readData);
            }
        } catch (error) {
            console.error('处理群聊消息时出错:', error);
        }
    }
    
    /**
     * 处理系统消息
     * @param {Object} message 消息对象
     * @private
     */
    _handleSystemMessage(message) {
        try {
            // 尝试解析消息体
            let msgBody;
            
            if (typeof message.body === 'string') {
                try {
                    msgBody = JSON.parse(message.body);
                } catch (e) {
                    console.log('系统消息不是JSON格式，以原始格式处理:', message.body);
                    msgBody = { type: 'text', content: message.body };
                }
            } else if (typeof message.body === 'object') {
                msgBody = message.body;
            } else {
                console.log('系统消息格式未知:', typeof message.body);
                msgBody = { type: 'unknown', content: String(message.body) };
            }
            
            // 处理不同类型的系统消息
            if (msgBody.type === 'ping') {
                console.log('收到心跳包，发送回应');
                // 可以在这里处理心跳响应，例如发送pong消息
                this.sendMessage(9999, { type: 'pong', timestamp: Date.now() });
            } else if (msgBody.userId === "system") {
                console.log('收到系统消息:', msgBody.data);
            } else {
                console.log('收到其他类型系统消息:', msgBody);
            }
        } catch (error) {
            console.error('处理系统消息时出错:', error);
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
                console.log('发送心跳');
                this.sendMessage(9999, { type: 'ping', timestamp: Date.now() });
            } else {
                console.warn('WebSocket未连接，无法发送心跳');
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
     * 重新连接
     * @private
     */
    _reconnect() {
        if (this.reconnectTimer) {
            clearTimeout(this.reconnectTimer);
        }
        
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.warn(`已达到最大重连次数(${this.maxReconnectAttempts})，停止重连`);
            return;
        }
        
        this.reconnectAttempts++;
        
        console.log(`尝试重新连接(${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
        
        this.reconnectTimer = setTimeout(() => {
            this.connect();
        }, this.options.reconnectInterval);
    }
    
    /**
     * 获取字符串长度
     * @param {string} str 字符串
     * @returns {number} 长度
     * @private
     */
    _getLen(str) {
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
    
    /**
     * 生成UUID
     * @returns {string} UUID
     * @private
     */
    _generateUuid() {
        var s = [];
        var hexDigits = "0123456789abcdef";
        for (var i = 0; i < 36; i++) {
            s[i] = hexDigits.substr(Math.floor(Math.random() * 0x10), 1);
        }
        s[14] = "4"; // bits 12-15 of the time_hi_and_version field to 0010
        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1); // bits 6-7 of the clock_seq_hi_and_reserved to 01
        s[8] = s[13] = s[18] = s[23] = "-";

        var uuid = s.join("");
        return uuid;
    }
    
    /**
     * 发起HTTP请求
     * @param {string} endpoint 接口路径
     * @param {string} method 请求方法
     * @param {Object} data 请求数据
     * @returns {Promise<Object>} 响应结果
     * @private
     */
    async _httpRequest(endpoint, method = 'GET', data = null) {
        try {
            const url = `${this.options.apiBaseUrl}${endpoint}`;
            const options = {
                method,
                headers: {
                    'Content-Type': 'application/json'
                }
            };
            
            if (data && (method === 'POST' || method === 'PUT')) {
                options.body = JSON.stringify(data);
            }
            
            const response = await fetch(url, options);
            const result = await response.json();
            
            return result;
        } catch (error) {
            console.error('HTTP请求失败:', error);
            throw error;
        }
    }
    
    /**
     * 设置用户信息
     * @param {Object} userInfo 用户信息
     */
    setUserInfo(userInfo) {
        this.userInfo = userInfo;
    }
    
    /**
     * 获取用户信息
     * @returns {Object} 用户信息
     */
    getUserInfo() {
        if (!this.userInfo) {
            console.warn('用户信息尚未设置，返回默认值');
            return {
                userId: '',
                appId: 10001,
                clientType: 1,
                imei: 'web'
            };
        }
        return this.userInfo;
    }
}

// 导出为全局变量，以便在不支持ES6模块的环境中使用
window.ByteBuffer = ByteBuffer;
window.WebSocketClient = WebSocketClient; 