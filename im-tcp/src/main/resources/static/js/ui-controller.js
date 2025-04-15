/**
 * UI控制器类
 * 负责界面交互和主题切换等UI相关功能
 */
class UIController {
    /**
     * 构造函数
     * @param {Object} options 配置选项
     */
    constructor(options = {}) {
        this.options = Object.assign({
            onThemeChange: () => {},
            onStyleChange: () => {},
            onFontSizeChange: () => {}
        }, options);
        
        this.currentTheme = 'light';
        this.currentStyle = 'classic';
        this.currentFontSize = 14;
        
        this.activePage = 'chats';
        this.activeTab = {
            chats: 'recent',
            contacts: 'all',
            groups: 'joined',
        };
        
        this.currentConversation = null;
        this.messageInputEnabled = false;
        
        this.init();
    }
    
    /**
     * 初始化UI控制器
     */
    init() {
        this._loadSettings();
        this._setupEventListeners();
        this._applySettings();
    }
    
    /**
     * 切换页面
     * @param {string} page 页面名称
     */
    switchPage(page) {
        const validPages = ['chats', 'contacts', 'groups', 'settings'];
        if (!validPages.includes(page)) {
            console.error(`无效的页面名称: ${page}`);
            return;
        }
        
        // 更新导航图标激活状态
        document.querySelectorAll('.sidebar-icon').forEach(icon => {
            icon.classList.remove('active');
        });
        document.querySelector(`.sidebar-icon[data-page="${page}"]`).classList.add('active');
        
        // 更新页面显示
        document.querySelectorAll('.page-content').forEach(pageEl => {
            pageEl.classList.remove('active');
        });
        document.getElementById(`${page}-page`).classList.add('active');
        
        this.activePage = page;
    }
    
    /**
     * 切换标签页
     * @param {string} page 页面名称
     * @param {string} tab 标签页名称
     */
    switchTab(page, tab) {
        const tabContainer = document.querySelector(`#${page}-page .tabs`);
        if (!tabContainer) return;
        
        // 更新标签激活状态
        tabContainer.querySelectorAll('.tab').forEach(tabEl => {
            tabEl.classList.remove('active');
        });
        tabContainer.querySelector(`.tab[data-tab="${tab}"]`).classList.add('active');
        
        // 特殊处理某些标签切换
        if (page === 'groups' && tab === 'create') {
            document.getElementById('groups-list').classList.add('hidden');
            document.getElementById('create-group-form').classList.remove('hidden');
        } else if (page === 'groups' && tab === 'joined') {
            document.getElementById('groups-list').classList.remove('hidden');
            document.getElementById('create-group-form').classList.add('hidden');
        }
        
        this.activeTab[page] = tab;
    }
    
    /**
     * 添加会话项
     * @param {Object} conversation 会话数据
     */
    addConversationItem(conversation) {
        const conversationsList = document.getElementById('conversations-list');
        
        // 检查是否已存在该会话
        const existingItem = document.getElementById(`conversation-${conversation.id}`);
        if (existingItem) {
            this.updateConversationItem(conversation);
            return;
        }
        
        const itemElement = document.createElement('div');
        itemElement.id = `conversation-${conversation.id}`;
        itemElement.className = 'conversation-item';
        itemElement.setAttribute('data-id', conversation.id);
        itemElement.setAttribute('data-type', conversation.type);
        
        // 如果是私聊，设置toId属性
        if (conversation.type === 'chat') {
            itemElement.setAttribute('data-to-id', conversation.toId);
        } else if (conversation.type === 'group') {
            itemElement.setAttribute('data-group-id', conversation.groupId);
        }
        
        // 设置头像显示的第一个字符
        const avatarChar = conversation.name ? conversation.name.charAt(0) : (conversation.type === 'chat' ? conversation.toId.charAt(0) : 'G');
        
        itemElement.innerHTML = `
            <div class="conversation-avatar">${avatarChar}</div>
            <div class="conversation-info">
                <div class="conversation-header">
                    <div class="conversation-name">${conversation.name || (conversation.type === 'chat' ? conversation.toId : `群聊(${conversation.groupId})`)}</div>
                    <div class="conversation-time">${this._formatTime(conversation.timestamp)}</div>
                </div>
                <div class="conversation-message">${conversation.lastMessage || ''}</div>
            </div>
            ${conversation.unread ? `<div class="conversation-badge">${conversation.unread}</div>` : ''}
        `;
        
        itemElement.addEventListener('click', () => {
            this.selectConversation(conversation);
        });
        
        // 添加到列表中，最新的对话放在最前面
        if (conversationsList.firstChild) {
            conversationsList.insertBefore(itemElement, conversationsList.firstChild);
        } else {
            conversationsList.appendChild(itemElement);
        }
    }
    
