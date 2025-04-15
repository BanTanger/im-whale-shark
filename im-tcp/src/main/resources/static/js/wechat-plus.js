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
        imClient: null,
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
            sendBtn: document.getElementById('send-btn'),
            chatTitle: document.getElementById('chat-title'),
            chatSubtitle: document.getElementById('chat-subtitle')
        };
        elements.sidebar = {
            currentAvatar: document.getElementById('current-avatar'),
            profileAvatar: document.getElementById('profile-avatar'),
            profileName: document.getElementById('profile-name'),
            profileStatus: document.getElementById('profile-status'),
            sidebarIcons: document.querySelectorAll('.sidebar-icon'),
            newChatBtn: document.getElementById('new-chat-btn')
        };
        elements.pageContents = document.querySelectorAll('.page-content');
        elements.lists = {
            conversationsList: document.getElementById('conversations-list'),
            contactsList: document.getElementById('contacts-list'),
            groupsList: document.getElementById('groups-list')
        };
        elements.settings = {
            userIdSpan: document.getElementById('settings-user-id'),
            clientTypeSpan: document.getElementById('settings-client-type'),
            appIdSpan: document.getElementById('settings-app-id'),
            logoutBtn: document.getElementById('logout-btn'),
            themeToggle: document.getElementById('theme-toggle'),
            uiStyleToggle: document.getElementById('ui-style-toggle'),
            fontSizeRange: document.getElementById('font-size-range'),
            fontSizeValue: document.getElementById('font-size-value')
        };
        elements.modals = {
            backdrop: document.getElementById('modal-backdrop'),
            container: document.getElementById('modal-container')
        };
        
        // 调试
        console.log('新聊天按钮元素:', elements.sidebar.newChatBtn);
    }
    
    /**
     * 设置API客户端
     */
    function setupApiClient() {
        state.apiClient = new ImApiClient();
    }
    
    /**
     * 设置事件监听器
     */
    function setupEventListeners() {
        // 登录按钮点击事件
        if (elements.loginForm.loginBtn) {
            elements.loginForm.loginBtn.addEventListener('click', handleLogin);
        }
        
        // 消息输入框输入事件
        if (elements.chat.messageInput) {
            elements.chat.messageInput.addEventListener('input', () => {
                if (elements.chat.sendBtn) {
                    elements.chat.sendBtn.disabled = !elements.chat.messageInput.value.trim();
                }
            });
            
            // 回车发送消息
            elements.chat.messageInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    if (elements.chat.messageInput.value.trim()) {
                        sendMessageToCurrentConversation(elements.chat.messageInput.value.trim());
                    }
                }
            });
        }
        
        // 发送按钮点击事件
        if (elements.chat.sendBtn) {
            elements.chat.sendBtn.addEventListener('click', () => {
                if (elements.chat.messageInput.value.trim()) {
                    sendMessageToCurrentConversation(elements.chat.messageInput.value.trim());
                }
            });
        }
        
        // 剩余的事件绑定将由UI控制器处理
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
            
            // 保存用户信息
            state.user = {
                userId,
                appId: parseInt(appId, 10),
                clientType: parseInt(clientType, 10),
                imei
            };
            
            // 保存到最近账号
            saveRecentAccount(state.user);
            
            // 创建WebSocket客户端
            const wsClient = new WebSocketClient({
                url: 'ws://localhost:19002/ws',
                onOpen: handleWebSocketOpen,
                onClose: handleWebSocketClose,
                onError: handleWebSocketError,
                onMessage: handleWebSocketMessage
            });
            
            // 保存到state
            state.wsClient = wsClient;
            
            // 设置用户信息
            wsClient.setUserInfo({
                userId: state.user.userId,
                appId: state.user.appId,
                clientType: state.user.clientType,
                imei: state.user.imei
            });
            
            // 连接WebSocket服务器
            wsClient.connect();
            
            // 等待WebSocket连接建立
            let connected = false;
            let attempts = 0;
            const maxAttempts = 20;
            
            while (!connected && attempts < maxAttempts) {
                await new Promise(resolve => setTimeout(resolve, 300));
                connected = wsClient.connected;
                attempts++;
            }
            
            if (!connected) {
                throw new Error('WebSocket连接超时');
            }
            
            // 发送登录消息
            const loginSuccess = wsClient.sendLoginMessage({
                userId: state.user.userId,
                appId: state.user.appId,
                clientType: state.user.clientType,
                imei: state.user.imei
            });
            
            if (!loginSuccess) {
                throw new Error('发送登录消息失败');
            }
            
            // 创建一个消息处理器
            const messageHandler = new MessageHandler();
            
            // 初始化IM客户端
            state.imClient = new IMClient({
                wsUrl: wsClient.options.url,
                apiBaseUrl: state.apiClient.options.baseUrl,
                callbacks: {
                    onConnected: handleWebSocketOpen,
                    onDisconnected: handleWebSocketClose,
                    onError: handleWebSocketError,
                    onChatMessage: handleChatMessage,
                    onGroupMessage: handleGroupMessage,
                    onSystemMessage: handleSystemMessage,
                    onMessageAck: handleMessageAck,
                    onReadReceipt: handleReadReceipt
                }
            });
            
            // 使用已经连接的WebSocket客户端
            state.imClient.wsClient = wsClient;
            
            // 初始化UI控制器
            initUIController();
            
            // 显示主应用界面
            showMainApp();
            
            // 加载用户数据
            setTimeout(loadUserData, 1000);
            
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
     * 初始化UI控制器
     */
    function initUIController() {
        // 创建UI控制器实例
        state.uiController = new UIController({
            elements: elements,
            onConversationSelect: handleConversationSelect,
            onSendMessage: handleSendMessage,
            onThemeChange: handleThemeChange,
            onStyleChange: handleStyleChange,
            onFontSizeChange: handleFontSizeChange,
            onCreateGroup: handleCreateGroup,
            onAddFriend: handleAddFriend
        });
        
        // 设置用户信息
        state.uiController.setUserInfo(state.user);
        
        // 加载UI设置
        loadUISettings();
        
        // 将WebSocket客户端和IMClient添加到全局作用域，以便UI控制器可以访问
        window.wsClient = state.wsClient;
        window.imClient = state.imClient;
        
        // 暴露状态到全局变量，方便调试
        window.state = state;
    }
    
    /**
     * 加载UI设置
     */
    function loadUISettings() {
        try {
            const settings = JSON.parse(localStorage.getItem('uiSettings')) || {};
            
            // 应用主题
            if (settings.theme) {
                handleThemeChange(settings.theme);
                if (elements.settings.themeToggle) {
                    elements.settings.themeToggle.checked = settings.theme === 'dark';
                }
            }
            
            // 应用UI风格
            if (settings.style) {
                handleStyleChange(settings.style);
                if (elements.settings.uiStyleToggle) {
                    elements.settings.uiStyleToggle.checked = settings.style === 'modern';
                }
            }
            
            // 应用字体大小
            if (settings.fontSize) {
                handleFontSizeChange(settings.fontSize);
                if (elements.settings.fontSizeRange) {
                    elements.settings.fontSizeRange.value = settings.fontSize;
                }
                if (elements.settings.fontSizeValue) {
                    elements.settings.fontSizeValue.textContent = `${settings.fontSize}px`;
                }
            }
        } catch (error) {
            console.error('加载UI设置失败:', error);
        }
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
        
        // 更新设置页面中的用户信息
        if (state.user) {
            if (elements.settings.userIdSpan) {
                elements.settings.userIdSpan.textContent = state.user.userId;
            }
            if (elements.settings.clientTypeSpan) {
                elements.settings.clientTypeSpan.textContent = state.user.clientType;
            }
            if (elements.settings.appIdSpan) {
                elements.settings.appIdSpan.textContent = state.user.appId;
            }
        }
        
        // 确保聊天输入框和发送按钮初始状态为禁用
        if (elements.chat.messageInput) {
            elements.chat.messageInput.disabled = true;
            elements.chat.messageInput.value = '';
        }
        
        if (elements.chat.sendBtn) {
            elements.chat.sendBtn.disabled = true;
        }
    }
    
    /**
     * 加载用户数据（会话、联系人、群组）
     */
    async function loadUserData() {
        try {
            // 确保用户已登录
            if (!state.user || !state.user.userId) {
                console.warn('用户未登录，无法加载数据');
                return;
            }
            
            // 确保wsClient已连接
            if (!state.wsClient || !state.wsClient.connected) {
                console.warn('WebSocket未连接，延迟加载用户数据');
                setTimeout(loadUserData, 1000);
                return;
            }
            
            // 加载好友列表
            let friends = [];
            try {
                friends = await state.apiClient.getFriendList(state.user.userId);
            } catch (error) {
                console.warn('通过API加载好友列表失败:', error);
            }
            processFriendList(friends);
            
            // 加载群组列表
            let groups = [];
            try {
                groups = await state.apiClient.getGroupList(state.user.userId);
            } catch (error) {
                console.warn('通过API加载群组列表失败:', error);
            }
            processGroupList(groups);
            
            // 同步会话列表
            try {
                const conversationResponse = await state.apiClient.syncConversationList(state.user.userId, 0, 100);
                console.log('同步会话列表响应:', conversationResponse);
                
                // 处理会话列表
                processConversationList(conversationResponse);
            } catch (error) {
                console.warn('同步会话列表失败:', error);
                
                // 如果同步失败，尝试从IM客户端获取会话列表（兼容处理）
                let conversations = [];
                if (state.imClient && state.imClient.getConversationList) {
                    try {
                        conversations = state.imClient.getConversationList();
                        processConversationList(conversations);
                    } catch (error) {
                        console.error('获取会话列表失败:', error);
                    }
                }
            }
            
        } catch (error) {
            console.error('加载用户数据失败:', error);
        }
    }
    
    /**
     * 处理好友列表
     * @param {Array} friends 好友列表
     */
    function processFriendList(friends) {
        friends.forEach(friend => {
            state.contacts[friend.toId] = {
                id: friend.toId,
                name: friend.toId || friend.remark,
                avatar: getAvatarForUser(friend.toId),
                status: 'offline'
            };
        });
        
        // 更新UI
        state.uiController.updateContactsList(Object.values(state.contacts));
    }
    
    /**
     * 处理群组列表
     * @param {Array} groups 群组列表
     */
    function processGroupList(groups) {
        groups.forEach(group => {
            state.groups[group.groupId] = {
                id: group.groupId,
                name: group.groupName,
                avatar: getAvatarForGroup(group.groupId),
                memberCount: group.memberCount || 0
            };
        });
        
        // 更新UI
        state.uiController.updateGroupsList(Object.values(state.groups));
    }
    
    /**
     * 处理会话列表
     * @param {Object} conversationData 会话数据
     */
    function processConversationList(conversationData) {
        // 确保有有效数据
        if (!conversationData) {
            console.warn('处理会话列表: 数据为空');
            return;
        }
        
        console.log('处理会话列表数据:', conversationData);
        
        // 检查数据结构并获取会话列表数组
        let conversationList = [];
        
        // 处理ResponseVO格式
        if (typeof conversationData === 'object' && conversationData.code !== undefined) {
            if (conversationData.code === 200 && conversationData.data) {
                conversationData = conversationData.data;
            } else {
                console.warn('处理会话列表: 响应状态码不正确', conversationData.code, conversationData.msg);
                return;
            }
        }
        
        // 如果是数组，直接使用
        if (Array.isArray(conversationData)) {
            conversationList = conversationData;
        } 
        // 如果包含dataList字段
        else if (conversationData.dataList && Array.isArray(conversationData.dataList)) {
            conversationList = conversationData.dataList;
        } 
        // 如果包含data字段
        else if (conversationData.data) {
            // 如果data是数组
            if (Array.isArray(conversationData.data)) {
                conversationList = conversationData.data;
            } 
            // 如果data包含dataList
            else if (conversationData.data.dataList && Array.isArray(conversationData.data.dataList)) {
                conversationList = conversationData.data.dataList;
            }
            // 如果data是单个对象
            else if (typeof conversationData.data === 'object') {
                conversationList = [conversationData.data];
            }
        }
        // 其他情况，尝试将对象作为单个会话处理
        else if (typeof conversationData === 'object' && conversationData.conversationId) {
            conversationList = [conversationData];
        }
        
        // 最后防御性检查
        if (!Array.isArray(conversationList)) {
            console.warn('处理会话列表: 无法识别的数据结构', conversationData);
            conversationList = [];
        }
        
        console.log('处理会话列表: 提取的会话列表', conversationList);
        
        if (conversationList.length === 0) {
            console.warn('处理会话列表: 没有会话数据');
            return;
        }
        
        // 清空当前会话列表，重新构建
        state.conversations = {};
        
        // 处理每个会话
        conversationList.forEach(conversation => {
            if (!conversation || !conversation.conversationId) {
                console.warn('跳过无效会话:', conversation);
                return;
            }
            
            try {
                // 根据会话类型获取目标ID和类型
                const conversationType = conversation.conversationType;
                const targetId = conversation.toId;
                
                // 确定会话类型的字符串表示
                let typeName;
                switch (conversationType) {
                    case 0:
                        typeName = 'C2C'; // 单聊
                        break;
                    case 1:
                        typeName = 'GROUP'; // 群聊
                        break;
                    case 2:
                        typeName = 'ROBOT'; // 机器人
                        break;
                    case 3:
                        typeName = 'PUBLIC'; // 公众号
                        break;
                    default:
                        typeName = 'UNKNOWN';
                }
                
                // 获取会话名称和头像
                let name, avatar;
                if (typeName === 'C2C') {
                    const contact = state.contacts[targetId];
                    name = contact ? contact.name : targetId;
                    avatar = contact ? contact.avatar : getAvatarForUser(targetId);
                } else if (typeName === 'GROUP') {
                    const group = state.groups[targetId];
                    name = group ? group.name : targetId;
                    avatar = group ? group.avatar : getAvatarForGroup(targetId);
                } else {
                    name = targetId;
                    avatar = typeName.charAt(0);
                }
                
                // 构建会话对象
                state.conversations[conversation.conversationId] = {
                    id: conversation.conversationId,
                    type: typeName,
                    targetId: targetId,
                    name: name,
                    avatar: avatar,
                    isTop: conversation.isTop === 1,
                    isMute: conversation.isMute === 1,
                    sequence: conversation.sequence,
                    readSequence: conversation.readSequence,
                    unreadCount: 0, // 初始设为0，后续根据需要计算
                    fromId: conversation.fromId,
                    lastMessage: {
                        content: '暂无消息',
                        timestamp: Date.now()
                    }
                };
            } catch (err) {
                console.error('处理会话项时出错:', err, conversation);
            }
        });
        
        console.log('处理后的会话列表:', Object.values(state.conversations));
        
        // 更新UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
    }
    
    /**
     * 获取用户头像
     * @param {string} userId 用户ID
     * @returns {string} 头像URL或首字母
     */
    function getAvatarForUser(userId) {
        return userId ? userId.charAt(0).toUpperCase() : 'U';
    }
    
    /**
     * 获取群组头像
     * @param {string} groupId 群组ID
     * @returns {string} 头像URL或首字母
     */
    function getAvatarForGroup(groupId) {
        return 'G';
    }
    
    /**
     * 切换页面
     * @param {string} page 页面名称
     */
    function switchPage(page) {
        // 更新侧边栏图标状态
        elements.sidebar.sidebarIcons.forEach(icon => {
            if (icon.getAttribute('data-page') === page) {
                icon.classList.add('active');
            } else {
                icon.classList.remove('active');
            }
        });
        
        // 更新页面内容显示
        elements.pageContents.forEach(content => {
            if (content.id === `${page}-page`) {
                content.classList.add('active');
            } else {
                content.classList.remove('active');
            }
        });
    }
    
    /**
     * 处理WebSocket连接成功
     * @param {Event} event 
     */
    function handleWebSocketOpen(event) {
        console.log('WebSocket连接已打开');
        
        // 更新用户状态为在线
        if (elements.sidebar.profileStatus) {
            elements.sidebar.profileStatus.textContent = '在线';
        }
    }
    
    /**
     * 处理WebSocket消息
     * @param {Object} message 
     */
    function handleWebSocketMessage(message) {
        console.log('收到WebSocket消息:', message);
        
        try {
            // 消息可能是ArrayBuffer格式
            if (message.data instanceof ArrayBuffer) {
                // 解码二进制消息
                const buffer = new ByteBuffer(message.data);
                const command = buffer.readInt32();
                const version = buffer.readInt32();
                const clientType = buffer.readInt32();
                const messageType = buffer.readInt32();
                const appId = buffer.readInt32();
                const imeiLength = buffer.readInt32();
                const bodyLength = buffer.readInt32();
                const imei = buffer.readVString(imeiLength);
                const body = buffer.readVString(bodyLength);
                
                console.log('解码消息:', { 
                    command, 
                    body, 
                    appId, 
                    clientType, 
                    messageType 
                });
                
                // 根据命令类型分发消息
                switch (command) {
                    case 9000: // 登录响应
                        console.log('登录响应:', body);
                        handleLoginResponse(JSON.parse(body));
                        break;
                    case 1103: // 私聊消息
                        try {
                            const parsedBody = JSON.parse(body);
                            console.log('解析私聊消息:', parsedBody);
                            // 检查不同的数据结构
                            if (parsedBody.data) {
                                // 如果包含data字段，传递data
                                handleChatMessage(parsedBody.data);
                            } else if (parsedBody.messageBody !== undefined) {
                                // 直接是消息体，传递整个对象
                                handleChatMessage(parsedBody);
                            } else {
                                console.warn('无法识别的私聊消息格式:', parsedBody);
                            }
                        } catch (e) {
                            console.error('解析私聊消息体失败:', e, body);
                        }
                        break;
                    case 2104: // 群聊消息
                        try {
                            const parsedBody = JSON.parse(body);
                            console.log('解析群聊消息:', parsedBody);
                            if (parsedBody.data) {
                                handleGroupMessage(parsedBody.data);
                            } else if (parsedBody.messageBody !== undefined) {
                                handleGroupMessage(parsedBody);
                            } else {
                                console.warn('无法识别的群聊消息格式:', parsedBody);
                            }
                        } catch (e) {
                            console.error('解析群聊消息体失败:', e, body);
                        }
                        break;
                    case 9999: // 系统消息
                        try {
                            handleSystemMessage(JSON.parse(body));
                        } catch (e) {
                            console.error('解析系统消息体失败:', e, body);
                        }
                        break;
                    default:
                        console.log(`未处理的消息命令: ${command}`);
                }
            } else if (typeof message.data === 'string') {
                // 处理文本消息
                try {
                    let data = JSON.parse(message.data);
                    console.log('解析JSON消息:', data);
                    
                    // 根据消息类型分发
                    if (data.command) {
                        switch (data.command) {
                            case 9000: // 登录响应
                                handleLoginResponse(data);
                                break;
                            case 1103: // 私聊消息
                                // 检查body是否是字符串，如果是则需要解析
                                if (data.body && typeof data.body === 'string') {
                                    try {
                                        let parsedBody = JSON.parse(data.body);
                                        console.log('解析私聊消息body:', parsedBody);
                                        if (parsedBody.data) {
                                            handleChatMessage(parsedBody.data);
                                        } else if (parsedBody.messageBody !== undefined) {
                                            handleChatMessage(parsedBody);
                                        } else {
                                            console.warn('无法识别的私聊消息格式:', parsedBody);
                                        }
                                    } catch (e) {
                                        console.error('解析私聊消息body失败:', e, data.body);
                                    }
                                } else {
                                    // body不是字符串或不存在，直接使用data
                                    if (data.data) {
                                        handleChatMessage(data.data);
                                    } else if (data.messageBody !== undefined) {
                                        handleChatMessage(data);
                                    } else {
                                        console.warn('无法识别的私聊消息格式:', data);
                                    }
                                }
                                break;
                            case 2104: // 群聊消息
                                // 检查body是否是字符串，如果是则需要解析
                                if (data.body && typeof data.body === 'string') {
                                    try {
                                        let parsedBody = JSON.parse(data.body);
                                        console.log('解析群聊消息body:', parsedBody);
                                        if (parsedBody.data) {
                                            handleGroupMessage(parsedBody.data);
                                        } else if (parsedBody.messageBody !== undefined) {
                                            handleGroupMessage(parsedBody);
                                        } else {
                                            console.warn('无法识别的群聊消息格式:', parsedBody);
                                        }
                                    } catch (e) {
                                        console.error('解析群聊消息body失败:', e, data.body);
                                    }
                                } else {
                                    // body不是字符串或不存在，直接使用data
                                    if (data.data) {
                                        handleGroupMessage(data.data);
                                    } else if (data.messageBody !== undefined) {
                                        handleGroupMessage(data);
                                    } else {
                                        console.warn('无法识别的群聊消息格式:', data);
                                    }
                                }
                                break;
                            case 9999: // 系统消息
                                if (data.body && typeof data.body === 'string') {
                                    try {
                                        handleSystemMessage(JSON.parse(data.body));
                                    } catch (e) {
                                        console.error('解析系统消息body失败:', e, data.body);
                                    }
                                } else {
                                    handleSystemMessage(data);
                                }
                                break;
                            default:
                                console.log(`未处理的消息命令: ${data.command}`);
                        }
                    }
                } catch (e) {
                    console.error('解析JSON消息失败:', e);
                }
            }
        } catch (error) {
            console.error('处理WebSocket消息时出错:', error);
        }
    }
    
    /**
     * 处理登录响应
     * @param {Object} response 登录响应
     */
    function handleLoginResponse(response) {
        console.log('处理登录响应:', response);
        
        if (response.success) {
            console.log('登录成功');
        } else {
            console.error('登录失败:', response.msg || '未知错误');
        }
    }
    
    /**
     * 处理WebSocket连接关闭
     * @param {Event} event 
     */
    function handleWebSocketClose(event) {
        console.log('WebSocket连接已关闭');
        
        // 更新用户状态为离线
        if (elements.sidebar.profileStatus) {
            elements.sidebar.profileStatus.textContent = '离线';
        }
    }
    
    /**
     * 处理WebSocket错误
     * @param {Event} event 
     */
    function handleWebSocketError(event) {
        console.error('WebSocket错误:', event);
    }
    
    /**
     * 处理主题变更
     * @param {string} theme 主题名称
     */
    function handleThemeChange(theme) {
        if (theme === 'dark') {
            document.body.classList.add('dark-theme');
        } else {
            document.body.classList.remove('dark-theme');
        }
        
        // 保存设置
        saveUISettings({ theme });
    }
    
    /**
     * 处理界面风格变更
     * @param {string} style 风格名称
     */
    function handleStyleChange(style) {
        if (style === 'modern') {
            document.body.classList.add('modern-style');
        } else {
            document.body.classList.remove('modern-style');
        }
        
        // 保存设置
        saveUISettings({ style });
    }
    
    /**
     * 处理字体大小变更
     * @param {number} size 字体大小
     */
    function handleFontSizeChange(size) {
        document.documentElement.style.setProperty('--font-size-base', `${size}px`);
        document.documentElement.style.setProperty('--font-size-small', `${size - 2}px`);
        document.documentElement.style.setProperty('--font-size-large', `${parseInt(size) + 2}px`);
        document.documentElement.style.setProperty('--font-size-xl', `${parseInt(size) + 4}px`);
        
        if (elements.settings.fontSizeValue) {
            elements.settings.fontSizeValue.textContent = `${size}px`;
        }
        
        // 保存设置
        saveUISettings({ fontSize: size });
    }
    
    /**
     * 保存UI设置
     * @param {Object} newSettings 新设置
     */
    function saveUISettings(newSettings) {
        try {
            const settings = JSON.parse(localStorage.getItem('uiSettings')) || {};
            const updatedSettings = { ...settings, ...newSettings };
            localStorage.setItem('uiSettings', JSON.stringify(updatedSettings));
        } catch (error) {
            console.error('保存UI设置失败:', error);
        }
    }
    
    /**
     * 处理发送消息
     * @param {Object} data 消息数据
     */
    async function handleSendMessage(data) {
        try {
            let result;
            
            if (data.conversationType === 'C2C') {
                result = await state.imClient.sendChatMessage(data.targetId, data.content);
            } else {
                result = await state.imClient.sendGroupMessage(data.targetId, data.content);
            }
            
            if (result.success) {
                // 清空输入框
                elements.chat.messageInput.value = '';
                elements.chat.sendBtn.disabled = true;
                
                // 添加消息到UI
                state.uiController.addMessage({
                    id: result.message.id,
                    content: result.message.content,
                    fromId: state.user.userId,
                    timestamp: result.message.timestamp,
                    status: 'sent',
                    isOwn: true
                });
                
                // 滚动到底部
                state.uiController.scrollToBottom();
            } else {
                console.error('发送消息失败:', result.error);
            }
        } catch (error) {
            console.error('发送消息时出错:', error);
        }
    }
    
    /**
     * 向当前会话发送消息
     * @param {string} content 消息内容
     */
    function sendMessageToCurrentConversation(content) {
        const currentConversation = state.uiController.getCurrentConversation();
        if (currentConversation) {
            handleSendMessage({
                conversationType: currentConversation.type,
                targetId: currentConversation.targetId,
                content: content
            });
        }
    }
    
    /**
     * 处理会话选择
     * @param {Object} conversation 会话对象
     */
    function handleConversationSelect(conversation) {
        if (!conversation) return;
        
        // 获取消息历史
        const messages = state.imClient.getMessageList(conversation.type, conversation.targetId);
        
        // 更新UI
        state.uiController.clearMessages();
        
        // 添加消息到UI
        messages.forEach(msg => {
            state.uiController.addMessage({
                id: msg.id,
                content: msg.content,
                fromId: msg.fromId,
                timestamp: msg.timestamp,
                status: msg.status,
                isOwn: msg.fromId === state.user.userId
            });
        });
        
        // 滚动到底部
        state.uiController.scrollToBottom();
        
        // 清空未读数
        state.imClient.clearUnreadCount(conversation.type, conversation.targetId);
        
        // 更新会话列表UI
        if (state.conversations[conversation.id]) {
            state.conversations[conversation.id].unreadCount = 0;
            state.uiController.updateConversationsList(Object.values(state.conversations));
        }
        
        // 启用消息输入框和发送按钮
        if (elements.chat.messageInput) {
            elements.chat.messageInput.disabled = false;
            elements.chat.messageInput.focus();
        }
        
        if (elements.chat.sendBtn) {
            elements.chat.sendBtn.disabled = !elements.chat.messageInput.value.trim();
        }
    }
    
    /**
     * 处理登出
     */
    function handleLogout() {
        if (state.imClient) {
            state.imClient.disconnect();
        }
        
        // 重置状态
        state.user = null;
        state.imClient = null;
        state.wsClient = null;
        state.conversations = {};
        state.contacts = {};
        state.groups = {};
        
        // 显示登录页面
        if (elements.mainApp) {
            elements.mainApp.classList.add('hidden');
        }
        
        if (elements.loginPage) {
            elements.loginPage.classList.remove('hidden');
        }
    }
    
    /**
     * 处理聊天消息
     * @param {Object} message 消息对象
     */
    function handleChatMessage(message) {
        console.log('处理聊天消息:', message);
        
        // 确保消息对象格式正确
        let messageContent = '';
        let fromUserId = '';
        let toUserId = '';
        let messageId = '';
        let timestamp = Date.now();
        
        // 提取关键字段
        if (message) {
            // 提取消息内容
            if (message.messageBody !== undefined) {
                messageContent = message.messageBody;
            } else if (message.content !== undefined) {
                messageContent = message.content;
            } else if (message.msg !== undefined) {
                messageContent = message.msg;
            }
            
            // 提取发送者ID
            if (message.fromId !== undefined) {
                fromUserId = message.fromId;
            } else if (message.from !== undefined) {
                fromUserId = message.from;
            }
            
            // 提取接收者ID
            if (message.toId !== undefined) {
                toUserId = message.toId;
            } else if (message.to !== undefined) {
                toUserId = message.to;
            }
            
            // 提取消息ID
            if (message.messageId !== undefined) {
                messageId = message.messageId;
            } else if (message.id !== undefined) {
                messageId = message.id;
            }
            
            // 提取时间戳
            if (message.timestamp !== undefined) {
                timestamp = message.timestamp;
            } else if (message.messageTime !== undefined) {
                timestamp = message.messageTime;
            } else if (message.time !== undefined) {
                timestamp = message.time;
            }
        }
        
        // 如果没有必要的字段，记录错误并返回
        if (!messageContent || !fromUserId) {
            console.error('聊天消息缺少必要字段:', message);
            return;
        }
        
        // 构建会话ID
        const conversationId = `C2C_${fromUserId === state.user.userId ? toUserId : fromUserId}`;
        const targetId = fromUserId === state.user.userId ? toUserId : fromUserId;
        
        // 检查是否是新会话
        if (!state.conversations[conversationId]) {
            const contact = state.contacts[targetId];
            state.conversations[conversationId] = {
                id: conversationId,
                type: 'C2C',
                targetId: targetId,
                name: contact ? contact.name : targetId,
                avatar: contact ? contact.avatar : getAvatarForUser(targetId),
                unreadCount: fromUserId === state.user.userId ? 0 : 1
            };
        } else if (fromUserId !== state.user.userId) {
            // 增加未读数
            state.conversations[conversationId].unreadCount = (state.conversations[conversationId].unreadCount || 0) + 1;
        }
        
        // 更新最后一条消息
        state.conversations[conversationId].lastMessage = {
            content: messageContent,
            timestamp: timestamp
        };
        
        // 更新会话列表UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
        
        // 如果当前正在查看该会话，则添加消息到UI
        const currentConversation = state.uiController.getCurrentConversation();
        if (currentConversation && currentConversation.id === conversationId) {
            state.uiController.addMessage({
                id: messageId,
                content: messageContent,
                fromId: fromUserId,
                timestamp: timestamp,
                status: 'received',
                isOwn: fromUserId === state.user.userId
            });
            
            // 滚动到底部
            state.uiController.scrollToBottom();
            
            // 清空未读数
            state.conversations[conversationId].unreadCount = 0;
            state.imClient.clearUnreadCount('C2C', targetId);
            state.uiController.updateConversationsList(Object.values(state.conversations));
        }
    }
    
    /**
     * 处理群聊消息
     * @param {Object} message 消息对象
     */
    function handleGroupMessage(message) {
        console.log('处理群聊消息:', message);
        
        // 确保消息对象格式正确
        let messageContent = '';
        let fromUserId = '';
        let groupId = '';
        let messageId = '';
        let timestamp = Date.now();
        
        // 提取关键字段
        if (message) {
            // 提取消息内容
            if (message.messageBody !== undefined) {
                messageContent = message.messageBody;
            } else if (message.content !== undefined) {
                messageContent = message.content;
            } else if (message.msg !== undefined) {
                messageContent = message.msg;
            }
            
            // 提取发送者ID
            if (message.fromId !== undefined) {
                fromUserId = message.fromId;
            } else if (message.from !== undefined) {
                fromUserId = message.from;
            }
            
            // 提取群组ID
            if (message.groupId !== undefined) {
                groupId = message.groupId;
            } else if (message.toId !== undefined) {
                groupId = message.toId;
            }
            
            // 提取消息ID
            if (message.messageId !== undefined) {
                messageId = message.messageId;
            } else if (message.id !== undefined) {
                messageId = message.id;
            }
            
            // 提取时间戳
            if (message.timestamp !== undefined) {
                timestamp = message.timestamp;
            } else if (message.messageTime !== undefined) {
                timestamp = message.messageTime;
            } else if (message.time !== undefined) {
                timestamp = message.time;
            }
        }
        
        // 如果没有必要的字段，记录错误并返回
        if (!messageContent || !fromUserId || !groupId) {
            console.error('群聊消息缺少必要字段:', message);
            return;
        }
        
        // 构建会话ID
        const conversationId = `GROUP_${groupId}`;
        
        // 检查是否是新会话
        if (!state.conversations[conversationId]) {
            const group = state.groups[groupId];
            state.conversations[conversationId] = {
                id: conversationId,
                type: 'GROUP',
                targetId: groupId,
                name: group ? group.name : groupId,
                avatar: group ? group.avatar : getAvatarForGroup(groupId),
                unreadCount: fromUserId === state.user.userId ? 0 : 1
            };
        } else if (fromUserId !== state.user.userId) {
            // 增加未读数
            state.conversations[conversationId].unreadCount = (state.conversations[conversationId].unreadCount || 0) + 1;
        }
        
        // 更新最后一条消息
        state.conversations[conversationId].lastMessage = {
            content: messageContent,
            timestamp: timestamp
        };
        
        // 更新会话列表UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
        
        // 如果当前正在查看该会话，则添加消息到UI
        const currentConversation = state.uiController.getCurrentConversation();
        if (currentConversation && currentConversation.id === conversationId) {
            state.uiController.addMessage({
                id: messageId,
                content: messageContent,
                fromId: fromUserId,
                timestamp: timestamp,
                status: 'received',
                isOwn: fromUserId === state.user.userId
            });
            
            // 滚动到底部
            state.uiController.scrollToBottom();
            
            // 清空未读数
            state.conversations[conversationId].unreadCount = 0;
            state.imClient.clearUnreadCount('GROUP', groupId);
            state.uiController.updateConversationsList(Object.values(state.conversations));
        }
    }
    
    /**
     * 处理系统消息
     * @param {Object} message 消息对象
     */
    function handleSystemMessage(message) {
        console.log('收到系统消息:', message);
        
        // 这里可以根据系统消息类型做不同处理
    }
    
    /**
     * 处理消息确认回执
     * @param {Object} ack 确认回执对象
     */
    function handleMessageAck(ack) {
        console.log('收到消息确认回执:', ack);
        
        // 更新消息状态
        state.uiController.updateMessageStatus(ack.messageId, 'delivered');
    }
    
    /**
     * 处理已读回执
     * @param {Object} receipt 已读回执对象
     */
    function handleReadReceipt(receipt) {
        console.log('收到已读回执:', receipt);
        
        // 更新消息状态
        // 这里需要根据sequence找到对应的消息
    }
    
    /**
     * 处理创建群组
     * @param {Object} groupData 群组数据
     */
    async function handleCreateGroup(groupData) {
        if (!state.user || !groupData.groupName) return;
        
        try {
            console.log('创建群组:', groupData);
            
            // 调用API创建群组
            const result = await state.apiClient.createGroup({
                userId: state.user.userId,
                groupName: groupData.groupName,
                introduction: groupData.introduction || ''
            });
            
            if (result && result.groupId) {
                // 添加到群组列表
                const group = {
                    id: result.groupId,
                    name: groupData.groupName,
                    avatar: getAvatarForGroup(result.groupId),
                    memberCount: 1
                };
                
                state.groups[result.groupId] = group;
                
                // 更新UI
                state.uiController.updateGroupsList(Object.values(state.groups));
                
                // 切换回已加入标签
                const joinedTab = document.querySelector('#groups-page .tab[data-tab="joined"]');
                if (joinedTab) {
                    joinedTab.click();
                }
                
                // 显示成功消息
                alert('群组创建成功');
            } else {
                alert('创建群组失败');
            }
        } catch (error) {
            console.error('创建群组时出错:', error);
            alert(`创建群组失败: ${error.message}`);
        }
    }
    
    /**
     * 处理添加好友
     * @param {Object} friendData 好友数据
     */
    async function handleAddFriend(friendData) {
        if (!state.user || !friendData.userId) return;
        
        try {
            console.log('添加好友:', friendData);
            
            // 调用API添加好友
            const result = await state.apiClient.addFriend({
                fromId: state.user.userId,
                toId: friendData.userId,
                remark: friendData.remark || '',
                addSource: friendData.addSource || '个人搜索',
                addWording: friendData.addWording || '请求添加您为好友',
                clientType: state.user.clientType,
                imei: state.user.imei
            });
            
            if (result) {
                // 添加到联系人列表
                const contact = {
                    id: friendData.userId,
                    name: friendData.remark || friendData.userId,
                    avatar: getAvatarForUser(friendData.userId),
                    status: 'offline'
                };
                
                state.contacts[friendData.userId] = contact;
                
                // 更新UI
                state.uiController.updateContactsList(Object.values(state.contacts));
                
                // 显示成功消息
                alert('好友添加成功');
            } else {
                alert('添加好友失败');
            }
        } catch (error) {
            console.error('添加好友时出错:', error);
            alert(`添加好友失败: ${error.message}`);
        }
    }
    
    // 初始化应用
    document.addEventListener('DOMContentLoaded', init);
})(); 