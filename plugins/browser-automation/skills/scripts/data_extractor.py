#!/usr/bin/env python3
"""
数据提取助手
从快照中提取结构化数据（链接、图片、表格等）
"""

import json
import sys
import argparse
from pathlib import Path
from datetime import datetime


def extract_links(snapshot_text):
    """
    提取页面中的链接
    """
    links = []
    lines = snapshot_text.split('\n')

    for line in lines:
        if 'link' in line.lower() or 'href' in line.lower() or 'a' in line:
            parts = line.split()
            if len(parts) >= 2:
                uid = parts[0].strip('[]')
                text = ' '.join(parts[1:]).strip()
                # 简单的 URL 提取
                if 'http' in text:
                    url = text
                else:
                    url = ''
                links.append({
                    'uid': uid,
                    'text': text,
                    'url': url
                })

    return links


def extract_images(snapshot_text):
    """
    提取页面中的图片
    """
    images = []
    lines = snapshot_text.split('\n')

    for line in lines:
        if 'image' in line.lower() or 'img' in line.lower():
            parts = line.split()
            if len(parts) >= 2:
                uid = parts[0].strip('[]')
                alt_text = ' '.join(parts[1:]).strip()
                images.append({
                    'uid': uid,
                    'alt': alt_text
                })

    return images


def extract_tables(snapshot_text):
    """
    提取页面中的表格
    """
    tables = []
    lines = snapshot_text.split('\n')

    for line in lines:
        if 'table' in line.lower():
            parts = line.split()
            if len(parts) >= 2:
                uid = parts[0].strip('[]')
                tables.append({
                    'uid': uid,
                    'caption': ' '.join(parts[1:]).strip()
                })

    return tables


def generate_extraction_script(extract_type, elements):
    """
    生成 JavaScript 提取脚本
    """
    if extract_type == 'links':
        script = """evaluate_script(function=\"\"\"
() => {
    const links = Array.from(document.querySelectorAll('a'));
    return links.map((a, index) => ({
        index: index,
        text: a.textContent.trim(),
        href: a.href,
        target: a.target || '_self'
    }));
}
\""")
"""
    elif extract_type == 'images':
        script = """evaluate_script(function=\"\"\"
() => {
    const images = Array.from(document.querySelectorAll('img'));
    return images.map((img, index) => ({
        index: index,
        src: img.src,
        alt: img.alt || '',
        width: img.width,
        height: img.height
    }));
}
\""")
"""
    elif extract_type == 'tables':
        script = """evaluate_script(function=\"\"\"
() => {
    const tables = Array.from(document.querySelectorAll('table'));
    return tables.map((table, index) => {
        const rows = Array.from(table.querySelectorAll('tr'));
        return {
            index: index,
            rowCount: rows.length,
            headers: Array.from(rows[0]?.querySelectorAll('th, td') || []).map(th => th.textContent.trim())
        };
    });
}
\""")
"""
    else:
        script = "# 未知类型"

    return script


def main():
    parser = argparse.ArgumentParser(description='数据提取助手')
    parser.add_argument('--type', required=True,
                        choices=['links', 'images', 'tables'],
                        help='提取类型')
    parser.add_argument('--snapshot', help='快照文件路径（可选，用于预览）')
    parser.add_argument('--output', help='输出 JSON 文件路径')

    args = parser.parse_args()

    # 如果提供了快照文件，先分析预览
    if args.snapshot:
        try:
            with open(args.snapshot, 'r', encoding='utf-8') as f:
                snapshot_text = f.read()
        except FileNotFoundError:
            print(f"错误：找不到快照文件 {args.snapshot}")
            sys.exit(1)

        if args.type == 'links':
            elements = extract_links(snapshot_text)
        elif args.type == 'images':
            elements = extract_images(snapshot_text)
        else:
            elements = extract_tables(snapshot_text)

        print(f"\n# 在快照中找到 {len(elements)} 个元素")

    # 生成提取脚本
    script = generate_extraction_script(args.type, None if not args.snapshot else elements)

    print(f"\n# {args.type.upper()} 提取脚本：")
    print(script)

    print(f"\n# 使用示例：")
    print(f"# 1. 先获取页面快照")
    print(f"take_snapshot()")
    print(f"# 2. 运行上述提取脚本")
    print(f"# 3. 结果会自动返回")


if __name__ == '__main__':
    main()
