import React, { useEffect, useMemo, useRef, useState } from 'react'
import {
  Box,
  Paper,
  IconButton,
  Typography,
  TextField,
  Avatar,
  List,
  ListItem,
  CircularProgress,
  Tooltip,
  Button
} from '@mui/material'
import {
  ChatBubble,
  Send,
  SmartToy,
  Close,
  InsertEmoticon,
  ImageSearch,
  ExpandMore,
  ReceiptLong
} from '@mui/icons-material'
import { useAuth } from '../contexts/AuthContext'
import { apiService, type Product } from '../services/api'
import { ChatSession } from '../types'
import { formatPrice } from '../utils/priceUtils'

interface ChatMessage {
  id?: number
  sessionId: string
  message: string
  response?: string
  messageType: 'USER' | 'BOT'
  timestamp: string
}

const GRADIENT_BG = 'linear-gradient(135deg, #ff7a18 0%, #ff4f58 100%)'

const normalizeText = (text: string): string =>
  text
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ') // Chuẩn hóa khoảng trắng
    .trim()

interface ChatWidgetProps {
  embedded?: boolean
  defaultOpen?: boolean
  title?: string
  welcomeMessage?: string
  headerGradient?: string
}

const ChatWidget: React.FC<ChatWidgetProps> = ({
  embedded = false,
  defaultOpen,
  title,
  welcomeMessage,
  headerGradient
}) => {
  const { user, isAuthenticated } = useAuth()

  const [isOpen, setIsOpen] = useState<boolean>(
    defaultOpen !== undefined ? defaultOpen : embedded
  )
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [inputMessage, setInputMessage] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [sessionId, setSessionId] = useState<number | null>(null)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [showEmoji, setShowEmoji] = useState(false)
  const [trackOpen, setTrackOpen] = useState(false)
  const [trackEmail, setTrackEmail] = useState('')
  const [trackNumber, setTrackNumber] = useState('')
  const lastSendRef = useRef<number>(0)

  const storageKey = useMemo(() => 'ai_widget_session_id', [])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const createNewSession = async (initialMessage: string = '') => {
    if (!user) return null
    try {
      const response = await apiService.createChatSession(user.id, initialMessage)
      if (response.success && response.data) {
        const sessionData = response.data as ChatSession;
        if (sessionData.id) {
          setSessionId(sessionData.id)
          localStorage.setItem(storageKey, String(sessionData.id))
          return sessionData.id
        }
      }
    } catch (e) {
      // Silent; widget will fall back to offline mode
    }
    return null
  }

  const handleProductIntent = async (rawText: string): Promise<boolean> => {
    const text = rawText.trim()
    if (!text) return false

    const norm = normalizeText(text)

    // Nếu user hỏi về quần áo / thời trang -> trả lời là shop chỉ bán đồ điện tử
    // Check cả cụm từ và từ đơn để bắt được nhiều cách hỏi
    const clothingKeywords = [
      'quan ao', 'ao thun', 'ao so mi', 'dam', 'vay', 'giay', 'quan jean', 'thoi trang',
      'ban quan ao', 'co ban quan ao', 'ban ao', 'ban quan', 'ban vay', 'ban giay',
      'ban thoi trang', 'kinh doanh quan ao', 'kinh doanh thoi trang',
      'shop ban quan ao', 'cua hang ban quan ao', 'co ban quan ao khong', 'ban quan ao khong'
    ]
    const foodKeywords = [
      'do an', 'thuc an', 'an uong', 'do uong', 'banh keo', 'pizza', 'tra sua',
      'ban do an', 'co ban do an', 'ban thuc an', 'co ban thuc an', 'ban do uong',
      'ban banh keo', 'ban pizza', 'ban tra sua', 'kinh doanh do an', 'kinh doanh thuc an',
      'shop ban do an', 'cua hang ban do an', 'co ban do an khong', 'ban do an khong',
      'co ban thuc an khong', 'ban thuc an khong'
    ]

    if (clothingKeywords.some(k => norm.includes(k))) {
      const botMsg: ChatMessage = {
        sessionId: '',
        message: '',
        response:
          'Hiện tại shop chỉ tập trung bán các sản phẩm điện tử (điện thoại, laptop, tai nghe, phụ kiện...). ' +
          'Bên mình **không kinh doanh quần áo / thời trang** bạn nhé. Nếu bạn cần tư vấn điện thoại hay laptop mình hỗ trợ rất chi tiết được.',
        messageType: 'BOT',
        timestamp: new Date().toISOString()
      }
      setMessages(prev => [...prev, botMsg])
      return true
    }

    if (foodKeywords.some(k => norm.includes(k))) {
      const botMsg: ChatMessage = {
        sessionId: '',
        message: '',
        response:
          'Shop hiện **không bán đồ ăn / nước uống**, mà chuyên về thiết bị điện tử và phụ kiện (điện thoại, laptop, tai nghe, smartwatch,...). ' +
          'Nếu bạn cần tìm một chiếc điện thoại, laptop hay phụ kiện phù hợp ngân sách, mình có thể tư vấn chi tiết cho bạn.',
        messageType: 'BOT',
        timestamp: new Date().toISOString()
      }
      setMessages(prev => [...prev, botMsg])
      return true
    }

    // Chỉ xử lý sản phẩm chi tiết khi người dùng hỏi về iPhone
    if (!norm.includes('iphone')) return false

    try {
      // Trường hợp hỏi chi tiết iPhone 15 Pro
      if (norm.includes('iphone 15 pro')) {
        const res = await apiService.getProducts(0, 20, 'iPhone 15 Pro')
        const products = (res.data?.content as Product[]) || []
        const target = products.find(p =>
          normalizeText(p.name).includes('iphone 15 pro')
        ) || products[0]

        if (target) {
          const price = formatPrice(target.salePrice ?? target.price)
          const rating = target.averageRating ?? target.rating
          const variants = Array.isArray(target.variants) ? target.variants : []
          const variantsText =
            variants.length > 0
              ? '\nCác phiên bản/tuỳ chọn hiện có:\n' +
                variants
                  .map(v => {
                    const vPrice = formatPrice(v.price)
                    const color = v.color ? ` • Màu: ${v.color}` : ''
                    return `- ${v.variantName}${color} • Giá: ${vPrice}`
                  })
                  .join('\n')
              : ''

          // Gợi ý phiên bản phù hợp nếu người dùng đang hỏi về ngân sách / ít tiền
          let recommendation = ''
          const wantsBudget =
            norm.includes('it tien') ||
            norm.includes('tiet kiem') ||
            norm.includes('sinh vien') ||
            norm.includes('re nhat') ||
            norm.includes('gia re') ||
            norm.includes('hop li') ||
            norm.includes('hop ly') ||
            norm.includes('phien ban nao') ||
            norm.includes('nen mua loai nao')

          if (variants.length > 0) {
            const sorted = [...variants].sort((a, b) => a.price - b.price)
            const cheapest = sorted[0]
            const mid = sorted[1]

            const cheapestPrice = formatPrice(cheapest.price)
            const cheapestName = cheapest.variantName

            if (wantsBudget) {
              recommendation =
                `\n\nNếu bạn đang muốn tối ưu chi phí, mình khuyên nên chọn phiên bản **${cheapestName}** (giá khoảng ${cheapestPrice}). ` +
                `Dung lượng này đủ dùng nếu bạn không quay/chụp quá nhiều 4K hoặc cài quá nhiều game nặng. ` +
                (mid
                  ? `Nếu bạn muốn thoải mái lưu trữ hơn một chút thì có thể cân nhắc ${mid.variantName} – giá cao hơn một chút nhưng bù lại dư dả dung lượng hơn.`
                  : 'Khi nào nhu cầu tăng (chụp/quay nhiều, cài nhiều app), lúc đó hãy cân nhắc các bản dung lượng cao hơn để tránh nhanh đầy bộ nhớ.')
            } else {
              recommendation =
                `\n\nVề lựa chọn phiên bản: **${cheapestName}** là lựa chọn cân bằng giữa giá và dung lượng cho đa số người dùng. ` +
                `Nếu bạn hay quay video nhiều, chơi game nặng hoặc dùng máy lâu năm, có thể cân nhắc các bản dung lượng cao hơn để tránh nhanh đầy bộ nhớ.`
            }
          }

          const details =
            `Đây là thông tin chi tiết về iPhone 15 Pro:\n` +
            `• Tên: ${target.name}\n` +
            (target.brand ? `• Thương hiệu: ${target.brand}\n` : '') +
            (target.category ? `• Danh mục: ${target.category}\n` : '') +
            `• Giá hiện tại: ${price}\n` +
            (rating ? `• Đánh giá trung bình: ${rating.toFixed(1)}/5\n` : '') +
            (target.stockQuantity > 0
              ? `• Tình trạng: Còn hàng (${target.stockQuantity} sản phẩm)\n`
              : '• Tình trạng: Có thể tạm hết hàng, bạn nên kiểm tra lại trong mục sản phẩm.\n') +
            variantsText +
            (target.description
              ? `\n\nMô tả nhanh: ${target.description}`
              : '\n\nBạn có thể bấm vào sản phẩm iPhone 15 Pro trong danh sách để xem thêm thông số chi tiết như màn hình, camera, pin, hiệu năng, v.v.') +
            recommendation

          const botMsg: ChatMessage = {
            sessionId: '',
            message: '',
            response: details,
            messageType: 'BOT',
            timestamp: new Date().toISOString()
          }
          setMessages(prev => [...prev, botMsg])
          return true
        }
      }

      // Hỏi chung: "có sản phẩm/dòng nào về iphone không"
      const res = await apiService.searchProducts('iPhone')
      const products = res.data?.content as Product[] | undefined
      if (!products || products.length === 0) {
        const botMsg: ChatMessage = {
          sessionId: '',
          message: '',
          response: 'Hiện tại mình chưa tìm thấy sản phẩm iPhone nào trong cửa hàng. Bạn thử lại sau nhé.',
          messageType: 'BOT',
          timestamp: new Date().toISOString()
        }
        setMessages(prev => [...prev, botMsg])
        return true
      }

      const iphoneProducts = products.filter(p =>
        normalizeText(p.name).includes('iphone')
      )

      const list = (iphoneProducts.length ? iphoneProducts : products)
        .slice(0, 8)
        .map((p, idx) => {
          const price = formatPrice(p.salePrice ?? p.price)
          const rating = p.averageRating ?? p.rating
          return `${idx + 1}. ${p.name} • Giá: ${price}` +
            (rating ? ` • Đánh giá: ${rating.toFixed(1)}/5` : '')
        })
        .join('\n')

      const botMsg: ChatMessage = {
        sessionId: '',
        message: '',
        response:
          `Mình tìm được một số sản phẩm iPhone phù hợp trong cửa hàng:\n\n${list}\n\n` +
          `Bạn có thể gõ lại đúng tên sản phẩm (ví dụ: "tư vấn cho tôi iPhone 15 Pro") để mình tư vấn chi tiết hơn từng mẫu nhé.`,
        messageType: 'BOT',
        timestamp: new Date().toISOString()
      }
      setMessages(prev => [...prev, botMsg])
      return true
    } catch (e) {
      // Nếu lỗi thì để backend AI xử lý như cũ
      return false
    }
  }

  useEffect(() => {
    const existing = localStorage.getItem(storageKey)
    if (existing) {
      const parsed = parseInt(existing, 10)
      if (!Number.isNaN(parsed)) {
        setSessionId(parsed)
      }
    }
  }, [storageKey])

  useEffect(() => {
    if (!isOpen) return
    if (messages.length === 0) {
      const welcome: ChatMessage = {
        sessionId: '',
        message: '',
        response:
          welcomeMessage ||
          'Xin chào, tôi là Stylist AI tư vấn thời trang. Bạn đang tìm đồ gì?',
        messageType: 'BOT',
        timestamp: new Date().toISOString()
      }
      setMessages([welcome])
      if (isAuthenticated && user && !sessionId) {
        createNewSession()
      }
    }
  }, [isOpen])

  useEffect(() => {
    scrollToBottom()
  }, [messages, isOpen])

  const handleSendMessage = async () => {
    const now = Date.now()
    if (now - lastSendRef.current < 400 || isLoading) return
    lastSendRef.current = now
    const text = inputMessage.trim()
    if (!text) return

    const userMsg: ChatMessage = {
      sessionId: sessionId ? String(sessionId) : '',
      message: text,
      response: undefined,
      messageType: 'USER',
      timestamp: new Date().toISOString()
    }
    setMessages(prev => [...prev, userMsg])
    setInputMessage('')
    setIsLoading(true)

    let workingSessionId = sessionId
    if (isAuthenticated && user && !workingSessionId) {
      workingSessionId = await createNewSession(text)
    }

    try {
      // Trước khi gọi AI backend, thử xử lý intent sản phẩm cục bộ (iphone, v.v.)
      const handled = await handleProductIntent(text)
      if (handled) {
        setIsLoading(false)
        return
      }

      if (workingSessionId && isAuthenticated && user) {
        // User is logged in, use session-based chat
        const response = await apiService.sendChatMessage(text, String(workingSessionId))
        if (response.success) {
          const botMsg: ChatMessage = {
            sessionId: response.data.sessionId ? String(response.data.sessionId) : '',
            message: response.data.message,
            response: response.data.response,
            messageType: 'BOT',
            timestamp: response.data.timestamp
          }
          setMessages(prev => [...prev, botMsg])
        } else {
          throw new Error('API returned unsuccessful status')
        }
      } else {
        // User is not logged in, use public chat endpoint
        const response = await apiService.sendChatMessagePublic(text, user?.id)
        if (response.success) {
          const botMsg: ChatMessage = {
            sessionId: response.data.sessionId ? String(response.data.sessionId) : '',
            message: response.data.message,
            response: response.data.response || (response.data as any).text,
            messageType: 'BOT',
            timestamp: response.data.timestamp || new Date().toISOString()
          }
          setMessages(prev => [...prev, botMsg])
        } else {
          throw new Error('API returned unsuccessful status')
        }
      }
    } catch (err) {
      // One silent retry via public endpoint if initial call failed
      try {
        const retry = await apiService.sendChatMessagePublic(text, user?.id)
        if (retry.success) {
          const botMsg: ChatMessage = {
            sessionId: retry.data.sessionId ? String(retry.data.sessionId) : '',
            message: retry.data.message,
          response: retry.data.response || (retry.data as any).text,
            messageType: 'BOT',
            timestamp: retry.data.timestamp || new Date().toISOString()
          }
          setMessages(prev => [...prev, botMsg])
          return
        }
      } catch (_) {
        // fall through to error message below
      }
      const errorMsg: ChatMessage = {
        sessionId: '',
        message: '',
        response: 'Xin lỗi, hiện chưa kết nối được tới AI. Bạn thử lại sau nhé.',
        messageType: 'BOT',
        timestamp: new Date().toISOString()
      }
      setMessages(prev => [...prev, errorMsg])
    } finally {
      setIsLoading(false)
    }
  }

  const openEmojiPicker = () => {
    setShowEmoji(prev => !prev)
  }
  const closeEmojiPicker = () => setShowEmoji(false)
  const addEmoji = (emoji: string) => {
    setInputMessage(prev => prev + emoji)
    closeEmojiPicker()
  }

  const handleOpenTrack = () => setTrackOpen(true)
  const handleCloseTrack = () => setTrackOpen(false)
  const handleSubmitTrack = () => {
    const composed = `Theo dõi đơn hàng\nEmail: ${trackEmail}\nMã đơn: ${trackNumber}`
    setTrackEmail('')
    setTrackNumber('')
    setTrackOpen(false)
    setInputMessage(composed)
  }

  return (
    <>
      {/* Floating Button */}
      {!embedded && (
        <Tooltip title="Chat với AI">
          <IconButton
            onClick={() => setIsOpen(v => !v)}
            sx={{
              position: 'fixed',
              right: 20,
              bottom: 20,
              width: 64,
              height: 64,
              borderRadius: '50%',
              background: headerGradient || GRADIENT_BG,
              color: 'white',
              boxShadow: '0 10px 24px rgba(255, 111, 60, 0.45)',
              border: '2px solid rgba(255,255,255,0.65)',
              zIndex: theme => theme.zIndex.modal,
              '&:hover': {
                transform: 'translateY(-1px) scale(1.03)',
                filter: 'brightness(1.05)'
              }
            }}
          >
            <ChatBubble />
          </IconButton>
        </Tooltip>
      )}

      {/* Chat Window */}
      {isOpen && (
        <Paper
          elevation={10}
          sx={{
            position: embedded ? 'relative' : 'fixed',
            right: embedded ? 'auto' : 20,
            bottom: embedded ? 'auto' : 20,
            width: embedded ? 400 : 380,
            maxWidth: embedded ? '100%' : 400,
            height: isCollapsed ? 64 : 560,
            display: 'flex',
            flexDirection: 'column',
            borderRadius: 4,
            overflow: 'hidden',
            zIndex: theme => theme.zIndex.modal + 1,
            border: '1px solid rgba(0,0,0,0.06)'
          }}
        >
          {/* Header */}
          <Box
            sx={{
              p: 2,
              background: headerGradient || GRADIENT_BG,
              color: 'white',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              boxShadow: 'inset 0 -1px 0 rgba(255,255,255,0.2)'
            }}
          >
            <Box display="flex" alignItems="center" gap={1.5}>
              <Avatar sx={{ bgcolor: 'rgba(255,255,255,0.2)' }}>
                <SmartToy sx={{ color: 'white' }} />
              </Avatar>
              <Typography variant="subtitle1" fontWeight={700}>
                {title || 'Stylist AI Tư Vấn'}
              </Typography>
            </Box>

            <Box>
              <Tooltip title="Theo dõi đơn hàng">
                <IconButton size="small" onClick={handleOpenTrack} sx={{ color: 'white', mr: 0.5 }}>
                  <ReceiptLong />
                </IconButton>
              </Tooltip>
              <Tooltip title={embedded ? (isCollapsed ? 'Mở rộng' : 'Thu gọn') : 'Thu gọn'}>
                <IconButton
                  size="small"
                  onClick={() => {
                    if (embedded) {
                      setIsCollapsed(v => !v)
                    } else {
                      setIsOpen(false) // thu gọn về nút tròn nhỏ
                    }
                  }}
                  sx={{ color: 'white' }}
                >
                  <ExpandMore sx={{ transform: embedded && isCollapsed ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }} />
                </IconButton>
              </Tooltip>
              {!embedded && (
                <IconButton size="small" onClick={() => setIsOpen(false)} sx={{ color: 'white' }}>
                  <Close />
                </IconButton>
              )}
            </Box>
          </Box>

          {/* Messages */}
          {!isCollapsed && (
          <Box sx={{ flex: 1, p: 2, backgroundColor: '#fbfbfb', overflowY: 'auto' }}>
            <List sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              {messages.map((msg, idx) => (
                <ListItem key={idx} sx={{ p: 0, display: 'block' }}>
                  {msg.messageType === 'USER' ? (
                    <Box display="flex" justifyContent="flex-end" mb={1}>
                      <Box sx={{ bgcolor: 'primary.main', color: 'white', px: 1.5, py: 1, borderRadius: 3, borderTopRightRadius: 0, maxWidth: '75%', boxShadow: '0 2px 10px rgba(0,0,0,0.06)' }}>
                        <Typography variant="body2">{msg.message}</Typography>
                      </Box>
                    </Box>
                  ) : (
                    <Box display="flex" alignItems="flex-start" gap={1.25} mb={1}>
                      <Avatar sx={{ bgcolor: 'primary.main' }}>
                        <SmartToy />
                      </Avatar>
                      <Box sx={{ bgcolor: 'grey.100', px: 1.5, py: 1, borderRadius: 3, borderTopLeftRadius: 0, maxWidth: '75%', boxShadow: '0 2px 10px rgba(0,0,0,0.04)' }}>
                        <Typography variant="body2">{msg.response}</Typography>
                      </Box>
                    </Box>
                  )}
                </ListItem>
              ))}

              {isLoading && (
                <ListItem sx={{ p: 0 }}>
                  <Box display="flex" alignItems="center" gap={1.25}>
                    <Avatar sx={{ bgcolor: 'primary.main' }}>
                      <SmartToy />
                    </Avatar>
                    <Box sx={{ bgcolor: 'grey.100', px: 1.5, py: 1, borderRadius: 3, borderTopLeftRadius: 0, display: 'flex', alignItems: 'center', gap: 1 }}>
                      <CircularProgress size={18} />
                      <Typography variant="body2">AI đang trả lời...</Typography>
                    </Box>
                  </Box>
                </ListItem>
              )}
            </List>
            <div ref={messagesEndRef} />
          </Box>
          )}

          {/* Input */}
          {!isCollapsed && (
          <Box sx={{ p: 1.25, borderTop: 1, borderColor: 'divider', display: 'flex', gap: 1, alignItems: 'center', backgroundColor: '#fff', position: 'relative' }}>
            <IconButton onClick={openEmojiPicker} sx={{ color: 'text.secondary' }} aria-label="emoji-picker">
              <InsertEmoticon />
            </IconButton>
            <IconButton sx={{ color: 'text.secondary' }} onClick={handleOpenTrack}>
              <ImageSearch />
            </IconButton>
            <TextField
              size="small"
              fullWidth
              placeholder="Nhập tin nhắn..."
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault()
                  handleSendMessage()
                }
              }}
              sx={{
                '& .MuiInputBase-root': {
                  borderRadius: 3,
                  backgroundColor: 'grey.50'
                }
              }}
            />
            <IconButton
              onClick={handleSendMessage}
              disabled={!inputMessage.trim() || isLoading}
              sx={{
                background: GRADIENT_BG,
                color: 'white',
                borderRadius: 3,
                px: 1.25,
                '&:hover': { filter: 'brightness(1.05)' }
              }}
            >
              <Send />
            </IconButton>

            {showEmoji && (
              <Paper elevation={8} sx={{ position: 'absolute', bottom: '52px', left: 8, p: 1, borderRadius: 2, zIndex: theme => theme.zIndex.modal + 2 }}>
                <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 0.5 }}>
                  {['😀','😁','😂','😊','😎','😍','😘','🤗','🤔','🤩','😅','🥳','😴','😢','😡','🤖','💡','❤️','🚀','🎉','🤝','👌','👍','🙏'].map(em => (
                    <IconButton key={em} onClick={() => addEmoji(em)} size="small">
                      <span style={{ fontSize: 20 }}>{em}</span>
                    </IconButton>
                  ))}
                </Box>
              </Paper>
            )}
          </Box>
          )}
        </Paper>
      )}


      {/* Track Order sheet anchored to the same corner as the chat button */}
      {trackOpen && !embedded && (
        <Paper
          elevation={16}
          sx={{
            position: 'fixed',
            right: 20,
            bottom: 96,
            width: 360,
            borderRadius: 4,
            overflow: 'hidden',
            zIndex: theme => theme.zIndex.modal + 2,
            boxShadow: '0 16px 32px rgba(0,0,0,0.18)',
            border: '1px solid rgba(0,0,0,0.06)'
          }}
        >
          <Box sx={{ p: 1.5, background: GRADIENT_BG, color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <ReceiptLong sx={{ color: 'white' }} />
              <Typography fontWeight={800}>Track order</Typography>
            </Box>
            <IconButton size="small" onClick={handleCloseTrack} sx={{ color: 'white' }}>
              <Close />
            </IconButton>
          </Box>
          <Box sx={{ p: 2, bgcolor: '#fff' }}>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Nhập email và mã đơn hàng để kiểm tra trạng thái.
            </Typography>
            <TextField
              fullWidth
              size="small"
              label="Email"
              margin="dense"
              value={trackEmail}
              onChange={(e) => setTrackEmail(e.target.value)}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                  backgroundColor: 'grey.50',
                  '& fieldset': { borderColor: 'rgba(0,0,0,0.12)' },
                  '&:hover fieldset': { borderColor: 'primary.light' },
                  '&.Mui-focused fieldset': { borderColor: 'primary.main', boxShadow: '0 0 0 2px rgba(99,102,241,0.15)' }
                }
              }}
            />
            <TextField
              fullWidth
              size="small"
              label="Order number"
              margin="dense"
              value={trackNumber}
              onChange={(e) => setTrackNumber(e.target.value)}
              sx={{
                '& .MuiOutlinedInput-root': {
                  borderRadius: 2,
                  backgroundColor: 'grey.50',
                  '& fieldset': { borderColor: 'rgba(0,0,0,0.12)' },
                  '&:hover fieldset': { borderColor: 'primary.light' },
                  '&.Mui-focused fieldset': { borderColor: 'primary.main', boxShadow: '0 0 0 2px rgba(99,102,241,0.15)' }
                }
              }}
            />
            <Button
              fullWidth
              variant="contained"
              onClick={handleSubmitTrack}
              sx={{
                mt: 1.75,
                background: GRADIENT_BG,
                borderRadius: 2,
                boxShadow: '0 8px 16px rgba(255, 111, 60, 0.35)',
                fontWeight: 700
              }}
            >
              Send
            </Button>
          </Box>
        </Paper>
      )}
    </>
  )
}

export default ChatWidget


