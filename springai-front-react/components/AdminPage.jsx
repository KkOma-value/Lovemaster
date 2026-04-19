// AdminPage — knowledge review
const { useState: useStateAdmin } = React;

const REVIEW_ITEMS = [
  { id: 1, cat: '情感分析', q: '怎么判断他是不是真的在意我？', a: '从三个维度看：响应速度、主动提起的频率，以及面对冲突时的态度。但记住——真实的在意不需要"判断"，它会在日常细节里自然流露。', status: 'pending', author: '小莓', time: '14:32' },
  { id: 2, cat: '话术建议', q: '冷战了三天，怎么先开口又不显得卑微？', a: '可以用「关心事实，不追问态度」的句式：「看你好几天没吃夜宵，今天公司楼下开了家新的馄饨，要不要带一份给你？」让台阶自然出现。', status: 'pending', author: 'AI', time: '13:50' },
  { id: 3, cat: '心理陪伴', q: '分手了怎么才能好起来？', a: '不要急着"好起来"。允许自己难过，是恢复的第一步。', status: 'pending', author: '小月', time: '11:20' },
  { id: 4, cat: '分析建议', q: '他总说忙，但朋友圈发得很勤', a: '「忙」也有很多种，可能是真的忙、不想专心回你、或已经在和别人聊。需要结合更多上下文判断。', status: 'pending', author: 'AI', time: '10:15' },
];

