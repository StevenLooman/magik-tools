#!/usr/bin/env python3

import html2text
from pathlib import Path
import re
import json

class Writer:
    FOOTER_NOTE = "\n> [!NOTE]\n> This page is generated. Any changes made to this page through the wiki will be lost in the future.\n"

    def __init__(self, file_path: Path):
        """
        Initialize the Writer class with output file path.
        """
        self.file_path = file_path

    def write_footer(self):
        """
        Write the footer to the file.
        """
        self.write_to_file(self.FOOTER_NOTE)

    def write_to_file(self, content: str):
        """
        Write the content to the file.
        """
        with self.file_path.open(mode="a", encoding="utf-8") as file:
            file.write(content)


class HTMLToMarkdown:
    def __init__(self, file_path: Path):
        """
        Initialize the HTMLToMarkdown class with HTML file path.
        """
        self.file_path = file_path
        self.markdown_content = None

    def convert_to_markdown(self):
        """
        Convert the HTML content to Markdown using html2text.
        """
        html_content = self.file_path.read_text()
        converter = html2text.HTML2Text()
        converter.body_width = 0
        markdown_content = converter.handle(html_content)
        self.markdown_content = markdown_content

    def write_to_file(self, file_path: Path):
        """
        Write the converted Markdown content to a file.
        """
        if self.markdown_content is None:
            raise ValueError(
                "Markdown content is not generated. Call convert_to_markdown() first."
            )

        file_path.parent.mkdir(parents=True, exist_ok=True)

        writer = Writer(file_path)
        writer.write_to_file(self.markdown_content)


class JavaToMarkdown:
    RULE_PROPERTY_PATTERN = r"@RuleProperty\s*\(((?:[^()]|\([^()]*\))*?)\)"
    DEFAULT_VALUE_PATTERN = r'defaultValue\s*=\s*""\s*\+\s*(DEFAULT_[^,\s]*)'
    DEFAULT_VALUE_DIRECT_PATTERN = r'defaultValue\s*=\s*"([^"]*)"'
    KEY_PATTERN = r'key\s*=\s*"([^"]*)"'
    DESCRIPTION_PATTERN = r'description\s*=\s*"([^"]*)"'
    KEBAB_CASE_PATTERN = r"(?<!^)(?=[A-Z])"
    TABLE_HEADER = "\n## Options\n\n| Option | Default value | Description |\n|--------|---------------|-------------|"

    def __init__(self, file_path: Path):
        """
        Initialize the JavaToMarkdown class with Java file_path.
        """
        self.file_path = file_path
        self.markdown_content = None

    def extract_default_value_from_property(self, content: str, property_text: str):
        """ """
        default_ref = re.search(self.DEFAULT_VALUE_PATTERN, property_text)
        if default_ref:
            constant_name = default_ref.group(1)
            constant_pattern = (
                f"private static final \\w+ {constant_name}\\s*=\\s*([^;]*?)\\s*;"
            )
            constant_match = re.search(constant_pattern, content, re.DOTALL)
            if constant_match:
                value = constant_match.group(1).replace("\n", "").replace('"', "")
                clean_value = "".join(value.split()).replace("+", "")
                return ",  ".join(item for item in clean_value.split(",") if item)

        direct_value = re.search(self.DEFAULT_VALUE_DIRECT_PATTERN, property_text)
        return direct_value.group(1) if direct_value else ""

    def extract_rule_properties(self):
        """
        Extract the RuleProperty's from the given Java file_path.
        """
        java_content = self.file_path.read_text()
        check_name = self.file_path.stem.replace("Check", "")
        check_name_as_kebab_case = re.sub(
            self.KEBAB_CASE_PATTERN, "-", check_name
        ).lower()

        properties = []

        for property in re.finditer(
            self.RULE_PROPERTY_PATTERN, java_content, re.DOTALL
        ):
            property_text = property.group(1)
            option_name = (
                re.search(self.KEY_PATTERN, property_text).group(1).replace(" ", "-")
            )

            check_name_with_option_name = f"{check_name_as_kebab_case}.{option_name}"
            description = re.search(self.DESCRIPTION_PATTERN, property_text).group(1)
            default_value = self.extract_default_value_from_property(
                java_content, property_text
            )

            properties.append(
                f"| {check_name_with_option_name} | {default_value} | {description} |"
            )

        return properties

    def convert_to_markdown(self):
        """
        Convert the Java content to Markdown by extracting the RuleProperty information.
        """

        properties = self.extract_rule_properties()
        if properties != []:
            properties.insert(0, self.TABLE_HEADER)
            java_content = "\n".join(properties) + "\n"
        else:
            java_content = ""
        self.markdown_content = java_content

    def write_to_file(self, file_path: Path, append: bool = False):
        """
        Write the converted Java content to a file.
        """
        if self.markdown_content is None:
            raise ValueError(
                "Java content is not generated. Call convert_to_markdown() first."
            )
        elif self.markdown_content == "":
            return

        writer = Writer(file_path)
        writer.write_to_file(self.markdown_content)


if __name__ == "__main__":
    output_folder = Path("wiki/checks")
    output_folder.mkdir(parents=True, exist_ok=True)

    index_file_path = output_folder / "Checks-Index.md"

    index_writer = Writer(index_file_path)
    index_writer.write_to_file("# Available checks\n")

    CHECK_TYPES = {
        "Magik checks": (
            Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/magik"),
            Path("magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magik/rules")
        ),
        "Magik typed checks": (
            Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/magiktyped"),
            Path("magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magiktyped/rules")
        ),
        "module.def checks": (
            Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/moduledef"),
            Path("magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/moduledef/rules")
        ),
        "product.def checks": (
            Path("magik-checks/src/main/java/nl/ramsolutions/sw/checks/productdef"),
            Path("magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/productdef/rules")
        ),
    }

    for check_type, (java_checks_folder, sonar_rules_folder) in CHECK_TYPES.items():
        index_writer.write_to_file(f"\n## {check_type}\n\n")

        for html_file_path in sorted(sonar_rules_folder.glob("*.html")):
            file_name = html_file_path.stem
            output_file_path = output_folder.joinpath(f"Check-{file_name}.md")

            print(f"Generating {output_file_path}")

            json_file_path = sonar_rules_folder / f"{file_name}.json"
            json_text = json_file_path.read_text()
            metadata = json.loads(json_text)
            title = metadata.get("sqKey", file_name)

            writer = Writer(output_file_path)
            writer.write_to_file(f"<!-- markdownlint-disable MD013 MD024 -->\n# `{title}` - ")

            html_to_markdown_converter = HTMLToMarkdown(html_file_path)
            html_to_markdown_converter.convert_to_markdown()
            html_to_markdown_converter.write_to_file(output_file_path)

            if check_type == "Magik typed checks":
              java_file = java_checks_folder.joinpath(f"{file_name}TypedCheck.java")
            else:
              java_file = java_checks_folder.joinpath(f"{file_name}Check.java")

            java_to_markdown_converter = JavaToMarkdown(java_file)
            java_to_markdown_converter.convert_to_markdown()
            java_to_markdown_converter.write_to_file(output_file_path)

            writer.write_footer()

            index_writer.write_to_file(f"- **[{title}](Check-{file_name})**\n")

    index_writer.write_footer()
