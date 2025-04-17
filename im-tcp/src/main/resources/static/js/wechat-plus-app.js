/**
 * WeChatPlus主应用
 * 负责初始化和协调WebSocket客户端、消息处理和UI控制器
 */
(function() {
    'use strict';
    
    // 全局状态
    const state = {
        user: null,
        imClient: null,
        uiController: null,
        conversations: {},
        contacts: {},
        groups: {},
        pendingConversations: null
    };
    
    // 设置全局状态，方便调试
    window.state = state;
    
    // DOM元素缓存
    const elements = {};
    
    /**
     * 初始化应用
     */
    function init() {
        cacheElements();
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
    }
    
    /**
     * 设置事件监听器
     */
    function setupEventListeners() {
        // 登录按钮点击事件
        if (elements.loginForm.loginBtn) {
            elements.loginForm.loginBtn.addEventListener('click', handleLogin);
        }
        
        // 其他初始事件监听器在UI Controller中处理
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
            // 保存用户信息
            state.user = {
                userId,
                appId: parseInt(appId, 10),
                clientType: parseInt(clientType, 10),
                imei
            };
            
            // 保存到最近账号
            saveRecentAccount(state.user);
            
            // 创建IM客户端
            createIMClient();
            
            // 连接WebSocket
            try {
                await state.imClient.connect();
                console.log('WebSocket连接成功');
            } catch (connError) {
                console.error('WebSocket连接失败:', connError);
                alert(`WebSocket连接失败: ${connError.message}`);
                return;
            }
            
            // 登录
            const loginResult = await state.imClient.login(state.user);
            
            if (loginResult.success) {
                console.log('登录成功');
                
                // 初始化UI控制器
                initUIController();
                
                // 加载UI设置
                loadUISettings();
                
                // 显示主应用
                showMainApp();
                
                // 加载用户数据 - 但不立即渲染会话列表
                let userData;
                try {
                    userData = await loadUserData();
                    console.log('用户数据加载完成');
                    
                    // 将会话列表数据保存到全局状态中，等待消息同步完成后再处理
                    if (userData && userData.conversations) {
                        state.pendingConversations = userData.conversations;
                        console.log('暂存会话列表数据，等待消息同步完成后处理');
                    }
                } catch (dataError) {
                    console.error('加载用户数据时出错:', dataError);
                }
                
                // 同步会话消息，它会在这里执行，在获取会话列表之后
                try {
                    if (state.imClient) {
                        // 同步用户所有会话消息，使用强制同步模式
                        console.log('开始同步会话消息');
                        await state.imClient._syncConversationMessages(null, true);
                        console.log('会话消息同步API调用完成');
                        // 注意：实际的消息处理和UI更新会在onMessageSyncComplete回调中完成
                    }
                } catch (error) {
                    console.error('同步数据失败:', error);
                    
                    // 如果消息同步调用失败，直接处理会话列表
                    if (state.pendingConversations) {
                        console.log('消息同步调用失败，直接处理暂存的会话列表');
                        processConversationList(state.pendingConversations, true);
                        state.pendingConversations = null;
                    }
                }
                
                // 确保从服务器获取的会话列表被显示 - 这部分改为只在没有数据时执行
                setTimeout(async () => {
                    try {
                        // 检查会话列表是否已有数据
                        const conversationsCount = Object.keys(state.conversations).length;
                        
                        if (conversationsCount > 0) {
                            console.log('会话列表已有数据，跳过延迟加载');
                            return;
                        }
                        
                        // 只有在会话列表为空时才重新获取
                        console.log('会话列表为空，尝试延迟加载');
                        
                        // 尝试从IM客户端获取
                        const conversations = state.imClient.getConversationList();
                        if (conversations && conversations.length > 0) {
                            console.log('延迟加载：从IM客户端获取到会话列表:', conversations.length);
                            processConversationList(conversations, true);
                            return;
                        }
                        
                        // 如果IM客户端没有数据，尝试从API直接获取
                        try {
                            const convData = await state.imClient.apiClient.syncConversationList(state.user.userId);
                            if (convData) {
                                console.log('延迟加载：从API获取到会话列表:', convData);
                                processConversationList(convData, true);
                            }
                        } catch (syncError) {
                            console.error('延迟加载：直接同步会话列表失败:', syncError);
                        }
                    } catch (error) {
                        console.error('延迟加载会话列表失败:', error);
                    }
                }, 1000); // 延长等待时间到1秒，确保有足够时间获取数据
                
                // 设置全局用户ID
                window.currentUserId = state.user.userId;
            } else {
                console.error('登录失败:', loginResult.error);
                alert(`登录失败: ${loginResult.error}`);
            }
        } catch (error) {
            console.error('登录过程出错:', error);
            alert(`登录出错: ${error.message}`);
        }
        
        // 解除按钮禁用状态
        elements.loginForm.loginBtn.disabled = false;
        elements.loginForm.loginBtn.textContent = '登录';
    }
    
    /**
     * 保存最近账号
     * @param {Object} account 账号信息
     */
    function saveRecentAccount(account) {
        try {
            let accounts = JSON.parse(localStorage.getItem('recentAccounts')) || [];
            
            // 检查是否已存在
            const existingIndex = accounts.findIndex(a => a.userId === account.userId);
            
            if (existingIndex !== -1) {
                // 已存在则移除
                accounts.splice(existingIndex, 1);
            }
            
            // 添加到最前面
            accounts.unshift(account);
            
            // 限制最多保存5个账号
            if (accounts.length > 5) {
                accounts = accounts.slice(0, 5);
            }
            
            localStorage.setItem('recentAccounts', JSON.stringify(accounts));
        } catch (error) {
            console.error('保存最近账号失败:', error);
        }
    }
    
    /**
     * 创建IM客户端
     */
    function createIMClient() {
        state.imClient = new IMClient({
            callbacks: {
                onConnected: handleWebSocketOpen,
                onDisconnected: handleWebSocketClose,
                onError: handleWebSocketError,
                onChatMessage: handleChatMessage,
                onGroupMessage: handleGroupMessage,
                onSystemMessage: handleSystemMessage,
                onMessageAck: handleMessageAck,
                onReadReceipt: handleReadReceipt,
                onFriendRequest: handleFriendRequest,
                onFriendStatusChange: handleFriendStatusChange,
                onGroupStatusChange: handleGroupStatusChange,
                onConversationsUpdated: handleConversationsUpdated,
                onConversationsCreated: handleConversationsCreated,
                onMessageSyncComplete: handleMessageSyncComplete
            }
        });
    }
    
    /**
     * 处理会话列表更新
     * @param {Array} conversations 会话列表
     */
    function handleConversationsUpdated(conversations) {
        console.log('会话列表已更新，数量:', conversations.length);
        // 更新UI
        if (state.uiController) {
            state.uiController.updateConversationsList(conversations);
        }
    }
    
    /**
     * 处理新创建的会话
     * @param {Array} conversations 新创建的会话列表
     */
    function handleConversationsCreated(conversations) {
        console.log('创建了新会话，数量:', conversations.length);
        // 转换为应用使用的会话格式
        conversations.forEach(conv => {
            const id = conv.id;
            state.conversations[id] = {
                id: id,
                type: conv.type,
                targetId: conv.targetId,
                name: conv.name || (conv.type === 'GROUP' ? `群组 ${conv.targetId}` : `用户 ${conv.targetId}`),
                lastMessage: conv.lastMessage || { content: '新消息', timestamp: Date.now() },
                unreadCount: 0
            };
        });
        
        // 更新UI
        if (state.uiController) {
            state.uiController.updateConversationsList(Object.values(state.conversations));
        }
    }
    
    /**
     * 处理消息同步完成
     * @param {boolean} success 是否成功
     * @param {string} message 消息
     */
    function handleMessageSyncComplete(success, message) {
        console.log(`消息同步完成: ${success ? '成功' : '失败'}, ${message}`);
        
        // 获取暂存的会话列表数据
        if (state.pendingConversations) {
            console.log('消息同步完成后处理暂存的会话列表');
            processConversationList(state.pendingConversations, true);
            state.pendingConversations = null;
        } else {
            console.log('没有暂存的会话列表数据');
        }
    }
    
    /**
     * 初始化UI控制器
     */
    function initUIController() {
        // 从localStorage加载自动跳转设置
        let uiSettings = {};
        try {
            uiSettings = JSON.parse(localStorage.getItem('uiSettings')) || {};
        } catch (error) {
            console.error('解析UI设置失败:', error);
        }
        
        state.uiController = new UIController({
            elements: elements,
            onConversationSelect: handleConversationSelect,
            onSendMessage: handleSendMessage,
            onThemeChange: handleThemeChange,
            onStyleChange: handleStyleChange,
            onFontSizeChange: handleFontSizeChange,
            onCreateGroup: handleCreateGroup,
            onAddFriend: handleAddFriend,
            autoSwitchToChat: uiSettings.autoSwitchToChat || false,
            onAutoSwitchChange: handleAutoSwitchChange,
            // 新增：滚动加载更多消息回调
            onLoadMoreMessages: async (conversationId) => {
                console.log('UI触发加载更多消息:', conversationId);
                
                if (!state.imClient) {
                    console.warn('IM客户端未初始化，无法加载更多消息');
                    return { hasMore: false };
                }
                
                try {
                    // 调用IMClient的滚动加载方法
                    const hasMore = await state.imClient.handleScrollLoadMessages(conversationId);
                    
                    // 获取当前会话的消息列表
                    let messages = [];
                    if (state.imClient.messageCache) {
                        messages = state.imClient.messageCache.get(conversationId) || [];
                    }
                    
                    console.log(`加载更多消息完成，hasMore=${hasMore}, 当前消息缓存数量=${messages.length}`);
                    
                    return {
                        hasMore: hasMore,
                        messages: messages
                    };
                } catch (error) {
                    console.error('加载更多消息失败:', error);
                    return { hasMore: false };
                }
            }
        });
        
        // 不再添加测试会话数据
        console.log('UI控制器初始化完成');
    }
    
    /**
     * 加载UI设置
     */
    function loadUISettings() {
        try {
            const settings = JSON.parse(localStorage.getItem('uiSettings')) || {};
            
            // 应用主题
            if (settings.theme) {
                if (settings.theme === 'dark') {
                    document.body.classList.add('dark-theme');
                    if (elements.settings.themeToggle) {
                        elements.settings.themeToggle.checked = true;
                    }
                } else {
                    document.body.classList.remove('dark-theme');
                    if (elements.settings.themeToggle) {
                        elements.settings.themeToggle.checked = false;
                    }
                }
            }
            
            // 应用界面风格
            if (settings.style) {
                if (settings.style === 'modern') {
                    document.body.classList.add('modern-style');
                    if (elements.settings.uiStyleToggle) {
                        elements.settings.uiStyleToggle.checked = true;
                    }
                } else {
                    document.body.classList.remove('modern-style');
                    if (elements.settings.uiStyleToggle) {
                        elements.settings.uiStyleToggle.checked = false;
                    }
                }
            }
            
            // 应用字体大小
            if (settings.fontSize) {
                document.documentElement.style.setProperty('--font-size-base', `${settings.fontSize}px`);
                document.documentElement.style.setProperty('--font-size-small', `${settings.fontSize - 2}px`);
                document.documentElement.style.setProperty('--font-size-large', `${parseInt(settings.fontSize) + 2}px`);
                document.documentElement.style.setProperty('--font-size-xl', `${parseInt(settings.fontSize) + 4}px`);
                
                if (elements.settings.fontSizeRange) {
                    elements.settings.fontSizeRange.value = settings.fontSize;
                }
                
                if (elements.settings.fontSizeValue) {
                    elements.settings.fontSizeValue.textContent = `${settings.fontSize}px`;
                }
            }
            
            // 应用聊天自动跳转设置
            if (settings.hasOwnProperty('autoSwitchToChat')) {
                const autoSwitchToggle = document.getElementById('auto-switch-toggle');
                if (autoSwitchToggle) {
                    autoSwitchToggle.checked = settings.autoSwitchToChat;
                }
            }
        } catch (error) {
            console.error('加载UI设置失败:', error);
        }
    }
    
    /**
     * 显示主应用
     */
    function showMainApp() {
        // 隐藏登录页面，显示主应用
        if (elements.loginPage) {
            elements.loginPage.classList.add('hidden');
        }
        
        if (elements.mainApp) {
            elements.mainApp.classList.remove('hidden');
        }
        
        // 默认选中会话页面
        const chatsIcon = document.querySelector('.sidebar-icon[data-page="chats"]');
        if (chatsIcon) {
            state.uiController._switchPage('chats');
        }
        
        // 设置用户信息
        state.uiController.setUserInfo(state.user);
        
        // 设置在线状态
        if (elements.sidebar && elements.sidebar.profileStatus) {
            elements.sidebar.profileStatus.textContent = '在线';
        }
    }
    
    /**
     * 加载用户数据
     * @returns {Promise<Object>} 包含用户数据的Promise
     */
    async function loadUserData() {
        let errorMessages = [];
        const userData = {
            friends: [],
            groups: [],
            conversations: null  // 用于暂存会话列表数据，但不立即渲染
        };
        
        // 加载好友列表
        try {
            const friends = await state.imClient.getFriendList();
            if (friends && Array.isArray(friends)) {
                processFriendList(friends);
                userData.friends = friends;
            } else {
                console.warn('好友列表格式不正确或为空');
            }
        } catch (error) {
            console.error('加载好友列表失败:', error);
            errorMessages.push(`好友列表: ${error.message}`);
        }
        
        // 加载群组列表
        try {
            const groups = await state.imClient.getGroupList();
            if (groups && Array.isArray(groups)) {
                processGroupList(groups);
                userData.groups = groups;
            } else {
                console.warn('群组列表格式不正确或为空');
            }
        } catch (error) {
            console.error('加载群组列表失败:', error);
            errorMessages.push(`群组列表: ${error.message}`);
        }

        // 同步会话列表 - 仅获取数据但不立即渲染
        try {
            // 清空现有会话列表，因为这是首次加载
            state.conversations = {};
            
            const conversationResponse = await state.imClient.apiClient.syncConversationList(state.user.userId);
            console.log('同步会话列表响应:', conversationResponse);

            // 保存会话列表数据但不立即处理
            if (conversationResponse) {
                userData.conversations = conversationResponse;
                console.log('会话列表数据已保存，等待消息同步后再渲染');
            } else {
                console.warn('同步会话列表返回空数据');
            }
        } catch (error) {
            console.warn('同步会话列表失败:', error);

            // 如果同步失败，尝试从IM客户端获取会话列表（兼容处理）
            if (state.imClient && state.imClient.getConversationList) {
                try {
                    const conversations = state.imClient.getConversationList();
                    if (conversations && conversations.length > 0) {
                        userData.conversations = conversations;
                        console.log('从IM客户端获取的会话列表已保存，等待消息同步后再渲染');
                    } else {
                        console.warn('从IM客户端获取会话列表为空');
                    }
                } catch (error) {
                    console.error('获取会话列表失败:', error);
                }
            }
        }
        
        // 如果有任何加载错误，以通知的形式显示而不是弹窗阻断用户操作
        if (errorMessages.length > 0) {
            const errorMessage = `加载用户数据部分失败: ${errorMessages.join('; ')}`;
            console.error(errorMessage);
            
            // 如果界面有通知组件，可以使用它而不是alert
            if (state.uiController && typeof state.uiController.showNotification === 'function') {
                state.uiController.showNotification({
                    type: 'error',
                    message: '部分用户数据加载失败，某些功能可能不可用',
                    duration: 5000
                });
            }
        }
        
        return userData;
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
     * @param {boolean} clearExisting 是否清空现有会话，默认为true
     */
    function processConversationList(conversationData, clearExisting = true) {
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

        // 根据im-manager.js的数据格式处理
        // 直接匹配im-manager.js中的格式：convData.data.dataList
        if (conversationData && conversationData.dataList && Array.isArray(conversationData.dataList)) {
            conversationList = conversationData.dataList;
        } 
        // 如果是数组，直接使用
        else if (Array.isArray(conversationData)) {
            conversationList = conversationData;
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

        // 获取现有会话列表大小，用于日志
        const existingCount = Object.keys(state.conversations).length;

        // 根据 clearExisting 参数决定是否清空当前会话列表
        if (clearExisting) {
            // 清空当前会话列表，重新构建
            state.conversations = {};
            console.log('处理会话列表: 已清空现有会话列表');
        } else {
            console.log('处理会话列表: 保留现有会话列表');
        }

        // 处理每个会话
        let processedCount = 0;
        conversationList.forEach(conversation => {
            // 支持两种ID格式：conversationId（服务端）和id（前端UI）
            if (!conversation || (!conversation.conversationId && !conversation.id)) {
                console.warn('跳过无效会话:', conversation);
                return;
            }

            try {
                // 使用conversationId或id作为会话ID
                const conversationId = conversation.conversationId || conversation.id;
                
                // 根据会话类型获取目标ID和类型
                // 如果已经有type字段则使用，否则根据conversationType解析
                let conversationType, targetId, typeName;
                
                if (conversation.type) {
                    // 前端传来的会话对象
                    typeName = conversation.type;
                    targetId = conversation.targetId;
                } else {
                    // 服务端传来的会话对象
                    conversationType = conversation.conversationType;
                    targetId = conversation.toId;

                    // 确定会话类型的字符串表示
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
                }

                // 获取会话名称和头像
                let name, avatar;
                if (typeName === 'C2C') {
                    const contact = state.contacts[targetId];
                    name = conversation.name || (contact ? contact.name : targetId);
                    avatar = contact ? contact.avatar : getAvatarForUser(targetId);
                } else if (typeName === 'GROUP') {
                    const group = state.groups[targetId];
                    name = conversation.name || (group ? group.name : targetId);
                    avatar = group ? group.avatar : getAvatarForGroup(targetId);
                } else {
                    name = conversation.name || targetId;
                    avatar = typeName.charAt(0);
                }

                // 构建会话对象
                state.conversations[conversationId] = {
                    id: conversationId,
                    type: typeName,
                    targetId: targetId,
                    name: name,
                    avatar: avatar,
                    isTop: conversation.isTop === 1,
                    isMute: conversation.isMute === 1,
                    sequence: conversation.sequence,
                    readSequence: conversation.readSequence,
                    unreadCount: conversation.unreadCount || 0,
                    fromId: conversation.fromId,
                    lastMessage: conversation.lastMessage || {
                        content: '暂无消息',
                        timestamp: Date.now()
                    }
                };
                
                processedCount++;
            } catch (err) {
                console.error('处理会话项时出错:', err, conversation);
            }
        });

        const totalCount = Object.keys(state.conversations).length;
        console.log(`处理会话列表: 处理了 ${processedCount} 个会话, 之前: ${existingCount}, 现在: ${totalCount}`);
        console.log('处理后的会话列表:', Object.values(state.conversations));

        // 更新UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
    }

    /**
     * 获取用户头像
     * @param {string} userId 用户ID
     * @returns {string} 头像URL
     */
    function getAvatarForUser(userId) {
        return `https://i.pravatar.cc/100?u=${userId}`;
    }
    
    /**
     * 获取群组头像
     * @param {string} groupId 群组ID
     * @returns {string} 头像URL
     */
    function getAvatarForGroup(groupId) {
        return `https://i.pravatar.cc/100?u=group_${groupId}`;
    }
    
    /**
     * 切换页面
     * @param {string} page 页面名称
     */
    function switchPage(page) {
        state.uiController._switchPage(page);
    }
    
    /**
     * 处理WebSocket连接打开
     * @param {Event} event 事件对象
     */
    function handleWebSocketOpen(event) {
        console.log('WebSocket连接已打开');
        
        // 更新用户状态为在线
        if (elements.sidebar && elements.sidebar.profileStatus) {
            elements.sidebar.profileStatus.textContent = '在线';
        }
    }
    
    /**
     * 处理WebSocket消息
     */
    function handleWebSocketMessage(message) {
        // 注意：IM客户端内部会处理消息并分发给相应的回调函数
        // 检查是否是ByteBuffer对象
        if (message && message._org_buf && message._list && message._littleEndian !== undefined) {
            console.error('收到未解析的ByteBuffer对象:', '使用内部ByteBuffer解析功能处理');
        } else {
            console.log('收到WebSocket消息:', message);
        }
    }
    
    /**
     * 处理WebSocket连接关闭
     * @param {Event} event 事件对象
     */
    function handleWebSocketClose(event) {
        console.log('WebSocket连接已关闭');
        
        // 更新用户状态为离线
        if (elements.sidebar && elements.sidebar.profileStatus) {
            elements.sidebar.profileStatus.textContent = '离线';
        }
    }
    
    /**
     * 处理WebSocket错误
     * @param {Event} event 事件对象
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
                console.log('发送消息成功:', result.message);
                
                // 添加消息到UI
                state.uiController.addMessage(result.message);
                
                // 确保消息已添加到IMClient的缓存中
                // 注意：IMClient.sendChatMessage和sendGroupMessage已经会调用_addToMessageCache
                // 这里额外检查一下，以防万一
                const conversationId = state.imClient._getConversationId(
                    data.conversationType, data.targetId);
                const messageCache = state.imClient.messageCache.get(conversationId);
                
                if (messageCache) {
                    // 检查消息是否已在缓存中
                    const messageExists = messageCache.some(m => m.id === result.message.id);
                    
                    if (!messageExists) {
                        console.log('将消息添加到IMClient缓存:', result.message.id);
                        state.imClient._addToMessageCache(result.message);
                    }
                } else {
                    // 如果缓存不存在，创建并添加消息
                    console.log('创建IMClient消息缓存并添加消息:', result.message.id);
                    state.imClient._addToMessageCache(result.message);
                }
                
                // 生成会话ID - 确保格式一致
                const conversationId2 = `${data.conversationType}_${data.targetId}`;
                
                // 检查会话是否存在，使用严格匹配
                let conversation = state.conversations[conversationId2];
                
                if (conversation) {
                    console.log('更新会话最新消息:', conversationId2);
                    // 更新现有会话的最后消息
                    conversation.lastMessage = {
                        content: data.content,
                        timestamp: Date.now()
                    };
                } else {
                    // 如果会话不存在，创建新会话
                    let name, avatar;
                    if (data.conversationType === 'C2C') {
                        const contact = state.contacts[data.targetId];
                        name = contact ? contact.name : data.targetId;
                        avatar = contact ? contact.avatar : getAvatarForUser(data.targetId);
                    } else {
                        const group = state.groups[data.targetId];
                        name = group ? group.name : data.targetId;
                        avatar = group ? group.avatar : getAvatarForGroup(data.targetId);
                    }
                    
                    conversation = {
                        id: conversationId2,
                        type: data.conversationType,
                        targetId: data.targetId,
                        name: name,
                        avatar: avatar,
                        unreadCount: 0,
                        lastMessage: {
                            content: data.content,
                            timestamp: Date.now()
                        }
                    };
                    state.conversations[conversationId2] = conversation;
                }
                
                // 更新UI
                state.uiController.updateConversationsList(Object.values(state.conversations));
                
                // 同步会话到IMClient
                state.imClient._updateConversation({
                    type: data.conversationType,
                    targetId: data.targetId,
                    lastMessage: conversation.lastMessage
                });
                
                // 清空输入框
                if (elements.chat.messageInput) {
                    elements.chat.messageInput.value = '';
                }
                
                // 滚动到底部
                state.uiController.scrollToBottom();
            } else {
                console.error('发送消息失败:', result.error);
                alert(`发送消息失败: ${result.error}`);
            }
        } catch (error) {
            console.error('发送消息出错:', error);
            alert(`发送消息出错: ${error.message}`);
        }
    }
    
    /**
     * 发送消息到当前会话
     * @param {string} content 消息内容
     */
    function sendMessageToCurrentConversation(content) {
        const currentConversation = state.uiController.getCurrentConversation();
        
        if (currentConversation) {
            handleSendMessage({
                conversationType: currentConversation.type,
                targetId: currentConversation.targetId,
                content
            });
        }
    }
    
    /**
     * 处理会话选择
     * @param {Object} conversation 会话对象
     */
    function handleConversationSelect(conversation) {
        console.log('选择会话:', conversation);
        
        // 清空消息区域
        state.uiController.clearMessages();
        
        // 生成标准会话ID格式
        const standardConversationId = `${conversation.type}_${conversation.targetId}`;
        console.log('标准会话ID:', standardConversationId, '原始会话ID:', conversation.id);
        
        // 确保会话使用标准ID格式
        if (conversation.id !== standardConversationId) {
            console.log('会话ID与标准格式不一致，更新为标准格式');
            conversation.id = standardConversationId;
        }
        
        // 当前时间戳，用于更新会话顺序
        const currentTimestamp = Date.now();
        console.log('当前时间戳:', currentTimestamp);
        
        // 确保会话有最新的时间戳
        if (!conversation.lastMessage) {
            console.log('会话缺少 lastMessage，创建并设置时间戳');
            conversation.lastMessage = {
                content: '开始聊天',
                timestamp: currentTimestamp
            };
        } else {
            console.log('更新会话时间戳，旧值:', conversation.lastMessage.timestamp);
            conversation.lastMessage.timestamp = currentTimestamp;
        }
        
        // 获取会话历史消息
        if (state.imClient.messageCache) {
            console.log('获取会话前的缓存信息:', 
                Array.from(state.imClient.messageCache.keys()).map(id => ({
                    id,
                    count: state.imClient.messageCache.get(id)?.length || 0
                }))
            );
        }
        
        const messages = state.imClient.getMessageList(conversation.type, conversation.targetId);
        
        console.log('从缓存获取会话消息数量:', messages ? messages.length : 0, 
                  '会话ID:', standardConversationId,
                  '详细消息:', messages ? messages.map(m => ({
                      id: m.id,
                      fromId: m.fromId,
                      content: m.content.substring(0, 10) + (m.content.length > 10 ? '...' : ''),
                      status: m.status
                  })) : 'None');
        
        // 加载消息到UI
        if (messages && messages.length > 0) {
            console.log('加载缓存消息到UI，消息状态:', messages.map(m => ({id: m.id, status: m.status})));
            
            messages.forEach(message => {
                // 确保消息状态属性正确 - 只有在消息没有状态时才设置默认状态
                if (!message.status) {
                    // 根据消息方向设置默认状态
                    if (message.fromId === state.user.userId) {
                        message.status = 'sent'; // 默认状态为"已发送"
                        console.log(`设置消息(${message.id})默认状态为: sent`);
                    } else {
                        message.status = 'received';
                        console.log(`设置消息(${message.id})默认状态为: received`);
                    }
                } else {
                    // 记录日志，有助于调试状态问题
                    console.log(`保留消息(${message.id})现有状态: ${message.status}`);
                }
                
                // 创建深拷贝，确保不会修改原始缓存对象
                const messageCopy = JSON.parse(JSON.stringify(message));
                
                // 添加到UI
                state.uiController.addMessage(messageCopy);
            });
        }
        
        // 滚动到底部
        state.uiController.scrollToBottom();
        
        // 清除未读计数
        state.imClient.clearUnreadCount(conversation.type, conversation.targetId);
        
        // 确保当前会话存在于state.conversations中，统一会话ID格式
        if (!state.conversations[standardConversationId]) {
            console.log('将当前会话添加到state.conversations:', standardConversationId);
            
            // 创建新会话对象
            state.conversations[standardConversationId] = {
                id: standardConversationId,
                type: conversation.type,
                targetId: conversation.targetId,
                name: conversation.name || conversation.targetId,
                avatar: conversation.avatar || (conversation.type === 'GROUP' ? 
                    getAvatarForGroup(conversation.targetId) : 
                    getAvatarForUser(conversation.targetId)),
                unreadCount: 0,
                lastMessage: {
                    content: conversation.lastMessage?.content || '开始聊天',
                    timestamp: currentTimestamp // 使用当前时间戳
                }
            };
        } else {
            // 已存在则更新未读计数
            state.conversations[standardConversationId].unreadCount = 0;
            
            // 确保使用最新的会话名称和头像
            if (conversation.name) {
                state.conversations[standardConversationId].name = conversation.name;
            }
            if (conversation.avatar) {
                state.conversations[standardConversationId].avatar = conversation.avatar;
            }
            
            // 更新时间戳，确保会话排序在顶部
            if (state.conversations[standardConversationId].lastMessage) {
                // 总是使用当前时间戳，确保点击的会话排在最前面
                state.conversations[standardConversationId].lastMessage.timestamp = currentTimestamp;
                console.log('已更新现有会话的时间戳:', currentTimestamp);
            } else {
                state.conversations[standardConversationId].lastMessage = {
                    content: '开始聊天',
                    timestamp: currentTimestamp
                };
                console.log('为现有会话添加lastMessage和时间戳:', currentTimestamp);
            }
        }
        
        // 同步会话到IMClient
        // 这是解决问题的关键 - 确保IMClient的conversations中也有这个会话
        const imClientConversation = {
            type: conversation.type,
            targetId: conversation.targetId,
            lastMessage: {
                content: state.conversations[standardConversationId].lastMessage.content,
                timestamp: currentTimestamp // 使用当前时间戳
            }
        };
        state.imClient._updateConversation(imClientConversation);
        
        // 查找并移除所有可能的重复会话
        // 策略：保留标准ID的会话，删除所有其他可能是同一会话的条目
        const allKeys = Object.keys(state.conversations);
        const targetId = conversation.targetId;
        
        // 查找可能的重复会话ID - 任何包含相同targetId的会话，除了标准ID
        const duplicateIds = allKeys.filter(id => {
            if (id === standardConversationId) return false; // 排除标准ID
            
            // 检查是否包含相同的targetId
            const conv = state.conversations[id];
            return conv && conv.targetId === targetId;
        });
        
        // 删除所有重复会话
        if (duplicateIds.length > 0) {
            console.log('发现并删除重复会话:', duplicateIds);
            duplicateIds.forEach(id => delete state.conversations[id]);
        }
        
        // 检查会话列表中当前会话的位置
        const conversationValues = Object.values(state.conversations);
        
        // 排序会话列表
        conversationValues.sort((a, b) => {
            const timeA = a.lastMessage && a.lastMessage.timestamp ? a.lastMessage.timestamp : 0;
            const timeB = b.lastMessage && b.lastMessage.timestamp ? b.lastMessage.timestamp : 0;
            return timeB - timeA; // 降序排列，最新的在前面
        });
        
        // 找到当前会话的位置
        const currentConvIndex = conversationValues.findIndex(
            conv => conv.id === standardConversationId
        );
        
        console.log('排序后当前会话在列表中的位置:', currentConvIndex);
        
        // 强制将当前会话移到首位（如果不在首位）
        if (currentConvIndex > 0) {
            console.log('当前会话不在首位，强制置顶');
            const currentConv = conversationValues.splice(currentConvIndex, 1)[0];
            conversationValues.unshift(currentConv);
        }
        
        // 更新UI展示所有会话
        state.uiController.updateConversationsList(conversationValues);
    }
    
    /**
     * 处理私聊消息
     * @param {Object} message 消息对象
     */
    function handleChatMessage(message) {
        console.log('收到私聊消息:', message);
        
        // 添加消息到UI
        state.uiController.addMessage(message);
        
        // 直接更新state.conversations中的相关会话
        updateConversationWithMessage(message, 'C2C');
        
        // 更新UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
    }
    
    /**
     * 处理群聊消息
     * @param {Object} message 消息对象
     */
    function handleGroupMessage(message) {
        console.log('收到群聊消息:', message);
        
        // 检查消息格式
        if (!message.groupId) {
            console.error('群聊消息缺少groupId字段:', message);
            // 尝试修复消息格式
            if (message.toId && message.type === 'group') {
                console.log('尝试使用toId作为groupId');
                message.groupId = message.toId;
            } else {
                console.error('无法修复群聊消息格式，跳过处理');
                return;
            }
        }
        
        console.log('处理后的群聊消息:', message);
        
        // 添加消息到UI
        state.uiController.addMessage(message);
        
        // 直接更新state.conversations中的相关会话
        updateConversationWithMessage(message, 'GROUP');
        
        // 更新UI
        state.uiController.updateConversationsList(Object.values(state.conversations));
    }
    
    /**
     * 根据消息更新会话
     * @param {Object} message 消息对象
     * @param {string} type 会话类型 (C2C或GROUP)
     */
    function updateConversationWithMessage(message, type) {
        console.log('更新会话:', message, type);
        
        // 确定目标ID
        let targetId;
        if (type === 'C2C') {
            // 私聊消息，确定对话的另一方ID
            targetId = message.fromId === state.user.userId ? message.toId : message.fromId;
        } else {
            // 群聊消息，使用群组ID
            if (message.groupId) {
                targetId = message.groupId;
            } else if (message.toId && message.type === 'group') {
                // 备选方案：如果没有groupId但有toId且类型为group
                targetId = message.toId;
                // 同时修复消息对象
                message.groupId = message.toId;
            } else {
                console.error('群聊消息缺少有效的群组ID:', message);
                return; // 无法确定目标ID，退出
            }
        }
        
        console.log('确定的目标ID:', targetId);
        
        // 生成标准会话ID格式
        const standardConversationId = `${type}_${targetId}`;
        console.log('生成的标准会话ID:', standardConversationId);
        
        // 当前时间戳，用于更新会话顺序
        const currentTimestamp = message.timestamp || Date.now();
        
        // 检查会话是否已存在
        if (state.conversations[standardConversationId]) {
            console.log('更新现有会话:', standardConversationId);
            // 更新现有会话的最后消息
            state.conversations[standardConversationId].lastMessage = {
                content: message.content,
                timestamp: currentTimestamp
            };
            
            // 如果消息不是自己发的，增加未读计数
            if (message.fromId !== state.user.userId) {
                state.conversations[standardConversationId].unreadCount = 
                    (state.conversations[standardConversationId].unreadCount || 0) + 1;
            }
        } else {
            console.log('创建新会话:', standardConversationId);
            // 创建新会话
            let name, avatar;
            if (type === 'C2C') {
                const contact = state.contacts[targetId];
                name = contact ? contact.name : targetId;
                avatar = contact ? contact.avatar : getAvatarForUser(targetId);
            } else {
                const group = state.groups[targetId];
                name = group ? group.name : `群组 ${targetId}`;
                avatar = group ? group.avatar : getAvatarForGroup(targetId);
            }
            
            // 添加新会话，使用标准ID格式
            state.conversations[standardConversationId] = {
                id: standardConversationId,  // 使用标准格式的ID
                type: type,
                targetId: targetId,
                name: name,
                avatar: avatar,
                unreadCount: message.fromId !== state.user.userId ? 1 : 0,
                lastMessage: {
                    content: message.content,
                    timestamp: currentTimestamp
                }
            };
        }
        
        // 查找并移除所有可能的重复会话
        // 策略：保留标准ID的会话，删除所有其他可能是同一会话的条目
        const allKeys = Object.keys(state.conversations);
        
        // 查找可能的重复会话ID - 任何包含相同targetId的会话，除了标准ID
        const duplicateIds = allKeys.filter(id => {
            if (id === standardConversationId) return false; // 排除标准ID
            
            // 检查是否包含相同的targetId
            const conv = state.conversations[id];
            if (!conv) return false; // 跳过无效会话
            
            // 确定是否为重复会话 - 有相同的targetId或会话ID格式与当前标准ID类似
            return (conv.targetId === targetId) || 
                   (id.includes(targetId) && id.includes(type));
        });
        
        // 删除所有重复会话
        if (duplicateIds.length > 0) {
            console.log('发现并删除重复会话:', duplicateIds);
            duplicateIds.forEach(id => delete state.conversations[id]);
        }
        
        console.log(`会话${standardConversationId}已更新`, state.conversations[standardConversationId]);
        
        // 确保消息被添加到IMClient的缓存中
        if (state.imClient) {
            // 确保有正确的状态 - 只在没有状态时设置默认值，避免修改已有的状态值
            if (!message.status) {
                if (message.fromId === state.user.userId) {
                    message.status = 'sent';
                } else {
                    message.status = 'received';
                }
                console.log(`为消息(${message.id})设置默认状态: ${message.status}`);
            } else {
                console.log(`保留消息(${message.id})现有状态: ${message.status}`);
            }
            
            // 添加到IMClient的消息缓存
            state.imClient._addToMessageCache(message);
            
            // 同步会话到IMClient
            state.imClient._updateConversation({
                type: type,
                targetId: targetId,
                lastMessage: {
                    content: message.content,
                    timestamp: currentTimestamp
                }
            });
        }
    }
    
    /**
     * 处理系统消息
     * @param {Object} message 系统消息
     */
    function handleSystemMessage(message) {
        console.log('收到系统消息:', message);
    }
    
    /**
     * 处理消息确认回执
     * @param {Object} ack 确认回执
     */
    function handleMessageAck(ack) {
        console.log('收到消息确认回执:', ack);
        
        // 更新消息状态为已发送
        if (ack.messageId) {
            // 更新UI显示
            state.uiController.updateMessageStatus(ack.messageId, 'sent');
            
            // 更新IMClient缓存中的消息状态
            // 首先寻找消息所在的会话
            let messageFound = false;
            
            if (state.imClient && state.imClient.messageCache) {
                // 遍历所有会话
                for (const [conversationId, messages] of state.imClient.messageCache.entries()) {
                    if (!messages || !Array.isArray(messages)) continue;
                    
                    // 查找消息
                    const message = messages.find(m => m.id === ack.messageId);
                    if (message) {
                        message.status = 'sent';
                        console.log(`更新IMClient缓存中消息(${ack.messageId})的状态为: sent`);
                        messageFound = true;
                        break;
                    }
                }
                
                if (!messageFound) {
                    console.log(`未找到消息(${ack.messageId})，无法更新状态`);
                }
            }
        }
        
        // 如果是多端同步消息，需要添加到当前会话
        if (ack.isSync && ack.content) {
            console.log('收到多端同步消息:', ack);
            
            // 将其转化为适合UI显示的消息格式
            const syncMessage = {
                id: ack.messageId,
                type: 'chat',  // 目前只处理单聊消息同步
                fromId: ack.fromId,
                toId: ack.toId,
                content: ack.content,
                timestamp: ack.timestamp,
                status: 'sent',
                isSelf: true  // 从其他设备同步过来的自己发送的消息
            };
            
            // 添加消息到UI
            state.uiController.addMessage(syncMessage);
            
            // 添加到IMClient缓存
            if (state.imClient) {
                state.imClient._addToMessageCache(syncMessage);
            }
            
            // 更新会话列表
            if (ack.fromId && ack.toId) {
                // 确定会话类型和目标ID
                const conversationType = 'C2C';
                const targetId = ack.fromId === state.user.userId ? ack.toId : ack.fromId;
                
                // 生成标准会话ID
                const conversationId = `${conversationType}_${targetId}`;
                
                // 检查会话是否存在
                if (state.conversations[conversationId]) {
                    // 更新现有会话的最后消息
                    state.conversations[conversationId].lastMessage = {
                        content: ack.content,
                        timestamp: ack.timestamp
                    };
                    
                    // 更新UI
                    state.uiController.updateConversationsList(Object.values(state.conversations));
                }
            }
        }
    }
    
    /**
     * 处理已读回执
     * @param {Object} receipt 已读回执
     */
    function handleReadReceipt(receipt) {
        console.log('收到已读回执:', receipt);
        
        // 如果包含messageId则直接更新消息状态
        if (receipt.messageId) {
            // 更新UI显示
            state.uiController.updateMessageStatus(receipt.messageId, 'read');
            
            // 更新IMClient缓存中的消息状态
            if (state.imClient && state.imClient.messageCache) {
                // 遍历所有会话
                for (const [conversationId, messages] of state.imClient.messageCache.entries()) {
                    if (!messages || !Array.isArray(messages)) continue;
                    
                    // 查找消息
                    const message = messages.find(m => m.id === receipt.messageId);
                    if (message) {
                        message.status = 'read';
                        console.log(`更新IMClient缓存中消息(${receipt.messageId})的状态为: read`);
                        break;
                    }
                }
            }
            return;
        }
        
        // 否则查找对应sequence的消息并更新状态
        if (receipt.sequence) {
            // 获取当前会话的消息列表
            const currentConversation = state.uiController.getCurrentConversation();
            if (!currentConversation) return;
            
            // 检查是否是当前正在查看的会话
            let conversationType, targetId;
            if (receipt.type === 'chat_receipt') {
                conversationType = 'C2C';
                // 接收到的已读回执中，toId是原始发送方（当前用户），fromId是阅读方
                targetId = receipt.fromId;
            } else if (receipt.type === 'group_receipt') {
                conversationType = 'GROUP';
                targetId = receipt.groupId;
            } else {
                return;
            }
            
            // 只有当已读回执与当前正在查看的会话匹配时才更新UI
            if (currentConversation.type === conversationType && 
                currentConversation.targetId === targetId) {
                
                console.log(`将${conversationType}会话中sequence <= ${receipt.sequence}的消息标记为已读`);
                
                // 从IMClient中获取消息列表
                const conversationId = `${conversationType}_${targetId}`;
                const messages = state.imClient.messageCache.get(conversationId);
                
                if (messages && messages.length > 0) {
                    // 找到所有sequence小于等于指定值且由当前用户发送的消息
                    const messagesToUpdate = messages.filter(msg => 
                        msg.sequence <= receipt.sequence && 
                        msg.fromId === state.user.userId &&
                        msg.status !== 'read' // 只更新还未标记为已读的消息
                    );
                    
                    if (messagesToUpdate.length > 0) {
                        console.log(`找到${messagesToUpdate.length}条消息需要更新为已读状态`);
                        
                        // 更新每条消息的状态
                        messagesToUpdate.forEach(msg => {
                            // 更新缓存中的消息状态
                            msg.status = 'read';
                            
                            // 更新UI显示
                            if (msg.id) {
                                state.uiController.updateMessageStatus(msg.id, 'read');
                            }
                        });
                        
                        // 显示提示动画
                        showReadStatusAnimation();
                    }
                }
                
                // 也可以从DOM中查找消息元素并更新
                const messageElements = document.querySelectorAll('.message.message-self');
                if (messageElements.length > 0) {
                    let updatedCount = 0;
                    messageElements.forEach(el => {
                        const msgId = el.dataset.id;
                        if (msgId) {
                            // 由于没有简单的方法获取消息的sequence，这里暂时先更新所有消息
                            // 实际实现应该根据消息ID查找对应的sequence进行比较
                            const statusEl = el.querySelector('.message-status');
                            if (statusEl && statusEl.textContent !== '已读') {
                                state.uiController.updateMessageStatus(msgId, 'read');
                                updatedCount++;
                                
                                // 同时更新IMClient缓存
                                if (state.imClient && state.imClient.messageCache) {
                                    for (const [convId, msgs] of state.imClient.messageCache.entries()) {
                                        if (!msgs || !Array.isArray(msgs)) continue;
                                        
                                        const msg = msgs.find(m => m.id === msgId);
                                        if (msg) {
                                            msg.status = 'read';
                                            console.log(`更新DOM中找到的消息(${msgId})缓存状态为: read`);
                                        }
                                    }
                                }
                            }
                        }
                    });
                    
                    if (updatedCount > 0) {
                        // 显示提示动画
                        showReadStatusAnimation();
                    }
                }
            }
        }
    }
    
    /**
     * 显示已读状态变更的动画效果
     */
    function showReadStatusAnimation() {
        // 创建一个临时元素显示已读动画
        const animEl = document.createElement('div');
        animEl.className = 'read-status-animation';
        animEl.textContent = '对方已读了你的消息';
        animEl.style.cssText = `
            position: fixed;
            bottom: 30%;
            left: 50%;
            transform: translateX(-50%);
            background-color: rgba(0, 0, 0, 0.7);
            color: white;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 14px;
            opacity: 0;
            transition: opacity 0.3s;
            z-index: 1000;
        `;
        
        document.body.appendChild(animEl);
        
        // 显示动画
        setTimeout(() => {
            animEl.style.opacity = '1';
        }, 100);
        
        // 3秒后移除
        setTimeout(() => {
            animEl.style.opacity = '0';
            setTimeout(() => {
                document.body.removeChild(animEl);
            }, 300);
        }, 3000);
    }
    
    /**
     * 处理好友请求
     * @param {Object} request 好友请求
     */
    function handleFriendRequest(request) {
        console.log('收到好友请求:', request);
        
        // 这里可以添加通知或提醒
        alert(`收到来自 ${request.fromId} 的好友请求: ${request.addWording}`);
    }
    
    /**
     * 处理好友状态变更
     * @param {Object} statusChange 状态变更
     */
    function handleFriendStatusChange(statusChange) {
        console.log('好友状态变更:', statusChange);
        
        // 刷新好友列表
        loadUserData();
    }
    
    /**
     * 处理群组状态变更
     * @param {Object} statusChange 状态变更
     */
    function handleGroupStatusChange(statusChange) {
        console.log('群组状态变更:', statusChange);
        
        // 刷新群组列表
        loadUserData();
    }
    
    /**
     * 处理创建群组
     * @param {Object} groupData 群组数据
     */
    async function handleCreateGroup(groupData) {
        try {
            console.log('创建群组:', groupData);
            
            // 确保群组数据包含必要字段
            if (!groupData.groupName) {
                throw new Error('群组名称不能为空');
            }
            
            // 将memberIds转换为GroupMemberDto对象集合
            const members = [];
            if (groupData.memberIds && Array.isArray(groupData.memberIds)) {
                groupData.memberIds.forEach(memberId => {
                    // 创建符合后端需要的GroupMemberDto对象
                    members.push({
                        memberId: memberId,
                        alias: "", // 可选别名
                        role: 0,   // 普通成员
                        joinType: "invite", // 邀请加入
                        joinTime: Date.now() // 当前时间戳
                    });
                });
            }
            
            // 构建API调用数据
            const apiGroupData = {
                groupName: groupData.groupName,
                introduction: groupData.introduction || '',
                member: members // 使用正确的字段名和结构
            };
            
            // 添加日志记录
            console.log('调用创建群组API:', apiGroupData);
            
            const result = await state.imClient.createGroup(apiGroupData);
            
            if (result.isOk()) {
                console.log('创建群组成功:', result);
                
                // 刷新群组列表
                const groups = await state.imClient.getGroupList();
                if (groups && Array.isArray(groups)) {
                    processGroupList(groups);
                }

                // 同步加入的群组列表
                try {
                    console.log('同步加入的群组列表');
                    const syncGroupResult = await state.imClient.apiClient.syncJoinedGroupList(state.user.userId);
                    console.log('同步加入的群组列表结果:', syncGroupResult);
                } catch (syncError) {
                    console.error('同步加入的群组列表失败:', syncError);
                }

                // 同步会话列表
                try {
                    console.log('同步会话列表');
                    const syncConvResult = await state.imClient.apiClient.syncConversationList(state.user.userId);
                    console.log('同步会话列表结果:', syncConvResult);
                    
                    // 处理会话列表数据
                    if (syncConvResult) {
                        processConversationList(syncConvResult, false); // 不清空现有会话列表
                    }
                } catch (syncError) {
                    console.error('同步会话列表失败:', syncError);
                }
                
                // 显示成功提示
                alert('创建群组成功');
                
                // 切换到已加入标签
                const joinedTab = document.querySelector('#groups-page .tab[data-tab="joined"]');
                if (joinedTab) {
                    joinedTab.click();
                }
            } else {
                console.error('创建群组失败');
                alert('创建群组失败');
            }
        } catch (error) {
            console.error('创建群组出错:', error);
            alert(`创建群组出错: ${error.message}`);
        }
    }
    
    /**
     * 处理添加好友
     * @param {Object} friendData 好友数据
     */
    async function handleAddFriend(friendData) {
        try {
            console.log('添加好友:', friendData);
            
            // 更安全的检查用户是否已登录
            if (!state.imClient) {
                throw new Error('IM客户端未初始化');
            }
            
            // 检查WebSocket客户端是否已初始化，如果没有则尝试重新连接
            if (!state.imClient.wsClient) {
                console.warn('WebSocket客户端未初始化，尝试重新连接...');
                
                // 重新创建IM客户端
                createIMClient();
                
                // 尝试连接
                try {
                    await state.imClient.connect();
                    console.log('WebSocket重新连接成功');
                    
                    // 重新登录
                    const loginResult = await state.imClient.login(state.user);
                    if (!loginResult.success) {
                        throw new Error('重新登录失败: ' + (loginResult.error || '未知错误'));
                    }
                    console.log('重新登录成功');
                } catch (reconnectError) {
                    console.error('WebSocket重新连接失败:', reconnectError);
                    throw new Error('WebSocket重新连接失败: ' + reconnectError.message);
                }
            }
            
            const userInfo = state.imClient.wsClient.getUserInfo && state.imClient.wsClient.getUserInfo();
            if (!userInfo || !userInfo.userId) {
                throw new Error('用户未登录或登录状态异常');
            }
            
            // 检查必要参数
            if (!friendData.toId) {
                throw new Error('缺少必要参数：toId');
            }
            
            const result = await state.imClient.addFriend(friendData);
            
            console.log('添加好友结果:', result);
            
            // 简化判断:
            // 1. result存在且result.success为true，是明确的成功
            // 2. 否则，只要没有抛出异常，都视为成功（这里依赖IMClient做了正确处理）
            if (result && result.success === true) {
                console.log('添加好友明确成功:', result);
            } else {
                console.log('添加好友可能成功 (没有明确成功标志但也没有抛出异常)');
            }
            
            // 刷新好友列表
            try {
                const friends = await state.imClient.getFriendList();
                if (friends && Array.isArray(friends)) {
                    processFriendList(friends);
                }
            } catch (refreshError) {
                console.warn('刷新好友列表失败:', refreshError);
            }

            // 同步好友关系列表
            try {
                console.log('同步好友关系列表');
                const syncFriendshipResult = await state.imClient.apiClient.syncFriendshipList(state.user.userId);
                console.log('同步好友关系列表结果:', syncFriendshipResult);
            } catch (syncError) {
                console.error('同步好友关系列表失败:', syncError);
            }

            // 同步会话列表
            try {
                console.log('同步会话列表');
                const syncConvResult = await state.imClient.apiClient.syncConversationList(state.user.userId);
                console.log('同步会话列表结果:', syncConvResult);
                
                // 处理会话列表数据
                if (syncConvResult) {
                    processConversationList(syncConvResult, false); // 不清空现有会话列表
                }
            } catch (syncError) {
                console.error('同步会话列表失败:', syncError);
            }
            
            // 显示成功提示
            alert(result && result.message ? result.message : '添加好友请求已发送');
        } catch (error) {
            console.error('添加好友出错:', error);
            
            // 更安全的错误详情日志
            const errorDetails = {
                message: error.message,
                stack: error.stack,
                friendData: JSON.stringify(friendData)
            };
            
            // 只有在安全的情况下才尝试获取用户信息
            if (state.imClient && state.imClient.wsClient && typeof state.imClient.wsClient.getUserInfo === 'function') {
                try {
                    errorDetails.userInfo = JSON.stringify(state.imClient.wsClient.getUserInfo());
                } catch (e) {
                    errorDetails.userInfoError = e.message;
                }
            } else {
                errorDetails.userInfo = '无法获取用户信息：客户端未初始化或不完整';
            }
            
            console.error('错误详情:', errorDetails);
            alert(`添加好友出错: ${error.message}`);
        }
    }
    
    // 在UI控制器中注册自动跳转设置变更回调
    function handleAutoSwitchChange(autoSwitch) {
        console.log('聊天自动跳转设置已更改:', autoSwitch);
        saveUISettings({ autoSwitchToChat: autoSwitch });
    }
    
    // 初始化应用
    document.addEventListener('DOMContentLoaded', init);
})(); 