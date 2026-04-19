// HomePage — bento grid with warm illustrative cards
const { useState: useStateHP } = React;

function HomePage({ tweaks, navigate }){
  const accentMap = {
    rose:  'linear-gradient(145deg, #F5D8D8 0%, #E8A4A4 50%, #D98B8B 100%)',
    peach: 'linear-gradient(145deg, #F5C4A8 0%, #E89B7A 55%, #C47B5A 100%)',
    sage:  'linear-gradient(145deg, #D5E3DA 0%, #8FB09F 55%, #6B9683 100%)',
    cream: 'linear-gradient(145deg, #F5E4D1 0%, #D8BF9F 55%, #A88D6A 100%)',
  };
  const heroGrad = accentMap[tweaks.heroAccent] || accentMap.peach;

  const features = [
    { icon: <Icon.Heart size={22}/>, title: '情感分析', desc: '读懂 TA 字里行间的小心思', tone: 'rose' },
    { icon: <Icon.Sparkles size={22}/>, title: '话术建议', desc: '贴心回复，不失温度的恰到好处', tone: 'peach' },
    { icon: <Icon.Shield size={22}/>, title: '隐私保护', desc: '你的秘密，永远只属于你', tone: 'sage' },
    { icon: <Icon.Zap size={22}/>, title: '即时响应', desc: '关键时刻不让你等待', tone: 'cream' },
  ];

  return (
    <div className="warm-bg grain relative w-full h-full overflow-y-auto nice-scroll">
      <div className="relative z-10 max-w-[1080px] mx-auto px-6 md:px-10 py-8 md:py-12">

        {/* Navbar-ish brand row */}
        <header className="flex items-center justify-between mb-8 fade-up" style={{ animationDelay: '0s' }}>
          <div className="flex items-center gap-2.5">
            <BrandMark size={36}/>
            <div>
              <div className="display text-[19px] leading-none" style={{ color: 'var(--text-ink)' }}>Love Master</div>
              <div className="text-[11px]" style={{ color: 'var(--text-muted)', fontFamily:'var(--font-en)', letterSpacing: '0.12em' }}>YOUR LITTLE LOVE COMPANION</div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => navigate('auth')} className="btn-ghost text-sm">登录</button>
            <button onClick={() => navigate('chat')} className="btn-primary text-sm">开始聊天</button>
          </div>
        </header>

        {/* ── BENTO ── */}
        <div className="grid grid-cols-12 gap-4 md:gap-5 mb-6">

          {/* HERO — col-span-7, row-span-2 */}
          <div
            className="col-span-12 md:col-span-7 row-span-2 relative overflow-hidden cursor-pointer fade-up"
            onClick={() => navigate('chat')}
            style={{
              background: heroGrad,
              borderRadius: 'var(--r-xl)',
              minHeight: 380,
              padding: '36px 36px 32px',
              boxShadow: '0 20px 44px rgba(232,155,122,0.28), inset 0 1px 0 rgba(255,255,255,0.35)',
              animationDelay: '0.05s',
            }}>
            {/* Decorative floating shapes */}
            <div className="absolute -top-6 -left-6 float-slow" style={{ opacity: 0.55 }}>
              <svg width="110" height="110" viewBox="0 0 100 100">
                <path d="M50 15 C55 5, 75 5, 75 25 C75 45, 50 60, 50 60 C50 60, 25 45, 25 25 C25 5, 45 5, 50 15 Z"
                  fill="rgba(255,250,245,0.35)" />
              </svg>
            </div>
            <div className="absolute top-10 right-16 float-med" style={{ opacity: 0.4 }}>
              <div style={{ width: 14, height: 14, borderRadius: '50%', background: 'rgba(255,250,245,0.7)' }}/>
            </div>
            <div className="absolute bottom-6 left-24 float-slow" style={{ animationDelay: '1s', opacity: 0.5 }}>
              <div style={{ width: 8, height: 8, borderRadius: '50%', background: 'rgba(255,250,245,0.8)' }}/>
            </div>
            <div className="absolute top-24 right-6" style={{ opacity: 0.3 }}>
              <svg width="28" height="28" viewBox="0 0 24 24"><path d="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3z" fill="rgba(255,250,245,0.8)"/></svg>
            </div>

            {/* Illustration placeholder — couple silhouette */}
            {tweaks.heroIllustration && (
              <div className="absolute right-4 bottom-0 float-slow" style={{ width: '46%', height: '78%', pointerEvents: 'none' }}>
                <CoupleIllustration/>
              </div>
            )}

            <div className="relative z-10 h-full flex flex-col justify-between">
              <div>
                <div className="inline-flex items-center gap-1.5 px-3 py-1 text-[11px] font-semibold uppercase"
                  style={{ background: 'rgba(255,250,245,0.28)', color: '#FFFAF5', borderRadius: 999, letterSpacing: '0.14em', backdropFilter: 'blur(6px)', border: '1px solid rgba(255,250,245,0.3)' }}>
                  <span className="heart-beat inline-flex"><Icon.Heart size={11} fill="#FFFAF5" stroke="#FFFAF5"/></span>
                  LOVE MASTER · 2026
                </div>
              </div>

              <div>
                <h1 className="display" style={{ color: '#FFFAF5', fontSize: 'clamp(36px, 5vw, 54px)', lineHeight: 1.1, textShadow: '0 2px 16px rgba(160,98,74,0.2)' }}>
                  你的恋爱小助手<br/>一直都在
                </h1>
                <p className="mt-3 max-w-[380px] text-[15px]" style={{ color: 'rgba(255,250,245,0.92)', lineHeight: 1.7 }}>
                  聊天卡壳了？心情不好？想要一点温柔的建议？<br/>轻轻点一下，我就来陪你。
                </p>
                <div className="mt-6 flex items-center gap-2">
                  <button onClick={(e)=>{e.stopPropagation(); navigate('chat');}}
                    className="inline-flex items-center gap-2 px-5 py-2.5 font-semibold text-sm"
                    style={{ background: '#FFFDF9', color: 'var(--primary-dark)', borderRadius: 999, boxShadow: '0 8px 20px rgba(160,98,74,0.22), inset 0 1px 0 rgba(255,255,255,0.8)' }}>
                    <Icon.MessageCircle size={15}/> 现在开始聊聊
                  </button>
                  <span className="inline-flex items-center gap-1.5 text-[12px] ml-1"
                    style={{ color: 'rgba(255,250,245,0.82)' }}>
                    <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: '#8FB09F', boxShadow: '0 0 0 3px rgba(143,176,159,0.3)' }}/>
                    1,248 人正在聊
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* 恋爱陪伴 */}
          <FeatureCard
            onClick={() => navigate('chat')}
            span="md:col-span-5"
            delay="0.12s"
            tone="rose"
            icon={<Icon.Heart size={22}/>}
            badge="陪伴模式"
            title="恋爱陪伴"
            subtitle="温柔地陪你聊天，不评判、不催促，只倾听"
            footnote="「今天想说说什么呢？」"
          />

          {/* 恋爱教练 */}
          <FeatureCard
            onClick={() => navigate('chat')}
            span="md:col-span-5"
            delay="0.18s"
            tone="sage"
            icon={<Icon.BookOpen size={22}/>}
            badge="教练模式"
            title="恋爱教练"
            subtitle="分析聊天记录，给你最懂人心的专业建议"
            footnote="「我帮你看看，再慢慢想。」"
          />

        </div>

        {/* Mood ticker */}
        <div className="soft-card fade-up mb-6 relative overflow-hidden"
          style={{ padding: '18px 24px', animationDelay: '0.24s' }}>
          <div className="flex items-center gap-4 flex-wrap">
            <span className="text-xs" style={{ color: 'var(--text-muted)', letterSpacing: '0.1em' }}>此刻最近的心情 →</span>
            <Chip tone="rose" icon={<span>💭</span>}>有点想他</Chip>
            <Chip tone="peach" icon={<span>☁️</span>}>聊着聊着冷了</Chip>
            <Chip tone="sage" icon={<span>🍃</span>}>想约第二次</Chip>
            <Chip tone="cream" icon={<span>✉️</span>}>不会回消息</Chip>
            <Chip tone="rose" icon={<span>🥲</span>}>刚被已读</Chip>
          </div>
        </div>

        {/* Features grid */}
        <section className="soft-card fade-up heart-pattern relative"
          style={{ padding: '36px 28px', animationDelay: '0.3s' }}>
          <div className="flex items-end justify-between flex-wrap gap-4 mb-7">
            <SectionTitle
              eyebrow="WHY LOVEMASTER"
              title="这次，不只是陪你聊天"
              subtitle="我们希望做一个懂分寸、有温度、也靠得住的小伙伴。"
            />
            <div className="text-xs" style={{ color: 'var(--text-muted)' }}>
              No. 01 — 04
            </div>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {features.map((f, i) => (
              <div key={f.title} className="relative p-5 pop-in"
                style={{
                  background: 'rgba(255,253,249,0.7)',
                  borderRadius: 'var(--r-md)',
                  border: '1px solid var(--border-soft)',
                  animationDelay: `${0.36 + i * 0.08}s`,
                }}>
                <IconSticker tone={f.tone} size={48}>{f.icon}</IconSticker>
                <div className="mt-4 text-[15px] font-medium" style={{ color: 'var(--text-ink)' }}>{f.title}</div>
                <div className="mt-1 text-[13px] leading-relaxed" style={{ color: 'var(--text-body)' }}>{f.desc}</div>
                <div className="absolute top-4 right-5 text-[10px]" style={{ color: 'var(--text-faint)', fontFamily: 'var(--font-en)' }}>
                  0{i + 1}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Testimonial strip */}
        <section className="grid grid-cols-12 gap-4 md:gap-5 mt-5 mb-8">
          <div className="col-span-12 md:col-span-8 soft-card fade-up" style={{ padding: '28px 28px', animationDelay: '0.42s', position: 'relative', overflow: 'hidden' }}>
            <div className="absolute top-4 left-5 display" style={{ fontSize: 72, color: 'var(--bg-peach)', lineHeight: 0.8, fontFamily: 'Georgia, serif' }}>"</div>
            <div className="relative z-10 pl-10">
              <p className="text-[15px] leading-[1.85]" style={{ color: 'var(--text-body)' }}>
                那天我因为一条没回的消息崩溃，Lovemaster 只是很轻地说：<br/>
                <span className="display text-[18px]" style={{ color: 'var(--text-ink)' }}>「先喝口水，我们慢慢来。」</span><br/>
                就这样一句话，我没忍住哭了出来。
              </p>
              <div className="flex items-center gap-2.5 mt-5">
                <Avatar name="小棠" tone="rose" size={32}/>
                <div>
                  <div className="text-sm" style={{ color: 'var(--text-ink)' }}>小棠 · 24岁</div>
                  <div className="text-[11px]" style={{ color: 'var(--text-muted)' }}>2026 年 3 月</div>
                </div>
              </div>
            </div>
          </div>
          <div className="col-span-12 md:col-span-4 fade-up" style={{ animationDelay: '0.48s', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div className="soft-card relative overflow-hidden"
              style={{ padding: 18, background: 'linear-gradient(135deg, #FCE7D5 0%, #F5C4A8 100%)', flex: 1 }}>
              <div className="display text-[32px] leading-none" style={{ color: 'var(--primary-dark)' }}>4.9</div>
              <div className="flex gap-0.5 mt-2" style={{ color: '#E89B7A' }}>
                {[1,2,3,4,5].map(i => <Icon.Stars key={i} size={14} stroke="none" fill="#E89B7A"/>)}
              </div>
              <div className="text-xs mt-1" style={{ color: 'var(--primary-dark)' }}>App Store · 12,480 评分</div>
            </div>
            <div className="soft-card flex items-center gap-3" style={{ padding: 16 }}>
              <IconSticker tone="sage" size={40}><Icon.Lock size={18}/></IconSticker>
              <div>
                <div className="text-[13px] font-medium" style={{ color: 'var(--text-ink)' }}>端到端加密</div>
                <div className="text-[11px]" style={{ color: 'var(--text-muted)' }}>聊天不留档 · 不卖数据</div>
              </div>
            </div>
          </div>
        </section>

        <footer className="py-6 text-center text-xs" style={{ color: 'var(--text-faint)' }}>
          Love Master AI · 2026 · 愿每一段感情都被温柔以待 🌿
        </footer>
      </div>
    </div>
  );
}

// Simple feature card
function FeatureCard({ onClick, span, delay, tone, icon, badge, title, subtitle, footnote }){
  return (
    <div onClick={onClick} role="button"
      className={`col-span-12 ${span} relative cursor-pointer fade-up soft-card group`}
      style={{
        padding: '24px 24px 22px',
        animationDelay: delay,
        transition: 'transform .22s cubic-bezier(0.34,1.56,0.64,1), box-shadow .22s',
      }}
      onMouseEnter={(e)=>{ e.currentTarget.style.transform='translateY(-4px)'; e.currentTarget.style.boxShadow='var(--shadow-lift)'; }}
      onMouseLeave={(e)=>{ e.currentTarget.style.transform=''; e.currentTarget.style.boxShadow=''; }}
      >
      <div className="flex items-start gap-3 mb-4">
        <IconSticker tone={tone}>{icon}</IconSticker>
        <div className="flex-1">
          <div className="text-[10px] uppercase tracking-widest mb-1" style={{ color: 'var(--text-muted)', letterSpacing:'0.16em' }}>{badge}</div>
          <div className="display text-[22px] leading-tight" style={{ color: 'var(--text-ink)' }}>{title}</div>
        </div>
        <div className="grid place-items-center opacity-0 group-hover:opacity-100 transition-opacity"
          style={{ width: 28, height: 28, borderRadius: '50%', background: 'var(--bg-peach)', color: 'var(--primary-dark)' }}>
          <Icon.ChevronRight size={14}/>
        </div>
      </div>
      <p className="text-[13px] leading-relaxed mb-3" style={{ color: 'var(--text-body)' }}>{subtitle}</p>
      <div className="dot-divider mb-3"/>
      <div className="text-[12px] italic" style={{ color: 'var(--text-muted)' }}>{footnote}</div>
    </div>
  );
}

// Cute couple illustration inside hero (simple, stylized)
function CoupleIllustration(){
  return (
    <svg viewBox="0 0 220 260" width="100%" height="100%" fill="none">
      {/* Back figure */}
      <ellipse cx="78" cy="260" rx="50" ry="16" fill="rgba(58,36,25,0.12)"/>
      <circle cx="78" cy="80" r="26" fill="#F5D8D8"/>
      <path d="M52 108 C52 108, 58 180, 78 180 C98 180, 104 108, 104 108 L104 220 L52 220 Z" fill="#FFFAF5" opacity="0.92"/>
      <path d="M72 70 Q78 62 84 70" stroke="#3A2419" strokeWidth="1.4" strokeLinecap="round"/>
      <circle cx="69" cy="78" r="1.3" fill="#3A2419"/>
      <circle cx="86" cy="78" r="1.3" fill="#3A2419"/>
      {/* hair */}
      <path d="M54 76 C50 60, 64 48, 78 50 C92 48, 106 62, 102 78 C100 72, 92 66, 78 66 C64 66, 56 72, 54 76 Z" fill="#6B4A38"/>
      {/* Front figure */}
      <circle cx="140" cy="90" r="24" fill="#F5C4A8"/>
      <path d="M116 116 C116 116, 122 188, 140 188 C158 188, 164 116, 164 116 L164 228 L116 228 Z" fill="#E89B7A" opacity="0.9"/>
      <path d="M134 82 Q140 76 146 82" stroke="#3A2419" strokeWidth="1.4" strokeLinecap="round"/>
      <circle cx="131" cy="88" r="1.3" fill="#3A2419"/>
      <circle cx="148" cy="88" r="1.3" fill="#3A2419"/>
      <path d="M118 86 C114 68, 130 58, 140 60 C150 58, 166 68, 162 86 C158 80, 152 74, 140 74 C128 74, 122 80, 118 86 Z" fill="#3A2419"/>
      {/* heart between */}
      <g className="heart-beat" style={{ transformOrigin: '109px 108px' }}>
        <path d="M109 98 C112 92, 122 92, 122 102 C122 112, 109 118, 109 118 C109 118, 96 112, 96 102 C96 92, 106 92, 109 98 Z" fill="#E8A4A4"/>
      </g>
      {/* leaf */}
      <path d="M38 200 Q28 180 42 170 Q46 188 38 200 Z" fill="#8FB09F" opacity="0.8"/>
      <path d="M180 210 Q190 200 188 188 Q176 196 180 210 Z" fill="#B8CFC2" opacity="0.8"/>
    </svg>
  );
}

window.HomePage = HomePage;
