#!/usr/bin/env python3

import glob
import os
import re


def extract_default_value(content, property_text):
    default_ref = re.search(
        r'defaultValue\s*=\s*""\s*\+\s*(DEFAULT_[^,\s]*)', property_text
    )
    if default_ref:
        constant_name = default_ref.group(1)
        constant_pattern = (
            f"private static final \w+ {constant_name}\s*=\s*([^;]*?)\s*;"
        )
        constant_match = re.search(constant_pattern, content, re.DOTALL)
        if constant_match:
            value = constant_match.group(1).replace("\n", "").replace('"', "")
            clean_value = "".join(value.split()).replace("+", "")
            return ",  ".join(item for item in clean_value.split(",") if item)

    direct_value = re.search(r'defaultValue\s*=\s*"([^"]*)"', property_text)
    return direct_value.group(1) if direct_value else ""


def extract_rule_options(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    check_name = os.path.basename(file_path).replace(".java", "").replace("Check", "")
    check_name_as_kebab_case = re.sub(r"(?<!^)(?=[A-Z])", "-", check_name).lower()
    wiki_link = f"[{check_name}](Check-{check_name})"

    options = []

    for property in re.finditer(
        r"@RuleProperty\s*\(((?:[^()]|\([^()]*\))*?)\)", content, re.DOTALL
    ):
        property_text = property.group(1)
        option_name = (
            re.search(r'key\s*=\s*"([^"]*)"', property_text).group(1).replace(" ", "-")
        )
        description = re.search(r'description\s*=\s*"([^"]*)"', property_text).group(1)
        default_value = extract_default_value(content, property_text)

        options.append(
            f"| {wiki_link} | {check_name_as_kebab_case}.{option_name} | {default_value} | {description} |"
        )

    return options


with open("wiki/checks/Checks-Options.md", "w") as f:
    f.write("# Options per check\n\n")
    f.write("| Check name | Option name | Default value | Description |\n")
    f.write("|------------|-------------|---------------|-------------|\n")

    for java_file in sorted(
        glob.glob(
            "magik-checks/src/main/java/nl/ramsolutions/sw/magik/checks/checks/*Check.java"
        )
    ):
        class_name = os.path.basename(java_file).replace(".java", "")
        print(f"Generating properties for {class_name}")
        options = extract_rule_options(java_file)
        if options:
            f.write("\n".join(options) + "\n")

    f.write(
        "\n> [!NOTE]\n> This page is generated. Any changes made to this page through the wiki will be lost in the future.\n"
    )
