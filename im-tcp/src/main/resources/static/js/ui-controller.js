/**
 * UI控制器类
 * 负责处理UI相关的逻辑和交互
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
            onAddFriend: () => {}
        }, options);
        
        this.elements = this.options.elements;
        this.currentConversation = null;
        this.messageMap = new Map(); // 用于存储消息ID到DOM元素的映射
        
        this._setupEventListeners();
    }
    
    /**
     * 设置事件监听器
     * @private
     */
    _setupEventListeners() {
        // 这里可以添加更多UI相关的事件监听器
        
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
        if (!this.currentConversation || !content || !this.options.onSendMessage) {
            return;
        }
        
        const messageData = {
            conversationId: this.currentConversation.id,
            conversationType: this.currentConversation.type,
            targetId: this.currentConversation.targetId,
            content: content
        };
        
        this.options.onSendMessage(messageData);
    }
    
    /**
     * 设置用户信息
     * @param {Object} userInfo 用户信息
     */
    setUserInfo(userInfo) {
        if (!userInfo) return;
        
        // 设置头像
        const avatar = userInfo.userId.charAt(0).toUpperCase();
        
        if (this.elements.sidebar && this.elements.sidebar.currentAvatar) {
            this.elements.sidebar.currentAvatar.textContent = avatar;
        }
        
        if (this.elements.sidebar && this.elements.sidebar.profileAvatar) {
            this.elements.sidebar.profileAvatar.textContent = avatar;
        }
        
        // 设置用户名
        if (this.elements.sidebar && this.elements.sidebar.profileName) {
            this.elements.sidebar.profileName.textContent = userInfo.userId;
        }
    }
    
    /**
     * 更新会话列表
     * @param {Array} conversations 会话列表
     */
    updateConversationsList(conversations) {
        if (!this.elements.lists || !this.elements.lists.conversationsList) return;
        
        // 清空列表
        this.elements.lists.conversationsList.innerHTML = '';
        
        // 按最后消息时间排序
        const sortedConversations = [...conversations].sort((a, b) => {
            const timeA = a.lastMessage ? a.lastMessage.timestamp : 0;
            const timeB = b.lastMessage ? b.lastMessage.timestamp : 0;
            return timeB - timeA;
        });
        
        // 添加会话项
        sortedConversations.forEach(conversation => {
            const item = this._createConversationItem(conversation);
            this.elements.lists.conversationsList.appendChild(item);
        });
    }
    
    /**
     * 创建会话列表项
     * @param {Object} conversation 会话对象
     * @returns {HTMLElement} 会话列表项元素
     * @private
     */
    _createConversationItem(conversation) {
        const item = document.createElement('div');
        item.className = 'conversation-item';
        item.setAttribute('data-id', conversation.id);
        if (this.currentConversation && this.currentConversation.id === conversation.id) {
            item.classList.add('active');
        }
        
        // 头像
        const avatar = document.createElement('div');
        avatar.className = 'conversation-avatar';
        avatar.textContent = conversation.avatar;
        
        // 信息区域
        const info = document.createElement('div');
        info.className = 'conversation-info';
        
        // 头部（名称和时间）
        const header = document.createElement('div');
        header.className = 'conversation-header';
        
        const name = document.createElement('div');
        name.className = 'conversation-name';
        name.textContent = conversation.name;
        
        const time = document.createElement('div');
        time.className = 'conversation-time';
        time.textContent = conversation.lastMessage ? this._formatTime(conversation.lastMessage.timestamp) : '';
        
        header.appendChild(name);
        header.appendChild(time);
        
        // 消息预览
        const message = document.createElement('div');
        message.className = 'conversation-message';
        message.textContent = conversation.lastMessage ? conversation.lastMessage.content : '';
        
        info.appendChild(header);
        info.appendChild(message);
        
        // 未读数徽章
        if (conversation.unreadCount && conversation.unreadCount > 0) {
            const badge = document.createElement('div');
            badge.className = 'conversation-badge';
            badge.textContent = conversation.unreadCount > 99 ? '99+' : conversation.unreadCount;
            info.appendChild(badge);
        }
        
        item.appendChild(avatar);
        item.appendChild(info);
        
        // 添加点击事件
        item.addEventListener('click', () => {
            this.selectConversation(conversation);
        });
        
        return item;
    }
    
    /**
     * 更新联系人列表
     * @param {Array} contacts 联系人列表
     */
    updateContactsList(contacts) {
        if (!this.elements.lists || !this.elements.lists.contactsList) return;
        
        // 清空列表
        this.elements.lists.contactsList.innerHTML = '';
        
        // 按名称排序
        const sortedContacts = [...contacts].sort((a, b) => {
            return a.name.localeCompare(b.name);
        });
        
        // 添加联系人项
        sortedContacts.forEach(contact => {
            const item = this._createContactItem(contact);
            this.elements.lists.contactsList.appendChild(item);
        });
    }
    
    /**
     * 创建联系人列表项
     * @param {Object} contact 联系人对象
     * @returns {HTMLElement} 联系人列表项元素
     * @private
     */
    _createContactItem(contact) {
        const item = document.createElement('div');
        item.className = 'contact-item';
        item.setAttribute('data-id', contact.id);
        
        // 头像
        const avatar = document.createElement('div');
        avatar.className = 'contact-avatar';
        avatar.textContent = contact.avatar;
        
        // 名称
        const name = document.createElement('div');
        name.className = 'contact-name';
        name.textContent = contact.name;
        
        item.appendChild(avatar);
        item.appendChild(name);
        
        // 添加点击事件
        item.addEventListener('click', () => {
            this._handleContactClick(contact);
        });
        
        return item;
    }
    
    /**
     * 处理联系人点击
     * @param {Object} contact 联系人对象
     * @private
     */
    _handleContactClick(contact) {
        // 创建或获取与该联系人的会话
        const conversationId = `C2C_${contact.id}`;
        
        // 检查是否已有该会话
        const conversationItem = document.querySelector(`.conversation-item[data-id="${conversationId}"]`);
        if (conversationItem) {
            // 已有会话，直接点击
            conversationItem.click();
        } else {
            // 创建新会话
            const conversation = {
                id: conversationId,
                type: 'C2C',
                targetId: contact.id,
                name: contact.name,
                avatar: contact.avatar
            };
            this.selectConversation(conversation);
        }
    }
    
    /**
     * 更新群组列表
     * @param {Array} groups 群组列表
     */
    updateGroupsList(groups) {
        if (!this.elements.lists || !this.elements.lists.groupsList) return;
        
        // 清空列表
        this.elements.lists.groupsList.innerHTML = '';
        
        // 按名称排序
        const sortedGroups = [...groups].sort((a, b) => {
            return a.name.localeCompare(b.name);
        });
        
        // 添加群组项
        sortedGroups.forEach(group => {
            const item = this._createGroupItem(group);
            this.elements.lists.groupsList.appendChild(item);
        });
    }
    
    /**
     * 创建群组列表项
     * @param {Object} group 群组对象
     * @returns {HTMLElement} 群组列表项元素
     * @private
     */
    _createGroupItem(group) {
        const item = document.createElement('div');
        item.className = 'contact-item';
        item.setAttribute('data-id', group.id);
        
        // 头像
        const avatar = document.createElement('div');
        avatar.className = 'contact-avatar';
        avatar.textContent = group.avatar;
        
        // 名称
        const name = document.createElement('div');
        name.className = 'contact-name';
        name.textContent = `${group.name} (${group.memberCount || 0}人)`;
        
        item.appendChild(avatar);
        item.appendChild(name);
        
        // 添加点击事件
        item.addEventListener('click', () => {
            this._handleGroupClick(group);
        });
        
        return item;
    }
    
    /**
     * 处理群组点击
     * @param {Object} group 群组对象
     * @private
     */
    _handleGroupClick(group) {
        // 创建或获取与该群组的会话
        const conversationId = `GROUP_${group.id}`;
        
        // 检查是否已有该会话
        const conversationItem = document.querySelector(`.conversation-item[data-id="${conversationId}"]`);
        if (conversationItem) {
            // 已有会话，直接点击
            conversationItem.click();
        } else {
            // 创建新会话
            const conversation = {
                id: conversationId,
                type: 'GROUP',
                targetId: group.id,
                name: group.name,
                avatar: group.avatar
            };
            this.selectConversation(conversation);
        }
    }
    
    /**
     * 选择会话
     * @param {Object} conversation 会话对象
     */
    selectConversation(conversation) {
        if (!conversation) return;
        
        // 更新当前会话
        this.currentConversation = conversation;
        
        // 更新会话列表项的选中状态
        const conversationItems = document.querySelectorAll('.conversation-item');
        conversationItems.forEach(item => {
            if (item.getAttribute('data-id') === conversation.id) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
        
        // 更新聊天标题
        if (this.elements.chat && this.elements.chat.chatTitle) {
            this.elements.chat.chatTitle.textContent = conversation.name || conversation.targetId;
        }
        
        // 更新聊天副标题
        if (this.elements.chat && this.elements.chat.chatSubtitle) {
            this.elements.chat.chatSubtitle.textContent = conversation.type === 'C2C' ? '私聊' : '群聊';
        }
        
        // 调用回调函数
        this.options.onConversationSelect(conversation);
    }
    
    /**
     * 获取当前会话
     * @returns {Object|null} 当前会话对象
     */
    getCurrentConversation() {
        return this.currentConversation;
    }
    
    /**
     * 清空消息列表
     */
    clearMessages() {
        if (!this.elements.chat || !this.elements.chat.messagesContainer) return;
        
        // 移除所有消息元素，但保留无会话提示
        const noConversation = this.elements.chat.messagesContainer.querySelector('.no-conversation');
        this.elements.chat.messagesContainer.innerHTML = '';
        
        if (noConversation) {
            this.elements.chat.messagesContainer.appendChild(noConversation);
        }
        
        // 隐藏或显示无会话提示
        if (this.currentConversation) {
            if (noConversation) noConversation.style.display = 'none';
        } else {
            if (noConversation) noConversation.style.display = 'flex';
        }
        
        // 清空消息映射
        this.messageMap.clear();
    }
    
    /**
     * 添加消息
     * @param {Object} message 消息对象
     */
    addMessage(message) {
        if (!this.elements.chat || !this.elements.chat.messagesContainer || !message) return;
        
        // 隐藏无会话提示
        const noConversation = this.elements.chat.messagesContainer.querySelector('.no-conversation');
        if (noConversation) {
            noConversation.style.display = 'none';
        }
        
        // 创建消息元素
        const messageEl = this._createMessageElement(message);
        this.elements.chat.messagesContainer.appendChild(messageEl);
        
        // 存储消息ID到DOM元素的映射
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
        const messageContainer = document.createElement('div');
        messageContainer.className = `message ${message.isOwn ? 'message-self' : 'message-other'}`;
        messageContainer.setAttribute('data-id', message.id);
        
        // 头像
        const avatar = document.createElement('div');
        avatar.className = 'message-avatar';
        avatar.textContent = message.fromId ? message.fromId.charAt(0).toUpperCase() : 'U';
        
        // 消息内容和信息
        const contentWrapper = document.createElement('div');
        contentWrapper.className = 'message-content-wrapper';
        
        // 消息头部（用户名）
        const header = document.createElement('div');
        header.className = 'message-header';
        header.textContent = message.fromId;
        
        // 消息内容
        const content = document.createElement('div');
        content.className = 'message-content';
        content.textContent = message.content;
        
        // 消息信息（时间和状态）
        const info = document.createElement('div');
        info.className = 'message-info';
        
        const time = document.createElement('span');
        time.className = 'message-time';
        time.textContent = this._formatTime(message.timestamp);
        info.appendChild(time);
        
        if (message.isOwn) {
            const status = document.createElement('span');
            status.className = 'message-status';
            status.textContent = this._getStatusText(message.status);
            info.appendChild(status);
        }
        
        contentWrapper.appendChild(header);
        contentWrapper.appendChild(content);
        contentWrapper.appendChild(info);
        
        messageContainer.appendChild(avatar);
        messageContainer.appendChild(contentWrapper);
        
        return messageContainer;
    }
    
    /**
     * 更新消息状态
     * @param {string} messageId 消息ID
     * @param {string} status 消息状态
     */
    updateMessageStatus(messageId, status) {
        if (!messageId || !status) return;
        
        const messageEl = this.messageMap.get(messageId);
        if (messageEl) {
            const statusEl = messageEl.querySelector('.message-status');
            if (statusEl) {
                statusEl.textContent = this._getStatusText(status);
            }
        }
    }
    
    /**
     * 获取状态文本
     * @param {string} status 状态标识
     * @returns {string} 状态文本
     * @private
     */
    _getStatusText(status) {
        switch (status) {
            case 'sending':
                return '发送中';
            case 'sent':
                return '已发送';
            case 'delivered':
                return '已送达';
            case 'read':
                return '已读';
            case 'failed':
                return '发送失败';
            default:
                return '';
        }
    }
    
    /**
     * 滚动到底部
     */
    scrollToBottom() {
        if (!this.elements.chat || !this.elements.chat.messagesContainer) return;
        
        this.elements.chat.messagesContainer.scrollTop = this.elements.chat.messagesContainer.scrollHeight;
    }
    
    /**
     * 格式化时间
     * @param {number} timestamp 时间戳
     * @returns {string} 格式化后的时间
     * @private
     */
    _formatTime(timestamp) {
        if (!timestamp) return '';
        
        const date = new Date(timestamp);
        const now = new Date();
        const isToday = date.getDate() === now.getDate() &&
                       date.getMonth() === now.getMonth() &&
                       date.getFullYear() === now.getFullYear();
        
        if (isToday) {
            return this._formatTimeOnly(date);
        } else {
            return this._formatDatetime(date);
        }
    }
    
    /**
     * 格式化时间（仅时分）
     * @param {Date} date 日期对象
     * @returns {string} 格式化后的时间
     * @private
     */
    _formatTimeOnly(date) {
        return `${this._padZero(date.getHours())}:${this._padZero(date.getMinutes())}`;
    }
    
    /**
     * 格式化日期时间
     * @param {Date} date 日期对象
     * @returns {string} 格式化后的日期时间
     * @private
     */
    _formatDatetime(date) {
        return `${date.getMonth() + 1}月${date.getDate()}日 ${this._formatTimeOnly(date)}`;
    }
    
    /**
     * 数字补零
     * @param {number} num 数字
     * @returns {string} 补零后的字符串
     * @private
     */
    _padZero(num) {
        return num < 10 ? `0${num}` : `${num}`;
    }
    
    /**
     * 退出登录
     * @private
     */
    _handleLogout() {
        // 确认是否退出
        if (window.confirm('确定要退出登录吗？')) {
            // 关闭WebSocket连接
            if (window.wsClient && typeof window.wsClient.disconnect === 'function') {
                window.wsClient.disconnect();
            }
            
            // 清除本地状态
            this.currentConversation = null;
            
            // 刷新页面，回到登录界面
            window.location.reload();
        }
    }
    
    /**
     * 处理创建群组
     * @param {Object} groupData 群组数据
     * @private
     */
    _handleCreateGroup(groupData) {
        if (!groupData.groupName || !this.options.onCreateGroup) {
            return;
        }
        
        this.options.onCreateGroup(groupData);
    }
    
    /**
     * 显示添加好友的模态框
     * @private
     */
    _showAddFriendModal() {
        const modalBackdrop = document.getElementById('modal-backdrop');
        const modalContainer = document.getElementById('modal-container');
        
        if (!modalBackdrop || !modalContainer) {
            console.error('找不到模态框元素');
            return;
        }
        
        console.log('显示添加好友模态框');
        
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
        
        try {
            // 确保模态框元素在DOM中
            if (!document.body.contains(modalBackdrop)) {
                document.body.appendChild(modalBackdrop);
            }
            if (!document.body.contains(modalContainer)) {
                document.body.appendChild(modalContainer);
            }
            
            // 显示模态框
            modalBackdrop.classList.remove('hidden');
            modalContainer.classList.remove('hidden');
            
            // 设置点击背景关闭模态框（确保事件只绑定一次）
            modalBackdrop.removeEventListener('click', this._closeBackdropHandler);
            this._closeBackdropHandler = (e) => {
                if (e.target === modalBackdrop) {
                    modalBackdrop.classList.add('hidden');
                    modalContainer.classList.add('hidden');
                }
            };
            modalBackdrop.addEventListener('click', this._closeBackdropHandler);
            
            // 立即绑定事件，无需延迟
            const closeBtn = modalContainer.querySelector('.modal-close');
            const cancelBtn = document.getElementById('cancel-add-friend');
            const confirmBtn = document.getElementById('confirm-add-friend');
            
            const closeModal = () => {
                modalBackdrop.classList.add('hidden');
                modalContainer.classList.add('hidden');
            };
            
            if (closeBtn) {
                closeBtn.removeEventListener('click', closeModal);
                closeBtn.addEventListener('click', closeModal);
            }
            
            if (cancelBtn) {
                cancelBtn.removeEventListener('click', closeModal);
                cancelBtn.addEventListener('click', closeModal);
            }
            
            if (confirmBtn) {
                const self = this;
                const clickHandler = function() {
                    const friendId = document.getElementById('friend-id');
                    const friendRemark = document.getElementById('friend-remark');
                    const friendWording = document.getElementById('friend-wording');
                    
                    if (friendId && friendId.value.trim()) {
                        self._handleAddFriend({
                            userId: friendId.value.trim(),
                            remark: friendRemark ? friendRemark.value.trim() : '',
                            addWording: friendWording ? friendWording.value.trim() : '请求添加您为好友'
                        });
                        
                        closeModal();
                    } else {
                        alert('请输入好友ID');
                    }
                };
                
                confirmBtn.removeEventListener('click', clickHandler);
                confirmBtn.addEventListener('click', clickHandler);
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
        if (!friendData.userId || !this.options.onAddFriend) {
            return;
        }
        
        this.options.onAddFriend(friendData);
    }
}

// 导出UIController类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = UIController;
} 