    /**
     * 更新会话项
     * @param {Object} conversation 会话数据
     */
    updateConversationItem(conversation) {
        const itemElement = document.getElementById(`conversation-${conversation.id}`);
        if (!itemElement) {
            this.addConversationItem(conversation);
            return;
        }
        
        // 更新最新消息和时间
        const messageEl = itemElement.querySelector('.conversation-message');
        const timeEl = itemElement.querySelector('.conversation-time');
        
        if (messageEl && conversation.lastMessage) {
            messageEl.textContent = conversation.lastMessage;
        }
        
        if (timeEl && conversation.timestamp) {
            timeEl.textContent = this._formatTime(conversation.timestamp);
        }
        
        // 更新未读消息数
        const badgeEl = itemElement.querySelector('.conversation-badge');
        if (conversation.unread && conversation.unread > 0) {
            if (badgeEl) {
                badgeEl.textContent = conversation.unread;
            } else {
                const badge = document.createElement('div');
                badge.className = 'conversation-badge';
                badge.textContent = conversation.unread;
                itemElement.appendChild(badge);
            }
        } else if (badgeEl) {
            badgeEl.remove();
        }
        
        // 移动到顶部（最新的对话）
        const conversationsList = document.getElementById('conversations-list');
        if (conversationsList.firstChild !== itemElement) {
            conversationsList.removeChild(itemElement);
            conversationsList.insertBefore(itemElement, conversationsList.firstChild);
        }
        
        // 如果当前正在查看这个会话，更新标题
        if (this.currentConversation && this.currentConversation.id === conversation.id) {
            this.selectConversation(conversation);
        }
    }
    
