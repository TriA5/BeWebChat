import React, { useState, useEffect, useRef, useCallback } from 'react';
import { getUserInfo } from '../../api/user/loginApi';
import { connect as wsConnect, subscribe as wsSubscribe, send as wsSend } from '../../api/websocket/stompClient';
import { getMessages, listConversations, createGroup, joinGroup, listGroups, getGroupMessages } from '../../api/chat/chatApi';
import { getFriendsList } from '../../api/user/friendshipApi';
import './Chat.css';

interface Message {
  id: string;
  senderId: string;
  senderName: string;
  senderAvatar?: string;
  content: string;
  timestamp: Date;
  type: 'text' | 'image' | 'file';
  isOwn: boolean;
}

interface ChatRoom {
  id: string;
  name: string;
  avatar?: string;
  lastMessage?: string;
  lastMessageTime?: Date;
  unreadCount: number;
  isOnline: boolean;
  participants: string[];
  type: 'private' | 'group';
  role?: 'ADMIN' | 'MEMBER';
}

interface User {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  avatar?: string;
  isOnline: boolean;
}

const Chat: React.FC = () => {
  const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
  const [selectedChatRoom, setSelectedChatRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState('');
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const [friendMap, setFriendMap] = useState<Record<string, { name: string; avatar?: string }>>({});
  const [showCreateGroupModal, setShowCreateGroupModal] = useState(false);
  const [groupName, setGroupName] = useState('');
  const [selectedFriends, setSelectedFriends] = useState<string[]>([]);
  const [friends, setFriends] = useState<any[]>([]);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadMessages = useCallback(async (room: ChatRoom) => {
    try {
      const me = getUserInfo();
      const myId = me?.id;
      const myName = me?.username || 'Tôi';
      let msgs: any[];
      if (room.type === 'private') {
        msgs = await getMessages(room.id);
      } else {
        msgs = await getGroupMessages(room.id);
      }
      const transformed: Message[] = msgs.map(m => ({
        id: m.id,
        senderId: m.senderId,
        senderName: m.senderId === myId ? myName : (friendMap[m.senderId]?.name || 'Bạn bè'),
        senderAvatar: friendMap[m.senderId]?.avatar,
        content: m.content,
        timestamp: new Date(m.createdAt),
        type: 'text',
        isOwn: m.senderId === myId,
      }));
      setMessages(transformed);
    } catch (e) {
      console.error('Load messages failed', e);
      setMessages([]);
    }
  }, [friendMap]);

  useEffect(() => {
    const init = async () => {
      const user = getUserInfo();
      if (user) {
        setCurrentUser({
          id: user.id || '1',
          username: user.username || 'user',
          firstName: user.lastName || 'User',
          lastName: user.lastName || '',
          avatar: user.avatar || '',
          isOnline: true,
        });
      }
      try {
        const me = getUserInfo();
        if (!me?.id) return;
        const friendsData = await getFriendsList();
        setFriends(friendsData);
        const map: Record<string, { name: string; avatar?: string }> = {};
        friendsData.forEach(f => {
          if (f.userId) {
            map[f.userId] = { name: `${f.firstName} ${f.lastName}`.trim(), avatar: f.avatar };
          }
        });
        setFriendMap(map);
        const convs = await listConversations(me.id);
        const privateRooms: ChatRoom[] = convs.map(c => {
          const otherId = c.participant1Id === me.id ? c.participant2Id : c.participant1Id;
          const info = map[otherId];
          return {
            id: c.id,
            name: info?.name || 'Cuộc trò chuyện',
            avatar: info?.avatar || '',
            lastMessage: '',
            lastMessageTime: undefined,
            unreadCount: 0,
            isOnline: true,
            participants: [c.participant1Id, c.participant2Id],
            type: 'private',
          };
        });
        const groups = await listGroups(me.id);
        const groupRooms: ChatRoom[] = groups.map(g => ({
          id: g.id,
          name: g.name,
          avatar: '',
          lastMessage: '',
          lastMessageTime: undefined,
          unreadCount: 0,
          isOnline: true,
          participants: [],
          type: 'group',
          role: g.createdBy === me.id ? 'ADMIN' : 'MEMBER',
        }));
        setChatRooms([...privateRooms, ...groupRooms]);
      } catch (e) {
        console.error('Init chat failed', e);
      }
    };
    init();
  }, []);

  useEffect(() => {
    const selectFirst = async () => {
      if (!selectedChatRoom && chatRooms.length > 0) {
        const first = chatRooms[0];
        setSelectedChatRoom(first);
        await loadMessages(first);
      }
    };
    selectFirst();
  }, [chatRooms, selectedChatRoom, loadMessages]);

  const handleSelectChatRoom = async (room: ChatRoom) => {
    try {
      setSelectedChatRoom(room);
      await loadMessages(room);
    } catch (e) {
      console.error('Select chat room failed', e);
    }
  };

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMessage.trim() || !selectedChatRoom || !currentUser) return;
    const content = newMessage.trim();
    setNewMessage('');

    try {
      if (selectedChatRoom.type === 'private') {
        wsSend('/app/chat.send', {
          conversationId: selectedChatRoom.id,
          senderId: currentUser.id,
          content,
        });
      } else {
        wsSend('/app/group.send', {
          groupId: selectedChatRoom.id,
          senderId: currentUser.id,
          content,
        });
      }
    } catch (e) {
      console.error('Send message failed', e);
    }
    inputRef.current?.focus();
  };

  const handleCreateGroup = async () => {
    if (!groupName.trim() || !currentUser) return;
    try {
      const group = await createGroup(currentUser.id, groupName, selectedFriends);
      setChatRooms(prev => [{
        id: group.id,
        name: group.name,
        avatar: '',
        lastMessage: '',
        lastMessageTime: undefined,
        unreadCount: 0,
        isOnline: true,
        participants: [currentUser.id, ...selectedFriends],
        type: 'group',
        role: 'ADMIN',
      }, ...prev]);
      setShowCreateGroupModal(false);
      setGroupName('');
      setSelectedFriends([]);
    } catch (e) {
      console.error('Create group failed', e);
    }
  };

  const handleJoinGroup = async (groupId: string) => {
    if (!currentUser) return;
    try {
      await joinGroup(groupId, currentUser.id);
      const groups = await listGroups(currentUser.id);
      const newGroup = groups.find(g => g.id === groupId);
      if (newGroup) {
        setChatRooms(prev => [{
          id: newGroup.id,
          name: newGroup.name,
          avatar: '',
          lastMessage: '',
          lastMessageTime: undefined,
          unreadCount: 0,
          isOnline: true,
          participants: [],
          type: 'group',
          role: newGroup.createdBy === currentUser.id ? 'ADMIN' : 'MEMBER',
        }, ...prev]);
      }
    } catch (e) {
      console.error('Join group failed', e);
    }
  };

  useEffect(() => {
    const me = getUserInfo();
    let subConv: any = null;
    let subMsg: any = null;
    let subGroup: any = null;
    let subGroupMsg: any = null;
    wsConnect(() => {
      if (me?.id) {
        subConv = wsSubscribe(`/topic/conversations/${me.id}`, (msg) => {
          const data = JSON.parse(msg.body);
          const myId = me.id;
          const otherId = data.participant1Id === myId ? data.participant2Id : data.participant1Id;
          const info = friendMap[otherId];
          setChatRooms(prev => [{
            id: data.id,
            name: info?.name || 'Cuộc trò chuyện',
            unreadCount: 0,
            isOnline: true,
            participants: [myId, otherId],
            avatar: info?.avatar || '',
            lastMessage: '',
            lastMessageTime: new Date(),
            type: 'private',
          }, ...prev]);
        });
        subGroup = wsSubscribe(`/topic/groups/${me.id}`, (msg) => {
          const data = JSON.parse(msg.body);
          setChatRooms(prev => [{
            id: data.id,
            name: data.name,
            avatar: '',
            lastMessage: '',
            lastMessageTime: undefined,
            unreadCount: 0,
            isOnline: true,
            participants: [],
            type: 'group',
            role: data.createdBy === me.id ? 'ADMIN' : 'MEMBER',
          }, ...prev]);
        });
      }
      if (selectedChatRoom) {
        if (selectedChatRoom.type === 'private') {
          subMsg = wsSubscribe(`/topic/chat/${selectedChatRoom.id}`, (msg) => {
            const data = JSON.parse(msg.body);
            const myId = me?.id;
            const myName = me?.username || 'Tôi';
            const parsedDate = new Date(data.createdAt);
            const ts = isNaN(parsedDate.getTime()) ? new Date() : parsedDate;
            const incoming: Message = {
              id: data.id,
              senderId: data.senderId,
              senderName: data.senderId === myId ? myName : (friendMap[data.senderId]?.name || 'Bạn bè'),
              senderAvatar: friendMap[data.senderId]?.avatar,
              content: data.content,
              timestamp: ts,
              type: 'text',
              isOwn: data.senderId === myId,
            };
            setMessages(prev => [...prev, incoming]);
            setChatRooms(prev => prev.map(r => r.id === selectedChatRoom.id ? { ...r, lastMessage: incoming.content, lastMessageTime: incoming.timestamp } : r));
          });
        } else {
          subGroupMsg = wsSubscribe(`/topic/group/${selectedChatRoom.id}`, (msg) => {
            const data = JSON.parse(msg.body);
            const myId = me?.id;
            const myName = me?.username || 'Tôi';
            const parsedDate = new Date(data.createdAt);
            const ts = isNaN(parsedDate.getTime()) ? new Date() : parsedDate;
            const incoming: Message = {
              id: data.id,
              senderId: data.senderId,
              senderName: data.senderId === myId ? myName : (friendMap[data.senderId]?.name || 'Bạn bè'),
              senderAvatar: friendMap[data.senderId]?.avatar,
              content: data.content,
              timestamp: ts,
              type: 'text',
              isOwn: data.senderId === myId,
            };
            setMessages(prev => [...prev, incoming]);
            setChatRooms(prev => prev.map(r => r.id === selectedChatRoom.id ? { ...r, lastMessage: incoming.content, lastMessageTime: incoming.timestamp } : r));
          });
        }
      }
    }, (err) => {
      console.error('WebSocket connection error:', err);
    });
    return () => {
      try { subConv?.unsubscribe?.(); } catch {}
      try { subMsg?.unsubscribe?.(); } catch {}
      try { subGroup?.unsubscribe?.(); } catch {}
      try { subGroupMsg?.unsubscribe?.(); } catch {}
    };
  }, [selectedChatRoom, friendMap]);

  const formatTime = (date: Date) => {
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    if (diff < 60 * 1000) return 'Vừa xong';
    if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))} phút trước`;
    if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))} giờ trước`;
    if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / (24 * 60 * 60 * 1000))} ngày trước`;
    return date.toLocaleDateString('vi-VN');
  };

  const formatMessageTime = (date: Date) => {
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const filteredChatRooms = chatRooms.filter(room =>
    room.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  useEffect(() => {
    wsConnect();
  }, []);

  if (!currentUser) {
    return (
      <div className="chat-container">
        <div className="chat-login-required">
          <h3>Vui lòng đăng nhập để sử dụng chat</h3>
          <a href="/login" className="login-link">Đăng nhập ngay</a>
        </div>
      </div>
    );
  }

  return (
    <div className="chat-container">
      <div className={`chat-sidebar ${isSidebarOpen ? 'open' : 'closed'}`}>
        <div className="sidebar-header">
          <h2>Tin nhắn</h2>
          <button className="sidebar-toggle" onClick={() => setIsSidebarOpen(!isSidebarOpen)}>
            {isSidebarOpen ? '←' : '→'}
          </button>
        </div>
        <div className="search-box">
          <input
            type="text"
            placeholder="Tìm kiếm cuộc trò chuyện..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
          <div className="search-icon">🔍</div>
        </div>
        <button className="create-group-btn" onClick={() => setShowCreateGroupModal(true)}>
          Tạo nhóm
        </button>
        <div className="chat-rooms-list">
          {filteredChatRooms.map(room => (
            <div
              key={room.id}
              className={`chat-room-item ${selectedChatRoom?.id === room.id ? 'active' : ''}`}
              onClick={() => handleSelectChatRoom(room)}
            >
              <div className="room-avatar">
                {room.avatar ? (
                  <img src={room.avatar} alt={room.name} />
                ) : (
                  <div className="avatar-placeholder">
                    {room.name.charAt(0).toUpperCase()}
                  </div>
                )}
                {room.isOnline && <div className="online-indicator"></div>}
              </div>
              <div className="room-info">
                <div className="room-header">
                  <h4 className="room-name">{room.name} {room.type === 'group' ? '(Nhóm)' : ''}</h4>
                  {room.lastMessageTime && (
                    <span className="last-time">{formatTime(room.lastMessageTime)}</span>
                  )}
                </div>
                <div className="room-footer">
                  <p className="last-message">{room.lastMessage || 'Chưa có tin nhắn'}</p>
                  {room.unreadCount > 0 && (
                    <span className="unread-badge">{room.unreadCount}</span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
      <div className="chat-main">
        {selectedChatRoom ? (
          <>
            <div className="chat-header">
              <div className="chat-info">
                <div className="chat-avatar">
                  {selectedChatRoom.avatar ? (
                    <img src={selectedChatRoom.avatar} alt={selectedChatRoom.name} />
                  ) : (
                    <div className="avatar-placeholder">
                      {selectedChatRoom.name.charAt(0).toUpperCase()}
                    </div>
                  )}
                  {selectedChatRoom.isOnline && <div className="online-indicator"></div>}
                </div>
                <div>
                  <h3>{selectedChatRoom.name} {selectedChatRoom.type === 'group' ? `(Nhóm - ${selectedChatRoom.role})` : ''}</h3>
                  <p className="chat-status">
                    {selectedChatRoom.isOnline ? 'Đang hoạt động' : 'Không hoạt động'}
                  </p>
                </div>
              </div>
              <div className="chat-actions">
                {selectedChatRoom.type === 'group' && selectedChatRoom.role === 'ADMIN' && (
                  <button className="action-btn" onClick={() => setShowCreateGroupModal(true)}>Thêm thành viên</button>
                )}
                <button className="action-btn">📞</button>
                <button className="action-btn">📹</button>
                <button className="action-btn">⚙️</button>
              </div>
            </div>
            <div className="messages-area">
              {messages.map(message => (
                <div
                  key={message.id}
                  className={`message ${message.isOwn ? 'own' : 'other'}`}
                >
                  {!message.isOwn && (
                    <div className="message-avatar">
                      {message.senderAvatar ? (
                        <img src={message.senderAvatar} alt={message.senderName} />
                      ) : (
                        <div className="avatar-placeholder">
                          {message.senderName.charAt(0).toUpperCase()}
                        </div>
                      )}
                    </div>
                  )}
                  <div className="message-content">
                    {!message.isOwn && (
                      <span className="message-sender">{message.senderName}</span>
                    )}
                    <div className="message-bubble">
                      <p>{message.content}</p>
                    </div>
                    <span className="message-time">
                      {formatMessageTime(message.timestamp)}
                    </span>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>
            <form className="message-input-area" onSubmit={handleSendMessage}>
              <div className="input-container">
                <button type="button" className="attachment-btn">📎</button>
                <input
                  ref={inputRef}
                  type="text"
                  placeholder="Nhập tin nhắn..."
                  value={newMessage}
                  onChange={(e) => setNewMessage(e.target.value)}
                  className="message-input"
                />
                <button type="button" className="emoji-btn">😊</button>
                <button type="submit" className="send-btn" disabled={!newMessage.trim()}>
                  ➤
                </button>
              </div>
            </form>
          </>
        ) : (
          <div className="no-chat-selected">
            <div className="welcome-message">
              <h3>Chào mừng đến với ChatWeb!</h3>
              <p>Chọn một cuộc trò chuyện hoặc nhóm để bắt đầu nhắn tin</p>
            </div>
          </div>
        )}
      </div>
      {showCreateGroupModal && (
        <div className="modal">
          <div className="modal-content">
            <h3>{selectedChatRoom?.type === 'group' && selectedChatRoom.role === 'ADMIN' ? 'Thêm thành viên' : 'Tạo nhóm mới'}</h3>
            <input
              type="text"
              placeholder="Tên nhóm"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              disabled={selectedChatRoom?.type === 'group'}
            />
            <div className="friend-list">
              {friends.map(friend => (
                <div key={friend.userId}>
                  <input
                    type="checkbox"
                    checked={selectedFriends.includes(friend.userId)}
                    onChange={() => {
                      setSelectedFriends(prev =>
                        prev.includes(friend.userId)
                          ? prev.filter(id => id !== friend.userId)
                          : [...prev, friend.userId]
                      );
                    }}
                  />
                  <span>{friend.firstName} {friend.lastName}</span>
                </div>
              ))}
            </div>
            <button
              onClick={selectedChatRoom?.type === 'group' ? () => {} : handleCreateGroup}
              disabled={!groupName.trim() || selectedFriends.length === 0}
            >
              {selectedChatRoom?.type === 'group' ? 'Thêm thành viên' : 'Tạo nhóm'}
            </button>
            <button onClick={() => setShowCreateGroupModal(false)}>Hủy</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Chat;