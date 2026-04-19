// Tweaks panel — floating bottom-right, many knobs
const { useState: useStateTweaks } = React;

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "primaryHue": 22,
  "primaryChroma": 0.12,
  "primaryLight": 0.72,
  "bgWarmth": 30,
  "radius": 28,
  "cardSoftness": 0.7,
  "displayFont": "ZCOOL KuaiLe",
  "bodyFont": "LXGW WenKai Screen",
  "fontScale": 1.0,
  "bubbleStyle": "soft",
  "density": "cozy",
  "grain": true,
  "heroIllustration": true,
  "showSage": true,
  "heroAccent": "rose",
  "sidebarStyle": "airy"
}/*EDITMODE-END*/;

function applyTokens(t){
  const r = document.documentElement.style;
  // primary via oklch
  const L = Math.max(0.3, Math.min(0.92, t.primaryLight));
  const C = Math.max(0, Math.min(0.25, t.primaryChroma));
  const H = t.primaryHue;
  r.setProperty('--primary', `oklch(${L} ${C} ${H})`);
  r.setProperty('--primary-dark', `oklch(${Math.max(0.35, L - 0.15)} ${C} ${H})`);
  r.setProperty('--primary-soft', `oklch(${Math.min(0.92, L + 0.1)} ${C * 0.7} ${H})`);

  // bg warmth — shifts cream
  const warm = t.bgWarmth;
  r.setProperty('--bg-cream', `oklch(0.97 ${0.02 + warm/2000} ${70 - warm/3})`);
  r.setProperty('--bg-peach', `oklch(0.92 ${0.05 + warm/1500} ${60 - warm/4})`);
  r.setProperty('--bg-sand', `oklch(0.9 ${0.04 + warm/1800} ${65 - warm/4})`);
  r.setProperty('--bg-card', `oklch(0.99 ${0.008 + warm/3500} ${75 - warm/4})`);

  // radii
  r.setProperty('--r-lg', `${t.radius}px`);
  r.setProperty('--r-md', `${Math.max(8, t.radius - 8)}px`);
  r.setProperty('--r-xl', `${t.radius + 8}px`);
  r.setProperty('--r-sm', `${Math.max(6, t.radius - 14)}px`);

  // fonts
  r.setProperty('--font-display', `"${t.displayFont}", system-ui, sans-serif`);
  r.setProperty('--font-body',    `"${t.bodyFont}", system-ui, sans-serif`);
  document.documentElement.style.fontSize = `${16 * t.fontScale}px`;
  document.body.style.fontSize = `${16 * t.fontScale}px`;
}

