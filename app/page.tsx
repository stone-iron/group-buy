'use client';

import { type SyntheticEvent, type WheelEvent as ReactWheelEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowLeft,
  Bell,
  CalendarClock,
  CalendarDays,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Clock3,
  Heart,
  KeyRound,
  LayoutDashboard,
  LoaderCircle,
  LogOut,
  Mail,
  MapPin,
  Menu,
  MessageCircle,
  Minus,
  PackageCheck,
  Plus,
  Radio,
  Search,
  Share2,
  ShieldCheck,
  ShoppingBag,
  Store,
  Truck,
  UserRound,
  Users,
  Video,
  Volume2,
  VolumeX,
  WalletCards,
  X,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Calendar } from '@/components/ui/calendar';
import { Textarea } from '@/components/ui/textarea';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

type Role = 'buyer' | 'seller' | 'admin';
type SignupRole = Exclude<Role, 'admin'>;
type View = 'home' | 'product' | 'orders' | 'seller' | 'seller-product' | 'seller-create' | 'live' | 'live-create' | 'live-watch' | 'live-studio' | 'admin' | 'profile';
type AuthMode = 'login' | 'signup';
type SocialProvider = 'kakao' | 'naver' | 'google';
type ProductFilter = 'all' | 'group' | 'limited' | 'new' | 'closing' | 'food' | 'living';
type SaleType = 'GROUP_BUY' | 'LIMITED_SALE';
type MemberProfile = { id: number; name: string; email: string; role: Role; createdAt?: string };

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';
const OBS_SERVER_URL = process.env.NEXT_PUBLIC_RTMP_URL ?? 'rtmp://localhost:1935';
const LIVE_PLAYER_BASE_URL = process.env.NEXT_PUBLIC_HLS_URL ?? 'http://localhost:8888';
const LIVE_CHAT_WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? API_BASE_URL.replace(/^http/, 'ws');
const TOSS_CLIENT_KEY = process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? '';
const TOSS_PAYMENT_CONTEXT_KEY = 'yeogimoyeo-toss-payment-context';

const products = [
  { id: 1, saleType: 'GROUP_BUY' as const, emoji: '🍊', tone: 'orange', badge: '공동구매', seller: '제주 햇살농원', title: '제주 노지 감귤 5kg 산지직송', origin: 32900, price: 23900, joined: 34, target: 40, time: '05:42:18' },
  { id: 2, saleType: 'GROUP_BUY' as const, emoji: '🥩', tone: 'red', badge: '공동구매', seller: '바른축산', title: '1++ 한우 불고기·국거리 세트', origin: 69000, price: 49800, joined: 72, target: 80, time: '1일 08:21' },
  { id: 3, saleType: 'GROUP_BUY' as const, emoji: '🫙', tone: 'amber', badge: '공동구매', seller: '오늘의 식탁', title: '저온숙성 국산 참기름 2병', origin: 42000, price: 31500, joined: 18, target: 30, time: '2일 14:03' },
  { id: 4, saleType: 'LIMITED_SALE' as const, emoji: '🧻', tone: 'blue', badge: '선착순 한정판매', seller: '매일생활', title: '천연펄프 3겹 화장지 30롤', origin: 27900, price: 19900, joined: 47, target: 50, time: '09:17:44' },
  { id: 5, saleType: 'GROUP_BUY' as const, emoji: '🍚', tone: 'green', badge: '공동구매', seller: '해남 들녘농장', title: '2026년 햅쌀 신동진 10kg', origin: 43800, price: 34900, joined: 56, target: 70, time: '1일 03:28' },
  { id: 6, saleType: 'GROUP_BUY' as const, emoji: '🍎', tone: 'rose', badge: '공동구매', seller: '청송 사과마을', title: '고당도 가정용 홍로 사과 3kg', origin: 35900, price: 26900, joined: 29, target: 40, time: '18:35:10' },
  { id: 7, saleType: 'GROUP_BUY' as const, emoji: '☕', tone: 'brown', badge: '공동구매', seller: '모닝빈 로스터스', title: '스페셜티 원두 3종 골라담기 600g', origin: 39000, price: 28900, joined: 21, target: 35, time: '2일 06:42' },
  { id: 8, saleType: 'LIMITED_SALE' as const, emoji: '🦐', tone: 'teal', badge: '선착순 한정판매', seller: '완도 푸른수산', title: '국내산 손질 새우 1kg 냉동배송', origin: 44500, price: 32900, joined: 63, target: 70, time: '07:54:02' },
  { id: 9, saleType: 'GROUP_BUY' as const, emoji: '🥜', tone: 'purple', badge: '공동구매', seller: '한줌상점', title: '매일견과 프리미엄 30봉 세트', origin: 31900, price: 22900, joined: 38, target: 50, time: '1일 11:09' },
  { id: 10, saleType: 'LIMITED_SALE' as const, emoji: '🫒', tone: 'green', badge: '선착순 한정판매', seller: '지중해 식탁', title: '엑스트라버진 올리브오일 500ml 2병', origin: 56000, price: 39900, joined: 44, target: 60, time: '12:21:37' },
  { id: 11, saleType: 'LIMITED_SALE' as const, emoji: '🧺', tone: 'purple', badge: '선착순 한정판매', seller: '포근한 하루', title: '고농축 캡슐 세탁세제 60개입', origin: 29900, price: 18900, joined: 81, target: 100, time: '2일 00:18' },
  { id: 12, saleType: 'GROUP_BUY' as const, emoji: '🥐', tone: 'amber', badge: '공동구매', seller: '브레드가든', title: '프랑스 버터 크루아상 생지 20개', origin: 29800, price: 21900, joined: 26, target: 30, time: '04:33:51' },
];
type Product = (typeof products)[number] & { imageUrl?: string };
type NewProductInput = { saleType: SaleType; title: string; emoji: string; imageUrl?: string; category: 'food' | 'living'; origin: number; price: number; target: number; time: string; seller: string };
type LiveStatus = 'live' | 'scheduled' | 'pending' | 'rejected' | 'ended';
type LiveBroadcast = { id: number; title: string; seller: string; productId: number; productTitle: string; emoji: string; imageUrl?: string; status: LiveStatus; scheduledAt: string; viewers: number; description: string; rejectionReason?: string; streamKey?: string; startedAt?: number };
type NewLiveInput = Omit<LiveBroadcast, 'id' | 'status' | 'viewers' | 'streamKey'>;
type LiveChatMessage = { id: string; user: string; message: string; sentAt: number };
type DeliveryTrackingEvent = { time: string; location: string; description: string };
type DeliveryTrackingResult = { demoMode: boolean; carrierCode: string; carrierName: string; invoice: string; status: string; complete: boolean; itemName: string; recipient: string; estimatedDelivery: string; message: string; events: DeliveryTrackingEvent[] };
type PurchaseQueueStatus = 'WAITING' | 'PROCESSING' | 'RESERVED' | 'CONFIRMED' | 'EXPIRED' | 'CANCELLED' | 'SOLD_OUT';
type PurchaseQueueTicket = { ticketId: number; productId: number; quantity: number; status: PurchaseQueueStatus; waitingAhead: number; position: number; totalQuantity: number; soldQuantity: number; reservedQuantity: number; remainingQuantity: number; maxPerMember: number; memberRequestedQuantity: number; memberRemainingLimit: number; expiresAt?: string; message: string };
type InventorySnapshot = { productId: number; totalQuantity: number; soldQuantity: number; reservedQuantity: number; remainingQuantity: number; availableQuantity: number };
type GroupBuyCampaignSnapshot = { productId: number; goalQuantity: number; joinedQuantity: number; remainingQuantity: number; goalReached: boolean; status: 'RECRUITING' | 'GOAL_REACHED'; message: string };
type TossPaymentPrepare = { ticketId: number; orderId: string; orderName: string; customerKey: string; amount: number };
type TossPaymentContext = { ticketId: number; user: MemberProfile; role: Role };
type ShippingInformation = { recipientName: string; phone: string; postalCode: string; address: string; addressDetail: string; deliveryMessage: string };
type DaumPostcodeData = { zonecode: string; roadAddress: string; jibunAddress: string; userSelectedType: 'R' | 'J'; bname: string; buildingName: string; apartment: 'Y' | 'N' };
type DaumPostcodeConstructor = new (options: { oncomplete: (data: DaumPostcodeData) => void }) => { open: () => void };
type TossPaymentRequest = { method: 'CARD'; amount: { currency: 'KRW'; value: number }; orderId: string; orderName: string; successUrl: string; failUrl: string; customerEmail?: string; customerName?: string };
type TossPaymentsFactory = (clientKey: string) => { payment: (options: { customerKey: string }) => { requestPayment: (request: TossPaymentRequest) => Promise<void> } };

declare global {
  interface Window {
    TossPayments?: TossPaymentsFactory;
    daum?: { Postcode: DaumPostcodeConstructor };
  }
}

let tossSdkPromise: Promise<void> | null = null;
let daumPostcodeSdkPromise: Promise<void> | null = null;

function loadTossPaymentsSdk() {
  if (typeof window === 'undefined') return Promise.reject(new Error('브라우저에서만 결제할 수 있습니다.'));
  if (window.TossPayments) return Promise.resolve();
  if (tossSdkPromise) return tossSdkPromise;
  tossSdkPromise = new Promise<void>((resolve, reject) => {
    const script = document.createElement('script');
    script.src = 'https://js.tosspayments.com/v2/standard';
    script.async = true;
    script.onload = () => window.TossPayments ? resolve() : reject(new Error('토스 결제 모듈을 불러오지 못했습니다.'));
    script.onerror = () => reject(new Error('토스 결제 모듈을 불러오지 못했습니다.'));
    document.head.appendChild(script);
  });
  return tossSdkPromise;
}

function loadDaumPostcodeSdk() {
  if (typeof window === 'undefined') return Promise.reject(new Error('브라우저에서만 주소를 검색할 수 있습니다.'));
  if (window.daum?.Postcode) return Promise.resolve();
  if (daumPostcodeSdkPromise) return daumPostcodeSdkPromise;
  daumPostcodeSdkPromise = new Promise<void>((resolve, reject) => {
    const existing = document.querySelector<HTMLScriptElement>('script[data-daum-postcode]');
    const script = existing ?? document.createElement('script');
    const handleLoad = () => window.daum?.Postcode ? resolve() : reject(new Error('주소 검색 서비스를 불러오지 못했습니다.'));
    const handleError = () => reject(new Error('주소 검색 서비스에 연결할 수 없습니다.'));
    script.addEventListener('load', handleLoad, { once: true });
    script.addEventListener('error', handleError, { once: true });
    if (!existing) {
      script.src = 'https://t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
      script.async = true;
      script.dataset.daumPostcode = 'true';
      document.head.appendChild(script);
    }
  }).catch((error) => {
    document.querySelector('script[data-daum-postcode]')?.remove();
    daumPostcodeSdkPromise = null;
    throw error;
  });
  return daumPostcodeSdkPromise;
}

const productDetails: Record<number, { description: string; origin: string; delivery: string; shipping: string; highlights: string[] }> = {
  1: { description: '제주 바닷바람과 따뜻한 햇살을 맞고 자란 새콤달콤한 노지 감귤입니다.', origin: '제주특별자치도', delivery: '결제 후 2~3일 이내', shipping: '무료배송', highlights: ['수확 당일 선별·포장', '가정용 실속 구성 5kg', '무른 과일 선별 보상'] },
  2: { description: '1++ 등급 한우의 불고기와 국거리 부위를 한 번에 만나보는 냉장 세트입니다.', origin: '국내산 한우', delivery: '결제 후 1~2일 이내', shipping: '무료 냉장배송', highlights: ['불고기 600g + 국거리 600g', '진공 포장', '축산물 이력번호 제공'] },
  3: { description: '국산 통참깨를 저온에서 천천히 볶아 고소한 향을 살린 참기름입니다.', origin: '참깨 100% 국내산', delivery: '결제 후 2~3일 이내', shipping: '3,000원 · 5만원 이상 무료', highlights: ['저온 압착 방식', '300ml 2병 구성', '선물용 완충 포장'] },
  4: { description: '도톰하고 부드러운 천연펄프 3겹 화장지로 온 가족이 편안하게 사용할 수 있습니다.', origin: '대한민국', delivery: '결제 후 2~4일 이내', shipping: '무료배송', highlights: ['30m 30롤 구성', '무형광·무향', '천연펄프 100%'] },
  5: { description: '해남의 비옥한 들녘에서 수확한 2026년산 신동진 햅쌀입니다.', origin: '전라남도 해남', delivery: '결제 후 1~3일 이내', shipping: '무료배송', highlights: ['당일 도정 선택 가능', '단일 품종 신동진', '10kg 지퍼백 포장'] },
  6: { description: '아삭한 식감과 풍부한 과즙을 가진 청송 홍로 사과를 실속 구성으로 담았습니다.', origin: '경상북도 청송', delivery: '결제 후 2~3일 이내', shipping: '무료배송', highlights: ['평균 당도 14Brix 이상', '가정용 3kg', '개별 완충 포장'] },
  7: { description: '매일 마시기 좋은 세 가지 스페셜티 원두를 취향에 맞게 골라 담는 세트입니다.', origin: '에티오피아·브라질·콜롬비아', delivery: '로스팅 후 1~2일 이내', shipping: '3,000원', highlights: ['원두별 200g 구성', '분쇄도 선택 가능', '주문 후 로스팅'] },
  8: { description: '완도 앞바다에서 잡아 손질한 탱글한 새우를 급속 냉동해 신선함을 지켰습니다.', origin: '전라남도 완도', delivery: '결제 후 1~2일 이내', shipping: '무료 냉동배송', highlights: ['내장 제거·손질 완료', '500g 2팩 구성', '아이스박스 포장'] },
  9: { description: '아몬드와 호두, 캐슈너트, 건과일을 하루 한 봉으로 간편하게 즐기는 견과 세트입니다.', origin: '원재료별 별도 표기', delivery: '결제 후 2~3일 이내', shipping: '무료배송', highlights: ['20g 30봉 구성', '무첨가 로스팅', '개별 소포장'] },
  10: { description: '신선한 올리브를 냉압착한 엑스트라버진 오일로 샐러드와 요리에 잘 어울립니다.', origin: '스페인', delivery: '결제 후 2~4일 이내', shipping: '무료배송', highlights: ['산도 0.3% 이하', '500ml 2병 구성', '빛 차단 유리병'] },
  11: { description: '세탁 한 번에 한 캡슐로 끝내는 고농축 세제로 깔끔하고 은은하게 세탁됩니다.', origin: '대한민국', delivery: '결제 후 2~3일 이내', shipping: '무료배송', highlights: ['60개입 대용량', '찬물 세탁 가능', '드럼·일반 겸용'] },
  12: { description: '프랑스산 버터의 풍미와 겹겹이 살아 있는 결을 집에서 간편하게 즐기는 생지입니다.', origin: '프랑스', delivery: '결제 후 1~2일 이내', shipping: '무료 냉동배송', highlights: ['개당 70g 총 20개', '해동 후 바로 굽기', '지퍼백 소분 포장'] },
};

const productFilters: Record<ProductFilter, { eyebrow: string; title: string; description: string }> = {
  all: { eyebrow: '두 가지 방식으로 만나요', title: '지금 진행 중인 판매', description: '목표를 함께 채우는 공동구매와 빠르게 구매하는 한정판매를 만나보세요.' },
  group: { eyebrow: '함께 모이면 성사돼요', title: '공동구매 상품', description: '목표 수량을 달성한 뒤 참여자에게 결제를 안내해요.' },
  limited: { eyebrow: '수량이 정해져 있어요', title: '선착순 한정판매', description: '구매 요청이 도착한 순서대로 결제 기회를 드려요.' },
  new: { eyebrow: '새롭게 시작했어요', title: '신규 오픈 공동구매', description: '방금 문을 연 새로운 공동구매를 가장 먼저 만나보세요.' },
  closing: { eyebrow: '놓치기 전에 참여하세요', title: '마감 임박 공동구매', description: '오늘 또는 곧 모집이 끝나는 상품을 모았어요.' },
  food: { eyebrow: '신선함을 함께 담아요', title: '식품 상품', description: '산지 먹거리부터 간편한 식탁 상품까지 준비했어요.' },
  living: { eyebrow: '생활비를 가볍게', title: '생활용품', description: '매일 쓰는 생활용품을 더 좋은 가격에 만나보세요.' },
};

const newProductIds = new Set([3, 5, 7, 10]);
const closingProductIds = new Set([1, 4, 8, 12]);
const livingProductIds = new Set([4, 11]);
const appNotifications = [
  { id: 1, icon: '🎯', title: '공동구매 목표 달성 임박', message: '제주 노지 감귤 공동구매가 목표까지 6명 남았어요.', time: '5분 전' },
  { id: 2, icon: '📦', title: '배송 준비 안내', message: '결제가 완료된 주문의 배송 정보를 확인해 주세요.', time: '32분 전' },
  { id: 3, icon: '🌿', title: '여기모여 소식', message: '이번 주 신규 공동구매 상품이 등록되었어요.', time: '2시간 전' },
];
const initialLiveBroadcasts: LiveBroadcast[] = [];

const won = (value: number) => `${value.toLocaleString('ko-KR')}원`;
const MAX_PURCHASE_PER_MEMBER = 3;
const roleLabels: Record<Role, string> = {
  buyer: '구매자',
  seller: '판매자',
  admin: '관리자',
};
const streamKeyFor = (broadcast: LiveBroadcast) => broadcast.streamKey || `live-${broadcast.id}`;

async function liveApi<T>(path = '', options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}/api/live-broadcasts${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...options?.headers },
  });
  const result = await response.json().catch(() => ({})) as T & { message?: string };
  if (!response.ok) throw new Error(result.message ?? '방송 정보를 처리하지 못했습니다.');
  return result;
}

function useLiveChat(broadcastId: number) {
  const [messages, setMessages] = useState<LiveChatMessage[]>([]);
  const [connected, setConnected] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    let active = true;
    let reconnectTimer: number | undefined;

    const connect = () => {
      if (!active) return;
      const socket = new WebSocket(`${LIVE_CHAT_WS_URL}/ws/live/${broadcastId}`);
      socketRef.current = socket;
      socket.onopen = () => active && setConnected(true);
      socket.onmessage = (event) => {
        try {
          const incoming = JSON.parse(String(event.data)) as LiveChatMessage;
          if (!incoming.id || !incoming.message) return;
          setMessages((current) => current.some((message) => message.id === incoming.id) ? current : [...current, incoming]);
        } catch {
          // Ignore malformed messages and keep the chat connection alive.
        }
      };
      socket.onclose = () => {
        if (!active) return;
        setConnected(false);
        reconnectTimer = window.setTimeout(connect, 2000);
      };
      socket.onerror = () => socket.close();
    };

    setMessages([]);
    connect();
    return () => {
      active = false;
      if (reconnectTimer) window.clearTimeout(reconnectTimer);
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [broadcastId]);

  const sendMessage = (user: string, message: string) => {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) return false;
    socket.send(JSON.stringify({ user, message }));
    return true;
  };

  return { messages, connected, sendMessage };
}

