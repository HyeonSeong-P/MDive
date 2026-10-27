"""Guard button label contrast and the agreed light/dark pressed direction.

The 1.15 normal/pressed ratio is an MDive regression floor, not a WCAG rule.
"""
import json
from pathlib import Path
import unittest


def luminance(color):
    values = [int(color[i:i + 2], 16) / 255 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= .04045 else ((v + .055) / 1.055) ** 2.4 for v in values]
    return sum(v * w for v, w in zip(linear, (.2126, .7152, .0722)))


def contrast(first, second):
    low, high = sorted((luminance(first), luminance(second)))
    return (high + .05) / (low + .05)


class ButtonStateColorsTest(unittest.TestCase):
    def test_pressed_direction_and_readable_labels_on_every_theme(self):
        tokens = json.loads((Path(__file__).resolve().parents[1] / 'design-system/tokens.json').read_text())
        for palette, theme in tokens['themes'].items():
            for mode, colors in theme['colors'].items():
                with self.subTest(palette=palette, mode=mode):
                    primary = colors['buttonPressedPrimary']
                    self.assertEqual(mode == 'dark', luminance(primary) > luminance(colors['primary']))
                    self.assertGreaterEqual(contrast(primary, colors['primary']), 1.15)
                    self.assertGreaterEqual(contrast(primary, colors['onPrimary']), 4.5)
                    neutral = colors['buttonPressedSurface']
                    for surface in ('background', 'surface', 'surfaceVariant'):
                        self.assertEqual(mode == 'dark', luminance(neutral) > luminance(colors[surface]))
                        self.assertGreaterEqual(contrast(neutral, colors[surface]), 1.15)
                    self.assertGreaterEqual(contrast(neutral, colors['buttonPressedText']), 4.5)
                    # Code surfaces are dark in both page modes, so copy feedback brightens.
                    self.assertGreater(luminance(colors['buttonPressedCode']), luminance(colors['codeBackground']))
                    self.assertGreaterEqual(contrast(colors['buttonPressedCode'], colors['codeText']), 4.5)
