"""Structural checks for the approved, mechanically traced welcome asset."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

from generate_welcome_illustration import SVG, VECTOR, android_vector


class WelcomeIllustrationTest(unittest.TestCase):
    def test_shared_asset_is_a_trimmed_monochrome_vector(self):
        root = ET.parse(SVG).getroot()
        self.assertEqual('0 0 1138 530', root.attrib['viewBox'])
        self.assertEqual('currentColor', root.attrib['fill'])
        self.assertTrue(list(root.iter('{http://www.w3.org/2000/svg}path')))
        self.assertFalse(list(root.iter('{http://www.w3.org/2000/svg}image')))

    def test_android_matches_shared_trace(self):
        self.assertEqual(android_vector(), VECTOR.read_text())

    def test_html_uses_theme_color_mask(self):
        css = (SVG.parents[1] / 'feedback.css').read_text()
        rule = css.split('.ds-welcome-illustration{', 1)[1].split('}', 1)[0]
        self.assertIn('background:var(--primary)', rule)
        self.assertIn('illustrations/document-plunge.svg', rule)
        self.assertIn('aspect-ratio:1138/530', rule)


if __name__ == '__main__':
    unittest.main()
