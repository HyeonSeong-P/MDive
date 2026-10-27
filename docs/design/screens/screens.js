'use strict';
// Review controls only. No file, clipboard, network, or persistence operations.
const boards = [...document.querySelectorAll('.board')];
const devices = [...document.querySelectorAll('.device')];
// Reuse the original board inside a specification iframe; this changes review chrome only.
const embedRequest = new URLSearchParams(location.search).get('embed');
const embedBoard = boards.find(board => board.id === embedRequest);
if (embedBoard) document.documentElement.dataset.embed = 'board';

const links = [...document.querySelectorAll('[data-select]')];
const fontTokens = ['body', 'title', 'ui', 'uiTitle', 'caption', 'code', 'bodySmall', 'documentH1', 'documentH2', 'documentH3', 'documentH4', 'documentH5', 'documentH6'];
const baseFonts = Object.fromEntries(fontTokens.map(key => [key, parseFloat(getComputedStyle(document.documentElement).getPropertyValue(`--font-${key}`))]));
const controls = ['palette', 'brightness', 'width', 'scale'].map(id => document.getElementById(id));
function updateAppearance() {
  const [palette, brightness, width, scale] = controls.map(control => control.value);
  devices.forEach(device => {
    device.dataset.palette = palette;
    device.dataset.theme = brightness;
    device.dataset.scale = scale;
    device.style.setProperty("--ds-font-scale", scale);
    device.dataset.narrow = String((embedBoard ? window.innerWidth : Number(width)) <= 320);
    fontTokens.forEach(key => device.style.setProperty(`--font-${key}`, `${baseFonts[key] * Number(scale)}px`));
    device.style.setProperty('--review-width', `${width}px`);
  });
}
let menuTabDirection = 0;
function selectBoard(id, moveFocus = false) {
  const closingPopup = document.getElementById('reader-more')?.hidden === false && id === 'reader';
  const active = boards.find(board => board.id === id);
  const all = !active;
  boards.forEach(board => { board.hidden = !all && board !== active; });
  links.forEach(link => {
    if (!all && link.dataset.select === id) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  window.MDiveComponents.layoutBanners();
  document.getElementById('show-all').setAttribute('aria-current', all ? 'page' : 'false');
  if (active && moveFocus) {
    active.tabIndex = -1;
    active.focus({preventScroll: true});
    active.scrollIntoView({block: 'start', behavior: 'instant'});
    if (id === 'reader-more') active.querySelector('[role="menuitem"]')?.focus({preventScroll: true});
    else if (closingPopup) {
      const trigger = active.querySelector('[aria-haspopup="menu"]');
      if (menuTabDirection) {
        const focusable = [...active.querySelectorAll('button:not(:disabled), a[href], input:not(:disabled), [tabindex="0"]')];
        const index = focusable.indexOf(trigger);
        const target = focusable[Math.max(0, Math.min(focusable.length - 1, index + menuTabDirection))];
        (target || trigger)?.focus({preventScroll: true});
      } else trigger?.focus({preventScroll: true});
      menuTabDirection = 0;
    }
  }
}
controls.forEach(control => control.addEventListener('change', () => { updateAppearance(); window.MDiveComponents.layoutBanners(); }));
window.addEventListener('hashchange', () => selectBoard(location.hash.slice(1), true));
document.addEventListener('click', event => {
  if (location.hash === '#reader-more' && event.target.closest('#reader-more .app-frame') && !event.target.closest('.ds-popup-menu, [aria-haspopup="menu"], [data-favorite-toggle]')) {
    location.hash = 'reader';
    return;
  }
  const reference = event.target.closest('[data-reference]');
  if (reference) {
    event.preventDefault();
    const target = document.getElementById(reference.dataset.reference);
    target?.scrollIntoView({block: 'center', behavior: 'instant'});
    target?.focus({preventScroll: true});
    return;
  }
  const favorite = event.target.closest('[data-favorite-toggle]');
  if (favorite) {
    const selected = favorite.getAttribute('aria-pressed') !== 'true';
    document.querySelectorAll('[data-favorite-toggle]').forEach(control => {
      control.setAttribute('aria-pressed', String(selected));
      control.setAttribute('aria-label', selected ? '즐겨찾기에서 해제 · 시안' : '즐겨찾기에 추가 · 시안');
    });
    return;
  }
  const button = event.target.closest('[data-go], [data-demo]');
  if (!button) return;
  if (button.tagName === 'A') event.preventDefault();
  if (button.dataset.go) {
    if (location.hash === `#${button.dataset.go}`) selectBoard(button.dataset.go, true);
    else location.hash = button.dataset.go;
  } else {
    const status = document.getElementById('demo-status');
    status.textContent = button.dataset.demo;
    status.hidden = false;
    clearTimeout(window.demoTimer);
    window.demoTimer = setTimeout(() => { status.hidden = true; }, 3500);
  }
});
document.addEventListener('keydown', event => {
  if (location.hash === '#reader-more' && event.target.closest('#reader-more-menu') && event.key === 'Tab') {
    event.preventDefault();
    menuTabDirection = event.shiftKey ? -1 : 1;
    location.hash = 'reader';
    return;
  }

  if (location.hash === '#reader-more' && event.target.closest('#reader-more-menu') && ['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) {
    const items = [...document.querySelectorAll('#reader-more-menu [role="menuitem"]')];
    const current = items.indexOf(document.activeElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : (current + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
    items[next]?.focus();
    event.preventDefault();
    return;
  }

  if (event.key === 'Escape') {
    document.getElementById('demo-status').hidden = true;
    if (location.hash === '#reader-more') location.hash = 'reader';
  }
});
const slider = document.getElementById('font-size');
slider.addEventListener('input', () => {
  document.getElementById('font-output').textContent = `${slider.value}sp`;
  slider.style.setProperty('--slider-progress', `${(slider.value - 14) / 10 * 100}%`);
  document.querySelector('.type-preview').style.fontSize = `${slider.value}px`;
});
slider.dispatchEvent(new Event('input'));
updateAppearance();
const initialBoard = embedBoard ? (boards.some(board => board.id === location.hash.slice(1)) ? location.hash.slice(1) : embedBoard.id) : (location.hash.slice(1) || 'home');
selectBoard(initialBoard);
if (embedBoard) window.addEventListener('resize', () => { updateAppearance(); window.MDiveComponents.layoutBanners(); });
