// Shared UI primitives
const { useState, useEffect, useRef, useMemo } = React;

// Brand mark — circled heart, softly animated
function BrandMark({ size = 32 }) {
  return (
    <div
      className="grid place-items-center breathe"
      style={{
        width: size, height: size,
        borderRadius: '50%',
        background: 'linear-gradient(135deg, #F5C4A8 0%, #E89B7A 55%, #E8A4A4 100%)',
        boxShadow: '0 6px 16px rgba(232,155,122,0.38), inset 0 1px 0 rgba(255,255,255,0.55)',
        color: '#FFFAF5',
      }}
    >
      <Icon.Heart size={size * 0.52} fill="#FFFAF5" stroke="#FFFAF5" strokeWidth={1.6}/>
    </div>
  );
}

// Placeholder image (striped + label)
function PlaceholderImg({ label = 'image', w = '100%', h = 200, tone = 'peach' }) {
  const palette = {
    peach: ['#F5C4A8', '#FCE7D5'],
    sage:  ['#B8CFC2', '#D5E3DA'],
    rose:  ['#EEB6B6', '#F5D8D8'],
    cream: ['#E8D7C2', '#F5E4D1'],
  }[tone] || ['#F5C4A8', '#FCE7D5'];
  return (
    <div
      className="relative overflow-hidden"
      style={{
        width: w, height: h,
        borderRadius: 'var(--r-md)',
        background: `repeating-linear-gradient(45deg, ${palette[0]} 0 12px, ${palette[1]} 12px 24px)`,
      }}
    >
      <div
        className="absolute inset-0 grid place-items-center"
        style={{ fontFamily: 'ui-monospace, monospace', fontSize: 12, color: 'rgba(58,36,25,0.55)' }}
      >
        <span style={{ background: 'rgba(255,253,249,0.85)', padding: '4px 10px', borderRadius: 999, backdropFilter: 'blur(4px)' }}>
          {label}
        </span>
      </div>
    </div>
  );
}

// Small "tag" chip with soft tinted bg
function Chip({ children, tone = 'peach', icon }) {
  const tones = {
    peach: { bg: '#FCE7D5', fg: '#A0624A', bd: 'rgba(232,155,122,0.3)' },
    sage:  { bg: '#D5E3DA', fg: '#4C7566', bd: 'rgba(143,176,159,0.4)' },
    rose:  { bg: '#F5D8D8', fg: '#A55F5F', bd: 'rgba(232,164,164,0.4)' },
    cream: { bg: '#F5E4D1', fg: '#6B4A38', bd: 'rgba(107,74,56,0.2)' },
  }[tone];
  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 text-xs font-medium"
      style={{ background: tones.bg, color: tones.fg, borderRadius: 999, border: `1px solid ${tones.bd}` }}>
      {icon}{children}
    </span>
  );
}

// Section framing
function SectionTitle({ eyebrow, title, subtitle, align = 'left' }) {
  return (
    <div style={{ textAlign: align }}>
      {eyebrow && (
        <div className="text-xs tracking-widest uppercase mb-2" style={{ color: 'var(--primary-dark)', fontFamily: 'var(--font-en)', letterSpacing: '0.18em' }}>
          {eyebrow}
        </div>
      )}
      <h2 className="display text-[28px] md:text-[34px] font-normal" style={{ color: 'var(--text-ink)' }}>
        {title}
      </h2>
      {subtitle && <p className="mt-2 text-sm md:text-base" style={{ color: 'var(--text-body)' }}>{subtitle}</p>}
    </div>
  );
}

// Tiny sticker icon wrapper
function IconSticker({ children, tone = 'peach', size = 44 }) {
  const tones = {
    peach: 'linear-gradient(135deg, #F5C4A8 0%, #E89B7A 100%)',
    sage:  'linear-gradient(135deg, #B8CFC2 0%, #8FB09F 100%)',
    rose:  'linear-gradient(135deg, #F5D8D8 0%, #E8A4A4 100%)',
    cream: 'linear-gradient(135deg, #F5E4D1 0%, #D8BF9F 100%)',
  };
  return (
    <div className="sticker-shadow grid place-items-center"
      style={{
        width: size, height: size,
        borderRadius: size * 0.38,
        background: tones[tone],
        color: '#FFFAF5',
        flexShrink: 0,
      }}>
      {children}
    </div>
  );
}

// Avatar bubble
function Avatar({ name = '她', tone = 'rose', size = 36 }) {
  const bgs = {
    peach: 'linear-gradient(135deg, #F5C4A8, #E89B7A)',
    sage:  'linear-gradient(135deg, #B8CFC2, #8FB09F)',
    rose:  'linear-gradient(135deg, #F5D8D8, #E8A4A4)',
  };
  return (
    <div className="grid place-items-center font-medium" style={{
      width: size, height: size, borderRadius: '50%',
      background: bgs[tone], color: '#FFFAF5',
      fontFamily: 'var(--font-display)',
      fontSize: size * 0.42,
      boxShadow: '0 3px 8px rgba(196,123,90,0.2), inset 0 1px 0 rgba(255,255,255,0.4)',
      flexShrink: 0,
    }}>
      {name[0]}
    </div>
  );
}

window.BrandMark = BrandMark;
window.PlaceholderImg = PlaceholderImg;
window.Chip = Chip;
window.SectionTitle = SectionTitle;
window.IconSticker = IconSticker;
window.Avatar = Avatar;
