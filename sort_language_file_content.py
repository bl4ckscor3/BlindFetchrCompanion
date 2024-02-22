languages = ["de_de", "en_us"]

for language in languages:
	file_name = "src/main/resources/assets/blindfetchrcompanion/lang/" + language + ".json"

	with open(file_name, encoding="UTF-8") as file:
		lines = file.readlines()
		lines.sort()
		lines.insert(0, "{\n")
		del lines[len(lines) - 2]

	with open(file_name, "w", encoding="UTF-8") as file:
		for line in lines:
			file.write(line)