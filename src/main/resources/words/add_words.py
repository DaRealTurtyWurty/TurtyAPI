all_words = []
with open('all_words.txt', 'r') as f:
    for line in f:
        all_words.append(line.strip())

new_words = []
with open('new_words.txt', 'r') as f:
    for line in f:
        word = line.strip()
        if word not in all_words:
            all_words.append(word)

with open('all_words.txt', 'w') as f:
    for word in all_words:
        f.write(word + '\n')