#!/usr/bin/env python3

import csv
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

# Set publication-quality font settings
matplotlib.rcParams['pdf.fonttype'] = 42
matplotlib.rcParams['ps.fonttype'] = 42
matplotlib.rcParams.update({'font.size': 13}) # Slightly smaller base font for narrower plot

# File Configuration
csv_file = "cold_start_results.csv"
output_file = "cold-starts.pdf"

# Raw data parsing containers
raw_data = {
    "Mosaic": [],
    "Uber": [],
    "XaaS": []
}

# 1. Parse CSV
try:
    with open(csv_file, mode='r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            mode = row['mode'].strip()
            if mode in raw_data:
                raw_data[mode].append(float(row['latency_ms']))
except FileNotFoundError:
    print(f"Error: Could not find {csv_file} in the current directory.")
    import sys
    sys.exit(1)

# 2. Process Statistics using NumPy
# Map the raw data keys to the new synchronized display order
data_mapping = ["Mosaic", "XaaS", "Uber", "Uber", "Uber"]
display_labels = ["Tessera", "XaaS", "Uber", "CPU-binding", "ISA-binding"]

means = []
std_devs = []

for mode in data_mapping:
    data_points = np.array(raw_data[mode])
    mean_val = np.mean(data_points)
    std_val = np.std(data_points, ddof=1)  # Sample standard deviation

    means.append(mean_val)
    std_devs.append(std_val)

# 3. Plotting Configuration
colors = ['#2ca02c', '#8c564b', '#9467bd', '#1f77b4', '#ff7f0e']
hatches = ['', 'o', '.', '//', '\\\\']

# Narrower figure for side-by-side column placement
fig, ax = plt.subplots(figsize=(5.5, 4))

x_pos = np.arange(len(display_labels))

# Generate the bar chart
bars = ax.bar(
    x_pos,
    means,
    yerr=std_devs,
    color=colors,
    hatch=hatches,
    width=0.65,
    edgecolor='black',
    linewidth=1.2,
    alpha=0.85,
    capsize=5,
    error_kw={'ecolor': 'black', 'lw': 1.5}
)

# Configuration adjustments
ax.set_yscale('log')
ax.set_ylabel("Cold Start Latency (ms)")
ax.grid(axis='y', linestyle='--', alpha=0.5)

# Rotate labels to fit the narrower width
ax.set_xticks(x_pos)
ax.set_xticklabels(display_labels, rotation=30, ha='right')

# Add numeric text labels directly over the bars
for bar in bars:
    yval = bar.get_height()
    offset_y = yval * 1.35 # Multiplicative offset for log scale

    if yval > 1000:
        label_text = f'{yval/1000:.2f} s'
    else:
        label_text = f'{yval:.0f} ms'

    ax.text(
        bar.get_x() + bar.get_width()/2,
        offset_y,
        label_text,
        ha='center',
        va='bottom',
        fontsize=11, # Scaled down to prevent overlap
        fontweight='bold'
    )

ax.set_ylim(ymin=100, ymax=max(means) * 4)

plt.tight_layout()
plt.savefig(output_file, bbox_inches="tight")
print(f"Plot saved successfully to: '{output_file}'")
