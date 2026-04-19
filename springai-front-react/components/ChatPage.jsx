// ChatPage — classic left sidebar + chat area
const { useState: useStateChat, useEffect: useEffectChat, useRef: useRefChat } = React;

const MOCK_CHATS = [
  { id: 'c1', title: '他总是已读不回，好难受', time: '今天 14:32', active: true, running: false },
  { id: 'c2', title: '第一次约会要不要送花', time: '昨天', active: false, running: true },
  { id: 'c3', title: '分析下这段聊天记录', time: '昨天', active: false },
  { id: 'c4', title: '周五想约她看电影', time: '3 月 18 日', active: false },
  { id: 'c5', title: '怎么拒绝暧昧又不伤感情', time: '3 月 14 日', active: false },
  { id: 'c6', title: '吵架了该先道歉吗', time: '3 月 10 日', active: false },
];

const MOCK_MESSAGES_DEMO = [
  { role: 'user', content: '我发了消息给他，隔了两个小时才回我，就一个"嗯"。我是不是太敏感了…', time: '14:30' },
  {
    role: 'ai',
    time: '14:30',
    thinking: true,
    steps: [
      { label: '读懂你现在的情绪', done: true },
      { label: '梳理这段对话的上下文', done: true },
      { label: '查找类似情境的参考', done: true },
      { label: '组织给你的回答', active: true },
    ],
  },
  { role: 'ai', content: '你不是太敏感，你只是在意他。\n\n两小时 + 一个"嗯"，换谁都会心里咯噔一下。这不是矫情，是你对这段关系有期待。\n\n想不想我陪你一起看看，他今天的状态可能是什么样？或者我们先聊聊，你此刻最想要的是一句什么话？', time: '14:31', probability: true },
  { role: 'user', content: '我想知道他是不是不喜欢我了', time: '14:32' },
];

// Which chats have messages (demo purposes — c2 has the sample convo)
const CHAT_HAS_MESSAGES = { c2: true };