export default function Home() {
  const [role, setRole] = useState<Role>('buyer');
  const [selectedRole, setSelectedRole] = useState<Role>('buyer');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [currentUser, setCurrentUser] = useState<MemberProfile | null>(null);
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [authError, setAuthError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [view, setView] = useState<View>('home');
  const [productFilter, setProductFilter] = useState<ProductFilter>('all');
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [likedProductIds, setLikedProductIds] = useState<Set<number>>(() => new Set());
  const [createdProducts, setCreatedProducts] = useState<Product[]>([]);
  const [createdProductCategories, setCreatedProductCategories] = useState<Record<number, 'food' | 'living'>>({});
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [notifications, setNotifications] = useState(appNotifications);
  const [unreadNotificationIds, setUnreadNotificationIds] = useState<Set<number>>(() => new Set([1, 2]));
  const [liveBroadcasts, setLiveBroadcasts] = useState<LiveBroadcast[]>(initialLiveBroadcasts);
  const [subscribedLiveIds, setSubscribedLiveIds] = useState<Set<number>>(() => new Set());
  const [selectedLiveId, setSelectedLiveId] = useState<number | null>(null);
  const [counts, setCounts] = useState<Record<number, number>>(Object.fromEntries(products.map((product) => [product.id, product.joined])));
  const [purchaseQueueTicket, setPurchaseQueueTicket] = useState<PurchaseQueueTicket | null>(null);
  const [isPaymentOpening, setIsPaymentOpening] = useState(false);
  const [isPurchaseCancelling, setIsPurchaseCancelling] = useState(false);
  const [shippingCheckoutOpen, setShippingCheckoutOpen] = useState(false);
  const [shippingInformation, setShippingInformation] = useState<ShippingInformation>({ recipientName: '', phone: '', postalCode: '', address: '', addressDetail: '', deliveryMessage: '' });
  const shippingAddressDetailRef = useRef<HTMLInputElement>(null);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [notice, setNotice] = useState('');
  const activeProducts = useMemo(() => [...createdProducts, ...products].map((product) => ({ ...product, joined: counts[product.id] ?? product.joined })), [counts, createdProducts]);
  const visibleProducts = useMemo(() => activeProducts.filter((product) => {
    if (productFilter === 'group') return product.saleType === 'GROUP_BUY';
    if (productFilter === 'limited') return product.saleType === 'LIMITED_SALE';
    if (productFilter === 'new') return newProductIds.has(product.id) || product.id > products.length;
    if (productFilter === 'closing') return closingProductIds.has(product.id);
    if (productFilter === 'food') return createdProductCategories[product.id] ? createdProductCategories[product.id] === 'food' : !livingProductIds.has(product.id);
    if (productFilter === 'living') return createdProductCategories[product.id] ? createdProductCategories[product.id] === 'living' : livingProductIds.has(product.id);
    return true;
  }), [activeProducts, createdProductCategories, productFilter]);

  useEffect(() => {
    let active = true;
    void Promise.all([
      fetch(`${API_BASE_URL}/api/purchase-queue/inventories`).then(async (response) => {
        if (!response.ok) throw new Error();
        return response.json() as Promise<InventorySnapshot[]>;
      }),
      fetch(`${API_BASE_URL}/api/group-buys`).then(async (response) => {
        if (!response.ok) throw new Error();
        return response.json() as Promise<GroupBuyCampaignSnapshot[]>;
      }),
    ])
      .then(([inventories, campaigns]) => {
        if (!active) return;
        const limitedProductIds = new Set(products.filter((product) => product.saleType === 'LIMITED_SALE').map((product) => product.id));
        setCounts((current) => ({
          ...current,
          ...Object.fromEntries(inventories.filter((inventory) => limitedProductIds.has(inventory.productId)).map((inventory) => [inventory.productId, inventory.soldQuantity])),
          ...Object.fromEntries(campaigns.map((campaign) => [campaign.productId, campaign.joinedQuantity])),
        }));
      })
      .catch(() => undefined);
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paymentResult = params.get('payment');
    if (paymentResult !== 'success' && paymentResult !== 'fail') return;

    const rawContext = window.sessionStorage.getItem(TOSS_PAYMENT_CONTEXT_KEY);
    let context: TossPaymentContext | null = null;
    try {
      context = rawContext ? JSON.parse(rawContext) as TossPaymentContext : null;
    } catch {
      context = null;
    }
    if (context?.user) {
      setRole(context.role);
      setSelectedRole(context.role);
      setCurrentUser(context.user);
      setView('home');
      setIsLoggedIn(true);
    }

    const cleanPaymentUrl = () => {
      window.sessionStorage.removeItem(TOSS_PAYMENT_CONTEXT_KEY);
      window.history.replaceState({}, '', window.location.pathname);
    };
    const ticketId = Number(params.get('ticketId') ?? context?.ticketId);

    if (paymentResult === 'fail') {
      setNotice(`결제가 완료되지 않았습니다. ${params.get('message') ?? '다시 시도해 주세요.'}`);
      if (Number.isFinite(ticketId)) {
        void fetch(`${API_BASE_URL}/api/purchase-queue/${ticketId}`)
          .then((response) => response.ok ? response.json() as Promise<PurchaseQueueTicket> : null)
          .then((ticket) => ticket && setPurchaseQueueTicket(ticket))
          .catch(() => undefined);
      }
      cleanPaymentUrl();
      return;
    }

    const amount = Number(params.get('amount'));
    const paymentKey = params.get('paymentKey');
    const orderId = params.get('orderId');
    if (!Number.isFinite(ticketId) || !Number.isFinite(amount) || amount <= 0 || !paymentKey || !orderId) {
      setNotice('토스 결제 승인 정보가 올바르지 않습니다.');
      cleanPaymentUrl();
      return;
    }

    setIsPaymentOpening(true);
    void fetch(`${API_BASE_URL}/api/payments/toss/confirm`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ticketId, paymentKey, orderId, amount }),
    })
      .then(async (response) => {
        const result = await response.json().catch(() => ({})) as PurchaseQueueTicket & { message?: string };
        if (!response.ok) throw new Error(result.message ?? '토스 결제 승인을 완료하지 못했습니다.');
        setPurchaseQueueTicket(result);
        setCounts((current) => ({ ...current, [result.productId]: result.soldQuantity }));
        setNotice('토스 결제가 완료되어 구매가 최종 확정되었습니다.');
      })
      .catch((error) => setNotice(error instanceof Error ? error.message : '결제 승인 중 오류가 발생했습니다.'))
      .finally(() => {
        setIsPaymentOpening(false);
        cleanPaymentUrl();
      });
  }, []);

  useEffect(() => {
    if (!purchaseQueueTicket || !['WAITING', 'PROCESSING', 'RESERVED'].includes(purchaseQueueTicket.status)) return;
    let active = true;
    const poll = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/api/purchase-queue/${purchaseQueueTicket.ticketId}`);
        const result = await response.json().catch(() => ({})) as PurchaseQueueTicket & { message?: string };
        if (!response.ok) throw new Error(result.message ?? '대기열 상태를 확인하지 못했습니다.');
        if (!active) return;
        setPurchaseQueueTicket(result);
        if (result.status === 'CONFIRMED') {
          setCounts((current) => ({ ...current, [result.productId]: result.soldQuantity }));
          setNotice(`${result.quantity}개 결제가 완료되어 구매가 확정되었습니다.`);
        } else if (result.status === 'SOLD_OUT') {
          setCounts((current) => ({ ...current, [result.productId]: result.soldQuantity }));
          setNotice('앞선 주문으로 한정 수량이 모두 소진되었습니다.');
        } else if (result.status === 'EXPIRED') {
          setNotice('결제 시간이 만료되어 확보 수량이 다음 순번에게 넘어갔습니다.');
        }
      } catch (error) {
        if (active) setNotice(error instanceof Error ? error.message : '대기열 서버에 연결할 수 없습니다.');
      }
    };
    const timer = window.setTimeout(() => void poll(), 300);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [purchaseQueueTicket]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 3000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    const saved = window.localStorage.getItem('yeogimoyeo-live-alerts');
    if (!saved) return;
    try {
      setSubscribedLiveIds(new Set(JSON.parse(saved) as number[]));
    } catch {
      window.localStorage.removeItem('yeogimoyeo-live-alerts');
    }
  }, []);

  useEffect(() => {
    const saved = window.localStorage.getItem('yeogimoyeo-liked-products');
    if (!saved) return;
    try {
      setLikedProductIds(new Set(JSON.parse(saved) as number[]));
    } catch {
      window.localStorage.removeItem('yeogimoyeo-liked-products');
    }
  }, []);

  useEffect(() => {
    if (!isLoggedIn) return;
    const match = window.location.hash.match(/^#live-(\d+)$/);
    if (!match) return;
    const broadcastId = Number(match[1]);
    if (!liveBroadcasts.some((broadcast) => broadcast.id === broadcastId && broadcast.status === 'live')) return;
    setSelectedLiveId(broadcastId);
    setView('live-watch');
  }, [isLoggedIn, liveBroadcasts]);

  useEffect(() => {
    let active = true;
    const refresh = async (migrateLegacy = false) => {
      try {
        let broadcasts = await liveApi<LiveBroadcast[]>();
        if (migrateLegacy && broadcasts.length === 0) {
          const raw = window.localStorage.getItem('yeogimoyeo-live-broadcasts');
          if (raw) {
            const legacy = JSON.parse(raw) as LiveBroadcast[];
            if (Array.isArray(legacy) && legacy.length > 0) {
              broadcasts = await liveApi<LiveBroadcast[]>('/import', { method: 'POST', body: JSON.stringify(legacy) });
            }
          }
        }
        if (active) setLiveBroadcasts(broadcasts);
      } catch (error) {
        if (migrateLegacy && active) setNotice(error instanceof Error ? `방송 서버 연결 실패: ${error.message}` : '방송 서버에 연결할 수 없습니다.');
      }
    };
    void refresh(true);
    const timer = window.setInterval(() => void refresh(), 3000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const socialResult = params.get('social');
    if (!socialResult) return;

    window.history.replaceState({}, '', window.location.pathname);
    if (socialResult === 'error') {
      setAuthError('소셜 로그인에 실패했습니다. 제공자 설정과 동의 항목을 확인해 주세요.');
      return;
    }

    setIsSubmitting(true);
    void fetch(`${API_BASE_URL}/api/members/social-session`, { credentials: 'include' })
      .then(async (response) => {
        const result = await response.json().catch(() => ({})) as { message?: string; id?: number; name?: string; email?: string; role?: string };
        if (!response.ok) throw new Error(result.message ?? '소셜 로그인 정보를 확인할 수 없습니다.');
        const accountRole = result.role?.toLowerCase();
        if (accountRole !== 'buyer' && accountRole !== 'seller' && accountRole !== 'admin') throw new Error('계정 권한 정보를 확인할 수 없습니다.');
        if (typeof result.id !== 'number') throw new Error('회원 정보를 확인할 수 없습니다.');

        setRole(accountRole);
        setSelectedRole(accountRole);
        setCurrentUser({
          id: result.id,
          name: result.name?.trim() || '여기모여 회원',
          email: result.email?.trim() || '',
          role: accountRole,
        });
        setView(accountRole === 'buyer' ? 'home' : accountRole);
        setIsLoggedIn(true);
        window.history.pushState({ liveGroupBuyAuthenticated: true }, '', window.location.href);
        setNotice('소셜 로그인이 완료되었습니다.');
      })
      .catch((error) => setAuthError(error instanceof Error ? error.message : '소셜 로그인에 실패했습니다.'))
      .finally(() => setIsSubmitting(false));
  }, []);

  useEffect(() => {
    const handleBrowserBack = () => {
      if (!isLoggedIn) return;
      setIsLoggedIn(false);
      setCurrentUser(null);
      setView('home');
      setNotice('');
      setNotificationsOpen(false);
    };
    window.addEventListener('popstate', handleBrowserBack);
    return () => window.removeEventListener('popstate', handleBrowserBack);
  }, [isLoggedIn]);

  const login = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthError('');
    const form = new FormData(event.currentTarget);
    const loginId = form.get('email');
    const password = form.get('password');

    if (typeof loginId !== 'string' || !loginId.trim()) {
      setAuthError('가입한 이메일을 입력해 주세요.');
      return;
    }
    if (typeof password !== 'string' || !password) {
      setAuthError('비밀번호를 입력해 주세요.');
      return;
    }

    setIsSubmitting(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/members/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          identifier: loginId.trim(),
          password,
        }),
      });
      const result = await response.json().catch(() => ({})) as { message?: string; id?: number; name?: string; email?: string; role?: string };
      if (!response.ok) throw new Error(result.message ?? '로그인에 실패했습니다.');

      const accountRole = result.role?.toLowerCase();
      if (accountRole !== 'buyer' && accountRole !== 'seller' && accountRole !== 'admin') {
        throw new Error('계정 권한 정보를 확인할 수 없습니다.');
      }
      setRole(accountRole);
      setSelectedRole(accountRole);
      if (typeof result.id !== 'number') throw new Error('회원 정보를 확인할 수 없습니다.');
      setCurrentUser({ id: result.id, name: result.name?.trim() || loginId.trim(), email: result.email?.trim() || loginId.trim(), role: accountRole });
      setView(accountRole === 'buyer' ? 'home' : accountRole);
      setIsLoggedIn(true);
      window.history.pushState({ liveGroupBuyAuthenticated: true }, '', window.location.href);
      setNotice('로그인이 완료되었습니다.');
    } catch (error) {
      setAuthError(error instanceof Error ? error.message : '서버에 연결할 수 없습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const signup = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthError('');
    setPasswordError('');
    setIsSubmitting(true);

    const form = new FormData(event.currentTarget);
    const readField = (name: string) => {
      const value = form.get(name);
      return typeof value === 'string' ? value : '';
    };
    const signupRole: SignupRole = selectedRole === 'seller' ? 'seller' : 'buyer';
    const payload = {
      name: readField('name'),
      email: readField('email'),
      password: readField('password'),
      passwordConfirm: readField('passwordConfirm'),
      role: signupRole.toUpperCase(),
    };

    if (payload.password.length < 8) {
      setPasswordError('비밀번호는 8자 이상 입력해 주세요.');
      setIsSubmitting(false);
      return;
    }
    if (payload.password !== payload.passwordConfirm) {
      setPasswordError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      setIsSubmitting(false);
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/members/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const result = await response.json().catch(() => ({})) as { message?: string; id?: number; name?: string; email?: string; role?: string };
      if (!response.ok) throw new Error(result.message ?? '회원가입에 실패했습니다.');
      if (typeof result.id !== 'number') throw new Error('회원 정보를 확인할 수 없습니다.');

      setRole(signupRole);
      setCurrentUser({ id: result.id, name: result.name?.trim() || payload.name.trim(), email: result.email?.trim() || payload.email.trim(), role: signupRole });
      setView(signupRole === 'buyer' ? 'home' : 'seller');
      setIsLoggedIn(true);
      window.history.pushState({ liveGroupBuyAuthenticated: true }, '', window.location.href);
      setNotice(`${roleLabels[signupRole]} 회원가입이 완료되었습니다.`);
    } catch (error) {
      const message = error instanceof Error ? error.message : '서버에 연결할 수 없습니다.';
      if (message.includes('비밀번호')) setPasswordError(message);
      else setAuthError(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  const changeAuthMode = (mode: AuthMode) => {
    setAuthError('');
    setPasswordError('');
    setAuthMode(mode);
    if (mode === 'signup' && selectedRole === 'admin') setSelectedRole('buyer');
  };

  const socialLogin = (provider: SocialProvider) => {
    setAuthError('');
    setIsSubmitting(true);
    window.location.assign(`${API_BASE_URL}/oauth2/authorization/${provider}`);
  };

  const logout = () => {
    setIsLoggedIn(false);
    setCurrentUser(null);
    setView('home');
    setNotice('');
    setPurchaseQueueTicket(null);
  };

  const participate = async (id: number, title: string, quantity = 1) => {
    if (role !== 'buyer') {
      setNotice('공동구매 참여는 구매자 계정에서만 가능합니다.');
      return;
    }
    if (!currentUser) {
      setNotice('로그인 정보를 확인할 수 없습니다.');
      return;
    }
    const targetProduct = activeProducts.find((product) => product.id === id);
    if (!targetProduct) {
      setNotice('상품 정보를 찾을 수 없습니다.');
      return;
    }

    if (targetProduct.saleType === 'GROUP_BUY') {
      try {
        const response = await fetch(`${API_BASE_URL}/api/group-buys/${id}/participate`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ memberId: currentUser.id, quantity }),
        });
        const result = await response.json().catch(() => ({})) as GroupBuyCampaignSnapshot & { message?: string };
        if (!response.ok) throw new Error(result.message ?? '공동구매에 참여하지 못했습니다.');
        setCounts((current) => ({ ...current, [id]: result.joinedQuantity }));
        setNotice(result.message ?? `‘${title}’ 공동구매 참여 신청이 완료되었습니다.`);
      } catch (error) {
        setNotice(error instanceof Error ? error.message : '공동구매 서버에 연결할 수 없습니다.');
      }
      return;
    }

    if (purchaseQueueTicket && ['WAITING', 'PROCESSING', 'RESERVED'].includes(purchaseQueueTicket.status)) {
      setNotice('현재 구매 요청이 처리 중입니다. 순번을 확인해 주세요.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/purchase-queue`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          productId: id,
          memberId: currentUser.id,
          quantity,
          requestKey: crypto.randomUUID(),
        }),
      });
      const result = await response.json().catch(() => ({})) as PurchaseQueueTicket & { message?: string };
      if (!response.ok) throw new Error(result.message ?? '구매 대기열에 접수하지 못했습니다.');
      setPurchaseQueueTicket(result);
      setNotice(`‘${title}’ 한정판매 구매 요청이 ${result.position}번째 순번으로 접수됐습니다.`);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '구매 대기열 서버에 연결할 수 없습니다.');
    }
  };

  const payForReservedPurchase = async () => {
    if (!purchaseQueueTicket || purchaseQueueTicket.status !== 'RESERVED' || !currentUser) return;
    const reservedTicket = purchaseQueueTicket;
    if (!TOSS_CLIENT_KEY) {
      setNotice('토스페이먼츠 테스트 클라이언트 키를 설정해 주세요.');
      return;
    }
    setIsPaymentOpening(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/payments/toss/prepare`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ticketId: purchaseQueueTicket.ticketId, memberId: currentUser.id, ...shippingInformation }),
      });
      const prepared = await response.json().catch(() => ({})) as TossPaymentPrepare & { message?: string };
      if (!response.ok) throw new Error(prepared.message ?? '토스 결제를 준비하지 못했습니다.');

      await loadTossPaymentsSdk();
      if (!window.TossPayments) throw new Error('토스 결제 모듈을 시작하지 못했습니다.');

      setPurchaseQueueTicket(null);
      window.sessionStorage.setItem(TOSS_PAYMENT_CONTEXT_KEY, JSON.stringify({
        ticketId: purchaseQueueTicket.ticketId,
        user: currentUser,
        role,
      } satisfies TossPaymentContext));
      const successUrl = new URL(window.location.origin + window.location.pathname);
      successUrl.searchParams.set('payment', 'success');
      successUrl.searchParams.set('ticketId', String(purchaseQueueTicket.ticketId));
      const failUrl = new URL(window.location.origin + window.location.pathname);
      failUrl.searchParams.set('payment', 'fail');
      failUrl.searchParams.set('ticketId', String(purchaseQueueTicket.ticketId));

      const payment = window.TossPayments(TOSS_CLIENT_KEY).payment({ customerKey: prepared.customerKey });
      await payment.requestPayment({
        method: 'CARD',
        amount: { currency: 'KRW', value: prepared.amount },
        orderId: prepared.orderId,
        orderName: prepared.orderName,
        successUrl: successUrl.toString(),
        failUrl: failUrl.toString(),
        customerEmail: currentUser.email,
        customerName: currentUser.name,
      });
    } catch (error) {
      window.sessionStorage.removeItem(TOSS_PAYMENT_CONTEXT_KEY);
      setPurchaseQueueTicket(reservedTicket);
      setNotice(error instanceof Error ? error.message : '결제 처리 중 오류가 발생했습니다.');
      setIsPaymentOpening(false);
    }
  };

  const openShippingCheckout = () => {
    let saved: Partial<ShippingInformation> = {};
    try {
      saved = JSON.parse(window.localStorage.getItem('yeogimoyeo-shipping-information') ?? '{}') as Partial<ShippingInformation>;
    } catch {
      saved = {};
    }
    setShippingInformation({
      recipientName: saved.recipientName || currentUser?.name || '',
      phone: saved.phone || '',
      postalCode: saved.postalCode || '',
      address: saved.address || '',
      addressDetail: saved.addressDetail || '',
      deliveryMessage: saved.deliveryMessage || '',
    });
    setShippingCheckoutOpen(true);
    void loadDaumPostcodeSdk().catch(() => undefined);
  };

  const openAddressSearch = async () => {
    try {
      await loadDaumPostcodeSdk();
      if (!window.daum?.Postcode) throw new Error('주소 검색 서비스를 시작하지 못했습니다.');
      new window.daum.Postcode({
        oncomplete: (data) => {
          const baseAddress = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress;
          const extras = data.userSelectedType === 'R'
            ? [data.bname, data.apartment === 'Y' ? data.buildingName : ''].filter(Boolean)
            : [];
          const address = extras.length > 0 ? `${baseAddress} (${extras.join(', ')})` : baseAddress;
          setShippingInformation((current) => ({ ...current, postalCode: data.zonecode, address }));
          window.setTimeout(() => shippingAddressDetailRef.current?.focus(), 0);
        },
      }).open();
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '주소 검색 서비스를 시작하지 못했습니다.');
    }
  };

  const submitShippingCheckout = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    window.localStorage.setItem('yeogimoyeo-shipping-information', JSON.stringify(shippingInformation));
    void payForReservedPurchase();
  };

  const cancelPurchase = async () => {
    if (!purchaseQueueTicket || !currentUser || !['WAITING', 'PROCESSING', 'RESERVED'].includes(purchaseQueueTicket.status)) return;

    setIsPurchaseCancelling(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/purchase-queue/${purchaseQueueTicket.ticketId}/cancel`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ memberId: currentUser.id }),
      });
      const result = await response.json().catch(() => ({})) as PurchaseQueueTicket & { message?: string };
      if (!response.ok) throw new Error(result.message ?? '구매 요청을 취소하지 못했습니다.');
      setCounts((current) => ({ ...current, [result.productId]: result.soldQuantity }));
      setCancelConfirmOpen(false);
      setShippingCheckoutOpen(false);
      setPurchaseQueueTicket(null);
      if (result.status === 'CANCELLED') {
        setNotice('구매 요청을 취소했습니다. 확보된 수량은 다음 순번에게 전달됩니다.');
      } else if (result.status === 'EXPIRED') {
        setNotice('이미 예약이 만료되어 확보된 수량이 다음 순번에게 전달됐습니다.');
      } else {
        setNotice(result.message ?? '구매 요청이 종료되었습니다.');
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '구매 취소 중 오류가 발생했습니다.');
    } finally {
      setIsPurchaseCancelling(false);
    }
  };

  const openProduct = (id: number) => {
    setSelectedProductId(id);
    setView('product');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const openSellerProduct = (id: number) => {
    setSelectedProductId(id);
    setView('seller-product');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const createGroupBuy = async (input: NewProductInput) => {
    const nextId = Math.max(...products.map((product) => product.id), ...createdProducts.map((product) => product.id), 0) + 1;
    try {
      const endpoint = input.saleType === 'GROUP_BUY' ? '/api/group-buys/campaigns' : '/api/purchase-queue/inventories';
      const payload = input.saleType === 'GROUP_BUY'
        ? { productId: nextId, productTitle: input.title, goalQuantity: input.target }
        : { productId: nextId, productTitle: input.title, totalQuantity: input.target, unitPrice: input.price };
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const result = await response.json().catch(() => ({})) as { message?: string };
      if (!response.ok) throw new Error(result.message ?? '판매 상품을 등록하지 못했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '상품 등록 서버에 연결할 수 없습니다.');
      return;
    }
    const product: Product = {
      id: nextId,
      saleType: input.saleType,
      emoji: input.emoji || (input.category === 'food' ? '🥬' : '📦'),
      imageUrl: input.imageUrl,
      tone: input.category === 'food' ? 'green' : 'blue',
      badge: input.saleType === 'GROUP_BUY' ? '신규 공동구매' : '신규 한정판매',
      seller: input.seller,
      title: input.title,
      origin: input.origin,
      price: input.price,
      joined: 0,
      target: input.target,
      time: input.time,
    };
    setCreatedProducts((current) => [product, ...current]);
    setCreatedProductCategories((current) => ({ ...current, [nextId]: input.category }));
    setCounts((current) => ({ ...current, [nextId]: 0 }));
    setView('seller');
    setNotice(input.saleType === 'GROUP_BUY' ? '새 공동구매가 등록되었습니다.' : '새 선착순 한정판매가 등록되었습니다.');
  };

  const refreshLiveBroadcasts = async () => {
    const broadcasts = await liveApi<LiveBroadcast[]>();
    setLiveBroadcasts(broadcasts);
    return broadcasts;
  };

  const createLiveBroadcast = async (input: NewLiveInput) => {
    try {
      const created = await liveApi<LiveBroadcast>('', { method: 'POST', body: JSON.stringify(input) });
      setLiveBroadcasts((current) => [created, ...current.filter((broadcast) => broadcast.id !== created.id)]);
      setView('live');
      setNotice('라이브 방송 신청이 접수되었습니다. 관리자 승인 후 공개됩니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '방송 신청을 저장하지 못했습니다.');
    }
  };

  const updateLiveStatus = async (id: number, status: 'scheduled' | 'rejected', rejectionReason?: string) => {
    try {
      const updated = await liveApi<LiveBroadcast>(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status, rejectionReason }) });
      setLiveBroadcasts((current) => current.map((broadcast) => broadcast.id === id ? updated : broadcast));
      setNotice(status === 'scheduled' ? '라이브 방송을 승인했습니다.' : '라이브 방송 신청을 반려했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '방송 상태를 변경하지 못했습니다.');
    }
  };

  const openLiveBroadcast = (id: number) => {
    setSelectedLiveId(id);
    setView('live-watch');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const openLiveStudio = (id: number) => {
    setSelectedLiveId(id);
    setView('live-studio');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const subscribeLiveBroadcast = async (id: number, title: string) => {
    if (!("Notification" in window)) {
      setNotice('이 브라우저에서는 시스템 알림을 지원하지 않습니다.');
      return;
    }
    let permission = Notification.permission;
    if (permission === 'default') permission = await Notification.requestPermission();
    if (permission !== 'granted') {
      setNotice('브라우저 알림을 허용해야 실제 방송 알림을 받을 수 있어요.');
      return;
    }
    setSubscribedLiveIds((current) => {
      const next = new Set(current);
      next.add(id);
      window.localStorage.setItem('yeogimoyeo-live-alerts', JSON.stringify([...next]));
      return next;
    });
    new Notification('여기모여 알림 신청 완료', { body: `‘${title}’ 방송이 시작되면 알려드릴게요.`, tag: `live-subscribe-${id}` });
    setNotice(`‘${title}’ 방송 알림을 신청했습니다.`);
  };

  const startLiveBroadcast = async (id: number) => {
    const target = liveBroadcasts.find((broadcast) => broadcast.id === id);
    if (!target || target.status !== 'scheduled') return;
    try {
      await liveApi<LiveBroadcast>(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'live' }) });
      await refreshLiveBroadcasts();
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '라이브 방송을 시작하지 못했습니다.');
      return;
    }

    if (subscribedLiveIds.has(id)) {
      const notificationId = Date.now();
      setNotifications((current) => [{ id: notificationId, icon: '🔴', title: '라이브 방송이 시작됐어요', message: `‘${target.title}’ 방송을 지금 시청해 보세요.`, time: '방금' }, ...current]);
      setUnreadNotificationIds((current) => new Set(current).add(notificationId));
      if (Notification.permission === 'granted') {
        new Notification('여기모여 라이브가 시작됐어요', { body: `‘${target.title}’ 방송을 지금 시청해 보세요.`, tag: `live-${id}` });
      }
    }
    setNotice('라이브 방송을 시작했습니다. 구매자 화면에 LIVE로 표시됩니다.');
  };

  const endLiveBroadcast = async (id: number) => {
    try {
      const updated = await liveApi<LiveBroadcast>(`/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'ended' }) });
      setLiveBroadcasts((current) => current.map((broadcast) => broadcast.id === id ? updated : broadcast));
      setNotice('라이브 방송을 종료했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '라이브 방송을 종료하지 못했습니다.');
    }
  };

  const toggleProductLike = (id: number, title: string) => {
    const isRemoving = likedProductIds.has(id);
    setLikedProductIds((current) => {
      const next = new Set(current);
      if (isRemoving) next.delete(id);
      else next.add(id);
      window.localStorage.setItem('yeogimoyeo-liked-products', JSON.stringify([...next]));
      return next;
    });
    setNotice(`‘${title}’ 상품을 찜 목록에서 ${isRemoving ? '삭제했어요.' : '추가했어요.'}`);
  };

  const openProductFilter = (filter: ProductFilter) => {
    setProductFilter(filter);
    setView('home');
    setMobileMenuOpen(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  useEffect(() => {
    const context = (document as Document & {
      modelContext?: {
        registerTool: (tool: {
          name: string;
          title: string;
          description: string;
          inputSchema: object;
          annotations: { readOnlyHint: boolean; untrustedContentHint: boolean };
          execute: (input: unknown) => unknown;
        }, options?: { signal?: AbortSignal }) => void | Promise<void>;
      };
    }).modelContext;
    if (!context?.registerTool) return;

    const lifecycle = new AbortController();
    void Promise.resolve(context.registerTool({
      name: 'join_group_buy',
      title: '공동구매 참여',
      description: '상품 ID로 공동구매에 한 명 참여하고 화면의 진행률을 업데이트합니다.',
      inputSchema: {
        type: 'object',
        properties: { productId: { type: 'number', enum: products.map((product) => product.id) } },
        required: ['productId'],
        additionalProperties: false,
      },
      annotations: { readOnlyHint: false, untrustedContentHint: false },
      async execute(input) {
        const productId = Number((input as { productId?: unknown })?.productId);
        const product = products.find((item) => item.id === productId);
        if (!product) throw new Error('존재하지 않는 상품입니다.');
        const response = await fetch(`${API_BASE_URL}/api/purchase-queue`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ productId, memberId: 0, quantity: 1, requestKey: crypto.randomUUID() }),
        });
        const result = await response.json().catch(() => ({})) as PurchaseQueueTicket & { message?: string };
        if (!response.ok) throw new Error(result.message ?? '구매 대기열에 접수하지 못했습니다.');
        setPurchaseQueueTicket(result);
        setNotice(`‘${product.title}’ 구매 요청이 ${result.position}번째 순번으로 접수됐습니다.`);
        setRole('buyer');
        setSelectedRole('buyer');
        setIsLoggedIn(true);
        setView('home');
        return { productId, ticketId: result.ticketId, status: 'queued', position: result.position, productTitle: product.title };
      },
    }, { signal: lifecycle.signal })).catch(() => undefined);

    return () => lifecycle.abort();
  }, []);

  if (!isLoggedIn) {
    return (
      <AuthPage
        mode={authMode}
        selectedRole={selectedRole}
        error={authError}
        passwordError={passwordError}
        isSubmitting={isSubmitting}
        onModeChange={changeAuthMode}
        onRoleChange={setSelectedRole}
        onClearAuthError={() => setAuthError('')}
        onClearPasswordError={() => setPasswordError('')}
        onLogin={login}
        onSignup={signup}
        onSocialLogin={socialLogin}
      />
    );
  }

  return (
    <main className="min-h-screen bg-[#f7f8fa] text-[#152019]">
      <header className="sticky top-0 z-30 border-b border-black/7 bg-white/95 backdrop-blur">
        <div className="topbar">
          <button className="brand" onClick={() => openProductFilter('all')} aria-label="여기모여 홈">
            <span className="brand-mark">M</span><span>여기모여</span>
          </button>
          <label className="searchbox"><Search size={18} /><input aria-label="상품 검색" placeholder="어떤 상품을 함께 살까요?" /></label>
          <button type="button" className={`mobile-menu-button ${mobileMenuOpen ? 'active' : ''}`} aria-label={mobileMenuOpen ? '메뉴 닫기' : '메뉴 열기'} aria-expanded={mobileMenuOpen} aria-controls="site-navigation" onClick={() => setMobileMenuOpen((open) => !open)}>{mobileMenuOpen ? <X /> : <Menu />}</button>
          <nav className="header-actions" aria-label="사용자 메뉴">
            <div className="notification-wrap">
              <button className={`icon-button ${notificationsOpen ? 'active' : ''}`} aria-label="알림 목록 열기" aria-expanded={notificationsOpen} onClick={() => setNotificationsOpen((open) => !open)}><Bell size={20} />{unreadNotificationIds.size > 0 && <span className="notification-dot" />}</button>
              {notificationsOpen && (
                <section className="notification-panel" aria-label="최근 알림">
                  <div className="notification-heading"><div><strong>알림</strong><span>{unreadNotificationIds.size > 0 ? `읽지 않은 알림 ${unreadNotificationIds.size}개` : '새로운 알림이 없습니다'}</span></div><button type="button" onClick={() => setUnreadNotificationIds(new Set())}>모두 읽음</button></div>
                  <div className="notification-list">
                    {notifications.map((notification) => {
                      const isUnread = unreadNotificationIds.has(notification.id);
                      return <button type="button" className={`notification-item ${isUnread ? 'unread' : ''}`} key={notification.id} onClick={() => setUnreadNotificationIds((current) => { const next = new Set(current); next.delete(notification.id); return next; })}><span className="notification-icon">{notification.icon}</span><span><strong>{notification.title}</strong><small>{notification.message}</small><time>{notification.time}</time></span>{isUnread && <i aria-label="읽지 않음" />}</button>;
                    })}
                  </div>
                  <button className="notification-close" type="button" onClick={() => setNotificationsOpen(false)}>닫기</button>
                </section>
              )}
            </div>
            <span className={`current-role ${role}`}>{roleLabels[role]}</span>
            <button type="button" className={`avatar ${view === 'profile' ? 'active' : ''}`} aria-label={`${currentUser?.name ?? ''} 프로필 열기`} onClick={() => setView('profile')}>{currentUser?.name.charAt(0) || '?'}</button>
            <button className="logout-button" onClick={logout}><LogOut size={16} /> 로그아웃</button>
          </nav>
        </div>
        <div className={`navrow ${mobileMenuOpen ? 'mobile-open' : ''}`} id="site-navigation">
          <nav className="navlinks" aria-label="주요 메뉴">
            <button className={(view === 'home' || view === 'product') && productFilter === 'all' ? 'active' : ''} onClick={() => openProductFilter('all')}>전체 상품</button>
            <button className={(view === 'home' || view === 'product') && productFilter === 'group' ? 'active' : ''} onClick={() => openProductFilter('group')}>공동구매</button>
            <button className={(view === 'home' || view === 'product') && productFilter === 'limited' ? 'active' : ''} onClick={() => openProductFilter('limited')}>한정판매</button>
            <button className={view === 'live' || view === 'live-create' || view === 'live-watch' || view === 'live-studio' ? 'active live-nav-button' : 'live-nav-button'} onClick={() => { setView('live'); setMobileMenuOpen(false); }}><Radio size={15} /> 라이브커머스</button>
            <button className={(view === 'home' || view === 'product') && productFilter === 'new' ? 'active' : ''} onClick={() => openProductFilter('new')}>신규 오픈</button>
            <button className={(view === 'home' || view === 'product') && productFilter === 'closing' ? 'active' : ''} onClick={() => openProductFilter('closing')}>마감 임박</button>
          </nav>
          <div className="role-links">
            <button className={view === 'profile' ? 'active' : ''} onClick={() => { setView('profile'); setMobileMenuOpen(false); }}><UserRound size={16} /> 내 프로필</button>
            {role === 'buyer' && <button className={view === 'orders' ? 'active' : ''} onClick={() => { setView('orders'); setMobileMenuOpen(false); }}><ShoppingBag size={16} /> 내 주문</button>}
            {role === 'seller' && <button className={view === 'seller' || view === 'seller-product' || view === 'seller-create' ? 'active' : ''} onClick={() => { setView('seller'); setMobileMenuOpen(false); }}><Store size={16} /> 판매자센터</button>}
            {role === 'admin' && <button className={view === 'admin' ? 'active' : ''} onClick={() => { setView('admin'); setMobileMenuOpen(false); }}><ShieldCheck size={16} /> 관리자센터</button>}
          </div>
        </div>
      </header>

      {notice && <div className="notice" role="status" key={notice}><PackageCheck size={19} /> {notice}<button onClick={() => setNotice('')}>닫기</button></div>}
      {purchaseQueueTicket && (() => {
        const queueProduct = activeProducts.find((product) => product.id === purchaseQueueTicket.productId);
        const isActive = purchaseQueueTicket.status === 'WAITING' || purchaseQueueTicket.status === 'PROCESSING';
        const isReserved = purchaseQueueTicket.status === 'RESERVED';
        const isConfirmed = purchaseQueueTicket.status === 'CONFIRMED';
        if (isReserved && shippingCheckoutOpen) {
          const orderAmount = (queueProduct?.price ?? 0) * purchaseQueueTicket.quantity;
          return <div className="purchase-queue-overlay" role="dialog" aria-modal="true" aria-labelledby="shipping-checkout-title">
            <form className="shipping-checkout-card" onSubmit={submitShippingCheckout}>
              <div className="shipping-checkout-heading">
                <button type="button" aria-label="구매 가능 화면으로 돌아가기" onClick={() => setShippingCheckoutOpen(false)}><ArrowLeft /></button>
                <div><p>배송정보 입력</p><h2 id="shipping-checkout-title">어디로 보내드릴까요?</h2><span>결제 전에 받는 분의 정보를 확인해 주세요.</span></div>
                <span className="shipping-checkout-icon"><Truck /></span>
              </div>
              <div className="shipping-checkout-product">
                <span className={`shipping-checkout-thumb ${queueProduct?.tone ?? 'green'}`}>{queueProduct?.emoji ?? '📦'}</span>
                <div><small>주문 상품</small><strong>{queueProduct?.title ?? '한정판매 상품'}</strong><span>{purchaseQueueTicket.quantity}개 · {won(orderAmount)}</span></div>
              </div>
              <div className="shipping-checkout-fields">
                <label><span>받는 분</span><Input required maxLength={50} autoComplete="name" value={shippingInformation.recipientName} onChange={(event) => setShippingInformation((current) => ({ ...current, recipientName: event.target.value }))} placeholder="이름을 입력하세요" /></label>
                <label><span>연락처</span><Input required inputMode="tel" autoComplete="tel" pattern="[0-9-]{9,15}" value={shippingInformation.phone} onChange={(event) => setShippingInformation((current) => ({ ...current, phone: event.target.value }))} placeholder="010-0000-0000" /></label>
                <div className="shipping-postal shipping-field"><span>우편번호</span><div className="shipping-postal-row"><Input required readOnly maxLength={10} inputMode="numeric" autoComplete="postal-code" value={shippingInformation.postalCode} placeholder="주소 찾기를 눌러주세요" onClick={() => void openAddressSearch()} /><Button type="button" variant="outline" onClick={() => void openAddressSearch()}><Search size={16} /> 주소 찾기</Button></div></div>
                <label className="shipping-address"><span>주소</span><Input required readOnly maxLength={200} autoComplete="street-address" value={shippingInformation.address} onClick={() => void openAddressSearch()} placeholder="검색한 주소가 자동으로 입력됩니다" /></label>
                <label className="shipping-address-detail"><span>상세주소</span><Input ref={shippingAddressDetailRef} maxLength={200} value={shippingInformation.addressDetail} onChange={(event) => setShippingInformation((current) => ({ ...current, addressDetail: event.target.value }))} placeholder="동·호수 등 상세주소" /></label>
                <label className="shipping-message"><span>배송 요청사항 <small>선택</small></span><Input maxLength={200} value={shippingInformation.deliveryMessage} onChange={(event) => setShippingInformation((current) => ({ ...current, deliveryMessage: event.target.value }))} placeholder="예: 문 앞에 놓아주세요" /></label>
              </div>
              <div className="shipping-checkout-summary"><span>최종 결제금액</span><strong>{won(orderAmount)}</strong></div>
              <p className="shipping-checkout-help"><ShieldCheck /> 입력한 배송정보는 주문 및 배송 목적으로만 사용됩니다.</p>
              <div className="shipping-checkout-actions">
                <Button type="submit" disabled={isPaymentOpening || isPurchaseCancelling}>{isPaymentOpening ? <LoaderCircle className="animate-spin" size={17} /> : <WalletCards size={17} />}{isPaymentOpening ? '토스 결제창 여는 중' : '토스로 결제하기'}</Button>
                <Button type="button" variant="outline" disabled={isPaymentOpening || isPurchaseCancelling} onClick={() => setCancelConfirmOpen(true)}>구매 취소</Button>
              </div>
            </form>
          </div>;
        }
        return <div className="purchase-queue-overlay" role="dialog" aria-modal="true" aria-labelledby="purchase-queue-title">
          <section className={`purchase-queue-card ${purchaseQueueTicket.status.toLowerCase()}`}>
            <div className="purchase-queue-icon">{isActive ? <LoaderCircle /> : isConfirmed || isReserved ? <PackageCheck /> : <ShoppingBag />}</div>
            <p className="purchase-queue-eyebrow">선착순 구매 대기열 · 티켓 #{purchaseQueueTicket.ticketId}</p>
            <h2 id="purchase-queue-title">{isActive ? '안전하게 순서를 확인하고 있어요' : isReserved ? '구매 수량을 임시 확보했어요' : isConfirmed ? '구매가 최종 확정됐어요' : purchaseQueueTicket.status === 'EXPIRED' ? '결제 시간이 만료됐어요' : '준비된 수량이 모두 소진됐어요'}</h2>
            <strong className="purchase-queue-product">{queueProduct?.title ?? '공동구매 상품'} · {purchaseQueueTicket.quantity}개</strong>
            {isActive ? <div className="purchase-queue-position"><span>현재 내 순번</span><strong>{purchaseQueueTicket.position}번째</strong><small>앞에 {purchaseQueueTicket.waitingAhead}개의 요청이 있어요</small></div> : isReserved ? <div className="purchase-queue-timer"><span>구매 가능</span><strong>지금 결제할 수 있어요</strong><small>결제를 오래 미루면 확보된 수량이 자동으로 해제될 수 있어요</small></div> : <div className="purchase-queue-result"><strong>{isConfirmed ? '결제 완료' : purchaseQueueTicket.status === 'EXPIRED' ? '예약 만료' : '품절'}</strong><span>{purchaseQueueTicket.message}</span></div>}
            <div className="purchase-queue-stock"><span>전체 한정 수량 {purchaseQueueTicket.totalQuantity}개</span><strong>남은 수량 {purchaseQueueTicket.remainingQuantity}개</strong></div>
            <div className="purchase-queue-limit"><span>1인 최대 {purchaseQueueTicket.maxPerMember}개</span><strong>내 추가 구매 가능 {purchaseQueueTicket.memberRemainingLimit}개</strong></div>
            <p className="purchase-queue-help">요청은 서버에 도착한 순서대로 한 건씩 처리되며, 재고를 넘겨 결제되지 않습니다.</p>
            {isReserved && <div className="purchase-queue-actions">
              <Button className="purchase-queue-pay" disabled={isPaymentOpening || isPurchaseCancelling} onClick={openShippingCheckout}><ShoppingBag size={17} /> 구매하기</Button>
              <Button type="button" variant="outline" className="purchase-queue-cancel" disabled={isPaymentOpening || isPurchaseCancelling} onClick={() => setCancelConfirmOpen(true)}>구매 취소</Button>
            </div>}
            {!isActive && !isReserved && <Button onClick={() => setPurchaseQueueTicket(null)}>{isConfirmed ? '확인' : '상품 목록으로 돌아가기'}</Button>}
          </section>
        </div>;
      })()}
      <Dialog open={cancelConfirmOpen} onOpenChange={(open) => { if (!isPurchaseCancelling) setCancelConfirmOpen(open); }}>
        <DialogContent className="purchase-cancel-dialog" showCloseButton={false}>
          <DialogHeader className="purchase-cancel-header">
            <div className="purchase-cancel-icon" aria-hidden="true"><ShoppingBag /></div>
            <div>
              <DialogTitle>구매 요청을 취소할까요?</DialogTitle>
              <DialogDescription>취소 후에는 현재 확보한 구매 순서를 되돌릴 수 없어요.</DialogDescription>
            </div>
          </DialogHeader>
          <div className="purchase-cancel-product">
            <span>취소할 상품</span>
            <strong>{purchaseQueueTicket ? activeProducts.find((product) => product.id === purchaseQueueTicket.productId)?.title ?? '한정판매 상품' : '한정판매 상품'}</strong>
            <small>{purchaseQueueTicket?.quantity ?? 0}개</small>
          </div>
          <div className="purchase-cancel-notice">
            <PackageCheck aria-hidden="true" />
            <p><strong>확보된 수량은 다음 순번에게 넘어갑니다.</strong><span>그래도 취소하시려면 아래 구매 취소 버튼을 눌러주세요.</span></p>
          </div>
          <DialogFooter className="purchase-cancel-actions">
            <DialogClose render={<Button type="button" variant="outline" disabled={isPurchaseCancelling} />}>계속 구매</DialogClose>
            <Button type="button" className="purchase-cancel-confirm" disabled={isPurchaseCancelling} onClick={() => void cancelPurchase()}>
              {isPurchaseCancelling && <LoaderCircle className="animate-spin" size={17} />}
              {isPurchaseCancelling ? '취소 처리 중' : '구매 취소'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      {view === 'home' && <BuyerHome products={visibleProducts} filter={productFilter} likedProductIds={likedProductIds} canParticipate={role === 'buyer'} onFilterChange={openProductFilter} onParticipate={participate} onOpenProduct={openProduct} onToggleLike={toggleProductLike} />}
      {view === 'product' && selectedProductId && (
        <ProductDetail
          product={activeProducts.find((product) => product.id === selectedProductId) ?? activeProducts[0]}
          isLiked={likedProductIds.has(selectedProductId)}
          canParticipate={role === 'buyer'}
          onBack={() => setView('home')}
          onParticipate={participate}
          onToggleLike={toggleProductLike}
        />
      )}
      {view === 'orders' && <OrdersPage />}
      {view === 'live' && <LiveCommercePage role={role} currentUserName={currentUser?.name ?? ''} broadcasts={liveBroadcasts} subscribedLiveIds={subscribedLiveIds} onWatch={openLiveBroadcast} onManage={openLiveStudio} onCreate={() => setView('live-create')} onApprove={(id) => updateLiveStatus(id, 'scheduled')} onReject={(id, reason) => updateLiveStatus(id, 'rejected', reason)} onSubscribe={subscribeLiveBroadcast} onStart={startLiveBroadcast} onEnd={endLiveBroadcast} onNotice={setNotice} />}
      {view === 'live-create' && role === 'seller' && <CreateLivePage sellerName={currentUser?.name ?? '판매자'} products={activeProducts} onBack={() => setView('live')} onCreate={createLiveBroadcast} />}
      {view === 'live-watch' && selectedLiveId && (() => { const broadcast = liveBroadcasts.find((item) => item.id === selectedLiveId); if (!broadcast) return null; const product = activeProducts.find((item) => item.id === broadcast.productId); return <LiveWatchPage broadcast={broadcast} broadcasts={liveBroadcasts.filter((item) => item.status === 'live')} product={product} products={activeProducts} currentUserName={currentUser?.name ?? '구매자'} canParticipate={role === 'buyer'} onBack={() => { window.history.replaceState({}, '', `${window.location.pathname}${window.location.search}`); setView('live'); }} onSelectBroadcast={setSelectedLiveId} onParticipate={participate} onNotice={setNotice} />; })()}
      {view === 'live-studio' && selectedLiveId && role === 'seller' && (() => { const broadcast = liveBroadcasts.find((item) => item.id === selectedLiveId); if (!broadcast) return null; const product = activeProducts.find((item) => item.id === broadcast.productId); return <LiveStudioPage broadcast={broadcast} product={product} onBack={() => setView('live')} onEnd={(id) => { endLiveBroadcast(id); setView('live'); }} onNotice={setNotice} />; })()}
      {view === 'profile' && currentUser && <ProfilePage profile={currentUser} products={activeProducts} broadcasts={liveBroadcasts} likedProductIds={likedProductIds} onProfileChange={(profile) => { setCurrentUser(profile); setRole(profile.role); }} onOpenProduct={openProduct} onOpenLive={openLiveBroadcast} onToggleProductLike={toggleProductLike} onNotice={setNotice} />}
      {view === 'seller' && role === 'seller' && <SellerDashboard products={activeProducts} onManageProduct={openSellerProduct} onCreateProduct={() => setView('seller-create')} />}
      {view === 'seller-product' && role === 'seller' && selectedProductId && (
        <SellerProductManagement
          product={activeProducts.find((product) => product.id === selectedProductId) ?? activeProducts[0]}
          onBack={() => setView('seller')}
          onSave={() => setNotice('상품 관리 설정이 저장되었습니다.')}
        />
      )}
      {view === 'seller-create' && role === 'seller' && <CreateGroupBuyPage sellerName={currentUser?.name ?? '판매자'} onBack={() => setView('seller')} onCreate={createGroupBuy} />}
      {view === 'admin' && role === 'admin' && <AdminDashboard />}
    </main>
  );
}

function AuthPage({
  mode,
  selectedRole,
  error,
  passwordError,
  isSubmitting,
  onModeChange,
  onRoleChange,
  onClearAuthError,
  onClearPasswordError,
  onLogin,
  onSignup,
  onSocialLogin,
}: {
  mode: AuthMode;
  selectedRole: Role;
  error: string;
  passwordError: string;
  isSubmitting: boolean;
  onModeChange: (mode: AuthMode) => void;
  onRoleChange: (role: Role) => void;
  onClearAuthError: () => void;
  onClearPasswordError: () => void;
  onLogin: (event: SyntheticEvent<HTMLFormElement>) => void;
  onSignup: (event: SyntheticEvent<HTMLFormElement>) => void;
  onSocialLogin: (provider: SocialProvider) => void;
}) {
  const signupRoleOptions = [
    { value: 'buyer' as const, label: '구매자', description: '공동구매 참여와 주문·배송 조회', icon: <UserRound /> },
    { value: 'seller' as const, label: '판매자', description: '상품·방송·주문과 배송 관리', icon: <Store /> },
  ];

  return (
    <main className="login-screen">
      <section className="login-showcase" aria-label="여기모여 서비스 소개">
        <div className="login-brand"><span className="brand-mark">M</span><span>여기모여</span></div>
        <div className="login-showcase-copy">
          <h1><span>여기 모이면,</span><span>가격이 더 좋아져요.</span></h1>
          <p>실시간 방송을 보며 한정 수량 공동구매에 참여하고, 판매와 운영까지 한곳에서 관리하세요.</p>
        </div>
        <div className="login-live-card"><span className="live-pulse" /> 지금 8,920명이 공동구매에 참여하고 있어요</div>
      </section>

      <section className="login-panel">
        <form className="login-form" noValidate onSubmit={mode === 'login' ? onLogin : onSignup}>
          <div className="login-heading">
            <p>{mode === 'login' ? '다시 만나서 반가워요' : '여기모여에 오신 것을 환영해요'}</p>
            <h2>{mode === 'login' ? '로그인' : '회원가입'}</h2>
            <span>{mode === 'login' ? '가입한 이메일과 비밀번호를 입력하세요.' : '구매자 또는 판매자를 선택해 계정을 만드세요.'}</span>
          </div>

          {mode === 'signup' && (
            <fieldset className="role-fieldset">
              <legend>가입 유형</legend>
              <RadioGroup value={selectedRole} onValueChange={(value) => onRoleChange(value as Role)} className="login-role-grid signup-roles">
                {signupRoleOptions.map((option) => (
                  <Label className={`login-role-card ${selectedRole === option.value ? 'selected' : ''}`} key={option.value}>
                    <RadioGroupItem value={option.value} aria-label={`${option.label} 선택`} />
                    <span className="login-role-icon">{option.icon}</span>
                    <span><strong>{option.label}</strong><small>{option.description}</small></span>
                  </Label>
                ))}
              </RadioGroup>
            </fieldset>
          )}

          <div className="login-fields">
            {mode === 'signup' && (
              <>
                <Label htmlFor="signup-name">이름</Label>
                <Input id="signup-name" name="name" placeholder="이름을 입력하세요" autoComplete="name" maxLength={50} required />
              </>
            )}
            <Label htmlFor="login-email">이메일</Label>
            <Input
              id="login-email"
              name="email"
              type={mode === 'login' ? 'text' : 'email'}
              placeholder={mode === 'login' ? '가입한 이메일을 입력하세요' : 'name@example.com'}
              autoComplete={mode === 'login' ? 'username' : 'email'}
              onChange={onClearAuthError}
              required
            />
            <Label htmlFor="login-password">비밀번호</Label>
            <Input id="login-password" name="password" type="password" placeholder={mode === 'signup' ? '8자 이상 입력하세요' : '비밀번호를 입력하세요'} autoComplete={mode === 'signup' ? 'new-password' : 'current-password'} minLength={mode === 'signup' ? 8 : undefined} maxLength={72} aria-invalid={mode === 'signup' && Boolean(passwordError)} aria-describedby={mode === 'signup' && passwordError ? 'signup-password-error' : undefined} onChange={() => { onClearPasswordError(); onClearAuthError(); }} required />
            {mode === 'signup' && (
              <>
                <Label htmlFor="signup-password-confirm">비밀번호 확인</Label>
                <Input id="signup-password-confirm" name="passwordConfirm" type="password" placeholder="비밀번호를 다시 입력하세요" autoComplete="new-password" minLength={8} maxLength={72} aria-invalid={Boolean(passwordError)} aria-describedby={passwordError ? 'signup-password-error' : undefined} onChange={onClearPasswordError} required />
                {passwordError && <p id="signup-password-error" className="password-error" role="alert">{passwordError}</p>}
              </>
            )}
          </div>

          {error && <p className="auth-error" role="alert">{error}</p>}
          <Button type="submit" className="login-submit" disabled={isSubmitting}>
            {isSubmitting ? '처리 중...' : mode === 'login' ? '로그인' : `${roleLabels[selectedRole]}로 가입하기`}
          </Button>
          {mode === 'login' && (
            <>
              <div className="social-divider"><span>또는 간편 로그인</span></div>
              <div className="social-login-grid" aria-label="소셜 로그인">
                <button type="button" className="social-login-button kakao" onClick={() => onSocialLogin('kakao')} disabled={isSubmitting}><span>K</span>카카오</button>
                <button type="button" className="social-login-button naver" onClick={() => onSocialLogin('naver')} disabled={isSubmitting}><span>N</span>네이버</button>
                <button type="button" className="social-login-button google" onClick={() => onSocialLogin('google')} disabled={isSubmitting}><span>G</span>Google</button>
              </div>
            </>
          )}
          {mode === 'login' ? (
            <p className="auth-switch">아직 계정이 없나요? <button type="button" onClick={() => onModeChange('signup')}>회원가입</button></p>
          ) : (
            <p className="auth-switch">이미 계정이 있나요? <button type="button" onClick={() => onModeChange('login')}>로그인</button></p>
          )}
          {mode === 'login' && <p className="login-demo-note">소셜 계정은 처음 로그인할 때 구매자 계정으로 자동 가입돼요.</p>}
        </form>
      </section>
    </main>
  );
}

function BuyerHome({
  products: items,
  filter,
  likedProductIds,
  canParticipate,
  onFilterChange,
  onParticipate,
  onOpenProduct,
  onToggleLike,
}: {
  products: Product[];
  filter: ProductFilter;
  likedProductIds: Set<number>;
  canParticipate: boolean;
  onFilterChange: (filter: ProductFilter) => void;
  onParticipate: (id: number, title: string) => void;
  onOpenProduct: (id: number) => void;
  onToggleLike: (id: number, title: string) => void;
}) {
  const filterCopy = productFilters[filter];
  const showHomeIntro = filter === 'all' || filter === 'food' || filter === 'living';

  return (
    <div className="page-shell">
      {showHomeIntro && <>
        <section className="hero">
          <img src="/hero-grocery.png" alt="과일과 채소, 빵으로 구성된 공동구매 꾸러미" />
          <div className="hero-overlay" />
          <div className="hero-copy">
            <Badge className="hero-badge">이번 주 추천 공구</Badge>
            <p className="eyebrow">함께 살수록 더 좋은 가격</p>
            <h1>가을 식탁을 채우는<br />산지직송 모음</h1>
            <p>농가에서 바로 보내는 신선한 제철 먹거리를<br className="desktop-only" /> 최대 32% 할인된 가격으로 만나보세요.</p>
            <Button className="hero-button" onClick={() => onFilterChange('food')}>추천 상품 보기 <ChevronRight size={17} /></Button>
          </div>
          <div className="hero-stat"><strong>1,284명</strong><span>오늘 함께 구매했어요</span></div>
        </section>

        <section className="quick-stats" aria-label="서비스 현황">
          <div><Users /><span><strong>8,920명</strong> 지금 참여 중</span></div>
          <div><Clock3 /><span><strong>26개</strong> 오늘 마감</span></div>
          <div><WalletCards /><span><strong>평균 27%</strong> 할인</span></div>
          <div><Truck /><span><strong>전 상품</strong> 배송 추적</span></div>
        </section>
      </>}

      <section className={`product-section ${showHomeIntro ? '' : 'product-section-direct'}`}>
        <div className="section-heading">
          <div>
            <p className="eyebrow dark">{filterCopy.eyebrow}</p>
            <h2>{filterCopy.title}</h2>
            <p className="section-description">{filterCopy.description}</p>
          </div>
          {filter !== 'all' && filter !== 'food' && filter !== 'living' && <button type="button" onClick={() => onFilterChange('all')}>전체보기 <ChevronRight size={17} /></button>}
        </div>
        {(filter === 'all' || filter === 'food' || filter === 'living') && (
          <nav className="product-category-tabs" aria-label="상품 카테고리">
            <button type="button" className={filter === 'all' ? 'active' : ''} aria-pressed={filter === 'all'} onClick={() => onFilterChange('all')}>전체</button>
            <button type="button" className={filter === 'food' ? 'active' : ''} aria-pressed={filter === 'food'} onClick={() => onFilterChange('food')}>식품</button>
            <button type="button" className={filter === 'living' ? 'active' : ''} aria-pressed={filter === 'living'} onClick={() => onFilterChange('living')}>생활</button>
          </nav>
        )}
        <div className="product-grid">
          {items.map((product) => {
            const rate = Math.min(100, Math.round((product.joined / product.target) * 100));
            const remaining = Math.max(0, product.target - product.joined);
            const isGroupBuy = product.saleType === 'GROUP_BUY';
            const isLiked = likedProductIds.has(product.id);
            return (
              <article className="product-card" key={product.id}>
                <div className={`product-visual ${product.tone}`}>
                  <Badge className="product-badge">{product.badge}</Badge>
                  <button type="button" aria-label={`${product.title} ${isLiked ? '찜 해제' : '찜하기'}`} aria-pressed={isLiked} className={`heart ${isLiked ? 'selected' : ''}`} onClick={() => onToggleLike(product.id, product.title)}><Heart size={19} fill={isLiked ? 'currentColor' : 'none'} /></button>
                  <button className="product-visual-link" type="button" onClick={() => onOpenProduct(product.id)} aria-label={`${product.title} 상세보기`}>{product.imageUrl ? <img className="product-uploaded-image" src={product.imageUrl} alt="" /> : <span aria-hidden="true">{product.emoji}</span>}<p>산지·브랜드 직배송</p></button>
                </div>
                <div className="product-body">
                  <p className="seller-name">{product.seller}</p><h3><button className="product-title-link" type="button" onClick={() => onOpenProduct(product.id)}>{product.title}</button></h3>
                  <div className="price-row"><strong>{won(product.price)}</strong><del>{won(product.origin)}</del></div>
                  <div className="progress-copy"><strong>{isGroupBuy ? `${rate}% 달성` : `${remaining}개 남음`}</strong><span>{isGroupBuy ? (remaining > 0 ? `${remaining}개 더 모이면 성사` : '목표 달성 · 결제 안내 예정') : (remaining > 0 ? '요청 순서대로 결제 가능' : '한정 수량 품절')}</span></div>
                  <Progress value={rate} className="deal-progress" />
                  <div className="card-footer"><span><Clock3 size={15} /> {product.time}</span>{canParticipate ? <Button size="sm" disabled={remaining === 0} onClick={() => onParticipate(product.id, product.title)}>{remaining === 0 ? (isGroupBuy ? '목표 달성' : '품절') : (isGroupBuy ? '참여 신청' : '선착순 구매')}</Button> : <Button size="sm" variant="outline" onClick={() => onOpenProduct(product.id)}>상세보기</Button>}</div>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function ProductDetail({ product, isLiked, canParticipate, onBack, onParticipate, onToggleLike }: { product: Product; isLiked: boolean; canParticipate: boolean; onBack: () => void; onParticipate: (id: number, title: string, quantity?: number) => void; onToggleLike: (id: number, title: string) => void }) {
  const [quantity, setQuantity] = useState(1);
  const isGroupBuy = product.saleType === 'GROUP_BUY';
  const detail = productDetails[product.id] ?? { description: '판매자가 새롭게 등록한 공동구매 상품입니다.', origin: '상품 정보 확인 중', delivery: '모집 완료 후 안내', shipping: '상품별 안내', highlights: ['새롭게 등록된 상품', '목표 달성 시 구매 확정', '판매자 직접 배송'] };
  const rate = Math.min(100, Math.round((product.joined / product.target) * 100));
  const remaining = Math.max(0, product.target - product.joined);
  const discount = Math.round(((product.origin - product.price) / product.origin) * 100);

  return (
    <div className="product-detail-shell">
      <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 상품 목록</button>

      <section className="product-detail-main">
        <div className={`detail-visual ${product.tone}`}>
          <Badge className="detail-badge">{product.badge}</Badge>
          <button className={`detail-heart ${isLiked ? 'selected' : ''}`} type="button" aria-label={`${product.title} ${isLiked ? '찜 해제' : '찜하기'}`} aria-pressed={isLiked} onClick={() => onToggleLike(product.id, product.title)}><Heart size={21} fill={isLiked ? 'currentColor' : 'none'} /></button>
          {product.imageUrl ? <img className="detail-product-image" src={product.imageUrl} alt={product.title} /> : <span className="detail-product-emoji" aria-hidden="true">{product.emoji}</span>}
          <p>산지·브랜드 직배송</p>
        </div>

        <div className="detail-purchase">
          <p className="detail-seller"><Store size={16} /> {product.seller}</p>
          <h1>{product.title}</h1>
          <div className="detail-rating"><span>★★★★★</span> 4.9 · 구매 후기 128개</div>
          <p className="detail-description">{detail.description}</p>

          <div className="detail-price">
            <span>{discount}%</span><strong>{won(product.price)}</strong><del>{won(product.origin)}</del>
          </div>

          <div className="detail-progress-card">
            <div><strong>{isGroupBuy ? `${rate}% 달성` : '선착순 한정판매'}</strong><span>{isGroupBuy ? `${product.joined}개 참여 · 목표 ${product.target}개` : `${product.joined}개 판매 · 전체 ${product.target}개`}</span></div>
            <Progress value={rate} className="deal-progress" />
            <div className="detail-progress-bottom"><span>{isGroupBuy ? (remaining > 0 ? `${remaining}개 더 모이면 성사돼요` : '목표 달성 · 참여자 결제 안내 예정') : (remaining > 0 ? `한정 수량 ${remaining}개 남았어요` : '한정 수량이 품절됐어요')}</span><strong><Clock3 size={15} /> {product.time}</strong></div>
          </div>

          <dl className="detail-info-list">
            <div><dt>원산지</dt><dd>{detail.origin}</dd></div>
            <div><dt>배송</dt><dd>{detail.delivery}</dd></div>
            <div><dt>배송비</dt><dd>{detail.shipping}</dd></div>
          </dl>

          <div className="detail-order-row">
            <div className="quantity-control" aria-label="수량 선택">
              <button type="button" aria-label="수량 줄이기" onClick={() => setQuantity((value) => Math.max(1, value - 1))}><Minus size={16} /></button>
              <output>{quantity}</output>
              <button type="button" aria-label="수량 늘리기" disabled={quantity >= Math.min(MAX_PURCHASE_PER_MEMBER, remaining)} onClick={() => setQuantity((value) => Math.min(MAX_PURCHASE_PER_MEMBER, remaining, value + 1))}><Plus size={16} /></button>
            </div>
            <div className="detail-total"><span>총 상품금액</span><strong>{won(product.price * quantity)}</strong></div>
          </div>

          <p className="detail-purchase-limit"><ShieldCheck size={14} /> {isGroupBuy ? '목표 달성 전에는 결제되지 않으며' : '선착순 한정판매는 요청 순서대로 처리되며'}, 계정당 최대 {MAX_PURCHASE_PER_MEMBER}개까지 신청할 수 있어요.</p>

          {canParticipate ? <Button className="detail-join-button" disabled={remaining === 0} onClick={() => onParticipate(product.id, product.title, quantity)}><ShoppingBag size={18} /> {remaining === 0 ? (isGroupBuy ? '공동구매 목표 달성' : '한정 수량 품절') : (isGroupBuy ? '결제 없이 공동구매 참여 신청' : '선착순 구매 요청')}</Button> : <Button className="detail-join-button seller-view-only" disabled><Store size={18} /> 판매자·관리자 계정은 상품 조회만 가능합니다</Button>}
        </div>
      </section>

      <section className="detail-content-grid">
        <article className="detail-content-card">
          <p className="eyebrow dark">PRODUCT POINT</p>
          <h2>이 상품을 추천하는 이유</h2>
          <div className="highlight-list">
            {detail.highlights.map((highlight, index) => <div key={highlight}><span>{index + 1}</span><strong>{highlight}</strong></div>)}
          </div>
        </article>
        <aside className="seller-card">
          <span className="seller-card-avatar">{product.seller.charAt(0)}</span>
          <div><small>판매자</small><h3>{product.seller}</h3><p>꼼꼼한 검수와 안전한 포장으로 보내드려요.</p></div>
          <Badge variant="secondary">판매자 인증</Badge>
        </aside>
      </section>

      <section className="detail-guide">
        <h2>공동구매 안내</h2>
        <div><PackageCheck /><p><strong>목표 달성 후 결제 확정</strong><span>목표 인원이 모이면 주문이 최종 확정됩니다.</span></p></div>
        <div><Truck /><p><strong>판매자 직접 배송</strong><span>상품별 배송 일정에 맞춰 안전하게 출고됩니다.</span></p></div>
        <div><ShieldCheck /><p><strong>안심 환불</strong><span>목표 미달 시 결제 금액은 자동으로 환불됩니다.</span></p></div>
      </section>
    </div>
  );
}

function ProfilePage({ profile: initialProfile, products: allProducts, broadcasts, likedProductIds, onProfileChange, onOpenProduct, onOpenLive, onToggleProductLike, onNotice }: { profile: MemberProfile; products: Product[]; broadcasts: LiveBroadcast[]; likedProductIds: Set<number>; onProfileChange: (profile: MemberProfile) => void; onOpenProduct: (id: number) => void; onOpenLive: (id: number) => void; onToggleProductLike: (id: number, title: string) => void; onNotice: (message: string) => void }) {
  const [profile, setProfile] = useState(initialProfile);
  const [activeProfileTab, setActiveProfileTab] = useState<'account' | 'favorites'>('account');
  const [likedLiveIds, setLikedLiveIds] = useState<Set<number>>(() => new Set());
  const [profileError, setProfileError] = useState('');
  const [passwordChangeError, setPasswordChangeError] = useState('');
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isSavingPassword, setIsSavingPassword] = useState(false);
  const likedProducts = allProducts.filter((product) => likedProductIds.has(product.id));
  const likedBroadcasts = broadcasts.filter((broadcast) => likedLiveIds.has(broadcast.id));

  useEffect(() => {
    const raw = window.localStorage.getItem('yeogimoyeo-liked-live-broadcasts');
    if (!raw) return;
    try {
      setLikedLiveIds(new Set(JSON.parse(raw) as number[]));
    } catch {
      window.localStorage.removeItem('yeogimoyeo-liked-live-broadcasts');
    }
  }, []);

  const removeLiveLike = (broadcast: LiveBroadcast) => {
    setLikedLiveIds((current) => {
      const next = new Set(current);
      next.delete(broadcast.id);
      window.localStorage.setItem('yeogimoyeo-liked-live-broadcasts', JSON.stringify([...next]));
      return next;
    });
    onNotice(`‘${broadcast.title}’ 방송을 찜 목록에서 삭제했습니다.`);
  };

  const readProfile = (result: { id?: number; name?: string; email?: string; role?: string; createdAt?: string }): MemberProfile => {
    const resultRole = result.role?.toLowerCase();
    if (typeof result.id !== 'number' || !result.name || !result.email || (resultRole !== 'buyer' && resultRole !== 'seller' && resultRole !== 'admin')) {
      throw new Error('회원 정보를 확인할 수 없습니다.');
    }
    return { id: result.id, name: result.name, email: result.email, role: resultRole, createdAt: result.createdAt };
  };

  useEffect(() => {
    const controller = new AbortController();
    void fetch(`${API_BASE_URL}/api/members/${initialProfile.id}/profile`, { signal: controller.signal })
      .then(async (response) => {
        const result = await response.json().catch(() => ({})) as { message?: string; id?: number; name?: string; email?: string; role?: string; createdAt?: string };
        if (!response.ok) throw new Error(result.message ?? '프로필을 불러오지 못했습니다.');
        const nextProfile = readProfile(result);
        setProfile(nextProfile);
        onProfileChange(nextProfile);
      })
      .catch((error: unknown) => {
        if (error instanceof DOMException && error.name === 'AbortError') return;
        setProfileError(error instanceof Error ? error.message : '프로필을 불러오지 못했습니다.');
      });
    return () => controller.abort();
  }, [initialProfile.id]);

  const updateProfile = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProfileError('');
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload = {
      name: String(form.get('name') ?? '').trim(),
      email: String(form.get('email') ?? '').trim(),
      currentPassword: String(form.get('currentPassword') ?? ''),
    };
    if (!payload.name || !payload.email || !payload.currentPassword) {
      setProfileError('모든 항목을 입력해 주세요.');
      return;
    }

    setIsSavingProfile(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/members/${profile.id}/profile`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      const result = await response.json().catch(() => ({})) as { message?: string; id?: number; name?: string; email?: string; role?: string; createdAt?: string };
      if (!response.ok) throw new Error(result.message ?? '개인정보를 변경하지 못했습니다.');
      const nextProfile = readProfile(result);
      setProfile(nextProfile);
      onProfileChange(nextProfile);
      formElement.reset();
      onNotice('개인정보가 변경되었습니다.');
    } catch (error) {
      setProfileError(error instanceof Error ? error.message : '개인정보를 변경하지 못했습니다.');
    } finally {
      setIsSavingProfile(false);
    }
  };

  const changePassword = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPasswordChangeError('');
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload = {
      currentPassword: String(form.get('currentPassword') ?? ''),
      newPassword: String(form.get('newPassword') ?? ''),
      newPasswordConfirm: String(form.get('newPasswordConfirm') ?? ''),
    };
    if (payload.newPassword.length < 8) {
      setPasswordChangeError('새 비밀번호는 8자 이상 입력해 주세요.');
      return;
    }
    if (payload.newPassword !== payload.newPasswordConfirm) {
      setPasswordChangeError('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    setIsSavingPassword(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/members/${profile.id}/password`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const result = await response.json().catch(() => ({})) as { message?: string };
        throw new Error(result.message ?? '비밀번호를 변경하지 못했습니다.');
      }
      formElement.reset();
      onNotice('비밀번호가 안전하게 변경되었습니다.');
    } catch (error) {
      setPasswordChangeError(error instanceof Error ? error.message : '비밀번호를 변경하지 못했습니다.');
    } finally {
      setIsSavingPassword(false);
    }
  };

  const joinedAt = profile.createdAt ? new Date(profile.createdAt).toLocaleDateString('ko-KR') : '가입 정보 확인 중';

  return (
    <DashboardShell title="내 프로필" description="계정 정보와 보안 설정을 안전하게 관리하세요." icon={<UserRound />}>
      <nav className="profile-tabs" aria-label="프로필 메뉴">
        <button type="button" className={activeProfileTab === 'account' ? 'active' : ''} onClick={() => setActiveProfileTab('account')}><UserRound size={17} /> 내 정보</button>
        <button type="button" className={activeProfileTab === 'favorites' ? 'active' : ''} onClick={() => setActiveProfileTab('favorites')}><Heart size={17} /> 찜 목록 <span>{likedProducts.length + likedBroadcasts.length}</span></button>
      </nav>
      <div className="profile-layout">
        <aside className="profile-summary">
          <span className="profile-avatar">{profile.name.charAt(0) || '?'}</span>
          <h2>{profile.name}</h2>
          <Badge className={`profile-role ${profile.role}`}>{roleLabels[profile.role]}</Badge>
          <div className="profile-meta">
            <p><Mail size={16} /><span><small>이메일 또는 아이디</small><strong>{profile.email}</strong></span></p>
            <p><CalendarDays size={16} /><span><small>가입일</small><strong>{joinedAt}</strong></span></p>
          </div>
          <p className="profile-safety"><ShieldCheck size={17} /> 개인정보 변경 시 현재 비밀번호를 확인합니다.</p>
        </aside>

        {activeProfileTab === 'account' ? <div className="profile-forms">
          <form className="profile-form-card" key={`${profile.name}-${profile.email}`} onSubmit={updateProfile} noValidate>
            <div className="profile-form-heading"><span><UserRound /></span><div><h2>개인정보 변경</h2><p>이름과 로그인 이메일을 수정할 수 있어요.</p></div></div>
            <div className="profile-fields">
              <Label htmlFor="profile-name">이름</Label>
              <Input id="profile-name" name="name" defaultValue={profile.name} maxLength={50} required />
              <Label htmlFor="profile-email">{profile.role === 'admin' ? '로그인 정보' : '이메일'}</Label>
              <Input id="profile-email" name="email" type={profile.role === 'admin' ? 'text' : 'email'} defaultValue={profile.email} maxLength={190} required />
              <Label htmlFor="profile-current-password">현재 비밀번호</Label>
              <Input id="profile-current-password" name="currentPassword" type="password" placeholder="변경 내용을 저장하려면 입력하세요" autoComplete="current-password" required />
            </div>
            {profileError && <p className="profile-form-error" role="alert">{profileError}</p>}
            <Button type="submit" className="profile-save" disabled={isSavingProfile}>{isSavingProfile ? '저장 중...' : '개인정보 저장'}</Button>
          </form>

          <form className="profile-form-card" onSubmit={changePassword} noValidate>
            <div className="profile-form-heading"><span><KeyRound /></span><div><h2>비밀번호 변경</h2><p>기존 비밀번호와 다른 8자 이상의 비밀번호를 사용하세요.</p></div></div>
            <div className="profile-fields">
              <Label htmlFor="password-current">현재 비밀번호</Label>
              <Input id="password-current" name="currentPassword" type="password" autoComplete="current-password" required />
              <Label htmlFor="password-new">새 비밀번호</Label>
              <Input id="password-new" name="newPassword" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />
              <Label htmlFor="password-new-confirm">새 비밀번호 확인</Label>
              <Input id="password-new-confirm" name="newPasswordConfirm" type="password" minLength={8} maxLength={72} autoComplete="new-password" required />
            </div>
            {passwordChangeError && <p className="profile-form-error" role="alert">{passwordChangeError}</p>}
            <Button type="submit" className="profile-save" disabled={isSavingPassword}>{isSavingPassword ? '변경 중...' : '비밀번호 변경'}</Button>
          </form>
        </div> : <div className="profile-favorites">
          <section className="profile-favorite-section">
            <div className="profile-favorite-heading"><div><span><ShoppingBag size={18} /></span><div><h2>찜한 공동구매</h2><p>관심 있는 상품을 모아봤어요.</p></div></div><Badge variant="secondary">{likedProducts.length}개</Badge></div>
            {likedProducts.length === 0 ? <div className="profile-favorite-empty"><Heart size={28} /><strong>찜한 상품이 없습니다</strong><span>공동구매 목록에서 하트 버튼을 눌러보세요.</span></div> : <div className="profile-favorite-grid">{likedProducts.map((product) => <article className="profile-favorite-card" key={product.id}><div className={`profile-favorite-thumb ${product.tone}`}>{product.imageUrl ? <img src={product.imageUrl} alt="" /> : product.emoji}</div><div><small>{product.seller}</small><strong>{product.title}</strong><span>{won(product.price)} <del>{won(product.origin)}</del></span></div><div className="profile-favorite-actions"><Button size="sm" onClick={() => onOpenProduct(product.id)}>상품 보기</Button><button type="button" aria-label={`${product.title} 찜 해제`} onClick={() => onToggleProductLike(product.id, product.title)}><Heart size={17} fill="currentColor" /></button></div></article>)}</div>}
          </section>

          <section className="profile-favorite-section">
            <div className="profile-favorite-heading"><div><span className="live"><Radio size={18} /></span><div><h2>찜한 라이브</h2><p>다시 보고 싶은 라이브 방송이에요.</p></div></div><Badge variant="secondary">{likedBroadcasts.length}개</Badge></div>
            {likedBroadcasts.length === 0 ? <div className="profile-favorite-empty"><Radio size={28} /><strong>찜한 라이브가 없습니다</strong><span>라이브 시청 화면에서 찜 버튼을 눌러보세요.</span></div> : <div className="profile-favorite-grid">{likedBroadcasts.map((broadcast) => <article className="profile-favorite-card live" key={broadcast.id}><div className="profile-favorite-thumb">{broadcast.imageUrl ? <img src={broadcast.imageUrl} alt="" /> : broadcast.emoji}</div><div><small>{broadcast.seller}</small><strong>{broadcast.title}</strong><span className={`profile-live-state ${broadcast.status}`}>{broadcast.status === 'live' ? '현재 방송 중' : broadcast.status === 'scheduled' ? '방송 예정' : broadcast.status === 'ended' ? '방송 종료' : '방송 준비 중'}</span></div><div className="profile-favorite-actions"><Button size="sm" disabled={broadcast.status !== 'live'} onClick={() => onOpenLive(broadcast.id)}>{broadcast.status === 'live' ? '방송 보기' : '시청 불가'}</Button><button type="button" aria-label={`${broadcast.title} 찜 해제`} onClick={() => removeLiveLike(broadcast)}><Heart size={17} fill="currentColor" /></button></div></article>)}</div>}
          </section>
        </div>}
      </div>
    </DashboardShell>
  );
}