function TweaksPanel({ tweaks, setTweaks, open, setOpen }){
  const set = (k, v) => setTweaks(prev => ({ ...prev, [k]: v }));

  const fontChoices = ['ZCOOL KuaiLe', 'LXGW WenKai Screen', 'Inter'];
  const bubbles = ['soft', 'sharp', 'sticker'];
  const densities = ['cozy', 'compact', 'airy'];
  const heroAccents = ['rose', 'sage', 'peach', 'cream'];
  const sidebars = ['airy', 'classic', 'icon'];

  if(!open){
    return (
      <button
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-[100] grid place-items-center breathe"
        style={{
          width: 52, height: 52, borderRadius: '50%',
          background: 'linear-gradient(135deg, #F5C4A8, #E89B7A)',
          color: '#FFFAF5',
          boxShadow: '0 10px 28px rgba(232,155,122,0.4), inset 0 1px 0 rgba(255,255,255,0.4)',
        }}
        aria-label="Open tweaks"
      >
        <Icon.Settings size={22}/>
      </button>
    );
  }

  return (
    <div className="fixed bottom-5 right-5 z-[100] soft-card p-5 fade-up hide-scroll"
      style={{ width: 320, maxHeight: 'calc(100vh - 40px)', overflowY: 'auto' }}>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <div className="grid place-items-center" style={{ width: 32, height: 32, borderRadius: 10, background: 'var(--bg-peach)', color: 'var(--primary-dark)' }}>
            <Icon.Settings size={16}/>
          </div>
          <div>
            <div className="display text-base" style={{ color: 'var(--text-ink)' }}>Tweaks</div>
            <div className="text-[11px]" style={{ color: 'var(--text-muted)' }}>设计系统可调项</div>
          </div>
        </div>
        <button onClick={() => setOpen(false)} className="grid place-items-center" style={{ width: 28, height: 28, borderRadius: 8, color: 'var(--text-muted)' }}>
          <Icon.X size={16}/>
        </button>
      </div>

      <Section title="主色 Primary">
        <Row label={`色相 ${tweaks.primaryHue}°`}>
          <input className="tweak-input" type="range" min="0" max="360" value={tweaks.primaryHue} onChange={e=>set('primaryHue', +e.target.value)}/>
        </Row>
        <Row label={`饱和 ${tweaks.primaryChroma.toFixed(2)}`}>
          <input className="tweak-input" type="range" min="0" max="0.25" step="0.01" value={tweaks.primaryChroma} onChange={e=>set('primaryChroma', +e.target.value)}/>
        </Row>
        <Row label={`明度 ${tweaks.primaryLight.toFixed(2)}`}>
          <input className="tweak-input" type="range" min="0.4" max="0.9" step="0.01" value={tweaks.primaryLight} onChange={e=>set('primaryLight', +e.target.value)}/>
        </Row>
      </Section>

      <Section title="背景温度">
        <Row label={`暖度 ${tweaks.bgWarmth}`}>
          <input className="tweak-input" type="range" min="0" max="60" value={tweaks.bgWarmth} onChange={e=>set('bgWarmth', +e.target.value)}/>
        </Row>
        <Toggle label="显示颗粒感 (grain)" value={tweaks.grain} onChange={v=>set('grain', v)}/>
      </Section>

      <Section title="形状 & 密度">
        <Row label={`圆角 ${tweaks.radius}px`}>
          <input className="tweak-input" type="range" min="8" max="44" value={tweaks.radius} onChange={e=>set('radius', +e.target.value)}/>
        </Row>
        <Row label="密度">
          <Pills options={densities} value={tweaks.density} onChange={v=>set('density', v)}/>
        </Row>
      </Section>

      <Section title="字体">
        <Row label="展示字体">
          <Pills options={fontChoices} value={tweaks.displayFont} onChange={v=>set('displayFont', v)}/>
        </Row>
        <Row label="正文字体">
          <Pills options={fontChoices} value={tweaks.bodyFont} onChange={v=>set('bodyFont', v)}/>
        </Row>
        <Row label={`字号 ${(tweaks.fontScale * 100).toFixed(0)}%`}>
          <input className="tweak-input" type="range" min="0.85" max="1.15" step="0.01" value={tweaks.fontScale} onChange={e=>set('fontScale', +e.target.value)}/>
        </Row>
      </Section>

      <Section title="聊天气泡 & 页面">
        <Row label="气泡风格">
          <Pills options={bubbles} value={tweaks.bubbleStyle} onChange={v=>set('bubbleStyle', v)}/>
        </Row>
        <Row label="侧栏">
          <Pills options={sidebars} value={tweaks.sidebarStyle} onChange={v=>set('sidebarStyle', v)}/>
        </Row>
        <Row label="首页强调色">
          <Pills options={heroAccents} value={tweaks.heroAccent} onChange={v=>set('heroAccent', v)}/>
        </Row>
        <Toggle label="首页插画" value={tweaks.heroIllustration} onChange={v=>set('heroIllustration', v)}/>
        <Toggle label="显示鼠尾草绿点缀" value={tweaks.showSage} onChange={v=>set('showSage', v)}/>
      </Section>

      <button
        onClick={() => setTweaks({...TWEAK_DEFAULTS})}
        className="w-full btn-ghost text-sm mt-2"
      >
        重置默认值
      </button>
    </div>
  );
}

function Section({ title, children }){
  return (
    <div className="mb-4 pb-4" style={{ borderBottom: '1px dashed var(--border-soft)' }}>
      <div className="text-[11px] uppercase tracking-widest mb-2" style={{ color: 'var(--text-muted)', letterSpacing: '0.15em' }}>{title}</div>
      <div className="flex flex-col gap-2.5">{children}</div>
    </div>
  );
}
function Row({ label, children }){
  return (
    <label className="block">
      <div className="text-[12px] mb-1.5" style={{ color: 'var(--text-body)' }}>{label}</div>
      {children}
    </label>
  );
}
function Toggle({ label, value, onChange }){
  return (
    <label className="flex items-center justify-between cursor-pointer">
      <span className="text-[12px]" style={{ color: 'var(--text-body)' }}>{label}</span>
      <button
        onClick={(e)=>{ e.preventDefault(); onChange(!value); }}
        className="relative transition-colors"
        style={{
          width: 36, height: 20, borderRadius: 999,
          background: value ? 'var(--primary)' : '#E4D3C2',
        }}>
        <span style={{
          position:'absolute', top: 2, left: value ? 18 : 2,
          width: 16, height: 16, borderRadius: '50%',
          background: '#FFFAF5',
          boxShadow: '0 2px 4px rgba(0,0,0,0.15)',
          transition: 'left .2s ease',
        }}/>
      </button>
    </label>
  );
}
function Pills({ options, value, onChange }){
  return (
    <div className="flex flex-wrap gap-1.5">
      {options.map(o => (
        <button key={o}
          onClick={()=>onChange(o)}
          className="px-2.5 py-1 text-[11px] transition-all"
          style={{
            borderRadius: 999,
            background: value === o ? 'var(--primary)' : 'rgba(255,253,249,0.6)',
            color: value === o ? '#FFFAF5' : 'var(--text-body)',
            border: `1px solid ${value === o ? 'transparent' : 'var(--border-soft)'}`,
            fontWeight: value === o ? 600 : 400,
          }}>
          {o}
        </button>
      ))}
    </div>
  );
}

window.TweaksPanel = TweaksPanel;
window.TWEAK_DEFAULTS = TWEAK_DEFAULTS;
window.applyTokens = applyTokens;