    /**
     * 选择会话
     * @param {Object} conversation 会话数据
     */
    selectConversation(conversation) {
        this.currentConversation = conversation;
        
        // 更新选中状态
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
        });
        
        const itemElement = document.getElementById(`conversation-${conversation.id}`);
        if (itemElement) {
            itemElement.classList.add('active');
            
            // 清除未读标记
            const badgeEl = itemElement.querySelector('.conversation-badge');
            if (badgeEl) {
                badgeEl.remove();
            }
        }
        
        // 更新聊天标题
        const titleEl = document.getElementById('chat-title');
        const subtitleEl = document.getElementById('chat-subtitle');
        
        if (titleEl) {
            titleEl.textContent = conversation.name || (conversation.type === 'chat' ? conversation.toId : `群聊(${conversation.groupId})`);
        }
        
        if (subtitleEl) {
            if (conversation.type === 'chat') {
                subtitleEl.textContent = '单聊';
            } else if (conversation.type === 'group') {
                subtitleEl.textContent = `群聊 · ${conversation.memberCount || 0}人`;
            }
        }
        
        // 启用消息输入
        this.enableMessageInput();
        
        // 移除"无会话"提示
        const noConversationEl = document.querySelector('.chat-messages .no-conversation');
        if (noConversationEl) {
            noConversationEl.style.display = 'none';
        }
        
        // 触发会话选择回调
        if (this.options.onConversationSelect) {
            this.options.onConversationSelect(conversation);
        }
    }
    
    /**
     * 启用消息输入
     */
    enableMessageInput() {
        const inputEl = document.getElementById('message-input');
        const sendBtn = document.getElementById('send-btn');
        
        if (inputEl) {
            inputEl.disabled = false;
            inputEl.focus();
        }
        
        if (sendBtn) {
            sendBtn.disabled = false;
        }
        
        this.messageInputEnabled = true;
    }
    
    /**
     * 禁用消息输入
     */
    disableMessageInput() {
        const inputEl = document.getElementById('message-input');
        const sendBtn = document.getElementById('send-btn');
        
        if (inputEl) {
            inputEl.disabled = true;
            inputEl.value = '';
        }
        
        if (sendBtn) {
            sendBtn.disabled = true;
        }
        
        this.messageInputEnabled = false;
    }
    
    /**
     * 添加联系人项
     * @param {Object} contact 联系人数据
     */
    addContactItem(contact) {
        const contactsList = document.getElementById('contacts-list');
        
        // 检查是否已存在该联系人
        const existingItem = document.getElementById(`contact-${contact.userId}`);
        if (existingItem) return;
        
        const itemElement = document.createElement('div');
        itemElement.id = `contact-${contact.userId}`;
        itemElement.className = 'contact-item';
        itemElement.setAttribute('data-id', contact.userId);
        
        // 设置头像显示的第一个字符
        const avatarChar = contact.nickname ? contact.nickname.charAt(0) : contact.userId.charAt(0);
        
        // 判断是否有备注
        const displayName = contact.remark || contact.nickname || contact.userId;
        
        itemElement.innerHTML = `
            <div class="contact-avatar">${avatarChar}</div>
            <div class="contact-info">
                <div class="contact-header">
                    <div class="contact-name">${displayName}</div>
                    ${contact.black === 1 ? '<span class="contact-label">已拉黑</span>' : ''}
                </div>
            </div>
        `;
        
        // 点击联系人创建或切换到对应会话
        itemElement.addEventListener('click', () => {
            this._createChatFromContact(contact);
        });
        
        contactsList.appendChild(itemElement);
    }
    
    /**
     * 从联系人创建聊天会话
     * @param {Object} contact 联系人数据
     * @private
     */
    _createChatFromContact(contact) {
        // 首先检查是否已存在该会话
        const conversationId = `chat-${contact.userId}`;
        const existingConversation = document.querySelector(`.conversation-item[data-to-id="${contact.userId}"]`);
        
        if (existingConversation) {
            // 如果存在，直接点击它
            existingConversation.click();
            this.switchPage('chats');
            return;
        }
        
        // 创建新会话
        const conversation = {
            id: conversationId,
            type: 'chat',
            toId: contact.userId,
            name: contact.remark || contact.nickname || contact.userId,
            timestamp: Date.now(),
            lastMessage: '',
            unread: 0
        };
        
        this.addConversationItem(conversation);
        this.switchPage('chats');
        this.selectConversation(conversation);
    }
    
    /**
     * 添加群组项
     * @param {Object} group 群组数据
     */
    addGroupItem(group) {
        const groupsList = document.getElementById('groups-list');
        
        // 检查是否已存在该群组
        const existingItem = document.getElementById(`group-${group.groupId}`);
        if (existingItem) return;
        
        const itemElement = document.createElement('div');
        itemElement.id = `group-${group.groupId}`;
        itemElement.className = 'contact-item';
        itemElement.setAttribute('data-id', group.groupId);
        
        // 设置头像显示的第一个字符
        const avatarChar = group.groupName ? group.groupName.charAt(0) : 'G';
        
        itemElement.innerHTML = `
            <div class="contact-avatar">${avatarChar}</div>
            <div class="contact-info">
                <div class="contact-header">
                    <div class="contact-name">${group.groupName || `群聊(${group.groupId})`}</div>
                    <span class="contact-label">${group.memberCount || 0}人</span>
                </div>
            </div>
        `;
        
        // 点击群组创建或切换到对应会话
        itemElement.addEventListener('click', () => {
            this._createChatFromGroup(group);
        });
        
        groupsList.appendChild(itemElement);
    }
    
    /**
     * 从群组创建聊天会话
     * @param {Object} group 群组数据
     * @private
     */
    _createChatFromGroup(group) {
        // 首先检查是否已存在该会话
        const conversationId = `group-${group.groupId}`;
        const existingConversation = document.querySelector(`.conversation-item[data-group-id="${group.groupId}"]`);
        
        if (existingConversation) {
            // 如果存在，直接点击它
            existingConversation.click();
            this.switchPage('chats');
            return;
        }
        
        // 创建新会话
        const conversation = {
            id: conversationId,
            type: 'group',
            groupId: group.groupId,
            name: group.groupName || `群聊(${group.groupId})`,
            timestamp: Date.now(),
            lastMessage: '',
            unread: 0,
            memberCount: group.memberCount || 0
        };
        
        this.addConversationItem(conversation);
        this.switchPage('chats');
        this.selectConversation(conversation);
    }
    
    /**
     * 添加消息到聊天窗口
     * @param {Object} message 消息数据
     */
    addMessage(message) {
        const messagesContainer = document.getElementById('chat-messages');
        if (!messagesContainer) return;
        
        // 如果当前没有选中会话，不添加消息
        if (!this.currentConversation) return;
        
        // 如果消息不属于当前会话，不添加
        if (message.type === 'chat' && 
            ((this.currentConversation.type === 'chat' && 
              (message.fromId !== this.currentConversation.toId && message.toId !== this.currentConversation.toId)) ||
             this.currentConversation.type === 'group')) {
            return;
        }
        
        if (message.type === 'group' && 
            (this.currentConversation.type !== 'group' || 
             message.groupId !== this.currentConversation.groupId)) {
            return;
        }
        
        const messageElement = document.createElement('div');
        messageElement.id = `message-${message.id}`;
        messageElement.className = `message ${this._isCurrentUser(message.fromId) ? 'message-self' : 'message-other'}`;
        
        // 获取发送者的第一个字符作为头像
        const avatarChar = message.fromId.charAt(0);
        
        // 创建消息头部（包含头像）
        const headerElement = document.createElement('div');
        headerElement.className = 'message-header';
        headerElement.innerHTML = `
            <div class="message-avatar">${avatarChar}</div>
        `;
        messageElement.appendChild(headerElement);
        
        // 创建消息内容
        const contentElement = document.createElement('div');
        contentElement.className = 'message-content';
        contentElement.textContent = message.content;
        messageElement.appendChild(contentElement);
        
        // 创建消息信息（时间和状态）
        const infoElement = document.createElement('div');
        infoElement.className = 'message-info';
        
        let statusHtml = '';
        if (this._isCurrentUser(message.fromId)) {
            switch(message.status) {
                case 'sending':
                    statusHtml = '<span class="message-status">发送中</span>';
                    break;
                case 'sent':
                    statusHtml = '<span class="message-status">已发送</span>';
                    break;
                case 'delivered':
                    statusHtml = '<span class="message-status">已送达</span>';
                    break;
                case 'read':
                    statusHtml = '<span class="message-status message-read">已读</span>';
                    break;
                case 'failed':
                    statusHtml = '<span class="message-status" style="color: red;">发送失败</span>';
                    break;
            }
        }
        
        infoElement.innerHTML = `
            <span class="message-time">${this._formatTime(message.timestamp)}</span>
            ${statusHtml}
        `;
        messageElement.appendChild(infoElement);
        
        // 添加到消息容器
        messagesContainer.appendChild(messageElement);
        
        // 滚动到底部
        this._scrollToBottom();
    }
    
    /**
     * 更新消息状态
     * @param {string} messageId 消息ID
     * @param {string} status 消息状态
     */
    updateMessageStatus(messageId, status) {
        const messageElement = document.getElementById(`message-${messageId}`);
        if (!messageElement) return;
        
        const statusElement = messageElement.querySelector('.message-status');
        if (!statusElement) return;
        
        statusElement.classList.remove('message-read');
        
        switch(status) {
            case 'sending':
                statusElement.textContent = '发送中';
                break;
            case 'sent':
                statusElement.textContent = '已发送';
                break;
            case 'delivered':
                statusElement.textContent = '已送达';
                break;
            case 'read':
                statusElement.textContent = '已读';
                statusElement.classList.add('message-read');
                break;
            case 'failed':
                statusElement.textContent = '发送失败';
                statusElement.style.color = 'red';
                break;
        }
    }
    
    /**
     * 切换主题
     * @param {string} theme 主题名称 ('light' 或 'dark')
     */
    switchTheme(theme) {
        if (theme !== 'light' && theme !== 'dark') {
            console.error(`无效的主题: ${theme}`);
            return;
        }
        
        document.body.classList.remove('dark-theme');
        
        if (theme === 'dark') {
            document.body.classList.add('dark-theme');
        }
        
        this.currentTheme = theme;
        localStorage.setItem('theme', theme);
        
        if (this.options.onThemeChange) {
            this.options.onThemeChange(theme);
        }
    }
    
    /**
     * 切换界面风格
     * @param {string} style 界面风格 ('classic' 或 'modern')
     */
    switchStyle(style) {
        if (style !== 'classic' && style !== 'modern') {
            console.error(`无效的界面风格: ${style}`);
            return;
        }
        
        document.body.classList.remove('modern-style');
        
        if (style === 'modern') {
            document.body.classList.add('modern-style');
        }
        
        this.currentStyle = style;
        localStorage.setItem('style', style);
        
        if (this.options.onStyleChange) {
            this.options.onStyleChange(style);
        }
    }
    
    /**
     * 设置字体大小
     * @param {number} size 字体大小
     */
    setFontSize(size) {
        if (isNaN(size) || size < 12 || size > 20) {
            console.error(`无效的字体大小: ${size}`);
            return;
        }
        
        document.documentElement.style.setProperty('--font-size-base', `${size}px`);
        document.documentElement.style.setProperty('--font-size-small', `${size - 2}px`);
        document.documentElement.style.setProperty('--font-size-large', `${size + 2}px`);
        document.documentElement.style.setProperty('--font-size-xl', `${size + 4}px`);
        
        this.currentFontSize = size;
        localStorage.setItem('fontSize', size);
        
        // 更新显示的字体大小值
        const fontSizeValueEl = document.getElementById('font-size-value');
        if (fontSizeValueEl) {
            fontSizeValueEl.textContent = `${size}px`;
        }
        
        if (this.options.onFontSizeChange) {
            this.options.onFontSizeChange(size);
        }
    }
    
    /**
     * 显示模态框
     * @param {Object} options 模态框选项
     */
    showModal(options) {
        const backdrop = document.getElementById('modal-backdrop');
        const container = document.getElementById('modal-container');
        
        if (!backdrop || !container) return;
        
        container.innerHTML = `
            <div class="modal-header">
                <h3>${options.title || '提示'}</h3>
            </div>
            <div class="modal-body">
                ${options.content || ''}
            </div>
            <div class="modal-footer">
                ${options.showCancel ? '<button class="btn btn-secondary" id="modal-cancel">取消</button>' : ''}
                <button class="btn btn-primary" id="modal-confirm">${options.confirmText || '确定'}</button>
            </div>
        `;
        
        backdrop.classList.remove('hidden');
        container.classList.remove('hidden');
        
        // 绑定按钮事件
        const confirmBtn = document.getElementById('modal-confirm');
        const cancelBtn = document.getElementById('modal-cancel');
        
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => {
                this.hideModal();
                if (options.onConfirm) options.onConfirm();
            });
        }
        
        if (cancelBtn) {
            cancelBtn.addEventListener('click', () => {
                this.hideModal();
                if (options.onCancel) options.onCancel();
            });
        }
    }
    
    /**
     * 隐藏模态框
     */
    hideModal() {
        const backdrop = document.getElementById('modal-backdrop');
        const container = document.getElementById('modal-container');
        
        if (!backdrop || !container) return;
        
        backdrop.classList.add('hidden');
        container.classList.add('hidden');
    }
    
    /**
     * 显示上下文菜单
     * @param {Event} event 事件对象
     * @param {Array} items 菜单项
     */
    showContextMenu(event, items) {
        event.preventDefault();
        
        const menuEl = document.getElementById('context-menu');
        if (!menuEl) return;
        
        // 清空菜单
        menuEl.innerHTML = '';
        
        // 添加菜单项
        items.forEach(item => {
            const itemEl = document.createElement('div');
            itemEl.className = `menu-item ${item.danger ? 'danger' : ''}`;
            itemEl.textContent = item.text;
            
            itemEl.addEventListener('click', () => {
                this.hideContextMenu();
                if (item.onClick) item.onClick();
            });
            
            menuEl.appendChild(itemEl);
        });
        
        // 定位菜单
        const x = event.clientX;
        const y = event.clientY;
        
        menuEl.style.top = `${y}px`;
        menuEl.style.left = `${x}px`;
        
        // 显示菜单
        menuEl.classList.remove('hidden');
        
        // 点击其他区域关闭菜单
        setTimeout(() => {
            window.addEventListener('click', this._handleOutsideClick = () => {
                this.hideContextMenu();
            }, { once: true });
        }, 0);
    }
    
    /**
     * 隐藏上下文菜单
     */
    hideContextMenu() {
        const menuEl = document.getElementById('context-menu');
        if (!menuEl) return;
        
        menuEl.classList.add('hidden');
        
        if (this._handleOutsideClick) {
            window.removeEventListener('click', this._handleOutsideClick);
            this._handleOutsideClick = null;
        }
    }
    
    /**
     * 设置用户信息
     * @param {Object} user 用户信息
     */
    setUserInfo(user) {
        this.currentUser = user;
        
        // 更新用户头像
        const avatarEls = document.querySelectorAll('#current-avatar, #profile-avatar');
        avatarEls.forEach(el => {
            if (el) {
                el.textContent = user.userId.charAt(0);
            }
        });
        
        // 更新用户名
        const nameEl = document.getElementById('profile-name');
        if (nameEl) {
            nameEl.textContent = user.userId;
        }
        
        // 更新设置页面的用户信息
        const userIdEl = document.getElementById('settings-user-id');
        const clientTypeEl = document.getElementById('settings-client-type');
        const appIdEl = document.getElementById('settings-app-id');
        
        if (userIdEl) userIdEl.textContent = user.userId;
        if (clientTypeEl) clientTypeEl.textContent = user.clientType;
        if (appIdEl) appIdEl.textContent = user.appId;
    }
    
    /**
     * 加载设置
     * @private
     */
    _loadSettings() {
        this.currentTheme = localStorage.getItem('theme') || 'light';
        this.currentStyle = localStorage.getItem('style') || 'classic';
        this.currentFontSize = parseInt(localStorage.getItem('fontSize')) || 14;
    }
    
    /**
     * 应用设置
     * @private
     */
    _applySettings() {
        this.switchTheme(this.currentTheme);
        this.switchStyle(this.currentStyle);
        this.setFontSize(this.currentFontSize);
        
        // 更新UI控件状态
        const themeToggle = document.getElementById('theme-toggle');
        const styleToggle = document.getElementById('ui-style-toggle');
        const fontSizeRange = document.getElementById('font-size-range');
        
        if (themeToggle) {
            themeToggle.checked = this.currentTheme === 'dark';
        }
        
        if (styleToggle) {
            styleToggle.checked = this.currentStyle === 'modern';
        }
        
        if (fontSizeRange) {
            fontSizeRange.value = this.currentFontSize;
        }
    }
    
    /**
     * 设置事件监听器
     * @private
     */
    _setupEventListeners() {
        // 导航图标点击
        document.querySelectorAll('.sidebar-icon').forEach(icon => {
            icon.addEventListener('click', () => {
                const page = icon.getAttribute('data-page');
                if (page) this.switchPage(page);
            });
        });
        
        // 标签切换
        document.querySelectorAll('.tab').forEach(tab => {
            tab.addEventListener('click', () => {
                const tabName = tab.getAttribute('data-tab');
                const page = tab.closest('.page-content').id.replace('-page', '');
                if (tabName && page) this.switchTab(page, tabName);
            });
        });
        
        // 主题切换
        const themeToggle = document.getElementById('theme-toggle');
        if (themeToggle) {
            themeToggle.addEventListener('change', () => {
                this.switchTheme(themeToggle.checked ? 'dark' : 'light');
            });
        }
        
        // 风格切换
        const styleToggle = document.getElementById('ui-style-toggle');
        if (styleToggle) {
            styleToggle.addEventListener('change', () => {
                this.switchStyle(styleToggle.checked ? 'modern' : 'classic');
            });
        }
        
        // 字体大小调整
        const fontSizeRange = document.getElementById('font-size-range');
        if (fontSizeRange) {
            fontSizeRange.addEventListener('input', () => {
                this.setFontSize(parseInt(fontSizeRange.value));
            });
        }
        
        // 退出登录按钮
        const logoutBtn = document.getElementById('logout-btn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                this.showModal({
                    title: '退出登录',
                    content: '确定要退出登录吗？',
                    showCancel: true,
                    onConfirm: () => {
                        if (this.options.onLogout) {
                            this.options.onLogout();
                        }
                    }
                });
            });
        }
        
        // 发送消息
        const sendBtn = document.getElementById('send-btn');
        const messageInput = document.getElementById('message-input');
        
        if (sendBtn && messageInput) {
            const sendMessage = () => {
                const content = messageInput.value.trim();
                if (!content || !this.currentConversation) return;
                
                if (this.options.onSendMessage) {
                    const messageData = {
                        type: this.currentConversation.type,
                        content: content
                    };
                    
                    if (this.currentConversation.type === 'chat') {
                        messageData.toId = this.currentConversation.toId;
                    } else if (this.currentConversation.type === 'group') {
                        messageData.groupId = this.currentConversation.groupId;
                    }
                    
                    this.options.onSendMessage(messageData);
                }
                
                messageInput.value = '';
            };
            
            sendBtn.addEventListener('click', sendMessage);
            
            messageInput.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                    event.preventDefault();
                    sendMessage();
                }
            });
        }
    }
    
    /**
     * 判断是否为当前用户
     * @param {string} userId 用户ID
     * @returns {boolean} 是否为当前用户
     * @private
     */
    _isCurrentUser(userId) {
        return this.currentUser && userId === this.currentUser.userId;
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
        const diff = now - date;
        
        // 一天内显示时间，否则显示日期
        if (diff < 24 * 60 * 60 * 1000 && date.getDate() === now.getDate()) {
            return date.toTimeString().slice(0, 5);
        } else {
            return `${date.getMonth() + 1}月${date.getDate()}日`;
        }
    }
    
    /**
     * 滚动到底部
     * @private
     */
    _scrollToBottom() {
        const messagesContainer = document.getElementById('chat-messages');
        if (messagesContainer) {
            messagesContainer.scrollTop = messagesContainer.scrollHeight;
        }
    }
}

// 导出UIController类
if (typeof module !== 'undefined' && module.exports) {
    module.exports = UIController;
} 