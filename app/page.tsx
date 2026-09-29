'use client';

import { type FormEvent, useEffect, useMemo, useState } from 'react';
import {
  Bell,
  ChevronRight,
  Clock3,
  Heart,
  LayoutDashboard,
  LogOut,
  PackageCheck,
  Search,
  ShieldCheck,
  ShoppingBag,
  Store,
  Truck,
  UserRound,
  Users,
  WalletCards,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Progress } from '@/components/ui/progress';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

type Role = 'buyer' | 'seller' | 'admin';
type View = 'home' | 'orders' | 'seller' | 'admin';

const products = [
  { id: 1, emoji: '🍊', tone: 'orange', badge: '오늘 마감', seller: '제주 햇살농원', title: '제주 노지 감귤 5kg 산지직송', origin: 32900, price: 23900, joined: 34, target: 40, time: '05:42:18' },
  { id: 2, emoji: '🥩', tone: 'red', badge: '인기 급상승', seller: '바른축산', title: '1++ 한우 불고기·국거리 세트', origin: 69000, price: 49800, joined: 72, target: 80, time: '1일 08:21' },
  { id: 3, emoji: '🫙', tone: 'amber', badge: '2단계 할인', seller: '오늘의 식탁', title: '저온숙성 국산 참기름 2병', origin: 42000, price: 31500, joined: 18, target: 30, time: '2일 14:03' },
  { id: 4, emoji: '🧻', tone: 'blue', badge: '무료배송', seller: '매일생활', title: '천연펄프 3겹 화장지 30롤', origin: 27900, price: 19900, joined: 47, target: 50, time: '09:17:44' },
];

const won = (value: number) => `${value.toLocaleString('ko-KR')}원`;
const roleLabels: Record<Role, string> = {
  buyer: '구매자',
  seller: '판매자',
  admin: '관리자',
};

export default function Home() {
  const [role, setRole] = useState<Role>('buyer');
  const [selectedRole, setSelectedRole] = useState<Role>('buyer');
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [view, setView] = useState<View>('home');
  const [counts, setCounts] = useState<Record<number, number>>(Object.fromEntries(products.map((product) => [product.id, product.joined])));
  const [notice, setNotice] = useState('');
  const activeProducts = useMemo(() => products.map((product) => ({ ...product, joined: counts[product.id] })), [counts]);

  const login = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setRole(selectedRole);
    setView(selectedRole === 'buyer' ? 'home' : selectedRole);
    setIsLoggedIn(true);
    setNotice(`${roleLabels[selectedRole]} 모드로 로그인했습니다.`);
  };

  const logout = () => {
    setIsLoggedIn(false);
    setView('home');
    setNotice('');
  };

  const participate = (id: number, title: string) => {
    setCounts((current) => ({ ...current, [id]: current[id] + 1 }));
    setNotice(`‘${title}’ 공동구매에 참여했어요. 가상 결제가 완료되었습니다.`);
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
      execute(input) {
        const productId = Number((input as { productId?: unknown })?.productId);
        const product = products.find((item) => item.id === productId);
        if (!product) throw new Error('존재하지 않는 상품입니다.');
        setCounts((current) => ({ ...current, [productId]: current[productId] + 1 }));
        setNotice(`‘${product.title}’ 공동구매에 참여했어요. 가상 결제가 완료되었습니다.`);
        setRole('buyer');
        setSelectedRole('buyer');
        setIsLoggedIn(true);
        setView('home');
        return { productId, status: 'joined', productTitle: product.title };
      },
    }, { signal: lifecycle.signal })).catch(() => undefined);

    return () => lifecycle.abort();
  }, []);

  if (!isLoggedIn) {
    return <LoginPage selectedRole={selectedRole} onRoleChange={setSelectedRole} onLogin={login} />;
  }

  return (
    <main className="min-h-screen bg-[#f7f8fa] text-[#152019]">
      <header className="sticky top-0 z-30 border-b border-black/7 bg-white/95 backdrop-blur">
        <div className="topbar">
          <button className="brand" onClick={() => setView('home')} aria-label="모두모임 홈">
            <span className="brand-mark">M</span><span>모두모임</span>
          </button>
          <label className="searchbox"><Search size={18} /><input aria-label="상품 검색" placeholder="어떤 상품을 함께 살까요?" /></label>
          <nav className="header-actions" aria-label="사용자 메뉴">
            <button className="icon-button" aria-label="알림"><Bell size={20} /><span className="notification-dot" /></button>
            <span className={`current-role ${role}`}>{roleLabels[role]}</span>
            <span className="avatar">김</span>
            <button className="logout-button" onClick={logout}><LogOut size={16} /> 로그아웃</button>
          </nav>
        </div>
        <div className="navrow">
          <nav className="navlinks" aria-label="주요 메뉴">
            <button className={view === 'home' ? 'active' : ''} onClick={() => setView('home')}>공동구매</button>
            <button>신규 오픈</button><button>마감 임박</button><button>식품</button><button>생활</button>
          </nav>
          <div className="role-links">
            {role === 'buyer' && <button className={view === 'orders' ? 'active' : ''} onClick={() => setView('orders')}><ShoppingBag size={16} /> 내 주문</button>}
            {role === 'seller' && <button className={view === 'seller' ? 'active' : ''} onClick={() => setView('seller')}><Store size={16} /> 판매자센터</button>}
            {role === 'admin' && <button className={view === 'admin' ? 'active' : ''} onClick={() => setView('admin')}><ShieldCheck size={16} /> 관리자센터</button>}
          </div>
        </div>
      </header>

      {notice && <div className="notice" role="status"><PackageCheck size={19} /> {notice}<button onClick={() => setNotice('')}>닫기</button></div>}
      {view === 'home' && <BuyerHome products={activeProducts} onParticipate={participate} />}
      {view === 'orders' && <OrdersPage />}
      {view === 'seller' && role === 'seller' && <SellerDashboard products={activeProducts} />}
      {view === 'admin' && role === 'admin' && <AdminDashboard />}
    </main>
  );
}