function LiveCommercePage({ role, currentUserName, broadcasts, subscribedLiveIds, onWatch, onManage, onCreate, onApprove, onReject, onSubscribe, onStart, onEnd, onNotice }: { role: Role; currentUserName: string; broadcasts: LiveBroadcast[]; subscribedLiveIds: Set<number>; onWatch: (id: number) => void; onManage: (id: number) => void; onCreate: () => void; onApprove: (id: number) => void; onReject: (id: number, reason: string) => void; onSubscribe: (id: number, title: string) => void; onStart: (id: number) => void; onEnd: (id: number) => void; onNotice: (message: string) => void }) {
  const [rejectingBroadcastId, setRejectingBroadcastId] = useState<number | null>(null);
  const [preparingBroadcastId, setPreparingBroadcastId] = useState<number | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [rejectionError, setRejectionError] = useState('');
  const liveNow = broadcasts.find((broadcast) => broadcast.status === 'live');
  const publicBroadcasts = broadcasts.filter((broadcast) => broadcast.status === 'live' || broadcast.status === 'scheduled');
  const sellerBroadcasts = broadcasts.filter((broadcast) => broadcast.seller === currentUserName);
  const pendingBroadcasts = broadcasts.filter((broadcast) => broadcast.status === 'pending');
  const rejectingBroadcast = broadcasts.find((broadcast) => broadcast.id === rejectingBroadcastId);
  const preparingBroadcast = broadcasts.find((broadcast) => broadcast.id === preparingBroadcastId);
  const statusLabel: Record<LiveStatus, string> = { live: '방송 중', scheduled: '승인 완료', pending: '승인 대기', rejected: '반려', ended: '방송 종료' };

  const copyStreamValue = async (value: string, label: string) => {
    await navigator.clipboard.writeText(value);
    onNotice(`${label}를 복사했습니다.`);
  };

  const openRejectionDialog = (id: number) => {
    setRejectingBroadcastId(id);
    setRejectionReason('');
    setRejectionError('');
  };

  const closeRejectionDialog = () => {
    setRejectingBroadcastId(null);
    setRejectionReason('');
    setRejectionError('');
  };

  const submitRejection = () => {
    const reason = rejectionReason.trim();
    if (!reason) {
      setRejectionError('판매자가 확인할 수 있도록 반려 사유를 입력해 주세요.');
      return;
    }
    if (rejectingBroadcastId === null) return;
    onReject(rejectingBroadcastId, reason);
    closeRejectionDialog();
  };

  return (
    <div className="live-shell">
      <div className="live-page-heading">
        <div><p className="eyebrow dark">LIVE COMMERCE</p><h1>라이브커머스</h1><p>판매자와 실시간으로 소통하고 라이브 전용 공동구매를 만나보세요.</p></div>
        {role === 'seller' && <Button onClick={onCreate}><Video size={17} /> 라이브 방송 신청</Button>}
      </div>

      {liveNow && (
        <section className="live-featured">
          <div className="live-featured-visual">
            {liveNow.imageUrl ? <img src={liveNow.imageUrl} alt="" /> : <span>{liveNow.emoji}</span>}
            <Badge className="live-badge"><i /> LIVE</Badge>
            <div className="live-viewers"><Users size={15} /> {liveNow.viewers.toLocaleString('ko-KR')}명 시청 중</div>
          </div>
          <div className="live-featured-copy"><p>{liveNow.seller}</p><h2>{liveNow.title}</h2><span>{liveNow.description}</span><div className="live-product-line"><span>{liveNow.emoji}</span><div><small>방송 상품</small><strong>{liveNow.productTitle}</strong></div></div>{role === 'seller' && liveNow.seller === currentUserName ? <Button onClick={() => onManage(liveNow.id)}><Video size={17} /> 방송 관리하기</Button> : <Button onClick={() => onWatch(liveNow.id)}><Radio size={17} /> 지금 시청하기</Button>}</div>
        </section>
      )}

      <section className="live-list-section">
        <div className="section-heading"><div><p className="eyebrow dark">LIVE SCHEDULE</p><h2>라이브 일정</h2><p className="section-description">진행 중이거나 관리자의 승인을 받은 방송입니다.</p></div></div>
        <div className="live-card-grid" onWheel={(event) => { const list = event.currentTarget; if (Math.abs(event.deltaY) <= Math.abs(event.deltaX)) return; const canMove = event.deltaY > 0 ? list.scrollLeft < list.scrollWidth - list.clientWidth - 1 : list.scrollLeft > 1; if (!canMove) return; event.preventDefault(); list.scrollLeft += event.deltaY; }}>
          {publicBroadcasts.map((broadcast) => { const isOwnLive = role === 'seller' && broadcast.seller === currentUserName && broadcast.status === 'live'; return <article className="live-card" key={broadcast.id}><div className="live-card-visual">{broadcast.imageUrl ? <img src={broadcast.imageUrl} alt="" /> : <span>{broadcast.emoji}</span>}<Badge className={broadcast.status === 'live' ? 'on-air' : 'scheduled'}>{broadcast.status === 'live' ? 'LIVE' : '예정'}</Badge></div><div className="live-card-body"><p>{broadcast.seller}</p><h3>{broadcast.title}</h3><span><CalendarClock size={14} /> {broadcast.scheduledAt}</span>{broadcast.status === 'live' ? <Button onClick={() => isOwnLive ? onManage(broadcast.id) : onWatch(broadcast.id)}>{isOwnLive ? '방송 관리' : '방송 보기'}</Button> : <Button variant="outline" disabled={subscribedLiveIds.has(broadcast.id)} onClick={() => onSubscribe(broadcast.id, broadcast.title)}>{subscribedLiveIds.has(broadcast.id) ? '알림 신청 완료' : '알림 받기'}</Button>}</div></article>; })}
        </div>
        {publicBroadcasts.length === 0 && <div className="live-public-empty"><Radio /><strong>예정된 라이브 방송이 없습니다.</strong><span>새로운 방송이 승인되면 이곳에 표시됩니다.</span></div>}
      </section>

      {role === 'seller' && (
        <section className="live-role-panel">
          <div className="live-role-heading"><div><h2>내 방송 신청 현황</h2><p>관리자 승인 후 라이브 일정에 공개됩니다.</p></div><Button variant="outline" onClick={onCreate}><Plus size={16} /> 새 방송 신청</Button></div>
          {sellerBroadcasts.length === 0 ? <p className="live-empty">아직 신청한 방송이 없습니다.</p> : sellerBroadcasts.map((broadcast) => (
            <div className={`live-application-row ${broadcast.status === 'rejected' ? 'has-rejection' : ''}`} key={broadcast.id}>
              <span className="live-application-icon">{broadcast.emoji}</span>
              <div>
                <strong>{broadcast.title}</strong>
                <p>{broadcast.productTitle} · {broadcast.scheduledAt}</p>
                {broadcast.status === 'rejected' && broadcast.rejectionReason && <div className="live-rejection-reason"><ShieldCheck size={15} /><span><b>반려 사유</b>{broadcast.rejectionReason}</span></div>}
              </div>
              <div className="live-seller-actions">
                <Badge className={`live-status ${broadcast.status}`}>{statusLabel[broadcast.status]}</Badge>
                {broadcast.status === 'scheduled' && <Button size="sm" onClick={() => setPreparingBroadcastId(broadcast.id)}><Video size={15} /> 방송 준비</Button>}
                {broadcast.status === 'live' && <Button size="sm" onClick={() => onManage(broadcast.id)}><Video size={15} /> 방송 관리</Button>}
              </div>
            </div>
          ))}
        </section>
      )}

      {role === 'admin' && (
        <section className="live-role-panel admin-live-panel">
          <div className="live-role-heading"><div><h2>방송 승인 대기</h2><p>판매자가 신청한 일정과 상품을 검토하세요.</p></div><Badge variant="secondary">{pendingBroadcasts.length}건 대기</Badge></div>
          {pendingBroadcasts.length === 0 ? <p className="live-empty">승인을 기다리는 방송이 없습니다.</p> : pendingBroadcasts.map((broadcast) => <div className="live-approval-row" key={broadcast.id}><span className="live-application-icon">{broadcast.emoji}</span><div><strong>{broadcast.title}</strong><p>{broadcast.seller} · {broadcast.productTitle} · {broadcast.scheduledAt}</p></div><div><Button variant="outline" size="sm" onClick={() => openRejectionDialog(broadcast.id)}>반려</Button><Button size="sm" onClick={() => onApprove(broadcast.id)}>승인</Button></div></div>)}
        </section>
      )}

      <Dialog open={preparingBroadcastId !== null} onOpenChange={(open) => !open && setPreparingBroadcastId(null)}>
        <DialogContent className="live-prep-dialog">
          <DialogHeader>
            <span className="live-prep-icon"><Video /></span>
            <div><DialogTitle>OBS 방송 준비</DialogTitle><DialogDescription>OBS에 아래 서버와 스트림 키를 입력한 뒤 방송을 시작하세요.</DialogDescription></div>
          </DialogHeader>
          <div className="live-prep-target"><small>방송</small><strong>{preparingBroadcast?.title}</strong><span>{preparingBroadcast?.scheduledAt}</span></div>
          <div className="live-stream-field"><span><small>서버</small><code>{OBS_SERVER_URL}</code></span><Button type="button" variant="outline" size="sm" onClick={() => void copyStreamValue(OBS_SERVER_URL, '서버 주소')}>복사</Button></div>
          <div className="live-stream-field"><span><small>스트림 키</small><code>{preparingBroadcast ? streamKeyFor(preparingBroadcast) : ''}</code></span><Button type="button" variant="outline" size="sm" onClick={() => preparingBroadcast && void copyStreamValue(streamKeyFor(preparingBroadcast), '스트림 키')}>복사</Button></div>
          <ol className="live-prep-steps"><li>OBS의 설정 → 방송에서 서비스는 사용자 지정으로 선택하세요.</li><li>위 서버 주소와 스트림 키를 붙여넣고 OBS의 방송 시작을 누르세요.</li><li>영상 송출을 확인한 뒤 아래 라이브 시작을 누르세요.</li></ol>
          <div className="live-preview-address"><Radio size={16} /><span>구매자 영상 주소</span><code>{preparingBroadcast ? `${LIVE_PLAYER_BASE_URL}/${streamKeyFor(preparingBroadcast)}` : ''}</code></div>
          <DialogFooter>
            <DialogClose render={<Button variant="outline" />}>나중에</DialogClose>
            <Button className="live-start-button" onClick={() => { if (!preparingBroadcast) return; onStart(preparingBroadcast.id); setPreparingBroadcastId(null); }}><Radio size={16} /> 라이브 시작</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={rejectingBroadcastId !== null} onOpenChange={(open) => !open && closeRejectionDialog()}>
        <DialogContent className="live-reject-dialog">
          <DialogHeader>
            <span className="live-reject-icon"><ShieldCheck /></span>
            <div><DialogTitle>방송 신청 반려</DialogTitle><DialogDescription>판매자가 수정할 내용을 구체적으로 알려주세요.</DialogDescription></div>
          </DialogHeader>
          <div className="live-reject-target"><small>반려할 방송</small><strong>{rejectingBroadcast?.title}</strong><span>{rejectingBroadcast?.seller} · {rejectingBroadcast?.scheduledAt}</span></div>
          <Label htmlFor="live-rejection-reason">반려 사유</Label>
          <Textarea id="live-rejection-reason" value={rejectionReason} onChange={(event) => { setRejectionReason(event.target.value); setRejectionError(''); }} placeholder="예: 상품 설명에 라이브 혜택과 배송 일정을 추가해 주세요." maxLength={300} rows={5} />
          <div className="live-reject-meta"><span className={rejectionError ? 'error' : ''}>{rejectionError || '입력한 내용은 판매자 화면에 그대로 표시됩니다.'}</span><small>{rejectionReason.length}/300</small></div>
          <DialogFooter>
            <DialogClose render={<Button variant="outline" />}>취소</DialogClose>
            <Button className="live-reject-confirm" onClick={submitRejection}>반려 확정</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function CreateLivePage({ sellerName, products: items, onBack, onCreate }: { sellerName: string; products: Product[]; onBack: () => void; onCreate: (input: NewLiveInput) => void }) {
  const [error, setError] = useState('');
  const [selectedDate, setSelectedDate] = useState<Date>(new Date(2026, 9, 7));
  const [selectedTime, setSelectedTime] = useState('19:00');
  const timeSlots = ['10:00', '11:30', '14:00', '16:00', '19:00', '20:30'];
  const hourOptions = Array.from({ length: 24 }, (_, hour) => String(hour).padStart(2, '0'));
  const minuteOptions = Array.from({ length: 60 }, (_, minute) => String(minute).padStart(2, '0'));
  const [selectedHour, selectedMinute] = selectedTime.split(':');
  const scheduleLabel = `${format(selectedDate, 'M월 d일 (EEE)', { locale: ko })} ${selectedTime}`;

  const changeHour = (hour: string) => setSelectedTime(`${hour}:${selectedMinute}`);
  const changeMinute = (minute: string) => setSelectedTime(`${selectedHour}:${minute}`);

  const submitLive = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const title = String(form.get('title') ?? '').trim();
    const productId = Number(form.get('productId'));
    const date = String(form.get('date') ?? '');
    const time = String(form.get('time') ?? '');
    const description = String(form.get('description') ?? '').trim();
    const product = items.find((item) => item.id === productId);
    if (!title || !product || !date || !time || !description) {
      setError('방송 제목, 상품, 일정과 소개를 모두 입력해 주세요.');
      return;
    }
    onCreate({ title, seller: sellerName, productId: product.id, productTitle: product.title, emoji: product.emoji, imageUrl: product.imageUrl, scheduledAt: `${date.replaceAll('-', '.')} ${time}`, description });
  };

  return (
    <div className="live-create-shell">
      <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 라이브 목록으로 돌아가기</button>
      <div className="seller-create-heading"><span><Video /></span><div><p className="eyebrow dark">SELLER LIVE</p><h1>라이브 방송 신청</h1><p>방송 정보를 등록하면 관리자가 검토한 후 라이브 일정에 공개합니다.</p></div></div>
      <form className="live-create-form" onSubmit={submitLive} noValidate>
        <section className="seller-create-card">
          <div className="seller-setting-title"><div><h2>방송 기본 정보</h2><p>구매자에게 표시될 방송 정보를 입력하세요.</p></div><Radio /></div>
          <div className="live-create-fields">
            <Label htmlFor="live-title">방송 제목</Label><Input id="live-title" name="title" placeholder="예: 제주 농장에서 만나는 감귤 라이브" maxLength={80} />
            <Label htmlFor="live-product">판매 상품</Label><select id="live-product" name="productId" defaultValue=""><option value="" disabled>상품을 선택하세요</option>{items.slice(0, 12).map((product) => <option key={product.id} value={product.id}>{product.title}</option>)}</select>
            <Label htmlFor="live-description">방송 소개</Label><textarea id="live-description" name="description" rows={5} placeholder="방송에서 소개할 내용과 특별 혜택을 적어주세요." />
          </div>
        </section>
        <section className="seller-create-card">
          <div className="seller-setting-title"><div><h2>방송 일정</h2><p>관리자가 확인할 희망 방송 시간을 선택하세요.</p></div><CalendarClock /></div>
          <input type="hidden" name="date" value={format(selectedDate, 'yyyy-MM-dd')} />
          <input type="hidden" name="time" value={selectedTime} />
          <div className="live-schedule-picker">
            <div className="live-schedule-heading"><span>방송 날짜</span><strong>{format(selectedDate, 'yyyy.MM.dd')}</strong></div>
            <Calendar
              mode="single"
              locale={ko}
              selected={selectedDate}
              onSelect={(date) => date && setSelectedDate(date)}
              defaultMonth={selectedDate}
              showOutsideDays={false}
              disabled={{ before: new Date(2026, 9, 1) }}
              className="live-schedule-calendar"
            />
            <div className="live-time-heading"><span>시작 시간</span><small>직접 정하거나 빠르게 선택하세요</small></div>
            <div className="live-custom-time">
              <span>직접 설정</span>
              <label><select aria-label="방송 시작 시" value={selectedHour} onChange={(event) => changeHour(event.target.value)}>{hourOptions.map((hour) => <option value={hour} key={hour}>{hour}</option>)}</select><small>시</small></label>
              <strong>:</strong>
              <label><select aria-label="방송 시작 분" value={selectedMinute} onChange={(event) => changeMinute(event.target.value)}>{minuteOptions.map((minute) => <option value={minute} key={minute}>{minute}</option>)}</select><small>분</small></label>
            </div>
            <p className="live-quick-time-label">빠른 선택</p>
            <div className="live-time-slots">
              {timeSlots.map((time) => (
                <button type="button" className={selectedTime === time ? 'selected' : ''} aria-pressed={selectedTime === time} onClick={() => setSelectedTime(time)} key={time}>{time}</button>
              ))}
            </div>
            <div className="live-schedule-summary"><CalendarDays size={18} /><p><span>신청할 방송 일정</span><strong>{scheduleLabel}</strong></p><ShieldCheck size={18} /></div>
          </div>
          <div className="live-approval-guide"><ShieldCheck /><p><strong>관리자 승인 필요</strong><span>상품 정보와 방송 일정을 검토한 뒤 승인 결과를 알려드려요.</span></p></div>
        </section>
        {error && <p className="seller-create-error" role="alert">{error}</p>}
        <div className="seller-create-actions"><Button type="button" variant="outline" onClick={onBack}>취소</Button><Button type="submit"><Video size={17} /> 방송 승인 신청</Button></div>
      </form>
    </div>
  );
}

function LiveStudioPage({ broadcast, product, onBack, onEnd, onNotice }: { broadcast: LiveBroadcast; product?: Product; onBack: () => void; onEnd: (id: number) => void; onNotice: (message: string) => void }) {
  const [elapsedSeconds, setElapsedSeconds] = useState(() => Math.max(0, Math.floor((Date.now() - (broadcast.startedAt ?? Date.now())) / 1000)));
  const [hiddenMessageIds, setHiddenMessageIds] = useState<Set<string>>(() => new Set());
  const { messages: studioMessages, connected: chatConnected } = useLiveChat(broadcast.id);

  useEffect(() => {
    const timer = window.setInterval(() => setElapsedSeconds(Math.max(0, Math.floor((Date.now() - (broadcast.startedAt ?? Date.now())) / 1000))), 1000);
    return () => window.clearInterval(timer);
  }, [broadcast.startedAt]);

  const elapsedLabel = `${String(Math.floor(elapsedSeconds / 3600)).padStart(2, '0')}:${String(Math.floor((elapsedSeconds % 3600) / 60)).padStart(2, '0')}:${String(elapsedSeconds % 60).padStart(2, '0')}`;
  const copyValue = async (value: string, label: string) => {
    await navigator.clipboard.writeText(value);
    onNotice(`${label}를 복사했습니다.`);
  };

  return (
    <div className="live-studio-shell">
      <div className="live-studio-heading">
        <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 라이브 목록</button>
        <div><span className="live-studio-state"><i /> LIVE</span><strong>{broadcast.title}</strong><small>{elapsedLabel} 방송 중</small></div>
        <Button variant="outline" className="live-studio-end" onClick={() => { if (window.confirm('라이브 방송을 종료할까요?')) onEnd(broadcast.id); }}>방송 종료</Button>
      </div>

      <div className="live-studio-grid">
        <section className="live-studio-preview">
          <iframe src={`${LIVE_PLAYER_BASE_URL}/${streamKeyFor(broadcast)}?autoplay=true&muted=true&controls=true`} title={`${broadcast.title} 판매자 미리보기`} allow="autoplay; fullscreen" />
          <div><span><Video size={16} /> 판매자 미리보기 · 음소거</span><span><Users size={16} /> {broadcast.viewers.toLocaleString('ko-KR')}명 시청 중</span></div>
        </section>

        <aside className="live-studio-chat">
          <div className="live-studio-panel-title"><MessageCircle size={18} /><div><strong>실시간 채팅 관리</strong><small>{chatConnected ? `${studioMessages.length}개 메시지 · 실시간 연결됨` : '채팅 서버 연결 중...'}</small></div></div>
          <div className="live-studio-message-list">
            {studioMessages.map((item) => hiddenMessageIds.has(item.id) ? null : <div key={item.id}><span><b>{item.user}</b><button type="button" onClick={() => setHiddenMessageIds((current) => new Set(current).add(item.id))}>숨기기</button></span><p>{item.message}</p></div>)}
            {studioMessages.every((item) => hiddenMessageIds.has(item.id)) && <p className="live-studio-chat-empty">아직 등록된 채팅이 없습니다.</p>}
          </div>
        </aside>

        <section className="live-studio-controls">
          <div className="live-studio-panel-title"><Radio size={18} /><div><strong>송출 상태</strong><small>MediaMTX와 OBS 연결 정보</small></div><Badge>정상 송출 중</Badge></div>
          <div className="live-studio-stream-row"><span><small>OBS 서버</small><code>{OBS_SERVER_URL}</code></span><button type="button" onClick={() => void copyValue(OBS_SERVER_URL, 'OBS 서버 주소')}>복사</button></div>
          <div className="live-studio-stream-row"><span><small>스트림 키</small><code>{streamKeyFor(broadcast)}</code></span><button type="button" onClick={() => void copyValue(streamKeyFor(broadcast), '스트림 키')}>복사</button></div>
          <p className="live-studio-help">OBS 송출을 먼저 종료한 다음 사이트의 방송 종료 버튼을 눌러주세요.</p>
        </section>

        <section className="live-studio-product">
          <div className="live-studio-panel-title"><ShoppingBag size={18} /><div><strong>방송 상품 관리</strong><small>구매자 화면에 노출 중인 상품</small></div></div>
          {product ? <div className="live-studio-product-row"><span className={`live-buy-thumb ${product.tone}`}>{product.imageUrl ? <img src={product.imageUrl} alt="" /> : product.emoji}</span><div><strong>{product.title}</strong><small>{won(product.price)} · 현재 {product.joined}명 참여</small></div><Button variant="outline" onClick={() => onNotice('상품 정보가 구매자 화면에 정상 노출되고 있습니다.')}>노출 확인</Button></div> : <p className="live-studio-chat-empty">연결된 상품을 찾을 수 없습니다.</p>}
        </section>
      </div>
    </div>
  );
}

function LiveWatchPage({ broadcast, broadcasts, product, products: liveProducts, currentUserName, canParticipate, onBack, onSelectBroadcast, onParticipate, onNotice }: { broadcast: LiveBroadcast; broadcasts: LiveBroadcast[]; product?: Product; products: Product[]; currentUserName: string; canParticipate: boolean; onBack: () => void; onSelectBroadcast: (id: number) => void; onParticipate: (id: number, title: string) => void; onNotice: (message: string) => void }) {
  const { messages, connected, sendMessage: sendChatMessage } = useLiveChat(broadcast.id);
  const orderedProducts = useMemo(() => product ? [product, ...liveProducts.filter((item) => item.id !== product.id)] : liveProducts, [liveProducts, product]);
  const [sideTab, setSideTab] = useState<'products' | 'intro' | 'benefits'>('products');
  const [likedBroadcastIds, setLikedBroadcastIds] = useState<Set<number>>(() => new Set());
  const [isMuted, setIsMuted] = useState(false);
  const wheelLockedRef = useRef(false);
  const currentBroadcastIndex = broadcasts.findIndex((item) => item.id === broadcast.id);
  const hasPreviousBroadcast = currentBroadcastIndex > 0;
  const hasNextBroadcast = currentBroadcastIndex >= 0 && currentBroadcastIndex < broadcasts.length - 1;
  const isLiked = likedBroadcastIds.has(broadcast.id);
  const liveProductDetail = product ? (productDetails[product.id] ?? { description: '판매자가 엄선한 라이브 공동구매 상품입니다.', origin: '상품 정보 확인 중', delivery: '모집 완료 후 안내', shipping: '상품별 안내', highlights: ['라이브에서 자세히 소개해 드려요', '목표 달성 시 공동구매 확정', '판매자 직접 배송'] }) : null;
  const liveDiscountRate = product ? Math.max(0, Math.round(((product.origin - product.price) / product.origin) * 100)) : 0;

  useEffect(() => {
    const raw = window.localStorage.getItem('yeogimoyeo-liked-live-broadcasts');
    if (!raw) return;
    try {
      setLikedBroadcastIds(new Set(JSON.parse(raw) as number[]));
    } catch {
      window.localStorage.removeItem('yeogimoyeo-liked-live-broadcasts');
    }
  }, []);

  const moveBroadcast = (direction: -1 | 1) => {
    const nextIndex = currentBroadcastIndex + direction;
    const next = broadcasts[nextIndex];
    if (!next) return;
    onSelectBroadcast(next.id);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleBroadcastWheel = (event: ReactWheelEvent<HTMLElement>) => {
    if (broadcasts.length < 2 || Math.abs(event.deltaY) < 25 || wheelLockedRef.current) return;
    const direction = event.deltaY > 0 ? 1 : -1;
    if ((direction === 1 && !hasNextBroadcast) || (direction === -1 && !hasPreviousBroadcast)) return;
    event.preventDefault();
    wheelLockedRef.current = true;
    moveBroadcast(direction);
    window.setTimeout(() => { wheelLockedRef.current = false; }, 700);
  };

  const toggleLiveLike = () => {
    setLikedBroadcastIds((current) => {
      const next = new Set(current);
      if (next.has(broadcast.id)) next.delete(broadcast.id);
      else next.add(broadcast.id);
      window.localStorage.setItem('yeogimoyeo-liked-live-broadcasts', JSON.stringify([...next]));
      return next;
    });
    onNotice(isLiked ? '라이브 찜을 해제했습니다.' : '라이브를 찜 목록에 추가했습니다.');
  };

  const shareLive = async () => {
    const shareUrl = new URL(window.location.href);
    shareUrl.hash = `live-${broadcast.id}`;
    try {
      if (navigator.share) {
        await navigator.share({ title: broadcast.title, text: `${broadcast.seller}의 여기모여 라이브를 시청해 보세요.`, url: shareUrl.toString() });
      } else {
        await navigator.clipboard.writeText(shareUrl.toString());
        onNotice('방송 링크를 복사했습니다.');
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === 'AbortError') return;
      onNotice('공유하지 못했습니다. 다시 시도해 주세요.');
    }
  };

  const toggleMute = () => {
    setIsMuted((current) => !current);
    onNotice(isMuted ? '방송 음량을 켰습니다.' : '방송을 음소거했습니다.');
  };

  const sendMessage = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const message = String(data.get('message') ?? '').trim();
    if (!message) return;
    if (sendChatMessage(currentUserName, message)) form.reset();
  };

  return (
    <div className="live-watch-shell">
      <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 라이브 목록</button>
      <header className="live-watch-heading">
        <span className="live-watch-seller">{broadcast.seller.slice(0, 1)}</span>
        <div><h1>{broadcast.title}</h1><p><b>LIVE</b><span>{broadcast.viewers.toLocaleString('ko-KR')}명 시청 중</span></p></div>
      </header>
      <div className="live-watch-grid">
        <main className="live-stage-area" onWheel={handleBroadcastWheel}>
          <section className="live-player">
            <iframe src={`${LIVE_PLAYER_BASE_URL}/${streamKeyFor(broadcast)}?autoplay=true&muted=${isMuted}&controls=true`} title={`${broadcast.title} 실시간 방송`} allow="autoplay; fullscreen" />
            <Badge className="live-badge"><i /> LIVE</Badge>
            {broadcasts.length > 1 && <div className="live-wheel-capture" onWheel={handleBroadcastWheel} aria-hidden="true" />}
            <div className="live-player-overlay"><strong>{broadcast.seller}</strong><span><Users size={15} /> {broadcast.viewers.toLocaleString('ko-KR')}명</span></div>
          </section>
          <nav className="live-action-rail" aria-label="라이브 빠른 기능">
            {broadcasts.length > 1 && <div className="live-broadcast-switcher"><button type="button" aria-label="이전 방송" disabled={!hasPreviousBroadcast} onClick={() => moveBroadcast(-1)}><ChevronUp size={19} /></button><span>{currentBroadcastIndex + 1}/{broadcasts.length}</span><button type="button" aria-label="다음 방송" disabled={!hasNextBroadcast} onClick={() => moveBroadcast(1)}><ChevronDown size={19} /></button></div>}
            <button type="button" className={isLiked ? 'active' : ''} aria-pressed={isLiked} aria-label={isLiked ? '라이브 찜 해제' : '라이브 찜'} onClick={toggleLiveLike}><Heart size={21} fill={isLiked ? 'currentColor' : 'none'} /><small>{isLiked ? '찜 완료' : '찜'}</small></button>
            <button type="button" aria-label="공유" onClick={() => void shareLive()}><Share2 size={21} /><small>공유</small></button>
            <button type="button" className={isMuted ? 'active' : ''} aria-pressed={isMuted} aria-label={isMuted ? '음량 켜기' : '음소거'} onClick={toggleMute}>{isMuted ? <VolumeX size={22} /> : <Volume2 size={22} />}<small>{isMuted ? '음소거' : '음량'}</small></button>
          </nav>
        </main>
        <aside className="live-commerce-side">
          <section className="live-side-card live-product-panel">
            <div className="live-side-tabs" role="tablist">
              <button type="button" role="tab" aria-selected={sideTab === 'products'} className={sideTab === 'products' ? 'active' : ''} onClick={() => setSideTab('products')}>상품목록 <em>{orderedProducts.length}</em></button>
              <button type="button" role="tab" aria-selected={sideTab === 'intro'} className={sideTab === 'intro' ? 'active' : ''} onClick={() => setSideTab('intro')}>라이브 소개</button>
              <button type="button" role="tab" aria-selected={sideTab === 'benefits'} className={sideTab === 'benefits' ? 'active' : ''} onClick={() => setSideTab('benefits')}>혜택</button>
            </div>
            {sideTab === 'products' && <div className="live-side-product-list" role="tabpanel">
              {orderedProducts.length === 0 ? <p className="live-side-empty">등록된 상품이 없습니다.</p> : orderedProducts.map((item, index) => (
                <article className={`live-side-product ${item.id === product?.id ? 'featured' : ''}`} key={item.id}>
                  <div className={`live-side-product-thumb ${item.tone}`}>{item.imageUrl ? <img src={item.imageUrl} alt="" /> : item.emoji}</div>
                  <div><small>{index === 0 && item.id === product?.id ? '방송 대표 상품' : item.seller}</small><strong>{item.title}</strong><del>{won(item.origin)}</del><b>{won(item.price)}</b></div>
                  <button type="button" disabled={!canParticipate || item.joined >= item.target} onClick={() => canParticipate && onParticipate(item.id, item.title)}>{!canParticipate ? '보기' : item.joined >= item.target ? '품절' : '참여'}</button>
                </article>
              ))}
            </div>}
            {sideTab === 'intro' && <div className="live-side-intro" role="tabpanel">
              <div className="live-side-intro-heading"><span className="live-side-intro-seller">{broadcast.seller.slice(0, 1)}</span><div><small>{broadcast.seller}</small><strong>{broadcast.title}</strong></div></div>
              <p>{broadcast.description || '방송에서 대표 상품의 특징과 공동구매 혜택을 자세히 소개합니다.'}</p>
              {product && liveProductDetail && <><div className="live-side-intro-product"><span className={`live-side-product-thumb ${product.tone}`}>{product.imageUrl ? <img src={product.imageUrl} alt="" /> : product.emoji}</span><div><small>대표 상품</small><strong>{product.title}</strong></div></div><p className="live-side-product-description">{liveProductDetail.description}</p><ul>{liveProductDetail.highlights.map((highlight) => <li key={highlight}><ShieldCheck size={14} /> {highlight}</li>)}</ul></>}
            </div>}
            {sideTab === 'benefits' && <div className="live-side-benefits" role="tabpanel">
              {product ? <><article><span><WalletCards size={18} /></span><div><small>라이브 공동구매가</small><strong>{liveDiscountRate}% 할인 · {won(product.price)}</strong><p>정상가 {won(product.origin)}에서 할인된 가격이에요.</p></div></article><article><span><Users size={18} /></span><div><small>목표 달성 혜택</small><strong>{product.joined}명 참여 · 목표 {product.target}명</strong><p>목표 인원이 모이면 공동구매 가격으로 주문이 확정돼요.</p></div></article><article><span><Truck size={18} /></span><div><small>배송 혜택</small><strong>{liveProductDetail?.shipping ?? '상품별 안내'}</strong><p>{liveProductDetail?.delivery ?? '모집 완료 후 배송 일정을 안내해 드려요.'}</p></div></article></> : <p className="live-side-empty">혜택을 확인할 대표 상품이 없습니다.</p>}
            </div>}
          </section>
          <section className="live-side-card live-chat">
            <div className="live-chat-heading"><MessageCircle size={18} /><strong>실시간 채팅</strong><i className={connected ? 'connected' : ''}>{connected ? '연결됨' : '연결 중'}</i><span>{messages.length}</span></div>
            <div className="live-chat-messages">
              {messages.length === 0 ? <div className="live-chat-empty"><MessageCircle size={27} /><strong>아직 채팅이 없습니다</strong><span>{connected ? '첫 메시지를 남겨보세요.' : '채팅 서버에 연결하고 있습니다.'}</span></div> : messages.map((item) => <p key={item.id}><span>{item.user}</span>{item.message}</p>)}
            </div>
            <form onSubmit={sendMessage}><Input name="message" placeholder={connected ? '실시간 채팅에 참여하세요' : '채팅 서버 연결 중...'} maxLength={100} disabled={!connected} /><Button type="submit" disabled={!connected}>전송</Button></form>
          </section>
          <button className="live-faq-button" type="button"><span>자주 묻는 질문</span><ChevronRight size={18} /></button>
        </aside>
      </div>
    </div>
  );
}

function OrdersPage() {
  const [trackingOpen, setTrackingOpen] = useState(false);
  const [trackingResult, setTrackingResult] = useState<DeliveryTrackingResult | null>(null);
  const [trackingError, setTrackingError] = useState('');
  const [isTracking, setIsTracking] = useState(false);
  const carriers = [
    { code: '01', name: '우체국택배' },
    { code: '04', name: 'CJ대한통운' },
    { code: '05', name: '한진택배' },
    { code: '06', name: '로젠택배' },
    { code: '08', name: '롯데택배' },
    { code: '23', name: '경동택배' },
    { code: '24', name: 'GS Postbox' },
    { code: '46', name: 'CU 편의점택배' },
  ];

  const lookupDelivery = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setTrackingError('');
    setTrackingResult(null);
    setIsTracking(true);
    const form = new FormData(event.currentTarget);
    const carrierCode = String(form.get('carrierCode') ?? '');
    const invoice = String(form.get('invoice') ?? '').replaceAll(' ', '').trim();
    if (invoice.length < 8) {
      setTrackingError('운송장 번호를 8자 이상 입력해 주세요.');
      setIsTracking(false);
      return;
    }
    try {
      const params = new URLSearchParams({ carrierCode, invoice });
      const response = await fetch(`${API_BASE_URL}/api/deliveries/track?${params.toString()}`);
      const result = await response.json().catch(() => ({})) as DeliveryTrackingResult & { message?: string };
      if (!response.ok) throw new Error(result.message || '배송정보를 조회하지 못했습니다.');
      setTrackingResult(result);
    } catch (error) {
      setTrackingError(error instanceof Error ? error.message : '배송정보를 조회하지 못했습니다.');
    } finally {
      setIsTracking(false);
    }
  };

  return (
    <DashboardShell title="내 공동구매" description="참여한 공동구매의 결제와 배송 상태를 확인하세요." icon={<ShoppingBag />}>
      <div className="order-grid">
        <article className="order-card"><div className="order-icon">🍊</div><div className="order-info"><Badge>모집 중</Badge><h3>제주 노지 감귤 5kg 산지직송</h3><p>참여일 2026.09.18 · 수량 1개</p><Progress value={88} className="order-progress" /><small>40명 중 35명 참여 · 마감까지 5시간 42분</small></div><strong>23,900원</strong></article>
        <article className="order-card"><div className="order-icon green">🥬</div><div className="order-info"><Badge variant="secondary">배송 중</Badge><h3>무농약 쌈채소 정기 꾸러미</h3><p>주문번호 MM-260914-0281</p><small>택배사와 운송장 번호로 실시간 배송 위치를 확인하세요.</small></div><Button variant="outline" onClick={() => { setTrackingOpen(true); setTrackingError(''); }}>배송 조회</Button></article>
      </div>

      <Dialog open={trackingOpen} onOpenChange={setTrackingOpen}>
        <DialogContent className="delivery-dialog">
          <DialogHeader>
            <span className="delivery-dialog-icon"><Truck /></span>
            <div><DialogTitle>실시간 배송조회</DialogTitle><DialogDescription>택배사와 운송장 번호를 입력하면 현재 배송 위치를 확인할 수 있습니다.</DialogDescription></div>
          </DialogHeader>
          <form className="delivery-search-form" onSubmit={lookupDelivery}>
            <Label htmlFor="delivery-carrier">택배사</Label>
            <select id="delivery-carrier" name="carrierCode" defaultValue="05">{carriers.map((carrier) => <option value={carrier.code} key={carrier.code}>{carrier.name}</option>)}</select>
            <Label htmlFor="delivery-invoice">운송장 번호</Label>
            <Input id="delivery-invoice" name="invoice" inputMode="numeric" placeholder="숫자만 입력하세요" maxLength={30} />
            <Button type="submit" disabled={isTracking}>{isTracking ? <><LoaderCircle className="delivery-spinner" size={17} /> 조회 중...</> : '배송 조회'}</Button>
          </form>
          {trackingError && <p className="delivery-error" role="alert">{trackingError}</p>}
          {trackingResult && (
            <section className="delivery-result" aria-live="polite">
              {trackingResult.demoMode && <Badge className="delivery-demo-badge">체험 데이터</Badge>}
              <div className="delivery-result-heading"><div><small>{trackingResult.carrierName} · {trackingResult.invoice}</small><h3>{trackingResult.status}</h3><p>{trackingResult.message}</p></div>{trackingResult.estimatedDelivery && <span><Clock3 size={15} /> {trackingResult.estimatedDelivery}</span>}</div>
              <div className="delivery-progress-steps"><span className="done">상품 인수</span><span className="done">이동 중</span><span className={trackingResult.status.includes('출발') || trackingResult.complete ? 'done' : 'active'}>배송 출발</span><span className={trackingResult.complete ? 'done' : ''}>배송 완료</span></div>
              <div className="delivery-timeline">
                {trackingResult.events.length === 0 ? <p className="delivery-empty">아직 등록된 이동 내역이 없습니다.</p> : trackingResult.events.map((item, index) => <div className="delivery-event" key={`${item.time}-${index}`}><i /><div><strong>{item.description}</strong><span><MapPin size={13} /> {item.location || '위치 확인 중'}</span></div><time>{item.time}</time></div>)}
              </div>
            </section>
          )}
        </DialogContent>
      </Dialog>
    </DashboardShell>
  );
}

