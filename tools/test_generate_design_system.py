"""Regression checks for shared-source propagation and stale-output rejection."""
import json
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

    def test_unbundled_code_weight_is_rejected(self):
        self.change('tokens.json', lambda t: t['typography']['code'].update(weight=600))
        self.assertNotEqual(0, self.run_generator().returncode)


if __name__ == '__main__':
    unittest.main()
