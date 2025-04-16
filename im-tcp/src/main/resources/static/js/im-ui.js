/**
 * IM UI控制器模块
 * 负责处理UI相关的逻辑和交互
 */

/**
 * UI控制器类
 */
class UIController {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     */
    constructor(options = {}) {
        this.options = Object.assign({
            elements: {},
            onConversationSelect: () => {},
            onSendMessage: () => {},
            onThemeChange: () => {},
            onStyleChange: () => {},
            onFontSizeChange: () => {},
            onCreateGroup: () => {},
            onAddFriend: () => {},
            autoSwitchToChat: false, // 新增：点击联系人或群组后是否自动切换到聊天页面
            onAutoSwitchChange: () => {} // 新增：聊天自动跳转设置变更的回调
        }, options);
        
        this.elements = this.options.elements;
        this.currentConversation = null;
        this.messageMap = new Map(); // 用于存储消息ID到DOM元素的映射
        this.userId = null;
        this._setupEventListeners();
    }
    
    /**
     * 设置事件监听器
     * @private
     */
    _setupEventListeners() {
        // 模态框背景点击事件
        const modalBackdrop = document.getElementById('modal-backdrop');
        if (modalBackdrop) {
            modalBackdrop.addEventListener('click', (e) => {
                if (e.target === modalBackdrop) {
                    modalBackdrop.classList.add('hidden');
                    document.getElementById('modal-container').classList.add('hidden');
                }
            });
        }
        
        // 侧边栏导航图标的点击事件
        if (this.elements.sidebar && this.elements.sidebar.sidebarIcons) {
            this.elements.sidebar.sidebarIcons.forEach(icon => {
                icon.addEventListener('click', () => {
                    const page = icon.getAttribute('data-page');
                    if (page) {
                        this._switchPage(page);
                    }
                });
            });
        }
        
        // 新聊天按钮点击事件
        const newChatBtn = document.getElementById('new-chat-btn');
        if (newChatBtn) {
            console.log('找到新聊天按钮，添加点击事件');
            
            // 移除可能存在的旧事件处理程序
            const newHandler = () => {
                console.log('新聊天按钮被点击');
                this._showAddFriendModal();
                return false;
            };
            
            // 使用捕获和冒泡阶段的监听器，确保点击事件被处理
            newChatBtn.addEventListener('click', newHandler, true);
            newChatBtn.addEventListener('click', newHandler);
            
            // 为了确保事件触发，添加鼠标按下事件
            newChatBtn.addEventListener('mousedown', () => {
                console.log('新聊天按钮鼠标按下');
            });
        } else {
            console.warn('未找到新聊天按钮');
        }
        
        // 发送消息按钮点击事件
        if (this.elements.chat && this.elements.chat.sendBtn) {
            this.elements.chat.sendBtn.addEventListener('click', () => {
                const messageInput = this.elements.chat.messageInput;
                if (messageInput && messageInput.value.trim()) {
                    this._handleSendMessage(messageInput.value.trim());
                    messageInput.value = '';
                    messageInput.focus();
                }
            });
        }
        
        // 消息输入框键盘事件
        if (this.elements.chat && this.elements.chat.messageInput) {
            this.elements.chat.messageInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    const messageInput = this.elements.chat.messageInput;
                    if (messageInput.value.trim()) {
                        this._handleSendMessage(messageInput.value.trim());
                        messageInput.value = '';
                    }
                }
            });
            
            // 输入时启用/禁用发送按钮
            this.elements.chat.messageInput.addEventListener('input', () => {
                if (this.elements.chat.sendBtn) {
                    this.elements.chat.sendBtn.disabled = !this.elements.chat.messageInput.value.trim();
                }
            });
        }
        
        // 群组页面的标签和创建群组表单
        const groupsPage = document.getElementById('groups-page');
        if (groupsPage) {
            // 标签切换事件
            const tabs = groupsPage.querySelectorAll('.tab');
            tabs.forEach(tab => {
                tab.addEventListener('click', () => {
                    const tabName = tab.getAttribute('data-tab');
                    
                    // 更新标签激活状态
                    tabs.forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    
                    // 处理特殊标签
                    const groupsList = document.getElementById('groups-list');
                    const createGroupForm = document.getElementById('create-group-form');
                    
                    if (tabName === 'create') {
                        // 显示创建群组表单
                        if (groupsList) groupsList.classList.add('hidden');
                        if (createGroupForm) createGroupForm.classList.remove('hidden');
                    } else {
                        // 显示群组列表
                        if (groupsList) groupsList.classList.remove('hidden');
                        if (createGroupForm) createGroupForm.classList.add('hidden');
                    }
                });
            });
            
            // 创建群组按钮
            const createGroupBtn = document.getElementById('create-group-btn');
            if (createGroupBtn) {
                createGroupBtn.addEventListener('click', () => {
                    const groupName = document.getElementById('group-name');
                    const groupIntro = document.getElementById('group-intro');
                    
                    if (groupName && groupName.value.trim()) {
                        this._handleCreateGroup({
                            groupName: groupName.value.trim(),
                            introduction: groupIntro ? groupIntro.value.trim() : ''
                        });
                        
                        // 清空表单
                        groupName.value = '';
                        if (groupIntro) groupIntro.value = '';
                    } else {
                        alert('请输入群组名称');
                    }
                });
            }
        }
        
        // 联系人页面的标签
        const contactsPage = document.getElementById('contacts-page');
        if (contactsPage) {
            const tabs = contactsPage.querySelectorAll('.tab');
            tabs.forEach(tab => {
                tab.addEventListener('click', () => {
                    // 更新标签激活状态
                    tabs.forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    
                    // 这里可以添加切换联系人分组的逻辑
                });
            });
        }
        
        // 设置页面的按钮和控件
        if (this.elements.settings) {
            // 主题切换
            if (this.elements.settings.themeToggle) {
                this.elements.settings.themeToggle.addEventListener('change', () => {
                    const theme = this.elements.settings.themeToggle.checked ? 'dark' : 'light';
                    if (this.options.onThemeChange) {
                        this.options.onThemeChange(theme);
                    }
                });
            }
            
            // UI风格切换
            if (this.elements.settings.uiStyleToggle) {
                this.elements.settings.uiStyleToggle.addEventListener('change', () => {
                    const style = this.elements.settings.uiStyleToggle.checked ? 'modern' : 'classic';
                    if (this.options.onStyleChange) {
                        this.options.onStyleChange(style);
                    }
                });
            }
            
            // 字体大小调整
            if (this.elements.settings.fontSizeRange) {
                this.elements.settings.fontSizeRange.addEventListener('input', () => {
                    const fontSize = this.elements.settings.fontSizeRange.value;
                    if (this.options.onFontSizeChange) {
                        this.options.onFontSizeChange(fontSize);
                    }
                });
            }
            
            // 聊天自动跳转设置
            const autoSwitchToggle = document.getElementById('auto-switch-toggle');
            if (autoSwitchToggle) {
                // 初始化状态
                autoSwitchToggle.checked = this.options.autoSwitchToChat;
                
                // 添加变更监听
                autoSwitchToggle.addEventListener('change', () => {
                    this.options.autoSwitchToChat = autoSwitchToggle.checked;
                    console.log('聊天自动跳转设置已更改:', this.options.autoSwitchToChat);
                    
                    // 执行回调函数
                    if (this.options.onAutoSwitchChange) {
                        this.options.onAutoSwitchChange(this.options.autoSwitchToChat);
                    }
                });
            }
            
            // 退出登录按钮
            if (this.elements.settings.logoutBtn) {
                this.elements.settings.logoutBtn.addEventListener('click', () => {
                    this._handleLogout();
                });
            }
        }
    }
    
    /**
     * 切换页面
     * @param {string} page 页面名称
     * @private
     */
    _switchPage(page) {
        // 更新侧边栏图标的激活状态
        if (this.elements.sidebar && this.elements.sidebar.sidebarIcons) {
            this.elements.sidebar.sidebarIcons.forEach(icon => {
                if (icon.getAttribute('data-page') === page) {
                    icon.classList.add('active');
                } else {
                    icon.classList.remove('active');
                }
            });
        }
        
        // 更新页面内容的显示状态
        const pageContents = document.querySelectorAll('.page-content');
        if (pageContents && pageContents.length > 0) {
            pageContents.forEach(content => {
                if (content.id === `${page}-page`) {
                    content.classList.add('active');
                } else {
                    content.classList.remove('active');
                }
            });
        }
    }
    
    /**
     * 处理发送消息
     * @param {string} content 消息内容
     * @private
     */
    _handleSendMessage(content) {
        if (this.currentConversation && this.options.onSendMessage) {
            this.options.onSendMessage({
                conversationType: this.currentConversation.type,
                targetId: this.currentConversation.targetId,
                content: content
            });
        }
    }
    
    /**
     * 设置用户信息
     * @param {Object} userInfo 用户信息
     */
    setUserInfo(userInfo) {
        this.userId = userInfo.userId;
        if (this.elements.sidebar) {
            if (this.elements.sidebar.currentAvatar) {
                this.elements.sidebar.currentAvatar.innerHTML = `
                    <div class="avatar-icon"><i class="fa fa-user"></i></div>
                `;
            }
            
            if (this.elements.sidebar.profileAvatar) {
                this.elements.sidebar.profileAvatar.innerHTML = `
                    <div class="avatar-icon"><i class="fa fa-user"></i></div>
                `;
            }
            
            if (this.elements.sidebar.profileName) {
                this.elements.sidebar.profileName.textContent = userInfo.userId;
            }
        }
        
        if (this.elements.settings) {
            if (this.elements.settings.userIdSpan) {
                this.elements.settings.userIdSpan.textContent = userInfo.userId;
            }
            
            if (this.elements.settings.clientTypeSpan) {
                this.elements.settings.clientTypeSpan.textContent = this._getClientTypeName(userInfo.clientType);
            }
            
            if (this.elements.settings.appIdSpan) {
                this.elements.settings.appIdSpan.textContent = userInfo.appId;
            }
        }
    }
    
    /**
     * 获取客户端类型名称
     * @param {number} clientType 客户端类型代码
     * @returns {string} 客户端类型名称
     * @private
     */
    _getClientTypeName(clientType) {
        const types = {
            1: 'Web',
            2: 'PC',
            3: 'Android',
            4: 'iOS',
            5: '其他'
        };
        return types[clientType] || '未知';
    }
    
    /**
     * 更新会话列表
     * @param {Array} conversations 会话列表
     */
    updateConversationsList(conversations) {
        console.log('UIController.updateConversationsList 被调用:', conversations);
        
        if (!this.elements.lists) {
            console.error('elements.lists不存在');
            return;
        }
        
        if (!this.elements.lists.conversationsList) {
            console.error('elements.lists.conversationsList不存在');
            return;
        }
        
        const conversationsList = this.elements.lists.conversationsList;
        
        try {
            if (!Array.isArray(conversations) || conversations.length === 0) {
                console.log('会话列表为空');
                // 清空列表
                conversationsList.innerHTML = '';
                return;
            }
            
            // 创建会话ID映射，用于之后检查会话是否存在
            const existingConversations = new Map();
            Array.from(conversationsList.querySelectorAll('.list-item')).forEach(element => {
                if (element.dataset && element.dataset.id) {
                    existingConversations.set(element.dataset.id, element);
                }
            });
            
            
            // 创建文档片段以提高性能
            const fragment = document.createDocumentFragment();
            // 跟踪已处理的会话ID
            const processedIds = new Set();
            
            // 排序会话列表，确保按时间顺序显示
            conversations.sort((a, b) => {
                const timeA = a.lastMessage && a.lastMessage.timestamp ? a.lastMessage.timestamp : 0;
                const timeB = b.lastMessage && b.lastMessage.timestamp ? b.lastMessage.timestamp : 0;
                return timeB - timeA; // 降序排列，最新的在前面
            });
            
            // 处理会话列表
            conversations.forEach(conversation => {
                try {
                    if (!conversation || !conversation.id) {
                        console.warn('会话缺少ID:', conversation);
                        return;
                    }
                    
                    processedIds.add(conversation.id);
                    
                    // 检查会话是否已存在
                    const existingElement = existingConversations.get(conversation.id);
                    
                    if (existingElement) {
                        // 会话已存在，更新内容
                        this._updateConversationItem(existingElement, conversation);
                    } else {
                        // 会话不存在，创建新的会话项
                        const conversationItem = this._createConversationItem(conversation);
                        if (conversationItem) {
                            fragment.appendChild(conversationItem);
                        }
                    }
                } catch (itemError) {
                    console.error('处理会话项时出错:', itemError, conversation);
                }
            });
            
            // 移除不在当前会话列表中的元素
            existingConversations.forEach((element, id) => {
                if (!processedIds.has(id)) {
                    element.remove();
                }
            });
            
            // 添加新创建的元素
            if (fragment.childNodes.length > 0) {
                console.log('添加新创建的会话项，数量:', fragment.childNodes.length);
                conversationsList.appendChild(fragment);
            }
        } catch (error) {
            console.error('更新会话列表时出错:', error);
            // 尝试使用最简单的方式添加项目
            try {
                conversationsList.innerHTML = '<div class="list-item">更新会话列表时出错</div>';
            } catch (fallbackError) {
                console.error('使用回退方法添加会话项也失败:', fallbackError);
            }
        }
    }
    
    /**
     * 更新会话项内容
     * @param {HTMLElement} element 会话项元素
     * @param {Object} conversation 会话对象
     * @private
     */
    _updateConversationItem(element, conversation) {
        try {
            // 判断会话类型
            const isGroup = conversation.type === 'GROUP';
            
            // 生成会话名称
            const name = conversation.name || (isGroup 
                ? `群组 ${conversation.targetId}` 
                : `用户 ${conversation.targetId}`);
            
            // 生成最近消息摘要
            let lastMessage = '无消息';
            if (conversation.lastMessage && conversation.lastMessage.content) {
                lastMessage = conversation.lastMessage.content.length > 30 
                    ? conversation.lastMessage.content.substring(0, 30) + '...' 
                    : conversation.lastMessage.content;
            }
            
            // 生成时间
            let timeText = '';
            if (conversation.lastMessage && conversation.lastMessage.timestamp) {
                timeText = this._formatTime(conversation.lastMessage.timestamp);
            }
            
            // 查找并更新相关元素
            const nameElement = element.querySelector('.item-name');
            const timeElement = element.querySelector('.item-time');
            const messageElement = element.querySelector('.item-message');
            const badgeElement = element.querySelector('.badge');
            
            if (nameElement) nameElement.textContent = name;
            if (timeElement) timeElement.textContent = timeText;
            if (messageElement) messageElement.textContent = lastMessage;
            
            // 更新未读消息数量
            if (conversation.unreadCount) {
                if (badgeElement) {
                    badgeElement.textContent = conversation.unreadCount;
                } else {
                    const subtitleElement = element.querySelector('.item-subtitle');
                    if (subtitleElement) {
                        const badge = document.createElement('span');
                        badge.className = 'badge';
                        badge.textContent = conversation.unreadCount;
                        subtitleElement.appendChild(badge);
                    }
                }
            } else if (badgeElement) {
                badgeElement.parentNode.removeChild(badgeElement);
            }
            
            // 如果是当前选中的会话，添加active类
            if (this.currentConversation && this.currentConversation.id === conversation.id) {
                element.classList.add('active');
            } else {
                element.classList.remove('active');
            }
        } catch (error) {
            console.error('更新会话项内容时出错:', error, conversation);
        }
    }
    
    /**
     * 创建会话项
     * @param {Object} conversation 会话对象
     * @returns {HTMLElement} 会话项DOM元素
     * @private
     */
    _createConversationItem(conversation) {
        try {
            console.log('创建会话项:', conversation);
            
            if (!conversation || typeof conversation !== 'object') {
                console.error('会话数据无效:', conversation);
                return null;
            }
            
            if (!conversation.id) {
                console.warn('会话缺少ID:', conversation);
                // 生成临时ID以避免错误
                conversation.id = 'temp_' + Math.random().toString(36).substring(2, 10);
            }
            
            const conversationEl = document.createElement('div');
            conversationEl.className = 'list-item';
            conversationEl.dataset.id = conversation.id;
            
            // 判断会话类型
            const isGroup = conversation.type === 'GROUP';
            
            // 生成会话名称
            const name = conversation.name || (isGroup 
                ? `群组 ${conversation.targetId}` 
                : `用户 ${conversation.targetId}`);
            
            // 生成最近消息摘要
            let lastMessage = '无消息';
            if (conversation.lastMessage && conversation.lastMessage.content) {
                lastMessage = conversation.lastMessage.content.length > 30 
                    ? conversation.lastMessage.content.substring(0, 30) + '...' 
                    : conversation.lastMessage.content;
            }
            
            // 生成时间
            let timeText = '';
            if (conversation.lastMessage && conversation.lastMessage.timestamp) {
                timeText = this._formatTime(conversation.lastMessage.timestamp);
            }
            
            // 构建HTML
            try {
                conversationEl.innerHTML = `
                    <div class="item-avatar">
                        <div class="avatar-icon">${isGroup ? '<i class="fa fa-users"></i>' : '<i class="fa fa-user"></i>'}</div>
                        ${isGroup ? '<div class="avatar-badge group"><i class="fa fa-users"></i></div>' : ''}
                    </div>
                    <div class="item-content">
                        <div class="item-title">
                            <span class="item-name">${name}</span>
                            <span class="item-time">${timeText}</span>
                        </div>
                        <div class="item-subtitle">
                            <span class="item-message">${lastMessage}</span>
                            ${conversation.unreadCount ? `<span class="badge">${conversation.unreadCount}</span>` : ''}
                        </div>
                    </div>
                `;
            } catch (htmlError) {
                console.error('设置会话项HTML时出错:', htmlError);
                conversationEl.textContent = name; // 简单的回退方案
            }
            
            // 添加点击事件
            try {
                conversationEl.addEventListener('click', () => {
                    this.selectConversation(conversation);
                });
            } catch (eventError) {
                console.error('添加会话项点击事件时出错:', eventError);
            }
            
            return conversationEl;
        } catch (error) {
            console.error('创建会话项时出错:', error, conversation);
            // 返回一个简单的错误项
            const errorEl = document.createElement('div');
            errorEl.className = 'list-item error';
            errorEl.textContent = '加载会话项出错';
            return errorEl;
        }
    }
    
    /**
     * 处理联系人点击
     * @param {Object} contact 联系人对象
     * @private
     */
    _handleContactClick(contact) {
        // 创建私聊会话
        const conversation = {
            id: `C2C_${contact.id}`,
            type: 'C2C',
            targetId: contact.id,
            name: contact.name || `用户 ${contact.id}`
        };
        
        this.selectConversation(conversation);
        
        // 根据配置决定是否自动切换到聊天页面
        if (this.options.autoSwitchToChat) {
            this._switchPage('chats');
        }
    }
    
    /**
     * 更新联系人列表
     * @param {Array} contacts 联系人列表
     */
    updateContactsList(contacts) {
        if (!this.elements.lists || !this.elements.lists.contactsList) {
            return;
        }
        
        this.elements.lists.contactsList.innerHTML = '';
        
        if (contacts.length === 0) {
            // 不再显示"暂无联系人"提示，保持联系人列表为空
            return;
        }
        
        contacts.forEach(contact => {
            const contactItem = this._createContactItem(contact);
            this.elements.lists.contactsList.appendChild(contactItem);
        });
    }
    
    /**
     * 创建联系人项
     * @param {Object} contact 联系人对象
     * @returns {HTMLElement} 联系人项DOM元素
     * @private
     */
    _createContactItem(contact) {
        const contactEl = document.createElement('div');
        contactEl.className = 'list-item';
        contactEl.dataset.id = contact.id;
        
        // 生成联系人名称
        const name = `用户 ${contact.id}` || contact.name || contact.remark;
        
        // 构建HTML
        contactEl.innerHTML = `
            <div class="item-avatar">
                <div class="avatar-icon"><i class="fa fa-user"></i></div>
            </div>
            <div class="item-content">
                <div class="item-title">
                    <span class="item-name">${name}</span>
                </div>
                <div class="item-subtitle">
                    <span class="item-message">ID: ${contact.id}</span>
                </div>
            </div>
        `;
        
        // 添加点击事件
        contactEl.addEventListener('click', () => {
            this._handleContactClick(contact);
        });
        
        return contactEl;
    }
    
    /**
     * 更新群组列表
     * @param {Array} groups 群组列表
     */
    updateGroupsList(groups) {
        if (!this.elements.lists || !this.elements.lists.groupsList) {
            return;
        }
        
        this.elements.lists.groupsList.innerHTML = '';
        
        if (groups.length === 0) {
            // 不再显示"暂无群组"提示，保持群组列表为空
            return;
        }
        
        groups.forEach(group => {
            const groupItem = this._createGroupItem(group);
            this.elements.lists.groupsList.appendChild(groupItem);
        });
    }
    
    /**
     * 创建群组项
     * @param {Object} group 群组对象
     * @returns {HTMLElement} 群组项DOM元素
     * @private
     */
    _createGroupItem(group) {
        const groupEl = document.createElement('div');
        groupEl.className = 'list-item';
        groupEl.dataset.id = group.id;
        
        // 生成群组名称
        const name = group.name || `群组 ${group.id}`;
        
        // 生成群组介绍
        const intro = group.introduction || '暂无介绍';
        
        // 构建HTML
        groupEl.innerHTML = `
            <div class="item-avatar">
                <div class="avatar-icon"><i class="fa fa-users"></i></div>
                <div class="avatar-badge group"><i class="fa fa-users"></i></div>
            </div>
            <div class="item-content">
                <div class="item-title">
                    <span class="item-name">${name}</span>
                </div>
                <div class="item-subtitle">
                    <span class="item-message">${intro.length > 30 ? intro.substring(0, 30) + '...' : intro}</span>
                </div>
            </div>
        `;
        
        // 添加点击事件
        groupEl.addEventListener('click', () => {
            this._handleGroupClick(group);
        });
        
        return groupEl;
    }
    
    /**
     * 处理群组点击
     * @param {Object} group 群组对象
     * @private
     */
    _handleGroupClick(group) {
        // 创建群聊会话
        const conversation = {
            id: `GROUP_${group.id}`,
            type: 'GROUP',
            targetId: group.id,
            name: group.name || `群组 ${group.id}`
        };
        
        this.selectConversation(conversation);
        
        // 根据配置决定是否自动切换到聊天页面
        if (this.options.autoSwitchToChat) {
            this._switchPage('chats');
        }
    }
    
    /**
     * 选择会话
     * @param {Object} conversation 会话对象
     */
    selectConversation(conversation) {
        // 更新当前会话
        this.currentConversation = conversation;
        
        // 更新聊天区域
        if (this.elements.chat) {
            // 更新标题
            if (this.elements.chat.chatTitle) {
                this.elements.chat.chatTitle.textContent = conversation.name || (conversation.type === 'GROUP' ? `群组 ${conversation.targetId}` : `用户 ${conversation.targetId}`);
            }
            
            // 更新子标题
            if (this.elements.chat.chatSubtitle) {
                this.elements.chat.chatSubtitle.textContent = conversation.type === 'GROUP' ? '群聊' : '私聊';
            }
            
            // 启用输入区域
            if (this.elements.chat.messageInput) {
                this.elements.chat.messageInput.disabled = false;
                this.elements.chat.messageInput.focus();
            }
            
            if (this.elements.chat.sendBtn) {
                this.elements.chat.sendBtn.disabled = true; // 初始状态禁用，等输入内容后启用
            }
        }
        
        // 高亮选中的会话
        const conversationItems = document.querySelectorAll('.list-item');
        conversationItems.forEach(item => {
            if (item.dataset.id === conversation.id) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
        
        // 触发会话选择回调
        if (this.options.onConversationSelect) {
            this.options.onConversationSelect(conversation);
        }
    }
    
    /**
     * 获取当前会话
     * @returns {Object|null} 当前会话
     */
    getCurrentConversation() {
        return this.currentConversation;
    }
    
    /**
     * 清空消息区域
     */
    clearMessages() {
        if (!this.elements.chat || !this.elements.chat.messagesContainer) {
            return;
        }
        
        // 清空消息
        this.elements.chat.messagesContainer.innerHTML = '';
        
        // 清空缓存
        this.messageMap.clear();
        
        // 如果没有选中的会话，显示提示信息
        if (!this.currentConversation) {
            this.elements.chat.messagesContainer.innerHTML = `
                <div class="no-conversation">
                    <div class="no-conversation-icon">
                        <i class="fa fa-comments-o"></i>
                    </div>
                    <p>选择一个联系人或群组开始聊天</p>
                </div>
            `;
        }
    }
    
    /**
     * 更新WebSocket客户端引用
     * @private
     */
    _updateWsClient() {
        if (!this.wsClient && window.state && window.state.imClient) {
            this.wsClient = window.state.imClient.wsClient;
        }
    }
    
    /**
     * 添加消息
     * @param {Object} message 消息对象
     */
    addMessage(message) {
        // 更新WebSocket客户端引用
        this._updateWsClient();
        
        if (!this.elements.chat || !this.elements.chat.messagesContainer) {
            return;
        }
        
        // 添加调试日志
        console.log('尝试添加消息:', message);
        console.log('当前会话:', this.currentConversation);
        
        // 判断是否属于当前会话
        let belongsToCurrentConversation = false;
        
        if (this.currentConversation) {
            if (this.currentConversation.type === 'C2C') {
                belongsToCurrentConversation = 
                    (message.fromId === this.currentConversation.targetId) || 
                    (message.toId === this.currentConversation.targetId);
                console.log('C2C消息判断结果:', belongsToCurrentConversation);
            } else if (this.currentConversation.type === 'GROUP') {
                // 修复：检查message.groupId是否存在，如果不存在可能需要从其他字段获取
                if (message.groupId) {
                    belongsToCurrentConversation = message.groupId === this.currentConversation.targetId;
                    console.log('GROUP消息判断结果(使用groupId):', belongsToCurrentConversation, 
                              'message.groupId=', message.groupId, 
                              'conversation.targetId=', this.currentConversation.targetId);
                } else if (message.toId && message.type === 'group') {
                    // 尝试使用toId作为替代
                    belongsToCurrentConversation = message.toId === this.currentConversation.targetId;
                    console.log('GROUP消息判断结果(使用toId):', belongsToCurrentConversation);
                }
            }
        }
        
        console.log('消息是否属于当前会话:', belongsToCurrentConversation);
        
        if (!belongsToCurrentConversation) {
            console.log('消息不属于当前会话，跳过渲染');
            return;
        }
        
        // 清除"无会话"提示
        const noConversation = this.elements.chat.messagesContainer.querySelector('.no-conversation');
        if (noConversation) {
            noConversation.remove();
        }
        
        // 创建消息元素
        const messageEl = this._createMessageElement(message);
        
        // 添加到消息列表
        this.elements.chat.messagesContainer.appendChild(messageEl);
        
        // 滚动到底部
        this.scrollToBottom();
        
        // 缓存消息元素
        if (message.id) {
            this.messageMap.set(message.id, messageEl);
        }
    }
    
    /**
     * 创建消息元素
     * @param {Object} message 消息对象
     * @returns {HTMLElement} 消息元素
     * @private
     */
    _createMessageElement(message) {
        // 添加调试日志
        console.log('创建消息元素:', message);
        
        // 判断是否是自己发送的消息
        let isSelf = message.fromId === this.userId;
        console.log('是否是自己发送的消息:', isSelf, 'message.fromId=', message.fromId, 'this.userId=', this.userId);
        
        // 创建外层元素
        const messageEl = document.createElement('div');
        messageEl.className = `message ${isSelf ? 'message-self' : 'message-other'}`;
        
        // 设置消息类型标记
        if (message.type) {
            messageEl.dataset.type = message.type;
        }
        
        // 设置消息ID
        if (message.id) {
            messageEl.dataset.id = message.id;
        }
        
        // 创建消息行元素
        const messageRow = document.createElement('div');
        messageRow.className = 'message-row';
        
        // 创建左侧头像 (对方消息时)
        if (!isSelf) {
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.innerHTML = '<div class="avatar-icon"><i class="fa fa-user"></i></div>';
            messageRow.appendChild(avatar);
        }
        
        // 创建消息内容部分
        const content = document.createElement('div');
        content.className = 'message-content';
        
        // 添加发送者名称 (仅对方消息)
        if (!isSelf) {
            const sender = document.createElement('div');
            sender.className = 'message-sender';
            // 群聊时显示发送者ID
            sender.textContent = message.fromId;
            content.appendChild(sender);
        }
        
        // 添加消息气泡
        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        
        const text = document.createElement('div');
        text.className = 'message-text';
        text.innerHTML = this._processMessageContent(message.content);
        
        bubble.appendChild(text);
        content.appendChild(bubble);
        
        // 添加消息信息 (时间和状态)
        const info = document.createElement('div');
        info.className = 'message-info';
        
        const time = document.createElement('span');
        time.className = 'message-time';
        // 确保timestamp是有效的
        if (message.timestamp && !isNaN(new Date(message.timestamp).getTime())) {
            time.textContent = this._formatTimeOnly(new Date(message.timestamp));
        } else {
            time.textContent = this._formatTimeOnly(new Date());
            console.warn('消息时间戳无效，使用当前时间:', message.timestamp);
        }
        info.appendChild(time);
        
        if (isSelf) {
            const status = document.createElement('span');
            status.className = 'message-status';
            status.textContent = this._getStatusText(message.status);
            info.appendChild(status);
        }
        
        content.appendChild(info);
        messageRow.appendChild(content);
        
        // 创建右侧头像 (自己消息时)
        if (isSelf) {
            const avatar = document.createElement('div');
            avatar.className = 'message-avatar';
            avatar.innerHTML = '<div class="avatar-icon"><i class="fa fa-user"></i></div>';
            messageRow.appendChild(avatar);
        }
        
        // 将消息行添加到消息元素
        messageEl.appendChild(messageRow);
        
        return messageEl;
    }
    
    /**
     * 处理消息内容，支持简单的文本格式化
     * @param {string} content 原始消息内容
     * @returns {string} 处理后的消息内容
     * @private
     */
    _processMessageContent(content) {
        if (!content) return '';
        
        // 转义HTML特殊字符防止XSS
        let processedContent = content
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
        
        // URL转为可点击链接
        processedContent = processedContent.replace(
            /(https?:\/\/[^\s]+)/g, 
            '<a href="$1" target="_blank" rel="noopener noreferrer">$1</a>'
        );
        
        // 支持简单的表情符号替换
        // 这里可以扩展添加表情符号支持
        
        return processedContent;
    }
    
    /**
     * 更新消息状态
     * @param {string} messageId 消息ID
     * @param {string} status 消息状态
     */
    updateMessageStatus(messageId, status) {
        const messageEl = this.messageMap.get(messageId);
        
        if (messageEl) {
            const statusEl = messageEl.querySelector('.message-status');
            
            if (statusEl) {
                statusEl.textContent = this._getStatusText(status);
                
                // 添加状态类
                messageEl.className = messageEl.className.replace(/status-\w+/g, '');
                messageEl.classList.add(`status-${status}`);
            }
        }
    }
    
    /**
     * 获取状态文本
     * @param {string} status 状态代码
     * @returns {string} 状态文本
     * @private
     */
    _getStatusText(status) {
        const statusMap = {
            'sending': '发送中...',
            'sent': '已发送',
            'delivered': '已送达',
            'read': '已读',
            'failed': '发送失败',
            'received': '已收到'
        };
        
        return statusMap[status] || '';
    }
    
    /**
     * 滚动到底部
     */
    scrollToBottom() {
        if (this.elements.chat && this.elements.chat.messagesContainer) {
            this.elements.chat.messagesContainer.scrollTop = this.elements.chat.messagesContainer.scrollHeight;
        }
    }
    
    /**
     * 格式化时间
     * @param {number} timestamp 时间戳
     * @returns {string} 格式化后的时间
     * @private
     */
    _formatTime(timestamp) {
        const date = new Date(timestamp);
        const now = new Date();
        
        // 今天的消息只显示时分
        if (date.toDateString() === now.toDateString()) {
            return this._formatTimeOnly(date);
        }
        
        // 昨天的消息显示"昨天"
        const yesterday = new Date(now);
        yesterday.setDate(now.getDate() - 1);
        if (date.toDateString() === yesterday.toDateString()) {
            return `昨天 ${this._formatTimeOnly(date)}`;
        }
        
        // 更早的消息显示年月日
        return this._formatDatetime(date);
    }
    
    /**
     * 格式化为时:分
     * @param {Date} date 日期对象
     * @returns {string} 格式化后的时间
     * @private
     */
    _formatTimeOnly(date) {
        return `${this._padZero(date.getHours())}:${this._padZero(date.getMinutes())}`;
    }
    
    /**
     * 格式化为年-月-日 时:分
     * @param {Date} date 日期对象
     * @returns {string} 格式化后的日期时间
     * @private
     */
    _formatDatetime(date) {
        return `${date.getFullYear()}-${this._padZero(date.getMonth() + 1)}-${this._padZero(date.getDate())} ${this._formatTimeOnly(date)}`;
    }
    
    /**
     * 补零
     * @param {number} num 数字
     * @returns {string} 补零后的字符串
     * @private
     */
    _padZero(num) {
        return num < 10 ? `0${num}` : `${num}`;
    }
    
    /**
     * 处理登出
     * @private
     */
    _handleLogout() {
        // 显示确认对话框
        if (confirm('确定要退出登录吗？')) {
            // 隐藏主应用，显示登录页面
            const loginPage = document.getElementById('login-page');
            const mainApp = document.getElementById('main-app');
            
            if (loginPage && mainApp) {
                loginPage.classList.remove('hidden');
                mainApp.classList.add('hidden');
            }
            
            // 断开WebSocket连接
            if (window.state && window.state.imClient) {
                window.state.imClient.disconnect();
            }
            
            // 清空当前状态
            window.state = {};
        }
    }
    
    /**
     * 处理创建群组
     * @param {Object} groupData 群组数据
     * @private
     */
    _handleCreateGroup(groupData) {
        if (this.options.onCreateGroup) {
            this.options.onCreateGroup(groupData);
        }
    }
    
    /**
     * 显示添加好友模态框
     * @private
     */
    _showAddFriendModal() {
        try {
            console.log('显示添加好友模态框');
            
            // 获取模态框元素
            const modalBackdrop = document.getElementById('modal-backdrop');
            const modalContainer = document.getElementById('modal-container');
            
            if (!modalBackdrop || !modalContainer) {
                console.error('模态框元素不存在');
                return;
            }
            
            // 创建模态框内容
            modalContainer.innerHTML = `
                <div class="modal">
                    <div class="modal-header">
                        <h3>添加好友</h3>
                        <div class="modal-close"><i class="fa fa-times"></i></div>
                    </div>
                    <div class="modal-body">
                        <div class="form-group">
                            <label for="friend-id">好友ID</label>
                            <input type="text" id="friend-id" placeholder="请输入好友ID">
                        </div>
                        <div class="form-group">
                            <label for="friend-remark">备注名称</label>
                            <input type="text" id="friend-remark" placeholder="请输入备注名称(可选)">
                        </div>
                        <div class="form-group">
                            <label for="friend-wording">验证消息</label>
                            <input type="text" id="friend-wording" value="请求添加您为好友" placeholder="请输入验证消息">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button id="cancel-add-friend">取消</button>
                        <button id="confirm-add-friend">添加</button>
                    </div>
                </div>
            `;
            
            // 显示模态框
            modalBackdrop.classList.remove('hidden');
            modalContainer.classList.remove('hidden');
            
            // 设置关闭模态框的事件
            const closeModal = () => {
                modalBackdrop.classList.add('hidden');
                modalContainer.classList.add('hidden');
            };
            
            // 添加点击背景关闭模态框
            modalBackdrop.onclick = (e) => {
                if (e.target === modalBackdrop) {
                    closeModal();
                }
            };
            
            // 添加点击关闭按钮关闭模态框
            const closeBtn = modalContainer.querySelector('.modal-close');
            if (closeBtn) {
                closeBtn.onclick = closeModal;
            }
            
            // 添加点击取消按钮关闭模态框
            const cancelBtn = document.getElementById('cancel-add-friend');
            if (cancelBtn) {
                cancelBtn.onclick = closeModal;
            }
            
            // 添加点击确认按钮处理添加好友
            const confirmBtn = document.getElementById('confirm-add-friend');
            if (confirmBtn) {
                const clickHandler = function() {
                    const friendId = document.getElementById('friend-id');
                    const friendRemark = document.getElementById('friend-remark');
                    const friendWording = document.getElementById('friend-wording');
                    
                    if (friendId && friendId.value.trim()) {
                        this._handleAddFriend({
                            toId: friendId.value.trim(),
                            remark: friendRemark ? friendRemark.value.trim() : '',
                            addWording: friendWording ? friendWording.value.trim() : '请求添加您为好友'
                        });
                        closeModal();
                    } else {
                        alert('请输入好友ID');
                    }
                }.bind(this);
                
                confirmBtn.onclick = clickHandler;
            }
            
            // 设置初始焦点
            const friendIdInput = document.getElementById('friend-id');
            if (friendIdInput) {
                friendIdInput.focus();
            }
        } catch (error) {
            console.error('显示添加好友模态框时出错:', error);
        }
    }
    
    /**
     * 处理添加好友
     * @param {Object} friendData 好友数据
     * @private
     */
    _handleAddFriend(friendData) {
        if (this.options.onAddFriend) {
            this.options.onAddFriend(friendData);
        }
    }
}

// 导出为全局变量，以便在不支持ES6模块的环境中使用
window.UIController = UIController; 