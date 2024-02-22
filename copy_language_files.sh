#!/bin/bash
cd src/main/resources/assets/blindfetchrcompanion/lang

declare -A languages=(
	["de"]="at ch"
)

for standard in "${!languages[@]}"; do
	declare -a dialects=(${languages[$standard]})

	for dialect in "${dialects[@]}"; do
		cp "${standard}_${standard}.json" "${standard}_${dialect}.json"
	done
done