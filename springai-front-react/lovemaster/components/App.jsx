// App root — ties routing, tweaks, and all pages
const { useState: useStateApp, useEffect: useEffectApp } = React;

function App(){
  const [route, setRoute] = useStateApp(() => localStorage.getItem('lm_route') || 'home');
  const [tweaks, setTweaks] = useStateApp(() => {
    try { const s = localStorage.getItem('lm_tweaks'); if (s) return {...window.TWEAK_DEFAULTS, ...JSON.parse(s)}; }
    catch(e){}
    return {...window.TWEAK_DEFAULTS};
  });
  const [tweaksOpen, setTweaksOpen] = useStateApp(false);
  const [editMode, setEditMode] = useStateApp(false);

  // Apply tokens whenever tweaks change
  useEffectApp(() => {
    window.applyTokens(tweaks);
    localStorage.setItem('lm_tweaks', JSON.stringify(tweaks));
    // grain toggle
    document.querySelectorAll('.grain').forEach(el => el.style.setProperty('--grain-opacity', tweaks.grain ? '0.35' : '0'));
    if (!tweaks.grain){
      const s = document.getElementById('grain-killer') || document.createElement('style');
      s.id = 'grain-killer';
      s.textContent = '.grain::after { opacity: 0 !important; }';
      if (!document.getElementById('grain-killer')) document.head.appendChild(s);
    } else {
      const s = document.getElementById('grain-killer');
      if (s) s.remove();
    }
  }, [tweaks]);

  useEffectApp(() => { localStorage.setItem('lm_route', route); }, [route]);

  const navigate = (r) => setRoute(r);

  // edit-mode protocol
  useEffectApp(() => {
    const handler = (e) => {
      const d = e?.data;
      if (!d) return;
      if (d.type === '__activate_edit_mode') { setEditMode(true); setTweaksOpen(true); }
      if (d.type === '__deactivate_edit_mode') { setEditMode(false); setTweaksOpen(false); }
    };
    window.addEventListener('message', handler);
    window.parent?.postMessage({ type: '__edit_mode_available' }, '*');
    return () => window.removeEventListener('message', handler);
  }, []);

  // persist selected tweak keys back to host
  useEffectApp(() => {
    if (editMode){
      window.parent?.postMessage({ type: '__edit_mode_set_keys', edits: tweaks }, '*');
    }
  }, [tweaks, editMode]);

  return (
    <div className="w-screen h-screen overflow-hidden relative" style={{ background: 'var(--bg-cream)' }}>


      {route === 'home' && <HomePage tweaks={tweaks} navigate={navigate}/>}
      {route === 'chat' && <ChatPage tweaks={tweaks} navigate={navigate}/>}
      {route === 'auth' && <AuthPage navigate={navigate}/>}
      {route === 'admin' && <AdminPage navigate={navigate}/>}

      <TweaksPanel tweaks={tweaks} setTweaks={setTweaks} open={tweaksOpen} setOpen={setTweaksOpen}/>
    </div>
  );
}

function RouteSwitcher({ route, setRoute }){
  const routes = [
    { k: 'home', label: '首页', icon: <Icon.Home size={12}/> },
    { k: 'chat', label: '聊天', icon: <Icon.MessageCircle size={12}/> },
    { k: 'auth', label: '登录', icon: <Icon.User size={12}/> },
    { k: 'admin', label: '后台', icon: <Icon.Shield size={12}/> },
  ];
  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[90] flex gap-1 p-1"
      style={{
        background: 'rgba(255,253,249,0.72)',
        backdropFilter: 'blur(14px)',
        borderRadius: 999,
        border: '1px solid var(--border-soft)',
        boxShadow: '0 6px 20px rgba(196,123,90,0.12)',
      }}>
      {routes.map(r => (
        <button key={r.k} onClick={() => setRoute(r.k)}
          className="flex items-center gap-1.5 px-3 py-1.5 text-[11.5px] font-medium transition-all"
          style={{
            borderRadius: 999,
            background: route === r.k ? 'linear-gradient(135deg, #F2A987, #E89B7A)' : 'transparent',
            color: route === r.k ? '#FFFAF5' : 'var(--text-body)',
            boxShadow: route === r.k ? '0 4px 12px rgba(232,155,122,0.35)' : 'none',
          }}>
          {r.icon} {r.label}
        </button>
      ))}
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
