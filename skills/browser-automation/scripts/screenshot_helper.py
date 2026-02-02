#!/usr/bin/env python3
"""
截图辅助工具
批量截图、元素截图、全页截图
"""

import json
import sys
import argparse
from pathlib import Path
from datetime import datetime


def generate_timestamp_filename(prefix="screenshot", extension="png"):
    """
    生成带时间戳的文件名
    """
    now = datetime.now()
    timestamp = now.strftime("%Y%m%d_%H%M%S")
    return f"{prefix}_{timestamp}.{extension}"


def generate_screenshot_command(uid=None, full_page=False, output_path=None, format="png"):
    """
    生成截图命令
    """
    if output_path is None:
        output_path = generate_timestamp_filename()

    command_parts = []

    if uid:
        command_parts.append(f'take_screenshot(uid="{uid}", filePath="{output_path}", format="{format}")')
    elif full_page:
        command_parts.append(f'take_screenshot(fullPage=true, filePath="{output_path}", format="{format}")')
    else:
        command_parts.append(f'take_screenshot(filePath="{output_path}", format="{format}")')

    return '\n'.join(command_parts)


def generate_batch_screenshot_sequence(uids, output_dir="/tmp/screenshots"):
    """
    生成批量截图序列
    """
    commands = []
    commands.append(f"# 批量截图序列 ({len(uids)} 个元素)")
    commands.append(f"# 输出目录: {output_dir}")
    commands.append("")

    for i, uid in enumerate(uids, 1):
        filename = f"element_{i}_{uid.replace(':', '_')}.png"
        output_path = f"{output_dir}/{filename}"
        commands.append(f"# {i}. 截图元素 {uid}")
        commands.append(f'take_screenshot(uid="{uid}", filePath="{output_path}")')
        commands.append("")

    return '\n'.join(commands)


def main():
    parser = argparse.ArgumentParser(description='截图辅助工具')
    parser.add_argument('--uid', help='元素的 uid（元素截图）')
    parser.add_argument('--full-page', action='store_true', help='全页截图')
    parser.add_argument('--output', help='输出文件路径')
    parser.add_argument('--format', default='png', choices=['png', 'jpeg', 'webp'],
                        help='图片格式')
    parser.add_argument('--batch', help='批量模式：uid 列表文件 (JSON)')
    parser.add_argument('--batch-uids', help='批量模式：逗号分隔的 uid 列表')

    args = parser.parse_args()

    if args.batch:
        # 批量截图模式
        try:
            with open(args.batch, 'r', encoding='utf-8') as f:
                data = json.load(f)
                uids = data.get('uids', [])
        except (FileNotFoundError, json.JSONDecodeError):
            print(f"错误：无法读取或解析批量文件 {args.batch}")
            sys.exit(1)

        print(generate_batch_screenshot_sequence(uids))

    elif args.batch_uids:
        # 从命令行批量截图
        uids = [uid.strip() for uid in args.batch_uids.split(',')]
        print(generate_batch_screenshot_sequence(uids))

    else:
        # 单次截图
        if args.uid:
            print(f"\n# 元素截图 (uid: {args.uid})")
        elif args.full_page:
            print("\n# 全页截图")
        else:
            print("\n# 视口截图")

        command = generate_screenshot_command(
            uid=args.uid,
            full_page=args.full_page,
            output_path=args.output,
            format=args.format
        )
        print(command)


if __name__ == '__main__':
    main()