function SellerDashboard({ products: items, onManageProduct, onCreateProduct }: { products: Product[]; onManageProduct: (id: number) => void; onCreateProduct: () => void }) {
  return (
    <DashboardShell title="판매자센터" description="안녕하세요, 제주 햇살농원님. 오늘의 판매 현황입니다." icon={<Store />}>
      <div className="metric-grid">
        <Metric label="진행 중 판매" value="4건" detail="공동구매·한정판매 통합" icon={<Clock3 />} />
        <Metric label="결제 완료 주문" value="128건" detail="어제보다 18건 증가" icon={<ShoppingBag />} />
        <Metric label="배송 준비" value="43건" detail="오늘 출고 권장" icon={<Truck />} />
        <Metric label="정산 예정" value="3,842,000원" detail="9월 25일 지급" icon={<WalletCards />} />
      </div>
      <div className="table-card">
        <div className="table-title"><div><h2>내 판매 상품 관리</h2><p>공동구매와 한정판매 상품을 구분해 관리하세요.</p></div><Button onClick={onCreateProduct}>+ 판매 상품 만들기</Button></div>
        <Table>
          <TableHeader><TableRow><TableHead>상품</TableHead><TableHead>판매 방식</TableHead><TableHead>진행 현황</TableHead><TableHead>마감</TableHead><TableHead className="text-right">관리</TableHead></TableRow></TableHeader>
          <TableBody>{items.slice(0, 3).map((product) => <TableRow key={product.id}><TableCell className="font-semibold">{product.title}</TableCell><TableCell><Badge variant={product.saleType === 'GROUP_BUY' ? 'default' : 'secondary'}>{product.saleType === 'GROUP_BUY' ? '공동구매' : '한정판매'}</Badge></TableCell><TableCell>{product.joined} / {product.target}개</TableCell><TableCell>{product.time}</TableCell><TableCell className="text-right"><Button variant="outline" size="sm" onClick={() => onManageProduct(product.id)}>상세 관리</Button></TableCell></TableRow>)}</TableBody>
        </Table>
      </div>
    </DashboardShell>
  );
}