function ChatPage({ tweaks, navigate }){
  const [sidebarOpen, setSidebarOpen] = useStateChat(true);
  const [input, setInput] = useStateChat('');
  const [activeChat, setActiveChat] = useStateChat('c1');
  const [mode, setMode] = useStateChat('companion'); // companion | coach
  const messagesEnd = useRefChat(null);
  const hasMessages = !!CHAT_HAS_MESSAGES[activeChat];

  useEffectChat(() => {
    if (hasMessages) messagesEnd.current?.scrollTo({ top: 999999, behavior: 'smooth' });
  }, [activeChat]);

  const bubbleRadius = tweaks.bubbleStyle === 'sharp' ? 10
    : tweaks.bubbleStyle === 'sticker' ? 26
    : 20;
  const bubbleStickerShadow = tweaks.bubbleStyle === 'sticker';

  const sidebarW = tweaks.sidebarStyle === 'icon' ? 72 : 276;
  const showSidebar = sidebarOpen && tweaks.sidebarStyle !== 'hidden';

  return (
    <div className="warm-bg grain relative w-full h-full flex overflow-hidden">

      {/* ── SIDEBAR ── */}
      {showSidebar && (
        <aside className="relative flex flex-col fade-up"
          style={{
            width: sidebarW, flexShrink: 0, height: '100%',
            background: tweaks.sidebarStyle === 'classic' ? 'var(--bg-sidebar)' : 'rgba(247, 235, 221, 0.55)',
            backdropFilter: 'blur(20px)',
            borderRight: tweaks.sidebarStyle === 'classic' ? '1px solid var(--border-soft)' : 'none',
            padding: tweaks.sidebarStyle === 'icon' ? '14px 10px' : '16px 14px',
            transition: 'width .25s ease',
          }}>
          {/* Brand + home */}
          <div className="flex items-center gap-2 mb-4" style={{ padding: '4px' }}>
            <button onClick={() => navigate('home')}><BrandMark size={34}/></button>
            {tweaks.sidebarStyle !== 'icon' && (
              <div className="flex-1">
                <div className="display text-[16px] leading-none" style={{ color: 'var(--text-ink)' }}>Love Master</div>
                <div className="text-[10px] mt-1" style={{ color: 'var(--text-muted)', letterSpacing: '0.12em', fontFamily: 'var(--font-en)' }}>COMPANION</div>
              </div>
            )}
            <button className="grid place-items-center" style={{ width: 32, height: 32, borderRadius: 10, color: 'var(--text-body)' }}
              onClick={() => setSidebarOpen(false)}>
              <Icon.ChevronLeft size={16}/>
            </button>
          </div>

          {/* Mode switcher */}
          {tweaks.sidebarStyle !== 'icon' && (
            <div className="mb-4 p-1 flex gap-1" style={{ background: 'rgba(255,253,249,0.6)', borderRadius: 999, border: '1px solid var(--border-soft)' }}>
              <ModeBtn active={mode === 'companion'} onClick={() => setMode('companion')} icon={<Icon.Heart size={13}/>}>陪伴</ModeBtn>
              <ModeBtn active={mode === 'coach'} onClick={() => setMode('coach')} icon={<Icon.BookOpen size={13}/>}>教练</ModeBtn>
            </div>
          )}

          {/* New chat */}
          <button className="flex items-center gap-2 mb-3 w-full" style={{
            padding: tweaks.sidebarStyle === 'icon' ? '10px' : '10px 14px',
            background: 'linear-gradient(135deg, #F2A987, #E89B7A)',
            color: '#FFFAF5',
            borderRadius: 14,
            boxShadow: '0 6px 16px rgba(232,155,122,0.32), inset 0 1px 0 rgba(255,255,255,0.4)',
            justifyContent: tweaks.sidebarStyle === 'icon' ? 'center' : 'flex-start',
          }}>
            <Icon.Plus size={16}/>
            {tweaks.sidebarStyle !== 'icon' && <span className="text-sm font-medium">开启一段新对话</span>}
          </button>

          {tweaks.sidebarStyle !== 'icon' && (
            <div className="text-[10px] uppercase tracking-widest mb-2 px-2" style={{ color: 'var(--text-muted)', letterSpacing: '0.16em' }}>最近</div>
          )}

          {/* Chat list */}
          <div className="flex-1 overflow-y-auto nice-scroll">
            {MOCK_CHATS.map(c => (
              <ChatListItem key={c.id} chat={c} activeId={activeChat} setActiveId={setActiveChat} iconMode={tweaks.sidebarStyle === 'icon'}/>
            ))}
          </div>

          {/* User footer */}
          {tweaks.sidebarStyle !== 'icon' && (
            <div className="mt-2 pt-3 flex items-center gap-2.5" style={{ borderTop: '1px dashed var(--border-soft)' }}>
              <Avatar name="我" tone="peach" size={34}/>
              <div className="flex-1 min-w-0">
                <div className="text-[13px] truncate" style={{ color: 'var(--text-ink)' }}>你好 · 小月亮</div>
                <div className="text-[10px]" style={{ color: 'var(--text-muted)' }}>Pro 会员 · 127 次对话</div>
              </div>
              <button className="grid place-items-center" style={{ width: 28, height: 28, borderRadius: 8, color: 'var(--text-muted)' }}>
                <Icon.Settings size={15}/>
              </button>
            </div>
          )}
        </aside>
      )}

      {!showSidebar && (
        <button onClick={() => setSidebarOpen(true)}
          className="fixed top-4 left-4 z-20 grid place-items-center soft-card"
          style={{ width: 40, height: 40, borderRadius: 12 }}>
          <Icon.Menu size={18}/>
        </button>
      )}

      {/* ── MAIN CHAT AREA ── */}
      <div className="flex-1 flex flex-col relative min-w-0">

        {/* Header */}
        <header className="flex items-center justify-between px-6 py-3.5"
          style={{ borderBottom: '1px solid var(--border-soft)', background: 'rgba(255,253,249,0.6)', backdropFilter: 'blur(12px)' }}>
          <div className="flex items-center gap-3">
            <button onClick={() => navigate('home')} className="btn-ghost flex items-center gap-1.5 text-xs">
              <Icon.Home size={14}/> 回家
            </button>
            <div className="h-5 w-px" style={{ background: 'var(--border-soft)' }}/>
            <div className="flex items-center gap-2">
              <div className="inline-flex items-center gap-1.5 text-[11px] px-2.5 py-1" style={{ background: mode === 'companion' ? 'var(--bg-peach)' : 'var(--sage-soft)', color: mode === 'companion' ? 'var(--primary-dark)' : '#4C7566', borderRadius: 999, fontWeight: 500 }}>
                {mode === 'companion' ? <Icon.Heart size={11}/> : <Icon.BookOpen size={11}/>}
                {mode === 'companion' ? '陪伴模式' : '教练模式'}
              </div>
              <div className="display text-[17px]" style={{ color: 'var(--text-ink)' }}>他总是已读不回，好难受</div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>
              <span className="inline-block w-1.5 h-1.5 rounded-full mr-1.5" style={{ background: '#8FB09F', boxShadow: '0 0 0 2px rgba(143,176,159,0.3)' }}/>
              在线
            </span>
          </div>
        </header>

        {/* Messages / Welcome */}
        <div ref={messagesEnd} className="flex-1 overflow-y-auto hide-scroll">
          {!hasMessages ? (
            /* ── Centered empty / welcome state ── */
            <div className="h-full flex flex-col items-center justify-center px-6 pb-8 gap-6 fade-up">
              <div className="breathe">
                <BrandMark size={64}/>
              </div>
              <div className="text-center">
                <h2 className="display text-[28px] mb-2" style={{ color: 'var(--text-ink)' }}>
                  {mode === 'companion' ? '今天，想聊点什么？' : '把聊天记录发给我看看'}
                </h2>
                <p className="text-[15px] max-w-[400px] leading-relaxed" style={{ color: 'var(--text-body)' }}>
                  {mode === 'companion'
                    ? '不管是高兴还是难受，都可以说给我听。没有评判，只有陪伴。'
                    : '我帮你分析对话里的细节，给出最懂人心的建议。'}
                </p>
              </div>
              {/* Suggestion chips */}
              <div className="flex flex-wrap gap-2 justify-center max-w-[480px]">
                {(mode === 'companion'
                  ? ['他今天没回我消息', '我们刚吵了架', '想约 TA 出去不知道怎么说', '心情有点乱']
                  : ['帮我看看这段对话', '他说这句话是什么意思', '我该怎么回复', '分析一下对方的态度']
                ).map(s => (
                  <button key={s}
                    onClick={() => setInput(s)}
                    className="text-[13px] px-4 py-2"
                    style={{
                      background: 'rgba(255,253,249,0.7)',
                      border: '1px solid var(--border-soft)',
                      borderRadius: 999,
                      color: 'var(--text-body)',
                      transition: 'all .2s',
                    }}
                    onMouseEnter={e => { e.currentTarget.style.background='#FFFDF9'; e.currentTarget.style.color='var(--primary-dark)'; e.currentTarget.style.borderColor='var(--primary-soft)'; }}
                    onMouseLeave={e => { e.currentTarget.style.background='rgba(255,253,249,0.7)'; e.currentTarget.style.color='var(--text-body)'; e.currentTarget.style.borderColor='var(--border-soft)'; }}>
                    {s}
                  </button>
                ))}
              </div>
              <p className="text-[11px]" style={{ color: 'var(--text-faint)' }}>
                Lovemaster 会犯错，重要的决定请由你来做
              </p>
            </div>
          ) : (
            /* ── Messages list ── */
            <div className="px-6 py-6">
              <div className="max-w-[760px] mx-auto flex flex-col gap-4">
                <div className="flex items-center gap-3 my-2">
                  <div className="flex-1 dot-divider"/>
                  <div className="text-[11px] px-3 py-1" style={{ background: 'rgba(255,253,249,0.7)', borderRadius: 999, color: 'var(--text-muted)', border: '1px solid var(--border-soft)' }}>
                    2026 年 4 月 19 日 · 下午好呀
                  </div>
                  <div className="flex-1 dot-divider"/>
                </div>
                {MOCK_MESSAGES_DEMO.map((m, i) => (
                  <MessageRow key={i} msg={m} bubbleRadius={bubbleRadius} stickerShadow={bubbleStickerShadow}/>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Input dock */}
        <div className="px-6 pb-6 pt-2">
          <div className="max-w-[760px] mx-auto">

            <div className="soft-card relative" style={{ padding: '14px 16px 12px', boxShadow: '0 10px 32px rgba(196,123,90,0.1), inset 0 1px 0 rgba(255,255,255,0.8)' }}>
              <textarea
                value={input}
                onChange={e => setInput(e.target.value)}
                placeholder="把今天的心情，慢慢说给我听…"
                className="w-full resize-none body text-[15px] leading-relaxed bg-transparent"
                style={{ color: 'var(--text-ink)', height: 60, border: 'none' }}
                rows={2}
              />
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5">
                  <IconBtn title="上传截图"><Icon.Image size={16}/></IconBtn>
                  <IconBtn title="情绪标记"><span style={{fontSize:16}}>🌸</span></IconBtn>
                  <IconBtn title="快捷模板"><Icon.Sparkles size={16}/></IconBtn>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>{input.length}/2000</span>
                  <button className="grid place-items-center"
                    disabled={!input.trim()}
                    style={{
                      width: 40, height: 40, borderRadius: 14,
                      background: input.trim() ? 'linear-gradient(135deg, #F2A987, #E89B7A)' : '#EDE4D8',
                      color: input.trim() ? '#FFFAF5' : '#B09080',
                      boxShadow: input.trim() ? '0 8px 20px rgba(232,155,122,0.4)' : 'none',
                      transition: 'all .2s',
                    }}>
                    <Icon.Send size={16}/>
                  </button>
                </div>
              </div>
            </div>
            <div className="text-center mt-2 text-[10.5px]" style={{ color: 'var(--text-faint)' }}>
              Lovemaster 会犯错，重要的决定请由你来做 · Enter 发送，Shift+Enter 换行
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ChatListItem({ chat, activeId, setActiveId, iconMode }){
  const isActive = activeId === chat.id;
  if (iconMode){
    return (
      <button onClick={() => setActiveId(chat.id)} className="grid place-items-center my-1"
        style={{ width: 48, height: 48, borderRadius: 14, background: isActive ? '#FFFDF9' : 'transparent', color: isActive ? 'var(--primary-dark)' : 'var(--text-body)' }}>
        <Icon.MessageCircle size={17}/>
      </button>
    );
  }
  return (
    <button onClick={() => setActiveId(chat.id)} className="w-full text-left flex items-start gap-2.5 px-2.5 py-2.5 mb-0.5 group"
      style={{
        borderRadius: 14,
        background: isActive ? '#FFFDF9' : 'transparent',
        boxShadow: isActive ? '0 4px 12px rgba(196,123,90,0.08), inset 0 1px 0 rgba(255,255,255,0.8)' : 'none',
        transition: 'background .2s',
      }}>
      <div className="mt-0.5 flex-shrink-0">
        {chat.running ? (
          <span className="inline-block" style={{ width: 10, height: 10, borderRadius: '50%', border: '2px solid rgba(232,155,122,0.3)', borderTopColor: '#E89B7A', animation: 'spin 1s linear infinite' }}/>
        ) : (
          <span className="inline-block" style={{ width: 8, height: 8, borderRadius: '50%', background: isActive ? '#E89B7A' : '#D8BF9F', opacity: isActive ? 1 : 0.5, marginTop: 2, marginLeft: 1 }}/>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-[13px] leading-snug truncate" style={{ color: isActive ? 'var(--text-ink)' : 'var(--text-body)', fontWeight: isActive ? 500 : 400 }}>
          {chat.title}
        </div>
        <div className="text-[10.5px] mt-0.5" style={{ color: 'var(--text-muted)' }}>
          {chat.running ? '正在生成回复…' : chat.time}
        </div>
      </div>
      <button className="grid place-items-center opacity-0 group-hover:opacity-60 hover:!opacity-100 transition-opacity"
        style={{ width: 22, height: 22, borderRadius: 6, color: 'var(--text-muted)' }}>
        <Icon.Trash size={12}/>
      </button>
    </button>
  );
}

function ModeBtn({ active, onClick, icon, children }){
  return (
    <button onClick={onClick} className="flex-1 flex items-center justify-center gap-1 py-1.5 text-xs font-medium"
      style={{
        borderRadius: 999,
        background: active ? 'linear-gradient(135deg, #F2A987, #E89B7A)' : 'transparent',
        color: active ? '#FFFAF5' : 'var(--text-body)',
        boxShadow: active ? '0 4px 10px rgba(232,155,122,0.3)' : 'none',
        transition: 'all .2s',
      }}>
      {icon} {children}
    </button>
  );
}

function QuickChip({ children }){
  return (
    <button className="text-[12px] px-3 py-1.5"
      style={{
        background: 'rgba(255,253,249,0.65)',
        color: 'var(--text-body)',
        border: '1px solid var(--border-soft)',
        borderRadius: 999,
        transition: 'all .2s',
      }}
      onMouseEnter={e => { e.currentTarget.style.background='#FFFDF9'; e.currentTarget.style.color='var(--primary-dark)'; }}
      onMouseLeave={e => { e.currentTarget.style.background='rgba(255,253,249,0.65)'; e.currentTarget.style.color='var(--text-body)'; }}>
      {children}
    </button>
  );
}

function IconBtn({ children, title }){
  return (
    <button title={title} className="grid place-items-center"
      style={{ width: 32, height: 32, borderRadius: 10, color: 'var(--text-body)', transition: 'all .2s' }}
      onMouseEnter={e => { e.currentTarget.style.background='var(--bg-peach)'; e.currentTarget.style.color='var(--primary-dark)'; }}
      onMouseLeave={e => { e.currentTarget.style.background='transparent'; e.currentTarget.style.color='var(--text-body)'; }}>
      {children}
    </button>
  );
}

function MessageRow({ msg, bubbleRadius, stickerShadow }){
  if (msg.role === 'user'){
    return (
      <div className="flex justify-end gap-2 bubble-in">
        <div className="flex flex-col items-end gap-1" style={{ maxWidth: '78%' }}>
          <div
            style={{
              background: 'linear-gradient(135deg, #F2A987 0%, #E89B7A 100%)',
              color: '#FFFAF5',
              padding: '12px 16px',
              borderRadius: `${bubbleRadius}px ${bubbleRadius}px 6px ${bubbleRadius}px`,
              boxShadow: stickerShadow
                ? '0 6px 16px rgba(232,155,122,0.3), inset 0 1px 0 rgba(255,255,255,0.3), inset 0 -2px 0 rgba(160,98,74,0.25)'
                : '0 6px 16px rgba(232,155,122,0.2)',
              fontSize: 15, lineHeight: 1.65,
            }}>
            {msg.content}
          </div>
          <div className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{msg.time}</div>
        </div>
        <Avatar name="我" tone="peach" size={32}/>
      </div>
    );
  }

  if (msg.thinking){
    return (
      <div className="flex gap-2 bubble-in">
        <BrandMark size={32}/>
        <div className="flex-1 max-w-[80%]">
          <div
            style={{
              background: '#FFFDF9',
              border: '1px solid var(--border-soft)',
              padding: '14px 18px',
              borderRadius: `6px ${bubbleRadius}px ${bubbleRadius}px ${bubbleRadius}px`,
              boxShadow: '0 4px 14px rgba(196,123,90,0.06)',
            }}>
            <div className="flex items-center gap-2 mb-3">
              <span className="inline-flex gap-1">
                <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--primary)', animation: 'dotPulse 1.4s ease-in-out infinite' }}/>
                <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--primary)', animation: 'dotPulse 1.4s ease-in-out infinite .2s' }}/>
                <span className="inline-block w-1.5 h-1.5 rounded-full" style={{ background: 'var(--primary)', animation: 'dotPulse 1.4s ease-in-out infinite .4s' }}/>
              </span>
              <span className="shimmer-text text-[13px]">正在温柔思考…</span>
            </div>
            {msg.steps.map((s, i) => (
              <div key={i} className="flex items-center gap-2 py-1.5 text-[12.5px]" style={{ color: s.done ? 'var(--text-muted)' : 'var(--text-body)' }}>
                {s.done ? (
                  <span className="grid place-items-center" style={{ width: 16, height: 16, borderRadius: '50%', background: 'var(--sage-soft)', color: '#4C7566' }}>
                    <Icon.Check size={10}/>
                  </span>
                ) : s.active ? (
                  <span className="inline-block" style={{ width: 12, height: 12, borderRadius: '50%', border: '2px solid rgba(232,155,122,0.3)', borderTopColor: '#E89B7A', animation: 'spin 1s linear infinite', marginLeft: 2 }}/>
                ) : (
                  <span className="inline-block" style={{ width: 6, height: 6, borderRadius: '50%', background: 'var(--text-faint)', marginLeft: 5 }}/>
                )}
                <span style={{ fontWeight: s.active ? 500 : 400 }}>{s.label}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  // regular AI message
  return (
    <div className="flex gap-2 bubble-in">
      <BrandMark size={32}/>
      <div className="flex flex-col gap-1.5" style={{ maxWidth: '80%' }}>
        <div
          style={{
            background: '#FFFDF9',
            border: '1px solid var(--border-soft)',
            padding: '14px 18px',
            borderRadius: `6px ${bubbleRadius}px ${bubbleRadius}px ${bubbleRadius}px`,
            boxShadow: stickerShadow
              ? '0 6px 14px rgba(196,123,90,0.08), inset 0 1px 0 rgba(255,255,255,0.9), inset 0 -2px 0 rgba(196,123,90,0.06)'
              : '0 4px 14px rgba(196,123,90,0.06)',
            fontSize: 15, lineHeight: 1.75,
            whiteSpace: 'pre-wrap',
            color: 'var(--text-ink)',
          }}>
          {msg.content}
        </div>

        {msg.probability && (
          <div className="soft-card relative overflow-hidden" style={{ padding: '14px 16px', marginTop: 2 }}>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-1.5 text-[11.5px]" style={{ color: 'var(--text-body)', fontWeight: 500 }}>
                <Icon.Sparkles size={12} stroke="var(--primary-dark)"/>
                <span>他此刻可能的心情分析</span>
              </div>
              <span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>仅供参考</span>
            </div>
            <div className="space-y-2.5">
              <ProbabilityBar label="工作在忙 · 没顾上" value={52} tone="peach"/>
              <ProbabilityBar label="情绪一般 · 不想深聊" value={28} tone="rose"/>
              <ProbabilityBar label="失去兴趣" value={12} tone="sage"/>
              <ProbabilityBar label="其他原因" value={8} tone="cream"/>
            </div>
          </div>
        )}

        <div className="flex items-center gap-2 mt-0.5">
          <span className="text-[10px]" style={{ color: 'var(--text-muted)' }}>{msg.time}</span>
          <button className="text-[10px] px-2 py-0.5" style={{ color: 'var(--text-muted)', borderRadius: 6 }}>复制</button>
          <button className="text-[10px] px-2 py-0.5" style={{ color: 'var(--text-muted)', borderRadius: 6 }}>重新回答</button>
          <span className="flex-1"/>
          <button className="text-[11px]">👍</button>
          <button className="text-[11px]">👎</button>
        </div>
      </div>
    </div>
  );
}

function ProbabilityBar({ label, value, tone }){
  const fills = {
    peach: 'linear-gradient(90deg, #F2A987, #E89B7A)',
    rose:  'linear-gradient(90deg, #F5D8D8, #E8A4A4)',
    sage:  'linear-gradient(90deg, #D5E3DA, #8FB09F)',
    cream: 'linear-gradient(90deg, #F5E4D1, #D8BF9F)',
  };
  return (
    <div>
      <div className="flex justify-between items-center text-[11.5px] mb-1" style={{ color: 'var(--text-body)' }}>
        <span>{label}</span><span style={{ color: 'var(--text-muted)', fontFamily: 'var(--font-en)', fontWeight: 600 }}>{value}%</span>
      </div>
      <div style={{ height: 6, borderRadius: 999, background: 'rgba(232,155,122,0.1)', overflow: 'hidden' }}>
        <div style={{ width: `${value}%`, height: '100%', background: fills[tone], borderRadius: 999, transition: 'width .5s ease' }}/>
      </div>
    </div>
  );
}

window.ChatPage = ChatPage;