function AdminPage({ navigate }){
  const [items, setItems] = useStateAdmin(REVIEW_ITEMS);
  const [selected, setSelected] = useStateAdmin(REVIEW_ITEMS[0].id);
  const [filter, setFilter] = useStateAdmin('all');
  const current = items.find(i => i.id === selected);

  const act = (id, status) => {
    setItems(items.map(i => i.id === id ? {...i, status} : i));
  };

  const stats = [
    { label: '待审核', value: items.filter(i=>i.status==='pending').length, tone: 'peach' },
    { label: '今日通过', value: 18, tone: 'sage' },
    { label: '今日驳回', value: 2, tone: 'rose' },
    { label: '总条目', value: 1248, tone: 'cream' },
  ];

  return (
    <div className="warm-bg grain relative w-full h-full overflow-hidden flex flex-col">

      <header className="flex items-center justify-between px-6 py-3.5"
        style={{ borderBottom: '1px solid var(--border-soft)', background: 'rgba(255,253,249,0.7)', backdropFilter: 'blur(12px)' }}>
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('home')}><BrandMark size={32}/></button>
          <div>
            <div className="display text-[17px] leading-none" style={{ color: 'var(--text-ink)' }}>知识审核</div>
            <div className="text-[10.5px]" style={{ color: 'var(--text-muted)', letterSpacing: '0.12em', fontFamily: 'var(--font-en)' }}>ADMIN · KNOWLEDGE REVIEW</div>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('chat')} className="btn-ghost text-xs">去聊天</button>
          <button onClick={() => navigate('home')} className="btn-ghost text-xs">回首页</button>
          <Avatar name="审" tone="sage" size={32}/>
        </div>
      </header>

      <div className="flex-1 overflow-hidden flex flex-col p-5 gap-4 max-w-[1200px] mx-auto w-full">

        {/* Stats */}
        <div className="grid grid-cols-4 gap-3 fade-up">
          {stats.map(s => (
            <div key={s.label} className="soft-card p-4 relative overflow-hidden">
              <div className="text-[11px]" style={{ color: 'var(--text-muted)' }}>{s.label}</div>
              <div className="display text-[28px] mt-1" style={{ color: 'var(--text-ink)' }}>{s.value}</div>
              <div className="absolute top-3 right-3 w-8 h-8 rounded-full" style={{
                background: s.tone === 'peach' ? 'var(--bg-peach)' : s.tone === 'sage' ? 'var(--sage-soft)' : s.tone === 'rose' ? '#F5D8D8' : '#F5E4D1',
                opacity: 0.6,
              }}/>
            </div>
          ))}
        </div>

        {/* Split view */}
        <div className="flex-1 grid grid-cols-12 gap-4 overflow-hidden fade-up" style={{ animationDelay: '0.1s' }}>

          {/* List */}
          <div className="col-span-5 soft-card flex flex-col overflow-hidden" style={{ padding: 0 }}>
            <div className="p-4 pb-3 flex items-center gap-2 flex-wrap" style={{ borderBottom: '1px solid var(--border-soft)' }}>
              {['all', 'pending', 'approved', 'rejected'].map(f => (
                <button key={f} onClick={()=>setFilter(f)}
                  className="text-[11px] px-2.5 py-1 font-medium"
                  style={{
                    borderRadius: 999,
                    background: filter === f ? 'var(--primary)' : 'rgba(255,253,249,0.6)',
                    color: filter === f ? '#FFFAF5' : 'var(--text-body)',
                    border: filter === f ? 'none' : '1px solid var(--border-soft)',
                  }}>
                  {{all:'全部', pending:'待审', approved:'通过', rejected:'驳回'}[f]}
                </button>
              ))}
              <span className="flex-1"/>
              <span className="text-[10.5px]" style={{ color: 'var(--text-muted)' }}>{items.length} 条</span>
            </div>
            <div className="flex-1 overflow-y-auto nice-scroll">
              {items.filter(i => filter === 'all' || i.status === filter).map(item => (
                <button key={item.id} onClick={()=>setSelected(item.id)}
                  className="w-full text-left px-4 py-3 flex gap-3 items-start"
                  style={{
                    background: selected === item.id ? 'var(--bg-peach)' : 'transparent',
                    borderBottom: '1px dashed var(--border-soft)',
                  }}>
                  <div style={{ width: 6, borderRadius: 3, flexShrink: 0, alignSelf: 'stretch',
                    background: item.status === 'approved' ? '#8FB09F' : item.status === 'rejected' ? '#E8A4A4' : '#E89B7A',
                    opacity: 0.7,
                  }}/>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <Chip tone={item.cat === '情感分析' ? 'rose' : item.cat === '话术建议' ? 'peach' : item.cat === '心理陪伴' ? 'sage' : 'cream'}>{item.cat}</Chip>
                      <span className="text-[10.5px]" style={{ color: 'var(--text-muted)' }}>{item.time}</span>
                    </div>
                    <div className="text-[13.5px] truncate" style={{ color: 'var(--text-ink)' }}>{item.q}</div>
                    <div className="text-[11.5px] mt-0.5 truncate" style={{ color: 'var(--text-muted)' }}>
                      {item.status === 'pending' ? '待审核' : item.status === 'approved' ? '✓ 已通过' : '✗ 已驳回'} · {item.author}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Detail */}
          <div className="col-span-7 soft-card flex flex-col overflow-hidden">
            {current && (
              <>
                <div className="p-5 flex items-start justify-between" style={{ borderBottom: '1px solid var(--border-soft)' }}>
                  <div>
                    <div className="flex items-center gap-2 mb-2">
                      <Chip tone={current.cat === '情感分析' ? 'rose' : current.cat === '话术建议' ? 'peach' : 'sage'}>{current.cat}</Chip>
                      <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>#{current.id}</span>
                      <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>· {current.author} · {current.time}</span>
                    </div>
                    <h3 className="display text-[20px] leading-snug" style={{ color: 'var(--text-ink)' }}>{current.q}</h3>
                  </div>
                  <button className="grid place-items-center" style={{ width: 32, height: 32, borderRadius: 10, color: 'var(--text-muted)' }}>
                    <Icon.Flag size={15}/>
                  </button>
                </div>

                <div className="flex-1 overflow-y-auto nice-scroll p-5">
                  <div className="text-[11px] uppercase tracking-widest mb-2" style={{ color: 'var(--text-muted)', letterSpacing: '0.16em' }}>AI 回答</div>
                  <div className="p-4 leading-[1.85] text-[14.5px]"
                    style={{ background: 'rgba(255,250,245,0.6)', borderRadius: 'var(--r-md)', border: '1px dashed var(--border-soft)', color: 'var(--text-ink)', whiteSpace:'pre-wrap' }}>
                    {current.a}
                  </div>

                  <div className="text-[11px] uppercase tracking-widest mt-5 mb-2" style={{ color: 'var(--text-muted)', letterSpacing: '0.16em' }}>审核意见</div>
                  <textarea placeholder="可以留下修改建议或通过备注…"
                    className="w-full p-3 text-[13px] leading-relaxed resize-none outline-none"
                    style={{ background: '#FFFDF9', border: '1px solid var(--border-soft)', borderRadius: 14, color: 'var(--text-ink)', height: 80, fontFamily: 'inherit' }}/>

                  <div className="flex items-center gap-2 mt-4">
                    <Chip tone="peach">温和</Chip>
                    <Chip tone="sage">有建设性</Chip>
                    <Chip tone="rose">需要更同理</Chip>
                    <Chip tone="cream">引用不当</Chip>
                    <button className="text-[11px] px-2.5 py-1" style={{ color: 'var(--text-muted)', borderRadius: 999, border: '1px dashed var(--border-soft)' }}>+ 自定义</button>
                  </div>
                </div>

                <div className="p-4 flex items-center gap-3 justify-end" style={{ borderTop: '1px solid var(--border-soft)' }}>
                  <button onClick={() => act(current.id, 'rejected')} className="px-5 py-2.5 text-sm font-medium"
                    style={{ background: '#FFFDF9', border: '1px solid rgba(232,164,164,0.4)', color: '#A55F5F', borderRadius: 999 }}>
                    ✗ 驳回
                  </button>
                  <button onClick={() => act(current.id, 'pending')} className="btn-ghost text-sm">标记待定</button>
                  <button onClick={() => act(current.id, 'approved')} className="btn-primary text-sm flex items-center gap-1.5">
                    <Icon.Check size={14}/> 通过发布
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

window.AdminPage = AdminPage;