function SellerProductManagement({ product, onBack, onSave }: { product: Product; onBack: () => void; onSave: () => void }) {
  const isGroupBuy = product.saleType === 'GROUP_BUY';
  const rate = Math.min(100, Math.round((product.joined / product.target) * 100));
  const remaining = Math.max(0, product.target - product.joined);
  const detail = productDetails[product.id] ?? { description: '새로 등록한 상품입니다.', origin: '미입력', delivery: '모집 완료 후 안내', shipping: '상품별 안내', highlights: [] };

  const saveProduct = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSave();
  };

  return (
    <div className="seller-manage-shell">
      <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 판매자센터로 돌아가기</button>

      <div className="seller-manage-heading">
        <div className={`seller-product-thumb ${product.tone}`}>{product.imageUrl ? <img src={product.imageUrl} alt="" /> : <span aria-hidden="true">{product.emoji}</span>}</div>
        <div><Badge>{product.badge}</Badge><h1>{product.title}</h1><p>{product.seller} · 상품번호 MM-{String(product.id).padStart(5, '0')}</p></div>
        <Button variant="outline" onClick={() => onBack()}>목록으로</Button>
      </div>

      <section className="seller-progress-panel">
        <div><span>{isGroupBuy ? '모집 진행률' : '판매 진행률'}</span><strong>{rate}%</strong></div>
        <Progress value={rate} className="deal-progress" />
        <div className="seller-progress-stats">
          <p><small>{isGroupBuy ? '현재 참여' : '판매 완료'}</small><strong>{product.joined}개</strong></p>
          <p><small>{isGroupBuy ? '목표 수량' : '한정 수량'}</small><strong>{product.target}개</strong></p>
          <p><small>{isGroupBuy ? '목표까지' : '남은 재고'}</small><strong>{remaining > 0 ? `${remaining}개` : (isGroupBuy ? '달성 완료' : '품절')}</strong></p>
          <p><small>남은 시간</small><strong>{product.time}</strong></p>
        </div>
      </section>

      <form className="seller-manage-form" onSubmit={saveProduct}>
        <section className="seller-setting-card">
          <div className="seller-setting-title"><div><h2>판매 설정</h2><p>{isGroupBuy ? '가격과 목표 수량을 관리하세요.' : '가격과 한정 재고를 관리하세요.'}</p></div><Store /></div>
          <div className="seller-setting-fields">
            <Label htmlFor="manage-price">판매 가격</Label><Input id="manage-price" name="price" type="number" min={0} defaultValue={product.price} />
            <Label htmlFor="manage-origin">정상 가격</Label><Input id="manage-origin" name="originPrice" type="number" min={0} defaultValue={product.origin} />
            <Label htmlFor="manage-target">{isGroupBuy ? '목표 수량' : '전체 한정 수량'}</Label><Input id="manage-target" name="target" type="number" min={1} defaultValue={product.target} />
            <Label htmlFor="manage-stock">{isGroupBuy ? '추가 판매 가능 수량' : '남은 재고'}</Label><Input id="manage-stock" name="stock" type="number" min={0} defaultValue={remaining} />
          </div>
        </section>

        <section className="seller-setting-card">
          <div className="seller-setting-title"><div><h2>배송 및 모집 설정</h2><p>배송 정보와 상품의 현재 상태를 변경하세요.</p></div><Truck /></div>
          <div className="seller-setting-fields">
            <Label htmlFor="manage-status">모집 상태</Label>
            <select id="manage-status" name="status" defaultValue="OPEN"><option value="OPEN">모집 중</option><option value="PAUSED">모집 일시중지</option><option value="CLOSED">모집 마감</option></select>
            <Label htmlFor="manage-origin-place">원산지</Label><Input id="manage-origin-place" name="origin" defaultValue={detail.origin} />
            <Label htmlFor="manage-delivery">배송 일정</Label><Input id="manage-delivery" name="delivery" defaultValue={detail.delivery} />
            <Label htmlFor="manage-shipping">배송비</Label><Input id="manage-shipping" name="shipping" defaultValue={detail.shipping} />
          </div>
        </section>

        <section className="seller-order-card">
          <div className="seller-setting-title"><div><h2>최근 참여 현황</h2><p>가장 최근에 참여한 주문 내역입니다.</p></div><Users /></div>
          {[['김모임', '2개', '결제 완료'], ['이구매', '1개', '결제 완료'], ['박함께', '3개', '입금 확인']].map(([name, quantity, status], index) => <div className="seller-order-row" key={name}><span className="seller-order-avatar">{name.charAt(0)}</span><div><strong>{name}</strong><p>MM-260929-{1024 - index} · {quantity}</p></div><Badge variant="secondary">{status}</Badge></div>)}
        </section>

        <div className="seller-manage-actions"><Button type="button" variant="outline" onClick={onBack}>취소</Button><Button type="submit">변경사항 저장</Button></div>
      </form>
    </div>
  );
}

