/**
 * WeChatPlus主应用
 * 负责初始化和协调WebSocket客户端、消息处理和UI控制器
 */
(function() {
    'use strict';
    
    // 全局状态
    const state = {
        user: null,
        wsClient: null,
        messageHandler: null,
        uiController: null,
        apiClient: null,
        conversations: {},
        contacts: {},
        groups: {}
    };
    
    // DOM元素缓存
    const elements = {};
    
    /**
     * 初始化应用
     */
    function init() {
        cacheElements();
        setupApiClient();
        setupEventListeners();
        loadRecentAccounts();
    }
    
    /**
     * 缓存常用DOM元素
     */
    function cacheElements() {
        elements.loginPage = document.getElementById('login-page');
        elements.mainApp = document.getElementById('main-app');
        elements.loginForm = {
            userId: document.getElementById('userId'),
            appId: document.getElementById('appId'),
            clientType: document.getElementById('clientType'),
            imei: document.getElementById('imei'),
            loginBtn: document.getElementById('login-btn'),
            recentAccounts: document.getElementById('recent-accounts')
        };
        elements.chat = {
            messagesContainer: document.getElementById('chat-messages'),
            messageInput: document.getElementById('message-input'),
            sendBtn: document.getElementById('send-btn')
        };
    }
    
    /**
     * 设置API客户端
     */
    function setupApiClient() {
        state.apiClient = new ImApiClient(window.location.origin);
    }
    
    /**
     * 设置事件监听器
     */
    function setupEventListeners() {
        // 登录按钮点击事件
        if (elements.loginForm.loginBtn) {
            elements.loginForm.loginBtn.addEventListener('click', handleLogin);
        }
    }
    
    /**
     * 加载最近账号
     */
    function loadRecentAccounts() {
        try {
            const accounts = JSON.parse(localStorage.getItem('recentAccounts')) || [];
            
            if (elements.loginForm.recentAccounts) {
                elements.loginForm.recentAccounts.innerHTML = '';
                
                accounts.forEach(account => {
                    const accountEl = document.createElement('div');
                    accountEl.className = 'recent-account';
                    accountEl.textContent = account.userId;
                    accountEl.addEventListener('click', () => {
                        fillLoginForm(account);
                    });
                    
                    elements.loginForm.recentAccounts.appendChild(accountEl);
                });
            }
        } catch (error) {
            console.error('加载最近账号失败:', error);
        }
    }
    
    /**
     * 填充登录表单
     * @param {Object} account 账号信息
     */
    function fillLoginForm(account) {
        if (elements.loginForm.userId) elements.loginForm.userId.value = account.userId;
        if (elements.loginForm.appId) elements.loginForm.appId.value = account.appId || '10001';
        if (elements.loginForm.clientType) elements.loginForm.clientType.value = account.clientType || '1';
        if (elements.loginForm.imei) elements.loginForm.imei.value = account.imei || 'web';
    }
    
    /**
     * 处理登录
     */
    async function handleLogin() {
        const userId = elements.loginForm.userId.value.trim();
        const appId = elements.loginForm.appId.value.trim() || '10001';
        const clientType = elements.loginForm.clientType.value.trim() || '1';
        const imei = elements.loginForm.imei.value.trim() || 'web';
        
        if (!userId) {
            alert('请输入用户ID');
            return;
        }
        
        try {
            // 设置API客户端的AppID
            state.apiClient.setAppId(parseInt(appId, 10));
            
            // 登录获取WebSocket连接信息
            const loginParams = {
                userId,
                appId: parseInt(appId, 10),
                clientType: parseInt(clientType, 10),
                imei
            };
            
            const routeInfo = await state.apiClient.login(loginParams);
            
            // 保存用户信息
            state.user = {
                userId,
                appId: parseInt(appId, 10),
                clientType: parseInt(clientType, 10),
                imei
            };
            
            // 保存到最近账号
            saveRecentAccount(state.user);
            
            // 初始化WebSocket客户端
            initWebSocketClient(routeInfo);
            
            // 初始化UI控制器
            initUIController();
            
            // 初始化消息处理器
            initMessageHandler();
            
            // 显示主应用界面
            showMainApp();
            
            // 加载会话、联系人和群组数据
            loadUserData();
            
        } catch (error) {
            alert(`登录失败: ${error.message}`);
            console.error('登录失败:', error);
        }
    }
    
    /**
     * 保存最近账号
     * @param {Object} account 账号信息
     */
    function saveRecentAccount(account) {
        try {
            let accounts = JSON.parse(localStorage.getItem('recentAccounts')) || [];
            
            // 检查是否已存在该账号
            const existingIndex = accounts.findIndex(a => a.userId === account.userId);
            if (existingIndex !== -1) {
                accounts.splice(existingIndex, 1);
            }
            
            // 添加到最前面
            accounts.unshift({
                userId: account.userId,
                appId: account.appId,
                clientType: account.clientType,
                imei: account.imei
            });
            
            // 最多保留5个账号
            if (accounts.length > 5) {
                accounts = accounts.slice(0, 5);
            }
            
            localStorage.setItem('recentAccounts', JSON.stringify(accounts));
        } catch (error) {
            console.error('保存最近账号失败:', error);
        }
    }
    
    /**
     * 初始化WebSocket客户端
     * @param {Object} routeInfo WebSocket路由信息
     */
    function initWebSocketClient(routeInfo) {
        // 根据路由信息构建WebSocket URL
        let wsUrl;
        if (routeInfo.type === 'WEB') {
            wsUrl = `ws://${routeInfo.ip}:${routeInfo.port}/ws`;
        } else {
            wsUrl = `ws://${routeInfo.ip}:${routeInfo.port}/ws`;
        }
        
        // 创建WebSocket客户端
        state.wsClient = new WebSocketClient({
            url: wsUrl,
            onOpen: handleWebSocketOpen,
            onMessage: handleWebSocketMessage,
            onClose: handleWebSocketClose,
            onError: handleWebSocketError
        });
        
        // 连接WebSocket
        state.wsClient.connect();
    }
    
    /**
     * 初始化UI控制器
     */
    function initUIController() {
        state.uiController = new UIController({
            onThemeChange: handleThemeChange,
            onStyleChange: handleStyleChange,
            onFontSizeChange: handleFontSizeChange,
            onSendMessage: handleSendMessage,
            onConversationSelect: handleConversationSelect,
            onLogout: handleLogout
        });
        
        // 设置用户信息
        state.uiController.setUserInfo(state.user);
    }
    
    /**
     * 初始化消息处理器
     */
    function initMessageHandler() {
        state.messageHandler = new MessageHandler({
            onChatMessage: handleChatMessage,
            onGroupMessage: handleGroupMessage,
            onSystemMessage: handleSystemMessage,
            onMessageAck: handleMessageAck,
            onReadReceipt: handleReadReceipt
        });
    }
    
    /**
     * 显示主应用界面
     */
    function showMainApp() {
        if (elements.loginPage) {
            elements.loginPage.classList.add('hidden');
        }
        
        if (elements.mainApp) {
            elements.mainApp.classList.remove('hidden');
        }
    }
    
    /**
     * 加载用户数据
     */
    async function loadUserData() {
        try {
            // 加载好友列表
            const friends = await state.apiClient.getAllFriendship(state.user.userId);
            if (friends && friends.length > 0) {
                friends.forEach(friend => {
                    state.contacts[friend.friendId] = friend;
                    state.uiController.addContactItem(friend);
                });
            }
            
            // 加载群组列表
            const groups = await state.apiClient.getJoinedGroups(state.user.userId);
            if (groups && groups.length > 0) {
                groups.forEach(group => {
                    state.groups[group.groupId] = group;
                    state.uiController.addGroupItem(group);
                });
            }
            
            // 加载会话列表（简化处理，实际应从后端获取）
            for (const contactId in state.contacts) {
                const contact = state.contacts[contactId];
                const conversation = {
                    id: `chat-${contactId}`,
                    type: 'chat',
                    toId: contactId,
                    name: contact.remark || contact.nickname || contact.friendId,
                    timestamp: Date.now(),
                    lastMessage: '',
                    unread: 0
                };
                
                state.conversations[conversation.id] = conversation;
                state.uiController.addConversationItem(conversation);
            }
            
            for (const groupId in state.groups) {
                const group = state.groups[groupId];
                const conversation = {
                    id: `group-${groupId}`,
                    type: 'group',
                    groupId: groupId,
                    name: group.groupName,
                    timestamp: Date.now(),
                    lastMessage: '',
                    unread: 0,
                    memberCount: group.memberCount || 0
                };
                
                state.conversations[conversation.id] = conversation;
                state.uiController.addConversationItem(conversation);
            }
            
        } catch (error) {
            console.error('加载用户数据失败:', error);
        }
    }
    
    /**
     * 处理WebSocket打开
     * @param {Event} event 事件对象
     */
    function handleWebSocketOpen(event) {
        console.log('WebSocket连接已建立');
        
        // 发送登录消息
        state.wsClient.sendLoginMessage(state.user);
    }
    
    /**
     * 处理WebSocket消息
     * @param {Object} message 消息对象
     */
    function handleWebSocketMessage(message) {
        console.log('收到WebSocket消息:', message);
        
        // 使用消息处理器处理消息
        state.messageHandler.handleMessage(message);
    }
    
    /**
     * 处理WebSocket关闭
     * @param {Event} event 事件对象
     */
    function handleWebSocketClose(event) {
        console.log('WebSocket连接已关闭');
    }
    
    /**
     * 处理WebSocket错误
     * @param {Event} event 事件对象
     */
    function handleWebSocketError(event) {
        console.error('WebSocket错误:', event);
    }
    
    /**
     * 处理主题切换
     * @param {string} theme 主题名称
     */
    function handleThemeChange(theme) {
        console.log('主题已切换:', theme);
    }
    
    /**
     * 处理风格切换
     * @param {string} style 风格名称
     */
    function handleStyleChange(style) {
        console.log('风格已切换:', style);
    }
    
    /**
     * 处理字体大小变更
     * @param {number} size 字体大小
     */
    function handleFontSizeChange(size) {
        console.log('字体大小已变更:', size);
    }
    
    /**
     * 处理发送消息
     * @param {Object} data 消息数据
     */
    function handleSendMessage(data) {
        console.log('发送消息:', data);
        
        // 创建消息对象
        const message = state.messageHandler.createMessage(data.type, {
            fromId: state.user.userId,
            ...data
        });
        
        // 添加到UI
        state.uiController.addMessage(message);
        
        // 发送消息
        if (data.type === 'chat') {
            state.wsClient.sendChatMessage({
                fromId: state.user.userId,
                toId: data.toId,
                content: data.content,
                appId: state.user.appId,
                clientType: state.user.clientType,
                imei: state.user.imei
            });
        } else if (data.type === 'group') {
            state.wsClient.sendGroupMessage({
                fromId: state.user.userId,
                groupId: data.groupId,
                content: data.content,
                appId: state.user.appId,
                clientType: state.user.clientType,
                imei: state.user.imei
            });
        }
        
        // 更新会话最后消息
        if (state.currentConversation) {
            state.currentConversation.lastMessage = data.content;
            state.currentConversation.timestamp = Date.now();
            state.uiController.updateConversationItem(state.currentConversation);
        }
    }
    
    /**
     * 处理会话选择
     * @param {Object} conversation 会话对象
     */
    function handleConversationSelect(conversation) {
        console.log('选择会话:', conversation);
        state.currentConversation = conversation;
    }
    
    /**
     * 处理退出登录
     */
    function handleLogout() {
        console.log('退出登录');
        
        // 关闭WebSocket连接
        if (state.wsClient) {
            state.wsClient.disconnect();
        }
        
        // 重置状态
        state.user = null;
        state.wsClient = null;
        state.messageHandler = null;
        state.uiController = null;
        state.conversations = {};
        state.contacts = {};
        state.groups = {};
        state.currentConversation = null;
        
        // 返回登录页面
        if (elements.loginPage) {
            elements.loginPage.classList.remove('hidden');
        }
        
        if (elements.mainApp) {
            elements.mainApp.classList.add('hidden');
        }
    }
    
    /**
     * 处理聊天消息
     * @param {Object} message 消息对象
     */
    function handleChatMessage(message) {
        console.log('处理聊天消息:', message);
        
        // 添加或更新会话
        const conversationId = `chat-${message.fromId === state.user.userId ? message.toId : message.fromId}`;
        const contactId = message.fromId === state.user.userId ? message.toId : message.fromId;
        
        let conversation = state.conversations[conversationId];
        if (!conversation) {
            conversation = {
                id: conversationId,
                type: 'chat',
                toId: contactId,
                name: state.contacts[contactId] ? 
                    (state.contacts[contactId].remark || state.contacts[contactId].nickname || contactId) : 
                    contactId,
                timestamp: message.timestamp,
                lastMessage: message.content,
                unread: message.fromId !== state.user.userId ? 1 : 0
            };
            state.conversations[conversationId] = conversation;
        } else {
            conversation.timestamp = message.timestamp;
            conversation.lastMessage = message.content;
            
            // 如果消息不是当前用户发送的，且不是当前会话，增加未读数
            if (message.fromId !== state.user.userId && 
                (!state.currentConversation || state.currentConversation.id !== conversationId)) {
                conversation.unread = (conversation.unread || 0) + 1;
            }
        }
        
        state.uiController.addConversationItem(conversation);
        
        // 如果是当前会话，显示消息
        if (state.currentConversation && state.currentConversation.id === conversationId) {
            state.uiController.addMessage(message);
            
            // 发送已读回执
            if (message.fromId !== state.user.userId && state.user.clientType === 1) {
                state.wsClient.sendP2PReadReceipt({
                    fromId: state.user.userId,
                    toId: message.fromId,
                    messageSequence: message.sequence,
                    appId: state.user.appId,
                    clientType: state.user.clientType,
                    imei: state.user.imei
                });
            }
        }
        
        // 发送消息确认回执
        if (message.fromId !== state.user.userId) {
            state.wsClient.sendMessageAck({
                fromId: state.user.userId,
                toId: message.fromId,
                messageKey: message.key,
                messageId: message.id,
                messageSequence: message.sequence,
                appId: state.user.appId,
                clientType: state.user.clientType,
                imei: state.user.imei
            });
        }
    }
    
    /**
     * 处理群聊消息
     * @param {Object} message 消息对象
     */
    function handleGroupMessage(message) {
        console.log('处理群聊消息:', message);
        
        // 添加或更新会话
        const conversationId = `group-${message.groupId}`;
        
        let conversation = state.conversations[conversationId];
        if (!conversation) {
            conversation = {
                id: conversationId,
                type: 'group',
                groupId: message.groupId,
                name: state.groups[message.groupId] ? 
                    state.groups[message.groupId].groupName : 
                    `群聊(${message.groupId})`,
                timestamp: message.timestamp,
                lastMessage: message.content,
                unread: message.fromId !== state.user.userId ? 1 : 0
            };
            state.conversations[conversationId] = conversation;
        } else {
            conversation.timestamp = message.timestamp;
            conversation.lastMessage = message.content;
            
            // 如果消息不是当前用户发送的，且不是当前会话，增加未读数
            if (message.fromId !== state.user.userId && 
                (!state.currentConversation || state.currentConversation.id !== conversationId)) {
                conversation.unread = (conversation.unread || 0) + 1;
            }
        }
        
        state.uiController.addConversationItem(conversation);
        
        // 如果是当前会话，显示消息
        if (state.currentConversation && state.currentConversation.id === conversationId) {
            state.uiController.addMessage(message);
            
            // 发送已读回执
            if (message.fromId !== state.user.userId && state.user.clientType === 1) {
                state.wsClient.sendGroupReadReceipt({
                    fromId: state.user.userId,
                    toId: message.fromId,
                    groupId: message.groupId,
                    messageSequence: message.sequence,
                    appId: state.user.appId,
                    clientType: state.user.clientType,
                    imei: state.user.imei
                });
            }
        }
    }
    
    /**
     * 处理系统消息
     * @param {Object} message 消息对象
     */
    function handleSystemMessage(message) {
        console.log('处理系统消息:', message);
        
        // 显示系统消息
        state.uiController.addMessage(message);
    }
    
    /**
     * 处理消息确认
     * @param {Object} ack 确认对象
     */
    function handleMessageAck(ack) {
        console.log('处理消息确认:', ack);
        
        // 更新消息状态
        state.uiController.updateMessageStatus(ack.messageId, 'delivered');
    }
    
    /**
     * 处理已读回执
     * @param {Object} receipt 回执对象
     */
    function handleReadReceipt(receipt) {
        console.log('处理已读回执:', receipt);
        
        // 实际应用中应该查找消息ID并更新状态
        // 这里简化处理
    }
    
    // 当DOM加载完成后初始化应用
    document.addEventListener('DOMContentLoaded', init);
})(); 