#!/usr/bin/env python3

import html2text
from pathlib import Path
import re


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

    def write_to_file(self, content: str, overwrite: bool = False):
        """
        Write the content to the file.
        """
        mode = "w" if overwrite else "a"
        with self.file_path.open(mode=mode, encoding="utf-8") as file:
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

    def write_to_file(self, file_path: Path, overwrite: bool):
        """
        Write the converted Markdown content to a file.
        """
        if self.markdown_content is None:
            raise ValueError(
                "Markdown content is not generated. Call convert_to_markdown() first."
            )

        file_path.parent.mkdir(parents=True, exist_ok=True)

        writer = Writer(file_path)
        writer.write_to_file(self.markdown_content, overwrite)


class JavaToMarkdown:
    RULE_PROPERTY_PATTERN = r"@RuleProperty\s*\(((?:[^()]|\([^()]*\))*?)\)"
    DEFAULT_VALUE_PATTERN = r'defaultValue\s*=\s*""\s*\+\s*(DEFAULT_[^,\s]*)'
    DEFAULT_VALUE_DIRECT_PATTERN = r'defaultValue\s*=\s*"([^"]*)"'
    KEY_PATTERN = r'key\s*=\s*"([^"]*)"'
    DESCRIPTION_PATTERN = r'description\s*=\s*"([^"]*)"'
    KEBAB_CASE_PATTERN = r"(?<!^)(?=[A-Z])"

    TABLE_HEADER = "## Options\n\n| Option | Default value | Description |\n|--------|---------------|-------------|"


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

    def write_to_file(self, file_path: Path, overwrite: bool = False):
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
        writer.write_to_file(self.markdown_content, overwrite)


if __name__ == "__main__":
    output_folder = Path("wiki/checks")
    if not output_folder.exists():
        output_folder.mkdir()

    index_file_path = output_folder / "Checks-Index.md"

    index_writer = Writer(index_file_path)
    index_writer.write_to_file("# Available checks\n\n", True)

    java_checks_folder = Path(
        "magik-checks/src/main/java/nl/ramsolutions/sw/magik/checks/checks"
    )

    for html_file_path in sorted(
        Path(
            "magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magik/rules"
        ).glob("*.html")
    ):
        html_file_name = html_file_path.stem
        output_file_path = output_folder.joinpath(f"Check-{html_file_name}.md")

        print(f"Generating {output_file_path}")

        converter = HTMLToMarkdown(html_file_path)
        converter.convert_to_markdown()
        converter.write_to_file(output_file_path, True)

        java_file = java_checks_folder.joinpath(f"{html_file_name}Check.java")
        if java_file.exists():
            converter = JavaToMarkdown(java_file)
            converter.convert_to_markdown()
            converter.write_to_file(output_file_path)

        writer = Writer(output_file_path)
        writer.write_footer()

        index_writer.write_to_file(f"- [[{html_file_name}|Check-{html_file_name}]]\n")

    index_writer.write_footer()
