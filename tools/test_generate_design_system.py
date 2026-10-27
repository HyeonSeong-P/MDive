"""Regression checks for shared-source propagation and stale-output rejection."""
import json
from html.parser import HTMLParser
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


class GeneratorTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        source = Path(__file__).resolve().parents[1]
        (self.root / 'tools').mkdir()
        (self.root / 'design-system').mkdir()
        shutil.copy(source / 'tools/generate_design_system.py', self.root / 'tools')
        for name in ['tokens.json', 'catalog.json', 'icons.json']:
            shutil.copy(source / 'design-system' / name, self.root / 'design-system')
        (self.root / 'design-system/brand').mkdir()
        shutil.copy(source / 'design-system/brand/mdive-mark.svg', self.root / 'design-system/brand')

    def run_generator(self, *args):
        return subprocess.run([sys.executable, str(self.root / 'tools/generate_design_system.py'), *args], capture_output=True, text=True)

    def change(self, name, transform):
        path = self.root / 'design-system' / name
        data = json.loads(path.read_text())
        transform(data)
        path.write_text(json.dumps(data))

    def test_catalog_loads_shared_component_renderer(self):
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertIn('href="../components.css"', page)
        self.assertIn('href="../feedback.css"', page)
        self.assertIn('src="../components.js"', page)
        self.assertNotIn('const layoutBanner = () =>', page)
        self.assertIn('class="ds-state-failure"', page)
        self.assertIn('class="ds-sheet-handle"', page)

    def test_button_size_matrix_and_shared_size_propagation(self):
        self.change('tokens.json', lambda t: t['sizes'].update(buttonS=38))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        css = (self.root / 'design-system/generated/tokens.css').read_text()
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/DesignTokens.kt').read_text()
        self.assertIn('--size-buttonS: 38px', css)
        self.assertIn('"buttonS" to 38.dp', kotlin)
        for size in ['s', 'm', 'l']:
            self.assertEqual(6 * 4 * 4, page.count(f'data-size="{size}"'))
        self.assertIn('outlined size-s', page)

    def test_stale_output_fails_until_regenerated(self):
        self.assertEqual(0, self.run_generator().returncode)
        self.assertEqual(0, self.run_generator('--check').returncode)
        self.change('tokens.json', lambda t: t['themes']['blue']['colors']['dark'].update(primary='#123456'))
        self.assertNotEqual(0, self.run_generator('--check').returncode)
        self.assertEqual(0, self.run_generator().returncode)
        self.assertEqual(0, self.run_generator('--check').returncode)
        self.assertIn('0xFF123456', (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/DesignTokens.kt').read_text())
        self.assertIn('--primary: #123456', (self.root / 'design-system/generated/tokens.css').read_text())

    def test_removing_state_updates_both_catalogs(self):
        self.change('catalog.json', lambda c: next(e for e in c['entries'] if e['id'] == 'search')['states'].remove('empty'))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertNotIn('0/0', page)
        self.assertIn('1/3', page)
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogEntries.kt').read_text()
        self.assertIn('"한 줄 검색바", listOf("idle", "results")', kotlin)

    def test_unknown_state_is_rejected(self):
        self.change('catalog.json', lambda c: c['entries'][0]['states'].append('unimplemented'))
        self.assertNotEqual(0, self.run_generator().returncode)

    def test_missing_role_in_another_theme_is_rejected(self):
        self.change('tokens.json', lambda t: t['themes']['green']['colors']['dark'].pop('onPrimary'))
        self.assertNotEqual(0, self.run_generator().returncode)

    def test_icon_stroke_changes_reach_both_platforms(self):
        self.change('icons.json', lambda i: i.update(strokeWidth=2.1))
        self.assertEqual(0, self.run_generator().returncode)
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogIcons.kt').read_text()
        css = (self.root / 'design-system/generated/tokens.css').read_text()
        self.assertIn('const val strokeWidth = 2.1f', kotlin)
        self.assertIn('--icon-stroke: 2.1', css)

    def test_type_role_size_and_line_height_propagate_together(self):
        self.change('tokens.json', lambda t: t['fontSizes'].update(uiTitle=20))
        self.assertEqual(0, self.run_generator().returncode)
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/DesignTokens.kt').read_text()
        css = (self.root / 'design-system/generated/tokens.css').read_text()
        self.assertIn('"uiTitle" to TypographyToken("ui", 20.sp, 30.0.sp, 600)', kotlin)
        self.assertIn('--font-uiTitle: 20px', css)
        self.assertIn('.type-uiTitle { font-family: var(--family-ui); font-size: var(--font-uiTitle); line-height: var(--leading-ui); font-weight: 600;', css)

    def test_overlay_measurements_follow_tokens_in_both_catalogs(self):
        self.change('tokens.json', lambda t: t['sizes'].update(dialogMaxWidth=420))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogEntries.kt').read_text()
        css = (self.root / 'design-system/generated/tokens.css').read_text()
        self.assertIn('sizes.dialogMaxWidth · 420dp', page)
        self.assertIn('sizes.dialogMaxWidth · 420dp', kotlin)
        self.assertIn('--size-dialogMaxWidth: 420px', css)

    def test_markdown_fixture_is_escaped_and_shared_without_parsing(self):
        fixture = '<script>alert("not markup")</script> 한글'
        self.change('catalog.json', lambda c: c['samples'].update(markdownBody=fixture))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogSamples.kt').read_text()
        self.assertNotIn(fixture, page)
        self.assertIn('&lt;script&gt;', page)
        self.assertIn('const val markdownBody =', kotlin)
        self.assertIn('한글', kotlin)
        self.assertIn('data-content-event="copy"', page)
        self.assertIn("querySelector('code').textContent", page)
        self.assertIn('<th scope="col">', page)
        self.assertIn('<ol start="98">', page)

    def test_document_heading_tokens_propagate_without_ui_title_scaling(self):
        self.change('tokens.json', lambda t: t['fontSizes'].update(documentH3=23))
        self.assertEqual(0, self.run_generator().returncode)
        css = (self.root / 'design-system/generated/tokens.css').read_text()
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/DesignTokens.kt').read_text()
        self.assertIn('--font-documentH3: 23px', css)
        self.assertIn('"documentH3" to TypographyToken("body", 23.sp', kotlin)

    def test_checkbox_semantics_and_content_actions_use_shared_shell(self):
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertNotIn('☑', page)
        self.assertNotIn('☐', page)
        self.assertIn('role="checkbox" aria-checked="true" aria-readonly="true"', page)
        self.assertIn('<input type="checkbox" checked disabled>', page)

        class ActionParser(HTMLParser):
            def __init__(self):
                super().__init__()
                self.stack = []
                self.outside = []
                self.readonly_inputs = []

            def handle_starttag(self, tag, attrs):
                attrs = dict(attrs)
                if tag == 'input' and any(a.get('aria-readonly') == 'true' for _, a in self.stack):
                    self.readonly_inputs.append(attrs)
                if attrs.get('data-content-event') in ['source', 'expand', 'retry', 'connect']:
                    if not any('md-content-banner' in a.get('class', '').split() for _, a in self.stack):
                        self.outside.append(attrs)
                if tag not in ['input', 'meta', 'link', 'br', 'hr', 'path']:
                    self.stack.append((tag, attrs))

            def handle_endtag(self, tag):
                for index in range(len(self.stack) - 1, -1, -1):
                    if self.stack[index][0] == tag:
                        self.stack = self.stack[:index]
                        break

        parser = ActionParser()
        parser.feed(page)
        self.assertEqual([], parser.outside)
        self.assertEqual([], parser.readonly_inputs)

    def test_extended_content_is_inert_and_preserves_reference_targets(self):
        fixture = '<iframe src="https://example.com/private"></iframe>'
        self.change('catalog.json', lambda c: c['samples'].update(extensionTerm=fixture))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertNotIn('<iframe', page)
        self.assertNotIn('<video', page)
        self.assertNotIn(fixture, page)
        self.assertIn('&lt;iframe', page)
        self.assertIn('&lt;section&gt;&lt;strong&gt;', page)
        self.assertIn('data-content-target="#note-1"', page)
        self.assertIn('data-content-event="backlink"', page)
        self.assertIn('<details class="md-details" open>', page)
        self.assertIn('<dl class="md-definitions">', page)
        for kind in ['info', 'success', 'important', 'warning', 'danger']:
            self.assertIn('md-callout-' + kind, page)
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogSamples.kt').read_text()
        self.assertIn('val extensionMetadata = listOf(listOf("title"', kotlin)

    def test_removing_extension_state_updates_html_and_manifest(self):
        self.change('catalog.json', lambda c: next(e for e in c['entries'] if e['id'] == 'markdown-extended')['states'].remove('details'))
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertNotIn('<details class="md-details"', page)
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogEntries.kt').read_text()
        self.assertIn('"inline", "references", "definitions", "callouts", "metadata"', kotlin)

    def test_extension_link_fixtures_and_embed_state_matrix_are_shared(self):
        def change_fixtures(catalog):
            catalog['samples']['extensionReferenceLinks'][0] = ['공유 각주', '#shared-footnote']
            catalog['samples']['extensionTocTargets'][0] = ['공유 목차', '#shared-heading', '1']
            catalog['samples']['extensionResolvedLinks'][0] = ['공유 링크', 'wiki:Shared']
        self.change('catalog.json', change_fixtures)
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        kotlin = (self.root / 'designsystem/src/main/java/com/parka/mdive/designsystem/generated/CatalogSamples.kt').read_text()
        for label, target in [('공유 각주', '#shared-footnote'), ('공유 목차', '#shared-heading'), ('공유 링크', 'wiki:Shared')]:
            self.assertIn('data-content-target="' + target + '">' + label, page)
            self.assertIn('listOf("' + label + '", "' + target + '"', kotlin)
        for kind in ['html', 'media']:
            for state in ['ready', 'loading', 'failure']:
                marker = 'data-embed-kind="' + kind + '" data-embed-state="' + state + '"'
                self.assertIn(marker, page)
                specimen = page.split(marker, 1)[1].split('data-embed-kind=', 1)[0]
                self.assertIn('data-content-event="source"', specimen)

    def test_entry_switching_preserves_nested_content_sections(self):
        self.assertEqual(0, self.run_generator().returncode)
        page = (self.root / 'design-system/generated/catalog.html').read_text()
        self.assertIn('<section aria-label="각주">', page)
        self.assertIn("theme.querySelectorAll(':scope > section[data-entry]')", page)
        self.assertNotIn("theme.querySelectorAll('section')", page)

    def test_unbundled_code_weight_is_rejected(self):
        self.change('tokens.json', lambda t: t['typography']['code'].update(weight=600))
        self.assertNotEqual(0, self.run_generator().returncode)


if __name__ == '__main__':
    unittest.main()
