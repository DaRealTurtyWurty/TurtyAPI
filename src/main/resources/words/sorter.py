def sort_all_words(filename='all_words.txt'):
    with open(filename, 'r') as f:
        all_words = [line.strip() for line in f]

    all_words.sort()

    with open(filename, 'w') as f:
        for word in all_words:
            f.write(word + '\n')

sort_all_words()