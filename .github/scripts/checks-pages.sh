#!/usr/bin/env bash

checks_index_path="wiki/checks/Checks-Index.md"
echo "Checks available:" > "$checks_index_path"

add_footer() {
    {
        echo ""
        echo "> [!NOTE]"
        echo "> This page is generated. Any changes made to this page through the wiki will be lost in the future."
    } >> "$1"
}

for f in magik-checks/src/main/resources/nl/ramsolutions/sw/sonar/l10n/magik/rules/*.html; do
    name=$(basename "$f" .html)

    # Generate individual check page
    path=wiki/checks/Check-$name.md
    echo "Generating $path"

    html2text -b 0 "$f" > "$path"
    add_footer "$path"

    # Add entry to index
    echo "" >> "$checks_index_path"
    echo "[[$name|Check-$name]]" >> "$checks_index_path"
done

add_footer "$checks_index_path"