function CreateGroupBuyPage({ sellerName, onBack, onCreate }: { sellerName: string; onBack: () => void; onCreate: (input: NewProductInput) => void }) {
  const [error, setError] = useState('');
  const [imagePreview, setImagePreview] = useState('');
  const [saleType, setSaleType] = useState<SaleType>('GROUP_BUY');

  const selectProductImage = (file?: File) => {
    if (!file) {
      setImagePreview('');
      return;
    }
    if (!file.type.startsWith('image/')) {
      setError('이미지 파일만 등록할 수 있습니다.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('상품 이미지는 5MB 이하로 등록해 주세요.');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setImagePreview(reader.result);
        setError('');
      }
    };
    reader.onerror = () => setError('이미지를 불러오지 못했습니다. 다른 파일을 선택해 주세요.');
    reader.readAsDataURL(file);
  };

  const submitGroupBuy = (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');
    const form = new FormData(event.currentTarget);
    const title = String(form.get('title') ?? '').trim();
    const origin = Number(form.get('originPrice'));
    const price = Number(form.get('price'));
    const target = Number(form.get('target'));
    if (!title || !origin || !price || !target) {
      setError(`상품명, 가격과 ${saleType === 'GROUP_BUY' ? '목표 수량' : '한정 판매 수량'}을 모두 입력해 주세요.`);
      return;
    }
    if (price >= origin) {
      setError('판매 가격은 정상 가격보다 낮게 입력해 주세요.');
      return;
    }

    onCreate({
      saleType,
      title,
      emoji: String(form.get('emoji') ?? '').trim(),
      imageUrl: imagePreview || undefined,
      category: form.get('category') === 'living' ? 'living' : 'food',
      origin,
      price,
      target,
      time: String(form.get('period') ?? '3일 00:00'),
      seller: sellerName,
    });
  };

  return (
    <div className="seller-create-shell">
      <button className="detail-back" type="button" onClick={onBack}><ArrowLeft size={18} /> 판매자센터로 돌아가기</button>
      <div className="seller-create-heading"><span><Plus /></span><div><p className="eyebrow dark">NEW SALE</p><h1>판매 상품 만들기</h1><p>공동구매 또는 선착순 한정판매 방식을 선택해 상품을 등록하세요.</p></div></div>

      <form className="seller-create-form" onSubmit={submitGroupBuy} noValidate>
        <section className="seller-create-card">
          <div className="seller-setting-title"><div><h2>상품 정보</h2><p>구매자에게 표시될 기본 정보입니다.</p></div><PackageCheck /></div>
          <div className="seller-create-fields">
            <Label htmlFor="create-sale-type">판매 방식</Label><select id="create-sale-type" name="saleType" value={saleType} onChange={(event) => setSaleType(event.target.value as SaleType)}><option value="GROUP_BUY">공동구매 · 목표 달성 후 결제</option><option value="LIMITED_SALE">선착순 한정판매 · 즉시 결제</option></select>
            <Label htmlFor="create-title">상품명</Label><Input id="create-title" name="title" placeholder="예: 제주 유기농 당근 3kg" maxLength={80} required />
            <Label htmlFor="create-category">카테고리</Label><select id="create-category" name="category" defaultValue="food"><option value="food">식품</option><option value="living">생활</option></select>
            <Label htmlFor="create-emoji">이미지 대체 아이콘 <small>(선택)</small></Label><div className="seller-field-help"><Input id="create-emoji" name="emoji" placeholder="예: 🥕" maxLength={4} /><small>상품 이미지를 등록하지 않으면 상품 카드에 이 아이콘이 대신 표시됩니다.</small></div>
            <Label htmlFor="create-image">상품 이미지</Label>
            <div className="product-image-upload">
              <label htmlFor="create-image" className={imagePreview ? 'has-image' : ''}>
                {imagePreview ? <img src={imagePreview} alt="선택한 상품 이미지 미리보기" /> : <span><PackageCheck /><strong>이미지 선택</strong><small>JPG, PNG, WEBP · 최대 5MB</small></span>}
              </label>
              <input id="create-image" name="image" type="file" accept="image/png,image/jpeg,image/webp,image/gif" onChange={(event) => selectProductImage(event.currentTarget.files?.[0])} />
              {imagePreview && <button type="button" onClick={() => setImagePreview('')}>이미지 삭제</button>}
            </div>
            <Label htmlFor="create-description">상품 소개</Label><textarea id="create-description" name="description" placeholder="상품의 특징과 구성 내용을 소개해 주세요." rows={5} />
          </div>
        </section>

        <section className="seller-create-card">
          <div className="seller-setting-title"><div><h2>가격 및 판매 조건</h2><p>{saleType === 'GROUP_BUY' ? '공동구매가 성사될 최소 목표를 설정하세요.' : '선착순으로 판매할 전체 한정 재고를 설정하세요.'}</p></div><WalletCards /></div>
          <div className="seller-create-fields">
            <Label htmlFor="create-origin">정상 가격</Label><Input id="create-origin" name="originPrice" type="number" min={1} placeholder="30000" required />
            <Label htmlFor="create-price">판매 가격</Label><Input id="create-price" name="price" type="number" min={1} placeholder="23900" required />
            <Label htmlFor="create-target">{saleType === 'GROUP_BUY' ? '최소 목표 수량' : '한정 판매 수량'}</Label><Input id="create-target" name="target" type="number" min={1} placeholder="30" required />
            <Label htmlFor="create-period">{saleType === 'GROUP_BUY' ? '모집 기간' : '판매 기간'}</Label><select id="create-period" name="period" defaultValue="3일 00:00"><option value="1일 00:00">1일</option><option value="3일 00:00">3일</option><option value="7일 00:00">7일</option></select>
          </div>
        </section>

        <section className="seller-create-card seller-create-delivery">
          <div className="seller-setting-title"><div><h2>배송 정보</h2><p>{saleType === 'GROUP_BUY' ? '공동구매 성사 후' : '결제 완료 후'} 적용할 배송 정보를 입력하세요.</p></div><Truck /></div>
          <div className="seller-create-fields wide">
            <Label htmlFor="create-origin-place">원산지</Label><Input id="create-origin-place" name="originPlace" placeholder="예: 제주특별자치도" />
            <Label htmlFor="create-delivery">배송 일정</Label><Input id="create-delivery" name="delivery" placeholder="예: 결제 후 2~3일 이내" />
            <Label htmlFor="create-shipping">배송비</Label><Input id="create-shipping" name="shipping" placeholder="예: 무료배송" />
          </div>
        </section>

        {error && <p className="seller-create-error" role="alert">{error}</p>}
        <div className="seller-create-actions"><Button type="button" variant="outline" onClick={onBack}>취소</Button><Button type="submit"><Plus size={17} /> {saleType === 'GROUP_BUY' ? '공동구매 등록' : '한정판매 등록'}</Button></div>
      </form>
    </div>
  );
}

