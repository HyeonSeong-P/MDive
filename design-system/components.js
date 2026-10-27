'use strict';
// Shared component layout only; product actions and prototype navigation stay in hosts.
window.MDiveComponents = (() => {
  const observed = new WeakSet();
  function layoutBanner(banner) {
    if (!banner.clientWidth) return;
    const style = getComputedStyle(banner);
    const hasBoth = banner.querySelector('[data-banner-retry]') && banner.querySelector('[data-banner-dismiss]');
    const breakpoint = parseFloat(style.getPropertyValue(hasBoth ? '--size-bannerDismissActionBreakpoint' : '--size-bannerActionBreakpoint'));
    const title = banner.querySelector('.ds-banner-content strong');
    const explicitScale = parseFloat(style.getPropertyValue('--ds-font-scale'));
    const measuredScale = title ? parseFloat(getComputedStyle(title).fontSize) / parseFloat(style.getPropertyValue('--font-ui')) : 1;
    const scale = Number.isFinite(explicitScale) && explicitScale > 0 ? explicitScale : measuredScale || 1;
    const actionWidth = banner.querySelector('.ds-banner-action')?.scrollWidth || 0;
    const stacked = banner.clientWidth / scale < breakpoint || actionWidth > banner.clientWidth * .5;
    if (banner.dataset.stacked !== String(stacked)) banner.dataset.stacked = String(stacked);
  }
  const observer = new ResizeObserver(entries => entries.forEach(entry => layoutBanner(entry.target)));
  function layoutBanners(root = document) {
    const banners = [...root.querySelectorAll('.ds-banner')];
    if (root.matches?.('.ds-banner')) banners.unshift(root);
    banners.forEach(banner => {
      if (!observed.has(banner)) { observer.observe(banner); observed.add(banner); }
      layoutBanner(banner);
    });
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', () => layoutBanners(), {once:true});
  else layoutBanners();
  return {layoutBanners};
})();
