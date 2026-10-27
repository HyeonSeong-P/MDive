"""Keep product boards on the same HTML component contracts as the DS catalog."""
import json
from html.parser import HTMLParser
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class Element:
    def __init__(self, tag, attrs, parent=None):
        self.tag, self.attrs, self.parent = tag, dict(attrs), parent
        self.children = []

    @property
    def classes(self):
        return set(self.attrs.get('class', '').split())

    def inside(self, class_name):
        node = self.parent
        while node:
            if class_name in node.classes:
                return True
            node = node.parent
        return False


class Tree(HTMLParser):
    def __init__(self, source):
        super().__init__()
        self.nodes = []
        self.stack = []
        self.feed(source)

    def handle_starttag(self, tag, attrs):
        node = Element(tag, attrs, self.stack[-1] if self.stack else None)
        self.nodes.append(node)
        if node.parent:
            node.parent.children.append(node)
        if tag not in {'area', 'base', 'br', 'col', 'embed', 'hr', 'img', 'input', 'link', 'meta', 'param', 'source', 'track', 'wbr', 'path', 'rect', 'circle'}:
            self.stack.append(node)

    def handle_endtag(self, tag):
        for index in range(len(self.stack) - 1, -1, -1):
            if self.stack[index].tag == tag:
                self.stack = self.stack[:index]
                break


class ScreenComponentContractsTest(unittest.TestCase):
    def setUp(self):
        self.source = (ROOT / 'docs/design/screens/index.html').read_text()
        self.tree = Tree(self.source)

    def test_product_loads_shared_renderer_and_keeps_navigation_intact(self):
        resources = [node.attrs.get('href', node.attrs.get('src', '')).split('?', 1)[0] for node in self.tree.nodes if node.tag in {'link', 'script'}]
        self.assertIn('../../../design-system/components.css', resources)
        self.assertIn('../../../design-system/components.js', resources)
        self.assertNotIn('../../../design-system/catalog.css', resources)
        ids = [node.attrs['id'] for node in self.tree.nodes if 'id' in node.attrs]
        self.assertEqual(len(ids), len(set(ids)))
        for node in self.tree.nodes:
            for attr in ('data-go', 'data-select', 'data-reference'):
                if attr in node.attrs:
                    self.assertIn(node.attrs[attr], ids)
        boards = [node.attrs['id'] for node in self.tree.nodes if 'board' in node.classes]
        index = json.loads((ROOT / 'docs/design/screens/screen-index.json').read_text())
        self.assertEqual(boards, [item['id'] for item in index])

    def test_content_and_overlay_nodes_use_shared_component_structure(self):
        for node in self.tree.nodes:
            with self.subTest(tag=node.tag, classes=node.classes):
                if 'document-row' in node.classes:
                    self.assertTrue(any(child.tag == 'div' for child in node.children))
                if 'md-code' in node.classes:
                    for child in node.children:
                        if child.tag == 'div':
                            self.assertIn('md-code-bar', child.classes)
                if node.tag == 'button' and node.inside('md-code-bar') and 'button' in node.classes:
                    self.assertTrue({'quiet', 'size-s'} <= node.classes)
                if node.tag == 'table' and node.inside('md-table-scroll'):
                    self.assertIn('md-table', node.classes)
                if node.tag == 'th' and node.inside('md-table'):
                    self.assertEqual('col', node.attrs.get('scope'))
                if node.tag == 'button' and node.inside('ds-banner'):
                    self.assertTrue(node.inside('ds-banner-action'))
                if 'ds-sheet-heading' in node.classes:
                    self.assertFalse(any(child.tag == 'button' for child in node.children))
                if 'ds-state-actions' in node.classes:
                    self.assertTrue(node.children, 'Do not reserve an empty action slot')
                self.assertFalse(node.classes & {'codeblock', 'table-wrap', 'image-button', 'confirm', 'handle'})

    def test_screen_css_cannot_reskin_shared_components(self):
        css = (ROOT / 'docs/design/screens/screens.css').read_text()
        css = re.sub(r'/\*.*?\*/', '', css, flags=re.S)
        shared = re.compile(r'\.(?:button|icon-button|topbar|document-row|search|tabs|ds-[\w-]+|md-[\w-]+)(?![\w-])')
        layout = {'display', 'position', 'top', 'right', 'bottom', 'left', 'inset', 'transform', 'width', 'min-width', 'max-width', 'height', 'min-height', 'max-height', 'flex', 'flex-direction', 'flex-shrink', 'flex-grow', 'align-self', 'overflow', 'overflow-x', 'overflow-y', 'break-inside', 'break-before'}
        for match in re.finditer(r'([^{}]+)\{([^{}]*)\}', css):
            selector, declarations = match.groups()
            if not shared.search(selector):
                continue
            for declaration in declarations.split(';'):
                if ':' not in declaration:
                    continue
                prop = declaration.split(':', 1)[0].strip()
                self.assertTrue(prop in layout or prop.startswith('margin'), f'{selector.strip()} must use DS appearance, not {prop}')

    def test_ui_icons_match_the_shared_path_source(self):
        paths = json.loads((ROOT / 'design-system/icons.json').read_text())['paths'].values()
        normalized = {re.sub(r'\s+', '', path) for path in paths}
        for node in self.tree.nodes:
            if node.tag == 'path' and node.inside('icon'):
                self.assertIn(re.sub(r'\s+', '', node.attrs['d']), normalized)