function AdminDashboard() {
  return (
    <DashboardShell title="관리자센터" description="플랫폼 운영 현황과 처리할 업무를 확인하세요." icon={<ShieldCheck />}>
      <div className="metric-grid">
        <Metric label="오늘 거래액" value="12,840,000원" detail="전일 대비 12.4% 증가" icon={<LayoutDashboard />} />
        <Metric label="신규 회원" value="84명" detail="누적 회원 8,920명" icon={<Users />} />
        <Metric label="판매자 승인 대기" value="7건" detail="검토가 필요합니다" icon={<Store />} />
        <Metric label="신고·분쟁" value="3건" detail="미처리 문의" icon={<ShieldCheck />} />
      </div>
      <div className="admin-columns">
        <section className="table-card">
          <div className="table-title"><div><h2>판매자 입점 승인</h2><p>사업자 정보와 판매 품목을 검토하세요.</p></div><button>전체보기</button></div>
          {['강원 청정마을', '소소한 살림', '바다담은 수산'].map((name, index) => <div className="approval-row" key={name}><span className="shop-avatar">{name[0]}</span><div><strong>{name}</strong><p>{index === 0 ? '농산물·가공식품' : index === 1 ? '생활·주방용품' : '수산물·건어물'} · 신청 {index + 1}일 전</p></div><Button variant="outline" size="sm">검토</Button></div>)}
        </section>
        <section className="table-card risk-card">
          <div className="table-title"><div><h2>운영 알림</h2><p>자동 처리 결과와 주의 항목입니다.</p></div></div>
          <div className="risk-item"><span className="risk-dot orange" /><div><strong>마감 실패 일괄 환불</strong><p>2개 공동구매 · 38건 환불 완료</p></div></div>
          <div className="risk-item"><span className="risk-dot green" /><div><strong>오늘 자동 성사</strong><p>5개 공동구매가 목표 인원을 달성했습니다.</p></div></div>
          <div className="risk-item"><span className="risk-dot red" /><div><strong>재고 확인 필요</strong><p>결제 완료 수량과 재고가 다른 상품 1건</p></div></div>
        </section>
      </div>
    </DashboardShell>
  );
}

function DashboardShell({ title, description, icon, children }: { title: string; description: string; icon: React.ReactNode; children: React.ReactNode }) {
  return <div className="dashboard-shell"><div className="dashboard-heading"><span>{icon}</span><div><h1>{title}</h1><p>{description}</p></div></div>{children}</div>;
}

function Metric({ label, value, detail, icon }: { label: string; value: string; detail: string; icon: React.ReactNode }) {
  return <article className="metric-card"><div className="metric-icon">{icon}</div><p>{label}</p><strong>{value}</strong><span>{detail}</span></article>;
}
