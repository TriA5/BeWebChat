import { getToken } from '../util/JwtService';

const API_BASE_URL = 'http://localhost:8080';

export interface ChatMessageDTO {
  id: string;
  conversationId?: string; // null cho group chat
  groupId?: string;       // null cho private chat
  senderId: string;
  content: string;
  createdAt: string;
}

export interface ConversationDTO {
  id: string;
  participant1Id: string;
  participant2Id: string;
}

export interface GroupConversationDTO {
  id: string;
  name: string;
  createdBy: string;
}

export async function ensureConversation(userAId: string, userBId: string): Promise<string> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/chat/ensure?userAId=${encodeURIComponent(userAId)}&userBId=${encodeURIComponent(userBId)}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to ensure conversation');
  const id = await res.text();
  return id.replace(/"/g, ''); // Normalize UUID
}

export async function getMessages(conversationId: string): Promise<ChatMessageDTO[]> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/chat/${encodeURIComponent(conversationId)}/messages`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to get messages');
  return res.json();
}

export async function listConversations(userId: string): Promise<ConversationDTO[]> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/chat/conversations?userId=${encodeURIComponent(userId)}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to list conversations');
  return res.json();
}

export async function createGroup(creatorId: string, groupName: string, initialMemberIds: string[]): Promise<GroupConversationDTO> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/groups/create?creatorId=${encodeURIComponent(creatorId)}&groupName=${encodeURIComponent(groupName)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(initialMemberIds)
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to create group');
  return res.json();
}

export async function joinGroup(groupId: string, userId: string): Promise<void> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/groups/${encodeURIComponent(groupId)}/join?userId=${encodeURIComponent(userId)}`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to join group');
}

export async function getGroupMessages(groupId: string): Promise<ChatMessageDTO[]> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/groups/${encodeURIComponent(groupId)}/messages`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to get group messages');
  return res.json();
}

export async function sendGroupMessage(groupId: string, senderId: string, content: string): Promise<ChatMessageDTO> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/groups/${encodeURIComponent(groupId)}/messages?senderId=${encodeURIComponent(senderId)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(content)
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to send group message');
  return res.json();
}

export async function listGroups(userId: string): Promise<GroupConversationDTO[]> {
  const token = getToken();
  if (!token) throw new Error('No JWT token found');
  const res = await fetch(`${API_BASE_URL}/groups/user/${encodeURIComponent(userId)}`, {
    method: 'GET',
    headers: {
      'Accept': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error(await res.text() || 'Failed to list groups');
  return res.json();
}