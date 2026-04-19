// AuthPage — login/register in the warm aesthetic
const { useState: useStateAuth } = React;

function AuthPage({ navigate, mode: initialMode = 'login' }){
  const [mode, setMode] = useStateAuth(initialMode);
  const [email, setEmail] = useStateAuth('');
  const [password, setPassword] = useStateAuth('');
  const [showPwd, setShowPwd] = useStateAuth(false);

  return (
    <div className="warm-bg grain relative w-full h-full overflow-hidden flex items-center justify-center">
      {/* decorative floating blobs */}
      <div className="absolute -top-20 -left-20 float-slow" style={{ width: 260, height: 260, borderRadius: '50%', background: 'radial-gradient(circle, rgba(232,164,164,0.4), transparent 70%)', filter: 'blur(20px)' }}/>
      <div className="absolute -bottom-24 -right-10 float-med" style={{ width: 320, height: 320, borderRadius: '50%', background: 'radial-gradient(circle, rgba(245,196,168,0.5), transparent 70%)', filter: 'blur(30px)' }}/>
      <div className="absolute top-40 right-40 float-slow" style={{ animationDelay: '1s', width: 160, height: 160, borderRadius: '50%', background: 'radial-gradient(circle, rgba(143,176,159,0.3), transparent 70%)', filter: 'blur(20px)' }}/>

      <div className="relative z-10 flex flex-col md:flex-row items-stretch gap-6 max-w-[920px] w-full px-6 fade-up">

        {/* Left: illustration + copy */}
        <div className="flex-1 flex flex-col justify-center p-8 relative overflow-hidden"
          style={{ background: 'linear-gradient(145deg, #FCE7D5 0%, #F5C4A8 100%)', borderRadius: 'var(--r-xl)', minHeight: 480, boxShadow: 'var(--shadow-card)' }}>
          <div className="absolute top-6 left-6">
            <BrandMark size={40}/>
          </div>
          <div className="absolute -bottom-4 -right-4 opacity-90 float-slow" style={{ width: 200, height: 240 }}>
            <svg viewBox="0 0 200 240">
              <circle cx="100" cy="80" r="26" fill="#F5D8D8"/>
              <path d="M74 110 C74 110, 80 190, 100 190 C120 190, 126 110, 126 110 L126 220 L74 220 Z" fill="#FFFAF5" opacity="0.8"/>
              <path d="M94 72 Q100 66 106 72" stroke="#3A2419" strokeWidth="1.4" strokeLinecap="round"/>
              <circle cx="91" cy="78" r="1.3" fill="#3A2419"/>
              <circle cx="108" cy="78" r="1.3" fill="#3A2419"/>
              <path d="M76 76 C72 60, 86 48, 100 50 C114 48, 128 62, 124 78 C122 72, 114 66, 100 66 C86 66, 78 72, 76 76 Z" fill="#6B4A38"/>
              <g className="heart-beat" style={{ transformOrigin: '100px 110px' }}>
                <path d="M100 98 C103 92, 113 92, 113 102 C113 112, 100 118, 100 118 C100 118, 87 112, 87 102 C87 92, 97 92, 100 98 Z" fill="#E8A4A4"/>
              </g>
            </svg>
          </div>
          <div className="relative z-10 mt-20">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 text-[11px] font-semibold uppercase mb-5"
              style={{ background: 'rgba(255,250,245,0.4)', color: '#A0624A', borderRadius: 999, letterSpacing: '0.14em' }}>
              ✨ 欢迎回来
            </div>
            <h1 className="display text-[40px] leading-tight" style={{ color: 'var(--text-ink)' }}>
              爱不是一个人<br/>
              读懂全世界
            </h1>
            <p className="mt-4 max-w-[320px] text-[14px] leading-relaxed" style={{ color: 'var(--text-body)' }}>
              登录后，你的聊天记录会被安全加密保存，只有你自己能看见。
            </p>
            <div className="mt-8 flex items-center gap-3">
              <div className="flex -space-x-2">
                <Avatar name="棠" tone="rose" size={28}/>
                <Avatar name="叶" tone="sage" size={28}/>
                <Avatar name="月" tone="peach" size={28}/>
              </div>
              <span className="text-[11px]" style={{ color: 'var(--text-body)' }}>已有 12,480+ 朋友在这里</span>
            </div>
          </div>
        </div>

        {/* Right: form */}
        <div className="flex-1 soft-card p-8 md:p-10 flex flex-col justify-center" style={{ minHeight: 480 }}>
          {/* tab switch */}
          <div className="flex gap-1 p-1 mb-6" style={{ background: 'var(--bg-peach)', borderRadius: 999, alignSelf: 'flex-start' }}>
            <button onClick={() => setMode('login')} className="px-5 py-1.5 text-sm font-medium"
              style={{ borderRadius: 999, background: mode === 'login' ? '#FFFDF9' : 'transparent', color: mode === 'login' ? 'var(--primary-dark)' : 'var(--text-muted)', boxShadow: mode === 'login' ? '0 2px 8px rgba(196,123,90,0.12)' : 'none' }}>
              登录
            </button>
            <button onClick={() => setMode('register')} className="px-5 py-1.5 text-sm font-medium"
              style={{ borderRadius: 999, background: mode === 'register' ? '#FFFDF9' : 'transparent', color: mode === 'register' ? 'var(--primary-dark)' : 'var(--text-muted)', boxShadow: mode === 'register' ? '0 2px 8px rgba(196,123,90,0.12)' : 'none' }}>
              注册
            </button>
          </div>

          <h2 className="display text-[26px] mb-1" style={{ color: 'var(--text-ink)' }}>
            {mode === 'login' ? '很高兴又见到你' : '来，一起开始吧'}
          </h2>
          <p className="text-[13px] mb-6" style={{ color: 'var(--text-muted)' }}>
            {mode === 'login' ? '你的小助手已经在这里等着了 🌸' : '30 秒注册，立刻开启温柔陪伴'}
          </p>

          <form className="flex flex-col gap-4">
            <Field icon={<Icon.Mail size={16}/>} type="email" value={email} onChange={setEmail} placeholder="you@example.com" label="邮箱"/>
            <Field icon={<Icon.Lock size={16}/>} type={showPwd ? 'text' : 'password'} value={password} onChange={setPassword} placeholder="••••••••" label="密码"
              right={
                <button type="button" onClick={()=>setShowPwd(!showPwd)} className="grid place-items-center" style={{ width: 28, height: 28, color: 'var(--text-muted)' }}>
                  <Icon.Eye size={14}/>
                </button>
              }/>

            {mode === 'login' && (
              <div className="flex items-center justify-between text-[12px]">
                <label className="flex items-center gap-1.5 cursor-pointer" style={{ color: 'var(--text-body)' }}>
                  <input type="checkbox" defaultChecked className="accent-orange-400" style={{ accentColor: 'var(--primary)' }}/>
                  记住我
                </label>
                <button type="button" className="hover:underline" style={{ color: 'var(--primary-dark)' }}>忘记密码？</button>
              </div>
            )}

            <button type="button" onClick={() => navigate('chat')} className="btn-primary mt-2">
              {mode === 'login' ? '登录' : '创建账号'}
            </button>

            <div className="flex items-center gap-3 my-1">
              <div className="flex-1 dot-divider"/>
              <span className="text-[11px]" style={{ color: 'var(--text-muted)' }}>或者</span>
              <div className="flex-1 dot-divider"/>
            </div>

            <button type="button" className="flex items-center justify-center gap-2 py-2.5 text-sm font-medium"
              style={{ background: '#FFFDF9', border: '1px solid var(--border-soft)', borderRadius: 999, color: 'var(--text-ink)' }}>
              <svg width="16" height="16" viewBox="0 0 18 18"><path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.17-1.84H9v3.48h4.84a4.14 4.14 0 01-1.79 2.72v2.26h2.9a8.78 8.78 0 002.69-6.62z"/><path fill="#34A853" d="M9 18a8.6 8.6 0 005.95-2.18l-2.9-2.26a5.4 5.4 0 01-8.03-2.84H.92v2.33A9 9 0 009 18z"/><path fill="#FBBC05" d="M3.96 10.72A5.4 5.4 0 013.68 9c0-.6.1-1.18.28-1.72V4.95H.92A9 9 0 000 9a9 9 0 00.92 4.05l3.04-2.33z"/><path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 009 0 9 9 0 00.92 4.95l3.04 2.33A5.4 5.4 0 019 3.58z"/></svg>
              使用 Google 继续
            </button>
          </form>

          <div className="mt-6 text-[11px] text-center" style={{ color: 'var(--text-muted)' }}>
            继续即表示同意 <a style={{ color: 'var(--primary-dark)' }}>服务条款</a> 和 <a style={{ color: 'var(--primary-dark)' }}>隐私政策</a>
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ icon, type, value, onChange, placeholder, label, right }){
  const [focus, setFocus] = useStateAuth(false);
  return (
    <label className="block">
      <div className="text-[12px] mb-1.5" style={{ color: 'var(--text-body)', fontWeight: 500 }}>{label}</div>
      <div
        className="flex items-center gap-2 px-3 py-2.5 transition-all"
        style={{
          background: '#FFFDF9',
          border: `1.5px solid ${focus ? 'var(--primary)' : 'var(--border-soft)'}`,
          borderRadius: 14,
          boxShadow: focus ? '0 0 0 4px rgba(232,155,122,0.12)' : 'none',
        }}>
        <span style={{ color: focus ? 'var(--primary-dark)' : 'var(--text-muted)' }}>{icon}</span>
        <input
          type={type}
          value={value}
          onChange={e => onChange(e.target.value)}
          onFocus={() => setFocus(true)}
          onBlur={() => setFocus(false)}
          placeholder={placeholder}
          className="flex-1 bg-transparent text-[14px] outline-none"
          style={{ color: 'var(--text-ink)', border: 'none' }}
        />
        {right}
      </div>
    </label>
  );
}

window.AuthPage = AuthPage;
