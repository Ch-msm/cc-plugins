#!/usr/bin/env python3
"""
表单自动填写助手
根据字段类型自动选择填写策略
"""

import json
import sys
import argparse
from pathlib import Path


def analyze_snapshot(snapshot_text):
    """
    分析快照文件，提取表单元素
    """
    elements = []
    lines = snapshot_text.split('\n')

    for line in lines:
        # 简单解析：查找输入框、下拉框等表单元素
        # 实际格式取决于 take_snapshot 的输出
        if 'textbox' in line.lower() or 'input' in line.lower():
            parts = line.split()
            if parts:
                uid = parts[0].strip('[]')
                label = ' '.join(parts[1:]).strip()
                elements.append({
                    'uid': uid,
                    'type': 'text',
                    'label': label
                })
        elif 'combobox' in line.lower() or 'select' in line.lower():
            parts = line.split()
            if parts:
                uid = parts[0].strip('[]')
                label = ' '.join(parts[1:]).strip()
                elements.append({
                    'uid': uid,
                    'type': 'select',
                    'label': label
                })

    return elements


def match_fields(elements, data):
    """
    根据标签匹配数据到表单字段
    """
    fills = []

    for elem in elements:
        label_lower = elem['label'].lower()
        uid = elem['uid']

        # 关键词匹配
        for key, value in data.items():
            key_lower = key.lower()
            if key_lower in label_lower or label_lower in key_lower:
                fills.append({
                    'uid': uid,
                    'value': str(value),
                    'field': key
                })
                break

    return fills


def generate_fill_commands(fills):
    """
    生成 fill_form 命令
    """
    elements = [{'uid': f['uid'], 'value': f['value']} for f in fills]

    print("\n# 生成的 fill_form 命令：")
    print("fill_form(elements=[")
    for elem in elements:
        print(f'    {{"uid": "{elem["uid"]}", "value": "{elem["value"]}"}},')
    print("])")

    return elements


def main():
    parser = argparse.ArgumentParser(description='表单自动填写助手')
    parser.add_argument('--snapshot', required=True, help='快照文件路径')
    parser.add_argument('--data', required=True, help='表单数据 (JSON 格式)')
    parser.add_argument('--output', help='输出文件路径')

    args = parser.parse_args()

    # 读取快照
    try:
        with open(args.snapshot, 'r', encoding='utf-8') as f:
            snapshot_text = f.read()
    except FileNotFoundError:
        print(f"错误：找不到快照文件 {args.snapshot}")
        sys.exit(1)

    # 解析数据
    try:
        data = json.loads(args.data)
    except json.JSONDecodeError:
        print(f"错误：无效的 JSON 格式")
        sys.exit(1)

    # 分析快照
    print(f"\n# 分析快照：{args.snapshot}")
    elements = analyze_snapshot(snapshot_text)
    print(f"# 找到 {len(elements)} 个表单元素")

    # 匹配字段
    fills = match_fields(elements, data)
    print(f"\n# 匹配到 {len(fills)} 个字段：")
    for fill in fills:
        print(f"#   {fill['field']}: {fill['value']} (uid: {fill['uid']})")

    # 生成命令
    elements_cmd = generate_fill_commands(fills)

    # 输出文件
    if args.output:
        result = {
            'matched_fields': fills,
            'fill_command_elements': elements_cmd,
            'unmatched_fields': [k for k in data.keys() if k not in [f['field'] for f in fills]]
        }
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(f"\n# 结果已保存到 {args.output}")


if __name__ == '__main__':
    main()
