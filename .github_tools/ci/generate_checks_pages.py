#!/usr/bin/env python3
"""Generate Markdown pages for checks based on Java and JSON files."""

from pathlib import Path
import re
import json

import html2text

OUTPUT_FOLDER = Path("wiki/checks")
INDEX_FILE = OUTPUT_FOLDER / "Checks-Index.md"
CHECK_TYPES = {
    "Magik checks": (
        Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/magik"),
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magik/rules"
        ),
    ),
    "Magik typed checks": (
        Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/magiktyped"),
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magiktyped/rules"
        ),
    ),
    "module.def checks": (
        Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/moduledef"),
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/moduledef/rules"
        ),
    ),
    "product.def checks": (
        Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/productdef"),
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/productdef/rules"
        ),
    ),
    "Load list checks": (
        Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/loadlist"),
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/loadlist/rules"
        ),
    ),
}
FOOTER_NOTE = \
    """\n> [!NOTE]\n> This page is generated. """ \
    """Any changes made to this page through the wiki will be lost in the future.\n"""
TABLE_HEADER = (
    """\n## Options\n\n| Option | Default value | Description |\n| --- | --- | --- |"""
)


def write_file(file_path: Path, content: str):
    """Write (overwrite) content to a file."""
    file_path.write_text(content, encoding="utf-8")


def html_to_markdown(file_path: Path) -> str:
    """Convert an HTML file to Markdown."""
    html_content = file_path.read_text(encoding="utf-8")
    converter = html2text.HTML2Text()
    converter.body_width = 0
    return converter.handle(html_content)


def extract_default_value(java_content: str, property_text: str) -> str:
    """Extract default value from @RuleProperty."""
    default_ref = re.search(
        r'defaultValue\s*=\s*""\s*\+\s*(DEFAULT_[^,\s]*)', property_text
    )
    if default_ref:
        constant_name = default_ref.group(1)
        pattern = f"private static final \\w+ {constant_name}\\s*=\\s*([^;]*?)\\s*;"
        match = re.search(pattern, java_content, re.DOTALL)
        if match:
            value = match.group(1).replace("\n", "").replace('"', "")
            clean_value = "".join(value.split()).replace("+", "")
            return ",  ".join(item for item in clean_value.split(",") if item)

    direct = re.search(r'defaultValue\s*=\s*"([^"]*)"', property_text)
    return direct.group(1) if direct else ""


def extract_rule_properties(java_file: Path) -> list[str]:
    """Extract @RuleProperty details from a Java file."""
    java_content = java_file.read_text(encoding="utf-8")
    check_name = java_file.stem.replace("Check", "")
    kebab_case = re.sub(r"(?<!^)(?=[A-Z])", "-", check_name).lower()

    props: list[str] = []
    for prop in re.finditer(
        r"@RuleProperty\s*\(((?:[^()]|\([^()]*\))*?)\)", java_content, re.DOTALL
    ):
        text = prop.group(1)
        key_match = re.search(r'key\s*=\s*"([^"]*)"', text)
        desc_match = re.search(r'description\s*=\s*"([^"]*)"', text)
        if not key_match or not desc_match:
            continue

        option_name = key_match.group(1).replace(" ", "-")
        description = desc_match.group(1)
        default_value = extract_default_value(java_content, text)
        default_value = f"| {default_value} |" if default_value != "" else "| |"

        props.append(f"| {kebab_case}.{option_name} {default_value} {description} |")

    return props


def java_to_markdown(java_file: Path) -> str:
    """Convert Java @RuleProperty annotations to Markdown table."""
    properties = extract_rule_properties(java_file)
    if not properties:
        return ""
    return "\n".join([TABLE_HEADER, *properties, ""]) + "\n"


def generate_markdown_pages():
    """Generate Markdown pages for all checks and an index page."""
    # pylint: disable=too-many-locals
    OUTPUT_FOLDER.mkdir(parents=True, exist_ok=True)

    index_content = ["# Available checks\n"]
    for check_type, (java_folder, sonar_folder) in CHECK_TYPES.items():
        index_content.append(f"\n## {check_type}\n\n")

        for json_file in sorted(sonar_folder.glob("*.json")):
            file_name = json_file.stem
            output_file = OUTPUT_FOLDER / f"Check-{file_name}.md"
            print(f"Generating {output_file}.")

            json_content = json_file.read_text(encoding="utf-8")
            metadata = json.loads(json_content)
            title = metadata.get("sqKey", file_name)

            html_file = sonar_folder / f"{file_name}.html"
            html_parts = html_to_markdown(html_file)

            java_file = java_folder / (
                f"{file_name}TypedCheck.java"
                if check_type == "Magik typed checks"
                else f"{file_name}Check.java"
            )
            java_parts = java_to_markdown(java_file)

            parts = [
                "<!-- markdownlint-disable MD013 MD024 -->",
                f"# `{title}` - " + html_parts,
                java_parts,
                FOOTER_NOTE,
            ]

            joined_parts = "\n".join(part for part in parts if part)

            write_file(output_file, joined_parts)
            index_content.append(f"- **[{title}](Check-{file_name})**\n")

    index_content.append(FOOTER_NOTE)
    write_file(INDEX_FILE, "".join(index_content))


if __name__ == "__main__":
    generate_markdown_pages()