function LoginPage({
  selectedRole,
  onRoleChange,
  onLogin,
}: {
  selectedRole: Role;
  onRoleChange: (role: Role) => void;
  onLogin: (event: FormEvent<HTMLFormElement>) => void;
}) {
  const roleOptions = [
    { value: 'buyer' as const, label: '구매자', description: '공동구매 참여와 주문·배송 조회', icon: <UserRound /> },
    { value: 'seller' as const, label: '판매자', description: '상품·방송·주문과 배송 관리', icon: <Store /> },
    { value: 'admin' as const, label: '관리자', description: '회원·판매자 승인과 플랫폼 운영', icon: <ShieldCheck /> },
  ];

  return (
    <main className="login-screen">
      <section className="login-showcase" aria-label="모두모임 서비스 소개">
        <div className="login-brand"><span className="brand-mark">M</span><span>모두모임</span></div>
        <div className="login-showcase-copy">
          <Badge className="login-badge">LIVE GROUP BUY</Badge>
          <h1>함께 사는 순간,<br />더 좋은 가격이 열립니다.</h1>
          <p>실시간 방송을 보며 한정 수량 공동구매에 참여하고, 판매와 운영까지 한곳에서 관리하세요.</p>
        </div>
        <div className="login-live-card"><span className="live-pulse" /> 지금 8,920명이 공동구매에 참여하고 있어요</div>
      </section>

      <section className="login-panel">
        <form className="login-form" onSubmit={onLogin}>
          <div className="login-heading">
            <p>다시 만나서 반가워요</p>
            <h2>로그인</h2>
            <span>이용할 역할을 선택하고 계정 정보를 입력하세요.</span>
          </div>

          <fieldset className="role-fieldset">
            <legend>로그인 역할</legend>
            <RadioGroup value={selectedRole} onValueChange={(value) => onRoleChange(value as Role)} className="login-role-grid">
              {roleOptions.map((option) => (
                <Label className={`login-role-card ${selectedRole === option.value ? 'selected' : ''}`} key={option.value}>
                  <RadioGroupItem value={option.value} aria-label={`${option.label} 선택`} />
                  <span className="login-role-icon">{option.icon}</span>
                  <span><strong>{option.label}</strong><small>{option.description}</small></span>
                </Label>
              ))}
            </RadioGroup>
          </fieldset>

          <div className="login-fields">
            <Label htmlFor="login-email">이메일</Label>
            <Input id="login-email" name="email" type="email" placeholder="name@example.com" autoComplete="email" required />
            <Label htmlFor="login-password">비밀번호</Label>
            <Input id="login-password" name="password" type="password" placeholder="비밀번호를 입력하세요" autoComplete="current-password" required />
          </div>

          <Button type="submit" className="login-submit">{roleLabels[selectedRole]}로 로그인</Button>
          <p className="login-demo-note">현재는 화면 확인용 로그인입니다. 테스트 정보를 입력해 이용할 수 있습니다.</p>
        </form>
      </section>
    </main>
  );
}

