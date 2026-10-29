/* Trusted application host. Input is passed as JSON, never interpolated as HTML or script. */
window.mdiveStatus = {state: 'loading'};
window.mdiveRender = async function(source, dark) {
  try {
    mermaid.initialize({startOnLoad:false, securityLevel:'strict', theme:dark?'dark':'default',
      htmlLabels:false, flowchart:{htmlLabels:false}, maxTextSize:50000, maxEdges:500,
      suppressErrorRendering:true, deterministicIds:true,
      secure:['securityLevel','startOnLoad','maxTextSize','maxEdges','htmlLabels','flowchart','theme']});
    const diagram = await mermaid.mermaidAPI.getDiagramFromText(source);
    // Count parsed connections rather than counting arrow-looking text in quoted labels.
    const db = diagram.db;
    let edges;
    switch (diagram.type) {
      case 'flowchart-v2': case 'flowchart': edges = db.getEdges(); break;
      case 'sequence': edges = db.getMessages().filter(m => m.from && m.to && m.type !== db.LINETYPE.NOTE); break;
      case 'classDiagram': case 'class': edges = db.getRelations(); break;
      case 'stateDiagram': case 'state': edges = db.getRootDocV2 ? db.getRootDocV2() : null; break;
      case 'er': edges = db.getRelationships(); break;
      default: throw Error('Unsupported diagram');
    }
    // State diagram edges are nested in the parsed root; the rendered DOM also gets checked.
    function countState(value) {
      if (!value || typeof value !== 'object') return 0;
      if (Array.isArray(value)) return value.reduce((n,v)=>n+countState(v),0);
      return (value.stmt === 'relation' ? 1 : 0) + countState(value.doc);
    }
    const count = Array.isArray(edges) ? edges.length : countState(edges);
    if (count > 500) throw Error('Too many connections');
    const rendered = await mermaid.render('mdiveDiagram', source);
    const doc = new DOMParser().parseFromString(rendered.svg, 'image/svg+xml');
    if (doc.querySelector('parsererror,script,foreignObject,image,a,iframe,object,embed')) throw Error('Unsupported output');
    for (const element of doc.querySelectorAll('*')) {
      for (const attr of element.attributes) {
        if (/^on/i.test(attr.name)) throw Error('Interactive output');
        if (/(?:href|src)$/i.test(attr.name) && !/^#/.test(attr.value)) throw Error('External reference');
        for (const match of attr.value.matchAll(/url\s*\(\s*([^)]+)\)/gi)) {
          if (!match[1].replace(/["']/g,'').trim().startsWith('#')) throw Error('External style');
        }
      }
    }
    for (const style of doc.querySelectorAll('style')) {
      for (const match of style.textContent.matchAll(/url\s*\(\s*([^)]+)\)/gi)) {
        if (!match[1].replace(/["']/g,'').trim().startsWith('#')) throw Error('External stylesheet');
      }
      if (/@import/i.test(style.textContent)) throw Error('External stylesheet');
    }
    const svg = document.importNode(doc.documentElement,true);
    const vb = svg.viewBox.baseVal;
    if (!Number.isFinite(vb.width) || !Number.isFinite(vb.height) || vb.width<=0 || vb.height<=0) throw Error('Invalid size');
    // Extra output check covers all diagram families, including nested state transitions.
    if (svg.querySelectorAll('.flowchart-link,.messageLine0,.messageLine1,.relation,.relationshipLine,.transition').length > 500) throw Error('Too many connections');
    svg.setAttribute('preserveAspectRatio','xMidYMid meet');
    svg.removeAttribute('width'); svg.removeAttribute('height');
    document.getElementById('drawing').replaceChildren(svg);
    window.mdiveStatus = {state:'ready', svg:new XMLSerializer().serializeToString(svg)};
  } catch (_) {
    document.getElementById('drawing').replaceChildren();
    window.mdiveStatus = {state:'failure'};
  }
};
