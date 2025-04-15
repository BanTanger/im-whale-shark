/**
 * IM API客户端类
 * 负责处理与后端API的HTTP通信
 */
class ImApiClient {
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
            memberIds: data.memberIds || [],
            appId: this.options.appId,
            operater: data.userId
        };
        
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
        
        const response = await this._post('/v1/friendship/addFriend', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '添加好友失败');
        }
        
        return response.data;
    }
    
    /**
     * 同步离线消息
     * @param {string} userId 用户ID
     * @param {number} lastSequence 上次同步序列号
     * @returns {Promise<Object>} 同步结果
     */
    async syncOfflineMessages(userId, lastSequence = 0) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
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
     * @returns {Promise<Object>} 同步结果
     */
    async syncFriendshipList(userId, lastSequence = 0) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
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
     * @returns {Promise<Object>} 同步结果
     */
    async syncJoinedGroupList(userId, lastSequence = 0) {
        const req = {
            userId: userId,
            appId: this.options.appId,
            lastSequence: lastSequence,
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
     * @param {number} maxLimit 最大返回数量
     * @returns {Promise<Object>} 同步结果
     */
    async syncConversationList(userId, lastSequence = 0, maxLimit = 100) {
        const req = {
            appId: this.options.appId,
            operater: userId,
            lastSequence: lastSequence,
            maxLimit: maxLimit,
            clientType: 1,
            imei: 'web'
        };
        
        const response = await this._post('/v1/conversation/syncConversationList', req);
        
        if (!response.isOk()) {
            throw new Error(response.msg || '同步会话列表失败');
        }
        
        return response.data;
    }
    
    /**
     * 发送POST请求
     * @param {string} endpoint 接口地址
     * @param {Object} data 请求数据
     * @returns {Promise<Object>} 响应对象
     * @private
     */
    async _post(endpoint, data) {
        const url = `${this.options.baseUrl}${endpoint}`;
        
        console.log(`发送API请求: ${endpoint}`, data);
        
        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(data)
            });
            
            const result = await response.json();
            
            console.log(`收到API响应: ${endpoint}`, result);
            
            // 添加isOk方法，与后端ResponseVO.java一致
            result.isOk = function() {
                return this.code === 200;
            };
            
            return result;
        } catch (error) {
            console.error(`API请求失败: ${endpoint}`, error);
            throw error;
        }
    }
}

// 导出ImApiClient类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ImApiClient;
} 