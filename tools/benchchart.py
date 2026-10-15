"""Chart of tools/benchmark.sh results for the README: python tools/benchchart.py <out.png> <label=chunks/s>...
Each argument is a bar, e.g. "1.21.1 vanilla=84.3" "1.21.1 ReTerraForged 0.0.13=76.5"."""
import sys

import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt

out = sys.argv[1]
bars = [a.rsplit('=', 1) for a in sys.argv[2:]]
labels = [b[0] for b in bars]
values = [float(b[1]) for b in bars]
colors = ['#8a8f98' if 'anilla' in l else '#3f8f4f' for l in labels]

fig, ax = plt.subplots(figsize=(8, 0.75 * len(bars) + 1.2), dpi=150)
ax.barh(labels[::-1], values[::-1], color=colors[::-1], height=0.6)
for i, v in enumerate(values[::-1]):
    ax.text(v + max(values) * 0.01, i, f'{v:.1f}', va='center', fontsize=11)
ax.set_xlabel('chunks generated per second (higher is better)')
ax.set_xlim(0, max(values) * 1.15)
ax.spines[['top', 'right']].set_visible(False)
ax.set_title('World generation speed: 48 × 48 chunks, NeoForge, 16 cores', fontsize=11)
fig.tight_layout()
fig.savefig(out)
print('saved', out)
