'use strict';
// Review controls only. No file, clipboard, network, or persistence operations.
const boards = [...document.querySelectorAll('.board')];
const devices = [...document.querySelectorAll('.device')];
const links = [...document.querySelectorAll('[data-select]')];
const fontTokens = ['body', 'title', 'ui', 'uiTitle', 'caption', 'code', 'bodySmall'];
const baseFonts = Object.fromEntries(fontTokens.map(key => [key, parseFloat(getComputedStyle(document.documentElement).getPropertyValue(`--font-${key}`))]));
const controls = ['palette', 'brightness', 'width', 'scale'].map(id => document.getElementById(id));
function updateAppearance() {
  const [palette, brightness, width, scale] = controls.map(control => control.value);
  devices.forEach(device => {
    device.dataset.palette = palette;
    device.dataset.theme = brightness;
    device.dataset.scale = scale;
    device.dataset.narrow = String(Number(width) <= 320);
    fontTokens.forEach(key => device.style.setProperty(`--font-${key}`, `${baseFonts[key] * Number(scale)}px`));
    device.style.setProperty('--review-width', `${width}px`);
  });
}
function selectBoard(id, moveFocus = false) {
  const active = boards.find(board => board.id === id);
  const all = !active;
  boards.forEach(board => { board.hidden = !all && board !== active; });
  links.forEach(link => {
    if (!all && link.dataset.select === id) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  document.getElementById('show-all').setAttribute('aria-current', all ? 'page' : 'false');
  if (active && moveFocus) {
    active.tabIndex = -1;
    active.focus({preventScroll: true});
    active.scrollIntoView({block: 'start', behavior: 'instant'});
  }
}
controls.forEach(control => control.addEventListener('change', updateAppearance));
window.addEventListener('hashchange', () => selectBoard(location.hash.slice(1), true));
document.addEventListener('click', event => {
  const button = event.target.closest('[data-go], [data-demo]');
  if (!button) return;
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
  if (event.key === 'Escape') document.getElementById('demo-status').hidden = true;
});
const slider = document.getElementById('font-size');
slider.addEventListener('input', () => {
  document.getElementById('font-output').textContent = `${slider.value}sp`;
  slider.style.setProperty('--slider-progress', `${(slider.value - 14) / 10 * 100}%`);
  document.querySelector('.type-preview').style.fontSize = `${slider.value}px`;
});
slider.dispatchEvent(new Event('input'));
updateAppearance();
selectBoard(location.hash.slice(1) || 'home');