function BuyerHome({ products: items, onParticipate }: { products: typeof products; onParticipate: (id: number, title: string) => void }) {
  return (
    <div className="page-shell">
      <section className="hero">
        <img src="/hero-grocery.png" alt="과일과 채소, 빵으로 구성된 공동구매 꾸러미" />
        <div className="hero-overlay" />
        <div className="hero-copy">
          <Badge className="hero-badge">이번 주 추천 공구</Badge>
          <p className="eyebrow">함께 살수록 더 좋은 가격</p>
          <h1>가을 식탁을 채우는<br />산지직송 모음</h1>
          <p>농가에서 바로 보내는 신선한 제철 먹거리를<br className="desktop-only" /> 최대 32% 할인된 가격으로 만나보세요.</p>
          <Button className="hero-button">추천 상품 보기 <ChevronRight size={17} /></Button>
        </div>
        <div className="hero-stat"><strong>1,284명</strong><span>오늘 함께 구매했어요</span></div>
      </section>

      <section className="quick-stats" aria-label="서비스 현황">
        <div><Users /><span><strong>8,920명</strong> 지금 참여 중</span></div>
        <div><Clock3 /><span><strong>26개</strong> 오늘 마감</span></div>
        <div><WalletCards /><span><strong>평균 27%</strong> 할인</span></div>
        <div><Truck /><span><strong>전 상품</strong> 배송 추적</span></div>
      </section>

      <section className="product-section">
        <div className="section-heading"><div><p className="eyebrow dark">지금 가장 활발한 모임</p><h2>곧 성사되는 공동구매</h2></div><button>전체보기 <ChevronRight size={17} /></button></div>
        <div className="product-grid">
          {items.map((product) => {
            const rate = Math.min(100, Math.round((product.joined / product.target) * 100));
            const remaining = Math.max(0, product.target - product.joined);
            return (
              <article className="product-card" key={product.id}>
                <div className={`product-visual ${product.tone}`}>
                  <Badge className="product-badge">{product.badge}</Badge>
                  <button aria-label={`${product.title} 찜하기`} className="heart"><Heart size={19} /></button>
                  <span aria-hidden="true">{product.emoji}</span><p>산지·브랜드 직배송</p>
                </div>
                <div className="product-body">
                  <p className="seller-name">{product.seller}</p><h3>{product.title}</h3>
                  <div className="price-row"><strong>{won(product.price)}</strong><del>{won(product.origin)}</del></div>
                  <div className="progress-copy"><strong>{rate}% 달성</strong><span>{remaining > 0 ? `${remaining}명 더 모이면 성사` : '목표 달성!'}</span></div>
                  <Progress value={rate} className="deal-progress" />
                  <div className="card-footer"><span><Clock3 size={15} /> {product.time}</span><Button size="sm" onClick={() => onParticipate(product.id, product.title)}>참여하기</Button></div>
                </div>
              </article>
            );
          })}
        </div>
      </section>
    </div>
  );
}

function OrdersPage() {
  return (
    <DashboardShell title="내 공동구매" description="참여한 공동구매의 결제와 배송 상태를 확인하세요." icon={<ShoppingBag />}>
      <div className="order-grid">
        <article className="order-card"><div className="order-icon">🍊</div><div className="order-info"><Badge>모집 중</Badge><h3>제주 노지 감귤 5kg 산지직송</h3><p>참여일 2026.09.18 · 수량 1개</p><Progress value={88} className="order-progress" /><small>40명 중 35명 참여 · 마감까지 5시간 42분</small></div><strong>23,900원</strong></article>
        <article className="order-card"><div className="order-icon green">🥬</div><div className="order-info"><Badge variant="secondary">배송 중</Badge><h3>무농약 쌈채소 정기 꾸러미</h3><p>주문번호 MM-260914-0281</p><small>한진택배 · 오늘 오후 도착 예정</small></div><Button variant="outline">배송 조회</Button></article>
      </div>
    </DashboardShell>
  );
}

function SellerDashboard({ products: items }: { products: typeof products }) {
  return (
    <DashboardShell title="판매자센터" description="안녕하세요, 제주 햇살농원님. 오늘의 판매 현황입니다." icon={<Store />}>
      <div className="metric-grid">
        <Metric label="진행 중 공동구매" value="4건" detail="오늘 마감 1건" icon={<Clock3 />} />
        <Metric label="결제 완료 주문" value="128건" detail="어제보다 18건 증가" icon={<ShoppingBag />} />
        <Metric label="배송 준비" value="43건" detail="오늘 출고 권장" icon={<Truck />} />
        <Metric label="정산 예정" value="3,842,000원" detail="9월 25일 지급" icon={<WalletCards />} />
      </div>
      <div className="table-card">
        <div className="table-title"><div><h2>내 공동구매 관리</h2><p>진행률과 재고를 확인하고 상품을 관리하세요.</p></div><Button>+ 공동구매 만들기</Button></div>
        <Table>
          <TableHeader><TableRow><TableHead>상품</TableHead><TableHead>상태</TableHead><TableHead>참여/목표</TableHead><TableHead>재고</TableHead><TableHead>마감</TableHead><TableHead className="text-right">관리</TableHead></TableRow></TableHeader>
          <TableBody>{items.slice(0, 3).map((product, index) => <TableRow key={product.id}><TableCell className="font-semibold">{product.title}</TableCell><TableCell><Badge variant={index === 0 ? 'default' : 'secondary'}>{index === 0 ? '모집 중' : '성사 임박'}</Badge></TableCell><TableCell>{product.joined} / {product.target}명</TableCell><TableCell>{86 - index * 21}개</TableCell><TableCell>{product.time}</TableCell><TableCell className="text-right"><Button variant="outline" size="sm">상세 관리</Button></TableCell></TableRow>)}</TableBody>
        </Table>
      </div>
    </DashboardShell>
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
